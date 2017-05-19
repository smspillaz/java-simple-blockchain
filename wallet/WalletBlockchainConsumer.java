import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays;

public class WalletBlockchainConsumer {
    private Blockchain chain;

    public WalletBlockchainConsumer(Blockchain chain) {
        this.chain = chain;
    }

    public static class TransactionHistoryObserver implements Ledger.TransactionObserver {
        private int walletID;
        private List<Transaction> transactions;

        public TransactionHistoryObserver(int walletID) {
            this.walletID = walletID;
            this.transactions = new LinkedList<Transaction>();
        }

        @Override
        public void consume(Transaction transaction) {
            if (transaction.src == walletID ||
                transaction.dst == walletID) {
                this.transactions.add(transaction);
            }
        }

        public TransactionHistory history() {
            return new TransactionHistory(walletID, transactions);
        }
    }

    public TransactionHistory transactionHistory(int walletID) throws Blockchain.WalkFailedException {
        TransactionHistoryObserver observer = new TransactionHistoryObserver(walletID);

        new Ledger(chain, new LinkedList<Ledger.TransactionObserver>() {{
            add(observer);
        }});
        return observer.history();
    }

}
