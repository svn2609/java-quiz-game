package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class AdminClient {
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 12345;

        try (
            Socket socket = new Socket(serverAddress, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("✅ Connected to Quiz Server as Admin.");

            // Print any initial server message
            String welcome = in.readLine();
            if (welcome != null) System.out.println("Server: " + welcome);

            // Wait for admin to type 'start'
            while (true) {
                System.out.print("Type 'start' to begin the quiz: ");
                String command = scanner.nextLine();
                if ("start".equalsIgnoreCase(command)) {
                    out.println("start");
                    System.out.println("✅ Quiz start signal sent.");
                    break;
                } else {
                    System.out.println("❌ Invalid command. Only 'start' is accepted.");
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Admin connection failed.");
            e.printStackTrace();
        }
    }
}
