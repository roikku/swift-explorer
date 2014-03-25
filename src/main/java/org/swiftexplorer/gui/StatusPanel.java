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

/*
*
* This file has been slightly modified by Loic Merckel
* (namely, the function "byteSize" has been rewritten)
* 
* The original version can  be found here:
*
*  https://github.com/javaswift/cloudie
*  package (src/main/java): org.javaswift.cloudie;
*  
*/


package org.swiftexplorer.gui;

import org.swiftexplorer.swift.operations.SwiftOperations;
import org.swiftexplorer.swift.operations.SwiftOperations.SwiftCallback;
import org.swiftexplorer.util.FileUtils;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;

/**
 * Status panel is the bottom part of the user interface. It shows details about
 * the current selected container, the current selected Stored Objects and the
 * busy status.
 * @author E.Hooijmeijer
 */
public class StatusPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private JLabel leftLabel;
    private JLabel callCountLabel;
    private JLabel rightLabel;
    private JLabel busyLabel;
    private Icon busy = getIcon("weather_clouds.png", "busy");
    private Icon ready = getIcon("weather_sun.png", "ready");
    private Icon lock = getIcon("lock.png", "private");
    private Icon lockOpen = getIcon("lock_open.png", "public");
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private SwiftOperations ops;
    private SwiftCallback callback;

    public StatusPanel(SwiftOperations ops, SwiftCallback callback) {
        super(new BorderLayout(0, 0));
        this.ops = ops;
        this.callback = callback;
        Box left = Box.createHorizontalBox();
        Box center = Box.createHorizontalBox();
        Box right = Box.createHorizontalBox();
        left.setBorder(BorderFactory.createEtchedBorder());
        right.setBorder(BorderFactory.createEtchedBorder());
        center.setBorder(BorderFactory.createEtchedBorder());
        center.setPreferredSize(new Dimension(320, 24));
        left.setPreferredSize(new Dimension(256, 24));
        //
        busyLabel = new JLabel();
        busyLabel.setPreferredSize(new Dimension(18, 18));
        right.add(busyLabel);
        //
        callCountLabel = new JLabel();
        callCountLabel.setPreferredSize(new Dimension(64, 18));
        right.add(Box.createHorizontalStrut(4));
        right.add(callCountLabel);
        right.add(Box.createHorizontalStrut(4));
        //
        this.add(left, BorderLayout.WEST);
        this.add(center, BorderLayout.CENTER);
        this.add(right, BorderLayout.EAST);
        //
        leftLabel = new JLabel("");
        left.add(Box.createHorizontalStrut(4));
        left.add(leftLabel);
        left.add(Box.createHorizontalStrut(8));
        //
        rightLabel = new JLabel("");
        center.add(Box.createHorizontalStrut(8));
        center.add(rightLabel);
        center.add(Box.createHorizontalStrut(4));
    }

	private Icon getIcon(String string, String desc) {
        return new ImageIcon(getClass().getResource("/icons/" + string), desc);
    }

    public void onNumberOfCalls(int nrOfCalls) {
        callCountLabel.setText(String.valueOf(nrOfCalls));
    }

    public void onSelectContainer(Container c) {
        if (c.isInfoRetrieved()) {
            leftLabel.setIcon(c.isPublic() ? lockOpen : lock);
            leftLabel.setText((c.isPublic() ? "public, " : "private,") + " " + c.getCount() + " objects, " + byteSize(c.getBytesUsed(), 0) + "");
        } else {
            leftLabel.setIcon(null);
            leftLabel.setText("");
            ops.getMetadata(c, callback);
        }
    }

    private String byteSize(long bytesUsed, int cnt) {
    	return FileUtils.humanReadableByteCount(bytesUsed, true) ;
    	/*
        if (bytesUsed > 1024) {
            return byteSize(bytesUsed / 1024, cnt + 1);
        }
        switch (cnt) {
        case 0:
            return bytesUsed + " bytes";
        case 1:
            return bytesUsed + " KB";
        case 2:
            return bytesUsed + " MB";
        case 3:
            return bytesUsed + " GB";
        case 4:
            return bytesUsed + " TB";
        case 5:
            return bytesUsed + " PB";
        default:
            return "Too Many Bytes.";
        }
    	 */
    }

    public void onSelectStoredObjects(List<StoredObject> selection) {
        if (selection.size() == 1) {
            StoredObject obj = selection.get(0);
            rightLabel.setText(byteSize(obj.getContentLength(), 0) + ", " + format(obj.getLastModifiedAsDate()) + ", " + obj.getContentType());
            //
            if (!obj.isInfoRetrieved()) {
                // Trigger metadata anyway as menu's depend on it..
                ops.getMetadata(obj, callback);
            }
        } else {
            long sum = 0;
            for (StoredObject obj : selection) {
                sum += obj.getContentLength();
            }
            rightLabel.setText("Selected " + selection.size() + " objects, " + byteSize(sum, 0) + " in total. ");
        }
    }

    private String format(Date date) {
        return sdf.format(date);
    }

    public void onDeselect() {
        onDeselectStoredObject();
        onDeselectContainer();
    }

    public void onDeselectStoredObject() {
        rightLabel.setText("");
    }

    public void onDeselectContainer() {
        leftLabel.setText("");
    }

    public void onStart() {
        busyLabel.setIcon(busy);
    }

    public void onEnd() {
        busyLabel.setIcon(ready);
    }
}
