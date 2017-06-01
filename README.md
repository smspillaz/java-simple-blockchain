# CITS3002

# Installation
Install Gradle from https://gradle.org/install . To generate an IntelliJ compatible project
file you can just use `gradle idea`

If you are running on Java 6, you will need to run an older version of Gradle
(2.14). You can obtain that from https://services.gradle.org/distributions/gradle-2.14.1-bin.zip.

Unzip the distribution and run the provided gradle binary in order to build
the project. You can just use ./path/to/gradle build to build the project.

# Building and Testing

The project itself can be built either using your favourite IDE after having exported the relevant project files from Gradle, or from Gradle itself. `gradle build` will get you to where you need to be pretty quickly. It will also run `FindBugs` and `PMD` on each run.

On Java 6, you won't be able to run `FindBugs` or `PMD`. Use `gradle compileJava` and `gradle test` to run
the unit tests.

# Generating SSL keys
SSL keys are not checked into the repository, since the server certificate needs to encode its hostname and the hostname is not stable.

Instead, you will need to use the provided `genkeys.py` script. This will generate Java Key Store keys and an X509 Certificate in `keys/server.jks` `keys/client.jks` and `keys/client.pem`. For runtime, you only need to care about the JKS files, though if you want to use curl or Postman, you'll need to use the provided `client.pem` certificate.

To generate keys, you can use something like.

Note that if you want to run on a server that doesn't have a publicly accessible
hostname, you'll need to provide the IP address in the HOSTNAME field. Sadly, however, the `keytool` utility
on Java 6 does not support specifying an IP address on its own. You will need to use `keytool` from a machine that has Java 8.

> python genkeys.py KEYSTORE_PASSWORD HOSTNAME

# Generating RSA signing keys
Another part of the equation is to generate wallet keypairs to sign and
validate individual transactions. Again, these keys are not provided in the
repository, but a tool exists to quickly generate them yourself. You can use
the `KeyGeneratorMain` class to generate these keys. By default it saves
the private key on disk and prints the public key to the standard out.
For instance:

    ./bin/keygen.sh -keyfile ./keys/wallet.pem > ./keys/wallet.pem.pub

Obviously, you should share the public portion and keep safe the private
portion, since it is used to sign transactions.

# Running

Built classes are saved in `build/classes/main` after a successful run of `gradle build`. The `chain` server takes one argument, the location of its server-side key-store. It also requires `KEYSTORE_PASSWORD` to be set in the environment. So, if you're running from the build directory, you can use something like `KEYSTORE_PASSWORD=your-keystore-password ./server.sh -keystore ./keys/server.jks` to start the server.

The server has two modes of operation. It can clone and validate a blockchain
from some other server, by providing `TRUSTSTORE_PASSWORD` `-truststore` and
`-download-blockchain-from`, for example:

    KEYSTORE_PASSWORD=your-keystore-password TRUSTSTORE_PASSWORD=your-keystore-password ./bin/server.sh -keystore ./keys/server.jks -truststore ./keys/client.jks -download-blockchain-from HOSTNAME

Or, the server can be a "genesis root" of a new blockchain that exists
on the network, by providing `-sign-genesis-block-with` `-genesis-block-public-key`
and `-genesis-amount`, for instance:

    KEYSTORE_PASSWORD=your-keystore-password ./bin/server.sh -keystore ./keys/server.jks -truststore ./keys/client.jks -genesis-block-public-key $(cat ./keys/wallet.pem.pub) -genesis-amount 50 -sign-genesis-block-with ./keys/wallet.pem

If you want to change the problem difficulty of the hash rate, you can use the
`-problem-difficulty` option to change the number of leading zeroes that must
be included in a block hash for it to be accepted on to the chain. The problem
difficulty is fixed over the life of the blockchain itself.

If you want to test that SSL validation works, you can try poking the server with curl - `curl http://localhost:3002/transaction`. It should error out with a certificate validation error. Running curl again with `curl --cacert path/to/client.pem https://localhost:3002/transaction` should print `Transaction Response`.

To run the `client` simply start WalletCLI (`./bin/clientCLI`) with `KEYSTORE_PASSWORD`
and pass a `-keystore` with a path to the generated keystore file, a `-wallet-id`
with the public key of the wallet (encoded in hexadecimal format) and a
`-host` with the address of the blockchain hosting server. For instance:

    KEYSTORE_PASSWORD=your-keystore-password ./clientCLI.sh -keystore ./keys/client.jks -host HOSTNAME -wallet-id $(cat ./keys/wallet.pem.pub)

# Testing bad behaviour

The included unit tests should cover how the system internally handles malformed
blockchains, not-sane transaction records and signature violations. However, if
you want to see how this works in a real testcase, you can have the chain server
generate an invalid blockchain and see what happens. To do that, pass
`-corrupt-chain-with` with a modifier such as:

* `MODIFY_TX`: Modify a transaction, breaking its hash (integrity checking
               should fail).
* `INVALID_TX`: Generate a clearly invalid transaction (flipping its polarity
                but regenerate the hash chain and re-sign the transaction). The
                client should reject this transaction because it doesn't
                make any sense
* `BAD_SIGNATURE`: Modify a transaction, but regenerate its block hash. The
                   signature check should fail.
* `BAD_POW`: Append a transaction to the chain with an invalid proof of work
             (nonce). The client should reject this chain because we cannot
             guaranatee that an independent third party carried out the
             transaction.
