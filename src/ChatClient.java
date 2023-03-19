import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatClient {

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JList<String> clientList;
    private DefaultListModel<String> clientListModel;
    private PrintWriter output;
    private String username;

    public ChatClient() {
        // Prompt the user for a username
        username = JOptionPane.showInputDialog(frame, "Enter username:", "Username", JOptionPane.PLAIN_MESSAGE);

        // Create the GUI components
        frame = new JFrame(username);
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
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage(messageField.getText());
                messageField.setText("");
            }
        });
        messagePanel.add(sendButton, BorderLayout.EAST);
        panel.add(messagePanel, BorderLayout.SOUTH);
        clientListModel = new DefaultListModel<String>();
        clientList = new JList<String>(clientListModel);
        JScrollPane clientScrollPane = new JScrollPane(clientList);
        clientScrollPane.setPreferredSize(new Dimension(150, 0));
        panel.add(clientScrollPane, BorderLayout.EAST);
        frame.add(panel);
        frame.setVisible(true);

        // Connect to the server and start listening for messages
        try {
            Socket socket = new Socket("localhost", 9000);
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            output.println(username);
            new Thread(() -> {
                try {
                    while (true) {
                        String message = input.readLine();
                        if (message == null) {
                            break;
                        }
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
            }).start();
        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
            JOptionPane.showMessageDialog(frame, "Error connecting to server. Exiting...", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void updateChatArea(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void sendMessage(String message) {
        if (!message.isEmpty()) {
            output.println(message);
        }
    }


    public static void main(String[] args) {
        new ChatClient();
    }
}