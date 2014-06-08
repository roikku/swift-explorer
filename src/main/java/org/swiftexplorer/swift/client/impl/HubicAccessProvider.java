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

package org.swiftexplorer.swift.client.impl;

import org.javaswift.joss.client.factory.AuthenticationMethod;
import org.javaswift.joss.command.shared.identity.access.AccessBasic;
import org.javaswift.joss.model.Access;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.swift.SwiftAccess;
import org.swiftexplorer.swift.util.HubicSwift;

public class HubicAccessProvider implements AuthenticationMethod.AccessProvider{

	final private Logger logger = LoggerFactory.getLogger(HubicAccessProvider.class);
	
	private final SwiftAccess swiftAccess ;
	private boolean hasAlreadyAuthenticated = false ;
	
	
	public HubicAccessProvider(SwiftAccess swiftAccess) {
		super();
		if (swiftAccess == null)
			throw new NullPointerException () ;
		this.swiftAccess = swiftAccess;
	}

	
	@Override
	public Access authenticate() {
		AccessBasic access = new AccessBasic();
		if (!hasAlreadyAuthenticated)
		{	
    		access.setToken(swiftAccess.getToken());
    		access.setUrl(swiftAccess.getEndpoint());
    		hasAlreadyAuthenticated = true ;
		}
		else
		{
			logger.info("Refresh the access token.");
			
    		SwiftAccess newSwiftAccess = HubicSwift.refreshAccessToken(this.swiftAccess.getAccessToken()) ;
    		access.setToken(newSwiftAccess.getToken());
    		access.setUrl(newSwiftAccess.getEndpoint());
		}
		return access ;
	}
}
