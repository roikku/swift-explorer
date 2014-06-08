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

package org.swiftexplorer.swift.client.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.config.HasConfiguration;
import org.swiftexplorer.config.proxy.HasProxySettings;
import org.swiftexplorer.swift.SwiftAccess;
import org.swiftexplorer.swift.command.impl.identity.ExtAuthenticationCommandImpl;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.client.factory.AuthenticationMethod;
import org.javaswift.joss.client.impl.AccountImpl;
import org.javaswift.joss.client.impl.ClientImpl;
import org.javaswift.joss.command.shared.factory.AuthenticationCommandFactory;
import org.javaswift.joss.command.shared.identity.AuthenticationCommand;
import org.javaswift.joss.command.shared.identity.access.AccessBasic;
import org.javaswift.joss.model.Account;


public class ExtClientImpl extends ClientImpl
{	
	public static enum SwiftProvider
	{
		GENERIC,
		HUBIC,
	}
	
	
	final static Logger logger = LoggerFactory.getLogger(ExtClientImpl.class);
	
	private SwiftAccess swiftAccess ;
	private org.apache.http.client.HttpClient httpClient;
	private HasConfiguration config = null ;
	
	
	public ExtClientImpl(AccountConfig accountConfig, SwiftAccess swiftAccess, HasConfiguration config) 
	{
		super(accountConfig);
	
		this.config = config ;
		this.swiftAccess = swiftAccess ;

		initHttpClient(accountConfig.getSocketTimeout(), this.config);
	}
	
    
	private void setProxySettings (org.apache.http.client.HttpClient client, HasProxySettings proxySettings, String prot)
	{
		if (client == null)
			return ;
        if (proxySettings == null || !proxySettings.isActive())
        	return ;
        if (prot == null || prot.isEmpty())
        	return ;
                    
    	org.apache.http.HttpHost proxy = new org.apache.http.HttpHost(proxySettings.getHost(), proxySettings.getPort(), prot) ;
    	client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy) ;
    	
    	CredentialsProvider credProvider = ((AbstractHttpClient) client).getCredentialsProvider();
    	credProvider.setCredentials(
    			new AuthScope(proxySettings.getHost(), proxySettings.getPort()), 
    			new UsernamePasswordCredentials(proxySettings.getUsername(), proxySettings.getPassword()));  
	}
	
	
	private org.apache.http.client.HttpClient newHttpClient (PoolingClientConnectionManager connectionManager)
	{
		return new DefaultHttpClient(connectionManager);
	}
	
	
    private void initHttpClient(int socketTimeout, HasConfiguration config) {
    	
        PoolingClientConnectionManager connectionManager = initConnectionManager();
        
        if(accountConfig.isDisableSslValidation()) {
            disableSslValidation(connectionManager);
        }
        
        //this.httpClient = new DefaultHttpClient(connectionManager);
        this.httpClient = newHttpClient (connectionManager) ;
        
        if (socketTimeout != -1) {
        	logger.info("JOSS / Set socket timeout on HttpClient: " + socketTimeout);
            HttpParams params = this.httpClient.getParams();
            HttpConnectionParams.setSoTimeout(params, socketTimeout);
        }
        
        // proxy setting
        if (config != null)
        {
	        HasProxySettings httpProxySettings = config.getHttpProxySettings() ; 
	        setProxySettings (this.httpClient, httpProxySettings, "http") ;
	        
	        //HasProxySettings httpsProxySettings = config.getHttpsProxySettings() ;
	        //setProxySettings (this.httpClient, httpsProxySettings, "https") ;
        }
    }
    

    @Override
    public AccountImpl authenticate() {
    	Account account = createAccount();
    	return (AccountImpl) account ;
    }
	
    
    // We need to override this method in order to manage the re-authentication
    @Override
    protected AuthenticationCommandFactory createFactory() {

        return new AuthenticationCommandFactory() {

			@Override
			public AuthenticationCommand createAuthenticationCommand(
					HttpClient httpClient,
					AuthenticationMethod authenticationMethod, String url,
					String tenantName, String tenantId, String username,
					String password, AuthenticationMethod.AccessProvider accessProvider) {
				
				return new ExtAuthenticationCommandImpl(httpClient, url, username, password, swiftAccess, SwiftProvider.HUBIC);
			}} ;
    }
    
    
	@Override
	protected AccountImpl createAccount() {
		
		// this will be necessary for re-authentication
		AuthenticationCommand command = this.factory
				.createAuthenticationCommand(httpClient,
						accountConfig.getAuthenticationMethod(),
						accountConfig.getAuthUrl(),
						accountConfig.getTenantName(),
						accountConfig.getTenantId(),
						accountConfig.getUsername(),
						accountConfig.getPassword(), null);

		AccessBasic access = new AccessBasic();

		access.setToken(swiftAccess.getToken());
		access.setUrl(swiftAccess.getEndpoint());

		return new AccountImpl(command, httpClient, access,
				accountConfig.isAllowCaching(),
				accountConfig.getTempUrlHashPrefixSource(), accountConfig.getDelimiter());
	}
    
    
    public ExtClientImpl setHttpClient(HttpClient httpClient) {
        if (httpClient != null) {
            this.httpClient = httpClient;
        }
        return this;
    }
}
