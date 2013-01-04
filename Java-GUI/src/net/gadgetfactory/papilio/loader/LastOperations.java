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

import java.util.EnumSet;
import java.util.StringTokenizer;

public enum LastOperations
{
	SCAN("[Scan]"), 
	MERGE("[Merge]"), 
	ERASE("[Erase]"), 
	WRITE_TO_FPGA("[WriteToFPGA]"), 
	WRITE_TO_SPI_FLASH("[WriteToSPIFlash]"), 
	WRITE_TO_DISK_FILE("[WriteToDisk]"), 
	VERIFY("[Verify]");

	private LastOperations(String displayStr) {
		this.displayStr = displayStr;
	}
	
	@Override
	public String toString() {
		return displayStr;
	}

	public static EnumSet<LastOperations> DecodePreferences(String operationsList)
	{
		EnumSet<LastOperations> estOperations = EnumSet.noneOf(LastOperations.class);
		StringTokenizer st = new StringTokenizer(operationsList, ",");
		String sOperation;
		
		while (st.hasMoreTokens()) {
			sOperation = st.nextToken().trim();
	        if (!sOperation.equals("")) {
	    		for (LastOperations iterOperation : values()) {
	    			if (iterOperation.displayStr.equalsIgnoreCase(sOperation))
	    				estOperations.add(iterOperation);
	    		}
	        }
		}
		return estOperations;
	}
	
	public static LastOperations Enum4WriteTarget(WriteTargets selTarget) {
		switch (selTarget)
		{
		case FPGA:
			return LastOperations.WRITE_TO_FPGA;
		case SPI_FLASH:
			return LastOperations.WRITE_TO_SPI_FLASH;
		case DISK_FILE:
			return LastOperations.WRITE_TO_DISK_FILE;
		default:
			return LastOperations.WRITE_TO_DISK_FILE;	// for compiler
		}
	}
	
	private String displayStr;

}
