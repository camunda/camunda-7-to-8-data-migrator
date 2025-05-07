#!/bin/bash
BASEDIR=$(dirname "$0")
CONFIGURATION=$BASEDIR/configuration/application.yml

OPTIONS_HELP="Options:
  --runtime     - Migrate runtime data only
  --history     - Migrate history data only
"

if [[ $# -gt 1 ]]; then
  echo "Error: Only one flag allowed."
  echo "Usage: $0 [--runtime|--history]"
  exit 1
fi

if [[ $# -eq 1 ]]; then
  case "$1" in
    --runtime|--history)
      echo "Starting migration with flag: $1"
      java -jar ./lib/c7-data-migrator-distro-0.0.1-SNAPSHOT.jar "$1"  --spring.config.location=file:"$CONFIGURATION"
      ;;
    *)
      echo "Invalid flag: $1"
      printf "Usage: run.sh [start|stop] (options...) \n%s" "$OPTIONS_HELP"
      exit 1
      ;;
  esac
else
  echo "Starting application without migration flag."
  java -jar ./lib/c7-data-migrator-distro-0.0.1-SNAPSHOT.jar  --spring.config.location=file:"$CONFIGURATION"
fi
