@echo off
call refreshenv
mvn -B package --file pom.xml
