import java.io.File;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.stage.WindowEvent;

import static java.lang.System.exit;

public class WalletMain extends Application {
    WalletOrchestrator walletOrchestrator;
    Stage launcherWindow;
    Stage transactionWindow;
    Console console;

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage applicationStart) {
        console = new Console();

        // Set up the Wallet Orchestrator Class
        walletOrchestrator = new WalletOrchestrator(console);

        // Launch Main menu
        launcherWindow();
    }

    private void launcherWindow() {
        // Add Start Window Components
        Label hostnameLabel = new Label("Hostname");
        Label keystoreFileLabel = new Label("Keystore File");
        Label keystorePasswordLabel = new Label("Keystore Password");
        TextField hostname = new TextField();
        TextField keystoreFile = new TextField();
        Button keystore = new Button();
        Button connect = new Button();
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Keystore File", "*.jks"));
        PasswordField keystorePassword = new PasswordField();
        MenuBar menuBar = new MenuBar();
        Menu file = new Menu("File");
        menuBar.getMenus().addAll(file);
        MenuItem open = new Menu("Open .jks");
        MenuItem menuConsole = new Menu("Console");
        MenuItem menuExit = new Menu("Exit");
        file.getItems().addAll(open, menuConsole, menuExit);
        file.setStyle("-fx-mark-color: transparent; -fx-focused-mark-color: transparent");

        // Set Field Labels
        hostname.setText("https://localhost:3002/transaction");
        keystore.setText("...");
        connect.setText("Connect");
        fileChooser.setTitle("Open Client Keystore File");
        connect.setMaxSize(320, 140);

        console.write("Welcome to ChrisCoin. Enter your details to connect to the BlockChain server.");

        // Arrange the window elements
        GridPane startScreen = new GridPane();

        startScreen.add(menuBar, 0, 0, 3, 1);
        startScreen.add(hostnameLabel, 0, 1, 1, 1);
        startScreen.add(hostname, 1, 1, 1, 1);
        startScreen.add(keystoreFileLabel, 0, 2, 1, 1);
        startScreen.add(keystoreFile, 1, 2, 1, 1);
        startScreen.add(keystore, 2, 2, 1, 1);
        startScreen.add(keystorePasswordLabel, 0, 3, 1, 1);
        startScreen.add(keystorePassword, 1, 3, 1, 1);
        startScreen.add(connect, 0, 4, 3, 1);

        GridPane.setMargin(hostnameLabel, new Insets(0, 0, 0, 5));
        GridPane.setMargin(keystoreFileLabel, new Insets(0, 0, 0, 5));
        GridPane.setMargin(keystorePasswordLabel, new Insets(0, 0, 0, 5));
        GridPane.setMargin(menuBar, new Insets(0, 0, 5, 0));
        GridPane.setMargin(hostname, new Insets(0, 0, 5, 5));
        GridPane.setMargin(keystoreFile, new Insets(0, 0, 5, 5));
        GridPane.setMargin(keystore, new Insets(0, 5, 5, 5));
        GridPane.setMargin(keystorePassword, new Insets(0, 0, 5, 5));
        GridPane.setMargin(connect, new Insets(5, 5, 5, 5));

        // Launcher window properties
        launcherWindow = new Stage();
        launcherWindow.setTitle("ChrisCoin");
        launcherWindow.setScene(new Scene(startScreen, 320, 168));
        launcherWindow.setResizable(false);
        launcherWindow.show();

        // Define Event Actions
        keystore.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                File selectedFile = fileChooser.showOpenDialog(launcherWindow);
                if (selectedFile != null) {
                    keystoreFile.setText(selectedFile.getAbsolutePath());
                }
            }
        });

        connect.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                walletOrchestrator.connect(hostname.getText().trim(), keystoreFile.getText().trim(), keystorePassword.getText().trim());
            }
        });

        open.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                File selectedFile = fileChooser.showOpenDialog(launcherWindow);
                if (selectedFile != null) {
                    keystoreFile.setText(selectedFile.getAbsolutePath());
                }
            }
        });

        menuConsole.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                console.toggle();
            }
        });

        menuExit.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                exit(1);
            }
        });

        launcherWindow.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent e) {
                exit(1);
            }
        });
    }

    public void transactionWindow() {
        StackPane root = new StackPane();

        Stage stage = new Stage();
        stage.setTitle("Connected Window");
        stage.setScene(new Scene(root, 450, 450));
        stage.show();
    }

    private void showTransactionWindow() {
        transactionWindow.show();
    }

    private void hideTransactionWindow() {
        transactionWindow.hide();
    }

    private void hideLauncher() {
        launcherWindow.hide();
    }
}
