#!/bin/bash

mvn clean install

mvn dependency:copy-dependencies 

mkdir -p target/dist/lib

BUILD_VERSION=$(date +"%Y%m%d_%H%M%S")
GIT_BRANCH=$(git name-rev --name-only HEAD | sed s'/[/-]//g')
echo "BUILD_VERSION=$BUILD_VERSION"
echo "GIT_BRANCH=$GIT_BRANCH"

VERSION="$BUILD_VERSION-$GIT_BRANCH"
echo "VERSION=$VERSION"

cp -r target/dependency/ target/dist/lib/

cp -r target/s3_backup_2.12-1.0-SNAPSHOT.jar target/dist/lib/s3_backup_2.12-1.0-$VERSION.jar
