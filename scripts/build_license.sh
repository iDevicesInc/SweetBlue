#!/bin/sh

LEVEL=$1
cd ..
if [ "$LEVEL" = "standard" ]
then
   sh gradlew createStandardLicense
else
   sh gradlew createProLicense
fi
cd -