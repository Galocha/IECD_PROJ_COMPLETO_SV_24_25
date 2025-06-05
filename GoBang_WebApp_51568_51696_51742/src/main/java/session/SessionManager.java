package session;

import game.Game;
import game.Player;
import java.util.*;
import protocol.CommandProtocol;
import server.GoBangServer;

public class SessionManager {
    private Queue<Player> waitingPlayers = new LinkedList<>();
    private List<Game> activeGames = new ArrayList<>();
    private Map<String, List<Game>> gamesByPlayer = new HashMap<>();

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
    
    public synchronized void removePlayerFromQueue(Player player) {
        waitingPlayers.remove(player);
    }

    public Game getGameByPlayer(Player player) {
        String nick = player.getNickname();
        for (Game game : activeGames) {
            if (game.getPlayer1().getNickname().equals(nick) || game.getPlayer2().getNickname().equals(nick)) {
                return game;
            }
        }
        return null;
    }

    public void endGame(Game game) {
        activeGames.remove(game);
        gamesByPlayer.getOrDefault(game.getPlayer1().getNickname(), new ArrayList<>()).remove(game);
        gamesByPlayer.getOrDefault(game.getPlayer2().getNickname(), new ArrayList<>()).remove(game);
        
        //atualização do tempo de jogo para cada jogador
        long gameDuration = System.currentTimeMillis() - game.getStartTime();
        game.getPlayer1().addPlayTime(game.getPlayer1().isConnected() ? gameDuration : 0); // Tempo para Player1
        game.getPlayer2().addPlayTime(game.getPlayer2().isConnected() ? gameDuration : 0); // Tempo para Player2
        
        Player winner = game.getWinner();
        if (winner != null) { //vitória normal
            winner.setWins(winner.getWins() + 1);
            Player loser = game.getPlayer1().equals(winner) ? game.getPlayer2() : game.getPlayer1();
            loser.setLosses(loser.getLosses() + 1);
        } else { //saber se algum dos players desconectou para dar a vitória ao conectado
            boolean p1Connected = game.getPlayer1().isConnected();
            boolean p2Connected = game.getPlayer2().isConnected();
            if (p1Connected && !p2Connected) {
                game.getPlayer1().setWins(game.getPlayer1().getWins() + 1);
                game.getPlayer2().setLosses(game.getPlayer2().getLosses() + 1);
            } else if (!p1Connected && p2Connected) {
                game.getPlayer2().setWins(game.getPlayer2().getWins() + 1);
                game.getPlayer1().setLosses(game.getPlayer1().getLosses() + 1);
            }
        }
        // Notifica os jogadores sobre como jogar novamente
        String playAgainMessage = "O jogo terminou! Digite /play para entrar na fila e jogar novamente.";
        
        if (game.getPlayer1().isConnected()) {
            GoBangServer.notifyPlayer(game.getPlayer1(), CommandProtocol.formatMessage(playAgainMessage));
        }
        
        if (game.getPlayer2().isConnected()) {
            GoBangServer.notifyPlayer(game.getPlayer2(), CommandProtocol.formatMessage(playAgainMessage));
        }
        GoBangServer.savePlayersToXML();
    }

    public List<Game> getActiveGames() {
        return activeGames;
    }
    
    public Queue<Player> getWaitingPlayers() {
        return waitingPlayers;
    }
    
    public String getWaitingList(){
    	String waitingList = "Jogadores em espera:";
    	if (!waitingPlayers.isEmpty()) {
    		for (Player p : waitingPlayers) {
        		waitingList += "\n" + p.getNickname() + " " + p.getNationality();
        	}
    	}
    	return waitingList;
    }
    
    public String getActiveGamesToString() {
    	String activeGamesString = "Jogos ativos de momento:";
    	if(!activeGames.isEmpty()) {
    		for (Game g : activeGames) {
    			activeGamesString += "\nJogo de " + g.getPlayer1().getNickname() + ", contra " + g.getPlayer2().getNickname();
    		}
    	}
    	return activeGamesString;
    }
    
    //NOVOS MÉTODOS
    public List<Game> getGamesForPlayer(String nickname) {
        return gamesByPlayer.getOrDefault(nickname, new ArrayList<>());
    }
    
    public void addGameForPlayers(Player p1, Player p2, Game game) {
        gamesByPlayer.computeIfAbsent(p1.getNickname(), k -> new ArrayList<>()).add(game);
        gamesByPlayer.computeIfAbsent(p2.getNickname(), k -> new ArrayList<>()).add(game);
        activeGames.add(game);
    }
    
    public Game getGameById(String nickname, String gameId) {
        List<Game> games = getGamesForPlayer(nickname);
        for (Game g : games) {
            if (g.getId().equals(gameId)) return g;
        }
        return null;
    }
}

