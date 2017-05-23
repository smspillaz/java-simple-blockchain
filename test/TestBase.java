public class TestBase {
  static byte[] convenienceLongToPubKey(long key) {
    return Globals.convertToByteArray(key, Globals.nBytesKeys);
  }

  static byte[] convenienceLongToSig(long sig) {
    return Globals.convertToByteArray(sig, Globals.nBytesSig);
  }

  static Transaction convenienceTransactionFromIntegerKeys(long src,
                                                           long dst,
                                                           long amount,
                                                           int sig) {
    return new Transaction(convenienceLongToPubKey(src),
                           convenienceLongToPubKey(dst),
                           amount,
                           convenienceLongToSig(sig));
  }

  static byte[] convenienceTransactionPayloadFromIntegerKeys(int src,
                                                             int dst,
                                                             int amount,
                                                             int sig) {
    return convenienceTransactionFromIntegerKeys(src, dst, amount, sig).serialize();
  }
}