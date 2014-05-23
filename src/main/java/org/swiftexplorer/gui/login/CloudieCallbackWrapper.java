/*
 *  Copyright 2012-2013 E.Hooijmeijer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.swiftexplorer.gui.login;

import org.swiftexplorer.swift.operations.SwiftOperations.SwiftCallback;

import java.util.Collection;




import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;

/**
 * Wrapper for the CloudieCallback to allow interception of the callbacks.
 * @author E. Hooijmeijer
 */
public class CloudieCallbackWrapper implements SwiftCallback {

    private SwiftCallback target;

    public CloudieCallbackWrapper(SwiftCallback target) {
        this.target = target;
    }

    @Override
    public void onContainerUpdate(Container container) {
        this.target.onContainerUpdate(container);
    }

    @Override
    public void onDone() {
        this.target.onDone();
    }

    @Override
    public void onError(CommandException ex) {
        this.target.onError(ex);
    }

    @Override
    public void onLoginSuccess() {
        this.target.onLoginSuccess();
    }

    @Override
    public void onLogoutSuccess() {
        this.target.onLogoutSuccess();
    }

    @Override
    public void onStart() {
        this.target.onStart();
    }

    @Override
    public void onStoredObjectUpdate(StoredObject obj) {
        this.target.onStoredObjectUpdate(obj);

    }

    @Override
    public void onUpdateContainers(Collection<Container> containers) {
        this.target.onUpdateContainers(containers);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void onNumberOfCalls(int nrOfCalls) {
        this.target.onNumberOfCalls(nrOfCalls);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void onAppendStoredObjects(Container container, int page, Collection<StoredObject> storedObjects) {
        this.target.onAppendStoredObjects(container, page, storedObjects);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void onNewStoredObjects() {
        this.target.onNewStoredObjects();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void onStoredObjectDeleted(Container container, StoredObject storedObject) {
        this.target.onStoredObjectDeleted(container, storedObject);
    }

    
    /**
     * {@inheritDoc}.
     */
	@Override
	public void onProgress(double totalProgress, String totalMsg,
			double currentProgress, String currentMsg) {
		this.target.onProgress(totalProgress, totalMsg, currentProgress, currentMsg);
	}
	
	
    /**
     * {@inheritDoc}.
     */
	@Override
	public void onStopped() {
		this.target.onStopped();
	}

	
    /**
     * {@inheritDoc}.
     */
	@Override
	public void onStoredObjectDeleted(Container container,
			Collection<StoredObject> storedObjects) {
		this.target.onStoredObjectDeleted(container, storedObjects);
	}
}
