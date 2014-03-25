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

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;

import org.junit.Before;
import org.junit.Test;

/**
 * DoubleClickListenerTest.
 * @author E.Hooijmeijer
 */
public class DoubleClickListenerTest {

    private DoubleClickListener listener;
    private Action action;
    private int cnt = 0;
    private boolean enabled = true;

    @Before
    public void init() {
        action = new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
            public void actionPerformed(ActionEvent e) {
                cnt++;
            }

            public boolean isEnabled() {
                return DoubleClickListenerTest.this.enabled;
            };
        };
        listener = new DoubleClickListener(action);
    }

    @Test
    public void shouldTrigger() {
        listener.mouseClicked(new MouseEvent(new JLabel(), 1, 0L, 0, 0, 0, 0, 0, 2, false, MouseEvent.BUTTON1));
        assertEquals(1, cnt);
    }

    @Test
    public void shouldNotTriggerOnSingleClick() {
        listener.mouseClicked(new MouseEvent(new JLabel(), 1, 0L, 0, 0, 0, 0, 0, 1, false, MouseEvent.BUTTON1));
        assertEquals(0, cnt);
    }

    @Test
    public void shouldNotTriggerOnOtherMouseButton() {
        listener.mouseClicked(new MouseEvent(new JLabel(), 1, 0L, 0, 0, 0, 0, 0, 2, false, MouseEvent.BUTTON3));
        assertEquals(0, cnt);
    }

    @Test
    public void shouldNotTriggerIfActionDisbled() {
        enabled = false;
        listener.mouseClicked(new MouseEvent(new JLabel(), 1, 0L, 0, 0, 0, 0, 0, 2, false, MouseEvent.BUTTON1));
        assertEquals(0, cnt);
    }

}
