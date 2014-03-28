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

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class ProgressPanel extends JPanel {

	private static final long serialVersionUID = 1L;

    private final JProgressBar progressBarTotal = new JProgressBar (0, 100) ;
    private final JLabel progressLabelTotal = new JLabel () ;
    private final JProgressBar progressBarCurrent = new JProgressBar (0, 100) ;
    private final JLabel progressLabelCurrent = new JLabel () ;
    
    
    public ProgressPanel ()
    {
    	super () ;
        initProgressBarAndLabel (progressBarTotal, progressLabelTotal) ;
        initProgressBarAndLabel (progressBarCurrent, progressLabelCurrent) ;
        
        final int border = 15 ;
        
    	Box box = Box.createVerticalBox() ;
    	box.setBorder(BorderFactory.createEmptyBorder(border, border, border, border));
    	
    	box.add (progressLabelTotal) ;
    	box.add (progressBarTotal) ;
    	
    	box.add(Box.createVerticalStrut(10)) ;
    	
    	box.add (progressLabelCurrent) ;
    	box.add (progressBarCurrent) ;
    	
    	this.add(box) ;
    }
    
    
    private final void initProgressBarAndLabel (JProgressBar progressBar, JLabel progressLabel)
    {
    	progressBar.setStringPainted(true);
    	progressLabel.setMinimumSize(new Dimension (200, 10)) ;
    	progressLabel.setPreferredSize(new Dimension (400, 30)) ;
    	progressLabel.setMaximumSize(new Dimension (Integer.MAX_VALUE, Integer.MAX_VALUE)) ;
    }
    
    
    public void start ()
    {
    	start (progressBarTotal, progressLabelTotal) ;
    	start (progressBarCurrent, progressLabelCurrent) ;
    }
    
    
    public void done ()
    {
    	done (progressBarTotal, progressLabelTotal) ;
    	done (progressBarCurrent, progressLabelCurrent) ;
    }
    
    
    private final void start (JProgressBar progressBar, JLabel progressLabel)
    {
    	progressBar.setValue(0);
    	progressLabel.setText("Processing");
        progressBar.setIndeterminate(true) ;
    }
    
    
    private final void done (JProgressBar progressBar, JLabel progressLabel)
    {
    	progressBar.setValue(100);
    }
    
   
    public void setProgress (double totalProgress, String totalMsg, double currentProgress, String currentMsg)
    {
		setProgressValues (progressBarTotal, progressLabelTotal, totalProgress, totalMsg) ;
		setProgressValues (progressBarCurrent, progressLabelCurrent, currentProgress, currentMsg) ;
    }
    
    
	private void setProgressValues (JProgressBar pb, JLabel lbl, double value, String msg)
	{
		if (!pb.isVisible())
			return ;
		int pbVal = Math.min((int) (value * 100), 100) ;
		if (pb.isIndeterminate())
			pb.setIndeterminate(false) ;
		pb.setValue(pbVal);
		lbl.setText(msg); 
	}
}
