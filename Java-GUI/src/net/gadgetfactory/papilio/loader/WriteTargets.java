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

public enum WriteTargets
{
	FPGA("FPGA"), SPI_FLASH("SPI Flash"), DISK_FILE("Disk File");
	
	private String displayStr;
	
	private WriteTargets(String displayStr) { this.displayStr = displayStr; }
	@Override
	public String toString() {
		return displayStr;
	}
	// TODO: Do we need to override valueOf method?
}
