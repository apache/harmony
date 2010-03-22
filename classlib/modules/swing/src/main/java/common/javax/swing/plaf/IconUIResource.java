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

package javax.swing.plaf;

import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;
import javax.swing.Icon;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class IconUIResource implements Icon, UIResource, Serializable {
    private Icon icon;

    public IconUIResource(final Icon icon) {
        if (icon == null) {
            throw new IllegalArgumentException(Messages.getString("swing.6B")); //$NON-NLS-1$
        }
        this.icon = icon;
    }

    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
        icon.paintIcon(c, g, x, y);
    }

    public int getIconWidth() {
        return icon.getIconWidth();
    }

    public int getIconHeight() {
        return icon.getIconHeight();
    }
}


