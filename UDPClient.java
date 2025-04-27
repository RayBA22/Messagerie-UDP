import java.net.*;
import java.util.Scanner;

public class UDPClient {
    private static final String SERVER_IP = "127.0.0.1"; // Adresse du serveur
    private static final int SERVER_PORT = 12345; // Port du serveur
    private static DatagramSocket clientSocket;
    private static String clientName;
    private static boolean isRunning = true;  // Flag pour gérer l'arrêt du client

    public static void main(String[] args) {
        try {
            // Créer un socket pour le client
            clientSocket = new DatagramSocket();
            Scanner scanner = new Scanner(System.in);

            // Demander le nom du client
            System.out.print("Entrez votre nom de client : ");
            clientName = scanner.nextLine();

            // Envoyer le nom du client au serveur
            sendMessage(clientName);

            // Thread pour recevoir les messages du serveur
            Thread receiveThread = new Thread(() -> {
                try {
                    byte[] receiveBuffer = new byte[1024];
                    while (isRunning) {
                        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        clientSocket.receive(receivePacket);

                        String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        System.out.println(receivedMessage);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            receiveThread.start();

            // Thread pour envoyer les messages au serveur
            while (true) {
                System.out.print(" Entrez votre message (ou @nomClient message pour parler à un autre client) : ");
                String message = scanner.nextLine();

                 // Si le client veut se déconnecter
                if (message.equals("#exit")) {
                    sendMessage("#exit");
                    System.out.println("Déconnexion du serveur...");
                    clientSocket.close();  // Fermer le socket
                    isRunning = false;  // Arrêter le thread de réception
                    break;  // Sortir de la boucle et terminer le programme
                }

                // Envoyer le message au serveur
                sendMessage(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Méthode pour envoyer des messages
    private static void sendMessage(String message) {
        try {
            byte[] sendData = message.getBytes();
            InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
            clientSocket.send(sendPacket);
            System.out.println(" Message envoyé !");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
