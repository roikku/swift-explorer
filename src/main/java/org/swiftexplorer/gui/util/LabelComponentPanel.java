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

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Useful composite panel.
 * @author E. Hooijmeier
 */
public class LabelComponentPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public LabelComponentPanel(String label, JComponent comp) {
        super(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        JLabel lbl = new JLabel(label, JLabel.RIGHT);
        lbl.setLabelFor(comp);
        lbl.setPreferredSize(new Dimension(170, 24));
        this.add(lbl, BorderLayout.WEST);
        this.add(comp, BorderLayout.CENTER);
    }

    public LabelComponentPanel(String label, JComponent comp, JComponent right) {
        this(label, comp);
        this.add(right, BorderLayout.EAST);
    }

}
