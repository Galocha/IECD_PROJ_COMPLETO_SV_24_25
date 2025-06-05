package server;

import client.ClientHandler;
import game.Game;
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

public class GoBangServer {
    private static final int PORT = 1234;
    private static Map<String, Player> players = new HashMap<>();
    private static List<ClientHandler> clients = new ArrayList<>();
    private static SessionManager sessionManager = new SessionManager();
    private static volatile boolean isShuttingDown = false; //volatile - indicado para variáveis que são utilizadas/modificadas entre threads
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
    	try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Servidor iniciado na porta " + PORT);
            loadPlayersFromXML();
            
            while (!isShuttingDown) {
                try {
                    Socket socket = serverSocket.accept();
	                System.out.println(CommandProtocol.formatMessage("Novo cliente conectado: " + socket.getInetAddress().getHostAddress()));
	                ClientHandler clientHandler = new ClientHandler(socket);
	                getClients().add(clientHandler);
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
            savePlayersToXML();
        }
    }

    public static void savePlayersToXML() {
        try {
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
                    playerElement.appendChild(playTime); // Apenas adiciona se houver conteúdos
                }

                root.appendChild(playerElement);
            }
            
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new File("src/main/java/server/players.xsd"));
            Validator validator = schema.newValidator();
            validator.validate(new DOMSource(doc));

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("src/main/java/server/players.xml"));
            transformer.transform(source, result);

            System.out.println("Jogadores guardados em players.xml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadPlayersFromXML() {
        try {
            File file = new File("src/main/java/server/players.xml");
            if (!file.exists()) return;
            
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new File("src/main/java/server/players.xsd"));
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

                // Processa tempos de jogo individuais
                NodeList playTimeNodes = playerElement.getElementsByTagName("Game");
                for (int j = 0; j < playTimeNodes.getLength(); j++) {
                    Element gameElement = (Element) playTimeNodes.item(j);
                    String formattedTime = gameElement.getTextContent();
                    long playTimeMillis = player.convertFormattedPlayTimeToMillis(formattedTime);
                    player.addPlayTime(playTimeMillis); // Adiciona tempo à lista do jogador
                    
                    // Lê UUID do atributo "id"
                    String uuid = gameElement.getAttribute("id");
                    player.addGameUUID(uuid);
                }

                getPlayers().put(nickname, player);
            }

            System.out.println("Jogadores carregados de players.xml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static Map<String, Player> parsePlayersFromXML(File xmlFile, File xsdFile) { //criado para utilizar no login.jsp
        Map<String, Player> players = new HashMap<>();
        try {
            if (!xmlFile.exists()) return players;

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(xsdFile);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(xmlFile));

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);

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
                players.put(nickname, player);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return players;
    }
    
    public static void notifyPlayer(Player player, String message) {
        for (ClientHandler client : getClients()) {
            if (client.getPlayer() != null && client.getPlayer().equals(player)) {
                client.sendMessage(message);
                break;
            }
        }
    }
    
    public static void notifyPlayers(Player player1, Player player2, String messagePlayer1, String messagePlayer2) {
        for (ClientHandler client : getClients()) {
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

	public static boolean isShuttingDown() {
		return isShuttingDown;
	}

	public static void setShuttingDown(boolean isShuttingDown) {
		GoBangServer.isShuttingDown = isShuttingDown;
	}
	
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
