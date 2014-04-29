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

/* This file incorporates work covered by the following copyright and  
 * permission notice:  
 *  
 * Copyright 2013 Robert Bor
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Some function have been adapted from 
 * - Joss (http://joss.javaswift.org/), package org.javaswift.joss.client.core, class AbstractStoredObject.java
 * - Joss (http://joss.javaswift.org/), package org.javaswift.joss.client.core, class AbstractContainer.java 
 */

package org.swiftexplorer.swift.operations;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javaswift.joss.client.core.AbstractContainer;
import org.javaswift.joss.client.core.AbstractStoredObject;
import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.headers.object.ObjectManifest;
import org.javaswift.joss.instructions.SegmentationPlan;
import org.javaswift.joss.instructions.UploadInstructions;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.swift.operations.SwiftOperations.SwiftCallback;
import org.swiftexplorer.swift.util.SwiftUtils;
import org.swiftexplorer.util.FileUtils;

class LargeObjectManager {

    final Logger logger = LoggerFactory.getLogger(LargeObjectManager.class);

    private final Account account ;
    private boolean checkExistingSegments = true ;
    
	LargeObjectManager (Account account) { 
		super () ; 
		this.account = account ;
	} ;
	    
    
    // Code taken from Joss, package org.javaswift.joss.client.core, class AbstractStoredObject.java 
    // and adapted here.
    public void uploadObjectAsSegments(StoredObject obj, UploadInstructions uploadInstructions, long size, ProgressInformation progInfo, SwiftCallback callback) 
    {    	
    	Container segmentsContainer = getSegmentsContainer (obj, true) ;
    	
    	AbstractContainer abstractContainer = (AbstractContainer)segmentsContainer ;
    	uploadSegmentedObjects(abstractContainer, (AbstractStoredObject)obj, uploadInstructions, size, progInfo, callback);
    	
    	StringBuilder sb = new StringBuilder () ;
    	sb.append(segmentsContainer.getName()) ;
    	sb.append(obj.getPath().replaceFirst(((AbstractStoredObject)obj).getContainer().getPath(), "")) ;
    	
        UploadInstructions manifest = new UploadInstructions(new byte[] {})
				        .setObjectManifest(new ObjectManifest(sb.toString())) // Manifest does not accept preceding slash
				        .setContentType(uploadInstructions.getContentType()
	        		);
        obj.uploadObject(manifest);
    }


    public boolean isSegmented (StoredObject obj)
    {
    	if (obj == null)
    		return false ;
    	if (!obj.exists())
    		return false ;
    	if (SwiftUtils.directoryContentType.equals(obj.getContentType()))
    		return false ;
    	Container segCont = getSegmentsContainer (obj, false) ;
    	if (segCont == null || !segCont.exists())
    		return false ;
    	StoredObject segObj = getObjectSegment ((AbstractContainer)segCont, (AbstractStoredObject)obj, Long.valueOf(1)) ;    	
    	return segObj != null && segObj.exists() ;
    }
	
    
    public List<StoredObject> getSegmentsList (StoredObject obj)
    {
    	return getSegmentsList (obj, 0) ;
    }
    
	
    public List<StoredObject> getSegmentsList (StoredObject obj, long offset)
    {
    	List<StoredObject> ret = new ArrayList<StoredObject> () ;
    	Container segCont = getSegmentsContainer (obj, false) ;
    	if (segCont == null || !segCont.exists())
    		return ret ;
    	int segCount = 1 ;
    	StoredObject segObj = getObjectSegment ((AbstractContainer)segCont, (AbstractStoredObject)obj, Long.valueOf(segCount)) ;
    	while (segObj != null && segObj.exists())
    	{
    		if (segCount >= (offset + 1))
    			ret.add (segObj) ;
    		++segCount ;
    		segObj = getObjectSegment ((AbstractContainer)segCont, (AbstractStoredObject)obj, Long.valueOf(segCount)) ;
    	}
    	return ret ;
    }
    
    
    private Container getSegmentsContainer (StoredObject obj, boolean createIfNeeded)
    {
    	StringBuilder segmentsContainerName = new StringBuilder () ;
    	segmentsContainerName.append(((AbstractStoredObject)obj).getContainer().getName()) ;
    	segmentsContainerName.append(SwiftUtils.segmentsContainerPostfix) ;
    	
    	Container segmentsContainer = account.getContainer(segmentsContainerName.toString()) ;
    	if (createIfNeeded && !segmentsContainer.exists())
    	{
    		segmentsContainer.create() ;
    		segmentsContainer.makePrivate();
    	}
    	return segmentsContainer ;
    }
    
    
    // Code taken from Joss, package org.javaswift.joss.client.core, class AbstractContainer.java 
    // and adapted here.
    private void uploadSegmentedObjects(AbstractContainer abstractContainer, AbstractStoredObject obj, UploadInstructions uploadInstructions, long size, ProgressInformation progInfo, SwiftCallback callback) 
    {
    	if (size < uploadInstructions.getSegmentationSize())
    		throw new AssertionError (String.format("The file size (%d) must be greater than the segmentation size (%d)", size, uploadInstructions.getSegmentationSize())) ;

    	StringBuilder pathBuilder = new StringBuilder () ;
    	pathBuilder.append(abstractContainer.getName()) ;
    	pathBuilder.append(SwiftUtils.separator) ;
    	pathBuilder.append(obj.getName()) ;
        String path = pathBuilder.toString()  ;
        try 
        {
            logger.info("Setting up a segmentation plan for " + path);
            
            Map<Long, String> md5PlanMap = null ;
            String currMsg = progInfo.getCurrentMessage() ;
            if (checkExistingSegments && !obj.exists())
            	md5PlanMap = getMd5PlanMap (uploadInstructions, obj, progInfo) ;
            SegmentationPlan plan = uploadInstructions.getSegmentationPlan();
            long numSegments = getNumberOfSegments (size, uploadInstructions) ;
            InputStream segmentStream = FileUtils.getInputStreamWithProgressFilter(progInfo, uploadInstructions.getSegmentationSize(), plan.getNextSegment()) ;
            while (segmentStream != null) 
            {
            	Long planSeg = plan.getSegmentNumber() ;
            	logger.info("Uploading segment " + planSeg);
            	progInfo.setCurrentMessage(String.format("%s (segment %d / %d)", currMsg, planSeg, numSegments));
                StoredObject segment = getObjectSegment(abstractContainer, obj, planSeg);  
                // check if this segment can be ignored
                boolean ignore = false ;
                if (md5PlanMap != null && !md5PlanMap.isEmpty())
                {
	                if (segment.exists() && md5PlanMap.containsKey(planSeg))
	                {
	        			String etag = segment.getEtag() ;
	        			String md5 = md5PlanMap.get(planSeg) ;
	        			if (etag != null && etag.equals(md5))
	        				ignore = true ; 
	                }
	                if (ignore)
	                	logger.info("{} already exists and has not changed (it won't be uploaded again)", String.format("Segment %d / %d", planSeg, numSegments)) ;
                }
                if (!ignore)
                	segment.uploadObject(segmentStream);
                segmentStream.close();
                segmentStream = FileUtils.getInputStreamWithProgressFilter(progInfo, (planSeg + 1 != numSegments)?(uploadInstructions.getSegmentationSize()):(size % uploadInstructions.getSegmentationSize()), plan.getNextSegment());
            }
            // we must remove extra segments that might remain from a previous large object with the same name
            cleanUpExtraSegments (obj, numSegments) ;
        } 
        catch (IOException err) 
        {
        	logger.error("Failed to set up a segmentation plan for " + path + ": " + err.getMessage());
        	callback.onError(new CommandException("Unable to upload segments", err));
        }
    }
    
    
    private void cleanUpExtraSegments (AbstractStoredObject obj, long numSegments)
    {
    	List<StoredObject> list =  getSegmentsList (obj, numSegments) ;
    	for (StoredObject so : list)
    	{
    		logger.info("Delete unused segment ({})", so.getBareName()) ;
    		so.delete(); 
    	}
    }
    
    
    private long getNumberOfSegments (long totalSize, UploadInstructions uploadInstructions)
    {
    	return totalSize / uploadInstructions.getSegmentationSize() + (long)((totalSize % uploadInstructions.getSegmentationSize() == 0)?(0):(1)) ;
    }
    
    
	@SuppressWarnings("unused")
	private Map<Long, String> getMd5PlanMap (UploadInstructions uploadInstructions) throws IOException
    {
    	Map<Long, String> ret = new HashMap<Long, String> () ;
    	SegmentationPlan plan = uploadInstructions.getSegmentationPlan();
    	InputStream segmentStream = plan.getNextSegment() ;
        while (segmentStream != null) 
        {
        	String md5 = FileUtils.readAllAndgetMD5(segmentStream) ;
        	if (md5 != null && !md5.isEmpty())
        		ret.put(plan.getSegmentNumber(), md5) ;
            segmentStream = plan.getNextSegment() ;
        }
        return ret ;
    }
	
	
	private Map<Long, String> getMd5PlanMap (UploadInstructions uploadInstructions, AbstractStoredObject obj, ProgressInformation progInfo) throws IOException
    {
		if (obj == null /*|| !isSegmented (obj)*/)
			return java.util.Collections.emptyMap() ;		
		List<StoredObject> listSeg = getSegmentsList (obj) ;
		final int numberOfExistingSegments = listSeg.size() ;

    	Map<Long, String> ret = new HashMap<Long, String> () ;
    	SegmentationPlan plan = uploadInstructions.getSegmentationPlan();
    	InputStream segmentStream = (progInfo == null) ? (plan.getNextSegment()) : (FileUtils.getInputStreamWithProgressFilter(progInfo, uploadInstructions.getSegmentationSize(), plan.getNextSegment())) ;
    	int count = 0 ;
        while (segmentStream != null) 
        {
            if (count >= numberOfExistingSegments)
            	break ;
            
            progInfo.setCurrentMessage(String.format("Hashing segment %d / %d", count + 1, numberOfExistingSegments)) ;
            
        	String md5 = FileUtils.readAllAndgetMD5(segmentStream) ;
        	if (md5 != null && !md5.isEmpty())
        		ret.put(plan.getSegmentNumber(), md5) ;
            segmentStream = (progInfo == null) ? (plan.getNextSegment()) : (FileUtils.getInputStreamWithProgressFilter(progInfo, uploadInstructions.getSegmentationSize(), plan.getNextSegment())) ;
            
            ++count ;
        }
        return ret ;
    }
	
	
    // Code taken from Joss, package org.javaswift.joss.client.core, class AbstractContainer.java 
    // and adapted here.
    private StoredObject getObjectSegment(Container segmentsContainer, AbstractStoredObject obj, Long part) 
    {
    	if (part <= 0)
    		throw new AssertionError ("Segments are 1-indexed.") ;
    	StringBuilder segmentName = new StringBuilder () ;
    	segmentName.append (obj.getName()) ;
    	segmentName.append (SwiftUtils.separator) ;
    	segmentName.append (String.format("%08d", part.intValue())) ;
        return segmentsContainer.getObject(segmentName.toString());
    }
}
