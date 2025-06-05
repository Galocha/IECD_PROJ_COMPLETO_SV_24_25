<%@ page import="session.SessionManager" %>
<%
    // Verifica se o SessionManager j� existe
    SessionManager sessionManager = (SessionManager) application.getAttribute("sessionManager");
    if (sessionManager == null) {
        // Se n�o existir, cria e armazena no contexto da aplica��o
        application.setAttribute("sessionManager", new SessionManager());
        System.out.println("SessionManager criado com sucesso!");
    }
%>
<% response.sendRedirect("jsp/login.jsp"); %>