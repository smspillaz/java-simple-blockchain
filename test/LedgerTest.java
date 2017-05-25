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
    Blockchain chain = new Blockchain(problemDifficulty);
    BlockMiner miner = registerForCleanup(new BlockMiner(chain, problemDifficulty));
    miner.waitFor(
      miner.appendPayload(convenienceTransactionPayloadFromIntegerKeys(senderKeys.getPublic(),
                                                                       senderKeys.getPublic(),
                                                                       50,
                                                                       senderKeys.getPrivate()))
    );
    new Ledger(chain);
  }

  @Test(expected=Blockchain.WalkFailedException.class)
  public void testLedgerFailedValidation() throws NoSuchAlgorithmException,
                                                  Blockchain.WalkFailedException,
                                                  Block.MiningException,
                                                  InvalidKeyException,
                                                  SignatureException {
    Blockchain chain = new Blockchain(problemDifficulty);
    BlockMiner miner = registerForCleanup(new BlockMiner(chain, problemDifficulty));
    miner.appendPayload(convenienceTransactionPayloadFromIntegerKeys(senderKeys.getPublic(),
                                                                     senderKeys.getPublic(),
                                                                     50,
                                                                     senderKeys.getPrivate()));

    /* Try to blindly append an invalid transaction to the chain, i.e, receiever
     * key (1) spending money that it doesn't have */
    miner.waitFor(
      miner.appendPayload(convenienceTransactionPayloadFromIntegerKeys(receiverKeys.getPublic(),
                                                                       senderKeys.getPublic(),
                                                                       20,
                                                                       senderKeys.getPrivate()))
    );

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
    Blockchain chain = new Blockchain(problemDifficulty);
    BlockMiner miner = registerForCleanup(new BlockMiner(chain, problemDifficulty));
    miner.appendPayload(convenienceTransactionPayloadFromIntegerKeys(senderKeys.getPublic(),
                                                                     senderKeys.getPublic(),
                                                                     50,
                                                                     senderKeys.getPrivate()));

    /* Try to blindly append a negative transaction. This wouldn't be allowed */
    miner.waitFor(
      miner.appendPayload(convenienceTransactionPayloadFromIntegerKeys(senderKeys.getPublic(),
                                                                       receiverKeys.getPublic(),
                                                                       -20,
                                                                       senderKeys.getPrivate()))
    );

    /* Now we create a new ledger from this chain.
     * It should throw an exception, because the chain transactions are
     * not valid */
    new Ledger(chain);
  }

  @Test(expected=Ledger.BlobSignatureValidationFailedException.class)
  public void testLedgerFailedValidationIncorrectlySigned() throws NoSuchAlgorithmException,
                                                                   Blockchain.WalkFailedException,
                                                                   Block.MiningException,
                                                                   InvalidKeyException,
                                                                   SignatureException {
    Blockchain chain = new Blockchain(problemDifficulty);
    BlockMiner miner = registerForCleanup(new BlockMiner(chain, problemDifficulty));
    miner.appendPayload(convenienceTransactionPayloadFromIntegerKeys(senderKeys.getPublic(),
                                                   senderKeys.getPublic(),
                                                   50,
                                                   senderKeys.getPrivate()));

    /* Append a transaction which was signed by the wrong private key. Signature
     * validation should fail */
    miner.waitFor(
      miner.appendPayload(convenienceTransactionPayloadFromIntegerKeys(senderKeys.getPublic(),
                                                                       receiverKeys.getPublic(),
                                                                       20,
                                                                       receiverKeys.getPrivate()))
    );

    /* Now we create a new ledger from this chain.
     * It should throw an exception, because the chain transactions are
     * not valid */
    new Ledger(chain);
  }

  @Test(expected=Ledger.BlobSignatureValidationFailedException.class)
  public void testLedgerFailedValidationTamperedWithData() throws NoSuchAlgorithmException,
                                                                  Blockchain.WalkFailedException,
                                                                  Block.MiningException,
                                                                  InvalidKeyException,
                                                                  SignatureException {
    Blockchain chain = new Blockchain(problemDifficulty);
    BlockMiner miner = registerForCleanup(new BlockMiner(chain, problemDifficulty));
    miner.appendPayload(convenienceTransactionPayloadFromIntegerKeys(senderKeys.getPublic(),
                                                                     senderKeys.getPublic(),
                                                                     50,
                                                                     senderKeys.getPrivate()));

    /* Append a transaction which was signed by the wrong private key. Signature
     * validation should fail */
    SignedObject blob = convenienceTransactionFromIntegerKeys(senderKeys.getPublic(),
                                                              receiverKeys.getPublic(),
                                                              20,
                                                              senderKeys.getPrivate());
    blob.payload = Transaction.withMutations(blob.payload, new Transaction.Mutator() {
      public void mutate(Transaction transaction) {
        transaction.amount -= 19;
      }
    });
    miner.waitFor(
      miner.appendPayload(convenienceTransactionPayloadFromIntegerKeys(senderKeys.getPublic(),
                                                                       receiverKeys.getPublic(),
                                                                       20,
                                                                       receiverKeys.getPrivate()))
    );

    /* Now we create a new ledger from this chain.
     * It should throw an exception, because the chain transactions are
     * not valid */
    new Ledger(chain);
  }

  @Test
  public void testLedgerAddBadTransactionNoModifyChain() throws NoSuchAlgorithmException,
                                                         Blockchain.WalkFailedException,
                                                         Block.MiningException,
                                                         InvalidKeyException,
                                                         SignatureException {
    Blockchain chain = new Blockchain(problemDifficulty);
    BlockMiner miner = registerForCleanup(new BlockMiner(chain, problemDifficulty));
    AsynchronouslyMutableLedger ledger = new AsynchronouslyMutableLedger(chain, miner);
    miner.waitFor(
      ledger.appendSignedTransaction(convenienceTransactionFromIntegerKeys(senderKeys.getPublic(),
                                                                           senderKeys.getPublic(),
                                                                           50,
                                                                           senderKeys.getPrivate()))
    );

    /* Try to append an invalid transaction to the chain, i.e, public
     * key (1) spending money that it doesn't have. The ledger should
     * just silently reject it and the chain should not be modified, eg
     * the tip hash of both cases should be the same */
    byte[] tip = chain.tipHash();
    miner.waitFor(
      ledger.appendSignedTransaction(convenienceTransactionFromIntegerKeys(receiverKeys.getPublic(),
                                                                           senderKeys.getPublic(),
                                                                           20,
                                                                           receiverKeys.getPrivate()))
    );
    assertThat(chain.tipHash(), equalTo(tip));
  }

  @Test
  public void testLedgerAddNegativeTransaction() throws NoSuchAlgorithmException,
                                                        Blockchain.WalkFailedException,
                                                        Block.MiningException,
                                                        InvalidKeyException,
                                                        SignatureException {
    Blockchain chain = new Blockchain(problemDifficulty);
    BlockMiner miner = registerForCleanup(new BlockMiner(chain, problemDifficulty));
    AsynchronouslyMutableLedger ledger = new AsynchronouslyMutableLedger(chain, miner);

    miner.waitFor(
      ledger.appendSignedTransaction(convenienceTransactionFromIntegerKeys(senderKeys.getPublic(),
                                                                           senderKeys.getPublic(),
                                                                           50,
                                                                           senderKeys.getPrivate()))
    );

    /* Whatever money people have, reverse transactions are not allowed */
    byte[] tip = chain.tipHash();
    miner.waitFor(
      ledger.appendSignedTransaction(convenienceTransactionFromIntegerKeys(senderKeys.getPublic(),
                                                                           receiverKeys.getPublic(),
                                                                           -20,
                                                                           senderKeys.getPrivate()))
    );
    assertThat(chain.tipHash(), equalTo(tip));
  }

  @Test
  public void testLedgerAddGoodTransactionModifyChain() throws NoSuchAlgorithmException,
                                                               Blockchain.WalkFailedException,
                                                               Block.MiningException,
                                                               InvalidKeyException,
                                                               SignatureException {
    Blockchain chain = new Blockchain(problemDifficulty);
    BlockMiner miner = registerForCleanup(new BlockMiner(chain, problemDifficulty));
    AsynchronouslyMutableLedger ledger = new AsynchronouslyMutableLedger(chain, miner);
    miner.waitFor(
      ledger.appendSignedTransaction(convenienceTransactionFromIntegerKeys(senderKeys.getPublic(),
                                                                           senderKeys.getPublic(),
                                                                           50,
                                                                           senderKeys.getPrivate()))
    );

    /* Append a valid transation to the chain. The tip hash should be
     * changed on the chain */
    byte[] tip = chain.tipHash();
    miner.waitFor(
      ledger.appendSignedTransaction(convenienceTransactionFromIntegerKeys(senderKeys.getPublic(),
                                                                           receiverKeys.getPublic(),
                                                                           20,
                                                                           senderKeys.getPrivate()))
    );
    assertThat(chain.tipHash(), not(tip));
  }

  @Test
  public void testLedgerAddGoodTransactionReverseModifyChain() throws NoSuchAlgorithmException,
                                                                      Blockchain.WalkFailedException,
                                                                      Block.MiningException,
                                                                      InvalidKeyException,
                                                                      SignatureException {
    Blockchain chain = new Blockchain(problemDifficulty);
    BlockMiner miner = registerForCleanup(new BlockMiner(chain, problemDifficulty));
    AsynchronouslyMutableLedger ledger = new AsynchronouslyMutableLedger(chain, miner);
    miner.waitFor(
      ledger.appendSignedTransaction(convenienceTransactionFromIntegerKeys(senderKeys.getPublic(),
                                                                           senderKeys.getPublic(),
                                                                           50,
                                                                           senderKeys.getPrivate()))
    );

    /* Append a valid transation to the chain, then reverse it. The hashes
     * should not be restored to their prior position */
    byte[] tip = chain.tipHash();
    ledger.appendSignedTransaction(convenienceTransactionFromIntegerKeys(senderKeys.getPublic(),
                                                                   receiverKeys.getPublic(),
                                                                   20,
                                                                   senderKeys.getPrivate()));
    miner.waitFor(
      ledger.appendSignedTransaction(convenienceTransactionFromIntegerKeys(receiverKeys.getPublic(),
                                                                           senderKeys.getPublic(),
                                                                           20,
                                                                           receiverKeys.getPrivate()))
    );
    assertThat(chain.tipHash(), not(tip));
  }
}
