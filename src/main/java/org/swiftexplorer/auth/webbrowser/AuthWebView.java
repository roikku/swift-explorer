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

package org.swiftexplorer.auth.webbrowser;

import org.swiftexplorer.auth.server.SynchronousDataProvider;
import org.swiftexplorer.gui.util.SwingUtils;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthWebView  {

	final private Logger logger = LoggerFactory.getLogger(AuthWebView.class);
	
	private SimpleSwingBrowser currBrowser = null ;
	private final Object lock = new Object () ;
	
	private volatile SynchronousDataProvider<?> syncDataProvider = null ;
	
	
	private AuthWebView (SynchronousDataProvider<?> syncDataProvider, String url)
	{
		super () ;
		this.syncDataProvider = syncDataProvider ;
		start (url) ;
	}
	
	
	public static AuthWebView openNewBrowser (SynchronousDataProvider<?> syncDataProvider, String url)
	{
		return new AuthWebView (syncDataProvider, url) ;
	}
	
	
	 /*
	public AuthWebView setSynchronousDataProvider (SynchronousDataProvider<?> syncDataProvider)
	{
        synchronized (lock)
        {
        	this.syncDataProvider = syncDataProvider ;	
        }
        return this ;
	}*/
	
	
	private void start(final String url) 
	{    	
	    SwingUtilities.invokeLater(new Runnable() 
	    {
	        @Override
	        public void run() 
	        {	        	
	        	// find a suitable owner, if any
	        	Frame owner = SwingUtils.tryFindSuitableFrameOwner() ;
	        	
	            SimpleSwingBrowser browser = (owner == null) ? new SimpleSwingBrowser() : new SimpleSwingBrowser(owner) ;
	            
	            // we center the dialog
	            int x = 0 ;
	            int y = 0 ;
	            if (owner != null)
	            {
		            x = owner.getLocation().x + (owner.getWidth() - browser.getWidth()) / 2;
		            y = owner.getLocation().y + (owner.getHeight() - browser.getHeight()) / 2;
	            }
	            else
	            {
	                Dimension screenSize =  java.awt.Toolkit.getDefaultToolkit().getScreenSize() ;
	                if (screenSize != null)
	                {
			            x = (int) ((screenSize.getWidth() - browser.getWidth()) / 2);
			            y = (int) ((screenSize.getHeight() - browser.getHeight()) / 2);
	                }
	            }
	            browser.setLocation(x, y);
	            
	            // Show the dialog
	            browser.setVisible(true);
	            browser.loadURL(url);
	            browser.addWindowListener(new WindowAdapter() {
	                @Override
	                public void windowClosing(WindowEvent evt) {

	                	// http://docs.oracle.com/javase/7/docs/api/java/awt/event/WindowListener.html
	                	// Invoked when the user attempts to close the window from the window's system menu.
	                	
	                    synchronized (lock)
	    	            {
	                    	cleanCurrentBrowser () ;
	    	            	
	    	                 // here, we should notify the server
		                    if (syncDataProvider != null)
		                    {
		                    	try 
		                    	{
									syncDataProvider.stopWaiting() ;
								} 
		                    	catch (InterruptedException e) 
		                    	{
		                    		logger.error("Error occurred while stopping the wait of the data provider", e);
								} 
		                    }
	    	            }
	                }
	            });
            	
	            synchronized (lock)
	            {
	            	cleanCurrentBrowser () ;
	            	currBrowser = browser ;
	            }
	        }
	    });
	}
	
	
	private void cleanCurrentBrowser ()
	{
    	if (currBrowser != null)
    	{
    		currBrowser.setVisible(false);
    		currBrowser.dispose();
    		currBrowser = null ;
    	}
	}
	
	
	public void setVisible (final boolean b)
	{
	    SwingUtilities.invokeLater(new Runnable() 
	    {
	        @Override
	        public void run() 
	        {
	        	synchronized (lock)
	        	{
	        		if (currBrowser == null)
	        			return ;
	        		currBrowser.setVisible(b);
	        	}
	        }
	    });
	}
}
