#!/bin/sh

if [ "$1" = "" ]
then
    echo "No version number provided"
    exit
fi

v=$(echo $1 | sed -e "s/\'|\"//g")
p="$(pwd)"
if [ "${p:(-7)}" = "scripts" ];
then
    cd ..
fi
./gradlew bumpBuildToolsVersion -PbuildToolsVersion=${v}
./gradlew gitAddCommitPush -Pmessage="Bump Build Tools Version to $v" # Push to SweetBlue repo
cd library/script_output/samples/
./gradlew gitAddCommitPush -Pmessage="Bump Build Tools Version to $v" # Push to samples repo
