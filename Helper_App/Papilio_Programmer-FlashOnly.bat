@echo off
set CYGWIN=nodosfilewarning

cd %0\..\bin
bash.exe %0\..\Papilio_Programmer-FlashOnly.sh %1

REM uncomment the pause below to troubleshoot.
REM pause