<!-- filepath: src\main\webapp\jsp\ranking.jsp -->
<%@ page import="java.util.*,game.Player" %>
<%
    @SuppressWarnings("unchecked")
    Map<String, Player> players = (Map<String, Player>) application.getAttribute("players");
    List<Player> lista = new ArrayList<>(players.values());
    lista.sort((a, b) -> {
        int cmp = Integer.compare(b.getWins(), a.getWins());
        if (cmp == 0) {
            double ta = a.getPlayTimes().stream().mapToLong(Long::longValue).average().orElse(0);
            double tb = b.getPlayTimes().stream().mapToLong(Long::longValue).average().orElse(0);
            return Double.compare(ta, tb);
        }
        return cmp;
    });
%>
<html>
<head><title>Quadro de Honra</title></head>
<body>
    <h2>Quadro de Honra</h2>
    <table border="1">
        <tr><th>Foto</th><th>Nickname</th><th>Bandeira</th><th>Vitórias</th><th>Tempo Médio</th></tr>
        <% for (Player p : lista) { %>
            <tr>
                <td><img src="<%= p.getPhoto() %>" width="40"/></td>
                <td><%= p.getNickname() %></td>
                <td><img src="bandeiras/<%= p.getNationality() %>.png" width="40"/></td>
                <td><%= p.getWins() %></td>
                <td>
                    <%
                        double avg = p.getPlayTimes().stream().mapToLong(Long::longValue).average().orElse(0);
                        out.print(String.format("%.1f s", avg/1000));
                    %>
                </td>
            </tr>
        <% } %>
    </table>
    <a href="lobby.jsp">Voltar</a>
</body>
</html>