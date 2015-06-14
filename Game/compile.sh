#! /bin/sh -e

pathToMainFolder="/opt/homebrew-cask/Caskroom/processing/2.2.1/Processing.app/Contents/Java/"

# core lib
procLib=$pathToMainFolder"core.jar"

# video library path
procLib+=":"$pathToMainFolder"modes/java/libraries/video/library/video.jar"

# some other OpenGL libraries
procLib+=":"$pathToMainFolder"modes/java/libraries/video/library/gstreamer-java.jar"

# another lib
procLib+=":"$pathToMainFolder"modes/java/libraries/video/library/jna.jar"

# seems we also need this!
procLib+=":"$pathToMainFolder"core/library/*"

outputdir="build/"

if [ ! -d $outputdir ]; then
    mkdir $outputdir
fi

if [ $1 ]; then
    procLib=$1
fi
procLib+=":../libraries/*"

# argRun='-Dgstreamer.library.path="'$pathToMainFolder'modes/java/libraries/video/library/macosx64"'
# argRun+=' -Dgstreamer.plugin.path="'$pathToMainFolder'modes/java/libraries/video/library/macosx64/plugins"'

if [ $2 ]; then
    argRun=$2
fi

echo $argRun

javac -classpath $procLib -g -d $outputdir *.java
java -classpath $procLib:$outputdir Game