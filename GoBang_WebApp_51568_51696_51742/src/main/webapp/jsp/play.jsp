<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="game.Player, session.SessionManager, game.Game" %>
<%
    Player player = (Player) session.getAttribute("player");
    if (player == null) {
        response.sendRedirect("login.jsp?error=session_expired");
        return;
    }

    SessionManager sessionManager = (SessionManager) application.getAttribute("sessionManager");
    if (sessionManager == null) {
        response.sendRedirect("lobby.jsp?error=server_error");
        return;
    }

    // Lógica de entrada e saída da fila
    String action = request.getParameter("action");
    if ("leaveQueue".equals(action)) {
        sessionManager.removePlayerFromQueue(player);
        response.sendRedirect("lobby.jsp");
        return;
    }

    String gameId = request.getParameter("gameId");
    Game currentGame = null;
    boolean isPlaying = false;
    boolean isCurrentPlayer = false;
    boolean wasInQueue = false;

    if (gameId != null) {
        currentGame = sessionManager.getGameById(player.getNickname(), gameId);
        isPlaying = currentGame != null;
        isCurrentPlayer = isPlaying && currentGame.getCurrentPlayer().equals(player);
    } else {
        // Compatibilidade com fluxo antigo: apanha o 1º jogo ativo, se existir
        java.util.List<Game> games = sessionManager.getGamesForPlayer(player.getNickname());
        if (!games.isEmpty()) {
            currentGame = games.get(0);
            isPlaying = true;
            isCurrentPlayer = currentGame.getCurrentPlayer().equals(player);
        }
    }
    wasInQueue = sessionManager.getWaitingPlayers().contains(player);

    // Corrigido: só adiciona o jogador se não estiver a jogar nem já estiver na fila
    if (!isPlaying && !wasInQueue) {
        sessionManager.addPlayerToQueue(player);
        response.sendRedirect("play.jsp");
        return;
    }

    // Serializa o tabuleiro como string para JS
    StringBuilder boardStr = new StringBuilder();
	if (isPlaying && currentGame != null) {
	    char[][] board = currentGame.getBoard();
	    for (int i = 0; i < board.length; i++)
	        for (int j = 0; j < board[i].length; j++)
	            boardStr.append(board[i][j]);
	}
%>
<!DOCTYPE html>
<html>
<head>
    <title>GoBang Game</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: <%= player.getPreferredColor() != null ? player.getPreferredColor() : "#f0f2f5" %>;
            margin: 0;
            padding: 20px;
            color: #333;
        }
        #game-container {
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
            background-color: white;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        #status {
            padding: 15px;
            background-color: #f8f9fa;
            border-radius: 5px;
            margin-bottom: 20px;
        }
        #board {
            display: grid;
            grid-template-columns: repeat(15, 32px);
            grid-template-rows: repeat(15, 32px);
            gap: 1px;
            margin: 20px auto;
            border: 2px solid #ddd;
            background-color: #fff;
            padding: 10px;
            border-radius: 5px;
        }
        .cell {
            width: 32px;
            height: 32px;
            border: 1px solid #ccc;
            display: flex;
            justify-content: center;
            align-items: center;
            cursor: pointer;
            background-color: #f8f9fa;
            transition: all 0.2s;
        }
        .cell:hover {
            background-color: #e9ecef;
        }
        .cell.x {
            color: #dc3545;
            font-weight: bold;
        }
        .cell.o {
            color: #007bff;
            font-weight: bold;
        }
        .cell.disabled {
            pointer-events: none;
            opacity: 0.7;
        }
        #timer {
            font-size: 1.2rem;
            color: #dc3545;
            margin: 10px 0;
            font-weight: bold;
        }
        #controls {
            margin-top: 20px;
            display: flex;
            gap: 10px;
        }
        button {
            padding: 8px 16px;
            background-color: #dc3545;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            transition: background-color 0.2s;
        }
        button:hover {
            background-color: #c82333;
        }
        .waiting-screen {
            text-align: center;
            padding: 40px;
        }
        .spinner {
            border: 4px solid rgba(0, 0, 0, 0.1);
            border-radius: 50%;
            border-top: 4px solid #007bff;
            width: 40px;
            height: 40px;
            animation: spin 1s linear infinite;
            margin: 20px auto;
        }
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
    </style>
</head>
<body>
    <div id="game-container">
        <% if (isPlaying) { %>
            <h1>GoBang Game</h1>
            
            <div id="status">
                <h3>Jogando contra: <%= currentGame.getOtherPlayer(player).getNickname() %></h3>
                <p>Seu símbolo: <span class="<%= currentGame.getPlayer1().equals(player) ? "x" : "o" %>">
                    <%= currentGame.getPlayer1().equals(player) ? "X" : "O" %>
                </span></p>
                <p id="turn-message">
                    <%= isCurrentPlayer ? 
                        "<span style='color:green'>✓ Sua vez!</span>" : 
                        "<span style='color:blue'>⏳ Aguardando oponente...</span>" %>
                </p>
            </div>
            <div id="timer" <%= isCurrentPlayer ? "" : "style='display:none;'" %>>Tempo restante: <span id="timeLeft">30</span>s</div>
            <div id="board">
                <% 
                char[][] board = currentGame.getBoard(); 
                for (int i = 0; i < board.length; i++) {
                    for (int j = 0; j < board[i].length; j++) {
                        String cellClass = "cell";
                        String cellContent = "";
                        if (board[i][j] == 'X') {
                            cellClass += " x";
                            cellContent = "X";
                        } else if (board[i][j] == 'O') {
                            cellClass += " o";
                            cellContent = "O";
                        }
                        // Só permite clique se for a vez do jogador e célula vazia
                        if (!isCurrentPlayer || board[i][j] != '.') {
                            cellClass += " disabled";
                        }
                %>
                <div class="<%= cellClass %>" 
                     data-row="<%= i %>" 
                     data-col="<%= j %>"
                     onclick="makeMove(this)">
                    <%= cellContent %>
                </div>
                <% } } %>
            </div>
            
            <div id="controls">
                <button onclick="surrender()">✖ Desistir</button>
                <a href="play.jsp?action=leaveQueue" class="button">← Voltar ao Lobby</a>
            </div>
            
            <script>
            	//setTimeout(() => location.reload(), 2000);
            	
                // Tabuleiro serializado para JS (acessível em todos os blocos)
                let boardString = "<%= boardStr.toString() %>";
                
                function getMoveCount() {
                    return (boardString.match(/[XO]/g) || []).length;
                }

                setInterval(() => {
                    const moveCount = getMoveCount();
                    fetch('gameAction.jsp?action=check&moveCount=' + moveCount)
                        .then(response => response.text())
                        .then(data => {
                            const cleanedData = data.trim();
                            console.log("Resposta do check:", cleanedData);
                            if (cleanedData === 'refresh') {
                                location.reload();
                            }
                        });
                }, 1000);

                let lastMoveCount = getMoveCount();

                <% if (isCurrentPlayer) { %>
                let timeLeft = 30;
                let timer = setInterval(updateTimer, 1000);
                let moveDone = false;

                function updateTimer() {
                    if (moveDone) return;
                    timeLeft--;
                    document.getElementById('timeLeft').textContent = timeLeft;
                    if (timeLeft <= 0) {
                        clearInterval(timer);
                        alert("Tempo esgotado! O seu turno acabou.");
                        document.querySelectorAll('.cell').forEach(cell => cell.classList.add('disabled'));
                        fetch('gameAction.jsp?action=timeout')
                            .then(() => location.reload()); // Reload só quando tempo acabar
                    }
                }

                // Função global para onclick
                function makeMove(cell) {
                    if (moveDone || cell.classList.contains('disabled')) return;
                    moveDone = true;
                    
                    const row = cell.getAttribute('data-row');
                    const col = cell.getAttribute('data-col');
                    const symbol = "<%= currentGame.getPlayer1().equals(player) ? 'X' : 'O' %>";
                    const symbolClass = "<%= currentGame.getPlayer1().equals(player) ? 'x' : 'o' %>";

                    // Feedback visual imediato
                    cell.textContent = symbol;
                    cell.classList.add(symbolClass, 'disabled');
                    
                    fetch('gameAction.jsp?action=move&row=' + row + '&col=' + col)
                        .then(response => response.text())
                        .then(data => {
                        	const cleanedData = data.trim();
                            console.log('Resposta limpa do move:', cleanedData);
                            if (cleanedData.startsWith('refresh')) {
                            	console.log('Faz reload da página após jogada');
                                setTimeout(() => location.reload(), 500);
                            } else if (cleanedData.startsWith('error')) {
                                cell.textContent = '';
                                cell.classList.remove('x', 'o', 'disabled');
                                alert(cleanedData.split(':')[1] || 'Movimento inválido');
                                moveDone = false;
                            } else if (cleanedData.startsWith('game_over')) {
                                alert(cleanedData.split(':')[1]);
                                location.href = 'lobby.jsp';
                            }
                        })
                        .catch(error => {
                            console.error('Erro ao fazer fetch move:', error);
                            cell.textContent = '';
                            cell.classList.remove('x', 'o', 'disabled');
                            alert('Erro ao processar movimento');
                            moveDone = false;
                        });
                }

                // Torna a função global para o onclick
                window.makeMove = makeMove;

                function surrender() {
                    if (confirm('Tem certeza que deseja desistir?')) {
                        fetch('gameAction.jsp?action=surrender')
                            .then(() => location.href = 'lobby.jsp');
                    }
                }
                window.surrender = surrender;
                <% } else { %>
                // Não é a vez do jogador
                function makeMove(cell) { /* nada */ }
                window.makeMove = makeMove;
                function surrender() {
                    fetch('gameAction.jsp?action=surrender')
                        .then(() => location.href = 'lobby.jsp');
                }
                window.surrender = surrender;
                <% } %>
            </script>
        <% } else { %>
            <div class="waiting-screen">
                <h2>Aguardando Oponente</h2>
                <div class="spinner"></div>
                <p>Jogadores na fila: <%= sessionManager.getWaitingPlayers().size() %></p>
                <p>Esta página será atualizada automaticamente...</p>
                <a href="play.jsp?action=leaveQueue">Cancelar e voltar ao lobby</a>
            </div>
            <script>
                setTimeout(() => location.reload(), 2000);
            </script>
        <% } %>
    </div>
</body>
</html>