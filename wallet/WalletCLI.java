import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.SignatureException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import javax.xml.bind.DatatypeConverter;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class WalletCLI {
    public static class Arguments {
        @Option(name="-keystore", usage="The Client-Side Java KeyStore to use (mandatory)", metaVar="KEYSTORE")
        public String keystore;

        @Option(name="-wallet-id", usage="The client's wallet ID (public key)", metaVar="WALLET_ID")
        public String walletID;

        @Option(name="-signing-key", usage="The client's secret key. Not required to read state, but required for making transactions", metaVar="SIGNING_KEY")
        public String signingKey;

        @Option(name="-amount", usage="Amount to send to a recipient", metaVar="AMOUNT")
        public Integer amount;

        @Option(name="-recipient", usage="The receiver's wallet ID (public key)", metaVar="WALLET_ID")
        public String recipient;

        @Option(name="-host", usage="The blockchain host to connect to (mandatory)", metaVar="HOST")
        public String host;

        public Arguments(String args[]) {
            CmdLineParser parser = new CmdLineParser(this);

            try {
                parser.parseArgument(args);

                if (keystore == null) {
                    throw new CmdLineException(parser, "Must provide a -keystore");
                }

                if (host == null) {
                    throw new CmdLineException(parser, "Must provide a -host");
                }

                if (walletID == null) {
                    throw new CmdLineException(parser, "Must provide a -wallet-id");
                }

                if ((recipient != null) &&
                    (signingKey == null || amount == null)) {
                    throw new CmdLineException(parser, "Must provide a -signing-key and -amount if specifying a -recipient");
                }

                if (System.getenv("KEYSTORE_PASSWORD") == null) {
                    throw new CmdLineException(parser, "Must set KEYSTORE_PASSWORD in the environment");
                }
            } catch (CmdLineException e) {
                parser.printUsage(System.err);
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    public static void main(String[] args) throws IOException,
                                                  CertificateException,
                                                  NoSuchAlgorithmException,
                                                  NoSuchProviderException,
                                                  KeyStoreException,
                                                  KeyManagementException,
                                                  UnrecoverableKeyException,
                                                  InvalidKeyException,
                                                  InvalidKeySpecException,
                                                  SignatureException,
                                                  Blockchain.WalkFailedException,
                                                  Blockchain.IntegrityCheckFailedException {
        Arguments arguments = new Arguments(args);
        Security.addProvider(new BouncyCastleProvider());

        /* In the case of the commandline interface, if something fails, we
         * just throw an exception and let it propogate. */
        WalletOrchestrator walletOrchestrator = new WalletOrchestrator(arguments.host,
                                                                       arguments.keystore,
                                                                       System.getenv("KEYSTORE_PASSWORD"));

        TransactionHistory history = walletOrchestrator.history(arguments.walletID);

        System.out.println("Current balance: " + history.balance());
        if (arguments.recipient != null) {
            System.out.println(walletOrchestrator.transaction(arguments.walletID,
                                                              arguments.recipient,
                                                              arguments.amount,
                                                              KeyGenerator.readKeyFromFile(arguments.signingKey)));
        }
    }
}
