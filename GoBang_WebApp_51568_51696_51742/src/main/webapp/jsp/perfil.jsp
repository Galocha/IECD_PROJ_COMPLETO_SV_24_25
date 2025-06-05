<!-- filepath: src\main\webapp\jsp\perfil.jsp -->
<%@ page import="java.util.Map" %>
<%@ page import="game.Player" %>
<%
    Player player = (Player) session.getAttribute("player");
    if (player == null) {
        response.sendRedirect("login.jsp");
        return;
    }
    String msg = "";
    if ("POST".equalsIgnoreCase(request.getMethod())) {
        String nationality = request.getParameter("nationality");
        String color = request.getParameter("color");
        String ageStr = request.getParameter("age");
        String photo = request.getParameter("photo");
        int age = 0;
        try { age = Integer.parseInt(ageStr); } catch (Exception e) {}

        player.setNationality(nationality);
        player.setPreferredColor(color);
        player.setAge(age);
        player.setPhoto(photo);

        @SuppressWarnings("unchecked")
        Map<String, Player> players = (Map<String, Player>) application.getAttribute("players");
        players.put(player.getNickname(), player);
        application.setAttribute("players", players);
        server.GoBangServer.savePlayersToXML();
        msg = "Perfil atualizado!";
    }
%>
<html>
<head><title>Perfil</title></head>
<body<% String cor = player.getPreferredColor(); if (cor != null && !cor.isEmpty()) { %> style="background-color:<%= cor %>;"<% } %>>
    <h2>Editar Perfil</h2>
    <form method="post">
        Nacionalidade: <input name="nationality" value="<%= player.getNationality() %>" required /><br>
        Idade: <input name="age" type="number" value="<%= player.getAge() %>" required /><br>
        Foto (URL): <input name="photo" value="<%= player.getPhoto() %>" /><br>
        Cor preferida: <input type="color" name="color" value="<%= player.getPreferredColor() != null ? player.getPreferredColor() : "#ffffff" %>"/><br>
        <input type="submit" value="Guardar"/>
    </form>
    <span style="color:green;"><%= msg %></span>
    <br><a href="lobby.jsp">Voltar</a>
</body>
</html>