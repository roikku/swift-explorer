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

package org.swiftexplorer.swift.client.factory;

import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.client.factory.AuthenticationMethod;
import org.swiftexplorer.swift.SwiftAccess;
import org.swiftexplorer.swift.client.impl.HubicAccessProvider;

public class AccountConfigFactory {
	private AccountConfigFactory () {} ;
	
	
	public static AccountConfig getHubicAccountConfig (final SwiftAccess sa)
	{
	    AccountConfig accConf = new AccountConfig () ;
	    accConf.setDisableSslValidation(false);
	    accConf.setAuthenticationMethod(AuthenticationMethod.EXTERNAL);
	    accConf.setMock(false);
	    accConf.setAllowReauthenticate(true);
	    accConf.setAllowCaching(true) ;
	    accConf.setAccessProvider (getHubicAccessProvider (sa)) ;
	    return accConf ;
	}
	
	
    private static AuthenticationMethod.AccessProvider getHubicAccessProvider (final SwiftAccess sa)
    {
    	return new HubicAccessProvider (sa) ;
    }
    
	
	public static AccountConfig getKeystoneAccountConfig ()
	{
	    AccountConfig accConf = new AccountConfig () ;
	    accConf.setDisableSslValidation(false);
	    accConf.setAuthenticationMethod(AuthenticationMethod.KEYSTONE);
	    accConf.setMock(false);
	    accConf.setAllowReauthenticate(true);
	    accConf.setAllowCaching(true) ;
	    return accConf ;
	}
	
	public static AccountConfig getKeystoneV3AccountConfig ()
	{
	    AccountConfig accConf = new AccountConfig () ;
	    accConf.setDisableSslValidation(false);
	    accConf.setAuthenticationMethod(AuthenticationMethod.KEYSTONE_V3);
	    accConf.setMock(false);
	    accConf.setAllowReauthenticate(true);
	    accConf.setAllowCaching(true) ;
	    return accConf ;
	}
	
	public static AccountConfig getMockAccountConfig ()
	{
	    AccountConfig accConf = new AccountConfig () ;
	    accConf.setAllowCaching(true) ;
        accConf.setAllowReauthenticate(true) ;
        accConf.setAllowContainerCaching(true) ;
        accConf.setAuthUrl(null) ;
        accConf.setHashPassword(null) ;
        accConf.setMock(true) ;
        accConf.setMockAllowEveryone(true) ;
        accConf.setMockAllowObjectDeleter(true) ;
        accConf.setMockMillisDelay(0) ;
        accConf.setMockOnFileObjectStore(null) ;
        accConf.setPassword(null) ;
        accConf.setPreferredRegion(null) ;
        accConf.setPrivateHost(null) ;
        accConf.setPublicHost(null) ;
        accConf.setSocketTimeout(0) ;
        accConf.setTenantName(null) ;
        accConf.setTenantId(null) ;
        accConf.setUsername(null);
	    return accConf ;
	}
}
