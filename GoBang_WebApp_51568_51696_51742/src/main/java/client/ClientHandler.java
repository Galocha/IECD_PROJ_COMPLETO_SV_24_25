package client;

import game.Game;
import game.Player;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import protocol.CommandProtocol;
import server.GoBangServer;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Player player;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }
    
    public Player getPlayer() {
        return player;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Mensagem recebida: " + inputLine);

                if (!CommandProtocol.isValidCommand(inputLine)) {
                    sendMessage(CommandProtocol.formatMessage("Comando inválido!"));
                    continue;
                }

                Map<String, String> parsedCommand = CommandProtocol.parseCommand(inputLine);
                String command = parsedCommand.get("command");

                switch (command) {
                    case "/register":
                        handleRegister(parsedCommand);
                        break;
                    case "/login":
                        handleLogin(parsedCommand);
                        break;
                    case "/move":
                        handleMove(parsedCommand);
                        break;
                    case "/disconnect":
                        disconnectClient();
                        return;
                    case "/shutdown":
                        shutdown();
                        break;
                    case "/get":
                        handleGet(parsedCommand);
                        break;
                    case "/comandos":
                        sendMessage(CommandProtocol.getAvailableCommands());
                        break;
                    case "/play":
                        handlePlay();
                        break;
                    case "/waitingList":
                    	sendMessage(CommandProtocol.formatMessage(GoBangServer.getSession().getWaitingList()));
                        break;
                    default:
                        sendMessage(CommandProtocol.formatMessage("Comando desconhecido."));
                }
            }
        } catch (IOException e) {
            System.err.println(CommandProtocol.formatMessage("Erro na comunicação com o cliente: " + e.getMessage()));
        }
    }

    private void handleRegister(Map<String, String> params) {
        if (!params.containsKey("param1") || !params.containsKey("param2") || 
            !params.containsKey("param3") || !params.containsKey("param4")) {
            sendMessage(CommandProtocol.formatMessage("Formato inválido! Usa: /register nickname password nationality age"));
            return;
        }
        
        String nickname = params.get("param1");
        if (GoBangServer.getPlayers().containsKey(nickname)) {
            sendMessage(CommandProtocol.formatMessage("Nickname já em uso!"));
        } else {
            Player player = new Player(
                nickname, 
                params.get("param2"), 
                params.get("param3"), 
                Integer.parseInt(params.get("param4")), 
                ""
            );
            GoBangServer.getPlayers().put(nickname, player);
            sendMessage(CommandProtocol.formatMessage("Registo bem-sucedido!"));
            GoBangServer.savePlayersToXML();
        }
    }

    private void handleLogin(Map<String, String> params) {
        if (!params.containsKey("param1") || !params.containsKey("param2")) {
            sendMessage(CommandProtocol.formatMessage("Formato inválido! Usa: /login nickname password"));
            return;
        }
        
        String nickname = params.get("param1");
        Player player = GoBangServer.getPlayers().get(nickname);
        if (player != null && player.getPassword().equals(params.get("param2"))) {
            this.player = player;
            sendMessage(CommandProtocol.formatMessage("Login bem-sucedido!"));
            GoBangServer.getSession().addPlayerToQueue(player);
        } else {
            sendMessage(CommandProtocol.formatMessage("Falha no login!"));
        }
    }

    private void handleMove(Map<String, String> params) {
        if (player == null) {
            sendMessage(CommandProtocol.formatMessage("Faça login primeiro!"));
            return;
        }

        if (!params.containsKey("param1") || !params.containsKey("param2")) {
            sendMessage(CommandProtocol.formatMessage("Formato inválido! Use: /move linha coluna"));
            return;
        }

        try {
            int row = Integer.parseInt(params.get("param1"));
            int col = Integer.parseInt(params.get("param2"));
            Game game = GoBangServer.getSession().getGameByPlayer(player);
            
            if (game != null) {
                String result = game.processMove(player, row, col);
                String boardState = game.getBoardAsString();
                
                if (!player.equals(game.getCurrentPlayer())) {
                    GoBangServer.notifyPlayer(game.getCurrentPlayer(), CommandProtocol.formatMessage(
                        "Não é a tua vez, aguarda que " + game.getCurrentPlayer().getNickname() + " acabe de jogar."
                    ));
                }
                
                // Mensagens personalizadas para cada jogador
                String symbolCurrent = (game.getCurrentPlayer() == game.getPlayer1()) ? "O" : "X"; // Símbolo do próximo jogador
                String messagePlayer1 = CommandProtocol.formatMessage(
                    "RESULTADO: " + result + 
                    "\nSeu símbolo: X" + 
                    "\nPróximo jogador: " + symbolCurrent + 
                    "\n\nTABULEIRO:\n" + boardState
                );
                
                String messagePlayer2 = CommandProtocol.formatMessage(
                    "RESULTADO: " + result + 
                    "\nSeu símbolo: O" + 
                    "\nPróximo jogador: " + symbolCurrent + 
                    "\n\nTABULEIRO:\n" + boardState
                );
                
                // Envia as mensagens específicas
                GoBangServer.notifyPlayers(
                    game.getPlayer1(), 
                    game.getPlayer2(), 
                    messagePlayer1, 
                    messagePlayer2
                );
                
                if (result.startsWith("VITÓRIA") || result.equals("EMPATE")) {
                    GoBangServer.getSession().endGame(game);
                    
                    // Mensagem adicional para o fim do jogo
                    String endMessage = CommandProtocol.formatMessage(
                        "FIM DE JOGO! " + 
                        (result.startsWith("VITÓRIA") ? result : "O jogo terminou num empate.")
                    );
                    
                    GoBangServer.notifyPlayer(game.getPlayer1(), endMessage);
                    GoBangServer.notifyPlayer(game.getPlayer2(), endMessage);
                }
            } else {
                sendMessage(CommandProtocol.formatMessage("Você não está num jogo ativo!"));
            }
        } catch (NumberFormatException e) {
            sendMessage(CommandProtocol.formatMessage("Coordenadas inválidas! Use números."));
        }
    }
    
    private void handleGet(Map<String, String> params) {
        if (!params.containsKey("param1")) {
            sendMessage(CommandProtocol.formatMessage("Formato inválido! Use: /get nickname"));
            return;
        }

        String nickname = params.get("param1");
        Player player = GoBangServer.getPlayers().get(nickname);
        if (player != null) {
            sendMessage(CommandProtocol.formatMessage(player.toString()));
        } else {
            sendMessage(CommandProtocol.formatMessage("Jogador '" + nickname + "' não foi encontrado."));
        }
    }
    
    private void handlePlay() {
        if (player == null) {
            sendMessage(CommandProtocol.formatMessage("Faz login primeiro! Usa /login nickname password"));
            return;
        }
        
        // Verifica se o jogador já está num jogo ativo
        Game activeGame = GoBangServer.getSession().getGameByPlayer(player);
        if (activeGame != null) {
            sendMessage(CommandProtocol.formatMessage("Já estás a jogar!"));
            return;
        }
        
        // Verifica se o jogador já está na fila de espera
        if (GoBangServer.getSession().getWaitingPlayers().contains(player)) {
            sendMessage(CommandProtocol.formatMessage("Já estás na lista de espera. Aguarda..."));
            return;
        }
        
        // Adiciona o jogador à fila de espera
        GoBangServer.getSession().addPlayerToQueue(player);
        sendMessage(CommandProtocol.formatMessage("Foste adicionado/a à lista de espera. Aguarda..."));
    }
    
    private void disconnectClient() {
        try {
            if (player != null) {
                // Verifica se o jogador está num jogo ativo
                Game activeGame = GoBangServer.getSession().getGameByPlayer(player);
                if (activeGame != null) {
                    // Declara o outro jogador como vencedor
                    Player opponent = activeGame.getOtherPlayer(player);
                    opponent.setWins(opponent.getWins() + 1); // Incrementa vitórias
                    player.setLosses(player.getLosses() + 1); // Incrementa derrotas
                    
                    GoBangServer.notifyPlayer(opponent, CommandProtocol.formatMessage("O jogador " + player.getNickname() + " desconectou. Ganhaste!"));
                    GoBangServer.notifyPlayer(player, CommandProtocol.formatMessage("Desconectaste, então o jogador " + opponent.getNickname() + " ganhou!"));
                    
                    GoBangServer.getSession().endGame(activeGame); // Encerra o jogo
                }
            }
            if (!socket.isClosed()) {
                out.println(CommandProtocol.formatMessage("A desconectar do servidor...")); // Mensagem final para o cliente
                out.close();
                in.close();
                socket.close(); // Fecha o socket completamente
            }
        } catch (IOException e) {
            System.err.println(CommandProtocol.formatMessage("Erro ao desconectar: " + e.getMessage()));
        }
    }
    
    private void shutdown() {
    	if (player == null) {
            sendMessage(CommandProtocol.formatMessage("Precisas de fazer login para poderes desligar o servidor!"));
            return;
        }
        for (ClientHandler client : GoBangServer.getClients()) {
            try {
                client.sendMessage(CommandProtocol.formatMessage("O servidor está a desligar..."));
            } catch (Exception e) {
                System.err.println(CommandProtocol.formatMessage("Erro ao notificar cliente: " + e.getMessage()));
            }
        }
        GoBangServer.shutdownServer();
        disconnectClient();
    }
    
    public Socket getSocket() {
    	return socket;
    }
}