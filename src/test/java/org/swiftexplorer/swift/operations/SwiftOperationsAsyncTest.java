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
* Note: the class has been renamed from CloudieOperationsAsyncTest
* to SwiftOperationsAsyncTest
*
*/


package org.swiftexplorer.swift.operations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.gui.util.AsyncWrapper;
import org.swiftexplorer.gui.util.SwiftOperationStopRequesterImpl;
import org.swiftexplorer.swift.client.factory.AccountConfigFactory;
import org.swiftexplorer.swift.operations.SwiftOperations.SwiftCallback;

import java.io.File;
import java.io.IOException;

import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.model.Account;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class SwiftOperationsAsyncTest {

	final Logger logger = LoggerFactory.getLogger(SwiftOperationsAsyncTest.class);
	
    private SwiftOperations ops;
    private SwiftCallback callback;
    private Account account;
    private AccountConfig accConf ;
    
    private SwiftOperationStopRequesterImpl stopRequester = new SwiftOperationStopRequesterImpl () ;

    @Before
    public void init() {
    	accConf = AccountConfigFactory.getMockAccountConfig() ;
    	SwiftOperationsImpl opsTmp =  new SwiftOperationsImpl() ;
        ops = AsyncWrapper.async(opsTmp);
        callback = Mockito.mock(SwiftCallback.class);
        opsTmp.login(accConf, "", "", "", "", callback);
        account = opsTmp.getAccount() ;
    }

    @Test
    public void shouldSignalStartAndDone() throws InterruptedException {
        ops.createContainer(new ContainerSpecification("x", true), callback);
        Thread.sleep(500L);
        Mockito.verify(callback, Mockito.atLeastOnce()).onStart();
        Mockito.verify(callback, Mockito.atLeastOnce()).onDone();
    }
    
    @Test
    public void shouldSignalCommandException() throws InterruptedException {
    	try
    	{
    		// container does not exist.
	        ops.createStoredObjects(account.getContainer("x"), new File[] { new File("pom.xml") }, stopRequester, callback); 
    	}
    	catch (IOException e)
    	{
    		logger.error("Error occurred while creating stored object", e) ;
    	}
        
        Thread.sleep(1000L);
        Mockito.verify(callback, Mockito.atLeastOnce()).onStart();
        Mockito.verify(callback, Mockito.atLeastOnce()).onError(Mockito.any(CommandException.class));
        Mockito.verify(callback, Mockito.atLeastOnce()).onDone();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptNullCallback() {
        ops.createContainer(new ContainerSpecification("x", true), null);
    }
}
