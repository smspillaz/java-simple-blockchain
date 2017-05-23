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
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class LedgerTest extends TestBase {
  @Test
  public void testLedgerConstruction() throws NoSuchAlgorithmException,
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
    new Ledger(chain);
  }

  @Test(expected=Blockchain.WalkFailedException.class)
  public void testLedgerFailedValidation() throws NoSuchAlgorithmException,
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

    /* Try to blindly append an invalid transaction to the chain, i.e, receiever
     * key (1) spending money that it doesn't have */
    chain.appendPayload(convenienceTransactionPayloadFromIntegerKeys(receiverKeys.getPublic(),
                                                                     senderKeys.getPublic(),
                                                                     20,
                                                                     senderKeys.getPrivate()));

    /* Now we create a new ledger from this chain.
     * It should throw an exception, because the chain transactions are
     * not valid */
    new Ledger(chain);
  }

  @Test(expected=Blockchain.WalkFailedException.class)
  public void testLedgerFailedValidationNegativeTransaction() throws NoSuchAlgorithmException,
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

    /* Try to blindly append a negative transaction. This wouldn't be allowed */
    chain.appendPayload(convenienceTransactionPayloadFromIntegerKeys(senderKeys.getPublic(),
                                                                     receiverKeys.getPublic(),
                                                                     -20,
                                                                     senderKeys.getPrivate()));

    /* Now we create a new ledger from this chain.
     * It should throw an exception, because the chain transactions are
     * not valid */
    new Ledger(chain);
  }

  @Test
  public void testLedgerAddBadTransaction() throws NoSuchAlgorithmException,
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
    Ledger ledger = new Ledger(chain);

    /* Try to append an invalid transaction to the chain, i.e, public
     * key (1) spending money that it doesn't have. The ledger should
     * just silently reject it (it will return false so that we could
     * potentially log that the transaction failed, but it won't throw
     * an error) */
    assertThat(ledger.appendSignedTransaction(convenienceTransactionFromIntegerKeys(receiverKeys.getPublic(),
                                                                              senderKeys.getPublic(),
                                                                              20,
                                                                              receiverKeys.getPrivate())),
               equalTo(false));
  }

  @Test
  public void testLedgerAddBadTransactionNoModifyChain() throws NoSuchAlgorithmException,
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
    Ledger ledger = new Ledger(chain);

    /* Try to append an invalid transaction to the chain, i.e, public
     * key (1) spending money that it doesn't have. The ledger should
     * just silently reject it and the chain should not be modified, eg
     * the tip hash of both cases should be the same */
    byte[] tip = chain.tipHash();
    ledger.appendSignedTransaction(convenienceTransactionFromIntegerKeys(receiverKeys.getPublic(),
                                                                   senderKeys.getPublic(),
                                                                   20,
                                                                   receiverKeys.getPrivate()));
    assertThat(chain.tipHash(), equalTo(tip));
  }

  @Test
  public void testLedgerAddNegativeTransaction() throws NoSuchAlgorithmException,
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
    Ledger ledger = new Ledger(chain);

    /* Whatever money people have, reverse transactions are not allowed */
    assertThat(ledger.appendSignedTransaction(convenienceTransactionFromIntegerKeys(senderKeys.getPublic(),
                                                                                    receiverKeys.getPublic(),
                                                                                    -20,
                                                                                    senderKeys.getPrivate())),
               equalTo(false));
  }

  @Test
  public void testLedgerAddGoodTransaction() throws NoSuchAlgorithmException,
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
    Ledger ledger = new Ledger(chain);

    /* Append a valid transation to the chain. It should return true
     * since this transaction was all fine */
    assertThat(ledger.appendSignedTransaction(convenienceTransactionFromIntegerKeys(senderKeys.getPublic(),
                                                                              receiverKeys.getPublic(),
                                                                              20,
                                                                              senderKeys.getPrivate())),
               equalTo(true));
  }

  @Test
  public void testLedgerAddGoodTransactionModifyChain() throws NoSuchAlgorithmException,
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
    Ledger ledger = new Ledger(chain);

    /* Append a valid transation to the chain. The tip hash should be
     * changed on the chain */
    byte[] tip = chain.tipHash();
    ledger.appendSignedTransaction(convenienceTransactionFromIntegerKeys(senderKeys.getPublic(),
                                                                   receiverKeys.getPublic(),
                                                                   20,
                                                                   senderKeys.getPrivate()));
    assertThat(chain.tipHash(), not(tip));
  }

  @Test
  public void testLedgerAddGoodTransactionReverseModifyChain() throws NoSuchAlgorithmException,
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
    Ledger ledger = new Ledger(chain);

    /* Append a valid transation to the chain, then reverse it. The hashes
     * should not be restored to their prior position */
    byte[] tip = chain.tipHash();
    ledger.appendSignedTransaction(convenienceTransactionFromIntegerKeys(senderKeys.getPublic(),
                                                                   receiverKeys.getPublic(),
                                                                   20,
                                                                   senderKeys.getPrivate()));
    ledger.appendSignedTransaction(convenienceTransactionFromIntegerKeys(receiverKeys.getPublic(),
                                                                   senderKeys.getPublic(),
                                                                   20,
                                                                   receiverKeys.getPrivate()));
    assertThat(chain.tipHash(), not(tip));
  }
}
