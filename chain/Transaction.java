import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Transaction {
    public int src;
    public int dst;

    public int amount;
    public int signature;

    public Transaction(int src,
                       int dst,
                       int amount,
                       int signature) {
        this.src = src;
        this.dst = dst;
        this.amount = amount;
        this.signature = signature;
    }

    public byte[] hash() throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = {
            (byte) src,
            (byte) dst,
            (byte) amount,
            (byte) signature
        };

        return digest.digest(bytes);
    }
}