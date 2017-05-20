import java.security.MessageDigest;

/**
 * Created by 19523162 on 2017-05-18.
 */

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Globals {

// 256BSenderPubKey|256ByteRecPubKey|4BAmount|64ByteSig|4BNonce|64BBlockchainHash
    public static byte[] myPubKey;
    public static byte[] myPrivKey;
    public static int nBytesKeys = 256;



    public static int nBytesNonce = 4;
    public static int nBytesAmount = 4;
    public static int nBytesSig = 64;
    public static int nBytesBlockChainHash = 4;


    public static int nBytesSPubKeyOffset=0;
    public static int nBytesRPubKeyOffset=256;
    public static int nBytesAmountOffset=512;
    public static int nBytesSigOffset=516;
    public static int nBytesNonceOffset=580;
    public static int nBytesBlockchainHashOffset = 584;

    /* Need to subtract two here from the shift amount since long is signed.
     * Then subtract 1, so that we get a binary string of all 1s at 2^31 */
    public static long maxValNonce = (1 << ((nBytesNonce * 8) - 2)) - 1;

    public static String hashAlg = "SHA-256";

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
            n |= ba[i + st];
        }
        return n;
    }

    public static byte[] convertToByteArray(long l, int nBytes){
        byte[] ba = new byte[nBytes];
        for(int i = 0; i < nBytes; i++)
            ba[--nBytes] = (byte) (l & 0xFF);
        return ba;
    }

    public static void log(String msg){ // TODO implement this


    }



}
