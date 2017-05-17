import java.util.Collection;

import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class BlockchainTest {
  static final String expectedGenesisHash = "76B9F3F69B12FCD6D6731ACC8B53B98118D17D1FFDF726C939DCB06DD6D7F58E";

  @Test
  public void testBlockchainInitialConstruction() throws NoSuchAlgorithmException {
    Blockchain chain = new Blockchain();
    assertThat(DatatypeConverter.printHexBinary(chain.tipHash()),
               equalTo(BlockchainTest.expectedGenesisHash));
  }

  @Test
  public void testAppendNewTransaction() throws NoSuchAlgorithmException {
    Blockchain chain = new Blockchain();
    chain.appendTransaction(new Transaction(0, 1, 25, 0));

    final String execeptedTransactionHash = "8D8596DC7C682499ADAF59A2463DFCDD760F3AAD980BFD22FDD8742B3FCB393B";

    assertThat(DatatypeConverter.printHexBinary(chain.tipHash()),
               equalTo(execeptedTransactionHash));
  }

  @Test
  public void testSerialiseToJSON() throws NoSuchAlgorithmException {
    Blockchain chain = new Blockchain();
    chain.serialise();
  }

  @Test
  public void testDeserialiseFromJSON() throws NoSuchAlgorithmException,
                                               Blockchain.IntegrityCheckFailedException {
    Blockchain chain = new Blockchain();
    Blockchain deserialised = Blockchain.deserialise(chain.serialise());

    assertThat(chain.tipHash(), equalTo(deserialised.tipHash()));
  }

  @Test(expected=Blockchain.IntegrityCheckFailedException.class)
  public void testIntegrityCheckFailsWhenModifyingHashes() throws NoSuchAlgorithmException,
                                                                  Blockchain.IntegrityCheckFailedException,
                                                                  Blockchain.WalkFailedException {
    Blockchain chain = new Blockchain();
    chain.walk(new Blockchain.BlockEnumerator() {
        public void consume(int index, Block block) {
            /* Block here is mutable, so we can mess with its contents. Its
             * hash will stay as is and this should fail validation. In this
             * scenario a malicious chain makes another wallet the genesis
             * node */
            block.getTransaction().src = 1;
            block.getTransaction().dst = 1;
        }
    });

    /* This should throw an integrity check failure */
    Blockchain.deserialise(chain.serialise());
  }
}
