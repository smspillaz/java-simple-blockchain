import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class WalletBlockchainConsumer {
    private Blockchain chain;

    public WalletBlockchainConsumer(Blockchain chain) {
        this.chain = chain;
    }

    public static class AccumulatingBalanceTransactionObserver implements Ledger.TransactionObserver {
        public int balance;
        private int walletID;

        AccumulatingBalanceTransactionObserver(int walletID) {
            this.balance = 0;
            this.walletID = walletID;
        }

        @Override
        public void consume(Transaction transaction) {
            if (transaction.dst == walletID) {
                balance += transaction.amount;
            } else if (transaction.src == walletID) {
                balance -= transaction.amount;
            }
        }
    }

    public static class LoggingTransactionObserver implements Ledger.TransactionObserver {
        Logger console;

        LoggingTransactionObserver(Logger console) {
            this.console = console;
        }

        @Override
        public void consume(Transaction transaction) {
            console.write(transaction.toString());
        }
    }

    public int ascertainBalance(int walletID, Logger transactionLogger) throws Blockchain.WalkFailedException {
        AccumulatingBalanceTransactionObserver observer = new AccumulatingBalanceTransactionObserver(walletID);
        List<Ledger.TransactionObserver> observers = new ArrayList<Ledger.TransactionObserver>() {{
            add(observer);
        }};

        if (transactionLogger != null) {
            observers.add(new LoggingTransactionObserver(transactionLogger));
        }

        new Ledger(chain, observers);
        return observer.balance;
    }

}
