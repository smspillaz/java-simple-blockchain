import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.IllegalArgumentException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Scanner;

import javax.xml.bind.DatatypeConverter;

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

    private String request(String endpoint, String method, String body) throws MalformedURLException,
                                                                               IOException {
        URL url = new URL("https://" + host + ":3002/" + endpoint);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        connection.setRequestMethod(method);
        if (body != null) {
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            OutputStream os = connection.getOutputStream();
            os.write(body.getBytes("UTF-8"));
            os.close();
        }
        InputStream response = connection.getInputStream();

        Scanner s = new Scanner(response, "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public String transaction(String src,
                              String dst,
                              int amount,
                              byte[] signingKey) throws MalformedURLException,
                                                        IOException,
                                                        InvalidKeyException,
                                                        InvalidKeySpecException,
                                                        NoSuchProviderException,
                                                        NoSuchAlgorithmException,
                                                        SignatureException {
        PrivateKey key = KeyFactory.getInstance("RSA", "BC").generatePrivate(
            new PKCS8EncodedKeySpec(signingKey)
        );
        Transaction transaction = new Transaction(DatatypeConverter.parseHexBinary(src),
                                                  DatatypeConverter.parseHexBinary(dst),
                                                  amount);
        SignedObject blob = new SignedObject(transaction.serialize(), key);
        Models.Transaction record = new Models.Transaction(src,
                                                           dst,
                                                           amount,
                                                           DatatypeConverter.printHexBinary(blob.signature));
        return request("transaction", "POST", record.serialise());
    }

    public Blockchain fetchBlockchain() throws MalformedURLException,
                                               IOException,
                                               NoSuchAlgorithmException,
                                               Blockchain.IntegrityCheckFailedException {
        return Blockchain.deserialise(request("download_blockchain", "GET", null));
    }

    public static TransactionHistory transactionHistoryFromChain(String walletID, Blockchain chain) throws Blockchain.WalkFailedException {
        return new WalletBlockchainConsumer(chain).transactionHistory(walletID);
    }

    public TransactionHistory history(String walletID) throws Blockchain.WalkFailedException,
                                                              MalformedURLException,
                                                              IOException,
                                                              NoSuchAlgorithmException,
                                                              Blockchain.IntegrityCheckFailedException {
        return transactionHistoryFromChain(walletID, fetchBlockchain());
    }



}
