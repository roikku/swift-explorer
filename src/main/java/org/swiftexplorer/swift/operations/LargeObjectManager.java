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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.javaswift.joss.client.core.AbstractContainer;
import org.javaswift.joss.client.core.AbstractStoredObject;
import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.headers.object.ObjectManifest;
import org.javaswift.joss.instructions.SegmentationPlan;
import org.javaswift.joss.instructions.UploadInstructions;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.PaginationMap;
import org.javaswift.joss.model.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.swift.instructions.FastSegmentationPlanFile;
import org.swiftexplorer.swift.operations.SwiftOperations.SwiftCallback;
import org.swiftexplorer.swift.util.SwiftUtils;
import org.swiftexplorer.util.FileUtils;


class LargeObjectManager {

    final Logger logger = LoggerFactory.getLogger(LargeObjectManager.class);

    private final Account account ;
    private final boolean checkExistingSegments = true ;
 
    private static final int MAX_PAGE_SIZE = 9999;
    
	LargeObjectManager (Account account) { 
		super () ; 
		this.account = account ;
	} ;
	
	
    // Code taken from Joss, package org.javaswift.joss.client.core, class AbstractStoredObject.java 
    // and adapted here.
    public void uploadObjectAsSegments(StoredObject obj, File file, UploadInstructions uploadInstructions, long size, ProgressInformation progInfo, SwiftCallback callback) 
    {    	
    	Container segmentsContainer = getSegmentsContainer (obj, true) ;
    	
    	AbstractContainer abstractContainer = (AbstractContainer)segmentsContainer ;
    	uploadSegmentedObjects(abstractContainer, (AbstractStoredObject)obj, file, uploadInstructions, size, progInfo, callback);
    	
    	StringBuilder sb = new StringBuilder () ;
    	sb.append(segmentsContainer.getName()) ;
    	sb.append(obj.getPath().replaceFirst(((AbstractStoredObject)obj).getContainer().getPath(), "")) ;
    	
        UploadInstructions manifest = new UploadInstructions(new byte[] {})
				        .setObjectManifest(new ObjectManifest(sb.toString())) // Manifest does not accept preceding slash
				        .setContentType(uploadInstructions.getContentType()
	        		);
        obj.uploadObject(manifest);
    }
    
    
    public void uploadObjectAsSegments(StoredObject obj, UploadInstructions uploadInstructions, long size, ProgressInformation progInfo, SwiftCallback callback) 
    {    	
    	uploadObjectAsSegments(obj, null, uploadInstructions, size, progInfo, callback) ;
    }


    public boolean isSegmented (StoredObject obj)
    {
    	if (obj == null)
    		return false ;
    	if (!obj.exists())
    		return false ;
    	if (SwiftUtils.directoryContentType.equals(obj.getContentType()))
    		return false ;
    	String manifest = obj.getManifest() ;
    	if (manifest != null && !manifest.isEmpty())
    		return true ;
    	return false ;
    	/*
    	Container segCont = getSegmentsContainer (obj, false) ;
    	if (segCont == null || !segCont.exists())
    		return false ;
    	StoredObject segObj = getObjectSegment ((AbstractContainer)segCont, (AbstractStoredObject)obj, Long.valueOf(1)) ;    	
    	return segObj != null && segObj.exists() ;	*/
    }
    
    
    public String getSumOfSegmentsMd5 (StoredObject obj)
    {
    	if (obj == null)
    		return FileUtils.emptyMd5 ;
    	List<StoredObject> segments = getSegmentsList (obj) ;
    	StringBuilder sb = new StringBuilder () ;
    	for (StoredObject so : segments){
    		sb.append(so.getEtag()) ;
    	}
    	InputStream stream = new java.io.ByteArrayInputStream (sb.toString().getBytes(StandardCharsets.UTF_8));
    	try 
    	{
			return FileUtils.readAllAndgetMD5(stream) ;
		} 
    	catch (IOException e) {
			logger.error("Error occurred while computing md5 value", e) ;
		}
    	return "" ;
    }
	
    
    public List<StoredObject> getSegmentsList (StoredObject obj)
    {
    	return getSegmentsList (obj, 0, -1) ;
    }
    
	
    private List<StoredObject> getSegmentsList (StoredObject obj, long offset, long nobj)
    {
    	List<StoredObject> ret = new ArrayList<StoredObject> () ;
    	Container segCont = getSegmentsContainer (obj, false) ;
    	if (segCont == null || !segCont.exists())
    		return ret ;
    	
    	// First, we try to get the segments from the manifest
    	ret.addAll(getSegmentsListFromManifest (segCont, obj, offset, nobj)) ;
    	if (!ret.isEmpty())
    		return ret ;
    	
    	// If no segments were found using the manifest (typically, because the object does not exist,
    	// e.g., if only some segments were uploaded)
    	long segCount = 1 ;
    	StoredObject segObj = getObjectSegment ((AbstractContainer)segCont, (AbstractStoredObject)obj, Long.valueOf(segCount)) ;
    	while (segObj != null && segObj.exists())
    	{
    		if (segCount >= (offset + 1))
    			ret.add (segObj) ;
    		
    		if (nobj > 0 && ret.size() >= nobj)
    			return ret ;
    		
    		++segCount ;
    		segObj = getObjectSegment ((AbstractContainer)segCont, (AbstractStoredObject)obj, Long.valueOf(segCount)) ;
    	}
    	return ret ;
    }
    
    
    private List<StoredObject> getSegmentsListFromManifest (Container segCont, StoredObject obj, long offset, long nobj)
    {
    	List<StoredObject> ret = new ArrayList<StoredObject> () ;
    	if (!obj.exists())
    		return ret ;
		String manifest = obj.getManifest() ;
		if (manifest == null || manifest.isEmpty())
			return ret ;
		// http://docs.openstack.org/api/openstack-object-storage/1.0/content/large-object-creation.html
		// {container}/{prefix} 
		int index = manifest.indexOf(SwiftUtils.separator) ;
		if (index > 0)
		{
			long segCount = 1 ;
			String prefix = manifest.substring(index) ;
	        PaginationMap map = segCont.getPaginationMap(MAX_PAGE_SIZE);
	        for (int page = 0; page < map.getNumberOfPages(); page++) 
	        {
	        	for (StoredObject so : segCont.list(prefix, map.getMarker(page), map.getPageSize()))
	        	{
	        		if (segCount >= (offset + 1))
	        			ret.add (so) ;
	        		if (nobj > 0 && ret.size() >= nobj)
	        			return ret ;
	        		++segCount ;
	        	}
	        }
		}
    	return ret ;
    }
    
    
    /*
     * The object might have been segmented using another application, or using 
     * a different segmentSize value (than the one currently set). This method 
     * tries to return the real segment size of an existing object (it reads and 
     * returns the size of the first segment).
     */
    public long getActualSegmentSize (StoredObject obj)
    {
    	final long notFound = -1 ;
    	Container segCont = getSegmentsContainer (obj, false) ;
    	if (segCont == null || !segCont.exists())
    		return notFound ;
    	StoredObject segObj = getObjectSegment ((AbstractContainer)segCont, (AbstractStoredObject)obj, Long.valueOf(1)) ;
    	if (segObj == null || !segObj.exists())
    	{
    		// the object may have been segmented using another convention,
    		// we check using the manifest
	    	List<StoredObject> sgl = getSegmentsList(obj, 0, 1) ;
	    	if (!sgl.isEmpty())
	    		segObj = sgl.get(0) ;
    	}
    	if (segObj == null || !segObj.exists())
    		return notFound ;
    	return segObj.getContentLength() ;
    }
    
    
    private Container getSegmentsContainer (StoredObject obj, boolean createIfNeeded)
    {
    	String containerName = null ;
    	if (obj.exists())
    	{
    		String manifest = obj.getManifest() ;
    		if (manifest != null && !manifest.isEmpty())
    		{
    			// http://docs.openstack.org/api/openstack-object-storage/1.0/content/large-object-creation.html
    			// {container}/{prefix} 
    			int index = manifest.indexOf(SwiftUtils.separator) ;
    			if (index > 0) {
    				containerName = manifest.substring(0, index) ;
    			}
    		}
    	}
    	
    	if (containerName == null)
    	{
	    	StringBuilder segmentsContainerName = new StringBuilder () ;
	    	segmentsContainerName.append(((AbstractStoredObject)obj).getContainer().getName()) ;
	    	segmentsContainerName.append(SwiftUtils.segmentsContainerPostfix) ;
	    	containerName = segmentsContainerName.toString() ;
    	}
    	else
    		logger.info("Segment objects container obtained from the manifest: " + containerName) ;
    	
    	Container segmentsContainer = account.getContainer(containerName) ;
    	if (createIfNeeded && !segmentsContainer.exists())
    	{
    		segmentsContainer.create() ;
    		segmentsContainer.makePrivate();
    	}
    	return segmentsContainer ;
    }
    
    
    // Code taken from Joss, package org.javaswift.joss.client.core, class AbstractContainer.java 
    // and adapted here.
    private void uploadSegmentedObjects(AbstractContainer abstractContainer, AbstractStoredObject obj, File file, UploadInstructions uploadInstructions, long size, ProgressInformation progInfo, SwiftCallback callback) 
    {
    	if (size < uploadInstructions.getSegmentationSize())
    		throw new AssertionError (String.format("The file size (%d) must be greater than the segmentation size (%d)", size, uploadInstructions.getSegmentationSize())) ;

    	StringBuilder pathBuilder = new StringBuilder () ;
    	pathBuilder.append(abstractContainer.getName()) ;
    	pathBuilder.append(SwiftUtils.separator) ;
    	pathBuilder.append(obj.getName()) ;
        String path = pathBuilder.toString()  ;
        Set<StoredObject> segmentsSet = new TreeSet<> () ;
        try 
        {
            logger.info("Setting up a segmentation plan for " + path);
            
            Map<Long, String> md5PlanMap = null ;
            String currMsg = progInfo.getCurrentMessage() ;
            if (checkExistingSegments /*&& !obj.exists()*/)
            	md5PlanMap = getMd5PlanMap (uploadInstructions, obj, file, progInfo) ;
            SegmentationPlan plan = uploadInstructions.getSegmentationPlan();
            long numSegments = getNumberOfSegments (size, uploadInstructions) ;
            InputStream segmentStream = FileUtils.getInputStreamWithProgressFilter(progInfo, uploadInstructions.getSegmentationSize(), plan.getNextSegment()) ;
            while (segmentStream != null) 
            {
            	Long planSeg = plan.getSegmentNumber() ;
            	logger.info("Uploading segment " + planSeg);
            	progInfo.setCurrentMessage(String.format("%s (segment %d / %d)", currMsg, planSeg, numSegments));
                StoredObject segment = getObjectSegment(abstractContainer, obj, planSeg); 
                segmentsSet.add(segment) ;
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
            cleanUpExtraSegments (obj, segmentsSet) ;
        } 
        catch (IOException err) 
        {
        	logger.error("Failed to set up a segmentation plan for " + path + ": " + err.getMessage());
        	callback.onError(new CommandException("Unable to upload segments", err));
        }
    }
    

    @SuppressWarnings("unused")
	private void uploadSegmentedObjects(AbstractContainer abstractContainer, AbstractStoredObject obj, UploadInstructions uploadInstructions, long size, ProgressInformation progInfo, SwiftCallback callback) 
    {
    	uploadSegmentedObjects (abstractContainer, obj, null, uploadInstructions, size, progInfo, callback) ;
    }
    
    
    private void cleanUpExtraSegments (AbstractStoredObject obj, Set<StoredObject> toKeep)
    {
    	List<StoredObject> list =  getSegmentsList (obj) ;
    	for (StoredObject so : list)
    	{
    		if (toKeep != null && toKeep.contains(so))
    			continue ;
    		logger.info("Delete unused segment ({})", so.getBareName()) ;
    		so.delete(); 
    	}
    }
    
    
    @SuppressWarnings("unused")
	private void cleanUpExtraSegments (AbstractStoredObject obj, long numSegments)
    {
    	List<StoredObject> list =  getSegmentsList (obj, numSegments, -1) ;
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
	
	
	private Map<Long, String> getMd5PlanMap (UploadInstructions uploadInstructions, AbstractStoredObject obj, File file, ProgressInformation progInfo) throws IOException
    {
		if (obj == null)
			return java.util.Collections.emptyMap() ;		
		List<StoredObject> listSeg = getSegmentsList (obj) ;
		final int numberOfExistingSegments = listSeg.size() ;

    	Map<Long, String> ret = new HashMap<Long, String> () ;
    	
    	SegmentationPlan plan = (file == null) ? (uploadInstructions.getSegmentationPlan()) : (new FastSegmentationPlanFile (file, uploadInstructions.getSegmentationSize())) ;
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
	
	
	@SuppressWarnings("unused")
	private Map<Long, String> getMd5PlanMap (UploadInstructions uploadInstructions, AbstractStoredObject obj, ProgressInformation progInfo) throws IOException
    {
		return getMd5PlanMap (uploadInstructions, obj, null, progInfo) ;
    }
	
	
    // Code taken from Joss, package org.javaswift.joss.client.core, class AbstractContainer.java 
    // and adapted here.
    private StoredObject getObjectSegment(Container segmentsContainer, AbstractStoredObject obj, Long part) 
    {
    	if (part <= 0)
    		throw new AssertionError ("Segments are 1-indexed.") ;
    	
    	StringBuilder segmentNameBuilder = new StringBuilder () ;
    	segmentNameBuilder.append (obj.getName()) ;
    	segmentNameBuilder.append (SwiftUtils.separator) ;
    	segmentNameBuilder.append (String.format("%08d", part.intValue())) ;
        return segmentsContainer.getObject(segmentNameBuilder.toString());
    }
}
