@echo off
rem Run unit tests with JaCoCo agent and generate:
rem   1) Standard JaCoCo report at test_cft\reports\jacoco\index.html
rem   2) Folder-tree report mirroring src\ at test_cft\reports\tree\index.html
rem Usage:
rem   scripts\jacoco.cmd
rem   scripts\jacoco.cmd -Dtest=CompoundMissionCfgTest
setlocal EnableDelayedExpansion
for %%I in ("%~dp0..") do set "HERE=%%~fI"
for %%I in ("%HERE%\..") do set "REPO_ROOT=%%~fI"

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

if defined SUREFIRE_JVM_ARG (
    call mvn -f "%HERE%\pom.xml" clean verify "%SUREFIRE_JVM_ARG%" %*
) else (
    call mvn -f "%HERE%\pom.xml" clean verify %*
)
set "EXIT_CODE=%ERRORLEVEL%"
if %EXIT_CODE% neq 0 exit /b %EXIT_CODE%

"%JAVA_EXE%" -cp "%HERE%\target\test-classes" tools.JacocoTreeReport "%REPO_ROOT%\src" "%HERE%\reports\jacoco" "%HERE%\reports\tree"
set "EXIT_CODE=%ERRORLEVEL%"
if %EXIT_CODE% equ 0 (
    echo.
    echo [JaCoCo] Folder-tree report: "%HERE%\reports\tree\index.html"
    echo [JaCoCo] Tree CSV:           "%HERE%\reports\tree\coverage_tree.csv"
    echo [JaCoCo] Raw JaCoCo report:  "%HERE%\reports\jacoco\index.html"
)
exit /b %EXIT_CODE%
