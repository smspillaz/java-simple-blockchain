import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.Signature;

import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import java.security.spec.X509EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;

import javax.xml.bind.DatatypeConverter;

class SignedObject {
    public byte[] payload;
    public byte[] signature;

    /* Construct a signed object from some payload and a signing key. The
     * signature is generated and written to on the spot */
    public SignedObject(byte[] payload,
                        PrivateKey signingKey)  throws NoSuchAlgorithmException,
                                                       InvalidKeyException,
                                                       SignatureException {
        this.payload = new byte[payload.length];
        System.arraycopy(payload, 0, this.payload, 0, payload.length);
        this.signature = generateSignatureBytes(this.payload, signingKey);
    }

    /* Construct a signed object from some payload and a pre-existing
     * signature of that payload */
    public SignedObject(byte[] payload, byte[] signature)  throws NoSuchAlgorithmException {
        this.payload = new byte[payload.length];
        System.arraycopy(payload, 0, this.payload, 0, payload.length);
        this.signature = new byte[signature.length];
        System.arraycopy(signature, 0, this.signature, 0, signature.length);
    }

    /* Constructed a SignedObject from a serialised bytestream. Note that this
     * does not do any validation of the signature itself, it just reads it
     * from the underlying bitstream. You can use the convenience method
     * signatureIsValid in order to check it yourself */
    public SignedObject(byte[] blob) {
        this.payload = new byte[blob.length - Globals.nBytesSig];
        this.signature = new byte[Globals.nBytesSig];

        System.arraycopy(blob, 0, this.payload, 0, this.payload.length);
        System.arraycopy(blob, this.payload.length, this.signature, 0, Globals.nBytesSig);
    }

    private static byte[] generateSignatureBytes(byte[] data,
                                                 PrivateKey signingKey) throws NoSuchAlgorithmException,
                                                                               InvalidKeyException,
                                                                               SignatureException { 
        Signature signature = Signature.getInstance("SHA1WithRSA");
        signature.initSign(signingKey);
        signature.update(data);
        return signature.sign();
    }

    public static boolean signatureIsValid(byte[] data, byte[] signatureBytes, byte[] keyBytes) throws NoSuchAlgorithmException,
                                                                                                       InvalidKeyException,
                                                                                                       InvalidKeySpecException,
                                                                                                       SignatureException {
        Signature signature = Signature.getInstance("SHA1WithRSA");
        PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        signature.initVerify(key);
        signature.update(data);
        return signature.verify(signatureBytes);
    }

    public byte[] serialize() {
        return Globals.concatByteArrays(new byte[][]{
            this.payload,
            this.signature
        });
    }

    public static interface Mutator {
        public void mutate(SignedObject blob);
    }

    public static byte[] withMutations(byte[] objectAsBytes, Mutator mutator) {
        SignedObject blob = new SignedObject(objectAsBytes);
        mutator.mutate(blob);
        return blob.serialize();
    }
}