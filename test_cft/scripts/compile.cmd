@echo off
rem Compile only (production code from ..\src + tests/tools) without running tests. Use to check the setup first.
setlocal
set HERE=%~dp0..
call mvn -f "%HERE%\pom.xml" clean test-compile %*
