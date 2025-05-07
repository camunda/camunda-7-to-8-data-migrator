@echo off
setlocal

REM Get the directory of the script
set "BASEDIR=%~dp0"
set "CONFIGURATION=%BASEDIR%configuration\application.yml"

set OPTIONS_HELP=Options:
set OPTIONS_HELP=%OPTIONS_HELP%^^^
  --runtime     - Migrate runtime data only^^^
  --history     - Migrate history data only

REM Check number of arguments
if not "%2"=="" (
  echo Error: Only one flag allowed.
  echo Usage: %~nx0 [--runtime|--history]
  exit /b 1
)

if not "%1"=="" (
  if "%1"=="--runtime" (
    echo Starting migration with flag: %1
    java -jar .\lib\c7-data-migrator-distro-0.0.1-SNAPSHOT.jar %1 --spring.config.location=file:"%CONFIGURATION%"
  ) else if "%1"=="--history" (
    echo Starting migration with flag: %1
    java -jar .\lib\c7-data-migrator-distro-0.0.1-SNAPSHOT.jar %1 --spring.config.location=file:"%CONFIGURATION%"
  ) else (
    echo Invalid flag: %1
    echo Usage: run.bat [--runtime|--history]
    echo %OPTIONS_HELP%
    exit /b 1
  )
) else (
  echo Starting application without migration flag.
  java -jar .\lib\c7-data-migrator-distro-0.0.1-SNAPSHOT.jar --spring.config.location=file:"%CONFIGURATION%"
)

endlocal