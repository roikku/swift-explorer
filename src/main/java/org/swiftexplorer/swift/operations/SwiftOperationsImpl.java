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


package org.swiftexplorer.swift.operations;


import org.swiftexplorer.config.proxy.HasProxySettings;
import org.swiftexplorer.config.swift.HasSwiftSettings;
import org.swiftexplorer.swift.client.impl.HttpClientFactoryImpl;
import org.swiftexplorer.swift.operations.DifferencesFinder.LocalItem;
import org.swiftexplorer.swift.operations.DifferencesFinder.RemoteItem;
import org.swiftexplorer.swift.util.SwiftUtils;
import org.swiftexplorer.util.FileUtils;
import org.swiftexplorer.util.Pair;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.client.factory.AccountFactory;
import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.exception.CommandExceptionError;
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
    
    private LargeObjectManager largeObjectManager = null ;
    
    private volatile boolean useCustomSegmentation = false ;
    private volatile long segmentationSize = 104857600 ; // 100MB
    
    private final int numberOfCommandErrorRetry = 5 ;

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
    	assert (largeObjectManager != null) ;
    }
    
    
    /**
     * {@inheritDoc}.
     */
	@Override
	public synchronized void login(AccountConfig accConf, SwiftCallback callback) {

		this.login(accConf, null, null, callback);
	}
	
	
    /**
     * {@inheritDoc}.
     */
	@Override
	public synchronized void login(AccountConfig accConf, HasProxySettings proxySettings, SwiftCallback callback) {

		this.login(accConf, null, proxySettings, callback);
	}
    
    
    /**
     * {@inheritDoc}.
     */
	@Override
	public synchronized void login(AccountConfig accConf, HasSwiftSettings swiftSettings, SwiftCallback callback) {

		this.login(accConf, swiftSettings, null, callback);
	}
	
	
    /**
     * {@inheritDoc}.
     */
	@Override
	public synchronized void login(AccountConfig accConf, HasSwiftSettings swiftSettings, HasProxySettings proxySettings, SwiftCallback callback) {

		String preferredRegion = null ;
		if (swiftSettings != null) {
			
	    	this.segmentationSize = swiftSettings.getSegmentationSize() ;
	    	useCustomSegmentation = true ;
	    	preferredRegion = swiftSettings.getPreferredRegion() ;
	    	preferredRegion = ((preferredRegion == null || preferredRegion.trim().isEmpty()) ? (null) : (preferredRegion.trim())) ;
		}
		
		if (swiftSettings == null && proxySettings == null) {
			
			account = new AccountFactory(accConf).setAuthUrl("").createAccount();
		} else if (swiftSettings == null) {
			
			account = new AccountFactory(accConf).setAuthUrl("").setHttpClient(new HttpClientFactoryImpl ().getHttpClient(accConf, proxySettings)).createAccount();
		} else if (proxySettings == null) {
			
			account = new AccountFactory(accConf).setAuthUrl("").setPreferredRegion(preferredRegion).createAccount();
		} else {
				
			account = new AccountFactory(accConf).setAuthUrl("").setPreferredRegion(preferredRegion).setHttpClient(new HttpClientFactoryImpl ().getHttpClient(accConf, proxySettings)).createAccount();
		}
		largeObjectManager = new LargeObjectManagerImpl (account) ;
        callback.onLoginSuccess();
        callback.onNumberOfCalls(account.getNumberOfCalls());
	}
	
	
    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void login(AccountConfig accConf, String url, String tenant, String user, String pass, SwiftCallback callback) {

    	this.login(accConf, null, url, tenant, user, pass, callback) ;
    }
    
	
    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void login(AccountConfig accConf, HasSwiftSettings swiftSettings, String url, String tenant, String user, String pass, SwiftCallback callback) {

		String preferredRegion = null ;
		if (swiftSettings != null) {
			
	    	this.segmentationSize = swiftSettings.getSegmentationSize() ;
	    	useCustomSegmentation = true ;
	    	preferredRegion = swiftSettings.getPreferredRegion() ;
	    	preferredRegion = ((preferredRegion == null || preferredRegion.trim().isEmpty()) ? (null) : (preferredRegion.trim())) ;
		}
		
    	account = new AccountFactory(accConf).setPreferredRegion(preferredRegion).setUsername(user).setPassword(pass).setTenantName(tenant).setAuthUrl(url).createAccount();
    	largeObjectManager = new LargeObjectManagerImpl (account) ;
        
        callback.onLoginSuccess();
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }


    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void logout(SwiftCallback callback) {
        account = null;
        largeObjectManager = null ;
        
        callback.onLogoutSuccess();
        callback.onNumberOfCalls(0);
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
    public synchronized void createStoredObjects(Container container, File[] selectedFiles, StopRequester stopRequester, SwiftCallback callback) throws IOException {
    	
    	CheckAccount () ;
    	
		int totalFiles = selectedFiles.length ;
		int currentUplodedFilesCount = 0 ;
		ProgressInformation progInfo = new ProgressInformation (callback, false) ;
		
		List<StoredObject> newObjects = new ArrayList<> () ;
		
        for (File selected : selectedFiles) {
        	
        	if (!keepGoing (stopRequester, callback))
        		break ;
        	
        	++currentUplodedFilesCount ;
        	
            if (selected.isFile() && selected.exists()) {
            	                
				totalProgress (currentUplodedFilesCount, totalFiles, Paths.get(selected.getPath()), progInfo, true) ;

                StoredObject obj = container.getObject(selected.getName());
                uploadObject(obj, selected, progInfo, callback) ;
                
                newObjects.add(obj) ;
            }
        }
    	//reloadContainer(container, callback);
    	addedObjectToContainer(container, newObjects, callback);
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

    
    private void deleteStoredObjects(Container container, List<StoredObject> storedObjects, StopRequester stopRequester, boolean notify, SwiftCallback callback) {
	
        for (StoredObject storedObject : storedObjects) 
        {
        	if (!keepGoing (stopRequester, callback))
        		break ;
        	
			// segmented objects should be deleted as well
			if (largeObjectManager.isSegmented (storedObject))
			{				
				List<StoredObject> segments = largeObjectManager.getSegmentsList (storedObject) ;
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
			{
				logger.debug("Attempt at deleting a non-existing object: " + storedObject.getName());
				continue ;
			}
			if (notify)
				callback.onStoredObjectDeleted(container, storedObject);
        }
        callback.onNumberOfCalls(account.getNumberOfCalls());
	}
    
    
    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void deleteStoredObjects(Container container, List<StoredObject> storedObjects, StopRequester stopRequester, SwiftCallback callback) {
    	
    	CheckAccount () ;
    	deleteStoredObjects (container, storedObjects, stopRequester, true, callback) ;
    }
    
    
    private File getDestinationFile (StoredObject srcDirRootStoredObject, File destDirRoot, StoredObject srcStoredObject)
    {
    	StringBuilder pathBuilder = new StringBuilder () ;
    	pathBuilder.append(destDirRoot.getPath ()) ;
    	if (!destDirRoot.getPath ().endsWith(File.separator))
    		pathBuilder.append(File.separator) ;
    	String dirName = srcDirRootStoredObject.getName() ;
    	int index = dirName.lastIndexOf(SwiftUtils.separator) ;
    	if (index >= 0)
    		dirName = dirName.substring(index + 1) ;
    	pathBuilder.append(dirName) ;
    	pathBuilder.append(srcStoredObject.getName().replaceFirst(srcDirRootStoredObject.getName(),"").trim()) ;
    	String path = pathBuilder.toString() ;
    	if (!SwiftUtils.separator.equals(File.separator))
    		path = path.replace(SwiftUtils.separator, File.separator) ;
    	
    	return new File(path);
    }

    
    /**
     * {@inheritDoc}.
     * @throws IOException 
     */
    @Override
    public synchronized void downloadStoredObject(Container container, StoredObject storedObject, File target, StopRequester stopRequester, SwiftCallback callback) throws IOException {
        
    	CheckAccount () ;
    	    	
    	// First, we check whether we want to download a full directory (i.e., "recursively")
    	if (SwiftUtils.isDirectory(storedObject) && target.isDirectory())
    	{
    		try
    		{
    			// Create the root destination directory
    			// If storedObject does not contain any files (if it is an empty directory), then 
    			// we still need to create an empty directory as a result of the download
            	File destDirRoot = getDestinationFile (storedObject, target, storedObject) ;
            	destDirRoot.mkdirs();
    			
	    		String prefix = storedObject.getName() + SwiftUtils.separator ;
	    		Collection<StoredObject> listObj = eagerFetchStoredObjects(container, prefix) ;
	    		
	    		int currentUplodedFilesCount = 0 ;
	    		int totalFiles = listObj.size() ;
	    		ProgressInformation progInfo = new ProgressInformation (callback, false) ;
	    		
	            for (StoredObject so : listObj) 
	            {
	            	if (!keepGoing (stopRequester, callback))
	            	{
		        		callback.onNumberOfCalls(account.getNumberOfCalls());
		        		return ;
		        	}
		        	
	            	++currentUplodedFilesCount ;
	            	
	            	if (so == null)
	            		continue ;
	            	
	            	totalProgress (currentUplodedFilesCount, totalFiles, so, progInfo, true) ;
	            	
	            	File destFile = getDestinationFile (storedObject, target, so) ;
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
        	if (!keepGoing (stopRequester, callback))
        		return ;
    		ProgressInformation progInfo = new ProgressInformation (callback, false) ;
    		totalProgress (1, 1, storedObject, progInfo, true) ;
    		downloadObject (storedObject, target, progInfo, callback) ;
    	}
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }
    
    
    /**
     * {@inheritDoc}.
     * @throws IOException 
     */
	@Override
	public synchronized void downloadStoredObject(Container container, Collection<Pair<? extends StoredObject, ? extends File> > pairObjectFiles, boolean overwriteAll, StopRequester stopRequester, SwiftCallback callback) throws IOException
	{
		CheckAccount () ;
		
		if (pairObjectFiles == null || pairObjectFiles.isEmpty())
			return ;
		
		int totalFiles = pairObjectFiles.size() ;
		int currentDownloadedFilesCount = 0 ;
		ProgressInformation progInfo = new ProgressInformation (callback, false) ;
		
		List<StoredObject> newObjects = new ArrayList<> () ;
		
		try
		{
			for (Pair<? extends StoredObject, ? extends File> pair : pairObjectFiles)
			{			
				if (!keepGoing (stopRequester, callback))
	        		return ;
	        	
				++currentDownloadedFilesCount ;
				
				if (pair == null)
					continue ;
				File file = pair.getSecond() ;
				if (pair.getSecond() == null) 
					continue ;
				StoredObject obj = pair.getFirst() ;
				if (obj == null)
					continue ;
				
				if (!overwriteAll && file.exists())
					continue ;
			
				// Progress notification
				totalProgress (currentDownloadedFilesCount, totalFiles, obj, progInfo, true) ;
				
				downloadObject (obj, file, progInfo, callback) ;
				
				newObjects.add(obj) ;
			}
		}
		finally
		{
        	//reloadContainer(container, callback);
        	addedObjectToContainer(container, newObjects, callback);
			callback.onNumberOfCalls(account.getNumberOfCalls());
		}
	}
    
	
	private boolean isRetryable (CommandException e)
	{
		return ((e.getHttpStatusCode() == 0 && e.getError() == null)
				|| e.getError() == CommandExceptionError.UNKNOWN
				|| e.getError() == CommandExceptionError.UNAUTHORIZED) ;
	}
	
	
	private void dealWithCommandException (CommandException e, AtomicInteger counter)
	{		
		if (isRetryable (e))
		{
			if (counter.getAndIncrement() >= numberOfCommandErrorRetry)
				throw e ;
			logger.info(String.format("Command Error: %s", e.toString()));
		}
		else
			throw e ;
	}
	
    
    private void downloadObject (StoredObject storedObject, File target, ProgressInformation progInfo, SwiftCallback callback) throws IOException
    {		
    	if (storedObject == null || target == null)
    		return ;
    	
    	// if object is a directory,
    	if (SwiftUtils.isDirectory(storedObject))
    	{
    		target.mkdirs();
    		return ;
    	}
    	// otherwise,
    	
    	if (!target.exists())
    	{
    		if (target.getParentFile() != null)
    			target.getParentFile().mkdirs() ;
    	}
    	
    	try
    	{    
    		AtomicInteger tryCounter = new AtomicInteger () ;
    		while (true)
    		{
	    		try
	    		{
		    		progInfo.setCurrentMessage(String.format("Downloading %s", storedObject.getName()));
		    		InputStream in = FileUtils.getInputStreamWithProgressFilter(progInfo, storedObject.getContentLength(), storedObject.downloadObjectAsInputStream()) ;	    		
		
		    		FileUtils.saveInputStreamInFile (in, target, true) ;
		    		break ;
	    		}
	    		catch (CommandException e) 
	    		{
	    			dealWithCommandException (e, tryCounter) ;
	    		}
    		}
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
    		AtomicInteger tryCounter = new AtomicInteger () ;
    		while (true)
    		{
	    		try
	    		{
		    		progInfo.setCurrentMessage(String.format("Uploading %s", file.getPath()));
		    		BasicFileAttributes attr = FileUtils.getFileAttr(Paths.get(file.getPath())) ;
			    	if (useCustomSegmentation)	
			    	{
			    		UploadInstructions ui = new UploadInstructions (file).setSegmentationSize(segmentationSize) ;
			    		if (ui.requiresSegmentation())
			    		{
			    			//largeObjectManager.uploadObjectAsSegments(storedObject, ui, attr.size(), progInfo, callback) ;
			    			largeObjectManager.uploadObjectAsSegments(storedObject, file, ui, attr.size(), progInfo, callback) ;
				    		return ;
			    		}
			    	}	    	
			    	in = FileUtils.getInputStreamWithProgressFilter(progInfo, attr.size(), Paths.get(file.getPath())) ;
			    	storedObject.uploadObject(in) ;
			    	break ;
	    		}
	    		catch (CommandException e) 
	    		{
	    			dealWithCommandException (e, tryCounter) ;
	    		}
    		}
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
    
    
    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void emptyContainer(Container container, StopRequester stopRequester, SwiftCallback callback) {
    	
    	CheckAccount () ;
    	
    	try
    	{
	        for (StoredObject so : eagerFetchStoredObjects(container)) 
	        {
	        	if (!keepGoing (stopRequester, callback))
	        		return ;
	        	
	        	if (so.exists())
	        		so.delete();
	            callback.onNumberOfCalls(account.getNumberOfCalls());
	        }
	        logger.info(String.format("Container %s has been emptied", container.getName()));
    	}
    	finally
    	{
	        reloadContainer(container, callback);
	        callback.onNumberOfCalls(account.getNumberOfCalls());
    	}
        
    }

    
    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void purgeContainer(Container container, StopRequester stopRequester, SwiftCallback callback) {
    	
    	CheckAccount () ;
    	
    	try
    	{
	        for (StoredObject so : eagerFetchStoredObjects(container)) 
	        {
	        	if (!keepGoing (stopRequester, callback))
	        		return ;
	        	
	        	if (so.exists())
	        		so.delete();
	            callback.onNumberOfCalls(account.getNumberOfCalls());
	        }
	        container.delete();
	        logger.info(String.format("Container %s has been removed", container.getName()));
    	}
    	finally
    	{
	        callback.onUpdateContainers(eagerFetchContainers(account));
	        callback.onNumberOfCalls(account.getNumberOfCalls());
    	}
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
    
    
    private void addedObjectToContainer(Container container, List<StoredObject> newObjects, SwiftCallback callback) {
    	
    	if (container == null)
    		throw new AssertionError ("container cannot be null") ;
    	
    	container.reload();

    	if (newObjects == null || newObjects.isEmpty())
    		return ;
    	
		int page = 0;
		while (page * MAX_PAGE_SIZE < newObjects.size())
		{
			callback.onAppendStoredObjects(container, page, newObjects.subList(page * MAX_PAGE_SIZE, Math.min(newObjects.size(), (page + 1) * MAX_PAGE_SIZE))) ;
			++page ;
		}
    }
    
    
    private void removedObjectFromContainer(Container container, List<StoredObject> deletedObjects, SwiftCallback callback) {
    	
    	if (container == null)
    		throw new AssertionError ("container cannot be null") ;
    	
    	container.reload();

    	if (deletedObjects == null || deletedObjects.isEmpty())
    		return ;

    	callback.onStoredObjectDeleted (container, deletedObjects) ;
    }
    
    
    /**
     * {@inheritDoc}.
     */
	@Override
	public void refreshDirectoriesOrStoredObjects(Container container, Directory parent, /*long depth,*/ SwiftCallback callback) {
		
		CheckAccount () ;
		
		try
		{
			//if (depth <= 0)
				loadContainerDirectory (container, parent, callback) ;
			/*else 
			{
				if (parent == null)
					callback.onNewStoredObjects();
				List<StoredObject> list = getContainedStoredObject (container, parent, depth) ;
				int page = 0;
				while (page * MAX_PAGE_SIZE < list.size())
				{
					callback.onAppendStoredObjects(container, page, list.subList(page * MAX_PAGE_SIZE, Math.min(list.size(), (page + 1) * MAX_PAGE_SIZE))) ;
					++page ;
				}
			}*/
			callback.onNumberOfCalls(account.getNumberOfCalls());
    	}
	    catch (OutOfMemoryError ome)
	    {
	    	dealWithOutOfMemoryError (ome, "refreshDirectoriesOrStoredObjects", callback) ;
	    }
	}
	
	
	/*
    private List<StoredObject> getContainedStoredObject(Container container, Directory parent, long depth) 
    {
    	if (container == null)
    		throw new AssertionError ("container cannot be null") ;
    	
    	List<StoredObject> ret = new ArrayList<StoredObject> () ;
    	
    	final String prefix ;
    	Character delimiter = SwiftUtils.separator.charAt(0) ;
    	List<DirectoryOrObject> directoriesOrObjects = new ArrayList<DirectoryOrObject> () ;
    	if (parent == null)
    		prefix = "" ;
    	else
    		prefix = parent.getName() + delimiter ;
 
    	directoriesOrObjects.addAll(container.listDirectory(prefix, delimiter, null, MAX_PAGE_SIZE)) ;
        while (!directoriesOrObjects.isEmpty()) 
        {	        	
        	List<StoredObject> listStoredObjects = new ArrayList<StoredObject> (directoriesOrObjects.size()) ;
        	for (DirectoryOrObject dirOrobj : directoriesOrObjects)
        	{
        		if (dirOrobj == null)
        			continue ;
        		if (depth > 0)
        		{
        			if (dirOrobj.isDirectory())
        				ret.addAll(getContainedStoredObject (container, dirOrobj.getAsDirectory(), depth - 1)) ;
        			else if (dirOrobj.isObject() && SwiftUtils.isDirectory(dirOrobj.getAsObject()))
        				ret.addAll(getContainedStoredObject (container, new Directory (dirOrobj.getAsObject().getName(), delimiter), depth - 1)) ;
        		}
        		if (dirOrobj.isObject())
        			listStoredObjects.add(dirOrobj.getAsObject()) ;
        	}
        	ret.addAll(listStoredObjects) ;

            String marker = directoriesOrObjects.get(directoriesOrObjects.size() - 1).getName() ;
            directoriesOrObjects.clear () ;
            directoriesOrObjects.addAll(container.listDirectory(prefix, delimiter, marker, MAX_PAGE_SIZE)) ;
        }
        return ret ;
    }*/
	
	
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
        		else
        		{
        			String dirName = dirOrobj.getName() ;
        			if (dirName.endsWith(SwiftUtils.separator)) 
        				dirName = dirName.substring(0, dirName.length() - 1) ;
        			StoredObject dir = container.getObject(dirName) ;
        			if (!dir.exists())
        			{
        				createDirectory(dir);
        				listStoredObjects.add(dir) ;
        			}
        		}
        	}
            callback.onAppendStoredObjects(container, page++, listStoredObjects);
            
            String marker = directoriesOrObjects.get(directoriesOrObjects.size() - 1).getName() ;
            directoriesOrObjects.clear () ;
            directoriesOrObjects.addAll(container.listDirectory(prefix, delimiter, marker, MAX_PAGE_SIZE)) ;
        }
    }
    
    
    private Collection<StoredObject> getAllContainedStoredObject (Container container, Directory parent)
    {
    	Set<StoredObject> results = new TreeSet<StoredObject>();
    	Queue<DirectoryOrObject> queue = new ArrayDeque<DirectoryOrObject> () ;
    	queue.addAll((parent == null) ? (container.listDirectory()) : (container.listDirectory(parent))) ;
    	while (!queue.isEmpty())
    	{
    		DirectoryOrObject currDirOrObj = queue.poll() ;    		
    		if (currDirOrObj != null)
    		{
				if (currDirOrObj.isObject())
					results.add(currDirOrObj.getAsObject()) ;
    			if (currDirOrObj.isDirectory())
    				queue.addAll(container.listDirectory(currDirOrObj.getAsDirectory())) ;
    		}
    	}
    	return results ;
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
			
			// the computation of the md5 value is quite resource demanding (when done for
			// a lots of large files)
			if (!overwrite)
			{
				logger.info("The file '{}' already exists in the cloud. It has been ignored.", path.toString());
				return true ;
			}
			
			String etag = obj.getEtag() ;
			String md5 = FileUtils.getMD5(path.toFile()) ;
			// the file is already uploaded, unless this is a collision... But we would then be quite unlucky
			// TODO: check other information in order to increase the confidence that the file is the same. 
			// ...
			if (etag != null && etag.equals(md5))
			{
				logger.info("The file '{}' already exists in the cloud.", path.toString());
				return true ; 
			}
			else
			{
				if (largeObjectManager != null && largeObjectManager.isSegmented(obj))
				{
					md5 = FileUtils.getSumOfSegmentsMd5(path.toFile(), getActualSegmentSize (obj)) ;
					if (etag.startsWith("\"")) ;
						etag = etag.replace("\"", "") ;
					if (etag != null && etag.equals(md5))
					{
						logger.info("The large file '{}' already exists in the cloud.", path.toString());
						return true ; 
					}
				}
				logger.info("A different version of the file '{}' already exists in the cloud. It {}.", path.toString(), (overwrite)?("will be overwritten"):("has been ignored"));
				if (!overwrite)
					return true ;
			}
		}
		return false ;
	}
	
	
	private long getActualSegmentSize (StoredObject obj)
	{
		if (largeObjectManager == null)
			return segmentationSize ;
		long segSize = largeObjectManager.getActualSegmentSize (obj) ;
		if (segSize <= 0)
			segSize = segmentationSize ;
		return segSize ;
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
	public synchronized void findDifferences (Container container, StoredObject remote, File local, ResultCallback<Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > > resultCallback, StopRequester stopRequester, SwiftCallback callback) throws IOException
	{
		CheckAccount () ;
		
		if (!remote.getBareName().equals(local.getName()))
			throw new IllegalArgumentException ("The local and the remote items must have the same name") ; 
		
    	if (SwiftUtils.isDirectory(remote) && Files.isDirectory(Paths.get(local.getPath()))) {
    		// here we compare two folders
    		findDirectoriesDifferences (container, remote, local, resultCallback, stopRequester, callback) ;
    	}
    	else if (SwiftUtils.isDirectory(remote)) {
    		throw new AssertionError ("A remote directory can only be compared with a local directory") ;
    	}
    	else{
    		// here we only compare two "files"
    		findFilesDifferences (container, remote, local, resultCallback, stopRequester, callback) ;
    	}
	}
	
	
	private void findFilesDifferences (Container container, StoredObject remote, File local, ResultCallback<Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > > resultCallback, StopRequester stopRequester, SwiftCallback callback) throws IOException
	{
		if (SwiftUtils.isDirectory(remote) || Files.isDirectory(Paths.get(local.getPath())))
			throw new IllegalArgumentException () ;
		
		List<Pair<? extends ComparisonItem, ? extends ComparisonItem> > ret = new ArrayList<> () ;

		RemoteItem ri = new RemoteItem (remote) ;
		LocalItem li = new LocalItem (Paths.get(local.getPath()), Paths.get(local.getPath()).getParent(), getActualSegmentSize (remote)) ;	
		
		if (ri.equals(li))
		{
			if (!li.getName().equals(ri.getName()))
			{
				ret.add(Pair.newPair(li, (ComparisonItem)null)) ;
				ret.add(Pair.newPair((ComparisonItem)null, ri)) ;
			}
		}
		else
		{
			if (!li.getName().equals(ri.getName()) && ri.exists() && li.exists ())
			{
				ret.add(Pair.newPair(li, (ComparisonItem)null)) ;
				ret.add(Pair.newPair((ComparisonItem)null, ri)) ;
			}
			else if (ri.exists() || li.exists ())
				ret.add(Pair.newPair(li, ri)) ;
		}
		
		callback.onNumberOfCalls(account.getNumberOfCalls());
		resultCallback.onResult(ret);
	}
	
	
	private void findDirectoriesDifferences (Container container, StoredObject remote, File local, ResultCallback<Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > > resultCallback, StopRequester stopRequester, SwiftCallback callback) throws IOException
	{
		if (!SwiftUtils.isDirectory(remote) || !Files.isDirectory(Paths.get(local.getPath())))
			throw new IllegalArgumentException () ;
			
		List<Pair<? extends ComparisonItem, ? extends ComparisonItem> > ret = new ArrayList<> () ;
		
		// here we compare two folders	
		Path parentLocalDir = Paths.get(local.getPath()) ;
		Queue<Path> filesQueue = getAllFilesPath(parentLocalDir, true) ;
		
		String parentDir = SwiftUtils.getParentDirectory(remote) ;		
		String ending = local.getName() + SwiftUtils.separator ;
		if (parentDir.endsWith(ending))
			parentDir = parentDir.substring(0, parentDir.length() - ending.length()) ;
		
		// Progress notification
		// two phases: 
		// 1. find missing objects or object that differ (50 %)
		// 2. find file that are missing (100%)
		int totalFiles = filesQueue.size() ;
		int currentUplodedFilesCount = 0 ;
		ProgressInformation progInfo = new ProgressInformation (callback, false) ;
		
		Set<StoredObject> consideredStoredObjectSet = new HashSet<> () ;
	
		try
		{
			progInfo.setTotalProgress(0.5);
			progInfo.setTotalMessage("Searching for missing remote files");
			for (Path path : filesQueue)
			{		
				if (!keepGoing (stopRequester, callback))
	        		return ;
	        	
				++currentUplodedFilesCount ;
	
				if (!isPathValid (path))
					continue ;
	
				progInfo.setCurrentMessage(String.format("Current file: %s)", path.toString()));
				progInfo.setCurrentProgress(currentUplodedFilesCount / (double)totalFiles);
				progInfo.report();
				
				StoredObject obj = getObjectRelativelyInDirectory (container, parentDir, parentLocalDir, path) ;
				
				if (obj.exists())
					consideredStoredObjectSet.add(obj) ;
				
	    		RemoteItem ri = new RemoteItem (obj) ;
	    		LocalItem li = new LocalItem (path, parentLocalDir, getActualSegmentSize (obj)) ;
	    		if (ri.equals(li) || (!ri.exists() && !li.exists ()))
	    			continue ;
	    		ret.add(Pair.newPair(li, ri)) ; 
	    		
				callback.onNumberOfCalls(account.getNumberOfCalls());
			}
			
			// here we need to determine the objects that exist only remotely
			progInfo.setTotalProgress(1.0);
			progInfo.setTotalMessage("Searching for missing local files");
			progInfo.report();
			
			//Set<StoredObject> allObjectsSet = new HashSet<> (eagerFetchStoredObjects(container, remote.getName()  + SwiftUtils.separator)) ;
			Set<StoredObject> allObjectsSet = new HashSet<> (getAllContainedStoredObject(container, new Directory(remote.getName()  + SwiftUtils.separator, SwiftUtils.separator.charAt(0)))) ;
			allObjectsSet.removeAll(consideredStoredObjectSet) ;
			currentUplodedFilesCount = 0 ;
			totalFiles = allObjectsSet.size() ;
			for (StoredObject so : allObjectsSet)
			{
				++currentUplodedFilesCount ;
				
				if (so == null || !so.exists())
					continue ;
				
				progInfo.setCurrentMessage(String.format("Current object: %s)", so.getName()));
				progInfo.setCurrentProgress(currentUplodedFilesCount / (double)totalFiles);
				progInfo.report();
				
				ret.add(Pair.newPair((LocalItem)null, new RemoteItem (so))) ;
			}
		}
	    catch (OutOfMemoryError ome)
	    {
	    	dealWithOutOfMemoryError (ome, "findDifferences", callback) ;
	    }
    	finally
    	{
    		callback.onNumberOfCalls(account.getNumberOfCalls());
    	}	
		resultCallback.onResult(ret);
	}
	
	
	private boolean isPathValid (Path path)
	{
		if (Files.notExists(path))
			return false ;
		if (Files.isSymbolicLink(path))
			return false ;
		return true ;
	}
	
	
	private StoredObject getObjectRelativelyInDirectory (Container container, String parentDir, Path source, Path path)
	{
		final String separator = SwiftUtils.separator ;
		
		StringBuilder objectPathBuilder = new StringBuilder () ;
		if (parentDir != null)
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
		
		return container.getObject(objectPath.toString());
	}
	
	
	private Queue<Path> getAllFilesPath (Path srcDir, boolean inludeDir) throws IOException
	{
		try 
		{
			return FileUtils.getAllFilesPath(srcDir, true) ;
		} 
		catch (IOException e) 
		{
			if (e instanceof AccessDeniedException) {
				StringBuilder msg = new StringBuilder () ;
				msg.append("File Access Denied") ;
				if (((AccessDeniedException)e).getFile() != null) {
					msg.append(": ") ;
					msg.append(((AccessDeniedException)e).getFile()) ;
				}
				throw new CommandException (msg.toString()) ;
			}
			else
				throw e ;
		}
	}
	
	
    /**
     * {@inheritDoc}.
     * @throws IOException 
     */
	@Override
	public synchronized void uploadDirectory(Container container, StoredObject parentObject, File directory, boolean overwriteAll, StopRequester stopRequester, SwiftCallback callback) throws IOException {

		CheckAccount () ;
    	
		Path source = Paths.get(directory.getPath()) ;
		Queue<Path> filesQueue = getAllFilesPath(source, true) ;
		
		String parentDir = SwiftUtils.getParentDirectory(parentObject) ;
		
		int totalFiles = filesQueue.size() ;
		int currentUplodedFilesCount = 0 ;
		ProgressInformation progInfo = new ProgressInformation (callback, false) ;
		
		List<StoredObject> newObjects = new ArrayList<> () ;
		
		try
		{
			for (Path path : filesQueue)
			{		
				if (!keepGoing (stopRequester, callback))
	        		return ;
	        	
				++currentUplodedFilesCount ;
	
				if (!isPathValid (path))
					continue ;
	
				// Progress notification
				totalProgress (currentUplodedFilesCount, totalFiles, path, progInfo, true) ;
				
				StoredObject obj = getObjectRelativelyInDirectory (container, parentDir, source, path) ;
				
				if (shouldBeIgnored (obj, path, overwriteAll))
					continue ;
				
				if (Files.isDirectory(path))
				{				
					// here we create a directory
					createDirectory (obj) ;
				}
				else
				{
					uploadObject (obj, path.toFile(), progInfo, callback) ;
				}
				newObjects.add(obj) ;
				callback.onNumberOfCalls(account.getNumberOfCalls());
			}
			
			logger.info(
					"Uploaded directory '{}', in directory '{}', in container '{}'. Number of files: {}",
					directory.getPath(), parentDir, container.getPath(),
					String.valueOf(filesQueue.size()));
		}
		finally
		{
        	//reloadContainer(container, callback);
        	addedObjectToContainer(container, newObjects, callback);
			callback.onNumberOfCalls(account.getNumberOfCalls());
		}
	}

	
    /**
     * {@inheritDoc}.
     * @throws IOException 
     */
	@Override
	public synchronized void uploadFiles(Container container, StoredObject parentObject, File[] files, boolean overwriteAll, StopRequester stopRequester, SwiftCallback callback) throws IOException 
	{	
		CheckAccount () ;
    	
		if (files == null || files.length == 0)
			return ;
		
		String parentDir = SwiftUtils.getParentDirectory(parentObject) ;
		
		int totalFiles = files.length ;
		int currentUplodedFilesCount = 0 ;
		ProgressInformation progInfo = new ProgressInformation (callback, false) ;
		
		List<StoredObject> newObjects = new ArrayList<> () ;
		
		try
		{
			for (File file : files)
			{			
				if (!keepGoing (stopRequester, callback))
	        		return ;
	        	
				++currentUplodedFilesCount ;
				
				if (file == null || !file.exists()) 
					continue ;
	
				// Progress notification
				totalProgress (currentUplodedFilesCount, totalFiles, Paths.get(file.toURI()), progInfo, true) ;
				
				StringBuilder objectPathBuilder = new StringBuilder () ;
				objectPathBuilder.append (parentDir) ;
				objectPathBuilder.append (file.getName()) ;
				String objectPath = objectPathBuilder.toString() ;		
				
				StoredObject obj = container.getObject(objectPath.toString());
				
				uploadFile (obj, file, progInfo, overwriteAll, callback) ;
				
				newObjects.add(obj) ;
			}
		}
		finally
		{
        	//reloadContainer(container, callback);
        	addedObjectToContainer(container, newObjects, callback);
			callback.onNumberOfCalls(account.getNumberOfCalls());
		}
	}
	
	
    /**
     * {@inheritDoc}.
     * @throws IOException 
     */
	@Override
	public synchronized void uploadFiles(Container container, Collection<Pair<? extends StoredObject, ? extends File> > pairObjectFiles, boolean overwriteAll, StopRequester stopRequester, SwiftCallback callback) throws IOException
	{
		CheckAccount () ;
		
		if (pairObjectFiles == null || pairObjectFiles.isEmpty())
			return ;
		
		int totalFiles = pairObjectFiles.size() ;
		int currentUplodedFilesCount = 0 ;
		ProgressInformation progInfo = new ProgressInformation (callback, false) ;
		
		List<StoredObject> newObjects = new ArrayList<> () ;
		
		try
		{
			for (Pair<? extends StoredObject, ? extends File> pair : pairObjectFiles)
			{			
				if (!keepGoing (stopRequester, callback))
	        		return ;
	        	
				++currentUplodedFilesCount ;
				
				if (pair == null)
					continue ;
				File file = pair.getSecond() ;
				if (pair.getSecond() == null || !pair.getSecond().exists()) 
					continue ;
				StoredObject obj = pair.getFirst() ;
				if (obj == null)
					continue ;
	
				// Progress notification
				totalProgress (currentUplodedFilesCount, totalFiles, Paths.get(file.toURI()), progInfo, true) ;
				
				uploadFile (obj, file, progInfo, overwriteAll, callback) ;
				
				newObjects.add(obj) ;
			}
		}
		finally
		{
        	//reloadContainer(container, callback);
        	addedObjectToContainer(container, newObjects, callback);
			callback.onNumberOfCalls(account.getNumberOfCalls());
		}
	}
	
	
	private void uploadFile (StoredObject obj, File file, ProgressInformation progInfo, boolean overwriteAll, SwiftCallback callback) throws IOException
	{
		if (file.isDirectory() && !obj.exists())
		{
			// we create a directory
			createDirectory (obj) ;
			return ;
		}
		if (shouldBeIgnored (obj, Paths.get(file.getPath()), overwriteAll))
			return ;
		uploadObject (obj, file, progInfo, callback) ;
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
			createDirectory (obj) ;
        	//reloadContainer(container, callback);
        	addedObjectToContainer(container, Arrays.asList(obj), callback);
		}
		callback.onNumberOfCalls(account.getNumberOfCalls());
	}
	
	
	private void createDirectory (StoredObject obj)
	{
		//obj.uploadObject(new UploadInstructions (new byte[] {}).setContentType(SwiftUtils.directoryContentType)) ;
		byte[] emptyfile = {} ; 
		UploadInstructions inst = new UploadInstructions (emptyfile) ;
		inst.setContentType(SwiftUtils.directoryContentType) ;
		obj.uploadObject(inst) ;
	}


    /**
     * {@inheritDoc}.
     */
	@Override
	public synchronized void deleteDirectory(Container container, StoredObject storedObject, StopRequester stopRequester, SwiftCallback callback) {
		
		CheckAccount () ;
    	
    	if (!SwiftUtils.isDirectory(storedObject))
    		throw new AssertionError ("The object to delete must be a directory.") ;
    	
    	List<StoredObject> deletedObjects = new ArrayList<> () ;
    	
    	try
    	{
			String prefix = storedObject.getName() + SwiftUtils.separator ;
			Collection<StoredObject> listObj = eagerFetchStoredObjects(container, prefix) ;
			int currentUplodedFilesCount = 0 ;
			int totalFiles = listObj.size() ;
			ProgressInformation progInfo = new ProgressInformation (callback, false) ;
			
	        for (StoredObject so : listObj) 
	        {
	        	if (!keepGoing (stopRequester, callback))
	        		return ;
	        	
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
	
	        	deleteStoredObjects (container, Arrays.asList(so), stopRequester, false, callback) ;
	        	deletedObjects.add(so) ;
	        }
	        logger.info("Deleted directory '{}'", storedObject.getName());
	        deleteStoredObjects (container, Arrays.asList(storedObject), stopRequester, false, callback) ;
	        deletedObjects.add(storedObject) ;
		}
	    catch (OutOfMemoryError ome)
	    {
	    	dealWithOutOfMemoryError (ome, "deleteDirectory", callback) ;
	    }
    	finally
    	{
        	//reloadContainer(container, callback);
        	removedObjectFromContainer(container, deletedObjects, callback);
	        callback.onNumberOfCalls(account.getNumberOfCalls());
    	}
	}
	
	
	private boolean keepGoing (StopRequester stopRequester, SwiftCallback callback)
	{
		if (stopRequester == null)
			return true ;
		boolean go = !stopRequester.isStopRequested() ;
		if (!go)
			callback.onStopped();
		return go ;
	}
	

	private void dealWithOutOfMemoryError (OutOfMemoryError ome, String functionName, SwiftCallback callback)
	{		
    	System.gc() ; // pointless at this stage, but anyway...
    	logger.error(String.format("OutOfMemory error occurred while calling SwiftOperationsImpl.%s", functionName), ome);
    	callback.onError(new CommandException ("The JVM ran out of memory - <font color=red><b>You must exit</b></font>"));
    	throw ome ; 
	}
}
