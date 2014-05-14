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

package org.swiftexplorer.swift.util;

import org.javaswift.joss.model.StoredObject;

public class SwiftUtils {
	
	private SwiftUtils () { super () ;} ;
	
	public static final String separator = "/" ; 
	public static final String directoryContentType = "application/directory" ;
	public static final String segmentsContainerPostfix = "_segments" ;
	
	public static boolean isDirectory (StoredObject so)
	{
		if (so == null || !so.exists())
			return false ;
		return directoryContentType.equalsIgnoreCase(so.getContentType()) ;
	}

	public static String getParentDirectory(StoredObject parentObject)
	{
		String parentDir = "" ;
		if (parentObject != null)
		{
			if (parentObject.exists())
			{
				if (directoryContentType.equalsIgnoreCase(parentObject.getContentType()))
				{
					parentDir = parentObject.getName() + separator ;
				}
				else
				{
					String name = parentObject.getName() ;
					int index = name.lastIndexOf(separator) ;
					if (index > 0)
					{
						parentDir = name.substring(0, index + 1) ;
					}
				}
			}
			else
				parentDir = parentObject.getName() + separator ;
		}
		return parentDir ;
	}

}
