#!/bin/sh

ARG=$1
if [ "$ARG" == "info" ]
then
	sh gradlew fullBuild --info
elif [ "$ARG" == "stacktrace" ]
then
	sh gradlew fullBuild --stacktrace
else
	sh gradlew fullBuild
fi
