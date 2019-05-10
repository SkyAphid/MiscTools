@ECHO OFF

ECHO Beginning JLink. Please input the requested values:

set /p jdkPath="Enter JDK \bin path: "
set /p modulePath="Enter executable/module JAR paths (comma separated): "
set /p modules="Enter modules to add (comma separated, use 'java.se' to include all): "
set /p main="Enter main class name to evoke from launcher: "
set /p exportPath="Enter an export path: "

ECHO Creating custom JRE with JLink, please wait...

REM Mounts the JDK path and calls jlink using the inputted variables. The commands after --output are just optimization commands that'll help reduce filesize.
cd %jdkPath%
jlink --module-path %modulePath% --add-modules %modules% --launcher launcher=%main% --output %exportPath% --strip-debug --compress 2 --no-header-files --no-man-pages

ECHO Finished.

pause