import java.net.URLConnection;
import java.io.InputStream;
import java.net.URL;
import java.io.IOException;
import java.util.Scanner;


public class WalletMain {
    public static void usage() {
        System.err.println("WalletMain SERVER_HOST");
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            usage();
            System.exit(1);
        }
        String serverHost = args[0];

        // Send a HTTP request to server
        String url = "http://" + serverHost + ":3002/transaction";

        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        InputStream response = connection.getInputStream();

        Scanner s = new Scanner(response, "UTF-8").useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";

        System.out.println(result);
    }
}
