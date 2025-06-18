<%@ page import="java.util.*,game.Player,server.GoBangServer" %>
<%
    Player player = (Player) session.getAttribute("player");
    String fromNick = request.getParameter("from");
    if (player == null || fromNick == null) {
        out.print("Convite inválido.");
        return;
    }
    @SuppressWarnings("unchecked")
    Map<String, String> convites = (Map<String, String>) application.getAttribute("convites");
    if (convites != null) {
        convites.remove(fromNick + "->" + player.getNickname());
        application.setAttribute("convites", convites);
    }
    // Notifica o remetente
    @SuppressWarnings("unchecked")
    Map<String, Player> players = (Map<String, Player>) application.getAttribute("players");
    Player fromPlayer = players != null ? players.get(fromNick) : null;
    if (fromPlayer != null) {
        server.GoBangServer.notifyPlayer(fromPlayer, "O teu convite para " + player.getNickname() + " foi recusado.");
    }
    out.print("recusado");
%>