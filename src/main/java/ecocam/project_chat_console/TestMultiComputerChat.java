package ecocam.project_chat_console;

import ecocam.project_chat_console.client.ChatClient;
import ecocam.project_chat_console.model.Message;

import java.util.Scanner;

public class TestMultiComputerChat {
    public static void main(String[] args) {
        System.out.println("Multi-Computer Chat Test Client");
        System.out.println("================================");
        
        Scanner scanner = new Scanner(System.in);
        
        // Get server details
        System.out.print("Enter server IP address (or 'localhost' for local testing): ");
        String serverIP = scanner.nextLine().trim();
        if (serverIP.isEmpty()) {
            serverIP = "localhost";
        }
        
        System.out.print("Enter server port (default 5000): ");
        String portInput = scanner.nextLine().trim();
        int port = 5000;
        if (!portInput.isEmpty()) {
            try {
                port = Integer.parseInt(portInput);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port, using default 5000");
            }
        }
        
        // Get user credentials
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            username = "testuser";
        }
        
        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();
        if (password.isEmpty()) {
            password = "password";
        }
        
        System.out.println("\nConnecting to " + serverIP + ":" + port + "...");
        
        // Create and configure client
        ChatClient client = new ChatClient(serverIP, port);
        
        // Add message listener
        client.addMessageListener(message -> {
            System.out.println("[" + message.getSender() + "]: " + message.getContent());
        });
        
        // Add error listener
        client.addErrorListener(error -> {
            System.out.println("ERROR: " + error);
        });
        
        // Connect to server
        if (client.connect()) {
            System.out.println("Connected to server successfully!");
            
            // Login
            client.login(username, password);
            
            System.out.println("\nChat started! Type your messages below (type 'quit' to exit):");
            System.out.println("============================================================");
            
            // Chat loop
            String input;
            while (true) {
                System.out.print("> ");
                input = scanner.nextLine();
                
                if ("quit".equalsIgnoreCase(input)) {
                    break;
                }
                
                if (!input.trim().isEmpty()) {
                    Message msg = new Message("ConsoleUser", input, Message.MessageType.TEXT);
                    client.sendMessage(msg, "Public");
                }
            }
            
            client.disconnect();
            System.out.println("Disconnected from server.");
        } else {
            System.out.println("Failed to connect to server at " + serverIP + ":" + port);
        }
        
        scanner.close();
    }
}