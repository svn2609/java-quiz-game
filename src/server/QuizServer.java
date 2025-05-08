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
    private static List<Socket> lobbyClients = new ArrayList<>();
    private static CountDownLatch startLatch = new CountDownLatch(1); // waits for admin to start

    public static void main(String[] args) {
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

            // Listen for "start" command in separate thread
            new Thread(() -> {
                try {
                    while (true) {
                        String command = adminIn.readLine();
                        if ("start".equalsIgnoreCase(command)) {
                            System.out.println("✅ Admin started the quiz.");
                            startLatch.countDown(); // release all players
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Step 3: Wait for admin to start
            System.out.println("🕒 Waiting for admin to start the quiz...");
            startLatch.await(); // block here until admin types 'start'

            // Step 4: Notify all players
            for (Socket clientSocket : lobbyClients) {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println("START");
            }

            // Step 5: Start quiz threads
            for (Socket clientSocket : lobbyClients) {
                ClientHandler handler = new ClientHandler(clientSocket, leaderboard);
                new Thread(handler).start();
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
