<%@ page import="java.util.*" %>
<%
    // Limpa convites
    @SuppressWarnings("unchecked")
    Map<String, String> convites = (Map<String, String>) application.getAttribute("convites");
    if (convites != null) {
        convites.clear();
        application.setAttribute("convites", convites);
    }

    // Limpa jogos ativos
    @SuppressWarnings("unchecked")
    Map<String, String> jogos = (Map<String, String>) application.getAttribute("jogos");
    if (jogos != null) {
        jogos.clear();
        application.setAttribute("jogos", jogos);
    }

    // Mensagem de confirmação
    out.print("Convites e jogos ativos limpos com sucesso!");
%>