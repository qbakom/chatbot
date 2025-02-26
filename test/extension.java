package extensions;

import java.util.HashMap;
import java.util.Map;

public class extension {
    private Map<String, ExtensionCommand> commands = new HashMap<>();

    public extension() {
        registerCommands();
    }

    private void registerCommands() {
        commands.put("/define", new DefineCommand());
        commands.put("/translate", new TranslateCommand());
    }

    public boolean isCommand(String input) {
        return input.startsWith("/");
    }

    public String processCommand(String input) {
        String[] parts = input.split(" ", 2);
        ExtensionCommand cmd = commands.get(parts[0]);
        if (cmd != null) {
            return cmd.execute(parts.length > 1 ? parts[1] : "");
        } else {
            return "Nieznane polecenie.";
        }
    }
}
