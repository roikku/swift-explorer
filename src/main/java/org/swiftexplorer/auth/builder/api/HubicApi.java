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

package org.swiftexplorer.auth.builder.api;


import org.swiftexplorer.auth.extractors.HubicTokenExtractorImpl;
import org.swiftexplorer.auth.oauth.HubicOAuth20ServiceImpl;
import org.swiftexplorer.config.Configuration;

import org.scribe.builder.api.DefaultApi20;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.scribe.utils.OAuthEncoder;
import org.scribe.utils.Preconditions;


public class HubicApi extends DefaultApi20 {

	private static final String AUTHORIZE_URL = "https://api.hubic.com/oauth/auth/?client_id=%s&redirect_uri=%s&response_type=code&state=RandomString";
	private static final String SCOPED_AUTHORIZE_URL = AUTHORIZE_URL + "&scope=%s";

	public static final String CREDENTIALS_URL = "https://api.hubic.com/1.0/account/credentials" ;
	
	public static final String CALLBACK_URL ; 
	static
	{
		String callback = Configuration.INSTANCE.getAuthenticationSettings().getCallBackUrl() ;
		CALLBACK_URL = ((callback == null || callback.isEmpty()) ? ("http://localhost:9000/") : (callback)) ;
	}

	@Override
	public String getAccessTokenEndpoint() {
		return "https://api.hubic.com/oauth/token";
	}

	@Override
	public Verb getAccessTokenVerb() {
		return Verb.POST;
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) 
	{
		Preconditions.checkValidUrl(config.getCallback(), "Must provide a valid url as callback.") ;
		if (config.hasScope()) 
		{
			return String.format(SCOPED_AUTHORIZE_URL, config.getApiKey(),
					OAuthEncoder.encode(config.getCallback()),
					OAuthEncoder.encode(config.getScope()));
		} 
		else 
		{
			return String.format(AUTHORIZE_URL, config.getApiKey(),
					OAuthEncoder.encode(config.getCallback()));
		}
	}
	
	@Override
	public OAuthService createService(OAuthConfig config)
	{
		return new HubicOAuth20ServiceImpl(this, config);
	}
	
	@Override
	public AccessTokenExtractor getAccessTokenExtractor()
	{
		return new HubicTokenExtractorImpl();
	}
}
