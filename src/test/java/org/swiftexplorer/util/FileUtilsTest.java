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

package org.swiftexplorer.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.TestUtils;

public class FileUtilsTest {
	
	final Logger logger = LoggerFactory.getLogger(FileUtilsTest.class);
	
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();
    
    
    @Test
    public void shouldComputeMD5() {
    	
    	try 
    	{
			File file = TestUtils.getTestFile (tmpFolder, "testmd5.dat", 64 * 1024) ;
			String md5_1 = FileUtils.getMD5(file) ;
			
			InputStream in = new FileInputStream(file.getPath()); 
			String md5_2 = FileUtils.readAllAndgetMD5(in) ;
			
			logger.info("FileUtils.getMD5 returns " + md5_1) ;
			logger.info("FileUtils.readAllAndgetMD5 returns " + md5_2) ;
			
			assertTrue (md5_1 != null && !md5_1.isEmpty()) ;
			assertTrue (md5_1.equals(md5_2)) ;
		} 
    	catch (IOException e) 
    	{
        	logger.error ("Error occurred in shouldComputeMD5", e) ;
        	assertFalse(true);
		}
    	
    }
}
