#! /bin/sh -e

procLib="/opt/homebrew-cask/Caskroom/processing/"
procLib+="2.2.1/Processing.app/Contents/Java/core.jar"

outputdir="build/"

if [ ! -d $outputdir ]; then
    mkdir $outputdir
fi

javac -classpath $procLib -g -d $outputdir *.java
java -classpath $procLib:$outputdir ImageProcessing