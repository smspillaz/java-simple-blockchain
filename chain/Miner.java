/**
 * Created by 19523162 on 2017-05-02.
 */
// test @14/05/17 1745
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.Json;
import java.io.StringReader;

public class Miner {
    private byte[] senderPublicKey, receiverPublicKey, signature;
    private long amount;

    public boolean parseRequest(String json){
        JsonReader reader = Json.createReader(new StringReader(json));
        JsonObject jObj = null;
        try {
            jObj = reader.readObject();
        } finally {
            reader.close();
        }

        //String senderPublicKeyB64 = jObj.getString("spk");
        //String receiverPublicKeyB64 = jObj.getString("rpk");

        return jObj != null;
    }

    public boolean walletRequest(byte[] senderPublicKey, byte[] receiverPublicKey, byte[] amount, byte[] signature) {
        return false;
    }
}
