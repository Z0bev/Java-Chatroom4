import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ChatClient {

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private PrintWriter output;
    private BufferedReader input;
    private String username;
    private UUID clientId;
    private JComboBox<String> clientList;

    public ChatClient() {
        // Prompt the user for a username
        boolean validUsername = false;
        while (!validUsername) {
            username = JOptionPane.showInputDialog(frame, "Enter username:", "Username", JOptionPane.PLAIN_MESSAGE);
            Pattern p = Pattern.compile("^[a-zA-Z0-9!@#\\$%\\^\\&*\\)\\(+=._-]+$");
            Matcher m = p.matcher(username);
            if (!m.find()) {
                JOptionPane.showMessageDialog(null, "Please enter a valid username!");
            } else {
                validUsername = true;
            }
        }

        if (validUsername) {
            // Generate a UUID for the client
            clientId = UUID.randomUUID();

            // Create the GUI components
            frame = new JFrame(username); // create a new JFrame instance called frame and set the title to the username
            frame.setSize(600, 400);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JPanel panel = new JPanel(new BorderLayout());
            chatArea = new JTextArea();
            chatArea.setEditable(false);
            JScrollPane chatScrollPane = new JScrollPane(chatArea);
            panel.add(chatScrollPane, BorderLayout.CENTER);
            JPanel messagePanel = new JPanel(new BorderLayout());
            messageField = new JTextField();
            messagePanel.add(messageField, BorderLayout.CENTER);
            JButton sendButton = new JButton("Send");
            // add an ActionListener to the sendButton
            sendButton.addActionListener(e -> {
                String message = messageField.getText();
                if (!message.isEmpty()) {
                    if (message.startsWith("@")) {
                        String recipient = (String) clientList.getSelectedItem();
                        if (recipient != null) {
                            sendMessage(message, recipient);
                        }
                    } else {
                        sendMessage(message, "all");
                    }
                    messageField.setText(""); // clear the text from the JTextField
                }
            });
            messagePanel.add(sendButton, BorderLayout.EAST);
            // Add a JComboBox for selecting the recipient
            JPanel clientPanel = new JPanel();
            clientList = new JComboBox<>();
            clientPanel.add(new JLabel("Send to: "));
            clientPanel.add(clientList);
            messagePanel.add(clientPanel, BorderLayout.NORTH);
            panel.add(messagePanel, BorderLayout.SOUTH);
            frame.add(panel); // add the panel to the JFrame
            frame.setVisible(true); // set the JFrame to be visible
            panel.add(messagePanel, BorderLayout.SOUTH);
            frame.add(panel); // add the panel to the JFrame
            frame.setVisible(true); // set the JFrame to be visible
            // Connect to the server and start listening for messages
            try {
                Socket socket = new Socket("localhost", 9000); // Connect to the server on localhost:9000
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println(username); // Send the username to the server
                output.println(clientId); //print out UUID
                String activeClients = input.readLine();
                if (activeClients != null && !activeClients.isEmpty()) {
                    String[] clientNames = activeClients.split(",");
                    clientList.addItem("all");
                    for (String name : clientNames) {
                        if (!name.equals(username)) {
                            clientList.addItem(name);
                        }
                    }
                }

                // start a new thread to listen for messages from the server
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (true) {
                                String message = input.readLine();
                                if (message != null) {
                                    chatArea.append(message + "\n");
                                }
                            }
                        } catch (IOException e) {
                            System.out.println("Error reading input: " + e.getMessage());
                        }
                    }
                }).start();
            } catch (IOException e) {
                System.out.println("Error connecting to server: " + e.getMessage());
                JOptionPane.showMessageDialog(frame, "Error connecting to server. Exiting...", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }
    }

    // Method to send a message to the server
    private void sendMessage(String message, String recipient) {
        if (!message.isEmpty()) {
            output.println(message);
        }
    }

    // Main method to start the chat client
    public static void main(String[] args) {
        new ChatClient();
    }
}