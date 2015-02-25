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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.gadgetfactory.papilio.loader.LoaderProject.PPJProject;

public class TargetPanel extends JPanel implements ActionListener, FocusListener, DocumentListener
{
	private PapilioLoader parentFrame;

	private JComboBox cboBoards;
	private JTextField txtQBitFile, txtBoardName, txtQBmmFile, txtQHexFile;
	
	private JComboBox cboWriteTargets;
	private JButton btnProceed;

	private JFileChooser commonDialog = new JFileChooser();
	// As [Info] button corresponds to 0th index and FileNameExtensionFilter is
	// not at all applicable to it, we use null for 0th index.
	private FileNameExtensionFilter progFileFilters[] = {null, 
			new FileNameExtensionFilter("Bit files", "bit"), 
			new FileNameExtensionFilter("Bmm files", "bmm"), 
			new FileNameExtensionFilter("Hex files", "hex")};

	private String[] labelCaptions;
	private String[] targetBoards = 
			{AUTO_DETECT_FPGA, "Papilio DUO", "Papilio One or Papilio Pro"};
	private final String[] labelCaptionsSimple = {"Target board:", 
				"Target .bit file:", "Board Name"};	
	private final String[] labelCaptionsExpert = {"Target board:", 
	  				"Target .bit file:", "Board Name", "Target .bmm file:", "Program .hex file:"};
	private final String[] buttonTexts = {"Erase", "Select...", "None", "Select...", "Select..."};
	// All the actions, except 1st one, must specify the Extension of file to open. 
	private final String[] buttonActions = {"Erase SPI Flash", "None", ".bit", ".bmm", ".hex"};
	
	private boolean bSimpleMode;

	public JFileChooser getCommonDialog() {
		return commonDialog;
	}

	public JButton getProceedButton() {
		return this.btnProceed;
	}
	
	public String getBoardName(){
		txtBoardName.getText();
		if (!txtBoardName.getText().isEmpty()) {
			return txtBoardName.getText();
		}
		else {
			return "";
		}
	}

	public String getTargetBoard() {
		return Integer.toString(cboBoards.getSelectedIndex());
	}
	
	public void setTargetBoard(int board) {
		cboBoards.setSelectedIndex(board);
	}	
	
	public void setBitFile(String bit_file) {
		txtQBitFile.setText(bit_file);
	}

	public void setBmmFile(String bmm_file) {
		txtQBmmFile.setText(bmm_file);
	}

	public void setHexFile(String hex_file) {
		txtQHexFile.setText(hex_file);
	}

	public void setWriteTarget(WriteTargets newTarget) {
		cboWriteTargets.setSelectedItem(newTarget);
	}

	public TargetPanel(PapilioLoader plframe, boolean simpleMode, 
					   int topMargin, int rightMargin, int bottomMargin, int leftMargin)
	{
		JLabel lblCaption; JTextField txtQFile; JButton btnSelect;
		int exceptionCellIndex = Integer.MAX_VALUE;
		
		parentFrame = plframe;
		bSimpleMode = simpleMode;
		
		
		if (bSimpleMode)
			labelCaptions = labelCaptionsSimple;
		else 
			labelCaptions = labelCaptionsExpert;

		// Create margin and Titled border around this panel. 
		Border bdrMargin = 
				BorderFactory.createEmptyBorder(topMargin, leftMargin, bottomMargin, rightMargin);
		Border bdrPanel = BorderFactory.createTitledBorder("Target Information");
		this.setBorder(BorderFactory.createCompoundBorder(bdrMargin, bdrPanel));
		
		// Use Spring layout as we are designing a Form.
		this.setLayout(new SpringLayout());
		
		// Create child controls for this panel.
		for (int i = 0; i < labelCaptions.length; i++) {
			lblCaption = new JLabel(labelCaptions[i], JLabel.TRAILING);
			this.add(lblCaption);
			
			if (i == 0) {
			// "Target board:"
				cboBoards = new JComboBox(targetBoards);
				lblCaption.setLabelFor(cboBoards);
				this.add(cboBoards);
			}
			else
			{
				txtQFile = new JTextField(50);
				txtQFile.addFocusListener(this);
				txtQFile.getDocument().addDocumentListener(this);
				lblCaption.setLabelFor(txtQFile);
				this.add(txtQFile);
				
				/*	Position is important
					We must initialize txtQBitFile, txtQBmmFile, txtQHexFile AFTER we have
					instantiated txtQFile and "add"ed it.
				*/
				switch (i)
				{
				case 1:			// "Target .bit file:"
					txtQBitFile = txtQFile;
					break;
				case 2:			// "Target board name:"
					txtBoardName = txtQFile;
					break;					
				case 3:			// "Target .bmm file:"
					txtQBmmFile = txtQFile;
					break;
				case 4:			// "Program .hex file:"
					txtQHexFile = txtQFile;
					break;
				}
			}
			

			btnSelect = new JButton(buttonTexts[i]);
			
/*	------------------------------------------------------------------------------------
 *	Performance note: 
 *		When considering whether to use an inner class, keep in mind that application 
 *		startup time and memory footprint are typically directly proportional to the 
 *		number of classes you load. The more classes you create, the longer your program 
 *		takes to start up and the more memory it will take. As an application developer 
 *		you have to balance this with other design constraints you may have. 
 *		We are not suggesting you turn your application into a single monolithic class in 
 *		hopes of cutting down startup time and memory footprint this would lead to 
 *		unnecessary headaches and maintenance burdens. 
 *	------------------------------------------------------------------------------------ */

			/* 	To reduce the number of classes, we are NOT going to create separate classes, 
				be it anonymous, for handling ActionListeners for each JButton. Instead,
				we implement ActionListener as part of this JPanel. To identify which
				JButton generated ActionEvent, we need to set Action Command for each
				JButton explicitly.
				Note: 3 of the JButtons have same text - "Select...". So, the default 
					  Action Command for those JButtons cannot be used to differentiate
					  between them.
			 */
			btnSelect.setActionCommand(buttonActions[i]);
			btnSelect.addActionListener(this);
			this.add(btnSelect);
			if (buttonTexts[i].contains("None"))
				btnSelect.hide();;
		}

		/*	If program is running in Simple mode, then there won't be any Operations panel.
 			But user need be given choice to select Write to target and a button to burn
 			bit files. So, add the Write to Combobox and [Do Selected Operations] JButton
 			in Target panel itself.
		 */
		if (bSimpleMode)
		{
			lblCaption = new JLabel("Write to:", JLabel.TRAILING);
			this.add(lblCaption);

			cboWriteTargets = new JComboBox(WriteTargets.values());
			this.add(cboWriteTargets);
			exceptionCellIndex = 13;
			
			btnProceed = new JButton("Run");
			
			// Associate ActionEvent listener with [Run] button.
			btnProceed.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e)
				{
					WriteTargets selTarget = (WriteTargets) cboWriteTargets.getSelectedItem();
					String currActionCommand = e.getActionCommand();

					if (currActionCommand.equals("Run"))
						parentFrame.BurnUsingProgrammer(selTarget);
				}
			});

			/*	Associate PropertyChangeEvent listener to [Run] button so as to inform 
				PapilioLoader class that [Run] button has become enabled - to signal the 
				fact that burning operation is over. This notification is only needed when 
				program is started with -x switch. 
			*/ 
			btnProceed.addPropertyChangeListener("enabled", new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent propChangeEvent)
				{
					if (((Boolean) propChangeEvent.getNewValue()).booleanValue())
						parentFrame.BurningOver();
				}
			});
			
			this.add(btnProceed);
		}

		// Associate ActionEvent listener with [Run] button.
		cboBoards.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				
				
				if (cboBoards.getSelectedItem().toString().equals("Papilio DUO"))
					txtBoardName.setText("Papilio DUO A");
				else if (cboBoards.getSelectedItem().toString().equals("Papilio One or Papilio Pro"))
					txtBoardName.setText("Dual RS232 A");
				else
					txtBoardName.setText("");

			}
		});		
		
        // Lay out the panel.
        HelperFunctions.makeCompactGrid(this,
                                        labelCaptions.length + (bSimpleMode ? 1 : 0),	//rows 
                                        3, //cols
                                        3, 3,        //initX, initY
                                        6, 6,       //xPad, yPad
                                        exceptionCellIndex);
        
        /* 	If we set Preferred Size for this JPanel - which has SpringLayout, in 
        	particular height, then it just screws up the height of JTextField, 
        	JButton and JComboBox even during the initial display of application
        	window. During the resizing of application window, situation becomes
        	worse further.
        	So, do NOT uncomment following line.
        */
//		this.setPreferredSize(new Dimension(width, height));
	}


	@Override
	public void actionPerformed(ActionEvent e)
	{
		JTextField txtQFile = txtQBitFile;
		File selFile, suggestedBmmFile;
		String sQFile, sQSuggestedBmmFile, currExtension = e.getActionCommand();
		int retval, i = 0;
		
		if (currExtension.equals(buttonActions[0])) {
		// Action Command = "Display Info"
			//parentFrame.BurnUsingProgrammer(selTarget);
			parentFrame.Erase();
			//parentFrame.BurnUsingProgrammer(selTarget);
		}
		else 
		{
			if (currExtension.equals(buttonActions[1])) {
			// Action Command = ".bit"
				txtQFile = txtQBitFile;
				i = 1;
			}
			else if (currExtension.equals(buttonActions[2])) {
			// Action Command = ".bmm"
				txtQFile = txtQBmmFile;
				i = 2;
			}
			else if (currExtension.equals(buttonActions[3])) {
			// Action Command = ".hex"
				txtQFile = txtQHexFile;
				i = 3;
			}

			commonDialog.resetChoosableFileFilters();	// Initialization
			commonDialog.setAcceptAllFileFilterUsed(false);
			commonDialog.setFileFilter(progFileFilters[i]);
			commonDialog.setDialogTitle("Select " + labelCaptions[i]);
			
/*	------------------------------------------------------------------------------------
 * 	JFileChooser.setCurrentDirectory(File dir)
 * 		Sets the current directory. Passing in null sets the file chooser to point to the 
 * 		user's default directory. This default depends on the operating system. It is 
 * 		typically the "My Documents" folder on Windows, and the user's home directory on Unix. 
 * 		If the file passed in as currentDirectory is not a directory, the parent of the file 
 * 		will be used as the currentDirectory. If the parent is not traversable, then it will 
 * 		walk up the parent tree until it finds a traversable directory, or hits the root of 
 * 		the file system. 
 *	------------------------------------------------------------------------------------ */
			
			sQFile = txtQFile.getText().trim();
			selFile = new File(sQFile);
			if (sQFile.isEmpty()) {
			// => Target XXX file text box is blank
				// Make file chooser point to the user's default directory.
//				commonDialog.setCurrentDirectory(null);
				// Clear file selected on previous invocation, if any, of file chooser.
				// Calling .setSelectedFile(null) will NOT clear previously selected file.
				commonDialog.setSelectedFile(new File(""));
			}
			else if (selFile.isFile()) {
			// => Target XXX file text box contains a valid file.
				sQFile = HelperFunctions.CanonicalPath(selFile);
				if (sQFile.endsWith(currExtension))
				// => The file contained in target XXX file text box has correct extension.
					// Make sure it is selected in file chooser as default with correct
					// working directory. (No need to call setCurrentDirectory() explicitly.)
					commonDialog.setSelectedFile(selFile);
			}
			else {
//				commonDialog.setCurrentDirectory(null);
				// Clear file selected on previous invocation, if any, of file chooser.
				// Calling .setSelectedFile(null) will NOT clear previously selected file.
				commonDialog.setSelectedFile(new File(""));
			}
			
			retval = commonDialog.showDialog(this, "Select");

			if (retval == JFileChooser.APPROVE_OPTION)
			{
				selFile = commonDialog.getSelectedFile();
				if (!selFile.isFile()) {
					JOptionPane.showMessageDialog(getTopLevelAncestor(), 
									"The file specified " + selFile.getPath() + " is invalid and does not exist on disk. " + 
									"Please specify a valid file.", 
									"File Not Found", 
									 JOptionPane.WARNING_MESSAGE);
/*	------------------------------------------------------------------------------------
 * 		The focus behavior of requestFocusInWindow() method can be implemented uniformly 
 * 		across platforms, and thus developers are strongly encouraged to use this method 
 * 		over requestFocus when possible. Code which relies on requestFocus may exhibit 
 * 		different focus behavior on different platforms. 
 *	------------------------------------------------------------------------------------ */
					txtQFile.requestFocusInWindow();
				}
				else {
					sQFile = HelperFunctions.CanonicalPath(selFile);
					if (!sQFile.isEmpty())
					{
						txtQFile.setText(sQFile);
//						if ((currExtension.equals(buttonActions[1])) && (txtQBmmFile.getText().trim().isEmpty())) {
//						// => Action Command = ".bit" and Target .bmm File text box is blank.
//// TODO: Better move .bmm file code to DocumentListener - fired when contents of Target .bit File textbox change
//// Thus, the .bmm file will be suggested even when user changes / edits contents of Target .bit File textbox.
//							sQSuggestedBmmFile = sQFile.substring(0, sQFile.lastIndexOf(currExtension)) + "_bd.bmm";
//							suggestedBmmFile = new File(sQSuggestedBmmFile);
//							if (suggestedBmmFile.isFile())
//								txtQBmmFile.setText(sQSuggestedBmmFile);
//						}
					}
				}
			}
			
		}
	}
	

	public boolean isBlank() {
		return ((cboBoards.getSelectedItem().toString().equals(AUTO_DETECT_FPGA)) && 
				(txtQBitFile.getText().trim().isEmpty()) && 
				(txtQBmmFile.getText().trim().isEmpty()) && 
				(txtQHexFile.getText().trim().isEmpty()));		
	}
	
	public boolean isNotDirty(Properties ppjProject) {
		return ((cboBoards.getSelectedItem().toString().equals(ppjProject.getProperty(PPJProject.Board.toString()))) && 
				(txtQBitFile.getText().trim().equals(ppjProject.getProperty(PPJProject.BitFile.toString()))) && 
				(txtQBmmFile.getText().trim().equals(ppjProject.getProperty(PPJProject.BmmFile.toString()))) && 
				(txtQHexFile.getText().trim().equals(ppjProject.getProperty(PPJProject.HexFile.toString()))));
	}
	
	public boolean OperationNotDirty(Properties ppjProject) {
		return (LastOperations.Enum4WriteTarget((WriteTargets) cboWriteTargets.getSelectedItem()).toString().equals(ppjProject.getProperty(PPJProject.Operations.toString())));
	}
	
	
	public void PopulateBoard(String selBoard) {
		cboBoards.setSelectedItem(selBoard);
	}
	
	public void SaveCurrentBoard(Properties ppjProject) {
		ppjProject.setProperty(PPJProject.Board.toString(), cboBoards.getSelectedItem().toString());
	}

	public void PopulateLastFiles(String lastBitFile, String lastBmmFile, String lastHexFile) {
		txtQBitFile.setText(lastBitFile);
		txtQBmmFile.setText(lastBmmFile);
		txtQHexFile.setText(lastHexFile);
	}
	
	public void StoreLastFiles(Properties settings) {
		settings.setProperty("LastBitFile", txtQBitFile.getText().trim());
		if (!bSimpleMode){
			settings.setProperty("LastBmmFile", txtQBmmFile.getText().trim());
			settings.setProperty("LastHexFile", txtQHexFile.getText().trim());
		}
	}

	public void SaveCurrentFiles(Properties ppjProject) {
		ppjProject.setProperty(PPJProject.BitFile.toString(), txtQBitFile.getText().trim());
		if (!bSimpleMode){
			ppjProject.setProperty(PPJProject.BmmFile.toString(), txtQBmmFile.getText().trim());
			ppjProject.setProperty(PPJProject.HexFile.toString(), txtQHexFile.getText().trim());
		}
	}

	public void PopulateLastWriteto(String operationsList)
	{
		EnumSet<LastOperations> estPrevOperations = LastOperations.DecodePreferences(operationsList);
		
		if (estPrevOperations.contains(LastOperations.WRITE_TO_FPGA))
			cboWriteTargets.setSelectedItem(WriteTargets.FPGA);
		else if (estPrevOperations.contains(LastOperations.WRITE_TO_SPI_FLASH))
			cboWriteTargets.setSelectedItem(WriteTargets.SPI_FLASH);
		else if (estPrevOperations.contains(LastOperations.WRITE_TO_DISK_FILE))
			cboWriteTargets.setSelectedItem(WriteTargets.DISK_FILE);
	}

	public void StoreLastWriteto(Properties settings) {
		settings.setProperty("LastOperations", 
							  LastOperations.Enum4WriteTarget((WriteTargets) cboWriteTargets.getSelectedItem()).toString());
	}

	public void SaveCurrentWriteto(Properties ppjProject) {
		ppjProject.setProperty(PPJProject.Operations.toString(), 
				  			   LastOperations.Enum4WriteTarget((WriteTargets) cboWriteTargets.getSelectedItem()).toString());
	}


	@Override
	public void changedUpdate(DocumentEvent e)
	{
	/*	This event will be fired when contents of Target XXX file textboxes will change
	 *	stylistically. In other words, this event fires as a result of attribute change
	 *	rather than content change.  
	 */
		// Do nothing.
	}

	@Override
	public void insertUpdate(DocumentEvent e)
	{
		/* 	In case program is running in Simple mode, Operations panel is not there
			at all. So, there is no question of selecting / unselecting JToggleButtons
			to depict operation flow.
		 */
		if (bSimpleMode) 
			return;
		
		parentFrame.SyncOperationButtons(txtQBitFile.getText().trim(), 
										 txtQBmmFile.getText().trim(), 
										 txtQHexFile.getText().trim());
	}

	@Override
	public void removeUpdate(DocumentEvent e)
	{
		/* 	In case program is running in Simple mode, Operations panel is not there
		at all. So, there is no question of selecting / unselecting JToggleButtons
		to depict operation flow.
		 */
		if (bSimpleMode) 
			return;

		parentFrame.SyncOperationButtons(txtQBitFile.getText().trim(), 
				 						 txtQBmmFile.getText().trim(), 
				 						 txtQHexFile.getText().trim());
	}
	

	@Override
	public void focusGained(FocusEvent e)
	{
		// Select all text in target file JTextField when it receives input focus. 
		JTextField txtQFile = (JTextField) e.getSource();
		txtQFile.select(0, txtQFile.getText().length());
	}

	@Override
	public void focusLost(FocusEvent e)
	{
	}

	
	/*	Java ALWAYS uses call by value. Passing a parameter as call by reference is 
	 * 	impossible in Java. In particular, this means that object references are 
	 * 	passed by value. This, in effect, means that a method cannot make an object
	 * 	parameter refer to a new object.
	 * 
	 * 	We wish to return the File objects corresponding to valid path and file names
	 * 	present in txtQBitFile, txtQBmmFile and txtQHexFile JTextFields back to caller
	 * 	of this method. However, we cannot use parameters like
	 * 			File targetBitFile, File targetBmmFile, File programHexFile
	 * 	Because, if we use them and store corresponding references of new File()
	 * 	objects in targetBitFile, targetBmmFile and programHexFile parameters, it
	 * 	will buy us nothing. Because when this method finishes, the references to
	 * 	new File objects created won't be copied from method parameters to corresponding
	 * 	actual arguments (variables) used to invoke this method. In fact, actual 
	 * 	arguments (variables) won't be "touched" at all. And as method has finished,
	 * 	the parameters will go out of scope and new File objects, not having any 
	 * 	references pointing to them, will be marked for garbage collection.
	 * 
	 * 	The only way to return the new File objects outside is to pass in a List<File>
	 * 	object, containing just 1 element (already), as parameters. This method can
	 * 	simply .set 0th element to appropriate (new) File object for each parameter.
	 * 	Back in caller of this method, it can simply .get the 0th element. Because
	 * 	this is just a case of this method changing the state of already instantiated
	 * 	List<File> objects, it will work happily.   
	 */
	public boolean DataValid(List<File> targetBitFile, 
							 List<File> targetBmmFile, 
							 List<File> programHexFile, 
							 boolean scanSelected, boolean mergeSelected, 
							 boolean eraseSelected, boolean writeSelected, boolean verifySelected)
	{
		String sQBitFile = txtQBitFile.getText().trim();
		String sQBmmFile = txtQBmmFile.getText().trim();
		String sQHexFile = txtQHexFile.getText().trim();
		boolean noFileSpecified = (sQBitFile.isEmpty() && sQBmmFile.isEmpty() && sQHexFile.isEmpty());
		boolean noOprSelected = ((scanSelected || mergeSelected || eraseSelected || 
								  writeSelected || verifySelected) == false);

		targetBitFile.set(0, new File(sQBitFile));
		targetBmmFile.set(0, new File(sQBmmFile));
		programHexFile.set(0, new File(sQHexFile));

/*	------------------------------------------------------------------------------------
 * 	requestFocusInWindow()
 * 		The focus behavior of requestFocusInWindow() method can be implemented uniformly 
 * 		across platforms, and thus developers are strongly encouraged to use this method 
 * 		over requestFocus when possible. Code which relies on requestFocus may exhibit 
 * 		different focus behavior on different platforms. 
 *	------------------------------------------------------------------------------------ */

		if (noFileSpecified)
		// => All 3 Target XXX file textboxes are blank.
		{
			if (noOprSelected) {
			// => All the JToggleButtons in Operation panel are off.
				JOptionPane.showMessageDialog(getTopLevelAncestor(), 
								"Please select at least one operation to proceed.", 
								"No Operation Specified", 
								 JOptionPane.WARNING_MESSAGE);
				txtQBitFile.requestFocusInWindow();
				return false;
			}
			else
			// => At least one JToggleButton in Operation panel is selected.
			{
				if (writeSelected) {
				// => The [Write to] is selected even though all 3 textboxes are blank.
					JOptionPane.showMessageDialog(getTopLevelAncestor(), 
									"Please enter a valid Bit file. Bit file cannot be kept blank.", 
									"Blank Bit File", 
									 JOptionPane.WARNING_MESSAGE);
					txtQBitFile.requestFocusInWindow();
					return false;
				}
				// GIRISH: Selected state of [Scan] and/or [Erase] should not matter. Correct?
				if (mergeSelected) {
				// => The [Merge] is selected even though all 3 textboxes are blank.
					JOptionPane.showMessageDialog(getTopLevelAncestor(), 
									"Please enter a valid Bit file and user Hex file. No files can be kept blank.", 
									"No File Specified", 
									 JOptionPane.WARNING_MESSAGE);
					txtQBitFile.requestFocusInWindow();
					return false;
				}

				/* At this point, even though all 3 textboxes are blank, the state of JToggleButtons
				   in Operations panel is valid. Overall, this "input" is valid. Signal this fact
				   to caller by setting 3 File objects to null and returning true.
				 */
				return true;
			}
		}

		// At this point, at least one of Target XXX file textboxes is not-blank.
		
		/*  */
		if ((writeSelected == false) && (mergeSelected == false)) {
		// => [Merge] AND [Write to] are NOT selected even though user is specifying file(s),
			return true;
		}

		// At this point, user has specified at least one file and [Write to]  is selected.

		if (writeSelected == false) {
		// => The [Write to] is NOT selected even though user is specifying file(s)
			JOptionPane.showMessageDialog(getTopLevelAncestor(), 
							"Please ensure that Write to operation is selected.", 
							"Blank Write Operation", 
							 JOptionPane.WARNING_MESSAGE);
			txtQBitFile.requestFocusInWindow();
			return false;
		}

		if (!targetBitFile.get(0).isFile()) {
		// Invalid Bit File  
			JOptionPane.showMessageDialog(getTopLevelAncestor(), 
							"The specified Bit file " + sQBitFile + " is invalid and does not exist on disk. " + 
							"Please enter a valid Bit file.", 
							"Bit File Not Found", 
							 JOptionPane.WARNING_MESSAGE);
			txtQBitFile.requestFocusInWindow();
			return false;
		}
		// TODO: Check whether extension of the file specified in Target .bit file text box is .bit.

		if (sQBmmFile.isEmpty()) {
			if (sQHexFile.isEmpty()) {
			// => Both target .bmm file and program .hex file, are blank.
				if (mergeSelected) {
				// Since [Merge] is selected, above condition is invalid.
					JOptionPane.showMessageDialog(getTopLevelAncestor(), 
							"Please enter both valid Bmm file and Hex file. " + 
							"Bmm file and Hex file cannot be kept blank.", 
							"Blank Bmm and Hex Files", 
							 JOptionPane.WARNING_MESSAGE);
					txtQBmmFile.requestFocusInWindow();
					return false;
				}
				else
					return true;
			}
			else {
			// => target .bmm file is blank and program .hex file is specified 
				if (mergeSelected) {
				// Above condition is invalid if [Merge] is selected.
					JOptionPane.showMessageDialog(getTopLevelAncestor(), 
							"Please enter a valid Bmm file. Bmm file cannot be kept blank.", 
							"Blank Bmm File", 
							 JOptionPane.WARNING_MESSAGE);
					txtQBmmFile.requestFocusInWindow();
					return false;
				}
				else
				// Even though above mentioned condition is invalid, user has not selected [Merge].
				// So, we can ignore specified .hex file.
					return true;
			}
		}
		else if (sQHexFile.isEmpty()) {
		// => target .bmm file is specified and program .hex file is blank
			if (mergeSelected) {
			// Above condition is invalid if [Merge] is selected.
				JOptionPane.showMessageDialog(getTopLevelAncestor(), 
						"Please enter a valid Hex file. Hex file cannot be kept blank.", 
						"Blank Hex File", 
						 JOptionPane.WARNING_MESSAGE);
				txtQHexFile.requestFocusInWindow();
				return false;
			}
			else
			// Even though above mentioned condition is invalid, user has not selected [Merge].
			// So, we can ignore specified .bmm file.
				return true;
		}
		
		// At this point, we are sure that user has specified both .bmm and .hex files.
		// It is implied that user has specified a valid .bit file.
		
		if (mergeSelected)
		{
			if (!targetBmmFile.get(0).isFile()) {
			// Invalid Bmm File
				JOptionPane.showMessageDialog(getTopLevelAncestor(), 
								"The specified Bmm file " + sQBmmFile + " is invalid and does not exist on disk. " + 
								"Please enter a valid Bit file.", 
								"Bmm File Not Found", 
								 JOptionPane.WARNING_MESSAGE);
				txtQBmmFile.requestFocusInWindow();
				return false;
			}
			// TODO: Check whether extension of the file specified in Target .bmm file text box is .bmm.
			else if (!programHexFile.get(0).isFile()) {
			// Invalid Hex File
				JOptionPane.showMessageDialog(getTopLevelAncestor(), 
								"The specified Hex file " + sQHexFile + " is invalid and does not exist on disk. " + 
								"Please enter a valid Bit file.", 
								"Hex File Not Found", 
								 JOptionPane.WARNING_MESSAGE);
				txtQHexFile.requestFocusInWindow();
				return false;
			}
			// TODO: Check whether extension of the file specified in Program .hex file text box is .hex.
		}
		// If [Merge] is not selected, then we will ignore contents of Target .bmm file textbox and 
		// Program .hex file textbox altogether.

		return true;
	}

	/* 	Values are set in scanSelected, mergeSelected and writeSelected as per the logic
		present in AsyncProgrammer.run() */
	public boolean SimpleModeDataValid(List<File> targetBitFile, 
									   List<File> targetBmmFile, 
									   List<File> programHexFile,  
									   boolean[] scanSelected, 
									   boolean[] mergeSelected, 
									   boolean[] writeSelected)
	{
		String sQBitFile = txtQBitFile.getText().trim();
		//String sQBmmFile = txtQBmmFile.getText().trim();
		//String sQHexFile = txtQHexFile.getText().trim();
		//boolean noFileSpecified = (sQBitFile.isEmpty() && sQBmmFile.isEmpty() && sQHexFile.isEmpty());
		boolean noFileSpecified = (sQBitFile.isEmpty());

		targetBitFile.set(0, new File(sQBitFile));
		//targetBmmFile.set(0, new File(sQBmmFile));
		//programHexFile.set(0, new File(sQHexFile));
		
		if (noFileSpecified) {
		// => All 3 Target XXX file textboxes are blank.
			scanSelected[0] = true;
			mergeSelected[0] = false;	// extraneous 
			writeSelected[0] = false;
		}
		else
		{
			scanSelected[0] = false;		// extraneous 
			writeSelected[0] = true;
			if (!targetBitFile.get(0).isFile()) {
			// Invalid Bit File  
				JOptionPane.showMessageDialog(getTopLevelAncestor(), 
								"The specified Bit file " + sQBitFile + " is invalid and does not exist on disk. " + 
								"Please enter a valid Bit file.", 
								"Bit File Not Found", 
								 JOptionPane.WARNING_MESSAGE);
				txtQBitFile.requestFocusInWindow();
				return false;
			}
			// TODO: Check whether extension of the file specified in Target .bit file text box is .bit.

//			if (sQBmmFile.isEmpty()) {
//				if (sQHexFile.isEmpty()) {
//				// => Both target .bmm file and program .hex file, are blank.
//					mergeSelected[0] = false;
//					return true;
//				}
//				else {
//				// => target .bmm file is blank and program .hex file is specified 
//					JOptionPane.showMessageDialog(getTopLevelAncestor(), 
//							"Please enter a valid Bmm file. Bmm file cannot be kept blank.", 
//							"Blank Bmm File", 
//							 JOptionPane.WARNING_MESSAGE);
//					txtQBmmFile.requestFocusInWindow();
//					return false;
//				}
//			}
//			else if (sQHexFile.isEmpty()) {
//			// => target .bmm file is specified and program .hex file is blank
//				JOptionPane.showMessageDialog(getTopLevelAncestor(), 
//						"Please enter a valid Hex file. Hex file cannot be kept blank.", 
//						"Blank Hex File", 
//						 JOptionPane.WARNING_MESSAGE);
//				txtQHexFile.requestFocusInWindow();
//				return false;
//			}
			
			// At this point, we are sure that user has specified both .bmm and .hex files.
			// It is implied that user has specified a valid .bit file.

//			mergeSelected[0] = true;
//			if (!targetBmmFile.get(0).isFile()) {
//			// Invalid Bmm File
//				JOptionPane.showMessageDialog(getTopLevelAncestor(), 
//								"The specified Bmm file " + sQBmmFile + " is invalid and does not exist on disk. " + 
//								"Please enter a valid Bit file.", 
//								"Bmm File Not Found", 
//								 JOptionPane.WARNING_MESSAGE);
//				txtQBmmFile.requestFocusInWindow();
//				return false;
//			}
//			// TODO: Check whether extension of the file specified in Target .bmm file text box is .bmm.
//			else if (!programHexFile.get(0).isFile()) {
//			// Invalid Hex File
//				JOptionPane.showMessageDialog(getTopLevelAncestor(), 
//								"The specified Hex file " + sQHexFile + " is invalid and does not exist on disk. " + 
//								"Please enter a valid Bit file.", 
//								"Hex File Not Found", 
//								 JOptionPane.WARNING_MESSAGE);
//				txtQHexFile.requestFocusInWindow();
//				return false;
//			}
//			// TODO: Check whether extension of the file specified in Program .hex file text box is .hex.
		}
		
		return true;
	}
	
}
