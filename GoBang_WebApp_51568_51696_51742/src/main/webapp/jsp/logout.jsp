<%@ page import="java.net.Socket" %>
<%@ page import="game.Player" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.HashSet" %>
<%
    Player player = (Player) session.getAttribute("player");
    Socket socket = (Socket) session.getAttribute("socket");
    String nickname = (player != null) ? player.getNickname() : null;

    if (socket != null && !socket.isClosed()) {
        // Envia comando /disconnect para o servidor GoBang
        java.io.PrintWriter outSocket = new java.io.PrintWriter(socket.getOutputStream(), true);
        outSocket.println("/disconnect");
        try {
            java.io.BufferedReader inSocket = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
            inSocket.readLine();
        } catch (Exception e) {
            // Ignora erros de leitura
        }
        socket.close();
    }

    @SuppressWarnings("unchecked")
    Set<String> onlinePlayers = (Set<String>) application.getAttribute("onlinePlayers");
    if (onlinePlayers != null && nickname != null) {
        onlinePlayers.remove(nickname);
        application.setAttribute("onlinePlayers", onlinePlayers);
    }

    // Remove do mapa global de jogadores online
    if (player != null) {
        @SuppressWarnings("unchecked")
        Map<String, Player> players = (Map<String, Player>) application.getAttribute("players");
        if (players != null) {
            players.remove(player.getNickname());
            application.setAttribute("players", players);
        }
        @SuppressWarnings("unchecked")
        Map<String, String> convites = (Map<String, String>) application.getAttribute("convites");
        if (convites != null) {
            convites.entrySet().removeIf(e -> e.getKey().endsWith("->" + player.getNickname()));
            application.setAttribute("convites", convites);
        }
    }
    session.invalidate();
    response.sendRedirect("login.jsp");
%>