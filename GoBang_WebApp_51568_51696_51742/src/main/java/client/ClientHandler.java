package client;

import game.Game;
import game.Player;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import protocol.CommandProtocol;
import server.GoBangServer;

/**
 * Classe responsável por gerir a ligação de um cliente ao servidor.
 * Cada cliente ligado ao servidor tem uma instância de ClientHandler a correr numa thread.
 * 
 * Responsabilidades:
 * - Receber e interpretar comandos do cliente (via socket TCP).
 * - Validar comandos e encaminhar para os métodos apropriados.
 * - Gerir autenticação, registo, jogadas, desconexão, etc.
 * - Comunicar com as classes Game, Player, GoBangServer e CommandProtocol.
 * 
 * Relações:
 * - GoBangServer mantém uma lista de ClientHandler ativos.
 * - Cada ClientHandler referencia um Player autenticado (após login).
 * - Usa Game para gerir o estado dos jogos.
 * - Usa CommandProtocol para validar e formatar comandos/mensagens.
 */
public class ClientHandler implements Runnable {
	private Socket socket; // Socket TCP do cliente
    private PrintWriter out; // Para enviar mensagens ao cliente
    private BufferedReader in; // Para ler mensagens do cliente
    private Player player; // Jogador autenticado (null até login)

    /**
     * Construtor: recebe o socket do cliente.
     * @param socket Socket TCP já aceite pelo servidor.
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }
    
    /**
     * Retorna o jogador autenticado neste handler.
     * Usado por GoBangServer e outros handlers.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Envia uma mensagem ao cliente (linha terminada por \n).
     * Usado em todos os métodos de resposta.
     */
    public void sendMessage(String message) {
        out.println(message);
    }

    /**
     * Método principal da thread do handler.
     * Inicializa streams, lê comandos do cliente e processa-os.
     * Cada comando é validado e encaminhado para o método respetivo.
     * 
     * Ligações:
     * - Usa CommandProtocol para validar e parsear comandos.
     * - Usa GoBangServer para aceder a jogadores, jogos, sessões, etc.
     * - Chama métodos como handleRegister, handleLogin, handleMove, etc.
     */
    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Mensagem recebida: " + inputLine);

                // Valida o comando recebido
                if (!CommandProtocol.isValidCommand(inputLine)) {
                    sendMessage(CommandProtocol.formatMessage("Comando inválido!"));
                    continue;
                }

                // Faz parsing do comando para um mapa de parâmetros
                Map<String, String> parsedCommand = CommandProtocol.parseCommand(inputLine);
                String command = parsedCommand.get("command");

                // Encaminha para o handler apropriado
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
                    case "/surrender":
                        handleSurrender();
                        break;
                    case "/startgame":
                        handleStartGame(parsedCommand);
                        break;
                    case "/timeout":
                        handleTimeout();
                        break;
                    case "/getgames":
                        handleGetGames(parsedCommand);
                        break;
                    default:
                        sendMessage(CommandProtocol.formatMessage("Comando desconhecido."));
                }
            }
        } catch (IOException e) {
            System.err.println(CommandProtocol.formatMessage("Erro na comunicação com o cliente: " + e.getMessage()));
        } finally {
            // Remove sempre este handler da lista global
            GoBangServer.getClients().remove(this);
            System.out.println("DEBUG: Handler removido. Handlers ativos: " + GoBangServer.getClients().size());
            // Fecha recursos
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
            try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Regista um novo jogador.
     * Valida se o nickname já existe, cria Player e adiciona ao mapa global.
     * Guarda os jogadores em XML.
     * 
     * Usado quando o comando /register é recebido.
     */
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

    /**
     * Faz login de um jogador.
     * Valida nickname e password, associa o Player ao handler.
     * 
     * Usado quando o comando /login é recebido.
     */
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
        } else {
            sendMessage(CommandProtocol.formatMessage("Falha no login!"));
        }
    }

    /**
     * Processa uma jogada (/move linha coluna).
     * Valida se o jogador está autenticado, se está num jogo, e executa a jogada.
     * Lê o resultado do método Game.processMove e trata respostas como "error:game_over":
     * - Se for "error:game_over", envia ao cliente e termina.
     * - Se for vitória/empate, termina o jogo e notifica ambos os jogadores.
     * - Caso contrário, envia o resultado e o tabuleiro atualizado.
     * 
     * Ligações:
     * - Usa Game.processMove (ver comentários em Game.java).
     * - Usa GoBangServer.getLastGameResults() para polling de fim de jogo.
     */
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
            	// Verifica se há resultado pendente (fim de jogo por polling)
            	String lastResult = GoBangServer.getLastGameResults().get(player.getNickname());
                if (lastResult != null) {
                    sendMessage(CommandProtocol.formatMessage(lastResult));
                    GoBangServer.getLastGameResults().remove(player.getNickname());
                    return;
                }
            	
                String result = game.processMove(player, row, col); // <- Aqui pode retornar "error:game_over", "refresh", "VITÓRIA...", "EMPATE!"
                String boardState = game.getBoardAsString();

                // Mensagens personalizadas para cada jogador
                String symbolCurrent = (game.getCurrentPlayer() == game.getPlayer1()) ? "O" : "X";
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
                GoBangServer.notifyPlayers(
                    game.getPlayer1(),
                    game.getPlayer2(),
                    messagePlayer1,
                    messagePlayer2
                );

                // Se o resultado indica fim de jogo, termina e notifica ambos
                if (result.startsWith("VITÓRIA") || result.equals("EMPATE")) {
                    // Guarda o resultado para polling (usado em AJAX global)
                    if (result.startsWith("VITÓRIA")) {
                        Player winner = game.getWinner();
                        Player loser = game.getOtherPlayer(winner);

                        GoBangServer.getLastGameResults().put(winner.getNickname(), "game_over:FIM DE JOGO! Ganhaste!");
                        GoBangServer.getLastGameResults().put(loser.getNickname(), "game_over:FIM DE JOGO! Perdeste!");
                    } else {
                        // Empate
                        GoBangServer.getLastGameResults().put(game.getPlayer1().getNickname(), "game_over:FIM DE JOGO! O jogo terminou num empate.");
                        GoBangServer.getLastGameResults().put(game.getPlayer2().getNickname(), "game_over:FIM DE JOGO! O jogo terminou num empate.");
                    }

                    GoBangServer.getSession().endGame(game);

                    String winnerMsg = CommandProtocol.formatMessage("FIM DE JOGO! Ganhaste!");
                    String loserMsg = CommandProtocol.formatMessage("FIM DE JOGO! Perdeste!");
                    String drawMsg = CommandProtocol.formatMessage("FIM DE JOGO! O jogo terminou num empate.");

                    if (result.startsWith("VITÓRIA")) {
                        Player winner = game.getWinner();
                        Player loser = game.getOtherPlayer(winner);

                        // Resposta direta ao jogador que fez a jogada final
                        if (player.equals(winner)) {
                            sendMessage(winnerMsg);
                            GoBangServer.notifyPlayer(loser, loserMsg);
                        } else {
                            sendMessage(loserMsg);
                            GoBangServer.notifyPlayer(winner, winnerMsg);
                        }
                    } else {
                        // Empate
                        sendMessage(drawMsg);
                        GoBangServer.notifyPlayer(game.getOtherPlayer(player), drawMsg);
                    }
                    return; // IMPORTANTE: não enviar mais nada depois deste return
                }

                // Resposta normal para jogada não-final
                sendMessage(CommandProtocol.formatMessage("RESULTADO: " + result));
            } else {
                sendMessage(CommandProtocol.formatMessage("Você não está num jogo ativo!"));
            }
        } catch (NumberFormatException e) {
            sendMessage(CommandProtocol.formatMessage("Coordenadas inválidas! Use números."));
        }
    }
    
    /**
     * Devolve o estado do jogo ou fila de espera para o frontend.
     * Também devolve resultados pendentes de fim de jogo (polling).
     * 
     * Ligações:
     * - Usa GoBangServer.getLastGameResults() para respostas como "error:game_over".
     * - Usa Game.getCurrentMoveStartMillis() e getMaxMoveTimeSeconds() para timeout.
     */
    private void handleGet(Map<String, String> params) {
        if (!params.containsKey("param1")) {
            sendMessage(CommandProtocol.formatMessage("Formato inválido! Use: /get nickname"));
            return;
        }

        String nickname = params.get("param1");
        Player player = GoBangServer.getPlayers().get(nickname);

        // Se o jogador tem um resultado pendente de fim de jogo, devolve-o e remove do mapa
        String lastResult = GoBangServer.getLastGameResults().get(nickname);
        if (lastResult != null) {
            sendMessage(CommandProtocol.formatMessage(lastResult));
            GoBangServer.getLastGameResults().remove(nickname);
            return;
        }

        if (player != null) {
            Game game = GoBangServer.getSession().getGameByPlayer(player);
            if (game != null) {
            	Player current = game.getCurrentPlayer();
                long jogadaInicio = game.getCurrentMoveStartMillis();
                int tempoMaximo = game.getMaxMoveTimeSeconds();
                int tempoRestante = tempoMaximo - (int)((System.currentTimeMillis() - jogadaInicio) / 1000);
                if (tempoRestante <= 0 && !game.isGameOver()) {
                    game.passTurnOnTimeout(current);
                }
                // Devolve info do jogo para o frontend
                StringBuilder sb = new StringBuilder();
                sb.append("JOGO INICIADO;");
                sb.append("PLAYER1:").append(game.getPlayer1().getNickname()).append(";");
                sb.append("PLAYER2:").append(game.getPlayer2().getNickname()).append(";");
                sb.append("OPONENTE:").append(game.getOtherPlayer(player).getNickname()).append(";");
                sb.append("TABULEIRO:");
                char[][] board = game.getBoard();
                for (int i = 0; i < board.length; i++) {
                    for (int j = 0; j < board[i].length; j++) {
                        sb.append(board[i][j]);
                    }
                    if (i < board.length - 1) sb.append("|");
                }
                sb.append(";");
                if (game.getCurrentPlayer().equals(player)) {
                    sb.append("SUA_VEZ;");
                    sb.append("JOGADA_INICIO:").append(game.getCurrentMoveStartMillis()).append(";");
                    sb.append("TEMPO_MAXIMO:").append(game.getMaxMoveTimeSeconds()).append(";");
                } else {
                    sb.append("AGUARDA");
                }
                sendMessage(CommandProtocol.formatMessage(sb.toString()));
            } else if (GoBangServer.getSession().getWaitingPlayers().contains(player)) {
                sendMessage(CommandProtocol.formatMessage("AGUARDANDO"));
            } else {
                sendMessage(CommandProtocol.formatMessage("NÃO_ESTÁS_NA_FILA"));
            }
        } else {
            sendMessage(CommandProtocol.formatMessage("Jogador '" + nickname + "' não foi encontrado."));
        }
    }
    
    /**
     * Adiciona o jogador à fila de espera para jogar.
     * Valida se já está num jogo ou na fila.
     */
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
    
    /**
     * Permite ao jogador desistir do jogo.
     * Marca o adversário como vencedor, termina o jogo e notifica ambos.
     * 
     * Ligações:
     * - Usa GoBangServer.getLastGameResults() para polling.
     * - Usa Game.getOtherPlayer() para identificar o adversário.
     */
    private void handleSurrender() { //NOVO -> PARA LIDAR COM DESISTÊNCIAS
        if (player == null) {
            sendMessage(CommandProtocol.formatMessage("Faça login primeiro!"));
            return;
        }
        System.out.println("[DEBUG] handleSurrender chamado por " + player.getNickname());
        Game game = GoBangServer.getSession().getGameByPlayer(player);
        if (game != null && !game.isEnded()) {
        	Player opponent = game.getOtherPlayer(player);
            String opponentNick = opponent.getNickname();
            ClientHandler opponentHandler = null;
            for (ClientHandler ch : GoBangServer.getClients()) {
                if (ch.getPlayer() != null && ch.getPlayer().getNickname().equals(opponentNick)) {
                    opponentHandler = ch;
                    break;
                }
            }
            
            // Define o adversário como vencedor ANTES de terminar o jogo
            game.setWinner(opponent);

            // Guarda resultado para polling
            GoBangServer.getLastGameResults().put(player.getNickname(), "FIM DE JOGO! Desististe! Perdeste!");
            GoBangServer.getLastGameResults().put(opponentNick, "FIM DE JOGO! O teu adversário desistiu. Ganhaste!");

            // Termina o jogo primeiro
            GoBangServer.getSession().endGame(game);

            // Só depois de terminar o jogo, notifica ambos
            sendMessage(CommandProtocol.formatMessage("FIM DE JOGO! Desististe! Perdeste!"));
            if (opponentHandler != null) {
                System.out.println("[DEBUG] Enviando mensagem de vitória para " + opponentNick);
                opponentHandler.sendMessage(CommandProtocol.formatMessage("FIM DE JOGO! O teu adversário desistiu. Ganhaste!"));
            } else {
                System.out.println("[DEBUG] Não foi encontrado handler para " + opponentNick);
            }
        } else {
            sendMessage(CommandProtocol.formatMessage("Não estás num jogo ativo."));
        }
    }
    
    /**
     * Inicia um novo jogo entre dois jogadores específicos (convite).
     * Garante que não existe já um jogo entre os dois.
     * Notifica ambos os jogadores com o ID do novo jogo.
     */
    private void handleStartGame(Map<String, String> params) { //NOVO -> PARA LIDAR COM CONVITES
    	if (!params.containsKey("param1") || !params.containsKey("param2")) {
            sendMessage(CommandProtocol.formatMessage("Uso: /startgame jogador1 jogador2"));
            return;
        }
        String nick1 = params.get("param1");
        String nick2 = params.get("param2");

        Player p1 = GoBangServer.getPlayers().get(nick1);
        Player p2 = GoBangServer.getPlayers().get(nick2);

        if (p1 == null || p2 == null) {
            sendMessage(CommandProtocol.formatMessage("Jogador não encontrado."));
            return;
        }

        // Permite múltiplos jogos, mas não entre o mesmo par de jogadores
        List<Game> games1 = GoBangServer.getSession().getGamesForPlayer(nick1);
        boolean jaExiste = false;
        for (Game g : games1) {
            if (
                (g.getPlayer1().getNickname().equals(nick1) && g.getPlayer2().getNickname().equals(nick2)) ||
                (g.getPlayer1().getNickname().equals(nick2) && g.getPlayer2().getNickname().equals(nick1))
            ) {
                jaExiste = true;
                break;
            }
        }
        if (jaExiste) {
            sendMessage(CommandProtocol.formatMessage("Já existe um jogo ativo entre esses dois jogadores."));
            return;
        }

        Game newGame = new game.Game(p1, p2);
        GoBangServer.getSession().addGameForPlayers(p1, p2, newGame);

        // Retorna o ID do jogo criado
        sendMessage("GAMEID:" + newGame.getId());

        // Notifica ambos os jogadores
        String msg1 = "JOGO INICIADO;OPONENTE:" + p2.getNickname() + ";PLAYER1:" + p1.getNickname() + ";PLAYER2:" + p2.getNickname() + ";TABULEIRO:" + boardToString(newGame.getBoard()) + ";SUA_VEZ";
        String msg2 = "JOGO INICIADO;OPONENTE:" + p1.getNickname() + ";PLAYER1:" + p1.getNickname() + ";PLAYER2:" + p2.getNickname() + ";TABULEIRO:" + boardToString(newGame.getBoard()) + ";AGUARDA";
        GoBangServer.notifyPlayers(p1, p2, msg1, msg2);
    }
    
    /**
     * Lida com o timeout de jogada.
     * Se for a vez do jogador, passa o turno e envia "refresh".
     * Caso contrário, envia "timeout_ignored".
     * 
     * Ligações:
     * - Usa Game.passTurnOnTimeout().
     */
    private void handleTimeout() { //NOVO -> PARA LIDAR COM O TEMPORIZADOR CHEGAR A 0s
        Game game = GoBangServer.getSession().getGameByPlayer(player);
        if (game != null && game.getCurrentPlayer().equals(player)) {
            game.passTurnOnTimeout(player);
            sendMessage(CommandProtocol.formatMessage("refresh"));
        } else {
            sendMessage(CommandProtocol.formatMessage("timeout_ignored"));
        }
    }
    
    /**
     * Devolve a lista de jogos ativos do jogador, com tempo restante e se é a sua vez.
     * Também trata timeouts automáticos.
     * 
     * Ligações:
     * - Usa Game.passTurnOnTimeout() se o tempo esgotou.
     */
    private void handleGetGames(Map<String, String> params) {
        if (!params.containsKey("param1")) {
            sendMessage(CommandProtocol.formatMessage("Formato inválido! Use: /getgames nickname"));
            return;
        }
        String nickname = params.get("param1");
        List<Game> games = GoBangServer.getSession().getGamesForPlayer(nickname);
        if (games.isEmpty()) {
            sendMessage(CommandProtocol.formatMessage("NO_GAMES"));
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Game game : games) {
            Player opponent = game.getOtherPlayer(GoBangServer.getPlayers().get(nickname));
            boolean isCurrent = game.getCurrentPlayer().getNickname().equals(nickname);
            long jogadaInicio = game.getCurrentMoveStartMillis();
            int tempoMaximo = game.getMaxMoveTimeSeconds();
            int tempoRestante = tempoMaximo - (int)((System.currentTimeMillis() - jogadaInicio) / 1000);

            // Se o tempo esgotou e o jogo não acabou, passa o turno
            if (tempoRestante <= 0 && !game.isGameOver()) {
                game.passTurnOnTimeout(game.getCurrentPlayer());
                // Atualiza para o novo jogador e tempo
                isCurrent = game.getCurrentPlayer().getNickname().equals(nickname);
                jogadaInicio = game.getCurrentMoveStartMillis();
                tempoRestante = tempoMaximo - (int)((System.currentTimeMillis() - jogadaInicio) / 1000);
                if (tempoRestante < 0) tempoRestante = 0;
            }
            if (tempoRestante < 0) tempoRestante = 0;

            // Adiciona as informações deste jogo ao sb
            sb.append("GAMEID:").append(game.getId())
              .append(";OPONENTE:").append(opponent.getNickname())
              .append(";SUA_VEZ:").append(isCurrent)
              .append(";TEMPO:").append(tempoRestante)
              .append(";");
        }
        sendMessage(CommandProtocol.formatMessage(sb.toString()));
    }

    /**
     * Converte o tabuleiro para string (usado em notificações).
     */
    private String boardToString(char[][] board) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                sb.append(board[i][j]);
            }
            if (i < board.length - 1) sb.append("|");
        }
        return sb.toString();
    }
    
    /**
     * Desconecta o cliente, fecha streams e socket.
     * Se o jogador estava num jogo, declara o adversário vencedor.
     * 
     * Ligações:
     * - Usa Game.getOtherPlayer() e GoBangServer.notifyPlayer().
     */
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
        } finally {
            // Remover este handler da lista global de clientes
            GoBangServer.getClients().remove(this);
        }
    }
    
    /**
     * Desliga o servidor (shutdown).
     * Só pode ser feito por um jogador autenticado.
     * Notifica todos os clientes e encerra o servidor.
     */
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
    
    /**
     * Retorna o socket associado a este handler.
     * Usado por Player.isConnected() e GoBangServer.
     */
    public Socket getSocket() {
    	return socket;
    }
}