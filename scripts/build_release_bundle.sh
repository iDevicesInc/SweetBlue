#!/bin/sh

#sh build_then_upload_docs.sh

source config.sh

SWEETBLUE_SRC_PATH=../src
STAGE=script_output
JAR_NAME=sweetblue_$SEMVER
JAVADOC_JAR_NAME="sweetblue_${SEMVER}-javadoc"
SOURCES_JAR_NAME="sweetblue_${SEMVER}-sources"
BUNDLE_FOLDER=$STAGE/$JAR_NAME
ANDROID_JAR=$ANDROID_HOME/platforms/android-$ANDROID_API_LEVEL/android.jar
JAR_DIR=$BUNDLE_FOLDER/jars
CP_TARGET=$BUNDLE_FOLDER/src_temp

echo "${GLITZ}COPYING FILES${GLITZ}"

mkdir -p $BUNDLE_FOLDER
mkdir -p $JAR_DIR
rsync -a ../ $BUNDLE_FOLDER --exclude scripts
mv $BUNDLE_FOLDER/src $CP_TARGET
cp -rf $SWEETBLUE_SRC_PATH $BUNDLE_FOLDER
rm -rf $BUNDLE_FOLDER/scripts
rm -rf $BUNDLE_FOLDER/.git
rm -rf $BUNDLE_FOLDER/.gitignore
rm -rf $BUNDLE_FOLDER/test

echo "${GLITZ}COMPILING${GLITZ}"

cd $CP_TARGET
javac -cp $ANDROID_JAR com/idevicesinc/sweetblue/*.java com/idevicesinc/sweetblue/utils/*.java
find ./com -name "*.java" -type f|xargs rm -f
find ./com -name "*.DS_Store" -type f|xargs rm -f
cd -

echo "${GLITZ}BUILDING JARS${GLITZ}"
cd $CP_TARGET
jar cf $JAR_NAME.jar com/*
cd -
mv $CP_TARGET/$JAR_NAME.jar $JAR_DIR/$JAR_NAME.jar


cd $BUNDLE_FOLDER/docs/api
jar cf $JAVADOC_JAR_NAME.jar ./*
cd -
mv $BUNDLE_FOLDER/docs/api/$JAVADOC_JAR_NAME.jar $JAR_DIR/$JAVADOC_JAR_NAME.jar


cd $BUNDLE_FOLDER/src
jar cf $SOURCES_JAR_NAME.jar com/*
cd -
mv $BUNDLE_FOLDER/src/$SOURCES_JAR_NAME.jar $JAR_DIR/$SOURCES_JAR_NAME.jar


echo "${GLITZ}ZIPPING UP${GLITZ}"
rm -rf $CP_TARGET
cd $STAGE
    zip -r $JAR_NAME.zip $JAR_NAME/*
cd -

echo "${GLITZ}CLEANING UP${GLITZ}"
rm -rf $BUNDLE_FOLDER