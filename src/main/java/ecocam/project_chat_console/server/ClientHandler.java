package ecocam.project_chat_console.server;

import ecocam.project_chat_console.model.Command;
import ecocam.project_chat_console.model.Group;
import ecocam.project_chat_console.model.Message;
import ecocam.project_chat_console.model.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String username;
    private volatile boolean connected;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.connected = false;
    }

    @Override
    public void run() {
        try {
            // បង្កើត Output មុន Input ដើម្បីការពារកុំឱ្យស្ទះ Socket (Deadlock)
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());

            connected = true;

            while (connected) {
                try {
                    Object received = input.readObject();

                    if (received instanceof Command) {
                        handleCommand((Command) received);
                    } else if (received instanceof Message) {
                        handleMessage((Message) received);
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Unknown object received: " + e.getMessage());
                } catch (IOException e) {
                    // បើដាច់ការតភ្ជាប់ក្នុងពេលកំពុងអាន
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Socket error for user " + username + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    // -------------------- HANDLE MESSAGE --------------------
    private void handleMessage(Message message) {
        if (message == null) return;

        // កំណត់គោលដៅផ្ញើ (Targeting Logic)
        if (message.getGroupId() > 0) {
            // ផ្ញើទៅកាន់ក្រុម
            server.broadcastToGroup(message.getGroupId(), message);
        } else if (message.getRecipient() != null && !message.getRecipient().isEmpty()) {
            // ផ្ញើឯកជន (Private)
            server.sendPrivateMessage(message);
            server.getGroupService().saveMessage(message);
        } else {
            // ផ្ញើទៅកាន់មនុស្សគ្រប់គ្នា (Public)
            server.broadcast(message);
        }
    }

    // -------------------- SEND MESSAGE TO CLIENT --------------------
    public synchronized void sendMessage(Message message) {
        if (!connected || output == null) return; // ឆែកការពារមុននឹងផ្ញើ

        try {
            output.writeObject(message);
            output.flush();
            output.reset();
        } catch (IOException e) {
            // ប្រើ Logger ឬ System.err ដើម្បីដឹងពីមូលហេតុដែលដាច់
            System.err.println("Communication error with " + username + ": " + e.getMessage());
            disconnect(); // ហៅ Method disconnect ដើម្បីបិទ Socket និងលុប Client ចេញពី List
        }
    }

    // -------------------- COMMAND HANDLING --------------------
    private void handleCommand(Command command) {
        switch (command.getType()) {
            case LOGIN -> handleLogin(command);
            case REGISTER -> handleRegister(command);
            case LOGOUT -> handleLogout();
            case GET_USERS -> server.broadcastUserList();
            case GET_GROUPS -> handleGetGroups();
            case GET_AVAILABLE_GROUPS -> handleGetAvailableGroups();
            case CREATE_GROUP -> handleCreateGroup(command);
            case JOIN_GROUP -> handleJoinGroup(command);
            case LEAVE_GROUP -> handleLeaveGroup(command);
            case DELETE_GROUP -> handleDeleteGroup(command);
            case INVITE_USER -> handleInviteUser(command);
            case GET_GROUP_MEMBERS -> handleGetGroupMembers(command);
            case GET_MESSAGE_HISTORY -> handleGetMessageHistory(command);
            case TYPING_INDICATOR -> handleTypingIndicator(command);
            case PING -> sendMessage(Message.createSystemMessage("PONG"));
            default -> System.err.println("Unknown command: " + command.getType());
        }
    }
    private void handleLogin(Command command) {
        if (command.getData() instanceof User credentials) {
            Optional<User> user = server.getUserService().login(
                    credentials.getUsername(),
                    credentials.getPassword()
            );

            if (user.isPresent()) {
                String uname = credentials.getUsername();

                // ១. ឆែកមើលថាតើ User កំពុង Online ឬអត់
                if (server.isUserOnline(uname)) {
                    sendMessage(Message.createSystemMessage("ERROR: User already logged in on another device"));
                    return;
                }

                // ២. ចុះឈ្មោះ Client ទៅក្នុង Server Map
                this.username = uname;
                if (server.registerClient(username, this)) {
                    // ប្រាប់ Client ថា Login ជោគជ័យ
                    sendMessage(Message.createSystemMessage("SUCCESS: Login successful"));

                    // ៣. បញ្ជូនទិន្នន័យចាំបាច់ទៅឱ្យ Client ភ្លាមៗ (Optional)
                    // ឧទាហរណ៍៖ បញ្ជូនបញ្ជី Group ដែល User នោះជាសមាជិក
                    List<Group> userGroups = server.getUserGroups(username);
                    // អ្នកអាចបង្កើត Command ថ្មីសម្រាប់បញ្ជូន Group List ទៅ Client
                    // sendCommand(new Command(Command.CommandType.SET_GROUPS, userGroups));
                } else {
                    sendMessage(Message.createSystemMessage("ERROR: Server is full or cannot register client"));
                }
            } else {
                sendMessage(Message.createSystemMessage("ERROR: Invalid username or password"));
            }
        }
    }

    private void handleRegister(Command command) {
        if (command.getData() instanceof User newUser) {
            if (server.getUserService().userExists(newUser.getUsername())) {
                sendMessage(Message.createSystemMessage("ERROR: Username already exists"));
            } else {
                if (server.getUserService().register(newUser)) {
                    sendMessage(Message.createSystemMessage("SUCCESS: Registration successful"));
                } else {
                    sendMessage(Message.createSystemMessage("ERROR: Registration failed"));
                }
            }
        }
    }

    private void handleLogout() {
        if (username != null) {
            server.removeClient(username);
        }
        connected = false;
    }

    private void handleGetGroups() {
        if (username != null) {
            List<Group> groups = server.getUserGroups(username);
            Message message = new Message();
            message.setType(Message.MessageType.SYSTEM);
            message.setContent("GROUPS:" + serializeGroups(groups));
            sendMessage(message);
        }
    }

    private void handleGetAvailableGroups() {
        if (username != null) {
            List<Group> allGroups = server.getGroupService().getAllGroups();
            List<Group> userGroups = server.getUserGroups(username);

            List<Group> availableGroups = allGroups.stream()
                    .filter(group -> userGroups.stream()
                            .noneMatch(userGroup -> userGroup.getGroupId() == group.getGroupId()))
                    .toList();

            Message message = new Message();
            message.setType(Message.MessageType.SYSTEM);
            message.setContent("AVAILABLE_GROUPS:" + serializeGroups(availableGroups));
            sendMessage(message);
        }
    }

    private void handleCreateGroup(Command command) {
        if (username != null && command.getData() instanceof Group groupData) {
            Group group = server.createGroup(groupData.getName(), groupData.getDescription(), username);
            if (group != null) {
                sendMessage(Message.createSystemMessage("SUCCESS: Group created: " + group.getGroupId()));
            } else {
                sendMessage(Message.createSystemMessage("ERROR: Failed to create group"));
            }
        }
    }

    private void handleJoinGroup(Command command) {
        if (username != null && command.getData() instanceof Integer groupId) {
            if (server.joinGroup(groupId, username)) {
                sendMessage(Message.createSystemMessage("SUCCESS: Joined group"));
                server.loadGroupMembers(groupId);
            } else {
                sendMessage(Message.createSystemMessage("ERROR: Failed to join group"));
            }
        }
    }

    private void handleLeaveGroup(Command command) {
        if (username != null && command.getData() instanceof Integer groupId) {
            if (server.leaveGroup(groupId, username)) {
                sendMessage(Message.createSystemMessage("SUCCESS: Left group"));
            } else {
                sendMessage(Message.createSystemMessage("ERROR: Failed to leave group"));
            }
        }
    }

    private void handleDeleteGroup(Command command) {
        if (username != null && command.getData() instanceof Integer groupId) {
            if (server.deleteGroup(groupId, username)) {
                sendMessage(Message.createSystemMessage("SUCCESS: Group deleted"));
            } else {
                sendMessage(Message.createSystemMessage("ERROR: Failed to delete group"));
            }
        }
    }

    private void handleInviteUser(Command command) {
        if (username != null && command.getData() instanceof String data) {
            String[] parts = data.split(":", 2);
            if (parts.length == 2) {
                try {
                    int groupId = Integer.parseInt(parts[0]);
                    String invitee = parts[1];

                    if (server.getGroupService().inviteUserToGroup(groupId, username, invitee)) {
                        sendMessage(Message.createSystemMessage("SUCCESS: User invited to group"));
                        ClientHandler inviteeHandler = server.getClientHandler(invitee);
                        if (inviteeHandler != null) {
                            inviteeHandler.sendMessage(Message.createSystemMessage(
                                    "INFO: You have been invited to join group ID " + groupId));
                        }
                    } else {
                        sendMessage(Message.createSystemMessage("ERROR: Failed to invite user to group"));
                    }
                } catch (NumberFormatException e) {
                    sendMessage(Message.createSystemMessage("ERROR: Invalid group ID"));
                }
            }
        }
    }

    private void handleGetGroupMembers(Command command) {
        if (command.getData() instanceof Integer groupId) {
            List<String> members = server.getGroupMembers(groupId);
            Message message = new Message();
            message.setType(Message.MessageType.SYSTEM);
            message.setContent("GROUP_MEMBERS:" + groupId + ":" + String.join(",", members));
            sendMessage(message);
        }
    }

    private void handleGetMessageHistory(Command command) {
        if (command.getData() instanceof String data) {
            String[] parts = data.split(":");
            if (parts.length >= 2) {
                String type = parts[0];
                int limit = parts.length > 2 ? Integer.parseInt(parts[2]) : 50;

                List<Message> messages;
                if ("GROUP".equals(type)) {
                    int groupId = Integer.parseInt(parts[1]);
                    messages = server.getGroupMessageHistory(groupId, limit);
                } else {
                    String otherUser = parts[1];
                    messages = server.getPrivateMessageHistory(username, otherUser, limit);
                }

                Message response = new Message();
                response.setType(Message.MessageType.SYSTEM);
                response.setContent("MESSAGE_HISTORY:" + type + ":" + serializeMessages(messages));
                sendMessage(response);
            }
        }
    }

    private void handleTypingIndicator(Command command) {
        if (username != null && command.getData() instanceof String data) {
            String[] parts = data.split(":");
            if (parts.length >= 2) {
                String type = parts[0];
                int groupId = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

                Message typingMsg = Message.createTypingIndicator(username, parts[1], groupId);

                if ("GROUP".equals(type) && groupId > 0) {
                    server.broadcastToGroup(groupId, typingMsg);
                } else if ("PRIVATE".equals(type)) {
                    server.sendToUser(parts[1], typingMsg);
                }
            }
        }
    }

    private String serializeGroups(List<Group> groups) {
        StringBuilder sb = new StringBuilder();
        for (Group group : groups) {
            if (sb.length() > 0) sb.append(";");
            sb.append(group.getGroupId()).append(",")
                    .append(group.getName()).append(",")
                    .append(group.getDescription()).append(",")
                    .append(group.getCreator()).append(",")
                    .append(group.getMemberCount());
        }
        return sb.toString();
    }

    private String serializeMessages(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            if (sb.length() > 0) sb.append(";");
            sb.append(msg.getSender()).append(",")
                    .append(msg.getContent()).append(",")
                    .append(msg.getType()).append(",")
                    .append(msg.getTimestamp());

            if (msg.getType() == Message.MessageType.FILE && msg.getAttachment() != null) {
                sb.append(",FILE_DATA:").append(msg.getAttachment());
            }
        }
        return sb.toString();
    }

    public void disconnect() {
        connected = false;
        if (username != null) {
            server.removeClient(username);
        }
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing client connection: " + e.getMessage());
        }
    }

    public String getUsername() { return username; }
    public boolean isConnected() { return connected; }
}