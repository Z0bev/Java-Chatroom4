import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Timer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatClient {


    private static final int AFK_TIMEOUT = 120000;
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JList<String> clientList;
    private DefaultListModel<String> clientListModel;
    private PrintWriter output;
    private String username;
    private UUID clientId;


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
                sendMessage(messageField.getText()); // call the sendMessage method with the text from the JTextField as an argument
                messageField.setText(""); // clear the text from the JTextField
            });
            messagePanel.add(sendButton, BorderLayout.EAST);
            panel.add(messagePanel, BorderLayout.SOUTH);
            frame.add(panel); // add the panel to the JFrame
            frame.setVisible(true); // set the JFrame to be visible

            // Connect to the server and start listening for messages
            try {
                Socket socket = new Socket("localhost", 9000); // Connect to the server on localhost:9000
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println(username); // Send the username to the server
                output.println(clientId);//print out UUID
            } catch (IOException e) {
                System.out.println("Error connecting to server: " + e.getMessage());
                JOptionPane.showMessageDialog(frame, "Error connecting to server. Exiting...", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }
    }
    // Method to send a message to the server
    private void sendMessage(String message) {
        if (!message.isEmpty()) {
            output.println(message);
        }
    }



    // Main method to start the chat client
    public static void main(String[] args) {
        new ChatClient();
    }
}