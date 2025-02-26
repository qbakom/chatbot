import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class local {
    // Adres lokalnego serwera LM Studio (może być 127.0.0.1 lub 172.30.160.1 - zależy od Twojej konfiguracji).
    private static final String API_URL = "http://127.0.0.1:1234/v1/chat/completions";
    
    // Nazwa modelu, którego używasz w LM Studio (sprawdź w interfejsie LM Studio lub w logach).
    private static final String MODEL_NAME = "deepseek-r1-distill-qwen-7b";

    public static void main(String[] args) {
        // Prosta pętla czytająca input z konsoli
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Lokalna rozmowa z chatbotem (LM Studio) ===");
        System.out.println("Wpisz 'exit' aby zakonczyc.\n");

        while (true) {
            System.out.print("Ty: ");
            String userInput = scanner.nextLine();

            // Wyjście z programu
            if (userInput.equalsIgnoreCase("exit")) {
                System.out.println("Zakończono rozmowę.");
                break;
            }

            // Wysyłamy prompt do LM Studio i wyświetlamy odpowiedź
            String botResponse = sendRequestToLMStudio(userInput);
            System.out.println("Bot: " + botResponse + "\n");
        }
        
        scanner.close();
    }

    private static String sendRequestToLMStudio(String prompt) {
        try {
            // Konfiguracja połączenia
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);  // Zezwalamy na wysyłanie danych w body żądania

            // Budujemy ciało żądania w formacie JSON
            // Używamy tutaj notacji stringowej, żeby uniknąć dodatkowych bibliotek do JSON-a
            // (Możesz użyć org.json, Gson, Jackson itp., jeśli wolisz).
            String requestBody = 
                "{\n" +
                "  \"model\": \"" + MODEL_NAME + "\",\n" +
                "  \"messages\": [\n" +
                "    {\"role\": \"system\", \"content\": \"You are a helpful AI assistant.\"},\n" +
                "    {\"role\": \"user\", \"content\": \"" + escapeJson(prompt) + "\"}\n" +
                "  ],\n" +
                "  \"temperature\": 0.7,\n" +
                "  \"max_tokens\": 100\n" +
                "}";

            // Wysyłamy zapytanie
            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes());
                os.flush();
            }

            // Odbieramy odpowiedź
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                responseBuilder.append(line);
            }
            br.close();

            // Prosta ekstrakcja odpowiedzi z JSON-a (ponownie: można użyć dedykowanej biblioteki)
            // Szukamy fragmentu "content" w "choices[0].message"
            String response = responseBuilder.toString();
            return extractContentFromJson(response);

        } catch (Exception e) {
            // W razie błędu zwracamy komunikat
            return "Wystąpił błąd: " + e.getMessage();
        }
    }

    /**
     * Funkcja pomocnicza do zabezpieczenia cudzysłowów i backslashy w promptach,
     * aby nie psuły nam struktury JSON-a.
     */
    private static String escapeJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }

    /**
     * Bardzo uproszczona metoda wyciągania contentu z JSON-a w polu
     * choices[0].message.content
     * 
     * Zalecane jest użycie biblioteki JSON w większych projektach.
     */
    private static String extractContentFromJson(String json) {
        // Szukamy klucza "content"
        // Przykładowa odpowiedź:
        // {
        //   "id": "...",
        //   "object": "chat.completion",
        //   "created": 123456789,
        //   "choices": [
        //     {
        //       "index": 0,
        //       "message": {
        //         "role": "assistant",
        //         "content": "Hello, how can I help you?"
        //       },
        //       "finish_reason": "stop"
        //     }
        //   ]
        // }
        
        // Szukamy pierwszego wystąpienia: "content": "
        // a potem czytamy do najbliższego cudzysłowu
        String marker = "\"content\": \"";
        int startIndex = json.indexOf(marker);
        if (startIndex == -1) {
            // Nie znaleziono contentu
            return "Brak pola 'content' w odpowiedzi.";
        }
        startIndex += marker.length();
        
        // Szukamy zamykającego cudzysłowu
        int endIndex = json.indexOf("\"", startIndex);
        if (endIndex == -1) {
            return "Niepoprawna odpowiedź JSON (brak zamykającego cudzysłowu).";
        }
        
        // Wyciągamy zawartość
        String content = json.substring(startIndex, endIndex);
        
        // Przywracamy ewentualne encje JSON (np. \" -> ")
        content = content.replace("\\\"", "\"")
                         .replace("\\\\", "\\");
        return content;
    }
}
