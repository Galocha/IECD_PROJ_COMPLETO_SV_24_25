<%@ page import="java.util.*,game.Player" %>
<%
    @SuppressWarnings("unchecked")
    Map<String, Player> players = (Map<String, Player>) application.getAttribute("players");
    Player player = (Player) session.getAttribute("player");
    
    if (player == null) {
        response.sendRedirect("login.jsp");
        return;
    }
    
 	// Lê o ranking do ficheiro XML de backup
    List<Map<String, String>> ranking = server.GoBangServer.loadRankingFromXML();
%>
<html>
<head>
    <title>Quadro de Honra</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #f0f2f5;
            margin: 0;
            padding: 20px;
            color: #333;
        }
        #ranking-container {
            max-width: 700px;
            margin: 30px auto;
            padding: 24px;
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
            margin-bottom: 20px;
            background: #fafbfc;
        }
        th, td {
            padding: 10px 8px;
            text-align: center;
        }
        th {
            background: #e3eafc;
            color: #1976d2;
            font-weight: bold;
            border-bottom: 2px solid #b6c7e6;
        }
        tr:nth-child(even) {
            background: #f6f8fa;
        }
        tr:hover {
            background: #e3eafc;
        }
        img {
            border-radius: 4px;
            background: #fff;
            border: 1px solid #ddd;
        }
        a {
            color: #1976d2;
            text-decoration: none;
            font-weight: bold;
        }
        a:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body<%
    String cor = "#f0f2f5";
    if (player != null && player.getPreferredColor() != null && !player.getPreferredColor().isEmpty()) {
        cor = player.getPreferredColor();
    }
	%> style="background-color:<%= cor %>;">
    <div id="ranking-container">
        <h2>Quadro de Honra (Backup)</h2>
        <!-- Botão para criar backup manualmente -->
        <form method="post" style="margin-bottom:18px;">
            <button type="submit" name="backup" value="1">Criar Backup do Ranking</button>
        </form>
        <%
            if ("1".equals(request.getParameter("backup"))) {
                server.GoBangServer.saveRankingToXML();
                out.print("<p style='color:green;'>Backup criado!</p>");
            }
        %>
        <table border="1">
            <tr>
                <th>Posição</th>
                <th>Nickname</th>
                <th>Foto</th>
                <th>Bandeira</th>
                <th>Vitórias</th>
                <th>Tempo Médio</th>
            </tr>
            <% for (Map<String, String> p : ranking) { 
                String cc = p.get("Nationality");
                String flagCode = (cc != null && cc.length() == 2) ? cc.toLowerCase() : "un";
                String nick = p.get("Nickname");
                String photo = "default.png";
                if (players != null && players.get(nick) != null && players.get(nick).getPhoto() != null && !players.get(nick).getPhoto().isEmpty()) {
                    photo = players.get(nick).getPhoto();
                }
            %>
            <tr>
                <td><%= p.get("Position") %></td>
                <td><%= p.get("Nickname") %></td>
                <td>
                    <img src="<%= photo %>" alt="Foto" style="border-radius:50%;width:40px;height:40px;object-fit:cover;border:1px solid #aaa;background:#fff;">
                </td>
                <td>
                    <img src="https://flagcdn.com/128x96/<%= flagCode %>.png"
                         alt="Bandeira"
                         style="border-radius:4px; width:40px; object-fit:cover; align-self:center;">
                </td>
                <td><%= p.get("Wins") %></td>
                <td><%= p.get("AverageTime") %> s</td>
            </tr>
            <% } %>
        </table>
        <a href="lobby.jsp">Voltar</a>
    </div>
    <script>
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