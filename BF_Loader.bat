REM In order to load bit files please make sure that the Xilinx Impact tool is in the path.

set CMDFILE=ButterFly_Loader.cmd
set IMPACTCMDFILE=Impactout.cmd
set SVFFile=%1
set TMPFile=SVFTemp.svf

if %~x1 == .bit goto :BittoSvf 

:SVFProg
cd /D %~dp0bin
copy %SVFFile% %TMPFile%
del %CMDFILE%
echo cable ft2232 > %CMDFILE%
echo bsdl path . >> %CMDFILE%
echo detect >> %CMDFILE%
echo svf %TMPFile% progress >> %CMDFILE%
jtag_0_9_1415.exe %CMDFILE%
pause
del %CMDFILE%
del %IMPACTCMDFILE%
del %TMPFile%
goto :eof

:BitToSvf
echo in bittosvf
cd /D %~dp0bin
set SVFFile=..\%~n1.svf
del %IMPACTCMDFILE%
echo setmode -bscan > %IMPACTCMDFILE%
echo setcable -p svf -file %SVFFile% >> %IMPACTCMDFILE%
echo addDevice -p 1 -file %1 >> %IMPACTCMDFILE%
echo program -e -p 1 >> %IMPACTCMDFILE%
echo quit >> %IMPACTCMDFILE%
impact -batch %IMPACTCMDFILE%

goto :SVFPROG