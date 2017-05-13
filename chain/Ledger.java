import java.util.HashMap;
import java.util.Map;

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

    public Ledger(Blockchain chain) throws Blockchain.WalkFailedException {
        this.chain = chain;
        ownership = new HashMap<Integer, Integer>();

        this.chain.walk(new Blockchain.TransactionEnumerator() {
            public void consume(int index, Transaction transaction) throws Blockchain.WalkFailedException {
                /* If the map doesn't contain an address, then add it with
                 * a balance of zero chriscoins */
                Ledger.maybeInitialiseMapEntry(ownership, transaction.src, 0);
                Ledger.maybeInitialiseMapEntry(ownership, transaction.dst, 0);

                /* On non-genesis blocks we need to perform transaction
                 * validation to ensure that nobody spends money that they
                 * don't have */
                if (index > 0) {
                    int srcCoins = ownership.get(transaction.src);
                    if (ownership.get(transaction.src) < transaction.amount) {
                        throw new TransactionValidationFailedException(transaction.src,
                                                                       srcCoins,
                                                                       transaction.dst,
                                                                       transaction.amount);
                    }
                }

                /* Transaction would have been successful. Allow this transaction
                 * on the chain and update our view */
                ownership.put(transaction.src,
                              ownership.get(transaction.src) - transaction.amount);
                ownership.put(transaction.dst,
                              ownership.get(transaction.dst) + transaction.amount);
            }
        });
    }

    /* Attempt to append a transaction to the underlying blockchain. If this
     * process fails, the transaction is just silently rejected - when the network
     * next downloads the transaction ledger it is as if it never took place */
    public boolean appendTransacton(Transaction transaction) {
        int srcCoins = ownership.get(transaction.src);
        if (srcCoins < transaction.amount) {
            return false;
        }

        /* Transaction would have been successful. Allow this transaction
         * on the chain and update our view */
        ownership.put(transaction.src,
                      srcCoins - transaction.amount);
        ownership.put(transaction.dst,
                      ownership.get(transaction.dst) + transaction.amount);

        return true;
    }
}