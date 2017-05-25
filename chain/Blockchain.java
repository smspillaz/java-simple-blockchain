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
 * genesis to the child-most block in the chain.
 *
 * The Blockchain itself is effectively immutable in that you can't validly
 * append blocks without mining them first. See BlockMiner for a wrapper
 * class that does that (although asynchronously).
 */
public class Blockchain {
    private List<Block> chain;
    private long problemDifficulty;

    public static byte[] mkHash(byte[] message, int offset, int len) throws NoSuchAlgorithmException {
        /* Rather surprisingly, MessageDigest.getInstance does not do any
         * caching of the algorithm instance and stores its own private data.
         *
         * However, the evidence seems to me that it is cheaper to just
         * create a new instance every time we need to do some hashing.
         *
         * See: http://stackoverflow.com/questions/13913075/to-pool-or-not-to-pool-java-crypto-service-providers
         */
        MessageDigest digest = MessageDigest.getInstance(Globals.hashAlg);

        /* MessageDigest.digest actually writes into an output buffer, the
         * signature byte[], int, int surprisingly does not start hashing
         * offset bytes in. So in order to get our (apparently) desired
         * behaviour here we actually need to copy the array from
         * the given offset into a new one with a specified length and then
         * hash that.
         *
         * Now, if the incoming message length is the same as the
         * desired length and there is no offset, we can just use the
         * incoming message directly */
        byte buf[];

        if (message.length == len && offset == 0) {
            buf = message;
        } else {
            buf = new byte[len];
            System.arraycopy(message, offset, buf, 0, len);
        }

        return digest.digest(buf);
    }

    public Blockchain(long problemDifficulty) {
        this.chain = new ArrayList<Block>();
        this.problemDifficulty = problemDifficulty;
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
                  + " (" + block + "). " + msg);
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

    /* Making a getter for this isn't nice, but its the only way we can
     * read this value from a deserialised chain */
    public long problemDifficulty() {
        return this.problemDifficulty;
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

            /* Also check to see if the block was mined correctly by checking
             * if the hash has a certain number of leading zeroes */
            if (!Block.satisfiesProblemDifficulty(block.hash, problemDifficulty)) {
                throw new IntegrityCheckFailedException(
                    index,
                    block,
                    " Expected hash " + DatatypeConverter.printHexBinary(block.hash) +
                    " to have at least eight leading zeroes, but it did not. The " +
                    " block was probably not mined correctly"
                );
            }
        }
    }

    /**
     * Append a new block to the chain. This does not validation on the
     * block itself, it is the responsibility of the caller to provide
     * an appropriately mined block */
    public void append(Block block) {
        this.chain.add(block);
    }

    /**
     * This is just a convenience method to validate that a chain's child most
     * block is what you expect it to be.
     *
     * Since the chain mining process operates asynchronously, you should use
     * BlockMiner.waitFor to ensure that the chain is up to date.
     */
    public byte[] tipHash() {
        return chain.size() > 0 ? chain.get(chain.size() - 1).hash : new byte[0];
    }

    /**
     * Serialise the entire chain to JSON
     *
     * Since the chain mining process operates asynchronously, you should use
     * BlockMiner.waitFor to ensure that the chain is up to date.
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