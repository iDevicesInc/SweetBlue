#!/bin/sh

if [ "$1" = "" ];
then
    echo
    echo "At least one parameter required. Enter 0 to leave parameter unchanged"
    echo
    echo "  #  | Parameter Description"
    echo "-----+-----------------------------"
    echo "  1  | Build Tools Version"
    echo "  2  | Gradle Plugin Version"
    echo "  3  | Gradle Version"
    echo "  4  | Compile/Target SDK Version"
    echo
    echo "EXAMPLE: bump_versions.sh '25.0.3' 0 '3.3'"
    echo
    exit
else
    v1=$(echo $1 | sed -e "s/\'|\"|\s//g")
    v2=$(echo $2 | sed -e "s/\'|\"|\s//g")
    v3=$(echo $3 | sed -e "s/\'|\"|\s//g")
    v4=$(echo $4 | sed -e "s/\'|\"|\s//g")

    p="$(pwd)"
    if [ "${p:(-7)}" = "scripts" ];
    then
       cd ..
    fi

    message="Bump:"
    if [[ ( "$v1" != "" ) && ( "$v1" != "0" ) ]];
    then
        ./gradlew bumpBuildToolsVersion -PbuildToolsVersion=${v1}
        message="$message Build Tools Version to $v1,"

    fi
    if [[ ( "$v2" != "" ) && ( "$v2" != "0" ) ]];
    then
        ./gradlew bumpGradlePluginVersion -PgradlePluginVersion=${v2}
        message="$message Gradle Plugin Version to $v2,"
    fi
    if [[ ( "$v3" != "" ) && ( "$v3" != "0" ) ]];
    then
        ./gradlew bumpGradleVersion -PgradleVersion=${v3}
        message="$message Gradle Version to $v3,"
    fi
    if [[ ( "$v4" != "" ) && ( "$v4" != "0" ) ]];
    then
        ./gradlew bumpCompileSdkVersion -PcompileSdkVersion=${v4}
        message="$message Compile/Target Sdk Version to $v4,"
    fi

    # Push to SweetBlue repo
    git add -u
    git commit -m "${message%?}"
    git push origin HEAD
    # Push to samples repo
    cd library/script_output/samples/
    git add -u
    git commit -m "${message%?}"
    git push origin HEAD
fi