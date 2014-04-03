/*
 *  Copyright 2014 Loic Merckel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.swiftexplorer.gui.settings;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.text.MessageFormat;
import java.util.Hashtable;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.config.swift.HasSwiftSettings;
import org.swiftexplorer.config.swift.SwiftParameters;
import org.swiftexplorer.gui.localization.HasLocalizedStrings;
import org.swiftexplorer.gui.util.ReflectionAction;
import org.swiftexplorer.util.FileUtils;


public class SwiftPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	
	public interface SwiftSettingsCallback {
        void setSwiftParameters(SwiftParameters newParameters);
    }
	
	
	@SuppressWarnings("unused")
	final private Logger logger = LoggerFactory.getLogger(SwiftPanel.class);
	
	
	private static class SegmentationSizeSlider
	{
		private final int scale ;
		private final JSlider slider ;
		
		public SegmentationSizeSlider (int scale, int orientation)
		{
			super () ;
			slider = new JSlider (orientation) ;
			this.scale = scale ;
		}
		
	    private int getScaledValue (long val)
	    {
	    	long scaledVal = val / scale ;
	    	if (scaledVal > Integer.MAX_VALUE)
	    		throw new AssertionError () ;
	    	return (int) scaledVal ;
	    }
	    
	    private long getActualValue (int val)
	    {
	    	return (long) val * scale ;
	    }
		
		public long getValue() {
			return getActualValue(slider.getValue());
		}

		public void setValue(long n) {
			slider.setValue(getScaledValue(n));
		}

		public long getMinimum() {
			return getActualValue(slider.getMinimum());
		}

		public void setMinimum(long minimum) {
			slider.setMinimum(getScaledValue(minimum));
		}

		public long getMaximum() {
			return getActualValue(slider.getMaximum());
		}

		public void setMaximum(long maximum) {
			slider.setMaximum(getScaledValue(maximum));
		}

		public void setExtent(int extent) {
			slider.setExtent(Math.max(getScaledValue(extent) / extent, 1));
		}

		public void setMajorTickSpacing(long n) {
			slider.setMajorTickSpacing(getScaledValue(n));
		}
		
		public void setPaintTicks (boolean b)
		{
			slider.setPaintTicks (b) ;
		}

		public JComponent getComponent ()
		{
			return slider ;
		}

		public void addChangeListener(ChangeListener changeListener) {
			slider.addChangeListener(changeListener) ;
		}
		
		public void showLabel (boolean b, boolean si)
		{
			if (b)
			{
		        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		        labelTable.put(new Integer(slider.getMinimum()), new JLabel(FileUtils.humanReadableByteCount(this.getMinimum(), si)));
		        labelTable.put(new Integer(Math.abs(slider.getMaximum() - slider.getMinimum()) / 2 ), new JLabel(FileUtils.humanReadableByteCount(Math.abs(this.getMaximum() - this.getMinimum()) / 2 , si)));
		        labelTable.put(new Integer(slider.getMaximum()), new JLabel(FileUtils.humanReadableByteCount(this.getMaximum(), si)));
		        slider.setLabelTable (labelTable);
		        slider.setPaintLabels(true);
			}
			slider.setPaintLabels(b);
		}

		public boolean getValueIsAdjusting() {
			return slider.getValueIsAdjusting() ;
		}
	}
	
	
	private Action okAction = null ;
    private Action cancelAction = null ;

    private JButton okButton = null ;
    private JButton cancelButton = null ;
    
    private final JTextField segmentationSizeTf = new JTextField();
    private final JCheckBox hideSegmentsContainer ;
    private final SegmentationSizeSlider segmentationSizeSlider ;
    
    private final HasSwiftSettings swiftSettings ;
    
    private final HasLocalizedStrings stringsBundle ;

    private final SwiftSettingsCallback callback;
    private JDialog owner;
    
    private final int scale = 4 ;
    private boolean si = false ;

    
    public SwiftPanel(SwiftSettingsCallback callback, HasSwiftSettings swiftSettings, HasLocalizedStrings stringsBundle) {
        super(new BorderLayout(0, 0));
        
        initMenuActions () ;
        
        this.callback = callback;
        this.stringsBundle = stringsBundle ;
        this.swiftSettings = swiftSettings ;
        
        hideSegmentsContainer = new JCheckBox (getLocalizedString("Hide_segments_container_check_box")) ;
        hideSegmentsContainer.setSelected(swiftSettings.hideSegmentsContainers());

        segmentationSizeTf.setText(getSizeLabel(this.swiftSettings.getSegmentationSize()));
        segmentationSizeSlider = new SegmentationSizeSlider (scale, JSlider.HORIZONTAL) ;
        segmentationSizeSlider.setMinimum(SwiftParameters.MIN_SEGMENTATION_SIZE) ;
        segmentationSizeSlider.setMaximum(SwiftParameters.MAX_SEGMENTATION_SIZE) ;
        segmentationSizeSlider.setValue(this.swiftSettings.getSegmentationSize()) ;
        
        initSegmentationSizeSelection () ;

        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        
        Box outer = Box.createVerticalBox();
        Box box = Box.createVerticalBox();

        Box boxSegmentationSize = Box.createVerticalBox();
        boxSegmentationSize.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 6));
        
        boxSegmentationSize.setBorder(BorderFactory.createTitledBorder(getLocalizedString("Segmentation_Size")));
        
        setMinPrefMaxWidth (segmentationSizeSlider.getComponent(), 400, Integer.MAX_VALUE, 100) ;
        boxSegmentationSize.add(segmentationSizeSlider.getComponent()) ;
        boxSegmentationSize.add(segmentationSizeTf) ;
       
        Box boxHideSegmentsContainer = Box.createHorizontalBox();
        boxHideSegmentsContainer.setBorder(BorderFactory.createEmptyBorder(20, 6, 20, 6));
        boxHideSegmentsContainer.add(hideSegmentsContainer) ;
        
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); 
        buttons.setBorder(BorderFactory.createEtchedBorder());
        buttons.add(okButton);
        buttons.add(cancelButton);

        box.add(boxSegmentationSize) ;
        box.add(boxHideSegmentsContainer) ;

        outer.add(box);
        this.add(outer, BorderLayout.NORTH);
        this.add(buttons, BorderLayout.SOUTH);
    }
  
    
    private void initSegmentationSizeSelection ()
    {
    	if (segmentationSizeSlider == null || segmentationSizeTf == null)
    		throw new AssertionError () ;
    	
    	segmentationSizeTf.setEditable(false);
    	segmentationSizeTf.setAlignmentX(CENTER_ALIGNMENT);
    	segmentationSizeTf.setBorder(null);
    	if (segmentationSizeSlider.getComponent().getFont() != null)
    		segmentationSizeTf.setFont(segmentationSizeSlider.getComponent().getFont());
    	
    	long range = SwiftParameters.MAX_SEGMENTATION_SIZE - SwiftParameters.MIN_SEGMENTATION_SIZE ;
    	segmentationSizeSlider.setExtent(100);
        segmentationSizeSlider.setMajorTickSpacing(range / 2);
        segmentationSizeSlider.setPaintTicks(true);
        
        // Labels
        segmentationSizeSlider.showLabel (true, si) ;
        
        // Events
        segmentationSizeSlider.addChangeListener(new ChangeListener () {
			@Override
			public void stateChanged(ChangeEvent e) {
		        if (!segmentationSizeSlider.getValueIsAdjusting()) {
		            long size = segmentationSizeSlider.getValue();
		            segmentationSizeTf.setText(getSizeLabel (size));
		        }  
			}}) ; 
    }
    
    
    private String getSizeLabel (long size)
    {
    	StringBuilder sb = new StringBuilder () ;
    	sb.append(getLocalizedString("Size")) ;
    	sb.append(": ") ;
    	sb.append(FileUtils.humanReadableByteCount(size, si)) ;
    	return sb.toString() ;
    }
    
    
    private void initMenuActions () 
    {
        okAction = new ReflectionAction<SwiftPanel>(getLocalizedString("Ok"), this, "onOk");
        cancelAction = new ReflectionAction<SwiftPanel>(getLocalizedString("Cancel"), this, "onCancel");
        
        okButton = new JButton(okAction);
        cancelButton = new JButton(cancelAction);        
    }
    
    
    private JComponent setMinPrefMaxWidth (JComponent comp, int minPrefW, int maxW, int prefHeight)
    {
    	if (comp == null)
    		return null ;
    	comp.setMinimumSize(new Dimension(minPrefW, 0));
    	comp.setPreferredSize(new Dimension(minPrefW, prefHeight));
    	comp.setMaximumSize(new Dimension(maxW, Integer.MAX_VALUE));
    	return comp ;
    }
    
    
    public void onOk() 
    {
        if (callback == null)
        {
        	owner.setVisible(false);
        	return ;
        }    
        /*
        long segmentationSize = SwiftParameters.MAX_SEGMENTATION_SIZE ;
        String segmentationSizeStr = segmentationSizeTf.getText() ;
        if (segmentationSizeStr == null || segmentationSizeStr.isEmpty())
        {
        	showInvalidSegmentationSizeDlg () ;
        	return ;
        }	
        try
        {
        	segmentationSize = Long.valueOf(segmentationSizeStr) ;
        }
        catch (NumberFormatException e)
        {
        	logger.error("Error occurred while converting the string user input into long", e);
        	showInvalidSegmentationSizeDlg () ;
        	return ;
        }*/
        long segmentationSize = segmentationSizeSlider.getValue() ;
        SwiftParameters.Builder paramBuilder = new SwiftParameters.Builder (segmentationSize, hideSegmentsContainer.isSelected()) ;
    	callback.setSwiftParameters(paramBuilder.build());
    }
    
    
    @SuppressWarnings("unused")
	private void showInvalidSegmentationSizeDlg ()
    {
    	String msg = MessageFormat.format (getLocalizedString("Please_enter_an_integer_between_val_min_and_val_max"), SwiftParameters.MIN_SEGMENTATION_SIZE, SwiftParameters.MAX_SEGMENTATION_SIZE) ;
    	JOptionPane.showMessageDialog(this, msg, "Invalid Inputs", JOptionPane.ERROR_MESSAGE) ; 
    }
    
    
    public void onShow() {
        
    }
    
    
    public void onCancel() {
        owner.setVisible(false);
    }

    
    public void setOwner(JDialog dialog) {
        this.owner = dialog;
        this.owner.getRootPane().setDefaultButton(okButton);
    }

    
    private String getLocalizedString (String key)
    {
    	if (stringsBundle == null)
	    	return key.replace("_", " ") ;
    	return stringsBundle.getLocalizedString(key) ;
    }
}
