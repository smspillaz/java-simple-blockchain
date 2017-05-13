import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Scanner;

public class WalletOrchestrator {

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

    public static void connect(String host, String keystore, String password) {
        try {
            Console.write("Attempting to connect...");

            if (host.isEmpty() || keystore.isEmpty() || password.isEmpty()) {
                Console.write("Error: A required parameter is missing. Please ensure you have set " +
                        "the server host, provided a certificate key and password.");
                return;
            }

            // Send a test HTTPS request to server to see if we can connect
            SSLContext context = createSSLContextForKeyFileStream(new FileInputStream(keystore),
                    password.toCharArray());

            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

            URL url = new URL(host);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            InputStream response = connection.getInputStream();

            Scanner s = new Scanner(response, "UTF-8").useDelimiter("\\A");
            String result = s.hasNext() ? s.next() : "";

            Console.write(result);

        } catch (Exception e) {
            Console.write(e.toString());
        }
    }

}
