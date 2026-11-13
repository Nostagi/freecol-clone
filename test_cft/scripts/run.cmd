@echo off
rem Build + run all *CfgTest + JaCoCo report + src folder-tree report. Extra args are passed to Maven, e.g.:
rem   scripts\run.cmd -Dtest=CompoundMissionCfgTest
rem Works from any directory. Output: test_cft\reports\surefire, test_cft\reports\jacoco, test_cft\reports\tree
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
exit /b %ERRORLEVEL%
