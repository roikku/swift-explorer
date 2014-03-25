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
package org.swiftexplorer.gui.preview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.gui.preview.PreviewPanel.PreviewComponent;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.swing.JPanel;
import javax.swing.JTextArea;




import org.apache.commons.io.IOUtils;

/**
 * PlainTextPanel renders plain text or html text on a panel.
 * @author E.Hooijmeijer
 */
public class PlainTextPanel extends JPanel implements PreviewComponent {

	private static final long serialVersionUID = 1L;

	private final Logger logger = LoggerFactory.getLogger(PlainTextPanel.class);
	
	private JTextArea area;

    public PlainTextPanel() {
        super(new BorderLayout());
        area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 10));
        this.add(area);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean supports(String type) {
        return "text/plain".equals(type) || "text/html".equals(type);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void displayPreview(String contentType, ByteArrayInputStream in) {
        try {
            try {
                StringBuilder sb = new StringBuilder();
                for (String line : IOUtils.readLines(in)) {
                    sb.append(line);
                    sb.append(System.getProperty("line.separator"));
                }
                area.setText(sb.toString());
            } finally {
                in.close();
            }
        } catch (IOException e) {
            area.setText("");
            logger.error("Error occurred while previewing text", e);
        }
    }
}
