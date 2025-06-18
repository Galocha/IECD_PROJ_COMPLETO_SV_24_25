<%@ page import="java.util.*,game.Player" %>
<%
	// JSP: convitesRecebidos.jsp (exemplo de nome)
	// Função: Mostra ao utilizador autenticado a lista de convites recebidos para jogar.
	// Técnicas utilizadas:
	// - Manipulação de sessão (session scope) para garantir autenticação.
	// - Acesso a atributos globais da aplicação (application scope) para obter o mapa de convites.
	// - Geração dinâmica de HTML para integração com AJAX no frontend.
	// - Integração com JavaScript do frontend para aceitar/recusar convites (funções aceitarConvite/recusarConvite).
	//
	// Onde é utilizado:
	// - Este JSP é normalmente incluído ou chamado via AJAX EM lobby.jsp,
	//   para atualizar dinamicamente a lista de convites recebidos sem recarregar a página.
	// - Utiliza o atributo de sessão "player" (deve estar autenticado).
	// - Utiliza o atributo de aplicação "convites" (mapa global de convites pendentes).
	// - Depende de funções JavaScript no frontend com nomes aceitarConvite(nick) e recusarConvite(nick).
	
	
    Player player = (Player) session.getAttribute("player");
    if (player == null) {
    	// Se não estiver autenticado, devolve mensagem de erro (usado em AJAX)
        out.print("<li>Não autenticado.</li>");
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
        <!-- Botões que chamam funções JS do frontend para aceitar ou recusar o convite -->
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
    UTILIZAÇÕES E DEPENDÊNCIAS:
    - Este JSP é chamado via AJAX (ex: setInterval ou fetch) para atualizar a lista de convites em tempo real.
    - Precisa de:
        - Sessão válida (player autenticado).
        - application.getAttribute("convites") inicializado (normalmente feito no contexto da aplicação).
        - Funções JS aceitarConvite(nick) e recusarConvite(nick) implementadas no frontend.
    - Comunica indiretamente com outros JSPs:
        - O botão "Aceitar" normalmente chama outro JSP (aceitarConvite.jsp) para processar a aceitação.
        - O botão "Recusar" chama outro JSP (recusarConvite.jsp) para remover o convite.
-->