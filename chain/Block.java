import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class Block {
    public byte[] payload;
    public int nonce;
    public byte[] hash;

    public Block(byte[] payload,
                 int nonce,
                 byte[] parentHash) throws NoSuchAlgorithmException {
        this.payload = new byte[payload.length];
        this.nonce = nonce;
        this.hash = this.computeContentHash(parentHash);

        System.arraycopy(payload, 0, this.payload, 0, payload.length);
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

        // cycle through all 2 ^ 630 values until loops back to 0
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

    // *BPayload|4BNonce|64BBlockchainHash
    public Block(byte[] contents) {
        /* We're assuming the payload length here based on what we know about
         * the hash and the nonce members */
        int payloadLength = contents.length - (Globals.nBytesBlockChainHash +
                                               Globals.nBytesNonce);
        this.payload = new byte[payloadLength];
        System.arraycopy(contents, 0, this.payload, 0, payloadLength);

        this.nonce = Globals.readIntFromByteArray(contents,
                                                  payloadLength,
                                                  Globals.nBytesNonce);
        this.hash = new byte[Globals.nBytesBlockChainHash];
        System.arraycopy(contents,
                         payloadLength + Globals.nBytesNonce,
                         this.hash,
                         0,
                         Globals.nBytesBlockChainHash);
    }

    public byte[] serialize() {
        return Globals.concatByteArrays(new byte[][] {
            this.payload,
            Globals.convertToByteArray(this.nonce, Globals.nBytesNonce),
            this.hash != null ? this.hash : new byte[0]
        });
    }

    public byte[] computeContentHash(byte[] parentHash) throws NoSuchAlgorithmException {
        return Blockchain.mkHash(Globals.concatByteArrays(new byte[][] {
                                     parentHash != null ? parentHash : new byte[0],
                                     this.serialize(),
                                 }),
                                 0,
                                 (parentHash != null ? parentHash.length : 0) +
                                 this.payload.length +
                                 Globals.nBytesNonce);
    }

    public String toString() {
        /* TODO: We'll need to dynamically convert this to an actual
         * transaction */
        return DatatypeConverter.printHexBinary(this.hash);
    }
}