@ECHO OFF

REM Mount the desired JDK bin folder to use its jlink command
cd C:\Program Files\Java\jdk-11.0.3\bin\

REM This runs a jlink command that'll create a JRE with the given modules
jlink --add-modules java.se --output C:\Users\Brayden\Desktop\jre

ECHO JLINK successful.

pause