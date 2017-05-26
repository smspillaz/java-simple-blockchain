import java.util.Random;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

public class ProofOfWork {
    public static void main(String[] args) throws NoSuchAlgorithmException,
                                                  Block.MiningException {
        int indexes = 580 / 4;
        Random r = new Random();
        ByteBuffer bb = ByteBuffer.allocate(580);
        for (int i = 0; i < indexes; ++i) {
            bb.putInt(i * 4, r.nextInt());
        }
        byte[] payload = bb.array();
        byte[] parent = new byte[0];

        int problemDifficulty = 0;
        while (true) {
            long startTime = System.currentTimeMillis();
            Block.mineNonce(payload, parent, problemDifficulty++);
            long finishTime = System.currentTimeMillis();

            System.out.println("" + problemDifficulty + " " + (finishTime - startTime));
        }
    }
}