package ecocam.project_chat_console.service;

import ecocam.project_chat_console.model.User;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {
    private static final String DATA_DIR = "data";
    private static final String USERS_FILE = DATA_DIR + "/users.dat";
    private final ConcurrentHashMap<String, User> users;
    private static UserService instance;

    private UserService() {
        users = new ConcurrentHashMap<>();
        loadUsers();
    }

    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private void loadUsers() {
        Path path = Paths.get(USERS_FILE);
        if (Files.exists(path)) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(USERS_FILE))) {
                List<User> userList = (List<User>) ois.readObject();
                userList.forEach(user -> users.put(user.getUsername(), user));
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error loading users: " + e.getMessage());
            }
        }
    }

    private void saveUsers() {
        try {
            Path dir = Paths.get(DATA_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
                oos.writeObject(new ArrayList<>(users.values()));
            }
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
        }
    }

    public synchronized boolean register(User user) {
        if (users.containsKey(user.getUsername())) {
            return false;
        }
        users.put(user.getUsername(), user);
        saveUsers();
        return true;
    }

    public Optional<User> login(String username, String password) {
        User user = users.get(username);
        if (user != null && user.getPassword().equals(password)) {
            user.setLastLogin(LocalDateTime.now());
            user.setStatus("online");
            saveUsers();
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public void logout(String username) {
        User user = users.get(username);
        if (user != null) {
            user.setStatus("offline");
            saveUsers();
        }
    }

    public boolean userExists(String username) {
        return users.containsKey(username);
    }

    public Optional<User> getUser(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public void updateUserStatus(String username, String status) {
        User user = users.get(username);
        if (user != null) {
            user.setStatus(status);
        }
    }
}
