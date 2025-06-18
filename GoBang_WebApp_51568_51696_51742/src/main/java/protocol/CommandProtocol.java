package protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe utilitária para gerir comandos do protocolo entre cliente e servidor.
 * Responsável por:
 * - Validar comandos recebidos.
 * - Fornecer instruções de uso para cada comando.
 * - Fazer parsing dos comandos em parâmetros.
 * - Formatar mensagens para envio.
 * 
 * Utilização:
 * - Usada em GoBangClient (cliente) para validar comandos antes de enviar.
 * - Usada em ClientHandler (servidor) para validar, interpretar e responder a comandos.
 * - Usada em todo o sistema para garantir consistência na comunicação.
 */
public class CommandProtocol {
	// Mapa de comandos disponíveis e respetivas instruções de uso
    private static final Map<String, String> commands = new HashMap<>();

    // Bloco estático: inicializa o mapa de comandos e instruções
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
        commands.put("/surrender", "Para desistires faz: /shutdown"); //NOVO -> CRIADO DEVIDO À IMPLEMENTAÇÃO DE UM BOTÃO DE DESISTÊNCIA NA WEB
        commands.put("/startgame", "Para convidares alguém para jogar faz: /startgame tu jogador 2"); //NOVO -> CRIADO DEVIDO À IMPLEMENTAÇÃO DE UM SISTEMA DE CONVITES
        commands.put("/timeout", "NÃO UTILIZAR -> SERVE PARA ACABAR COM O TEMPORIZADOR"); //NOVO -> CRIADO DEVIDO À IMPLEMENTAÇÃO DE UM TIMER POR JOGADA
        commands.put("/getgames", "Para veres os teus jogos ativos faz: /getgames"); //NOVO -> CRIADO DEVIDO À IMPLEMENTAÇÃO DE VÁRIOS JOGOS EM SIMULTÂNEO
    }

    /**
     * Valida se o comando recebido é um comando reconhecido.
     * 
     * Usado em:
     * - GoBangClient: antes de enviar comandos ao servidor.
     * - ClientHandler: antes de processar comandos recebidos.
     */
    public static boolean isValidCommand(String command) {
        String baseCommand = command.split(" ")[0];
        return commands.containsKey(baseCommand);
    }

    /**
     * Devolve a lista de comandos disponíveis e respetivas instruções.
     * 
     * Usado em:
     * - ClientHandler: para responder ao comando /comandos.
     * - GoBangClient: pode ser usado para mostrar ajuda.
     */
    public static String getAvailableCommands() {
        StringBuilder sb = new StringBuilder("Comandos disponíveis:\n");
        for (Map.Entry<String, String> entry : commands.entrySet()) {
            sb.append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Faz parsing de um comando recebido, separando-o em base e parâmetros.
     * Exemplo: "/move 3 4" -> command=/move, param1=3, param2=4
     * 
     * Usado em:
     * - ClientHandler: para extrair parâmetros dos comandos recebidos.
     */
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

    /**
     * Formata uma mensagem para envio ao cliente ou servidor.
     * Atualmente, apenas devolve a mensagem tal como está.
     * 
     * Usado em:
     * - GoBangClient: para mostrar mensagens formatadas ao utilizador.
     * - ClientHandler: para enviar mensagens formatadas ao cliente.
     */
    public static String formatMessage(String message) {
        return message;
    }
}

/*
UTILIZAÇÕES E LIGAÇÕES:
- GoBangClient: valida comandos antes de enviar (isValidCommand), mostra ajuda (getAvailableCommands), formata mensagens (formatMessage).
- ClientHandler: valida comandos recebidos (isValidCommand), faz parsing (parseCommand), responde a /comandos (getAvailableCommands), envia mensagens (formatMessage).
- Todos os comandos aceites pelo sistema estão definidos aqui, garantindo centralização e consistência.
*/