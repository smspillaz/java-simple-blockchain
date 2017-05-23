import javax.xml.bind.DatatypeConverter;
import java.security.NoSuchAlgorithmException;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class BlockchainVisualTest {
    public static void main(String[] args) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
            generator.initialize(2048);

            KeyPair genesisKeys = generator.generateKeyPair();

            Blockchain chain = new Blockchain(
                new Transaction(genesisKeys.getPublic(),
                                genesisKeys.getPublic(),
                                50,
                                genesisKeys.getPrivate()).serialize()
            );

            System.out.println(DatatypeConverter.printHexBinary(chain.tipHash()));
        } catch (NoSuchAlgorithmException exception) {
            System.err.println("Java installation does not support SHA-256, cannot continue");
            System.exit(1);
        }
    }
}