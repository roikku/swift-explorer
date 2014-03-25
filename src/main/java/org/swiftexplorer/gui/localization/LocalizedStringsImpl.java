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

package org.swiftexplorer.gui.localization;

import org.swiftexplorer.gui.MainPanel;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalizedStringsImpl implements HasLocalizedStrings {

	final Logger logger = LoggerFactory.getLogger(LocalizedStringsImpl.class);
	
	private final ResourceBundle stringsBundle ;
	
    public LocalizedStringsImpl(Locale locale) {
		super();
		stringsBundle = getStringsBundle (locale) ;
	}

	private static ResourceBundle getStringsBundle(Locale locale) 
    {   	
    	URL[] urls = new URL[]{MainPanel.class.getResource("/strings/")};
    	ClassLoader loader = new URLClassLoader(urls);
        return ResourceBundle.getBundle("StringsBundle", locale, loader);
    }
	
	@Override
	public String getLocalizedString(String key) {
    	if (stringsBundle == null)
    	{
	    	return key.replace("_", " ") ;
    	}
    	if (key == null)
    		return null ;
    	String ret = key ;
    	try
    	{
    		ret = stringsBundle.getString(key) ;
    	}
    	catch (MissingResourceException e)
    	{
    		logger.error("Missing string in the StringsBundle: " + key, e);
    	}
    	// http://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle
    	if (ret != null)
    	{
        	try 
        	{
    			return new String(ret.getBytes("ISO-8859-1"), "UTF-8");
    		} 
        	catch (UnsupportedEncodingException e) 
        	{
        		logger.error("Error occurred while converting localized string encoding.", e);
    		}
    	}
    	return ret ;
	}
}
