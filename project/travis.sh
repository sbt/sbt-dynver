#!/usr/bin/env bash

[[ "$TRAVIS_PULL_REQUEST" == "false" ]] && PUBLISH=publish || PUBLISH=publishLocal

sbt ++$TRAVIS_SCALA_VERSION test "$PUBLISH"
