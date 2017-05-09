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


    public static void main(String[] args) throws IOException,
                                                  CertificateException,
                                                  NoSuchAlgorithmException,
                                                  KeyStoreException,
                                                  KeyManagementException,
                                                  UnrecoverableKeyException {


        launch(args);



        if (args.length < 2) {
            usage();
            System.exit(1);
        }
        String serverHost = args[0];
        String serverCertificateKeyStore = args[1];

        // Send a HTTPS request to server
        SSLContext context = createSSLContextForKeyFileStream(new FileInputStream(serverCertificateKeyStore),
                                                              System.getenv("KEYSTORE_PASSWORD")
                                                                    .toCharArray());

        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

        URL url = new URL("https://" + serverHost + ":3002/transaction");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        InputStream response = connection.getInputStream();

        Scanner s = new Scanner(response, "UTF-8").useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";

        System.out.println(result);
    }

    @Override
    public void start(Stage applicationStart) {
        // Add Start Window Components
        Label hostnameLabel = new Label("Hostname");
        Label keystoreFileLabel = new Label("Keystore File");
        Label keystorePasswordLabel = new Label("Keystore Password");
        TextField hostname = new TextField();
        TextField keystoreFile = new TextField();
        TextArea console = new TextArea();
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
                transactionWindow();
                File selectedFile = fileChooser.showOpenDialog(applicationStart);
                if (selectedFile != null) {
                    // transactionWindow();
                    System.out.println(selectedFile.getAbsolutePath());
                    // textField.setText(selectedFile.getAbsolutePath());
                }
                applicationStart.hide();
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
        stage.setTitle("My New Stage Title");
        stage.setScene(new Scene(root, 450, 450));
        stage.show();
    }

    public static void usage() {
        System.err.println("WalletMain SERVER_HOST SERVER_JKS_KEYSTORE");
    }

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
}
