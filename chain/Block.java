import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class Block {
    private Transaction transaction;
    public byte[] hash;

    public Block(Transaction transaction,
                 byte[] parentHash) throws NoSuchAlgorithmException {
        this.transaction = transaction;
        this.hash = this.computeContentHash(parentHash);
    }

    /* Perhaps expose a method to apply this to some ledger that
     * gets computed over time instead of a public field */
    Transaction getTransaction() {
        return transaction;
    }

    public static class MiningException extends Exception {
        public MiningException() {
            super("Ran out of numbers whilst mining");
        }
    }

    public static int mineNonce(byte[] payload,
                                byte[] parentHash) throws NoSuchAlgorithmException,
                                                          MiningException {
        parentHash = parentHash != null ? parentHash : new byte[0];
        byte[] blockContents = Globals.concatByteArrays(new byte[][] {
            parentHash,
            payload,
            new byte[Globals.nBytesNonce]
        });

        // cycle through all 2 ^ 64 values until loops back to 0
        for (int nonce = 0; nonce <= Globals.maxValNonce; nonce++) {
            System.arraycopy(Globals.convertToByteArray((long) nonce, Globals.nBytesBlockChainHash),
                             0,
                             blockContents,
                             payload.length + parentHash.length,
                             Globals.nBytesBlockChainHash);

            byte[] blockChainHash = Blockchain.mkHash(blockContents, 0, blockContents.length);

            /* First byte is all zeroes, we have our nonce */
            if (blockChainHash[blockChainHash.length - 1] == 0) {
                return nonce;
            }
        }

        throw new MiningException();
    }

    public byte[] computeContentHash(byte[] parentHash) throws NoSuchAlgorithmException {
        /* Compute a hash based on the transaction itself
         * and the parent block.
         *
         * Note that this hash does not include the computed hash */
        parentHash = parentHash != null ? parentHash : new byte[0];

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] transactionHash = transaction.hash();
        byte[] message = new byte[parentHash.length + transactionHash.length];
        System.arraycopy(parentHash, 0, message, 0, parentHash.length);
        System.arraycopy(transactionHash,
                         0,
                         message,
                         parentHash.length,
                         transactionHash.length);

        return digest.digest(message);
    }

    public String toString() {
        return DatatypeConverter.printHexBinary(this.hash) + ": " + this.transaction;
    }
}