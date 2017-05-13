import java.util.HashMap;
import java.util.Map;

import java.security.NoSuchAlgorithmException;

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
    private Blockchain chain;
    private Map<Integer, Integer> ownership;

    private static <K, V> void maybeInitialiseMapEntry(Map<K, V> map,
                                                       K key,
                                                       V init) {
        if (!map.containsKey(key)) {
            map.put(key, init);
        }
    }

    private static class TransactionValidationFailedException extends Blockchain.WalkFailedException {
        public TransactionValidationFailedException(int src,
                                                    int srcCoins,
                                                    int dst,
                                                    int amount) {
            super("Transaction validation failed: " + src +
                  " -> " + dst + ". " + src + " only has " +
                  srcCoins + " Chriscoins, but attempted to " +
                  "spend " + amount + " chriscoins");
        }
    } 

    public interface TransactionObserver {
        void consume(Transaction transaction);
    }

    /* Build up a view the transaction history for each public key and
     * address, optionally calling out to a TransactionObserver for each
     * transaction */
    private static Map<Integer, Integer> walkTransactions(Blockchain chain,
                                                          TransactionObserver observer) throws Blockchain.WalkFailedException {
        Map<Integer, Integer> ownership = new HashMap<Integer, Integer>();

        chain.walk(new Blockchain.TransactionEnumerator() {
            public void consume(int index, Transaction transaction) throws Blockchain.WalkFailedException {
                /* If the map doesn't contain an address, then add it with
                 * a balance of zero chriscoins */
                Ledger.maybeInitialiseMapEntry(ownership, transaction.src, 0);
                Ledger.maybeInitialiseMapEntry(ownership, transaction.dst, 0);

                int srcCoins = ownership.get(transaction.src);

                /* Reverse transactions are never allowed */
                if (transaction.amount < 0) {
                    throw new TransactionValidationFailedException(transaction.src,
                                                                   srcCoins,
                                                                   transaction.dst,
                                                                   transaction.amount);
                }

                /* On non-genesis blocks we need to perform transaction
                 * validation to ensure that nobody spends money that they
                 * don't have. On the genesis block however, we don't deduct
                 * money, only add it */
                if (index > 0 &&
                    ownership.get(transaction.src) < transaction.amount) {
                    throw new TransactionValidationFailedException(transaction.src,
                                                                   srcCoins,
                                                                   transaction.dst,
                                                                   transaction.amount);
                }

                /* Transaction would have been successful. Allow this transaction
                 * on the chain and update our view */
                ownership.put(transaction.src,
                              Math.max(ownership.get(transaction.src) -
                                       transaction.amount, 0));
                ownership.put(transaction.dst,
                              ownership.get(transaction.dst) + transaction.amount);

                if (observer != null) {
                    observer.consume(transaction);
                }
            }
        });

        return ownership;
    }

    /* Construct a new ledger from a Blockchain. */
    public Ledger(Blockchain chain) throws Blockchain.WalkFailedException {
        this.chain = chain;
        this.ownership = Ledger.walkTransactions(this.chain, null);
    }

    /* Construct a new ledger from a Blockchain, but also observe
     * transactions as they are validated */
    public Ledger(Blockchain chain,
                  TransactionObserver observer) throws Blockchain.WalkFailedException {
        this.chain = chain;
        this.ownership = Ledger.walkTransactions(this.chain, observer);
    }

    /* Attempt to append a transaction to the underlying blockchain. If this
     * process fails, the transaction is just silently rejected - when the network
     * next downloads the transaction ledger it is as if it never took place */
    public boolean appendTransaction(Transaction transaction) throws NoSuchAlgorithmException {
        /* If the map doesn't contain an address, then add it with
         * a balance of zero chriscoins */
        Ledger.maybeInitialiseMapEntry(ownership, transaction.src, 0);
        Ledger.maybeInitialiseMapEntry(ownership, transaction.dst, 0);

        /* Negative transactions never allowed */
        if (transaction.amount < 0) {
            return false;
        }

        int srcCoins = ownership.get(transaction.src);
        if (srcCoins < transaction.amount) {
            return false;
        }

        chain.appendTransaction(transaction);

        /* Transaction would have been successful. Allow this transaction
         * on the chain and update our view */
        ownership.put(transaction.src,
                      srcCoins - transaction.amount);
        ownership.put(transaction.dst,
                      ownership.get(transaction.dst) + transaction.amount);

        return true;
    }
}