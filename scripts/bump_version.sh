#!/bin/sh
current=$(sh echo_version.sh)
vers=(${current//_/ })
major=${vers[0]}
minor=${vers[1]}
nano=${vers[2]}

performBump () {
	sed -i '' 's/'${current}'/'${1}'/g' gradle.properties
	nver=(${1//_/.})
	sed -i '' 's/version-.*-blue/version-'${nver}'-blue/g' ../README.md
}

echo "Current SweetBlue version: ${current}"
echo

if [ -z "${1}" ];
then
	echo "Please format version like X_X_X (eg 1_26_10)"
	echo
	read -p "Enter new version: " newver
else
	newver=$1
fi

if [ -z "${newver}" ]
then
    echo "Please enter a valid version in the format X_XX_XX"
    exit 1
fi
new=(${newver//_/ })
nmj=${new[0]}
nmn=${new[1]}
nn=${new[2]}

if [ "$nmj" -gt "$major" ]
then
	read -n 1 -p "Are you sure you want to increase the major version? " inc
	if [ "$inc" == "y" -o "$inc" == "Y" ]
	then 
		performBump "${newver}"
		exit 0
	else
		echo "\nVersion bump aborted."
		exit 1
	fi
fi

if [ "$nmn" -gt "$minor" ]
then
	performBump "${newver}"
	exit 0
fi

if [ "$nn" -gt "$nano" ]
then
	performBump "${newver}"
	exit 0
else
	echo "New version was not higher than current version. Bump aborted."
fi