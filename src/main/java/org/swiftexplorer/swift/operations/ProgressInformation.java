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

package org.swiftexplorer.swift.operations;

import org.swiftexplorer.swift.operations.SwiftOperations.SwiftCallback;
import org.swiftexplorer.util.FileUtils.InputStreamProgressFilter;

public class ProgressInformation implements InputStreamProgressFilter.StreamProgressCallback
{
	private double totalProgress = 0 ;
	private double currentProgress = 0 ;
	private String totalMessage = null ;
	private String currentMessage = null ;
	private final boolean isSingleTask ;
	
	private final SwiftCallback callback ;
	
	public ProgressInformation (SwiftCallback callback, boolean isSingleTask)
	{
		super () ;
		this.callback = callback ;
		this.isSingleTask = isSingleTask ;
	}
	
	public synchronized void report ()
	{
		if (callback == null)
			return ;
		callback.onProgress(totalProgress, totalMessage, (isSingleTask)?(totalProgress):(currentProgress), (isSingleTask)?(totalMessage):(currentMessage));
	}

	@Override
	public synchronized void onStreamProgress(double progress) {
		setCurrentProgress(progress) ;
		report () ;
	}
	
	public synchronized void setCurrentProgress (double p)
	{
		currentProgress = p ;
	}
	
	public synchronized void setTotalProgress (double p)
	{
		totalProgress = p ;
	}
	
	public synchronized void setTotalMessage (String msg)
	{
		totalMessage = msg ;
	}
	
	public synchronized void setCurrentMessage (String msg)
	{
		currentMessage = msg ;
	}
	
	public synchronized String getCurrentMessage ()
	{
		return currentMessage ;
	}
}
