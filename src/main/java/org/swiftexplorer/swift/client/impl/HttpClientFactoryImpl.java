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

/* This file incorporates work covered by the following copyright and  
 * permission notice:  
 *  
 * Copyright 2013 Robert Bor
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Some function have been adapted from 
 * - Joss (http://joss.javaswift.org/), package org.javaswift.joss.client.impl, class ClientImpl.java 
 */


package org.swiftexplorer.swift.client.impl;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.javaswift.joss.client.factory.AccountConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.config.proxy.HasProxySettings;
import org.swiftexplorer.swift.client.factory.HttpClientFactory;

public class HttpClientFactoryImpl implements HttpClientFactory {

	final private Logger logger = LoggerFactory.getLogger(HttpClientFactoryImpl.class);
	
	@Override
	public HttpClient getHttpClient(AccountConfig accountConfig, HasProxySettings proxySettings) {
    	
        PoolingClientConnectionManager connectionManager = initConnectionManager();
        
        if(accountConfig.isDisableSslValidation()) {
            disableSslValidation(connectionManager);
        }
        
        org.apache.http.client.HttpClient httpClient = newHttpClient (connectionManager) ;
        
        int socketTimeout = accountConfig.getSocketTimeout() ;
        if (socketTimeout != -1) {
        	logger.info("Set socket timeout on HttpClient: " + socketTimeout);
            HttpParams params = httpClient.getParams();
            HttpConnectionParams.setSoTimeout(params, socketTimeout);
        }
        
        // proxy setting
        if (proxySettings != null)
	        setProxySettings (httpClient, proxySettings, "http") ;
        
        return httpClient ;
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
	
	
    protected PoolingClientConnectionManager initConnectionManager() {
        PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
        connectionManager.setMaxTotal(50);
        connectionManager.setDefaultMaxPerRoute(25);
        return connectionManager;
    }
    
    
    protected void disableSslValidation(PoolingClientConnectionManager connectionManager) {
        try {
            connectionManager.getSchemeRegistry().register(
                    new Scheme("https", 443,
                            new SSLSocketFactory(createGullibleSslContext(),
                                    SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Could not initialize SSL Context: " + e, e);
        }
    }
    
    
    public static TrustManager[] gullibleManagers = new TrustManager[]{
        new X509TrustManager() {
            @Override
            public void checkClientTrusted( X509Certificate[] x509Certificates, String s ) throws CertificateException {
            }

            @Override
            public void checkServerTrusted( X509Certificate[] x509Certificates, String s ) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }
    };
    
    
    public static SSLContext createGullibleSslContext() throws GeneralSecurityException {
        SSLContext ctx = SSLContext.getInstance( "SSL" );
        ctx.init( null, gullibleManagers, new SecureRandom() );
        return ctx;
    }
}
