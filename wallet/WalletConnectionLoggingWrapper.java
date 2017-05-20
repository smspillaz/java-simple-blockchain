import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.security.KeyStoreException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * WalletConnctionLoggingWrapper
 *
 * The purpose of this class is to wrap a WalletOrchestactor and a Logger,
 * catching exceptions and displaying a sensible error message in the console
 * when something goes wrong. Note that none of the public methods return
 * anything - this is intentional, since we're just logging the result.
 *
 * If we're unable to construct a WalletOrchestrator, all subsequent operations
 * on this class will be no-ops.
 */
class WalletConnectionLoggingWrapper {
    private Logger logger;
    private WalletOrchestrator wallet;

    public WalletConnectionLoggingWrapper(Logger logger,
                                          String host,
                                          String keystore,
                                          String password) {
        this.logger = logger;
        try {
            this.wallet = new WalletOrchestrator(host, keystore, password);
        } catch (IllegalArgumentException err) {
            logger.write(err.toString());
        } catch (FileNotFoundException err) {
            logger.write("File not found: " + keystore);
        } catch (CertificateException err) {
            logger.write(err.toString());
        } catch (IOException err) {
            logger.write(err.toString());
        } catch (KeyStoreException err) {
            logger.write(err.toString());
        } catch (KeyManagementException err) {
            logger.write(err.toString());
        } catch (UnrecoverableKeyException err) {
            logger.write(err.toString());
        } catch (NoSuchAlgorithmException err) {
            logger.write(err.toString());
        }
    }

    public void transaction() {
        if (this.wallet != null) {
            logger.write("Performing Transaction");

            try {
                logger.write(this.wallet.transaction());
            } catch (MalformedURLException e) {
                logger.write("Connecting to /transaction on host yielded a malformed URL: " + e.toString());
            } catch (IOException e) {
                logger.write("Input/Output error occurred when connecting to /transaction on host: " + e.toString());
            }
        }
    }

    TransactionHistory history(String walletID) {
        if (this.wallet != null) {
            logger.write("Ascertaining balance from blockchain");

            try {
                return wallet.history(walletID);
            } catch (Blockchain.IntegrityCheckFailedException e) {
                logger.write("Blockchain integrity check failed: " + e.toString());
            } catch (Blockchain.WalkFailedException e) {
                logger.write(e.toString());
            } catch (MalformedURLException e) {
                logger.write("Connecting to /download_blockchain on host yielded a malformed URL: " + e.toString());
            } catch (IOException e) {
                logger.write("Input/Output error occurred when connecting to /download_blockchain on host: " + e.toString());
            } catch (NoSuchAlgorithmException e) {
                logger.write(e.toString());
            }
        }

        return null;
    }
}