package server;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import utilities.Question;
import java.net.SocketTimeoutException;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ConcurrentMap<String, Integer> leaderboard;

    private List<Question> questions = Arrays.asList(
        new Question("What is the capital of France?",
            new String[]{"Berlin", "London", "Paris", "Madrid"}, 2),
        new Question("Which planet is known as the Red Planet?",
            new String[]{"Earth", "Mars", "Jupiter", "Saturn"}, 1),
        new Question("What is 5 + 7?",
            new String[]{"10", "12", "14", "15"}, 1)
    );

    public ClientHandler(Socket socket, ConcurrentMap<String, Integer> leaderboard) {
        this.clientSocket = socket;
        this.leaderboard = leaderboard;
    }
    
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
                out.flush();

                for (String opt : q.getFormattedOptions()) {
                    out.println(opt);
                }
                out.println("Enter option number (1–4):");

                String response = null;
                clientSocket.setSoTimeout(10000);
                try {
                    response = in.readLine();
                } catch (SocketTimeoutException e) {
                    out.println("⏰ Time's up! Moving to next question.");
                } catch (IOException e) {
                    e.printStackTrace();
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
            out.println("Quiz over! Your score: " + score + "/" + questions.size());

         // Register this client's writer to broadcast later
         synchronized (QuizServer.allClientWriters) {
             QuizServer.allClientWriters.add(out);
         }

         // Track finished clients
         synchronized (QuizServer.leaderboardLock) {
             QuizServer.finishedPlayers++;

             if (QuizServer.finishedPlayers < QuizServer.TOTAL_PLAYERS) {
                 // Wait until everyone finishes
                 try {
                     QuizServer.leaderboardLock.wait();
                 } catch (InterruptedException e) {
                     e.printStackTrace();
                 }
             } else {
                 // Last player finished — notify all and broadcast leaderboard
                 QuizServer.leaderboardLock.notifyAll();

                 List<String> sortedEntries = leaderboard.entrySet().stream()
                     .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                     .map(entry -> entry.getKey() + ": " + entry.getValue())
                     .toList();

                 for (PrintWriter writer : QuizServer.allClientWriters) {
                     writer.println("🏆 Leaderboard:");
                     for (String s : sortedEntries) {
                         writer.println(s);
                     }
                     writer.println(); // signals end of leaderboard
                 }
             }
         }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}