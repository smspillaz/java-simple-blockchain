import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class Console {

    static TextArea console;
    static Stage consoleWindow;

    // Launch the JavaFX Console Window
    public static void launch() {
        GridPane consolePane = new GridPane();

        console = new TextArea();
        console.setWrapText(true);
        console.setEditable(false);

        consolePane.add(console, 0, 0, 4, 5);

        // Console window properties
        consoleWindow = new Stage();
        consoleWindow.setTitle("Console");
        consoleWindow.setScene(new Scene(consolePane, 530, 180));
        consoleWindow.setResizable(false);
        Rectangle2D desktop = Screen.getPrimary().getVisualBounds();
        consoleWindow.setX(desktop.getMinX() + desktop.getWidth() - 550);
        consoleWindow.setY(desktop.getMinY() + desktop.getHeight() - 200);
        show();
    }

    // Show Console
    public static void show() {
        consoleWindow.show();
    }

    // Hide Console
    public static void hide() {
        consoleWindow.hide();
    }

    // Write to Console
    public static void write(String message) {
        // Print to GUI Console
        console.appendText("[" + System.currentTimeMillis() + "] " + message + "\n");
        // Print to Java Console
        System.out.println(message);
    }
}
