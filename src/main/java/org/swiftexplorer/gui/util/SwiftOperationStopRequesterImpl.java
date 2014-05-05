package org.swiftexplorer.gui.util;

import org.swiftexplorer.swift.operations.SwiftOperations;

public class SwiftOperationStopRequesterImpl implements SwiftOperations.StopRequester {

	private volatile boolean stopRequested = false ;
	
	@Override
	public synchronized boolean isStopRequested() {
		return stopRequested;
	}

	public synchronized void stop ()
	{
		stopRequested = true ;
	}
}
