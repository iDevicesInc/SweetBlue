#!/bin/sh
source config_paths.sh

ARG=$1

sh gradlew -b lint.gradle :lint

if [ "$?" != 0 ];
then
	echo "Lint errors! Please fix them before running this again."
	exit 1
fi

if [ "$ARG" == "info" ]
then
	sh gradlew fullBuild --info
	cp $STAGE/sweetblue.zip $STAGE/$JAR_NAME.zip
elif [ "$ARG" == "stacktrace" ]
then
	sh gradlew fullBuild --stacktrace
	cp $STAGE/sweetblue.zip $STAGE/$JAR_NAME.zip
elif [ "$ARG" == "no_zip" ]
then
	sh gradlew bundleNoZip
else
	sh gradlew fullBuild
	cp $STAGE/sweetblue.zip $STAGE/$JAR_NAME.zip	
fi
