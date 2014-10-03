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

package org.swiftexplorer.config.swift;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.javaswift.joss.instructions.UploadInstructions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwiftSettingsImpl implements HasSwiftSettings {

	final private Logger logger = LoggerFactory.getLogger(SwiftSettingsImpl.class);
	
	private volatile XMLConfiguration config = null ;
	final private String baseProperty ;
	
	private final long defaultSegmentationSize = 104857600 ; //UploadInstructions.MAX_SEGMENTATION_SIZE ;
	private volatile long segmentationSize = defaultSegmentationSize ; 
	
	private volatile boolean hideSegmentsContainers = true ;
	
	private volatile String preferredRegion = null ;
	
	public SwiftSettingsImpl (String baseProperty)
	{
		super () ;
		this.baseProperty = baseProperty ;
	}
	
	public synchronized void setConfig (XMLConfiguration config) 
	{
		this.config = config ;
		if (this.config == null)
		{
			logger.info("Swift configuration cannot be set because the config parameter is null");
			return ;
		}
		// TODO: when the commom-configuration 2.0 will be released
		// Lock config
		// ...
		hideSegmentsContainers = this.config.getBoolean(baseProperty + ".hideSegmentsContainers", true) ;
		segmentationSize = Math.min(this.config.getLong(baseProperty + ".segmentationSize", defaultSegmentationSize), UploadInstructions.MAX_SEGMENTATION_SIZE) ;
		preferredRegion = this.config.getString(baseProperty + ".preferredRegion", null) ;
	}
	
	
	@Override
	public synchronized long getSegmentationSize() {
		return segmentationSize;
	}
	
	
	@Override
	public boolean hideSegmentsContainers() {
		return hideSegmentsContainers;
	}
	
	
	@Override
	public String getPreferredRegion() {
		return preferredRegion;
	}
	
	
	public synchronized void update (SwiftParameters swiftParam) throws ConfigurationException
	{
		if (config == null)
		{
			logger.info("Swift configuration cannot be updated because the config parameter is null");
			return ;
		}
		if (swiftParam == null)
		{
			logger.info("Swift configuration cannot be updated because the new SwiftParameters is null");
			return ;
		}
		
		this.segmentationSize = Math.min(swiftParam.getSegmentationSize(), UploadInstructions.MAX_SEGMENTATION_SIZE) ;
		this.hideSegmentsContainers = swiftParam.hideSegmentsContainers() ;
		this.preferredRegion = swiftParam.getPreferredRegion() ;
				
		config.setProperty(baseProperty + ".segmentationSize", segmentationSize);	
		config.setProperty(baseProperty + ".hideSegmentsContainers", hideSegmentsContainers);	
		if (this.preferredRegion != null && !this.preferredRegion.isEmpty())
			config.setProperty(baseProperty + ".preferredRegion", preferredRegion);
		config.save();
	}
}
