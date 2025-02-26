import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class chatbot {
    private static final String API_URL = "http://127.0.0.1:1234/v1/chat/completions";
    private static final String MODEL_NAME = "deepseek-r1-distill-qwen-7b";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Local chatbot conversation (LM Studio) ===");
        System.out.println("Type 'exit' to quit.\n");

        while (true) {
            System.out.print("You: ");
            String userInput = scanner.nextLine();

            if (userInput.equalsIgnoreCase("exit")) {
                System.out.println("Chat ended.");
                break;
            }

            String botResponse = sendRequestToLMStudio(userInput);
            System.out.println("Bot: " + botResponse + "\n");
        }

        scanner.close();
    }

    private static String sendRequestToLMStudio(String prompt) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String requestBody =
                "{\n" +
                "  \"model\": \"" + MODEL_NAME + "\",\n" +
                "  \"messages\": [\n" +
                "    {\"role\": \"system\", \"content\": \"You are a helpful AI assistant.\"},\n" +
                "    {\"role\": \"user\", \"content\": \"" + escapeJson(prompt) + "\"}\n" +
                "  ],\n" +
                "  \"temperature\": 0.7,\n" +
                "  \"max_tokens\": 128\n" +
                "}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes());
                os.flush();
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                responseBuilder.append(line);
            }
            br.close();

            String response = responseBuilder.toString();
            return extractContentFromJson(response);

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private static String escapeJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }

    private static String extractContentFromJson(String json) {
        String marker = "\"content\": \"";
        int startIndex = json.indexOf(marker);
        if (startIndex == -1) {
            return "No 'content' field in response.";
        }
        startIndex += marker.length();

        int endIndex = json.indexOf("\"", startIndex);
        if (endIndex == -1) {
            return "Invalid JSON response (missing closing quote).";
        }

        String content = json.substring(startIndex, endIndex);
        content = content.replace("\\\"", "\"")
                         .replace("\\\\", "\\");
        return content;
    }
}
