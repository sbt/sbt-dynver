#!/usr/bin/env bash

if [[
  "$TRAVIS_PULL_REQUEST" == "false" -a
  "$TRAVIS_BRANCH" == "master" -a
  "$TRAVIS_SECURE_ENV_VARS" == "true"
]]; then
  PUBLISH=publish
  mkdir ~/.bintray
  cat > ~/.bintray/.credentials <<EOF
realm = Bintray API Realm
host = api.bintray.com
user = dwijnand
password = $BINTRAY_API_KEY
EOF
  # sbt ++$TRAVIS_SCALA_VERSION publish
  echo "WAS ABOUT TO PUBLISH"
else
  echo "Skipping publish"
fi
