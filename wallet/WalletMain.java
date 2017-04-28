import java.net.URLConnection;
import java.io.InputStream;
import java.net.URL;
import java.io.IOException;
import java.util.Scanner;


public class WalletMain {
    public static void main(String[] args) throws IOException {

        // Send a HTTP request

        String url = "http://localhost:3002/transaction";

        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        InputStream response = connection.getInputStream();

        Scanner s = new Scanner(response).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";

        System.out.println(result);
    }
}
