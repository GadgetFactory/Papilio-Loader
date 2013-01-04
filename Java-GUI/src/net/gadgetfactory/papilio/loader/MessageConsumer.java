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

/**
 * Interface for dealing with output of spawned console program.
 * <P>
 * Different clients of MessageSiphon class need to process messages / output from 
 * spawned console program in a different way. These clients should implement this 
 * interface.
 * <P>
 */
public interface MessageConsumer
{
	public void DeliverMessage(final String stdline);
}
