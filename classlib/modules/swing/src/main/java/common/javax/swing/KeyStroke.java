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
 * @author Alexander T. Simbirtsev
 */
package javax.swing;

import java.awt.AWTKeyStroke;
import java.awt.event.KeyEvent;
import java.io.Serializable;

import org.apache.harmony.x.swing.Utilities;


public class KeyStroke extends AWTKeyStroke implements Serializable {

    static {
        AWTKeyStroke.registerSubclass(KeyStroke.class);
    }

    private KeyStroke() {
        super();
    }

    private KeyStroke(final char keyChar, final int keyCode, final int modifiers, final boolean onKeyRelease){
        super(keyChar, keyCode, modifiers, onKeyRelease);
    }

    public static KeyStroke getKeyStroke(final String str) {
        if (Utilities.isEmptyString(str)) {
            return null;
        }
        synchronized(AWTKeyStroke.class) {
            try {
                return (KeyStroke)AWTKeyStroke.getAWTKeyStroke(str);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public static KeyStroke getKeyStroke(final Character keyChar, final int modifiers) {
        synchronized(AWTKeyStroke.class) {
            return (KeyStroke)AWTKeyStroke.getAWTKeyStroke(keyChar, modifiers);
        }
    }

    public static KeyStroke getKeyStrokeForEvent(final KeyEvent event) {
        synchronized(AWTKeyStroke.class) {
            return (KeyStroke)AWTKeyStroke.getAWTKeyStrokeForEvent(event);
        }
    }

    public static KeyStroke getKeyStroke(final int keyCode, final int modifiers, final boolean onKeyRelease) {
        synchronized(AWTKeyStroke.class) {
            return (KeyStroke)AWTKeyStroke.getAWTKeyStroke(keyCode, modifiers, onKeyRelease);
        }
    }

    public static KeyStroke getKeyStroke(final int keyCode, final int modifiers) {
        synchronized(AWTKeyStroke.class) {
            return (KeyStroke)AWTKeyStroke.getAWTKeyStroke(keyCode, modifiers);
        }
    }

    /**
     * @deprecated <i>use getKeyStroke(char)</i>
     */
    public static KeyStroke getKeyStroke(final char keyChar, final boolean onKeyRelease) {
        return new KeyStroke(keyChar, 0, 0, onKeyRelease);
    }

    public static KeyStroke getKeyStroke(final char keyChar) {
        synchronized(AWTKeyStroke.class) {
            return (KeyStroke)AWTKeyStroke.getAWTKeyStroke(keyChar);
        }
    }
}
