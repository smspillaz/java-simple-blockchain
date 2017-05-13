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
        /* In the case of the commandline interface, if something fails, we
         * just throw an exception and let it propogate. */
        WalletOrchestrator walletOrchestrator = new WalletOrchestrator();

        if (args.length < 2) {
            usage();
            System.exit(1);
        }
        String serverHost = args[0];
        String serverCertificateKeyStore = args[1];

        walletOrchestrator.connect(serverHost,
                                   serverCertificateKeyStore,
                                   System.getenv("KEYSTORE_PASSWORD"));
    }
}
