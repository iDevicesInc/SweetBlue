#!/bin/sh

source config.sh
ver=$(sh echo_version.sh)
SWEETBLUE_DIR=sweetblue_${ver}
SWEETBLUE_DOCS_DIR=$SWEETBLUE_DIR/docs/api/
JAR_NAME=sweetblue_$ver

# Run lint checks before doing the full build
sh gradlew -b lint.gradle :lint

if [ "$?" != 0 ];
then
	echo "Lint errors! Please fix them before running this again."
	exit 1
fi

sh gradlew fullBuild
if [ "$?" != 0 ];
then
	echo "Build failed! Fix errors, then run this script again."
	exit 1
fi
cp $STAGE/sweetblue.zip $STAGE/$JAR_NAME.zip
read -n 1 -p "Are you sure you want to upload? " sure
if [ "$sure" = "y" -o "$sure" = "Y" ];
	then
		echo "\n${GLITZ} UPLOADING ZIPS TO SERVER ${GLITZ}"
	    cd $STAGE
	    SERVER_ADDRESS="162.209.102.219"	     
    	sshpass -p $SWEETBLUE_COM_FTP_PASSWORD scp -p $JAR_NAME.zip "${SWEETBLUE_COM_FTP_USERNAME}@${SERVER_ADDRESS}:/var/www/html/sweetblue/downloads"
	    sshpass -p $SWEETBLUE_COM_FTP_PASSWORD scp -p sweetblue.zip "${SWEETBLUE_COM_FTP_USERNAME}@${SERVER_ADDRESS}:/var/www/html/sweetblue/downloads"
	    echo "\n${GLITZ} UPLOADING DOCS TO SERVER ${GLITZ}"
		sshpass -p $SWEETBLUE_COM_FTP_PASSWORD scp -rp $SWEETBLUE_DOCS_DIR "${SWEETBLUE_COM_FTP_USERNAME}@${SERVER_ADDRESS}:/var/www/html/sweetblue/docs"
    	cd -
    	if [ "$?" != 0 ];
		then	
			ssh -o StrictHostKeyChecking=no sweetblue@162.209.102.219 uptime
			echo "Problem with scp. Attempted to setup host key, please run upload_release.sh to upload to server."
			exit 1
		fi
		cd -
    else
    	echo "\nRelease bundle built, but the upload was aborted.\n"
fi
