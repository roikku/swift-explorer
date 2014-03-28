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


import org.swiftexplorer.gui.util.SwingUtils;

import java.awt.Component;
import java.awt.Frame;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import java.nio.file.*;
import java.util.ArrayDeque;
import java.util.Queue;

import javax.swing.ProgressMonitorInputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.FileVisitResult.*;

public class FileUtils {
	
	final static Logger logger = LoggerFactory.getLogger(FileUtils.class);
	
	private FileUtils () { super () ; } ;
	
	
	public static String getMD5 (File file) throws IOException
	{
		HashCode hc = com.google.common.io.Files.hash(file, Hashing.md5());
		return (hc != null) ? (hc.toString()) : (null) ;
	}
	
	
	public static Queue<Path> getAllFilesPath (Path srcDir, boolean inludeDir) throws IOException
	{
		if (srcDir == null)
			return null ;
		Queue<Path> queue = new ArrayDeque<Path> () ;
		if (!Files.isDirectory(srcDir))
			return queue ;
		
		TreePathFinder tpf = new TreePathFinder(queue, inludeDir);
		Files.walkFileTree(srcDir, tpf);
	
		return queue ;
	}
	
	
	public static BasicFileAttributes getFileAttr (Path path) throws IOException
	{
		BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class) ;
		return attr ;
	}
	
	
	private static class TreePathFinder extends SimpleFileVisitor<Path> {

		private final Queue<Path> filesPath ;
		private final boolean inludeDir ;
		
		public TreePathFinder(Queue<Path> filesPath, boolean inludeDir) {
			super();
			if (filesPath == null)
				throw new AssertionError ("TreePathFinder cannot accept null queue.") ;
			this.inludeDir = inludeDir ;
			this.filesPath = filesPath;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attr) 
		{
			filesPath.add(file) ;
			return CONTINUE;
		}
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) 
		{
			if (inludeDir)
				filesPath.add(dir) ;
			return CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
			return CONTINUE;
		}

		// If there is some error accessing
		// the file, let the user know.
		// If you don't override this method
		// and an error occurs, an IOException
		// is thrown.
		/*
		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) {
            if (exc instanceof FileSystemLoopException) {
                logger.error("Error occurred while walking the directory to find all the files path (cycle detected: " + file + ").", exc);
            } else {
                logger.error("Error occurred while walking the directory to find all the files path.", exc);
            }
            return CONTINUE;
		}
		*/
	}
	
	
    public static class InputStreamProgressFilter extends FilterInputStream
    {
    	public static interface StreamProgressCallback
    	{
    		public void onStreamProgress (double progress) ;
    	}
    	
    	private final long size ;
    	private volatile long read = 0 ;
    	private final StreamProgressCallback callback ;
    	
		protected InputStreamProgressFilter(InputStream in, long size, StreamProgressCallback callback) {

			super(in);
			if (size < 0)
				throw new AssertionError ("The size of the stream to minitored must be positive") ;
			
			// Not we need the size because, in.available() does not necessarily return the size (see specs).
			this.size = size ;
			this.callback = callback ;
		}
    	
		private double getProgress ()
		{
			if (size == 0)
				return 1.0 ;
			return read / (double)size ;
		}
		
		private void checkProgress ()
		{
			if (callback == null)
				return ;
			callback.onStreamProgress(getProgress());
		}

		@Override
		public int read() throws IOException {
			int r = super.read();
			read += r ;
			checkProgress () ;
			return r;
		}

		@Override
		public int read(byte[] b) throws IOException {
			int r = super.read(b);
			read += r ;
			checkProgress () ;
			return r;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int r = super.read(b, off, len);
			read += r ;
			checkProgress () ;
			return r;
		}

		@Override
		public long skip(long n) throws IOException {
			long r = super.skip(n);
			read += r ;
			checkProgress () ;
			return r;
		}
    }
    
    
	public static InputStream getInputStreamWithProgressFilter(InputStreamProgressFilter.StreamProgressCallback callback, long size, Path path) throws FileNotFoundException 
	{
		return getInputStreamWithProgressFilter (callback, size, new FileInputStream(path.toFile())) ;
	}
	
	
	public static InputStream getInputStreamWithProgressFilter(InputStreamProgressFilter.StreamProgressCallback callback, long size, InputStream input)
	{
		InputStream in = new BufferedInputStream(
				new InputStreamProgressFilter(input, size, callback));
		return in;
	}
	
	
	public static InputStream getInputStreamWithProgressMonitor(Path path, Component parentComponent, String message) throws FileNotFoundException 
	{
		return getInputStreamWithProgressMonitor (new FileInputStream(path.toFile()), parentComponent, message) ;
	}
	
	
	public static InputStream getInputStreamWithProgressMonitor(InputStream input, Component parentComponent, String message)
	{
		Frame owner = null;
		if (parentComponent == null) 
			owner = SwingUtils.tryFindSuitableFrameOwner () ;
		InputStream in = new BufferedInputStream(
				new ProgressMonitorInputStream(
						(parentComponent == null) ? (owner) : (parentComponent),
						message, input));
		return in;
	}
	
	
	public static void saveInputStreamInFile (InputStream input, File target, boolean closeInputStream) throws IOException
	{
		if (input == null || target == null)
			throw new AssertionError () ;
		FileOutputStream output = new FileOutputStream(target);
		try
		{
			IOUtils.copyLarge(input, output);
		}
		finally
		{
			if (output != null)
				output.close();
			if (closeInputStream)
				input.close();
		}
	}
	
	
    // http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
}
