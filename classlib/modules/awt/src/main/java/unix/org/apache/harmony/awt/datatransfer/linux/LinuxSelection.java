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
 * @author Michael Danilov
 */
package org.apache.harmony.awt.datatransfer.linux;

import org.apache.harmony.awt.ContextStorage;
import org.apache.harmony.awt.datatransfer.NativeClipboard;
import org.apache.harmony.awt.nativebridge.linux.X11.XEvent;
import org.apache.harmony.awt.wtk.linux.LinuxEventQueue;
import org.apache.harmony.awt.wtk.linux.LinuxWindowFactory;

public final class LinuxSelection extends NativeClipboard 
        implements LinuxEventQueue.Preprocessor {

    private final LinuxWindowFactory factory;

    private final long xaSelection;
    private final long xaTargets;
    private final long xaMultiple;
    private final long xaText;
    private final long xaUTF8;
    private final long xaSTRING;

    public LinuxSelection(String selection) {
        super("System"); //$NON-NLS-1$

        factory = (LinuxWindowFactory)ContextStorage.getWindowFactory();

        xaSelection = factory.internAtom(selection);
        xaTargets = factory.internAtom("TARGETS"); //$NON-NLS-1$
        xaMultiple = factory.internAtom("MULTIPLE"); //$NON-NLS-1$
        xaText = factory.internAtom("TEXT"); //$NON-NLS-1$
        xaUTF8 = factory.internAtom("UTF8_STRING"); //$NON-NLS-1$
        xaSTRING = factory.internAtom("STRING"); //$NON-NLS-1$
    }

    public boolean preprocess(XEvent event) {
        return false;
    }

}
