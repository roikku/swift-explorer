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
package org.swiftexplorer.gui.util;

import java.awt.event.ActionEvent;
import java.lang.reflect.Method;

import javax.swing.AbstractAction;
import javax.swing.Icon;

/**
 * ActionListener that delegates to a no-args method.
 * 
 * @author E.Hooijmeijer
 * @param <A> the type to invoke the method on.
 */
public class ReflectionAction<A> extends AbstractAction {

	private static final long serialVersionUID = 1L;

	private final Method method;
    private final A instance;

    /**
     * creates a new action that invokes on the given method.
     * @param name the name of the action.
     * @param instance the instance to invoke the method on.
     * @param method the method.
     */
    public ReflectionAction(String name, A instance, String method) {
        super(name);
        this.instance = instance;
        this.method = find(instance, method);
    }

    /**
     * creates a new action that invokes on the given method.
     * @param name the name of the action.
     * @param icon the icon of the action.
     * @param instance the instance to invoke the method on.
     * @param method the method.
     */
    public ReflectionAction(String name, Icon icon, A instance, String method) {
        super(name, icon);
        this.instance = instance;
        this.method = find(instance, method);
    }

    private Method find(A obj, String name) {
        for (Method m : obj.getClass().getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                m.setAccessible(true);
                return m;
            }
        }
        throw new IllegalArgumentException("Missing method " + name + " in " + obj);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            method.invoke(instance, new Object[0]);
        } catch (Exception ex) {
            throw new RuntimeException("Action " + method + " failed", ex);
        }
    }
}
