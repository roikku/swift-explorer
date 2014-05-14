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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import org.swiftexplorer.swift.operations.SwiftOperations.ComparisonItem;
import org.swiftexplorer.swift.util.SwiftUtils;
import org.swiftexplorer.util.FileUtils;

public class DifferencesFinder {
	
	private DifferencesFinder () { super () ; } 

	private static abstract class AbstractComparisonItem implements ComparisonItem {
		
		protected final String separator = SwiftUtils.separator ;
		
		@Override
		public int hashCode() {
			return 31 *  (int) (getSize() ^ (getSize() >>> 32)) + ((getMD5 () == null) ? 0 : getMD5 ().hashCode());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			AbstractComparisonItem other = (AbstractComparisonItem) obj;
			return (compareTo(other) == 0) ;
		}

		@Override
		public int compareTo(ComparisonItem o) {
			int res = this.getMD5().compareTo(o.getMD5());
			if (res != 0)
				return res ;
			return Long.valueOf(this.getSize()).compareTo(Long.valueOf(o.getSize())) ;
		}
		
		@Override
		public String toString() {
			return getName() ;
		}
	}
	
	
	public static class LocalItem extends AbstractComparisonItem
	{		
		private final Path filePath ;
		private final String md5 ;
		private final long size ;
		private final boolean exists ; 
		private final String remoteFullName ;
		
		public LocalItem (Path filePath, Path rootDir, long segmentSize) throws IOException
		{
			super () ;
			if (filePath == null)
				throw new IllegalArgumentException ("The file path cannot be null") ;
			this.filePath = filePath ;
			
			StringBuilder fullRelativeNameBuilder = new StringBuilder () ;
			if (rootDir == null)
				fullRelativeNameBuilder.append((separator.equals(File.separator)) ? (filePath.toString()) : (filePath.toString().replace(File.separator, separator))) ;
			else
			{
				fullRelativeNameBuilder.append(rootDir.getFileName()) ;
				fullRelativeNameBuilder.append(SwiftUtils.separator) ;
				fullRelativeNameBuilder.append((separator.equals(File.separator)) ? (rootDir.relativize(filePath).toString()) : (rootDir.relativize(filePath).toString().replace(File.separator, separator))) ;
			}
			
			this.remoteFullName = fullRelativeNameBuilder.toString() ;
			this.exists = Files.exists(filePath) ; 
			this.size = (exists) ? ((Files.isDirectory(filePath)) ? (0) : (FileUtils.getFileAttr(filePath).size())) : (-1) ;
			
			if (!exists)
				this.md5 = "" ;
			else
			{
				long size = FileUtils.getFileAttr(filePath).size() ;
				if (size <= segmentSize)
					this.md5 = FileUtils.getMD5(filePath.toFile()) ;
				else
					this.md5 = FileUtils.getSumOfSegmentsMd5(filePath.toFile(), segmentSize) ;
			}
		}

		@Override
		public boolean isRemote() {
			return false;
		}

		@Override
		public String getMD5() {
			return md5;
		}

		@Override
		public String getName() {
			return filePath.getFileName().toString() ;
		}

		@Override
		public String getPath() {
			return filePath.toString() ;
		}

		@Override
		public long getSize() {
			return size;
		}

		@Override
		public boolean exists() {
			return exists;
		}

		@Override
		public String getRemoteFullName() {
			return remoteFullName;
		}

		public Path getFile() {
			return filePath;
		}
	}
	
	
	public static class RemoteItem extends AbstractComparisonItem
	{		
		private final StoredObject storedObject ;

		private final String md5 ;
		private final long size ;
		private final boolean exists ; 
		
		public RemoteItem (StoredObject storedObject) throws IOException
		{
			super () ;
			if (storedObject == null)
				throw new IllegalArgumentException ("storedObject cannot be null") ;
			this.storedObject = storedObject ;
			this.exists = storedObject.exists() ;
			this.size = (exists) ? (storedObject.getContentLength()) : (-2) ;
			if (exists)
			{
				// For segmented object, the etag may be bounded by "".
				String etag = storedObject.getEtag()  ;
				if (etag.startsWith("\"")) 
					etag = etag.replace("\"", "") ;
				this.md5 = etag ;
			}
			else
				this.md5 = "" ;
		}

		@Override
		public boolean isRemote() {
			return true;
		}

		@Override
		public String getMD5() {
			return md5;
		}

		@Override
		public String getName() {
			return storedObject.getBareName();
		}

		@Override
		public String getPath() {
			return storedObject.getPath();
		}

		@Override
		public long getSize() {
			return size ;
		}

		@Override
		public boolean exists() {
			return exists;
		}

		@Override
		public String getRemoteFullName() {
			return storedObject.getName();
		}

		public StoredObject getStoredObject() {
			return storedObject;
		}
	}
	
	
	public static StoredObject getParentObject (Container container, ComparisonItem item)
	{
		if (container == null || item == null)
			throw new IllegalArgumentException () ;
		String name = item.getRemoteFullName() ;
		int index = name.lastIndexOf(SwiftUtils.separator) ;
		if (index < 0)
			return null ;
		return container.getObject(name.substring(0, index)) ;		
	}
	
	
	public static StoredObject getObject (Container container, ComparisonItem item, StoredObject root)
	{
		if (container == null || item == null)
			throw new IllegalArgumentException () ;
		if (item instanceof RemoteItem)
			return ((RemoteItem)item).getStoredObject () ;
		if (item instanceof LocalItem)
		{
			if (root == null)
				throw new IllegalArgumentException () ;
			String name = item.getRemoteFullName() ;
			if (SwiftUtils.isDirectory(root))
			{
				if (!name.startsWith(root.getBareName()))
					throw new IllegalStateException () ;	
				name = name.replaceFirst(root.getBareName(), "") ;
				StringBuilder sb = new StringBuilder () ;
				sb.append(root.getName());
				if (!name.startsWith(SwiftUtils.separator))
					sb.append(SwiftUtils.separator);
				sb.append(name);
				return container.getObject(sb.toString()) ;
			}
			else
			{
				if (!name.equals(root.getBareName()))
					throw new IllegalStateException () ;
				return root ;
			}
		}
		return null ;		
	}
	
	
	public static File getFile (ComparisonItem item, File root)
	{
		if (item == null)
			throw new IllegalArgumentException () ;
		if (item instanceof RemoteItem)
		{
			if (root == null)
				throw new IllegalArgumentException () ;
			String name = ((RemoteItem)item).getRemoteFullName() ;
			if (!File.separator.equals(SwiftUtils.separator))
				name = name.replace(SwiftUtils.separator, File.separator) ;
				
			int index = name.lastIndexOf(root.getName()) ;
			if (index >= 0)
				name = name.substring(index + root.getName().length()) ;
			if (name.startsWith(File.separator))
				name = name.substring(1) ;
					
			return Paths.get(root.getPath()).resolve(Paths.get(name)).toFile() ;
		}
		if (item instanceof LocalItem)
			return ((LocalItem)item).getFile().toFile() ;
		return null ;		
	}
	
	
	public static boolean isDirectory (ComparisonItem item)
	{
		if (item == null)
			return false ;
		if (item instanceof RemoteItem)
			return SwiftUtils.isDirectory(((RemoteItem)item).getStoredObject ()) ;
		if (item instanceof LocalItem)
			return Files.isDirectory(((LocalItem)item).getFile ()) ;
		return false ;
	}
}
