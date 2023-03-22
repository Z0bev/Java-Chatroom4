import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.util.TimerTask;
import java.util.Timer;
import javax.swing.*;


public class ChatServer extends JFrame {

    // GUI elements
    private JTextArea chatArea;
    private JList<String> clientList;
    private DefaultListModel<String> clientListModel;

    // Server elements
    private ServerSocket serverSocket;
    private boolean running;

    public ChatServer() {
        super("Java Chatroom Server");

        // Set up GUI
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatArea = new JTextArea(10, 30);
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        JPanel inputPanel = new JPanel(new BorderLayout());
        JPanel clientPanel = new JPanel(new BorderLayout());
        JLabel clientLabel = new JLabel("Connected Clients");
        clientPanel.add(clientLabel, BorderLayout.NORTH);
        clientListModel = new DefaultListModel<String>();
        clientList = new JList<String>(clientListModel);
        clientPanel.add(new JScrollPane(clientList), BorderLayout.CENTER);
        chatPanel.add(clientPanel, BorderLayout.EAST);
        getContentPane().add(chatPanel);

        // Set up server
        running = false;
        serverSocket = null;

        setSize(600, 300); // set the size of the JFrame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // set the close operation of the JFrame
        setVisible(true); // make the JFrame visible
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(9000); // create a server socket
            running = true;
            while (running) { // continuously accept new client connections
                Socket clientSocket = serverSocket.accept(); // accept a new client connection
                ClientThread clientThread = new ClientThread(clientSocket); // create a new thread for the client
                clientThread.start(); // start the thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void addClient(String clientName) {
        clientListModel.addElement(clientName); // add a client to the client list
    }

    private synchronized void removeClient(String clientName) {
        clientListModel.removeElement(clientName); // remove a client from the client list
    }

    private synchronized void broadcastMessage(String sender, String message) {
        chatArea.append(sender + ": " + message + "\n"); // add the message to the chat area
        chatArea.setCaretPosition(chatArea.getDocument().getLength()); // set the chat area caret to the end of the text
    }

    private class ClientThread extends Thread {
        private Socket clientSocket;
        private String clientName;
        private BufferedReader input;
        private PrintWriter output;
        private long lastActiveTime;
        private Timer afkTimer;

        public ClientThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            this.clientName = "";
            this.lastActiveTime = System.currentTimeMillis();
            this.afkTimer = new Timer();
            startAFKTimer();
        }

        public void run() {
            try {
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                output = new PrintWriter(clientSocket.getOutputStream(), true);

                String line;
                while ((line = input.readLine()) != null) {
                    lastActiveTime = System.currentTimeMillis(); // update the last active time of the client
                    if (clientName.equals("")) { // if the client name is not set yet
                        clientName = line.trim(); // set the client name to the received input
                        addClient(clientName); // add the client to the client list
                        broadcastMessage("Server", clientName + " has joined the chatroom."); // broadcast a message to all clients
                    } else {
                        broadcastMessage(clientName, line); // broadcast the client's message to all clients
                    }
                }

                removeClient(clientName); // remove the client from the client list
                broadcastMessage("Server", clientName + " has left the chatroom."); // broadcast a message to all clients
                clientSocket.close(); // close the client socket

            } catch (IOException e) {
                removeClient(clientName); // remove the client from the client list
                broadcastMessage("Server", clientName + " has left the chatroom."); // broadcast a message to all clients
            }
        }

        private class AFKTimerTask extends TimerTask {
            public void run() {
                long currentTime = System.currentTimeMillis();
                long inactiveTime = currentTime - lastActiveTime;
                if (inactiveTime >= 120000) { // if the client has been inactive for more than 1 minute (60000 milliseconds)
                    try {
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        broadcastMessage("Server", clientName + " has been disconnected due to inactivity.");
                        clientSocket.close(); // close the client socket
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }


        private void startAFKTimer() {
            afkTimer.schedule(new AFKTimerTask(), 0, 30000); // start the AFK timer to check for inactivity every minute
        }
    }



    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.startServer(); // start the server
    }
}
