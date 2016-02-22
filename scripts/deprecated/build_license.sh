#!/bin/sh

LEVEL=$1
if [ "$LEVEL" = "standard" ]
then
   sh gradlew createStandardLicense
else
   sh gradlew createProLicense
fi
