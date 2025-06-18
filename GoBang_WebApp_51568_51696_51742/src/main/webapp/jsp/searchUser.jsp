<%@ page contentType="application/json;charset=UTF-8" %>
<%@ page import="java.util.*,game.Player" %>
<%
    String q = request.getParameter("q");
    List<String> nomes = new ArrayList<>();
    @SuppressWarnings("unchecked")
    Map<String, Player> players = (Map<String, Player>) application.getAttribute("players");

    if (q != null && players != null) {
        for (Player p : players.values()) {
            String nick = p.getNickname();
            if (nick != null && !nick.trim().isEmpty() && nick.toLowerCase().startsWith(q.toLowerCase())) {
                nomes.add(nick);
            }
        }
    }

    response.setContentType("application/json");

    StringBuilder json = new StringBuilder("[");
    for (int i = 0; i < nomes.size(); i++) {
        if (i > 0) json.append(",");
        json.append("\"").append(nomes.get(i).replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
    }
    json.append("]");
    out.print(json.toString());
%>
