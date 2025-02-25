package console;

import context.ConversationContext;
import context.Message;
import extensions.ExtensionsManager;
import network.LMStudioClient;
import java.util.Scanner;

public class MainConsole {
    public static void main(String[] args) {
        LMStudioClient client = new LMStudioClient("http://localhost:port/api");
        ConversationContext conversation = new ConversationContext();
        ExtensionsManager extensions = new ExtensionsManager();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String userInput = scanner.nextLine();

            if (userInput.equalsIgnoreCase("/exit")) {
                break;
            }

            if (extensions.isCommand(userInput)) {
                String extensionResult = extensions.processCommand(userInput);
                System.out.println(extensionResult);
                conversation.addMessage(new Message("User", userInput));
                conversation.addMessage(new Message("Extension", extensionResult));
                continue;
            }

            try {
                conversation.addMessage(new Message("User", userInput));
                String payload = buildPayload(conversation.getContextAsString(), userInput);
                String apiResponse = client.sendRequest(payload);
                String chatbotAnswer = parseApiResponse(apiResponse);
                System.out.println(chatbotAnswer);
                conversation.addMessage(new Message("Bot", chatbotAnswer));
            } catch (Exception e) {
                System.out.println("Błąd podczas komunikacji z LM Studio.");
            }
        }
    }

    private static String buildPayload(String context, String userInput) {
        return "{\"prompt\":\"" + context + "\",\"userInput\":\"" + userInput + "\"}";
    }

    private static String parseApiResponse(String response) {
        // np. org.json
        return response;
    }
}
