import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class Block {
    private Transaction transaction;
    public byte[] hash;

    public Block(Transaction transaction,
                 Block parent) throws NoSuchAlgorithmException {
        this.transaction = transaction;
        this.hash = this.computeContentHash(parent);
    }

    /* Perhaps expose a method to apply this to some ledger that
     * gets computed over time instead of a public field */
    Transaction getTransaction() {
        return transaction;
    }

    public byte[] computeContentHash(Block parent) throws NoSuchAlgorithmException {
        /* Compute a hash based on the transaction itself
         * and the parent block.
         *
         * Note that this hash does not include the computed hash */
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] parentHash = parent != null ? parent.hash : new byte[0];
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