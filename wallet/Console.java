import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class Console implements Logger {

    private TextArea console;
    private Stage consoleWindow;

    // Launch the JavaFX Console Window
    public Console() {
        GridPane consolePane = new GridPane();

        console = new TextArea();
        console.setWrapText(true);
        console.setEditable(false);
        console.setStyle("-fx-focus-color: transparent;" +
                "-fx-faint-focus-color: transparent; " +
                "-fx-border-style: solid; " +
                "-fx-border-width: 1px; " +
                "-fx-indent: 0px; " +
                "-fx-border-color: #CCC;");

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

    public void write(String msg) {
        console.appendText("[" + System.currentTimeMillis() + "] " + msg + "\n");
    }

    // Show Console
    public void show() {
        consoleWindow.show();
    }

    public void toggle() {
        if (consoleWindow.isShowing() == false) {
            this.show();
        } else {
            this.hide();
        }
    }

    // Hide Console
    public void hide() {
        consoleWindow.hide();
    }
}
