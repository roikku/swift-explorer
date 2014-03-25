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

package org.swiftexplorer.auth;

public class AccessToken {

	private final String access_token;
	private final int expires_in;
	private final String refresh_token;
	private final String token_type;
	
	public AccessToken(String access_token, int expires_in,
			String refresh_token, String token_type) {
		super();
		this.access_token = access_token;
		this.expires_in = expires_in;
		this.refresh_token = refresh_token;
		this.token_type = token_type;
	}

	public String getAccessToken() {
		return access_token;
	}

	public int getExpiresIn() {
		return expires_in;
	}

	public String getRefreshToken() {
		return refresh_token;
	}

	public String getTokenType() {
		return token_type;
	}
}
