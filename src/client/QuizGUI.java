package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class QuizGUI extends JFrame {
    private BufferedReader in;
    private PrintWriter out;
    private JFrame frame;
    private JLabel questionLabel;
    private JButton[] optionButtons;
    private JLabel statusLabel;
    private int selectedOption = -1;
    private boolean collectingLeaderboard = false;
    private StringBuilder leaderboardText = new StringBuilder();
    private boolean gameStarted = false;
    private JLabel timerLabel;
    private Timer countdownTimer;
    private int timeLeft = 10;

    public QuizGUI(String serverAddress, int port) {
        try {
            Socket socket = new Socket(serverAddress, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            initUI();
            handleServerMessages();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initUI() {
        frame = new JFrame("Real-Time Quiz Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 300);
        frame.setLayout(new BorderLayout());

        questionLabel = new JLabel("Waiting for game to start...", SwingConstants.CENTER);
        questionLabel.setFont(new Font("Arial", Font.BOLD, 16));
        frame.add(questionLabel, BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel(new GridLayout(2, 2));
        optionButtons = new JButton[4];

        for (int i = 0; i < 4; i++) {
            final int index = i;
            optionButtons[i] = new JButton("Option " + (i + 1));
            optionButtons[i].setFont(new Font("Arial", Font.PLAIN, 14));
            optionButtons[i].addActionListener(e -> {
                selectedOption = index + 1;
                out.println(selectedOption);
                disableButtons();
                if (countdownTimer != null && countdownTimer.isRunning()) {
                    countdownTimer.stop();
                }
            });
            optionsPanel.add(optionButtons[i]);
        }

        frame.add(optionsPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new GridLayout(2, 1));
        statusLabel = new JLabel("✅ Waiting for more players to join...", SwingConstants.CENTER);
        timerLabel = new JLabel("", SwingConstants.CENTER);
        bottomPanel.add(statusLabel);
        bottomPanel.add(timerLabel);

        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void handleServerMessages() {
        new Thread(() -> {
            try {
                while (true) {
                    String line = in.readLine();
                    if (line == null) break;

                    if (line.equals("START")) {
                        gameStarted = true;
                        SwingUtilities.invokeLater(() -> {
                            questionLabel.setText("✅ All players joined. Starting quiz...");
                            statusLabel.setText("Quiz started!");
                        });
                        continue;
                    }

                    if (!gameStarted) {
                        statusLabel.setText("✅ " + line);
                        continue;
                    }

                    if (line.contains("Enter your name")) {
                        String name = JOptionPane.showInputDialog(frame, "Enter your name:");
                        out.println(name);
                    } else if (line.startsWith("You have")) {
                        selectedOption = -1;
                        enableButtons();
                        statusLabel.setText("⏳ " + line);
                        startCountdown();
                    } else if (line.startsWith("Enter option number")) {
                        // Ignore — we use buttons
                    } else if (line.trim().matches("^[1-4]\\. .*")) {
                        int index = Integer.parseInt(line.substring(0, 1)) - 1;
                        optionButtons[index].setText(line);
                    } else if (line.startsWith("✅") || line.startsWith("❌") || line.contains("Time")) {
                        statusLabel.setText(line);
                    } else if (line.startsWith("🏆 Leaderboard:")) {
                        collectingLeaderboard = true;
                        leaderboardText = new StringBuilder("🏆 Leaderboard:\n");
                    } else if (collectingLeaderboard) {
                        if (line.trim().isEmpty()) {
                            collectingLeaderboard = false;
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(frame, leaderboardText.toString(), "Leaderboard", JOptionPane.INFORMATION_MESSAGE);
                                frame.dispose();
                            });
                        } else {
                            leaderboardText.append(line).append("\n");
                        }
                    } else if (line.startsWith("Quiz over")) {
                        statusLabel.setText(line);
                    } else if (!line.trim().isEmpty()) {
                        questionLabel.setText("<html><div style='text-align:center'>" + line + "</div></html>");
                        for (int i = 0; i < 4; i++) {
                            optionButtons[i].setText("Option " + (i + 1));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startCountdown() {
        if (countdownTimer != null && countdownTimer.isRunning()) {
            countdownTimer.stop();
        }

        timeLeft = 10;
        timerLabel.setText("⏳ Time left: " + timeLeft + "s");

        countdownTimer = new Timer(1000, e -> {
            timeLeft--;
            if (timeLeft >= 0) {
                timerLabel.setText("⏳ Time left: " + timeLeft + "s");
            }
            if (timeLeft <= 0) {
                countdownTimer.stop();
                disableButtons();
            }
        });
        countdownTimer.start();
    }

    private void enableButtons() {
        for (JButton b : optionButtons) b.setEnabled(true);
    }

    private void disableButtons() {
        for (JButton b : optionButtons) b.setEnabled(false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QuizGUI("localhost", 12345));
    }
}