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

package org.swiftexplorer.config;

import org.swiftexplorer.config.auth.AuthenticationSettingsImpl;
import org.swiftexplorer.config.auth.HasAuthenticationSettings;
import org.swiftexplorer.config.localization.HasLocalizationSettings;
import org.swiftexplorer.config.localization.HasLocalizationSettings.LanguageCode;
import org.swiftexplorer.config.localization.HasLocalizationSettings.RegionCode;
import org.swiftexplorer.config.localization.LocalizationSettingsImpl;
import org.swiftexplorer.config.proxy.HasProxySettings;
import org.swiftexplorer.config.proxy.Proxy;
import org.swiftexplorer.config.proxy.ProxySettingsImpl;
import org.swiftexplorer.config.swift.HasSwiftSettings;
import org.swiftexplorer.config.swift.SwiftParameters;
import org.swiftexplorer.config.swift.SwiftSettingsImpl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Configuration implements HasConfiguration {
	INSTANCE ;
	
	final private Logger logger = LoggerFactory.getLogger(Configuration.class);
	
	private final String defaultXmlPath ; 
	
	private final String appName = "Swift Explorer" ;
	private final String appVersion = "1.0.7-SNAPSHOT" ;
	
	@Override
	public String getAppName ()
	{
		return appName ;
	}
	
	@Override
	public String getAppVersion ()
	{
		return appVersion ;
	}
	
	private Configuration ()
	{
		String app = appName.toLowerCase().replaceAll(" ", "") ; 
		StringBuilder sb = new StringBuilder ();
		sb.append(System.getProperty("user.home")) ;
		sb.append(File.separator) ;
		sb.append(".") ;
		sb.append(app) ;
		sb.append(File.separator) ;
		sb.append(appVersion) ;
		sb.append(File.separator) ;
		sb.append(app) ;
		sb.append("-settings.xml") ;
		defaultXmlPath = sb.toString() ;
	}
	
	private volatile XMLConfiguration config = null ;
	
	private final ProxySettingsImpl httpProxySettings = new ProxySettingsImpl ("proxy.http", 80, "http") ;
	private final ProxySettingsImpl httpsProxySettings = new ProxySettingsImpl ("proxy.https", 443, "https") ;
	
	private final AuthenticationSettingsImpl authenticationSettings = new AuthenticationSettingsImpl () ;
	
	private final LocalizationSettingsImpl localizationSettings = new LocalizationSettingsImpl ("localization") ;
	
	private final SwiftSettingsImpl swiftSettings = new SwiftSettingsImpl ("swift") ;
	
	public void load (String xmlPath) throws ConfigurationException
	{
		String settingFilePath = (xmlPath == null || xmlPath.isEmpty()) ? (defaultXmlPath) : (xmlPath) ;
		
		logger.debug("Load setting file {}.", settingFilePath);
		
		checkConfigFile (settingFilePath) ;
		try
		{
			config = new XMLConfiguration(settingFilePath);
		}
		catch (ConfigurationException e)
		{
			logger.error("Error occurred while opening the settings file", e);
		}
		if (config == null)
			return ;
		// TODO: when the commom-configuration 2.0 will be released
		// set the synchronizer
		//config.setSynchronizer(new ReadWriteSynchronizer());
		config.setThrowExceptionOnMissing(false);
		httpProxySettings.setConfig(config);
		httpsProxySettings.setConfig(config);
		authenticationSettings.setConfig(config);
		localizationSettings.setConfig(config);
		swiftSettings.setConfig(config);
		
		setProxy () ;
	}	
	
	
	private void checkConfigFile (String path)
	{
		try 
		{			
			File file = new File (path) ;
			if (!file.exists())
			{
				File parent = file.getParentFile() ;
				if (parent != null)
					parent.mkdirs() ;
				file.createNewFile() ;
				PrintWriter writer = new PrintWriter(file.getPath(), "UTF-8");
				writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?><settings></settings>" );
				writer.close();	
			}
		} 
		catch (IOException | SecurityException | NullPointerException e) 
		{
			logger.error("Error occurred while creating the settings file", e);
		}	
	}
	
	
	@Override
	public HasLocalizationSettings getLocalizationSettings ()
	{
		return localizationSettings ;
	}
	
	
	@Override
	public HasProxySettings getHttpProxySettings ()
	{
		return httpProxySettings ;
	}
	
	
	@Override
	public HasProxySettings getHttpsProxySettings ()
	{
		return httpsProxySettings ;
	}
	
	
	@Override
	public HasAuthenticationSettings getAuthenticationSettings ()
	{
		return authenticationSettings ;
	}
	
	
	@Override
	public HasSwiftSettings getSwiftSettings() 
	{
		return swiftSettings;
	}
	
	
	@Override
	public void updateProxy(Proxy newProxy) {
		
		try 
		{
			if ("http".equalsIgnoreCase(newProxy.getProtocol()))
			{
				httpProxySettings.update(newProxy);
				setProxySystemProperty (httpProxySettings, "http") ;
			}
			else if ("https".equalsIgnoreCase(newProxy.getProtocol()))
			{
				httpsProxySettings.update(newProxy);
				setProxySystemProperty (httpsProxySettings, "https") ;
			}
			else
			{
				logger.info("Unsupported proxy protocol: {}.", newProxy.getProtocol());
			}
		} 
		catch (ConfigurationException e) 
		{
			logger.error("Error occurred while updating the proxy settings", e);
		}
	}
	
	
	@Override
	public void updateLanguage(LanguageCode language, RegionCode region) 
	{	
		try 
		{
			if (localizationSettings != null)
				localizationSettings.update (language, region) ;
		} 
		catch (ConfigurationException e) 
		{
			logger.error("Error occurred while updating the language settings", e);
		}
	}
	
	
	@Override
	public void updateSwiftParameters(SwiftParameters newParameters) 
	{
		try 
		{
			swiftSettings.update(newParameters);
		} 
		catch (ConfigurationException e) 
		{
			logger.error("Error occurred while updating the Swift settings", e);
		}
	}
	
	
	private void setProxySystemProperty (HasProxySettings proxySettings, String prot)
	{
		if (proxySettings == null)
			return ;
		if (prot == null || prot.isEmpty())
			return ;
		
		prot = prot.toLowerCase() ;
		
    	if (proxySettings.isActive())
    	{
    		String host = proxySettings.getHost() ;
    		String user = proxySettings.getUsername() ;
    		String pwd = proxySettings.getPassword() ;
    		int port = proxySettings.getPort() ;
    		
    		if (host != null)
    			System.setProperty(prot + ".proxyHost", host);
	    	System.setProperty(prot + ".proxyPort", "" + port);
	    	if (user != null)
	    		System.setProperty(prot + ".proxyUser", user);
	    	if (pwd != null)
	    		System.setProperty(prot + ".proxyPassword", pwd);
    	}
	}
	
	
    private void setProxy ()
    {    	
    	setProxySystemProperty (httpProxySettings, "http") ;
    	setProxySystemProperty (httpsProxySettings, "https") ;
    	    	
		Authenticator.setDefault(new Authenticator() {
		    @Override
		    protected PasswordAuthentication getPasswordAuthentication() 
		    {
		        if (getRequestorType() == RequestorType.PROXY) 
		        {
		            String prot = getRequestingProtocol().toLowerCase();
		            
		            String host = System.getProperty(prot + ".proxyHost", "");
		            String port = System.getProperty(prot + ".proxyPort", "80");
		            String user = System.getProperty(prot + ".proxyUser", "");
		            String password = System.getProperty(prot + ".proxyPassword", "");

		            if (getRequestingHost().equalsIgnoreCase(host)) 
		            {
		                if (Integer.parseInt(port) == getRequestingPort()) 
		                    return new PasswordAuthentication(user, password.toCharArray());  
		            }
		        }
		        return null;
		    }  
		});
    }
}
