import java.security.DigestException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
    /**
     * PayloadValidator
     *
     * This interface describes how a payload to be appended to the
     * blockchain should be validated just before it is mined. The reason
     * why we need to have a separate interface here is because validation
     * might depend on the underlying chain being up to date. We want to check
     * to see if this payload makes any sense before appending to the chain
     * where it is stuck there permanently */
    public static interface PayloadValidator {
        /* Return true if the underlying payload should be appended to
         * the blockchain and false otherwise */
        public boolean validate(byte[] payload, int index);

        /* Couldn't mine a block for some reason. Report this to
         * whoever might be interested */
        public void onMiningFailure(byte[] payload);
    }

    public static class HashWorker extends Thread {
        public static class HashJob {
            public byte[] payload;
            Blockchain.PayloadValidator validator;
            long problemDifficulty;

            public HashJob(byte[] payload,
                           long problemDifficulty,
                           Blockchain.PayloadValidator validator) {
                this.payload = new byte[payload.length];

                System.arraycopy(payload, 0, this.payload, 0, payload.length);
                this.problemDifficulty = problemDifficulty;
                this.validator = validator;
            }
        }

        public static class Command<E> {
            static final int END_OF_QUEUE = 1;
            static final int HASH_JOB = 2;

            public E payload;
            public int cmd;

            public Command(int cmd, E payload) {
                this.cmd = cmd;
                this.payload = payload;
            }
        }

        public BlockingQueue<Command<HashJob>> jobs;
        private List<Block> chain;
        private int jobsProcessed;
        private int jobsSent;

        public HashWorker(List<Block> chain) {
            this.jobs = new LinkedBlockingQueue<Command<HashJob>>();
            this.chain = chain;
            this.jobsProcessed = new Integer(0);
            this.jobsSent = new Integer(0);

            this.start();
        }

        public void run() {
            while (true) {
                try {
                    Command<HashJob> command = jobs.take();

                    switch(command.cmd) {
                        case Command.END_OF_QUEUE:
                            return;
                        case Command.HASH_JOB: {
                            HashJob job = command.payload;

                            try {
                                if (job.validator != null &&
                                    !job.validator.validate(job.payload, chain.size())) {
                                    continue;
                                }

                                byte[] parentHash = chain.size() > 0 ? chain.get(chain.size() - 1).hash : new byte[0];
                                int nonce = Block.mineNonce(job.payload,
                                                            parentHash,
                                                            job.problemDifficulty);
                                this.chain.add(new Block(job.payload,
                                                         nonce,
                                                         parentHash));
                            } catch (NoSuchAlgorithmException e) {
                            } catch (Block.MiningException e) {
                            }
                            break;
                        }
                        default:
                            break;
                    }
                } catch (InterruptedException e) {
                } finally {
                    ++this.jobsProcessed;
                }
            }
        }

        public int pushJob(HashWorker.HashJob job) {
            this.jobs.add(new Command(Command.HASH_JOB, job));
            return ++this.jobsSent;
        }

        public void waitFor(int index) {
            /* Just spin-wait until we have dealt with this index */
            while (this.jobsProcessed < index) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                }
            }
        }

        public void finish() {
            this.jobs.add(new Command(Command.END_OF_QUEUE, null));
        }

        public void finishAndWait() {
            finish();
            try {
                this.join();
            } catch (InterruptedException e) {
            }
        }
    }

    private List<Block> chain;
    private transient HashWorker worker;
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

    public Blockchain(long problemDifficulty) throws NoSuchAlgorithmException,
                                                     Block.MiningException {
        this.chain = new ArrayList<Block>();
        this.problemDifficulty = problemDifficulty;
        this.worker = new HashWorker(this.chain);
    }

    public void shutdown() {
        this.worker.finishAndWait();
    }

    public Blockchain waitFor(int index) {
        this.worker.waitFor(index);
        return this;
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
     * Appends a new payload to the chain by creating a new block for it
     * with a reference to the child-most block as its parent. Note that
     * this does absolutely no validation to check if the payload was
     * valid - you will need to validate this before you append the block
     * to the chain.
     *
     * Note that this method will attempt to mine the block (by computing
     * its nonce) before appending it to the chain. This happens asynchronously.
     */
    public int appendPayload(byte[] payload) {
        return this.appendPayload(payload, null);
    }

    /* Append a new payload to the chain by creating a new block for it
     * with a reference to the child most block as its parent. The passed
     * in PayloadValidator will validate that the payload is sane
     * just before it is mined and the chain is guaranteed to be up to date
     * by the time that the validate method is called.
     *
     * The return value is the job identifier for the mining process. Since
     * mining can take some time and we want the server to return straight
     * away, the chain is not guarnateed to reflect the state of this payload
     * as soon as it is appended. If you want that guarantee, you can use
     * Blockchain.waitFor(jobId) to wait until that job identifier has been
     * processed */
    public int appendPayload(byte[] payload, PayloadValidator validator) {
        return worker.pushJob(new HashWorker.HashJob(payload,
                                                     problemDifficulty,
                                                     validator));
    }

    /**
     * This is just a convenience method to validate that a chain's child most
     * block is what you expect it to be.
     *
     * Since the chain mining process operates asynchronously, you should use
     * Blockchain.waitFor to ensure that the chain is up to date.
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