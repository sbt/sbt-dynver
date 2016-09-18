#!/usr/bin/env bash

if [[ "$TRAVIS_PULL_REQUEST" == "false" ]]; then
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

sbt ++$TRAVIS_SCALA_VERSION test "$PUBLISH"
