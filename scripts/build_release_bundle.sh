#!/bin/bash
source ./config_paths.sh
#ANDROID_API_LEVEL=23
#SWEETBLUE_SRC_PATH=../src
#STAGE=script_output
#JAR_NAME=sweetblue_$SEMVER
#JAVADOC_JAR_NAME="sweetblue_${SEMVER}-javadoc"
#SOURCES_JAR_NAME="sweetblue_${SEMVER}-sources"
#BUNDLE_FOLDER=$STAGE/$JAR_NAME

ARG=$1

#./gradlew -b lint.gradle :lint

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
