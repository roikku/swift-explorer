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

package org.swiftexplorer.gui.util;

import java.awt.Frame;

public class SwingUtils {
	
	private SwingUtils () { super () ;} 
	
	public static Frame tryFindSuitableFrameOwner ()
	{
		Frame owner = null;
		// find a suitable owner, if any
		Frame[] allFrames = Frame.getFrames();
		if (allFrames != null) {
			for (Frame frame : allFrames) {
				if (frame == null)
					continue;
				if (!frame.isShowing())
					continue;
				if (!frame.isActive())
					continue;
				owner = frame;
				break;
			}
		}
		return owner ;
	}
}
