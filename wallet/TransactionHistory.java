import java.util.List;
import java.util.LinkedList;

public class TransactionHistory {
    private List<Transaction> transactions;
    private int walletID;

    public TransactionHistory(int walletID, List<Transaction> transactions) {
        this.transactions = transactions;
        this.walletID = walletID;
    }

    public String log() {
        StringBuilder builder = new StringBuilder();

        for (Transaction transaction : transactions) {
            builder.append(transaction.toString() + "\n");
        }

        return builder.toString();
    }

    public int balance() {
        int balance = 0;

        for (Transaction transaction : transactions) {
            /* Handle the edge case where the genesis node gives
             * coins to itself */
            if (transaction.dst == walletID) {
                balance += transaction.amount;
            } else if (transaction.src == walletID) {
                balance -= transaction.amount;
            }
        }

        return balance;
    }
}