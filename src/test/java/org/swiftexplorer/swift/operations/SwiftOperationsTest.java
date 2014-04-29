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
*  package (src/test/java): org.javaswift.cloudie.ops
*  
* Note: the class has been renamed from CloudieOperationsTest
* to SwiftOperationsTest
*
*/


package org.swiftexplorer.swift.operations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.TestUtils;
import org.swiftexplorer.swift.client.factory.AccountConfigFactory;
import org.swiftexplorer.swift.operations.SwiftOperations.SwiftCallback;
import org.swiftexplorer.swift.util.SwiftUtils;
import org.swiftexplorer.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.Directory;
import org.javaswift.joss.model.StoredObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class SwiftOperationsTest {

	final Logger logger = LoggerFactory.getLogger(SwiftOperationsTest.class);
	
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();
    
    private SwiftOperations ops;
    private SwiftCallback callback;
    private Account account;
    private AccountConfig accConf ;

    @Before
    public void init() {
    	accConf = AccountConfigFactory.getMockAccountConfig() ;
        ops = new SwiftOperationsImpl();
        callback = Mockito.mock(SwiftCallback.class);
        
        shouldLogin () ;
    }

    public void shouldLogin() {
        ops.login(accConf, "http://localhost:8080/", "user", "pass", "secret", callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onLoginSuccess();
        account = ((SwiftOperationsImpl)ops).getAccount() ;
    }
    
    
    @Test
    public void shouldAddAndListContainer() {
    	    
    	@SuppressWarnings({ "unchecked", "rawtypes" })
		ArgumentCaptor<List<Container> > argument = ArgumentCaptor.forClass((Class) List.class);
    	
    	//First refresh the empty account
    	ops.refreshContainers(callback) ;
    	
    	// Then add a new container
    	ops.createContainer(new ContainerSpecification("x", true), callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onUpdateContainers(argument.capture()); 
        
        // Then verify that the new container has been added.
        assertFalse(argument.getValue().isEmpty());
        assertFalse(account.getContainer("x").isPublic());
    }
    

    @Test
    public void shouldLogout() {
        ops.logout(callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onLogoutSuccess();
    }

    @Test
    public void shouldCreateContainer() {
        ops.createContainer(new ContainerSpecification("x", true), callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onUpdateContainers(Mockito.anyListOf(Container.class));
        assertFalse(account.getContainer("x").isPublic());
    }

    @Test
    public void shouldNotCreateContainer() {
        ops.createContainer(null, callback);
        Mockito.verify(callback, Mockito.never()).onUpdateContainers(Mockito.anyListOf(Container.class));
    }

    @Test
    public void shouldNotCreateContainerTwice() {
        ops.createContainer(new ContainerSpecification("x", true), callback);
        ops.createContainer(new ContainerSpecification("x", true), callback);
        Mockito.verify(callback, Mockito.atMost(2)).onUpdateContainers(Mockito.anyListOf(Container.class));
    }

    @Test
    public void shouldCreateContainerPublic() {
        ops.createContainer(new ContainerSpecification("x", false), callback);
        Mockito.verify(callback, Mockito.atMost(1)).onUpdateContainers(Mockito.anyListOf(Container.class));
        assertTrue(account.getContainer("x").isPublic());
    }

    @Test
    public void shouldCreateStoredObject() {
    	try
    	{	
    		ops.createStoredObjects(account.getContainer("x").create(), new File[] { new File("pom.xml") }, callback);
		}
		catch (IOException e)
		{
			logger.error("Error occurred while creating stored object", e) ;
		}
    
        Mockito.verify(callback, Mockito.atLeastOnce()).onNewStoredObjects();
        Mockito.verify(callback, Mockito.atLeastOnce()).onAppendStoredObjects(Mockito.any(Container.class), Mockito.eq(0),
                Mockito.anyListOf(StoredObject.class));
    }

    @Test
    public void shouldDeleteContainer() {
        ops.deleteContainer(account.getContainer("x").create(), callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onUpdateContainers(Mockito.anyListOf(Container.class));
    }

    @Test
    public void shouldDeleteStoredObject() {
        Container create = account.getContainer("x").create();
        StoredObject object = create.getObject("y");
        object.uploadObject(new byte[0]);
        ops.deleteStoredObjects(create, Collections.singletonList(object), callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onStoredObjectDeleted(Mockito.any(Container.class), Mockito.any(StoredObject.class));
    }

    @Test
    public void shouldDownloadStoredObject() {
        Container create = account.getContainer("x").create();
        StoredObject object = create.getObject("y");
        object.uploadObject(new byte[8192]);
        File target = new File("./target/downloadTest.dat");
        boolean exceptionThrown = false ;
        try
        {
        	ops.downloadStoredObject(create, object, target, callback);
        }
        catch (IOException e) 
        {
        	exceptionThrown = true ;
		}
        finally
        {
	        assertTrue(target.exists());
	        target.delete();
	        assertFalse(exceptionThrown);
        }
    }

    @Test
    public void shouldEmptyContainer() {
        Container create = account.getContainer("x").create();
        StoredObject object = create.getObject("y");
        object.uploadObject(new byte[8192]);
        //
        ops.emptyContainer(create, callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onNewStoredObjects();
    }

    @Test
    public void shouldGetMetadata() {
        Container create = account.getContainer("x").create();
        StoredObject object = create.getObject("y");
        object.uploadObject(new byte[8192]);
        //
        ops.getMetadata(create, callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onContainerUpdate(create);
        //
        ops.getMetadata(object, callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onStoredObjectUpdate(object);
    }

    @Test
    public void shouldPurgeContainer() {
        Container create = account.getContainer("x").create();
        StoredObject object = create.getObject("y");
        object.uploadObject(new byte[8192]);
        //
        ops.purgeContainer(create, callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onUpdateContainers(Mockito.anyListOf(Container.class));
        assertFalse(create.exists());
    }

    @Test
    public void shouldRefreshContainers() {
        ops.refreshContainers(callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onUpdateContainers(Mockito.anyListOf(Container.class));
    }

    @Test
    public void shouldRefreshStoredObjects() {
        Container create = account.getContainer("x").create();
        ops.refreshStoredObjects(create, callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onNewStoredObjects();
    }
    
    
    @Test(expected = AssertionError.class)
    public void shouldNotEndWithSegments() {
        ops.createContainer(new ContainerSpecification("name" + SwiftUtils.segmentsContainerPostfix, true), callback);
    }
    
    
    private SwiftOperations getCustomSegmentationSizeSwiftOperations (long segmentSize)
    {
    	SwiftOperations ops = new SwiftOperationsImpl();
        ops.login(accConf, segmentSize, "http://localhost:8080/", "user", "pass", "secret", callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onLoginSuccess();
        return ops ;
    }
    
    
    @Test
    public void shouldCreateSegments() {
    	
    	final long segmentSize = 800 ;
    	final long fileSize = 8192 ;
    	final String fileName = "segmentationTest.dat" ;
    	
		final long numSegments = TestUtils.getNumberOfSegments (fileSize, segmentSize) ;
    	
    	SwiftOperations ops = getCustomSegmentationSizeSwiftOperations (segmentSize) ;
        Account acc = ((SwiftOperationsImpl)ops).getAccount() ;
    	
        Container container = acc.getContainer("x").create();
        ops.refreshStoredObjects(container, callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onNewStoredObjects();
        
        File file = null ;
        try
        {         
        	file = TestUtils.getTestFile (tmpFolder, fileName, fileSize) ;
        	
        	// upload it
        	ops.uploadFiles(container, null, new File[] {file}, true, callback);
  
        	// verify that the segments container has been created 
        	Container containerSegments = acc.getContainer(container.getName() + SwiftUtils.segmentsContainerPostfix);
        	assertTrue (containerSegments.exists()) ;
        	
        	// verify the number of segments
        	assertTrue (containerSegments.getCount() == numSegments) ;
        	
        	// check the object
        	StoredObject object = container.getObject(fileName);
        	assertTrue (object.exists()) ;
        }
        catch (IOException e) 
        {
        	logger.error ("Error occurred in shouldCreateSegments", e) ;
        	assertFalse(true);
		}
    }
    
    
    @Test
    public void shouldDownloadSegmentedObject() {
    	
    	final long segmentSize = 800 ;
    	final long fileSize = 8192 ;
    	final String fileName = "segmentationTest.dat" ;
    	
    	SwiftOperations ops = getCustomSegmentationSizeSwiftOperations (segmentSize) ;
        Account acc = ((SwiftOperationsImpl)ops).getAccount() ;
    	
        Container container = acc.getContainer("x").create();
        ops.refreshStoredObjects(container, callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onNewStoredObjects();

        File file = null ;
        File target = new File("./target/downloadTest.dat");
        boolean exceptionThrown = false ;
        try
        {        
        	// get test file
        	file = TestUtils.getTestFile (tmpFolder, fileName, fileSize) ;
        	      
        	// upload it
        	ops.uploadFiles(container, null, new File[] {file}, true, callback);
        	
        	// check the object
        	StoredObject object = container.getObject(fileName);
        	assertTrue (object.exists()) ;

        	// download object
        	ops.downloadStoredObject(container, object, target, callback);
        	assertTrue(target.exists());
        	
        	// verify that the files are identical
        	assertTrue(FileUtils.getMD5(file).equals(FileUtils.getMD5(target))) ;
        }
        catch (IOException e) 
        {
        	logger.error ("Error occurred in shouldDownloadSegmentedObject", e) ;
        	exceptionThrown = true ;
		}
        finally
        {
	        target.delete() ;
	        assertFalse(exceptionThrown);
        }	
    }
    
    
    @Test
    public void shouldDeleteSegmentedObject() {
    	
    	final long segmentSize = 800 ;
    	final long fileSize = 8192 ;
    	final String fileName = "segmentationTest.dat" ;
    	
    	final long numSegments = TestUtils.getNumberOfSegments (fileSize, segmentSize) ;
    	
    	SwiftOperations ops = getCustomSegmentationSizeSwiftOperations (segmentSize) ;
        Account acc = ((SwiftOperationsImpl)ops).getAccount() ;
    	
        Container container = acc.getContainer("x").create();
        ops.refreshStoredObjects(container, callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onNewStoredObjects();

        File file = null ;
        try
        {        
        	file = TestUtils.getTestFile (tmpFolder, fileName, fileSize) ;
        	
        	// upload it
        	ops.uploadFiles(container, null, new File[] {file}, true, callback);
  
        	// verify that the segments container has been created 
        	Container containerSegments = acc.getContainer(container.getName() + SwiftUtils.segmentsContainerPostfix);
        	assertTrue (containerSegments.exists()) ;
        	
        	// verify the number of segments
        	assertTrue (containerSegments.getCount() == numSegments) ;
        	
        	// check the object
        	StoredObject object = container.getObject(fileName);
        	assertTrue (object.exists()) ;
        	
        	// delete the object
        	ops.deleteStoredObjects(container, Arrays.asList(object), callback) ;
        	Mockito.verify(callback, Mockito.times(1)).onStoredObjectDeleted(container, object);
        	
        	// verify the number of segments is now zero
        	containerSegments.reload() ;
        	assertTrue (containerSegments.getCount() == 0) ;
        }
        catch (IOException e) 
        {
        	logger.error ("Error occurred in shouldDeleteSegmentedObject", e) ;
        	assertFalse(true) ;
		}	
    }
    
    
    @Test
    public void shouldRefreshDirectoriesOrStoredObjectsWithoutParent() 
    {
        try
        {  
	        Container container = account.getContainer("x").create() ;
	        
	        final String directoryName = "directory" ;

	        File folder = TestUtils.getTestDirectoryWithFiles (tmpFolder, directoryName, "file", 10) ;
	        ops.uploadDirectory(container, null, folder, true, callback);
	        
	        // verify that the uploaded files are correctly listed
	    	@SuppressWarnings({ "unchecked", "rawtypes" })
			ArgumentCaptor<Collection<StoredObject> > argument = ArgumentCaptor.forClass((Class) Collection.class);
	    
	        ops.refreshDirectoriesOrStoredObjects(container, null, callback);
	        
	        // one time when uploading the directory, one time when calling refreshDirectoriesOrStoredObjects
	        Mockito.verify(callback, Mockito.times(2)).onNewStoredObjects();
	        Mockito.verify(callback, Mockito.atLeastOnce()).onAppendStoredObjects(Mockito.any(Container.class), Mockito.anyInt(), argument.capture());

	        // this collection should have only one directory, whose name is directoryName
	        assertTrue(argument.getValue().size() == 1) ;
	        StoredObject obj = argument.getValue().iterator().next() ;
	        assertTrue (obj.getBareName().equals(directoryName)) ;
        }
        catch (IOException e) 
        {
        	logger.error ("Error occurred in shouldRefreshDirectoriesOrStoredObjectsWithoutParent", e) ;
        	assertFalse(true) ;
		}	
    }
    
    
    @Test
    public void shouldRefreshEmptyDirectoriesOrStoredObjectsWithParent() 
    {
        try
        {  
	        Container container = account.getContainer("x").create() ;
	        
	        final String directoryName = "directory" ;

	        File folder = TestUtils.getTestDirectoryWithFiles (tmpFolder, directoryName, "", 0) ;
 
	        ops.uploadDirectory(container, null, folder, true, callback);

	        // verify that the uploaded files are correctly listed
	    	Directory parent = new Directory (folder.getName(), SwiftUtils.separator.charAt(0)) ;
	        ops.refreshDirectoriesOrStoredObjects(container, parent, callback);

	        // onAppendStoredObjects is called one time when calling uploadDirectory, but should not be called again when calling refreshDirectoriesOrStoredObjects
	        Mockito.verify(callback, Mockito.times(1)).onAppendStoredObjects(Mockito.any(Container.class), Mockito.anyInt(), Mockito.anyCollectionOf(StoredObject.class));
	        //Mockito.verify(callback, Mockito.never()).onAppendStoredObjects(Mockito.any(Container.class), Mockito.anyInt(), Mockito.anyCollectionOf(StoredObject.class));
        }
        catch (IOException e) 
        {
        	logger.error ("Error occurred in shouldRefreshEmptyDirectoriesOrStoredObjectsWithParent", e) ;
        	assertFalse(true) ;
		}	
    }
    
    
    @Test
    public void shouldRefreshDirectoriesOrStoredObjectsWithParent() 
    {
        try
        {  
	        Container container = account.getContainer("x").create() ;
	        
	        final String directoryName = "directory" ;
	        final String subDirectoryName = "subdirectory" ;
	        
	        final int numberOfFiles = 10 ;

	        File folder = TestUtils.getTestDirectoryWithFiles (tmpFolder, directoryName, "file", numberOfFiles) ;
	        File subfolder = TestUtils.getTestDirectoryWithFiles (tmpFolder, subDirectoryName, "fileInSubfolder", numberOfFiles) ;	        
	        Files.move(Paths.get(subfolder.getPath()), Files.createDirectories(Paths.get(folder.getPath() + File.separator + subDirectoryName)), StandardCopyOption.REPLACE_EXISTING) ;
	        
	        ops.uploadDirectory(container, null, folder, true, callback);
	        
	        // verify that the uploaded files are correctly listed
	    	@SuppressWarnings({ "unchecked", "rawtypes" })
			ArgumentCaptor<Collection<StoredObject> > argument = ArgumentCaptor.forClass((Class) Collection.class);
	    
	    	Directory parent = new Directory (folder.getName(), SwiftUtils.separator.charAt(0)) ;
	        ops.refreshDirectoriesOrStoredObjects(container, parent, callback);

	        Mockito.verify(callback, Mockito.atLeastOnce()).onAppendStoredObjects(Mockito.any(Container.class), Mockito.anyInt(), argument.capture());
	        
	        // this collection should have numberOfFiles + 1 files
	        assertTrue(argument.getValue().size() == numberOfFiles + 1) ;
        }
        catch (IOException e) 
        {
        	logger.error ("Error occurred in shouldRefreshDirectoriesOrStoredObjectsWithParent", e) ;
        	assertFalse(true) ;
		}
    }
    
    
    private void uploadAndOverwrite (boolean overwrite) throws IOException
    {
    	final String objName = "testObject.dat" ;
    	
        Container container = account.getContainer("x").create();
        StoredObject object = container.getObject(objName);
        object.uploadObject(TestUtils.getTestFile(tmpFolder, objName, 4096));

        assertTrue (object.exists()) ;
        String md5 = object.getEtag() ;
        
        // upload a new file with the same name, the overwrite flag is set to overwrite
        ops.uploadFiles(container, null, new File [] {TestUtils.getTestFile(tmpFolder, objName, 4096)}, overwrite, callback);
        
        StoredObject newObject = container.getObject(objName);
        assertTrue (newObject.exists()) ;
        
        // we assume no collision ;-)
        assertTrue (md5.equals(newObject.getEtag()) == !overwrite) ;
    }
    
    
    @Test
    public void shouldNotOverwiteObject () 
    {
        try
        {  
        	uploadAndOverwrite (false) ;
        }
        catch (IOException e) 
        {
        	logger.error ("Error occurred in shouldNotOverwiteObject", e) ;
        	assertFalse(true) ;
		}
    }
    
    
    @Test
    public void shouldOverwiteObject () 
    {
        try
        {  
        	uploadAndOverwrite (true) ;
        }
        catch (IOException e) 
        {
        	logger.error ("Error occurred in shouldOverwiteObject", e) ;
        	assertFalse(true) ;
		}
    }
}
