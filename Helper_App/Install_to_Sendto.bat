REM This will copy Butterfly_Loader.bat and Butterfly_Loader_SPI_Flash.bat to your sendto directory. Then just right click on a bit file and choose Sendto.

REM Windows 7 and Vista
cd %0\..\bin
Shortcut.exe /F:%appdata%\microsoft\windows\sendto\Butterfly_Loader.bat.lnk /A:C /T:%0\..\Butterfly_Loader.bat
Shortcut.exe /F:%appdata%\microsoft\windows\sendto\Butterfly_Loader_SPI_Flash.bat.lnk /A:C /T:%0\..\Butterfly_Loader_SPI_Flash.bat

REM Windows XP
Shortcut.exe /F:"%appdata%\..\sendto\Butterfly_Loader.bat.lnk" /A:C /T:%0\..\Butterfly_Loader.bat
Shortcut.exe /F:"%appdata%\..\sendto\Butterfly_Loader_SPI_Flash.bat.lnk" /A:C /T:%0\..\Butterfly_Loader_SPI_Flash.bat

pause