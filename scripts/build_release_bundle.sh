#!/bin/sh

sh build_docs.sh

source config.sh

ANDROID_JAR=$ANDROID_HOME/platforms/android-$ANDROID_API_LEVEL/android.jar
JAR_DIR=$BUNDLE_FOLDER/jars
CP_TARGET=$BUNDLE_FOLDER/src_temp
ARGS=( "$@" )

function contains() {
local n=$#
local value=${!n}
for ((i=1;i < $#;i++)) {
if [ "${!i}" == "${value}" ]; then
echo "y"
return 0
fi
}
echo "n"
return 1
}

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


if [ $(contains "${ARGS[@]}" "no_samples") == "n" ];
then
    echo "${GLITZ}BUILDING SAMPLES${GLITZ}"
    git clone https://github.com/iDevicesInc/SweetBlue_Samples.git $STAGE/samples
    #sh $STAGE/samples/scripts/update_sweetblue.sh
    rsync -a $STAGE/samples/samples $BUNDLE_FOLDER --exclude scripts --exclude scripts
fi

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

if [ $(contains "${ARGS[@]}" "no_zip") == "n" ];
then
    echo "${GLITZ}ZIPPING UP${GLITZ}"
    rm -rf $CP_TARGET
    cd $STAGE
    zip -r $JAR_NAME.zip $JAR_NAME/*
    cd -
fi

if [ $(contains "${ARGS[@]}" "no_clean") == "n" ];
then
    echo "${GLITZ}CLEANING UP${GLITZ}"
    rm -rf $BUNDLE_FOLDER
    rm -rf $STAGE/samples
fi