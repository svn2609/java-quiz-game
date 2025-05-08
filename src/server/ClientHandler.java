package server;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.SocketTimeoutException;
import utilities.Question;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ConcurrentMap<String, Integer> leaderboard;
    private List<Question> questions;

    public ClientHandler(Socket socket, ConcurrentMap<String, Integer> leaderboard) {
        this.clientSocket = socket;
        this.leaderboard = leaderboard;
        this.questions = fetchQuestionsFromDB();
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            out.println("Enter your name:");
            String name = in.readLine();
            System.out.println("Player joined: " + name);
            out.println("Welcome to the Real-Time Quiz, " + name + "!");

            int score = 0;
            for (Question q : questions) {
                try { Thread.sleep(300); } catch (InterruptedException e) {}

                out.println("You have 10 seconds to answer:");
                out.println("QUESTION: " + q.getQuestionText());

                for (String opt : q.getFormattedOptions()) {
                    out.println(opt);
                }
                out.println("Enter option number (1–4):");

                clientSocket.setSoTimeout(10000);
                String response = null;
                try {
                    response = in.readLine();
                } catch (SocketTimeoutException e) {
                    out.println("⏰ Time's up! Moving to next question.");
                }

                if (response != null) {
                    try {
                        int answer = Integer.parseInt(response.trim()) - 1;
                        if (q.isCorrect(answer)) {
                            score++;
                            out.println("✅ Correct!");
                        } else {
                            out.println("❌ Incorrect.");
                        }
                    } catch (NumberFormatException e) {
                        out.println("Invalid input.");
                    }
                }

                out.println("------");
            }

            leaderboard.put(name, score);
            saveScoreToDB(name, score);
            out.println("Quiz over! Your score: " + score + "/" + questions.size());

            // Register this writer
            synchronized (QuizServer.allClientWriters) {
                QuizServer.allClientWriters.add(out);
            }

            // Wait for all players to finish
            synchronized (QuizServer.leaderboardLock) {
                QuizServer.finishedPlayers++;
                if (QuizServer.finishedPlayers < QuizServer.TOTAL_PLAYERS) {
                    try {
                        QuizServer.leaderboardLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    QuizServer.leaderboardLock.notifyAll();

                    List<String> sorted = leaderboard.entrySet().stream()
                        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                        .map(e -> e.getKey() + ": " + e.getValue())
                        .toList();

                    for (PrintWriter writer : QuizServer.allClientWriters) {
                        writer.println("🏆 Leaderboard:");
                        for (String s : sorted) {
                            writer.println(s);
                        }
                        writer.println(); // End
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Question> fetchQuestionsFromDB() {
        List<Question> list = new ArrayList<>();
        String dbUrl = "jdbc:sqlite:quiz.db";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM questions")) {

            while (rs.next()) {
                String question = rs.getString("question");
                String[] options = {
                    rs.getString("option1"),
                    rs.getString("option2"),
                    rs.getString("option3"),
                    rs.getString("option4")
                };
                int correctIndex = rs.getInt("answer_index") - 1;
                list.add(new Question(question, options, correctIndex));
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to load questions from DB");
            e.printStackTrace();
        }

        return list;
    }

    private void saveScoreToDB(String name, int score) {
        String sql = "INSERT INTO scores(name, score) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:quiz.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, score);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Failed to save score");
            e.printStackTrace();
        }
    }
}
