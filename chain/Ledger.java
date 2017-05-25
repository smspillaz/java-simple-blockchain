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
 * The Ledger class uses a blockchain internally to store transactions, but
 * is also responsible for validating transactions as they come into the
 * system. If a transaction is invalid, it gets rejected silently and never
 * ends up on the blockchain.
 *
 * The way this is done is that we maintain a 'view' over who owns what
 * amount of Chriscoins as determined by the blockchain itself. This object
 * will validate the individual transactions themselves to ensure that nobody
 * spends money that they don't own, but it does not validate that the sequence
 * of transactions was correct. That is the responsibility of the whoever is
 * validating a downloaded blockchain.
 */
public class Ledger {
    protected Blockchain chain;
    protected Map<String, Long> ownership;

    protected static <K, V> void maybeInitialiseMapEntry(Map<K, V> map,
                                                       K key,
                                                       V init) {
        if (!map.containsKey(key)) {
            map.put(key, init);
        }
    }

    public static class TransactionValidationFailedException extends Blockchain.WalkFailedException {
        @SuppressFBWarnings
        public TransactionValidationFailedException(byte[] src,
                                                    long srcCoins,
                                                    byte[] dst,
                                                    long amount) {
            super("Transaction validation failed: " + DatatypeConverter.printHexBinary(src) +
                  " -> " + DatatypeConverter.printHexBinary(dst) + ". " +
                  DatatypeConverter.printHexBinary(src) + " only has " +
                  srcCoins + " Chriscoins, but attempted to " +
                  "spend " + amount + " chriscoins");
        }
    }

    public static class BlobSignatureValidationFailedException extends Blockchain.WalkFailedException {
        @SuppressFBWarnings
        public BlobSignatureValidationFailedException(Transaction transaction,
                                                      byte[] signature) {
            super("Signature validation failed: signature " +
                  DatatypeConverter.printHexBinary(signature) + " " +
                  " was not valid for transaction " +
                  transaction.toString());
        }
    }

    public interface TransactionObserver {
        void consume(Transaction transaction);
    }

    /* Build up a view the transaction history for each public key and
     * address, optionally calling out to a TransactionObserver for each
     * transaction */
    private static Map<String, Long> walkTransactions(Blockchain chain,
                                                      List<TransactionObserver> observers) throws Blockchain.WalkFailedException {
        Map<String, Long> ownership = new HashMap<String, Long>();

        chain.walk(new Blockchain.BlockEnumerator() {
            public void consume(int index, Block block) throws Blockchain.WalkFailedException {
                SignedObject blob = new SignedObject(block.payload);
                Transaction transaction = new Transaction(blob.payload);

                String srcMapKey = DatatypeConverter.printHexBinary(transaction.sPubKey);
                String dstMapKey = DatatypeConverter.printHexBinary(transaction.rPubKey);

                /* If the map doesn't contain an address, then add it with
                 * a balance of zero chriscoins.
                 *
                 * NOTE: Converting from byte arrays to digest strings like
                 * this is a pretty lazy way to get into a map, but perhaps
                 * later we can make the public keys a first class object */
                Ledger.maybeInitialiseMapEntry(ownership,
                                               srcMapKey,
                                               0L);
                Ledger.maybeInitialiseMapEntry(ownership,
                                               dstMapKey,
                                               0L);

                long srcCoins = ownership.get(srcMapKey);

                /* Reverse transactions are never allowed */
                if (transaction.amount < 0) {
                    throw new TransactionValidationFailedException(transaction.sPubKey,
                                                                   srcCoins,
                                                                   transaction.rPubKey,
                                                                   transaction.amount);
                }

                /* On non-genesis blocks we need to perform transaction
                 * validation to ensure that nobody spends money that they
                 * don't have. On the genesis block however, we don't deduct
                 * money, only add it */
                if (index > 0 &&
                    ownership.get(srcMapKey) < transaction.amount) {
                    throw new TransactionValidationFailedException(transaction.sPubKey,
                                                                   srcCoins,
                                                                   transaction.rPubKey,
                                                                   transaction.amount);
                }

                /* Self-transactions are not valid on genesis blocks */
                if (index > 0 && srcMapKey.equals(dstMapKey)) {
                    throw new Blockchain.WalkFailedException(transaction +
                                                             " is a self transaction on a non-genesis block");
                }

                boolean signatureVerificationResult = false;

                try {
                    signatureVerificationResult = SignedObject.signatureIsValid(blob.payload,
                                                                                blob.signature,
                                                                                transaction.sPubKey);
                } catch (NoSuchAlgorithmException e) {
                    /* Not much we can do here other than re-throw and abort
                     * the walk process */
                    throw new Blockchain.WalkFailedException(e.getMessage());
                } catch (InvalidKeyException e) {
                    throw new Blockchain.WalkFailedException(transaction +
                                                             " does not have a processable signature: " +
                                                             e.getMessage());
                } catch (InvalidKeySpecException e) {
                    throw new Blockchain.WalkFailedException(transaction +
                                                             "has an invalid public key PEM contents: " +
                                                             e.getMessage());
                } catch (SignatureException e) {
                    throw new Blockchain.WalkFailedException(transaction +
                                                             " has an invalid public key: " +
                                                             e.getMessage());
                }

                /* Check to make sure that the signature is valid on the blob */
                if (!signatureVerificationResult) {
                    throw new BlobSignatureValidationFailedException(transaction,
                                                                     blob.signature);
                }

                /* Transaction would have been successful. Allow this transaction
                 * on the chain and update our view */
                ownership.put(srcMapKey,
                              Math.max(ownership.get(srcMapKey) -
                                       transaction.amount, 0L));
                ownership.put(dstMapKey,
                              ownership.get(dstMapKey) + transaction.amount);

                for (TransactionObserver observer : observers) {
                    observer.consume(transaction);
                }
            }
        });

        return ownership;
    }

    /* Construct a new ledger from a Blockchain. */
    public Ledger(Blockchain chain) throws Blockchain.WalkFailedException {
        this.chain = chain;
        this.ownership = Ledger.walkTransactions(this.chain,
                                                 new ArrayList<TransactionObserver>());
    }

    /* Construct a new ledger from a Blockchain, but also observe
     * transactions as they are validated */
    public Ledger(Blockchain chain,
                  TransactionObserver observer) throws Blockchain.WalkFailedException {
        this.chain = chain;
        this.ownership = Ledger.walkTransactions(this.chain,
                                                 Arrays.asList(new TransactionObserver[] {
                                                    observer
                                                 }));
    }

    public Ledger(Blockchain chain,
                  List<TransactionObserver> observers) throws Blockchain.WalkFailedException {
        this.chain = chain;
        this.ownership = Ledger.walkTransactions(this.chain, observers);
    }
}