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
package org.swiftexplorer.gui;

import org.swiftexplorer.swift.operations.SwiftOperations;
import org.swiftexplorer.swift.operations.SwiftOperations.SwiftCallback;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * StatusPaneltest.
 * @author E.Hooijmeijer
 * 
 */
public class StatusPanelTest {

    private SwiftOperations ops;
    private SwiftCallback callback;

    @Before
    public void init() {
        ops = Mockito.mock(SwiftOperations.class);
        callback = Mockito.mock(SwiftCallback.class);
    }

    @Test
    public void shouldCreate() {
        new StatusPanel(ops, callback);
    }

    @Test
    public void shouldRefreshContainer() {
        Container mock = Mockito.mock(Container.class);
        Mockito.when(mock.isInfoRetrieved()).thenReturn(Boolean.FALSE);
        //
        StatusPanel statusPanel = new StatusPanel(ops, callback);
        statusPanel.onSelectContainer(mock);
        //
        Mockito.verify(ops).getMetadata(mock, callback);
    }

    @Test
    public void shouldNotRefreshContainer() {
        Container mock = Mockito.mock(Container.class);
        Mockito.when(mock.isInfoRetrieved()).thenReturn(Boolean.TRUE);
        //
        StatusPanel statusPanel = new StatusPanel(ops, callback);
        statusPanel.onSelectContainer(mock);
        //
        Mockito.verify(ops, Mockito.never()).getMetadata(mock, callback);
    }

    @Test
    public void shouldUpdateStoredObjects() {
        List<StoredObject> obj = new ArrayList<StoredObject>();
        obj.add(Mockito.mock(StoredObject.class));
        obj.add(Mockito.mock(StoredObject.class));
        //
        StatusPanel statusPanel = new StatusPanel(ops, callback);
        statusPanel.onSelectStoredObjects(obj);
        //
    }

    @Test
    public void shouldRefreshStoredObject() {
        StoredObject mock = Mockito.mock(StoredObject.class);
        Mockito.when(mock.isInfoRetrieved()).thenReturn(Boolean.FALSE);
        Mockito.when(mock.getLastModifiedAsDate()).thenReturn(new Date());
        List<StoredObject> obj = new ArrayList<StoredObject>();
        obj.add(mock);
        //
        StatusPanel statusPanel = new StatusPanel(ops, callback);
        statusPanel.onSelectStoredObjects(obj);
        //
        Mockito.verify(ops).getMetadata(mock, callback);
    }

    @Test
    public void shouldNotRefreshStoredObject() {
        StoredObject mock = Mockito.mock(StoredObject.class);
        Mockito.when(mock.isInfoRetrieved()).thenReturn(Boolean.TRUE);
        Mockito.when(mock.getLastModifiedAsDate()).thenReturn(new Date());
        List<StoredObject> obj = new ArrayList<StoredObject>();
        obj.add(mock);
        //
        StatusPanel statusPanel = new StatusPanel(ops, callback);
        statusPanel.onSelectStoredObjects(obj);
        //
        Mockito.verify(ops, Mockito.never()).getMetadata(mock, callback);
    }

    @Test
    public void shouldDeselect() {
        StatusPanel statusPanel = new StatusPanel(ops, callback);
        statusPanel.onDeselect();
    }

    @Test
    public void shouldStartStop() {
        StatusPanel statusPanel = new StatusPanel(ops, callback);
        statusPanel.onStart();
        statusPanel.onEnd();
    }

}
