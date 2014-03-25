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

package org.swiftexplorer.gui;

import com.apple.mrj.MRJAboutHandler;
import com.apple.mrj.MRJApplicationUtils;
import com.apple.mrj.MRJPrefsHandler;
import com.apple.mrj.MRJQuitHandler;

// TODO: needless to detail what must be done here...

// http://www.kfu.com/~nsayer/Java/reflection.html
@SuppressWarnings("deprecation")
class MacOsMenuHandler implements MRJQuitHandler, MRJPrefsHandler, MRJAboutHandler {
	
	private final MainPanel cp;

	public MacOsMenuHandler(MainPanel theProgram) {
		cp = theProgram;
		MRJApplicationUtils.registerAboutHandler(this);
		MRJApplicationUtils.registerPrefsHandler(this);
		MRJApplicationUtils.registerQuitHandler(this);
	}

	@Override
	public void handleAbout() {
		cp.onAbout() ;
	}

	@Override
	public void handlePrefs() throws IllegalStateException {
		cp.onPreferences() ;
	}

	@Override
	public void handleQuit() throws IllegalStateException {
		cp.onQuit() ;
	}
}
