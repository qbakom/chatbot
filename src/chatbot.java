import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.FileWriter;
import java.io.IOException;

public class chatbot {
    private static final String API_URL = "http://127.0.0.1:1234/v1/chat/completions";
    private static final String MODEL_NAME = "deepseek-r1-distill-qwen-7b";

    private static final int MAX_TOKENS = 1024; 
    private static final double TEMPERATURE = 0.7;
    private static final double TOP_P = 0.9;
    private static final double FREQUENCY_PENALTY = 0.0;
    private static final double PRESENCE_PENALTY = 0.0;
    private static final String SYSTEM_PROMPT = "You are a helpful AI assistant. Provide detailed and informative responses.";
    
    private static final int MAX_REQUESTS_PER_USER = 3;
    private static final Map<String, Integer> userRequestCounts = new HashMap<>();
    
    private static final List<Map<String, String>> conversationHistory = new ArrayList<>();
    private static final int MAX_HISTORY_LENGTH = 10;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Local chatbot conversation (LM Studio) ===");
        System.out.println("Please enter your username:");
        String username = scanner.nextLine().trim();
        
        userRequestCounts.putIfAbsent(username, 0);
        
        System.out.println("Welcome, " + username + "!");
        System.out.println("Type 'exit' to quit.\n");
        System.out.println("You have " + (MAX_REQUESTS_PER_USER - userRequestCounts.get(username)) + 
                           " requests remaining.");

        while (true) {
            System.out.print("You: ");
            String userInput = scanner.nextLine();

            if (userInput.equalsIgnoreCase("exit")) {
                System.out.println("Chat ended.");
                break;
            }
            
            if (userRequestCounts.get(username) >= MAX_REQUESTS_PER_USER) {
                System.out.println("Bot: Sorry, you have reached your maximum request limit.");
                System.out.println("Please try again later or upgrade your subscription.");
                continue;
            }
            
            userRequestCounts.put(username, userRequestCounts.get(username) + 1);
            int remainingRequests = MAX_REQUESTS_PER_USER - userRequestCounts.get(username);
            
            String botResponse = sendRequest(userInput);
            System.out.println("Bot: " + botResponse + "\n");
            
            saveChatToFile(username, userInput, botResponse);
            
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userInput);
            conversationHistory.add(userMessage);
            
            Map<String, String> assistantMessage = new HashMap<>();
            assistantMessage.put("role", "assistant");
            assistantMessage.put("content", botResponse);
            conversationHistory.add(assistantMessage);
            
            // Trim history if too long
            while (conversationHistory.size() > MAX_HISTORY_LENGTH * 2) {
                conversationHistory.remove(0);
            }
            
            System.out.println("Requests remaining: " + remainingRequests);
        }

        scanner.close();
    }

    private static String sendRequest(String prompt) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            StringBuilder messagesJson = new StringBuilder();
            messagesJson.append("[{\"role\": \"system\", \"content\": \"" + SYSTEM_PROMPT + "\"}");
            
            for (Map<String, String> message : conversationHistory) {
                messagesJson.append(", {\"role\": \"" + message.get("role") + 
                            "\", \"content\": \"" + escapeJson(message.get("content")) + "\"}");
            }
            
            messagesJson.append(", {\"role\": \"user\", \"content\": \"" + escapeJson(prompt) + "\"}]");

            String requestBody =
                "{\n" +
                "  \"model\": \"" + MODEL_NAME + "\",\n" +
                "  \"messages\": " + messagesJson.toString() + ",\n" +
                "  \"temperature\": " + TEMPERATURE + ",\n" +
                "  \"max_tokens\": " + MAX_TOKENS + ",\n" +
                "  \"top_p\": " + TOP_P + ",\n" +
                "  \"frequency_penalty\": " + FREQUENCY_PENALTY + ",\n" +
                "  \"presence_penalty\": " + PRESENCE_PENALTY + "\n" +
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
            return extractJson(response);

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private static String escapeJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }

    private static String extractJson(String json) {
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

    private static void saveChatToFile(String username, String userMessage, String botResponse) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            FileWriter writer = new FileWriter(username + "_chat_history.txt", true); // true for append mode
            writer.write(timestamp + "\nUser: " + userMessage + "\nBot: " + botResponse + "\n\n");
            writer.close();
        } catch (IOException e) {
            System.err.println("Error saving chat: " + e.getMessage());
        }
    }
}
