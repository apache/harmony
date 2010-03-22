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
 * @author Roman I. Chernyatchik
 */
package javax.swing.text;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.BasicSwingTestCase;
import javax.swing.event.DocumentEvent;

public class LabelViewRTest extends BasicSwingTestCase {
    private DefaultStyledDocument styledDoc;

    private LabelView labelView;

    public LabelViewRTest() {
        super();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        styledDoc = new DefaultStyledDocument();
        styledDoc.insertString(0, "Hello world", null);
        labelView = new LabelView(styledDoc.getDefaultRootElement().getElement(0).getElement(0));
    }

    public void testSetPropertiesFromAttributes() {
        final Marker backgroundColorSetterMarker = new Marker();
        labelView = new LabelView(styledDoc.getDefaultRootElement().getElement(0).getElement(0)) {
            @Override
            protected void setBackground(Color bg) {
                super.setBackground(bg);
                backgroundColorSetterMarker.setOccurred();
            }
        };
        labelView.setPropertiesFromAttributes();
        if (isHarmony()) {
            assertTrue(backgroundColorSetterMarker.isOccurred());
        } else {
            assertFalse(backgroundColorSetterMarker.isOccurred());
        }
    }

    public void testChangedUpdate() {
        final Marker preferenceChangeMarker = new Marker();
        labelView = new LabelView(styledDoc.getDefaultRootElement().getElement(0).getElement(0)) {
            @Override
            public void preferenceChanged(View child, boolean width, boolean height) {
                preferenceChangeMarker.setOccurred();
                super.preferenceChanged(child, width, height);
            }

            @Override
            public void changedUpdate(DocumentEvent event, Shape allocation, ViewFactory factory) {
                // Do nothing.
            }
        };
        DocumentEvent event = new DocumentEvent() {
            public int getOffset() {
                return labelView.getStartOffset();
            }

            public int getLength() {
                return labelView.getEndOffset() - labelView.getStartOffset();
            }

            public Document getDocument() {
                return null;
            }

            public EventType getType() {
                return EventType.CHANGE;
            }

            public ElementChange getChange(Element elem) {
                return null;
            }
        };
        Rectangle rectangle = new Rectangle();
        labelView.changedUpdate(event, rectangle, null);
        assertFalse(preferenceChangeMarker.isOccurred());
    }
}