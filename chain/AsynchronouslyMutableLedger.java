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

    public static void logTransactionRejectionFailure(Transaction transaction,
                                                      String reason) {
        System.out.println("[chain] Rejecting transaction " + transaction + ": " + reason);
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
                /* Re-create our SignedObject and Transaction from the mined
                 * payload */
                SignedObject blob = new SignedObject(payload);
                Transaction transaction = new Transaction(blob.payload);

                /* If the map doesn't contain an address, then add it with
                 * a balance of zero chriscoins */
                String srcMapKey = DatatypeConverter.printHexBinary(transaction.sPubKey);
                String dstMapKey = DatatypeConverter.printHexBinary(transaction.rPubKey);
                Ledger.maybeInitialiseMapEntry(ownership, srcMapKey, 0L);
                Ledger.maybeInitialiseMapEntry(ownership, dstMapKey, 0L);

                /* Transaction must have an amount */
                if (transaction.amount <= 0) {
                    logTransactionRejectionFailure(transaction,
                                                   "amount is less than or equal to zero");
                    return false;
                }

                /* We only want to do this check on non-genesis nodes, since
                 * the genesis node will have to pay money to itself */
                long srcCoins = ownership.get(srcMapKey);
                if (index > 0 && srcCoins < transaction.amount) {
                    logTransactionRejectionFailure(transaction,
                                                   "source does not have requisite coins, " +
                                                   "only has " + srcCoins);
                    return false;
                }

                /* Check to make sure that the signature is valid on the blob */
                boolean signatureVerificationResult = false;

                try {
                    signatureVerificationResult = SignedObject.signatureIsValid(blob.payload,
                                                                                blob.signature,
                                                                                transaction.sPubKey);
                } catch (InvalidKeyException e) {
                    logTransactionRejectionFailure(transaction,
                                                   "has an invalid public key: " + e.getMessage());
                    return false;
                } catch (InvalidKeySpecException e) {
                    logTransactionRejectionFailure(transaction,
                                                   "has an invalid public key PEM contents: " + e.getMessage());
                    return false;
                } catch (SignatureException e) {
                    logTransactionRejectionFailure(transaction,
                                                   "didn't have a processable signature: " + e.getMessage());
                    return false;
                } catch (NoSuchAlgorithmException e) {
                    logTransactionRejectionFailure(transaction,
                                                   " - requested hashing algorithm wasn't available: " + e.getMessage());
                }

                if (!signatureVerificationResult) {
                    logTransactionRejectionFailure(transaction,
                                                   "was not signed correctly, signature " +
                                                   DatatypeConverter.printHexBinary(blob.signature) +
                                                   " is invalid for public key " +
                                                   DatatypeConverter.printHexBinary(transaction.sPubKey));
                    return false;
                }

                /* Transaction would have been successful. Allow this transaction
                 * on the chain and update our view.
                 *
                 * As above, the first transaction gives money to itself, so
                 * just allow it to continue without updating the ledger */
                if (index > 0) {
                    ownership.put(srcMapKey,
                                  srcCoins - transaction.amount);
                }
                ownership.put(dstMapKey,
                              ownership.get(dstMapKey) + transaction.amount);

                return true;
            }

            public void onMiningFailure(byte[] payload) {
                /* Same thing, but log error */
                SignedObject blob = new SignedObject(payload);
                Transaction transaction = new Transaction(blob.payload);

                logTransactionRejectionFailure(transaction,
                                               "could not find a valid solution to " +
                                               "mine a block, sorry");
            }
        });
    }
}