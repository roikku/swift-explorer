/*
 * Copyright 2014 Loic Merckel
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.swiftexplorer.swift.operations;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.instructions.UploadInstructions;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.TestUtils;
import org.swiftexplorer.swift.client.factory.AccountConfigFactory;
import org.swiftexplorer.swift.operations.SwiftOperations.SwiftCallback;
import org.swiftexplorer.swift.util.SwiftUtils;

public class DifferencesFinderTest {

	
	final Logger logger = LoggerFactory.getLogger(DifferencesFinderTest.class);
	
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
	public void shouldReturnObjectParentObject () throws IOException
	{
		Container container = account.getContainer("x").create() ;
		
		StoredObject parentObject = container.getObject("parent") ;
		parentObject.uploadObject(new UploadInstructions (new byte[] {}).setContentType(SwiftUtils.directoryContentType)) ;
		
		StoredObject childObject = container.getObject("parent" + SwiftUtils.separator + "object") ;
		childObject.uploadObject(new byte[] {'a', 'b', 'c'}) ;

		DifferencesFinder.RemoteItem ri = new DifferencesFinder.RemoteItem (childObject) ;
		StoredObject parent = DifferencesFinder.getParentObject(container, ri) ;
		
		assertTrue (parent.exists()) ;
		assertTrue (parent.getName().equals(parentObject.getName())) ;
	}
	
	
	@Test
	public void shouldReturnFileParentObject () throws IOException
	{
		Container container = account.getContainer("x").create() ;
		
		final String root = "root" ;
		final String myDir = "myDir" ;
		final String directoryName = root + SwiftUtils.separator + myDir ;
		File rootDirectory = TestUtils.getTestDirectoryWithFiles (tmpFolder, root, "file", 0) ;
		File directory = new File (rootDirectory.getPath() + File.separator + myDir) ;
		assertTrue(directory.mkdirs()) ;
		Path file = Paths.get(new File (directory.getPath() + File.separator + "file_1").getPath()) ;
		Files.createFile(file) ;
		assertTrue (Files.exists(file)) ;

		DifferencesFinder.LocalItem li = new DifferencesFinder.LocalItem(file, Paths.get(rootDirectory.getPath()), Long.MAX_VALUE) ;
		StoredObject parent = DifferencesFinder.getParentObject(container, li) ;
		
		assertTrue (parent.getName().equals(directoryName)) ;
	}
	
	
	@Test
	public void shouldReturnFileObject () throws IOException
	{
		Container container = account.getContainer("x").create() ;
		
		final String root = "root" ;
		
		StoredObject rootObj = container.getObject(root) ;
		rootObj.uploadObject(new UploadInstructions (new byte[] {}).setContentType(SwiftUtils.directoryContentType)) ;
		
		File rootDirectory = TestUtils.getTestDirectoryWithFiles (tmpFolder, root, "file", 2) ;
		Path file = Paths.get(new File (rootDirectory.getPath() + File.separator + "file_1").getPath()) ;
		assertTrue (Files.exists(file)) ;

		DifferencesFinder.LocalItem li = new DifferencesFinder.LocalItem(file, Paths.get(rootDirectory.getPath()), Long.MAX_VALUE) ;
		StoredObject obj = DifferencesFinder.getObject(container, li, rootObj) ;
		
		assertTrue (obj.getBareName().equals(file.getFileName().toString())) ;
		assertTrue (obj.getName().equals(root + SwiftUtils.separator + file.getFileName().toString())) ;
	}
	
	
	@Test
	public void shouldReturnObjectObject () throws IOException
	{
		Container container = account.getContainer("x").create() ;
		StoredObject obj = container.getObject("object") ;
		obj.uploadObject(new byte[]{'a', 'b', 'c'}) ;
		assertTrue (obj.exists()) ;
		
		DifferencesFinder.RemoteItem ri = new DifferencesFinder.RemoteItem(obj) ;
		StoredObject object = DifferencesFinder.getObject(container, ri, null) ;
		
		assertTrue (obj.getEtag().equals(object.getEtag())) ;
	}
	
	
	@Test
	public void shouldReturnFileFile () throws IOException
	{
		File file = tmpFolder.newFile() ;
		DifferencesFinder.LocalItem li = new DifferencesFinder.LocalItem(Paths.get(file.getPath()), null, Long.MAX_VALUE) ;
		assertTrue (DifferencesFinder.getFile(li, null).getPath().equals(file.getPath())) ;
	}
	
	
	@Test
	public void shouldReturnObjectFile () throws IOException
	{
		Container container = account.getContainer("x").create() ;
		
		final String root = "root" ;
		File rootDirectory = TestUtils.getTestDirectoryWithFiles (tmpFolder, root, "file", 2) ;
		Path file = Paths.get(new File (rootDirectory.getPath() + File.separator + "file_1").getPath()) ;
		assertTrue (Files.exists(file)) ;
	
		DifferencesFinder.LocalItem li = new DifferencesFinder.LocalItem(file, Paths.get(rootDirectory.getPath()), Long.MAX_VALUE) ;
		
		StoredObject obj = container.getObject(li.getRemoteFullName()) ;
		obj.uploadObject(new byte[]{'a', 'b', 'c'}) ;
		assertTrue (obj.exists()) ;

		DifferencesFinder.RemoteItem ri = new DifferencesFinder.RemoteItem(obj) ;

		assertTrue (Paths.get(DifferencesFinder.getFile(ri, rootDirectory).getPath()).equals(file)) ;
	}
	
	
	@Test
	public void shouldBeDirectoryObject () throws IOException
	{
		Container container = account.getContainer("x").create() ;
		StoredObject object = container.getObject("dir") ;
		object.uploadObject(new UploadInstructions (new byte[] {}).setContentType(SwiftUtils.directoryContentType)) ;
		
		DifferencesFinder.RemoteItem ri = new DifferencesFinder.RemoteItem (object) ;
		assertTrue (DifferencesFinder.isDirectory(ri)) ;
	}
	
	
	@Test
	public void shouldNotBeDirectoryObject () throws IOException
	{
		Container container = account.getContainer("x").create() ;
		StoredObject object = container.getObject("object") ;
		object.uploadObject(new byte[] {'a', 'b', 'c'}) ;
		
		DifferencesFinder.RemoteItem ri = new DifferencesFinder.RemoteItem (object) ;
		assertFalse (DifferencesFinder.isDirectory(ri)) ;
	}
	
	
	@Test
	public void shouldBeDirectoryFile () throws IOException
	{
		DifferencesFinder.LocalItem li = new DifferencesFinder.LocalItem (Paths.get(tmpFolder.newFolder().getPath()), null, Long.MAX_VALUE) ;
		assertTrue (DifferencesFinder.isDirectory(li)) ;
	}
	
	
	@Test
	public void shouldNotBeDirectoryFile () throws IOException
	{
		DifferencesFinder.LocalItem li = new DifferencesFinder.LocalItem (Paths.get(tmpFolder.newFile().getPath()), null, Long.MAX_VALUE) ;
		assertFalse (DifferencesFinder.isDirectory(li)) ;
	}
}
