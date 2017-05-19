import java.security.DigestException;
import java.util.List;
import java.util.ArrayList;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.security.MessageDigest;

/**
 * The Blockchain class just keeps a list of blocks and transactions.
 * It doesn't care about whether the transactions themselves are valid, it
 * is assumed that some wrapper class will inspect the chain in order to
 * make sure that a transaction that is about to be appended makes sense.
 *
 * Each block comprises of a transaction and a parent block, and its hash
 * is influenced by the parent-most block's hash. The chain also provides a
 * mechanism to iterate over all the prior transactions starting from the
 * genesis to the child-most block in the chain */

public class Blockchain {
    private List<Block> chain;
    private  MessageDigest digest;
    public static byte[] mkHash(byte[] message, int offset, int len) {
        /* Rather surprisingly, MessageDigest.getInstance does not do any
         * caching of the algorithm instance and stores its own private data.
         *
         * However, the evidence seems to me that it is cheaper to just
         * create a new instance every time we need to do some hashing.
         *
         * See: http://stackoverflow.com/questions/13913075/to-pool-or-not-to-pool-java-crypto-service-providers
         */
        MessageDigest digest = MessageDigest.getInstance(Globals.hashAlg);
        try {
            return digest.digest(message, offset, len);
        } catch (DigestException e){
            // TODO sort out proper logging and error handling
        }

        return null;
    }

    public Blockchain() throws NoSuchAlgorithmException {
        digest = MessageDigest.getInstance(Globals.hashAlg); // already throws it's own NoSuchAlgorithmException
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

    public static class IntegrityCheckFailedException extends Exception {
        public IntegrityCheckFailedException(int index, Block block, String msg) {
            super("Blockchain integrity check failed at block " + index
                  + " (" + block.getTransaction() + "). " + msg);
        }
    }

    public interface BlockEnumerator {
        void consume(int index, Block block) throws WalkFailedException;
    }

    public void walk(BlockEnumerator enumerator) throws WalkFailedException {
        int size = chain.size();
        for (int i = 0; i < size; ++i) {
            enumerator.consume(i, chain.get(i));
        }
    }

    /**
     * parentBlockHash
     *
     * Get the hash of the parent block. This function is effectively
     * internal and intended to be used by functions that need to compute
     * block hashes. The consumer of the blockchain should usually not
     * need to use this.
     *
     * Note that as there is a genesis block, not all blocks are guaranteed
     * to have a hash. The caller must handle this case.
     *
     * It is an error to pass an index that is out of bounds.
     */
    public byte[] parentBlockHash(int index) {
        return index > 0 ? chain.get(index - 1).hash : null;
    }

    public int length() {
        return chain.size();
    }

    /**
     * validate
     *
     * Validate the integrity of the underlying blockchain. This does not
     * check the payloads of each block (the transactions) but rather checks
     * that the hash of each block (starting from the child most block to
     * the parent most block) computes correctly. Implicit in this check
     * is whether the nonce was a valid proof of work, since the nonce is
     * included in the block itself.
     *
     * Throws Blockchain.IntegrityCheckFailedException if something goes wrong
     */
    private void validate() throws NoSuchAlgorithmException,
                                   IntegrityCheckFailedException {
        int index = chain.size();
        while (index-- > 0) {
            Block block = chain.get(index);
            byte[] computedHash = block.computeContentHash(parentBlockHash(index));
            if (!Arrays.equals(block.hash, computedHash)) {
                throw new IntegrityCheckFailedException(
                    index,
                    block,
                    " Expected hash " + DatatypeConverter.printHexBinary(computedHash) +
                    " but the block hash was instead " + DatatypeConverter.printHexBinary(block.hash)
                );
            }

            /* Also check the hash to make sure that it has a certain number
             * of leading zeroes (TODO) */
        }
    }

    /**
     * Appends a new transaction to the chain by creating a new block for it
     * with a reference to the child-most block as its parent. Note that
     * this does absolutely no validation to check if the transaction was
     * valid - you will need to validate this before you append the block
     * to the chain.
     */
    public void appendTransaction(Transaction transaction) throws NoSuchAlgorithmException {
        chain.add(new Block(transaction, parentBlockHash(chain.size())));
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