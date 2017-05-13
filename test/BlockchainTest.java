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

    final String execeptedTransactionHash = "95BE1F104D6A8C778327982CBF475FC9869BACB63A2FCBF1E54491B9FDD60E97";

    assertThat(DatatypeConverter.printHexBinary(chain.tipHash()),
               equalTo(execeptedTransactionHash));
  }
}
