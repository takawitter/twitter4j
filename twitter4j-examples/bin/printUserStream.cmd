echo off
SETLOCAL enabledelayedexpansion
call setEnv.cmd

echo %JAVA% %MEM_ARGS% -classpath "%CLASSPATH%" twitter4j.examples.stream.PrintUserStream %*
"%JAVA%" %MEM_ARGS% -classpath "%CLASSPATH%" twitter4j.examples.stream.PrintUserStream %*

ENDLOCAL