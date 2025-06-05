package client;

import java.io.*;
import java.net.*;

import protocol.CommandProtocol;

public class GoBangClient {
    private static final String SERVER_IP = "192.168.5.101"; //substituir pelo IP do servidor
    private static final int PORT = 1234;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, PORT)) {
            System.out.println(CommandProtocol.formatMessage("Conectado ao servidor em " + SERVER_IP + ":" + PORT));

            // Fluxos de comunicação
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

            //thread para receber mensagens do servidor
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

            System.out.println("Bem-vindo/a ao GoBang (5 em linha)." + "\n" +
            					"Para fazeres login, faz /login nickname password" + "\n" +
            					"Caso não tenhas conta, faz /register nickname password nationality age" + "\n" +
            					"Se quiseres mais comandos, faz /comandos"
            );
            String userInput;
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
