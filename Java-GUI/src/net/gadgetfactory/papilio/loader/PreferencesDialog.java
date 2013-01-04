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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.border.Border;
import net.gadgetfactory.papilio.loader.PapilioLoader.ShellDefaultActions;
import net.gadgetfactory.papilio.loader.PapilioLoader.UserModes;

public class PreferencesDialog extends JDialog implements ActionListener, MessageConsumer
{
	private final String REGISTRY_KEY_DEFAULT_ACTION = "HKEY_CLASSES_ROOT\\Papilio Loader bitstream\\Shell";
	
	private File preferencesFile;
	private Properties settings;
	private Box boxUserModes = Box.createVerticalBox();
	private Box boxShellDefaultActions = Box.createVerticalBox();
	private JCheckBox chkLoadLastSettings = new JCheckBox("Load last files on startup");
	private boolean runningonWindows;
	private ShellDefaultActions currShellDefaultAction = ShellDefaultActions.OpenBitFileAndWait;
	private boolean bFound;

	public void setPreferencesFile(File preferencesFile) {
		this.preferencesFile = preferencesFile;
	}

	public PreferencesDialog(JFrame owner, Properties currsettings, boolean bOnWindows)
	{
		super(owner, "Preferences", true);

		runningonWindows = bOnWindows;
		settings = currsettings;
		
		final int PANEL_LEFT_MARGIN = 10;
		final int PANEL_RIGHT_MARGIN = 10;
		final int OPTION_BUTTON_WIDTH = 315;
		final int OPTION_BUTTON_HEIGHT = 25;	// Layout manager should ignore this
		final Dimension OPTION_BUTTON_SIZE = new Dimension(OPTION_BUTTON_WIDTH, OPTION_BUTTON_HEIGHT); 
		final int BUTTON_WIDTH = 67;
		final int BUTTON_HEIGHT = 25;
		final Dimension BUTTON_SIZE = new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT); 

		final String[] userModeDescriptions = {"<html>Simple (Shows minimal information and automatically select<br> steps to perform)</html>", 
											   "<html>Expert (Shows full information about flow and automatically<br> lets you select steps to perform)</html>"};
		final String[] userModeActions = {UserModes.Simple.toString(), UserModes.Expert.toString()};
		final String[] defaultActionDescriptions = {"Write to FPGA and quit", 
		   											"Write to SPI Flash and quit", 
		   											"Start Papilio Loader and wait"};
		final String[] shellDefaultActions = {ShellDefaultActions.WriteToFPGAandQuit.toString(), 
											  ShellDefaultActions.WriteToSPIFlashAndQuit.toString(),
											  ShellDefaultActions.OpenBitFileAndWait.toString()};

		Container contentPane = this.getContentPane();
		Box boxLoadLastSettings = Box.createVerticalBox();
		Box boxPreferences = Box.createVerticalBox();
		Box boxButtons = Box.createHorizontalBox();
		Border bdrMargin, bdrBox;
		JButton btnDismiss;
		JLabel lblRestartRequired = new JLabel("   (Preferences will be applied after Papilio Loader is restarted)");

		/*	Create User Mode option button group and lay it from top to bottom. */
		
		// Create margin and Titled border around the Box. 
		bdrMargin = BorderFactory.createEmptyBorder(10, PANEL_LEFT_MARGIN, 
												    10, PANEL_RIGHT_MARGIN);
		bdrBox = BorderFactory.createTitledBorder("User Mode");
		
		HelperFunctions.CreateOptionButtonGroup(boxUserModes, 
												BorderFactory.createCompoundBorder(bdrMargin, bdrBox), 
												userModeDescriptions, 
												userModeActions, 
												null);

		boxPreferences.add(boxUserModes);

		/*	Create Default Action option button group and lay it from top to bottom. */
		
		if (runningonWindows) {
			// Create margin and Titled border around the Box. 
			bdrMargin = BorderFactory.createEmptyBorder(0, PANEL_LEFT_MARGIN, 
													    10, PANEL_RIGHT_MARGIN);
			bdrBox = BorderFactory.createTitledBorder("Default Action");
			
			HelperFunctions.CreateOptionButtonGroup(boxShellDefaultActions, 
													BorderFactory.createCompoundBorder(bdrMargin, bdrBox), 
													defaultActionDescriptions, 
													shellDefaultActions, 
													OPTION_BUTTON_SIZE);
	
			boxPreferences.add(boxShellDefaultActions);
		}

		/*	Create Last Settings and lay it from top to bottom. */

		bdrMargin = BorderFactory.createEmptyBorder(0, PANEL_LEFT_MARGIN + 5, 
			    									10, PANEL_RIGHT_MARGIN);
		boxLoadLastSettings.setBorder(bdrMargin);

		chkLoadLastSettings.setPreferredSize(OPTION_BUTTON_SIZE);
		chkLoadLastSettings.setMaximumSize(OPTION_BUTTON_SIZE);
		boxLoadLastSettings.add(chkLoadLastSettings);

		// Layout hack required for cross-platform appearanece. 
		// If Preferrred and Maximum size of lblRestartRequired are set in Linux, then it displays 
		//		"   (Preferences will be applied after Papilio Load..."
		// thereby adding ... and truncating label caption.
		if (runningonWindows) {
			lblRestartRequired.setPreferredSize(OPTION_BUTTON_SIZE);
			lblRestartRequired.setMaximumSize(OPTION_BUTTON_SIZE);
		}
		boxLoadLastSettings.add(lblRestartRequired);
		
		boxPreferences.add(boxLoadLastSettings);

		/*	Create [OK] and [Cancel] buttons and lay them from left to right. */
		
		// Provide empty space (margin) inside the Box, otherwise bottom sides of [OK]
		// [Cancel] buttons will butt against bottom side of dialog and so on.
		boxButtons.setBorder(BorderFactory.createEmptyBorder(0, PANEL_LEFT_MARGIN, 
															 10, PANEL_RIGHT_MARGIN));

		// Add horizontal glue first so that [OK], [Cancel] buttons will be right-aligned.
		boxButtons.add(Box.createHorizontalGlue());
        
        // [OK] button
		btnDismiss = new JButton("OK");
		/* If we do not set size explicitly, then Swing makes width of [OK] button < that
		   of [Cancel] button - owing to their captions being different. To override this,
		   at least make widths of both buttons same, we set size explicitly here.
		 */
		btnDismiss.setMinimumSize(BUTTON_SIZE);
		btnDismiss.setPreferredSize(BUTTON_SIZE);
		btnDismiss.addActionListener(this);
        boxButtons.add(btnDismiss);

		this.getRootPane().setDefaultButton(btnDismiss);	// Set [OK] button as default

/*	------------------------------------------------------------------------------------
 * 	NOTE:	We cannot use a horizontal strut in place of rigid area. Because, struts have 
 * 			unlimited maximum heights or widths (for horizontal and vertical struts, respectively). 
 * 			This means that if you use a horizontal box within a vertical box (as in this
 * 			JDialog), for example, the horizontal box can sometimes become too tall. For 
 * 			this reason, rigid area is used instead of struts. 
 *	------------------------------------------------------------------------------------ */
        // Add a fixed space (10 pixles horizontally) between [OK] and [Cancel] buttons.  
        boxButtons.add(Box.createRigidArea(new Dimension(10, 0)));

        // [Cancel] button
		btnDismiss = new JButton("Cancel");
		/* If we do not set size explicitly, then Swing makes width of [OK] button < that
		   of [Cancel] button - owing to their captions being different. To override this,
		   at least make widths of both buttons same, we set size explicitly here.
		 */
		btnDismiss.setMinimumSize(BUTTON_SIZE);
		btnDismiss.setPreferredSize(BUTTON_SIZE);
		btnDismiss.addActionListener(this);
        boxButtons.add(btnDismiss);
		
        /*	Finally, put everything together. */
        
		// The default contentPane for JDialog has a BorderLayout manager set on it.
		contentPane.add(boxPreferences, BorderLayout.CENTER);
        contentPane.add(boxButtons, BorderLayout.PAGE_END);

/*	------------------------------------------------------------------------------------
 * 	Window.pack()
 * 		Causes this Window to be sized to fit the preferred size and layouts of its 
 * 		subcomponents. If the window and/or its owner are not yet displayable, both are 
 * 		made displayable before calculating the preferred size. The Window will be 
 * 		validated after the preferredSize is calculated.
 *	------------------------------------------------------------------------------------ */
/*	------------------------------------------------------------------------------------
 * 		After a component is created it is in the invalid state by default. 
 * 		The Window.pack method validates the window and lays out the window's 
 * 		component hierarchy for the first time. 		
 *	------------------------------------------------------------------------------------ */
        this.pack();
/*	------------------------------------------------------------------------------------
 * 	Window.setLocationRelativeTo(Component c)
 * 		Sets the location of the window relative to the specified component.
 * 		If the component is not currently showing, or c is null, the window is placed at 
 * 		the center of the screen. The center point can be determined with 
 * 		GraphicsEnvironment.getCenterPoint
 * 		If the bottom of the component is offscreen, the window is placed to the side of 
 * 		the Component that is closest to the center of the screen. So if the Component is 
 * 		on the right part of the screen, the Window is placed to its left, and visa versa.  		
 *	------------------------------------------------------------------------------------ */
		this.setLocationRelativeTo(owner);
		
		// The defaultCloseOperation property is set to HIDE_ON_CLOSE, by default, which is
		// the desirable behaviour for dialogbox.
		this.setResizable(false);
//		System.out.println("User Mode Box size : " + boxUserModes.getSize().toString());
//		System.out.println("Default Action Box size : " + boxShellDefaultActions.getSize().toString());
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand().equals("OK")) {
			SaveForm();
		}
		
		this.setVisible(false);
	}
	
	
	public void PopulateForm()
	{
		/*	Populate current User Mode */
		if (settings.getProperty("UserMode").equalsIgnoreCase(UserModes.Simple.toString()))
			HelperFunctions.SelectOption4Group(boxUserModes, UserModes.Simple.toString());
		else
			HelperFunctions.SelectOption4Group(boxUserModes, UserModes.Expert.toString());
		
		/*	Determine Shell Default Action */
		
		if (runningonWindows)
		{
			String[] commandLine = {"reg.exe", "query", REGISTRY_KEY_DEFAULT_ACTION, "/ve"};
			bFound = false;
			// We can pass null as workingDir argument because reg.exe is already in the path.
			// Besides, if we pass null as workingDir, the subprocess (reg.exe) inherits the 
			// current working directory of the current process. 
			execSynchronously(commandLine, null);

			/*	Select JRadioButton corresponding to current Shell Default Action */
			HelperFunctions.SelectOption4Group(boxShellDefaultActions, currShellDefaultAction.toString());
		}
		else
		{
			
		}

		/*	Populate whether to Load Last Settings */
		chkLoadLastSettings.setSelected(Boolean.parseBoolean(settings.getProperty("LoadLastSettings")));
		
		/*	Finally, show Preferences dialogbox. */
		this.setVisible(true);
	}
	
	private void SaveForm()
	{
		OutputStreamWriter osw = null;
		
		settings.setProperty("UserMode", HelperFunctions.SelectedOptionButton(boxUserModes));
		settings.setProperty("LoadLastSettings", "" + chkLoadLastSettings.isSelected());

		/*	Save selected User Mode and Load Last Settings action in preferences.txt file. */
		
		if (preferencesFile != null)
		{
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
		
		
		/*	Save selected Shell Default Action in OS Registry. */

		if (runningonWindows)
		{
			String[] commandLine = {"reg.exe", "add", REGISTRY_KEY_DEFAULT_ACTION, "/ve", 
									"/t", "REG_SZ", "/d", "", "/f"};
			currShellDefaultAction = ShellDefaultActions.valueOf(HelperFunctions.SelectedOptionButton(boxShellDefaultActions));
			switch (currShellDefaultAction)
			{
			case OpenBitFileAndWait:
				commandLine[commandLine.length - 2] = "Open";
				break;
	
			case WriteToFPGAandQuit:
				commandLine[commandLine.length - 2] = "WriteToFPGA";
				break;
				
			case WriteToSPIFlashAndQuit:
				commandLine[commandLine.length - 2] = "WritePermanently";
				break;
			}
		
			bFound = true;	// To stop DeliverMessage from processing StdOut line. 
			// We can pass null as workingDir argument because reg.exe is already in the path.
			// Besides, if we pass null as workingDir, the subprocess (reg.exe) inherits the 
			// current working directory of the current process. 
			execSynchronously(commandLine, null);
		}

	}
	
	
	private void execSynchronously(String[] command, File workingDir)
	{
	    Process process = null;
	    int exitCode = 0;

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
	    boolean querying = true;
	    while (querying) {
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
			System.out.println("Exit code (Shell Default Action) is " + exitCode);

			querying = false;
	      } catch (InterruptedException ignored) { }
	    }
	}

	@Override
	public void DeliverMessage(final String stdline)
	{
		if ((bFound) || (stdline.isEmpty()))
			return;

		if (stdline.contains("Open")) {
			currShellDefaultAction = ShellDefaultActions.OpenBitFileAndWait;
			bFound = true;
		}
		else if (stdline.contains("WritePermanently")) {
			currShellDefaultAction = ShellDefaultActions.WriteToSPIFlashAndQuit;
			bFound = true;
		}
		else if (stdline.contains("WriteToFPGA")) {
			currShellDefaultAction = ShellDefaultActions.WriteToFPGAandQuit;
			bFound = true;
		}
	}

}
