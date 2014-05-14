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

package org.swiftexplorer.gui.log;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.gui.localization.HasLocalizedStrings;
import org.swiftexplorer.gui.util.ReflectionAction;

public class ErrorDlg extends JDialog {

	private static final long serialVersionUID = 1L;

	final Logger logger = LoggerFactory.getLogger(ErrorDlg.class);
	
	private final LogPanel logPanel ;
	
    private JButton okButton = null ;
    private JButton exitButton = null ;
    private Action okAction = null ;
    private Action exitAction = null ;
	
    private final JFrame owner ;
    
    private final HasLocalizedStrings stringsBundle ;
    
	public ErrorDlg (JFrame owner, LogPanel logPanel, HasLocalizedStrings stringsBundle)
	{
		super (owner) ;

		this.owner = owner ;
		this.logPanel = logPanel ;
		this.stringsBundle = stringsBundle ;
		
		initActions () ;
		initDlg () ;
	}
	
	
    private void initActions () 
    {
        okAction = new ReflectionAction<ErrorDlg>(getLocalizedString("Ok"), null, this, "onOk");        
        okButton = new JButton(okAction);
        
        exitAction = new ReflectionAction<ErrorDlg>(getLocalizedString("Exit"), null, this, "onExit");        
        exitButton = new JButton(exitAction);
        
        exitButton.setBackground(Color.RED);
        exitButton.setOpaque(true);
   }
    
	
	private void initDlg ()
	{
		Box main = Box.createVerticalBox();
		Border marginBorder = new EmptyBorder(5, 5, 5, 5);
		main.setBorder(marginBorder);
		
		Dimension dim = new Dimension (600, 150) ;
		if (owner != null)
		{
			Dimension parentDim = owner.getSize() ;
			dim = new Dimension ((int) Math.max(200, parentDim.getWidth() / 2), 150) ;		
		}
		main.add (setPrefSize(logPanel, (int)dim.getWidth(), (int)dim.getHeight())) ;
		
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.setBorder(BorderFactory.createEtchedBorder());
        buttons.add(okButton);
        buttons.add(exitButton);
        
        add(main, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
	}
	

	private JComponent setPrefSize (JComponent comp, int minPrefW, int minPrefH)
    {
    	if (comp == null)
    		return null ;
    	comp.setMinimumSize(new Dimension(200, 150));
    	comp.setPreferredSize(new Dimension(minPrefW, minPrefH));
    	comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    	return comp ;
    }
	
	
    private String getLocalizedString (String key)
    {
    	if (stringsBundle == null)
	    	return key.replace("_", " ") ;
    	try
    	{
    		return stringsBundle.getLocalizedString(key) ;
    	}
    	catch (Throwable t)
    	{
    		logger.error("Error occurred while getting localized string", t);
    	}	
    	return key.replace("_", " ") ;
    }
    
    
    @SuppressWarnings("unused")
	private void onOk ()
    {
    	setVisible (false) ;
    }
    
    
    @SuppressWarnings("unused")
	private void onExit ()
    {
    	if (JOptionPane.showConfirmDialog(this, getLocalizedString("confirm_quit_application"), getLocalizedString("Confirmation"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
    		System.exit(ERROR);
    }
}
