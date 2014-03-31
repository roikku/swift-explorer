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
import java.util.List;
import java.util.Queue;

import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.instructions.UploadInstructions;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.PaginationMap;
import org.javaswift.joss.model.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SwiftOperationsImpl implements SwiftOperations {
	
    private static final int MAX_PAGE_SIZE = 9999;
    
    final Logger logger = LoggerFactory.getLogger(SwiftOperationsImpl.class);

    private volatile Account account = null;
    
    // TODO: temporary. For experimentation
    private final boolean experimentInputStreamProgressMonitor = true ;
    
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
	}
    

    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void createContainer(ContainerSpecification spec, SwiftCallback callback) {
    	
    	CheckAccount () ;
    	
        if (spec != null) {
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
    	   	
        List<StoredObject> results = new ArrayList<StoredObject>(parent.getCount());
        PaginationMap map = parent.getPaginationMap(MAX_PAGE_SIZE);
        for (int page = 0; page < map.getNumberOfPages(); page++) {
            results.addAll(parent.list(map, page));
        }
        return results;
    }
    
    
    private Collection<StoredObject> eagerFetchStoredObjects(Container parent, String prefix) {
        List<StoredObject> results = new ArrayList<StoredObject>(parent.getCount());
        PaginationMap map = parent.getPaginationMap(MAX_PAGE_SIZE);
        for (int page = 0; page < map.getNumberOfPages(); page++) {
            results.addAll(parent.list(prefix, map.getMarker(page), map.getPageSize()));
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
    	
        for (StoredObject storedObject : storedObjects) {
            storedObject.delete();
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
            	pathBuilder.append(storedObject.getName()) ;
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
	    	if (!experimentInputStreamProgressMonitor)
	    		storedObject.downloadObject(target);
	    	else
	    	{	    		    		
	    		progInfo.setCurrentMessage(String.format("Downloading %s", storedObject.getName()));
	    		InputStream in = FileUtils.getInputStreamWithProgressFilter(progInfo, storedObject.getContentLength(), storedObject.downloadObjectAsInputStream()) ;	    		

	    		FileUtils.saveInputStreamInFile (in, target, true) ;
	    	}
    	}
	    catch (OutOfMemoryError ome)
	    {
	    	System.gc() ; // pointless at this stage, but anyway...
	    	callback.onError(new CommandException ("The JVM ran out of memory")) ;
	    }
    }
    
    
    private void uploadObject (StoredObject storedObject, File file, ProgressInformation progInfo, SwiftCallback callback) throws IOException
    {			
    	if (storedObject == null || file == null)
    		return ;
    	try
    	{
	    	if (useCustomSegmentation)	
	    	{
		    	if (!experimentInputStreamProgressMonitor)
		    		storedObject.uploadObject(new UploadInstructions (file).setSegmentationSize(segmentationSize));
		    	else
		    	{	   		
		    		progInfo.setCurrentMessage(String.format("Uploading %s", file.getPath()));
		    		BasicFileAttributes attr = FileUtils.getFileAttr(Paths.get(file.getPath())) ;
		    		storedObject.uploadObject(new UploadInstructions (FileUtils.getInputStreamWithProgressFilter(progInfo, attr.size(), Paths.get(file.getPath()))).setSegmentationSize(segmentationSize)) ;
		    	}
	    	}
	    	else
	    	{
		    	if (!experimentInputStreamProgressMonitor)
		    		storedObject.uploadObject(file);
		    	else
		    	{	   		
		    		progInfo.setCurrentMessage(String.format("Uploading %s", file.getPath()));
		    		BasicFileAttributes attr = FileUtils.getFileAttr(Paths.get(file.getPath())) ;
		    		storedObject.uploadObject(FileUtils.getInputStreamWithProgressFilter(progInfo, attr.size(), Paths.get(file.getPath()))) ;
		    	}
	    	}
    	}
	    catch (OutOfMemoryError ome)
	    {
	    	System.gc() ; // pointless at this stage, but anyway...
	    	callback.onError(new CommandException ("The JVM ran out of memory")) ;
	    }
    }

    
    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void emptyContainer(Container container, SwiftCallback callback) {
    	
    	CheckAccount () ;
    	
        for (StoredObject so : eagerFetchStoredObjects(container)) {
            so.delete();
            callback.onNumberOfCalls(account.getNumberOfCalls());
        }
        reloadContainer(container, callback);
        callback.onNumberOfCalls(account.getNumberOfCalls());
    }

    
    /**
     * {@inheritDoc}.
     */
    @Override
    public synchronized void purgeContainer(Container container, SwiftCallback callback) {
    	
    	CheckAccount () ;
    	
        for (StoredObject so : eagerFetchStoredObjects(container)) {
            so.delete();
            callback.onNumberOfCalls(account.getNumberOfCalls());
        }
        container.delete();
        callback.onUpdateContainers(eagerFetchContainers(account));
        callback.onNumberOfCalls(account.getNumberOfCalls());
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
    	
    	int total = container.getCount() ;
    	int current = 0 ;
    	
        int page = 0;
        List<StoredObject> list = (List<StoredObject>) container.list("", null, MAX_PAGE_SIZE);
        callback.onNewStoredObjects();
        while (!list.isEmpty()) {
        	
        	if (showProgress)
        	{
        		current += list.size() ;
        		callback.onProgress(current / (double)total, "Refreshing the list of documents", 0, "") ;
        	}
        	
            callback.onAppendStoredObjects(container, page++, list);
            list = (List<StoredObject>) container.list("", list.get(list.size() - 1).getName(), MAX_PAGE_SIZE);
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
			
			String etag = obj.getEtag() ;
			String md5 = FileUtils.getMD5(path.toFile()) ;
			// the file is already uploaded, unless this is a collision... But we would then be quite unlucky
			// TODO: check other information in order to increase the confidence that the file is the same. 
			// ...
			if (etag != null && etag.equals(md5))
				return true ; 
			else
			{
				logger.info("file '{}' already exists on the cloud, but differs. it has been {}", path.toString(), (overwrite)?("overwritten"):("ignored"));
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
		progInfo.setTotalMessage(String.format("%d / %d files processed (current file size: %s).", currentUplodedFilesCount, totalFiles, FileUtils.humanReadableByteCount(attr.size(),  true)));
		
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
		progInfo.setTotalMessage(String.format("%d / %d objects processed (current object size: %s).", currentUplodedFilesCount, totalFiles, FileUtils.humanReadableByteCount(so.getContentLength(),  true)));
		
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


	@Override
	public synchronized void deleteDirectory(Container container, StoredObject storedObject, SwiftCallback callback) {
		
		CheckAccount () ;
    	
    	if (!SwiftUtils.isDirectory(storedObject))
    		throw new AssertionError ("The object to delete must be a directory.") ;
    	
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
	}
}
