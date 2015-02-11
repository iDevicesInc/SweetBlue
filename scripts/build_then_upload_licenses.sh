#!/bin/sh

sh build_licenses.sh

source ./config_license.sh

SERVER_ADDRESS="162.209.102.219"
sshpass -p $SWEETBLUE_COM_FTP_PASSWORD scp -rp $OUTDIR "${SWEETBLUE_COM_FTP_USERNAME}@${SERVER_ADDRESS}:/var/www/html/sweetblue"
