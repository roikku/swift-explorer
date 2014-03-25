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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

/**
 * PopupTrigger that first calls an enable disable method to enable and disable the actions in the menu.
 * 
 * @author E.Hooijmeijer
 */
public class PopupTrigger<A> extends MouseAdapter {

    private final JPopupMenu popupMenu;
    private final A instance;
    private Method method;

    /**
     * creates a new popup trigger.
     * 
     * @param popupMenu the popup menu trigger.
     * @param instance the instance to invoke enable/disable on.
     * @param enableDisableMethod the name of the enable/disable method.
     */
    public PopupTrigger(JPopupMenu popupMenu, A instance, String enableDisableMethod) {
        this.popupMenu = popupMenu;
        this.instance = instance;
        this.method = find(instance, enableDisableMethod);
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

    private void enableDisable() {
        try {
            method.invoke(instance, new Object[0]);
        } catch (Exception ex) {
            throw new RuntimeException("Enable/Disable '" + method + "' failed", ex);
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    /**
     * {@inheritDoc}.
     */
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            JComponent comp = (JComponent) e.getComponent();
            enableDisable();
            popupMenu.show(comp, e.getX(), e.getY());
        }
    }

}
