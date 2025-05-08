package utilities;
import java.util.*;

public class Question {
    private String questionText;
    private String[] options;
    private int correctIndex;

    public Question(String questionText, String[] options, int correctIndex) {
        this.questionText = questionText;
        this.options = options;
        this.correctIndex = correctIndex;
    }

    public String getFormattedQuestion() {
        StringBuilder sb = new StringBuilder(questionText + "\n");
        for (int i = 0; i < options.length; i++) {
            sb.append((i + 1)).append(". ").append(options[i]).append("\n");
        }
        return sb.toString();
    }

    public boolean isCorrect(int answerIndex) {
        return answerIndex == correctIndex;
    }
    
    public String getQuestionText() {
        return questionText;
    }

    public List<String> getFormattedOptions() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < options.length; i++) {
            list.add((i + 1) + ". " + options[i]);
        }
        return list;
    }

}

