#!/bin/bash
BASEDIR=$(dirname "$0")
CONFIGURATION="$BASEDIR/configuration/application.yml"
classPath="$BASEDIR/userlib"

JAR_PATH="./lib/c7-data-migrator-distro-0.0.1-SNAPSHOT.jar"
COMMON_OPTS="-Dloader.path=$classPath -Dspring.config.location=file:$CONFIGURATION"

OPTIONS_HELP="Options:
  --runtime     - Migrate runtime data only
  --history     - Migrate history data only
"

if [[ $# -gt 1 ]]; then
  echo "Error: Only one flag allowed."
  printf "Usage: run.sh [--runtime|--history] \n%s" "$OPTIONS_HELP"
  exit 1
fi

if [[ $# -eq 1 ]]; then
  case "$1" in
    --runtime|--history)
      echo "Starting migration with flag: $1"
      java $COMMON_OPTS -jar "$JAR_PATH" "$1"
      ;;
    *)
      echo "Invalid flag: $1"
      printf "Usage: run.sh [--runtime|--history] \n%s" "$OPTIONS_HELP"
      exit 1
      ;;
  esac
else
  echo "Starting application without migration flag."
  java $COMMON_OPTS -jar "$JAR_PATH"
fi
