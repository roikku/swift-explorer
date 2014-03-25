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

package org.swiftexplorer.config.auth;

import org.apache.commons.configuration.XMLConfiguration;

public class AuthenticationSettingsImpl implements HasAuthenticationSettings {

	private XMLConfiguration config = null ;
	
	private String apiKey = null;
	private String apiSecret = null;
	private String callbackUrl = null;
	
	private final String myApiKey = "api_hubic_..." ;
	private final String myApiSecret = "..." ;
	
	@Override
	public synchronized String getClientId() {
		if (apiKey == null || apiKey.isEmpty())
			return myApiKey ;
		return apiKey;
	}

	@Override
	public synchronized String getClientSecret() {
		if (apiSecret == null || apiSecret.isEmpty())
			return myApiSecret ;
		return apiSecret;
	}
	
	public synchronized void setConfig (XMLConfiguration config) 
	{
		this.config = config ;
		
		if (this.config == null)
			return ;
		apiKey = this.config.getString("authentication.api.key") ;
		apiSecret = this.config.getString("authentication.api.secret") ;
		callbackUrl = this.config.getString("authentication.api.callback") ;
	}

	@Override
	public String getCallBackUrl() {
		return callbackUrl;
	}
}
