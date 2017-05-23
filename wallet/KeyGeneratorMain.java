import java.io.FileNotFoundException;
import java.io.IOException;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javafx.application.Platform;

class KeyGeneratorMain {
    public static class Arguments {
        @Option(name="-keyfile", usage="Where to store an RSA private key", metaVar="KEYSTORE")
        public String keyfile;

        public Arguments(String args[]) {
            CmdLineParser parser = new CmdLineParser(this);

            try {
                parser.parseArgument(args);

                if (keyfile == null) {
                    throw new CmdLineException(parser, "Must provide a -keyfile");
                }
            } catch (CmdLineException e) {
                System.err.println(e.getMessage());
                parser.printUsage(System.err);
                Platform.exit();
            }
        }
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchProviderException {
        Arguments arguments = new Arguments(args);

        try {
            System.out.println(KeyGenerator.generateRSAKeyPairIntoKeyFilePath(arguments.keyfile));
        } catch (FileNotFoundException ex) {
            System.err.println("Couldn't write file " + arguments.keyfile + ": " + ex.getMessage());
            Platform.exit();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            Platform.exit();
        }
    }
}