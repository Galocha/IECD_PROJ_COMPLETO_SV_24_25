package server;

import client.ClientHandler;
import game.Player;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.w3c.dom.*;
import protocol.CommandProtocol;
import session.SessionManager;

/**
 * Classe principal do servidor GoBang.
 * Responsável por:
 * - Aceitar ligações de clientes (ClientHandler).
 * - Gerir jogadores registados (Player), sessões de jogo (SessionManager) e resultados.
 * - Persistir e carregar jogadores em XML (com validação XSD).
 * - Notificar clientes de eventos (vitória, derrota, etc).
 * 
 * Ligações:
 * - Cada cliente ligado é gerido por um ClientHandler (thread).
 * - Usa Player para representar utilizadores registados.
 * - Usa SessionManager para gerir jogos ativos e fila de espera.
 * - Usa CommandProtocol para formatar mensagens.
 * - Utiliza players.xml (e players.xsd) para persistência dos jogadores.
 */
public class GoBangServer {
    private static final int PORT = 1234;
    private static Map<String, Player> players = new HashMap<>(); // Mapa de jogadores registados (nickname -> Player)
    private static List<ClientHandler> clients = new ArrayList<>(); // Lista de clientes ligados (um ClientHandler por cliente)
    private static SessionManager sessionManager = new SessionManager(); // Gerente de sessões de jogo (um por servidor)
    private static volatile boolean isShuttingDown = false; //volatile - indicado para variáveis que são utilizadas/modificadas entre threads
    private static ServerSocket serverSocket; // Socket do servidor (aceita novas ligações)
    private static Map<String, String> lastGameResults = new HashMap<>(); 
    // Mapa de resultados finais de jogos (nickname -> mensagem de fim de jogo)
    // Usado para polling no frontend (ex: "FIM DE JOGO! Ganhaste!")

    /**
     * Ponto de entrada do servidor.
     * - Carrega jogadores do XML.
     * - Aceita ligações de clientes e cria um ClientHandler para cada um.
     * - Guarda jogadores no XML ao terminar.
     * 
     * Ligações:
     * - Para cada cliente, cria um ClientHandler (thread).
     * - Usa CommandProtocol para mensagens de log.
     * - Usa loadPlayersFromXML/savePlayersToXML para persistência.
     */
    public static void main(String[] args) {
    	try {
            loadPlayersFromXML();
            serverSocket = new ServerSocket(PORT);
            System.out.println("Servidor iniciado na porta " + PORT);
            
            while (!isShuttingDown) {
                try {
                    Socket socket = serverSocket.accept();
	                System.out.println(CommandProtocol.formatMessage("Novo cliente conectado: " + socket.getInetAddress().getHostAddress()));
	                ClientHandler clientHandler = new ClientHandler(socket);
	                getClients().add(clientHandler);
	                System.out.println("DEBUG: Número de handlers ativos: " + GoBangServer.getClients().size());
	                new Thread(clientHandler).start();
                } catch (SocketException e) {
                    if (isShuttingDown) {
                        System.out.println(CommandProtocol.formatMessage("Servidor encerrando normalmente..."));
                    } else {
                        System.err.println(CommandProtocol.formatMessage("Erro inesperado no ServerSocket: " + e.getMessage()));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println(CommandProtocol.formatMessage("Erro ao iniciar o servidor: " + e.getMessage()));
        } finally {
        	GoBangServer.saveRankingToXML();
            savePlayersToXML();
        }
    }

    /**
     * Guarda todos os jogadores no ficheiro players.xml (com validação XSD).
     * Cada jogador é guardado com estatísticas, cor, jogos e tempos.
     * 
     * Ligações:
     * - Chamado no fim do main() e em shutdownServer().
     * - Usa Player.getFormattedPlayTimes(), getGameUUIDs(), etc.
     * - O ficheiro é lido por loadPlayersFromXML().
     */
    public static void savePlayersToXML() {
        try {
            System.out.println("DEBUG: Número de jogadores a guardar: " + getPlayers().size());

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("Players");
            doc.appendChild(root);

            for (Player player : getPlayers().values()) {
                Element playerElement = doc.createElement("Player");

                Element nickname = doc.createElement("Nickname");
                nickname.appendChild(doc.createTextNode(player.getNickname()));
                playerElement.appendChild(nickname);

                Element password = doc.createElement("Password");
                password.appendChild(doc.createTextNode(player.getPassword()));
                playerElement.appendChild(password);

                Element nationality = doc.createElement("Nationality");
                nationality.appendChild(doc.createTextNode(player.getNationality()));
                playerElement.appendChild(nationality);

                Element age = doc.createElement("Age");
                age.appendChild(doc.createTextNode(String.valueOf(player.getAge())));
                playerElement.appendChild(age);

                Element photo = doc.createElement("Photo");
                photo.appendChild(doc.createTextNode(player.getPhoto()));
                playerElement.appendChild(photo);

                Element preferredColor = doc.createElement("Color");
                preferredColor.appendChild(doc.createTextNode(player.getPreferredColor()));
                playerElement.appendChild(preferredColor);

                Element wins = doc.createElement("Wins");
                wins.appendChild(doc.createTextNode(String.valueOf(player.getWins())));
                playerElement.appendChild(wins);

                Element losses = doc.createElement("Losses");
                losses.appendChild(doc.createTextNode(String.valueOf(player.getLosses())));
                playerElement.appendChild(losses);

                Element playTime = doc.createElement("PlayTime");
                List<String> formattedTimes = player.getFormattedPlayTimes();
                List<String> uuids = player.getGameUUIDs();
                if (!formattedTimes.isEmpty() && formattedTimes.size() == uuids.size()) {
                    for (int i = 0; i < formattedTimes.size(); i++) {
                        Element game = doc.createElement("Game");
                        game.setAttribute("id", uuids.get(i));
                        game.appendChild(doc.createTextNode(formattedTimes.get(i)));
                        playTime.appendChild(game);
                    }
                }
                // Garante que <PlayTime> é sempre adicionado
                playerElement.appendChild(playTime);

                root.appendChild(playerElement);
            }

            // DEBUG: Mostra o XML gerado antes da validação
            Transformer debugTf = TransformerFactory.newInstance().newTransformer();
            debugTf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            StringWriter debugWriter = new StringWriter();
            debugTf.transform(new DOMSource(doc), new StreamResult(debugWriter));
            System.out.println("DEBUG: XML gerado antes da validação:\n" + debugWriter.toString());

            // Validação XSD
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            InputStream xsdStream = GoBangServer.class.getClassLoader().getResourceAsStream("server/players.xsd");
            if (xsdStream == null) {
                File fallbackXsd = new File("src/main/java/server/players.xsd");
                if (fallbackXsd.exists()) {
                    xsdStream = new FileInputStream(fallbackXsd);
                } else {
                    throw new FileNotFoundException("players.xsd não encontrado!");
                }
            }
            Schema schema = schemaFactory.newSchema(new StreamSource(xsdStream));
            Validator validator = schema.newValidator();
            validator.validate(new DOMSource(doc));

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            // Caminho portátil para user/GoBangData
            String userHome = System.getProperty("user.home");
            File xmlFile = new File(userHome + "/GoBangData/players.xml");
            System.out.println("DEBUG: Caminho do ficheiro XML a guardar: " + xmlFile.getAbsolutePath());

            // Garante que o diretório existe antes de gravar
            File parentDir = xmlFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            StreamResult result = new StreamResult(xmlFile);
            transformer.transform(source, result);

            System.out.println("Jogadores guardados em players.xml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Carrega jogadores do ficheiro players.xml (valida com XSD).
     * Preenche o mapa global de jogadores.
     * 
     * Ligações:
     * - Chamado no início do main().
     * - Usa Player.convertFormattedPlayTimeToMillis(), addPlayTime(), addGameUUID().
     * - O ficheiro é escrito por savePlayersToXML().
     */
    public static void loadPlayersFromXML() {
        try {
            System.out.println("DEBUG: loadPlayersFromXML chamado!");
            String userHome = System.getProperty("user.home");
            File file = new File(userHome + "/GoBangData/players.xml");
            System.out.println("DEBUG: players.xml em: " + file.getAbsolutePath());
            System.out.println("DEBUG: players.xml existe? " + file.exists());
            if (!file.exists()) return;

            InputStream xsdStream = GoBangServer.class.getClassLoader().getResourceAsStream("server/players.xsd");
            if (xsdStream == null) {
                File fallbackXsd = new File("src/main/java/server/players.xsd");
                if (fallbackXsd.exists()) {
                    xsdStream = new FileInputStream(fallbackXsd);
                } else {
                    throw new FileNotFoundException("players.xsd não encontrado!");
                }
            }

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new StreamSource(xsdStream));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(file));

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList nodeList = doc.getElementsByTagName("Player");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element playerElement = (Element) nodeList.item(i);

                String nickname = playerElement.getElementsByTagName("Nickname").item(0).getTextContent();
                String password = playerElement.getElementsByTagName("Password").item(0).getTextContent();
                String nationality = playerElement.getElementsByTagName("Nationality").item(0).getTextContent();
                int age = Integer.parseInt(playerElement.getElementsByTagName("Age").item(0).getTextContent());
                String photo = playerElement.getElementsByTagName("Photo").item(0).getTextContent();
                String color = playerElement.getElementsByTagName("Color").item(0).getTextContent();
                int wins = Integer.parseInt(playerElement.getElementsByTagName("Wins").item(0).getTextContent());
                int losses = Integer.parseInt(playerElement.getElementsByTagName("Losses").item(0).getTextContent());

                Player player = new Player(nickname, password, nationality, age, photo);
                player.setWins(wins);
                player.setLosses(losses);
                player.setPreferredColor(color);

                NodeList playTimeNodes = playerElement.getElementsByTagName("Game");
                for (int j = 0; j < playTimeNodes.getLength(); j++) {
                    Element gameElement = (Element) playTimeNodes.item(j);
                    String formattedTime = gameElement.getTextContent();
                    long playTimeMillis = player.convertFormattedPlayTimeToMillis(formattedTime);
                    player.addPlayTime(playTimeMillis);

                    String uuid = gameElement.getAttribute("id");
                    player.addGameUUID(uuid);
                }

                getPlayers().put(nickname, player);
            }

            System.out.println("DEBUG: Jogadores carregados do XML: " + getPlayers().keySet());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    //MÉTODOS NOVOS PARA CRIAR CÓPIAS DE SEGURANÇA DO QUADRO DE HONRA (RANKING)
    
    /**
     * Guarda o quadro de honra (ranking) dos jogadores num ficheiro XML de backup.
     * O ranking é ordenado por número de vitórias (decrescente) e tempo médio de jogo (crescente).
     * O ficheiro é guardado por omissão em user.home/GoBangData/ranking_backup.xml.
     */
    public static void saveRankingToXML() {
        try {
            // Caminho por omissão
            String userHome = System.getProperty("user.home");
            String filePath = userHome + "/GoBangData/ranking_backup.xml";

            // 1. Obter lista de jogadores ordenada como em ranking.jsp
            List<Player> ranking = new ArrayList<>(getPlayers().values());
            ranking.sort((a, b) -> {
                int cmp = Integer.compare(b.getWins(), a.getWins());
                if (cmp == 0) {
                    double ta = a.getPlayTimes().stream().mapToLong(Long::longValue).average().orElse(0);
                    double tb = b.getPlayTimes().stream().mapToLong(Long::longValue).average().orElse(0);
                    return Double.compare(ta, tb);
                }
                return cmp;
            });

            // 2. Criar documento XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("Ranking");
            doc.appendChild(root);

            int pos = 1;
            for (Player player : ranking) {
                Element playerElement = doc.createElement("Player");

                Element position = doc.createElement("Position");
                position.appendChild(doc.createTextNode(String.valueOf(pos++)));
                playerElement.appendChild(position);

                Element nickname = doc.createElement("Nickname");
                nickname.appendChild(doc.createTextNode(player.getNickname()));
                playerElement.appendChild(nickname);

                Element nationality = doc.createElement("Nationality");
                nationality.appendChild(doc.createTextNode(player.getNationality()));
                playerElement.appendChild(nationality);

                Element wins = doc.createElement("Wins");
                wins.appendChild(doc.createTextNode(String.valueOf(player.getWins())));
                playerElement.appendChild(wins);

                Element avgTime = doc.createElement("AverageTime");
                double avg = player.getPlayTimes().stream().mapToLong(Long::longValue).average().orElse(0);
                avgTime.appendChild(doc.createTextNode(String.format("%.1f", avg / 1000)));
                playerElement.appendChild(avgTime);

                root.appendChild(playerElement);
            }

            // 3. Guardar o ficheiro XML
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);

            File xmlFile = new File(filePath);
            File parentDir = xmlFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            StreamResult result = new StreamResult(xmlFile);
            transformer.transform(source, result);

            System.out.println("Ranking guardado em " + filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Lê o quadro de honra (ranking) do ficheiro XML de backup em user.home/GoBangData/ranking_backup.xml.
     * Devolve uma lista de mapas com os dados de cada jogador do ranking.
     * 
     * @return Lista de mapas (cada mapa representa um jogador do ranking)
     */
    public static List<Map<String, String>> loadRankingFromXML() {
        List<Map<String, String>> ranking = new ArrayList<>();
        try {
            String userHome = System.getProperty("user.home");
            String filePath = userHome + "/GoBangData/ranking_backup.xml";
            File file = new File(filePath);
            if (!file.exists()) return ranking;

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList nodeList = doc.getElementsByTagName("Player");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element playerElement = (Element) nodeList.item(i);
                Map<String, String> playerData = new HashMap<>();
                playerData.put("Position", playerElement.getElementsByTagName("Position").item(0).getTextContent());
                playerData.put("Nickname", playerElement.getElementsByTagName("Nickname").item(0).getTextContent());
                playerData.put("Nationality", playerElement.getElementsByTagName("Nationality").item(0).getTextContent());
                playerData.put("Wins", playerElement.getElementsByTagName("Wins").item(0).getTextContent());
                playerData.put("AverageTime", playerElement.getElementsByTagName("AverageTime").item(0).getTextContent());
                ranking.add(playerData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ranking;
    }
    
    /**
     * Notifica um jogador específico com uma mensagem.
     * Procura o ClientHandler correspondente e envia a mensagem.
     * 
     * Ligações:
     * - Usado por ClientHandler (ex: handleMove, handleSurrender, disconnectClient).
     */
    public static void notifyPlayer(Player player, String message) {
        for (ClientHandler client : getClients()) {
            if (client.getPlayer() != null && client.getPlayer().equals(player)) {
                client.sendMessage(message);
                break;
            }
        }
    }
    
    /**
     * Notifica dois jogadores com mensagens personalizadas.
     * Usado para enviar mensagens diferentes a cada jogador (ex: resultado da jogada).
     * 
     * Ligações:
     * - Usado por ClientHandler.handleMove(), handleStartGame(), etc.
     */
    public static void notifyPlayers(Player player1, Player player2, String messagePlayer1, String messagePlayer2) {
        // Cria uma cópia para evitar ConcurrentModificationException
        List<ClientHandler> clientsCopy = new ArrayList<>(getClients());
        for (ClientHandler client : clientsCopy) {
            if (client.getPlayer() != null) {
                if (client.getPlayer().equals(player1)) {
                    client.sendMessage(messagePlayer1);
                } else if (client.getPlayer().equals(player2)) {
                    client.sendMessage(messagePlayer2);
                }
            }
        }
    }

    public static List<ClientHandler> getClients() {
		return clients;
	}

	public static Map<String, Player> getPlayers() {
		return players;
	}
	
	public static SessionManager getSession() { return sessionManager; }
	
	public static Map<String, String> getLastGameResults() { return lastGameResults; }

	public static boolean isShuttingDown() {
		return isShuttingDown;
	}

	public static void setShuttingDown(boolean isShuttingDown) {
		GoBangServer.isShuttingDown = isShuttingDown;
	}
	
	/**
     * Encerra o servidor de forma controlada.
     * Fecha o ServerSocket, guarda jogadores e termina o processo.
     * 
     * Ligações:
     * - Chamado por ClientHandler.shutdown().
     */
	public static void shutdownServer() {
        isShuttingDown = true;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println(CommandProtocol.formatMessage("Erro ao fechar ServerSocket: " + e.getMessage()));
        }
        savePlayersToXML();
        System.exit(0);
    }
}
/*
UTILIZAÇÕES E LIGAÇÕES:
- GoBangServer é o ponto central do servidor.
- ClientHandler: cada ligação de cliente é gerida por um ClientHandler, que usa GoBangServer para aceder a jogadores, sessões, notificações, etc.
- Player: representa cada utilizador registado, guardado/carregado em XML.
- SessionManager: gere jogos ativos, fila de espera, etc.
- CommandProtocol: usado para formatar mensagens de log e protocolo.
- Métodos como notifyPlayer, notifyPlayers são usados para comunicação entre threads/handlers.
- lastGameResults é usado para polling de resultados finais (ex: "error:game_over" ou "FIM DE JOGO! Ganhaste!").
*/