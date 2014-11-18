CSS_FILE=doc_style.css
CSS_SOURCE_PATH=./assets/$CSS_FILE
TARGET_PATH=../doc/api/

cd ../src

javadoc -stylesheetfile ../scripts/$CSS_SOURCE_PATH -protected -windowtitle SweetBlue -author -d $TARGET_PATH com.idevicesinc.sweetblue

cd -