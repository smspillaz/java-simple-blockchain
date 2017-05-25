import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;


import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import javax.xml.bind.DatatypeConverter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * The AsynchronouslyMutableLedger class is an extension on Ledger that allows
 * for new transactions to be appended, as opposed to just viewed. In order
 * to do this, it requires a BlockMiner to be passed in.
 */
public class AsynchronouslyMutableLedger extends Ledger {
    private BlockMiner miner;

    public AsynchronouslyMutableLedger(Blockchain chain,
                                       BlockMiner miner) throws Blockchain.WalkFailedException {
        super(chain, new ArrayList<Ledger.TransactionObserver>());
        this.miner = miner;
    }

    public static void logTransactionRejectionFailure(String reason) {
        System.out.println("[chain] Rejecting transaction: " + reason);
    }

    /* Attempt to append a transaction to the underlying blockchain. If this
     * process fails, the transaction is just silently rejected - when the network
     * next downloads the transaction ledger it is as if it never took place.
     *
     * If you need to check the success of a transaction, you'll need to wait
     * until all transactions have completed (you can use sync() on the underlying
     * blockchain to wait until mining has finished to a particular point) */
    public int appendSignedTransaction(SignedObject blob) throws NoSuchAlgorithmException {
        return miner.appendPayload(blob.serialize(), new BlockMiner.PayloadValidator() {
            public boolean validate(byte[] payload, int index) {
                try {
                    validateAndProcessPayload(payload,
                                              index,
                                              ownership,
                                              new ArrayList<TransactionObserver>());
                } catch (Ledger.TransactionValidationFailedException e) {
                    logTransactionRejectionFailure(e.getMessage());
                    return false;
                } catch (Ledger.BlobSignatureValidationFailedException e) {
                    logTransactionRejectionFailure(e.getMessage());
                    return false;
                } catch (Blockchain.WalkFailedException e) {
                    logTransactionRejectionFailure(e.getMessage());
                    return false;
                }

                return true;
            }

            public void onMiningFailure(byte[] payload) {
                /* Same thing, but log error */
                SignedObject blob = new SignedObject(payload);
                Transaction transaction = new Transaction(blob.payload);

                logTransactionRejectionFailure("Couldn't find a valid solution to " +
                                               "mine a block for " +
                                               transaction + ", sorry");
            }
        });
    }
}