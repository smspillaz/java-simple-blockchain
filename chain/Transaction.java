import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class Transaction {
    public byte[] rPubKey, sPubKey;
    public long amount;
    public byte[] signature;

    public Transaction(byte[] sPubKey, byte[] rPubKey, long amt, byte[] sig) {
        this.sPubKey = new byte[sPubKey.length];
        this.rPubKey = new byte[rPubKey.length];

        System.arraycopy(sPubKey, 0, this.sPubKey, 0, sPubKey.length);
        System.arraycopy(rPubKey, 0, this.rPubKey, 0, rPubKey.length);

        this.amount = amt;
        this.signature = new byte[sig.length];

        System.arraycopy(sig, 0, this.signature, 0, sig.length);
    }

    public Transaction(byte[] byteArray) {
        // 256BSenderPubKey|256ByteRecPubKey|4BAmount|64ByteSig
        this.sPubKey = new byte[Globals.nBytesKeys];
        System.arraycopy(byteArray, Globals.nBytesSPubKeyOffset, this.sPubKey, 0, Globals.nBytesKeys);

        this.rPubKey = new byte[Globals.nBytesKeys];
        System.arraycopy(byteArray, Globals.nBytesRPubKeyOffset, this.rPubKey, 0, Globals.nBytesKeys);

        this.amount = Globals.readIntFromByteArray(byteArray, Globals.nBytesAmountOffset, Globals.nBytesAmount);
        this.signature = new byte[Globals.nBytesSig];

        System.arraycopy(byteArray, Globals.nBytesSigOffset, this.signature, 0, Globals.nBytesSig);
    }

    public byte[] serialize() {
        return Globals.concatByteArrays(new byte[][]{
            this.sPubKey,
            this.rPubKey,
            Globals.convertToByteArray(this.amount, Globals.nBytesAmount),
            this.signature
        });
    }

    public static interface Mutator {
        void mutate(Transaction transaction);
    }

    public static byte[] withMutations(byte[] contents, Mutator mutator) {
        Transaction transaction = new Transaction(contents);
        mutator.mutate(transaction);
        return transaction.serialize();
    }

    public String toString() {
        return DatatypeConverter.printHexBinary(sPubKey) +
               " -(" + amount +")> " +
               DatatypeConverter.printHexBinary(rPubKey) +
               " (sig: " +
               DatatypeConverter.printHexBinary(signature) + ") ";
    }
}