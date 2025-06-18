<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="game.Player" %>
<%@ page import="java.io.PrintWriter, java.io.BufferedReader, java.net.Socket" %>
<%@ page import="game.Player,server.GoBangServer" %>
<%@ page import="java.util.Map" %>
<%
	// JSP: AJAX de jogo (ex: playAjax.jsp)
	// Responsável por receber pedidos AJAX do frontend (JS) durante o jogo:
	// - Verifica se o convite foi aceite (polling)
	// - Processa jogadas (/move)
	// - Faz polling ao estado do jogo (/get)
	// - Lida com timeout e desistência
	// Técnicas utilizadas:
	// - AJAX (frontend JS faz pedidos a este JSP)
	// - Polling (verifica resultados e estado do jogo)
	// - Comunicação com o servidor GoBang via socket TCP
	// - Manipulação de sessão e atributos globais
	
	// 1. Garante que o jogador está autenticado (sessão válida)
    Player player = (Player) session.getAttribute("player");
    if (player == null) {
        out.print("error:session");
        return;
    }

 	// 2. Obtém parâmetros do pedido AJAX
    String action = request.getParameter("action");
    String nickname = player.getNickname();
    String password = player.getPassword();
    
    // 3. Polling: verifica se há resultado de convite aceite (usado no lobby)
    Map<String, String> lastGameResults = server.GoBangServer.getLastGameResults();
    String msg = lastGameResults.get(player.getNickname());
    if ("checkGlobal".equals(action)) {
        if (msg != null && msg.startsWith("game_over:")) {
            out.print(msg);
            lastGameResults.remove(player.getNickname());
        } else {
            out.print("");
        }
        return;
    }
    if (msg != null && msg.startsWith("CONVITE_ACEITE:")) {
        out.print(msg);
        lastGameResults.remove(player.getNickname());
        return;
    }

    // 4. Abre um novo socket para cada pedido AJAX (stateless)
    String SERVER_IP = "26.106.140.96";
    int PORT = 1234;
    try (Socket socket = new Socket(SERVER_IP, PORT);
         PrintWriter outSocket = new PrintWriter(socket.getOutputStream(), true);
         BufferedReader inSocket = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()))) {

    	// 4.1. Faz login automático do jogador no servidor GoBang
        outSocket.println("/login " + nickname + " " + password);
        String loginResp = inSocket.readLine();
        if (loginResp == null || !loginResp.toLowerCase().contains("bem-sucedido")) {
            out.print("error:login");
            return;
        }

     	// 5. Processa ações AJAX vindas do frontend
        if ("move".equals(action)) {
        	// 5.1. Jogada do utilizador (/move linha coluna)
            String row = request.getParameter("row");
            String col = request.getParameter("col");
            outSocket.println("/move " + row + " " + col);
            String resposta = inSocket.readLine();

            System.out.println("DEBUG resposta do servidor: " + resposta);

            String respostaLower = resposta != null ? resposta.toLowerCase() : "";
            if (
                respostaLower.startsWith("vitória") ||
                respostaLower.startsWith("empate") ||
                respostaLower.startsWith("fim de jogo")
            ) {
                out.print("game_over:" + resposta);
            } else if (respostaLower.startsWith("error:invalid_move")) {
                out.print("error:invalid_move");
            } else if (respostaLower.contains("refresh")) {
                out.print("refresh");
            } else if (respostaLower.startsWith("error:")) {
                out.print("error:invalid_move");
            } else {
                // fallback: trata qualquer resposta inesperada como fim de jogo
                out.print("game_over:" + resposta);
            }
            return;
        }

        if ("check".equals(action)) {
        	// 5.2. Polling ao estado do jogo (/get nickname)
            String moveCountStr = request.getParameter("moveCount");
            int moveCount = 0;
            try { moveCount = Integer.parseInt(moveCountStr); } catch (Exception e) {}

            outSocket.println("/get " + nickname);
            String resposta = inSocket.readLine();

         	// Conta o número de jogadas no tabuleiro devolvido pelo servidor
            int serverMoveCount = 0;
            boolean suaVez = false;
            if (resposta != null && resposta.contains("TABULEIRO:")) {
                String[] parts = resposta.split(";");
                for (String part : parts) {
                    if (part.startsWith("TABULEIRO:")) {
                        String[] linhas = part.substring("TABULEIRO:".length()).split("\\|");
                        for (String linha : linhas) {
                            for (char c : linha.toCharArray()) {
                                if (c == 'X' || c == 'O') serverMoveCount++;
                            }
                        }
                    }
                    if (part.equals("SUA_VEZ")) suaVez = true;
                }
            }

            // Analisa resposta para determinar se deve atualizar o frontend
            if (resposta.contains("FIM DE JOGO") || resposta.contains("VITÓRIA") || resposta.contains("EMPATE")) {
                out.print("game_over:" + resposta);
            } else if (serverMoveCount > moveCount || suaVez) {
                out.print("refresh");
            } else {
                out.print("wait");
            }
            return;
        }

        if ("timeout".equals(action)) {
        	// 5.3. Timeout: notifica o servidor que o tempo esgotou
            outSocket.println("/timeout");
            String resposta = inSocket.readLine();
            out.print("refresh");
            return;
        }

        if ("surrender".equals(action)) {
        	// 5.4. Desistência: notifica o servidor que o jogador desistiu
            outSocket.println("/surrender");
            String resposta = inSocket.readLine();
            out.print("game_over:Desististe!");
            return;
        }

    } catch (Exception e) {
        out.print("error:socket");
    }
%>