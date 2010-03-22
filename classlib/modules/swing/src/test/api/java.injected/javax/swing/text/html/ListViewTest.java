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

import java.io.StringReader;

import javax.swing.BasicSwingTestCase;
import javax.swing.SizeRequirements;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.ViewTestHelpers.ChildrenFactory;
import javax.swing.text.html.BlockViewTest.InlineViewFactory;

public class ListViewTest extends BasicSwingTestCase {
    private class ListViewImpl extends ListView {
        public ListViewImpl(final Element element) {
            super(element);
            loadChildren();
        }

        public ViewFactory getViewFactory() {
            return factory;
        }

        public void loadChildren() {
            loadChildren(getViewFactory());
        }
    }

    private HTMLEditorKit kit;
    private HTMLDocument doc;
    private Element listU;
    private ListView view;
    private ViewFactory factory;

    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        kit = new HTMLEditorKit();
        doc = (HTMLDocument)kit.createDefaultDocument();
        StringReader reader = new StringReader("<html><head></head>" +
               "<body>" +
               "<ul>" +
               "    <li>first</li>" +
               "    <li>second</li>" +
               "</ul>" +
               "</body></html>");
        kit.read(reader, doc, 0);

        listU = doc.getDefaultRootElement().getElement(1).getElement(0);
        //         | html                  | body        | ul
        assertEquals(HTML.Tag.UL.toString(), listU.getName());

        factory = new InlineViewFactory();
        view = new ListViewImpl(listU);
    }

    public void testListView() {
        assertEquals(View.Y_AXIS, view.getAxis());
        assertNotSame(listU.getAttributes(), view.getAttributes());
        assertEquals(listU.getElementCount(), view.getViewCount());
    }

    public void testGetAlignment() {
        assertEquals(0.5f, view.getAlignment(View.X_AXIS), 0);
        assertEquals(0.5f, view.getAlignment(View.Y_AXIS), 0);
    }

    public void testGetAlignmentFlexible() {
        factory = new ChildrenFactory();
        ((ChildrenFactory)factory).makeFlexible();
        view = new ListViewImpl(listU);

        assertEquals(0.5f, view.getAlignment(View.X_AXIS), 0);
        assertEquals(0.5f, view.getAlignment(View.Y_AXIS), 0);

        SizeRequirements r = view.calculateMajorAxisRequirements(View.Y_AXIS,
                                                                 null);
        assertEquals(0.5f, r.alignment, 0);

        r = view.calculateMajorAxisRequirements(View.X_AXIS, r);
        assertEquals(0.5f, r.alignment, 0);
    }

    public void testSetPropertiesFromAttributes() {
        final Marker listImage = new Marker();
        final Marker listType = new Marker();
        view = new ListView(listU) {
            private AttributeSet attributes;
            public AttributeSet getAttributes() {
                if (attributes == null) {
                    attributes = new SimpleAttributeSet(super.getAttributes()) {
                        public Object getAttribute(Object name) {
                            if (name == CSS.Attribute.LIST_STYLE_IMAGE) {
                                listImage.setOccurred();
                            } else if (name == CSS.Attribute.LIST_STYLE_TYPE) {
                                listType.setOccurred();
                            }
                            return super.getAttribute(name);
                        }
                    };
                }
                return attributes;
            }
        };
        assertFalse(listImage.isOccurred());
        assertFalse(listType.isOccurred());
        view.setPropertiesFromAttributes();
        assertEquals(!isHarmony(), listImage.isOccurred());
        assertEquals(!isHarmony(), listType.isOccurred());
    }

    public void testSetPropertiesFromAttributesPainter() {
        final Marker boxMarker = new Marker();
        final Marker listMarker = new Marker();
        final StyleSheet ss = new StyleSheet() {
            public BoxPainter getBoxPainter(final AttributeSet attr) {
                boxMarker.setOccurred();
                return super.getBoxPainter(attr);
            }
            public ListPainter getListPainter(final AttributeSet attr) {
                listMarker.setOccurred();
                return null;
            }
        };
        view = new ListView(listU) {
            protected StyleSheet getStyleSheet() {
                return ss;
            }
        };
        assertFalse(boxMarker.isOccurred());
        assertFalse(listMarker.isOccurred());
        view.setPropertiesFromAttributes();
        assertTrue(boxMarker.isOccurred());
        assertTrue(listMarker.isOccurred());
    }

//    public void testPaint() {
//    }

//    public void testPaintChild() {
//    }
}
