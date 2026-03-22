package ecocam.project_chat_console.server;

import ecocam.project_chat_console.model.Group;
import ecocam.project_chat_console.model.Message;
import ecocam.project_chat_console.service.DatabaseUserService;
import ecocam.project_chat_console.service.GroupService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private static final int DEFAULT_PORT = 5000;
    private static final int MAX_CLIENTS = 100;

    private final String host;
    private final int port;
    private ServerSocket serverSocket;
    private final ConcurrentHashMap<String, ClientHandler> clients;
    private final ConcurrentHashMap<Integer, Set<String>> groupMembers;
    private final ExecutorService executorService;
    private volatile boolean running;
    private final DatabaseUserService userService;
    private final GroupService groupService;

    public ChatServer() {
        this("0.0.0.0", DEFAULT_PORT); // Bind all interfaces for LAN
    }

    public ChatServer(int port) {
        this("0.0.0.0", port);
    }

    public ChatServer(String host, int port) {
        this.host = host;
        this.port = port;
        this.clients = new ConcurrentHashMap<>();
        this.groupMembers = new ConcurrentHashMap<>();
        this.executorService = Executors.newFixedThreadPool(MAX_CLIENTS);
        this.userService = DatabaseUserService.getInstance();
        this.groupService = GroupService.getInstance();
    }

    public void start() {
        try {
            // Start UDP Discovery Server
            Thread discoveryThread = new Thread(new DiscoveryServer());
            discoveryThread.setDaemon(true);
            discoveryThread.start();
            System.out.println("UDP Discovery Server started on port 8888");
            
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName(host));
            running = true;
            System.out.println("Chat Server started on " + host + ":" + port);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, this);
                executorService.execute(handler);
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            executorService.shutdownNow();
            clients.values().forEach(ClientHandler::disconnect);
            clients.clear();
            System.out.println("Server stopped");
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    // ====== Client Management ======
    public boolean registerClient(String username, ClientHandler handler) {
        if (clients.containsKey(username)) return false;
        clients.put(username, handler);
        broadcast(Message.createJoinMessage(username));
        broadcastUserList();
        return true;
    }

    public void removeClient(String username) {
        clients.remove(username);
        userService.logout(username);
        broadcast(Message.createLeaveMessage(username));
        broadcastUserList();
    }

    public boolean isUserOnline(String username) {
        return clients.containsKey(username);
    }

    public ClientHandler getClientHandler(String username) {
        return clients.get(username);
    }

    public void broadcastUserList() {
        Message msg = new Message();
        msg.setType(Message.MessageType.USER_LIST);
        msg.setContent(String.join(",", clients.keySet()));
        broadcast(msg);
    }

    // ====== Messaging ======
    public void broadcast(Message message) {
        clients.values().forEach(handler -> handler.sendMessage(message));
    }

    public void sendPrivateMessage(Message message) {
        ClientHandler recipient = clients.get(message.getRecipient());
        ClientHandler sender = clients.get(message.getSender());
        if (recipient != null) recipient.sendMessage(message);
        if (sender != null) sender.sendMessage(message);
    }

    public void sendToUser(String username, Message message) {
        ClientHandler handler = clients.get(username);
        if (handler != null) handler.sendMessage(message);
    }

    // ====== Group Management ======
    public Group createGroup(String name, String description, String creator) {
        Group group = groupService.createGroup(name, description, creator);
        if (group != null) {
            groupMembers.put(group.getGroupId(), ConcurrentHashMap.newKeySet());
            groupMembers.get(group.getGroupId()).add(creator);
        }
        return group;
    }

    public boolean joinGroup(int groupId, String username) {
        if (groupService.addMember(groupId, username)) {
            groupMembers.computeIfAbsent(groupId, k -> ConcurrentHashMap.newKeySet()).add(username);
            broadcastToGroup(groupId, Message.createSystemMessage(username + " joined the group"));
            return true;
        }
        return false;
    }

    public boolean leaveGroup(int groupId, String username) {
        if (groupService.removeMember(groupId, username)) {
            Set<String> members = groupMembers.get(groupId);
            if (members != null) members.remove(username);
            broadcastToGroup(groupId, Message.createSystemMessage(username + " left the group"));
            return true;
        }
        return false;
    }

    public boolean deleteGroup(int groupId, String requester) {
        Group group = groupService.getGroup(groupId);
        if (group != null && group.getCreator().equals(requester)) {
            if (groupService.deleteGroup(groupId, requester)) {
                groupMembers.remove(groupId);
                return true;
            }
        }
        return false;
    }

    public void broadcastToGroup(int groupId, Message message) {
        message.setGroupId(groupId);
        groupService.saveMessage(message);
        Set<String> members = groupMembers.get(groupId);
        if (members != null) {
            for (String member : members) {
                ClientHandler handler = clients.get(member);
                if (handler != null) handler.sendMessage(message);
            }
        }
    }

    public List<Group> getUserGroups(String username) {
        return groupService.getUserGroups(username);
    }

    public List<String> getGroupMembers(int groupId) {
        return groupService.getGroupMembers(groupId);
    }

    public void loadGroupMembers(int groupId) {
        List<String> members = groupService.getGroupMembers(groupId);
        groupMembers.put(groupId, new CopyOnWriteArraySet<>(members));
    }

    public List<Message> getGroupMessageHistory(int groupId, int limit) {
        return groupService.getGroupMessageHistory(groupId, limit);
    }

    public List<Message> getPrivateMessageHistory(String user1, String user2, int limit) {
        return groupService.getMessageHistory(user1, user2, limit);
    }

    // ====== Services ======
    public DatabaseUserService getUserService() {
        return userService;
    }

    public GroupService getGroupService() {
        return groupService;
    }

    // ====== Main ======
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); }
            catch (NumberFormatException e) { System.err.println("Invalid port number. Using default."); }
        }
        ChatServer server = new ChatServer("0.0.0.0", port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        System.out.println("Starting Chat Server with UDP Discovery on port 8888...");
        server.start();
    }
}