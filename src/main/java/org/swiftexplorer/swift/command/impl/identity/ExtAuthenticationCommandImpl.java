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

package org.swiftexplorer.swift.command.impl.identity;

import org.swiftexplorer.config.Configuration;
import org.swiftexplorer.swift.SwiftAccess;
import org.swiftexplorer.swift.client.impl.ExtClientImpl.SwiftProvider;
import org.swiftexplorer.swift.util.HubicSwift;

import org.apache.http.client.HttpClient;
import org.javaswift.joss.command.impl.identity.AbstractSimpleAuthenticationCommandImpl;
import org.javaswift.joss.command.shared.identity.access.AccessBasic;
import org.javaswift.joss.headers.identity.XAuthKey;
import org.javaswift.joss.headers.identity.XAuthUser;
import org.javaswift.joss.model.Access;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtAuthenticationCommandImpl extends AbstractSimpleAuthenticationCommandImpl {
	
	final private Logger logger = LoggerFactory.getLogger(Configuration.class);
	
	private final SwiftAccess swiftAccess ;
	private final SwiftProvider swiftProvider ;
	
    public ExtAuthenticationCommandImpl(HttpClient httpClient, String url, String username, String password, SwiftAccess swiftAccess, SwiftProvider swiftProvider) {
        super(httpClient, url);
        setHeader(new XAuthUser(username));
        setHeader(new XAuthKey(password));
        this.swiftAccess = swiftAccess ;
        this.swiftProvider = swiftProvider ;
    }

    @Override
    public Access call() {
    	
    	logger.info("Refresh the access token.");
    	
    	if (swiftAccess == null)
    	{
    		logger.debug("The swiftAccess parameter is not valid.");
    		return null ;
    	}
    	
    	switch (swiftProvider)
    	{
	    	case HUBIC:
	    	{
	    		SwiftAccess newSwiftAccess = HubicSwift.refreshAccessToken(this.swiftAccess.getAccessToken()) ;
	
	        	AccessBasic access = new AccessBasic();
	    		access.setToken(newSwiftAccess.getToken());
	    		access.setUrl(newSwiftAccess.getEndpoint());
	        	return access ;
	    	}
			case GENERIC:
	    		default :
	    			throw new UnsupportedOperationException () ;
    	}
    }
}
