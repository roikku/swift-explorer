/*
 *  Copyright 2014 Loic Merckel
 *  Copyright 2012-2013 E.Hooijmeijer
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

/*
*
* The original version of this file (i.e., the one that is copyrighted 2012-2013 E.Hooijmeijer) 
* can  be found here:
*
*  https://github.com/javaswift/cloudie
*  package (src/main/java): org.javaswift.cloudie.ops
*  
* Note: the class has been renamed from CloudieOperationsImpl
* to SwiftOperationsImpl
*
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


import org.swiftexplorer.swift.SwiftAccess;
import org.swiftexplorer.swift.client.factory.AccountFactory;
import org.swiftexplorer.swift.util.SwiftUtils;
import org.swiftexplorer.util.FileUtils;
import org.swiftexplorer.util.FileUtils.InputStreamProgressFilter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import org.javaswift.joss.client.core.AbstractContainer;
import org.javaswift.joss.client.core.AbstractStoredObject;
import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.headers.object.ObjectManifest;
import org.javaswift.joss.instructions.SegmentationPlan;
import org.javaswift.joss.instructions.UploadInstructions;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.Directory;
import org.javaswift.joss.model.DirectoryOrObject;
import org.javaswift.joss.model.PaginationMap;
import org.javaswift.joss.model.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SwiftOperationsImpl implements SwiftOperations {
	
    private static final int MAX_PAGE_SIZE = 9999;
    
    final Logger logger = LoggerFactory.getLogger(SwiftOperationsImpl.class);

    private volatile Account account = null;
    
    private volatile boolean useCustomSegmentation = false ;
    private volatile long segmentationSize = 104857600 ; // 100MB
    
    public SwiftOperationsImpl() {
    	super () ;
    }

    
    public Account getAccount ()
    {
    	return account ;
    }
    
    
    private void CheckAccount ()
    {
    	if (account == null)
    		throw new AssertionError ("Must login first") ;
    }
    
    
    /**
     * {@inheritDoc}.
     */
	@Override
	public synchronized void login(AccountConfig accConf, SwiftAccess swiftAccess, SwiftCallback callback) {

		account = new AccountFactory(accConf).setSwiftAccess(swiftAccess).setAuthUrl("").createAccount();
        callback.onLoginSuccess();
        callback.onNumberOfCalls(account.getNumberOfCalls());
	}
	
	
    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void login(AccountConfig accConf, String url, String tenant, String user, String pass, SwiftCallback callback) {

    	account = new AccountFactory(accConf).setUsername(user).setPassword(pass).setTenantName(tenant).setAuthUrl(url).createAccount();
    	
        callback.onLoginSuccess();
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }
    
    
    /**
     * {@inheritDoc}.
     */
	@Override
	public synchronized void login(AccountConfig accConf, long segmentationSize, SwiftAccess swiftAccess, SwiftCallback callback) {

    	this.login(accConf, swiftAccess, callback);
    	
    	this.segmentationSize = segmentationSize ;
    	useCustomSegmentation = true ;
	}
	
	
    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void login(AccountConfig accConf, long segmentationSize, String url, String tenant, String user, String pass, SwiftCallback callback) {

    	this.login(accConf, url, tenant, user, pass, callback);
        
    	this.segmentationSize = segmentationSize ;
    	useCustomSegmentation = true ;
    }


    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void logout(SwiftCallback callback) {
        account = null;
        callback.onLogoutSuccess();
        callback.onNumberOfCalls(0);
    }
    
    
	private static class ProgressInformation implements InputStreamProgressFilter.StreamProgressCallback
	{
		private double totalProgress = 0 ;
		private double currentProgress = 0 ;
		private String totalMessage = null ;
		private String currentMessage = null ;
		private final boolean isSingleTask ;
		
		private final SwiftCallback callback ;
		
		public ProgressInformation (SwiftCallback callback, boolean isSingleTask)
		{
			super () ;
			this.callback = callback ;
			this.isSingleTask = isSingleTask ;
		}
		
		public synchronized void report ()
		{
			if (callback == null)
				return ;
			callback.onProgress(totalProgress, totalMessage, (isSingleTask)?(totalProgress):(currentProgress), (isSingleTask)?(totalMessage):(currentMessage));
		}

		@Override
		public synchronized void onStreamProgress(double progress) {
			setCurrentProgress(progress) ;
			report () ;
		}
		
		public synchronized void setCurrentProgress (double p)
		{
			currentProgress = p ;
		}
		
		public synchronized void setTotalProgress (double p)
		{
			totalProgress = p ;
		}
		
		public synchronized void setTotalMessage (String msg)
		{
			totalMessage = msg ;
		}
		
		public synchronized void setCurrentMessage (String msg)
		{
			currentMessage = msg ;
		}
		
		public synchronized String getCurrentMessage ()
		{
			return currentMessage ;
		}
	}
    

    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void createContainer(ContainerSpecification spec, SwiftCallback callback) {
    	
    	CheckAccount () ;
    	
        if (spec != null) {
        	
        	if (spec.getName().endsWith(SwiftUtils.segmentsContainerPostfix))
        		throw new AssertionError ("A container name cannot end with \"" + SwiftUtils.segmentsContainerPostfix + "\"") ;
        	
            Container c = account.getContainer(spec.getName());
            if (!c.exists()) {
                c.create();
                if (spec.isPrivateContainer()) {
                    c.makePrivate();
                } else {
                    c.makePublic();
                }
            }
            callback.onUpdateContainers(eagerFetchContainers(account));
            callback.onNumberOfCalls(account.getNumberOfCalls());
        }
    }

    
    private Collection<Container> eagerFetchContainers(Account parent) {   	
    	
    	parent.reload();
    	
        List<Container> results = new ArrayList<Container>(parent.getCount());
        PaginationMap map = parent.getPaginationMap(MAX_PAGE_SIZE);
        for (int page = 0; page < map.getNumberOfPages(); page++) {
            results.addAll(parent.list(map, page));
        }
        return results;
    }
    

    private Collection<StoredObject> eagerFetchStoredObjects(Container parent) {
    	 
        Set<StoredObject> results = new TreeSet<StoredObject>();
        PaginationMap map = parent.getPaginationMap(MAX_PAGE_SIZE);
        for (int page = 0; page < map.getNumberOfPages(); page++) 
        {
        	for (StoredObject obj : parent.list(map, page))
        	{
        		if (!results.add (obj)) 
        			logger.debug("Object listed twice: " + obj.getName());
        	}
        }
        return results;
    }
    
    
    private Collection<StoredObject> eagerFetchStoredObjects(Container parent, String prefix) {
        Set<StoredObject> results = new TreeSet<StoredObject>();
        PaginationMap map = parent.getPaginationMap(MAX_PAGE_SIZE);
        for (int page = 0; page < map.getNumberOfPages(); page++) 
        {
        	for (StoredObject obj : parent.list(prefix, map.getMarker(page), map.getPageSize()))
        	{
        		if (!results.add (obj)) 
        			logger.debug(String.format("Object listed twice: %s (prefix used: %s)", obj.getName(), prefix));
        	}
        }
        return results;
    }
    

    /**
     * {@inheritDoc}.
     * @throws IOException 
     */
    @Override
    public synchronized void createStoredObjects(Container container, File[] selectedFiles, SwiftCallback callback) throws IOException {
    	
    	CheckAccount () ;
    	
		int totalFiles = selectedFiles.length ;
		int currentUplodedFilesCount = 0 ;
		ProgressInformation progInfo = new ProgressInformation (callback, false) ;
		
        for (File selected : selectedFiles) {
        	
        	++currentUplodedFilesCount ;
        	
            if (selected.isFile() && selected.exists()) {
            	                
				totalProgress (currentUplodedFilesCount, totalFiles, Paths.get(selected.getPath()), progInfo, true) ;

                StoredObject obj = container.getObject(selected.getName());
                uploadObject(obj, selected, progInfo, callback) ;
            }
        }
        reloadContainer(container, callback);
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }

    
    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void deleteContainer(Container container, SwiftCallback callback) {
    	
    	CheckAccount () ;
    	
        container.delete();
        callback.onUpdateContainers(eagerFetchContainers(account));
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }

    
    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void deleteStoredObjects(Container container, List<StoredObject> storedObjects, SwiftCallback callback) {
    	
    	CheckAccount () ;
    	
        for (StoredObject storedObject : storedObjects) 
        {
			// segmented objects should be deleted as well
			if (isSegmented (storedObject))
			{				
				List<StoredObject> segments = getSegmentsList (storedObject) ;
				// we delete each segment, starting by the last one, so that if something
				// unexpected happen at the middle of the iteration, the getSegmentsList method
				// will still be able to get the list of the remaining segments. 
	            ListIterator<StoredObject> segmentsIter = segments.listIterator(segments.size()) ;
				while (segmentsIter.hasPrevious()) 
				{
					StoredObject segment = segmentsIter.previous() ;
					if (segment == null || !segment.exists())
						continue ;
					logger.info("Deleting segment " + segment.getPath());
					segment.delete();
				}
			}
			if (storedObject.exists())
			{
				storedObject.delete();
				logger.info("Deleted object: " + storedObject.getName());
			}
			else
				logger.debug("Attempt at deleting a non-existing object: " + storedObject.getName());
            callback.onStoredObjectDeleted(container, storedObject);
        }
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }
    

    /**
     * {@inheritDoc}.
     * @throws IOException 
     */
    @Override
    public synchronized void downloadStoredObject(Container container, StoredObject storedObject, File target, SwiftCallback callback) throws IOException {
        
    	CheckAccount () ;
    	    	
    	// First, we check whether we want to download a full directory (i.e., "recursively")
    	if (SwiftUtils.isDirectory(storedObject) && target.isDirectory())
    	{
    		try
    		{
	    		String prefix = storedObject.getName() + SwiftUtils.separator ;
	    		Collection<StoredObject> listObj = eagerFetchStoredObjects(container, prefix) ;
	    		
	    		int currentUplodedFilesCount = 0 ;
	    		int totalFiles = listObj.size() ;
	    		ProgressInformation progInfo = new ProgressInformation (callback, false) ;
	    		
	            for (StoredObject so : listObj) 
	            {
	            	++currentUplodedFilesCount ;
	            	
	            	if (so == null)
	            		continue ;
	            	
	            	totalProgress (currentUplodedFilesCount, totalFiles, so, progInfo, true) ;
	            	
	            	StringBuilder pathBuilder = new StringBuilder () ;
	            	pathBuilder.append(target.getPath ()) ;
	            	if (!target.getPath ().endsWith(File.separator))
	            		pathBuilder.append(File.separator) ;
	            	String dirName = storedObject.getName() ;
	            	int index = dirName.lastIndexOf(SwiftUtils.separator) ;
	            	if (index >= 0)
	            		dirName = dirName.substring(index + 1) ;
	            	pathBuilder.append(dirName) ;
	            	pathBuilder.append(so.getName().replaceFirst(storedObject.getName(),"").trim()) ;
	            	String path = pathBuilder.toString() ;
	            	if (!SwiftUtils.separator.equals(File.separator))
	            		path = path.replace(SwiftUtils.separator, File.separator) ;
	            	
	            	File destFile = new File(path);
	            	if (SwiftUtils.isDirectory(so))
	            		destFile.mkdirs();
	            	else
	            	{
	            		File parent = destFile.getParentFile() ;
	            		if (parent != null)
	            			parent.mkdirs() ;
	            		downloadObject (so, destFile, progInfo, callback) ;
	            	}
	            }
	    		logger.info("Downloaded directory '{}' into '{}'", storedObject.getName(), target.getPath());
    		}
    	    catch (OutOfMemoryError ome)
    	    {
    	    	dealWithOutOfMemoryError (ome, "downloadStoredObject", callback) ;
    	    }
    	}
    	else if (SwiftUtils.isDirectory(storedObject))
    	{
    		// here target cannot be a directory
    		throw new AssertionError ("An object directory can only be downloaded in a directory.") ;
    	}
    	else
    	{
    		ProgressInformation progInfo = new ProgressInformation (callback, false) ;
    		totalProgress (1, 1, storedObject, progInfo, true) ;
    		downloadObject (storedObject, target, progInfo, callback) ;
    	}
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }
    
    
    private void downloadObject (StoredObject storedObject, File target, ProgressInformation progInfo, SwiftCallback callback) throws IOException
    {		
    	if (storedObject == null || target == null)
    		return ;
    	try
    	{    		    		
    		progInfo.setCurrentMessage(String.format("Downloading %s", storedObject.getName()));
    		InputStream in = FileUtils.getInputStreamWithProgressFilter(progInfo, storedObject.getContentLength(), storedObject.downloadObjectAsInputStream()) ;	    		

    		FileUtils.saveInputStreamInFile (in, target, true) ;
    	}
	    catch (OutOfMemoryError ome)
	    {
	    	dealWithOutOfMemoryError (ome, "downloadObject", callback) ;
	    }
    }
    
    
    private void uploadObject (StoredObject storedObject, File file, ProgressInformation progInfo, SwiftCallback callback) throws IOException
    {			
    	if (storedObject == null || file == null)
    		return ;
    	
    	InputStream in = null ;
    	try
    	{
    		progInfo.setCurrentMessage(String.format("Uploading %s", file.getPath()));
    		BasicFileAttributes attr = FileUtils.getFileAttr(Paths.get(file.getPath())) ;
	    	if (useCustomSegmentation)	
	    	{
	    		UploadInstructions ui = new UploadInstructions (file).setSegmentationSize(segmentationSize) ;
	    		if (ui.requiresSegmentation())
	    		{
		    		uploadObjectAsSegments (storedObject, ui, attr.size(), progInfo, callback) ;
		    		return ;
	    		}
	    	}	    	
	    	in = FileUtils.getInputStreamWithProgressFilter(progInfo, attr.size(), Paths.get(file.getPath())) ;
	    	storedObject.uploadObject(in) ;
    	}
	    catch (OutOfMemoryError ome)
	    {
	    	dealWithOutOfMemoryError (ome, "uploadObject", callback) ;
	    }
    	finally
    	{
    		if (in != null)
    			in.close();
    	}
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
    
    
    // Code taken from Joss, package org.javaswift.joss.client.core, class AbstractStoredObject.java 
    // and adapted here.
    private void uploadObjectAsSegments(StoredObject obj, UploadInstructions uploadInstructions, long size, ProgressInformation progInfo, SwiftCallback callback) 
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
    
    
    private long getNumberOfSegments (long totalSize, UploadInstructions uploadInstructions)
    {
    	return totalSize / uploadInstructions.getSegmentationSize() + (long)((totalSize % uploadInstructions.getSegmentationSize() == 0)?(0):(1)) ;
    }
    
    
	private boolean isSegmented (StoredObject obj)
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
    
    
	private List<StoredObject> getSegmentsList (StoredObject obj)
    {
    	List<StoredObject> ret = new ArrayList<StoredObject> () ;
    	Container segCont = getSegmentsContainer (obj, false) ;
    	if (segCont == null || !segCont.exists())
    		return ret ;
    	int segCount = 1 ;
    	StoredObject segObj = getObjectSegment ((AbstractContainer)segCont, (AbstractStoredObject)obj, Long.valueOf(segCount)) ;
    	while (segObj != null && segObj.exists())
    	{
    		ret.add (segObj) ;
    		++segCount ;
    		segObj = getObjectSegment ((AbstractContainer)segCont, (AbstractStoredObject)obj, Long.valueOf(segCount)) ;
    	}
    	return ret ;
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
            logger.info("JOSS / Setting up a segmentation plan for " + path);
            
            //Map<Long, String> md5PlanMap = getMd5PlanMap (uploadInstructions) ;
            SegmentationPlan plan = uploadInstructions.getSegmentationPlan();
            long numSegments = getNumberOfSegments (size, uploadInstructions) ;
            String currMsg = progInfo.getCurrentMessage() ;
            InputStream segmentStream = FileUtils.getInputStreamWithProgressFilter(progInfo, uploadInstructions.getSegmentationSize(), plan.getNextSegment()) ;
            while (segmentStream != null) 
            {
            	Long planSeg = plan.getSegmentNumber() ;
            	logger.info("JOSS / Uploading segment " + planSeg);
            	progInfo.setCurrentMessage(String.format("%s (segment %d / %d)", currMsg, planSeg, numSegments));
                StoredObject segment = getObjectSegment(abstractContainer, obj, planSeg);                
                // check if this segment can be ignored
                /*boolean ignore = false ;
                if (segment.exists() && md5PlanMap.containsKey(planSeg))
                {
        			String etag = segment.getEtag() ;
        			String md5 = md5PlanMap.get(planSeg) ;
        			if (etag != null && etag.equals(md5))
        				ignore = true ; 
                }
              
                if (!ignore)*/
                segment.uploadObject(segmentStream);
                segmentStream.close();
                segmentStream = FileUtils.getInputStreamWithProgressFilter(progInfo, (planSeg + 1 != numSegments)?(uploadInstructions.getSegmentationSize()):(size % uploadInstructions.getSegmentationSize()), plan.getNextSegment());
            }
        } 
        catch (IOException err) 
        {
        	logger.error("JOSS / Failed to set up a segmentation plan for " + path + ": " + err.getMessage());
        	callback.onError(new CommandException("Unable to upload segments", err));
        }
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

    
    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void emptyContainer(Container container, SwiftCallback callback) {
    	
    	CheckAccount () ;
    	
        for (StoredObject so : eagerFetchStoredObjects(container)) {
        	if (so.exists())
        		so.delete();
            callback.onNumberOfCalls(account.getNumberOfCalls());
        }
        reloadContainer(container, callback);
        callback.onNumberOfCalls(account.getNumberOfCalls());
        logger.info(String.format("Container %s has been emptied", container.getName()));
    }

    
    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void purgeContainer(Container container, SwiftCallback callback) {
    	
    	CheckAccount () ;
    	
        for (StoredObject so : eagerFetchStoredObjects(container)) {
        	if (so.exists())
        		so.delete();
            callback.onNumberOfCalls(account.getNumberOfCalls());
        }
        container.delete();
        callback.onUpdateContainers(eagerFetchContainers(account));
        callback.onNumberOfCalls(account.getNumberOfCalls());
        logger.info(String.format("Container %s has been removed", container.getName()));
    }

    
    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void refreshContainers(SwiftCallback callback) {
    	
    	CheckAccount () ;    	
        callback.onUpdateContainers(eagerFetchContainers(account));
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }
    

    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void refreshStoredObjects(Container container, SwiftCallback callback) {
    	
    	CheckAccount () ;
    	
        reloadContainer(container, callback);
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }

    
    private void reloadContainer(Container container, SwiftCallback callback) {
    	reloadContainer(container, callback, true);
    }
    
    
    private void reloadContainer(Container container, SwiftCallback callback, boolean showProgress) {
    	
    	if (container == null)
    		throw new AssertionError ("container cannot be null") ;
    	
    	container.reload();
    	
    	int total = container.getCount() ;
    	int current = 0 ;
    	ProgressInformation progInfo = new ProgressInformation (callback, true) ;
    	progInfo.setTotalMessage("Refreshing the list of documents") ;
    	
        int page = 0;
        List<StoredObject> list = (List<StoredObject>) container.list("", null, MAX_PAGE_SIZE);
        callback.onNewStoredObjects();
        while (!list.isEmpty()) {
        	
        	if (showProgress)
        	{
        		current += list.size() ;
        		
        		progInfo.setTotalProgress(current / (double)total);
        		progInfo.report();
        	}
        	
            callback.onAppendStoredObjects(container, page++, list);
            list = (List<StoredObject>) container.list("", list.get(list.size() - 1).getName(), MAX_PAGE_SIZE);
        }
    }
    
    
    /**
     * {@inheritDoc}.
     */
	@Override
	public void refreshDirectoriesOrStoredObjects(Container container, Directory parent, SwiftCallback callback) {
		
		CheckAccount () ;
		
		loadContainerDirectory (container, parent, callback) ;
		callback.onNumberOfCalls(account.getNumberOfCalls());
	}
	
	
    private void loadContainerDirectory(Container container, Directory parent, SwiftCallback callback) 
    {
    	if (container == null)
    		throw new AssertionError ("container cannot be null") ;
    	
    	final String prefix ;
    	Character delimiter = SwiftUtils.separator.charAt(0) ;
    	List<DirectoryOrObject> directoriesOrObjects = new ArrayList<DirectoryOrObject> () ;
    	if (parent == null)
    	{
    		callback.onNewStoredObjects();
    		prefix = "" ;
    	}
    	else
    		prefix = parent.getName() + delimiter ;
    	
    	int page = 0;
    	directoriesOrObjects.addAll(container.listDirectory(prefix, delimiter, null, MAX_PAGE_SIZE)) ;
        while (!directoriesOrObjects.isEmpty()) 
        {	        	
        	List<StoredObject> listStoredObjects = new ArrayList<StoredObject> (directoriesOrObjects.size()) ;
        	for (DirectoryOrObject dirOrobj : directoriesOrObjects)
        	{
        		if (dirOrobj == null)
        			continue ;
        		if (dirOrobj.isObject())
        			listStoredObjects.add(dirOrobj.getAsObject()) ;
        	}
            callback.onAppendStoredObjects(container, page++, listStoredObjects);
            
            String marker = directoriesOrObjects.get(directoriesOrObjects.size() - 1).getName() ;
            directoriesOrObjects.clear () ;
            directoriesOrObjects.addAll(container.listDirectory(prefix, delimiter, marker, MAX_PAGE_SIZE)) ;
        }
    }
    

    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void getMetadata(Container c, SwiftCallback callback) {
    	
    	CheckAccount () ;
    	
        c.getMetadata();
        callback.onContainerUpdate(c);
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }
    

    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void getMetadata(StoredObject obj, SwiftCallback callback) {
    	
    	CheckAccount () ;
    	
        obj.getMetadata();
        callback.onStoredObjectUpdate(obj);
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }

    
	private boolean shouldBeIgnored (StoredObject obj, Path path, boolean overwrite) throws IOException
	{
		if (obj == null)
			return true ;
		if (path == null)
			return true ;
		if (obj.exists())
		{
			if (Files.isDirectory(path))
				return true ; // the folder already exists
			
			//if (isSegmented (obj))
			//	return false ;
			
			String etag = obj.getEtag() ;
			String md5 = FileUtils.getMD5(path.toFile()) ;
			// the file is already uploaded, unless this is a collision... But we would then be quite unlucky
			// TODO: check other information in order to increase the confidence that the file is the same. 
			// ...
			if (etag != null && etag.equals(md5))
				return true ; 
			else
			{
				logger.info("A different version of the file '{}' already exists in the cloud. it has been {}", path.toString(), (overwrite)?("overwritten"):("ignored"));
				if (overwrite)
					return true ;
			}
		}
		return false ;
	}
	
	
	private void totalProgress (int currentUplodedFilesCount, int totalFiles, Path currentFile, ProgressInformation progInfo, boolean report) throws IOException
	{
		if (progInfo == null || currentFile == null)
			return;
		// Progress notification
		double progress = currentUplodedFilesCount / (double)totalFiles ;
		BasicFileAttributes attr = FileUtils.getFileAttr(currentFile) ;
		progInfo.setTotalProgress(progress);
		progInfo.setTotalMessage(String.format("%d / %d files processed (current file size: %s)", currentUplodedFilesCount, totalFiles, FileUtils.humanReadableByteCount(attr.size(),  true)));
		
		if (report)
			progInfo.report();
	}
	
	
	private void totalProgress (int currentUplodedFilesCount, int totalFiles, StoredObject so, ProgressInformation progInfo, boolean report) 
	{
		if (progInfo == null)
			return;
		// Progress notification
		double progress = currentUplodedFilesCount / (double)totalFiles ;
		progInfo.setTotalProgress(progress);
		progInfo.setTotalMessage(String.format("%d / %d objects processed (current object size: %s)", currentUplodedFilesCount, totalFiles, FileUtils.humanReadableByteCount(so.getContentLength(),  true)));
		
		if (report)
			progInfo.report();
	}
	
	
    /**
     * {@inheritDoc}.
     * @throws IOException 
     */
	@Override
	public synchronized void uploadDirectory(Container container, StoredObject parentObject, File directory, boolean overwriteAll, SwiftCallback callback) throws IOException {

		CheckAccount () ;
    	
		final String separator = SwiftUtils.separator ;
		
		Path source = Paths.get(directory.getPath()) ;
		Queue<Path> filesQueue = FileUtils.getAllFilesPath(source, true) ;
		if (filesQueue == null)
		{
			logger.info("No file found in the directory '{}'", directory.getPath ());
			return ;
		}
		
		String parentDir = SwiftUtils.getParentDirectory(parentObject) ;
		
		int totalFiles = filesQueue.size() ;
		int currentUplodedFilesCount = 0 ;
		ProgressInformation progInfo = new ProgressInformation (callback, false) ;
		
		for (Path path : filesQueue)
		{			
			++currentUplodedFilesCount ;

			if (Files.notExists(path))
				continue ;
			if (Files.isSymbolicLink(path))
				continue ;

			// Progress notification
			totalProgress (currentUplodedFilesCount, totalFiles, path, progInfo, true) ;
			
			StringBuilder objectPathBuilder = new StringBuilder () ;
			objectPathBuilder.append (parentDir) ;
			objectPathBuilder.append (source.getFileName().toString()) ;
			objectPathBuilder.append(separator) ;
			objectPathBuilder.append(source.relativize(path).toString()) ;

			String objectPath = objectPathBuilder.toString() ;
			if (!separator.equals(File.separator))
				objectPath = objectPath.replace(File.separator, separator) ;
		
			// relevant when creating folder
			if (objectPath.length() > 1 && objectPath.endsWith(separator))
				objectPath = objectPath.substring(0, objectPath.length() - 1) ;
			
			StoredObject obj = container.getObject(objectPath.toString());
			
			if (shouldBeIgnored (obj, path, overwriteAll))
				continue ;
			
			if (Files.isDirectory(path))
			{				
				// here we create a directory
				byte[] emptyfile = {} ; 
				UploadInstructions inst = new UploadInstructions (emptyfile) ;
				inst.setContentType(SwiftUtils.directoryContentType) ;
				obj.uploadObject(inst) ;
			}
			else
			{
				uploadObject (obj, path.toFile(), progInfo, callback) ;
			}
		}
		reloadContainer(container, callback);
		callback.onNumberOfCalls(account.getNumberOfCalls());

		logger.info(
				"Uploaded directory '{}', in directory '{}', in container '{}'. Number of files: {}",
				directory.getPath(), parentDir, container.getPath(),
				String.valueOf(filesQueue.size()));
	}

	
    /**
     * {@inheritDoc}.
     * @throws IOException 
     */
	@Override
	public synchronized void uploadFiles(Container container, StoredObject parentObject, File[] files, boolean overwriteAll, SwiftCallback callback) throws IOException 
	{	
		CheckAccount () ;
    	
		if (files == null || files.length == 0)
			return ;
		
		String parentDir = SwiftUtils.getParentDirectory(parentObject) ;
		
		int totalFiles = files.length ;
		int currentUplodedFilesCount = 0 ;
		ProgressInformation progInfo = new ProgressInformation (callback, false) ;
		
		for (File file : files)
		{			
			++currentUplodedFilesCount ;
			
			if (file == null || !file.exists() || !file.isFile()) 
				continue ;

			// Progress notification
			totalProgress (currentUplodedFilesCount, totalFiles, Paths.get(file.toURI()), progInfo, true) ;
			
			StringBuilder objectPathBuilder = new StringBuilder () ;
			objectPathBuilder.append (parentDir) ;
			objectPathBuilder.append (file.getName()) ;
			String objectPath = objectPathBuilder.toString() ;		
			
			StoredObject obj = container.getObject(objectPath.toString());
			
			if (shouldBeIgnored (obj, Paths.get(file.getPath()), overwriteAll))
				continue ;
			
			uploadObject (obj, file, progInfo, callback) ;
		}
		reloadContainer(container, callback);
		callback.onNumberOfCalls(account.getNumberOfCalls());
	}
	

    /**
     * {@inheritDoc}.
     * @throws IOException 
     */
	@Override
	public synchronized void createDirectory(Container container, StoredObject parentObject, String directoryName, SwiftCallback callback)
	{
		CheckAccount () ;
    	
		if (directoryName == null || directoryName.isEmpty())
			return ;
		
		String parentDir = SwiftUtils.getParentDirectory(parentObject) ;
		
		StringBuilder objectPathBuilder = new StringBuilder () ;
		objectPathBuilder.append (parentDir) ;
		objectPathBuilder.append (directoryName) ;
		
		StoredObject obj = container.getObject(objectPathBuilder.toString());
		if (obj.exists())
		{
			if (!SwiftUtils.directoryContentType.equalsIgnoreCase(obj.getContentType()))
				callback.onError(new CommandException ("A non-directory file with the same name already exists")) ;
		}
		else
		{
			byte[] emptyfile = {} ; 
			UploadInstructions inst = new UploadInstructions (emptyfile) ;
			inst.setContentType(SwiftUtils.directoryContentType) ;
			obj.uploadObject(inst) ;
		}
		reloadContainer(container, callback);
		callback.onNumberOfCalls(account.getNumberOfCalls());
	}


    /**
     * {@inheritDoc}.
     */
	@Override
	public synchronized void deleteDirectory(Container container, StoredObject storedObject, SwiftCallback callback) {
		
		CheckAccount () ;
    	
    	if (!SwiftUtils.isDirectory(storedObject))
    		throw new AssertionError ("The object to delete must be a directory.") ;
    	
    	try
    	{
			String prefix = storedObject.getName() + SwiftUtils.separator ;
			Collection<StoredObject> listObj = eagerFetchStoredObjects(container, prefix) ;
			int currentUplodedFilesCount = 0 ;
			int totalFiles = listObj.size() ;
			ProgressInformation progInfo = new ProgressInformation (callback, false) ;
			
	        for (StoredObject so : listObj) 
	        {
	        	++currentUplodedFilesCount ;
	        	
	        	if (so == null)
	        		continue ;
	        	
	        	// defensive check, it should not be necessary, provided that
	        	// the prefix was given to eagerFetchStoredObjects 
	        	if (!so.getName().startsWith(storedObject.getName()))
	        		continue ;
	        	
	        	progInfo.setCurrentMessage(String.format ("Deleting %s", so.getName())) ;
	        	progInfo.setCurrentProgress(1) ;
	        	totalProgress (currentUplodedFilesCount, totalFiles, so, progInfo, true) ;
	
	        	deleteStoredObjects (container, Arrays.asList(so), callback) ;
	        }
	        logger.info("Deleted directory '{}'", storedObject.getName());
	        deleteStoredObjects (container, Arrays.asList(storedObject), callback) ;
	        reloadContainer(container, callback);
	        callback.onNumberOfCalls(account.getNumberOfCalls());
		}
	    catch (OutOfMemoryError ome)
	    {
	    	dealWithOutOfMemoryError (ome, "deleteDirectory", callback) ;
	    }
	}
	
	
	private void dealWithOutOfMemoryError (OutOfMemoryError ome, String functionName, SwiftCallback callback)
	{
    	System.gc() ; // pointless at this stage, but anyway...
    	logger.error(String.format("OutOfMemory error occurred while calling SwiftOperationsImpl.%s", functionName), ome);
    	callback.onError(new CommandException ("The JVM ran out of memory")) ;
	}
}
