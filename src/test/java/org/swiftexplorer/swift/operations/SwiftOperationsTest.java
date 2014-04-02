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
import org.swiftexplorer.swift.client.factory.AccountConfigFactory;
import org.swiftexplorer.swift.operations.SwiftOperations.SwiftCallback;
import org.swiftexplorer.swift.util.SwiftUtils;
import org.swiftexplorer.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class SwiftOperationsTest {

	final Logger logger = LoggerFactory.getLogger(SwiftOperationsTest.class);
	
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
    
    
    private File getTestFile (String fileName, long fileSize) throws IOException
    {
        byte data [] = new byte[(int) fileSize] ;
        for (int i = 0 ; i < fileSize ; ++i)
        	data[i] = (byte)(Math.random() * 256) ;
        File file = new File("./target/" + fileName);
        
    	// generate test file
    	FileOutputStream out = new FileOutputStream(file);
      	out.write(data);
    	out.close();
        
        return file ;
    }
    
    
    private long getNumberOfSegments (long fileSize, long segmentSize)
    {
    	return fileSize / segmentSize + (long)((fileSize % segmentSize == 0)?(0):(1)) ;
    }
    
    
    @Test
    public void shouldCreateSegments() {
    	
    	final long segmentSize = 800 ;
    	final long fileSize = 8192 ;
    	final String fileName = "segmentationTest.dat" ;
    	
		final long numSegments = getNumberOfSegments (fileSize, segmentSize) ;
    	
    	SwiftOperations ops = getCustomSegmentationSizeSwiftOperations (segmentSize) ;
        Account acc = ((SwiftOperationsImpl)ops).getAccount() ;
    	
        Container container = acc.getContainer("x").create();
        ops.refreshStoredObjects(container, callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onNewStoredObjects();
        
        File file = null ;
        boolean exceptionThrown = false ;
        try
        {         
        	file = getTestFile (fileName, fileSize) ;
        	
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
        	exceptionThrown = true ;
		}
        finally
        {
        	if (file != null)
        		file.delete();
	        assertFalse(exceptionThrown);
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
        	file = getTestFile (fileName, fileSize) ;
        	      
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
        	exceptionThrown = true ;
		}
        finally
        {
        	if (file != null)
        		file.delete();
	        target.delete() ;
	        assertFalse(exceptionThrown);
        }	
    }
    
    
    @Test
    public void shouldDeleteSegmentedObject() {
    	
    	final long segmentSize = 800 ;
    	final long fileSize = 8192 ;
    	final String fileName = "segmentationTest.dat" ;
    	
    	final long numSegments = getNumberOfSegments (fileSize, segmentSize) ;
    	
    	SwiftOperations ops = getCustomSegmentationSizeSwiftOperations (segmentSize) ;
        Account acc = ((SwiftOperationsImpl)ops).getAccount() ;
    	
        Container container = acc.getContainer("x").create();
        ops.refreshStoredObjects(container, callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onNewStoredObjects();

        File file = null ;
        boolean exceptionThrown = false ;
        try
        {        
        	file = getTestFile (fileName, fileSize) ;
        	
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
        	exceptionThrown = true ;
		}
        finally
        {
        	if (file != null)
        		file.delete();
	        assertFalse(exceptionThrown);
        }	
    }
}
