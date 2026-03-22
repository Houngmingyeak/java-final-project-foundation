package ecocam.project_chat_console.client;

import ecocam.project_chat_console.model.Command;
import ecocam.project_chat_console.model.Group;
import ecocam.project_chat_console.model.Message;
import ecocam.project_chat_console.model.User;
import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ChatClient TCP & UDP Discovery - Fixed for Full Image & File Support
 */
public class ChatClient {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;
    private static final int DISCOVERY_PORT = 8888;

    private String host;
    private int port;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private volatile boolean connected;
    private String username;

    // Listeners
    private final List<Consumer<Message>> messageListeners = new ArrayList<>();
    private final List<Consumer<List<String>>> userListListeners = new ArrayList<>();
    private final List<Consumer<List<Group>>> groupListListeners = new ArrayList<>();
    private final List<Consumer<List<Group>>> availableGroupsListeners = new ArrayList<>();
    private final List<Consumer<String>> typingListeners = new ArrayList<>();
    private final List<Consumer<Boolean>> connectionListeners = new ArrayList<>();
    private final List<Consumer<String>> errorListeners = new ArrayList<>();
    private final List<Consumer<Object[]>> groupMembersListeners = new ArrayList<>();
    private final List<Consumer<String>> successListeners = new ArrayList<>();

    public ChatClient() { this(DEFAULT_HOST, DEFAULT_PORT); }

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setHost(String host) { this.host = host; }
    public void setPort(int port) { this.port = port; }

    // -------------------- AUTO-DISCOVERY (UDP) --------------------

    /**
     * ស្វែងរក IP របស់ Server ក្នុងបណ្តាញ LAN ដោយស្វ័យប្រវត្តិ
     */
    public String discoverServerIP() {
        String requestMsg = "DISCOVER_HYPERCHAT_SERVER";
        String responseExpected = "HYPERCHAT_SERVER_HERE";

        try (DatagramSocket udpSocket = new DatagramSocket()) {
            udpSocket.setBroadcast(true);
            udpSocket.setSoTimeout(3000); // រង់ចាំចម្លើយ ៣ វិនាទី

            // ១. បាញ់សារសួរទៅគ្រប់ម៉ាស៊ីនក្នុងបណ្តាញ
            byte[] sendData = requestMsg.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                    sendData, sendData.length,
                    InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT
            );
            udpSocket.send(sendPacket);

            // ២. ចាំទទួលចម្លើយពី Server
            byte[] buf = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
            udpSocket.receive(receivePacket);

            String response = new String(receivePacket.getData()).trim();
            if (response.equals(responseExpected)) {
                return receivePacket.getAddress().getHostAddress();
            }
        } catch (IOException e) {
            System.err.println("Auto-Discovery: Server not found or timeout.");
        }
        return null;
    }

    // -------------------- CONNECT/DISCONNECT --------------------

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            // សំខាន់៖ បង្កើត Output មុន Input ដើម្បីការពារ Deadlock
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
            connected = true;

            Thread receiveThread = new Thread(this::receiveLoop);
            receiveThread.setDaemon(true);
            receiveThread.start();

            notifyConnectionListeners(true);
            return true;
        } catch (IOException e) {
            notifyErrorListeners("Failed to connect: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        connected = false;
        if (username != null) sendCommand(new Command(Command.CommandType.LOGOUT, null, username));
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        notifyConnectionListeners(false);
    }

    // -------------------- RECEIVE LOOP --------------------

    private void receiveLoop() {
        while (connected) {
            try {
                Object received = input.readObject();
                if (received instanceof Message) {
                    handleMessage((Message) received);
                }
            } catch (IOException | ClassNotFoundException e) {
                if (connected) {
                    notifyErrorListeners("Connection lost");
                    disconnect();
                }
                break;
            }
        }
    }

    private void handleMessage(Message message) {
        if (message == null) return;
        switch (message.getType()) {
            case USER_LIST -> notifyUserListListeners(parseUserList(message.getContent()));
            case TYPING_INDICATOR -> notifyTypingListeners(message.getSender());
            case SYSTEM -> handleSystemMessage(message);
            default -> notifyMessageListeners(message);
        }
    }

    // -------------------- SEND METHODS --------------------

    public void sendMessage(Message msg, String mode, int groupId, String recipient) {
        if (!connected || msg == null) {
            notifyErrorListeners("Not connected to server");
            return;
        }

        if ("GROUP".equalsIgnoreCase(mode)) {
            msg.setGroupId(groupId);
        } else if ("PRIVATE".equalsIgnoreCase(mode)) {
            msg.setRecipient(recipient);
        }

        sendObject(msg);
    }

    public void sendMessage(Message msg, String mode) {
        sendMessage(msg, mode, 0, null);
    }

    public synchronized void sendObject(Object obj) {
        try {
            if (output != null && connected) {
                output.writeObject(obj);
                output.flush();
                output.reset(); // សំខាន់បំផុត៖ បង្ខំឱ្យផ្ញើទិន្នន័យថ្មី (រូបភាព) ជានិច្ច
            }
        } catch (IOException e) {
            notifyErrorListeners("Send failed: " + e.getMessage());
        }
    }

    public void sendCommand(Command cmd) { sendObject(cmd); }

    public void login(String user, String pass) {
        this.username = user;
        sendCommand(new Command(Command.CommandType.LOGIN, new User(user, pass, null)));
    }

    public void register(String user, String display, String pass) {
        sendCommand(new Command(Command.CommandType.REGISTER, new User(user, display, pass)));
    }

    public void requestUserList() {
        sendCommand(new Command(Command.CommandType.GET_USERS, null, username));
    }

    // -------------------- HELPERS & LISTENERS --------------------

    private void handleSystemMessage(Message message) {
        String content = message.getContent();
        if (content != null && content.startsWith("SUCCESS:")) {
            if (content.contains("Login successful")) {
                notifyConnectionListeners(true);
                notifySuccessListeners("Welcome " + username);
            }
        } else if (content != null && content.startsWith("ERROR:")) {
            notifyErrorListeners(content.substring(6).trim());
        }
        notifyMessageListeners(message);
    }

    private List<String> parseUserList(String content) {
        List<String> users = new ArrayList<>();
        if (content != null && !content.isEmpty()) {
            for (String u : content.split(",")) users.add(u.trim());
        }
        return users;
    }

    // Listener Registry
    public void addMessageListener(Consumer<Message> l) { messageListeners.add(l); }
    public void addUserListListener(Consumer<List<String>> l) { userListListeners.add(l); }
    public void addGroupListListener(Consumer<List<Group>> l) { groupListListeners.add(l); }
    public void addAvailableGroupsListener(Consumer<List<Group>> l) { availableGroupsListeners.add(l); }
    public void addTypingListener(Consumer<String> l) { typingListeners.add(l); }
    public void addConnectionListener(Consumer<Boolean> l) { connectionListeners.add(l); }
    public void addErrorListener(Consumer<String> l) { errorListeners.add(l); }
    public void addSuccessListener(Consumer<String> l) { successListeners.add(l); }
    public void addGroupMembersListener(Consumer<Object[]> l) { groupMembersListeners.add(l); }

    // UI-Safe Notifications
    private void notifyMessageListeners(Message m) { Platform.runLater(() -> messageListeners.forEach(l -> l.accept(m))); }
    private void notifyConnectionListeners(boolean c) { Platform.runLater(() -> connectionListeners.forEach(l -> l.accept(c))); }
    private void notifyErrorListeners(String e) { Platform.runLater(() -> errorListeners.forEach(l -> l.accept(e))); }
    private void notifySuccessListeners(String s) { Platform.runLater(() -> successListeners.forEach(l -> l.accept(s))); }
    private void notifyUserListListeners(List<String> u) { Platform.runLater(() -> userListListeners.forEach(l -> l.accept(u))); }
    private void notifyGroupListListeners(List<Group> g) { Platform.runLater(() -> groupListListeners.forEach(l -> l.accept(g))); }
    private void notifyAvailableGroupsListeners(List<Group> g) { Platform.runLater(() -> availableGroupsListeners.forEach(l -> l.accept(g))); }
    private void notifyTypingListeners(String user) { Platform.runLater(() -> typingListeners.forEach(l -> l.accept(user))); }
    private void notifyGroupMembersListeners(Object[] d) { Platform.runLater(() -> groupMembersListeners.forEach(l -> l.accept(d))); }

    public String getUsername() { return username; }
    public boolean isConnected() { return connected; }
}