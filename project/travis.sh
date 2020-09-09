#!/usr/bin/env bash

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

sbt test scripted mimaReportBinaryIssues "$PUBLISH"
