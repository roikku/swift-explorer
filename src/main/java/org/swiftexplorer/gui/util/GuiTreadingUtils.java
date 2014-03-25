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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.swing.SwingUtilities;

/**
 * Gui Threading utilities.
 * @author E. Hooijmeier
 */
public class GuiTreadingUtils {

    /**
     * wraps the given instance and makes sure that the methods invoked are executed on the GUI Thread.
     * @param type the type to invoke on.
     * @param target the instance to invoke on.
     * @return a wrapped instance.
     */
    @SuppressWarnings("unchecked")
    public static <A> A guiThreadSafe(Class<A> type, final A target) {
        return (A) Proxy.newProxyInstance(GuiTreadingUtils.class.getClassLoader(), new Class[] { type }, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            method.invoke(target, args);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                return null;
            }
        });

    }

}
