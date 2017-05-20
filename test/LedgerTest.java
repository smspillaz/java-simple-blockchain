import java.util.Collection;

import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class LedgerTest extends TestBase {
  static final String expectedGenesisHash = "76B9F3F69B12FCD6D6731ACC8B53B98118D17D1FFDF726C939DCB06DD6D7F58E";

  @Test
  public void testLedgerConstruction() throws NoSuchAlgorithmException,
                                              Blockchain.WalkFailedException,
                                              Block.MiningException {
    Blockchain chain = new Blockchain();
    new Ledger(chain);
  }

  @Test(expected=Blockchain.WalkFailedException.class)
  public void testLedgerFailedValidation() throws NoSuchAlgorithmException,
                                                  Blockchain.WalkFailedException,
                                                  Block.MiningException {
    Blockchain chain = new Blockchain();

    /* Try to blindly append an invalid transaction to the chain, i.e, public
     * key (1) spending money that it doesn't have */
    chain.appendPayload(convenienceTransactionPayloadFromIntegerKeys(1, 0, 20, 0));

    /* Now we create a new ledger from this chain.
     * It should throw an exception, because the chain transactions are
     * not valid */
    new Ledger(chain);
  }

  @Test(expected=Blockchain.WalkFailedException.class)
  public void testLedgerFailedValidationNegativeTransaction() throws NoSuchAlgorithmException,
                                                                     Blockchain.WalkFailedException,
                                                                     Block.MiningException {
    Blockchain chain = new Blockchain();

    /* Try to blindly append a negative transaction. This wouldn't be allowed */
    chain.appendPayload(convenienceTransactionPayloadFromIntegerKeys(0, 1, -20, 0));

    /* Now we create a new ledger from this chain.
     * It should throw an exception, because the chain transactions are
     * not valid */
    new Ledger(chain);
  }

  @Test
  public void testLedgerAddBadTransaction() throws NoSuchAlgorithmException,
                                                   Blockchain.WalkFailedException,
                                                   Block.MiningException {
    Blockchain chain = new Blockchain();
    Ledger ledger = new Ledger(chain);

    /* Try to append an invalid transaction to the chain, i.e, public
     * key (1) spending money that it doesn't have. The ledger should
     * just silently reject it (it will return false so that we could
     * potentially log that the transaction failed, but it won't throw
     * an error) */
    assertThat(ledger.appendTransaction(convenienceTransactionFromIntegerKeys(1, 0, 20, 0)),
               equalTo(false));
  }

  @Test
  public void testLedgerAddBadTransactionNoModifyChain() throws NoSuchAlgorithmException,
                                                         Blockchain.WalkFailedException,
                                                         Block.MiningException {
    Blockchain chain = new Blockchain();
    Ledger ledger = new Ledger(chain);

    /* Try to append an invalid transaction to the chain, i.e, public
     * key (1) spending money that it doesn't have. The ledger should
     * just silently reject it and the chain should not be modified, eg
     * the tip hash of both cases should be the same */
    byte[] tip = chain.tipHash();
    ledger.appendTransaction(convenienceTransactionFromIntegerKeys(1, 0, 20, 0));
    assertThat(chain.tipHash(), equalTo(tip));
  }

  @Test
  public void testLedgerAddNegativeTransaction() throws NoSuchAlgorithmException,
                                                        Blockchain.WalkFailedException,
                                                        Block.MiningException {
    Blockchain chain = new Blockchain();
    Ledger ledger = new Ledger(chain);

    /* Whatever money people have, reverse transactions are not allowed */
    assertThat(ledger.appendTransaction(convenienceTransactionFromIntegerKeys(0, 1, -20, 0)),
               equalTo(false));
  }

  @Test
  public void testLedgerAddGoodTransaction() throws NoSuchAlgorithmException,
                                                    Blockchain.WalkFailedException,
                                                    Block.MiningException {
    Blockchain chain = new Blockchain();
    Ledger ledger = new Ledger(chain);

    /* Append a valid transation to the chain. It should return true
     * since this transaction was all fine */
    assertThat(ledger.appendTransaction(convenienceTransactionFromIntegerKeys(0, 1, 20, 0)),
               equalTo(true));
  }

  @Test
  public void testLedgerAddGoodTransactionModifyChain() throws NoSuchAlgorithmException,
                                                               Blockchain.WalkFailedException,
                                                               Block.MiningException {
    Blockchain chain = new Blockchain();
    Ledger ledger = new Ledger(chain);

    /* Append a valid transation to the chain. The tip hash should be
     * changed on the chain */
    byte[] tip = chain.tipHash();
    ledger.appendTransaction(convenienceTransactionFromIntegerKeys(0, 1, 20, 0));
    assertThat(chain.tipHash(), not(tip));
  }

  @Test
  public void testLedgerAddGoodTransactionReverseModifyChain() throws NoSuchAlgorithmException,
                                                                      Blockchain.WalkFailedException,
                                                                      Block.MiningException {
    Blockchain chain = new Blockchain();
    Ledger ledger = new Ledger(chain);

    /* Append a valid transation to the chain, then reverse it. The hashes
     * should not be restored to their prior position */
    byte[] tip = chain.tipHash();
    ledger.appendTransaction(convenienceTransactionFromIntegerKeys(0, 1, 20, 0));
    ledger.appendTransaction(convenienceTransactionFromIntegerKeys(1, 0, 20, 0));
    assertThat(chain.tipHash(), not(tip));
  }
}
