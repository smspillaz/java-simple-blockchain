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
import javafx.scene.Scene;
import javafx.scene.control.Button;
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

        TextField textField = new TextField();

        PasswordField passwordField = new PasswordField();
        TextArea textArea = new TextArea();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Client Pem File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.jks"));

        Button btn = new Button();
        btn.setText("Launch Transaction Window");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                transactionWindow();
                File selectedFile = fileChooser.showOpenDialog(applicationStart);
                if (selectedFile != null) {
                    // transactionWindow();
                    System.out.println(selectedFile.getAbsolutePath());
                    textField.setText(selectedFile.getAbsolutePath());
                }
                applicationStart.hide();
            }
        });



        StackPane root = new StackPane();
        root.getChildren().add(btn);
        root.getChildren().add(textField);
        root.getChildren().add(passwordField);


        // Launch Welcome Window
        applicationStart.setTitle("Welcome to Chris Coin");
        applicationStart.setScene(new Scene(root, 300, 250));
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
