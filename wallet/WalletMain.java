import java.io.*;
import java.net.URLConnection;
import java.util.Scanner;

import java.io.InputStream;
import java.io.IOException;

import java.net.URL;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.HttpsURLConnection;

import java.security.cert.CertificateException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

public class WalletMain extends Application {
    // Global Console Output Object
    static TextArea console;

    private static SSLContext createSSLContextForKeyFileStream(InputStream keyStoreStream,
                                                               char[] password) throws CertificateException,
            NoSuchAlgorithmException,
            KeyStoreException,
            IOException,
            KeyManagementException,
            UnrecoverableKeyException {
        SSLContext context = SSLContext.getInstance("TLS");
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(keyStoreStream, password);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStore);

        context.init(null,
                trustManagerFactory.getTrustManagers(),
                null);

        return context;
    }

    public static void console(String message) {
        console.setText("[" + System.currentTimeMillis() + "] "
                + message +"\n" + console.getText());
    }

    @Override
    public void start(Stage applicationStart) {
        // Add Start Window Components
        Label hostnameLabel = new Label("Hostname");
        Label keystoreFileLabel = new Label("Keystore File");
        Label keystorePasswordLabel = new Label("Keystore Password");
        TextField hostname = new TextField();
        TextField keystoreFile = new TextField();
        console = new TextArea();
        console.setWrapText(true);
        console.setEditable(false);
        Button keystore = new Button();
        Button connect = new Button();
        connect.setMaxSize(320, 140);
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Keystore File", "*.jks"));
        PasswordField keystorePassword = new PasswordField();

        // Set Field Labels
        keystore.setText("...");
        connect.setText("Connect");
        hostname.setText("http://localhost/");
        fileChooser.setTitle("Open Client Keystore File");
        console("Welcome. Enter your details to connect to the blockchain server.");

        // Define Event Actions
        keystore.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                File selectedFile = fileChooser.showOpenDialog(applicationStart);
                if (selectedFile != null) {
                    keystoreFile.setText(selectedFile.getAbsolutePath());
                }
            }
        });

        connect.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    console("Attempting to connect...");

                    String host = hostname.getText().trim();
                    String keystore = keystoreFile.getText().trim();
                    String password = keystorePassword.getText().trim();

                    if (host.isEmpty() || keystore.isEmpty() || password.isEmpty()) {
                        console("Error: A required parameter is missing. Please ensure you have set " +
                        "the server host, provided a certificate key and password.");
                        return;
                    }

                    // Send a test HTTPS request to server to see if we can connect
                    SSLContext context = createSSLContextForKeyFileStream(new FileInputStream(keystore),
                            password.toCharArray());

                    HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

                    URL url = new URL("https://" + hostname.getText() + ":3002/transaction");
                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestProperty("Accept-Charset", "UTF-8");
                    InputStream response = connection.getInputStream();

                    Scanner s = new Scanner(response, "UTF-8").useDelimiter("\\A");
                    String result = s.hasNext() ? s.next() : "";

                    console(result);
                    console("Connected.");

                    transactionWindow();
                    applicationStart.hide();
                } catch (Exception e) {
                    console(e.toString());
                }
            }
        });

        // Arrange the window elements
        GridPane window = new GridPane();

        window.add(hostnameLabel, 0, 0, 1, 1);
        window.add(hostname, 1, 0, 1, 1);
        window.add(keystoreFileLabel, 0, 1, 1, 1);
        window.add(keystoreFile, 1, 1, 1, 1);
        window.add(keystore, 2, 1, 1, 1);
        window.add(keystorePasswordLabel, 0, 2, 1, 1);
        window.add(keystorePassword, 1, 2, 1, 1);
        window.add(connect, 0, 3, 3, 1);
        window.add(console, 0, 4, 3, 1);

        GridPane.setMargin(hostname, new Insets(0, 0, 5, 5));
        GridPane.setMargin(keystoreFile, new Insets(0, 0, 5, 5));
        GridPane.setMargin(keystore, new Insets(0, 0, 5, 5));
        GridPane.setMargin(keystorePassword, new Insets(0, 0, 5, 5));
        GridPane.setMargin(connect, new Insets(5, 0, 5, 0));
        GridPane.setMargin(console, new Insets(5, 0, 5, 0));
        window.setPadding(new Insets(5, 5, 5, 5));

        // Launch Welcome Window
        applicationStart.setTitle("ChrisCoin");
        applicationStart.setScene(new Scene(window, 320, 260));
        applicationStart.setResizable(false);
        applicationStart.show();
    }

    public void transactionWindow() {
        StackPane root = new StackPane();

        Stage stage = new Stage();
        stage.setTitle("Connected Window");
        stage.setScene(new Scene(root, 450, 450));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
