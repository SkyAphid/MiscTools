@ECHO OFF

ECHO Beginning jdeps scan. Please enter the requested values:

set /p jdkPath="Enter JDK \bin path: "
set /p jarPath="Enter the path for the JAR you wish to scan with jdeps: "

cd %jdkPath%
jdeps --generate-module-info %jarPath%

pause