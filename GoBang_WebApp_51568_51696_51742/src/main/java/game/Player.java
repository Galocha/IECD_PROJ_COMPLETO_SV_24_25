package game;

import client.ClientHandler;
import java.util.ArrayList;
import java.util.List;
import server.GoBangServer;

public class Player {
    private String nickname;
    private String password;
    private String nationality;
    private int age;
    private String photo; //path para a foto
    private int wins;
    private int losses;
    private List<Long> playTimes; //tempo em milissegundos
    private String preferredColor;
    private List<String> gameUUIDs;

    // Construtor
    public Player(String nickname, String password, String nationality, int age, String photo) {
        this.nickname = nickname;
        this.password = password;
        this.nationality = nationality;
        this.age = age;
        this.photo = photo;
        this.wins = 0; //sem vitórias
        this.losses = 0; //sem derrotas
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

    public void addPlayTime(long playTime) {
        this.playTimes.add(playTime); // Adiciona o tempo de jogo à lista
    }
    
    public void addGameUUID(String uuid) {
        gameUUIDs.add(uuid);
    }
    
    public List<String> getGameUUIDs() {
        return gameUUIDs;
    }
    
    public String getPreferredColor() {
        return preferredColor != null ? preferredColor : "#ffffff"; // Default branco
    }
    
    public void setPreferredColor(String preferredColor) {
        this.preferredColor = preferredColor;
    }
    
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
    
    public long getTotalPlayTime() {
        long total = 0;
        for (long time : playTimes) {
            total += time;
        }
        return total;
    }

    public String getTotalPlayTimeFormatted() {
        long totalTime = getTotalPlayTime();
        long totalSeconds = totalTime / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d min %d seg", minutes, seconds);
    }
    
    public long convertFormattedPlayTimeToMillis(String formattedTime) {
        try {
            // Divide o tempo formatado em minutos e segundos
            String[] parts = formattedTime.split("\\D+"); // Divide por caracteres não numéricos
            if (parts.length >= 2) { // Certifique-se de que há ao menos minutos e segundos
                long minutes = Long.parseLong(parts[0]); // Primeiro número é minutos
                long seconds = Long.parseLong(parts[1]); // Segundo número é segundos
                return (minutes * 60 + seconds) * 1000; // Converte para milissegundos
            }
        } catch (NumberFormatException e) {
            System.err.println("Formato inválido para tempo: " + formattedTime);
        }
        return 0L; // Retorna 0 se o formato estiver incorreto
    }
    
    public boolean isConnected() {
        for (ClientHandler client : GoBangServer.getClients()) {
            if (client.getPlayer() != null && client.getPlayer().equals(this)) {
                try {
                    return !client.getSocket().isClosed();
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }
    
    //métodos de verificação -> NOVO
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