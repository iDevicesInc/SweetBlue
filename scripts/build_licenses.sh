#!/bin/sh

#source ./config_license.sh

#rm -rf $OUTDIR

#sh build_license.sh standard
#sh build_license.sh professional
cd ..
sh gradlew createStandardLicense createProLicense
cd -
