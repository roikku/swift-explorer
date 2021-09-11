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

package org.swiftexplorer.gui;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.config.Configuration;
import org.swiftexplorer.gui.localization.HasLocalizedStrings;

public class AboutDlg 
{
	private static final Logger logger = LoggerFactory.getLogger(AboutDlg.class);

	public static void show (Component parent, HasLocalizedStrings stringsBundle) 
	{
        URI uri = null ;
		try 
		{
			uri = new URI("http://www.619.io/swift-explorer");
		} 
		catch (URISyntaxException e) 
		{
			logger.error("URL seems to be ill-formed", e);
		}
		final String buttontext = "Original Swift Explorer: http://www.619.io/swift-explorer" ;
    	
		Box mainBox = Box.createVerticalBox() ;  
		mainBox.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
		
        StringBuilder sb = loadResource("/about.html");
        JLabel label = new JLabel(sb.toString());
        label.getAccessibleContext().setAccessibleDescription(getTitle (stringsBundle));
        mainBox.add(label) ;
		
		if (uri != null)
		{
	        JButton button = new JButton();
	        button.setText(buttontext);
	        button.setToolTipText(uri.toString());
	        button.addActionListener(new OpenUrlAction(uri));
	        FontMetrics metrics = button.getFontMetrics(button.getFont());
	        if (metrics != null)
	        	button.setSize(metrics.stringWidth(buttontext), button.getHeight());
	        button.getAccessibleContext().setAccessibleDescription(buttontext);
	        mainBox.add(button);
		}
		
		mainBox.add(Box.createVerticalStrut(10)) ;
		
		JPanel panel = new JPanel () ;
		panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		panel.add(mainBox) ;
		
        JOptionPane.showMessageDialog(parent, panel, getTitle (stringsBundle), JOptionPane.INFORMATION_MESSAGE, getIcon());
	}
	
	
    private static Icon getIcon() {
        return new ImageIcon(AboutDlg.class.getResource("/icons/logo.png" ));
    }
    
	
	private static String getTitle (HasLocalizedStrings stringsBundle)
	{
        StringBuilder title = new StringBuilder () ;
        title.append (" ") ;
        title.append (Configuration.INSTANCE.getAppName()) ;
        title.append (" (") ;
        title.append (Configuration.INSTANCE.getAppVersion()) ;
        title.append (")") ;
        return MessageFormat.format ((stringsBundle != null)?(stringsBundle.getLocalizedString("About_dlg_title")):("About"), title.toString()) ;
	}
	
	
	private static class OpenUrlAction implements ActionListener {
		
		private final URI uri ;
		
		public OpenUrlAction (URI uri)
		{
			super () ;
			this.uri = uri ;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			open(uri);
		}
	}
	
	
	private static void open(URI uri) {
		if (Desktop.isDesktopSupported()) 
		{
			try {
				Desktop.getDesktop().browse(uri);
			} catch (IOException e) {

			}
		} 
	}
	
	
    private static StringBuilder loadResource(String resource) {
        StringBuilder sb = new StringBuilder();
        InputStream input = AboutDlg.class.getResourceAsStream(resource);
        try {
            try {
                List<String> lines = IOUtils.readLines(input);
                for (String line : lines) {
                    sb.append(line);
                }
            } finally {
                input.close();
            }
        } catch (IOException ex) 
        {
        	logger.error("Error occurred while loading resources.", ex);
            throw new RuntimeException(ex);
        }
        return sb;
    }
}
