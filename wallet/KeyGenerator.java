import java.io.FileNotFoundException;
import java.io.IOException;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import javax.xml.bind.DatatypeConverter;

public class KeyGenerator {
    private static void formatAndWritePemFile(Key key,
                                              String description,
                                              String keyFilePath) throws FileNotFoundException,
                                                                         IOException,
                                                                         NoSuchAlgorithmException,
                                                                         NoSuchProviderException {
        FileOutputStream fos = new FileOutputStream(keyFilePath);
        OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
        PemWriter pemWriter = new PemWriter(writer);

        try {
            pemWriter.writeObject(new PemObject(description, key.getEncoded()));
        } finally {
            pemWriter.close();
        }
    }

    public static String generateRSAKeyPairIntoKeyFilePath(String keyFilePath) throws NoSuchAlgorithmException,
                                                                                      NoSuchProviderException,
                                                                                      IOException,
                                                                                      FileNotFoundException {
        Security.addProvider(new BouncyCastleProvider());

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
        generator.initialize(2048);

        KeyPair pair = generator.generateKeyPair();

        /* Write files out to the given paths. The public key is always written
         * to the standard output and the private key is written to the
         * given keyFilePath */
        formatAndWritePemFile(pair.getPrivate(), "RSA PRIVATE KEY", keyFilePath);

        return DatatypeConverter.printHexBinary(pair.getPublic().getEncoded());
    }
}