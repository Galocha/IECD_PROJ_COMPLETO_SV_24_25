package game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Game {
	private final String id;
    private static final int BOARD_SIZE = 15;
    private char[][] board;
    private Player player1;
    private Player player2;
    private Player currentPlayer;
    private boolean isGameOver;
    private Player winner;
    private long startTime;
    private Map<Player, Long> lastMoveTime = new HashMap<>();

    public Game(Player player1, Player player2) {
    	this.id = UUID.randomUUID().toString();
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayer = player1;
        this.board = new char[BOARD_SIZE][BOARD_SIZE];
        this.startTime = System.currentTimeMillis();
        initializeBoard();
        lastMoveTime.put(player1, System.currentTimeMillis());
        lastMoveTime.put(player2, System.currentTimeMillis());
    }

    // Inicializa o tabuleiro vazio
    private void initializeBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = '.';
            }
        }
    }

    // Processa uma jogada e retorna o resultado - ATUALIZADO PARA PODER SER USADO EM FRONTEND
    public String processMove(Player player, int row, int col) {
        if (isGameOver) {
            return "error:game_over";
        }
        if (!player.equals(currentPlayer)) {
            return "error:not_your_turn";
        }
        if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE || board[row][col] != '.') {
            return "error:invalid_move";
        }

        board[row][col] = (currentPlayer == player1) ? 'X' : 'O';
        lastMoveTime.put(currentPlayer, System.currentTimeMillis()); // Atualiza o tempo da última jogada

        if (checkWinner(row, col)) {
            isGameOver = true;
            winner = currentPlayer;
            return "VITÓRIA de " + currentPlayer.getNickname();
        }
        if (isBoardFull()) {
            isGameOver = true;
            return "EMPATE!";
        }

        // Passa o turno
        currentPlayer = (currentPlayer == player1) ? player2 : player1;
        return "refresh";
    }
    
    //EM CASO DE TIMEOUT DA JOGADA, PASSA AO PRÓXIMO JOGADOR
    public void passTurnOnTimeout(Player player) {
        if (!isGameOver && getCurrentPlayer().equals(player)) {
            // Apenas passa a vez, não marca derrota
            currentPlayer = getOtherPlayer(player);
        }
    }

    // Verifica se há um vencedor
    private boolean checkWinner(int row, int col) {
        char symbol = board[row][col];
        return checkDirection(row, col, 1, 0, symbol) ||  // Horizontal
               checkDirection(row, col, 0, 1, symbol) ||  // Vertical
               checkDirection(row, col, 1, 1, symbol) ||  // Diagonal \
               checkDirection(row, col, 1, -1, symbol);   // Diagonal /
    }

    // Verifica uma direção específica
    private boolean checkDirection(int row, int col, int rowDir, int colDir, char symbol) {
        int count = 1;
        count += countInDirection(row, col, rowDir, colDir, symbol);
        count += countInDirection(row, col, -rowDir, -colDir, symbol);
        return count >= 5;
    }

    // Conta peças consecutivas numa direção
    private int countInDirection(int row, int col, int rowDir, int colDir, char symbol) {
        int count = 0;
        int r = row + rowDir;
        int c = col + colDir;
        while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == symbol) {
            count++;
            r += rowDir;
            c += colDir;
        }
        return count;
    }

    // Verifica se o tabuleiro está cheio (empate)
    private boolean isBoardFull() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == '.') return false;
            }
        }
        return true;
    }

    // Exibe o tabuleiro na consola (FORMATADO)
    public void displayBoard() {
        // Cabeçalho com números das colunas
        System.out.print("   ");
        for (int col = 0; col < BOARD_SIZE; col++) {
            System.out.printf("%2d ", col);
        }
        System.out.println();

        // Linha separadora
        System.out.print("  +");
        for (int col = 0; col < BOARD_SIZE; col++) {
            System.out.print("---+");
        }
        System.out.println();

        // Tabuleiro com números das linhas e peças
        for (int row = 0; row < BOARD_SIZE; row++) {
            System.out.printf("%2d|", row);
            for (int col = 0; col < BOARD_SIZE; col++) {
                char c = board[row][col];
                // Adiciona cores (opcional)
                String piece = (c == 'X') ? "\u001B[31mX\u001B[0m" : 
                              (c == 'O') ? "\u001B[34mO\u001B[0m" : " ";
                System.out.printf(" %s |", piece);
            }
            System.out.println();

            // Linha separadora
            System.out.print("  +");
            for (int col = 0; col < BOARD_SIZE; col++) {
                System.out.print("---+");
            }
            System.out.println();
        }
    }
    
 // Retorna o tabuleiro como String formatada (para enviar aos clientes)
    public String getBoardAsString() {
        StringBuilder sb = new StringBuilder();
        
        // Cabeçalho com números das colunas
        sb.append("   "); // Espaço inicial para alinhar os números às colunas
        for (int col = 0; col < board[0].length; col++) {
            sb.append(String.format(" %-2d ", col));
        }
        sb.append("\n");

        // Linha separadora
        sb.append("  +");
        for (int col = 0; col < BOARD_SIZE; col++) {
            sb.append("---+");
        }
        sb.append("\n");

        // Tabuleiro com números das linhas
        for (int row = 0; row < BOARD_SIZE; row++) {
            sb.append(String.format("%2d|", row));
            for (int col = 0; col < BOARD_SIZE; col++) {
                char c = board[row][col];
                String piece = (c == 'X') ? "X" : (c == 'O') ? "O" : " ";
                sb.append(String.format(" %s |", piece));
            }
            sb.append("\n");

            // Linha separadora
            sb.append("  +");
            for (int col = 0; col < BOARD_SIZE; col++) {
                sb.append("---+");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
    
    //FUNÇÃO NOVA
    public int getMoveCount() {
        int count = 0;
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == 'X' || board[i][j] == 'O') {
                    count++;
                }
            }
        }
        return count;
    }

    // Getters
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public Player getCurrentPlayer() { return currentPlayer; }
    public Player getWinner() { return winner; }
    public boolean isGameOver() { return isGameOver; }
    public long getStartTime() { return startTime; }
    public Player getOtherPlayer(Player current) { return player1.equals(current) ? player2 : player1; }
    public char[][] getBoard() { return board; }
    public String getId() { return id; }
}