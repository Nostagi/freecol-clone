@echo off
rem Build + run all *CfgTest + JaCoCo report (High-priority scope). Extra args are passed to Maven, e.g.:
rem   scripts\run.cmd -Dtest=CollectionUtilsCfgTest
rem Works from any directory. Output: test_cft\reports\surefire and test_cft\reports\jacoco\index.html
setlocal
set HERE=%~dp0..
call mvn -f "%HERE%\pom.xml" clean verify %*
