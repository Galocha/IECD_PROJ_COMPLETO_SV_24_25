<%@ page import="java.util.Map" %>
<%@ page import="game.Player" %>
<%@ page import="java.net.Socket, java.io.PrintWriter, java.io.BufferedReader, java.io.InputStreamReader" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.HashSet" %>
<%
	//==========================================================
	// JSP: login.jsp
	// Função: Autenticação do utilizador e criação de sessão.
	// Técnicas: Validação de credenciais, comunicação via socket TCP com o servidor GoBang,
	// manipulação de sessão, redirecionamento, e integração com frontend HTML.
	// ==========================================================
	
	// 1. Obtém parâmetros do pedido HTTP (POST ou GET)
    String action = request.getParameter("action");
    String nickname = request.getParameter("nickname");
    String password = request.getParameter("password");

 	// 2. Se o parâmetro action for "login", processa o pedido de autenticação
    if ("login".equals(action)) {
    	// 2.1. Carrega jogadores do ficheiro XML do servidor GoBang (garante que o mapa está atualizado)
        server.GoBangServer.loadPlayersFromXML();
        Map<String, Player> players = server.GoBangServer.getPlayers();
        application.setAttribute("players", players);

     	// 2.2. Procura o jogador pelo nickname e valida a password
        Player player = players.get(nickname);
        if (player != null && player.getPassword().equals(password)) {
        	// 2.3. Se as credenciais estiverem corretas, guarda o jogador na sessão
            session.setAttribute("player", player);

            // 2.4. Cria um socket TCP para o servidor GoBang (para comandos em tempo real)
            try {
                String SERVER_IP = "26.106.140.96";
                int PORT = 1234;
                Socket socket = new Socket(SERVER_IP, PORT);
                PrintWriter outSocket = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader inSocket = new BufferedReader(new InputStreamReader(socket.getInputStream()));

             	// 2.5. Guarda o socket e fluxos na sessão (para uso posterior em outras páginas/JSPs)
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
            
         	// 2.9. Atualiza a lista de jogadores online no contexto da aplicação
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
        <p style="color: red;">Login inválido!</p>
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
        <p class="center">Não tens conta? <a href="register.jsp">Regista-te aqui</a></p>
    </div>
</body>
</html>

<!--
Explicação:
----------------------------------------------------------
- O JSP começa por importar as classes necessárias para manipulação de jogadores, sockets e fluxos.
- Obtém os parâmetros "action", "nickname" e "password" do pedido HTTP.
- Se "action" for "login", executa o processo de autenticação:
    1. Carrega os jogadores do XML do servidor GoBang (garante que o mapa está atualizado).
    2. Atualiza o atributo global "players" na aplicação.
    3. Procura o jogador pelo nickname e valida a password.
    4. Se válido, guarda o objeto Player na sessão.
    5. Cria um socket TCP para o servidor GoBang (localhost:1234).
    6. Cria PrintWriter e BufferedReader para comunicação com o servidor GoBang.
    7. Guarda o socket e fluxos na sessão (para uso posterior em outras páginas).
    8. Envia o comando "/login nickname password" ao servidor GoBang.
    9. Lê a resposta do servidor GoBang. Se não for bem-sucedido, redireciona para login.jsp com erro.
    10. Se tudo correr bem, redireciona para lobby.jsp.
    11. Se as credenciais estiverem erradas, redireciona para login.jsp com erro.
- O HTML apresenta o formulário de login e, se necessário, uma mensagem de erro.
- O formulário envia os dados para o próprio JSP via POST.
- Link para registo de novo utilizador (register.jsp).

==========================================================
Técnicas utilizadas:
- Validação de credenciais no backend (Java).
- Comunicação via socket TCP com o servidor GoBang (envio de comandos e leitura de respostas).
- Manipulação de sessão HTTP para guardar o utilizador autenticado e fluxos de comunicação.
- Redirecionamento automático após login bem-sucedido ou falhado.
- Integração com frontend HTML para formulário de login.

==========================================================
Utilizações:
- Este JSP é a porta de entrada da aplicação GoBang Web.
- É chamado diretamente pelo utilizador ao aceder à página de login ou ao submeter o formulário.
- Comunica com o servidor GoBang (Java) para autenticação real-time.
- Atualiza o contexto da aplicação com o mapa de jogadores (para autocomplete, convites, etc).
- Guarda o utilizador autenticado e fluxos de socket na sessão para uso em outras páginas (lobby.jsp, play.jsp, etc).

==========================================================
Dependências:
- O ficheiro players.xml deve existir e estar acessível para carregar os jogadores.
- O servidor GoBang deve estar ativo e a aceitar ligações em localhost:1234.
- O contexto da aplicação deve permitir guardar atributos globais ("players").
- O frontend deve ter um formulário que envie os campos "nickname" e "password" para este JSP.
- Outras páginas (lobby.jsp, play.jsp) dependem da sessão criada aqui para funcionar corretamente.
-->