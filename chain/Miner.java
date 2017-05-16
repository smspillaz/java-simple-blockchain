/**
 * Created by 19523162 on 2017-05-02.
 */
// test @14/05/17 1745
import javax.json.JsonObject;
import java.io.StringReader;

public class Miner {
    private byte[] senderPublicKey, receiverPublicKey, signature;
    private long amount;

    public boolean parseRequest(String json){
        JsonObject jObj = new JsonObject(new StringReader(json)); // json
        String senderPublicKeyB64 = jObj.getString("spk");
        String receiverPublicKeyB64 = jObj.getString("rpk");
        return false;
    }

    public boolean walletRequest(byte[] senderPublicKey, byte[] receiverPublicKey, byte[] amount, byte[] signature) {
        return false;
    }
}
