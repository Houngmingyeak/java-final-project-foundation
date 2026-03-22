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

import java.io.IOException;

public class SignUpController {
    @FXML
    private TextField usernameField;

    @FXML
    private TextField displayNameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Button signUpButton;

    @FXML
    private Hyperlink backButton;

    @FXML
    private Label errorLabel;

    @FXML
    private Label successLabel;

    @FXML
    private VBox errorContainer;

    @FXML
    private VBox successContainer;

    @FXML
    private TextField serverHostField;

    @FXML
    private TextField serverPortField;

    private ChatClient chatClient;

    @FXML
    public void initialize() {
        chatClient = new ChatClient();
        
        // Initialize UI elements
        initializeUI();
        
        signUpButton.setOnAction(this::handleSignUp);
        backButton.setOnAction(this::handleBack);

        chatClient.addMessageListener(this::handleServerMessage);
        chatClient.addErrorListener(this::handleError);
        
        // Set initial focus
        if (usernameField != null) {
            Platform.runLater(() -> usernameField.requestFocus());
        }
    }
    
    private void initializeUI() {
        // Hide error and success containers initially
        if (errorContainer != null) {
            errorContainer.setVisible(false);
            errorContainer.setManaged(false);
        }
        if (successContainer != null) {
            successContainer.setVisible(false);
            successContainer.setManaged(false);
        }
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
        if (successLabel != null) {
            successLabel.setVisible(false);
        }
        
        // Set default server configuration
        if (serverHostField != null) serverHostField.setText("localhost");
        if (serverPortField != null) serverPortField.setText("5000");
        
        // Enable signup button
        if (signUpButton != null) {
            signUpButton.setDisable(false);
        }
        
        // Clear form fields
        if (usernameField != null) usernameField.clear();
        if (displayNameField != null) displayNameField.clear();
        if (passwordField != null) passwordField.clear();
        if (confirmPasswordField != null) confirmPasswordField.clear();
    }
    private void handleSignUp(ActionEvent event) {
        String username = usernameField.getText().trim();
        String displayName = displayNameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // --- កែសម្រួលចំណុចនេះ ដើម្បីបំបាត់ NullPointerException ---
        // ប្រសិនបើអ្នកមាន Variable 'discoveredIP' ក្នុង SignUpController ដូចក្នុង LoginController ប្រើវា
        // បើមិនទាន់មានទេ ប្រើ IP ថេរដែលអ្នកដឹង (ឧទាហរណ៍ IP Server របស់អ្នក)
        String host = "192.168.1.17";
        int port = 5000;
        // ------------------------------------------------------

        // Validate input
        if (username.isEmpty() || displayName.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        if (!isValidUsername(username)) {
            showError("Username: 3-20 characters, alphanumeric and underscore only");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        if (!isValidPassword(password)) {
            showError("Password must be 4-50 characters long");
            return;
        }

        // កំណត់តម្លៃទៅឱ្យ ChatClient ដោយផ្ទាល់
        chatClient.setHost(host);
        chatClient.setPort(port);

        errorLabel.setVisible(false);
        if (successLabel != null) successLabel.setVisible(false);
        signUpButton.setDisable(true);

        new Thread(() -> {
            // ព្យាយាមភ្ជាប់ទៅ Server មុននឹងផ្ញើទិន្នន័យចុះឈ្មោះ
            if (chatClient.connect()) {
                // ផ្ញើ Command ចុះឈ្មោះ (ត្រូវប្រាកដថា Parameter ត្រូវតាម ChatClient.register)
                chatClient.register(username, displayName, password);
            } else {
                Platform.runLater(() -> {
                    showError("Failed to connect to server at " + host);
                    signUpButton.setDisable(false);
                });
            }
        }).start();
    }
    private void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ecocam/project_chat_console/log_in.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/ecocam/project_chat_console/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Login - Chat Console");
            stage.show();
            
            // Reset signup form after successful navigation
            resetSignupForm();
        } catch (IOException e) {
            showError("Error loading login page: " + e.getMessage());
        }
    }

    private void handleServerMessage(Message message) {
        Platform.runLater(() -> {
            String content = message.getContent();

            if (content.startsWith("SUCCESS:")) {
                if (content.contains("Registration successful")) {
                    showSuccess("Registration successful! Please login.");
                    signUpButton.setDisable(false);

                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                            Platform.runLater(this::goToLogin);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }
            } else if (content.startsWith("ERROR:")) {
                showError(content.substring(6).trim());
                signUpButton.setDisable(false);
            }
        });
    }

    private void handleError(String error) {
        Platform.runLater(() -> {
            showError(error);
            signUpButton.setDisable(false);
        });
    }

    private void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ecocam/project_chat_console/log_in.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) signUpButton.getScene().getWindow();
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/ecocam/project_chat_console/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Login - Chat Console");
            stage.show();
            
            // Reset signup form after successful navigation
            resetSignupForm();
        } catch (IOException e) {
            showError("Error loading login page: " + e.getMessage());
        }
    }

    private void showError(String message) {
        if (errorContainer != null) {
            errorLabel.setText(message);
            errorContainer.setVisible(true);
            errorContainer.setManaged(true);
            if (successContainer != null) {
                successContainer.setVisible(false);
                successContainer.setManaged(false);
            }
        } else {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            successLabel.setVisible(false);
        }
    }

    private void showSuccess(String message) {
        if (successContainer != null) {
            successLabel.setText(message);
            successContainer.setVisible(true);
            successContainer.setManaged(true);
            if (errorContainer != null) {
                errorContainer.setVisible(false);
                errorContainer.setManaged(false);
            }
        } else {
            successLabel.setText(message);
            successLabel.setVisible(true);
            errorLabel.setVisible(false);
        }
    }
    
    // ==================== UI REFRESH METHODS ====================
    
    private void resetSignupForm() {
        // Clear form fields
        if (usernameField != null) usernameField.clear();
        if (displayNameField != null) displayNameField.clear();
        if (passwordField != null) passwordField.clear();
        if (confirmPasswordField != null) confirmPasswordField.clear();
        
        // Reset server configuration to defaults
        if (serverHostField != null) serverHostField.setText("localhost");
        if (serverPortField != null) serverPortField.setText("5000");
        
        // Hide error and success messages
        if (errorContainer != null) {
            errorContainer.setVisible(false);
            errorContainer.setManaged(false);
        }
        if (successContainer != null) {
            successContainer.setVisible(false);
            successContainer.setManaged(false);
        }
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
        if (successLabel != null) {
            successLabel.setVisible(false);
        }
        
        // Enable signup button
        if (signUpButton != null) {
            signUpButton.setDisable(false);
        }
        
        // Reset focus to username field
        if (usernameField != null) {
            Platform.runLater(() -> usernameField.requestFocus());
        }
    }
    
    // Input validation methods
    private boolean isValidUsername(String username) {
        return username != null && 
               username.length() >= 3 && 
               username.length() <= 20 && 
               username.matches("^[a-zA-Z0-9_]+$");
    }
    
    private boolean isValidDisplayName(String displayName) {
        return displayName != null && 
               displayName.length() >= 1 && 
               displayName.length() <= 50 &&
               displayName.matches("^[a-zA-Z0-9_\\s\\-\\.]+$");
    }
    
    private boolean isValidPassword(String password) {
        return password != null && 
               password.length() >= 4 && 
               password.length() <= 50;
    }
}
