import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.Security;

import java.security.SignatureException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.Before;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


public class TestBase {
  protected KeyPair senderKeys;
  protected KeyPair receiverKeys;

  protected long problemDifficulty = 2;

  @Before
  public void setUp() throws SignatureException,
                             NoSuchAlgorithmException,
                             NoSuchProviderException {
    /* Generate sender and receiver key pairs so that we can easily create
     * signed objects and transactions */
    Security.addProvider(new BouncyCastleProvider());
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
    generator.initialize(2048);

    senderKeys = generator.generateKeyPair();
    receiverKeys = generator.generateKeyPair();
  }

  static SignedObject convenienceTransactionFromIntegerKeys(PublicKey sPubKey,
                                                            PublicKey rPubKey,
                                                            long amount,
                                                            PrivateKey signingKey) throws NoSuchAlgorithmException,
                                                                                          InvalidKeyException,
                                                                                          SignatureException {
    Transaction transaction = new Transaction(sPubKey.getEncoded(),
                                              rPubKey.getEncoded(),
                                              amount);
    return new SignedObject(transaction.serialize(), signingKey);
  }

  static byte[] convenienceTransactionPayloadFromIntegerKeys(PublicKey sPubKey,
                                                             PublicKey rPubKey,
                                                             long amount,
                                                             PrivateKey signingKey) throws NoSuchAlgorithmException,
                                                                                           InvalidKeyException,
                                                                                           SignatureException {
    return convenienceTransactionFromIntegerKeys(sPubKey,
                                                 rPubKey,
                                                 amount,
                                                 signingKey).serialize();
  }
}