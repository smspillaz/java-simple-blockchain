import javax.xml.bind.DatatypeConverter;
import java.security.NoSuchAlgorithmException;

public class BlockchainVisualTest {
    public static void main(String[] args) {
        try {
            Blockchain chain = new Blockchain();

            System.out.println(DatatypeConverter.printHexBinary(chain.tipHash()));
        } catch (NoSuchAlgorithmException exception) {
            System.err.println("Java installation does not support SHA-256, cannot continue");
            System.exit(1);
        }
    }
}