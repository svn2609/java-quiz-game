package utilities;

import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:quiz.db";

    public static List<Question> fetchQuestionsFromDB() {
        List<Question> questions = new ArrayList<>();
        String query = "SELECT question_text, option1, option2, option3, option4, correct_option FROM questions";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String qText = rs.getString("question_text");
                String[] options = {
                    rs.getString("option1"),
                    rs.getString("option2"),
                    rs.getString("option3"),
                    rs.getString("option4")
                };
                int correct = rs.getInt("correct_option");

                questions.add(new Question(qText, options, correct));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return questions;
    }
}
