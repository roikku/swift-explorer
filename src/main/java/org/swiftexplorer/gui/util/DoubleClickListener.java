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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;

/**
 * DoubleClickListener.
 * @author E.Hooijmeijer
 *
 */
public class DoubleClickListener extends MouseAdapter {

    private Action action;

    public DoubleClickListener(Action action) {
        this.action = action;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
            if (action.isEnabled()) {
                action.actionPerformed(new ActionEvent(e.getSource(), e.getID(), "DoubleClicked"));
            }
        }
    }
}
