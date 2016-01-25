./gradlew -b lint.gradle :lint
if [ "$?" != 0 ];
then
    cat /home/travis/build/iDevicesInc/SweetBlue/build/outputs/lint-results.xml
    exit 1
fi