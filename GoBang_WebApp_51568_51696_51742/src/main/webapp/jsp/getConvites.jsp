<%@ page import="java.util.*,game.Player" %>
<%
	// JSP: convitesRecebidos.jsp (exemplo de nome)
	// Fun��o: Mostra ao utilizador autenticado a lista de convites recebidos para jogar.
	// T�cnicas utilizadas:
	// - Manipula��o de sess�o (session scope) para garantir autentica��o.
	// - Acesso a atributos globais da aplica��o (application scope) para obter o mapa de convites.
	// - Gera��o din�mica de HTML para integra��o com AJAX no frontend.
	// - Integra��o com JavaScript do frontend para aceitar/recusar convites (fun��es aceitarConvite/recusarConvite).
	//
	// Onde � utilizado:
	// - Este JSP � normalmente inclu�do ou chamado via AJAX EM lobby.jsp,
	//   para atualizar dinamicamente a lista de convites recebidos sem recarregar a p�gina.
	// - Utiliza o atributo de sess�o "player" (deve estar autenticado).
	// - Utiliza o atributo de aplica��o "convites" (mapa global de convites pendentes).
	// - Depende de fun��es JavaScript no frontend com nomes aceitarConvite(nick) e recusarConvite(nick).
	
	
    Player player = (Player) session.getAttribute("player");
    if (player == null) {
    	// Se n�o estiver autenticado, devolve mensagem de erro (usado em AJAX)
        out.print("<li>N�o autenticado.</li>");
        return;
    }
    @SuppressWarnings("unchecked")
    Map<String, String> convites = (Map<String, String>) application.getAttribute("convites");
    boolean temConvite = false;
    if (convites != null) {
        for (Map.Entry<String, String> entry : convites.entrySet()) {
        	// Cada convite tem a chave "remetente->destinatario" e valor "pending"
            String[] partes = entry.getKey().split("->");
            if (partes.length == 2 && partes[1].equalsIgnoreCase(player.getNickname()) && "pending".equals(entry.getValue())) {
                temConvite = true;
%>
    <li>
        <b><%= partes[0] %></b> convidou-o para jogar!
        <!-- Bot�es que chamam fun��es JS do frontend para aceitar ou recusar o convite -->
        <button onclick="aceitarConvite('<%= partes[0] %>')">Aceitar</button>
        <button onclick="recusarConvite('<%= partes[0] %>')">Recusar</button>
    </li>
<%
            }
        }
    }
    if (!temConvite) {
%>
    <li>Nenhum convite recebido.</li>
<%
    }
%>
<!--
    UTILIZA��ES E DEPEND�NCIAS:
    - Este JSP � chamado via AJAX (ex: setInterval ou fetch) para atualizar a lista de convites em tempo real.
    - Precisa de:
        - Sess�o v�lida (player autenticado).
        - application.getAttribute("convites") inicializado (normalmente feito no contexto da aplica��o).
        - Fun��es JS aceitarConvite(nick) e recusarConvite(nick) implementadas no frontend.
    - Comunica indiretamente com outros JSPs:
        - O bot�o "Aceitar" normalmente chama outro JSP (aceitarConvite.jsp) para processar a aceita��o.
        - O bot�o "Recusar" chama outro JSP (recusarConvite.jsp) para remover o convite.
-->