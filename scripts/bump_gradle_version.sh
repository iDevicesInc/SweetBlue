#!/bin/sh

v=$(echo $1 | sed -e "s/\'//g")
cd ..
./gradlew bumpGradleVersion -PgradleVersion=${v}
