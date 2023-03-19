import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ChatServer extends JFrame implements ActionListener {

    // GUI elements
    private JTextArea chatArea;
    private JTextField chatInput;
    private JButton sendButton;
    private JList<String> clientList;
    private DefaultListModel<String> clientListModel;

    // Server elements
    private ServerSocket serverSocket;
    private Thread serverThread;
    private boolean running;

    public ChatServer() {
        super("Java Chatroom Server");

        // Set up GUI
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatArea = new JTextArea(10, 30); // create a text area for displaying messages
        chatArea.setEditable(false); // disable editing of the text area
        chatArea.setLineWrap(true); // enable line wrapping in the text area
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER); // add the text area to a scroll pane and add the pane to the chat panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        JPanel clientPanel = new JPanel(new BorderLayout());
        JLabel clientLabel = new JLabel("Connected Clients"); // create a label for the client list
        clientPanel.add(clientLabel, BorderLayout.NORTH); // add the label to the client panel
        clientListModel = new DefaultListModel<String>();
        clientList = new JList<String>(clientListModel); // create a list for displaying connected clients
        clientPanel.add(new JScrollPane(clientList), BorderLayout.CENTER); // add the client list to a scroll pane and add the pane to the client panel
        chatPanel.add(clientPanel, BorderLayout.EAST); // add the client panel to the right side of the chat panel
        getContentPane().add(chatPanel); // add the chat panel to the content pane of the JFrame

        // Set up server
        running = false;
        serverSocket = null;

        setSize(600, 300); // set the size of the JFrame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // set the close operation of the JFrame
        setVisible(true); // make the JFrame visible
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sendButton) { // check if the send button was clicked
            String message = chatInput.getText().trim(); // get the text from the input field and remove leading and trailing whitespace
            if (!message.equals("")) { // check if the message is not empty
                broadcastMessage("Server", message); // broadcast the message to all connected clients
                chatInput.setText(""); // clear the input field
            }
        }
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
        for (int i = 0; i < clientListModel.size(); i++) { // loop through all the clients
            String clientName = clientListModel.getElementAt(i); // get the name of the current client
            ClientThread clientThread = (ClientThread) Thread.currentThread(); // get the current client thread
            if (!clientThread.clientName.equals(clientName)) { // make sure the message is not sent to the client who sent it
                    clientThread.sendMessage(sender, message); // send the message
            }
        }
    }

    private class ClientThread extends Thread {
        private Socket clientSocket;
        private String clientName;
        private BufferedReader input;
        private PrintWriter output;
        private long lastActiveTime;

        public ClientThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            this.clientName = "";
            this.lastActiveTime = System.currentTimeMillis();
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

        public synchronized void sendMessage(String sender, String message) {
            output.println(sender + ": " + message);
        }


    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.startServer(); // start the server
    }
}
