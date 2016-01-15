#!/bin/bash

source ./config_versions.sh

SWEETBLUE_SRC_PATH=../src
STAGE=script_output
JAR_NAME=sweetblue_$SEMVER
JAVADOC_JAR_NAME="sweetblue_${SEMVER}-javadoc"
SOURCES_JAR_NAME="sweetblue_${SEMVER}-sources"
BUNDLE_FOLDER=$STAGE/$JAR_NAME