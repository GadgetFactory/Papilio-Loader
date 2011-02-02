@echo off
set CYGWIN=nodosfilewarning

cd %0\..\bin
bash.exe %0\..\Papilio_Loader.sh %1

pause