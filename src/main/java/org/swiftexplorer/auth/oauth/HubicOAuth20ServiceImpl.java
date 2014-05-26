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

package org.swiftexplorer.auth.oauth;

import org.swiftexplorer.auth.AccessToken;

import org.swiftexplorer.auth.builder.api.HubicApi;
import org.swiftexplorer.auth.extractors.HubicTokenExtractorImpl;
import org.swiftexplorer.auth.server.AuthHttpServer;
import org.swiftexplorer.auth.webbrowser.AuthWebView;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HubicOAuth20ServiceImpl implements OAuthService {

	private static final String VERSION = "2.0";
	
	final private Logger logger = LoggerFactory.getLogger(HubicOAuth20ServiceImpl.class);

	private final HubicApi api;
	private final OAuthConfig config;
	
	
	public HubicOAuth20ServiceImpl(HubicApi api, OAuthConfig config) {
		this.api = api;
		this.config = config;
	}
	
	
	public Token refreshAccessToken (Token expiredToken)
	{
		if (expiredToken == null)
			return null ;
		
		OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), api.getAccessTokenEndpoint());
		
		String authenticationCode = config.getApiKey() + ":" + config.getApiSecret() ;
		byte[] bytesEncodedAuthenticationCode = Base64.encodeBase64(authenticationCode.getBytes()); 
		request.addHeader ("Authorization", "Basic " + bytesEncodedAuthenticationCode) ;
		
		String charset = "UTF-8";
		
		request.setCharset(charset);
		request.setFollowRedirects(false);
		
		AccessToken at = new HubicTokenExtractorImpl ().getAccessToken(expiredToken.getRawResponse()) ;
		
		try
		{
			request.addBodyParameter("refresh_token", at.getRefreshToken());
			//request.addBodyParameter("refresh_token", URLEncoder.encode(at.getRefreshToken(), charset));
			request.addBodyParameter("grant_type", "refresh_token");
			request.addBodyParameter(OAuthConstants.CLIENT_ID, URLEncoder.encode(config.getApiKey(), charset));
			request.addBodyParameter(OAuthConstants.CLIENT_SECRET, URLEncoder.encode(config.getApiSecret(), charset));
		} 
		catch (UnsupportedEncodingException e) 
		{			
			logger.error("Error occurred while refreshing the access token", e);
		}
		
		Response response = request.send();
		Token newToken = api.getAccessTokenExtractor().extract(response.getBody());		
		// We need to keep the initial RowResponse because it contains the refresh token
		return new Token (newToken.getToken(), newToken.getSecret(), expiredToken.getRawResponse()) ;
	}
	

	@Override
	public Token getAccessToken(Token requestToken, Verifier verifier) {

		OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), api.getAccessTokenEndpoint());
		
		String authenticationCode = config.getApiKey() + ":" + config.getApiSecret() ;
		byte[] bytesEncodedAuthenticationCode = Base64.encodeBase64(authenticationCode.getBytes()); 
		request.addHeader ("Authorization", "Basic " + bytesEncodedAuthenticationCode) ;
		
		String charset = "UTF-8";
		
		request.setCharset(charset);
		request.setFollowRedirects(false);
		
		try
		{
			request.addBodyParameter(OAuthConstants.CODE, URLEncoder.encode(verifier.getValue(), charset));
			request.addBodyParameter(OAuthConstants.REDIRECT_URI, URLEncoder.encode(config.getCallback(), charset));
			request.addBodyParameter("grant_type", "authorization_code");
			request.addBodyParameter(OAuthConstants.CLIENT_ID, URLEncoder.encode(config.getApiKey(), charset));
			request.addBodyParameter(OAuthConstants.CLIENT_SECRET, URLEncoder.encode(config.getApiSecret(), charset));
		} 
		catch (UnsupportedEncodingException e) 
		{
			logger.error("Error occurred while getting the access token", e);
		}
		
		Response response = request.send();
		return api.getAccessTokenExtractor().extract(response.getBody());
	}

	
	public Verifier obtainVerifier ()
	{
		int port = 80 ;
		try 
		{
			port = new URL(config.getCallback()).getPort() ;
		} 
		catch (MalformedURLException e) 
		{
			logger.error("Error occurred while obtaining the code verifier", e);
		}
		
		AuthHttpServer httpServer = new AuthHttpServer(port);
		
		// here we assume that the server will be started by the time the
		// user enters the required information
		AuthWebView authWebView = AuthWebView.openNewBrowser (httpServer, getAuthorizationUrl(null)) ;
		
		Map<String, String> params = null ;
		try 
		{
			// the server is being started
			params = httpServer.startAndWaitForData() ;
			httpServer.stopServer();
			//authWebView.setVisible(false);
			
		} 
		catch (IOException | InterruptedException e) 
		{	
			logger.error("Error occurred while obtaining the code verifier", e);
		}
		finally
		{
			authWebView.dispose();
		}
		if (params == null)
			return null ;
		
		String code = params.get("code") ;
		
		return ((code == null) ? (null) : (new Verifier(code))) ;
		
		/*
		if (code != null)
			return new Verifier(code) ;
		
		StringBuilder sb = new StringBuilder () ; 
		sb.append("Error") ;
		if (params.containsKey("error"))
		{
			sb.append(": ") ;
			sb.append(params.get("error")) ;
		}
		if (params.containsKey("error_description"))
		{
			sb.append("\n Error Description: ") ;
			sb.append(params.get("error_description")) ;
		}
		return new Verifier(sb.toString()) ;*/
	}
	
	
	@Override
	public Token getRequestToken() 
	{
		throw new UnsupportedOperationException("Unsupported operation, please use 'getAuthorizationUrl' and redirect your users there");
	}
	
	
	@Override
	public String getVersion() {
		return VERSION;
	}


	@Override
	public void signRequest(Token accessToken, OAuthRequest request) {
		request.addHeader("Authorization",  "Bearer " + accessToken.getToken());
	}


	@Override
	public String getAuthorizationUrl(Token requestToken) {
		return api.getAuthorizationUrl(config);
	}
}
