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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MessageSiphon implements Runnable
{
	private BufferedReader buffReader;
	private Thread thread;
	private MessageConsumer consumer;

	public MessageSiphon(String threadName, InputStream stream, MessageConsumer consumer) {
		this.buffReader = new BufferedReader(new InputStreamReader(stream));
		this.consumer = consumer;
	
		thread = new Thread(this, threadName);
		
		// don't set priority too low, otherwise exceptions won't
		// bubble up in time (i.e. compile errors have a weird delay)
		//thread.setPriority(Thread.MIN_PRIORITY);
		thread.setPriority(Thread.MAX_PRIORITY-1);
	}

	public Thread getThread() {
		return thread;
	}

	public void KickOff() {
		thread.start();
	}

	
	@Override
	public void run()
	{
//	    System.out.println("MessageSiphon thread: " + Thread.currentThread().getName());
		try {
			// process data until we hit EOF; this will happily block
			// (effectively sleeping the thread) until new data comes in.
			// when the program is finally done, null will come through.
			//
			String currentLine;
			String eol = System.getProperty("line.separator");
			while ((currentLine = buffReader.readLine()) != null) {
				if (consumer != null)
					consumer.DeliverMessage(currentLine + eol);
			}
			// EditorConsole.systemOut.println("messaging thread done");
		}
		catch (NullPointerException npe) {
			// Fairly common exception during shutdown
		}
		catch (Exception e) {
			// On Linux and sometimes on Mac OS X, a "bad file descriptor"
			// message comes up when closing an applet that's run externally.
			// That message just gets supressed here..
			String mess = e.getMessage();
			if ((mess != null) && (mess.indexOf("Bad file descriptor") != -1)) {
				// if (e.getMessage().indexOf("Bad file descriptor") == -1) {
				// System.err.println("MessageSiphon err " + e);
				// e.printStackTrace();
			}
			else {
				e.printStackTrace();
			}
		}
		finally {
			thread = null;
		}
	}
	
}
