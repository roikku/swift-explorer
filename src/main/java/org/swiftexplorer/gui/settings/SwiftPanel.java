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
import java.awt.FlowLayout;
import java.text.MessageFormat;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.config.swift.HasSwiftSettings;
import org.swiftexplorer.config.swift.SwiftParameters;
import org.swiftexplorer.gui.localization.HasLocalizedStrings;
import org.swiftexplorer.gui.util.ReflectionAction;


public class SwiftPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public interface SwiftSettingsCallback {
        void setSwiftParameters(SwiftParameters newParameters);
    }
	
	final private Logger logger = LoggerFactory.getLogger(SwiftPanel.class);
	
	private Action okAction = null ;
    private Action cancelAction = null ;

    private JButton okButton = null ;
    private JButton cancelButton = null ;
    
    private final JTextField segmentationSizeTf = new JTextField();
    private final JCheckBox hideSegmentsContainer ;
    
    private final HasSwiftSettings swiftSettings ;
    
    private final HasLocalizedStrings stringsBundle ;

    private final SwiftSettingsCallback callback;
    private JDialog owner;

    
    public SwiftPanel(SwiftSettingsCallback callback, HasSwiftSettings swiftSettings, HasLocalizedStrings stringsBundle) {
        super(new BorderLayout(0, 0));
        
        initMenuActions () ;
        
        this.callback = callback;
        this.stringsBundle = stringsBundle ;
        this.swiftSettings = swiftSettings ;
        
        hideSegmentsContainer = new JCheckBox (getLocalizedString("Hide_segments_container_check_box")) ;
        hideSegmentsContainer.setSelected(swiftSettings.hideSegmentsContainers());

        segmentationSizeTf.setText(String.valueOf(this.swiftSettings.getSegmentationSize()));
        
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        
        Box outer = Box.createVerticalBox();
        Box box = Box.createVerticalBox();

        Box boxSegmentationSize = Box.createHorizontalBox();
        boxSegmentationSize.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 6));
        boxSegmentationSize.add(new JLabel(getLocalizedString("Segmentation_Size_In_Bytes"))) ;
        boxSegmentationSize.add(Box.createHorizontalStrut(8)) ;
        boxSegmentationSize.add(segmentationSizeTf) ;
       
        Box boxHideSegmentsContainer = Box.createHorizontalBox();
        boxHideSegmentsContainer.setBorder(BorderFactory.createEmptyBorder(10, 6, 20, 6));
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
    
    
    private void initMenuActions () 
    {
        okAction = new ReflectionAction<SwiftPanel>(getLocalizedString("Ok"), this, "onOk");
        cancelAction = new ReflectionAction<SwiftPanel>(getLocalizedString("Cancel"), this, "onCancel");
        
        okButton = new JButton(okAction);
        cancelButton = new JButton(cancelAction);        
    }
    
    
    public void onOk() 
    {
        if (callback == null)
        {
        	owner.setVisible(false);
        	return ;
        }    
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
        }
        SwiftParameters.Builder paramBuilder = new SwiftParameters.Builder (segmentationSize, hideSegmentsContainer.isSelected()) ;
    	callback.setSwiftParameters(paramBuilder.build());
    }
    
    
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
