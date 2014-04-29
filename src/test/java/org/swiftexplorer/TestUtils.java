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

package org.swiftexplorer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.rules.TemporaryFolder;

public class TestUtils {

	private TestUtils (){ super () ; } ;
    
    public static File getTestFile (TemporaryFolder tmpFolder, String fileName, long fileSize) throws IOException
    {
        byte data [] = new byte[(int) fileSize] ;
        for (int i = 0 ; i < fileSize ; ++i)
        	data[i] = (byte)(Math.random() * 256) ;

        File file = tmpFolder.newFile(fileName) ;
        
    	// generate test file
    	FileOutputStream out = new FileOutputStream(file);
      	out.write(data);
    	out.close();
        
        return file ;
    }
    
    
    public static  File getTestDirectoryWithFiles (TemporaryFolder tmpFolder, String directoryName, String fileNamePrefix, int numberOfFiles) throws IOException
    {
    	assert (numberOfFiles >= 0) ;
    	
        File folder = tmpFolder.newFolder(directoryName) ;
        
        if (numberOfFiles == 0)
        	return folder ;
        
        StringBuilder fileNameBase = new StringBuilder () ;
        fileNameBase.append(directoryName) ;
        fileNameBase.append(File.separator) ;
        fileNameBase.append(fileNamePrefix) ;
        fileNameBase.append("_") ;
        
        File [] fileList = new File [numberOfFiles] ;
        for (int i = 0 ; i < numberOfFiles ; ++i)
        {
        	final String fileName = fileNameBase.toString() + i ;
        	fileList[i] = getTestFile (tmpFolder, fileName, (long) (Math.random() * 1000 + 1)) ;
        }
        
        return folder ;
    }
    
    
    public static long getNumberOfSegments (long fileSize, long segmentSize)
    {
    	return fileSize / segmentSize + (long)((fileSize % segmentSize == 0)?(0):(1)) ;
    }
}
