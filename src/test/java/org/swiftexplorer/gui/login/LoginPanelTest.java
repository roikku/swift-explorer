/*
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
package org.swiftexplorer.gui.login;


import org.swiftexplorer.gui.login.LoginPanel.LoginCallback;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * LoginPanelTest.
 * @author E.Hooijmeijer
 */
public class LoginPanelTest {

    private LoginCallback callback;
    private CredentialsStore credentialsStore;

    @Before
    public void init() {
        callback = Mockito.mock(LoginCallback.class);
        credentialsStore = Mockito.mock(CredentialsStore.class);
    }

    @Test
    public void shouldCreate() {
        new LoginPanel(callback, credentialsStore, null);
    }

    @Test
    public void shouldOnOk() {
        LoginPanel loginPanel = new LoginPanel(callback, credentialsStore, null);
        loginPanel.onOk();
        //
        Mockito.verify(callback).doLogin(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(char[].class));
    }

}
