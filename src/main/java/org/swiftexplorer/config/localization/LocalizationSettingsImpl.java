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

package org.swiftexplorer.config.localization;

import java.util.Locale;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalizationSettingsImpl implements HasLocalizationSettings {

	final private Logger logger = LoggerFactory.getLogger(LocalizationSettingsImpl.class);
	
	private volatile XMLConfiguration config = null ;
	
	final private String baseProperty ; 
	
	private LanguageCode language = null ;
	private RegionCode region = null ;

	public LocalizationSettingsImpl(String baseProperty) {
		super();
		this.baseProperty = baseProperty;
	}

	public synchronized void setConfig (XMLConfiguration config) 
	{
		this.config = config ;
		if (this.config == null)
		{
			logger.info("Proxy cannot be set because the config parameter is null");
			return ;
		}
		// TODO: when the commom-configuration 2.0 will be released
		// Lock config
		// ...
		
		language = LanguageCode.en ;
		String langStr =  this.config.getString(baseProperty + ".language") ;
		try
		{
			if (langStr != null && !langStr.isEmpty())
				language = LanguageCode.valueOf(langStr) ;
			else
				language = LanguageCode.valueOf(Locale.getDefault().getLanguage()) ;
		}
		catch (IllegalArgumentException e)
		{
			logger.info("Invalid or unsupported language code", e);
		}
		
		region = null ;
		String regStr = this.config.getString(baseProperty + ".region") ; 
		try
		{
			if (regStr != null && !regStr.isEmpty())
				region = RegionCode.valueOf(regStr) ;
		}
		catch (IllegalArgumentException e)
		{
			logger.info("Invalid or unsupported region code", e);
		}
	}
	
	public synchronized void update (LanguageCode language, RegionCode region) throws ConfigurationException
	{
		if (config == null)
		{
			logger.info("Proxy cannot be updated because the config parameter is null");
			return ;
		}
		
		this.language = language ;
		this.region = region ;
		
		config.setProperty(baseProperty + ".language", language.toString());	
		config.setProperty(baseProperty + ".region", (region != null) ? (region.toString()) : ("")) ;
		
		config.save();
	}
	
	@Override
	public synchronized LanguageCode getLanguage() {
		return language;
	}

	@Override
	public synchronized RegionCode getRegion() {
		return region;
	}
}
