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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
    public void shouldComputeMD5() 
    {	
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
        
    
    @Test
    public void shouldComputeSumOfSegmentsMd5() throws IOException 
    {	
    	final int size = 1024 ;
    	final int numSeg = 3 ;
    	List<File> segList = new ArrayList<File> () ;
    	List<String> segMd5List = new ArrayList<String> () ;
    	for (int i = 0 ; i < numSeg ; ++i)
    	{
    		File f = TestUtils.getTestFile (tmpFolder, "md5_" + i + ".dat", size) ;
    		segList.add(f) ;
    		segMd5List.add(FileUtils.getMD5(f)) ;
    	}
    	
		StringBuilder sb = new StringBuilder () ;
		for (String str : segMd5List){	
			sb.append(str) ;
		}
    	InputStream stream = new ByteArrayInputStream (sb.toString().getBytes(StandardCharsets.UTF_8));
		String sumMd5 = FileUtils.readAllAndgetMD5(stream) ;
		logger.info("Result of the md5 of the sum of md5s: " + sumMd5) ;
		
    	File file = tmpFolder.newFile() ;
    	final ByteBuffer buffer = ByteBuffer.allocateDirect(128);
    	OutputStream out = null ;	
    	try
    	{
			out = new FileOutputStream(file);
			final WritableByteChannel outCh = Channels.newChannel(out);
			for (File seg : segList) 
			{
				InputStream in = null;
				try {
					in = new FileInputStream(seg);
					ReadableByteChannel inCh = Channels.newChannel(in);
					while (inCh.read(buffer) != -1) {
						buffer.flip();
						outCh.write(buffer);
						buffer.compact();
					}
					buffer.flip();
					while (buffer.hasRemaining()) {
						outCh.write(buffer);
					}
				} finally {
					if (in != null)
						in.close () ;
				}
			}
    	}
    	finally
    	{
    		if (out != null)
    			out.close() ;
    	}
    	assertTrue (FileUtils.getFileAttr(Paths.get(file.getPath())).size() == numSeg * size) ;
    	
    	String sum = FileUtils.getSumOfSegmentsMd5(file, size) ;
    	logger.info("Result of getSumOfSegmentsMd5: " + sum) ;
    	
    	assertTrue (sumMd5.equals(sum)) ;
    } 	
}
