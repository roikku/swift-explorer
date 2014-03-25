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

import org.swiftexplorer.swift.SwiftAccess;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.model.Container;
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
         * signals the progress of the current task.
         * @param p, the progress, should be between 0 to 1.
         * @param msg, and arbitrary message
         */
        void onProgress(double p, String msg);
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
     * @param swiftAccess.
     * @param callback the callback.
     */
    void login(AccountConfig accConf, SwiftAccess swiftAccess, SwiftCallback callback);
    

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
     * @param callback the callback to call when done.
     */
    void createStoredObjects(Container container, File[] file, SwiftCallback callback);

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
     * @param callback the callback to call.
     */
    void deleteStoredObjects(Container container, List<StoredObject> storedObject, SwiftCallback callback);

    /**
     * downloads a stored object into a file.
     * @param container the container.
     * @param storedObject the stored object to download.
     * @param target the target file.
     * @param callback the callback to call when done.
     */
    void downloadStoredObject(Container container, StoredObject storedObject, File target, SwiftCallback callback) throws IOException;

    /**
     * purges a container, deleting all files and the container.
     * @param container the container
     * @param callback the callback to call when done.
     */
    void purgeContainer(Container container, SwiftCallback callback);

    /**
     * empties a container, deleting all files but not the container.
     * @param c the container
     * @param callback the callback to call when done.
     */
    void emptyContainer(Container c, SwiftCallback callback);

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
     * @param callback the callback to call when done.
     */
	void uploadDirectory(Container container, StoredObject parentObject, File directory, boolean overwriteAll, SwiftCallback callback) throws IOException;

	
    /**
     * creates a new stored objects from a files list.
     * @param container the container to store in.
     * @param parentObject the "parent" of the new StoredObject.
     * @param file the file(s) to upload.
     * @param callback the callback to call when done.
     */
    void uploadFiles(Container container, StoredObject parentObject, File[] files, boolean overwriteAll, SwiftCallback callback) throws IOException;
    
    
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
     * @param callback the callback to call.
     */
    void deleteDirectory(Container container, StoredObject storedObject, SwiftCallback callback);
}
