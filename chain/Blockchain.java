import java.util.List;
import java.util.ArrayList;
import java.security.NoSuchAlgorithmException;

public class Blockchain {
    private List<Block> chain;
    public Blockchain() throws NoSuchAlgorithmException {
        chain = new ArrayList<Block>();
        /* On the construction of the blockchain, create a genesis node.
         * Note that right now, we are not signing transactions */
        chain.add(new Block(new Transaction(0, 0, 50, 0),
                            null));
    }

    public void appendTransaction(Transaction transaction) throws NoSuchAlgorithmException {
        chain.add(new Block(transaction, null));
    }

    public byte[] tipHash() {
        Block lastBlock = chain.get(chain.size() - 1);
        return lastBlock.hash;
    }
}