@echo off
rem Run unit tests with JaCoCo agent restricted to net.sf.freecol.common.**
rem Supports running all tests or a subset derived from a unit CSV file.
rem
rem Usage:
rem   scripts\jacoco.cmd
rem   scripts\jacoco.cmd test_cft\config\suong_high_priority_files.csv
rem   scripts\jacoco.cmd -csv config\suong_high_priority_files.csv
rem   scripts\jacoco.cmd -Dtest=CompoundMissionCfgTest
setlocal EnableDelayedExpansion
for %%I in ("%~dp0..") do set "HERE=%%~fI"
for %%I in ("%HERE%\..") do set "REPO_ROOT=%%~fI"

if "%~1"=="-h" goto :usage
if "%~1"=="--help" goto :usage
if "%~1"=="/?" goto :usage

set "SUREFIRE_JVM_ARG="
set "JAVA_EXE=java"
for /f "delims=" %%J in ('where java 2^>nul') do (
    if not defined SUREFIRE_JVM_ARG (
        set "CAND=%%J"
        if "!CAND:(=!"=="!CAND!" (
            if /i "!CAND:\bin\java.exe=!" neq "!CAND!" (
                set "SUREFIRE_JVM_ARG=-Dsurefire.jvm=%%J"
                set "JAVA_EXE=%%J"
            )
        )
    )
)
if not defined SUREFIRE_JVM_ARG (
    for /d %%D in ("D:\SoftWares\IntelliJ IDEA*") do (
        if not defined SUREFIRE_JVM_ARG if exist "%%~D\jbr\bin\java.exe" (
            set "SUREFIRE_JVM_ARG=-Dsurefire.jvm=%%~D\jbr\bin\java.exe"
            set "JAVA_EXE=%%~D\jbr\bin\java.exe"
        )
    )
)

set "HAS_CSV="
set "SUBSET_NAME="
set "TEST_LIST="
set "EXTRA_ARGS="
set "RAW_ARGS=%*"
if defined RAW_ARGS (
    for %%A in (%*) do (
        if /i "%%~A"=="-csv" set "HAS_CSV=1"
        if /i "%%~A"=="--csv" set "HAS_CSV=1"
        if /i "%%~A"=="-Dcsv" set "HAS_CSV=1"
        if /i "%%~xA"==".csv" set "HAS_CSV=1"
    )
)

if defined HAS_CSV (
    set "ENV_FILE=%TEMP%\jacoco_subset_%RANDOM%_%RANDOM%.env"
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0resolve_subset_tests.ps1" -Here "%HERE%" -RepoRoot "%REPO_ROOT%" -OutFile "!ENV_FILE!" %*
    set "EXIT_CODE=!ERRORLEVEL!"
    if !EXIT_CODE! neq 0 (
        if exist "!ENV_FILE!" del "!ENV_FILE!" >nul 2>&1
        set "FINAL_CODE=!EXIT_CODE!"
        goto :fail
    )
    for /f "usebackq tokens=1* delims==" %%K in ("!ENV_FILE!") do (
        if "%%K"=="SUBSET_NAME" set "SUBSET_NAME=%%L"
        if "%%K"=="TEST_LIST" set "TEST_LIST=%%L"
        if "%%K"=="EXTRA_ARGS" set "EXTRA_ARGS=%%L"
    )
    if exist "!ENV_FILE!" del "!ENV_FILE!" >nul 2>&1
    if not defined TEST_LIST (
        echo [JaCoCo] ERROR: Failed to resolve subset test list.
        set "FINAL_CODE=2"
        goto :fail
    )
)

set "REPORT_DIR=%HERE%\reports\jacoco"
if exist "%REPORT_DIR%" rmdir /s /q "%REPORT_DIR%"

if defined HAS_CSV (
    if defined SUREFIRE_JVM_ARG (
        call mvn -f "%HERE%\pom.xml" clean verify -Dmaven.test.failure.ignore=true "%SUREFIRE_JVM_ARG%" "-Dtest=%TEST_LIST%" %EXTRA_ARGS%
    ) else (
        call mvn -f "%HERE%\pom.xml" clean verify -Dmaven.test.failure.ignore=true "-Dtest=%TEST_LIST%" %EXTRA_ARGS%
    )
) else (
    if defined SUREFIRE_JVM_ARG (
        call mvn -f "%HERE%\pom.xml" clean verify -Dmaven.test.failure.ignore=true "%SUREFIRE_JVM_ARG%" %*
    ) else (
        call mvn -f "%HERE%\pom.xml" clean verify -Dmaven.test.failure.ignore=true %*
    )
)
set "EXIT_CODE=%ERRORLEVEL%"
if %EXIT_CODE% neq 0 exit /b %EXIT_CODE%

if defined SUBSET_NAME (
    "%JAVA_EXE%" -cp "%HERE%\target\test-classes" tools.JacocoCommonReport "%REPORT_DIR%" "%SUBSET_NAME%"
) else (
    "%JAVA_EXE%" -cp "%HERE%\target\test-classes" tools.JacocoCommonReport "%REPORT_DIR%"
)
set "EXIT_CODE=%ERRORLEVEL%"
if %EXIT_CODE% neq 0 exit /b %EXIT_CODE%

if defined SUBSET_NAME (
    set "SUBSET_DIR=%HERE%\reports\subsets\%SUBSET_NAME%"
    if exist "!SUBSET_DIR!" rmdir /s /q "!SUBSET_DIR!"
    mkdir "!SUBSET_DIR!" >nul 2>&1
    xcopy "%REPORT_DIR%\*" "!SUBSET_DIR!\" /E /I /Q /Y >nul
)

echo.
echo [JaCoCo] Common HTML report: "%REPORT_DIR%\index.html"
echo [JaCoCo] Common raw CSV:     "%REPORT_DIR%\jacoco.csv"
echo [JaCoCo] Common summary CSV: "%REPORT_DIR%\common_summary.csv"
if defined SUBSET_NAME (
    echo [JaCoCo] Subset snapshot:    "%HERE%\reports\subsets\%SUBSET_NAME%\index.html"
)
exit /b 0

:usage
echo Usage:
echo   scripts\jacoco.cmd
echo   scripts\jacoco.cmd ^<units.csv^> [extra maven args]
echo   scripts\jacoco.cmd -csv ^<units.csv^> [extra maven args]
echo   scripts\jacoco.cmd -Dtest=^<TestClass^> [extra maven args]
echo.
echo Examples:
echo   scripts\jacoco.cmd config\suong_high_priority_files.csv
echo   scripts\jacoco.cmd test_cft\config\suong_high_priority_files.csv
exit /b 0

:fail
endlocal & exit /b %FINAL_CODE%
