package ecocam.project_chat_console.service;

import ecocam.project_chat_console.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DatabaseUserService {

    private static DatabaseUserService instance;

    private DatabaseUserService() {
        initializeDatabase();
    }

    public static synchronized DatabaseUserService getInstance() {
        if (instance == null) {
            instance = new DatabaseUserService();
        }
        return instance;
    }

    // ==================== Initialize Database Tables ====================
    private void initializeDatabase() {
        String createUsersTableSQL = """
            CREATE TABLE IF NOT EXISTS users (
                username VARCHAR(50) PRIMARY KEY,
                password VARCHAR(100) NOT NULL,
                display_name VARCHAR(100) NOT NULL,
                status VARCHAR(20) DEFAULT 'offline',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_login TIMESTAMP
            );
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createUsersTableSQL);
            System.out.println("Database users table ready.");

        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== REGISTER ====================
    public boolean register(User user) {
        String insertSQL = """
            INSERT INTO users (username, password, display_name, status, created_at)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword()); // TODO: hash password with BCrypt
            pstmt.setString(3, user.getDisplayName());
            pstmt.setString(4, "offline");
            pstmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

            int rows = pstmt.executeUpdate();
            System.out.println("User " + user.getUsername() + " registered successfully.");
            return rows > 0;

        } catch (SQLException e) {
            if (e.getMessage().contains("duplicate key") || e.getMessage().contains("PRIMARY KEY")) {
                System.err.println("User already exists: " + user.getUsername());
            } else {
                System.err.println("Error registering user: " + e.getMessage());
                e.printStackTrace();
            }
            return false;
        }
    }

    // ==================== LOGIN ====================
    public Optional<User> login(String username, String password) {
        String selectSQL = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password); // TODO: verify hashed password if using BCrypt

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);

                    // Update status and last login
                    String updateSQL = "UPDATE users SET status = ?, last_login = ? WHERE username = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                        updateStmt.setString(1, "online");
                        updateStmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                        updateStmt.setString(3, username);
                        updateStmt.executeUpdate();
                    }

                    user.setStatus("online");
                    user.setLastLogin(LocalDateTime.now());

                    System.out.println("User " + username + " logged in successfully.");
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error during login: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // ==================== LOGOUT ====================
    public void logout(String username) {
        String updateSQL = "UPDATE users SET status = ? WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {

            pstmt.setString(1, "offline");
            pstmt.setString(2, username);
            pstmt.executeUpdate();

            System.out.println("User " + username + " logged out successfully.");

        } catch (SQLException e) {
            System.err.println("Error during logout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== USER EXISTS ====================
    public boolean userExists(String username) {
        String selectSQL = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking user existence: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ==================== GET USER ====================
    public Optional<User> getUser(String username) {
        String selectSQL = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // ==================== GET ALL USERS ====================
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String selectSQL = "SELECT * FROM users ORDER BY created_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all users: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    // ==================== HELPER ====================
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setDisplayName(rs.getString("display_name"));
        user.setStatus(rs.getString("status"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) user.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) user.setLastLogin(lastLogin.toLocalDateTime());

        return user;
    }
}