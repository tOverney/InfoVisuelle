#! /bin/sh -e

javac -classpath /opt/homebrew-cask/Caskroom/processing/2.2.1/Processing.app/Contents/Java/core.jar -g $1.java
java -classpath /opt/homebrew-cask/Caskroom/processing/2.2.1/Processing.app/Contents/Java/core.jar:. $1