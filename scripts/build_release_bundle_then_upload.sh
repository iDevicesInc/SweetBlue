#!/bin/sh

source config.sh

JAR_NAME=sweetblue_$SEMVER

sh gradlew fullBuild
read -n 1 -p "Are you sure you want to upload? " sure
if [ "$sure" = "y" -o "$sure" = "Y" ];
	then
		echo "\n${GLITZ}UPLOADING ZIPS TO SERVER${GLITZ}"
	    cd $STAGE
	    SERVER_ADDRESS="162.209.102.219"
    	sshpass -p $SWEETBLUE_COM_FTP_PASSWORD scp -p $JAR_NAME.zip "${SWEETBLUE_COM_FTP_USERNAME}@${SERVER_ADDRESS}:/var/www/html/sweetblue/downloads"
	    sshpass -p $SWEETBLUE_COM_FTP_PASSWORD scp -p sweetblue.zip "${SWEETBLUE_COM_FTP_USERNAME}@${SERVER_ADDRESS}:/var/www/html/sweetblue/downloads"
    	cd -
    else
    	echo "\nRelease bundle built, but the upload was aborted.\n"
fi
