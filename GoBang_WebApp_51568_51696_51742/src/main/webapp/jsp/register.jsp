<%@ page import="java.util.Map" %>
<%@ page import="game.Player" %>
<%@ page import="java.net.Socket, java.io.PrintWriter, java.io.BufferedReader, java.io.InputStreamReader" %>
<%
    String action = request.getParameter("action");
    if ("register".equals(action)) {
        String nickname = request.getParameter("nickname");
        String password = request.getParameter("password");
        String nationality = request.getParameter("nationality");
        String ageStr = request.getParameter("age");
        String photo = request.getParameter("photo");
        String color = request.getParameter("color");
        int age = 0;
        try { age = Integer.parseInt(ageStr); } catch (Exception e) {}

        // Envia o comando /register para o GoBangServer via socket
        String SERVER_IP = "26.106.140.96";
        int PORT = 1234;
        try (Socket socket = new Socket(SERVER_IP, PORT);
             PrintWriter outSocket = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader inSocket = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String cmd = "/register " + nickname + " " + password + " " + nationality + " " + age;
            outSocket.println(cmd);
            String resposta = inSocket.readLine();

            if (resposta != null && resposta.toLowerCase().contains("já em uso")) {
                response.sendRedirect("register.jsp?error=1");
                return;
            } else if (resposta != null && resposta.toLowerCase().contains("registo bem-sucedido")) {
                // Opcional: podes guardar foto/cor preferida no XML local, se quiseres
                response.sendRedirect("login.jsp?registered=1");
                return;
            } else {
                response.sendRedirect("register.jsp?error=2");
                return;
            }
        } catch (Exception e) {
            response.sendRedirect("register.jsp?error=socket");
            return;
        }
    }
%>
<html>
<head>
    <title>Registo</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css" />
    <style>
	    body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            margin: 0;
            padding: 20px;
            color: #333;
        }
        .register-container {
            max-width: 350px;
            margin: 60px auto;
            padding: 32px 28px 24px 28px;
            background: #fff;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.08);
        }
        .register-container label {
            display: block;
            margin-bottom: 12px;
            font-weight: 500;
        }
        .register-container input[type="text"],
        .register-container input[type="password"],
        .register-container input[type="number"] {
            width: 100%;
            padding: 7px 10px;
            margin-top: 4px;
            margin-bottom: 10px;
            border: 1px solid #ccc;
            border-radius: 4px;
            font-size: 1rem;
        }
        .register-container input[type="color"] {
            width: 40px;
            height: 32px;
            padding: 0;
            margin-bottom: 10px;
            border: none;
            background: none;
        }
        .register-container input[type="submit"] {
            width: 100%;
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
        .register-container input[type="submit"]:hover {
            background: #125ca1;
        }
        .register-container p {
            text-align: center;
            margin-top: 18px;
        }
        .register-container a {
            color: #1976d2;
            text-decoration: none;
        }
        .register-container a:hover {
            text-decoration: underline;
        }
        .error-message {
            color: #d32f2f;
            text-align: center;
            margin-bottom: 12px;
        }
        h2{
		text-align: center;
		}
		h3{
			text-align: center;
		}
    </style>
</head>
<body>
    <% if ("1".equals(request.getParameter("error"))) { %>
        <div class="error-message">Nickname já existe!</div>
    <% } %>
    <div class="register-container">
    	<h2>Regista-te</h2>
    	<h3>Campos com "*" são obrigatórios</h3>
        <form action="register.jsp" method="post">
            <input type="hidden" name="action" value="register" />
            <label>Nickname*: <input type="text" name="nickname" required /></label>
            <label>Password*: <input type="password" name="password" required /></label>
            <label>Nacionalidade*: <input type="text" name="nationality" required /></label>
            <label>Idade*: <input type="number" name="age" required /></label>
            <label>Foto de perfil (URL): <input type="text" name="photo" placeholder="https://..." /></label>
            <label>Cor preferida: <input type="color" name="color" value="#ffffff" /></label>
            <input type="submit" value="Registar" />
        </form>
        <p><a href="login.jsp">Voltar ao login</a></p>
    </div>
</body>
</html>