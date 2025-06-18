<%@ page import="game.Player" %>
<%@ page import="session.SessionManager" %>
<%@ page import="java.util.*" %>
<%
	// JSP: lobby.jsp
	// Função: Página principal do lobby do utilizador autenticado.
	// Mostra perfil, permite procurar jogadores, ver convites, aceder a jogos ativos, etc.
	
	//Garante que existe uma instância global do SessionManager partilhada entre utilizadores
	SessionManager sessionManager = (SessionManager) application.getAttribute("sessionManager");
	if (sessionManager == null) {
	    sessionManager = new SessionManager(); // Cria uma nova instância se ainda não existir
	    application.setAttribute("sessionManager", sessionManager); // Guarda no contexto da aplicação
	    System.out.println("[DEBUG] SessionManager criado em lobby.jsp");// Log para debug
	}

	// Obtém o jogador autenticado a partir da sessão HTTP
    Player player = (Player) session.getAttribute("player");
    if (player == null) {
    	// Se o jogador não estiver autenticado, redireciona para a página de login
        response.sendRedirect("login.jsp");
        return;
    }

 	// Obtém o código do país do jogador para determinar a bandeira a mostrar
    String cc = player.getNationality();
    String flagCode = "un"; // Código por omissão (unknown)
    if (cc != null && cc.length() == 2) {
        flagCode = cc.toLowerCase(); // Converte para minúsculas, formato esperado pela CDN
    }

 	// Após garantir que o jogador está autenticado, regista-o no mapa global de jogadores
    @SuppressWarnings("unchecked")
    Map<String, Player> players = (Map<String, Player>) application.getAttribute("players");
    if (players == null) {
        players = new HashMap<>(); // Cria o mapa se ainda não existir
    }
    players.put(player.getNickname(), player); // Adiciona/actualiza o jogador no mapa global
    application.setAttribute("players", players); // Atualiza o atributo global da aplicação
%>
<html>
<head>
    <title>Lobby</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: <%= player.getPreferredColor() != null ? player.getPreferredColor() : "#f0f2f5" %>;
            margin: 0;
            padding: 20px;
            color: #333;
        }
        #lobby-container {
            width: 700px;
            margin: 30px auto;
            padding: 24px;
            background-color: #fff;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.08);
        }
        h2 {
            margin-top: 0;
            color: #1976d2;
        }
        .profile-flex {
            display: flex;
            align-items: flex-start;
            justify-content: center;
            gap: 16px;
            margin-bottom: 10px;
            width: fit-content;
        }
        .profile-img {
            border-radius: 50%;
            border: 2px solid #aaa;
            margin-bottom: 10px;
            background: #fff;
            object-fit: cover;
            width: 120px;
            height: 120px;
        }
        .flag-img {
            border-radius: 4px;
            border: none;
            background: none;
            width: 32px;
            height: 24px;
            margin-left: 16px;
            align-self: center;
        }
        a {
            color: #1976d2;
            text-decoration: none;
            margin: 0 8px;
            font-weight: bold;
        }
        a:hover {
            text-decoration: underline;
        }
        hr {
            margin: 24px 0;
            border: none;
            border-top: 1px solid #eee;
        }
        h3 {
            color: #333;
            margin-bottom: 10px;
        }
        ul {
            padding-left: 20px;
        }
        li {
            margin-bottom: 8px;
        }
        #search {
            width: 100%;
            padding: 8px;
            border: 1px solid #ccc;
            border-radius: 4px;
            margin-top: 12px;
            margin-bottom: 8px;
            font-size: 1rem;
        }
        #suggestions {
            background: #f8f9fa;
            border: 1px solid #ddd;
            border-radius: 4px;
            max-height: 120px;
            overflow-y: auto;
            margin-bottom: 10px;
        }
        #suggestions div {
            padding: 6px 10px;
            cursor: pointer;
        }
        #suggestions div:hover {
            background: #e3eafc;
        }
        .public-profile {
            width: 100%;
            background: #f9f9f9;
            border: 1px solid #ddd;
            border-radius: 4px;
            margin-top: 10px;
            padding: 16px;
            box-sizing: border-box;
        }
        .suggestion {
            cursor: pointer;
        }
        #convites-container {
            position: fixed;
            top: 40px;
            right: 40px;
            min-width: 260px;
            max-width: 320px;
            padding: 18px 16px;
            background: #fff;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.08);
            font-size: 1rem;
            z-index: 100;
        }
        #convites-container ul {
            padding-left: 18px;
        }
        #convites-container li {
            margin-bottom: 10px;
        }
    </style>
</head>
<body<% String cor = player.getPreferredColor(); if (cor != null && !cor.isEmpty()) { %> style="background-color:<%= cor %>;"<% } %>>
	<div style="display: flex; justify-content: center; align-items: flex-start; min-height: 100vh;">
	    <div id="lobby-container">
	        <h2>Bem-vindo, <%= player.getNickname() %>!</h2>
	        <div class="profile-flex">
	            <img src="<%= player.getPhoto() != null && !player.getPhoto().isEmpty() ? player.getPhoto() : "default.png" %>"
	                alt="Foto de perfil"
	                style="width:120px;height:120px;border-radius:50%;border:2px solid #aaa;object-fit:cover;background:#fff;"
	                class="profile-img">
	            <img src="https://flagcdn.com/128x96/<%= flagCode %>.png"
	                alt="Bandeira"
	                style="width:128px;height:96px;margin-left:16px;align-self:center;"
	                class="flag-img">
	        </div>
	        <a href="perfil.jsp">Editar Perfil</a> |
	        <a href="ranking.jsp">Quadro de Honra</a> |
	        <a href="activeGames.jsp">Jogos Ativos</a> |
	        <a href="logout.jsp">Logout</a>
	        <hr>
	        <h3>O que deseja fazer?</h3>
	        <ul>
	            <li><a href="play.jsp?action=joinQueue">Entrar na fila para jogar</a></li>
	        </ul>
	        <input type="text" id="search" autocomplete="off" placeholder="Procurar jogador..." />
	        <div id="suggestions"></div>
	        <div id="public-profile"></div>
		    <!--
                AJAX: Pesquisa dinâmica de jogadores
                - O utilizador escreve no campo de pesquisa.
                - O JS faz fetch para searchUser.jsp?q=... (AJAX).
                - O resultado (JSON) é mostrado como sugestões.
                - Ao clicar numa sugestão, faz fetch para perfilPublico.jsp?nick=... e mostra o perfil público.
            -->
		    <script>
		 		// Pesquisa dinâmica de jogadores (AJAX)
				document.getElementById('search').addEventListener('input', function () {
				    const q = this.value;
				    const suggestionsDiv = document.getElementById('suggestions');
	
				    if (q.length < 2) {
				        suggestionsDiv.innerHTML = '';
				        return;
				    }
					
				 	// Requisição AJAX para obter sugestões (resposta JSON)
				    fetch('<%= request.getContextPath() %>/jsp/searchUser.jsp?q=' + encodeURIComponent(q))
				        .then(r => r.json())
				        .then(arr => {
				            console.log('SugestÃµes recebidas:', arr);
				
				            suggestionsDiv.innerHTML = ''; // limpar antes de adicionar
				            arr.forEach(n => {
				                const nick = (n || '').trim();
				                console.log('Nick processado:', nick);
				
				                const div = document.createElement('div');
				                div.className = 'suggestion';
				                div.dataset.nick = nick;
				                div.textContent = nick;
				
				                suggestionsDiv.appendChild(div);
				            });
				
				            console.log("HTML gerado:", suggestionsDiv.innerHTML);
				        });
				});
				
				// Ao clicar numa sugestão, mostra o perfil público via AJAX
				document.getElementById('suggestions').addEventListener('click', function (e) {
				    let target = e.target;
				    while (target && !target.classList.contains('suggestion')) {
				        target = target.parentElement;
				    }
				
				    if (target && target.classList.contains('suggestion')) {
				        const nick = target.dataset.nick;
				        console.log('Nick clicado:', nick);
				
				        fetch('<%= request.getContextPath() %>/jsp/perfilPublico.jsp?nick=' + encodeURIComponent(nick))
				            .then(r => r.text())
				            .then(html => {
	                            document.getElementById('public-profile').innerHTML = html;
	                        });
				    }
				});
			</script>
	    </div>
	    <!--
            Caixa de convites recebidos (atualizada por AJAX/polling)
            - O conteúdo da <ul> é atualizado a cada segundo via getConvites.jsp.
            - Os botões "Aceitar" e "Recusar" chamam funções JS que fazem AJAX para aceitarConvite.jsp ou recusarConvite.jsp.
        -->
	    <div id="convites-container" style="min-width:260px;max-width:320px;margin-left:32px;padding:18px 16px;background:#fff;border-radius:8px;box-shadow:0 2px 10px rgba(0,0,0,0.08);">
	        <h3>Convites Recebidos</h3>
	        <ul>
	        <%
	            @SuppressWarnings("unchecked")
	            Map<String, String> convites = (Map<String, String>) application.getAttribute("convites");
	            boolean temConvite = false;
	            if (convites != null) {
	                for (Map.Entry<String, String> entry : convites.entrySet()) {
	                    String[] partes = entry.getKey().split("->");
	                    if (partes.length == 2 && partes[1].equalsIgnoreCase(player.getNickname()) && "pending".equals(entry.getValue())) {
	                        temConvite = true;
	        %>
	            <li>
	                <b><%= partes[0] %></b> convidou-o para jogar!
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
	        </ul>
	    </div>
	</div>
	<script>
	// =========================
    // Funções JavaScript cruciais para o lobby
    // =========================
    	
    // Envia convite para outro jogador (AJAX)
    function inviteUser(nick) {
        fetch('inviteUser.jsp?nick=' + encodeURIComponent(nick))
            .then(r => r.text())
            .then(msg => {
                // Mostra a mensagem no local correto, se existir a div
                var msgDiv = document.getElementById('inviteMsg');
                if (msgDiv) msgDiv.innerText = msg.trim();
                else alert(msg.trim());
            });
    }
	
 	// Atualiza a lista de convites recebidos (polling AJAX)
    function atualizarConvites() {
        fetch('getConvites.jsp')
            .then(r => r.text())
            .then(html => {
                var ul = document.querySelector('#convites-container ul');
                if (ul) ul.innerHTML = html;
            });
    }
 	
 	// Aceita convite (AJAX) e redireciona para o jogo se aceite
    function aceitarConvite(fromNick) {
	    fetch('aceitarConvite.jsp?from=' + encodeURIComponent(fromNick))
	        .then(r => r.text())
	        .then(resp => {
	            alert(resp); // Mostra sempre a mensagem personalizada
	            atualizarConvites();
	            window.location.href = "activeGames.jsp";
	        });
	}
 	
 	// Recusa convite (AJAX)
    function recusarConvite(fromNick) {
        fetch('recusarConvite.jsp?from=' + encodeURIComponent(fromNick))
            .then(r => r.text())
            .then(resp => {
                if (resp.trim() === "recusado") {
                    alert("Convite recusado.");
                } else {
                    alert(resp);
                }
                atualizarConvites();
            });
    }
 	
 	// Polling: atualiza convites a cada 1 segundo
    setInterval(atualizarConvites, 1000);
    
 	// Polling: verifica se algum convite enviado foi aceite (a cada 2 segundos)
    function verificarNotificacoes() {
        fetch('gameAction.jsp?action=checkInvite')
            .then(r => r.text())
            .then(msg => {
            	msg = msg.trim();
                if (msg.startsWith("CONVITE_ACEITE:")) {
                    var parts = msg.split(":");
                    var gameId = parts[1];
                    var acceptedBy = parts[2]; // nickname de quem aceitou
                    alert("O teu convite foi aceite por " + acceptedBy + "!\nJogo criado com ID: " + gameId +
                          "\nAcede aos teus jogos ativos para entrar em jogo.");
                    window.location.href = "activeGames.jsp";
                }
            });
    }
    setInterval(verificarNotificacoes, 1000);
    
    //Polling: envia para o jogador, caso esteja em jogo mas não esteja em play.jsp, uma notificação de fim de jogo
    function verificarNotificacoesGlobais() {
        fetch('gameAction.jsp?action=checkGlobal')
            .then(r => r.text())
            .then(msg => {
                msg = msg.trim();
                if (msg.startsWith("game_over:")) {
                    alert(msg.replace(/^game_over:/, '').trim());
                    window.location.href = "lobby.jsp?fromGame=1";
                }
            });
    }
    setInterval(verificarNotificacoesGlobais, 1000);
    </script>
</body>
</html>
<!--
- Utiliza:
    - session.getAttribute("player"): garante autenticação.
    - application.getAttribute("sessionManager"): gere fila de espera e jogos ativos.
    - application.getAttribute("players"): mapa global de jogadores online.
    - application.getAttribute("convites"): mapa global de convites pendentes.
- Comunica com:
    - searchUser.jsp (AJAX): sugestões de pesquisa de jogadores.
    - perfilPublico.jsp (AJAX): mostra perfil público de outro jogador.
    - inviteUser.jsp (AJAX): envia convite para outro jogador.
    - getConvites.jsp (AJAX/polling): atualiza lista de convites recebidos.
    - aceitarConvite.jsp / recusarConvite.jsp (AJAX): aceitar/recusar convites.
    - gameAction.jsp?action=checkInvite (AJAX/polling): verifica se convite enviado foi aceite.
    - play.jsp: página do jogo (redirecionamento após aceitar convite).
- Técnicas cruciais:
    - AJAX (fetch): comunicação assíncrona com backend sem recarregar página.
    - Polling: atualização periódica da lista de convites e notificações.
    - Manipulação dinâmica do DOM: atualização de sugestões, perfis e convites.
    - Segurança: validação de sessão e atualização de atributos globais.
- Dependências (o que precisa para funcionar):
    - Sessão válida (player autenticado).
    - sessionManager, players e convites inicializados no contexto da aplicação.
    - JSPs auxiliares (searchUser.jsp, getConvites.jsp, aceitarConvite.jsp, etc.) implementados.
    - Funções JS no frontend para AJAX/polling.
    - Servidor GoBang ativo para funcionalidades de jogo.
-->