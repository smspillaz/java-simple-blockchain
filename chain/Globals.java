import java.security.MessageDigest;

/**
 * Created by 19523162 on 2017-05-18.
 */

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Globals {

// 256BSenderPubKey|256ByteRecPubKey|4BAmount|64ByteSig|4BNonce|64BBlockchainHash

    public static final int nBytesKeys = 256;

    public static final int nBytesNonce = 4;
    public static final int nBytesAmount = 4;
    public static final int nBytesSig = 64;
    public static final int nBytesBlockChainHash = 4;


    public static final int nBytesSPubKeyOffset=0;
    public static final int nBytesRPubKeyOffset=256;
    public static final int nBytesAmountOffset=512;
    public static final int nBytesSigOffset=516;
    public static final int nBytesNonceOffset=580;
    public static final int nBytesBlockchainHashOffset = 584;

    /* Need to subtract two here from the shift amount since long is signed.
     * Then subtract 1, so that we get a binary string of all 1s at 2^31 */
    public static final long maxValNonce = 1 << (nBytesNonce * 8 - 2) - 1;

    public static final String hashAlg = "SHA-256";

    public static byte[] concatByteArrays(byte[][] arrays){
        int len = 0, currIndex = 0;
        for(int i = 0; i < arrays.length; i++)
            len += arrays[i].length;
        byte[] a = new byte[len];
        for(int i = 0; i < arrays.length; i++)
            for(int j = 0; j < arrays[i].length; j++)
                a[currIndex++] = arrays[i][j];
        return a;
    }


    // convert bytes from byte array into primitive <E>
    public static int readIntFromByteArray(byte[] ba, int st, int len){
        int n = 0;
        for(int i = 0; i < len; ++i){
            n = n << 8;
            n |= ba[i + st] & 0xff;
        }
        return n;
    }

    public static byte[] convertToByteArray(long l, int nBytes){
        byte[] ba = new byte[nBytes];
        /* We're using the most significant byte first here */
        for(int i = 0; i < nBytes; i++)
            ba[--nBytes] = (byte) ((l >> (i * 8)) & 0xFF);
        return ba;
    }
}
