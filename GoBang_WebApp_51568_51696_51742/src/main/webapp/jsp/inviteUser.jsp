<%@ page import="java.util.*,game.Player" %>
<%
	// JSP: inviteUser.jsp
	// Função: Permite a um jogador convidar outro para jogar GoBang.
	// Técnicas utilizadas:
	// - Manipulação de sessão (session scope) para garantir que o utilizador está autenticado.
	// - Acesso e atualização de atributos globais da aplicação (application scope) para gerir convites.
	// - Geração de resposta dinâmica para integração AJAX no frontend.
	//
	// Como funciona:
	// 1. Obtém o jogador autenticado (remetente) a partir da sessão.
	// 2. Obtém o nickname do destinatário do convite a partir do parâmetro "nick".
	// 3. Valida se o remetente está autenticado, se o nickname do destinatário foi fornecido,
	//    e se o jogador não está a tentar convidar-se a si próprio.
	// 4. Obtém (ou cria) o mapa global de convites da aplicação.
	// 5. Garante que só pode existir um convite pendente por par de jogadores (from->to).
	// 6. Se já existir convite, devolve mensagem de erro. Caso contrário, adiciona o convite ao mapa.
	// 7. Atualiza o atributo global "convites" e devolve mensagem de sucesso ao frontend.
	//
	// Utilizações:
	// - Este JSP é chamado via AJAX a partir do frontend, normalmente a partir de um botão "Convidar para jogar"
	//   presente em páginas como perfilPublico.jsp ou lobby.jsp.
	// - O botão de convite chama uma função JS (ex: inviteUser(nick)), que faz fetch/post para este JSP.
	// - A resposta é apresentada ao utilizador (ex: "Convite enviado para X!" ou "Já convidou este jogador.").
	//
	// Dependências:
	// - Sessão válida: o jogador remetente tem de estar autenticado (session.getAttribute("player")).
	// - O atributo de aplicação "convites" deve estar inicializado (ou será criado aqui).
	// - O nickname do destinatário deve ser fornecido via parâmetro "nick".
	// - O frontend deve ter lógica para tratar a resposta (exibir mensagem ao utilizador).
	// - Este JSP não comunica diretamente com o servidor GoBang, apenas gere o estado dos convites na aplicação web.
	// - O convite será posteriormente processado por aceitarConvite.jsp ou recusarConvite.jsp.
	
    Player from = (Player) session.getAttribute("player");
    String toNick = request.getParameter("nick");
    if (from == null || toNick == null || from.getNickname().equalsIgnoreCase(toNick)) {
        out.print("Convite inválido.");
        return;
    }
    
    // Verifica se o destinatário está online (usando a lista de onlinePlayers do contexto da aplicação)
    @SuppressWarnings("unchecked")
    Set<String> onlinePlayers = (Set<String>) application.getAttribute("onlinePlayers");
    if (onlinePlayers == null || !onlinePlayers.contains(toNick)) {
        out.print("O jogador não está online.");
        return;
    }
    
    @SuppressWarnings("unchecked")
    Map<String, String> convites = (Map<String, String>) application.getAttribute("convites");
    if (convites == null) {
        convites = new HashMap<>();
    }
    // Só permite um convite por par
    String key = from.getNickname() + "->" + toNick;
    if (convites.containsKey(key)) {
        out.print("Já convidou este jogador.");
    } else {
        convites.put(key, "pending");
        application.setAttribute("convites", convites);
        out.print("Convite enviado para " + toNick + "!");
    }
    
    /*
    Resumo:
	- Chamado via AJAX por perfilPublico.jsp (botão "Convidar para jogar") e potencialmente lobby.jsp.
	- Depende de sessão autenticada e do atributo global "convites".
	- A resposta é tratada por JS no frontend para feedback ao utilizador.
	- O convite criado será mostrado em getConvites.jsp e pode ser aceite (aceitarConvite.jsp) ou recusado (recusarConvite.jsp).
    */
%>