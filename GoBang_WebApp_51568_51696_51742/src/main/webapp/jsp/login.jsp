<%@ page import="java.util.Map" %>
<%@ page import="game.Player" %>
<%@ page import="java.net.Socket, java.io.PrintWriter, java.io.BufferedReader, java.io.InputStreamReader" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.HashSet" %>
<%
	//==========================================================
	// JSP: login.jsp
	// Fun��o: Autentica��o do utilizador e cria��o de sess�o.
	// T�cnicas: Valida��o de credenciais, comunica��o via socket TCP com o servidor GoBang,
	// manipula��o de sess�o, redirecionamento, e integra��o com frontend HTML.
	// ==========================================================
	
	// 1. Obt�m par�metros do pedido HTTP (POST ou GET)
    String action = request.getParameter("action");
    String nickname = request.getParameter("nickname");
    String password = request.getParameter("password");

 	// 2. Se o par�metro action for "login", processa o pedido de autentica��o
    if ("login".equals(action)) {
    	// 2.1. Carrega jogadores do ficheiro XML do servidor GoBang (garante que o mapa est� atualizado)
        server.GoBangServer.loadPlayersFromXML();
        Map<String, Player> players = server.GoBangServer.getPlayers();
        application.setAttribute("players", players);

     	// 2.2. Procura o jogador pelo nickname e valida a password
        Player player = players.get(nickname);
        if (player != null && player.getPassword().equals(password)) {
        	// 2.3. Se as credenciais estiverem corretas, guarda o jogador na sess�o
            session.setAttribute("player", player);

            // 2.4. Cria um socket TCP para o servidor GoBang (para comandos em tempo real)
            try {
                String SERVER_IP = "26.106.140.96";
                int PORT = 1234;
                Socket socket = new Socket(SERVER_IP, PORT);
                PrintWriter outSocket = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader inSocket = new BufferedReader(new InputStreamReader(socket.getInputStream()));

             	// 2.5. Guarda o socket e fluxos na sess�o (para uso posterior em outras p�ginas/JSPs)
                session.setAttribute("socket", socket);
                session.setAttribute("outSocket", outSocket);
                session.setAttribute("inSocket", inSocket);

             	// 2.6. Faz login no servidor GoBang via socket (envia comando /login)
                outSocket.println("/login " + nickname + " " + password);
                String resposta = inSocket.readLine();
             	// 2.7. Se o login via socket falhar, redireciona para login.jsp com erro
                if (resposta == null || !resposta.toLowerCase().contains("bem-sucedido")) {
                    response.sendRedirect("login.jsp?error=socket");
                    return;
                }
            } catch (Exception e) {
            	// 2.8. Se houver erro ao criar o socket, redireciona para login.jsp com erro
                response.sendRedirect("login.jsp?error=socket");
                return;
            }
            
         	// 2.9. Atualiza a lista de jogadores online no contexto da aplica��o
         	@SuppressWarnings("unchecked")
            Set<String> onlinePlayers = (Set<String>) application.getAttribute("onlinePlayers");
            if (onlinePlayers == null) {
                onlinePlayers = new HashSet<>();
            }
            onlinePlayers.add(nickname);
            application.setAttribute("onlinePlayers", onlinePlayers);

            // 2.10. Se tudo correr bem, redireciona para o lobby
            response.sendRedirect("lobby.jsp");
            return;
        } else {
            // 2.11. Se as credenciais estiverem erradas, redireciona para login.jsp com erro
            response.sendRedirect("login.jsp?error=1");
            return;
        }
    }
%>

<html>
<head>
    <title>Login</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css" />
    <style>
    body {
	    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
	    margin: 0;
	    padding: 20px;
	    color: #333;
	}
    </style>
</head>
<body>
    <% if ("1".equals(request.getParameter("error"))) { %>
        <p style="color: red;">Login inv�lido!</p>
    <% } %>
    <div class="login-container">
    	<h2>Bem-vindo ao GoBang!</h2>
    	<h3>O teu jogo 5 em linha favorito!</h3>
        <form action="login.jsp" method="post">
            <input type="hidden" name="action" value="login" />
            <label>Nickname: <input type="text" name="nickname" required /></label><br>
            <label>Password: <input type="password" name="password" required /></label><br>
            <input type="submit" value="Entrar" />
        </form>
        <p class="center">N�o tens conta? <a href="register.jsp">Regista-te aqui</a></p>
    </div>
</body>
</html>

<!--
Explica��o:
----------------------------------------------------------
- O JSP come�a por importar as classes necess�rias para manipula��o de jogadores, sockets e fluxos.
- Obt�m os par�metros "action", "nickname" e "password" do pedido HTTP.
- Se "action" for "login", executa o processo de autentica��o:
    1. Carrega os jogadores do XML do servidor GoBang (garante que o mapa est� atualizado).
    2. Atualiza o atributo global "players" na aplica��o.
    3. Procura o jogador pelo nickname e valida a password.
    4. Se v�lido, guarda o objeto Player na sess�o.
    5. Cria um socket TCP para o servidor GoBang (localhost:1234).
    6. Cria PrintWriter e BufferedReader para comunica��o com o servidor GoBang.
    7. Guarda o socket e fluxos na sess�o (para uso posterior em outras p�ginas).
    8. Envia o comando "/login nickname password" ao servidor GoBang.
    9. L� a resposta do servidor GoBang. Se n�o for bem-sucedido, redireciona para login.jsp com erro.
    10. Se tudo correr bem, redireciona para lobby.jsp.
    11. Se as credenciais estiverem erradas, redireciona para login.jsp com erro.
- O HTML apresenta o formul�rio de login e, se necess�rio, uma mensagem de erro.
- O formul�rio envia os dados para o pr�prio JSP via POST.
- Link para registo de novo utilizador (register.jsp).

==========================================================
T�cnicas utilizadas:
- Valida��o de credenciais no backend (Java).
- Comunica��o via socket TCP com o servidor GoBang (envio de comandos e leitura de respostas).
- Manipula��o de sess�o HTTP para guardar o utilizador autenticado e fluxos de comunica��o.
- Redirecionamento autom�tico ap�s login bem-sucedido ou falhado.
- Integra��o com frontend HTML para formul�rio de login.

==========================================================
Utiliza��es:
- Este JSP � a porta de entrada da aplica��o GoBang Web.
- � chamado diretamente pelo utilizador ao aceder � p�gina de login ou ao submeter o formul�rio.
- Comunica com o servidor GoBang (Java) para autentica��o real-time.
- Atualiza o contexto da aplica��o com o mapa de jogadores (para autocomplete, convites, etc).
- Guarda o utilizador autenticado e fluxos de socket na sess�o para uso em outras p�ginas (lobby.jsp, play.jsp, etc).

==========================================================
Depend�ncias:
- O ficheiro players.xml deve existir e estar acess�vel para carregar os jogadores.
- O servidor GoBang deve estar ativo e a aceitar liga��es em localhost:1234.
- O contexto da aplica��o deve permitir guardar atributos globais ("players").
- O frontend deve ter um formul�rio que envie os campos "nickname" e "password" para este JSP.
- Outras p�ginas (lobby.jsp, play.jsp) dependem da sess�o criada aqui para funcionar corretamente.
-->