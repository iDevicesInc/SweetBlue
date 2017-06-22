#!/bin/sh

if [ "$1" = "" ]
then
    echo "No version number provided"
    exit
fi

v=$(echo $1 | sed -e "s/\'|\"//g")
cd ..
./gradlew bumpGradlePluginVersion -PgradlePluginVersion=${v}
./gradlew gitAddCommitPush -Pmessage="Bump Gradle Plugin Version to $v"
