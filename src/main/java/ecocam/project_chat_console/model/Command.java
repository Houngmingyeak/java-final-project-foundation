package ecocam.project_chat_console.model;

import java.io.Serializable;

public class Command implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum CommandType {
        LOGIN,
        REGISTER,
        LOGOUT,
        SEND_MESSAGE,
        SEND_PRIVATE_MESSAGE,
        SEND_GROUP_MESSAGE,
        GET_USERS,
        GET_GROUPS,
        GET_AVAILABLE_GROUPS,    // <-- REQUIRED FOR BROWSE
        CREATE_GROUP,
        JOIN_GROUP,
        LEAVE_GROUP,
        GET_GROUP_MEMBERS,
        GET_MESSAGE_HISTORY,
        TYPING_INDICATOR,
        ADD_REACTION,
        PING,
        DELETE_GROUP,
        INVITE_USER
    }

    private CommandType type;
    private Object data;
    private String sender;

    public Command() {}
    public Command(CommandType type, Object data) { this.type = type; this.data = data; }
    public Command(CommandType type, Object data, String sender) { this(type, data); this.sender = sender; }

    public CommandType getType() { return type; }
    public void setType(CommandType type) { this.type = type; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    @Override
    public String toString() {
        return "Command{" + "type=" + type + ", sender='" + sender + '\'' + '}';
    }
}