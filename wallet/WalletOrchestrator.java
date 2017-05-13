import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.IllegalArgumentException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Scanner;

public class WalletOrchestrator {
    private SSLContext createSSLContextForKeyFileStream(InputStream keyStoreStream,
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

    public String connect(String host, String keystore, String password) throws FileNotFoundException,
                                                                                MalformedURLException,
                                                                                CertificateException,
                                                                                IOException,
                                                                                NoSuchAlgorithmException,
                                                                                KeyStoreException,
                                                                                KeyManagementException,
                                                                                UnrecoverableKeyException {
        if (host.isEmpty() || keystore.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("A required parameter is missing. Please ensure you have set " +
                                               "the server host, provided a certificate key and password.");
        }

        FileInputStream keyStoreStream = new FileInputStream(keystore);
        SSLContext context;

        try {
            context = createSSLContextForKeyFileStream(keyStoreStream,
                                                       password.toCharArray());
        } finally {
            keyStoreStream.close();
        }

        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

        URL url = new URL("https://" + host + ":3002/transaction");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        InputStream response = connection.getInputStream();

        Scanner s = new Scanner(response, "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public String fetchBlockchain(String host, String keystore, String password) throws FileNotFoundException,
                                                                                        MalformedURLException,
                                                                                        CertificateException,
                                                                                        IOException,
                                                                                        NoSuchAlgorithmException,
                                                                                        KeyStoreException,
                                                                                        KeyManagementException,
                                                                                        UnrecoverableKeyException {
        if (host.isEmpty() || keystore.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("A required parameter is missing. Please ensure you have set " +
                    "the server host, provided a certificate key and password.");
        }

        FileInputStream keyStoreStream = new FileInputStream(keystore);
        SSLContext context;

        try {
            context = createSSLContextForKeyFileStream(keyStoreStream,
                    password.toCharArray());
        } finally {
            keyStoreStream.close();
        }

        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

        URL url = new URL("https://" + host + ":3002/download_blockchain");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        InputStream response = connection.getInputStream();

        Scanner s = new Scanner(response, "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
