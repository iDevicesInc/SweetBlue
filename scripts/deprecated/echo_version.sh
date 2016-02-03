#!/bin/sh
grep SEMVER gradle.properties | sed 's/SEMVER=//g'
