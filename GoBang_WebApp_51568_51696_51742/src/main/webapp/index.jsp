<%@ page import="session.SessionManager" %>
<%
    // Verifica se o SessionManager já existe
    SessionManager sessionManager = (SessionManager) application.getAttribute("sessionManager");
    if (sessionManager == null) {
        // Se não existir, cria e armazena no contexto da aplicação
        application.setAttribute("sessionManager", new SessionManager());
        System.out.println("SessionManager criado com sucesso!");
    }
%>
<% response.sendRedirect("jsp/login.jsp"); %>