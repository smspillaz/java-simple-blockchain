import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ChainMain {
    public static void main(String[] args) throws IOException {
        System.out.println("ChainMain server running, post requests to /transaction");
        HttpServer server = HttpServer.create(new InetSocketAddress(3002), 0);
        server.createContext("/transaction", new ChainHTTPHandler());
        server.start();
    }

    public static class ChainHTTPHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Transaction Response";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream stream = exchange.getResponseBody();
            stream.write(response.getBytes(Charset.forName("UTF-8")));
            stream.close();
        }
    }
}

