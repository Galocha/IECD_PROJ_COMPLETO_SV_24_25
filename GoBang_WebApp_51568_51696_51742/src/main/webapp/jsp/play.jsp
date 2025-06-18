<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="game.Player" %>
<%@ page import="java.io.PrintWriter, java.io.BufferedReader, java.net.Socket, java.util.*" %>
<%
	// ==========================================================
	// JSP: play.jsp
	// Função: Página principal do jogo GoBang (tabuleiro, lógica de jogada, timers, polling).
	// Técnicas: AJAX, polling, manipulação de sessão, sockets TCP, DOM dinâmico, timers JS.
	// ==========================================================
	
	// 1. Garante que o jogador está autenticado
    Player player = (Player) session.getAttribute("player");
    if (player == null) {
        response.sendRedirect("login.jsp?error=session_expired");
        return;
    }

 	// 2. Obtém a ação do pedido (entrar/sair da fila, etc)
    String action = request.getParameter("action");

 	// 3. Entrar na fila de jogo
    if ("joinQueue".equals(action)) {
    	// Cria socket para o servidor GoBang e envia comandos para entrar na fila
        try (Socket socket = new Socket("26.106.140.96", 1234);
             PrintWriter outSocket = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader inSocket = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()))) {

            outSocket.println("/login " + player.getNickname() + " " + player.getPassword());
            String loginResp = inSocket.readLine();
            if (loginResp == null || !loginResp.toLowerCase().contains("bem-sucedido")) {
                response.sendRedirect("login.jsp?error=socket");
                return;
            }
            outSocket.println("/play");
        } catch (Exception e) {
            response.sendRedirect("login.jsp?error=socket");
            return;
        }
        response.sendRedirect("play.jsp");
        return;
    }

 	// 4. Sair da fila de jogo
    if ("leaveQueue".equals(action)) {
    	// Cria socket para o servidor GoBang e envia comando para sair da fila
        try (Socket socket = new Socket("26.106.140.96", 1234);
             PrintWriter outSocket = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader inSocket = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()))) {

            outSocket.println("/login " + player.getNickname() + " " + player.getPassword());
            String loginResp = inSocket.readLine();
            if (loginResp == null || !loginResp.toLowerCase().contains("bem-sucedido")) {
                response.sendRedirect("login.jsp?error=socket");
                return;
            }
            outSocket.println("/disconnect");
        } catch (Exception e) {
            response.sendRedirect("login.jsp?error=socket");
            return;
        }
        response.sendRedirect("lobby.jsp");
        return;
    }

    // 5. Pedir estado do jogo ao servidor GoBang
    String resposta = null;
    try (Socket socket = new Socket("26.106.140.96", 1234);
         PrintWriter outSocket = new PrintWriter(socket.getOutputStream(), true);
         BufferedReader inSocket = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()))) {

        outSocket.println("/login " + player.getNickname() + " " + player.getPassword());
        String loginResp = inSocket.readLine();
        if (loginResp == null || !loginResp.toLowerCase().contains("bem-sucedido")) {
            response.sendRedirect("login.jsp?error=socket");
            return;
        }
        outSocket.println("/get " + player.getNickname());
        resposta = inSocket.readLine();
    } catch (Exception e) {
        response.sendRedirect("login.jsp?error=socket");
        return;
    }

 	// 6. Parsing da resposta do servidor para obter o estado do jogo
    boolean isPlaying = resposta != null && resposta.contains("JOGO INICIADO");
    String boardStr = "";
    String opponent = "";
    char[][] board = new char[15][15];
    String playerSymbol = "";
    boolean isCurrentPlayer = false;

    if (isPlaying && resposta != null) {
        String player1 = "";
        String player2 = "";

        String[] parts = resposta.split(";");
        for (String part : parts) {
            if (part.startsWith("OPONENTE:")) {
                opponent = part.substring("OPONENTE:".length());
            }
            if (part.startsWith("PLAYER1:")) {
                player1 = part.substring("PLAYER1:".length());
            }
            if (part.startsWith("PLAYER2:")) {
                player2 = part.substring("PLAYER2:".length());
            }
            if (part.startsWith("TABULEIRO:")) {
                String[] linhas = part.substring("TABULEIRO:".length()).split("\\|");
                for (int i = 0; i < linhas.length && i < 15; i++) {
                    for (int j = 0; j < linhas[i].length() && j < 15; j++) {
                        board[i][j] = linhas[i].charAt(j);
                        boardStr += linhas[i].charAt(j);
                    }
                }
            }
        }
        if (player.getNickname().equals(player1)) {
            playerSymbol = "X";
        } else if (player.getNickname().equals(player2)) {
            playerSymbol = "O";
        }
        isCurrentPlayer = resposta.contains("SUA_VEZ");
    }
    
 	// 7. Gestão do timer para cada jogada
    int tempoMaximo = 30;
    long jogadaInicio = 0;
    int tempoRestante = 0;
    
    if (isPlaying && resposta != null) {
        String[] parts = resposta.split(";");
        for (String part : parts) {
            if (part.startsWith("OPONENTE:")) {
                opponent = part.substring("OPONENTE:".length());
            }
            if (part.equals("SUA_VEZ")) isCurrentPlayer = true;
            if (part.startsWith("JOGADA_INICIO:")) jogadaInicio = Long.parseLong(part.substring("JOGADA_INICIO:".length()));
            if (part.startsWith("TEMPO_MAXIMO:")) tempoMaximo = Integer.parseInt(part.substring("TEMPO_MAXIMO:".length()));
        }
        if (isCurrentPlayer && jogadaInicio > 0) {
            tempoRestante = tempoMaximo - (int)((System.currentTimeMillis() - jogadaInicio) / 1000);
            if (tempoRestante < 0) tempoRestante = 0;
            if (tempoRestante > tempoMaximo) tempoRestante = tempoMaximo;
        }
    }
    
    String opponentPhoto = "assets/img/default.png";

    if (opponent != null && !opponent.isEmpty()) {
        @SuppressWarnings("unchecked")
        Map<String, Player> players = (Map<String, Player>) application.getAttribute("players");
        if (players != null && players.containsKey(opponent)) {
            Player opponentPlayer = players.get(opponent);
            if (opponentPlayer.getPhoto() != null && !opponentPlayer.getPhoto().isEmpty()) {
                opponentPhoto = opponentPlayer.getPhoto();
            }
        }
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
    <!--
        Elemento escondido para partilhar o estado do jogo (tempo e vez) com o JavaScript.
        Usado para polling e timers no frontend.
    -->
    <span id="estado-jogo" style="display:none"
          data-tempo="<%= tempoRestante %>"
          data-sua-vez="<%= isCurrentPlayer %>"></span>
    <div id="game-container">
        <% if (isPlaying) { %>
        	<!-- =======================
                 ESTADO DE JOGO ATIVO
                 ======================= -->
            <h1>GoBang Game</h1>
            <div id="status">
                <h3>Jogando contra:</h3>
			    <div style="display: flex; align-items: center; gap: 16px; margin-bottom: 10px;">
			        <img src="<%= opponentPhoto %>" alt="Foto do oponente"
			             style="width:100px;height:100px;border-radius:50%;object-fit:cover;box-shadow:0 2px 8px #0002;">
			        <span style="font-size:1.5em;"><%= opponent %></span>
			    </div>
                <p>Seu símbolo: <span class="<%= playerSymbol.equals("X") ? "x" : "o" %>">
                    <%= playerSymbol %>
                </span></p>
                <p id="turn-message">
                    <%= isCurrentPlayer ?
                        "<span style='color:green'>✓ Sua vez!</span>" :
                        "<span style='color:blue'>⏳ Aguardando oponente...</span>" %>
                </p>
            </div>
            <div id="timer" <%= isCurrentPlayer ? "" : "style='display:none;'" %>>
                Tempo restante: <span id="timeLeft"><%= tempoRestante %></span>s
            </div>
            <div id="board">
                <%
             	// Renderiza o tabuleiro 15x15
                for (int i = 0; i < 15; i++) {
                    for (int j = 0; j < 15; j++) {
                        String cellClass = "cell";
                        String cellContent = "";
                        if (board[i][j] == 'X') {
                            cellClass += " x";
                            cellContent = "X";
                        } else if (board[i][j] == 'O') {
                            cellClass += " o";
                            cellContent = "O";
                        }
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
                <button id="surrenderBtn" onclick="surrender()">✖ Desistir</button>
                <a id="lobbyBtn" href="lobby.jsp" class="button">← Voltar ao Lobby</a>
            </div>
            <script>
                let boardString = "<%= boardStr %>";
                
                // Função: getMoveCount
                // Conta o número de jogadas feitas (usado para polling eficiente)
                function getMoveCount() {
                    return (boardString.match(/[XO]/g) || []).length;
                }
                
                let gameEnded = false;

             	// Função: makeMove(cell)
                // Envia jogada para o backend via AJAX (gameAction.jsp?action=move)
                // - Só permite jogar se for a tua vez e a célula estiver vazia
                // - Atualiza o DOM imediatamente
                // - Se o backend devolver erro, reverte a jogada
                // - Se o backend devolver "refresh", recarrega o estado do jogo
                // - Se o backend devolver "game_over", termina o jogo e redireciona
                function makeMove(cell) {
                    if (gameEnded) return; // Não permite jogar após fim de jogo
                    if (typeof moveDone !== "undefined" && moveDone) return;
                    if (cell.classList.contains('disabled')) return;
                    moveDone = true;

                    const row = cell.getAttribute('data-row');
                    const col = cell.getAttribute('data-col');
                    const symbol = "<%= playerSymbol %>";
                    const symbolClass = "<%= playerSymbol.toLowerCase() %>";

                    cell.textContent = symbol;
                    cell.classList.add(symbolClass, 'disabled');

                    fetch('gameAction.jsp?action=move&row=' + row + '&col=' + col)
                        .then(response => response.text())
                        .then(data => {
                            const cleanedData = data.trim();
                            if (cleanedData.startsWith('refresh')) {
                                setTimeout(() => location.reload(), 500);
                            } else if (cleanedData.startsWith('error')) {
                                // Só mostra alerta se o jogo ainda não acabou
                                if (!gameEnded) {
                                    cell.textContent = '';
                                    cell.classList.remove('x', 'o', 'disabled');
                                    alert(cleanedData.split(':')[1] || 'Movimento inválido');
                                }
                                moveDone = false;
                            } else if (cleanedData.startsWith('game_over')) {
                                gameEnded = true;
                                alert(cleanedData.replace(/^game_over:/, '').trim());
                                window.location.href = 'lobby.jsp?fromGame=1';
                            }
                        })
                        .catch(error => {
                            if (!gameEnded) {
                                cell.textContent = '';
                                cell.classList.remove('x', 'o', 'disabled');
                                alert('Erro ao processar movimento');
                            }
                            moveDone = false;
                        });
                }
                window.makeMove = makeMove;

             	// Função: surrender()
                // Permite ao jogador desistir do jogo (AJAX para gameAction.jsp?action=surrender)
                function surrender() {
                    if (confirm('Tem certeza que deseja desistir?')) {
                        fetch('gameAction.jsp?action=surrender')
                            .then(() => window.location.href = 'lobby.jsp?fromGame=1');
                    }
                }
                window.surrender = surrender;
            </script>
        <% } else { %>
        	<!-- =======================
                 ESTADO DE ESPERA NA FILA
                 ======================= -->
            <div class="waiting-screen">
                <h2>Aguardando Oponente</h2>
                <div class="spinner"></div>
                <p>Esta página será atualizada automaticamente...</p>
                <a href="play.jsp?action=leaveQueue">Cancelar e voltar ao lobby</a>
            </div>
            <script>
         		// Polling: recarrega a página a cada 1 segundos enquanto espera por adversário
                setTimeout(() => location.reload(), 2000);
            </script>
        <% } %>
    </div>
    <script>
	    let timerInterval = null;
	    
	 	// Função: startTimer(tempoRestante)
        // Inicia e atualiza o timer de jogada no frontend.
        // Quando chega a 0, bloqueia o tabuleiro e faz AJAX para gameAction.jsp?action=timeout
	    function startTimer(tempoRestante) {
	        clearInterval(timerInterval);
	        if (tempoRestante > 0) {
	            let timeLeft = tempoRestante;
	            let timeLeftSpan = document.getElementById('timeLeft');
	            if (timeLeftSpan) timeLeftSpan.textContent = timeLeft;
	            timerInterval = setInterval(function() {
	                timeLeft--;
	                let timeLeftSpan = document.getElementById('timeLeft');
	                if (timeLeftSpan) timeLeftSpan.textContent = timeLeft;
	                if (timeLeft <= 0) {
	                    clearInterval(timerInterval);
	                    document.querySelectorAll('.cell').forEach(cell => cell.classList.add('disabled'));
	                    fetch('gameAction.jsp?action=timeout')
		                    .then(() => {
		                        setTimeout(() => location.reload(), 300); // reload rápido para mostrar "Aguardando oponente"
		                    });
	                }
	            }, 1000);
	        }
	    }
	
	 	// Ao carregar a página, inicia o timer se for a tua vez
	    document.addEventListener('DOMContentLoaded', function() {
	        const estado = document.getElementById('estado-jogo');
	        if (estado) {
	            let tempo = parseInt(estado.dataset.tempo, 10);
	            let suaVez = estado.dataset.suaVez === "true";
	            if (suaVez && tempo > 0) {
	                startTimer(tempo);
	            }
	        }
	    });
	 	
	 	// POLLING: verifica o estado do jogo a cada 1 segundo
        // - Se houver "game_over", termina o jogo e redireciona
        // - Se houver "refresh", atualiza só o game-container e reinicia o timer se necessário
	    let pollingInterval = setInterval(function() {
	        fetch('gameAction.jsp?action=check&moveCount=' + getMoveCount())
	            .then(response => response.text())
	            .then(data => {
	                const cleanedData = data.trim();
	                if (cleanedData.startsWith('game_over')) {
	                    clearInterval(pollingInterval);
	                    gameEnded = true;
	                    alert(cleanedData.replace(/^game_over:/, '').trim());
	                    window.location.href = 'lobby.jsp?fromGame=1';
	                } else if (cleanedData === 'refresh') {
	                    // Atualiza só o game-container e reinicia o timer se necessário
	                    fetch('play.jsp')
		                    .then(response => response.text())
		                    .then(html => {
		                        const parser = new DOMParser();
		                        const doc = parser.parseFromString(html, 'text/html');
		                        const novoContainer = doc.getElementById('game-container');
		                        const estado = doc.getElementById('estado-jogo');
		                        if (novoContainer && estado) {
		                            document.getElementById('game-container').innerHTML = novoContainer.innerHTML;
		                            // Reinicia o timer se for a tua vez
		                            let novoTempo = parseInt(estado.dataset.tempo, 10);
		                            let novaSuaVez = estado.dataset.suaVez === "true";
		                            if (novaSuaVez && novoTempo > 0) {
		                                startTimer(novoTempo);
		                            }
		                        }
		                    });
		            }
	            });
	    }, 1000);
    </script>
</body>
</html>
<!--
Explicação:
1. O JSP começa por garantir que o jogador está autenticado (session.getAttribute("player")).
2. Se o parâmetro "action" for "joinQueue", faz login no servidor GoBang e envia "/play" para entrar na fila.
   - Redireciona para play.jsp para mostrar o estado de espera.
3. Se "action" for "leaveQueue", faz login e envia "/disconnect" para sair da fila.
   - Redireciona para lobby.jsp.
4. Caso contrário, faz login e envia "/get nickname" para obter o estado do jogo.
   - Se o jogo estiver ativo, faz parsing da resposta para obter o tabuleiro, adversário, símbolo do jogador, etc.
   - Calcula o tempo restante para a jogada.
5. O HTML mostra:
   - Se estiver a jogar: tabuleiro, estado, timer, botões de desistência e voltar ao lobby.
   - Se estiver à espera: spinner e mensagem de espera.
6. O JavaScript implementa:
   - makeMove(cell): envia jogada via AJAX, atualiza o DOM, trata erros e fim de jogo.
   - surrender(): permite desistir via AJAX.
   - startTimer(tempoRestante): inicia e atualiza o timer da jogada, bloqueia tabuleiro e faz timeout via AJAX.
   - Polling (setInterval): a cada segundo, faz AJAX para gameAction.jsp?action=check&moveCount=... para atualizar o estado do jogo.
     - Se houver "game_over", termina o jogo e redireciona.
     - Se houver "refresh", atualiza só o game-container e reinicia o timer.
7. O elemento <span id="estado-jogo"> serve para partilhar o estado do timer e vez entre backend e JS.
8. O JSP depende de:
   - Sessão válida (player autenticado).
   - Servidor GoBang ativo em localhost:1234.
   - gameAction.jsp para AJAX de jogadas, polling, timeout e desistência.
   - CSS inline para layout e experiência visual.
   - O backend GoBang deve suportar os comandos /login, /play, /disconnect, /get, etc.
9. Técnicas cruciais:
   - AJAX (fetch): comunicação assíncrona para jogadas, polling, timeout, surrender.
   - Polling: atualização do estado do jogo em tempo real.
   - Manipulação dinâmica do DOM: atualização do tabuleiro, timer, estado do jogo.
   - Timer JS: controlo do tempo de jogada.
   - Segurança: validação de sessão e tratamento de erros de socket.
10. Utilizações:
    - Chamado diretamente pelo utilizador ao entrar num jogo ou ao aceitar convite.
    - Comunica com gameAction.jsp para todas as ações AJAX.
    - Redireciona para lobby.jsp após fim de jogo ou desistência.
    - Pode ser recarregado automaticamente enquanto espera por adversário.
-->