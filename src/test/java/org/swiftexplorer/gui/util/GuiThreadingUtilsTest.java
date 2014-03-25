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

import org.junit.Ignore;
import org.junit.Test;

public class GuiThreadingUtilsTest {

    public interface Subject {
        void doIt();
    }

    public static class Target implements Subject {
        private int cnt;
        private boolean fail;

        public Target(boolean fail) {
            this.fail = fail;
        }

        public void doIt() {
            cnt++;
            if (fail) {
                throw new RuntimeException("fail==true");
            }
        }

        public int getCnt() {
            return cnt;
        }
    }

    @Test
    public void shouldWrap() throws InterruptedException {
        Target target = new Target(false);
        GuiTreadingUtils.guiThreadSafe(Subject.class, target).doIt();
        Thread.sleep(100L);
        assertEquals(1, target.getCnt());
    }

    @Test
    @Ignore
    public void shouldWrapAndFail() throws InterruptedException {
        Target target = new Target(true);
        GuiTreadingUtils.guiThreadSafe(Subject.class, target).doIt();
        Thread.sleep(100L);
        assertEquals(1, target.getCnt());
    }

}
