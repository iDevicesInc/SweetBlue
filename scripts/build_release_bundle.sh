#!/bin/sh
ARG=$1
if [ "$ARG" == "info" ]
then
	sh gradlew fullBuild --info
elif [ "$ARG" == "stacktrace" ]
then
	sh gradlew fullBuild --stacktrace
elif [ "$ARG" == "no_zip" ]
then
	sh gradlew bundleNoZip
else
	sh gradlew fullBuild
fi
