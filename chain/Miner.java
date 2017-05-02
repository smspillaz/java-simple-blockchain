/**
 * Created by 19523162 on 2017-05-02.
 */

import javax.json.*;

public class Miner {
    private byte[] senderPublicKey, receiverPublicKey, signature;
    private long amount;
    public boolean parseRequest(String json){
        JSONObject obj = new JSONObject(json);
        String pageName = .getJSONObject("pageInfo").getString("pageName");

    }
    public boolean walletRequest(byte[] senderPublicKey, byte[] receiverPublicKey, byte[] amount, byte[] signature);
}
