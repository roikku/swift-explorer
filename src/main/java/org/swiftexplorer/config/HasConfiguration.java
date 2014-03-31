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


package org.swiftexplorer.config;

import org.swiftexplorer.config.auth.HasAuthenticationSettings;
import org.swiftexplorer.config.localization.HasLocalizationSettings;
import org.swiftexplorer.config.localization.HasLocalizationSettings.LanguageCode;
import org.swiftexplorer.config.localization.HasLocalizationSettings.RegionCode;
import org.swiftexplorer.config.proxy.HasProxySettings;
import org.swiftexplorer.config.proxy.Proxy;
import org.swiftexplorer.config.swift.HasSwiftSettings;
import org.swiftexplorer.config.swift.SwiftParameters;

public interface HasConfiguration {
	public String getAppName () ;
	public String getAppVersion () ;
	public HasProxySettings getHttpProxySettings () ;
	public HasProxySettings getHttpsProxySettings () ;
	public HasAuthenticationSettings getAuthenticationSettings () ;
	public HasLocalizationSettings getLocalizationSettings () ;
	public HasSwiftSettings getSwiftSettings () ;
	public void updateProxy (Proxy newProxy) ;
	public void updateLanguage (LanguageCode language, RegionCode region) ;
	public void updateSwiftParameters (SwiftParameters newParameters) ;
}
