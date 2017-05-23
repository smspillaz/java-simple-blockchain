import java.io.File;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.stage.WindowEvent;

import javax.xml.bind.DatatypeConverter;

import java.security.NoSuchAlgorithmException;

import static java.lang.System.exit;

public class WalletMain extends Application {
    LauncherWindow launcherWindow;
    TransactionWindow transactionWindow;
    Console console;

    String publicKey;

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage applicationStart) {
        console = new Console();

        // Start a new Connection Window
        launcherWindow = new LauncherWindow();
    }

    private class LauncherWindow {
        Stage launcherWindow;

        public LauncherWindow() {
            // Add Start Window Components
            Label hostnameLabel = new Label("Hostname");
            Label keystoreFileLabel = new Label("Keystore File");
            Label keystorePasswordLabel = new Label("Keystore Password");
            Label publicKeyLabel = new Label("Public Key");
            TextField hostname = new TextField();
            TextField keystoreFile = new TextField();
            TextField publicKey = new TextField();
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
            hostname.setText("localhost");
            keystore.setText("...");
            connect.setText("Connect");
            fileChooser.setTitle("Open Client Keystore File");
            connect.setMaxSize(320, 160);

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
            startScreen.add(publicKeyLabel, 0, 4, 1, 1);
            startScreen.add(publicKey, 1, 4, 1, 1);
            startScreen.add(connect, 0, 5, 3, 1);

            GridPane.setMargin(hostnameLabel, new Insets(0, 0, 0, 5));
            GridPane.setMargin(keystoreFileLabel, new Insets(0, 0, 0, 5));
            GridPane.setMargin(keystorePasswordLabel, new Insets(0, 0, 0, 5));
            GridPane.setMargin(publicKeyLabel, new Insets(0, 0, 0, 5));
            GridPane.setMargin(menuBar, new Insets(0, 0, 5, 0));
            GridPane.setMargin(hostname, new Insets(0, 0, 5, 5));
            GridPane.setMargin(keystoreFile, new Insets(0, 0, 5, 5));
            GridPane.setMargin(keystore, new Insets(0, 5, 5, 5));
            GridPane.setMargin(keystorePassword, new Insets(0, 0, 5, 5));
            GridPane.setMargin(publicKey, new Insets(0, 0, 5, 5));
            GridPane.setMargin(connect, new Insets(5, 5, 5, 5));

            // Launcher window properties
            launcherWindow = new Stage();
            launcherWindow.setTitle("ChrisCoin");
            launcherWindow.setScene(new Scene(startScreen, 320, 200));
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
                    authenticateLogin(hostname.getText().trim(),
                                      keystoreFile.getText().trim(),
                                      keystorePassword.getText().trim(),
                                      publicKey.getText().trim());
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
                    Platform.exit();
                }
            });

            launcherWindow.setOnCloseRequest(new EventHandler<WindowEvent>() {
                public void handle(WindowEvent e) {
                    Platform.exit();
                }
            });
        }

        public void hide() {
            launcherWindow.hide();
        }
    }

    private class TransactionWindow {
        Stage transactionWindow;

        Label publicKey = new Label("Public Key:");
        Label coins = new Label("Coins Available:");
        TextArea ledger = new TextArea();

        public TransactionWindow() {
            // Add Window Components
            MenuBar menuBar = new MenuBar();
            Menu file = new Menu("File");
            menuBar.getMenus().addAll(file);
            MenuItem menuTransaction = new Menu("New Transaction");
            MenuItem menuConsole = new Menu("Console");
            MenuItem menuExit = new Menu("Exit");
            file.getItems().addAll(menuTransaction, menuConsole, menuExit);
            file.setStyle("-fx-mark-color: transparent; -fx-focused-mark-color: transparent");
            Button newTransaction = new Button();

            // Set Labels
            ledger.setWrapText(true);
            ledger.setEditable(false);
            newTransaction.setText("New Transaction");
            newTransaction.setMaxSize(400, 80);
            ledger.setPrefRowCount(20);
            ledger.setStyle("-fx-focus-color: transparent;" +
                    "-fx-faint-focus-color: transparent; " +
                    "-fx-border-style: solid; " +
                    "-fx-border-width: 1px; " +
                    "-fx-indent: 0px; " +
                    "-fx-border-color: #CCC;");

            // Arrange the window elements
            GridPane transactionScreen = new GridPane();

            transactionScreen.add(menuBar, 0, 0, 4, 1);
            transactionScreen.add(publicKey, 0, 1, 1, 1);
            transactionScreen.add(coins, 0, 2, 1, 1);
            transactionScreen.add(ledger, 0, 3, 4, 4);
            transactionScreen.add(newTransaction, 0, 9, 4, 1);

            GridPane.setMargin(menuBar, new Insets(0, 0, 5, 0));
            GridPane.setMargin(publicKey, new Insets(5, 0, 0, 5));
            GridPane.setMargin(coins, new Insets(5, 0, 5, 5));
            GridPane.setMargin(newTransaction, new Insets(5, 5, 0, 5));

            // Transaction Window Properties
            transactionWindow = new Stage();
            transactionWindow.setTitle("My Transactions");
            transactionWindow.setScene(new Scene(transactionScreen, 400, 455));
            transactionWindow.setResizable(false);
            transactionWindow.show();

            // Define Event Actions
            newTransaction.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent event) {
                    resetConnection();
                }
            });

            menuTransaction.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent event) {
                }
            });

            menuConsole.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent event) {
                    console.toggle();
                }
            });

            menuExit.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent event) {
                    Platform.exit();
                }
            });

            transactionWindow.setOnCloseRequest(new EventHandler<WindowEvent>() {
                public void handle(WindowEvent e) {
                    Platform.exit();
                }
            });
        }

        public void show() {
            transactionWindow.show();
        }

        public void hide() {
            transactionWindow.hide();
        }

        public void displayCoins(int count) {
            coins.setText("Coins Available: " + Integer.toString(count));
        }

        public void displayTransactions(String transactions) {
            ledger.setText(transactions);
        }

        public void displayPublicKey(String key) {
            publicKey.setText("Public Key: " + key);
        }
    }

    private static class NewTransactionWindow {
        public NewTransactionWindow() {
            Stage newTransactionWindow;

            // Add Window Components
            Label publicKeyLabel = new Label("User Public Key");
            Label coinsLabel = new Label("Coins");
            TextField publicKey = new TextField();
            TextField coins = new TextField();
            Button makeTransaction = new Button();

            // Set Labels
            makeTransaction.setText("Send Coins");
            makeTransaction.setMaxSize(400, 100);

            // Arrange the window elements
            GridPane newTransactionScreen = new GridPane();

            newTransactionScreen.add(publicKeyLabel, 0, 0, 1, 1);
            newTransactionScreen.add(publicKey, 1, 0, 1, 1);
            newTransactionScreen.add(coinsLabel, 0, 1, 1, 1);
            newTransactionScreen.add(coins, 1, 1, 1, 1);
            newTransactionScreen.add(makeTransaction, 0, 2, 2, 1);

            GridPane.setMargin(publicKeyLabel, new Insets(5, 5, 5, 5));
            GridPane.setMargin(coinsLabel, new Insets(5, 5, 5, 5));
            GridPane.setMargin(publicKey, new Insets(5, 0, 5, 0));
            GridPane.setMargin(coins, new Insets(5, 0, 0, 0));
            GridPane.setMargin(makeTransaction, new Insets(5, 0, 5, 5));

            // Transaction Window Properties
            newTransactionWindow = new Stage();
            newTransactionWindow.setTitle("New Transaction");
            newTransactionWindow.setScene(new Scene(newTransactionScreen, 280, 110));
            newTransactionWindow.setResizable(false);
            newTransactionWindow.setX(200);
            newTransactionWindow.setY(200);
            newTransactionWindow.show();

            // Define Event Actions
            makeTransaction.setOnAction(new EventHandler<ActionEvent>() {
                public void handle(ActionEvent event) {
                    // Do something on Button Click
                }
            });
        }
    }

    private void authenticateLogin(String host,  String keyFile, String keyPass, String pblKey) {
        // Create a new Wallet Orchestrator Class
        WalletConnectionLoggingWrapper orchestrator = new WalletConnectionLoggingWrapper(this.console,
                                                                                         host,
                                                                                         keyFile,
                                                                                         keyPass);

        if (pblKey.trim().length() == 0) {
            console.write("A required parameter is missing. Please ensure you have entered your public key");
            return;
        }

        // If we are successfully connected, save our connection details globally
        this.publicKey = pblKey;

        // Load the Transaction Window
        transactionWindow = new TransactionWindow();
        fetchWindowData(orchestrator);
        launcherWindow.hide();
    }

    // Clear Connection and Reset Application
    private void resetConnection() {
        // Hide Windows
        transactionWindow.hide();
        launcherWindow.hide();

        // Stored Variables Garbage Collector
        this.publicKey = null;

        // Window Classes Garbage Collector
        transactionWindow = null;
        launcherWindow = null;

        console.write("Disconnected");

        // Open a new Connection Window
        launcherWindow = new LauncherWindow();
    }

    // Load a list of users transactions & currency on the transaction window
    private void fetchWindowData(WalletConnectionLoggingWrapper orchestrator) {
        /* Get the transaction log for our wallet ID (hardcoded to 0) */
        TransactionHistory history = orchestrator.history(
            DatatypeConverter.printHexBinary(Globals.convertToByteArray(0L,
                                                                        Globals.nBytesKeys))
        );

        if (history == null) {
            console.write("The BlockChain could not be downloaded. Please reconnect.");
            this.resetConnection();
            return;
        }

        transactionWindow.displayPublicKey(this.publicKey);
        transactionWindow.displayCoins(history.balance());
        transactionWindow.displayTransactions(history.log());
    }
}
