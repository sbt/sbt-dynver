#!/usr/bin/env bash

case "$TRAVIS_SBT_VERSION" in
  0.13.x) SWITCH_SBT_VERSION="";        ;;
     1.x) SWITCH_SBT_VERSION="^^1.0.0"; ;;
       *) echo >&2 "Aborting: Unknown TRAVIS_SBT_VERSION: $TRAVIS_SBT_VERSION"; exit 1; ;;
esac

[[ "$TRAVIS_PULL_REQUEST" == "false"
&& "$TRAVIS_BRANCH" == "master"
&& "$TRAVIS_SECURE_ENV_VARS" == "true"
]]
on_the_master_branch=$?

[[ "$TRAVIS_TAG" != ""
&& "$TRAVIS_SECURE_ENV_VARS" == "true"
]]
on_a_tag=$?

if [[ $on_the_master_branch == 0 || $on_a_tag == 0 ]]; then
  PUBLISH=publish
else
  PUBLISH=publishLocal
fi

sbt "$SWITCH_SBT_VERSION" test scripted mimaReportBinaryIssues "$PUBLISH"
