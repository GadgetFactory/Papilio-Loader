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

import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;

public class HelperFunctions
{
	private static JFileChooser m_CommonDialog;
	private static Component m_Parent;

    public static void initVariables(JFileChooser commonDialog, Component parent) {
		HelperFunctions.m_CommonDialog = commonDialog;
		HelperFunctions.m_Parent = parent;
	}


	/**
     * A debugging utility that prints to stdout the component's
     * minimum, preferred, and maximum sizes.
     */
    public static void printSizes(Component c) {
        System.out.println("minimumSize = " + c.getMinimumSize());
        System.out.println("preferredSize = " + c.getPreferredSize());
        System.out.println("maximumSize = " + c.getMaximumSize());
    }

    /**
     * Aligns the first <code>rows</code> * <code>cols</code>
     * components of <code>parent</code> in
     * a grid. Each component in a column is as wide as the maximum
     * preferred width of the components in that column;
     * height is similarly determined for each row.
     * The parent is made just big enough to fit them all.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @param initialX x location to start the grid at
     * @param initialY y location to start the grid at
     * @param xPad x padding between cells
     * @param yPad y padding between cells
     */
    public static void makeCompactGrid(Container parent,
                                       int rows, int cols,
                                       int initialX, int initialY,
                                       int xPad, int yPad, 
                                       int exceptionCellIndex) {
        SpringLayout layout;
        try {
            layout = (SpringLayout)parent.getLayout();
        } catch (ClassCastException exc) {
            System.err.println("The first argument to makeCompactGrid must use SpringLayout.");
            return;
        }

        //Align all cells in each column and make them the same width.
        Spring x = Spring.constant(initialX);
        for (int c = 0; c < cols; c++) {
            Spring width = Spring.constant(0);
            for (int r = 0; r < rows; r++) {
                width = Spring.max(width,
                                   getConstraintsForCell(r, c, parent, cols).
                                       getWidth());
            }
            for (int r = 0; r < rows; r++) {
                SpringLayout.Constraints constraints =
                        getConstraintsForCell(r, c, parent, cols);
                constraints.setX(x);
                if ((r * cols + c) == exceptionCellIndex)
               	// Make exception
                	constraints.setWidth(
                			Spring.constant(constraints.getWidth().getPreferredValue() + 5));
                else
                	constraints.setWidth(width);
            }
            x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)));
        }

        //Align all cells in each row and make them the same height.
        Spring y = Spring.constant(initialY);
        for (int r = 0; r < rows; r++) {
            Spring height = Spring.constant(0);
            for (int c = 0; c < cols; c++) {
                height = Spring.max(height,
                                    getConstraintsForCell(r, c, parent, cols).
                                        getHeight());
            }
            for (int c = 0; c < cols; c++) {
                SpringLayout.Constraints constraints =
                        getConstraintsForCell(r, c, parent, cols);
                constraints.setY(y);
                constraints.setHeight(height);
            }
            y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)));
        }

        //Set the parent's size.
        SpringLayout.Constraints pCons = layout.getConstraints(parent);
        pCons.setConstraint(SpringLayout.SOUTH, y);
        pCons.setConstraint(SpringLayout.EAST, x);
    }

    /* Used by makeCompactGrid. */
    private static SpringLayout.Constraints getConstraintsForCell(
                                                int row, int col,
                                                Container parent,
                                                int cols) {
        SpringLayout layout = (SpringLayout) parent.getLayout();
        Component c = parent.getComponent(row * cols + col);
        return layout.getConstraints(c);
    }

    /**
     * Gets the canonical path of File object. Canonical path is somehow more
     * "real" than absolute path for the File object.  
     * @param anyFile File object to be operated on
     * @return 
     * 		"" in case of IOException
     * 		canonical path otherwise
     */
    public static String CanonicalPath(File anyFile)
	{
    	String sQFile = "";
		try {
/*	------------------------------------------------------------------------------------
 * 		Exactly what a canonical path is, and how it differs from an absolute path, 
 * 		is system-dependent, but it tends to mean that the path is somehow more real than 
 * 		the absolute path. Typically, if the full path contains aliases, shortcuts, shadows, 
 * 		or symbolic links of some kind, the canonical path resolves those aliases to the 
 * 		actual directories they refer to.
 *	------------------------------------------------------------------------------------ */
			sQFile = anyFile.getCanonicalPath();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
		
		return sQFile;
	}

    // Exactly like above except returns File rather than String.
    public static File CanonicalFile(File anyFile)
	{
    	File canonFile = null;
		try {
			canonFile = anyFile.getCanonicalFile();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
		
		return canonFile;
	}


    /**
     *	This uses Java 1.6's Desktop class. Unfortunately, Desktop.browse(URI uri) takes 
     *	years to complete execution.  
     */
    public static boolean JavaBrowseURL(final String sURL)
    {
    	final Desktop desktop;
    	
    	if (!Desktop.isDesktopSupported())
    		return false;
    	
    	// TODO: Prior to .getDesktop(), do we need to check for GraphicsEnvironment.isHeadless() as well?
    	desktop = Desktop.getDesktop();

/*	------------------------------------------------------------------------------------
 * 	Desktop.isSupported(Desktop.Action)
 * 		Even when the platform supports an action, a file or URI may not have a registered 
 * 		application for the action. For example, most of the platforms support the 
 * 		Desktop.Action.OPEN action. But for a specific file, there may not be an application 
 * 		registered to open it. In this case, isSupported(java.awt.Desktop.Action) may return 
 * 		true, but the corresponding action method will throw an IOException. 
 *	------------------------------------------------------------------------------------ */

        if (!desktop.isSupported(Desktop.Action.BROWSE))
        	return false;

        Thread thread = new Thread(new Runnable() {
			@Override
			public void run()
			{
		        try {
		        	URI uri = new URI(sURL);
		        	desktop.browse(uri);
		        } catch(IOException ioe) {
		            System.err.println(ioe.getMessage());
		        } catch(URISyntaxException use) {
		            System.err.println(use.getMessage());
		        } catch(Exception e) {
		            System.err.println(e.getMessage());
		        }
			}
		});
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();
        
        return true;
    }
    
    public static void BrowseURL(String sURL, boolean runningonWindows)
    {
	    Process process = null;
	    String[] commandLine = {"cmd.exe", "/C", "start", sURL};
	    String[] commandLineLinux = {"xdg-open", sURL};

	    try {
	    	if (runningonWindows) 
	    		process = Runtime.getRuntime().exec(commandLine, null, null);
	    	else
	    		process = Runtime.getRuntime().exec(commandLineLinux, null, null);
	    } catch (IOException e) {
	      System.err.println(e.getMessage());
	    }

/*	------------------------------------------------------------------------------------
 *  Class Process 
 *  	The created subprocess does not have its own terminal or console. All its standard io 
 *  	(i.e. stdin, stdout, stderr) operations will be redirected to the parent process through 
 *  	three streams (getOutputStream(), getInputStream(), getErrorStream()). The parent process 
 *  	uses these streams to feed input to and get output from the subprocess.
 *  	Because some native platforms only provide limited buffer size for standard input and 
 *  	output streams, FAILURE TO PROMPTLY WRITE THE INPUT STREAM OR READ THE OUTPUT STREAM 
 *  	of the subprocess may cause the subprocess to block, and even deadlock. 
 *	------------------------------------------------------------------------------------ */

	    /*	As per Java guidelines mentioned above, when spawning a console program, we should 
			consume the StdOut and StdErr of spawned console program - otherwise deadlock or
			blocking will happen. However, in case of using start "command" of %COMSPEC% to 
			launch the default Internet Browser, the URL passed is always valid. Thus, nothing 
			will be spewed in StdErr. Besides, executing - start http://papilio.cc - does not
			write anything to StdOut. Thus, there is NO need to consume StdOut and StdErr.
			As a matter of fact, by not consuming StdOut and StdErr, BrowseURL becomes very fast.
	    */
    }

    
    public static void CreateOptionButtonGroup(Box boxGeneric, Border bdrButtonGroup, 
    										   String[] elements, String[] commandActions, 
    										   Dimension elementDimension) {
        ButtonGroup group = new ButtonGroup();
        JRadioButton optElement;
        int i = 0;
    	
    	boxGeneric.setBorder(bdrButtonGroup);
    	
        for (String iterElement : elements) {
	        optElement = new JRadioButton(iterElement);
	        optElement.setActionCommand(commandActions[i]);
	        if (elementDimension != null) {
		        optElement.setPreferredSize(elementDimension);
		        optElement.setMaximumSize(elementDimension);
	        }

	        boxGeneric.add(optElement);
	        group.add(optElement);
	        i++;
        }
    }
    
    public static String SelectedOptionButton(Container container)
    {
    	for (Component optIterator : container.getComponents()) {
			if (optIterator instanceof JRadioButton) {
				JRadioButton optSelected = (JRadioButton) optIterator;
				if (optSelected.isSelected()) {
					return optSelected.getActionCommand();
				}
			}
		}
    	return "";
    }
    
    public static void SelectOption4Group(Container container, String selActionCommand)
    {
    	for (Component optIterator : container.getComponents()) {
			if (optIterator instanceof JRadioButton) {
				JRadioButton optSelected = (JRadioButton) optIterator;
				if (optSelected.getActionCommand().equals(selActionCommand)) {
					optSelected.setSelected(true);
				}
			}
		}
    }
 
    
    /**
     * Show the About box.
     */
    public static void DisplayAboutBox(Frame owner, File imageFile) 
	{
        Toolkit tk = Toolkit.getDefaultToolkit();
        
	    Image image = tk.getImage(imageFile.getAbsolutePath());
	    MediaTracker tracker = new MediaTracker(owner);
	    tracker.addImage(image, 0);
	    try {
	      tracker.waitForID(0);
	    } catch (InterruptedException e) { }
	    final Image aboutImage = image;

	    final Window window = new Window(owner) {
	    	@Override
			public void paint(Graphics g) {
	    		g.drawImage(aboutImage, 0, 0, null);
	    	}
	    };
	    
		window.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				window.dispose();
			}
		});

		int w = image.getWidth(owner);
		int h = image.getHeight(owner);
		Dimension screen = tk.getScreenSize();
		window.setBounds((screen.width-w)/2, (screen.height-h)/2, w, h);
		window.setVisible(true);
	}

    
    public static File ShowFileOpen(String dialogTitle, 
									FileNameExtensionFilter extFileFilter,
									String defaultExtension, 
									File selectedDirectory, 
									File selFile)
    {
		File extFile;

		m_CommonDialog.resetChoosableFileFilters(); // Initialization
		m_CommonDialog.setAcceptAllFileFilterUsed(false);
		m_CommonDialog.setFileFilter(extFileFilter);
		m_CommonDialog.setDialogTitle(dialogTitle);

		 m_CommonDialog.setCurrentDirectory(selectedDirectory);
		if (selFile == null)
			// Clear file selected on previous invocation, if any, of file chooser.
			// Calling .setSelectedFile(null) will NOT clear previously selected file.
			m_CommonDialog.setSelectedFile(new File(""));
		else
			m_CommonDialog.setSelectedFile(selFile);

		int retval = m_CommonDialog.showOpenDialog(m_Parent);

		if (retval == JFileChooser.APPROVE_OPTION) {
			extFile = m_CommonDialog.getSelectedFile();
			// TODO: User has selected a file with extension other than default extension

			// Invalid file prompt if selected file does not exist.
			if (!extFile.isFile()) {
				JOptionPane.showMessageDialog(m_Parent, "The file specified "
						+ CanonicalPath(extFile)
						+ " is invalid and does not exist on disk.\nPlease specify a valid file.",
						"File Not Found", JOptionPane.WARNING_MESSAGE);
					return null;
			}

			// TODO: Validate for invalid characters in file name, depending on OS.
			return extFile;
		}
		else
			return null;
}

    public static File ShowFileSave(String dialogTitle, 
    								FileNameExtensionFilter extFileFilter,
    								String defaultExtension, 
    								File selectedDirectory, 
    								File selFile)
    {
    	File extFile;
    	
		m_CommonDialog.resetChoosableFileFilters();	// Initialization
		m_CommonDialog.setAcceptAllFileFilterUsed(false);
		m_CommonDialog.setFileFilter(extFileFilter);
		m_CommonDialog.setDialogTitle(dialogTitle);

//		m_CommonDialog.setCurrentDirectory(selectedDirectory);
		if (selFile == null)
			// Clear file selected on previous invocation, if any, of file chooser.
			// Calling .setSelectedFile(null) will NOT clear previously selected file.
			m_CommonDialog.setSelectedFile(new File(""));
		else
			m_CommonDialog.setSelectedFile(selFile);

		int retval = m_CommonDialog.showSaveDialog(m_Parent);
		
		if (retval == JFileChooser.APPROVE_OPTION) {
			extFile = m_CommonDialog.getSelectedFile();
			// If user has not specified default file extension, append it manually
			if (!extFile.getName().endsWith(defaultExtension))
				extFile = new File(extFile.getAbsolutePath() + defaultExtension);

			// Overwrite prompt if selected file already exists.
			if (extFile.isFile()) {
				if (JOptionPane.showConfirmDialog(m_Parent, 
										 "The file " + CanonicalPath(extFile) + " already exists.\nDo you want to replace it?", 
										 "Confirm Overwrite", 
										  JOptionPane.YES_NO_OPTION, 
										  JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION)
					return null;
			}

			// TODO: Validate for invalid characters in file name, depending on OS.
			return extFile;
		}
		else
			return null;
    }
    
}
