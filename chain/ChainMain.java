import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

public class ChainMain {
    private static SSLContext createSSLContextForKeyFileStream(InputStream keyStoreStream,
                                                               char[] password) throws CertificateException,
                                                                                       NoSuchAlgorithmException,
                                                                                       KeyStoreException,
                                                                                       IOException,
                                                                                       KeyManagementException,
                                                                                       UnrecoverableKeyException {
        SSLContext context = SSLContext.getInstance("TLS");
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(keyStoreStream, password);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, password);

        context.init(keyManagerFactory.getKeyManagers(),
                     null,
                     null);

        return context;
    }

    public static void main(String[] args) throws IOException,
                                                  NoSuchAlgorithmException,
                                                  KeyStoreException,
                                                  KeyManagementException,
                                                  CertificateException,
                                                  UnrecoverableKeyException {
        HttpsServer server = HttpsServer.create(new InetSocketAddress(3002), 0);
        SSLContext context = ChainMain.createSSLContextForKeyFileStream(new FileInputStream(args[0]),
                                                                        System.getenv("KEYSTORE_PASSWORD")
                                                                              .toCharArray());

        server.setHttpsConfigurator(new HttpsConfigurator(context) {
            public void configure(HttpsParameters params) {
                try {
                    SSLContext context = SSLContext.getDefault();
                    SSLEngine engine = context.createSSLEngine();
                    params.setNeedClientAuth(false);
                    params.setCipherSuites(engine.getEnabledCipherSuites());
                    params.setProtocols(engine.getEnabledProtocols());

                    params.setSSLParameters(context.getDefaultSSLParameters());
                } catch (NoSuchAlgorithmException exception) {
                    System.err.println(exception.toString());
                    server.stop(0);
                }
            }
        });

        server.createContext("/transaction", new ChainHTTPHandler());
        System.out.println("ChainMain server running, post requests to /transaction");

        /* Main loop - the server can only be stopped here if we
         * call server.stop() elsewhere in the program */
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

