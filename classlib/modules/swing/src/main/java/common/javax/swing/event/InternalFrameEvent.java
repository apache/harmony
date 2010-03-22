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

/**
 * @author Vadim L. Bogdanov
 */

package javax.swing.event;

import java.awt.AWTEvent;

import javax.swing.JInternalFrame;

public class InternalFrameEvent extends AWTEvent {
    public static final int INTERNAL_FRAME_ACTIVATED = 25554;

    public static final int INTERNAL_FRAME_CLOSED = 25551;

    public static final int INTERNAL_FRAME_CLOSING = 25550;

    public static final int INTERNAL_FRAME_DEACTIVATED = 25555;

    public static final int INTERNAL_FRAME_DEICONIFIED = 25553;

    public static final int INTERNAL_FRAME_FIRST = 25549;

    public static final int INTERNAL_FRAME_ICONIFIED = 25552;

    public static final int INTERNAL_FRAME_LAST = 25555;

    public static final int INTERNAL_FRAME_OPENED =  25549;

    public InternalFrameEvent(final JInternalFrame source, final int id) {
        super(source, id);
    }

    public JInternalFrame getInternalFrame() {
        return (JInternalFrame)getSource();
    }

    public String paramString() {
        return super.paramString();
    }
}
