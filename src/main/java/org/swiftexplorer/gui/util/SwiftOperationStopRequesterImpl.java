/*
 * Copyright 2014 Loic Merckel
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.swiftexplorer.gui.util;

import java.util.Observable;
import java.util.Observer;

import org.swiftexplorer.swift.operations.SwiftOperations;

public class SwiftOperationStopRequesterImpl implements SwiftOperations.StopRequester, Observer {

	public static class Stopper extends Observable  implements SwiftOperations.StopRequester
	{
		private volatile boolean stopRequested = false ;
		
		public void stop ()
		{
			stopRequested = true ;
			setChanged();
			notifyObservers () ;
		}

		@Override
		public synchronized void deleteObservers() {
			stopRequested = false ;
			super.deleteObservers();
		}

		@Override
		public boolean isStopRequested() {
			return stopRequested;
		}
	}
	
	private volatile boolean stopRequested = false ;
	
	@Override
	public synchronized boolean isStopRequested() {
		return stopRequested;
	}

	public synchronized void stop ()
	{
		stopRequested = true ;
	}

	@Override
	public void update(Observable o, Object arg) {
		stop () ;
		if (o != null)
			o.deleteObserver(this);
	}
}
