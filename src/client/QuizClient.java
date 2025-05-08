package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class QuizClient {
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 12345;

        try (
            Socket socket = new Socket(serverAddress, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in)
        ) {
            boolean collectingLeaderboard = false;
            StringBuilder leaderboard = new StringBuilder();

            while (true) {
                String serverMessage = in.readLine();
                if (serverMessage == null) break;

                if (serverMessage.contains("Enter your name")) {
                    System.out.print("Server: " + serverMessage + "\n> ");
                    String name = scanner.nextLine();
                    out.println(name);
                } else if (serverMessage.startsWith("Enter option number")) {
                    System.out.print("Your answer (1–4): ");
                    String answer = scanner.nextLine();
                    out.println(answer);
                } else if (serverMessage.startsWith("🏆 Leaderboard:")) {
                    collectingLeaderboard = true;
                    leaderboard = new StringBuilder();
                    leaderboard.append(serverMessage).append("\n");
                } else if (collectingLeaderboard) {
                    leaderboard.append(serverMessage).append("\n");
                    if (!in.ready()) {
                        collectingLeaderboard = false;
                        System.out.println("\n" + leaderboard);
                        break;
                    }
                } else {
                    System.out.println("Server: " + serverMessage);
                }
            }

        } catch (IOException e) {
            System.err.println("Disconnected from server.");
            e.printStackTrace();
        }
    }
}
