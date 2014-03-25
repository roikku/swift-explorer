/*
 *  Copyright 2014 Loic Merckel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.swiftexplorer.config.localization;

public interface HasLocalizationSettings {
	public LanguageCode getLanguage () ;
	public RegionCode getRegion () ;
	
    //http://www.loc.gov/standards/iso639-2/php/code_list.php
    public static enum LanguageCode
    {
    	en,
    	ja,
    	fr,
    	de,
    }
    
    public static enum RegionCode
    {
    	GB,
    	US,
    	JP,
    	FR,
    	DE,
    }
}
