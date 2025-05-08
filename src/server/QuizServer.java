package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class QuizServer {
    private static final int PORT = 12345;
    private static final int MAX_PLAYERS = 3; // Customize how many players to wait for
    public static final int TOTAL_PLAYERS = 3; // change as needed
    public static final Object leaderboardLock = new Object();
    public static int finishedPlayers = 0;
    public static List<PrintWriter> allClientWriters = new ArrayList<>();


    public static ConcurrentMap<String, Integer> leaderboard = new ConcurrentHashMap<>();
    private static List<Socket> lobbyClients = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Quiz Server started on port " + PORT);
        System.out.println("Waiting for " + MAX_PLAYERS + " players to join...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            // Step 1: Collect clients until MAX_PLAYERS are in
            while (lobbyClients.size() < MAX_PLAYERS) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Player connected from: " + clientSocket.getInetAddress());
                lobbyClients.add(clientSocket);
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println("✅ Waiting for more players to join... (" + lobbyClients.size() + "/" + MAX_PLAYERS + ")");
            }

            // Step 2: Notify all players the quiz is starting
            System.out.println("All players joined! Starting quiz...");
            for (Socket clientSocket : lobbyClients) {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println("START");
            }

            // Step 3: Start one thread per client
            for (Socket clientSocket : lobbyClients) {
                ClientHandler handler = new ClientHandler(clientSocket, leaderboard);
                new Thread(handler).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
