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

package org.swiftexplorer.swift.instructions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.javaswift.joss.instructions.SegmentationPlanFile;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.TestUtils;
import org.swiftexplorer.util.FileUtils;

public class FastSegmentationPlanFileTest {

	final Logger logger = LoggerFactory.getLogger(FastSegmentationPlanFileTest.class);
	
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();
    
    
    @Test
    public void shouldFastSegmentationPlanMD5BeCorrect() {
    	
    	try 
    	{
    		File file = TestUtils.getTestFile (tmpFolder, "md5.dat", 64 * 1024) ;

    		List<String> md5Fast = new ArrayList<String> () ;
    		List<String> md5Normal = new ArrayList<String> () ;
    		
			long size = FileUtils.getFileAttr(Paths.get(file.getPath())).size() ;
			FastSegmentationPlanFile fastSeg = new FastSegmentationPlanFile (file, size/2) ;
			InputStream segmentStream = fastSeg.getNextSegment() ;
			Date start = new Date () ;
			while (segmentStream != null)
			{
				md5Fast.add(FileUtils.readAllAndgetMD5(segmentStream)) ;
				segmentStream = fastSeg.getNextSegment() ;
			}
			logger.info("Fast segmentation: " + (new Date ().getTime() - start.getTime()) / 1000 + " s") ;
			
			SegmentationPlanFile seg = new SegmentationPlanFile (file, size/2) ;
			segmentStream = seg.getNextSegment() ;
			start = new Date () ;
			while (segmentStream != null)
			{
				md5Normal.add(FileUtils.readAllAndgetMD5(segmentStream)) ;
				segmentStream = seg.getNextSegment() ;
			}
			logger.info("Normal segmentation: " + (new Date ().getTime() - start.getTime()) / 1000 + " s") ;
				
			assertTrue (md5Normal.size() == md5Fast.size()) ;
			for (int i = 0 ; i < md5Fast.size() ; i++)
				assertTrue (md5Fast.get(i).equals(md5Normal.get(i))) ;
		} 
    	catch (IOException e) 
    	{
        	logger.error ("Error occurred in shouldComputeMD5", e) ;
        	assertFalse(true);
		}
    }
}
