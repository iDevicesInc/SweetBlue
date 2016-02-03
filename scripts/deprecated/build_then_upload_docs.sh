#!/bin/sh

source config.sh

CUR_VER=$(sh echo_version.sh)
SWEETBLUE_DIR="script_output/sweetblue_${CUR_VER}"
SWEETBLUE_DOCS_DIR=$SWEETBLUE_DIR/docs/api/

echo $SWEETBLUE_DOCS_DIR
sh build_docs.sh
if [ "$?" != 0 ];
then
	echo "Build failed! Fix errors, then run this script again."
	exit 1
fi
read -n 1 -p "Are you SURE you want to upload? " sure
if [ "$sure" == "y" -o "$sure" == "Y" ];
then 
	SERVER_ADDRESS="162.209.102.219"
	echo "\n${GLITZ}UPLOADING DOCS TO ${SERVER_ADDRESS}...${GLITZ}"
	sshpass -p $SWEETBLUE_COM_FTP_PASSWORD scp -rp $SWEETBLUE_DOCS_DIR "${SWEETBLUE_COM_FTP_USERNAME}@${SERVER_ADDRESS}:/var/www/html/sweetblue/docs"
else
	echo "\nDocs were built, but the upload was aborted.\n"
fi

