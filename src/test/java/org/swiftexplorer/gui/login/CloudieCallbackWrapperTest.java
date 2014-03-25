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

import java.util.ArrayList;



import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import org.junit.Test;
import org.mockito.Mockito;

public class CloudieCallbackWrapperTest {

    @Test
    public void shouldWrap() {
        Container container = Mockito.mock(Container.class);
        StoredObject storedObject = Mockito.mock(StoredObject.class);
        SwiftCallback mock = Mockito.mock(SwiftCallback.class);
        CloudieCallbackWrapper wrap = new CloudieCallbackWrapper(mock);
        //
        wrap.onContainerUpdate(container);
        Mockito.verify(mock).onContainerUpdate(container);
        //
        wrap.onDone();
        Mockito.verify(mock).onDone();
        //
        CommandException ex = new CommandException("msg");
        wrap.onError(ex);
        Mockito.verify(mock).onError(ex);
        //
        wrap.onLoginSuccess();
        Mockito.verify(mock).onLoginSuccess();
        //
        wrap.onLogoutSuccess();
        Mockito.verify(mock).onLogoutSuccess();
        //
        wrap.onStart();
        Mockito.verify(mock).onStart();
        //
        wrap.onStoredObjectUpdate(storedObject);
        Mockito.verify(mock).onStoredObjectUpdate(storedObject);
        //
        ArrayList<Container> containers = new ArrayList<Container>();
        wrap.onUpdateContainers(containers);
        Mockito.verify(mock).onUpdateContainers(containers);
        //
    }
}
