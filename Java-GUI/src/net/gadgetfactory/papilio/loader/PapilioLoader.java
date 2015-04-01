/*
  Part of the Papilio Loader

  Copyright (c) 2010-11 GadgetFactory LLC

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package net.gadgetfactory.papilio.loader;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.gadgetfactory.papilio.loader.LoaderProject.PPJProject;

public class PapilioLoader extends JFrame implements ActionListener
{
	private static final String LOADER_NAME = "Papilio Loader 2.8";
	public static final String AUTO_DETECT_FPGA = "Auto-detect onboard FPGA device";
	public static final boolean DEBUG = false;
	public static final boolean ECHO_COMMAND = false;
	public static final String SETTINGS_FOLDER = "papilio-loader";
	public static final String PREFERENCES_FILE = "preferences.txt";
	private static final String ERASE_SWITCH = "-e";
	private static final String WRITE_SWITCH = "-q";
	private static final String QUIT_SWITCH = "-x";

	public int nErrorCount = 0;
	
	public static final boolean runningonWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
	private static final File AppPath = new File(System.getProperty("java.class.path")).getParentFile();
	// GIRISH: What should default operation(s) be for each mode?
	private static final LastOperations DEFAULT_SIMPLE_OPERATION = LastOperations.WRITE_TO_SPI_FLASH;
	private static final LastOperations DEFAULT_EXPERT_OPERATION = LastOperations.SCAN;
	
	public enum UserModes {Simple, Expert};
	public enum ShellDefaultActions {WriteToFPGAandQuit, WriteToSPIFlashAndQuit, OpenBitFileAndWait};
	private enum PopulateStatus {TextboxesAndCombobox, Combobox, None};
	
	private TargetPanel pnlTarget;
	private OperationPanel pnlOperations;
	private OutputPanel pnlOutput;
	
	private FileNameExtensionFilter bitFileFilter = new FileNameExtensionFilter("Bit files", "bit");
	private JButton btnProceed;
	private JTextArea txtOutput;

	private PreferencesDialog dlgPreferences;
	private LoaderProject currProject;
	
	private File rootProgrammerPath; 
	private File programmerPath;	// OS dependent folder where papilio-prog.exe, etc are located 
	private File papilioProgrammerFile, srecCatFile, dataToMemFile;

	private List<File> targetBitFile = new ArrayList<File>(1);
	private List<File> targetBmmFile = new ArrayList<File>(1);
	private List<File> programHexFile = new ArrayList<File>(1);
	
	private boolean scanSelected, mergeSelected, eraseSelected;
	private boolean writeSelected, verifySelected; 
	private boolean unloadAfterBurn = false;
	
	private Properties settings;
	private boolean bSimpleMode = true;
	private int targetBoard;


	
// GIRISH: Review various MsgBox strings and titles 
	
	/*	We need to access the args[] array (command line arguments) inside the anonymous 
	 * 	inner class which will create JFrame on EDT. Access from anonymous inner class
	 * 	dictates String[] args to be declared as final.
	 * 	In addition, the command-line arguments (args[] array) from main thread will be 
	 * 	accessed from the EDT, thus making it cross-thread access. As Swing (EDT) is not 
	 * 	thread-safe, we must make String[] args final to ensure thread-safety of program.
	 */
	public static void main(final String[] args)
	{
		try {
			System.out.println(UIManager.getSystemLookAndFeelClassName());
			if (runningonWindows)
			    // Set System L&F
		        UIManager.setLookAndFeel(
		            UIManager.getSystemLookAndFeelClassName());
			else
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
	    } 
	    catch (UnsupportedLookAndFeelException e) {
	    	System.out.println(e.getMessage());
	    }
	    catch (ClassNotFoundException e) {
	    	System.out.println(e.getMessage());
	    }
	    catch (InstantiationException e) {
	    	System.out.println(e.getMessage());
	    }
	    catch (IllegalAccessException e) {
	    	System.out.println(e.getMessage());
	    }
	    catch (Exception e) {
	    	System.out.println(e.getMessage());
	    }
		System.getProperties().list(System.out); 
//	    System.out.println(Arrays.toString(args));
//	    System.out.println("Main thread: " + Thread.currentThread().getName());
	    
	    EventQueue.invokeLater(new Runnable() {
			@Override
			public void run()
			{
				PapilioLoader pl;
				String initial_bit_file = "", initial_hex_file = "";
				String eraseSwitch = "", targetSwitch = "", writeSwitch = "", quitSwitch = "";

				/*	No point in validating the command line parameters here. It is best to
					delegate validation and processing of command line arguments to 
					PapilioLoader constructor. Just try to "guess" the arguments here. */
				
				if (args.length >= 1) {
					initial_bit_file = args[0];
					if (args.length >= 2) {
						initial_hex_file = args[1];
						if (args.length >= 3) {
							targetSwitch = args[2];
							if (args.length >= 4) {
								writeSwitch = args[3];
								if (args.length >= 5)
									quitSwitch = args[4];
							}
						}
					}
				}

				pl = new PapilioLoader(initial_bit_file, initial_hex_file, 
						eraseSwitch, targetSwitch, writeSwitch, quitSwitch);
				pl.setVisible(true);
			}
		});
	}

	
	public PapilioLoader(String initial_bit_file, String initial_hex_file, String eraseSwitch,
						 String targetSwitch, String writeSwitch, String quitSwitch)
	{
		final int PANEL_LEFT_MARGIN = 10;
		final int PANEL_RIGHT_MARGIN = 10;
		
		// GIF image of the Papilio logo.
		final byte[] ICON_IMAGE = { 71, 73, 70, 56, 57, 97, 16, 0, 16, 0, -77,
				0, 0, 4, 2, 4, -124, -126, -124, -28, -30, -28, 68, 66, 68,
				-76, -78, -76, -4, -2, -4, 52, 50, 52, -20, -18, -20, 116, 114,
				116, -68, -66, -68, 0, 0, 0, 0, 0, 0, 73, -55, 91, -86, -8,
				-26, -128, 0, -44, 124, 0, 119, 33, -7, 4, 1, 0, 0, 0, 0, 44,
				0, 0, 0, 0, 16, 0, 16, 0, 3, 4, 85, 16, -56, 73, -85, -107,
				-31, -46, -111, 67, -55, 26, 80, 36, 28, 2, 14, 73, 82, 21,
				-58, -105, 32, 0, 58, -96, 84, 114, 124, 25, 45, 21, 86, -102,
				-111, 19, -98, -91, -128, -101, -24, 60, -60, 17, 17, 22, 43,
				12, 0, 1, -43, 36, 112, 35, 0, 124, 78, -127, 101, 64, -28, 96,
				2, -47, 103, 76, -62, 77, -128, 61, 79, -108, -7, -64, -12,
				-127, -59, -63, 116, 42, -44, 28, -63, -23, -12, 8, 0, 59 };

		boolean wasMaximized = false, loadLastSettings = false;
		int x = 150, y = 80, formWidth = 450, formHeight = 200;
		PopulateStatus cmdlineProcess;
		boolean bCreateNew = false; LastOperations defaultOperation;
		File lastProjectFile; String sQLastProject;

		rootProgrammerPath = new File(AppPath, "programmer");
		// Determine locations of Papilio Programmer, srec_cat and data2mem executables
		// depending on the current platform and architecture.
		if (runningonWindows)
		{
			programmerPath = new File(rootProgrammerPath, "win32");
			papilioProgrammerFile = new File(programmerPath, "papilio-prog.exe");
			srecCatFile = new File(programmerPath, "srec_cat.exe");
			dataToMemFile = new File(programmerPath, "data2mem.exe");
		}
		else
		{
			programmerPath = new File(rootProgrammerPath, "linux32");
			papilioProgrammerFile = new File(programmerPath, "papilio-prog");
			srecCatFile = new File(programmerPath, "srec_cat");
			dataToMemFile = new File(programmerPath, "data2mem");
		}

		ReadSettings();
		
		this.setIconImage(Toolkit.getDefaultToolkit().createImage(ICON_IMAGE));
		File icon = new File(AppPath, "papilio_48.png");
		if (icon.exists())
			this.setIconImage(Toolkit.getDefaultToolkit().getImage(icon.toString()));
		this.setJMenuBar(CreateMenuBar());
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we)
			{
				CleanupAndExit();
			}
		});

		// Use Boxlayout to stack the JPanels on top of each other.
		this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
		
		/*	All the JPanels stacked vertically have same width, viz. FORM_WIDTH, and
			same left and right margins from JFrame. As such, it is not required to call 
			.setAlignmentX() for any of the JPanels.
		 */
		
		pnlTarget = new TargetPanel(this, bSimpleMode, 20, PANEL_RIGHT_MARGIN, 5, PANEL_LEFT_MARGIN);
		this.add(pnlTarget);
		HelperFunctions.initVariables(pnlTarget.getCommonDialog(), this);

		if (bSimpleMode) 
		// => Program is running in Simple mode, so there is no question of adding Operations panel.
			btnProceed = pnlTarget.getProceedButton();
		else {
		// => Program is running in Expert mode, so add Operations panel.
			pnlOperations = new OperationPanel(this, 5, PANEL_RIGHT_MARGIN, 5, PANEL_LEFT_MARGIN);
			this.add(pnlOperations);
			btnProceed = pnlOperations.getProceedButton();
		}
		this.getRootPane().setDefaultButton(btnProceed);
		
		pnlOutput = new OutputPanel(formWidth, 200, 5, PANEL_RIGHT_MARGIN, 10, PANEL_LEFT_MARGIN);
		this.add(pnlOutput);
		txtOutput = pnlOutput.getOutputTextArea();

/*	------------------------------------------------------------------------------------
 * 		After a component is created it is in the invalid state by default. 
 * 		The Window.pack method validates the window and lays out the window's 
 * 		component hierarchy for the first time. 		
 *	------------------------------------------------------------------------------------ */
		this.pack();
/*	------------------------------------------------------------------------------------
 *		A container can be valid (namely, isValid() returns true) or invalid. 
 *		For a container to be valid, all the container's children must be laid out 
 *		already and must all be valid also. The Container.validate method can be used 
 *		to validate an invalid container. This method triggers the layout for the 
 *		container and all the child containers down the component hierarchy and 
 *		marks this container as valid. 
 *	------------------------------------------------------------------------------------ */
//	    System.out.println("GUI thread: " + Thread.currentThread().getName());

		wasMaximized = Boolean.parseBoolean(settings.getProperty("WindowMaximized"));
		try {
			x = Integer.parseInt(settings.getProperty("WindowX"));
			y = Integer.parseInt(settings.getProperty("WindowY"));
			formWidth = Integer.parseInt(settings.getProperty("WindowWidth"));
			formHeight = Integer.parseInt(settings.getProperty("WindowHeight"));
		}
		catch (NumberFormatException e) {
			System.err.println(e.getMessage());
		}
		if (wasMaximized)
			this.setExtendedState(Frame.MAXIMIZED_BOTH);
		else {
			this.setLocation(x, y);
//			this.setBounds(x, y, formWidth, formHeight);
		}

	    // Initialize and add placeholders.
	    targetBitFile.add(new File(""));
	    targetBmmFile.add(new File(""));
	    programHexFile.add(new File(""));

	    currProject = new LoaderProject();
	    
	    cmdlineProcess = ProcessCommandLineArguments(initial_bit_file, initial_hex_file, 
	    					eraseSwitch, targetSwitch, writeSwitch, quitSwitch);

		// Restore contents and state of various controls from previous program invocation, if reqd.
	    if (cmdlineProcess != PopulateStatus.None)
		{
	    	loadLastSettings = Boolean.parseBoolean(settings.getProperty("LoadLastSettings"));
	    	if (loadLastSettings) {
	    		sQLastProject = settings.getProperty("LastProject");
    			// sQLastProject should be canonical path.
	    		if (sQLastProject.isEmpty()) {
	    		// => The Papilio Loader project open last time program was closed was
	    		//	  "untitled" and hence was not saved on disk.
	    		// => Or user is using old preferences.txt file which contains no "LastProject"
	    		//	  property.
			    	if (cmdlineProcess == PopulateStatus.TextboxesAndCombobox)
						pnlTarget.PopulateLastFiles(settings.getProperty("LastBitFile"), 
													settings.getProperty("LastBmmFile"), 
													settings.getProperty("LastHexFile"));
			    	if (bSimpleMode)
						pnlTarget.PopulateLastWriteto(settings.getProperty("LastOperations"));
					else
						pnlOperations.PopulateLastOperations(settings.getProperty("LastOperations"));
			    	
			    	currProject.LoadOrphan(settings.getProperty("LastBitFile"), 
			    						   settings.getProperty("LastBmmFile"), 
			    						   settings.getProperty("LastHexFile"), 
			    						   settings.getProperty("LastOperations"));
	    		}
	    		else {
	    		/*	If (cmdlineProcess == PopulateStatus.Combobox) then it => Target XXX file
	    			textboxes need not be populated which further => the textboxes were populated
	    			as part of command-line arguments. In this case, it would be an error to
	    			load last project even if it is present.
	    			So, load last project only if there are no command-line arguments which is
	    			implied by (cmdlineProcess == PopulateStatus.TextboxesAndCombobox).
	    		*/
	    			if (cmdlineProcess == PopulateStatus.TextboxesAndCombobox) {
		    			lastProjectFile = new File(sQLastProject);
		    			if (lastProjectFile.isFile()) {
		    				if (currProject.Open(lastProjectFile)) {
		    					Properties ppjProject = currProject.getPpjProject();

		    					PopulateProject(ppjProject.getProperty(PPJProject.Board.toString()), 
		    									ppjProject.getProperty(PPJProject.BitFile.toString()), 
		    									ppjProject.getProperty(PPJProject.BmmFile.toString()), 
		    									ppjProject.getProperty(PPJProject.HexFile.toString()), 
		    									ppjProject.getProperty(PPJProject.Operations.toString()));
		    				}
		    			}
		    			else
		    				bCreateNew = true;
	    			}
	    			else if (cmdlineProcess == PopulateStatus.Combobox) {
	    				// Target XXX file textboxes have already been populated.
	    				// TODO: Corner case
				    	currProject.LoadOrphan("", "", "", 
	    						   settings.getProperty("LastOperations"));
	    			}
	    		}
	    	}
	    	else {
		    // => Last setings (stored in preferences.txt) need not be restored.
	    		// Create a new "Untitled" project.
	    		bCreateNew = true;
	    	}

    		if (bCreateNew) {
				if (bSimpleMode) {
					currProject.CreateNew(DEFAULT_SIMPLE_OPERATION.toString());
					pnlTarget.PopulateLastWriteto(DEFAULT_SIMPLE_OPERATION.toString());
				}
				else {
					// In advanced mode, we should ensure that [Scan] buton is selected and as [Write to]
					// button will be unselected, ensure that cboWriteTargets Combobox is disabled.
					currProject.CreateNew(DEFAULT_EXPERT_OPERATION.toString());
					pnlOperations.PopulateLastOperations(DEFAULT_EXPERT_OPERATION.toString());
				}
			}
			this.setTitle(LOADER_NAME + currProject.getProjectTitle() );
		}
	    pnlTarget.setTargetBoard(targetBoard);
	    this.setTitle(LOADER_NAME);
	}

	private void CleanupAndExit()
	{
		if (!unloadAfterBurn) {
		// => Program is running normally and not in batch mode having QUIT_SWITCH specified
			if (!DoDirtyAction(currProject.getPpjProject()))
				// => User cancelled the operation
					return;
		}
		
		if (dlgPreferences != null)
			dlgPreferences.dispose();
		SaveSettings();
		System.exit(0);
	}

	
	/**
	 * <p>This function processes command line arguments passed, if any. By processing, various 
	 * controls on the form are filled with values specified in command-line arguments. Optionally 
	 * [Do Selected Operation] is performed and program quits, again as per the arguments.</p>
	 * 
	 * <p>There can be 4 different "Command-line Arguments Status" as follows:</p>
	 * 
	 * <ol>
	 * <li>None : No command-arguments are passed. In this case, function simply returns.</li>
	 * <li>Valid : In this case, all the command-line arguments passed are valid as far as 
	 * 			   their positioning and contents are concerned. Processing is done as 
	 * 			   mentioned above.</li>
	 * <li>Forgiving : The <bitfile> and <hexfile>, if specified, arguments or <ppjfile> are 
	 * 				   valid files but at least one of subsequent switches is invalid.
	 * 				   The invalid switch, and any subsequent switche(s), if any, are completely 
	 * 				   ignored. 
	 * 				   All the Target XXX File: textboxes are setup as per the arguments.
	 * 				   Whether Write to Combobox is setup or not depends on whether target
	 * 				   device switch is valid or not. Rest behaviour depends on validity of 
	 * 				   subsequent switches.</li>
	 * <li>Invalid : The <bitfile> and <hexfile> argument(s) or <ppjfile> argument point to 
	 * 				 invalid files. Controls are not "touched" and there is no question of performing 
	 * 				 any operation.</li>
	 * </ol>
	 * 
	 * @param initial_bit_file	1st command-line argument
	 * @param initial_hex_file	2nd command-line argument / switch
	 * @param targetSwitch		3rd command-line switch
	 * @param writeSwitch		4th command-line switch
	 * @param quitSwitch		5th command-line switch
	 * @return
	 */
	private PopulateStatus ProcessCommandLineArguments(String initial_bit_file, String initial_hex_file, 
			 									String eraseSwitch, String targetSwitch,
			 									String writeSwitch, String quitSwitch)
	{
//				initial_bit_file = args[0];
//				initial_hex_file = args[1];
//				targetSwitch = args[2];
//				writeSwitch = args[3];
//				quitSwitch = args[4];
					
		File WorkingDir = new File(System.getProperty("user.dir"));
		File initialBitFile = null, initialHexFile = null;
		File suggestedBmmFile; String sQSuggestedBmmFile;
		boolean bPpjSpecified = false;	// true indicates initial_bit_file contains .ppj file

		/*	If no command-line arguments are present, simply return. */
		
		if (initial_bit_file.isEmpty())
			return PopulateStatus.TextboxesAndCombobox;

		
		/* 	Validate and sanitize command-line arguments as at least one command-line
			argument is present. */

    	// 1st argument must be a .bit file or a .ppj file, or "-e".

		if (initial_bit_file.equals(ERASE_SWITCH))
		{
			// Erase switch is exception to the rule that first argument
			// must be a filename, hence its separate treatment
			// This switch can be accompanied by "-q" and "-x"
			// Target is implied to be flash (-s)
			eraseSwitch = ERASE_SWITCH;
			if (initial_hex_file.equals(WRITE_SWITCH))
				writeSwitch = WRITE_SWITCH;
			else
				writeSwitch = ""; // Ignore it
			if (targetSwitch.equals(QUIT_SWITCH)) {
				quitSwitch = QUIT_SWITCH;
			}
				
			else
				quitSwitch = ""; // Ignore it
			// Set target to be SPI flash
			targetSwitch = "-s";
			initial_bit_file = "";
			initial_hex_file = "";
		}
		else
		{
			eraseSwitch = "";
	    	initialBitFile = FullyQualifiedFile(WorkingDir, initial_bit_file);
	    	if (!initialBitFile.isFile()) {
				JOptionPane.showMessageDialog(this, 
						"The file specified " + initialBitFile.getName() + 
						" does not exist on disk.", 
						"Invalid File", 
						 JOptionPane.WARNING_MESSAGE);
	    		return PopulateStatus.TextboxesAndCombobox;
	    	}
	    	else if (initialBitFile.getName().endsWith(".ppj"))
	        // => 1st command-line argument is a valid .ppj file
	    	{
	    		/*	2nd and 3rd command-line arguments, if specified, should be write switch 
	    			and quit switch respectively. */
	    		
	    		if (initial_hex_file.isEmpty())
	    	    // As 2nd command-line argument is not specied, stop validating further 
	    			;	// Do nothing as targetSwitch, writeSwitch, quitSwitch must be "".
	    		else if (initial_hex_file.equals(WRITE_SWITCH))
	        	// => 2nd argument is write switch.
	    		{
	    			writeSwitch = WRITE_SWITCH;
	    			
	    			if (targetSwitch.isEmpty()) {
	    			// => 3rd argument is not specified, so stop.
	        			;	// Do nothing as targetSwitch, quitSwitch must be "".
	    			}
	    			else if (targetSwitch.equals(QUIT_SWITCH)) {
	    			// => 3rd command-line argument is quit switch.
	        			// Sanitize switches.
	    				quitSwitch = QUIT_SWITCH;
	    				bSimpleMode = false;
	            		targetSwitch = "";
	    			}
	    			else {
	    	    	// => 3rd argument (very last one) is neither quit switch nor "", so 
	    			//	  it is invalid and needs to be ignored.
	    				quitSwitch = "";
	        			// Sanitize switches.
	            		targetSwitch = "";
	    			}
	    		}
	    		else {
	    		// => 2nd argument is neither write switch nor "", so it is invalid. 
	    		// That argument as well as subsequent arguments should be ignored.
					quitSwitch = "";
					writeSwitch = "";
	        		targetSwitch = "";
	    		}
	    		
	    		// Signal to processing logic that user has specified .ppj file as 1st argument.
	    		bPpjSpecified = true;
	    		
	    		/*	All the validation and sanitization of command-line argument is done here,
	    			in case of user specifying .ppj file as 1st argument. Thus, signal to 
	    			subsequent validation logic (used in case of .bit file) that nothing
	    			needs to be done. 
	    		 */
	    		initial_hex_file = "";
	    	}
	    	else if (!initialBitFile.getName().endsWith(".bit")) {
	    	// => 1st command-line argument is neither a .ppj file nor a .bit file and,
	    	//	  hence, invalid.
				JOptionPane.showMessageDialog(this, 
						"The file specified as 1st argument must either be .bit file or .ppj file.", 
						"Invalid File", 
						 JOptionPane.WARNING_MESSAGE);
	    		return PopulateStatus.TextboxesAndCombobox;
	    	}
	
	    	// 2nd argument may be .hex file or it may be target device switch or it may
	    	// be completely blank.
	
	    	if (initial_hex_file.isEmpty()) {
	    	// As 2nd command-line argument is not specied, stop validating further 
	    		;	// Do nothing as targetSwitch, writeSwitch, quitSwitch must be "".
	    	}
	
			// Checking 2nd argument for [-f | -s | -d] must be done PRIOR to checking for .hex file. 
	    	else if ((initial_hex_file.equals("-f")) || (initial_hex_file.equals("-s"))) 
	//    			 (initial_hex_file.equals("-d")))
	    	// => 2nd command-line argument is a target device switch and not a .hex file.
	    	{
	    		if (targetSwitch.isEmpty()) {
		    	// As 3rd command-line argument is not specied, stop validating further
	    			// Sanitize switches; maintain reverse order in assignments.
	        		targetSwitch = initial_hex_file;
	        		initial_hex_file = "";
	    			// writeSwitch, quitSwitch must already be "".
	    		}
	    		
	    		else if (targetSwitch.equals(WRITE_SWITCH))
	    		{
	    		// => 3rd argument is write switch.
	    			if (writeSwitch.isEmpty()) {
	    			// => 4th argument is not specified, so stop.
	        			// Sanitize switches; maintain reverse order in assignments.
	    				writeSwitch = targetSwitch;
	            		targetSwitch = initial_hex_file;
	            		initial_hex_file = "";
	    			}
	    			else if (writeSwitch.equals(QUIT_SWITCH)) {
	    			// => 4th command-line argument is quit switch.
	        			// Sanitize switches; maintain reverse order in assignments.
	    				quitSwitch = writeSwitch;
	    				writeSwitch = targetSwitch;
	            		targetSwitch = initial_hex_file;
	            		initial_hex_file = "";
	    			}
	    			else {
	    	    	// => 4th argument (very last one) is neither quit switch nor "", so 
	    			//	  it is invalid and needs to be ignored.
	    				quitSwitch = "";
	        			// Sanitize switches; maintain reverse order in assignments.
	    				writeSwitch = targetSwitch;
	            		targetSwitch = initial_hex_file;
	            		initial_hex_file = "";
	    			}
	    		}
	    		
	    		else {
	    		// => 3rd argument is neither write switch nor "", so it is invalid. 
	    		// It as well as subsequent arguments should be ignored.
	    			quitSwitch = "";
	    			writeSwitch = "";
	    			// Sanitize switches; maintain reverse order in assignments.
	        		targetSwitch = initial_hex_file;
	        		initial_hex_file = "";
	    		}
	    	}
	
	    	// Now, attempt to parse 2nd command-line argument as .hex file
	    	
	    	else
	    	{
	    		initialHexFile = FullyQualifiedFile(WorkingDir, initial_hex_file);
		    	if (!initialHexFile.isFile()) {
					JOptionPane.showMessageDialog(this, 
							"The Hex file specified " + initialHexFile.getName() + 
							" does not exist on disk.", 
							"Invalid Hex File", 
							 JOptionPane.ERROR_MESSAGE);
		    		return PopulateStatus.TextboxesAndCombobox;
		    	}
	
		    	// At this point, we are sure that 2nd command-line argument is a valid .hex file. 
		    	
		    	if (targetSwitch.isEmpty())
		        // As 3rd command-line argument is not specied, stop validating further 
		    		;	// Do nothing as writeSwitch, quitSwitch must be "".
		    	
		    	else if ((targetSwitch.equals("-f")) || (targetSwitch.equals("-s"))) 
	//	    			 (targetSwitch.equals("-d")))
		    	{
		        // => 3rd command-line argument is indeed a target device switch.
		    		if (writeSwitch.isEmpty())
		    	    // As 4th command-line argument is not specied, stop validating further 
			    		;	// Do nothing as quitSwitch must be "".
		    		else if (writeSwitch.equals(WRITE_SWITCH))
		    		{
		    		// => 4th argument is write switch.
		    			if (quitSwitch.isEmpty())
		    				;
		    			else if (!quitSwitch.equals(QUIT_SWITCH))
		    	        // => 5th argument is neither quit switch nor "", so it is invalid. 
		    				quitSwitch = "";
		    		}
		    		else {
		        	// => 4th argument is neither write switch nor "", so it is invalid. 
		        		// It as well as subsequent arguments should be ignored.
		    			writeSwitch = "";
		    			quitSwitch = "";
		    		}
		    	}
	
		    	else {
		        // => 3rd argument is neither target device switch nor "", so it is invalid. 
	        		// It as well as subsequent arguments should be ignored.
		    		targetSwitch = "";
	    			writeSwitch = "";
	    			quitSwitch = "";
		    	}
	    	}
		}


	    /* 	At this point, we are sure that at least 1 command-line argument is present and all
	    	are valid and sanitized. So, process the command-line arguments. */

    	if (bPpjSpecified) {
			if (currProject.Open(initialBitFile)) {
				Properties ppjProject = currProject.getPpjProject();

				PopulateProject(ppjProject.getProperty(PPJProject.Board.toString()), 
								ppjProject.getProperty(PPJProject.BitFile.toString()), 
								ppjProject.getProperty(PPJProject.BmmFile.toString()), 
								ppjProject.getProperty(PPJProject.HexFile.toString()), 
								ppjProject.getProperty(PPJProject.Operations.toString()));
				
				this.setTitle(LOADER_NAME + currProject.getProjectTitle() );
			}

			if (quitSwitch.equals(QUIT_SWITCH)){
				bSimpleMode = false;
				unloadAfterBurn = true;
			}
				

			if (writeSwitch.equals(WRITE_SWITCH))
			// Burn .ppj file passed as 1st argument immediately.
				btnProceed.doClick();
			return PopulateStatus.None;
    	}
    	
    	if (eraseSwitch.equals(ERASE_SWITCH))
    	{
    		// Erase switch is on its own, no document instertUpdate event is triggered
    		// So we must set the buttons ourselves
    		//pnlOperations.setEraseButton(true);
    		eraseSelected = true;
    	}
    	else
    	{
    		// Do this only if the first switch is not erase but is a file name
    		
	    	// initial_bit_file may contain ".." or symlinks, etc. It is best to get its canonical 
	    	// path prior to populating it in Target .bit File: textbox.
			initial_bit_file = HelperFunctions.CanonicalPath(initialBitFile);
			pnlTarget.setBitFile(initial_bit_file);
	
			// We need to find the suggested .bmm file and populate it if present.
			sQSuggestedBmmFile = initial_bit_file.substring(0, initial_bit_file.lastIndexOf(".bit")) + "_bd.bmm";
			suggestedBmmFile = new File(sQSuggestedBmmFile);
			if (suggestedBmmFile.isFile()) {
				pnlTarget.setBmmFile(sQSuggestedBmmFile);
				pnlTarget.getCommonDialog().setCurrentDirectory(suggestedBmmFile);
			}
	
			if (initialHexFile != null)
			// => User is specifying program .hex file as command-line argument.
			// => The .bit file specified as 1st argument is probably unprogrammed empty processor
			{
				// Otherwise, [Do Selected Operation] won't work.
				if (!suggestedBmmFile.isFile()) 
				{
					pnlTarget.setBmmFile("");
					JOptionPane.showMessageDialog(this, 
							"The BMM file corresponding to specified bit file " + 
							initial_bit_file + " is not found.", 
							"BMM File Not Found", 
							 JOptionPane.ERROR_MESSAGE);
		    		return PopulateStatus.Combobox;
				}
				
				// Finally, populate specified program .hex file.
				pnlTarget.setHexFile(HelperFunctions.CanonicalPath(initialHexFile));
			}
    	}

		if (quitSwitch.equals(QUIT_SWITCH))
			unloadAfterBurn = true;
		
		if (targetSwitch.equals("-f")) {
			if (bSimpleMode)
				pnlTarget.setWriteTarget(WriteTargets.FPGA);
			else
				pnlOperations.setWriteTarget(WriteTargets.FPGA);
			
			if (writeSwitch.equals(WRITE_SWITCH))
				// Burn .bit file passed as 1st argument to "FPGA" immediately.
				btnProceed.doClick();
			
			return PopulateStatus.None;
		}
		else if (targetSwitch.equals("-s")) {
			if (bSimpleMode)
				pnlTarget.setWriteTarget(WriteTargets.SPI_FLASH);
			else
				pnlOperations.setWriteTarget(WriteTargets.SPI_FLASH);

			if (writeSwitch.equals(WRITE_SWITCH))
			// Burn .bit file passed as 1st argument to "SPI Flash" immediately.
				btnProceed.doClick();

			return PopulateStatus.None;
		}
		else if (targetSwitch.equals("-d")) {
			if (bSimpleMode)
				pnlTarget.setWriteTarget(WriteTargets.DISK_FILE);
			else
				pnlOperations.setWriteTarget(WriteTargets.DISK_FILE);
			
			if (writeSwitch.equals(WRITE_SWITCH))
			// Write .bit file passed as 1st argument to "Disk File" immediately.
				btnProceed.doClick();

			return PopulateStatus.None;
		}
		else
			return PopulateStatus.None;
	}
	
	
	private JMenuBar CreateMenuBar()
	{
        JMenuBar menuBar = new JMenuBar();
        JMenu menu; JMenuItem menuItem;

        /* Construct File Menu */
        
        menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(menu);

        // New Project... menu item
        menuItem = new JMenuItem("New Project...", KeyEvent.VK_N);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
		menuItem.addActionListener(this);
		menu.add(menuItem);

        // Open Project... menu item
        menuItem = new JMenuItem("Open Project...", KeyEvent.VK_O);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		menuItem.addActionListener(this);
		menu.add(menuItem);

        // Save Project menu item
        menuItem = new JMenuItem("Save Project", KeyEvent.VK_S);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		menuItem.addActionListener(this);
		menu.add(menuItem);

        // Save Project As... menu item
        menuItem = new JMenuItem("Save Project As...", KeyEvent.VK_A);
		menuItem.addActionListener(this);
		menu.add(menuItem);

		menu.addSeparator();

        // Preferences... menu item
        menuItem = new JMenuItem("Preferences...", KeyEvent.VK_P);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, ActionEvent.CTRL_MASK));
		menuItem.addActionListener(this);
		menu.add(menuItem);

		menu.addSeparator();
		
        // Exit menu item
        menuItem = new JMenuItem("Exit", KeyEvent.VK_X);
		menuItem.addActionListener(this);
		menu.add(menuItem);

        /* Construct Help Menu */
        
        menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        menuBar.add(menu);
        
        // Contents menu item
        menuItem = new JMenuItem("Papilio Loader Help", KeyEvent.VK_H);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
		menuItem.addActionListener(this);
		menu.add(menuItem);
		
		menu.addSeparator();
		
        // About menu item
        menuItem = new JMenuItem("About Papilio Loader", KeyEvent.VK_A);
		menuItem.addActionListener(this);
		menu.add(menuItem);

		menuBar.add(Box.createHorizontalGlue());
        menu = new JMenu("<html><a href='http://papilio.cc'>http://papilio.cc</a></html>");
        menu.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(MenuEvent e)
			{
				HelperFunctions.BrowseURL("http://papilio.cc", runningonWindows);
//				HelperFunctions.JavaBrowseURL("http://papilio.cc");
				System.out.println("menuSelected");
//				((JMenu) e.getSource()).setSelected(false);
			}
			
			@Override
			public void menuDeselected(MenuEvent e)
			{
				System.out.println("menuDeselected");
			}
			
			@Override
			public void menuCanceled(MenuEvent e)
			{
				System.out.println("menuCanceled");
			}
		});
        menuBar.add(menu);
//        menuBar.add(new JLabel("<html><a href='http://papilio.cc'>http://papilio.cc</a></html>"));
		
        return menuBar;
	}

	/** Handle menu events here. */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		String currActionCommand = e.getActionCommand();
		
		if (currActionCommand.equals("Exit")) {
			CleanupAndExit();
		}
		else if (currActionCommand.equals("New Project..."))
		{
			if (!DoDirtyAction(currProject.getPpjProject()))
			// => User cancelled the operation
				return;

			String defaultOperation;
			if (bSimpleMode)
				defaultOperation = DEFAULT_SIMPLE_OPERATION.toString();
			else
				defaultOperation = DEFAULT_EXPERT_OPERATION.toString();
			
			PopulateProject(AUTO_DETECT_FPGA, "", "", "", defaultOperation);
			currProject.CreateNew(defaultOperation);
			
			this.setTitle(LOADER_NAME + currProject.getProjectTitle() );
		}
		else if (currActionCommand.equals("Open Project..."))
		{
			if (!DoDirtyAction(currProject.getPpjProject()))
			// => User cancelled the operation
				return;
			
			if (currProject.Open(null)) {
				Properties ppjProject = currProject.getPpjProject();

				PopulateProject(ppjProject.getProperty(PPJProject.Board.toString()), 
								ppjProject.getProperty(PPJProject.BitFile.toString()), 
								ppjProject.getProperty(PPJProject.BmmFile.toString()), 
								ppjProject.getProperty(PPJProject.HexFile.toString()), 
								ppjProject.getProperty(PPJProject.Operations.toString()));
				
				this.setTitle(LOADER_NAME + currProject.getProjectTitle() );
			}
		}
		else if ((currActionCommand.equals("Save Project")) || 
				 (currActionCommand.equals("Save Project As...")))
		{
			if (pnlTarget.isBlank())
				return;

			Properties ppjProject = currProject.getPpjProject();

			pnlTarget.SaveCurrentBoard(ppjProject);
			pnlTarget.SaveCurrentFiles(ppjProject);
			if (bSimpleMode)
				pnlTarget.SaveCurrentWriteto(ppjProject);
			else
				pnlOperations.StoreLastOperations(ppjProject, PPJProject.Operations.toString());
			
			currProject.Save(currActionCommand.equals("Save Project As..."));
			
			this.setTitle(LOADER_NAME + currProject.getProjectTitle() );
		}
		else if (currActionCommand.equals("Preferences..."))
		{
			File preferencesFile, settingsPath;

			if (dlgPreferences == null)
				dlgPreferences = new PreferencesDialog(this, settings, runningonWindows);
			
			settingsPath = EnsureSettingsFolder();
			if (settingsPath == null)
				preferencesFile = null;
			else
				preferencesFile = new File(settingsPath, PREFERENCES_FILE);
			dlgPreferences.setPreferencesFile(preferencesFile);

			dlgPreferences.PopulateForm();
		}
		else if (currActionCommand.equals("Papilio Loader Help")) {
			File helpContentsHTMLFile = new File(AppPath, "help/index.htm");
			
			if (helpContentsHTMLFile.isFile())
				HelperFunctions.BrowseURL(helpContentsHTMLFile.toURI().toString(), runningonWindows);
			else
				HelperFunctions.BrowseURL("http://papilio.cc/index.php?n=Papilio.PapilioLoaderV2", runningonWindows);
		}
		else if (currActionCommand.equals("About Papilio Loader"))
		{
			File aboutImageFile = new File(AppPath, "images/loader_about.png");
			if (aboutImageFile.isFile())
				HelperFunctions.DisplayAboutBox(this, aboutImageFile);
		}
//		System.out.println(currActionCommand);
	}


	// Returns false if user cancelled the operation. True otherwise.
	// The Papilio Loader project (.ppj) file is just a convenience to user and hence not
	// a document in conventional terms. So, asking user "Do you wish to save..." would be
	// pretty obtrusive. So, I have commented out the code which works perfectly. The
	// return true; statement makes this function a stub. But the function is kept 
	// nevertheless - to have "hook" points.
	private boolean DoDirtyAction(Properties ppjProject)
	{
		return true;
		
/*		boolean isNotDirty = false;
		int iResponse;
		
		if (!currProject.isSavedOnDisk())
		// => The project is still "untitled", i.e. it has not yet been saved to disk.
		{
			if (pnlTarget.isBlank())
				return true;
		}
		else
		// => The project has been saved on disk.
		//	  In this case, it makes sense to check whether UI is dirty or not?
		{
			if (pnlTarget.isNotDirty(ppjProject)) {
				if (bSimpleMode)
					isNotDirty = pnlTarget.OperationNotDirty(ppjProject);
				else
					isNotDirty = pnlOperations.OperationsNotDirty(ppjProject);
			}
		}

		if (isNotDirty)
		// => Current project is not dirty
			return true;
		else
		{
			iResponse = JOptionPane.showConfirmDialog(this, 
								 "The project " + currProject.getProjectName() + " has been modified.\nDo you wish to save it?", 
								 "Confirm Save", 
								  JOptionPane.YES_NO_CANCEL_OPTION, 
								  JOptionPane.QUESTION_MESSAGE);
			switch (iResponse)
			{
			case JOptionPane.YES_OPTION:
				pnlTarget.SaveCurrentBoard(ppjProject);
				pnlTarget.SaveCurrentFiles(ppjProject);
				
				if (bSimpleMode)
					pnlTarget.SaveCurrentWriteto(ppjProject);
				else
					pnlOperations.StoreLastOperations(ppjProject, PPJProject.Operations.toString());

				currProject.Save(false);
				return true;
				
			case JOptionPane.NO_OPTION:
				return true;
			case JOptionPane.CANCEL_OPTION:
				return false;	// User cancelled operation
				
			default:
				return false;	// for compiler
			}
		}
*/	}
	
	public void PopulateProject(String selBoard, 
								String sQBitFile, String sQBmmFile, String sQHexFile, 
								String operationsList)
	{
		pnlTarget.PopulateBoard(selBoard);
		pnlTarget.PopulateLastFiles(sQBitFile, sQBmmFile, sQHexFile);
		
		if (bSimpleMode)
			pnlTarget.PopulateLastWriteto(operationsList);
		else 
    		pnlOperations.PopulateLastOperations(operationsList);
	}

	
	private File EnsureSettingsFolder()
	{
		File settingsPath;

		if (runningonWindows)
			settingsPath = new File(System.getenv("APPDATA") + "\\" + SETTINGS_FOLDER + "\\");
		else 
			settingsPath = new File(System.getProperty("user.home"), "." + SETTINGS_FOLDER);

		if (!settingsPath.isDirectory()) {
			if (settingsPath.mkdir())
				return settingsPath;
			else
				return null;
		}
		else
			return settingsPath;
	}
	
	private void ReadSettings()
	{
		Properties defaultSettings = new Properties();
		File preferencesFile, settingsPath;
		InputStreamReader isr = null;

		Properties installSettings = ReadPapilioInit();
		
		defaultSettings.setProperty("UserMode", UserModes.Simple.toString());
		if (installSettings != null) {
		// => papilio-init is present in program directory.
			// Read UserMode from papilio-init, if present and use it as default UserMode.
			String installUserMode = installSettings.getProperty("UserMode");
			if (installUserMode != null)
				defaultSettings.setProperty("UserMode", installUserMode);
		}

		defaultSettings.setProperty("LoadLastSettings", "false");
		defaultSettings.setProperty("LastProject", "");
		defaultSettings.setProperty("LastBitFile", "");
		defaultSettings.setProperty("LastBmmFile", "");
		defaultSettings.setProperty("LastHexFile", "");
		defaultSettings.setProperty("LastOperations", LastOperations.WRITE_TO_FPGA.toString());
		defaultSettings.setProperty("WindowMaximized", "false");
		defaultSettings.setProperty("WindowX", "150");
		defaultSettings.setProperty("WindowY", "80");
		defaultSettings.setProperty("WindowWidth", "450");
		defaultSettings.setProperty("WindowHeight", "400");
		defaultSettings.setProperty("TargetBoard", "0");

		settings = new Properties(defaultSettings);

		settingsPath = EnsureSettingsFolder();
		if (settingsPath == null) {
			bSimpleMode = settings.getProperty("UserMode").equalsIgnoreCase(UserModes.Simple.toString());
			return;
		}

		preferencesFile = new File(settingsPath, PREFERENCES_FILE);

		if (preferencesFile.isFile()) {
			try {
				isr = new InputStreamReader(new FileInputStream(preferencesFile), "UTF-8");
				settings.load(isr);
			}
			catch (IOException e) {
				System.err.println(e.getMessage());
			}
			finally
			{
				if (isr != null) {
					try {
						isr.close();  
					}
					catch (IOException ioex) {
						System.err.println(ioex.getMessage());
					}
				}
			}
		}

		bSimpleMode = settings.getProperty("UserMode").equalsIgnoreCase(UserModes.Simple.toString());
		targetBoard = Integer.parseInt(settings.getProperty("TargetBoard"));
		//pnlTarget.setTargetBoard(targetBoard);
		

	}
	
	private Properties ReadPapilioInit()
	{
		final String INSTALL_INIT_FILE = "papilio-init";
		Properties installSettings = new Properties();
		File installInitFile;
		InputStreamReader isr = null;
		
		installInitFile = new File(AppPath, INSTALL_INIT_FILE);
		
		if (installInitFile.isFile()) {
			try {
				isr = new InputStreamReader(new FileInputStream(installInitFile), "UTF-8");
				installSettings.load(isr);
			}
			catch (IOException e) {
				System.err.println(e.getMessage());
			}
			finally
			{
				if (isr != null) {
					try {
						isr.close();  
					}
					catch (IOException ioex) {
						System.err.println(ioex.getMessage());
					}
				}
			}
		}
		else
			return null;

		return installSettings;
	}
	
	private void SaveSettings()
	{
		File preferencesFile, settingsPath;
		OutputStreamWriter osw = null;

		settingsPath = EnsureSettingsFolder();
		if (settingsPath == null)
			return;
		
		preferencesFile = new File(settingsPath, PREFERENCES_FILE);

/*	------------------------------------------------------------------------------------
 *	Properties.store(Writer writer, String comments)
 *		Writes this property list (key and element pairs) in this Properties table to the 
 *		output character stream in a format suitable for using the load(Reader) method. 
 *		Properties from the defaults table of this Properties table (if any) are **NOT** 
 *		written out by this method. 
 *	------------------------------------------------------------------------------------ */

		if (!preferencesFile.isFile()) {
			// Write default properties explicitly, otherwise they won't be persisted.
			settings.setProperty("UserMode", settings.getProperty("UserMode"));
			settings.setProperty("LoadLastSettings", settings.getProperty("LoadLastSettings"));
		}
		settings.setProperty("LastProject", currProject.QDiskPath());
		
		if ((this.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH)
			settings.setProperty("WindowMaximized", "true");
		else
			settings.setProperty("WindowMaximized", "false");
		settings.setProperty("WindowX", "" + this.getX());
		settings.setProperty("WindowY", "" + this.getY());
		settings.setProperty("WindowWidth", "" + this.getWidth());
		settings.setProperty("WindowHeight", "" + this.getHeight());
		
		settings.setProperty("TargetBoard", pnlTarget.getTargetBoard());

		pnlTarget.StoreLastFiles(settings);
		if (bSimpleMode)
			pnlTarget.StoreLastWriteto(settings);
		else
			pnlOperations.StoreLastOperations(settings, "LastOperations");
		
		try {
			osw = new OutputStreamWriter(new FileOutputStream(preferencesFile), "UTF-8");
			settings.store(osw, null);
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
		}
		finally
		{
			if (osw != null) {
				try {
					osw.close();  
				}
				catch (IOException ioex) {
					System.err.println(ioex.getMessage());
				}
			}
		}
		
	}
	
	/*	If file_name contains a fully-qualified file name, then
	 * 		a File object corresponding to file_name is returned
	 * 	Else if file_name contains a relative file name, then
	 * 		file_name is considered relative WorkingDir and a reference to 
	 * 		File object created from WorkingDir and file_name is returned.
	 * 		Note : If file_name contains aliases, shortcuts, "..", those are resolved.
	 */
	private File FullyQualifiedFile(File WorkingDir, String file_name)
	{
		File completeFile = new File(file_name);
		if (completeFile.isAbsolute())
			return completeFile;
		else
			return HelperFunctions.CanonicalFile(new File(WorkingDir, file_name));
	}
	
	/**	This is called whenever contents of any of the Target XXX file textboxes 
	 * 	(in Target panel) change. Depending on the contents of textboxes, Operation
	 * 	panel adjusts the state of various JToggleButtons so that operation flow
	 * 	is conveyed to the user.
	 */
	public void SyncOperationButtons(String editedTargetBitFile, 
									 String editedTargetBmmFile, 
									 String editedProgramHexFile) {
		// TODO: Optimize for case when some text in textbox is selected and new text is typed.
		pnlOperations.DisplayOperationFlow(editedTargetBitFile, editedTargetBmmFile, editedProgramHexFile);
	}


	/* [Do Selected Operation] is over. */
	public void BurningOver() {
		if (unloadAfterBurn && nErrorCount == 0)
			CleanupAndExit();
	}
	
	public void Erase(){
		//pnlOperations.setEraseButton(true);
		eraseSelected = true;
		btnProceed.doClick();
		//EraseSPIFlash();
	}

	public void BurnUsingProgrammer(WriteTargets selTarget)
	{
		File diskBitFile = null; 
		
		if (!FormValid())
			return;

		if (selTarget == WriteTargets.DISK_FILE) {
			diskBitFile = HelperFunctions.ShowFileSave("Save .bit + .bmm + .hex file as", 
								bitFileFilter, 
								".bit", 
								null, 
								null);
			if (diskBitFile == null)
			// => Either user clicked [Cancel] button or 
			//	  responded [No] to "Confirm Overwrite" prompt  
				return;
		}

		btnProceed.setEnabled(false);
		txtOutput.setText("");
//		System.out.println("Total threads : " + Thread.activeCount());
		new AsyncProgrammer(selTarget, diskBitFile);
	}
	
	
	private boolean FormValid()
	{
		if (!papilioProgrammerFile.isFile()) {
		// => papilio-prog.exe is NOT present
			JOptionPane.showMessageDialog(this, 
							"Papilio programmer (" + papilioProgrammerFile.getName() + ") does not exist on disk. Please reinstall the program.", 
							"Papilio Programmer Not Found", 
							 JOptionPane.ERROR_MESSAGE);
			return false;
		}
		else if (!srecCatFile.isFile()) {
		// => srec_cat.exe is NOT present
			JOptionPane.showMessageDialog(this, 
							"srec_cat program (" + srecCatFile.getName() + ") does not exist on disk. Please reinstall the program.", 
							"srec_cat Program Not Found", 
							 JOptionPane.ERROR_MESSAGE);
			return false;
		}
		else if (!dataToMemFile.isFile()) {
		// => data2mem.exe is NOT present
			JOptionPane.showMessageDialog(this, 
							"data2mem program (" + dataToMemFile.getName() + ") does not exist on disk. Please reinstall the program.", 
							"data2mem Program Not Found", 
							 JOptionPane.ERROR_MESSAGE);
			return false;
		}
		else {
			if (bSimpleMode)
			{
				boolean[] ret_ScanSelected = {false};
				boolean[] ret_MergeSelected = {false};
				boolean[] ret_WriteSelected = {false};
				
				if (pnlTarget.SimpleModeDataValid(targetBitFile, targetBmmFile, programHexFile, 
												  ret_ScanSelected, ret_MergeSelected, ret_WriteSelected)) {
					scanSelected = ret_ScanSelected[0];
					mergeSelected = ret_MergeSelected[0];
					writeSelected = ret_WriteSelected[0];
				}
				else
					return false;
			}
			else
			{
				scanSelected = pnlOperations.isScanSelected();
				mergeSelected = pnlOperations.isMergeSelected();
				eraseSelected = pnlOperations.isEraseSelected();
				writeSelected = pnlOperations.isWriteSelected();
				verifySelected = pnlOperations.isVerifySelected();

				if (!pnlTarget.DataValid(targetBitFile, targetBmmFile, programHexFile, 
										 scanSelected, mergeSelected, eraseSelected, 
										 writeSelected, verifySelected))
					return false;
			}
		}
		return true;
	}


	public class AsyncProgrammer implements Runnable, MessageConsumer
	{
		private File finalBitFile, bscanSPIBitFile;
		private String q_papilio_prog_exe, q_srec_cat_exe, q_data_2_mem_exe, boardName;
		private WriteTargets useTarget;
		private boolean lookforDesc; private String deviceID;

		public AsyncProgrammer(WriteTargets selTarget, File diskBitFile)
		{
			Thread thread;
			
			q_papilio_prog_exe = HelperFunctions.CanonicalPath(papilioProgrammerFile);
			q_srec_cat_exe = HelperFunctions.CanonicalPath(srecCatFile);
			q_data_2_mem_exe = HelperFunctions.CanonicalPath(dataToMemFile);
			useTarget = selTarget;
			finalBitFile = diskBitFile;

			thread = new Thread(this, "Async-Papilio-Programmer");
			// It is better to set the priority of AsyncProgrammer thread same as that of
			// MessageSiphon thread (but greater than priority of EDT).
			// Anyway, this (AsyncProgrammer thread) is going to block (wait) on MessageSiphon 
			// threads, so everything will work out.
			thread.setPriority(Thread.MAX_PRIORITY-2);
			thread.start();
		}

		
		@Override
		public void run()
		{
			nErrorCount = 0;
			
//		    System.out.println("AsyncProgrammer thread: " + Thread.currentThread().getName());
		    if (writeSelected == false)
		    // => Anything out of {[Scan], [Erase], [Verify]} is selected, but at least one is selected.
		    // => [Write to] and [Merge] is definitely not selected.
		    // => Any of the files specified in Target XXX file textboxes need be ignored completely.
		    {
		    	bscanSPIBitFile = DetectJTAGchain();
		    	
		    	if (bscanSPIBitFile != null) {
			    	if (eraseSelected)
			    		EraseSPIFlash();	// Erase also does verification
			    	else if (verifySelected)
			    		VerifySPIFlash();
		    	}
		    }
		    else
		    // => [Write to] is selected, [Merge] may or may not be selected.
		    // => [Scan] is always implied.
		    // => [Erase] and [Verify], if selected, are applicable in the context of [Write to].
		    {
//		    	if (eraseSelected){
//		    		bscanSPIBitFile = DetectJTAGchain();
//		    		if (bscanSPIBitFile != null) {
//		    			EraseSPIFlash();	// Erase also does verification
//		    		}
//		    		btnProceed.setEnabled(true);
//		    		return;
//		    	}
			    if (!mergeSelected)
		    	// => The file specified in "Target .bit file:" text box is the final .bit
			    //	  file to be burned, i.e. the result of .bit + .bmm + .hex merging. 
				// Even if user has specified "Target .bmm file:" and/or "Program .hex file:",
			    // they are ignored.
			    	finalBitFile = targetBitFile.get(0);
			    
			    else {
			    	if (!MergeBitBmmHexFiles()) {
						btnProceed.setEnabled(true);
			    		return;
			    	}
			    }

				switch (useTarget) {
				case FPGA:
					BurnToFPGA();
					break;
				case SPI_FLASH:
					if (!verifySelected && !eraseSelected && !bSimpleMode)
						BurnToSPIFlashOnly();
					else
						BurnToSPIFlash();
					break;
				case DISK_FILE:
					// Do nothing.
					// MergeBitBmmHexFiles already does that if selected target is disk file
					break;
				}
		    }
			btnProceed.setEnabled(true);
		}
		
		
		private boolean MergeBitBmmHexFiles()
		{
			final String INTERMEDIATE_MEM_FILE = "tmp.mem";
			final File intermediateMemFile = new File(programmerPath, INTERMEDIATE_MEM_FILE);
			final String OUTPUT_MEM_FILE = "out.mem";
			final File outputMemFile = new File(programmerPath, OUTPUT_MEM_FILE);
			final String FINAL_BIT_FILE = "out.bit";

			String[] srecCatCommand = {q_srec_cat_exe, 
									   HelperFunctions.CanonicalPath(programHexFile.get(0)), 
									   "-Intel", "-Byte_Swap", "2", "-Data_Only", 
									   "-o", INTERMEDIATE_MEM_FILE, "-vmem", "8"};
			FileInputStream fin = null; FileOutputStream fout = null;
			BufferedReader br = null; BufferedWriter bw = null;
			String sLine; int pos;
//			String[] commandLine = {"cmd.exe", "/C", "dir", "/S", "C:\\WINDOWS\\System32\\"};
			String[] data2memCommand = {q_data_2_mem_exe, 
										"-bm", HelperFunctions.CanonicalPath(targetBmmFile.get(0)), 
										"-bt", HelperFunctions.CanonicalPath(targetBitFile.get(0)), 
										"-bd", OUTPUT_MEM_FILE, 
										"-o", "b", FINAL_BIT_FILE};

			/*	Run srec_cat to create "tmp.mem" from program .hex file. */
			
			execSynchronously(srecCatCommand, programmerPath, false);
			if (!intermediateMemFile.isFile())
				return false;

			
			/* 	Convert "tmp.mem" to a format which is understood by data2mem.
				This means to strip the starting offsets/addresses present on 
				each line and changing the line ending to Unix style. The created
				file is "out.mem". */
			
//			txtOutput.append("Converting srec_cat .mem file to data2mem format...\n\n");
			try 
			{
				fin = new FileInputStream(intermediateMemFile);
				// The "tmp.mem" is ASCII file. 
				br = new BufferedReader(new InputStreamReader(fin, "US-ASCII"));
				fout = new FileOutputStream(outputMemFile);
				// The "out.mem" should also be a ASCII file with Unix line ending.
				bw = new BufferedWriter(new OutputStreamWriter(fout, "US-ASCII"));
				
				while ((sLine = br.readLine()) != null) {
					pos = sLine.indexOf(" ");
					if (pos != -1) 
						sLine = sLine.substring(pos);
			        bw.write(sLine + "\n");	// data2mem expects "out.mem" to have Unix line ending
				}
			}
			catch (IOException e) {
				System.err.println(e.getMessage());
			}
			finally
			{
				if (br != null) {
					try {
						br.close();		// This also closes fin FileInputStream.  
					}
					catch (IOException ioex) {
						System.err.println(ioex.getMessage());
					}
				}
				if (bw != null) {
					try {
						// Not all line endings are created equal. So, it is better if
						// flush the BufferedWriter for "out.mem".
						bw.flush();
						bw.close();
						fout.close();
					}
					catch (IOException ioex) {
						System.err.println(ioex.getMessage());
					}
				}
			}
			if (!outputMemFile.isFile())
				return false;
			

			/*	Run data2mem to combine .bit (unprogrammed empty processor) file, 
				.bmm memory map file and program .hex file into final .bit file. */
			
			if (finalBitFile == null)
			// => User has selected either "FPGA" or "SPI Flash" in Write to combobox. 
				finalBitFile = new File(programmerPath, FINAL_BIT_FILE);
			else
			// => User has selected "Disk File" in Write to combobox.
				data2memCommand[data2memCommand.length - 1] = HelperFunctions.CanonicalPath(finalBitFile);
			execSynchronously(data2memCommand, programmerPath, false);

			if (finalBitFile.isFile())
				return true;
			else
				return false;
		}


		private File DetectJTAGchain()
		{
			File bscanBitFile = null;
			String[] scanJTAG;
			String[] scanJTAGOrig = {q_papilio_prog_exe, "-j"};
			String[] scanJTAGID = {q_papilio_prog_exe, "-j", "-d", "\"" + pnlTarget.getBoardName() + "\""};
			
			if (pnlTarget.getBoardName().isEmpty())
				scanJTAG = scanJTAGOrig;
			else
				scanJTAG = scanJTAGID;

			execSynchronously(scanJTAG, programmerPath, true);
			
			//txtOutput.append("In DetectJTAG: " + deviceID);
			
			if (!deviceID.isEmpty()) {
				//txtOutput.append("In isEmpty: " + deviceID);
				if (deviceID.equals("XC3S250E"))
					bscanBitFile = new File(rootProgrammerPath, "bscan_spi_xc3s250e.bit");
				else if (deviceID.equals("XC3S500E"))
					bscanBitFile = new File(rootProgrammerPath, "bscan_spi_xc3s500e.bit");
				else if (deviceID.equals("XC3S100E"))
					bscanBitFile = new File(rootProgrammerPath, "bscan_spi_xc3s100e.bit");
				else if (deviceID.equals("XC6SLX9")) 
					bscanBitFile = new File(rootProgrammerPath, "bscan_spi_xc6slx9.bit");
				else if (deviceID.equals("XC6SLX4"))
					bscanBitFile = new File(rootProgrammerPath, "bscan_spi_xc6slx4.bit");
			}
			
			return bscanBitFile;
		}
		
		private void BurnToFPGA()
		{
			String[] commandLine;
			String[] commandLineID = {q_papilio_prog_exe, "-v", "-d", "\"" + pnlTarget.getBoardName() + "\"",
									"-f", HelperFunctions.CanonicalPath(finalBitFile)};
			String[] commandLineOrig = {q_papilio_prog_exe, "-v",
									"-f", HelperFunctions.CanonicalPath(finalBitFile)};
			
			if (pnlTarget.getBoardName().isEmpty())
				commandLine = commandLineOrig;
			else
				commandLine = commandLineID;
			
			execSynchronously(commandLine, programmerPath, false);
		}

		public void EraseSPIFlash()
		{
			String[] commandLine;
			String[] commandLineOrig = {q_papilio_prog_exe, "-v", 
									"-b", HelperFunctions.CanonicalPath(bscanSPIBitFile), 
									"-se"};
			String[] commandLineID = {q_papilio_prog_exe, "-v", "-d", "\"" + pnlTarget.getBoardName() + "\"", 
					"-b", HelperFunctions.CanonicalPath(bscanSPIBitFile), 
					"-se"};
			
			if (pnlTarget.getBoardName().isEmpty())
				commandLine = commandLineOrig;
			else
				commandLine = commandLineID;

			execSynchronously(commandLine, programmerPath, false);
			eraseSelected = false;
		}
		
		private void BurnToSPIFlash()
		{
	    	bscanSPIBitFile = DetectJTAGchain();
						
			if (bscanSPIBitFile != null)
			{
				String[] commandLine;
				String[] commandLineOrig = {q_papilio_prog_exe, "-v", 
										"-f", HelperFunctions.CanonicalPath(finalBitFile), 
										"-b", HelperFunctions.CanonicalPath(bscanSPIBitFile), 
										"-sa", "-r"};
				String[] commandLineID = {q_papilio_prog_exe, "-v", "-d", "\"" + pnlTarget.getBoardName() + "\"",  
						"-f", HelperFunctions.CanonicalPath(finalBitFile), 
						"-b", HelperFunctions.CanonicalPath(bscanSPIBitFile), 
						"-sa", "-r"};				
				if (pnlTarget.getBoardName().isEmpty())
					commandLine = commandLineOrig;
				else
					commandLine = commandLineID;
				
				execSynchronously(commandLine, programmerPath, false);

				execSynchronously(new String[] {q_papilio_prog_exe, "-c"}, programmerPath, false);
			}
		}

		private void BurnToSPIFlashOnly()
		{
	    	bscanSPIBitFile = DetectJTAGchain();
			//txtOutput.append("In SPI Flash Burn: " + bscanSPIBitFile);
			
			if (bscanSPIBitFile != null)
			{
				String[] commandLine;
				String[] commandLineOrig = {q_papilio_prog_exe, "-v", 
										"-f", HelperFunctions.CanonicalPath(finalBitFile), 
										"-b", HelperFunctions.CanonicalPath(bscanSPIBitFile), 
										"-sp", "-r"};
				String[] commandLineID = {q_papilio_prog_exe, "-v", "-d", "\"" + pnlTarget.getBoardName() + "\"",
						"-f", HelperFunctions.CanonicalPath(finalBitFile), 
						"-b", HelperFunctions.CanonicalPath(bscanSPIBitFile), 
						"-sp", "-r"};
				if (pnlTarget.getBoardName().isEmpty())
					commandLine = commandLineOrig;
				else
					commandLine = commandLineID;
				
				execSynchronously(commandLine, programmerPath, false);

				execSynchronously(new String[] {q_papilio_prog_exe, "-c"}, programmerPath, false);
			}
		}		
		
		private void VerifySPIFlash()
		{
			String[] commandLine;
			String[] commandLineOrig = {q_papilio_prog_exe, "-v", 
									"-b", HelperFunctions.CanonicalPath(bscanSPIBitFile), 
									"-sv"};
			String[] commandLineID = {q_papilio_prog_exe, "-v", "-d", "\"" + pnlTarget.getBoardName() + "\"",
					"-b", HelperFunctions.CanonicalPath(bscanSPIBitFile), 
					"-sv"};
			
			if (pnlTarget.getBoardName().isEmpty())
				commandLine = commandLineOrig;
			else
				commandLine = commandLineID;

			execSynchronously(commandLine, programmerPath, false);
		}


		private void execSynchronously(String[] command, File workingDir, boolean parseStdOut)
		{
		    Process process = null;
		    int exitCode = 0;
		    
		    if (ECHO_COMMAND) {
		    	String execCommand = "Executing...\n";
		    	for (String item : command) {
					execCommand += item + " ";
				}
				txtOutput.append(execCommand + "\n\n");		// JTextArea .append is thread-safe
		    }
		    
		    lookforDesc = parseStdOut;
		    if (parseStdOut)
		    	deviceID = "";
		    
		    try {
		      process = Runtime.getRuntime().exec(command, null, workingDir);
		    } catch (IOException e) {
		      System.err.println(e.getMessage());
		    }
		    
		    // any output?
		    MessageSiphon in = new MessageSiphon("Message-Siphon-StdOut", process.getInputStream(), this);
		    // any error message?
		    MessageSiphon err = new MessageSiphon("Message-Siphon-StdErr", process.getErrorStream(), this);

		    // Kick both of them off.
		    err.KickOff();
		    in.KickOff();

		    // wait for the exec'd process to finish.  if interrupted
		    // before waitFor returns, continue waiting
		    boolean burning = true;
		    while (burning) {
		      try {
	/*	------------------------------------------------------------------------------------
	 * 		One thread can wait for another thread to terminate by using one of the other
	 * 		thread's join methods.
	 * 		When in.getThread().join() returns, in.run is guaranteed to have finished.
	 * 		If in.run is already finished by the time in.getThread().join() is called,
	 * 		in.getThread().join() returns immediately. 
	 *	------------------------------------------------------------------------------------ */
		    	if (in.getThread() != null)
		    		in.getThread().join();
		        
	/*	------------------------------------------------------------------------------------
	 * 		One thread can wait for another thread to terminate by using one of the other
	 * 		thread's join methods.
	 * 		When err.getThread().join() returns, err.run is guaranteed to have finished.
	 * 		If err.run is already finished by the time err.getThread().join() is called,
	 * 		err.getThread().join() returns immediately. 
	 *	------------------------------------------------------------------------------------ */
				if (err.getThread() != null)
					err.getThread().join();

	/*	------------------------------------------------------------------------------------
	 * 		waitFor causes the current thread to wait, if necessary, until the process 
	 * 		represented by the Process object on which .waitFor() is called, has terminated. 
	 * 		waitFor method returns immediately if the subprocess has already terminated. 
	 * 		If the subprocess has not yet terminated, the calling thread will be blocked 
	 * 		until the subprocess exits. 
	 *	------------------------------------------------------------------------------------ */
				exitCode = process.waitFor();
				System.out.println("Exit code is " + exitCode);
				nErrorCount += exitCode;

				burning = false;
		      } catch (InterruptedException ignored) { }
		    }
		}

		/**
		 * Part of the MessageConsumer interface.
		 * This is called whenever a piece (usually a line) of error message is
		 * spewed out from the compiler. The errors are parsed for their contents
		 * and line number, which is then reported back to Editor.
		 */
		@Override
		public void DeliverMessage(final String stdline)
		{
			try {
				EventQueue.invokeAndWait(new Runnable() {
					@Override
					public void run()
					{
						txtOutput.append(stdline);
					}
				});
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			
			if ((lookforDesc) && (deviceID.isEmpty())) {
				int pos = stdline.lastIndexOf("Desc: ");
				if (pos != -1) {
					if (runningonWindows)
						deviceID = stdline.substring(pos + "Desc: ".length(), stdline.length() - 2);
					else
						deviceID = stdline.substring(pos + "Desc: ".length(), stdline.length() - 1);
				}
			}
		}

	}

}
