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

import java.awt.CardLayout;
import java.io.ByteArrayInputStream;

import javax.swing.JPanel;

/**
 * PreviewPanel renders a preview of the stored object (if possible).
 * @author E.Hooijmeijer
 */
public class PreviewPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/**
     * A component that can display a preview.
     */
    public interface PreviewComponent {
        /**
         * @param type the content type.
         * @return true if the given content type is supported.
         */
        boolean supports(String type);

        /**
         * displays the preview.
         * @param contentType the content type.
         * @param in the raw data.
         */
        void displayPreview(String contentType, ByteArrayInputStream in);
    }

    private CardLayout cardLayout;
    private ImagePanel imagePanel = new ImagePanel();
    private PlainTextPanel textPanel = new PlainTextPanel();
    private PdfPanel pdfPanel = new PdfPanel () ;

    public PreviewPanel() {
        super();
        cardLayout = new CardLayout();
        setLayout(cardLayout);
        //
        this.add(imagePanel, "image");
        this.add(textPanel, "text");
        this.add(pdfPanel, "pdf");
        this.add(new JPanel(), "none");
        //
    }

    /**
     * previews the data as the given content type. 
     * @param contentType the content type.
     * @param in the data.
     */
    public void preview(String contentType, byte[] in) 
    {
        if (imagePanel.supports(contentType)) 
        {
            imagePanel.displayPreview(contentType, new ByteArrayInputStream(in));
            cardLayout.show(this, "image");
        } 
        else if (textPanel.supports(contentType)) 
        {
            textPanel.displayPreview(contentType, new ByteArrayInputStream(in));
            cardLayout.show(this, "text");
        }        
        else if (pdfPanel.supports(contentType)) 
        {
        	pdfPanel.displayPreview(contentType, new ByteArrayInputStream(in));
            cardLayout.show(this, "pdf");
        }  
        else 
        {
            cardLayout.show(this, "none");
        }
    }

}
