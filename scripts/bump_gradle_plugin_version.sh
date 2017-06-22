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
./gradlew bumpGradlePluginVersion -PgradlePluginVersion=${v}

# Push to SweetBlue repo
git add -u
git commit -m "Bump Gradle Plugin Version to $v"
git push origin HEAD
# Push to samples repo
cd library/script_output/samples/
git add -u
git commit -m "Bump Gradle Plugin Version to $v"
git push origin HEAD