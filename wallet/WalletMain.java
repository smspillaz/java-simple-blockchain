import java.net.URLConnection;
import java.io.InputStream;
import java.io.IOException;
import java.util.Scanner;

import java.io.InputStream;
import java.io.FileInputStream;
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


public class WalletMain {
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

    public static void main(String[] args) throws IOException,
                                                  CertificateException,
                                                  NoSuchAlgorithmException,
                                                  KeyStoreException,
                                                  KeyManagementException,
                                                  UnrecoverableKeyException {
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
}
