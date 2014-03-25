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


package org.swiftexplorer.gui.settings;

import org.swiftexplorer.config.HasConfiguration;
import org.swiftexplorer.config.proxy.HasProxySettings;
import org.swiftexplorer.config.proxy.Proxy;

import java.util.ArrayList;
import java.util.List;

public class ProxiesStore {
	
	private HasConfiguration config ;
	
	
	public ProxiesStore (HasConfiguration config)
	{
		super ();
		this.config = config ;
	}
    
	private Proxy getProxy(HasProxySettings proxySettings, String prot) {
		return new Proxy.Builder(prot)
				.setActivated(proxySettings.isActive())
				.setHost(proxySettings.getHost())
				.setPassword(proxySettings.getPassword())
				.setPort(proxySettings.getPort())
				.setUsername(proxySettings.getUsername()).build();
	}
    
	
    public List<Proxy> getProxies() 
    {
        List<Proxy> results = new ArrayList<Proxy>();
        if (config == null)
        	return results ;

        // HTTP
        HasProxySettings httpProxy = config.getHttpProxySettings() ;
		if (httpProxy != null) 
		{
			results.add(getProxy (httpProxy, "http"));
		}
		// HTTPS
        HasProxySettings httpsProxy = config.getHttpsProxySettings() ;
		if (httpsProxy != null) 
		{
			results.add(getProxy (httpsProxy, "https"));
		}
        return results;
    }
}
