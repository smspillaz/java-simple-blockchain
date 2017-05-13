import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.KeyStoreException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

    public class WalletCLI {
        public static void usage() {
            System.err.println("WalletMain SERVER_HOST SERVER_JKS_KEYSTORE");
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

        WalletOrchestrator.connect(serverHost, serverCertificateKeyStore, System.getenv("KEYSTORE_PASSWORD"));
    }
}
