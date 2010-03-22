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
 * @author Alexey A. Ivanov
 */
package javax.swing.text.html;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.text.Element;
import javax.swing.text.html.StyleSheet.ListPainter;

public class ListView extends BlockView {
    private ListPainter listPainter;

    public ListView(final Element element) {
        super(element, Y_AXIS);
    }

    public float getAlignment(final int axis) {
        return 0.5f;
    }

    public void paint(final Graphics g, final Shape allocation) {
        super.paint(g, allocation);
    }

    protected void paintChild(final Graphics g,
                              final Rectangle alloc,
                              final int index) {
        listPainter.paint(g,
                          alloc.x, alloc.y, alloc.width, alloc.height,
                          this, index);
        super.paintChild(g, alloc, index);
    }

    protected void setPropertiesFromAttributes() {
        super.setPropertiesFromAttributes();
        listPainter = getStyleSheet().getListPainter(getAttributes());
    }
}

