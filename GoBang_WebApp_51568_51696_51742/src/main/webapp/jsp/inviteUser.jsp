<%@ page import="java.util.*,game.Player" %>
<%
	// JSP: inviteUser.jsp
	// Fun��o: Permite a um jogador convidar outro para jogar GoBang.
	// T�cnicas utilizadas:
	// - Manipula��o de sess�o (session scope) para garantir que o utilizador est� autenticado.
	// - Acesso e atualiza��o de atributos globais da aplica��o (application scope) para gerir convites.
	// - Gera��o de resposta din�mica para integra��o AJAX no frontend.
	//
	// Como funciona:
	// 1. Obt�m o jogador autenticado (remetente) a partir da sess�o.
	// 2. Obt�m o nickname do destinat�rio do convite a partir do par�metro "nick".
	// 3. Valida se o remetente est� autenticado, se o nickname do destinat�rio foi fornecido,
	//    e se o jogador n�o est� a tentar convidar-se a si pr�prio.
	// 4. Obt�m (ou cria) o mapa global de convites da aplica��o.
	// 5. Garante que s� pode existir um convite pendente por par de jogadores (from->to).
	// 6. Se j� existir convite, devolve mensagem de erro. Caso contr�rio, adiciona o convite ao mapa.
	// 7. Atualiza o atributo global "convites" e devolve mensagem de sucesso ao frontend.
	//
	// Utiliza��es:
	// - Este JSP � chamado via AJAX a partir do frontend, normalmente a partir de um bot�o "Convidar para jogar"
	//   presente em p�ginas como perfilPublico.jsp ou lobby.jsp.
	// - O bot�o de convite chama uma fun��o JS (ex: inviteUser(nick)), que faz fetch/post para este JSP.
	// - A resposta � apresentada ao utilizador (ex: "Convite enviado para X!" ou "J� convidou este jogador.").
	//
	// Depend�ncias:
	// - Sess�o v�lida: o jogador remetente tem de estar autenticado (session.getAttribute("player")).
	// - O atributo de aplica��o "convites" deve estar inicializado (ou ser� criado aqui).
	// - O nickname do destinat�rio deve ser fornecido via par�metro "nick".
	// - O frontend deve ter l�gica para tratar a resposta (exibir mensagem ao utilizador).
	// - Este JSP n�o comunica diretamente com o servidor GoBang, apenas gere o estado dos convites na aplica��o web.
	// - O convite ser� posteriormente processado por aceitarConvite.jsp ou recusarConvite.jsp.
	
    Player from = (Player) session.getAttribute("player");
    String toNick = request.getParameter("nick");
    if (from == null || toNick == null || from.getNickname().equalsIgnoreCase(toNick)) {
        out.print("Convite inv�lido.");
        return;
    }
    
    // Verifica se o destinat�rio est� online (usando a lista de onlinePlayers do contexto da aplica��o)
    @SuppressWarnings("unchecked")
    Set<String> onlinePlayers = (Set<String>) application.getAttribute("onlinePlayers");
    if (onlinePlayers == null || !onlinePlayers.contains(toNick)) {
        out.print("O jogador n�o est� online.");
        return;
    }
    
    @SuppressWarnings("unchecked")
    Map<String, String> convites = (Map<String, String>) application.getAttribute("convites");
    if (convites == null) {
        convites = new HashMap<>();
    }
    // S� permite um convite por par
    String key = from.getNickname() + "->" + toNick;
    if (convites.containsKey(key)) {
        out.print("J� convidou este jogador.");
    } else {
        convites.put(key, "pending");
        application.setAttribute("convites", convites);
        out.print("Convite enviado para " + toNick + "!");
    }
    
    /*
    Resumo:
	- Chamado via AJAX por perfilPublico.jsp (bot�o "Convidar para jogar") e potencialmente lobby.jsp.
	- Depende de sess�o autenticada e do atributo global "convites".
	- A resposta � tratada por JS no frontend para feedback ao utilizador.
	- O convite criado ser� mostrado em getConvites.jsp e pode ser aceite (aceitarConvite.jsp) ou recusado (recusarConvite.jsp).
    */
%>