#!/bin/bash

java -cp "build/classes/main:build/libs/args4j-2.33.jar:build/libs/bcprov-jdk15on-1.51.jar" KeyGeneratorMain $@
