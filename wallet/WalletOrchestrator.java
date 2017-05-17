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
    public String host;

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

    public WalletOrchestrator(String host, String keystore, String password) throws FileNotFoundException,
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
        this.host = host;
    }

    private String request(String endpoint) throws MalformedURLException,
                                                   IOException {
        URL url = new URL("https://" + host + ":3002/" + endpoint);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        InputStream response = connection.getInputStream();

        Scanner s = new Scanner(response, "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public String transaction() throws MalformedURLException,
                                       IOException {
        return request("transaction");
    }

    public Blockchain fetchBlockchain() throws MalformedURLException,
                                               IOException,
                                               NoSuchAlgorithmException,
                                               Blockchain.IntegrityCheckFailedException {
        return Blockchain.deserialise(request("download_blockchain"));
    }

    public static int ascertainBalanceFromChain(int walletID, Blockchain chain, Logger logger) throws Blockchain.WalkFailedException {
        return new WalletBlockchainConsumer(chain).ascertainBalance(walletID, logger);
    }

    public int ascertainBalance(int walletID) throws Blockchain.WalkFailedException,
                                                     MalformedURLException,
                                                     IOException,
                                                     NoSuchAlgorithmException,
                                                     Blockchain.IntegrityCheckFailedException {
        return ascertainBalanceFromChain(walletID, fetchBlockchain(), null);
    }
}
