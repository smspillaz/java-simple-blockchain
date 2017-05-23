import java.util.ArrayList;
import java.util.Collection;

import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import javax.xml.bind.DatatypeConverter;

import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class BlockchainTest extends TestBase {
  @Test
  public void testSerialiseToJSON() throws NoSuchAlgorithmException,
                                           Block.MiningException,
                                           InvalidKeyException,
                                           SignatureException {
    Blockchain chain = new Blockchain(
      convenienceTransactionPayloadFromIntegerKeys(senderKeys.getPublic(),
                                                   senderKeys.getPublic(),
                                                   50,
                                                   senderKeys.getPrivate())
    );
    chain.serialise();
  }

  @Test
  public void testDeserialiseFromJSON() throws NoSuchAlgorithmException,
                                               Blockchain.IntegrityCheckFailedException,
                                               Block.MiningException,
                                               InvalidKeyException,
                                               SignatureException {
    Blockchain chain = new Blockchain(
      convenienceTransactionPayloadFromIntegerKeys(senderKeys.getPublic(),
                                                   senderKeys.getPublic(),
                                                   50,
                                                   senderKeys.getPrivate())
    );
    Blockchain deserialised = Blockchain.deserialise(chain.serialise());

    assertThat(chain.tipHash(), equalTo(deserialised.tipHash()));
  }

  @Test
  public void testDeserialiseManyBlocksFromJSON() throws NoSuchAlgorithmException,
                                                         Blockchain.IntegrityCheckFailedException,
                                                         Blockchain.WalkFailedException,
                                                         Block.MiningException,
                                                         InvalidKeyException,
                                                         SignatureException {
    Blockchain chain = new Blockchain(
      convenienceTransactionPayloadFromIntegerKeys(senderKeys.getPublic(),
                                                   senderKeys.getPublic(),
                                                   50,
                                                   senderKeys.getPrivate())
    );
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
                                                                  Block.MiningException,
                                                                  InvalidKeyException,
                                                                  SignatureException {
    Blockchain chain = new Blockchain(
      convenienceTransactionPayloadFromIntegerKeys(senderKeys.getPublic(),
                                                   senderKeys.getPublic(),
                                                   50,
                                                   senderKeys.getPrivate())
    );
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
                                                                 Block.MiningException,
                                                                 InvalidKeyException,
                                                                 SignatureException {
    Blockchain chain = new Blockchain(
      convenienceTransactionPayloadFromIntegerKeys(senderKeys.getPublic(),
                                                   senderKeys.getPublic(),
                                                   50,
                                                   senderKeys.getPrivate())
    );
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
                                                                      Block.MiningException,
                                                                      InvalidKeyException,
                                                                      SignatureException {
    Blockchain chain = new Blockchain(
      convenienceTransactionPayloadFromIntegerKeys(senderKeys.getPublic(),
                                                   senderKeys.getPublic(),
                                                   50,
                                                   senderKeys.getPrivate())
    );
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
