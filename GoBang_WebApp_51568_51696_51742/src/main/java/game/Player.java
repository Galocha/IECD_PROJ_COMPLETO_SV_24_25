package game;

import client.ClientHandler;
import java.util.ArrayList;
import java.util.List;
import server.GoBangServer;

/**
 * Classe que representa um jogador do GoBang.
 * Mantém toda a informação relevante do jogador, incluindo estatísticas,
 * preferências e métodos utilitários para integração com o servidor e frontend.
 * 
 * É utilizada em praticamente todas as classes do sistema:
 * - [game.Game]: cada jogo referencia dois Player.
 * - [client.ClientHandler]: para autenticação, notificações, etc.
 * - [server.GoBangServer]: para gerir lista global de jogadores.
 * - [session.SessionManager]: para associar jogadores a jogos.
 * - JSPs como lobby.jsp, perfil.jsp, perfilPublico.jsp, etc.
 */
public class Player {
	// Identificação e dados pessoais
    private String nickname; // Nome único do jogador
    private String password; // Palavra-passe (armazenada em texto simples)
    private String nationality; // Nacionalidade
    private int age; // Idade
    private String photo; // Caminho para a foto de perfil

    // Estatísticas de jogo
    private int wins; // Número de vitórias
    private int losses; // Número de derrotas
    private List<Long> playTimes; // Lista de tempos de jogo (em ms) para cada jogo

    // Preferências e histórico
    private String preferredColor; // Cor preferida para o frontend
    private List<String> gameUUIDs; // Lista de UUIDs dos jogos em que participou

    /**
     * Construtor principal.
     * Inicializa todos os campos obrigatórios e listas.
     * 
     * @param nickname Nome do jogador (único)
     * @param password Palavra-passe
     * @param nationality Nacionalidade
     * @param age Idade
     * @param photo Caminho para a foto
     */
    public Player(String nickname, String password, String nationality, int age, String photo) {
        this.nickname = nickname;
        this.password = password;
        this.nationality = nationality;
        this.age = age;
        this.photo = photo;
        this.wins = 0; // Inicialmente sem vitórias
        this.losses = 0; // Inicialmente sem derrotas
        this.playTimes = new ArrayList<>();
        this.gameUUIDs = new ArrayList<>();
    }

    //Getters e setters para todos os atributos
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public List<Long> getPlayTimes() {
        return playTimes;
    }

    /**
     * Adiciona um tempo de jogo (em ms) à lista de tempos.
     * Usado ao terminar cada jogo.
     * 
     * Exemplo de utilização:
     * - [session.SessionManager.endGame()]: ao terminar um jogo, regista o tempo.
     */
    public void addPlayTime(long playTime) {
        this.playTimes.add(playTime);
    }
    
    /**
     * Adiciona o UUID de um jogo à lista de jogos do jogador.
     * Garante que não há duplicados.
     */
    public void addGameUUID(String uuid) {
        if (!gameUUIDs.contains(uuid)) {
            gameUUIDs.add(uuid);
        }
    }
    
    public List<String> getGameUUIDs() {
        return gameUUIDs;
    }
    
    /**
     * Retorna a cor preferida do jogador para o frontend.
     * Se não estiver definida, retorna branco por omissão.
     * 
     * Usado em:
     * - jsps para personalização da cor de fundo da página
     */
    public String getPreferredColor() {
        return preferredColor != null ? preferredColor : "#ffffff"; // Default branco
    }
    
    public void setPreferredColor(String preferredColor) {
        this.preferredColor = preferredColor;
    }
    
    /**
     * Retorna a lista de tempos de jogo formatados (ex: "2 min 15 seg").
     * Útil para mostrar estatísticas no frontend.
     * 
     * Usado em:
     * - [perfil.jsp](../../webapp/jsp/perfil.jsp), [perfilPublico.jsp](../../webapp/jsp/perfilPublico.jsp)
     */
    public List<String> getFormattedPlayTimes() {
        List<String> formattedTimes = new ArrayList<>();
        for (long playTime : playTimes) {
            long totalSeconds = playTime / 1000;
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            formattedTimes.add(String.format("%d min %d seg", minutes, seconds));
        }
        return formattedTimes;
    }
    
    /**
     * Soma todos os tempos de jogo do jogador (em ms).
     * 
     * Usado em:
     * - [perfil.jsp](../../webapp/jsp/perfil.jsp) e [perfilPublico.jsp](../../webapp/jsp/perfilPublico.jsp)
     */
    public long getTotalPlayTime() {
        long total = 0;
        for (long time : playTimes) {
            total += time;
        }
        return total;
    }

    /**
     * Retorna o tempo total de jogo já formatado (ex: "5 min 30 seg")
     */
    public String getTotalPlayTimeFormatted() {
        long totalTime = getTotalPlayTime();
        long totalSeconds = totalTime / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d min %d seg", minutes, seconds);
    }
    
    /**
     * Converte uma string formatada (ex: "2 min 15 seg") para milissegundos.
     * Útil para importar/exportar tempos de jogo.
     * 
     * Usado em:
     * - [GoBangServer.loadPlayersFromXML()] ao ler tempos do XML.
     */
    public long convertFormattedPlayTimeToMillis(String formattedTime) {
        try {
            // Divide o tempo formatado em minutos e segundos
            String[] parts = formattedTime.split("\\D+"); // Divide por caracteres não numéricos
            if (parts.length >= 2) { // Certifique-se de que há pelo menos minutos e segundos
                long minutes = Long.parseLong(parts[0]); // Primeiro número é minutos
                long seconds = Long.parseLong(parts[1]); // Segundo número é segundos
                return (minutes * 60 + seconds) * 1000; // Converte para milissegundos
            }
        } catch (NumberFormatException e) {
            System.err.println("Formato inválido para tempo: " + formattedTime);
        }
        return 0L; // Retorna 0 se o formato estiver incorreto
    }
    
    /**
     * Verifica se o jogador está atualmente ligado ao servidor.
     * Procura nos clientes ativos do GoBangServer.
     * 
     * Usado em:
     * - [SessionManager.endGame()](../session/SessionManager.java) para saber se pode notificar o jogador.
     * - [GoBangServer.notifyPlayer()](../server/GoBangServer.java)
     */
    public boolean isConnected() {
        for (client.ClientHandler client : server.GoBangServer.getClients()) {
            if (client.getPlayer() != null) {
                System.out.println("[DEBUG] Comparar " + client.getPlayer().getNickname() + " com " + this.nickname);
                if (client.getPlayer().equals(this)) {
                    try {
                        System.out.println("[DEBUG] Socket fechado? " + client.getSocket().isClosed());
                        return !client.getSocket().isClosed();
                    } catch (Exception e) {
                        return false;
                    }
                }
            }
        }
        return false;
    }
    
    //métodos de verificação -> NOVO
    /**
     * Dois jogadores são iguais se tiverem o mesmo nickname.
     * Importante para garantir unicidade em listas, mapas, etc.
     * 
     * Usado em:
     * - [Game.getOtherPlayer()](Game.java)
     * - [GoBangServer.getPlayers()](../server/GoBangServer.java)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return nickname.equals(player.nickname);
    }
    
    @Override
    public int hashCode() {
        return nickname.hashCode();
    }

    /**
     * Retorna uma string formatada com os dados principais do jogador.
     * Útil para debug e logs.
     */
    @Override
    public String toString() {
    	return String.format(
                "Nickname: %s\nNacionalidade: %s\nIdade: %d\nVitórias: %d\nDerrotas: %d\nTempo de Jogo: %s",
                getNickname(),
                getNationality(),
                getAge(),
                getWins(),
                getLosses(),
                String.join(", ", getFormattedPlayTimes())
            );
    }
}