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

import java.io.File;
import java.io.IOException;


import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * PreviewPanelTest.
 * @author E.Hooijmeijer
 */
public class PreviewPanelTest {

    private PreviewPanel pp;

    @Before
    public void init() {
        pp = new PreviewPanel();
    }

    @Test
    public void shouldPreviewText() {
        pp.preview("text/plain", "Hello World".getBytes());
        pp.preview("text/html", "Hello World".getBytes());
    }

    @Test
    public void shouldPreviewImage() throws IOException {
        pp.preview("image/png", FileUtils.readFileToByteArray(new File("./src/main/resources/icons/zoom.png")));
        pp.preview("image/jpeg", FileUtils.readFileToByteArray(new File("./src/main/resources/icons/zoom.png")));
        pp.preview("image/gif", FileUtils.readFileToByteArray(new File("./src/main/resources/icons/zoom.png")));
    }

    @Test
    public void shouldNotFailBadImage() {
        pp.preview("image/png", "BadImage".getBytes());
    }

    @Test
    public void shouldNotPreviewUnknown() {
        pp.preview("application/blahblahbla", "Hiaar".getBytes());
    }
}
