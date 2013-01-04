Papilio Loader - Current Version 2.4

The Papilio Loader is a branch of xc3sprog (http://sourceforge.net/projects/xc3sprog/) that is used to load bit files to the Open Source Papilio FPGA boards (http://papilio.cc) made by Gadget Factory (http://www.gadgetfactory.net).

Papilio Loader Homepage:
http://papilio.cc/index.php?n=Papilio.PapilioLoaderV2

Directories:
	Fpga - Contains bit files that allow SPI Flash to be programmed.
	Helper-App - A collection of scripts to ease loading bit files.
	Installer - InstallJammer project.
	Java-GUI - A Java GUI that wraps the C++ application. (Recommended)
	Program - The C++ application that can be compiled under Linux. View the Readme for instructions on cross compiling for Windows.
	

CHANGELOG
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