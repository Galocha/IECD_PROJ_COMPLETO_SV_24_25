package client;

import java.io.*;
import java.net.*;

import protocol.CommandProtocol;

/**
 * Classe principal do cliente GoBang.
 * Responsável por:
 * - Estabelecer ligação TCP ao servidor GoBang (GoBangServer).
 * - Ler comandos do utilizador (stdin) e enviá-los ao servidor.
 * - Receber e mostrar respostas do servidor (numa thread separada).
 * 
 * Ligações e utilizações:
 * - Usa CommandProtocol para validar e formatar comandos/mensagens.
 * - Comunica com o servidor, que por sua vez usa ClientHandler para cada cliente.
 * - O servidor espera comandos como /login, /register, /move, etc.
 * - O método main é o ponto de entrada da aplicação cliente.
 */
public class GoBangClient {
	// IP e porta do servidor (pode ser alterado conforme necessário)
    private static final String SERVER_IP = "26.106.140.96"; //substituir pelo IP do servidor
    private static final int PORT = 1234;

    /**
     * Ponto de entrada do cliente.
     * Estabelece ligação ao servidor, cria fluxos de comunicação e gere o ciclo principal.
     * 
     * Utiliza:
     * - Socket: ligação TCP ao servidor.
     * - PrintWriter: para enviar comandos ao servidor.
     * - BufferedReader: para ler respostas do servidor e comandos do utilizador.
     * - CommandProtocol: para validar e formatar comandos/mensagens.
     */
    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, PORT)) {
            System.out.println(CommandProtocol.formatMessage("Conectado ao servidor em " + SERVER_IP + ":" + PORT));

            // Fluxos de comunicação:
            // out: envia comandos para o servidor
            // in: recebe respostas do servidor
            // stdIn: lê comandos do utilizador (stdin)
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

            // Thread para receber mensagens do servidor e mostrar ao utilizador
            // Utiliza CommandProtocol.formatMessage para formatação
            // Se receber "A desconectar do servidor...", termina o cliente
            new Thread(() -> {
                try {
                    String serverResponse;
                    while ((serverResponse = in.readLine()) != null) {
                    	if (serverResponse.equals("A desconectar do servidor...")) {
                            System.out.println(serverResponse);
                            System.exit(0);
                        }
                        System.out.println(CommandProtocol.formatMessage(serverResponse));
                        System.out.print("> ");
                    }
                } catch (IOException e) {
                    System.out.println(CommandProtocol.formatMessage("Conexão com o servidor perdida."));
                    System.exit(0);
                }
            }).start();

            // Mensagem de boas-vindas e instruções iniciais
            System.out.println("Bem-vindo/a ao GoBang (5 em linha)." + "\n" +
            					"Para fazeres login, faz /login nickname password" + "\n" +
            					"Caso não tenhas conta, faz /register nickname password nationality age" + "\n" +
            					"Se quiseres mais comandos, faz /comandos"
            );
            String userInput;
            // Ciclo principal: lê comandos do utilizador, valida e envia ao servidor
            // Usa CommandProtocol.isValidCommand para validar comandos
            while ((userInput = stdIn.readLine()) != null) {
                if (!CommandProtocol.isValidCommand(userInput)) {
                    System.out.println(CommandProtocol.formatMessage("Comando inválido!"));
                    continue;
                }
                out.println(userInput);
            }
        } catch (IOException e) {
            System.err.println(CommandProtocol.formatMessage("Erro ao conectar ao servidor: " + e.getMessage()));
        }
    }
}

/*
UTILIZAÇÕES E LIGAÇÕES:
- GoBangClient é executado pelo utilizador final (linha de comandos).
- Usa CommandProtocol (src/protocol/CommandProtocol.java) para validação e formatação.
- Comunica com GoBangServer (src/server/GoBangServer.java) via socket TCP.
- O servidor cria um ClientHandler (src/client/ClientHandler.java) para cada cliente.
- Os comandos enviados pelo cliente são processados por ClientHandler, que interage com Player, Game, etc.
- As respostas do servidor podem ser mensagens de erro, estado do jogo, notificações de vitória/empate, etc.
*/
