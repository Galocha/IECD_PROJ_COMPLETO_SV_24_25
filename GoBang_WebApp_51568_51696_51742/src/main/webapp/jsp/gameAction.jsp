<%@ page import="game.Player, session.SessionManager, game.Game, server.GoBangServer" %>
<%@ page import="java.util.concurrent.TimeUnit" %>
<%
    Player player = (Player) session.getAttribute("player");
    if (player == null) {
        out.print("error:not_logged_in");
        return;
    }

    SessionManager sessionManager = (SessionManager) application.getAttribute("sessionManager");
    if (sessionManager == null) {
        out.print("error:no_session");
        return;
    }

    String action = request.getParameter("action");
    Game game = sessionManager.getGameByPlayer(player);

    if ("move".equals(action)) {
        if (game != null && game.getCurrentPlayer().equals(player)) {
            try {
                int row = Integer.parseInt(request.getParameter("row"));
                int col = Integer.parseInt(request.getParameter("col"));
                String result = game.processMove(player, row, col);

                if (result.startsWith("VITÓRIA") || result.equals("EMPATE")) {
                    sessionManager.endGame(game);
                    out.print("game_over:" + result);
                } else {
                    // Forçar atualização imediata para ambos os jogadores
                    out.print("refresh:all");
                }
            } catch (Exception e) {
                out.print("error:invalid_move");
            }
        } else {
            out.print("error:not_your_turn");
        }
    }
    else if ("surrender".equals(action)) {
        if (game != null) {
            Player winner = game.getOtherPlayer(player);
            winner.setWins(winner.getWins() + 1);
            player.setLosses(player.getLosses() + 1);
            sessionManager.endGame(game);
            out.print("redirect:lobby.jsp");
        }
    }
    else if ("check".equals(action)) {
        if (game != null) {
            String moveCountStr = request.getParameter("moveCount");
            if (moveCountStr != null) {
                int clientMoveCount = Integer.parseInt(moveCountStr);
                int serverMoveCount = game.getMoveCount();
                if (serverMoveCount > clientMoveCount) {
                    out.print("refresh");
                } else {
                    out.print("wait");
                }
            } else {
                out.print("wait");
            }
        } else {
            out.print("wait");
        }
    }
    else if ("timeout".equals(action)) {
        // Nova ação para timeout: passa o turno ou desiste
        if (game != null && game.getCurrentPlayer().equals(player)) {
            game.passTurnOnTimeout(player);
            out.print("refresh");
        } else {
            out.print("error:not_your_turn");
        }
    }
%>