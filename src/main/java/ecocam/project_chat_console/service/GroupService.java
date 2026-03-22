package ecocam.project_chat_console.service;

import ecocam.project_chat_console.model.Group;
import ecocam.project_chat_console.model.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupService {
    private static GroupService instance;

    private GroupService() {
        initializeDatabase();
    }

    public static synchronized GroupService getInstance() {
        if (instance == null) {
            instance = new GroupService();
        }
        return instance;
    }
    private void initializeDatabase() {
        String createGroupsTable = """
            CREATE TABLE IF NOT EXISTS groups (
                group_id SERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                description TEXT,
                creator VARCHAR(50) REFERENCES users(username),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createGroupMembersTable = """
            CREATE TABLE IF NOT EXISTS group_members (
                group_id INTEGER REFERENCES groups(group_id),
                username VARCHAR(50) REFERENCES users(username),
                joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (group_id, username)
            )
            """;

        String createMessagesTable = """
            CREATE TABLE IF NOT EXISTS messages (
                message_id SERIAL PRIMARY KEY,
                sender VARCHAR(50) REFERENCES users(username),
                recipient VARCHAR(50),
                group_id INTEGER REFERENCES groups(group_id),
                content TEXT,
                type VARCHAR(20),
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createReactionsTable = """
            CREATE TABLE IF NOT EXISTS message_reactions (
                reaction_id SERIAL PRIMARY KEY,
                message_id INTEGER REFERENCES messages(message_id) ON DELETE CASCADE,
                username VARCHAR(50) NOT NULL,
                emoji VARCHAR(10) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(message_id, username, emoji)
            )
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createGroupsTable);
            stmt.execute(createGroupMembersTable);
            stmt.execute(createMessagesTable);
            stmt.execute(createReactionsTable);
            System.out.println("Group and message tables initialized successfully");
        } catch (SQLException e) {
            System.err.println("Error initializing group tables: " + e.getMessage());
        }
    }

    public Group createGroup(String name, String description, String creator) {
        String insertSQL = """
            INSERT INTO groups (name, description, creator)
            VALUES (?, ?, ?)
            RETURNING group_id
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            Group group = new Group(name, description, creator);

            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setString(3, creator);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                group.setGroupId(rs.getInt(1));
                addMember(group.getGroupId(), creator);
                return group;
            }
        } catch (SQLException e) {
            System.err.println("Error creating group: " + e.getMessage());
        }
        return null;
    }

    public boolean addMember(int groupId, String username) {
        String insertSQL = """
            INSERT INTO group_members (group_id, username)
            VALUES (?, ?)
            ON CONFLICT DO NOTHING
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setInt(1, groupId);
            pstmt.setString(2, username);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding member: " + e.getMessage());
            return false;
        }
    }

    public boolean removeMember(int groupId, String username) {
        String deleteSQL = "DELETE FROM group_members WHERE group_id = ? AND username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {

            pstmt.setInt(1, groupId);
            pstmt.setString(2, username);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error removing member: " + e.getMessage());
            return false;
        }
    }

    public Group getGroup(int groupId) {
        String selectSQL = """
            SELECT g.*, array_agg(gm.username) as members
            FROM groups g
            LEFT JOIN group_members gm ON g.group_id = gm.group_id
            WHERE g.group_id = ?
            GROUP BY g.group_id
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToGroup(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error getting group: " + e.getMessage());
        }
        return null;
    }

    public List<Group> getUserGroups(String username) {
        List<Group> groups = new ArrayList<>();
        String selectSQL = """
            SELECT g.*, array_agg(gm2.username) as members
            FROM groups g
            JOIN group_members gm ON g.group_id = gm.group_id
            LEFT JOIN group_members gm2 ON g.group_id = gm2.group_id
            WHERE gm.username = ?
            GROUP BY g.group_id
            ORDER BY g.created_at DESC
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                groups.add(mapResultSetToGroup(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting user groups: " + e.getMessage());
        }
        return groups;
    }

    public List<Group> getAllGroups() {
        List<Group> groups = new ArrayList<>();
        String selectSQL = """
            SELECT g.*, COUNT(gm.username) as member_count
            FROM groups g
            LEFT JOIN group_members gm ON g.group_id = gm.group_id
            GROUP BY g.group_id
            ORDER BY g.created_at DESC
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {

            while (rs.next()) {
                groups.add(mapResultSetToGroupWithMemberCount(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all groups: " + e.getMessage());
        }
        return groups;
    }

    public List<String> getGroupMembers(int groupId) {
        List<String> members = new ArrayList<>();
        String selectSQL = "SELECT username FROM group_members WHERE group_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                members.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting group members: " + e.getMessage());
        }
        return members;
    }

    public boolean isGroupMember(int groupId, String username) {
        String selectSQL = "SELECT 1 FROM group_members WHERE group_id = ? AND username = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setInt(1, groupId);
            pstmt.setString(2, username);

            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error checking group membership: " + e.getMessage());
            return false;
        }
    }

    public boolean isGroupCreator(int groupId, String username) {
        String selectSQL = "SELECT 1 FROM groups WHERE group_id = ? AND creator = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setInt(1, groupId);
            pstmt.setString(2, username);

            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error checking group creator: " + e.getMessage());
            return false;
        }
    }

    public boolean inviteUserToGroup(int groupId, String inviter, String invitee) {
        // Check if the inviter is the group creator or a member
        if (!isGroupCreator(groupId, inviter) && !isGroupMember(groupId, inviter)) {
            return false;
        }
        
        // Check if the invitee exists
        if (!DatabaseUserService.getInstance().userExists(invitee)) {
            return false;
        }
        
        // Add the user to the group
        return addMember(groupId, invitee);
    }

    public boolean deleteGroup(int groupId, String requester) {
        String deleteSQL = """
            DELETE FROM groups
            WHERE group_id = ? AND creator = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {

            pstmt.setInt(1, groupId);
            pstmt.setString(2, requester);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting group: " + e.getMessage());
            return false;
        }
    }

    public void saveMessage(Message message) {
        String insertSQL = """
            INSERT INTO messages (sender, recipient, group_id, content, type)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setString(1, message.getSender());
            pstmt.setString(2, message.getRecipient());
            pstmt.setInt(3, message.getGroupId() > 0 ? message.getGroupId() : null);
            pstmt.setString(4, message.getContent());
            pstmt.setString(5, message.getType().name());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving message: " + e.getMessage());
        }
    }

    public List<Message> getMessageHistory(String user1, String user2, int limit) {
        List<Message> messages = new ArrayList<>();
        String selectSQL = """
            SELECT * FROM messages
            WHERE (sender = ? AND recipient = ?) OR (sender = ? AND recipient = ?)
            AND group_id IS NULL
            ORDER BY timestamp DESC
            LIMIT ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setString(1, user1);
            pstmt.setString(2, user2);
            pstmt.setString(3, user2);
            pstmt.setString(4, user1);
            pstmt.setInt(5, limit);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                messages.add(0, mapResultSetToMessage(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting message history: " + e.getMessage());
        }
        return messages;
    }

    public List<Message> getGroupMessageHistory(int groupId, int limit) {
        List<Message> messages = new ArrayList<>();
        String selectSQL = """
            SELECT * FROM messages
            WHERE group_id = ?
            ORDER BY timestamp DESC
            LIMIT ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setInt(1, groupId);
            pstmt.setInt(2, limit);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                messages.add(0, mapResultSetToMessage(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting group message history: " + e.getMessage());
        }
        return messages;
    }

    public boolean addReaction(int messageId, String username, String emoji) {
        String insertSQL = """
            INSERT INTO message_reactions (message_id, username, emoji)
            VALUES (?, ?, ?)
            ON CONFLICT DO NOTHING
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setInt(1, messageId);
            pstmt.setString(2, username);
            pstmt.setString(3, emoji);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding reaction: " + e.getMessage());
            return false;
        }
    }

    private Group mapResultSetToGroup(ResultSet rs) throws SQLException {
        Group group = new Group();
        group.setGroupId(rs.getInt("group_id"));
        group.setName(rs.getString("name"));
        group.setDescription(rs.getString("description"));
        group.setCreator(rs.getString("creator"));
        group.setAvatarColor(rs.getString("avatar_color"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            group.setCreatedAt(createdAt.toLocalDateTime());
        }

        Array membersArray = rs.getArray("members");
        if (membersArray != null) {
            String[] members = (String[]) membersArray.getArray();
            for (String member : members) {
                if (member != null) {
                    group.addMember(member);
                }
            }
        }

        return group;
    }

    private Group mapResultSetToGroupWithMemberCount(ResultSet rs) throws SQLException {
        Group group = new Group();
        group.setGroupId(rs.getInt("group_id"));
        group.setName(rs.getString("name"));
        group.setDescription(rs.getString("description"));
        group.setCreator(rs.getString("creator"));
        group.setAvatarColor(rs.getString("avatar_color"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            group.setCreatedAt(createdAt.toLocalDateTime());
        }

        // Set member count from the COUNT query result
        int memberCount = rs.getInt("member_count");
        group.setMemberCount(memberCount);

        return group;
    }

    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setSender(rs.getString("sender"));
        message.setRecipient(rs.getString("recipient"));
        message.setGroupId(rs.getInt("group_id"));
        message.setContent(rs.getString("content"));
        message.setType(Message.MessageType.valueOf(rs.getString("type")));
        message.setReplyTo(rs.getString("reply_to"));
        message.setAttachment(rs.getString("attachment"));
        message.setFileName(rs.getString("file_name"));

        Timestamp timestamp = rs.getTimestamp("timestamp");
        if (timestamp != null) {
            message.setTimestamp(timestamp.toLocalDateTime());
        }

        return message;
    }
}
