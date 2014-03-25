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

package org.swiftexplorer.config.proxy;

public class Proxy implements HasProxySettings {
	
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String prot ;
    private final boolean activated ;
    
    @Override
    public String getHost() {
		return host;
	}

    @Override
	public int getPort() {
		return port;
	}

    @Override
	public String getUsername() {
		return username;
	}

    @Override
	public String getPassword() {
		return password;
	}
	
	@Override
	public boolean isActive() {
		return activated;
	}

	@Override
	public String getProtocol() {
		return prot;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + port;
		result = prime * result + ((prot == null) ? 0 : prot.hashCode());
		result = prime * result
				+ ((username == null) ? 0 : username.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Proxy other = (Proxy) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (port != other.port)
			return false;
		if (prot == null) {
			if (other.prot != null)
				return false;
		} else if (!prot.equals(other.prot))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}

	public String toString() {
        return prot + ":\\" + host + ":" + port ;
    }
	
	private Proxy (Builder b)
	{
		super () ;
		this.username = b.username ;
		this.host = b.host ;
		this.port = b.port ;
		this.password = b.password ;
		this.activated = b.activated ;
		this.prot = b.prot ;
	}
	
	public static class Builder
	{
        private String host;
        private int port;
        private String username;
        private String password;
        private final String prot ;
        boolean activated ;
        
        public Builder (String prot)
        {
        	super () ;
        	this.prot = prot ;
        }
        
        public Builder setHost (String str)
        {
        	host = str ;
        	return this ;
        }
        
        public Builder setUsername (String str)
        {
        	username = str ;
        	return this ;
        }
        
        public Builder setPassword (String str)
        {
        	password = str ;
        	return this ;
        }
        
        public Builder setPort (int p)
        {
        	port = p ;
        	return this ;
        }
        
        public Builder setActivated (boolean b)
        {
        	activated = b ;
        	return this ;
        }
        
        public Proxy build ()
        {
        	return new Proxy (this) ;
        }
	}
}
