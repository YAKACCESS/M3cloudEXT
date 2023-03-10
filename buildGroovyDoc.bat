@echo off
cls
call refreshenv
call mvn gplus:generateStubs gplus:groovydoc
target\gapidocs\index.html