@ECHO OFF

ECHO Generating a JRE. Please input the requested values:

set /p jdkPath="Enter JDK \bin path: "
set /p exportPath="Enter an export path: "

ECHO Creating a basic JRE with JLink, please wait...

REM Mounts the JDK path and calls jlink using the inputted variables. The commands after --output are just optimization commands that'll help reduce filesize.
cd %jdkPath%
jlink --add-modules java.se --output %exportPath% --strip-debug --compress 2 --no-header-files --no-man-pages

ECHO Finished.

pause