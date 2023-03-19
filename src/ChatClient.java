import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatClient {

    private JFrame frame; // declare a JFrame instance variable called frame
    private JTextArea chatArea; // declare a JTextArea instance variable called chatArea
    private JTextField messageField; // declare a JTextField instance variable called messageField
    private JList<String> clientList; // declare a JList instance variable called clientList
    private DefaultListModel<String> clientListModel; // declare a DefaultListModel instance variable called clientListModel
    private PrintWriter output; // declare a PrintWriter instance variable called output
    private String username; // declare a String instance variable called username

    public ChatClient() {
        // Prompt the user for a username
        username = JOptionPane.showInputDialog(frame, "Enter username:", "Username", JOptionPane.PLAIN_MESSAGE);

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
        clientListModel = new DefaultListModel<String>(); // create a new DefaultListModel instance called clientListModel
        clientList = new JList<String>(clientListModel); // create a new JList instance called clientList and pass the DefaultListModel as an argument
        JScrollPane clientScrollPane = new JScrollPane(clientList); // create a new JScrollPane instance called clientScrollPane and add the JList to it
        clientScrollPane.setPreferredSize(new Dimension(150, 0));
        panel.add(clientScrollPane, BorderLayout.EAST);
        frame.add(panel); // add the panel to the JFrame
        frame.setVisible(true); // set the JFrame to be visible

        // Connect to the server and start listening for messages
        try {
            Socket socket = new Socket("localhost", 9000); // Connect to the server on localhost:9000
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            output.println(username); // Send the username to the server
            new Thread(() -> {
                try {
                    while (true) {
                        String message = input.readLine(); // Read messages from the server
                        if (message == null) {
                            break;
                        }
                        updateChatArea(message); // Update the chat area with the received message
                    }
                } catch (IOException e) {
                    System.out.println("Error receiving message from server: " + e.getMessage());
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        System.out.println("Error closing socket: " + e.getMessage());
                    }
                    System.exit(0);
                }
            }).start(); // Start a new thread to handle incoming messages
        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
            JOptionPane.showMessageDialog(frame, "Error connecting to server. Exiting...", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }


    private void updateChatArea(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength()); // Scroll to the bottom of the chat area
        });
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