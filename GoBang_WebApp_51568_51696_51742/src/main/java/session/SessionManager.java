package session;

import game.Game;
import game.Player;
import java.util.*;
import protocol.CommandProtocol;
import server.GoBangServer;

/**
 * Classe responsável por gerir as sessões de jogo no servidor.
 * Mantém a fila de espera de jogadores, jogos ativos e associações entre jogadores e jogos.
 * 
 * Ligações:
 * - Usada por GoBangServer para gerir jogos e emparelhamentos.
 * - ClientHandler usa métodos desta classe para adicionar jogadores à fila, terminar jogos, etc.
 * - Game e Player são usados para criar e gerir jogos e estatísticas.
 */
public class SessionManager {
    private Queue<Player> waitingPlayers = new LinkedList<>(); // Fila de espera de jogadores para emparelhamento automático
    private List<Game> activeGames = new ArrayList<>(); // Lista de jogos ativos no servidor
    private Map<String, List<Game>> gamesByPlayer = new HashMap<>(); // Mapa: nickname -> lista de jogos em que o jogador está envolvido

    /**
     * Adiciona um jogador à fila de espera.
     * Se houver pelo menos dois jogadores, cria um novo jogo e emparelha-os.
     * Notifica ambos os jogadores do início do jogo e de quem começa.
     * 
     * Usado em:
     * - ClientHandler.handlePlay()
     */
    public synchronized void addPlayerToQueue(Player player) {
    	if (!waitingPlayers.contains(player)) {
            waitingPlayers.add(player);
            System.out.println("Jogador adicionado à fila: " + player.getNickname());

	        if (waitingPlayers.size() >= 2) {
	            Player player1 = waitingPlayers.poll();
	            Player player2 = waitingPlayers.poll();
	            Game newGame = new Game(player1, player2);
	            addGameForPlayers(player1, player2, newGame);
	
	            // Mensagens personalizadas para cada jogador
	            String messagePlayer1 = "JOGO INICIADO\nEstás a jogar contra: " + player2.getNickname() + 
	                                 "\nO teu símbolo: X" + 
	                                 "\nUse /move [linha] [coluna] para jogar";
	
	            String messagePlayer2 = "JOGO INICIADO\nEstás a jogar contra: " + player1.getNickname() + 
	                                 "\nO teu símbolo: O" + 
	                                 "\nUse /move [linha] [coluna] para jogar";
	
	            GoBangServer.notifyPlayers(player1, player2, messagePlayer1, messagePlayer2);
	            
	            // Notifica quem começa
	            if (newGame.getCurrentPlayer().equals(player1)) {
	            	String msgPlayer1 = "🏁 Começas tu " + player1.getNickname();
	            	String msgPlayer2 = "⏳ Começa o player " + player1.getNickname() + ". Espera pela tua vez!";
	            	GoBangServer.notifyPlayers(player1, player2, msgPlayer1, msgPlayer2);
	            } else if (newGame.getCurrentPlayer().equals(player2)) {
	            	String msgPlayer2 = "🏁 Começas tu " + player2.getNickname();
	            	String msgPlayer1 = "⏳ Começa o player " + player2.getNickname() + ". Espera pela tua vez!";
	            	GoBangServer.notifyPlayers(player1, player2, msgPlayer1, msgPlayer2);
	            }
	        }
    	}
    }
    
    /**
     * Remove um jogador da fila de espera.
     * Usado para cancelar espera ou ao emparelhar.
     */
    public synchronized void removePlayerFromQueue(Player player) {
        waitingPlayers.remove(player);
    }

    /**
     * Procura e devolve o jogo ativo em que o jogador está envolvido.
     * Retorna null se não estiver em nenhum jogo.
     * 
     * Usado em:
     * - ClientHandler.handleMove(), handleGet(), handleSurrender(), etc.
     */
    public Game getGameByPlayer(Player player) {
        String nick = player.getNickname();
        for (Game game : activeGames) {
            if (game.getPlayer1().getNickname().equals(nick) || game.getPlayer2().getNickname().equals(nick)) {
                return game;
            }
        }
        return null;
    }

    /**
     * Termina um jogo, atualiza estatísticas dos jogadores, guarda resultados para polling,
     * notifica ambos os jogadores e remove o jogo das listas.
     * 
     * Ligações:
     * - Chamado por ClientHandler.handleMove(), handleSurrender(), disconnectClient(), etc.
     * - Usa GoBangServer.getLastGameResults() para guardar mensagens de fim de jogo.
     * - Usa Player.addGameUUID(), addPlayTime(), setWins(), setLosses().
     */
    public void endGame(Game game) {
        if (game.isEnded()) return;
        game.setEnded(true);

        long gameDuration = System.currentTimeMillis() - game.getStartTime();
        game.getPlayer1().addGameUUID(game.getId());
        game.getPlayer1().addPlayTime(gameDuration);
        game.getPlayer2().addGameUUID(game.getId());
        game.getPlayer2().addPlayTime(gameDuration);

        Player winner = game.getWinner();
        Player player1 = game.getPlayer1();
        Player player2 = game.getPlayer2();

        if (winner != null) {
            winner.setWins(winner.getWins() + 1);
            Player loser = player1.equals(winner) ? player2 : player1;
            loser.setLosses(loser.getLosses() + 1);

            // Guarda o resultado para ambos os jogadores (usado em polling)
            GoBangServer.getLastGameResults().put(winner.getNickname(), "FIM DE JOGO! Ganhaste!");
            GoBangServer.getLastGameResults().put(loser.getNickname(), "FIM DE JOGO! Perdeste!");
        } else {
        	// Se não há vencedor, verifica se algum está offline para atribuir vitória
            boolean p1Connected = player1.isConnected();
            boolean p2Connected = player2.isConnected();
            if (p1Connected && !p2Connected) {
                player1.setWins(player1.getWins() + 1);
                player2.setLosses(player2.getLosses() + 1);
                GoBangServer.getLastGameResults().put(player1.getNickname(), "FIM DE JOGO! Ganhaste!");
                GoBangServer.getLastGameResults().put(player2.getNickname(), "FIM DE JOGO! Perdeste!");
            } else if (!p1Connected && p2Connected) {
                player2.setWins(player2.getWins() + 1);
                player1.setLosses(player1.getLosses() + 1);
                GoBangServer.getLastGameResults().put(player2.getNickname(), "FIM DE JOGO! Ganhaste!");
                GoBangServer.getLastGameResults().put(player1.getNickname(), "FIM DE JOGO! Perdeste!");
            } else {
                // Empate
                GoBangServer.getLastGameResults().put(player1.getNickname(), "FIM DE JOGO! O jogo terminou num empate.");
                GoBangServer.getLastGameResults().put(player2.getNickname(), "FIM DE JOGO! O jogo terminou num empate.");
            }
        }

        // Mensagem para ambos: convite para jogar novamente
        String playAgainMessage = "O jogo terminou! Digite /play para entrar na fila e jogar novamente.";

        if (player1.isConnected()) {
            GoBangServer.notifyPlayer(player1, CommandProtocol.formatMessage(playAgainMessage));
        }
        if (player2.isConnected()) {
            GoBangServer.notifyPlayer(player2, CommandProtocol.formatMessage(playAgainMessage));
        }

        // Atualiza o mapa global de jogadores
        Map<String, Player> players = GoBangServer.getPlayers();
        players.put(player1.getNickname(), player1);
        players.put(player2.getNickname(), player2);
        GoBangServer.savePlayersToXML();
        GoBangServer.saveRankingToXML();
        
        removeGame(game);
    }

    /**
     * Devolve a lista de jogos ativos.
     * Usado para debug ou estatísticas.
     */
    public List<Game> getActiveGames() {
        return activeGames;
    }
    
    /**
     * Devolve a fila de espera de jogadores.
     * Usado por ClientHandler e para mostrar no frontend.
     */
    public Queue<Player> getWaitingPlayers() {
        return waitingPlayers;
    }
    
    /**
     * Devolve uma string com a lista de jogadores em espera.
     * Usado para mostrar no frontend (/waitingList).
     */
    public String getWaitingList(){
    	String waitingList = "Jogadores em espera:";
    	if (!waitingPlayers.isEmpty()) {
    		for (Player p : waitingPlayers) {
        		waitingList += "\n" + p.getNickname() + " " + p.getNationality();
        	}
    	}
    	return waitingList;
    }
    
    /**
     * Devolve uma string com os jogos ativos.
     * Usado para debug ou estatísticas.
     */
    public String getActiveGamesToString() {
    	String activeGamesString = "Jogos ativos de momento:";
    	if(!activeGames.isEmpty()) {
    		for (Game g : activeGames) {
    			activeGamesString += "\nJogo de " + g.getPlayer1().getNickname() + ", contra " + g.getPlayer2().getNickname();
    		}
    	}
    	return activeGamesString;
    }
    
    // =======================
    // NOVOS MÉTODOS PARA SUPORTE A VÁRIOS JOGOS POR JOGADOR
    // =======================
    
    /**
     * Devolve a lista de jogos em que o jogador está envolvido.
     * Usado em ClientHandler.handleGetGames().
     */
    public List<Game> getGamesForPlayer(String nickname) {
        return gamesByPlayer.getOrDefault(nickname, new ArrayList<>());
    }
    
    /**
     * Adiciona um novo jogo às listas de ambos os jogadores e à lista global de jogos ativos.
     * Usado ao criar jogos automáticos ou por convite.
     * 
     * computeIfAbsent: serve para garantir que existe uma entrada (chave) no mapa. Se não existir,
     * ele cria e associa um novo valor usando a função fornecida. Isto evita ter de se fazer um if.
     * Neste caso:
     * - Para cada jogador (p1 e p2), verifica se já existe uma lista de jogos associada ao nickname no mapa gamesByPlayer
     * - Se não existir, cria uma nova ArrayList e associa ao nickname.
     * - Depois, adiciona o jogo à lista desse jogador.
     */
    public void addGameForPlayers(Player p1, Player p2, Game game) {
        gamesByPlayer.computeIfAbsent(p1.getNickname(), k -> new ArrayList<>()).add(game);
        gamesByPlayer.computeIfAbsent(p2.getNickname(), k -> new ArrayList<>()).add(game);
        activeGames.add(game);
    }
    
    /**
     * Procura um jogo específico pelo ID, dado o nickname do jogador.
     * Usado para identificar jogos em simultâneo.
     */
    public Game getGameById(String nickname, String gameId) {
        List<Game> games = getGamesForPlayer(nickname);
        for (Game g : games) {
            if (g.getId().equals(gameId)) return g;
        }
        return null;
    }

    /**
     * Remove um jogo das listas de ambos os jogadores e da lista global.
     * Usado ao terminar um jogo.
     */
    public void removeGame(Game game) {
        List<Game> list1 = gamesByPlayer.getOrDefault(game.getPlayer1().getNickname(), new ArrayList<>());
        list1.remove(game);
        if (list1.isEmpty()) {
            gamesByPlayer.remove(game.getPlayer1().getNickname());
        }
        List<Game> list2 = gamesByPlayer.getOrDefault(game.getPlayer2().getNickname(), new ArrayList<>());
        list2.remove(game);
        if (list2.isEmpty()) {
            gamesByPlayer.remove(game.getPlayer2().getNickname());
        }
        activeGames.remove(game);
    }
}

/*
UTILIZAÇÕES E LIGAÇÕES:
- SessionManager é instanciado e acedido via GoBangServer.getSession().
- ClientHandler usa addPlayerToQueue, getGameByPlayer, endGame, getGamesForPlayer, etc.
- Game é criado e terminado por esta classe.
- Player é usado para emparelhamento, estatísticas e notificações.
- GoBangServer.getLastGameResults() é usado para guardar mensagens de fim de jogo (ex: "error:game_over").
*/