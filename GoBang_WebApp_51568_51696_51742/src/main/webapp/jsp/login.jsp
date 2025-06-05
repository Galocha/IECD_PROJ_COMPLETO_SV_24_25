<%@ page import="java.util.Map" %>
<%@ page import="game.Player" %>
<%@ page import="javax.xml.parsers.*,org.w3c.dom.*,java.io.File" %>
<%@ page import="session.SessionManager" %>
<%
	//Inicializa o SessionManager se não existir
	SessionManager sessionManager = (SessionManager) application.getAttribute("sessionManager");
	if (sessionManager == null) {
	    sessionManager = new SessionManager();
	    application.setAttribute("sessionManager", sessionManager);
	    System.out.println("[DEBUG] SessionManager criado em login.jsp");
	}
	
	String action = request.getParameter("action");
	String nickname = request.getParameter("nickname");
	String password = request.getParameter("password");

	if ("login".equals(action)) {
	    // XML
	    String xmlPath = application.getRealPath("/WEB-INF/classes/server/players.xml");
	    File file = new File(xmlPath);
	    if (!file.exists()) {
	        file = new File("src/main/java/server/players.xml");
	    }
	    // XSD
	    String xsdPath = application.getRealPath("/WEB-INF/classes/server/players.xsd");
	    File xsdFile = new File(xsdPath);
	    if (!xsdFile.exists()) {
	        xsdFile = new File("src/main/java/server/players.xsd");
	    }

	    Map<String, Player> players = server.GoBangServer.parsePlayersFromXML(file, xsdFile);
	    application.setAttribute("players", players);

	    Player player = players.get(nickname);
	    if (player != null && player.getPassword().equals(password)) {
	        session.setAttribute("player", player);
	        response.sendRedirect("lobby.jsp");
	        return;
	    } else {
	        response.sendRedirect("login.jsp?error=1");
	        return;
	    }
	}
%>

<html>
<head>
	<title>Login</title>
	<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css" />
</head>
<body>
    <% if ("1".equals(request.getParameter("error"))) { %>
        <p style="color: red;">Login inválido!</p>
    <% } %>
    <div class="login-container">
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