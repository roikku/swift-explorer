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
import static org.junit.Assert.fail;

import java.awt.event.ActionEvent;


import org.junit.Test;

public class ReflectionActionTest {

    public class Target {
        private int cnt;
        private boolean fail;

        public Target(boolean fail) {
            this.fail = fail;
        }

        public void onAction() {
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
    public void shouldInvokeAction() {
        Target target = new Target(false);
        ReflectionAction<Target> action = new ReflectionAction<ReflectionActionTest.Target>("action", target, "onAction");
        //
        action.actionPerformed(new ActionEvent(this, 1, "action"));
        //
        assertEquals(1, target.getCnt());
    }

    @Test
    public void shouldInvokeActionWithException() {
        Target target = new Target(true);
        ReflectionAction<Target> action = new ReflectionAction<ReflectionActionTest.Target>("action", null, target, "onAction");
        //
        try {
            action.actionPerformed(new ActionEvent(this, 1, "action"));
            fail();
        } catch (RuntimeException ex) {
            assertEquals(1, target.getCnt());
        }
        //
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailOnBadMethod() {
        new ReflectionAction<ReflectionActionTest.Target>("action", new Target(false), "onActionXXXX");
    }

}
