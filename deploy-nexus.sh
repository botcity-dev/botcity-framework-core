#!/bin/bash

<<COMMENT1
    @Author: Eli Gomes
    @Description: Script for deploying artifacts in NEXUS
    Enter one of the commands below to upload.
    1 - To upload no botcity-snapshots repository run ./deploy-nexus.sh snap 1.3.3
    2 - To upload no botcity-releases repository run ./deploy-nexus.sh rel 1.3.3
    3 - To upload no botcity-public repository run ./deploy-nexus.sh pub 1.3.3
    4 - To upload no botcity-public-dev repository run ./deploy-nexus.sh pub-dev 1.3.3
COMMENT1

ERROR="\033[0;31m"
INFO="\033[0;34m"
SUCCESS="\033[0;32m"

DISTRIBUTION=$1
echo $DISTRIBUTION

VERSION=$2
echo $VERSION

if [ $DISTRIBUTION == "snap" ] || [ $DISTRIBUTION == "pub-dev" ]; then
  VERSION=${VERSION}-SNAPSHOT
fi

echo -e "$INFO############  Changing the version in pom.xml to: $VERSION############  "
# Set version per tag in git
# mvn versions:set -DnewVersion=$VERSION

echo -e "$INFO############  Generating the build (compile and package). ############  "
mvn clean package

CURRENT_DIR=$(pwd)
DIR_JAR_FILE=$(ls $CURRENT_DIR/target/*jar)
JAR_BUILD_NAME=$(echo $DIR_JAR_FILE | rev | cut -f1 -d'/' | rev)
echo -e "$INFO############  Moving generated artifact to the dist directory. ############  "
mv target/$JAR_BUILD_NAME dist

JAR_NAME_AND_VERSION=$(echo $JAR_BUILD_NAME| sed -e "s/.jar//g")
CUT_NAME_FIRST_RESULT_NUMERIC=${JAR_NAME_AND_VERSION%%[0-9]*}
INDEX_VERSION=${#CUT_NAME_FIRST_RESULT_NUMERIC}
ARTIFACT_ID=${CUT_NAME_FIRST_RESULT_NUMERIC:0:INDEX_VERSION-1}

CURRENT_DIR=$(pwd)
cd "${CURRENT_DIR}/dist"

ARTIFACT_JAR=$(eval "ls");
echo $ARTIFACT_JAR

case $DISTRIBUTION in
  "snap")
  REPO_NEXUS=botcity-snapshots
  REPO_ID=nexus-botcity-snapshots
  ;;
  "rel")
  REPO_NEXUS=botcity-releases
  REPO_ID=nexus-botcity-releases
  ;;
  "pub")
  REPO_NEXUS=botcity-public
  REPO_ID=nexus-botcity-public
  ;;
  "pub-dev")
  REPO_NEXUS=botcity-public-dev
  REPO_ID=nexus-botcity-dev
  ;;
  *)
    echo -e "$ERROR****************************************************************"
    echo -e "$ERROR###### FAIL: $DISTRIBUTION repository does not exist :( "
    echo -e "$ERROR###### Available Repositories: rel, snap, pub and pub-dev "
    echo -e "$ERROR****************************************************************"
    ;;
esac

MVN_VERSION=(-Dversion=$VERSION)
MVN_URL_NEXUS=(-Durl=https://devtools.botcity.dev:8081/repository/$REPO_NEXUS/)
MVN_PACK=(-Dpackaging=jar)
MVN_REPO_ID=(-DrepositoryId=$REPO_ID)
MVN_DEPLOY=deploy:deploy-file
MVN_FILE=(-Dfile=)
MVN_GROUP_ID=(-DgroupId=dev.botcity)
MVN_ARTIFACT_ID=(-DartifactId=$ARTIFACT_ID)
MVN_POM_FILE=(-DpomFile=$CURRENT_DIR/pom.xml)

mvn $MVN_DEPLOY $MVN_URL_NEXUS $MVN_PACK $MVN_REPO_ID ${MVN_FILE}${ARTIFACT_JAR} $MVN_GROUP_ID $MVN_ARTIFACT_ID $MVN_VERSION $MVN_POM_FILE
CMD_MVN_DEPLOY_NEXUS=$?
if [ $CMD_MVN_DEPLOY_NEXUS -ne 0 ] ; then
  echo -e "$ERROR************************************************************************"
  echo -e "$ERROR###### FAIL: an error occurred when trying to deploy in nexus :( ######"
  echo -e "$ERROR************************************************************************"
  exit $CMD_MVN_DEPLOY_NEXUS
fi

echo
echo -e "$SUCCESS***********************************************************************************"
echo -e "$SUCCESS###### Upload of version $ARTIFACT_ID-$VERSION.jar to repository $REPO_NEXUS ######"
echo -e "$SUCCESS######                      SUCCESSFULLY COMPLETED!!!!                       ######"
echo -e "$SUCCESS***********************************************************************************"
echo

