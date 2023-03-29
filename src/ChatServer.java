import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatServer {

    private ArrayList<ClientHandler> clientHandlers;

    public ChatServer() {
        clientHandlers = new ArrayList<>();
    }
    // broadcast message method and commands
    private void broadcastMessage(String message, ClientHandler sender) {
        String senderMessage = message.split(":")[1].trim();
        if (senderMessage.startsWith("/")) {
            String command = (String) senderMessage.split(" ")[0].trim().substring(1);
            System.out.println(command + " command");
            if (command != null) {
                if (command.equals("quit")) {
                    clientHandlers.remove(sender);
                    broadcastClientsList();
                    sender.sendMessage("QUIT");
                    // close the client socket
                    try {
                        sender.socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                if (command.equals("list")) {
                    String clients = "ACTIVE USERS:";
                    for (ClientHandler client : clientHandlers) {
                        clients += " " + client.getUsername();
                    }
                    sender.sendMessage(clients);
                    return;
                }
            }
        }
        if (senderMessage.startsWith("@")) {
            String recipientUsername = (String) senderMessage.split(" ")[0].trim().substring(1);
            System.out.println(recipientUsername + " recipient");
            if (recipientUsername != null) {
                ClientHandler recipientHandler = clientHandlers.stream().filter(client -> client.getUsername().equals(recipientUsername)).findFirst().orElse(null);
                if (recipientHandler != null) {
                    recipientHandler.sendMessage(message);
                    sender.sendMessage(message);
                } else {
                    sender.sendMessage("User " + recipientUsername + " not found");
                }
            }
        } else {
            for (ClientHandler client : clientHandlers) {
                client.sendMessage(message);
            }
        }
    }
    
    private void broadcastClientsList() {
        String clients = "ACTIVE USERS:";
        for (ClientHandler client : clientHandlers) {
            clients += " " + client.getUsername();
        }
        for (ClientHandler client : clientHandlers) {
            client.sendMessage(clients);
        }
    }

    public void start(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Chat server started on port " + port);

            while (true) {
                System.out.println("Waiting for clients to connect...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from " + clientSocket.getInetAddress().getHostAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clientHandlers.add(clientHandler);
                Thread t = new Thread(clientHandler);
                t.start();
            }
        } catch (IOException e) {
            System.out.println("Error starting server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start(9000);
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader input;
        private PrintWriter output;
        private String username;
        private UUID clientId;
        private ChatServer server;

        public ClientHandler(Socket socket, ChatServer server) {
            this.socket = socket;
            this.server = server;
        }

        public String getUsername() {
            return username;
        }

        public void sendMessage(String message) {
            output.println(message);
        }

        public void run() {
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);

                // Prompt the user for a username
                boolean validUsername = false;
                while (!validUsername) {
                    username = input.readLine();
                    Pattern p = Pattern.compile("^[a-zA-Z0-9!@#\\$%\\^\\&*\\)\\(+=._-]+$");
                    Matcher m = p.matcher(username);
                    if (!m.find()) {
                        output.println("INVALID USERNAME");
                    } else {
                        validUsername = true;
                    }
                }

                // Generate a UUID for the client
                clientId = UUID.fromString(input.readLine());

                // Send a welcome message to the client
                output.println("Welcome to the chat, " + username + "!");
                output.println("Your ID is " + clientId);

                // Broadcast the clients list to all clients
                server.broadcastClientsList();

                String message;
                while ((message = input.readLine()) != null) {

                    server.broadcastMessage(username + ": " + message, this);
                }

                // Remove the client from the clientHandlers list
                clientHandlers.remove(this);

                // Broadcast the clients list to all clients
                server.broadcastClientsList();

                // Close the socket, input and output streams
                socket.close();
                input.close();
                output.close();
            } catch (IOException e) {
                System.out.println("Error handling client: " + e.getMessage());
            }
        }
    }
}
