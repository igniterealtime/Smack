@echo off

rem //------------------------------------------------------------------------//
rem // $RCSfile$
rem // $Revision$
rem // $Date$
rem //
rem // Standard Jive Software ant.bat file. Do not change this file. If you do,
rem // you will have seven years of bad luck and bad builds.
rem //------------------------------------------------------------------------//

rem //------------------------------------------------------------------------//
rem // Uncomment the following if you wish to set JAVA_HOME in this bat file:
rem //------------------------------------------------------------------------//
rem SET JAVA_HOME=

rem //------------------------------------------------------------------------//
rem // Check for the JAVA_HOME environment variable
rem //------------------------------------------------------------------------//
if "%JAVA_HOME%" == "" goto noJavaHome

rem //------------------------------------------------------------------------//
rem // Make the correct classpath (should include the java jars and the
rem // Ant jars)
rem //------------------------------------------------------------------------//
SET CP="%JAVA_HOME%\lib\tools.jar;.\ant.jar;.\junit.jar"

rem //------------------------------------------------------------------------//
rem // Run Ant
rem // Note for Win 98/95 users: You need to change "%*" in the following
rem // line to be "%1 %2 %3 %4 %5 %6 %7 %8 %9"
rem //------------------------------------------------------------------------//
"%JAVA_HOME%\bin\java" -Xms32m -Xmx128m -classpath %CP% -Dant.home=. org.apache.tools.ant.Main %*
goto end

rem //------------------------------------------------------------------------//
rem // Error message for missing JAVA_HOME
rem //------------------------------------------------------------------------//
:noJavaHome
echo.
echo Jive Forums Build Error:
echo.
echo The JAVA_HOME environment variable is not set. JAVA_HOME should point to
echo your java directory, ie: c:\jdk1.3.1. You can set this via the command
echo line like so:
echo   SET JAVA_HOME=c:\jdk1.3
echo.
goto end

:end

