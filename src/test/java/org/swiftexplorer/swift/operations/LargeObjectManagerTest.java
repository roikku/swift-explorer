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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.swiftexplorer.config.swift.SwiftParameters;
import org.swiftexplorer.swift.client.factory.AccountConfigFactory;
import org.swiftexplorer.swift.operations.SwiftOperations.SwiftCallback;
import org.swiftexplorer.swift.util.SwiftUtils;
import org.swiftexplorer.util.FileUtils;

public class LargeObjectManagerTest {

	final Logger logger = LoggerFactory.getLogger(LargeObjectManagerTest.class);
	
    private SwiftCallback callback;
    private Account account;
    private LargeObjectManagerImpl largeObjectManager ;
    
    private final long segmentSize = 800 ;
    private final long fileSize = 8192 ;
    private final String objName = "segmentationTest.dat" ;
    private Container container ;
    private File largeFile = null ;
    
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();
    
    
    @Before
    public void init() {
    	AccountConfig accConf = AccountConfigFactory.getMockAccountConfig() ;
        callback = Mockito.mock(SwiftCallback.class);

    	SwiftOperations ops = new SwiftOperationsImpl();
    	// We need to mockup SwiftParameters, because the builder constrains the minimum size
    	//SwiftParameters param = new SwiftParameters.Builder(segmentSize, true).build() ;
    	SwiftParameters param = Mockito.mock(SwiftParameters.class);
    	Mockito.when(param.getSegmentationSize()).thenReturn(segmentSize) ;
    	ops.login(accConf, param, "http://localhost:8080/", "user", "pass", "secret", callback);
        Mockito.verify(callback, Mockito.atLeastOnce()).onLoginSuccess();
        account = ((SwiftOperationsImpl)ops).getAccount() ;
        
        largeObjectManager = new LargeObjectManagerImpl (account) ;
        
        container = account.getContainer("x").create();
    }
    
    
    private void uploadFile (Container container, String objName, File file) throws IOException
    {
    	StoredObject obj = container.getObject(objName);
    	UploadInstructions ui = new UploadInstructions (file).setSegmentationSize(segmentSize) ;
    	BasicFileAttributes attr = FileUtils.getFileAttr(Paths.get(file.getPath())) ;
    	
    	ProgressInformation progInfo = new ProgressInformation (callback, false) ;
    	
    	largeObjectManager.uploadObjectAsSegments(obj, ui, attr.size(), progInfo, callback) ;
    }
    
    
    private void uploadAndCheckSegmentedFile() 
    {
    	uploadAndCheckSegmentedFile(fileSize, segmentSize) ;
    }
  
    
    private void uploadAndCheckSegmentedFile(long fSize, long sSize) 
    {
    	final long numSegments = TestUtils.getNumberOfSegments (fSize, sSize) ;
        assert (sSize > 1) ;
        
        try
        {   
        	if (largeFile == null)	
        		largeFile = TestUtils.getTestFile (tmpFolder, objName, fSize) ;
        	
        	// upload it
        	uploadFile (container, objName, largeFile) ;
        	
        	// verify that the segments container has been created 
        	Container containerSegments = account.getContainer(container.getName() + SwiftUtils.segmentsContainerPostfix);
        	assertTrue (containerSegments.exists()) ;
        	
        	// verify the number of segments
        	assertTrue (containerSegments.getCount() == numSegments) ;
        	
        	// check the object
        	StoredObject object = container.getObject(objName);
        	assertTrue (object.exists()) ;
        }
        catch (IOException e) 
        {
        	logger.error ("Error occurred in shouldUploadSegmentedFile", e) ;
        	assertFalse(true) ;
		}	
    }
    
	
    @Test
    public void shouldListSegments() 
    {
    	uploadAndCheckSegmentedFile () ;
    	
    	StoredObject object = container.getObject(objName);
    	List<StoredObject> list = largeObjectManager.getSegmentsList(object) ;

    	assertFalse (list.isEmpty()) ;
    	assertTrue (list.size() == TestUtils.getNumberOfSegments (fileSize, segmentSize)) ;
    	
    	// upload a non-segmented file...
        try 
        {
			File smallFile = TestUtils.getTestFile (tmpFolder, "nonSegmentedData.dat", segmentSize / 2) ;
			StoredObject smallObj = container.getObject(smallFile.getName());
			smallObj.uploadObject(smallFile);
        	assertTrue (smallObj.exists()) ;
        	
        	assertTrue(largeObjectManager.getSegmentsList(smallObj).isEmpty()) ;
		} 
        catch (IOException e) 
		{
        	logger.error ("Error occurred in shouldListSegments", e) ;
        	assertFalse(true) ;
		}
    }
    
    
    @Test
    public void shouldFindOutWhetherItIsSegmented() 
    {
    	uploadAndCheckSegmentedFile () ;
    	
    	StoredObject object = container.getObject(objName);
    	assertTrue(largeObjectManager.isSegmented(object)) ;
    	
    	// upload a non-segmented file...
        try 
        {
			File smallFile = TestUtils.getTestFile (tmpFolder, "nonSegmentedData.dat", segmentSize / 2) ;
			StoredObject obj = container.getObject(smallFile.getName());
			obj.uploadObject(smallFile);
        	assertTrue (obj.exists()) ;
        	
        	assertFalse(largeObjectManager.isSegmented(obj)) ;
		} 
        catch (IOException e) 
		{
        	logger.error ("Error occurred in shouldFindOutThatItIsSegmented", e) ;
        	assertFalse(true) ;
		}
    }
    
    
    @Test
    public void shouldReuseExistingSegments() 
    {
    	uploadAndCheckSegmentedFile () ;
    	
    	StoredObject manifest = container.getObject(objName);
    	List<StoredObject> listSegments = largeObjectManager.getSegmentsList(manifest) ;
    	
    	Set<Date> setDates = new HashSet<Date> () ;
    	for (StoredObject so : listSegments)
    		setDates.add(so.getLastModifiedAsDate()) ;
    
    	// we delete the manifest only (so that the segments remain)
    	manifest.delete();
    	assertFalse (manifest.exists()) ;
    	
    	// we upload again the file
    	uploadAndCheckSegmentedFile () ;
    	
    	// we check the dates
    	Set<Date> setNewDates = new HashSet<Date> () ;
    	for (StoredObject so : largeObjectManager.getSegmentsList(container.getObject(objName)))
    		setNewDates.add(so.getLastModifiedAsDate()) ;
    	
    	assertTrue (setNewDates.removeAll(setDates)) ;
    	assertTrue (setNewDates.isEmpty ()) ;
    }
    
    
    @Test
    public void shouldNotReuseExistingSegments() 
    {
    	uploadAndCheckSegmentedFile () ;
    	
    	StoredObject manifest = container.getObject(objName);
    	List<StoredObject> listSegments = largeObjectManager.getSegmentsList(manifest) ;
    	
    	Set<Date> setDates = new HashSet<Date> () ;
    	for (StoredObject so : listSegments)
    		setDates.add(so.getLastModifiedAsDate()) ;
    
    	// we delete the manifest only (so that the segments remain)
    	manifest.delete();
    	assertFalse (manifest.exists()) ;
    	
    	// We modify the file 
    	// (randomly regenerated: it would be unlucky that even one segment hashes to the same value...)
    	largeFile = null ;
    	
    	// we upload again a new file with the same name
    	uploadAndCheckSegmentedFile () ;
    	
    	// we check the dates
    	Set<Date> setNewDates = new HashSet<Date> () ;
    	for (StoredObject so : largeObjectManager.getSegmentsList(container.getObject(objName)))
    		setNewDates.add(so.getLastModifiedAsDate()) ;
    	
    	assertFalse (setNewDates.removeAll(setDates)) ;
    }
    
    
    @Test
    public void shouldCleanUpExtraSegments() 
    {
    	uploadAndCheckSegmentedFile () ;
    	
    	StoredObject manifest = container.getObject(objName);
    	int initialSegmentCount = largeObjectManager.getSegmentsList(manifest).size() ;
    
    	// we delete the manifest only (so that the segments remain)
    	manifest.delete();
    	assertFalse (manifest.exists()) ;
    	
    	// We reduce the file size so that less segments are required
    	long newFileSize = fileSize / 2 ;
		largeFile = null ;
    	uploadAndCheckSegmentedFile (newFileSize, segmentSize) ;
    	
    	// we verify that the actual number of segments is coherent
    	int newSegmentCount = largeObjectManager.getSegmentsList(container.getObject(objName)).size() ;
    	assertTrue (initialSegmentCount > newSegmentCount) ;
    	assertTrue (newSegmentCount == TestUtils.getNumberOfSegments (newFileSize, segmentSize)) ;
    }
    
    
    @Test
    public void shouldReturnSegmentSize() 
    {
    	uploadAndCheckSegmentedFile () ;
    	
    	StoredObject manifest = container.getObject(objName);
    	
    	assertTrue (manifest.exists()) ;
    	assertTrue (segmentSize == largeObjectManager.getActualSegmentSize(manifest)) ;
    }
    
    
    @Test
    public void shouldNotReturnSegmentSize() 
    {
    	StoredObject obj = container.getObject(objName);
    	obj.uploadObject(new byte [] {'a', 'b', 'c'});
    	
    	// here obj is not segmented
    	assertTrue (obj.exists()) ;
    	assertTrue (-1 == largeObjectManager.getActualSegmentSize(obj)) ;
    }
}
