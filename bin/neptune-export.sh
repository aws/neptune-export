#!/bin/bash -e

jar=$(find . -name neptune-export-*-all.jar -print -quit)
java -jar ${jar} "$@"
