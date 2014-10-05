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
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

public class ProgressPanel extends JPanel {

	private static final long serialVersionUID = 1L;

    private final JProgressBar progressBarTotal = new JProgressBar (0, 100) ;
    private final JLabel progressLabelTotal = new JLabel () ;
    private final JProgressBar progressBarCurrent = new JProgressBar (0, 100) ;
    private final JLabel progressLabelCurrent = new JLabel () ;
   
    private final int border = 15 ;
    private final int initialWidth = 500 ;
    private final int initialHeight = 160 ;
    
    
	static private class Listener extends ComponentAdapter implements AncestorListener {

		private int height = -1 ;
		private final JPanel panel ;
		
	
		public Listener(JPanel panel) {
			super();
			this.panel = panel;
		}


		@Override
		public void componentResized(ComponentEvent e) {
			if (height > 0 && e.getComponent() instanceof JPanel) {
				JDialog parent = (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, e.getComponent());
				if (parent != null) {
					parent.setSize(new Dimension(parent.getWidth(), height));
				}
			}
			super.componentResized(e);
		}


		@Override
		public void ancestorAdded(AncestorEvent event) { 
			JDialog parent = (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, panel);
			if (parent != null) {
				height = parent.getHeight() ;
			}
		}


		@Override
		public void ancestorRemoved(AncestorEvent event) {
		}


		@Override
		public void ancestorMoved(AncestorEvent event) {
		}
	}
	
    
    public ProgressPanel ()
    {
    	super (new GridBagLayout()) ;
    	GridBagConstraints c = new GridBagConstraints();    	
    	setBorder(BorderFactory.createEmptyBorder(border, border, border, border)) ;
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(5, 0, 0, 0) ;
        
        c.gridx = 0;
        c.gridy = 0;
    	this.add(progressLabelTotal, c) ;
    	
        c.gridx = 0;
        c.gridy = 1;
    	this.add(progressBarTotal, c) ;
    	
        c.gridx = 0;
        c.gridy = 2;
    	this.add(Box.createVerticalStrut(10), c) ;
    	
        c.gridx = 0;
        c.gridy = 3;
    	this.add(progressLabelCurrent, c) ;
    	
        c.gridx = 0;
        c.gridy = 4;
    	this.add(progressBarCurrent, c) ;
    	
    	setPreferredSize(new Dimension (initialWidth, initialHeight)) ;
    	
    	Listener listener = new Listener(this) ;
    	addComponentListener(listener);
    	addAncestorListener(listener) ;
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
		setProgressValues (progressBarCurrent, progressLabelCurrent, currentProgress, processTextToFit (progressLabelCurrent, currentMsg)) ;
		
		progressLabelCurrent.setToolTipText(currentMsg);
    }
    
    
    private String processTextToFit (JLabel lbl, String text)
    {
    	if (text == null)
    		return "" ;
    	FontMetrics fm = lbl.getFontMetrics(lbl.getFont());
    	if (fm == null)
    		return text ;
    	int textWidth = fm.stringWidth(text);
    	int lblWidth = lbl.getWidth() ;
    	if (lblWidth >= textWidth)
    		return text ;
    	
    	// we assume that the set of all characters have a mean length equal or smaller than 'Aa' / 2
    	String dots = " ... " ;
    	int charWidth = fm.stringWidth("Aa") / 2;
    	int dotsWidth = fm.stringWidth(dots);
    	int maxNumChar = (lblWidth - dotsWidth) / charWidth ;
    	int blockWidth = maxNumChar / 2 ;
    	if (blockWidth <= 0)
    		return text ; 
    	
    	StringBuilder sb = new StringBuilder () ;
    	sb.append(text.substring(0, blockWidth)) ;
    	sb.append(dots) ;
    	sb.append(text.substring(text.length() - blockWidth)) ;
    	return sb.toString() ;
    }
    
    
	private void setProgressValues (JProgressBar pb, JLabel lbl, double value, String msg)
	{
		if (!pb.isVisible())
			return ;
		int pbVal = Math.min((int) (value * 100), 100) ;
		if (pb.isIndeterminate())
			pb.setIndeterminate(false) ;
		pb.setValue(pbVal);
		lbl.setText((msg == null)?(""):(msg)); 	
	}
}
