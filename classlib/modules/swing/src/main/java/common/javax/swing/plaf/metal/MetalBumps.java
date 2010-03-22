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
 * @author Vadim Bogdanov
 */
package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

final class MetalBumps {
    public static void paintBumps(final Graphics g, final Rectangle bounds,
                                  final Color highlight, final Color shadow) {
        paintBumps(g, bounds.x, bounds.y, bounds.width, bounds.height,
                   highlight, shadow);
    }

    public static void paintBumps(final Graphics g, final int x, final int y,
                                  final int w, final int h,
                                  final Color highlight, final Color shadow) {
        if (w <= 0 || h <= 0) {
            return;
        }

        Shape oldClip = g.getClip();
        g.clipRect(x, y, w, h);
        g.translate(x, y);
        Color oldColor = g.getColor();

        paintLines(g, 0, w, h, highlight);
        paintLines(g, 2, w, h, shadow);

        g.setColor(oldColor);
        g.translate(-x, -y);
        g.setClip(oldClip);
    }

    private static void paintLines(final Graphics g, final int offset,
                                   final int w, final int h,
                                   final Color color) {
        g.setColor(color);
        for (int i = offset; i <= h + w; i += 7) {
            g.drawLine(0, i, w, i - w);
        }
    }
}
