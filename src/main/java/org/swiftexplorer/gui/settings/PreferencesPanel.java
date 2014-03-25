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

import org.swiftexplorer.config.HasConfiguration;
import org.swiftexplorer.config.localization.HasLocalizationSettings;
import org.swiftexplorer.config.localization.HasLocalizationSettings.LanguageCode;
import org.swiftexplorer.config.localization.HasLocalizationSettings.RegionCode;
import org.swiftexplorer.gui.localization.HasLocalizedStrings;
import org.swiftexplorer.gui.util.ReflectionAction;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

public class PreferencesPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public interface PreferencesCallback {
        void setLanguage(LanguageCode lang, RegionCode reg);
    }
	
    public static Icon getFlagsIcon(String string) {
        return new ImageIcon(PreferencesPanel.class.getResource("/flags/" + string));
    }
        
    private static class LanguageInfo
    {
    	private final String language ;
    	private final Icon flag ;
    	private final LanguageCode languageCode ;
    	private final RegionCode regionCode ;
		
    	public LanguageInfo(LanguageCode languageCode, RegionCode regionCode) {
			super();
			this.languageCode = languageCode ;
			this.regionCode = regionCode ;
			
			Locale.Builder builder = new Locale.Builder () ;
			builder.setLanguage(languageCode.toString()) ;
			if (regionCode != null)
				builder.setRegion(regionCode.toString()) ;
			Locale locale = builder.build() ;

			StringBuilder langBuilder = new StringBuilder () ;
			langBuilder.append(locale.getDisplayLanguage()) ;
			String country = locale.getDisplayCountry() ;
			if (country != null && !country.isEmpty())
			{
				langBuilder.append(" (") ;
				langBuilder.append(country) ;
				langBuilder.append(")") ;
			}
			this.language = langBuilder.toString() ;
			
			if (languageCode != LanguageCode.en)
				this.flag = getFlagsIcon(languageCode.toString() + ".png");
			else
			{
				if (regionCode == null)
					this.flag = getFlagsIcon("gb.png");
				else if (regionCode == RegionCode.GB)
					this.flag = getFlagsIcon("gb.png");
				else if (regionCode == RegionCode.US)
					this.flag = getFlagsIcon("us.png");
				else
					this.flag = getFlagsIcon("gb.png");
			}
		}
    	
    	public LanguageInfo(LanguageCode languageCode) 
    	{
			this(languageCode, null);
		}
    	
    	public JLabel toLabel ()
    	{
    		return new JLabel (language, flag, SwingConstants.LEFT) ;
    	}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((languageCode == null) ? 0 : languageCode.hashCode());
			result = prime * result
					+ ((regionCode == null) ? 0 : regionCode.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			LanguageInfo other = (LanguageInfo) obj;
			if (languageCode != other.languageCode)
				return false;
			if (regionCode != other.regionCode)
				return false;
			return true;
		}

		public LanguageCode getLanguageCode() {
			return languageCode;
		}

		public RegionCode getRegionCode() {
			return regionCode;
		}
    }
	
	private Action okAction = null ;
    private Action cancelAction = null ;

    private JButton okButton = null ;
    private JButton cancelButton = null ;

    private DefaultComboBoxModel<LanguageInfo> modelLang = new DefaultComboBoxModel<LanguageInfo>();
    private JComboBox<LanguageInfo> languagesListCombo = new JComboBox<LanguageInfo>(modelLang);
    
    private PreferencesCallback callback;
    private JDialog owner;
    private ActionListener comboActionListener;
    
    private final HasLocalizedStrings stringsBundle ;
    private final HasConfiguration config ;
    
    private LanguageInfo selectedLanguage = null ;
    private final LanguageInfo initialLanguage ;

    public PreferencesPanel(PreferencesCallback callback, HasConfiguration config, HasLocalizedStrings stringsBundle) {
        super(new BorderLayout(0, 0));
        
        this.callback = callback;
        this.stringsBundle = stringsBundle ;
        this.config = config ;
        
        if (config == null)
        	throw new UnsupportedOperationException ("The config parameter must not be null in preference setting dialog.") ;
        
        initMenuActions () ;
        
        modelLang.addElement(new LanguageInfo (LanguageCode.en));
        modelLang.addElement(new LanguageInfo (LanguageCode.en, RegionCode.US));
        modelLang.addElement(new LanguageInfo (LanguageCode.ja, RegionCode.JP));
        modelLang.addElement(new LanguageInfo (LanguageCode.fr, RegionCode.FR));
        modelLang.addElement(new LanguageInfo (LanguageCode.de, RegionCode.DE));
        
        languagesListCombo.setRenderer(new ListCellRenderer<Object> (){
			@Override
			public Component getListCellRendererComponent(JList<?> list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				
				if (value instanceof LanguageInfo)
					return ((LanguageInfo)value).toLabel();
				else
					return new JLabel (value.toString()) ;
			}});
        
        HasLocalizationSettings currentLocalSettings =  this.config.getLocalizationSettings () ;
        if (currentLocalSettings != null)
        {
        	initialLanguage = new LanguageInfo (currentLocalSettings.getLanguage(), currentLocalSettings.getRegion()) ;
        	modelLang.setSelectedItem(initialLanguage);
        }
        else
        	initialLanguage = null ;
        
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        
        Box outer = Box.createVerticalBox();
        Box box = Box.createVerticalBox();

        Box boxLanguage = Box.createHorizontalBox();
        boxLanguage.setBorder(BorderFactory.createEmptyBorder(10, 6, 20, 6));
        boxLanguage.add(new JLabel(getLocalizedString("Language"))) ;
        boxLanguage.add(Box.createHorizontalStrut(8)) ;
        boxLanguage.add(languagesListCombo) ;
        
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); 
        buttons.setBorder(BorderFactory.createEtchedBorder());
        buttons.add(okButton);
        buttons.add(cancelButton);

        box.add(boxLanguage) ;

        outer.add(box);
        this.add(outer, BorderLayout.NORTH);
        this.add(buttons, BorderLayout.SOUTH);

        bindLanguageSelectionListener();
    }
    
    private void initMenuActions () 
    {
        okAction = new ReflectionAction<PreferencesPanel>(getLocalizedString("Ok"), this, "onOk");
        cancelAction = new ReflectionAction<PreferencesPanel>(getLocalizedString("Cancel"), this, "onCancel");
        
        okButton = new JButton(okAction);
        cancelButton = new JButton(cancelAction);        
    }

    private void bindLanguageSelectionListener() {
        comboActionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
            	LanguageInfo langInfo = (LanguageInfo) languagesListCombo.getSelectedItem();
                if (langInfo == null) 
                	return ;
                selectedLanguage = langInfo ;
            }
        };
        languagesListCombo.addActionListener(comboActionListener);
    }


    public void onShow() {
        
    }

    public void onOk() 
    {
        if (callback != null)
        {        	
        	if (selectedLanguage != null && !selectedLanguage.equals(initialLanguage))
        		callback.setLanguage(selectedLanguage.getLanguageCode(), selectedLanguage.getRegionCode());
        	else
        		owner.setVisible(false);
        }
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
