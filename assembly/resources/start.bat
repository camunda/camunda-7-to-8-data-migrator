@echo off
setlocal

REM Get the directory of the script
set "BASEDIR=%~dp0"
set "CONFIGURATION=%BASEDIR%configuration\application.yml"
set "CLASSPATH=%BASEDIR%userlib"

set "JAR_PATH=.\internal\c7-data-migrator.jar"
set "COMMON_OPTS=-Dloader.path=%CLASSPATH% -Dspring.config.location=file:%CONFIGURATION%"

set OPTIONS_HELP=Options:
set OPTIONS_HELP=%OPTIONS_HELP%^^^
  --runtime     - Migrate runtime data only^^^
  --history     - Migrate history data only

REM Check number of arguments
if not "%2"=="" (
  echo Error: Only one flag allowed.
  echo Usage: %~nx0 [--runtime|--history]
  echo %OPTIONS_HELP%
  exit /b 1
)

if not "%1"=="" (
  if "%1"=="--runtime" (
    echo Starting migration with flag: %1
    java %COMMON_OPTS% -jar "%JAR_PATH%" %1
  ) else if "%1"=="--history" (
    echo Starting migration with flag: %1
    java %COMMON_OPTS% -jar "%JAR_PATH%" %1
  ) else (
    echo Invalid flag: %1
    echo Usage: %~nx0 [--runtime|--history]
    echo %OPTIONS_HELP%
    exit /b 1
  )
) else (
  echo Starting application without migration flag.
  java %COMMON_OPTS% -jar "%JAR_PATH%"
)

endlocal
