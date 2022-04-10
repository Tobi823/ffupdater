#!/bin/bash

(
  cd ..
  ./gradlew testDebug --fail-fast --tests="*IT"
)