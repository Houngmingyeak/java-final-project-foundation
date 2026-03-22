package ecocam.project_chat_console.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static Message createFileMessage(String username, String fileName, String encodedFile) {
        Message message = new Message();
        message.setSender(username);
        message.setContent("File: " + fileName);
        message.setType(MessageType.FILE);
        message.setAttachment(encodedFile);
        message.setFileName(fileName);
        return message;
    }

    public enum MessageType {
        TEXT,
        PRIVATE,
        GROUP,
        SYSTEM,
        USER_JOIN,
        USER_LEAVE,
        USER_LIST,
        TYPING_INDICATOR,
        FILE,
        // Enhanced message types with titles
        ANNOUNCEMENT,
        ALERT,
        NOTICE,
        IMPORTANT,
        QUESTION,
        ANSWER,
        POLL,
        EVENT
    }

    private String sender;
    private String recipient;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    private int groupId;
    private String replyTo;
    private String attachment;
    private String fileName;
    // New fields for title functionality
    private String title;
    private boolean pinned;
    private String category;

    public Message() {
        this.timestamp = LocalDateTime.now();
        this.type = MessageType.TEXT;
    }

    public Message(String sender, String content, MessageType type) {
        this();
        this.sender = sender;
        this.content = content;
        this.type = type;
    }

    public Message(String sender, String recipient, String content, MessageType type) {
        this(sender, content, type);
        this.recipient = recipient;
    }

    public static Message createSystemMessage(String content) {
        return new Message("System", content, MessageType.SYSTEM);
    }

    public static Message createJoinMessage(String username) {
        return new Message(username, username + " joined the chat", MessageType.USER_JOIN);
    }

    public static Message createLeaveMessage(String username) {
        return new Message(username, username + " left the chat", MessageType.USER_LEAVE);
    }

    public static Message createPrivateMessage(String sender, String recipient, String content) {
        return new Message(sender, recipient, content, MessageType.PRIVATE);
    }

    public static Message createGroupMessage(String sender, int groupId, String content) {
        Message message = new Message(sender, content, MessageType.GROUP);
        message.setGroupId(groupId);
        return message;
    }

    public static Message createTypingIndicator(String sender, String recipient, int groupId) {
        Message message = new Message(sender, "", MessageType.TYPING_INDICATOR);
        message.setRecipient(recipient);
        message.setGroupId(groupId);
        return message;
    }

    public static Message createFileMessage(String sender, String recipient, int groupId, String fileName, String attachment) {
        Message message = new Message(sender, recipient, "File: " + fileName, MessageType.FILE);
        message.setGroupId(groupId);
        message.setFileName(fileName);
        message.setAttachment(attachment);
        return message;
    }
    
    // ==================== NEW TITLE MESSAGE FUNCTIONS ====================
    
    public static Message createTitleMessage(String sender, String title, String content, MessageType type) {
        Message message = new Message(sender, content, type);
        message.setTitle(title);
        return message;
    }
    
    public static Message createAnnouncement(String sender, String title, String content) {
        return createTitleMessage(sender, title, content, MessageType.ANNOUNCEMENT);
    }
    
    public static Message createAlert(String sender, String title, String content) {
        Message message = createTitleMessage(sender, title, content, MessageType.ALERT);
        message.setPinned(true); // Alerts are typically pinned
        return message;
    }
    
    public static Message createNotice(String sender, String title, String content) {
        return createTitleMessage(sender, title, content, MessageType.NOTICE);
    }
    
    public static Message createImportantMessage(String sender, String title, String content) {
        Message message = createTitleMessage(sender, title, content, MessageType.IMPORTANT);
        message.setPinned(true); // Important messages are typically pinned
        return message;
    }
    
    public static Message createQuestion(String sender, String title, String content) {
        return createTitleMessage(sender, title, content, MessageType.QUESTION);
    }
    
    public static Message createAnswer(String sender, String title, String content, String replyTo) {
        Message message = createTitleMessage(sender, title, content, MessageType.ANSWER);
        message.setReplyTo(replyTo);
        return message;
    }
    
    public static Message createPoll(String sender, String title, String content) {
        return createTitleMessage(sender, title, content, MessageType.POLL);
    }
    
    public static Message createEvent(String sender, String title, String content) {
        return createTitleMessage(sender, title, content, MessageType.EVENT);
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedTime() {
        return timestamp.format(FORMATTER);
    }

    public boolean isPrivate() {
        return type == MessageType.PRIVATE;
    }

    public boolean isGroup() {
        return type == MessageType.GROUP;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    // ==================== TITLE MESSAGE GETTERS AND SETTERS ====================
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public boolean isPinned() {
        return pinned;
    }
    
    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    // ==================== HELPER METHODS ====================
    
    public boolean hasTitle() {
        return title != null && !title.trim().isEmpty();
    }
    
    public boolean isTitleMessage() {
        return hasTitle() || type == MessageType.ANNOUNCEMENT || 
               type == MessageType.ALERT || type == MessageType.NOTICE ||
               type == MessageType.IMPORTANT || type == MessageType.QUESTION ||
               type == MessageType.ANSWER || type == MessageType.POLL ||
               type == MessageType.EVENT;
    }
    
    public String getDisplayTitle() {
        if (title != null && !title.trim().isEmpty()) {
            return title;
        }
        
        // Generate default titles based on message type
        switch (type) {
            case ANNOUNCEMENT: return "📢 Announcement";
            case ALERT: return "⚠️ Alert";
            case NOTICE: return "📝 Notice";
            case IMPORTANT: return "⭐ Important";
            case QUESTION: return "❓ Question";
            case ANSWER: return "✅ Answer";
            case POLL: return "📊 Poll";
            case EVENT: return "📅 Event";
            default: return "";
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(getFormattedTime()).append("] ");
        
        if (hasTitle()) {
            sb.append(getDisplayTitle()).append(": ");
        }
        
        sb.append(sender).append(": ").append(content);
        
        if (pinned) {
            sb.append(" 📌");
        }
        
        return sb.toString();
    }
}
