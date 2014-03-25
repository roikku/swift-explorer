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

/*
*  This file is based on the LoginPanel.java, 
*  which can be found here:
*
*  https://github.com/javaswift/cloudie
*  package (src/main/java): org.javaswift.cloudie.login;
*  
*  We have adapted the LoginPanel to the ProxyPanel. 
*  The LoginPanel.java has the following license:
*
*  Copyright 2012-2013 E.Hooijmeijer
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

import org.swiftexplorer.config.proxy.Proxy;
import org.swiftexplorer.gui.MainPanel;
import org.swiftexplorer.gui.localization.HasLocalizedStrings;
import org.swiftexplorer.gui.util.ReflectionAction;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.util.HashSet;
import java.util.Set;

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
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyPanel  extends JPanel {

	private static final long serialVersionUID = 1L;

	public interface ProxyCallback {
        void setProxy(Set<Proxy> newProxies);
    }
	
	final private Logger logger = LoggerFactory.getLogger(ProxyPanel.class);

    private Action okAction = null ;
    private Action cancelAction = null ;

    private JButton okButton = null ;
    private JButton cancelButton = null;

    private JTextField hostHttp = new JTextField();
    private JTextField portHttp = new JTextField();
    private JTextField usernameHttp = new JTextField();
    private JPasswordField passwordHttp = new JPasswordField();
    
    private JTextField hostHttps = new JTextField();
    private JTextField portHttps = new JTextField();
    private JTextField usernameHttps = new JTextField();
    private JPasswordField passwordHttps = new JPasswordField();
    
    private JLabel warningLabel = null ;

    private JCheckBox activatedHttp = new JCheckBox ("Activated", true) ;
    private JCheckBox activatedHttps = new JCheckBox ("Activated", true) ;
    
    private ProxyCallback callback;
    private JDialog owner;
    private ProxiesStore proxiesStore;

    private final HasLocalizedStrings stringsBundle ;
    
    public ProxyPanel(ProxyCallback callback, ProxiesStore proxiesStore, HasLocalizedStrings stringsBundle) 
    {
        super(new BorderLayout(0, 0));
        
        this.callback = callback;
        this.proxiesStore = proxiesStore;
        this.stringsBundle = stringsBundle ;
        
        initMenuActions () ;
        
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        
        Box outer = Box.createVerticalBox();
        Box box = Box.createVerticalBox();

        box.setBorder(BorderFactory.createTitledBorder(getLocalizedString("Proxies")));
        
        Box boxLabel = Box.createHorizontalBox();
        Box boxHttp = Box.createHorizontalBox();
        Box boxHttps = Box.createHorizontalBox();
        
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JPanel warn = new JPanel(new BorderLayout());
        warn.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
        warn.add(warningLabel, BorderLayout.CENTER);
        buttons.setBorder(BorderFactory.createEtchedBorder());
        buttons.add(okButton);
        buttons.add(cancelButton);
        
        JLabel label11 = new JLabel (" ") ;
        
        FontMetrics fm = label11.getFontMetrics(label11.getFont()); 
        
        int protocolWidth = fm.stringWidth("https://") + 10 ;
        int usernameWidth = fm.stringWidth("username") + 10 ;
        int passwordWidth = fm.stringWidth("password") + 10 ;
        int portWidth = fm.stringWidth("port") + 30 ;
        int activatedWidth = fm.stringWidth("activated") + 35 ;
        int oneCharWidth = fm.stringWidth("@") + 5 ;
        int hostMinWidth = 80 ;
        int hostMaxWidth = Integer.MAX_VALUE ;
        
        setMinPrefMaxWidth(label11, protocolWidth, protocolWidth);
        
        JLabel label12 = new JLabel ("username") ;
        setMinPrefMaxWidth(label12, usernameWidth, usernameWidth);
        
        JLabel label13 = new JLabel (" ") ;
        setMinPrefMaxWidth(label13, oneCharWidth, oneCharWidth);
        
        JLabel label14 = new JLabel ("password") ;
        setMinPrefMaxWidth(label14, passwordWidth, passwordWidth);
        
        JLabel label15 = new JLabel (" ") ;
        setMinPrefMaxWidth(label15, oneCharWidth, oneCharWidth);
        
        JLabel label16 = new JLabel ("host") ;
        setMinPrefMaxWidth(label16, hostMinWidth, hostMaxWidth);
        
        JLabel label17 = new JLabel (" ") ;
        setMinPrefMaxWidth(label17, oneCharWidth, oneCharWidth);
        
        JLabel label18 = new JLabel ("port") ;
        setMinPrefMaxWidth(label18, portWidth, portWidth);
        
        JLabel label19 = new JLabel (" ") ;
        setMinPrefMaxWidth(label19, activatedWidth, activatedWidth);
        
        boxLabel.add(label11) ;
        boxLabel.add(label12) ;
        boxLabel.add(label13) ;
        boxLabel.add(label14) ;
        boxLabel.add(label15) ;
        boxLabel.add(label16) ;
        boxLabel.add(label17) ;
        boxLabel.add(label18) ;
        boxLabel.add(label19) ;
        
        box.add(boxLabel) ;

        JLabel label21 = new JLabel ("http://") ;
        setMinPrefMaxWidth(label21, protocolWidth, protocolWidth);
        
        setMinPrefMaxWidth(usernameHttp, usernameWidth, usernameWidth);
        setMinPrefMaxWidth(passwordHttp, passwordWidth, passwordWidth);
        setMinPrefMaxWidth(hostHttp, hostMinWidth, hostMaxWidth);
        setMinPrefMaxWidth(portHttp, portWidth, portWidth);
        setMinPrefMaxWidth(activatedHttp, activatedWidth, activatedWidth);
        
        boxHttp.add(label21) ;
        boxHttp.add(usernameHttp) ;
        boxHttp.add(setMinPrefMaxWidth(new JLabel (":", JLabel.CENTER), oneCharWidth, oneCharWidth)) ;
        boxHttp.add(passwordHttp) ;
        boxHttp.add(setMinPrefMaxWidth(new JLabel ("@", JLabel.CENTER), oneCharWidth, oneCharWidth)) ;
        boxHttp.add(hostHttp) ;
        boxHttp.add(setMinPrefMaxWidth(new JLabel (":", JLabel.CENTER), oneCharWidth, oneCharWidth)) ;
        boxHttp.add(portHttp) ;
        boxHttp.add(activatedHttp) ;
        
        box.add(boxHttp) ;

        JLabel label31 = new JLabel ("https://") ;
        setMinPrefMaxWidth(label31, protocolWidth, protocolWidth);

        setMinPrefMaxWidth(usernameHttps, usernameWidth, usernameWidth);
        setMinPrefMaxWidth(passwordHttps, passwordWidth, passwordWidth);
        setMinPrefMaxWidth(hostHttps, hostMinWidth, hostMaxWidth);
        setMinPrefMaxWidth(portHttps, portWidth, portWidth);
        setMinPrefMaxWidth(activatedHttps, activatedWidth, activatedWidth);
        
        boxHttps.add(label31) ;
        boxHttps.add(usernameHttps) ;
        boxHttps.add(setMinPrefMaxWidth(new JLabel (":", JLabel.CENTER), oneCharWidth, oneCharWidth)) ;
        boxHttps.add(passwordHttps) ;
        boxHttps.add(setMinPrefMaxWidth(new JLabel ("@", JLabel.CENTER), oneCharWidth, oneCharWidth)) ;
        boxHttps.add(hostHttps) ;
        boxHttps.add(setMinPrefMaxWidth(new JLabel (":", JLabel.CENTER), oneCharWidth, oneCharWidth)) ;
        boxHttps.add(portHttps) ;
        boxHttps.add(activatedHttps) ;
        
        box.add(boxHttps) ;
  
        outer.add(box);
        outer.add(warn);
        this.add(outer, BorderLayout.NORTH);
        this.add(buttons, BorderLayout.SOUTH);

        initForm () ;
    }
    
    private void initMenuActions () 
    {
        okAction = new ReflectionAction<ProxyPanel>(getLocalizedString("Ok"), MainPanel.getIcon("server_connect.png"), this, "onOk");
        cancelAction = new ReflectionAction<ProxyPanel>(getLocalizedString("Cancel"), this, "onCancel");
        
        okButton = new JButton(okAction);
        cancelButton = new JButton(cancelAction);
        
        warningLabel = new JLabel(getLocalizedString("warning_proxies_stored_plain_text"), MainPanel.getIcon("table_error.png"), JLabel.CENTER);
    }
    
    
    private JComponent setMinPrefMaxWidth (JComponent comp, int minPrefW, int maxW)
    {
    	if (comp == null)
    		return null ;
    	comp.setMinimumSize(new Dimension(minPrefW, 0));
    	comp.setPreferredSize(new Dimension(minPrefW, 25));
    	comp.setMaximumSize(new Dimension(maxW, Integer.MAX_VALUE));
    	return comp ;
    }

    
    private void initForm ()
    {
        for (Proxy pr : proxiesStore.getProxies()) 
        {
        	if("http".equalsIgnoreCase(pr.getProtocol()))
        	{
                hostHttp.setText(pr.getHost());
                portHttp.setText(String.valueOf(pr.getPort()));
                usernameHttp.setText(pr.getUsername());
                passwordHttp.setText(pr.getPassword());
                activatedHttp.setSelected(pr.isActive()) ;
        	}
        	else if ("https".equalsIgnoreCase(pr.getProtocol()))
        	{
                hostHttps.setText(pr.getHost());
                portHttps.setText(String.valueOf(pr.getPort()));
                usernameHttps.setText(pr.getUsername());
                passwordHttps.setText(pr.getPassword());
                activatedHttps.setSelected(pr.isActive()) ;
        	}
        }
    }
   

    @SuppressWarnings("unused")
	private void clearProxyForm() {
        hostHttp.setText("");
        portHttp.setText("");
        usernameHttp.setText("");
        passwordHttp.setText("");
        
        hostHttps.setText("");
        portHttps.setText("");
        usernameHttps.setText("");
        passwordHttps.setText("");
    }

    public void onShow() {
        hostHttp.requestFocus();
    }
    
    
    private int getPortNumber (String str)
    {
		try
		{
			return Integer.valueOf(portHttp.getText().trim());
		}
		catch (NumberFormatException e)
		{
			logger.error("Invalid port number", e);
			JOptionPane.showMessageDialog(this, "The port number must be an integer", "Invalid Inputs", JOptionPane.ERROR_MESSAGE) ; 
			return -1;
		}
    }

    public void onOk() {
    	
    	int portHttpVal = getPortNumber (portHttp.getText().trim()) ;
    	if (portHttpVal < 0)
    	{
    		portHttp.requestFocus() ;
    		portHttp.selectAll();
    		return ;
    	}
    	int portHttpsVal = getPortNumber (portHttps.getText().trim()) ;
    	if (portHttpsVal < 0)
    	{
    		portHttps.requestFocus() ;
    		portHttps.selectAll();
    		return ;
    	}

    	Set<Proxy> proxies = new HashSet<Proxy> () ;
		// HTTP
		proxies.add(new Proxy.Builder("http")
				.setActivated(activatedHttp.isSelected())
				.setHost(hostHttp.getText().trim())
				.setPassword(String.valueOf(passwordHttp.getPassword()))
				.setPort(portHttpVal)
				.setUsername(usernameHttp.getText().trim()).build());
		// HTTPS
		proxies.add(new Proxy.Builder("https")
				.setActivated(activatedHttps.isSelected())
				.setHost(hostHttps.getText().trim())
				.setPassword(String.valueOf(passwordHttps.getPassword()))
				.setPort(portHttpsVal)
				.setUsername(usernameHttps.getText().trim()).build());
    	
        callback.setProxy(proxies);
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
