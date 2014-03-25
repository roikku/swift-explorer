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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PopupTriggerTest {

    public class Target {
        private int cnt;
        private boolean fail;

        public Target(boolean fail) {
            this.fail = fail;
        }

        public void enableDisable() {
            cnt++;
            if (fail) {
                throw new RuntimeException("fail==true");
            }
        }

        public int getCnt() {
            return cnt;
        }
    }

    private JPopupMenu popup;
    private JComponent component;

    @Before
    public void init() {
        popup = Mockito.mock(JPopupMenu.class);
        component = Mockito.mock(JComponent.class);
    }

    @Test
    public void shouldPopup() {
        Target target = new Target(false);
        PopupTrigger<Target> trigger = new PopupTrigger<Target>(popup, target, "enableDisable");
        //
        trigger.mousePressed(new MouseEvent(component, 1, 0, 0, 0, 0, 0, 0, 1, true, 0));
        //
        assertEquals(1, target.getCnt());
        Mockito.verify(popup).show(component, 0, 0);
    }

    @Test
    public void shouldNotPopup() {
        Target target = new Target(false);
        PopupTrigger<Target> trigger = new PopupTrigger<Target>(popup, target, "enableDisable");
        //
        trigger.mousePressed(new MouseEvent(component, 1, 0, 0, 0, 0, 0, 0, 1, false, 0));
        trigger.mouseReleased(new MouseEvent(component, 1, 0, 0, 0, 0, 0, 0, 1, false, 0));
        //
        assertEquals(0, target.getCnt());
        Mockito.verify(popup, Mockito.never()).show(component, 0, 0);

    }

    @Test
    public void shouldPopupWithException() {
        Target target = new Target(true);
        PopupTrigger<Target> trigger = new PopupTrigger<Target>(popup, target, "enableDisable");
        //
        try {
            trigger.mouseReleased(new MouseEvent(component, 1, 0, 0, 0, 0, 0, 0, 1, true, 0));
            fail();
        } catch (RuntimeException ex) {
            assertEquals(1, target.getCnt());
        }
        //
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailOnBadMethod() {
        Target target = new Target(false);
        new PopupTrigger<Target>(popup, target, "enableDisableXXXX");
    }

}
