<%@ page import="java.util.*,game.Player" %>
<%@ page import="java.net.Socket, java.io.PrintWriter, java.io.BufferedReader, java.io.InputStreamReader" %>
<%
	// JSP PARA ACEITAR UM CONVITE DE JOGO
	// Técnicas utilizadas:
	// - Acesso a atributos de sessão e aplicação (session/application scope)
	// - Comunicação com o servidor GoBang via socket TCP (envio de comandos como um cliente)
	// - Atualização de mapas globais (convites, resultados)
	// - Resposta ao frontend para integração AJAX
	// - Comentários HTML para debug
	
	// 1. Obtém o jogador autenticado da sessão e o nickname do remetente do convite
    Player player = (Player) session.getAttribute("player");
    String fromNick = request.getParameter("from");
    if (player == null || fromNick == null) {
        out.print("Convite inválido.");
        return;
    }
    
 	// 2. Remove o convite aceite do mapa global de convites (application scope)
    @SuppressWarnings("unchecked")
    Map<String, String> convites = (Map<String, String>) application.getAttribute("convites");
    if (convites != null) {
        convites.remove(fromNick + "->" + player.getNickname());
        application.setAttribute("convites", convites);
    }

 	// 3. Prepara comunicação com o servidor GoBang via socket TCP
    String SERVER_IP = "26.106.140.96";
    int PORT = 1234;
    String gameId = null;
    try (Socket socket = new Socket(SERVER_IP, PORT);
         PrintWriter outSocket = new PrintWriter(socket.getOutputStream(), true);
         BufferedReader inSocket = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

    	// 4. Obtém o mapa global de jogadores (application scope)
        @SuppressWarnings("unchecked")
        Map<String, game.Player> players = (Map<String, game.Player>) application.getAttribute("players");
        if (players == null || !players.containsKey(fromNick)) {
            out.print("Jogador remetente não existe.");
            return;
        }
        game.Player fromPlayer = players.get(fromNick);

     	// 5. Faz login do remetente no servidor GoBang (necessário para criar o jogo)
        outSocket.println("/login " + fromNick + " " + fromPlayer.getPassword());
        String loginResp1 = inSocket.readLine();
        if (loginResp1 == null || !loginResp1.toLowerCase().contains("bem-sucedido")) {
            out.print("Erro ao fazer login do remetente no servidor GoBang: " + loginResp1);
            return;
        }

     	// 6. Faz login do destinatário no servidor GoBang
        outSocket.println("/login " + player.getNickname() + " " + player.getPassword());
        String loginResp2 = inSocket.readLine();
        if (loginResp2 == null || !loginResp2.toLowerCase().contains("bem-sucedido")) {
            out.print("Erro ao fazer login do destinatário no servidor GoBang: " + loginResp2);
            return;
        }

     	// 7. Envia comando para criar o jogo entre os dois jogadores
        outSocket.println("/startgame " + fromNick + " " + player.getNickname());
        String resposta = inSocket.readLine();
     	// Comentário HTML para debug/troubleshooting (não visível para o utilizador)
        //out.println("<!-- DEBUG: resposta do servidor: " + resposta + " -->");
        // Espera resposta do tipo: GAMEID:<uuid>
        if (resposta != null && resposta.startsWith("GAMEID:")) {
            gameId = resposta.substring("GAMEID:".length());
        }
    } catch (Exception e) {
    	// Se houver erro na comunicação com o servidor GoBang, devolve erro
        out.print("Erro ao criar jogo no servidor GoBang.");
        return;
    }
    
 	// 8. Atualiza o mapa de resultados do servidor para notificar o remetente do convite
    if (gameId != null) {
        // O remetente será notificado via polling AJAX (GoBangServer.getLastGameResults)
        server.GoBangServer.getLastGameResults().put(fromNick, "CONVITE_ACEITE:" + gameId + ":" + player.getNickname());
     	// Responde ao frontend (AJAX) com o ID do jogo criado
        out.print("Convite aceite! Jogo criado com ID: " + gameId +
                "\nAcede aos teus jogos ativos para entrar em jogo");
    } else {
        out.print("Erro ao criar jogo.");
    }
%>