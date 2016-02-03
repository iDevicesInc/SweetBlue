#!/bin/bash
source ./config_paths.sh

ARG=$1

#./gradlew -b lint.gradle :lint

if [ "$?" != 0 ];
then
	echo "Lint errors! Please fix them before running this again."
	exit 1
fi

if [ "$ARG" == "info" ]
then
	./gradlew fullBuild --info
	cp $STAGE/sweetblue.zip $STAGE/$JAR_NAME.zip
elif [ "$ARG" == "stacktrace" ]
then
	./gradlew fullBuild --stacktrace
	cp $STAGE/sweetblue.zip $STAGE/$JAR_NAME.zip
elif [ "$ARG" == "no_zip" ]
then
	./gradlew bundleNoZip
else
	./gradlew fullBuild
	cp $STAGE/sweetblue.zip $STAGE/$JAR_NAME.zip	
fi
