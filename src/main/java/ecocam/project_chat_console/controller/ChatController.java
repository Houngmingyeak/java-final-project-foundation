package ecocam.project_chat_console.controller;

import ecocam.project_chat_console.client.ChatClient;
import ecocam.project_chat_console.model.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    // FXML UI Components
    @FXML private ListView<Message> messageListView;
    @FXML private ListView<String> onlineUsersList;
    @FXML private TextField messageInput;
    @FXML private Button sendMessageBtn, logoutBtn, emojiToggleBtn, attachFileBtn, attachImageBtn;
    @FXML private VBox emojiPickerContainer, emptyState;
    @FXML private FlowPane emojiGrid;
    @FXML private Label chatRoomLabel, chatRoomDesc, topBarUserName, topBarUserStatus, onlineUsersCount, miniAvatarLabel;
    @FXML private Label currentUserDisplayName, currentUserStatusLabel, currentUserAvatarLabel, connectionStatusLabel, activeUsersLabel;
    @FXML private Button userMenuDropdownBtn;
    @FXML private ContextMenu userContextMenu;
    @FXML private BorderPane rootPane;

    // Reaction Buttons
    @FXML private Button reactionBtn1, reactionBtn2, reactionBtn3, reactionBtn4, reactionBtn5, reactionBtn6;

    private ChatClient chatClient;
    private final ObservableList<Message> messages = FXCollections.observableArrayList();
    private final ObservableList<String> onlineUsers = FXCollections.observableArrayList();

    private static final String[] EMOJIS = {
            "😀", "😂", "😍", "👍", "🔥", "✨", "❤️", "🙏", "🎉", "😎", "🤝", "💯",
            "🤣", "😊", "😇", "🙂", "😉", "😘", "😋", "😛", "😜", "🙄", "😴"
    };

    @FXML
    public void initialize() {
        messageListView.setItems(messages);
        onlineUsersList.setItems(onlineUsers);

        // កំណត់ CellFactory សម្រាប់ការរចនា Bubble និង User List
        messageListView.setCellFactory(param -> new MessageBubbleCell(this::handleFileDownload));
        onlineUsersList.setCellFactory(param -> new UserListCell());

        // ១. រៀបចំគ្រាប់ Emoji និងប៊ូតុង Reaction
        setupEmojiPicker();
        setupReactionButtons();

        // ២. ចាប់យកព្រឹត្តិការណ៍ Enter
        if (messageInput != null) {
            messageInput.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                    handleSendMessage(null);
                    event.consume();
                }
            });
        }

        // ៣. កំណត់ Actions សម្រាប់ប៊ូតុង
        if (sendMessageBtn != null) sendMessageBtn.setOnAction(this::handleSendMessage);
        if (emojiToggleBtn != null) emojiToggleBtn.setOnAction(e -> toggleEmojiPicker());
        if (attachImageBtn != null) attachImageBtn.setOnAction(this::handleAttachImage);
        if (attachFileBtn != null) attachFileBtn.setOnAction(this::handleAttachFile);
        if (logoutBtn != null) logoutBtn.setOnAction(this::handleLogout);

        setupDynamicUI();
    }

    private void setupReactionButtons() {
        Button[] reactionBtns = {reactionBtn1, reactionBtn2, reactionBtn3, reactionBtn4, reactionBtn5, reactionBtn6};
        for (Button btn : reactionBtns) {
            if (btn != null) {
                btn.setFocusTraversable(false);
                btn.setOnAction(e -> {
                    if (messageInput != null) {
                        messageInput.appendText(btn.getText());
                        messageInput.requestFocus();
                        messageInput.positionCaret(messageInput.getText().length());
                    }
                });
            }
        }
    }

    private void setupEmojiPicker() {
        if (emojiGrid == null) return;
        emojiGrid.getChildren().clear();
        for (String emoji : EMOJIS) {
            Button b = new Button(emoji);
            b.getStyleClass().add("emoji-btn");
            b.setFocusTraversable(false);
            b.setStyle("-fx-background-color: transparent; -fx-font-size: 20px; -fx-cursor: hand;");
            b.setOnAction(e -> {
                messageInput.appendText(emoji);
                messageInput.requestFocus();
                messageInput.positionCaret(messageInput.getText().length());
            });
            emojiGrid.getChildren().add(b);
        }
    }

    @FXML
    private void toggleEmojiPicker() {
        if (emojiPickerContainer != null) {
            boolean isVisible = !emojiPickerContainer.isVisible();
            emojiPickerContainer.setVisible(isVisible);
            emojiPickerContainer.setManaged(isVisible);
            if (isVisible) emojiPickerContainer.toFront();
            messageInput.requestFocus();
        }
    }

    @FXML
    private void handleSendMessage(ActionEvent event) {
        String content = messageInput.getText();
        if (content == null || content.trim().isEmpty() || chatClient == null) return;

        Message msg = new Message(chatClient.getUsername(), content.trim(), Message.MessageType.TEXT);
        msg.setTimestamp(LocalDateTime.now());
        chatClient.sendMessage(msg, "Public");
        messageInput.clear();
        messageInput.requestFocus();
    }

    @FXML
    private void showUserMenu(ActionEvent event) {
        if (userContextMenu != null && userMenuDropdownBtn != null) {
            userContextMenu.show(userMenuDropdownBtn.getScene().getWindow(),
                    userMenuDropdownBtn.localToScreen(userMenuDropdownBtn.getBoundsInLocal()).getMinX(),
                    userMenuDropdownBtn.localToScreen(userMenuDropdownBtn.getBoundsInLocal()).getMaxY());
        }
    }

    @FXML
    private void handleAttachImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());

        if (file != null) {
            new Thread(() -> {
                try {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    String base64 = Base64.getEncoder().encodeToString(bytes);
                    Message msg = new Message(chatClient.getUsername(), "Sent an image", Message.MessageType.FILE);
                    msg.setAttachment(base64);
                    msg.setFileName(file.getName());
                    msg.setTimestamp(LocalDateTime.now());
                    chatClient.sendMessage(msg, "Public");
                } catch (IOException e) { log.error("Image upload error", e); }
            }).start();
        }
    }

    @FXML
    private void handleAttachFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (file != null) {
            new Thread(() -> {
                try {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    String base64 = Base64.getEncoder().encodeToString(bytes);
                    Message msg = new Message(chatClient.getUsername(), file.getName(), Message.MessageType.FILE);
                    msg.setAttachment(base64);
                    msg.setFileName(file.getName());
                    msg.setTimestamp(LocalDateTime.now());
                    chatClient.sendMessage(msg, "Public");
                } catch (IOException e) { log.error("File upload error", e); }
            }).start();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        if (chatClient != null) chatClient.disconnect();
        goToLogin();
    }

    public void setChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
        if (chatClient != null) {
            String username = chatClient.getUsername();
            String initial = (username == null || username.isEmpty()) ? "?" : username.substring(0, 1).toUpperCase();
            Platform.runLater(() -> {
                if (topBarUserName != null) topBarUserName.setText(username);
                if (currentUserDisplayName != null) currentUserDisplayName.setText(username);
                if (miniAvatarLabel != null) miniAvatarLabel.setText(initial);
                if (currentUserAvatarLabel != null) currentUserAvatarLabel.setText(initial);
            });
            chatClient.addMessageListener(this::handleMessage);
            chatClient.addUserListListener(this::handleUserListUpdate);
            chatClient.addErrorListener(this::handleError);
            chatClient.addConnectionListener(this::handleConnectionChange);
            chatClient.requestUserList();
        }
    }

    private void handleMessage(Message message) {
        if (message == null) return;
        Platform.runLater(() -> {
            messages.add(message);
            messageListView.scrollTo(messages.size() - 1);
        });
    }

    private void handleUserListUpdate(List<String> users) {
        Platform.runLater(() -> {
            onlineUsers.setAll(users);
            if (onlineUsersCount != null) onlineUsersCount.setText(String.valueOf(users.size()));
        });
    }

    private void handleConnectionChange(boolean connected) {
        Platform.runLater(() -> {
            if (connectionStatusLabel != null) {
                connectionStatusLabel.setText(connected ? "Connected" : "Disconnected");
                connectionStatusLabel.setTextFill(connected ? Color.GREEN : Color.RED);
            }
            if (sendMessageBtn != null) sendMessageBtn.setDisable(!connected);
        });
    }

    private void handleError(String error) {
        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, error).show());
    }

    private void setupDynamicUI() {
        messages.addListener((ListChangeListener<Message>) c -> {
            boolean isEmpty = messages.isEmpty();
            if (emptyState != null) {
                emptyState.setVisible(isEmpty);
                emptyState.setManaged(isEmpty);
            }
        });
    }

    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ecocam/project_chat_console/log_in.fxml"));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.setScene(new Scene(root, 1200, 600));
            stage.centerOnScreen();
        } catch (IOException e) { log.error("Navigation error", e); }
    }

    private void handleFileDownload(Message message) {
        if (message.getAttachment() == null) return;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(message.getFileName());
        File file = fileChooser.showSaveDialog(rootPane.getScene().getWindow());
        if (file != null) {
            try {
                Files.write(file.toPath(), Base64.getDecoder().decode(message.getAttachment()));
            } catch (IOException e) { log.error("Download error", e); }
        }
    }

    // ==================== MODERN CHAT BUBBLE CELL ====================
    private class MessageBubbleCell extends ListCell<Message> {
        private final Consumer<Message> downloadHandler;
        private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

        public MessageBubbleCell(Consumer<Message> handler) { this.downloadHandler = handler; }

        @Override
        protected void updateItem(Message msg, boolean empty) {
            super.updateItem(msg, empty);
            if (empty || msg == null) { setGraphic(null); return; }

            boolean isMe = msg.getSender() != null && chatClient != null && msg.getSender().equals(chatClient.getUsername());

            VBox container = new VBox(2);
            HBox bubbleWrapper = new HBox();
            bubbleWrapper.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            VBox bubbleContent = new VBox(5);
            bubbleContent.setPadding(new Insets(8, 12, 8, 12));
            bubbleContent.setMaxWidth(400); // រីកធំជាងមុនសម្រាប់ Full Screen

            if (isMe) {
                bubbleContent.setStyle("-fx-background-color: #10b981; -fx-background-radius: 15 15 2 15;");
            } else {
                bubbleContent.setStyle("-fx-background-color: #21262d; -fx-background-radius: 15 15 15 2;");
                Label senderName = new Label(msg.getSender());
                senderName.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px; -fx-font-weight: bold;");
                bubbleContent.getChildren().add(senderName);
            }

            // Image support
            if (msg.getAttachment() != null && isImage(msg.getFileName())) {
                try {
                    byte[] bytes = Base64.getDecoder().decode(msg.getAttachment());
                    ImageView iv = new ImageView(new Image(new ByteArrayInputStream(bytes)));
                    iv.setFitWidth(300); // រីកធំសមស្របនឹង Full Screen
                    iv.setPreserveRatio(true);
                    bubbleContent.getChildren().add(iv);
                } catch (Exception e) { log.error("Image render error"); }
            } else if (msg.getAttachment() != null) {
                Hyperlink fileLink = new Hyperlink("📎 " + msg.getFileName());
                fileLink.setTextFill(Color.WHITE);
                fileLink.setOnAction(e -> downloadHandler.accept(msg));
                bubbleContent.getChildren().add(fileLink);
            }

            if (msg.getContent() != null && !msg.getContent().equals("Sent an image")) {
                Text text = new Text(msg.getContent());
                text.setFill(Color.WHITE);
                bubbleContent.getChildren().add(new TextFlow(text));
            }

            bubbleWrapper.getChildren().add(bubbleContent);
            Label timeLabel = new Label(msg.getTimestamp() != null ? msg.getTimestamp().format(TIME_FMT) : "");
            timeLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #484f58;");
            container.getChildren().addAll(bubbleWrapper, timeLabel);
            container.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            setGraphic(container);
            setStyle("-fx-background-color: transparent; -fx-padding: 5 10;");
        }

        private boolean isImage(String name) {
            if (name == null) return false;
            String n = name.toLowerCase();
            return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif");
        }
    }

    private class UserListCell extends ListCell<String> {
        @Override
        protected void updateItem(String username, boolean empty) {
            super.updateItem(username, empty);
            if (empty || username == null) { setGraphic(null); return; }
            HBox container = new HBox(10);
            container.setAlignment(Pos.CENTER_LEFT);
            Circle statusDot = new Circle(4, Color.web("#10b981"));
            Label nameLabel = new Label(username + (chatClient != null && username.equals(chatClient.getUsername()) ? " (You)" : ""));
            nameLabel.setTextFill(Color.WHITE);
            container.getChildren().addAll(statusDot, nameLabel);
            setGraphic(container);
            setStyle("-fx-background-color: transparent; -fx-padding: 5 10;");
        }
    }
}