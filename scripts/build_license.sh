#!/bin/sh


#PACKAGE=$1
#COMPANY=$2
#LEVEL=$1

#source ./config_license.sh

#mkdir $OUTDIR

#rsync -a ./sweetblue_license_standard_files $OUTDIR
#cp -R /* $OUT_DIR/

#SUPPORT=""
#LEVEL_PRETTY=""

#if [ "$LEVEL" = "standard" ]
#then
#    LEVEL_PRETTY="Standard"
#    SUPPORT="Level of support is identical to the open source GPLv3 version. Please see <a href='https:\/\/github.com\/iDevicesInc\/SweetBlue\/wiki\/FAQ#how-do-i-report-bugs-or-'request-features'>our FAQ<\/a> for more details."
#else
#    LEVEL_PRETTY="Professional"
#    SUPPORT="You have priority support for one year from date of purchase. This means guaranteed 2-business-day e-mail response, best-effort 1-week turnaround on bugs in the codebase, phone support if needed, and significant say in the development of new features."
#fi

#TEMP_FILE="temp.html"

#sed "s/{{support}}/$SUPPORT/g" ./assets/license_template.html > $TEMP_FILE
#sed "s/{{level}}/$LEVEL_PRETTY/g" $TEMP_FILE > $OUTDIR/$LEVEL.html

#rm $TEMP_FILE
LEVEL=$1
cd ..
if [ "$LEVEL" = "standard" ]
then
   sh gradlew createStandardLicense
else
   sh gradlew createProLicense
fi
cd -