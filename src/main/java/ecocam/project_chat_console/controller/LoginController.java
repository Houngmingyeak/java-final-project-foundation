package ecocam.project_chat_console.controller;

import ecocam.project_chat_console.client.ChatClient;
import ecocam.project_chat_console.model.Message;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LoginController {
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink signUpButton;
    @FXML private Label errorLabel;
    @FXML private VBox errorContainer;

    private ChatClient chatClient;
    private String discoveredIP = null;

    @FXML
    public void initialize() {
        chatClient = new ChatClient();

        // ១. រៀបចំ UI លក្ខណៈដើម
        initializeUI();

        // ២. ចាប់ផ្តើមស្វែងរក Server ក្នុង LAN ដោយស្វ័យប្រវត្តិ
        autoDiscoverServer();

        // ៣. កំណត់ Actions ឱ្យប៊ូតុង
        loginButton.setOnAction(this::handleLogin);
        signUpButton.setOnAction(this::handleSignUp);

        // ៤. ចាប់ស្តាប់សារពី Server
        chatClient.addMessageListener(this::handleServerMessage);
        chatClient.addErrorListener(this::handleError);

        if (usernameField != null) {
            Platform.runLater(() -> usernameField.requestFocus());
        }
    }

    private void initializeUI() {
        if (errorContainer != null) {
            errorContainer.setVisible(false);
            errorContainer.setManaged(false);
        }
        if (loginButton != null) loginButton.setDisable(false);
    }

    /**
     * បើកទំព័រចុះឈ្មោះ (Sign Up)
     */
    @FXML
    private void handleSignUp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ecocam/project_chat_console/sign_up.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) signUpButton.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 600); // រក្សាទំហំសមរម្យសម្រាប់ Login/SignUp

            try {
                scene.getStylesheets().add(getClass().getResource("/ecocam/project_chat_console/styles.css").toExternalForm());
            } catch (Exception e) { log.warn("Styles not found"); }

            stage.setScene(scene);
            stage.setTitle("HyperChat - Create Account");
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            log.error("Navigation error", e);
            showError("មិនអាចបើកទំព័រ Sign Up បានទេ");
        }
    }

    private void autoDiscoverServer() {
        new Thread(() -> {
            discoveredIP = chatClient.discoverServerIP();
            Platform.runLater(() -> {
                if (discoveredIP != null) log.info("Server detected at: {}", discoveredIP);
            });
        }).start();
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // កំណត់ Host (ប្រើ IP ដែលរកឃើញ ឬ Default)
        String host = (discoveredIP != null) ? discoveredIP : "192.168.1.17";
        int port = 5000;

        if (username.isEmpty() || password.isEmpty()) {
            showError("សូមបញ្ចូលឈ្មោះអ្នកប្រើ និងលេខសម្ងាត់");
            return;
        }

        loginButton.setDisable(true);
        chatClient.setHost(host);
        chatClient.setPort(port);

        new Thread(() -> {
            if (chatClient.connect()) {
                chatClient.login(username, password);
            } else {
                Platform.runLater(() -> {
                    showError("មិនអាចភ្ជាប់ទៅ Server បានទេ សូមឆែកបណ្តាញរបស់អ្នក");
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }

    private void handleServerMessage(Message message) {
        Platform.runLater(() -> {
            String content = message.getContent();
            if (content != null && content.startsWith("SUCCESS:")) {
                openChatWindow();
            } else if (content != null && content.startsWith("ERROR:")) {
                showError(content.replace("ERROR:", "").trim());
                loginButton.setDisable(false);
            }
        });
    }

    /**
     * បើកផ្ទាំង Chat បែប Full Screen
     */
    private void openChatWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ecocam/project_chat_console/chat_view.fxml"));
            Parent root = loader.load();

            // បញ្ជូន Client ទៅឱ្យ ChatController
            ChatController chatController = loader.getController();
            chatController.setChatClient(chatClient);

            Stage stage = (Stage) loginButton.getScene().getWindow();

            // ✅ ចំណុចសំខាន់សម្រាប់ Full Screen
            stage.setResizable(true);
            stage.setMaximized(true);

            Scene scene = new Scene(root);
            try {
                scene.getStylesheets().add(getClass().getResource("/ecocam/project_chat_console/styles.css").toExternalForm());
            } catch (Exception ignored) {}

            stage.setScene(scene);
            stage.setTitle("HyperChat - " + chatClient.getUsername());
            stage.show();

            log.info("Logged in successfully. Chat opened in Full Screen.");
        } catch (Exception e) {
            log.error("UI Error", e);
            showError("កំហុសក្នុងការបង្ហាញផ្ទាំង Chat: " + e.getMessage());
            loginButton.setDisable(false);
        }
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            if (errorContainer != null) {
                errorLabel.setText(message);
                errorContainer.setVisible(true);
                errorContainer.setManaged(true);
            }
        });
    }

    private void handleError(String error) {
        showError(error);
        loginButton.setDisable(false);
    }
}