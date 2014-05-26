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


package org.swiftexplorer.swift.util;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import org.swiftexplorer.auth.builder.api.HubicApi;
import org.swiftexplorer.auth.oauth.HubicOAuth20ServiceImpl;
import org.swiftexplorer.config.Configuration;
import org.swiftexplorer.config.auth.HasAuthenticationSettings;
import org.swiftexplorer.swift.SwiftAccess;

public final class HubicSwift {
	
	final private static Logger logger = LoggerFactory.getLogger(HubicSwift.class);
	
	private HubicSwift () { super () ; } ;
	
	private static final Gson gson  = new Gson () ;
	
	private static final String scope = "credentials.r" ;
	
	public static SwiftAccess getSwiftAccess ()
	{
    	final HasAuthenticationSettings authSettings = Configuration.INSTANCE.getAuthenticationSettings() ;
		String apiKey = authSettings.getClientId() ;
		String apiSecret = authSettings.getClientSecret() ;

		HubicOAuth20ServiceImpl service = (HubicOAuth20ServiceImpl) new ServiceBuilder()
				.provider(HubicApi.class).apiKey(apiKey).apiSecret(apiSecret)
				//.scope("account.r,links.rw,usage.r,credentials.r").callback(HubicApi.CALLBACK_URL)
				.scope(scope).callback(HubicApi.CALLBACK_URL)
				.build();
		
		Verifier verif = service.obtainVerifier();
		
		if (verif == null)
			return null ;
		
		Token accessToken = service.getAccessToken(null, verif);
		return getSwiftAccess (service, accessToken) ;
	}

	
	public static SwiftAccess refreshAccessToken(Token expiredToken) 
	{
		final HasAuthenticationSettings authSettings = Configuration.INSTANCE.getAuthenticationSettings();
		String apiKey = authSettings.getClientId();
		String apiSecret = authSettings.getClientSecret();

		HubicOAuth20ServiceImpl service = (HubicOAuth20ServiceImpl) new ServiceBuilder()
				.provider(HubicApi.class).apiKey(apiKey).apiSecret(apiSecret)
				//.scope("account.r,links.rw,usage.r,credentials.r")
				.scope(scope)
				.callback(HubicApi.CALLBACK_URL).build();

		Token accessToken = service.refreshAccessToken(expiredToken);	    
		return getSwiftAccess (service, accessToken) ;
	}
	
	
	private static SwiftAccess getSwiftAccess (HubicOAuth20ServiceImpl service, Token accessToken)
	{
		String urlCredential = HubicApi.CREDENTIALS_URL;
		
	    OAuthRequest request = new OAuthRequest(Verb.GET, urlCredential);
	    request.setConnectionKeepAlive(false);
	    service.signRequest(accessToken, request);
	    Response responseReq = request.send();
	    
	    SwiftAccess ret = gson.fromJson(responseReq.getBody(), SwiftAccess.class) ;
	    ret.setAccessToken(accessToken);
	    
	    logger.info("Swift access token expiry date: " + ret.getExpires());
	    
		return ret ;
	}
}
