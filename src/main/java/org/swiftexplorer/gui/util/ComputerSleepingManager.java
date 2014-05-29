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

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum ComputerSleepingManager {
	INSTANCE ;

	private final static Logger logger = LoggerFactory.getLogger(ComputerSleepingManager.class);
	
	private final int interval = 15 * 60 * 1000 ; // 15 minutes
	private SeeminglyStaticMouseShaker mouseShaker = null ;
	private Thread thread = null ;
	private int counter = 0 ;
	
	public synchronized void keepAwake (boolean ka)
	{
		counter = Math.max(0, counter + ((ka) ? (1) : (-1))) ;
		if (ka && (mouseShaker == null))
		{
			mouseShaker = new SeeminglyStaticMouseShaker (interval) ;
			thread = new Thread (mouseShaker) ;
			thread.start();
		}
		else if (!ka && (mouseShaker != null) && counter == 0)
		{
			try 
			{
				mouseShaker.stop();
				if (thread != null)
					thread.join();
			} 
			catch (InterruptedException e) 
			{
				logger.error ("Error occurred while keeping the computer awake", e);
			}
			finally
			{
				mouseShaker = null ;
			}
		}
	}
	
	
	private static class SeeminglyStaticMouseShaker implements Runnable
	{
		private volatile boolean stop = false;
		private final int interval ;
		private final Object lock = new Object () ;
		
		public SeeminglyStaticMouseShaker (int interval)
		{
			super () ;
			this.interval = interval ;
		}
		
		public void stop() {
			synchronized (lock)
			{
				stop = true;
				lock.notifyAll();
			}
		}
		   
		@Override
		public void run() {
			stop = false;
		   	Robot robot = null ;
			try 
			{
				robot = new Robot();
				Point prevPos = MouseInfo.getPointerInfo().getLocation();
				synchronized (lock)
				{
			    	while(true)
			    	{
			    		lock.wait(interval);
			    		if (stop)
			    			break ;
			    		Point currPos = MouseInfo.getPointerInfo().getLocation();
			    		if (prevPos.equals(currPos))
			    		{
				    		int x = currPos.x ;
				    		int y = currPos.y ;
				    		robot.mouseMove(x + 1, y + 1);
				    		robot.mouseMove(x - 1, y - 1);
				    		//robot.mouseMove(x, y);
				    		
				    		logger.info ("Shaked mouse pointer to prevent the computer from sleeping");
			    		}
			    		prevPos = currPos ;
			    	}
				}
			} 
			catch (AWTException | InterruptedException e) 
			{
				logger.error ("Error occurred while keeping the computer awake", e);
			}
		}
	}
}
