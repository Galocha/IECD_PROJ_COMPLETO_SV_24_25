<!-- filepath: src\main\webapp\jsp\register.jsp -->
<%@ page import="java.util.Map" %>
<%@ page import="game.Player" %>

<%
    String action = request.getParameter("action");
    if ("register".equals(action)) {
        String nickname = request.getParameter("nickname");
        String password = request.getParameter("password");
        String nationality = request.getParameter("nationality");
        String ageStr = request.getParameter("age");
        int age = 0;
        try { age = Integer.parseInt(ageStr); } catch (Exception e) {}

        @SuppressWarnings("unchecked")
        Map<String, Player> players = (Map<String, Player>) application.getAttribute("players");
        if (players == null) {
            players = new java.util.HashMap<>();
            application.setAttribute("players", players);
        }

        if (players.containsKey(nickname)) {
            response.sendRedirect("register.jsp?error=1");
            return;
        } else {
            Player player = new Player(nickname, password, nationality, age, "");
            players.put(nickname, player);
            application.setAttribute("players", players);
            server.GoBangServer.savePlayersToXML();
            response.sendRedirect("login.jsp?registered=1");
            return;
        }
    }
%>
<html>
<head><title>Registo</title></head>
<body>
    <% if ("1".equals(request.getParameter("error"))) { %>
        <p style="color: red;">Nickname já existe!</p>
    <% } %>
    <form action="register.jsp" method="post">
        <input type="hidden" name="action" value="register" />
        <label>Nickname: <input type="text" name="nickname" required /></label><br>
        <label>Password: <input type="password" name="password" required /></label><br>
        <label>Nacionalidade: <input type="text" name="nationality" required /></label><br>
        <label>Idade: <input type="number" name="age" required /></label><br>
        <input type="submit" value="Registar" />
    </form>
    <a href="login.jsp">Voltar ao login</a>
</body>
</html>