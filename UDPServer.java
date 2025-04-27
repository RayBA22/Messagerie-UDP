import java.net.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class UDPServer {
    private static final int SERVER_PORT = 12345; // Port de base du serveur
    private static DatagramSocket serverSocket;
    private static ConcurrentHashMap<String, InetSocketAddress> clients = new ConcurrentHashMap<>();
    private static Set<Integer> usedPorts = new HashSet<>(); // Ensemble pour garder une trace des ports utilisés


    public static void main(String[] args) {
        try {
            // Initialisation du socket serveur
            serverSocket = new DatagramSocket(SERVER_PORT);
            System.out.println("Serveur UDP en attente sur le port " + SERVER_PORT + "...");

            // Gestion des clients dans un thread séparé
            while (true) {
                byte[] receiveBuffer = new byte[1024];
                DatagramPacket receivedPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivedPacket);

                // Récupérer les infos du client
                InetAddress clientAddress = receivedPacket.getAddress();
                int clientPort = receivedPacket.getPort();
                InetSocketAddress clientSocket = new InetSocketAddress(clientAddress, clientPort);

                // Récupérer le message du client
                String message = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
                
              

                 // Vérifier si c'est une première connexion (nom du client)
                 if (!clients.containsKey(message) && !clients.containsValue(clientSocket)) {
                    // Attribuer un port unique à ce client
                    int portForClient = getAvailablePort();
                    if (portForClient != -1) {
                        clients.put(message, clientSocket); 
                        usedPorts.add(portForClient);
                        System.out.println(message + " connecté depuis " + clientSocket + " avec le port " + portForClient);
                    }
                    continue; // On passe à la boucle suivante sans envoyer de réponse
                }

                // Créer un thread pour gérer ce client
                new Thread(() -> handleClientMessage(message, clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static int getAvailablePort() {
        // Scanner des ports disponibles et les retourner
        for (int port = 12346; port < 65535; port++) {
            if (!usedPorts.contains(port)) {
                usedPorts.add(port); // Ajouter ce port à la liste des ports utilisés
                return port;
            }
        }
        return -1; // Aucun port disponible
    }

    private static void handleClientMessage(String message, InetSocketAddress senderSocket) {
        try {
            // Récupérer le nom du client en fonction de l'adresse
            String senderName = getClientName(senderSocket);
            
            // Si le message est "#exit", déconnecter le client
            if (message.equals("#exit")) {
                clients.remove(senderName);  // Supprimer le client de la HashMap
                System.out.println(senderName + " a quitté le serveur.");
                return;  // On quitte la fonction après avoir supprimé le client
            }
            
            // Préparer le message à renvoyer a tous les clients si c'est un message normal 
            String messageToSend = "Message reçu de " + senderName + ": " + message;
    
            // Vérifier si c'est un message destiné à un autre client
            if (message.startsWith("@")) {
                String[] parts = message.split(" ", 2);
                if (parts.length < 2) return;
    
                String targetName = parts[0].substring(1); // @NomClient
                String msgToSend = "Message reçu de " + senderName + ": " + parts[1]; // Formatage du message avec le nom de l'expéditeur
    
                // Vérifier si le destinataire existe
                if (clients.containsKey(targetName)) {
                    InetSocketAddress targetSocket = clients.get(targetName);
                    sendMessage(msgToSend, targetSocket);  // Envoie le message formaté
                    System.out.println("Message relayé à " + targetName);
                } else {
                    System.out.println("Client " + targetName + " non trouvé.");
                }
            } else {
                // Si c'est un message normal, envoyer le message aux clients
                broadcastMessage(messageToSend, senderSocket);
            }
    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   private static String getClientName(InetSocketAddress clientSocket) {
        // Recherche de l'adresse pour obtenir le nom du client
        for (Map.Entry<String, InetSocketAddress> entry : clients.entrySet()) {
            if (entry.getValue().equals(clientSocket)) {
                return entry.getKey(); // Retourner le nom du client
            }
        }
        return "Inconnu"; // Si l'adresse n'est pas trouvée, renvoie "Inconnu"
    }

    private static void sendMessage(String message, InetSocketAddress targetSocket) {
        try {
            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                sendData, sendData.length, targetSocket.getAddress(), targetSocket.getPort()
            );
            serverSocket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void broadcastMessage(String message, InetSocketAddress senderSocket) {
        for (Map.Entry<String, InetSocketAddress> entry : clients.entrySet()) {
            // On vérifie que ce n'est pas l'expéditeur
            if (!entry.getValue().equals(senderSocket)) {
                sendMessage(message, entry.getValue());
            }
        }
    }
}
