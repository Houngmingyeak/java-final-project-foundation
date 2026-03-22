package ecocam.project_chat_console;

import ecocam.project_chat_console.server.ChatServer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServerLauncher extends Application {
    private ChatServer server;
    private Thread serverThread;
    private TextArea logArea;
    private Button startButton;
    private Button stopButton;
    private TextField hostField;
    private TextField portField;

    @Override
    public void start(Stage stage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        Label titleLabel = new Label("Chat Server Control");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Host field
        hostField = new TextField("0.0.0.0"); // Listen on all interfaces by default
        hostField.setPromptText("Host IP (0.0.0.0 for all interfaces)");

        // Port field
        portField = new TextField("5000");
        portField.setPromptText("Port");

        // Show local IP addresses
        VBox ipInfoBox = new VBox(5);
        Label ipLabel = new Label("Your Local IP Addresses:");
        ipLabel.setStyle("-fx-font-weight: bold;");
        TextArea ipList = new TextArea();
        ipList.setEditable(false);
        ipList.setPrefRowCount(3);
        ipList.setText(getLocalIPAddresses());
        ipInfoBox.getChildren().addAll(ipLabel, ipList);

        startButton = new Button("Start Server");
        stopButton = new Button("Stop Server");
        stopButton.setDisable(true);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(15);

        startButton.setOnAction(e -> {
            String host = hostField.getText().trim();
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException ex) {
                log("Invalid port number");
                return;
            }
            startServer(host, port);
        });

        stopButton.setOnAction(e -> stopServer());

        root.getChildren().addAll(
            titleLabel, 
            new Label("Host:"), hostField,
            new Label("Port:"), portField,
            ipInfoBox,
            startButton, 
            stopButton, 
            new Label("Log:"), logArea
        );

        Scene scene = new Scene(root, 600, 500);
        stage.setTitle("Chat Server - Multi-Computer Setup");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            stopServer();
            Platform.exit();
        });
        stage.show();
    }

    private void startServer(String host, int port) {
        try {
            server = new ChatServer(host, port);
            serverThread = new Thread(() -> {
                server.start();
            });
            serverThread.setDaemon(true);
            serverThread.start();

            log("Server started on " + host + ":" + port);
            log("UDP Discovery Server also started on port 8888 for auto-discovery");
            startButton.setDisable(true);
            stopButton.setDisable(false);
        } catch (Exception e) {
            log("Error starting server: " + e.getMessage());
        }
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            log("Server stopped");
        }
        startButton.setDisable(false);
        stopButton.setDisable(true);
    }

    private void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    private String getLocalIPAddresses() {
        StringBuilder sb = new StringBuilder();
        try {
            // Get localhost addresses
            InetAddress[] addresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
            for (InetAddress addr : addresses) {
                if (!addr.isLoopbackAddress() && addr.getHostAddress().contains(".")) {
                    sb.append(addr.getHostAddress()).append("\n");
                }
            }
            
            // Get network interface addresses
            java.util.Enumeration<java.net.NetworkInterface> networkInterfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                java.net.NetworkInterface networkInterface = networkInterfaces.nextElement();
                java.util.Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.getHostAddress().contains(".")) {
                        if (sb.indexOf(inetAddress.getHostAddress()) == -1) {
                            sb.append(inetAddress.getHostAddress()).append(" (").append(networkInterface.getName()).append(")\n");
                        }
                    }
                }
            }
        } catch (Exception e) {
            sb.append("Unable to determine IP addresses");
        }
        return sb.toString().isEmpty() ? "No network interfaces found" : sb.toString();
    }

    public static void main(String[] args) {
        launch();
    }
}
