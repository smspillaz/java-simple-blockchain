import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.nio.ByteBuffer;

import javax.xml.bind.DatatypeConverter;

public class Transaction {
    public byte[] rPubKey, sPubKey;
    public int amount;

    public Transaction(byte[] sPubKey,
                       byte[] rPubKey,
                       int amt) {
        this.sPubKey = new byte[sPubKey.length];
        this.rPubKey = new byte[rPubKey.length];

        System.arraycopy(sPubKey, 0, this.sPubKey, 0, sPubKey.length);
        System.arraycopy(rPubKey, 0, this.rPubKey, 0, rPubKey.length);

        this.amount = amt;
    }

    public Transaction(byte[] byteArray) {
        // 256BSenderPubKey|256ByteRecPubKey|4BAmount|64ByteSig
        this.sPubKey = new byte[Globals.nBytesKeys];
        System.arraycopy(byteArray, 0, this.sPubKey, 0, Globals.nBytesKeys);

        this.rPubKey = new byte[Globals.nBytesKeys];
        System.arraycopy(byteArray, Globals.nBytesKeys, this.rPubKey, 0, Globals.nBytesKeys);

        this.amount = ByteBuffer.wrap(byteArray,
                                      Globals.nBytesKeys * 2,
                                      Globals.nBytesAmount).getInt();
    }

    public byte[] serialize() {
        return Globals.concatByteArrays(new byte[][]{
            this.sPubKey,
            this.rPubKey,
            ByteBuffer.allocate(Globals.nBytesAmount).putInt(this.amount).array(),
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
               DatatypeConverter.printHexBinary(rPubKey);
    }
}