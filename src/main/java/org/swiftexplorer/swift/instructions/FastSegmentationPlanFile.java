/*
 * Copyright 2014 Loic Merckel
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

/* This file incorporates work covered by the following copyright and  
 * permission notice:  
 *  
 * Copyright 2013 Robert Bor
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Some function have been adapted from 
 * - Joss (http://joss.javaswift.org/), package org.javaswift.joss.instructions, class SegmentationPlanFile.java
 */

package org.swiftexplorer.swift.instructions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;

import org.apache.commons.io.input.BoundedInputStream;
import org.javaswift.joss.instructions.SegmentationPlan;

public class FastSegmentationPlanFile extends SegmentationPlan {

    private RandomAccessFile randomAccessFile;

    private Long fileLength;

    public FastSegmentationPlanFile(File file, long segmentationSize) throws IOException {
        super(segmentationSize);
        this.randomAccessFile = new RandomAccessFile(file, "r");
        this.fileLength = this.randomAccessFile.length();
    }

    @Override
    protected Long getFileLength() {
        return this.fileLength;
    }

    @Override
    protected InputStream createSegment() throws IOException {
    	InputStream res = new BoundedInputStream(Channels.newInputStream(this.randomAccessFile.getChannel().position(currentSegment * segmentationSize)), segmentationSize);
    	((BoundedInputStream) res).setPropagateClose(false) ;
    	return res ;                
    }

    @Override
    public void close() throws IOException {
        this.randomAccessFile.close();
    }
}

