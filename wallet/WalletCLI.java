import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.KeyStoreException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

public class WalletCLI {
    private static class CLILogger implements Logger {
        public void write(String msg) {
            System.out.println(msg);
        }
    }

    public static void usage(Logger logger) {
        logger.write("WalletMain SERVER_HOST SERVER_JKS_KEYSTORE");
    }

    public static void main(String[] args) throws IOException,
            CertificateException,
            NoSuchAlgorithmException,
            KeyStoreException,
            KeyManagementException,
            UnrecoverableKeyException {

        Logger logger = new WalletCLI.CLILogger();

        WalletOrchestrator walletOrchestrator = new WalletOrchestrator(logger);

        if (args.length < 2) {
            usage(logger);
            System.exit(1);
        }
        String serverHost = args[0];
        String serverCertificateKeyStore = args[1];

        walletOrchestrator.connect(serverHost, serverCertificateKeyStore, System.getenv("KEYSTORE_PASSWORD"));
    }
}
