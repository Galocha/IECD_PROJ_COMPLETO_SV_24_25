<!-- filepath: src\main\webapp\jsp\logout.jsp -->
<%
    session.invalidate();
    response.sendRedirect("login.jsp");
%>