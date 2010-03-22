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
 * @author Sergey Burlak
 */
package javax.swing.event;

import java.awt.Component;
import java.awt.event.KeyEvent;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;

public class MenuKeyEvent extends KeyEvent {

    private final MenuSelectionManager manager;
    private final MenuElement[] path;

    public MenuKeyEvent(final Component src, final int id, final long when, final int modifiers, final int keyCode,
            final char keyChar, final MenuElement[] p, final MenuSelectionManager m)
    {
        super(src, id, when, modifiers, keyCode, keyChar);
        path = p;
        manager = m;
    }

    public MenuElement[] getPath() {
        return path;
    }

    public MenuSelectionManager getMenuSelectionManager() {
        return manager;
    }
}

