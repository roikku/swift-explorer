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

package org.swiftexplorer;

import org.swiftexplorer.config.Configuration;
import org.swiftexplorer.config.localization.HasLocalizationSettings;
import org.swiftexplorer.config.localization.HasLocalizationSettings.LanguageCode;
import org.swiftexplorer.config.localization.HasLocalizationSettings.RegionCode;
import org.swiftexplorer.gui.MainPanel;
import org.swiftexplorer.gui.localization.HasLocalizedStrings;
import org.swiftexplorer.gui.localization.LocalizedStringsImpl;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javafx.application.Platform;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwiftExplorer 
{
	final static Logger logger = LoggerFactory.getLogger(SwiftExplorer.class);
	//static{
    	// logging to the console
    //	org.apache.log4j.BasicConfigurator.configure();
	//}
	
	private static final boolean activateNativeLookAndFeelForMacOs = true ;
	
	
	public static boolean isMacOsX ()
	{
		if (!activateNativeLookAndFeelForMacOs)
			return false ;
		String os = System.getProperty("os.name") ;
		return (os != null && os.toLowerCase().contains("mac")) ;
	}
	
	
    public static void main( String[] args )
    {    	
    	if (isMacOsX ())
    	{
    		System.setProperty("apple.laf.useScreenMenuBar", "true");
    		System.setProperty("com.apple.mrj.application.apple.menu.about.name", Configuration.INSTANCE.getAppName());
    		try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
				logger.error("Error occurred while initializing the UI", e);
			}
    	}
    	
    	// required: http://docs.oracle.com/javafx/2/api/javafx/application/Platform.html#setImplicitExit(boolean)
    	// Otherwise, we may get an exception if we attempt to re-open the web-browser login windows
    	Platform.setImplicitExit(false);
  
		// load the settings
    	String settingsFile = "swiftexplorer-settings.xml" ;
    	if (!new File (settingsFile).exists())
    		settingsFile = null ;
		try 
		{
			Configuration.INSTANCE.load(settingsFile);
		} 
		catch (ConfigurationException e) 
		{
			logger.error("Error occurred while initializing the configuration", e);
		}
		
        
		Locale locale = Locale.getDefault() ;
		HasLocalizationSettings localizationSettings = Configuration.INSTANCE.getLocalizationSettings() ;
		if (localizationSettings != null)
		{
			Locale.Builder builder = new Locale.Builder () ;
			LanguageCode lang = localizationSettings.getLanguage() ;
			RegionCode reg = localizationSettings.getRegion() ;
			builder.setLanguage(lang.toString()) ;
			if (reg != null)
				builder.setRegion(reg.toString()) ;
			else
				builder.setRegion("") ;
			locale = builder.build();
		}
		
		final HasLocalizedStrings  localizedStrings = new LocalizedStringsImpl (locale) ;
		
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
            	try 
            	{
            		openMainWindow (new MainPanel(Configuration.INSTANCE, localizedStrings)) ;
        		} 
            	catch (IOException e) 
            	{
            		logger.error("Error occurred while opening the main window", e);
        		}
            }
        });
    }
    
    
    private static void openMainWindow(final MainPanel cp) throws IOException {
        JFrame frame = new JFrame(Configuration.INSTANCE.getAppName());
        
        Dimension screenSize =  java.awt.Toolkit.getDefaultToolkit().getScreenSize() ;
        
        float ratio = (float) 0.8 ;
        Dimension windowSize = new Dimension ((int)(screenSize.getWidth() * ratio), (int)(screenSize.getHeight() * ratio)) ;

        frame.setSize(windowSize.getSize());
        frame.setLocationByPlatform(true);
        frame.setIconImage(ImageIO.read(SwiftExplorer.class.getResource("/icons/logo.png")));
        frame.getContentPane().add(cp);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (cp.onClose()) {
                    System.exit(0);
                }
            }
        });
        cp.setOwner(frame);
        frame.setJMenuBar(cp.createMenuBar());
        
        // center the frame
        int x = (int) ((screenSize.getWidth() - frame.getWidth()) / 2);
        int y = (int) ((screenSize.getHeight() - frame.getHeight()) / 2);
        frame.setLocation(x, y);
        
        frame.setVisible(true);
    }
}
