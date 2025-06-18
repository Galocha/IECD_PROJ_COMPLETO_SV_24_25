package game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Classe que representa um jogo de GoBang entre dois jogadores.
 * Responsável por gerir o estado do tabuleiro, turnos, validação de jogadas,
 * deteção de vitória/empate, timeout de jogadas e formatação do tabuleiro.
 * 
 * Utilizada principalmente por:
 * - session.SessionManager: para criar, terminar e gerir jogos ativos.
 * - client.ClientHandler: para processar comandos dos clientes (ex: /move, /get).
 * - server.GoBangServer: para aceder ao estado dos jogos e notificar jogadores.
 */
public class Game {
	private final String id; // Identificador único do jogo (UUID)
    private static final int BOARD_SIZE = 15; // Tamanho do tabuleiro (15x15)
    private char[][] board; // Matriz do tabuleiro ('.' = vazio, 'X'/'O' = peças)
    // Jogadores do jogo
    private Player player1; 
    private Player player2;
    private Player currentPlayer; // Jogador cuja vez é atualmente
    private boolean isGameOver; // Indica se o jogo terminou (vitória/empate)
    private boolean ended = false; // Indica se o jogo já foi processado como terminado (para evitar repetições)
    private int checksAfterEnd = 0; // Contador de verificações após o fim (para polling)
    private Player winner; // Jogador vencedor (null se empate)
    private long startTime; // Timestamp do início do jogo
    private Map<Player, Long> lastMoveTime = new HashMap<>(); // Mapa: jogador -> timestamp da última jogada (para timeout)

    /**
     * Construtor do jogo.
     * Inicializa o tabuleiro, define o jogador inicial, regista tempos de início.
     * @param player1 Jogador 1 (começa sempre)
     * @param player2 Jogador 2
     */
    public Game(Player player1, Player player2) {
    	this.id = UUID.randomUUID().toString(); // Gera um UUID único para o jogo
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayer = player1; // Jogador 1 começa
        this.board = new char[BOARD_SIZE][BOARD_SIZE];
        this.startTime = System.currentTimeMillis();
        initializeBoard(); // Preenche o tabuleiro com '.'
        lastMoveTime.put(currentPlayer, startTime); // Marca início da 1ª jogada
        lastMoveTime.put(player1, System.currentTimeMillis());
        lastMoveTime.put(player2, System.currentTimeMillis());
    }

    /**
     * Inicializa o tabuleiro vazio, preenchendo todas as casas com '.'.
     * Usado apenas no início do jogo.
     */
    private void initializeBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = '.';
            }
        }
    }

    /**
     * Processa uma jogada de um jogador.
     * Valida se o jogo já terminou, se é a vez do jogador, se a posição é válida.
     * Atualiza o tabuleiro, verifica vitória/empate, passa o turno.
     * 
     * @param player Jogador que faz a jogada
     * @param row Linha da jogada
     * @param col Coluna da jogada
     * @return String de resultado: 
     *         - "error:game_over" se o jogo já terminou
     *         - "error:not_your_turn" se não é a vez do jogador
     *         - "error:invalid_move" se a jogada é inválida
     *         - "VITÓRIA de <nick>" se o jogador venceu
     *         - "EMPATE!" se o tabuleiro ficou cheio
     *         - "refresh" se a jogada foi válida e o turno passou
     * 
     * Onde é usada/tratada:
     * - client.ClientHandler.handleMove() lê o retorno e envia ao cliente.
     * - gameAction.jsp e play.jsp tratam as respostas para mostrar ao utilizador.
     */
    public String processMove(Player player, int row, int col) {
        if (isGameOver) { // Se o jogo já terminou, não permite mais jogadas
            return "error:game_over";
        }
        if (!player.equals(currentPlayer)) { // Só o jogador da vez pode jogar
            return "error:not_your_turn";
        }
        if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE || board[row][col] != '.') { // Jogada fora do tabuleiro ou casa ocupada
            return "error:invalid_move";
        }

        // Marca a peça no tabuleiro ('X' para player1, 'O' para player2)
        board[row][col] = (currentPlayer == player1) ? 'X' : 'O';
        // Atualiza o tempo da última jogada deste jogador
        lastMoveTime.put(currentPlayer, System.currentTimeMillis()); // Atualiza o tempo da última jogada

        // Verifica se esta jogada deu vitória
        if (checkWinner(row, col)) {
            isGameOver = true;
            winner = currentPlayer;
            return "VITÓRIA de " + currentPlayer.getNickname();
        }
        
        // Verifica empate (tabuleiro cheio)
        if (isBoardFull()) {
            isGameOver = true;
            return "EMPATE!";
        }

        // Passa a vez ao outro jogador
        currentPlayer = (currentPlayer == player1) ? player2 : player1;
        lastMoveTime.put(currentPlayer, System.currentTimeMillis());
        return "refresh";
    }
    
    /**
     * Em caso de timeout, passa a vez ao próximo jogador.
     * Só executa se ainda não terminou e se for a vez do jogador indicado.
     * 
     * Usado em:
     * - client.ClientHandler.handleGet() e handleGetGames() para forçar passagem de turno.
     */
    public void passTurnOnTimeout(Player player) {
        if (!isGameOver && getCurrentPlayer().equals(player)) {
            currentPlayer = getOtherPlayer(player);
            lastMoveTime.put(currentPlayer, System.currentTimeMillis());
        }
    }

    /**
     * Verifica se a jogada feita na posição (row, col) resultou em vitória.
     * Procura 5 peças consecutivas do mesmo símbolo em todas as direções.
     * 
     * @param row Linha da jogada
     * @param col Coluna da jogada
     * @return true se encontrou 5 ou mais peças consecutivas
     */
    private boolean checkWinner(int row, int col) {
        char symbol = board[row][col];
        return checkDirection(row, col, 1, 0, symbol) ||  // Horizontal
               checkDirection(row, col, 0, 1, symbol) ||  // Vertical
               checkDirection(row, col, 1, 1, symbol) ||  // Diagonal \
               checkDirection(row, col, 1, -1, symbol);   // Diagonal /
    }

    /**
     * Verifica uma direção específica a partir de (row, col).
     * Conta peças consecutivas do mesmo símbolo para ambos os lados.
     * 
     * @param row Linha inicial
     * @param col Coluna inicial
     * @param rowDir Direção linha (+1, 0, -1)
     * @param colDir Direção coluna (+1, 0, -1)
     * @param symbol Símbolo a procurar ('X' ou 'O')
     * @return true se encontrou 5 ou mais consecutivas
     */
    private boolean checkDirection(int row, int col, int rowDir, int colDir, char symbol) {
        int count = 1;
        count += countInDirection(row, col, rowDir, colDir, symbol);
        count += countInDirection(row, col, -rowDir, -colDir, symbol);
        return count >= 5;
    }

    /**
     * Conta peças consecutivas numa direção a partir de (row, col).
     * 
     * @param row Linha inicial
     * @param col Coluna inicial
     * @param rowDir Incremento linha
     * @param colDir Incremento coluna
     * @param symbol Símbolo a procurar
     * @return Número de peças consecutivas encontradas
     */
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

    /**
     * Verifica se o tabuleiro está cheio (empate).
     * 
     * @return true se não há casas vazias
     */
    private boolean isBoardFull() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == '.') return false;
            }
        }
        return true;
    }

    /**
     * Mostra o tabuleiro formatado na consola (apenas para debug/IDE).
     * Usa cores ANSI para X (vermelho) e O (azul).
     */
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
                // Adiciona cores
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
    
    /**
     * Retorna o tabuleiro como String formatada (para mostrar ao cliente).
     * Usado por ClientHandler e para debug.
     * 
     * @return String com o tabuleiro desenhado
     */
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
    /**
     * Conta o número de jogadas feitas (casas ocupadas).
     * Usado para polling e para saber se houve novas jogadas.
     * 
     * Usado em:
     * - play.jsp (JS) e gameAction.jsp para comparar moveCount.
     */
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
    
    //FUNÇÃO NOVA
    /**
     * Retorna o timestamp do início da jogada atual.
     * Usado para calcular o tempo restante para o jogador.
     * 
     * Usado em:
     * - ClientHandler.handleGet() e handleGetGames() para informar o frontend.
     */
    public long getCurrentMoveStartMillis() {
    	return lastMoveTime.containsKey(currentPlayer)
    		    ? lastMoveTime.get(currentPlayer)
    		    : (startTime > 0 ? startTime : System.currentTimeMillis());
    }
    
    //FUNÇÃO NOVA
    /**
     * Retorna o tempo máximo permitido para cada jogada (em segundos).
     * Valor fixo (30s), deve ser igual ao usado no frontend (play.jsp).
     */
    public int getMaxMoveTimeSeconds() {
        return 30; // igual ao play.jsp
    }
    
    public void setWinner(Player winner) {
        this.winner = winner;
        this.isGameOver = true;
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
    public boolean isEnded() { return ended;}
    public void setEnded(boolean ended) { this.ended = ended; }
    public int getChecksAfterEnd() { return checksAfterEnd; }
    public void incrementChecksAfterEnd() { this.checksAfterEnd++; }
}