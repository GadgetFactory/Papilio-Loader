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

import static net.gadgetfactory.papilio.loader.PapilioLoader.AUTO_DETECT_FPGA;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Properties;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *	Data model class for Papilio Loader project (.ppj) 
 */
public class LoaderProject
{
	private static final String UNTITLED_LABEL = "NoName_";
	private static int untitledIndex = 0;
	
	public enum PPJProject {
		Board, 
		BitFile, 
		BmmFile, 
		HexFile, 
		Operations
	};

	
	private FileNameExtensionFilter ppjFileFilter = new FileNameExtensionFilter("Papilio Loader Project files", "ppj");

	private String projectName;
	private File projectFile;
	
	private Properties ppjProject = new Properties();

	public Properties getPpjProject() {
		return this.ppjProject;
	}

	// To use in title of form
	public String getProjectTitle() {
		if (this.projectName.startsWith(UNTITLED_LABEL))
			return "";
		else
			return " - (" + this.projectName + ")";
	}

	public String QDiskPath() {
		if (projectFile != null)
			return HelperFunctions.CanonicalPath(projectFile);	// Use canonical path
		else
			return "";
	}

	public boolean isSavedOnDisk() {
		return (projectFile != null);
	}

	public LoaderProject() {
		// Ensure required project properties are available.
		ppjProject.setProperty(PPJProject.Board.toString(), "");
		ppjProject.setProperty(PPJProject.BitFile.toString(), "");
		ppjProject.setProperty(PPJProject.BmmFile.toString(), "");
		ppjProject.setProperty(PPJProject.HexFile.toString(), "");
		ppjProject.setProperty(PPJProject.Operations.toString(), "");
	}

	
	/** 
	 * Create new Papilio Loader project in memory.
	 */
	public void CreateNew(String untitledOperationsList)
	{
		untitledIndex++;
		projectName = UNTITLED_LABEL + untitledIndex;
		
		projectFile = null;
	
		// Reset ppj project properties.
		ppjProject.setProperty(PPJProject.Board.toString(), AUTO_DETECT_FPGA);
		ppjProject.setProperty(PPJProject.BitFile.toString(), "");
		ppjProject.setProperty(PPJProject.BmmFile.toString(), "");
		ppjProject.setProperty(PPJProject.HexFile.toString(), "");
		ppjProject.setProperty(PPJProject.Operations.toString(), untitledOperationsList);
	}

	/** 
	 * Load "untitled" Papilio Loader project in memory.
	 */
	public void LoadOrphan(String lastBitFile, String lastBmmFile, String lastHexFile, 
						   String lastOperations)
	{
		untitledIndex++;
		projectName = UNTITLED_LABEL + untitledIndex;
		
		projectFile = null;

		// Reset ppj project properties.
		ppjProject.setProperty(PPJProject.Board.toString(), AUTO_DETECT_FPGA);
		ppjProject.setProperty(PPJProject.BitFile.toString(), lastBitFile);
		ppjProject.setProperty(PPJProject.BmmFile.toString(), lastBmmFile);
		ppjProject.setProperty(PPJProject.HexFile.toString(), lastHexFile);
		ppjProject.setProperty(PPJProject.Operations.toString(), lastOperations);
	}
	
	/** 
	 * Open a saved (disk) Papilio Loader project in memory as current project.
	 */
	public boolean Open(File lastProjectFile)
	{
		InputStreamReader isr = null;
		File existingProjectFile;
		boolean bError = false;
		
		if (lastProjectFile != null)
			existingProjectFile = lastProjectFile;
		else {
			existingProjectFile = HelperFunctions.ShowFileOpen("Open Papilio Loader Project", 
														ppjFileFilter, ".ppj", projectFile, null);
			if (existingProjectFile == null)
				return false;
		}
		
		try {
			isr = new InputStreamReader(new FileInputStream(existingProjectFile), "UTF-8");
			ppjProject.load(isr);
			
			// Update file name of opened project file.
			projectName = existingProjectFile.getName();
			// Make newly opened project file as current.
			projectFile = existingProjectFile;
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
			bError = true;
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
		
		return !bError;
	}
	
	public void Save(boolean bSaveAs)
	{
		OutputStreamWriter osw = null;

		if (bSaveAs) {
		// => Save As operation
			File newProjectFile = HelperFunctions.ShowFileSave("Save Papilio Loader Project As", 
											ppjFileFilter, 
											".ppj", 
											null, 
											projectFile);
			if (newProjectFile == null)
			// => Either user clicked [Cancel] button or 
			//	  responded [No] to "Confirm Overwrite" prompt  
				return;

			// Make newly saved project file as current.
			projectFile = newProjectFile;
		}
		else {
		// => Normal Save operation
			if (projectFile == null)
			// => This is "Untitled" (new) Papilio Loader project which has not been saved to
			//	  disk yet.
			{
				projectFile = HelperFunctions.ShowFileSave("Save Papilio Loader Project", 
										ppjFileFilter, 
										".ppj", 
										null, 
										null);
				if (projectFile == null)
				// => Either user clicked [Cancel] button or 
				//	  responded [No] to "Confirm Overwrite" prompt  
					return;
			}
		}
		
		try {
			osw = new OutputStreamWriter(new FileOutputStream(projectFile), "UTF-8");
			ppjProject.store(osw, "Papilio Loader Project");
			// Update file name of saved project file.
			projectName = projectFile.getName();
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

}
