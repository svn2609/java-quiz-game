package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class QuizServer {
    private static final int PORT = 12345;
    private static final int MAX_PLAYERS = 3;
    public static final int TOTAL_PLAYERS = MAX_PLAYERS;

    public static final Object leaderboardLock = new Object();
    public static int finishedPlayers = 0;
    public static List<PrintWriter> allClientWriters = new ArrayList<>();

    public static ConcurrentMap<String, Integer> leaderboard = new ConcurrentHashMap<>();
    private static final List<Socket> lobbyClients = new ArrayList<>();
    private static final CountDownLatch startLatch = new CountDownLatch(1); // waits for admin to start

    public static void main(String[] args) {
        DatabaseInitializer.initializeDatabase();

        System.out.println("Quiz Server started on port " + PORT);
        System.out.println("Waiting for " + MAX_PLAYERS + " players and 1 admin to join...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            // Step 1: Accept PLAYER clients
            while (lobbyClients.size() < MAX_PLAYERS) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Player connected from: " + clientSocket.getInetAddress());
                lobbyClients.add(clientSocket);
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println("✅ Waiting for more players to join... (" + lobbyClients.size() + "/" + MAX_PLAYERS + ")");
            }

            // Step 2: Accept ADMIN connection
            Socket adminSocket = serverSocket.accept();
            System.out.println("✅ Admin connected: " + adminSocket.getInetAddress());
            BufferedReader adminIn = new BufferedReader(new InputStreamReader(adminSocket.getInputStream()));
            PrintWriter adminOut = new PrintWriter(adminSocket.getOutputStream(), true);
            adminOut.println("✅ Connected as Admin. Type 'start' to begin the quiz.");

            // Step 3: Listen for 'start' command from admin
            new Thread(() -> {
                try {
                    while (true) {
                        String command = adminIn.readLine();
                        if ("start".equalsIgnoreCase(command)) {
                            System.out.println("✅ Admin started the quiz.");
                            startLatch.countDown(); // unblock the game start
                            break;
                        } else {
                            adminOut.println("❌ Invalid command. Type 'start' to begin.");
                        }
                    }
                } catch (IOException e) {
                    System.err.println("❌ Admin command error.");
                    e.printStackTrace();
                }
            }).start();

            // Step 4: Wait for admin to start
            System.out.println("🕒 Waiting for admin to start the quiz...");
            startLatch.await(); // block until 'start' is received

            // Step 5: Notify all players to start
            for (Socket clientSocket : lobbyClients) {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println("START");
            }

            // Step 6: Launch quiz for each player
            for (Socket clientSocket : lobbyClients) {
                ClientHandler handler = new ClientHandler(clientSocket, leaderboard);
                new Thread(handler).start();
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Server error occurred.");
            e.printStackTrace();
        }
    }
}
