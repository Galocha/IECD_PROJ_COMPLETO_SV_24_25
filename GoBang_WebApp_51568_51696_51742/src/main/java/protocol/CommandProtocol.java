package protocol;

import java.util.HashMap;
import java.util.Map;

public class CommandProtocol {
    private static final Map<String, String> commands = new HashMap<>();

    static {
        commands.put("/register", "Para te registares faz: /register nickname password nationality age");
        commands.put("/login", "Para fazeres login faz: /login nickname password");
        commands.put("/move", "Para fazeres uma jogada faz: /move linha coluna");
        commands.put("/get", "Para receberes dados de um determinado jogador faz: /get nickname");
        commands.put("/play", "Para entrares na fila de espera para jogar novamente, faz: /play");
        commands.put("/waitingList", "Para veres a lista de espera faz: /waitingList");
        commands.put("/disconnect", "Para te desconectares do servidor faz: /disconnect");
        commands.put("/comandos", "Para veres os comandos disponíveis faz: /comandos");
        commands.put("/shutdown", "Para desligares o servidor faz: /shutdown");
    }

    public static boolean isValidCommand(String command) {
        String baseCommand = command.split(" ")[0];
        return commands.containsKey(baseCommand);
    }

    public static String getAvailableCommands() {
        StringBuilder sb = new StringBuilder("Comandos disponíveis:\n");
        for (Map.Entry<String, String> entry : commands.entrySet()) {
            sb.append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    public static Map<String, String> parseCommand(String command) {
        Map<String, String> parameters = new HashMap<>();
        String[] parts = command.split(" ");
        String baseCommand = parts[0];

        if (!isValidCommand(baseCommand)) {
            parameters.put("error", "Comando inválido.");
            return parameters;
        }

        parameters.put("command", baseCommand);
        for (int i = 1; i < parts.length; i++) {
            parameters.put("param" + i, parts[i]);
        }
        return parameters;
    }

    public static String formatMessage(String message) {
        return message;
    }
}

