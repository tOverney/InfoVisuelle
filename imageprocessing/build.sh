#! /bin/sh -e

procLib="/opt/homebrew-cask/Caskroom/processing/"
procLib+="2.2.1/Processing.app/Contents/Java/core.jar"


javac -classpath $procLib -g *.java
java -classpath $procLib:. ImageProcessing