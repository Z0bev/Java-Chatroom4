import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ChatServer extends JFrame implements ActionListener {

    private JTextArea chatArea;
    private JTextField chatInput;
    private JButton sendButton;
    private JList<String> clientList;
    private DefaultListModel<String> clientListModel;

    private ServerSocket serverSocket;
    private Thread serverThread;
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

        setSize(600, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sendButton) {
            String message = chatInput.getText().trim();
            if (!message.equals("")) {
                broadcastMessage("Server", message);
                chatInput.setText("");
            }
        }
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(9000);
            running = true;
            while (running) {
                Socket clientSocket = serverSocket.accept();
                ClientThread clientThread = new ClientThread(clientSocket);
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void addClient(String clientName) {
        clientListModel.addElement(clientName);
    }

    private synchronized void removeClient(String clientName) {
        clientListModel.removeElement(clientName);
    }

    private synchronized void broadcastMessage(String sender, String message) {
        chatArea.append(sender + ": " + message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
        for (int i = 0; i < clientListModel.size(); i++) {
            String clientName = clientListModel.getElementAt(i);
            if (!clientName.equals("Server")) {
                ClientThread clientThread = (ClientThread) Thread.currentThread();
                if (!clientThread.clientName.equals(clientName)) {
                    clientThread.sendMessage(sender, message);
                }
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
                    lastActiveTime = System.currentTimeMillis();

                    if (clientName.equals("")) {
                        clientName = line.trim();
                        addClient(clientName);
                        broadcastMessage("Server", clientName + " has joined the chatroom.");
                    } else {
                        broadcastMessage(clientName, line);
                    }
                }

                removeClient(clientName);
                broadcastMessage("Server", clientName + " has left the chatroom.");
                clientSocket.close();
            } catch (IOException e) {
                removeClient(clientName);
                broadcastMessage("Server", clientName + " has left the chatroom.");
            }
        }

        public synchronized void sendMessage(String sender, String message) {
            output.println(sender + ": " + message);
        }


    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.startServer();
    }
}
