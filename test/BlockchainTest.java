import java.util.ArrayList;
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

public class BlockchainTest extends TestBase {
  static final String expectedGenesisHash = "8A47E6B9A38BA3E928883E20CA9CCB5B22887C062760A21072E51F55B67FEE00";

  @Test
  public void testBlockchainInitialConstruction() throws NoSuchAlgorithmException,
                                                         Block.MiningException {
    Blockchain chain = new Blockchain();
    assertThat(DatatypeConverter.printHexBinary(chain.tipHash()),
               equalTo(BlockchainTest.expectedGenesisHash));
  }

  @Test
  public void testAppendNewTransaction() throws NoSuchAlgorithmException,
                                                Block.MiningException {
    Blockchain chain = new Blockchain();
    chain.appendPayload(convenienceTransactionPayloadFromIntegerKeys(0, 1, 25, 0));

    final String execeptedTransactionHash = "F040F3C16453938B3725539808AA818B12FCABC0C7F43A35F59A2EF750FE4C00";

    assertThat(DatatypeConverter.printHexBinary(chain.tipHash()),
               equalTo(execeptedTransactionHash));
  }

  @Test
  public void testSerialiseToJSON() throws NoSuchAlgorithmException,
                                           Block.MiningException {
    Blockchain chain = new Blockchain();
    chain.serialise();
  }

  @Test
  public void testDeserialiseFromJSON() throws NoSuchAlgorithmException,
                                               Blockchain.IntegrityCheckFailedException,
                                               Block.MiningException {
    Blockchain chain = new Blockchain();
    Blockchain deserialised = Blockchain.deserialise(chain.serialise());

    assertThat(chain.tipHash(), equalTo(deserialised.tipHash()));
  }

  @Test
  public void testDeserialiseManyBlocksFromJSON() throws NoSuchAlgorithmException,
                                                         Blockchain.IntegrityCheckFailedException,
                                                         Blockchain.WalkFailedException,
                                                         Block.MiningException {
    Blockchain chain = new Blockchain();
    Ledger ledger = new Ledger(chain, new ArrayList<Ledger.TransactionObserver>());

    ledger.appendTransaction(convenienceTransactionFromIntegerKeys(0, 1, 20, 0));
    ledger.appendTransaction(convenienceTransactionFromIntegerKeys(0, 1, 10, 0));
    ledger.appendTransaction(convenienceTransactionFromIntegerKeys(0, 1, 10, 0));

    Blockchain deserialised = Blockchain.deserialise(chain.serialise());

    assertThat(chain.tipHash(), equalTo(deserialised.tipHash()));
  }

  @Test(expected=Blockchain.IntegrityCheckFailedException.class)
  public void testIntegrityCheckFailsWhenModifyingHashes() throws NoSuchAlgorithmException,
                                                                  Blockchain.IntegrityCheckFailedException,
                                                                  Blockchain.WalkFailedException,
                                                                  Block.MiningException {
    Blockchain chain = new Blockchain();
    chain.walk(new Blockchain.BlockEnumerator() {
        public void consume(int index, Block block) {
            /* Block here is mutable, so we can mess with its contents. Its
             * hash will stay as is and this should fail validation. In this
             * scenario a malicious chain makes another wallet the genesis
             * node */
            block.payload = Transaction.withMutations(block.payload, new Transaction.Mutator() {
                public void mutate(Transaction transaction) {
                    transaction.sPubKey = convenienceLongToPubKey(1L);
                    transaction.rPubKey = transaction.sPubKey;
                }
            });
        }
    });

    /* This should throw an integrity check failure */
    Blockchain.deserialise(chain.serialise());
  }

  @Test(expected=Blockchain.IntegrityCheckFailedException.class)
  public void testIntegrityCheckFailedWhenBlockNotMined() throws NoSuchAlgorithmException,
                                                                 Blockchain.IntegrityCheckFailedException,
                                                                 Blockchain.WalkFailedException,
                                                                 Block.MiningException {
    Blockchain chain = new Blockchain();
    chain.walk(new Blockchain.BlockEnumerator() {
        public void consume(int index, Block block) {
            /* Change the nonce to something that doesn't prove that we did
             * the work required to mine this block and then re-hash the block */
            block.nonce = 0;
            try {
              block.hash = block.computeContentHash(chain.parentBlockHash(index));
            } catch (NoSuchAlgorithmException e) {
              System.err.println(e.getMessage());
            }
        }
    });

    /* This should throw an integrity check failure */
    Blockchain.deserialise(chain.serialise());
  }

  @Test(expected=Blockchain.IntegrityCheckFailedException.class)
  public void testIntegrityCheckFailsWhenModifyingCenterHash() throws NoSuchAlgorithmException,
                                                                      Blockchain.IntegrityCheckFailedException,
                                                                      Blockchain.WalkFailedException,
                                                                      Block.MiningException {
    Blockchain chain = new Blockchain();
    Ledger ledger = new Ledger(chain, new ArrayList<Ledger.TransactionObserver>());

    ledger.appendTransaction(convenienceTransactionFromIntegerKeys(0, 1, 20, 0));
    ledger.appendTransaction(convenienceTransactionFromIntegerKeys(0, 1, 10, 0));
    ledger.appendTransaction(convenienceTransactionFromIntegerKeys(0, 1, 10, 0));

    chain.walk(new Blockchain.BlockEnumerator() {
        public void consume(int index, Block block) {
            /* Be a little bit evil and only modify the second transaction */
            if (index == 1) {
                block.payload = Transaction.withMutations(block.payload, new Transaction.Mutator() {
                    public void mutate(Transaction transaction) {
                        transaction.sPubKey = convenienceLongToPubKey(1L);
                        transaction.rPubKey = convenienceLongToPubKey(0L);
                    }
                });
            }
        }
    });

    /* This should throw an integrity check failure */
    Blockchain.deserialise(chain.serialise());
  }
}
