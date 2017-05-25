#!/usr/bin/env python
#
# Generate some keys for the client and the server by shelling out to
# keytool. The server keystore is stored at server.jks and the client
# keystore will be stored at client.jks and client.pem, the latter of which
# can be used by curl for testing.

import argparse

import errno

import os

import subprocess

import shutil

import socket

import sys


def get_ip_address():
    """Get the IP address of this machine."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("8.8.8.8", 80))
    return s.getsockname()[0]


def main(args):
    """Generate keys for the client and the server."""
    parser = argparse.ArgumentParser("Generate keys for the client and server")
    parser.add_argument("password",
                        help="Password to use for the keystore",
                        metavar="PASSWORD",
                        type=str)
    parser.add_argument("host",
                        help="Hostname of the server",
                        metavar="HOSTNAME",
                        type=str)
    args = parser.parse_args(args)

    try:
        shutil.rmtree("keys")
    except OSError as error:
        if error.errno != errno.ENOENT:
            raise error

    try:
        os.makedirs("keys")
    except OSError as error:
        if error.errno != errno.EEXIST:
            raise error

    print("Generating server key-pairs into ./keys/server.jks")
    subprocess.check_call([
        "keytool",
        "-genkeypair",
        "-dname",
        "CN={}, OU=CSSE, O=University of Western Australia, "
        "L=Perth, S=Western Australia, C=AU".format(args.host),
        "-ext",
        "san=dns:{},ip:{}".format(args.host, get_ip_address()),
        "-keystore",
        os.path.join("keys", "server.jks"),
        "-keyalg",
        "RSA",
        "-keysize",
        "2048",
        "-storepass",
        args.password,
        "-alias",
        "server",
        "-keypass",
        args.password
    ])

    print("Generating client certificate into ./keys/client.pem")
    with open(os.path.join("keys", "client.pem"), "w") as clientPEMStream:
        clientPEMStream.write(subprocess.check_output([
            "keytool",
            "-exportcert",
            "-rfc",
            "-keystore",
            os.path.join("keys", "server.jks"),
            "-storepass",
            args.password,
            "-alias",
            "server"
        ]))

    print("Generating client keystore into ./keys/client.jks from certificate")
    subprocess.check_call([
        "keytool",
        "-import",
        "-noprompt",
        "-file",
        os.path.join("keys", "client.pem"),
        "-alias",
        "server",
        "-keystore",
        os.path.join("keys", "client.jks"),
        "-storepass",
        args.password,
        "-keypass",
        args.password,
    ])

    print("-----\n")
    print("All done. You can now run the server with something like\n"
          "KEYSTORE_PASSWORD=your-password java ChainMain path/to/server.jks\n"
          "and the client with\n"
          "KEYSTORE_PASSWORD=your-password java WalletMain HOST "
          "path/to/client.jks\n"
          "\n"
          "If you need to interact with the server using curl, you can\n"
          "use curl --cacert ./keys/client.pem -X METHOD https://HOST:PORT")


if __name__ == "__main__":
    main(sys.argv[1:])