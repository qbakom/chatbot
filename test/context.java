package context;

import java.util.ArrayList;
import java.util.List;

public class context {
    private final List<Message> history = new ArrayList<>();

    public void addMessage(Message message) {
        history.add(message);
    }

    public String getContextAsString() {
        StringBuilder sb = new StringBuilder();
        for (Message msg : history) {
            sb.append(msg.getAuthor()).append(": ").append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }
}
