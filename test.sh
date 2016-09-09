#!/bin/sh
# A simple script to run tests both under java 7 and java 8.
# JAVA7_HOME and JAVA8_HOME environment variables must be set.

echo JAVA_HOME=$JAVA8_HOME
JAVA_HOME=$JAVA8_HOME ./gradlew gradle-retrolambda:test
echo JAVA_HOME=$JAVA7_HOME
JAVA_HOME=$JAVA7_HOME ./gradlew gradle-retrolambda:test
