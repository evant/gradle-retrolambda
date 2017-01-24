#!/bin/sh
# A simple script to run tests both under java 7 and java 8.
# JAVA7_HOME and JAVA8_HOME environment variables must be set.

cd ./gradle-retrolambda
echo JAVA_HOME=$JAVA8_HOME
JAVA_HOME=$JAVA8_HOME ../gradlew test
echo JAVA_HOME=$JAVA7_HOME
JAVA_HOME=$JAVA7_HOME ../gradlew test
