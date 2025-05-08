package server;

import java.sql.*;
import java.nio.file.*;
import java.io.IOException;

public class DatabaseInitializer {
    private static final String DB_URL = "jdbc:sqlite:quiz.db";

    public static void initializeDatabase() {
        try {
            // Explicitly load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                if (conn != null) {
                    String sql = Files.readString(Path.of("sql/schema.sql"));
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate(sql);
                        System.out.println("✅ Database initialized successfully.");
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ SQLite JDBC driver not found.");
            e.printStackTrace();
        } catch (SQLException | IOException e) {
            System.err.println("❌ Failed to initialize database.");
            e.printStackTrace();
        }
    }
}
