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
 * The purpose of this class is to wrap a WalletOrchestractor and a Logger,
 * catching exceptions and displaying a sensible error message in the console
 * when something goes wrong
 */
class WalletConnectionLoggingWrapper {
    private Logger logger;
    private WalletOrchestrator wallet;

    public WalletConnectionLoggingWrapper(Logger logger,
                                          WalletOrchestrator wallet) {
        this.logger = logger;
        this.wallet = wallet;
    }

    public void connect(String host,
                        String keystore,
                        String password) {
        logger.write("Attempting to connect...");

        try {
            logger.write(this.wallet.connect(host, keystore, password));
        } catch (IllegalArgumentException err) {
            logger.write(err.toString());
        }catch (FileNotFoundException err) {
            logger.write("File not found: " + keystore);
        } catch (MalformedURLException err) {
            logger.write("Host " + host + " is not a valid hostname");
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
}