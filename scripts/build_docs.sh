cd ../src

#CSS_PATH=$ANDROID_HOME/extras/google/google_play_services/docs/assets/android-developer-docs.css
#CSS_PATH=$ANDROID_HOME/docs/assets/android-developer-docs.css
CSS_PATH=../assets/doc_style.css


javadoc -protected -stylesheetfile $CSS_PATH -windowtitle BtFresh -author -d ../doc/api com.idevicesllc.btfresh

cd -