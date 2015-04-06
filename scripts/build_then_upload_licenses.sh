#!/bin/sh

sh build_licenses.sh

source ./config_license.sh

read -n 1 -p "Are you sure you want to upload? " sure
if [ "$sure" = "y" -o "$sure" = "Y" ];
then
	SERVER_ADDRESS="162.209.102.219"
	sshpass -p $SWEETBLUE_COM_FTP_PASSWORD scp -rp $OUTDIR/* "${SWEETBLUE_COM_FTP_USERNAME}@${SERVER_ADDRESS}:/var/www/html/sweetblue/licenses"
else
	echo "\nLicenses built, but upload was aborted.\n"
fi
