# CITS3002
Group Assignment

# Installation
Install Gradle from https://gradle.org/install . To generate an IntelliJ compatible project
file you can just use `gradle idea`

# Building and Testing

The project itself can be built either using your favourite IDE after having exported the relevant project files from Gradle, or from Gradle itself. `gradle build` will get you to where you need to be pretty quickly. It will also run `FindBugs` and `PMD` on each run.

# Generating SSL keys
SSL keys are not checked into the repository, since the server certificate needs to encode its hostname and the hostname is not stable.

Instead, you will need to use the provided `genkeys.py` script. This will generate Java Key Store keys and an X509 Certificate in `keys/server.jks` `keys/client.jks` and `keys/client.pem`. For runtime, you only need to care about the JKS files, though if you want to use curl or Postman, you'll need to use the provided `client.pem` certificate.

To generate keys, you can use something like

> python genkeys.py KEYSTORE_PASSWORD HOSTNAME

# Running

Built classes are saved in `build/classes/main` after a successful run of `gradle build`. The `chain` server takes one argument, the location of its server-side key-store. It also requires `KEYSTORE_PASSWORD` to be set in the environment. So, if you're running from the build directory, you can use something like `KEYSTORE_PASSWORD=your-keystore-password ./server.sh -keystore ./keys/server.jks` to start the server.

If you want to test that SSL validation works, you can try poking the server with curl - `curl http://localhost:3002/transaction`. It should error out with a certificate validation error. Running curl again with `curl --cacert path/to/client.pem https://localhost:3002/transaction` should print `Transaction Response`.

To run the `client` simply start WalletMain (`./bin/clientGUI`). A window will display asking for hostname of the server it will be connecting to and a path to a client keystore. You will also be required to enter the `KEYSTORE_PASSWORD` you generated. If you generated keys using the `genkeys.py` script, the password will be the same for both (though note that the client keystore only contains the server's public certificate).

You can also test the wallet by running WalletCLI with something like:

`KEYSTORE_PASSWORD=your-keystore-password ./clientCLI.sh -keystore ./keys/server.jks -host HOSTNAME`

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
