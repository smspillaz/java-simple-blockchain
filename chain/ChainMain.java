import java.util.Arrays;

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

import javafx.application.Platform;

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

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

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

    public static Blockchain fetchInitialBlockchain(String host) throws NoSuchAlgorithmException,
                                                                        Blockchain.WalkFailedException {
        if (host == null) {
            System.out.println("No host specified to download blockchain from, assuming this is the gensis node");
            return new Blockchain();
        } else {
            /* TODO: Implement ability to download, parse and validate existing
             * blockchain */
            return null;
        }
    }

    public static class Arguments {
        @Option(name="-keystore", usage="The Java KeyStore file to use (mandatory)", metaVar="KEYSTORE")
        public String keystore;

        @Option(name="-download-blockchain-from",
                usage="Name of a host to download a blockchain from. This node will be a gensis node otherwise",
                metaVar="HOST")
        public String downloadBlockchainFrom;

        public Arguments(String[] args) {
            CmdLineParser parser = new CmdLineParser(this);

            try {
                parser.parseArgument(args);

                if (keystore == null) {
                    throw new CmdLineException(parser, "Must provide a -keystore");
                }
            } catch (CmdLineException e) {
                System.err.println(e.getMessage());
                parser.printUsage(System.err);
                Platform.exit();
            }

            System.out.println(keystore);
        }
    }

    public static void main(String[] args) throws IOException,
                                                  NoSuchAlgorithmException,
                                                  KeyStoreException,
                                                  KeyManagementException,
                                                  CertificateException,
                                                  UnrecoverableKeyException,
                                                  Blockchain.WalkFailedException {
        Arguments arguments = new Arguments(args);
        HttpsServer server = HttpsServer.create(new InetSocketAddress(3002), 0);
        SSLContext context = ChainMain.createSSLContextForKeyFileStream(new FileInputStream(arguments.keystore),
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

        Blockchain chain = fetchInitialBlockchain(arguments.downloadBlockchainFrom);

        server.createContext("/transaction", new HttpHandler () {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = "Transaction Response";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream stream = exchange.getResponseBody();
                stream.write(response.getBytes(Charset.forName("UTF-8")));
                stream.close();
            }
        });
        server.createContext("/download_blockchain", new HttpHandler () {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = chain.serialise();
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseHeaders().put("Content-Type",
                                                  Arrays.asList(new String[] { "application/json" }));
                OutputStream stream = exchange.getResponseBody();
                stream.write(response.getBytes(Charset.forName("UTF-8")));
                stream.close();
            }
        });
        System.out.println("ChainMain server running, post requests to /transaction\n" +
                           "download blockchain from /download_blockchain");

        /* Main loop - the server can only be stopped here if we
         * call server.stop() elsewhere in the program */
        server.start();
    }
}

