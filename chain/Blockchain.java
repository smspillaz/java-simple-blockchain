import java.util.List;
import java.util.ArrayList;
import java.security.NoSuchAlgorithmException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

/**
 * The Blockchain class just keeps a list of blocks and transactions.
 * It doesn't care about whether the transactions themselves are valid, it
 * is assumed that some wrapper class will inspect the chain in order to
 * make sure that a transaction that is about to be appended makes sense.
 *
 * Each block comprises of a transaction and a parent block, and its hash
 * is influced by the parent-most block's hash. The chain also provides a
 * mechanism to iterate over all the prior transactions starting from the
 * genesis to the child-most block in the chain */
public class Blockchain {
    private List<Block> chain;
    public Blockchain() throws NoSuchAlgorithmException {
        chain = new ArrayList<Block>();
        /* On the construction of the blockchain, create a genesis node.
         * Note that right now, we are not signing transactions */
        chain.add(new Block(new Transaction(0, 0, 50, 0),
                            null));
    }

    public static class WalkFailedException extends Exception {
        public WalkFailedException(String msg) {
            /* Perhaps specify block hash */
            super("Chain walk failed: " + msg);
        }
    }

    public interface TransactionEnumerator {
        void consume(int index, Transaction transaction) throws WalkFailedException;
    }

    public void walk(TransactionEnumerator enumerator) throws WalkFailedException {
        int size = chain.size();
        for (int i = 0; i < size; ++i) {
            enumerator.consume(i, chain.get(i).getTransaction());
        }
    }

    private void validate() {
    }

    /**
     * Appends a new transaction to the chain by creating a new block for it
     * with a reference to the child-most block as its parent. Note that
     * this does absolutely no validation to check if the transaction was
     * valid - you will need to validate this before you append the block
     * to the chain.
     */
    public void appendTransaction(Transaction transaction) throws NoSuchAlgorithmException {
        chain.add(new Block(transaction, chain.get(chain.size() - 1)));
    }

    /**
     * This is just a convenience method to validate that a chain's child most
     * block is what you expect it to be
     */
    public byte[] tipHash() {
        Block lastBlock = chain.get(chain.size() - 1);
        return lastBlock.hash;
    }

    /**
     * Serialise the entire chain to JSON
     */
    public String serialise() {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.toJson(this).toString();
    }

    public static Blockchain deserialise(String json) throws NoSuchAlgorithmException,
                                                             IntegrityCheckFailedException {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        Blockchain result = gson.fromJson(json, Blockchain.class);

        /* Call result.validate now. If something goes wrong, we'll propogate
         * an exception up to the caller */
        result.validate();
        return result;
    }
}