<%@ page import="game.Player" %>
<%@ page import="java.io.PrintWriter, java.io.BufferedReader, java.net.Socket" %>
<%@ page import="java.util.*,java.io.InputStreamReader" %>
<%
	// JSP: activeGames.jsp
	// Mostra ao utilizador os seus jogos ativos, o adversário, o tempo restante e permite entrar no jogo.
	// Técnicas utilizadas:
	// - AJAX (fetch + setInterval) para polling e atualização dinâmica da tabela de jogos ativos.
	// - Comunicação com o servidor GoBang via socket TCP (envio de comandos como um cliente).
	// - Manipulação de sessão (session scope) para garantir autenticação.
	// - Suporte a modo "partial" para AJAX (só devolve a tabela, não o HTML completo).
	// - Integração com play.jsp (entrar no jogo) e lobby.jsp (voltar ao lobby).
	
	// 1. Garante que o utilizador está autenticado (tem sessão válida)
    Player player = (Player) session.getAttribute("player");
    if (player == null) {
        out.print("Sessão inválida.");
        return;
    }

 	// 2. Verifica se é pedido AJAX (partial=1) para devolver só a tabela
    boolean partial = request.getParameter("partial") != null;

 	// 3. Prepara lista de jogos ativos e variáveis para polling de tempo/vez
    List<Map<String, Object>> jogos = new ArrayList<>();
    int tempoRestanteGlobal = 0;
    boolean suaVezGlobal = false;

 	// 4. Comunica com o servidor GoBang via socket TCP para obter os jogos ativos do utilizador
    try (Socket socket = new Socket("26.106.140.96", 1234);
         PrintWriter outSocket = new PrintWriter(socket.getOutputStream(), true);
         BufferedReader inSocket = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

    	// 4.1. Faz login automático do utilizador no servidor GoBang
        outSocket.println("/login " + player.getNickname() + " " + player.getPassword());
        String loginResp = inSocket.readLine();
        if (loginResp == null || !loginResp.toLowerCase().contains("bem-sucedido")) {
            out.print("Erro ao ligar ao servidor GoBang.");
            return;
        }

     	// 4.2. Pede ao servidor a lista de jogos ativos deste jogador
        outSocket.println("/getgames " + player.getNickname());
        String resposta = inSocket.readLine();
        if (resposta != null && !resposta.contains("NO_GAMES")) {
        	// 4.3. Faz parsing da resposta do servidor (formato: GAMEID:...;OPONENTE:...;SUA_VEZ:...;TEMPO:...;)
            String[] parts = resposta.split(";");
            Map<String, Object> jogo = null;
            for (String part : parts) {
                if (part.startsWith("GAMEID:")) {
                    if (jogo != null) jogos.add(jogo);
                    jogo = new HashMap<>();
                    jogo.put("gameId", part.substring("GAMEID:".length()));
                } else if (part.startsWith("OPONENTE:")) {
                    if (jogo != null) jogo.put("oponente", part.substring("OPONENTE:".length()));
                } else if (part.startsWith("SUA_VEZ:")) {
                    boolean suaVez = "true".equals(part.substring("SUA_VEZ:".length()));
                    if (jogo != null) jogo.put("suaVez", suaVez);
                    // Para o polling JS, guarda o primeiro jogo em que é a sua vez
                    if (suaVez && tempoRestanteGlobal == 0 && jogo != null && jogo.get("tempo") != null) {
                        suaVezGlobal = true;
                        tempoRestanteGlobal = (Integer) jogo.get("tempo");
                    }
                } else if (part.startsWith("TEMPO:")) {
                    if (jogo != null) jogo.put("tempo", Integer.parseInt(part.substring("TEMPO:".length())));
                }
            }
            if (jogo != null) jogos.add(jogo);
        }
     	// 4.4. Se não encontrou nenhum jogo em que é a sua vez, usa o primeiro jogo para o polling
        if (!jogos.isEmpty() && tempoRestanteGlobal == 0) {
            Map<String, Object> primeiro = jogos.get(0);
            suaVezGlobal = Boolean.TRUE.equals(primeiro.get("suaVez"));
            tempoRestanteGlobal = primeiro.get("tempo") != null ? (Integer)primeiro.get("tempo") : 0;
        }
    } catch (Exception e) {
        out.print("Erro ao comunicar com o servidor GoBang.");
        return;
    }
%>
<% if (!partial) { %>
<!DOCTYPE html>
<html>
<head>
    <title>Jogos Ativos</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: <%= player.getPreferredColor() != null ? player.getPreferredColor() : "#f0f2f5" %>;
            margin: 0;
            padding: 20px;
            color: #333;
        }
        #active-games-container {
            width: 700px;
            margin: 40px auto;
            padding: 28px;
            background-color: #fff;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.08);
        }
        h2 {
            margin-top: 0;
            color: #1976d2;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 18px;
        }
        th, td {
            padding: 12px 10px;
            border-bottom: 1px solid #eee;
            text-align: left;
        }
        th {
            background: #f5f7fa;
            color: #1976d2;
            font-weight: 600;
        }
        tr:last-child td {
            border-bottom: none;
        }
        a {
            color: #1976d2;
            text-decoration: none;
            font-weight: bold;
            border-radius: 4px;
            padding: 6px 14px;
            background: #e3eafc;
            transition: background 0.2s;
        }
        a:hover {
            background: #1976d2;
            color: #fff;
            text-decoration: none;
        }
        .voltar-link {
            display: inline-block;
            margin-top: 18px;
            background: #f5f7fa;
            color: #1976d2;
            padding: 7px 18px;
            border-radius: 6px;
            font-weight: 500;
            box-shadow: 0 1px 4px rgba(25, 118, 210, 0.06);
        }
        .voltar-link:hover {
            background: #1976d2;
            color: #fff;
        }
        .timer {
            color: #d32f2f;
            font-weight: bold;
        }
    </style>
</head>
<body>
	<!--
        Elemento escondido para partilhar o estado do jogo (tempo e vez) com o JavaScript.
        Usado para polling e timers no frontend.
    -->
    <span id="estado-jogo" style="display:none"
          data-tempo="<%= tempoRestanteGlobal %>"
          data-sua-vez="<%= suaVezGlobal %>"></span>
    <div id="active-games-container">
        <h2>Jogos Ativos</h2>
<% } %>
        <div id="active-games-table-wrapper">
        <table id="active-games-table">
            <tr>
                <th>Adversário</th>
                <th>Tempo para jogar</th>
                <th>Entrar</th>
            </tr>
            <% if (!jogos.isEmpty()) {
                for (Map<String, Object> jogo : jogos) { %>
            <tr>
                <td><%= jogo.get("oponente") %></td>
                <td>
                    <% if ((Boolean.TRUE.equals(jogo.get("suaVez"))) && ((Integer)jogo.get("tempo")) > 0) { %>
                        <span class="timer"><%= jogo.get("tempo") %></span> segundos para acabares a tua jogada
                    <% } else if (Boolean.TRUE.equals(jogo.get("suaVez"))) { %>
                        <span class="timer">Tempo esgotado!</span>
                    <% } else { %>
                        A aguardar jogada do adversário
                    <% } %>
                </td>
                <td>
                    <a href="play.jsp?gameId=<%= jogo.get("gameId") %>">Entrar</a>
                </td>
            </tr>
            <% }
            } else { %>
            <tr>
                <td colspan="3">Não tens jogos ativos.</td>
            </tr>
            <% } %>
        </table>
        </div>
<% if (!partial) { %>
        <a href="lobby.jsp" class="voltar-link">Voltar</a>
    </div>
    <script>
    // ===========================================
    // Técnica: AJAX + POLLING
    // O JavaScript faz polling a cada 1 segundo para atualizar a tabela de jogos ativos.
    // Chama o próprio JSP com ?partial=1 para obter só a tabela e o estado do jogo.
    // Atualiza a tabela e o elemento #estado-jogo sem recarregar a página.
    // ===========================================
    function atualizarTabelaJogos() {
        fetch('activeGames.jsp?partial=1')
            .then(response => response.text())
            .then(html => {
                const parser = new DOMParser();
                const doc = parser.parseFromString(html, 'text/html');
                // Atualiza a tabela
                const novaTabela = doc.getElementById('active-games-table');
                const tabelaAtual = document.getElementById('active-games-table');
                if (novaTabela && tabelaAtual) {
                    tabelaAtual.innerHTML = novaTabela.innerHTML;
                }
                // Atualiza o estado-jogo escondido para polling de vez/tempo
                const novoEstado = doc.getElementById('estado-jogo');
                const estadoAtual = document.getElementById('estado-jogo');
                if (novoEstado && estadoAtual) {
                    estadoAtual.dataset.tempo = novoEstado.dataset.tempo;
                    estadoAtual.dataset.suaVez = novoEstado.dataset.suaVez;
                }
            });
    }
 	// Faz polling a cada meio segundo
    setInterval(atualizarTabelaJogos, 500);
 	
    //Polling: envia para o jogador, caso esteja em jogo mas não esteja em play.jsp, uma notificação de fim de jogo
    function verificarNotificacoesGlobais() {
        fetch('gameAction.jsp?action=checkGlobal')
            .then(r => r.text())
            .then(msg => {
                msg = msg.trim();
                if (msg.startsWith("game_over:")) {
                    alert(msg.replace(/^game_over:/, '').trim());
                    window.location.href = "lobby.jsp?fromGame=1";
                }
            });
    }
    setInterval(verificarNotificacoesGlobais, 1000);
    </script>
</body>
</html>
<% } %>