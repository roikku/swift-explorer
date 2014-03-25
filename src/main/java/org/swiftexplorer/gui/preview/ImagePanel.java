/*
 * Copyright 2014 Loic Merckel
 * Copyright 2012-2013 E.Hooijmeijer
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

/*
*
* The original version of this file (i.e., the one that is copyrighted 2012-2013 E.Hooijmeijer) 
* can  be found here:
*
*  https://github.com/javaswift/cloudie
*  package (src/main/java): org.javaswift.cloudie.preview;
*  
*  This version has a different implementation of the method paintComponent
*
*/

package org.swiftexplorer.gui.preview;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.gui.preview.PreviewPanel.PreviewComponent;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;


/**
 * ImagePanel, renders a given image on a panel.
 * @author E.Hooijmeijer
 * @author Loic Merckel
 */
public class ImagePanel extends JPanel implements PreviewComponent {

	private static final long serialVersionUID = 1L;

	private final Logger logger = LoggerFactory.getLogger(ImagePanel.class);
	
	private volatile BufferedImage image;

    public ImagePanel() {
        super();
        setMinimumSize(new Dimension(128, 128));
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void displayPreview(String contentType, ByteArrayInputStream in) {
        try {
            setImage(ImageIO.read(in));
        } catch (IOException e) {
            clearImage();
            logger.error("Error occurred while previewing image", e);
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean supports(String type) {
        return "image/jpeg".equals(type) || "image/png".equals(type) || "image/gif".equals(type);
    }

    /**
     * @param image the image to set
     */
    public synchronized void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    public synchronized void clearImage() {
        this.image = null;
        repaint();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected synchronized void paintComponent(Graphics g) {
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (image != null) 
        {
        	Mode mode = Mode.AUTOMATIC ;
        	int maxSize = Math.min(this.getWidth(), this.getHeight()) ;
        	double dh = (double)image.getHeight() ;
        	if (dh > Double.MIN_VALUE)
        	{
        		double imageAspectRatio = (double)image.getWidth() / dh ;
	        	if (this.getHeight() * imageAspectRatio <=  this.getWidth())
	        	{
	        		maxSize = this.getHeight() ;
	        		mode = Mode.FIT_TO_HEIGHT ;
	        	}
	        	else
	        	{
	        		maxSize = this.getWidth() ;
	        		mode = Mode.FIT_TO_WIDTH ;
	        	}	
        	}
        	BufferedImage scaledImg = Scalr.resize(image, Method.AUTOMATIC, mode, maxSize, Scalr.OP_ANTIALIAS) ;  
            g.drawImage(scaledImg, 0, 0, scaledImg.getWidth(), scaledImg.getHeight(), this);
        }
    }

}
