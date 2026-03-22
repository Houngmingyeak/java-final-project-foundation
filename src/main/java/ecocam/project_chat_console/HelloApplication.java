package ecocam.project_chat_console;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.util.Objects;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // ✅ Load FXML safely (absolute path recommended)
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(
                        getClass().getResource("/ecocam/project_chat_console/log_in.fxml")
                )
        );

        Parent root = loader.load();

        Scene scene = new Scene(root, 1200, 600);

        // ✅ Load BootstrapFX (IMPORTANT)
        scene.getStylesheets().add(
                BootstrapFX.bootstrapFXStylesheet()
        );

        // ✅ Load your custom CSS safely
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/ecocam/project_chat_console/styles.css")
                ).toExternalForm()
        );

        stage.setTitle("Chat Console - Login");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
