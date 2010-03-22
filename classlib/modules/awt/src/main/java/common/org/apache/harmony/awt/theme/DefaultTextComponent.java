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
 * @author Dmitry A. Durnev
 */
package org.apache.harmony.awt.theme;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.SystemColor;

import org.apache.harmony.awt.state.TextComponentState;


public class DefaultTextComponent extends DefaultStyle {    

    public static void drawBackground(Graphics g, TextComponentState s) {        
        g.setColor(s.isEnabled() ? s.getBackground() : SystemColor.control);
        Rectangle client = s.getClient();
        Dimension size = s.getSize();
        g.fillRect(client.x, client.y, client.width, client.height);
        Insets ins = s.getInsets();
        int x = client.x + client.width;
        int y = client.y + client.height;
        int w = size.width - x;
        int h = size.height - y;
        // fill areas outside of client area &&
        // scrollbars
        g.fillRect(x, y, w, h);
        g.fillRect(0, size.height - ins.bottom, size.width, ins.bottom);
        g.fillRect(size.width - ins.right, 0, ins.right, size.height);

        DefaultButton.drawButtonFrame(g, new Rectangle(size), true);
    }
}
