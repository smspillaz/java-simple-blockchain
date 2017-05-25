import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class Models {
    /* This class is essentially a more convenient way of representing a
     * transaction in a JSON request. The user of this class should probably
     * build up a real Transaction and Signed object first and then use the
     * fields from that to populate this model */
    public static class Transaction {
        public String src;
        public String dst;
        public int amount;
        public String signature;

        public Transaction(String src,
                           String dst,
                           int amount,
                           String signature) {
            this.src = src;
            this.dst = dst;
            this.amount = amount;
            this.signature = signature;
        }

        public static Transaction deserialise(String json) {
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            return gson.fromJson(json, Transaction.class);
        }

        public String serialise() {
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            return gson.toJson(this).toString();
        }
    }
}