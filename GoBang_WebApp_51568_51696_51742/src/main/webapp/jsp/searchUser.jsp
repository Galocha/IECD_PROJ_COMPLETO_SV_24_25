<!-- filepath: src\main\webapp\jsp\searchUser.jsp -->
<%@ page import="java.util.*,game.Player" %>
<%
    String q = request.getParameter("q");
    List<String> nomes = new ArrayList<>();
    @SuppressWarnings("unchecked")
    Map<String, Player> players = (Map<String, Player>) application.getAttribute("players");
    if (q != null && players != null) {
        for (Player p : players.values()) {
            if (p.getNickname().toLowerCase().startsWith(q.toLowerCase())) {
                nomes.add(p.getNickname());
            }
        }
    }
    response.setContentType("application/json");
    out.print("[" + String.join(",", nomes.stream().map(n -> "\"" + n + "\"").toArray(String[]::new)) + "]");
%>