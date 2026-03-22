package ecocam.project_chat_console.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    private int groupId;
    private String name;
    private String description;
    private String creator;
    private List<String> members;
    private LocalDateTime createdAt;
    private String avatarColor;
    private int memberCount;          // <-- STORED FROM SERVER

    public Group() {
        this.members = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.avatarColor = generateRandomColor();
        this.memberCount = 0;
    }

    public Group(String name, String description, String creator) {
        this();
        this.name = name;
        this.description = description;
        this.creator = creator;
        this.members.add(creator);
        this.memberCount = 1;
    }

    private String generateRandomColor() {
        String[] colors = {"#3498db", "#e74c3c", "#2ecc71", "#f39c12", "#9b59b6", "#1abc9c", "#e91e63", "#ff5722"};
        return colors[(int) (Math.random() * colors.length)];
    }

    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }
    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getAvatarColor() { return avatarColor; }
    public void setAvatarColor(String avatarColor) { this.avatarColor = avatarColor; }

    // --- Member count (server value) ---
    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    // --- Local member management ---
    public void addMember(String username) {
        if (!members.contains(username)) {
            members.add(username);
            memberCount = members.size();
        }
    }
    public void removeMember(String username) {
        if (members.remove(username)) {
            memberCount = members.size();
        }
    }
    public boolean isMember(String username) { return members.contains(username); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return groupId == group.groupId;
    }
    @Override
    public int hashCode() { return Integer.hashCode(groupId); }
    @Override
    public String toString() { return name + " (" + getMemberCount() + " members)"; }
}