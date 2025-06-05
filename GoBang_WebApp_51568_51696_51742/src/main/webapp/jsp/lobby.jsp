<!-- filepath: src\main\webapp\jsp\lobby.jsp -->
<%@ page import="game.Player" %>
<%@ page import="session.SessionManager" %>
<%
	//Garante que o SessionManager existe
	SessionManager sessionManager = (SessionManager) application.getAttribute("sessionManager");
	if (sessionManager == null) {
	    sessionManager = new SessionManager();
	    application.setAttribute("sessionManager", sessionManager);
	    System.out.println("[DEBUG] SessionManager criado em lobby.jsp");
	}

    Player player = (Player) session.getAttribute("player");
    if (player == null) {
        response.sendRedirect("login.jsp");
        return;
    }
%>
<html>
<head><title>Lobby</title></head>
<body<% String cor = player.getPreferredColor(); if (cor != null && !cor.isEmpty()) { %> style="background-color:<%= cor %>;"<% } %>>
    <h2>Bem-vindo, <%= player.getNickname() %>!</h2>
    <img src="<%= player.getPhoto() != null && !player.getPhoto().isEmpty() ? player.getPhoto() : "default.png" %>" width="80" /><br>
    <a href="perfil.jsp">Editar Perfil</a> |
    <a href="ranking.jsp">Quadro de Honra</a> |
    <a href="searchUser.jsp">Procurar Jogadores</a> |
    <a href="activeGames.jsp">Jogos Ativos</a> |
    <a href="logout.jsp">Logout</a>
    <hr>
    <h3>O que deseja fazer?</h3>
    <ul>
        <li><a href="play.jsp?action=joinQueue">Entrar na fila para jogar</a></li>
    </ul>
    <input type="text" id="search" autocomplete="off" placeholder="Procurar jogador..." />
    <div id="suggestions"></div>
    <!-- este próximo script foi feito tendo em conta a técnica AJAX -->
    <!-- utilizou-se o fetch() por ser mais simples de usar -->
    <script> 
    document.getElementById('search').addEventListener('input', function() {
        var q = this.value;
        if (q.length < 2) return;
        fetch('searchUser.jsp?q=' + encodeURIComponent(q)) <!-- A função fetch() em JavaScript é usada para efetuar requisições HTTP -->
        <!-- (como GET ou POST) a um servidor, de forma assíncrona. É uma API nova que substitui, na maioria dos casos, o antigo XMLHttpRequest -->
        .then(r => r.json())
        .then(arr => {
            document.getElementById('suggestions').innerHTML = arr.map(n => '<div>' + n + '</div>').join('');
        });
    });
    </script>
</body>
</html>