#!/usr/bin/env bash

if [[ "$TRAVIS_SBT_VERSION" == "0.13.x" ]]; then
  SWITCH_SBT_VERSION=""
else
  SWITCH_SBT_VERSION="^^$TRAVIS_SBT_VERSION"
fi

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
  mkdir ~/.bintray
  cat > ~/.bintray/.credentials <<EOF
realm = Bintray API Realm
host = api.bintray.com
user = dwijnand
password = $BINTRAY_API_KEY
EOF
else
  PUBLISH=publishLocal
fi

sbt "$SWITCH_SBT_VERSION" verify "$PUBLISH"
