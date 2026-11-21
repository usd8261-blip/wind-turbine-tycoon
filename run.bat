@echo off
set "JAVA_BIN=C:\Program Files\Microsoft\jdk-21.0.13.102-hotspot\bin"
cd /d "%~dp0"
start "" "%JAVA_BIN%\javaw.exe" -cp . WindTycoon
