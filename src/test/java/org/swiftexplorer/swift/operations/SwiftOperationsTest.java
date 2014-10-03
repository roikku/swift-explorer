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
import org.swiftexplorer.config.swift.SwiftParameters;
import org.swiftexplorer.gui.util.SwiftOperationStopRequesterImpl;
import org.swiftexplorer.swift.client.factory.AccountConfigFactory;
import org.swiftexplorer.swift.operations.SwiftOperations.ComparisonItem;
import org.swiftexplorer.swift.operations.SwiftOperations.ResultCallback;
import org.swiftexplorer.swift.operations.SwiftOperations.SwiftCallback;
import org.swiftexplorer.swift.util.SwiftUtils;
import org.swiftexplorer.util.FileUtils;
import org.swiftexplorer.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.instructions.UploadInstructions;
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
    private SwiftOperationStopRequesterImpl stopRequester = new SwiftOperationStopRequesterImpl () ;

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
    		ops.createStoredObjects(account.getContainer("x").create(), new File[] { new File("pom.xml") }, stopRequester, callback);
		}
		catch (IOException e)
		{
			logger.error("Error occurred while creating stored object", e) ;
		}
    
        //Mockito.verify(callback, Mockito.atLeastOnce()).onNewStoredObjects();
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
        ops.deleteStoredObjects(create, Collections.singletonList(object), stopRequester, callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onStoredObjectDeleted(Mockito.any(Container.class), Mockito.any(StoredObject.class));
    }
    
    
    @Test
    public void shouldUploadStoredObject() throws IOException 
    {
    	Container container = account.getContainer("x").create();
        
    	final String dir = "dir" ; 
    	final String name = "file.dat" ; 
    	
        StoredObject object = container.getObject(dir);
        object.uploadObject(new UploadInstructions (new byte[] {}).setContentType(SwiftUtils.directoryContentType)) ;
		  
        File file = TestUtils.getTestFile(tmpFolder, name, 4096) ; 
        
    	ops.uploadFiles(container, object, new File [] {file}, true, stopRequester, callback);
    	
        //Mockito.verify(callback, Mockito.atLeastOnce()).onNewStoredObjects();
        Mockito.verify(callback, Mockito.atLeastOnce()).onAppendStoredObjects(Mockito.any(Container.class), Mockito.eq(0),
                Mockito.anyListOf(StoredObject.class));
        
        StoredObject upObj = container.getObject(dir + SwiftUtils.separator + name) ;
        assertTrue (upObj.exists());
        assertTrue (upObj.getEtag().equals(FileUtils.getMD5(file))) ;
    }
    
    
    @Test
    public void shouldUploadStoredObjectCollection() throws IOException 
    {
        Container container = account.getContainer("x").create();
        
        StoredObject object1 = container.getObject("object1");
        object1.uploadObject(TestUtils.getTestFile(tmpFolder, "src1", 8192));
        assertTrue (object1.exists()) ;
        
        StoredObject object2 = container.getObject("dir" + SwiftUtils.separator + "object2");
        assertFalse (object2.exists()) ;
        
        File file1 = TestUtils.getTestFile(tmpFolder, "file1", 4096) ;
        File file2 = TestUtils.getTestFile(tmpFolder, "file2", 4096) ;
        
        List<Pair<? extends StoredObject, ? extends File> > pairObjectFile = new ArrayList<> () ;
        pairObjectFile.add(Pair.newPair(object1, file1)) ;
        pairObjectFile.add(Pair.newPair(object2, file2)) ;
        
    	ops.uploadFiles(container, pairObjectFile, true, stopRequester, callback);

        //Mockito.verify(callback, Mockito.atLeastOnce()).onNewStoredObjects();
        Mockito.verify(callback, Mockito.atLeastOnce()).onAppendStoredObjects(Mockito.any(Container.class), Mockito.eq(0),
                Mockito.anyListOf(StoredObject.class));
    	
        assertTrue (object2.exists()) ;
        
    	assertTrue(FileUtils.getMD5(file1).equals(object1.getEtag())) ;
    	assertTrue(FileUtils.getMD5(file2).equals(object2.getEtag())) ;
    }
    
    
    @Test
    public void shouldNotUploadStoredObjectCollection() throws IOException 
    {
        Container container = account.getContainer("x").create();
        
        StoredObject object1 = container.getObject("object1");
        object1.uploadObject(TestUtils.getTestFile(tmpFolder, "src1", 8192));
        assertTrue (object1.exists()) ;
        
        StoredObject object2 = container.getObject("dir" + SwiftUtils.separator + "object2");
        object2.uploadObject(TestUtils.getTestFile(tmpFolder, "src1", 8192));
        assertTrue (object2.exists()) ;
        
        String etag1 = object1.getEtag() ;
        String etag2 = object2.getEtag() ;
        
        File file1 = TestUtils.getTestFile(tmpFolder, "file1", 4096) ;
        File file2 = TestUtils.getTestFile(tmpFolder, "file2", 4096) ;
        
        String hash1 = FileUtils.getMD5(file1) ;
        String hash2 = FileUtils.getMD5(file2) ;
        
        assertFalse (hash1.equals(etag1)) ;
        assertFalse (hash2.equals(etag2)) ;
        
        List<Pair<? extends StoredObject, ? extends File> > pairObjectFile = new ArrayList<> () ;
        pairObjectFile.add(Pair.newPair(object1, file1)) ;
        pairObjectFile.add(Pair.newPair(object2, file2)) ;
        
        boolean overwrite = false ;
    	ops.uploadFiles(container, pairObjectFile, overwrite, stopRequester, callback);
    	
    	assertTrue (etag1.equals(object1.getEtag())) ;
    	assertTrue (etag2.equals(object2.getEtag())) ;
    }
    
    
    @Test
    public void shouldStopUploadingStoredObjectCollection() throws IOException 
    {
        Container container = account.getContainer("x").create();
        
        StoredObject object1 = container.getObject("obj1");
        assertFalse (object1.exists()) ;
        
        StoredObject object2 = container.getObject("obj2");
        assertFalse (object2.exists()) ;
        
        File target1 = TestUtils.getTestFile(tmpFolder, "file1", 4096) ;
        File target2 = TestUtils.getTestFile(tmpFolder, "file2", 4096) ;
    	assertTrue(target1.exists());
    	assertTrue(target2.exists());
        
        List<Pair<? extends StoredObject, ? extends File> > pairObjectFile = new ArrayList<> () ;
        pairObjectFile.add(Pair.newPair(object1, target1)) ;
        pairObjectFile.add(Pair.newPair(object2, target2)) ;
        
    	SwiftOperationStopRequesterImpl stopReq = new SwiftOperationStopRequesterImpl () ;
    	stopReq.stop();
    	
    	ops.downloadStoredObject(container, pairObjectFile, false, stopReq, callback);

    	Mockito.verify(callback, Mockito.times(1)).onStopped();
    	
    	assertFalse(object1.exists());
    	assertFalse(object2.exists());
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
        	ops.downloadStoredObject(create, object, target, stopRequester, callback);
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
    public void shouldDownloadStoredObjectCollection() throws IOException 
    {
        Container container = account.getContainer("x").create();
        
        StoredObject object1 = container.getObject("sr1");
        object1.uploadObject(TestUtils.getTestFile(tmpFolder, "src1", 8192));
        assertTrue (object1.exists()) ;
        
        StoredObject object2 = container.getObject("sr2");
        object2.uploadObject(TestUtils.getTestFile(tmpFolder, "src2", 8192));
        assertTrue (object2.exists()) ;
        
        File destFolder = tmpFolder.newFolder() ;
        File target1 = new File (destFolder.getPath() + File.separator + "target1.dat") ;
        File target2 = new File (destFolder.getPath() + File.separator + "target2.dat") ;
    	assertFalse(target1.exists());
    	assertFalse(target2.exists());
    	
        List<Pair<? extends StoredObject, ? extends File> > pairObjectFile = new ArrayList<> () ;
        pairObjectFile.add(Pair.newPair(object1, target1)) ;
        pairObjectFile.add(Pair.newPair(object2, target2)) ;
        
    	ops.downloadStoredObject(container, pairObjectFile, false, stopRequester, callback);
    	assertTrue(target1.exists());
    	assertTrue(target2.exists());
    	
    	assertTrue(FileUtils.getMD5(target1).equals(object1.getEtag())) ;
    	assertTrue(FileUtils.getMD5(target2).equals(object2.getEtag())) ;
    }
    
    
    @Test
    public void shouldStopDownloadingStoredObjectCollection() throws IOException 
    {
        Container container = account.getContainer("x").create();
        
        StoredObject object1 = container.getObject("sr1");
        object1.uploadObject(TestUtils.getTestFile(tmpFolder, "src1", 8192));
        assertTrue (object1.exists()) ;
        
        StoredObject object2 = container.getObject("sr2");
        object2.uploadObject(TestUtils.getTestFile(tmpFolder, "src2", 8192));
        assertTrue (object2.exists()) ;
        
        File destFolder = tmpFolder.newFolder() ;
        File target1 = new File (destFolder.getPath() + File.separator + "target1.dat") ;
        File target2 = new File (destFolder.getPath() + File.separator + "target2.dat") ;
    	assertFalse(target1.exists());
    	assertFalse(target2.exists());
        
        List<Pair<? extends StoredObject, ? extends File> > pairObjectFile = new ArrayList<> () ;
        pairObjectFile.add(Pair.newPair(object1, target1)) ;
        pairObjectFile.add(Pair.newPair(object2, target2)) ;
        
    	SwiftOperationStopRequesterImpl stopReq = new SwiftOperationStopRequesterImpl () ;
    	stopReq.stop();
    	
    	ops.downloadStoredObject(container, pairObjectFile, false, stopReq, callback);

    	Mockito.verify(callback, Mockito.times(1)).onStopped();
    	
    	assertFalse(target1.exists());
    	assertFalse(target2.exists());
    }
    
    
    @Test
    public void shouldNotDownloadStoredObjectCollection() throws IOException 
    {
        Container container = account.getContainer("x").create();
        
        StoredObject object1 = container.getObject("sr1");
        object1.uploadObject(TestUtils.getTestFile(tmpFolder, "src1", 8192));
        assertTrue (object1.exists()) ;
        
        StoredObject object2 = container.getObject("sr2");
        object2.uploadObject(TestUtils.getTestFile(tmpFolder, "src2", 8192));
        assertTrue (object2.exists()) ;
        
        File target1 = TestUtils.getTestFile(tmpFolder, "target1", 128) ;
        File target2 = TestUtils.getTestFile(tmpFolder, "target2", 128) ;
    	assertTrue(target1.exists());
    	assertTrue(target2.exists());
        
    	String hash1 = FileUtils.getMD5(target1) ;
    	String hash2 = FileUtils.getMD5(target2) ;
    	
    	assertFalse (hash1.equals(object1.getEtag())) ;
    	assertFalse (hash2.equals(object2.getEtag())) ;
 
        List<Pair<? extends StoredObject, ? extends File> > pairObjectFile = new ArrayList<> () ;
        pairObjectFile.add(Pair.newPair(object1, target1)) ;
        pairObjectFile.add(Pair.newPair(object2, target2)) ;
        
        boolean overwrite = false ;
    	ops.downloadStoredObject(container, pairObjectFile, overwrite, stopRequester, callback);

    	assertTrue (hash1.equals(FileUtils.getMD5(target1))) ;
    	assertTrue (hash2.equals(FileUtils.getMD5(target2))) ;
    }
    

    @Test
    public void shouldEmptyContainer() {
        Container create = account.getContainer("x").create();
        StoredObject object = create.getObject("y");
        object.uploadObject(new byte[8192]);
        //
        ops.emptyContainer(create, stopRequester, callback);
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
        ops.purgeContainer(create, stopRequester, callback);
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
    	
    	// We need to mockup SwiftParameters, because the builder constrains the minimum size
    	//SwiftParameters param = new SwiftParameters.Builder(segmentSize, true).build() ;
    	SwiftParameters param = Mockito.mock(SwiftParameters.class);
    	Mockito.when(param.getSegmentationSize()).thenReturn(segmentSize) ;
    	
        ops.login(accConf, param, "http://localhost:8080/", "user", "pass", "secret", callback);
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
        	ops.uploadFiles(container, null, new File[] {file}, true, stopRequester, callback);
  
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
        	ops.uploadFiles(container, null, new File[] {file}, true, stopRequester, callback);
        	
        	// check the object
        	StoredObject object = container.getObject(fileName);
        	assertTrue (object.exists()) ;

        	// download object
        	ops.downloadStoredObject(container, object, target, stopRequester, callback);
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
        	ops.uploadFiles(container, null, new File[] {file}, true, stopRequester, callback);
        	
        	// verify that the segments container has been created 
        	Container containerSegments = acc.getContainer(container.getName() + SwiftUtils.segmentsContainerPostfix);
        	assertTrue (containerSegments.exists()) ;
        	
        	// verify the number of segments
        	assertTrue (containerSegments.getCount() == numSegments) ;
        	
        	// check the object
        	StoredObject object = container.getObject(fileName);
        	assertTrue (object.exists()) ;

        	// delete the object
        	ops.deleteStoredObjects(container, Arrays.asList(object), stopRequester, callback) ;
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
	        ops.uploadDirectory(container, null, folder, true, stopRequester, callback);
	        
	        // verify that the uploaded files are correctly listed
	    	@SuppressWarnings({ "unchecked", "rawtypes" })
			ArgumentCaptor<Collection<StoredObject> > argument = ArgumentCaptor.forClass((Class) Collection.class);
	    
	        ops.refreshDirectoriesOrStoredObjects(container, null, callback);
	        
	        Mockito.verify(callback, Mockito.atLeastOnce()).onNewStoredObjects();
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
 
	        ops.uploadDirectory(container, null, folder, true, stopRequester, callback);

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
	        
	        ops.uploadDirectory(container, null, folder, true, stopRequester, callback);
	        
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
        ops.uploadFiles(container, null, new File [] {TestUtils.getTestFile(tmpFolder, objName, 4096)}, overwrite, stopRequester, callback);
        
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
    
    
    @Test
    public void shouldStopUploadingFiles ()
    {
    	final String objName = "testObject.dat" ;
        Container container = account.getContainer("x").create();
        try 
        {
        	SwiftOperationStopRequesterImpl stopReq = new SwiftOperationStopRequesterImpl () ;
        	stopReq.stop();
			ops.uploadFiles(container, null, new File [] {TestUtils.getTestFile(tmpFolder, objName, 4096)}, false, stopReq, callback);
			Mockito.verify(callback, Mockito.times(1)).onStopped();
		} 
        catch (IOException e) 
		{
        	logger.error ("Error occurred in shouldStopUploadingFiles", e) ;
        	assertFalse(true) ;
		}  
    }
    
    
    @Test
    public void shouldStopDeletingDirectory ()
    {
    	final String dirName = "dir" ;
        Container container = account.getContainer("x").create();
        
    	ops.createDirectory(container, null, dirName, callback);
    	
    	StoredObject obj = container.getObject(dirName) ;
    	assertTrue (obj.exists()) ;
    	
    	SwiftOperationStopRequesterImpl stopReq = new SwiftOperationStopRequesterImpl () ;
    	stopReq.stop();
		ops.deleteDirectory(container, obj, stopReq, callback);
		Mockito.verify(callback, Mockito.times(1)).onStopped();
    }
    
    
    @Test
    public void shouldStopUploadingDirectory ()
    {
    	final String directoryName = "directory" ;
        Container container = account.getContainer("x").create();
        try 
        {
        	SwiftOperationStopRequesterImpl stopReq = new SwiftOperationStopRequesterImpl () ;
        	stopReq.stop();
        	
	        File folder = TestUtils.getTestDirectoryWithFiles (tmpFolder, directoryName, "file", 10) ;
	        ops.uploadDirectory(container, null, folder, true, stopReq, callback);     
       		Mockito.verify(callback, Mockito.times(1)).onStopped();
		} 
        catch (IOException e) 
		{
        	logger.error ("Error occurred in shouldStopUploadingDirectory", e) ;
        	assertFalse(true) ;
		}  
    }
    
    
    @Test
    public void shouldStopDownloadingStoredObject ()
    {
        Container container = account.getContainer("x").create();
        StoredObject object = container.getObject("y");
        object.uploadObject(new byte[100]);
        
    	SwiftOperationStopRequesterImpl stopReq = new SwiftOperationStopRequesterImpl () ;
    	stopReq.stop();
    	
    	try 
    	{
			ops.downloadStoredObject(container, object, tmpFolder.newFile(), stopReq, callback);
			Mockito.verify(callback, Mockito.times(1)).onStopped();
		} 
    	catch (IOException e) 
    	{
        	logger.error ("Error occurred in shouldStopDownloadingStoredObject", e) ;
        	assertFalse(true) ;
		}	
    }
    
    
    @Test
    public void shouldStopDownloadingDirectory ()
    {
    	final String directoryName = "directory" ;
    	
        Container container = account.getContainer("x").create();       
        
        ops.createDirectory(container, null, directoryName, callback);
        StoredObject objectDir = container.getObject(directoryName);
        assertTrue (objectDir.exists()) ;
        
        StoredObject object = container.getObject(directoryName + SwiftUtils.separator + "y");
        object.uploadObject(new byte[100]);
        assertTrue (object.exists()) ;
        
    	SwiftOperationStopRequesterImpl stopReq = new SwiftOperationStopRequesterImpl () ;
    	stopReq.stop();
    	
    	try 
    	{	
			ops.downloadStoredObject(container, objectDir, tmpFolder.newFolder(), stopReq, callback);
			Mockito.verify(callback, Mockito.times(1)).onStopped();
		} 
    	catch (IOException e) 
    	{
        	logger.error ("Error occurred in shouldStopDownloadingDirectory", e) ;
        	assertFalse(true) ;
		}	
    }
    
    
    @Test(expected = AssertionError.class)
    public void shouldNotDownloadStoredObject() 
    {
    	final String directoryName = "directory" ;
        Container container = account.getContainer("x").create();  
        
        ops.createDirectory(container, null, directoryName, callback);
        StoredObject objectDir = container.getObject(directoryName);
        assertTrue (objectDir.exists()) ;
        
        try 
        {
			ops.downloadStoredObject(container, objectDir, tmpFolder.newFile(), null, callback);
		} 
        catch (IOException e) 
		{
        	logger.error ("Error occurred in shouldNotDownloadStoredObject", e) ;
        	assertFalse(true) ;
		}
    }
    
    
    @Test
    public void shouldStopPurgingContainer ()
    {
    	Container container = account.getContainer("x").create();
        StoredObject object = container.getObject("y");
        object.uploadObject(new byte[100]);
        
    	SwiftOperationStopRequesterImpl stopReq = new SwiftOperationStopRequesterImpl () ;
    	stopReq.stop();
    	ops.purgeContainer(container, stopReq, callback);
    	Mockito.verify(callback, Mockito.times(1)).onStopped();
    }
    
    
    @Test
    public void shouldStopEmptyingContainer ()
    {
    	Container container = account.getContainer("x").create();
        StoredObject object = container.getObject("y");
        object.uploadObject(new byte[100]);
        
    	SwiftOperationStopRequesterImpl stopReq = new SwiftOperationStopRequesterImpl () ;
    	stopReq.stop();
    	ops.emptyContainer(container, stopReq, callback);
    	Mockito.verify(callback, Mockito.times(1)).onStopped();
    }
    
    
	@Test
    public void shouldStopFindingDifferences ()
    {
        Container container = account.getContainer("x").create() ;
        final String directoryName = "directory" ;
		try 
		{
	        File folder = TestUtils.getTestDirectoryWithFiles (tmpFolder, directoryName, "file", 10) ;
	        ops.uploadDirectory(container, null, folder, true, stopRequester, callback);
	    	
	        StoredObject remote = container.getObject(directoryName) ;
	        assertTrue (remote.exists ()) ;
	        
	    	SwiftOperationStopRequesterImpl stopReq = new SwiftOperationStopRequesterImpl () ;
	    	stopReq.stop();
	    	
	    	@SuppressWarnings("unchecked")
	    	ResultCallback<Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > > resultCallback 
	    	 	= (ResultCallback<Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > >)Mockito.mock(ResultCallback.class) ;
	    	
			ops.findDifferences(container, remote, folder, resultCallback, stopReq, callback);
			
			Mockito.verify(callback, Mockito.times(1)).onStopped();
		} 
		catch (IOException e) {
        	logger.error ("Error occurred in shouldStopFindingDifferences", e) ;
        	assertFalse(true) ;
		}
    }
    
	
    enum FindFilesDifferencesTestScenario
    {
    	MISSING_LOCAL,
    	MISSING_REMOTE,
    	MODIFIED_FILE,
    	NO_DIFF,
    }
    
    
    public void shouldFindFilesDifferences (FindFilesDifferencesTestScenario scenario)
    {
    	Container container = account.getContainer("x").create() ;
    	final String fileName = "myFile" ;
		try 
		{
			File local = TestUtils.getTestFile(tmpFolder, fileName, 100) ;
			ops.uploadFiles(container, null, new File[]{local} , true, stopRequester, callback);
			StoredObject remote = container.getObject(fileName) ;
			assertTrue (remote.exists()) ;
			
			switch (scenario)
			{
			case MISSING_LOCAL:
				{
					assertTrue (local.delete()) ;
				}
				break;
			case MISSING_REMOTE:
				{
					remote.delete();
					assertFalse (remote.exists()) ;
				}
				break ;
			case MODIFIED_FILE:
				{
					// we modify the file
			    	FileOutputStream out = new FileOutputStream(local);
			      	out.write(new byte[]{'a', 'b', 'c', 'd', 'e'});
			    	out.close();
				}
				break;
			case NO_DIFF:
				break;
	        default:
	        	throw new IllegalStateException () ;
			}

	    	@SuppressWarnings("unchecked")
	    	ResultCallback<Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > > resultCallback 
	    	 	= (ResultCallback<Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > >)Mockito.mock(ResultCallback.class) ;
	    	
	    	@SuppressWarnings({ "unchecked", "rawtypes" })
			ArgumentCaptor<Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > > argument = ArgumentCaptor.forClass((Class) ResultCallback.class);
	    	
	    	ops.findDifferences(container, remote, local, resultCallback, stopRequester, callback);
	    	
	    	Mockito.verify(resultCallback, Mockito.times(1)).onResult(argument.capture());
	    	
	    	assertTrue ((scenario == FindFilesDifferencesTestScenario.NO_DIFF) ? (argument.getValue().isEmpty()) : (!argument.getValue().isEmpty())) ;
		} 
		catch (IOException e) {
        	logger.error ("Error occurred in shouldFindFilesDifferences", e) ;
        	assertFalse(true) ;
		}
    }
    
    
    @Test
    public void shouldFindNoFilesDifferences ()
    {
    	shouldFindFilesDifferences (FindFilesDifferencesTestScenario.NO_DIFF) ;
    }
    
    
    @Test
    public void shouldFindFilesDifferencesModifiedFile ()
    {
    	shouldFindFilesDifferences (FindFilesDifferencesTestScenario.MODIFIED_FILE) ;
    }
    
    
    @Test
    public void shouldFindFilesDifferencesMissingLocal ()
    {
    	shouldFindFilesDifferences (FindFilesDifferencesTestScenario.MISSING_LOCAL) ;
    }
    
    
    @Test
    public void shouldFindFilesDifferencesMissingRemote ()
    {
    	shouldFindFilesDifferences (FindFilesDifferencesTestScenario.MISSING_REMOTE) ;
    }
    
    
    enum FindDirectoriesDifferencesTestScenario
    {
    	NEW_FILE,
    	MISSING_FILE,
    	MODIFIED_FILE,
    	NO_DIFF,
    }
           
    
    private void shouldFindDirectoriesDifferences (FindDirectoriesDifferencesTestScenario scenario)
    {
    	Container container = account.getContainer("x").create() ;
    	final String directoryName = "myDir" ;
		try 
		{
	        File local = TestUtils.getTestDirectoryWithFiles (tmpFolder, directoryName, "file", 10) ;
	        ops.uploadDirectory(container, null, local, true, stopRequester, callback);
	        StoredObject remote = container.getObject(directoryName) ;
	        assertTrue (remote.exists ()) ;
			
	        switch (scenario)
	        {
	        case NEW_FILE:
		        {
			        // we add a file in the directory
			        File newFile = new File (local.getPath() + File.separator + "newFileName.dat") ;
			        assertTrue(newFile.createNewFile()) ;
		        }
	        	break;
	        case MISSING_FILE:
		        {
			        // we delete a file in the directory
			        File file = new File (local.getPath() + File.separator + "file_1") ;
			        assertTrue (file.exists()) ;
			        assertTrue (file.delete()) ;
		        }
	        	break;
	        case NO_DIFF:
	        	break;
	        case MODIFIED_FILE:
	        {
		        // we modify a file in the directory
		        File file = new File (local.getPath() + File.separator + "file_1") ;
		        assertTrue (file.exists()) ;
		    	FileOutputStream out = new FileOutputStream(file);
		      	out.write(new byte[]{'a', 'b', 'c', 'd', 'e'});
		    	out.close();
	        }
        	break;
	        default:
	        	throw new IllegalStateException () ;
	        }
	        
	    	@SuppressWarnings("unchecked")
	    	ResultCallback<Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > > resultCallback 
	    	 	= (ResultCallback<Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > >)Mockito.mock(ResultCallback.class) ;
	    	
	    	@SuppressWarnings({ "unchecked", "rawtypes" })
			ArgumentCaptor<Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > > argument = ArgumentCaptor.forClass((Class) ResultCallback.class);
	    	
	    	ops.findDifferences(container, remote, local, resultCallback, stopRequester, callback);
	    	
	    	Mockito.verify(resultCallback, Mockito.times(1)).onResult(argument.capture());
	    	
	    	assertTrue ((scenario == FindDirectoriesDifferencesTestScenario.NO_DIFF) ? (argument.getValue().isEmpty()) : (!argument.getValue().isEmpty())) ;
		} 
		catch (IOException e) {
        	logger.error ("Error occurred in shouldFindDirectoriesDifferences", e) ;
        	assertFalse(true) ;
		}
    }
    
    
    @Test
    public void shouldFindNoDirectoriesDifferences ()
    {
    	shouldFindDirectoriesDifferences (FindDirectoriesDifferencesTestScenario.NO_DIFF) ;
    }
    
    
    @Test
    public void shouldFindDirectoriesDifferencesNewFile ()
    {
    	shouldFindDirectoriesDifferences (FindDirectoriesDifferencesTestScenario.NEW_FILE) ;
    }
    
    
    @Test
    public void shouldFindDirectoriesDifferencesMissingFile ()
    {
    	shouldFindDirectoriesDifferences (FindDirectoriesDifferencesTestScenario.MISSING_FILE) ;
    }
    
    
    @Test
    public void shouldFindDirectoriesDifferencesModifiedFile ()
    {
    	shouldFindDirectoriesDifferences (FindDirectoriesDifferencesTestScenario.MODIFIED_FILE) ;
    }
    
    
    @SuppressWarnings("unchecked")
	@Test(expected = IllegalArgumentException.class)
    public void shouldFailToCompareFiles() throws IOException 
    {
        Container container = account.getContainer("x").create();  
        StoredObject remote = container.getObject("remote");
        
        ops.findDifferences(container, remote, tmpFolder.newFile("locale"), Mockito.mock(ResultCallback.class) , stopRequester, callback) ;
    }
    
    
    @SuppressWarnings("unchecked")
	@Test(expected = IllegalArgumentException.class)
    public void shouldFailToCompareDirectories() throws IOException 
    {
        Container container = account.getContainer("x").create();  
        StoredObject remote = container.getObject("remote");
		remote.uploadObject(new UploadInstructions (new byte[] {}).setContentType(SwiftUtils.directoryContentType)) ;
		  
        ops.findDifferences(container, remote, tmpFolder.newFile("locale"), Mockito.mock(ResultCallback.class) , stopRequester, callback) ;
    }
    
    
    @Test
    public void shouldGetAllContainedStoredObjectInContainer () throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException
    {
        Container container = account.getContainer("x").create();
        
        StoredObject object1 = container.getObject("obj1");
        object1.uploadObject(new byte[]{'a', 'b', 'c'});
        
        StoredObject objectDir1 = container.getObject("dir1");
        objectDir1.uploadObject(new UploadInstructions (new byte[] {}).setContentType(SwiftUtils.directoryContentType)) ;
    	
        StoredObject object2 = container.getObject(objectDir1.getName() + SwiftUtils.separator + "obj2");
        object2.uploadObject(new byte[]{'a', 'b', 'c'});
        
        StoredObject objectDir2 = container.getObject(objectDir1.getName() + SwiftUtils.separator + "subDir");
        objectDir2.uploadObject(new UploadInstructions (new byte[] {}).setContentType(SwiftUtils.directoryContentType)) ;
    	
        // call a private method via reflection
        Class<?> c = SwiftOperationsImpl.class;
    	Method method = c.getDeclaredMethod ("getAllContainedStoredObject", Container.class, Directory.class);
    	method.setAccessible(true);
    	@SuppressWarnings("unchecked")
		Collection<StoredObject> ret = (Collection<StoredObject>) method.invoke(ops, container, null);
    	
    	assertTrue (ret.size() == 4) ;
    }
    
    
    @Test
    public void shouldGetAllContainedStoredObjectInParent () throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException
    {
        Container container = account.getContainer("x").create();
        
        StoredObject object1 = container.getObject("virtualDir" + SwiftUtils.separator + "obj1");
        object1.uploadObject(new byte[]{'a', 'b', 'c'});
        
        StoredObject objectDir1 = container.getObject("virtualDir" + SwiftUtils.separator + "dir1");
        objectDir1.uploadObject(new UploadInstructions (new byte[] {}).setContentType(SwiftUtils.directoryContentType)) ;
    	
        StoredObject object2 = container.getObject(objectDir1.getName() + SwiftUtils.separator + "obj2");
        object2.uploadObject(new byte[]{'a', 'b', 'c'});
        
        StoredObject objectDir2 = container.getObject(objectDir1.getName() + SwiftUtils.separator + "subDir");
        objectDir2.uploadObject(new UploadInstructions (new byte[] {}).setContentType(SwiftUtils.directoryContentType)) ;
    	
        Directory dir = new Directory ("virtualDir" + SwiftUtils.separator + "dir1", SwiftUtils.separator.charAt(0)) ;
        
        // call a private method via reflection
        Class<?> c = SwiftOperationsImpl.class;
    	Method method = c.getDeclaredMethod ("getAllContainedStoredObject", Container.class, Directory.class);
    	method.setAccessible(true);
    	@SuppressWarnings("unchecked")
		Collection<StoredObject> ret = (Collection<StoredObject>) method.invoke(ops, container, dir);
    	
    	// objectDir1, object2 and objectDir2
    	assertTrue (ret.size() == 3) ;
    }
}
