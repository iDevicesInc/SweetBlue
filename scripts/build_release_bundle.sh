#!/bin/sh

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

echo "${GLITZ}BUILD_JAVADOC${GLITZ}"
if [ $(contains "${ARGS[@]}" "no_docs") == "n" ];
then
    if [ $(contains "${ARGS[@]}" "upload") == "y" ];
    then
        sh build_then_upload_docs.sh
    else
        sh build_docs.sh
    fi
fi


echo "${GLITZ}BUILD_LICENSES${GLITZ}"

if [ $(contains "${ARGS[@]}" "upload") == "y" ];
then
    sh build_then_upload_licenses.sh
else
    sh build_licenses.sh
fi

if [ $(contains "${ARGS[@]}" "no_samples") == "n" ];
then
    echo "${GLITZ}BUILDING SAMPLES${GLITZ}"
    cd ..
    git clone https://github.com/iDevicesInc/SweetBlue_Samples.git $STAGE/samples
    cd $STAGE/samples/scripts/
	sh update_sweetblue.sh
    cd -
    rsync -a $STAGE/samples/samples $BUNDLE_FOLDER --exclude scripts --exclude scripts
fi

echo "${GLITZ}COMPILING AND BUILDING JARS${GLITZ}"
cd ..
sh gradlew build sourceJar javadocJar

cd -

if [ $(contains "${ARGS[@]}" "no_zip") == "n" ];
then
    echo "${GLITZ}ZIPPING UP${GLITZ}"
    cd ..
    sh gradlew createZips cleanZipFolders
    cd -
fi

if [ $(contains "${ARGS[@]}" "upload") == "y" ];
then
    echo "${GLITZ}UPLOADING ZIPS TO SERVER${GLITZ}"
    cd $STAGE
    SERVER_ADDRESS="162.209.102.219"
    sshpass -p $SWEETBLUE_COM_FTP_PASSWORD scp -p $JAR_NAME.zip "${SWEETBLUE_COM_FTP_USERNAME}@${SERVER_ADDRESS}:/var/www/html/sweetblue/downloads"
    sshpass -p $SWEETBLUE_COM_FTP_PASSWORD scp -p sweetblue.zip "${SWEETBLUE_COM_FTP_USERNAME}@${SERVER_ADDRESS}:/var/www/html/sweetblue/downloads"
    cd -
fi

if [ $(contains "${ARGS[@]}" "no_clean") == "n" ];
then
    echo "${GLITZ}CLEANING UP${GLITZ}"
    rm -rf $BUNDLE_FOLDER
    rm -rf $STAGE/samples
    rm -rf ../build
fi