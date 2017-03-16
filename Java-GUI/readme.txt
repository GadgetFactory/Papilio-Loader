Papilio Loader is an open-source GUI shell for programming 
Papilio FPGA boards.

For more information, see the website at: http://www.papilio.cc/
To report a bug or a make a suggestions forums 
at: http://www.gadgetfactory.net/gadgetforum/index.php?board=21.0

INSTALLATION
Detailed instructions are available on the following page:
http://papilio.cc/index.php?n=Papilio.PapilioLoaderV2

You will need to install the drivers for the FTDI chip on the board.
These can be found at FTDI website: http://www.ftdichip.com/Drivers/D2XX.htm

CONTACT
Email: support@gadgetfactory.net
Web: http://www.papilio.cc
     http://www.gadgetfactory.net
Facebook: http://www.facebook.com/PapilioPlatform
Twitter: http://www.twitter.com/gadgetfactory

CHANGELOG
2/24/2015 Version 2.7
	Papilio GUI
		-Add Board Name field so you can manually specify the board name.
		-Added Papilio Pro, Papilio One, and Papilio DUO to Target Board dropdown.
		-Removed .bmm and .hex fields from simple mode.
		-Updated Icon
		-Drivers are signed for easy install in Windows 8
		
	papilio-prog
		-Support for the Papilio DUO VID/PID

1/2/2014 Version 2.6
	Papilio GUI
		-Changed the default behavior to write to SPI flash.
		-Fixed problem with not doing an erase, verify, program, verify cycle.
		-Removed old board types.

9/13/2013 Version 2.5
	Fixed error with Java-GUI not detecting line endings in JTextBox. Everything compiles and works on Windows and Linux now.
	Added Linux installer script.

1/4/2012  Version 2.4
	Fixes from MagnusK
		* New cmd-line option (-d) that allows you to specify the FTDI device.  This was asked for in this forum.
		* Spartan-6 LX4 - LX45 parts are added to the built-in device list
		* The fpga wait-code is put back
		* The code now supports many more flash parts
		* The number of pages in the Macronix part is corrected (32768, not 250000)
		* Misc corrections here and there in the code
		* The erase-before-programming is now handled by a new routine (Spi_PartialErase) that tries to be smarter about the erase. 	

12/3/2012 Version 2.3
	Added support for larger bit files with Macronix chips.

8/31/2012 Version 2.2
	Added support for Macronix 64Mb SPI Flash chips.
	Added ability to Erase Flash chip from Explorer context menu.

6/29/2012 Version 2.1
	Java Gui added.