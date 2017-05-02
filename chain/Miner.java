/**
 * Created by 19523162 on 2017-05-02.
 */

import javax.json.*;
import java.io.StringReader;

public class Miner {
    private byte[] senderPublicKey, receiverPublicKey, signature;
    private long amount;
    public boolean parseRequest(String json){
        javax.json.JsonObject jObj  = new JSONObject(new StringReader(json)); // json
        String senderPublicKeyB64 = jObj.getString('spk');
        String receiverPublicKeyB64 = jObj.getString('rpk');

    }
    public boolean walletRequest(byte[] senderPublicKey, byte[] receiverPublicKey, byte[] amount, byte[] signature);
}
