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
* Note: the class has been renamed from CloudieOperations
* to SwiftOperations
*
*/


package org.swiftexplorer.swift.operations;

import org.swiftexplorer.config.proxy.HasProxySettings;
import org.swiftexplorer.config.swift.HasSwiftSettings;
import org.swiftexplorer.swift.SwiftAccess;
import org.swiftexplorer.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.Directory;
import org.javaswift.joss.model.StoredObject;

public interface SwiftOperations {
	
	  /**
     * callback interface, SwiftOperations will use this to notify the user
     * interface of updates.
     */
    public interface SwiftCallback {

        /**
         * signals the start of an operation.
         */
        void onStart();

        /**
         * signals the end of an operation. Its always called if onStart() was
         * called first.
         */
        void onDone();

        /**
         * signals an protocol exception.
         * @param ex the exception.
         */
        void onError(CommandException ex);

        /**
         * signals an update of the available containers.
         * @param containers the containers.
         */
        void onUpdateContainers(Collection<Container> containers);


        /**
         * called when a new list of objects starts. followed by at least one
         * call to onAppendStoredObjects.
         */
        void onNewStoredObjects();

        /**
         * appends newly loaded objects to the list.
         * @param container the container to which the objects belong.
         * @param page the page.
         * @param storedObjects the objects.
         */
        void onAppendStoredObjects(Container container, int page, Collection<StoredObject> storedObjects);

        /**
         * signals a successful authentication.
         */
        void onLoginSuccess();

        /**
         * signals a successful logout.
         */
        void onLogoutSuccess();

        /**
         * signals the update of a containers metadata.
         * @param container the container.
         */
        void onContainerUpdate(Container container);

        /**
         * signals the update of a stored objects meta data.
         * @param obj the stored object.
         */
        void onStoredObjectUpdate(StoredObject obj);

        /**
         * called when the number of calls have changed.
         */
        void onNumberOfCalls(int nrOfCalls);

        /**
         * @param storedObject the deleted stored object.
         */
        void onStoredObjectDeleted(Container container, StoredObject storedObject);

        /**
         * @param storedObjects the deleted stored objects.
         */
        void onStoredObjectDeleted(Container container, Collection<StoredObject> storedObjects);
        
        /**
         * signals the progress of the total task and current task.
         * @param totalProgress, the progress of the total task, should be between 0 to 1.
         * @param totalMsg, and arbitrary message
         * @param currentProgress, the progress of the current task, should be between 0 to 1.
         * @param currentMsg, and arbitrary message
         */
        void onProgress(double totalProgress, String totalMsg, double currentProgress, String currentMsg);
        
        /**
         * signals that an operation has been stopped, possibly before it was complete.
         */
        void onStopped();
    }
    
    
	/**
	 * StopRequestor interface, the client of SwiftOperations uses this to
	 * notify the task to stop
	 */
	public interface StopRequester {
		public boolean isStopRequested();
	}
	
	
	/**
	 * ComparisonItem interface
	 */
	public interface ComparisonItem extends Comparable<ComparisonItem> {
		public boolean isRemote () ;
		public String getMD5 () ;
		public String getName () ;
		public String getPath () ;
		public long getSize () ;
		public boolean exists () ;
		public String getRemoteFullName () ;
	}
	
	
	/**
	 * ResultCallback interface, SwiftOperations uses this to return result
	 * to the client
	 */
	public interface ResultCallback <T> {
		public void onResult (T res) ;
	}
	
    
    /**
     * performs a login.
     * @param accConf.
     * @param url the url to login against.
     * @param tenant the tenant.
     * @param user the username.
     * @param pass the password.
     * @param callback the callback.
     */
    void login(AccountConfig accConf, String url, String tenant, String user, String pass, SwiftCallback callback);

    
    /**
     * performs a login.
     * @param accConf.
     * @param callback the callback.
     */
    void login(AccountConfig accConf, SwiftCallback callback);
    
    
    /**
     * performs a login.
     * @param accConf.
     * @param proxySettings.
     * @param callback the callback.
     */
	void login(AccountConfig accConf, HasProxySettings proxySettings, SwiftCallback callback) ;

    
    /**
     * performs a login.
     * @param accConf.
     * @param swiftSettings has the preferred region and the segmentation size (the maximum segment size for large object support (must be between 10485760 and 5368709120)).
     * @param url the url to login against.
     * @param tenant the tenant.
     * @param user the username.
     * @param pass the password.
     * @param callback the callback.
     */
    void login(AccountConfig accConf, HasSwiftSettings swiftSettings, String url, String tenant, String user, String pass, SwiftCallback callback);

    
    /**
     * performs a login.
     * @param accConf.
     * @param swiftSettings has the preferred region and the segmentation size (the maximum segment size for large object support (must be between 10485760 and 5368709120)).
     * @param callback the callback.
     */
    void login(AccountConfig accConf, HasSwiftSettings swiftSettings, SwiftCallback callback);
    
    
    /**
     * performs a login.
     * @param accConf.
     * @param swiftSettings has the preferred region and the segmentation size (the maximum segment size for large object support (must be between 10485760 and 5368709120)).
     * @param proxySettings.
     * @param callback the callback.
     */
    void login(AccountConfig accConf, HasSwiftSettings swiftSettings, HasProxySettings proxySettings, SwiftCallback callback);
    

    /**
     * logout from the current session
     * @param callback the callback.
     */
    void logout(SwiftCallback callback);

    
    /**
     * creates a new container.
     * @param spec the container specifications.
     * @param callback the callback to call when done.
     */
    void createContainer(ContainerSpecification spec, SwiftCallback callback);

    
    /**
     * creates a new stored objects.
     * @param container the container to store in.
     * @param file the file(s) to upload.
     * @param stopRequester to stop the task
     * @param callback the callback to call when done.
     * @throws IOException 
     */
    void createStoredObjects(Container container, File[] file, StopRequester stopRequester, SwiftCallback callback) throws IOException;

    
    /**
     * deletes a container and all files in it.
     * @param container the container.
     * @param callback the callback to call when done.
     */
    void deleteContainer(Container container, SwiftCallback callback);

    
    /**
     * deletes a single stored object.
     * @param container the container holding the object.
     * @param storedObject the object.
     * @param stopRequester to stop the task
     * @param callback the callback to call.
     */
    void deleteStoredObjects(Container container, List<StoredObject> storedObject, StopRequester stopRequester, SwiftCallback callback);

    
    /**
     * downloads a stored object into a file.
     * @param container the container.
     * @param storedObject the stored object to download.
     * @param target the target file.
     * @param stopRequester to stop the task
     * @param callback the callback to call when done.
     * @throws IOException
     */
    void downloadStoredObject(Container container, StoredObject storedObject, File target, StopRequester stopRequester, SwiftCallback callback) throws IOException;

    
    /**
     * downloads a stored object into a file.
     * @param container the container.
     * @param pairObjectFile collection of pair, first is the StoredObject to download, and second is the destination file. 
     * @param stopRequester to stop the task
     * @param callback the callback to call when done.
     * @throws IOException
     */
	void downloadStoredObject(Container container, Collection<Pair<? extends StoredObject, ? extends File> > pairObjectFiles, boolean overwriteAll, StopRequester stopRequester, SwiftCallback callback) throws IOException ;
	
	
    /**
     * purges a container, deleting all files and the container.
     * @param container the container
     * @param callback the callback to call when done.
     */
    void purgeContainer(Container container, StopRequester stopRequester, SwiftCallback callback);

    
    /**
     * empties a container, deleting all files but not the container.
     * @param c the container
     * @param callback the callback to call when done.
     */
    void emptyContainer(Container c, StopRequester stopRequester, SwiftCallback callback);

    
    /**
     * refreshes the container list.
     * @param callback the callback to call when done.
     */
    void refreshContainers(SwiftCallback callback);

    
    /**
     * refreshes the stored object list in the given container.
     * @param container the container.
     * @param callback the callback to call when done.
     */
    void refreshStoredObjects(Container container, SwiftCallback callback);

    
    /**
     * retrieves metadata for the given container.
     * @param c the container.
     * @param callback the callback when done.
     */
    void getMetadata(Container c, SwiftCallback callback);

    
    /**
     * retrieves metadata for the given stored object.
     * @param obj the object.
     * @param callback the callback to call when done.
     */
    void getMetadata(StoredObject obj, SwiftCallback callback);


    /**
     * creates new stored objects from a directory.
     * @param container the container to store in.
     * @param parentObject the parent of the folder.
     * @param directory the directory to upload.
     * @param stopRequester to stop the task.
     * @param callback the callback to call when done.
     * @throws IOException
     */
	void uploadDirectory(Container container, StoredObject parentObject, File directory, boolean overwriteAll, StopRequester stopRequester, SwiftCallback callback) throws IOException;

	
    /**
     * creates a new stored objects from a files list. Note that if a given file is a directory, then an empty directory
     * is created, but its contents, if any, will not be (recursively) uploaded (for uploading a full directory, see 
     * the method uploadDirectory). 
     * @param container the container to store in.
     * @param parentObject the "parent" of the new StoredObject.
     * @param file the file(s) to upload.
     * @param stopRequester to stop the task.
     * @param callback the callback to call when done.
     * @throws IOException
     */
    void uploadFiles(Container container, StoredObject parentObject, File[] files, boolean overwriteAll, StopRequester stopRequester, SwiftCallback callback) throws IOException;
    
    
    /**
     * creates a new stored objects from a files list. Note that if a given file is a directory, then an empty directory
     * is created, but its contents, if any, will not be (recursively) uploaded (for uploading a full directory, see 
     * the method uploadDirectory). 
     * @param container the container to store in.
     * @param pairObjectFile collection of pair, first is the StoredObject, and second is the file to upload. 
     * @param stopRequester to stop the task.
     * @param callback the callback to call when done.
     * @throws IOException
     */
    void uploadFiles(Container container, Collection<Pair<? extends StoredObject, ? extends File> > pairObjectFiles, boolean overwriteAll, StopRequester stopRequester, SwiftCallback callback) throws IOException;
     
    
    /**
     * creates a new "directory".
     * @param container the container to store in.
     * @param parentObject the "parent" of the new StoredObject.
     * @param directoryName the name of the new directory.
     * @param callback the callback to call when done.
     */
    void createDirectory(Container container, StoredObject parentObject, String directoryName, SwiftCallback callback);
    
    
    /**
     * deletes a directory stored object.
     * @param container the container holding the object directory.
     * @param storedObject the object.
     * @param stopRequester to stop the task.
     * @param callback the callback to call.
     */
    void deleteDirectory(Container container, StoredObject storedObject, StopRequester stopRequester, SwiftCallback callback);
    
    
    /**
     * refreshes the directories and stored object list in the given container under the given parent.
     * @param container the container.
     * @param parent the parent directory.
     * @param callback the callback to call when done.
     */
    void refreshDirectoriesOrStoredObjects(Container container, Directory parent, SwiftCallback callback);
    
    
    /**
     * find the differences between local and remote objects.
     * 
     * Note that remote and local must be both either directory or file.
     * Furthermore, they MUST have the same name.
     * 
     * @param container the container.
     * @param remote the remote object.
     * @param local the local file.
     * @param resultCallback the callback to call when the result is ready
     * @param stopRequester to stop the task
     * @param callback the callback to call when done.
     * @throws IOException
     */
    void findDifferences (Container container, StoredObject remote, File local, ResultCallback<Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > > resultCallback, StopRequester stopRequester, SwiftCallback callback) throws IOException ;
}
