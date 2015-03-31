#!/bin/sh

#sh build_release_bundle.sh upload

ext.JAR_BASE_NAME="sweetblue_${SEMVER}"
ext.BUNDLE_FOLDER=STAGE + "/" + JAR_BASE_NAME
ext.JAR_NAME=BUNDLE_FOLDER + "/${JAR_BASE_NAME}.jar"

sh gradlew fullBuild
echo "${GLITZ}UPLOADING ZIPS TO SERVER${GLITZ}"
    cd $STAGE
    SERVER_ADDRESS="162.209.102.219"
    sshpass -p $SWEETBLUE_COM_FTP_PASSWORD scp -p $JAR_NAME.zip "${SWEETBLUE_COM_FTP_USERNAME}@${SERVER_ADDRESS}:/var/www/html/sweetblue/downloads"
    sshpass -p $SWEETBLUE_COM_FTP_PASSWORD scp -p sweetblue.zip "${SWEETBLUE_COM_FTP_USERNAME}@${SERVER_ADDRESS}:/var/www/html/sweetblue/downloads"
    cd -