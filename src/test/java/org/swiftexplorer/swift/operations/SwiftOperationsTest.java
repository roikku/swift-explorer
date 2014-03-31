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

import java.io.File;
import java.io.IOException;
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
}
