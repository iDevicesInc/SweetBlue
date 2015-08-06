#!/bin/sh

SWEETBLUE_DIR=../
SWEETBLUE_DOCS_DIR=$SWEETBLUE_DIR/docs/api/

source config.sh

sh build_docs.sh

read -n 1 -p "Are you SURE you want to upload? " sure
if [ "$sure" == "y" -o "$sure" == "Y" ];
then 
	SERVER_ADDRESS="162.209.102.219"
	echo "\n${GLITZ}UPLOADING DOCS TO ${SERVER_ADDRESS}...${GLITZ}"
	sshpass -p $SWEETBLUE_COM_FTP_PASSWORD scp -rp $SWEETBLUE_DOCS_DIR "${SWEETBLUE_COM_FTP_USERNAME}@${SERVER_ADDRESS}:/var/www/html/sweetblue/docs"
else
	echo "\nDocs were built, but the upload was aborted.\n"
fi

