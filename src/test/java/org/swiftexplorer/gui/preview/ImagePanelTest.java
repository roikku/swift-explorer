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
* can be found here:
*
*  https://github.com/javaswift/cloudie
*  package (src/test/java): org.javaswift.cloudie.preview;
*  
*  Very minor modifications were made to take into consideration the changes
*  in the painting method
*
*/

package org.swiftexplorer.gui.preview;

import static org.junit.Assert.assertEquals;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;


import org.junit.Test;
import org.mockito.Mockito;

public class ImagePanelTest {

    private Graphics graphics = Mockito.mock(Graphics.class);

    @Test
    public void shouldCreate() {
        ImagePanel panel = new ImagePanel();
        assertEquals(new Dimension(128, 128), panel.getMinimumSize());
    }

    @Test
    public void shouldPaintWithoutImage() {
        ImagePanel panel = new ImagePanel();
        panel.paintComponent(graphics);
        //
        Mockito.verify(graphics).fillRect(Mockito.eq(0), Mockito.eq(0), Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    public void shouldPaintWithImage() {
        ImagePanel panel = new ImagePanel();
        BufferedImage img = Mockito.mock(BufferedImage.class);
        panel.setImage(img);
        //
        panel.paintComponent(graphics);
        //
        Mockito.verify(graphics).drawImage(Mockito.eq(img), Mockito.eq(0), Mockito.eq(0), Mockito.anyInt(), Mockito.anyInt(), Mockito.eq(panel));
        //
        panel.clearImage();
        panel.paintComponent(graphics);
        //
        Mockito.verify(graphics, Mockito.atLeast(1)).fillRect(Mockito.eq(0), Mockito.eq(0), Mockito.anyInt(), Mockito.anyInt());
    }
}
