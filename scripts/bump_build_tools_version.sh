#!/bin/sh

buildToolsVersion=$(echo $1 | sed -e "s/\'//g")
cd ..
./gradlew bumpBuildToolsVersion -PbuildToolsVersion=${buildToolsVersion}
