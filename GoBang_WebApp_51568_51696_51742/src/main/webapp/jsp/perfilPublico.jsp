<%@ page import="game.Player,java.util.*" %>
<% 
	//Linha de debug que imprime o nickname recebido para inspecção no HTML
	out.println("<!-- DEBUG nick recebido: '" + request.getParameter("nick") + "' -->"); 
%>
<%
	//Obtém o parâmetro "nick" da query string
    String nick = request.getParameter("nick");

	//Recupera o mapa de jogadores previamente armazenado na aplicação
    @SuppressWarnings("unchecked")
    Map<String, Player> players = (Map<String, Player>) application.getAttribute("players");
	
 	// Inicializa o objeto Player vazio
    Player p = null;
 	
 	// Se o mapa de jogadores e o nickname forem válidos
    if (players != null && nick != null) {
        nick = nick.trim(); // remove espaços em branco
        
     	// Procura no mapa um jogador com nickname igual
        for (Player playerObj : players.values()) {
            if (playerObj.getNickname().trim().equalsIgnoreCase(nick)) {
                p = playerObj;
                break;
            }
        }
    }
%>
<% if (p != null) { 
	// Define o código do país (para a bandeira) com base na nacionalidade do jogador
    String cc = p.getNationality();
    String flagCode = "un";
    if (cc != null && cc.length() == 2) {
        flagCode = cc.toLowerCase();
    }
%>
    <div class="public-profile">
        <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 10px;">
            <img src="<%= p.getPhoto() != null && !p.getPhoto().isEmpty() ? p.getPhoto() : "default.png" %>" width="80" style="border-radius:50%;border:2px solid #aaa;background:#fff;">
            <img src="https://flagcdn.com/128x96/<%= flagCode %>.png"
                 alt="Bandeira" style="width:48px;height:36px;">
        </div>
        <h4><%= p.getNickname() %></h4>
        <p>Vitórias: <%= p.getWins() %></p>
        <p>Idade: <%= p.getAge() %></p>
        <p>Tempo médio: 
            <%
                double avg = p.getPlayTimes().stream().mapToLong(Long::longValue).average().orElse(0);
                out.print(String.format("%.1f s", avg/1000));
            %>
        </p>
        <%-- Botão para convidar, só aparece se não for o próprio jogador --%>
        <% 
            Player current = (Player) session.getAttribute("player");
            if (current != null && !current.getNickname().equalsIgnoreCase(p.getNickname())) { 
        %>
            <button id="inviteBtn" onclick="inviteUser('<%= p.getNickname() %>')">Convidar para jogar</button>
            <div id="inviteMsg"></div>
            <script>
                function inviteUser(nick) {
                    fetch('inviteUser.jsp?nick=' + encodeURIComponent(nick))
                        .then(r => r.text())
                        .then(msg => document.getElementById('inviteMsg').innerText = msg.trim());
                }
            </script>
        <% } %>
    </div>
<% } else { %>
    <div class="public-profile">Jogador não encontrado.</div>
<% } %>