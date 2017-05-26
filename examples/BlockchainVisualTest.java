import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.InvalidKeyException;
import java.security.NoSuchProviderException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class BlockchainVisualTest {
    public static void main(String[] args) throws InvalidKeyException,
                                                  NoSuchProviderException,
                                                  NoSuchAlgorithmException,
                                                  SignatureException {
        BlockMiner miner = null;

        try {
            Security.addProvider(new BouncyCastleProvider());
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
            generator.initialize(2048);

            KeyPair genesisKeys = generator.generateKeyPair();

            Blockchain chain = new Blockchain(1);
            miner = new BlockMiner(chain, 1);
            miner.waitFor(
                miner.appendPayload(new SignedObject(
                    new Transaction(genesisKeys.getPublic().getEncoded(),
                                    genesisKeys.getPublic().getEncoded(),
                                    50).serialize(),
                    genesisKeys.getPrivate()
                ).serialize())
            );
            System.out.println(chain.serialise());

        } finally {
            if (miner != null) {
                miner.shutdown();
            }
        }
    }
}