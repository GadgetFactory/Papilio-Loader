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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EnumSet;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.border.Border;
import net.gadgetfactory.papilio.loader.LoaderProject.PPJProject;

public class OperationPanel extends JPanel implements ItemListener
{
	private PapilioLoader parentFrame;

	private JCheckBox btnScan = new JCheckBox("Scan");
	private JCheckBox btnMerge = new JCheckBox("Merge");
	private JCheckBox btnErase = new JCheckBox("Erase");
	private JCheckBox btnWrite = new JCheckBox("Write to");
	private JCheckBox btnVerify = new JCheckBox("Verify");
	private Icon imgRightArrow = new ImageIcon("images/right_arrow.png");
/*	------------------------------------------------------------------------------------
 * 	NOTE:	When you specify a filename or URL to an ImageIcon constructor, processing is 
 * 			BLOCKED until after the image data is completely loaded or the data location 
 * 			has proven to be invalid. If the data location is invalid (but non-null), an 
 * 			ImageIcon is still successfully created; it just has no size and, therefore, 
 * 			paints nothing.   
 *	------------------------------------------------------------------------------------ */

	private JComboBox cboWriteTargets = new JComboBox(WriteTargets.values());
	private JButton btnProceed = new JButton("Do Selected Operations");

	public JButton getProceedButton() {
		return this.btnProceed;
	}
	
	public void setWriteTarget(WriteTargets newTarget) {
		cboWriteTargets.setSelectedItem(newTarget);
	}

	public boolean isScanSelected() {
		return btnScan.isSelected();
	}
	public boolean isMergeSelected() {
		return btnMerge.isSelected();
	}
	public boolean isEraseSelected() {
		return btnErase.isSelected();
	}
	public boolean isWriteSelected() {
		return btnWrite.isSelected();
	}
	public boolean isVerifySelected() {
		return btnVerify.isSelected();
	}


	public OperationPanel(PapilioLoader plframe, 
						  int topMargin, int rightMargin, int bottomMargin, int leftMargin) {
		final double BUTTON_WEIGHT_X = 1.0;
		final double ARROW_WEIGHT_X = 0.5;
		final double IDENTITY_WEIGHT = 1.0;
		
		parentFrame = plframe;

		JLabel lblScanArrow = new JLabel(imgRightArrow);
		JLabel lblMergeArrow = new JLabel(imgRightArrow);
		JLabel lblEraseArrow = new JLabel(imgRightArrow);
		JLabel lblWriteArrow = new JLabel(imgRightArrow);
		Insets emptyInsets = new Insets(0, 0, 0, 0);

		// Create margin and Titled border around this panel.
		Border bdrMargin = BorderFactory.createEmptyBorder(topMargin,
				leftMargin, bottomMargin, rightMargin);
		Border bdrPanel = BorderFactory.createTitledBorder("Operations");
		this.setBorder(BorderFactory.createCompoundBorder(bdrMargin, bdrPanel));

		/*
		 * We cannot use FlowLayout here as we want the buttons [Scan], [Merge], [Erase], ... 
		 * (with direction arrow separators in them) one after the other on the SAME row. 
		 * FlowLayout would "wrap" them over subsequent rows if width of the JFrame is made 
		 * smaller. BoxLayout is out of question since we clearly want at least 2 rows and within 
		 * first row, we want the button stacked one after the other horizontally. Thus, we
		 * want stacking in both horizontal and vertical direction. So, we use GridBagLayout.
		 */
		this.setLayout(new GridBagLayout());

		// It does not matter the order in which you place components for GridBagLayout.
		// GridBagConstraints.HORIZONTAL implies natural height, maximum width.

		// Row Index 0
		// [Scan] toggle button (Column 0)
		this.add(btnScan, new GridBagConstraints(0, 0, 1, 1, BUTTON_WEIGHT_X,
				IDENTITY_WEIGHT, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, emptyInsets, 0, 0));
		// [->] arrow image (Column 1)
		this.add(lblScanArrow, new GridBagConstraints(1, 0, 1, 1,
				ARROW_WEIGHT_X, IDENTITY_WEIGHT, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, emptyInsets, 0, 0));
		// [Merge] toggle button (Column 2)
		this.add(btnMerge, new GridBagConstraints(2, 0, 1, 1, BUTTON_WEIGHT_X,
				IDENTITY_WEIGHT, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, emptyInsets, 0, 0));
		// [->] arrow image (Column 3)
		this.add(lblMergeArrow, new GridBagConstraints(3, 0, 1, 1,
				ARROW_WEIGHT_X, IDENTITY_WEIGHT, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, emptyInsets, 0, 0));
		// [Erase] toggle button (Column 4)
		this.add(btnErase, new GridBagConstraints(4, 0, 1, 1, BUTTON_WEIGHT_X,
				IDENTITY_WEIGHT, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, emptyInsets, 0, 0));
		// [->] arrow image (Column 5)
		this.add(lblEraseArrow, new GridBagConstraints(5, 0, 1, 1,
				ARROW_WEIGHT_X, IDENTITY_WEIGHT, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, emptyInsets, 0, 0));
		// [Write to] toggle button (Column 6)
		this.add(btnWrite, new GridBagConstraints(6, 0, 1, 1, BUTTON_WEIGHT_X,
				IDENTITY_WEIGHT, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, emptyInsets, 0, 0));
		// [->] arrow image (Column 7)
		this.add(lblWriteArrow, new GridBagConstraints(7, 0, 1, 1,
				ARROW_WEIGHT_X, IDENTITY_WEIGHT, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, emptyInsets, 0, 0));
		// [Verify] toggle button (Column 8)
		this.add(btnVerify, new GridBagConstraints(8, 0, 1, 1, BUTTON_WEIGHT_X,
				IDENTITY_WEIGHT, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, emptyInsets, 0, 0));

		// Row 1 Column 6, Write targets Combobox.
		this.add(cboWriteTargets, new GridBagConstraints(6, 1, 1, 1,
				IDENTITY_WEIGHT, IDENTITY_WEIGHT, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, emptyInsets, 0, 0));

		// Row 2 Column 6, ColSpan = 3
		// [Do Selected Operations] button
		this.add(btnProceed, new GridBagConstraints(6, 2, 3, 1,
				IDENTITY_WEIGHT, IDENTITY_WEIGHT, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(7, 0, 4, 0), 0, 0));

		this.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

		// Define ActionListener for [Do Selected Operation] button.
		ActionListener proceedButtonClick = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				WriteTargets selTarget = (WriteTargets) cboWriteTargets.getSelectedItem();
				String currActionCommand = e.getActionCommand();

				if (currActionCommand.equals("Do Selected Operations"))
					parentFrame.BurnUsingProgrammer(selTarget);
			}
		};
		// Associate ActionEvent listener with [Do Selected Operation] button.
		btnProceed.addActionListener(proceedButtonClick);

		/*	Associate PropertyChangeEvent listener to [Do Selected Operation] button
			so as to inform PapilioLoader class that [Do Selected Operation] button
			has become enabled - to signal the fact that burning operation is over.
			This notification is only needed when program is started with -x switch. 
		*/ 
		btnProceed.addPropertyChangeListener("enabled", new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent propChangeEvent)
			{
				Boolean enabledState = (Boolean) propChangeEvent.getNewValue();
				if (enabledState.booleanValue())
					parentFrame.BurningOver();
			}
		});
		
		// Associate ItemEvent listener for JCheckBox.
		btnWrite.addItemListener(this);
		/*
		 * To hook to "ItemChange" event for Combobox and find out the selected element in 
		 * that event, it is better to listen to ItemEvent rather than ActionEvent.
		 */
		// Associate ItemEvent listener to Write to Combobox.
		cboWriteTargets.addItemListener(this);
	}


	@Override
	public void itemStateChanged(ItemEvent e)
	{
	/*
	 * The itemStateChanged will be called by JVM as a result of - 
	 * 		a) Change in the state of JCheckBox(es) from on <--> off. 
	 * 		b) Change in the selected item of cboWriteTargets JComboBox. 
	 * However, in case of 
	 * 		a) e.getItem() returns a reference to the actual JCheckBox 
	 * 		   which fired the event. 
	 * 		b) e.getItem() returns the item being selected / deselected in 
	 * 		   JComboBox and NOT a reference to cboWriteTargets JComboBox.
	 */

		if (e.getSource() instanceof JCheckBox) {
			JCheckBox btnOperation = (JCheckBox) e.getItem();

			if (btnOperation == btnWrite) {
				cboWriteTargets.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
				// It is better not to link state of [Verify] with state of [Write to].
				// This is because even if [Write to] is off, [Verify] can well be on. 
			}
		}
		else if (e.getSource() instanceof JComboBox) {
		// At this point, we are sure that this "ItemChange" is getting fired for cboWriteTargets.
			if (e.getStateChange() == ItemEvent.SELECTED) {
			/*
			 * The itemStateChanged will be called twice - once for item which is getting 
			 * deselected and next for the item which is getting selected. We are interested 
			 * in the event only when item is getting selected.
			 */

				WriteTargets selTarget = (WriteTargets) e.getItem();
			}
		}
	}

	
	public boolean OperationsNotDirty(Properties ppjProject) {
		return (CurrOperationsList().equals(
						ppjProject.getProperty(PPJProject.Operations.toString())));
	}

	
	public void PopulateLastOperations(String operationsList)
	{
		EnumSet<LastOperations> estPrevOperations = LastOperations.DecodePreferences(operationsList);
		
		if (estPrevOperations.contains(LastOperations.SCAN))
			btnScan.setSelected(true);
		if (estPrevOperations.contains(LastOperations.MERGE))
			btnMerge.setSelected(true);
		if (estPrevOperations.contains(LastOperations.ERASE))
			btnErase.setSelected(true);
		if (estPrevOperations.contains(LastOperations.VERIFY))
			btnVerify.setSelected(true);
		
		if (estPrevOperations.contains(LastOperations.WRITE_TO_FPGA)) {
			btnWrite.setSelected(true);
			cboWriteTargets.setSelectedItem(WriteTargets.FPGA);
		}
		else if (estPrevOperations.contains(LastOperations.WRITE_TO_SPI_FLASH)) {
			btnWrite.setSelected(true);
			cboWriteTargets.setSelectedItem(WriteTargets.SPI_FLASH);
		}
		else if (estPrevOperations.contains(LastOperations.WRITE_TO_DISK_FILE)) {
			btnWrite.setSelected(true);
			cboWriteTargets.setSelectedItem(WriteTargets.DISK_FILE);
		}
	}
	
	public void StoreLastOperations(Properties settings, String propertyName)
	{
		settings.setProperty(propertyName, CurrOperationsList());
	}
	
	private String CurrOperationsList()
	{
		String operationsList = "";

		if (btnScan.isSelected())
			operationsList += LastOperations.SCAN.toString() + ", ";
		if (btnMerge.isSelected())
			operationsList += LastOperations.MERGE.toString() + ", ";
		if (btnErase.isSelected())
			operationsList += LastOperations.ERASE.toString() + ", ";
		if (btnVerify.isSelected())
			operationsList += LastOperations.VERIFY.toString() + ", ";
		
		if (btnWrite.isSelected())
			operationsList += 
					LastOperations.Enum4WriteTarget(
							(WriteTargets) cboWriteTargets.getSelectedItem()).toString() + ", ";
		
		// Remove trailing ", ".
		if (operationsList.endsWith(", "))
			operationsList = operationsList.substring(0, operationsList.length() - ", ".length());
		
		return operationsList;
	}

	public void setEraseButton(boolean erase) {
		btnErase.setSelected(erase);
	}
	
	/**
	 * Assumption: Trim() has already been applied to each of the arguments.
	 * 
	 * @param editedTargetBitFile xx
	 * @param editedTargetBmmFile xx
	 * @param editedProgramHexFile xx
	 */
	public void DisplayOperationFlow(String editedTargetBitFile,
			String editedTargetBmmFile, String editedProgramHexFile)
	{
		/* 	If we check whether contents of textboxes refer to non-existent, invalid files and try 
			to adjust the operation flow based on that, it will be overkill. So it is best to 
			consider contents of Target XXX file textboxes as blank or non-blank and adjust the
			state of JCheckBoxes to display suggested flow.
		 */

		if (editedTargetBitFile.isEmpty()) {
			if ((editedTargetBmmFile.isEmpty()) && (editedProgramHexFile.isEmpty()))
			// => All 3 Target XXX file textboxes are blank.
			{
				// [Scan] operation should be on.
				btnScan.setSelected(true);
				// [Merge] operation is, of course, N.A., so deselect it.
				btnMerge.setSelected(false);
				// [Erase] operation may or may not be required. Don't hard-code its
				// state here. Rather keep its previous state.

/*	------------------------------------------------------------------------------------
 *  JCheckBox.setSelected(b) 
 *  	Sets the state of the button. Note that this method does not trigger
 * 		an actionEvent. Call doClick to perform a programatic action change.
 *	------------------------------------------------------------------------------------ */

				// [Write to] operation are not applicable, so deselect them.
				btnWrite.setSelected(false);
				cboWriteTargets.setEnabled(false); // Disable cboWriteTargets JComboBox.
				// Don't hard-code [Verify] operation state here. Rather keep its previous state.
			}
			else
			/* => Target .bit file textbox is blank but either Target .bmm file textbox or 
			 * 	  Program .hex file textbox is blank or both are non-blank. This is invalid. */
			{
				// GIRISH:
			}
		}
		else {
			if ((editedTargetBmmFile.isEmpty())	&& (editedProgramHexFile.isEmpty()))
			/* => Target .bit file textbox is Not blank but both Target .bmm file textbox 
			 * 	  and Program .hex file textbox are blank. 
			 * => User has specified final .bit file. */
			{
				// [Scan] operation may or may not be required. Don't hard-code its
				// state here. Rather keep its previous state.
				// [Merge] operation is definitely not required, so deselect it.
				btnMerge.setSelected(false);
				btnErase.setSelected(true);

				// [Write to] operation is indeed applicable. Select it iff it is deselected.
				if (btnWrite.isSelected() == false) {
					btnWrite.setSelected(true);

					// As we are setting [Write to] which was previously off, it means
					// cboWriteTargets JComboBox was also disabled. As [Write to] is being
					// enabled, cboWriteTargets needs to be enabled also. Further, it is
					// preferable to set it to "SPI Flash" irrespective of its previous selection.
					cboWriteTargets.setEnabled(true);
					cboWriteTargets.setSelectedItem(WriteTargets.SPI_FLASH);

					btnVerify.setSelected(true); // better to select [Verify] as well
				}
			}
			else
			/* => Target .bit file textbox is Not blank but either Target .bmm file textbox or 
			 * 	  Program .hex file textbox is blank or both are non-blank. 
			 * => User has to merge .bit + .bmm + .hex files to create final .bit file. */
			{
				// [Scan] operation may or may not be required. Don't hard-code its
				// state here. Rather keep its previous state.
				btnMerge.setSelected(((!editedTargetBmmFile.isEmpty()) && 
									  (!editedProgramHexFile.isEmpty())));
				// [Erase] operation may or may not be required. Don't hard-code its
				// state here. Rather keep its previous state.
				// [Write to] operation is indeed applicable. Select it iff it is deselected.
				if (btnWrite.isSelected() == false) {
					btnWrite.setSelected(true);

					// As we are setting [Write to] which was previously off, it means
					// cboWriteTargets JComboBox was also disabled. As [Write to] is being
					// enabled, cboWriteTargets needs to be enabled also. Further, it is
					// preferable to set it to "SPI Flash" irrespective of its previous selection.
					cboWriteTargets.setEnabled(true);
//					cboWriteTargets.setSelectedItem(WriteTargets.SPI_FLASH);

					btnVerify.setSelected(true); // better to select [Verify] as well
				}
			}
		}

	}

}