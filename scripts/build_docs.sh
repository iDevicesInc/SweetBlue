#!/bin/sh

#CSS_FILE=doc_style.css
#CSS_SOURCE_PATH=./assets/$CSS_FILE
#TARGET_PATH=../docs/api/

#source config.sh

#echo "${GLITZ}BUILDING DOCS${GLITZ}"

#cd ../src

#javadoc -stylesheetfile ../scripts/$CSS_SOURCE_PATH -protected -windowtitle SweetBlue #-author -d $TARGET_PATH -subpackages com.idevicesinc.sweetblue
#cd -
cd ..
sh ./gradlew javadocJar
cd -