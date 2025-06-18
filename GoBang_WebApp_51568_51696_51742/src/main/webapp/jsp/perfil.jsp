<%@ page import="java.util.Map,java.io.File" %>
<%@ page import="game.Player" %>
<%
	//Verifica se há um jogador autenticado na sessão; caso contrário, redireciona para o login
    Player player = (Player) session.getAttribute("player");
    if (player == null) {
        response.sendRedirect("login.jsp");
        return;
    }

    String msg = "";
 	// Se o formulário for submetido por POST, processa os dados recebidos
    if ("POST".equalsIgnoreCase(request.getMethod())) {
        String nationality = request.getParameter("nationality");
        String color = request.getParameter("color");
        String ageStr = request.getParameter("age");
        String photo = request.getParameter("photo");
        int age = 0;
        try { age = Integer.parseInt(ageStr); } catch (Exception e) {} // Ignora erros na conversão da idade

     	// Atualiza os atributos do jogador com os dados recebidos
        player.setNationality(nationality);
        player.setPreferredColor(color);
        player.setAge(age);
        player.setPhoto(photo);

     	// Atualiza o mapa de jogadores na aplicação e persiste no XML
        @SuppressWarnings("unchecked")
        Map<String, Player> players = (Map<String, Player>) application.getAttribute("players");
        players.put(player.getNickname(), player); // Substitui o jogador antigo pelo atualizado
        application.setAttribute("players", players); // Atualiza o contexto da aplicação
        server.GoBangServer.savePlayersToXML(); // Persiste os dados atualizados no ficheiro XML
        msg = "Perfil atualizado!";
    }

 	// Obtém o código do país para exibir a bandeira correspondente
    String cc = player.getNationality();
    String flagCode = "un"; // Código padrão
    if (cc != null && cc.length() == 2) {
        flagCode = cc.toLowerCase(); // Assume que o código tem 2 letras
    }
%>
<html>
<head>
    <title>Perfil</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: <%= player.getPreferredColor() != null ? player.getPreferredColor() : "#f0f2f5" %>;
            margin: 0;
            padding: 20px;
            color: #333;
        }
        #perfil-container {
            max-width: 400px;
            margin: 40px auto;
            padding: 24px 28px 20px 28px;
            background-color: #fff;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        h2 {
            margin-top: 0;
            color: #1976d2;
            text-align: center;
        }
        .profile-img {
            display: block;
            margin: 0 auto 18px auto;
            border-radius: 50%;
            border: 2px solid #aaa;
            background: #fff;
            object-fit: cover;
            width: 120px;
            height: 120px;
        }
        .profile-flex {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 16px;
            margin-bottom: 18px;
            width: fit-content;
            margin-left: auto;
            margin-right: auto;
        }
        form {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }
        input[type="text"],
        input[type="number"],
        input[type="color"] {
            width: 100%;
            padding: 7px 10px;
            border: 1px solid #ccc;
            border-radius: 4px;
            font-size: 1rem;
        }
        input[type="color"] {
            padding: 0;
            height: 36px;
            width: 50px;
        }
        input[type="submit"] {
            padding: 10px;
            background: #1976d2;
            color: #fff;
            border: none;
            border-radius: 4px;
            font-size: 1rem;
            font-weight: bold;
            cursor: pointer;
            margin-top: 10px;
            transition: background 0.2s;
        }
        input[type="submit"]:hover {
            background: #125ca1;
        }
        .success-message {
            color: #388e3c;
            text-align: center;
            margin-top: 10px;
            margin-bottom: 10px;
            font-weight: bold;
        }
        a {
            color: #1976d2;
            text-decoration: none;
            display: block;
            text-align: center;
            margin-top: 18px;
        }
        a:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body<% String cor = player.getPreferredColor(); if (cor != null && !cor.isEmpty()) { %> style="background-color:<%= cor %>;"<% } %>>
    <div id="perfil-container">
        <h2>Editar Perfil</h2>
        <div class="profile-flex">
            <% if (player.getPhoto() != null && !player.getPhoto().isEmpty()) { %>
                <img src="<%= player.getPhoto() %>" alt="Foto de perfil" class="profile-img">
            <% } %>
            <img src="https://flagcdn.com/128x96/<%= flagCode %>.png"
                alt="Bandeira"
                style="border-radius:4px; width:128px; height:96px; object-fit:cover; align-self:center;">
        </div>
        <%-- 
		    Quando o utilizador submete o formulário, os dados são enviados ao servidor 
		    através de uma requisição HTTP com o método POST.
		    Isto garante que os dados são enviados de forma mais segura e não visível na URL.
		--%>
        <form method="post"> 
            Nacionalidade: <input name="nationality" value="<%= player.getNationality() %>" required /><br>
            Idade: <input name="age" type="number" value="<%= player.getAge() %>" required /><br>
            Foto (URL): <input name="photo" value="<%= player.getPhoto() %>" /><br>
            Cor preferida: <input type="color" name="color" value="<%= player.getPreferredColor() != null ? player.getPreferredColor() : "#ffffff" %>"/><br>
            <input type="submit" value="Guardar"/>
        </form>
        <% if (msg != null && !msg.isEmpty()) { %>
            <div class="success-message"><%= msg %></div>
        <% } %>
        <a href="lobby.jsp">Voltar</a>
    </div>
</body>
</html>