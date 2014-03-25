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
*  This file is based on the AccountFactory.java, 
*  which can be found here:
*
*  https://github.com/javaswift/joss
*  package (src/main.java): org.javaswift.joss.client.factory;
*
*  This initial work is covered by the following license 
*  (see http://joss.javaswift.org/ for further details):
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
*  
*/


package org.swiftexplorer.swift.client.factory;


import org.swiftexplorer.config.Configuration;
import org.swiftexplorer.swift.SwiftAccess;
import org.swiftexplorer.swift.client.impl.ExtClientImpl;

import org.apache.http.client.HttpClient;
import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.client.factory.AuthenticationMethod;
import org.javaswift.joss.client.factory.TempUrlHashPrefixSource;
import org.javaswift.joss.client.impl.AccountImpl;
import org.javaswift.joss.client.impl.ClientImpl;
import org.javaswift.joss.client.mock.ClientMock;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Client;


public class AccountFactory {

    private final AccountConfig config;

    private org.apache.http.client.HttpClient httpClient;

    public AccountFactory() {
        this(new AccountConfig());
    }
    

    private SwiftAccess swiftAccess = null ;
  
    public AccountFactory setSwiftAccess(SwiftAccess swiftAccess) {
        this.swiftAccess = swiftAccess ;
        return this;
    }

    public AccountFactory(AccountConfig config) {
        this.config = config;
    }

    @SuppressWarnings("unchecked")
	public Account createAccount() {
        final Client<AccountImpl> client;
        if (config.isMock()) 
            client = createClientMock();
        else 
        {
        	if (swiftAccess == null)
        		client = createClientImpl();
        	else
        		client = createExtClientImpl();
        }
        return client.authenticate();
    }

    @SuppressWarnings("rawtypes")
	public Client createClientMock() {
        return new ClientMock(config);
    }

    public Client<AccountImpl> createExtClientImpl() {
        return new ExtClientImpl(config, swiftAccess, Configuration.INSTANCE).setHttpClient(this.httpClient);
    }
    
    public Client<AccountImpl> createClientImpl() {
        return new ClientImpl(config).setHttpClient(this.httpClient);
    }
    
    public AccountFactory setTenantName(String tenantName) {
        this.config.setTenantName(tenantName);
        return this;
    }

    public AccountFactory setTenantId(String tenantId) {
        this.config.setTenantId(tenantId);
        return this;
    }

    public AccountFactory setUsername(String username) {
        this.config.setUsername(username);
        return this;
    }

    public AccountFactory setPassword(String password) {
        this.config.setPassword(password);
        return this;
    }

    public AccountFactory setAuthUrl(String authUrl) {
        this.config.setAuthUrl(authUrl);
        return this;
    }

    public AccountFactory setMock(boolean mock) {
        this.config.setMock(mock);
        return this;
    }

    public AccountFactory setPublicHost(String publicHost) {
        this.config.setPublicHost(publicHost);
        return this;
    }

    public AccountFactory setPrivateHost(String privateHost) {
        this.config.setPrivateHost(privateHost);
        return this;
    }

    public AccountFactory setMockMillisDelay(int mockMillisDelay) {
        this.config.setMockMillisDelay(mockMillisDelay);
        return this;
    }

    public AccountFactory setAllowReauthenticate(boolean allowReauthenticate) {
        this.config.setAllowReauthenticate(allowReauthenticate);
        return this;
    }

    public AccountFactory setAllowCaching(boolean allowCaching) {
        this.config.setAllowCaching(allowCaching);
        return this;
    }

    public AccountFactory setAllowContainerCaching(boolean allowContainerCaching) {
        this.config.setAllowContainerCaching(allowContainerCaching);
        return this;
    }

    public AccountFactory setMockAllowObjectDeleter(boolean mockAllowObjectDeleter) {
        this.config.setMockAllowObjectDeleter(mockAllowObjectDeleter);
        return this;
    }

    public AccountFactory setMockAllowEveryone(boolean mockAllowEveryone) {
        this.config.setMockAllowEveryone(mockAllowEveryone);
        return this;
    }

    public AccountFactory setMockOnFileObjectStore(String mockOnFileObjectStore) {
        this.config.setMockOnFileObjectStore(mockOnFileObjectStore);
        return this;
    }

    public AccountFactory setSocketTimeout(int socketTimeout) {
        this.config.setSocketTimeout(socketTimeout);
        return this;
    }

    public AccountFactory setPreferredRegion(String preferredRegion) {
        this.config.setPreferredRegion(preferredRegion);
        return this;
    }

    public AccountFactory setHashPassword(String hashPassword) {
        this.config.setHashPassword(hashPassword);
        return this;
    }

    public AccountFactory setTempUrlHashPrefixSource(TempUrlHashPrefixSource source) {
        this.config.setTempUrlHashPrefixSource(source);
        return this;
    }

    public AccountFactory setAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        this.config.setAuthenticationMethod(authenticationMethod);
        return this;
    }

    public AccountFactory setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }
}
