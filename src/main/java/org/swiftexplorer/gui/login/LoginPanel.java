/*
 *  Copyright 2014 Loic Merckel
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

/*
*
* The original version of this file (i.e., the one that is copyrighted 2012-2013 E.Hooijmeijer) 
* can  be found here:
*
*  https://github.com/javaswift/cloudie
*  package (src/main/java): org.javaswift.cloudie.login;
*  
*  Some minor changes have been made by Loic Merckel, such as:
*  - adding localization 
*
*/

package org.swiftexplorer.gui.login;

import org.swiftexplorer.gui.MainPanel;
import org.swiftexplorer.gui.localization.HasLocalizedStrings;
import org.swiftexplorer.gui.login.CredentialsStore.Credentials;
import org.swiftexplorer.gui.util.LabelComponentPanel;
import org.swiftexplorer.gui.util.ReflectionAction;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;



public class LoginPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public interface LoginCallback {
        void doLogin(String authUrl, String tenant, String username, char[] pass, String preferredRegion);
    }

    private Action okAction = null ;
    private Action saveAction = null ;
    private Action deleteAction = null ;
    private Action cancelAction = null ;

    private JButton okButton = null ;
    private JButton cancelButton = null ;
    private JButton saveButton = null ;
    private JButton deleteButton = null ;

    private DefaultComboBoxModel<Credentials> model = new DefaultComboBoxModel<Credentials>();
    private JComboBox<Credentials> savedCredentials = new JComboBox<Credentials>(model);
    private JTextField authUrl = new JTextField();
    private JTextField tenant = new JTextField();
    private JTextField username = new JTextField();
    private JPasswordField password = new JPasswordField();
    private JTextField preferredRegion = new JTextField();
    private JLabel warningLabel = null ;

    private LoginCallback callback;
    private JDialog owner;
    private ActionListener comboActionListener;
    private CredentialsStore credentialsStore;
    
    private final HasLocalizedStrings stringsBundle ;

    public LoginPanel(LoginCallback callback, CredentialsStore credentialsStore, HasLocalizedStrings stringsBundle) {
        super(new BorderLayout(0, 0));
        
        this.callback = callback;
        this.credentialsStore = credentialsStore;
        this.stringsBundle = stringsBundle ;
        
        initMenuActions () ;
        
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        Box outer = Box.createVerticalBox();
        Box box = Box.createVerticalBox();
        Box btn = Box.createHorizontalBox();
        box.setBorder(BorderFactory.createTitledBorder(getLocalizedString("Credentials")));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JPanel warn = new JPanel(new BorderLayout());
        warn.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
        warn.add(warningLabel, BorderLayout.CENTER);
        buttons.setBorder(BorderFactory.createEtchedBorder());
        buttons.add(okButton);
        buttons.add(cancelButton);
        //
        saveButton.setToolTipText(getLocalizedString("Save_Credentials"));
        deleteButton.setToolTipText(getLocalizedString("Delete_Credentials"));
        btn.add(saveButton);
        btn.add(deleteButton);
        //
        box.add(new LabelComponentPanel("", savedCredentials, btn));
        box.add(new LabelComponentPanel("AuthURL", authUrl));
        box.add(new LabelComponentPanel("Tenant", tenant));
        box.add(new LabelComponentPanel("Username", username));
        box.add(new LabelComponentPanel("Password", password));
        box.add(new LabelComponentPanel("Preferred Region (optional)", preferredRegion));
        //
        outer.add(box);
        outer.add(warn);
        this.add(outer, BorderLayout.NORTH);
        this.add(buttons, BorderLayout.SOUTH);
        //
        bindSelectionListener();
        refreshCredentials();
        enableDisable();
        bindDocumentListeners();
    }
    
    private void initMenuActions () 
    {
        okAction = new ReflectionAction<LoginPanel>(getLocalizedString("Ok"), MainPanel.getIcon("server_connect.png"), this, "onOk");
        saveAction = new ReflectionAction<LoginPanel>("", MainPanel.getIcon("table_save.png"), this, "onSave");
        deleteAction = new ReflectionAction<LoginPanel>("", MainPanel.getIcon("table_delete.png"), this, "onDelete");
        cancelAction = new ReflectionAction<LoginPanel>(getLocalizedString("Cancel"), this, "onCancel");
        
        okButton = new JButton(okAction);
        cancelButton = new JButton(cancelAction);
        saveButton = new JButton(saveAction);
        deleteButton = new JButton(deleteAction);
        
        warningLabel = new JLabel(getLocalizedString("warning_credentials_stored_plain_text"), MainPanel.getIcon("table_error.png"), JLabel.CENTER);
    }

    private void bindSelectionListener() {
        comboActionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = savedCredentials.getSelectedIndex();
                if (selectedIndex <= 0) {
                    clearLoginForm();
                } else {
                    Credentials cr = (Credentials) savedCredentials.getSelectedItem();
                    if (cr != null) {
                        authUrl.setText(cr.authUrl);
                        tenant.setText(cr.tenant);
                        username.setText(cr.username);
                        password.setText(String.valueOf(cr.password));
                        preferredRegion.setText(cr.preferredRegion);
                        enableDisable();
                    }
                }
            }
        };
        savedCredentials.addActionListener(comboActionListener);
    }

    private void clearLoginForm() {
        authUrl.setText("");
        tenant.setText("");
        username.setText("");
        password.setText("");
        preferredRegion.setText("");
        enableDisable();
    }

    private void refreshCredentials() {
        savedCredentials.removeActionListener(comboActionListener);
        try {
            model.removeAllElements();
            for (Credentials cr : credentialsStore.getAvailableCredentials()) {
                model.addElement(cr);
            }
            if (model.getSize() > 0) {
                Credentials credentials = new Credentials();
                credentials.tenant = "";
                credentials.username = "";
                credentials.password = new char[0];
                credentials.authUrl = "";
                credentials.preferredRegion = "";
                model.insertElementAt(credentials, 0);
                savedCredentials.setSelectedIndex(0);
            }
        } finally {
            savedCredentials.addActionListener(comboActionListener);
        }
    }

    private void bindDocumentListeners() {
        DocumentListener lst = new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                enableDisable();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                enableDisable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                enableDisable();
            }
        };
        authUrl.getDocument().addDocumentListener(lst);
        tenant.getDocument().addDocumentListener(lst);
        username.getDocument().addDocumentListener(lst);
        password.getDocument().addDocumentListener(lst);
        preferredRegion.getDocument().addDocumentListener(lst);
    }

    private void enableDisable() {
        saveAction.setEnabled(isAuthComplete());
        deleteAction.setEnabled(savedCredentials.getSelectedIndex() > 0);
        savedCredentials.setEnabled(model.getSize() > 0);
    }

    private boolean isAuthComplete() {
        return !authUrl.getText().isEmpty() && !tenant.getText().isEmpty() && !username.getText().isEmpty() && !(password.getPassword().length == 0);
    }

    public void onShow() {
        authUrl.requestFocus();
    }

    public void onOk() {
        callback.doLogin(authUrl.getText().trim(), tenant.getText().trim(), username.getText().trim(), password.getPassword(), preferredRegion.getText().trim());
    }

    public void onCancel() {
        owner.setVisible(false);
    }

    public void onSave() {
        Credentials cr = new Credentials();
        cr.authUrl = authUrl.getText().trim();
        cr.tenant = tenant.getText().trim();
        cr.username = username.getText().trim();
        cr.password = password.getPassword();
        cr.preferredRegion = preferredRegion.getText().trim();
        credentialsStore.save(cr);
        refreshCredentials();
        savedCredentials.setSelectedItem(cr);
        enableDisable();
    }

    public void onDelete() {

        if (confirm(getLocalizedString ("confirm_remove_credential"))) {
            credentialsStore.delete((Credentials) savedCredentials.getSelectedItem());
            refreshCredentials();
            clearLoginForm();
            enableDisable();
        }
    }

    public void setOwner(JDialog dialog) {
        this.owner = dialog;
        this.owner.getRootPane().setDefaultButton(okButton);
    }

    public boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, getLocalizedString("Confirmation"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private String getLocalizedString (String key)
    {
    	if (stringsBundle == null)
	    	return key.replace("_", " ") ;
    	return stringsBundle.getLocalizedString(key) ;
    }
}
