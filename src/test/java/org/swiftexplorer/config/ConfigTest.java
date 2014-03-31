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

import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.config.Configuration;
import org.swiftexplorer.config.auth.HasAuthenticationSettings;
import org.swiftexplorer.config.localization.HasLocalizationSettings;
import org.swiftexplorer.config.proxy.HasProxySettings;
import org.swiftexplorer.config.swift.HasSwiftSettings;

public class ConfigTest {

	private static boolean printout = false ;
	
	final private static Logger logger = LoggerFactory.getLogger(ConfigTest.class);
	
	public static boolean test ()
	{
		// default values
		if (80 != Configuration.INSTANCE.getHttpProxySettings().getPort()) 
			return false ;
		if (443 != Configuration.INSTANCE.getHttpsProxySettings().getPort()) 
			return false ;
		
		// load the test settings
		try 
		{
			Configuration.INSTANCE.load(new ConfigTest().getClass().getResource("/swiftexplorer-settings-test.xml").getFile()) ;
		} 
		catch (ConfigurationException e) 
		{
			logger.error("Error occurred while loading the cinfiguration file", e);
			return false ;
		}
		
		HasProxySettings http = Configuration.INSTANCE.getHttpProxySettings() ;
		
		if (http.isActive())
			return false ;
		if (!"user-http".equals(http.getUsername()))
			return false ;
		if (!"password-http".equals(http.getPassword()))
			return false ;
		if (!"host-http".equals(http.getHost()))
			return false ;
		if (9000 != http.getPort())
			return false ;
		
		if (printout)
		{
			logger.debug ("HTTP proxy config test") ;
			logger.debug ("Active: " + http.isActive()) ;
			logger.debug ("User: " + http.getUsername()) ;
			logger.debug ("Password: " + http.getPassword()) ;
			logger.debug ("Host: " + http.getHost()) ;
			logger.debug ("Port: " + http.getPort()) ;
		}
		
		HasProxySettings https = Configuration.INSTANCE.getHttpsProxySettings() ;
		
		if (https.isActive())
			return false ;
		if (!"user-https".equals(https.getUsername()))
			return false ;
		if (!"password-https".equals(https.getPassword()))
			return false ;
		if (!"host-https".equals(https.getHost()))
			return false ;
		if (9001 != https.getPort())
			return false ;
		
		if (printout)
		{
			logger.debug ("HTTPS proxy config test") ;
			logger.debug ("Active: " + https.isActive()) ;
			logger.debug ("User: " + https.getUsername()) ;
			logger.debug ("Password: " + https.getPassword()) ;
			logger.debug ("Host: " + https.getHost()) ;
			logger.debug ("Port: " + https.getPort()) ;
		}
		
		HasAuthenticationSettings auth = Configuration.INSTANCE.getAuthenticationSettings() ;
		
		if (!"api_key".equals(auth.getClientId()))
			return false ;
		if (!"api_secret".equals(auth.getClientSecret()))
			return false ;
		if (!"http://localhost:9000/".equals(auth.getCallBackUrl()))
			return false ;
		
		if (printout)
		{
			logger.debug ("Authentification config test") ;
			logger.debug ("Key: " + auth.getClientId()) ;
			logger.debug ("Secret: " + auth.getClientSecret()) ;
			logger.debug ("Callback URL: " + auth.getCallBackUrl()) ;
		}
		
		HasLocalizationSettings localizationSettings = Configuration.INSTANCE.getLocalizationSettings() ;
		
		if (!HasLocalizationSettings.LanguageCode.en.equals(localizationSettings.getLanguage()))
			return false ;
		if (!HasLocalizationSettings.RegionCode.US.equals(localizationSettings.getRegion()))
			return false ;
		
		if (printout)
		{
			logger.debug ("Language: " + localizationSettings.getLanguage().toString()) ;
			logger.debug ("Region: " + localizationSettings.getRegion().toString()) ;
		}
		
		HasSwiftSettings swiftSettings = Configuration.INSTANCE.getSwiftSettings() ;
		if (50000000 != swiftSettings.getSegmentationSize())
			return false ;
		
		if (printout)
		{
			logger.debug ("Segmentation Size: " + swiftSettings.getSegmentationSize()) ;
		}
			
		return true ;
	}
}
