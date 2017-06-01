import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
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
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyManagementException;
import java.security.Security;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;

import javax.xml.bind.DatatypeConverter;

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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.Security;
import java.security.NoSuchProviderException;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

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

    public static class LedgerChain {
        AsynchronouslyMutableLedger ledger;
        Blockchain chain;
        BlockMiner miner;
        int postedTransactionId;

        public LedgerChain(AsynchronouslyMutableLedger ledger,
                           Blockchain chain,
                           BlockMiner miner,
                           int postedTransactionId) {
            this.ledger = ledger;
            this.chain = chain;
            this.miner = miner;
            this.postedTransactionId = postedTransactionId;
        }
    }

    public static class TransactionLoggingMiningObserver implements BlockMiner.MiningObserver {
        public void blockMined(byte[] payload) {
            SignedObject blob = new SignedObject(payload);
            Transaction transaction = new Transaction(blob.payload);

            System.out.println("[chain] Mined transaction " + transaction);
        }
    }

    public static LedgerChain fetchInitialLedgerAndChain(String host,
                                                         String truststore,
                                                         String truststorePassword,
                                                         String genesisBlockPublicKey,
                                                         Integer genesisBlockAmount,
                                                         String signGenesisBlockWith,
                                                         Long problemDifficulty) throws NoSuchAlgorithmException,
                                                                                        NoSuchProviderException,
                                                                                        IOException,
                                                                                        InvalidKeyException,
                                                                                        InvalidKeySpecException,
                                                                                        SignatureException,
                                                                                        Blockchain.WalkFailedException,
                                                                                        Block.MiningException,
                                                                                        FileNotFoundException,
                                                                                        CertificateException,
                                                                                        KeyStoreException,
                                                                                        KeyManagementException,
                                                                                        UnrecoverableKeyException,
                                                                                        MalformedURLException,
                                                                                        Blockchain.IntegrityCheckFailedException {
        if (host == null) {
            System.out.println("No host specified to download blockchain from, " +
                               "creating genesis node with public key " +
                               genesisBlockPublicKey + " and starting with amount " +
                               genesisBlockAmount);

            /* The byte-encoded public key, deserialised from a hex-binary representation */
            byte[] pubKey = DatatypeConverter.parseHexBinary(genesisBlockPublicKey);

            try {
                Blockchain chain = new Blockchain(problemDifficulty);
                BlockMiner miner = new BlockMiner(chain,
                                                  new TransactionLoggingMiningObserver(),
                                                  problemDifficulty);
                AsynchronouslyMutableLedger ledger = new AsynchronouslyMutableLedger(chain, miner);
                int postedTransactionId = ledger.appendSignedTransaction(new SignedObject(
                    new Transaction(pubKey,
                                    pubKey,
                                    50).serialize(),
                    KeyFactory.getInstance("RSA", "BC").generatePrivate(
                        new PKCS8EncodedKeySpec(
                            KeyGenerator.readKeyFromFile(signGenesisBlockWith)
                        )
                    )
                ));

                return new ChainMain.LedgerChain(ledger, chain, miner, postedTransactionId);
            } catch (FileNotFoundException e) {
                System.err.println("Error creating genesis node, the genesis " +
                                   "block signing key was not found at " + signGenesisBlockWith +
                                   ": " + e.getMessage());
                throw new RuntimeException(e.getMessage());
            }
        }

        /* Download and parse an existing blockchain from some other server */
        System.out.println("Downloading blockchain from host " + host);
        WalletOrchestrator orchestrator = new WalletOrchestrator(host, truststore, truststorePassword);
        Blockchain chain = orchestrator.fetchBlockchain();
        BlockMiner miner = new BlockMiner(chain,
                                          new TransactionLoggingMiningObserver(),
                                          problemDifficulty);
        AsynchronouslyMutableLedger ledger = new AsynchronouslyMutableLedger(chain, miner);
        return new ChainMain.LedgerChain(ledger, chain, miner, 0);
    }

    public static class Arguments {
        @Option(name="-keystore", usage="The Java KeyStore file to use (mandatory)", metaVar="KEYSTORE")
        public String keystore;

        @Option(name="-truststore", usage="The Java KeyStore file to use when trusting other servers", metaVar="KEYSTORE")
        public String truststore;

        @Option(name="-corrupt-chain-with",
                usage="Op to corrupt chain with (MODIFY_TX, INVALID_TX, BAD_SIGNATURE, BAD_POW)",
                metaVar="OP")
        public String corruptChainWith;

        @Option(name="-download-blockchain-from",
                usage="Name of a host to download a blockchain from. This node will be a gensis node otherwise",
                metaVar="HOST")
        public String downloadBlockchainFrom;

        @Option(name="-genesis-block-public-key",
                usage="Public key of genesis block",
                metaVar="PUBLIC_KEY_STRING")
        public String genesisBlockPublicKey;

        @Option(name="-genesis-amount",
                usage="Amount paid to gensis block on first transaction",
                metaVar="AMOUNT")
        public Integer genesisBlockAmount;

        @Option(name="-sign-genesis-block-with",
                usage="Path to private key to sign genesis block with",
                metaVar="PRIVATE_KEY_PATH")
        public String signGensisBlockWith;

        @Option(name="-problem-difficulty",
                usage="The problem difficulty (1 to 63)",
                metaVar="DIFFICULTY")
        public Long problemDifficulty = Long.valueOf(4);

        @SuppressFBWarnings(value="UR_UNINIT_READ",
                            justification="Values are set by CmdLineParser")
        public Arguments(String[] args) {
            CmdLineParser parser = new CmdLineParser(this);

            try {
                parser.parseArgument(args);

                if (keystore == null) {
                    throw new CmdLineException(parser, "Must provide a -keystore");
                }

                if (downloadBlockchainFrom == null &&
                    (genesisBlockPublicKey == null ||
                     genesisBlockAmount == null ||
                     signGensisBlockWith == null)) {
                    throw new CmdLineException(
                        parser,
                        "Must provide either a -download-blockchain-from " +
                        "or a triple of -genesis-block-public-key " +
                        "-genesis-amount and " +
                        "-sign-genesis-block-with"
                    );
                }

                if (downloadBlockchainFrom != null && truststore == null) {
                    throw new CmdLineException(
                        parser,
                        "Must provide a -truststore when specifying " +
                        "-download-blockchain-from"
                    );
                }

                if (corruptChainWith != null) {
                    /* Check to make sure that it is a valid operation */
                    List<String> validOps = Arrays.asList(new String[] {
                        "MODIFY_TX",
                        "INVALID_TX",
                        "BAD_SIGNATURE",
                        "BAD_POW"
                    });

                    if (!validOps.contains(corruptChainWith)) {
                        throw new CmdLineException(parser, "-corrupt-chain-with must be one of " +
                                                           "MODIFY_TX, INVALID_TX, BAD_SIGNATURE, or BAD_POW");
                    }
                }

                if (System.getenv("KEYSTORE_PASSWORD") == null) {
                    throw new CmdLineException(parser, "Must set KEYSTORE_PASSWORD in the environment");
                }

                if (System.getenv("TRUSTSTORE_PASSWORD") == null && truststore != null) {
                    throw new CmdLineException(parser, "Must set TRUSTSTORE_PASSWORD in the environment when using -truststore");
                }
            } catch (CmdLineException e) {
                parser.printUsage(System.err);
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    public static void rehashChainFromIndex(final Blockchain chain,
                                            final int rehashFrom,
                                            final long problemDifficulty) {
        try {
            chain.walk(new Blockchain.BlockEnumerator() {
                public void consume(int index, Block block) throws Blockchain.WalkFailedException {
                    if (index >= rehashFrom) {
                        try {
                            int nonce = Block.mineNonce(block.payload,
                                                        chain.parentBlockHash(index),
                                                        problemDifficulty);
                            block.nonce = nonce;
                            block.hash = block.computeContentHash(chain.parentBlockHash(index));
                        } catch (NoSuchAlgorithmException e) {
                            throw new RuntimeException(e.getMessage());
                        } catch (Block.MiningException e) {
                            throw new Blockchain.WalkFailedException(e.getMessage());
                        }
                    }
                }
            });
        } catch (Blockchain.WalkFailedException e) {
            System.err.println("Failed to walk entire chain because: " + e.getMessage());
        }
    }

    /* Perform some corruption on the underlying chain itself, such that
     * when clients download it they should be able to detect problems.
     *
     * Note that because the private key of a client is necessarily secret,
     * we are unable to re-sign transactions once they have been modified -
     * their signature is necessarily destroyed. However, the signature is
     * always checked last in the validation process, which means that
     * we can detect other problems beforehand */
    public static void performChainCorruption(final Blockchain chain,
                                              final Ledger ledger,
                                              final String op,
                                              final long problemDifficulty) throws Blockchain.WalkFailedException,
                                                                                   Block.MiningException {
        final int indexToModify = ((int) Math.ceil(Math.random())) % chain.length();
        chain.walk(new Blockchain.BlockEnumerator() {
            public void consume(int index, Block block) {
                StringBuilder msg = new StringBuilder();
                msg.append("Maliciously modifying block " + block.toString() + " by: ");
                if (index == indexToModify) {
                    switch (op) {
                    case "MODIFY_TX":
                        /* Modify a transaction in flight, for instance, by subtracting
                         * one from the amount. This should be caught by the fact that
                         * the block doesn't hash correctly anymore. */
                        msg.append("subtracting 1 from the transaction amount");
                        block.payload = SignedObject.withMutations(block.payload, new SignedObject.Mutator() {
                            public void mutate(SignedObject blob) {
                                blob.payload = Transaction.withMutations(blob.payload, new Transaction.Mutator() {
                                    public void mutate(Transaction transaction) {
                                        transaction.amount -= 1;
                                    }
                                });
                            }
                        });
                        break;
                    case "INVALID_TX":
                        /* Make a transaction negative and remine it. This
                         * problem should be caught by the fact that transaction
                         * is nonsensical */
                        msg.append("negating the transaction amount");
                        block.payload = SignedObject.withMutations(block.payload, new SignedObject.Mutator() {
                            public void mutate(SignedObject blob) {
                                blob.payload = Transaction.withMutations(blob.payload, new Transaction.Mutator() {
                                    public void mutate(Transaction transaction) {
                                        transaction.amount *= -1;
                                    }
                                });
                            }
                        });
                        rehashChainFromIndex(chain, index, problemDifficulty);
                        break;
                    case "BAD_SIGNATURE":
                        /* Subtract one from the transaction and remine it. This
                         * problem should be caught by the fact that the signature
                         * on the transaction is no longer valid */
                        msg.append("modifying the transaction amount but rehashing the block (and all children)");
                        block.payload = SignedObject.withMutations(block.payload, new SignedObject.Mutator() {
                            public void mutate(SignedObject blob) {
                                blob.payload = Transaction.withMutations(blob.payload, new Transaction.Mutator() {
                                    public void mutate(Transaction transaction) {
                                        transaction.amount -= 1;
                                    }
                                });
                            }
                        });
                        rehashChainFromIndex(chain, index, problemDifficulty);
                        break;
                    case "BAD_POW":
                        /* Change the nonce on the block but don't remine it. This
                         * problem should be caught by the fact that the hash
                         * does not satisfy the problem difficulty. */
                        msg.append("adding a block with a bad proof of work function");
                        block.nonce = 0;
                        try {
                            block.hash = block.computeContentHash(chain.parentBlockHash(index));
                        } catch (NoSuchAlgorithmException e) {
                            throw new RuntimeException(e.getMessage());
                        }

                        /* We rehash from this index + 1 onwards - we wanted to rehash
                         * the current block without necessarily re-mining it
                         * which is what we did above. */
                        rehashChainFromIndex(chain, index + 1, problemDifficulty);
                        break;
                    default:
                        msg.append("... doing nothing. Is this a correct modify op?");
                        break;
                    }
                }

                System.out.println(msg.toString());
            }
        });
    }

    public static byte[] readAllBytes(InputStream stream) throws IOException {
        try (ByteArrayOutputStream ba = new ByteArrayOutputStream();) {
            byte[] buffer = new byte[0xFFFF];
            int read = 0;

            while ((read = stream.read(buffer, 0, buffer.length)) != -1) {
                ba.write(buffer, 0, read);
            }

            ba.flush();
            return ba.toByteArray();
        }
    }

    public static void main(String[] args) throws IOException,
                                                  NoSuchAlgorithmException,
                                                  NoSuchProviderException,
                                                  InvalidKeyException,
                                                  InvalidKeySpecException,
                                                  KeyStoreException,
                                                  KeyManagementException,
                                                  CertificateException,
                                                  UnrecoverableKeyException,
                                                  SignatureException,
                                                  Blockchain.WalkFailedException,
                                                  Block.MiningException,
                                                  FileNotFoundException,
                                                  MalformedURLException,
                                                  Blockchain.IntegrityCheckFailedException {
        Arguments arguments = new Arguments(args);
        Security.addProvider(new BouncyCastleProvider());

        final HttpsServer server = HttpsServer.create(new InetSocketAddress(3002), 0);
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

        /* We need to create the ledger and chain at the same time so that
         * we can track all the transactions, including the genesis node */
        ChainMain.LedgerChain lc = fetchInitialLedgerAndChain(arguments.downloadBlockchainFrom,
                                                              arguments.truststore,
                                                              System.getenv("TRUSTSTORE_PASSWORD"),
                                                              arguments.genesisBlockPublicKey,
                                                              arguments.genesisBlockAmount,
                                                              arguments.signGensisBlockWith,
                                                              arguments.problemDifficulty);
        final Blockchain chain = lc.chain;
        final AsynchronouslyMutableLedger ledger = lc.ledger;
        BlockMiner miner = lc.miner;
        int postedTransactionId = lc.postedTransactionId;

        if (arguments.corruptChainWith != null) {
            /* Wait for the first transaction to complete */
            miner.waitFor(postedTransactionId);
            performChainCorruption(chain, ledger, arguments.corruptChainWith, arguments.problemDifficulty);
        }

        server.createContext("/transaction", new HttpHandler () {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String json = new String(readAllBytes(exchange.getRequestBody()), "UTF-8");
                Models.Transaction record = Models.Transaction.deserialise(json);

                OutputStream stream = exchange.getResponseBody();

                try {
                    /* Put the transaction on the queue. We won't be able to
                     * validate it until we've finished mining all our other
                     * blocks, so the client will just have to wait */
                    Transaction transaction = new Transaction(DatatypeConverter.parseHexBinary(record.src),
                                                              DatatypeConverter.parseHexBinary(record.dst),
                                                              record.amount);
                    SignedObject blob = new SignedObject(transaction.serialize(),
                                                         DatatypeConverter.parseHexBinary(record.signature));
                    ledger.appendSignedTransaction(blob);

                    String response = Boolean.TRUE.toString();

                    exchange.sendResponseHeaders(200, response.length());

                    stream.write(response.getBytes(Charset.forName("UTF-8")));
                } catch (NoSuchAlgorithmException e) {
                    stream.write("false".getBytes(Charset.forName("UTF-8")));
                } finally {
                    stream.close();
                }
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

