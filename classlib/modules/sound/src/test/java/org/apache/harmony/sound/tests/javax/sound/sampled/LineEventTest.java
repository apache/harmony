/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.sound.tests.javax.sound.sampled;

import javax.sound.sampled.Control;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;

import junit.framework.TestCase;

public class LineEventTest extends TestCase {

    public void testLineEvent() {

        Line line = new MyLine();
        LineEvent le = new LineEvent(line, LineEvent.Type.CLOSE, 1l);
        assertEquals(line, le.getSource());
        assertEquals(1, le.getFramePosition());
        assertEquals(line, le.getLine());
        assertEquals(LineEvent.Type.CLOSE, le.getType());
        assertEquals("Close event from line MyLine", le.toString());

    }

    private class MyLine implements Line {

        public MyLine() {
        }

        public void addLineListener(LineListener listener) {
        }

        public void close() {
        }

        public Control getControl(Control.Type control) {
            return null;
        }

        public Control[] getControls() {
            return null;
        }

        public Line.Info getLineInfo() {
            return null;
        }

        public boolean isControlSupported(Control.Type control) {
            return false;
        }

        public boolean isOpen() {
            return false;
        }

        public void open() throws LineUnavailableException {
        }

        public void removeLineListener(LineListener listener) {
        }

        public String toString() {
            return "MyLine";
        }
    }
}