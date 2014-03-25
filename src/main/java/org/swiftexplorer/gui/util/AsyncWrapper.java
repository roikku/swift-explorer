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

import org.swiftexplorer.swift.operations.SwiftOperations;
import org.swiftexplorer.swift.operations.SwiftOperations.SwiftCallback;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.javaswift.joss.exception.CommandException;

/**
 * AsyncWrapper, wraps cloudie operations to make it asynchronous.
 * @author E.Hooijmeijer
 */
public class AsyncWrapper {

    public static SwiftOperations async(final SwiftOperations target) {
        return (SwiftOperations) Proxy.newProxyInstance(AsyncWrapper.class.getClassLoader(), new Class[] { SwiftOperations.class },
                new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
                        final SwiftCallback cb = (SwiftCallback) args[args.length - 1];
                        if (cb == null) {
                            throw new IllegalArgumentException("Callback must not be null.");
                        }
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    cb.onStart();
                                    method.invoke(target, args);
                                } catch (InvocationTargetException ex) {
                                    if (ex.getCause() instanceof CommandException) {
                                        cb.onError((CommandException) ex.getCause());
                                    } else {
                                        throw new RuntimeException(ex.getCause());
                                    }
                                } catch (Exception ex) {
                                    throw new RuntimeException(ex);
                                } finally {
                                    cb.onDone();
                                }
                            }
                        }).start();
                        return null;
                    }
                });
    }

}
