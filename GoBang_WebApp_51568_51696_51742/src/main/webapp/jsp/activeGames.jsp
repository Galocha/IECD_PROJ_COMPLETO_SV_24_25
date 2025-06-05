<!-- filepath: src\main\webapp\jsp\activeGames.jsp -->
<%@ page import="session.SessionManager" %>
<%@ page import="game.Player, game.Game" %>
<%
    SessionManager sessionManager = (SessionManager) application.getAttribute("sessionManager");
	Player player = (Player) session.getAttribute("player");
	if (sessionManager == null || player == null) {
	    out.print("Sessão inválida.");
	    return;
	}
	java.util.List<game.Game> games = sessionManager.getGamesForPlayer(player.getNickname());
%>
<html>
<head><title>Jogos Ativos</title></head>
<body>
    <h2>Jogos Ativos</h2>
    <ul>
    <% for (game.Game g : games) { %>
        <li>
            <a href="play.jsp?gameId=<%= g.getId() %>">Contra <%= g.getOtherPlayer(player).getNickname() %></a>
        </li>
    <% } %>
    </ul>
    <a href="lobby.jsp">Voltar</a>
</body>
</html>