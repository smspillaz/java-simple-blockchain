import java.util.List;
import java.util.LinkedList;

import javax.xml.bind.DatatypeConverter;

public class TransactionHistory {
    private List<Transaction> transactions;
    private String walletID;

    public TransactionHistory(String walletID, List<Transaction> transactions) {
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
            if (DatatypeConverter.printHexBinary(transaction.rPubKey) == walletID) {
                balance += transaction.amount;
            } else if (DatatypeConverter.printHexBinary(transaction.sPubKey) == walletID) {
                balance -= transaction.amount;
            }
        }

        return balance;
    }
}