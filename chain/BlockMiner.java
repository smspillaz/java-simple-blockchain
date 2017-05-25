import java.security.NoSuchAlgorithmException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A BlockMiner provider a mechanism to mine blocks for a particular
 * Blockchain. It is essentially a wrapper around a Blockchain that provides
 * asynchronous mutability. Mining blocks is a CPU intensive task.
 */
public class BlockMiner {
    public static interface MiningObserver {
        /* Called when a block gets mined, with the payload contents
         * of that block. Callers might find it interesting */
        void blockMined(byte[] payload);
    }

    private long problemDifficulty;
    private transient HashWorker worker;

    public BlockMiner(Blockchain sink,
                      long problemDifficulty) {
        this.problemDifficulty = problemDifficulty;
        this.worker = new HashWorker(sink, null);
    }

    public BlockMiner(Blockchain sink,
                      MiningObserver observer,
                      long problemDifficulty) {
        this.problemDifficulty = problemDifficulty;
        this.worker = new HashWorker(sink, observer);
    }

    public void shutdown() {
        this.worker.finishAndWait();
    }

    public BlockMiner waitFor(int index) {
        this.worker.waitFor(index);
        return this;
    }

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
            BlockMiner.PayloadValidator validator;
            long problemDifficulty;

            public HashJob(byte[] payload,
                           long problemDifficulty,
                           BlockMiner.PayloadValidator validator) {
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
        private Blockchain chain;
        private BlockMiner.MiningObserver observer;
        private int jobsProcessed;
        private int jobsSent;

        public HashWorker(Blockchain chain,
                          BlockMiner.MiningObserver observer) {
            this.jobs = new LinkedBlockingQueue<Command<HashJob>>();
            this.chain = chain;
            this.observer = observer;
            this.jobsProcessed = 0;
            this.jobsSent = 0;

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
                                    !job.validator.validate(job.payload, chain.length())) {
                                    continue;
                                }

                                byte[] parentHash = chain.tipHash();
                                int nonce = Block.mineNonce(job.payload,
                                                            parentHash,
                                                            job.problemDifficulty);
                                if (observer != null) {
                                    observer.blockMined(job.payload);
                                }

                                this.chain.append(new Block(job.payload,
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
        return this.worker.pushJob(new HashWorker.HashJob(payload,
                                                          this.problemDifficulty,
                                                          validator));
    }

    public int appendPayload(byte[] payload) {
        return this.appendPayload(payload, null);
    }
}