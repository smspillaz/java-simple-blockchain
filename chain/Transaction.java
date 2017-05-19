import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Transaction {

    private byte[] rPubKey, sPubKey;
    public long amount;
    public byte[] signature, blockChainHash;
    public int nonce;
    public Transaction parent;
    public Blockchain blockchain;
    public byte[] message;


    public Transaction(Transaction parent, Blockchain blockchain, byte[] rPubKey, long amt, byte[] sig, long nonce) {
        this.rPubKey = rPubKey;
        this.amount = amt;
        this.signature = sig;
        this.blockchain = blockchain;
        this.nonce = nonce;
    }

    // 256BSenderPubKey|256ByteRecPubKey|4BAmount|64ByteSig|4BNonce|64BBlockchainHash

    public Transaction(byte[] b) { //  parse from byte array
        this.sPubKey = new byte[Globals.nBytesKeys];
        System.arraycopy(ba, Globals.nBytesSPubKeyOffset, this.sPubKey, 0, Globals.nBytesKeys);
        this.rPubKey = new byte[Globals.nBytesKeys];
        System.arraycopy(ba, Globals.nBytesRPubKeyOffset, this.rPubKey, 0, Globals.nBytesKeys);
        this.amount = Globals.readIntFromByteArray(ba, Globals.nBytesAmountOffset, Globals.nBytesAmount);
        this.signature = new byte[Globals.nBytesSig];
        System.arraycopy(ba, Globals.nBytesSigOffset, this.signature, 0, Globals.nBytesSig);
        this.nonce = Globals.readIntFromByteArray(ba, Globals.nBytesNonceOffset, Globals.nBytesNonce);
        this.blockChainHash = new byte[Globals.nBytesBlockChainHash];
        System.arraycopy(ba, Globals.nBytesBlockchainHashOffset, this.blockChainHash, 0, Globals.nBytesBlockchainHashOffset);
    }


    public void findNonce(byte[] message) throws Exception {
        for(int nonce = 0; nonce <= Globals.maxValNonce; nonce++){ // cycle through all 2 ^ 64 values until loops back to 0
            blockChainHash = blockchain.mkHash(message, 0, Globals.nBytesBlockchainHashOffset);
            if(blockChainHash[blockChainHash.length-1] == 0){
                this.nonce = nonce;
            }
        }
        throw new Exception("No nonce found!");
    }

    public void writeHashToMessage(byte[] message) throws Exception {  // TODO create custom exception for this
        try {
            findNonce(message);
        }
        catch(Exception e){
            throw new Exception("No nonce found!");
        }
        System.arraycopy(Globals.convertToByteArray(this.nonce, Globals.nBytesNonce), 0, message, Globals.nBytesNonceOffset, Globals.nBytesNonce);
        System.arraycopy(this.blockChainHash, 0, message, Globals.nBytesBlockchainHashOffset, Globals.nBytesBlockChainHash);
        return;
    }

    public boolean validateBlockChainHash(byte[] message){
        byte[] hash = blockchain.mkHash(message, 0, Globals.nBytesBlockchainHashOffset);
        return hash[hash.length - 1] == 0;
    }

    private void mkMessage(){
        // 64BBlockchainHash|256BSenderPubKey|256ByteRecPubKey|4BAmount|64ByteSig|4BNonce

        int len = Globals.nBytesBlockChainHash + Globals.nBytesKeys + Globals.nBytesKeys + Globals.nBytesAmount + Globals.nBytesSig + Globals.nBytesNonce;
        this.message = Globals.concatByteArrays(new byte[][]{ Globals.sPubKey, this.rPubKey, Globals.convertToByteArray(this.amount, Globals.nBytesAmount), this.signature, Globals.convertToByteArray(this.nonce, Globals.nBytesNonce), this.blockChainHash});
        try {
            writeHashToMessage(message);
        }
        catch(Exception e){
            Globals.log("Could not create nonce!"); // TODO abort from here
        }
    }

    public String toString() {
        return src + " -(" + amount +")> " + dst + " (sig: " + signature + ") " + nonce;
    }
}