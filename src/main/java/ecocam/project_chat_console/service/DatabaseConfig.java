package ecocam.project_chat_console.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {
    private static final String CONFIG_FILE = "database.properties";

    // Configuration loaded from database.properties
    private static String url;
    private static String username;
    private static String password;
    private static boolean initialized = false;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        Properties props = new Properties();

        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                props.load(input);
                url = props.getProperty("db.url");
                username = props.getProperty("db.user");
                password = props.getProperty("db.password");
                
                if (url == null || username == null || password == null) {
                    throw new IOException("Missing required database configuration properties");
                }
            } else {
                throw new IOException("Config file 'database.properties' not found in classpath");
            }
        } catch (IOException e) {
            System.err.println("Error loading config: " + e.getMessage());
            System.err.println("Please ensure database.properties file exists with db.url, db.user, and db.password");
            // Set to empty strings to fail fast on connection attempt
            url = "";
            username = "";
            password = "";
        }

        // Load PostgreSQL driver explicitly (optional but safe)
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL Driver not found: " + e.getMessage());
        }

        initialized = true;
    }

    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            loadConfig();
        }

        // Debug info
        System.out.println("DB URL: " + url);
        System.out.println("DB USER: " + username);

        // Correct way: separate URL, user, password
        Connection conn = DriverManager.getConnection(url, username, password);

        System.out.println("CONNECTED SUCCESSFULLY TO: " + conn.getMetaData().getURL());

        return conn;
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            if (conn.isValid(5)) {
                System.out.println("Database connection successful!");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return false;
        }
        return false;
    }

    public static void setConnectionParams(String dbUrl, String dbUser, String dbPass) {
        url = dbUrl;
        username = dbUser;
        password = dbPass;
        initialized = true;
    }
}