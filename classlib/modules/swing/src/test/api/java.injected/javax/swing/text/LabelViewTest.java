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
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import javax.swing.BasicSwingTestCase;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;

public class LabelViewTest extends BasicSwingTestCase {
    private final Marker backgroundColorSetterMarker = new Marker();

    private final Marker strikeThroughSetterMarker = new Marker();

    private final Marker subscriptSetterMarker = new Marker();

    private final Marker superscriptSetterMarker = new Marker();

    private final Marker underlineSetterMarker = new Marker();

    private final Marker propertiesFromAttributesSetter = new Marker();

    private final Marker fontMetricsGetter = new Marker();

    private final Marker containerFontMetricsGetter = new Marker();

    private class TestLabelView extends LabelView {
        public TestLabelView(Element element) {
            super(element);
        }

        @Override
        protected void setBackground(Color bg) {
            super.setBackground(bg);
            backgroundColorSetterMarker.setOccurred();
        }

        @Override
        protected void setStrikeThrough(boolean strike) {
            super.setStrikeThrough(strike);
            strikeThroughSetterMarker.setOccurred();
        }

        @Override
        protected void setSubscript(boolean subscript) {
            super.setSubscript(subscript);
            subscriptSetterMarker.setOccurred();
        }

        @Override
        protected void setSuperscript(boolean superscript) {
            super.setSuperscript(superscript);
            superscriptSetterMarker.setOccurred();
        }

        @Override
        protected void setUnderline(boolean underline) {
            super.setUnderline(underline);
            underlineSetterMarker.setOccurred();
        }

        @Override
        protected FontMetrics getFontMetrics() {
            fontMetricsGetter.setOccurred();
            return super.getFontMetrics();
        }

        @Override
        protected void setPropertiesFromAttributes() {
            propertiesFromAttributesSetter.setOccurred();
            super.setPropertiesFromAttributes();
        }

        @Override
        public Container getContainer() {
            return textArea;
        }
    }

    private DefaultStyledDocument styledDoc;

    private LabelView labelView;

    private MutableAttributeSet attrs;

    private final JTextArea textArea;

    public LabelViewTest() {
        super();
        textArea = new JTextArea() {
            private static final long serialVersionUID = 1L;

            @Override
            public FontMetrics getFontMetrics(Font f) {
                containerFontMetricsGetter.setOccurred();
                return super.getFontMetrics(f);
            }
        };
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        styledDoc = new DefaultStyledDocument();
        styledDoc.insertString(0, "Hello world", null);
        attrs = new SimpleAttributeSet();
        StyleConstants.setBold(attrs, true);
        styledDoc.insertString(styledDoc.getLength(), "bold \n string", attrs);
        labelView = new LabelView(styledDoc.getDefaultRootElement().getElement(0).getElement(0));
    }

    public void testChangedUpdate() {
        StyleConstants.setUnderline(attrs, true);
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
        StyleConstants.setUnderline(attrs, true);
        StyleConstants.setBackground(attrs, Color.BLUE);
        setLabelViewAttributeSet(attrs);
        labelView.changedUpdate(event, rectangle, null);
        assertTrue(labelView.isUnderline());
        assertEquals(Color.BLUE, labelView.getBackground());
        labelView.setUnderline(false);
        labelView.changedUpdate(event, rectangle, null);
        assertTrue(labelView.isUnderline());
    }

    public void testIsSuperscript() {
        StyleConstants.setSuperscript(attrs, false);
        setLabelViewAttributeSet(attrs);
        assertFalse(labelView.isSuperscript());
        StyleConstants.setSuperscript(attrs, true);
        setLabelViewAttributeSet(attrs);
        assertFalse(labelView.isSuperscript());
        labelView.setPropertiesFromAttributes();
        assertTrue(labelView.isSuperscript());
        StyleConstants.setSuperscript(attrs, false);
        setLabelViewAttributeSet(attrs);
        assertTrue(labelView.isSuperscript());
        labelView.setPropertiesFromAttributes();
        assertFalse(labelView.isSuperscript());
        labelView = createTestLabelView();
        labelView.isSuperscript();
        assertTrue(propertiesFromAttributesSetter.isOccurred());
        clearMarkers();
        labelView.isSuperscript();
        assertFalse(propertiesFromAttributesSetter.isOccurred());
    }

    public void testSetSuperscript() {
        boolean defaultSuperscript = false;
        assertEquals(defaultSuperscript, labelView.isSuperscript());
        StyleConstants.setSuperscript(attrs, !defaultSuperscript);
        assertEquals(defaultSuperscript, labelView.isSuperscript());
        labelView.setSuperscript(true);
        assertTrue(labelView.isSuperscript());
        labelView.setSuperscript(false);
        assertFalse(labelView.isSuperscript());
        StyleConstants.setSuperscript(attrs, true);
        setLabelViewAttributeSet(attrs);
        assertFalse(labelView.isSuperscript());
        labelView.setPropertiesFromAttributes();
        assertTrue(labelView.isSuperscript());
        StyleConstants.setSuperscript(attrs, false);
        setLabelViewAttributeSet(attrs);
        assertTrue(labelView.isSuperscript());
        labelView.setPropertiesFromAttributes();
        assertFalse(labelView.isSuperscript());
        labelView = createTestLabelView();
        labelView.setSuperscript(true);
        assertFalse(propertiesFromAttributesSetter.isOccurred());
    }

    public void testIsSubscript() {
        StyleConstants.setSubscript(attrs, false);
        setLabelViewAttributeSet(attrs);
        assertFalse(labelView.isSubscript());
        StyleConstants.setSubscript(attrs, true);
        setLabelViewAttributeSet(attrs);
        assertFalse(labelView.isSubscript());
        labelView.setPropertiesFromAttributes();
        assertTrue(labelView.isSubscript());
        StyleConstants.setSubscript(attrs, false);
        setLabelViewAttributeSet(attrs);
        assertTrue(labelView.isSubscript());
        labelView.setPropertiesFromAttributes();
        assertFalse(labelView.isSubscript());
        labelView = createTestLabelView();
        labelView.isSuperscript();
        assertTrue(propertiesFromAttributesSetter.isOccurred());
        clearMarkers();
        labelView.isSuperscript();
        assertFalse(propertiesFromAttributesSetter.isOccurred());
    }

    public void testSetSubscript() {
        boolean defaultSubscript = false;
        assertEquals(defaultSubscript, labelView.isSubscript());
        StyleConstants.setSubscript(attrs, !defaultSubscript);
        assertEquals(defaultSubscript, labelView.isSubscript());
        labelView.setSubscript(true);
        assertTrue(labelView.isSubscript());
        labelView.setSubscript(false);
        assertFalse(labelView.isSubscript());
        StyleConstants.setSubscript(attrs, true);
        setLabelViewAttributeSet(attrs);
        assertFalse(labelView.isSubscript());
        labelView.setPropertiesFromAttributes();
        assertTrue(labelView.isSubscript());
        StyleConstants.setSubscript(attrs, false);
        setLabelViewAttributeSet(attrs);
        assertTrue(labelView.isSubscript());
        labelView.setPropertiesFromAttributes();
        assertFalse(labelView.isSubscript());
        labelView = createTestLabelView();
        labelView.setSubscript(true);
        assertFalse(propertiesFromAttributesSetter.isOccurred());
    }

    public void testIsStrikeThrough() {
        StyleConstants.setStrikeThrough(attrs, false);
        setLabelViewAttributeSet(attrs);
        assertFalse(labelView.isStrikeThrough());
        StyleConstants.setStrikeThrough(attrs, true);
        setLabelViewAttributeSet(attrs);
        assertFalse(labelView.isStrikeThrough());
        labelView.setPropertiesFromAttributes();
        assertTrue(labelView.isStrikeThrough());
        StyleConstants.setStrikeThrough(attrs, false);
        setLabelViewAttributeSet(attrs);
        assertTrue(labelView.isStrikeThrough());
        labelView.setPropertiesFromAttributes();
        assertFalse(labelView.isStrikeThrough());
        labelView = createTestLabelView();
        labelView.isStrikeThrough();
        assertTrue(propertiesFromAttributesSetter.isOccurred());
        clearMarkers();
        labelView.isStrikeThrough();
        assertFalse(propertiesFromAttributesSetter.isOccurred());
    }

    public void testSetStrikeThrough() {
        boolean defaultStrikeThrough = false;
        assertEquals(defaultStrikeThrough, labelView.isStrikeThrough());
        StyleConstants.setStrikeThrough(attrs, !defaultStrikeThrough);
        assertEquals(defaultStrikeThrough, labelView.isStrikeThrough());
        labelView.setStrikeThrough(true);
        assertTrue(labelView.isStrikeThrough());
        labelView.setStrikeThrough(false);
        assertFalse(labelView.isStrikeThrough());
        StyleConstants.setStrikeThrough(attrs, true);
        setLabelViewAttributeSet(attrs);
        assertFalse(labelView.isStrikeThrough());
        labelView.setPropertiesFromAttributes();
        assertTrue(labelView.isStrikeThrough());
        StyleConstants.setStrikeThrough(attrs, false);
        setLabelViewAttributeSet(attrs);
        assertTrue(labelView.isStrikeThrough());
        labelView.setPropertiesFromAttributes();
        assertFalse(labelView.isStrikeThrough());
        labelView = createTestLabelView();
        labelView.setStrikeThrough(true);
        assertFalse(propertiesFromAttributesSetter.isOccurred());
    }

    public void testIsUnderline() {
        StyleConstants.setUnderline(attrs, false);
        setLabelViewAttributeSet(attrs);
        assertFalse(labelView.isUnderline());
        StyleConstants.setUnderline(attrs, true);
        setLabelViewAttributeSet(attrs);
        assertFalse(labelView.isUnderline());
        labelView.setPropertiesFromAttributes();
        assertTrue(labelView.isUnderline());
        StyleConstants.setUnderline(attrs, false);
        setLabelViewAttributeSet(attrs);
        assertTrue(labelView.isUnderline());
        labelView.setPropertiesFromAttributes();
        assertFalse(labelView.isUnderline());
        labelView = createTestLabelView();
        labelView.isUnderline();
        assertTrue(propertiesFromAttributesSetter.isOccurred());
        clearMarkers();
        labelView.isUnderline();
        assertFalse(propertiesFromAttributesSetter.isOccurred());
    }

    public void testSetUnderline() {
        boolean defaultUnderline = false;
        assertEquals(defaultUnderline, labelView.isUnderline());
        StyleConstants.setUnderline(attrs, !defaultUnderline);
        assertEquals(defaultUnderline, labelView.isUnderline());
        labelView.setUnderline(true);
        assertTrue(labelView.isUnderline());
        labelView.setUnderline(false);
        assertFalse(labelView.isUnderline());
        StyleConstants.setUnderline(attrs, true);
        setLabelViewAttributeSet(attrs);
        assertFalse(labelView.isUnderline());
        labelView.setPropertiesFromAttributes();
        assertTrue(labelView.isUnderline());
        StyleConstants.setUnderline(attrs, false);
        setLabelViewAttributeSet(attrs);
        assertTrue(labelView.isUnderline());
        labelView.setPropertiesFromAttributes();
        assertFalse(labelView.isUnderline());
        labelView = createTestLabelView();
        labelView.setUnderline(true);
        assertFalse(propertiesFromAttributesSetter.isOccurred());
    }

    public void testGetFont() {
        Font font = labelView.getFont();
        assertSame(styledDoc.getFont(labelView.getAttributes()), font);
        assertSame(styledDoc.getFont(labelView.getAttributes()), font);
        StyleConstants.setFontSize(attrs, font.getSize() + 1);
        setLabelViewAttributeSet(attrs);
        assertSame(font, labelView.getFont());
        assertSame(font, labelView.getFont());
        labelView.setPropertiesFromAttributes();
        font = labelView.getFont();
        assertSame(styledDoc.getFont(labelView.getAttributes()), font);
        assertSame(font, labelView.getFont());
        StyleConstants.setFontSize(attrs, font.getSize() + 1);
        labelView.setPropertiesFromAttributes();
        assertSame(font, labelView.getFont());
        assertSame(font, styledDoc.getFont(labelView.getAttributes()));
        labelView = createTestLabelView();
        labelView.getFont();
        assertTrue(propertiesFromAttributesSetter.isOccurred());
        clearMarkers();
        labelView.getFont();
        assertFalse(propertiesFromAttributesSetter.isOccurred());
    }

    public void testGetFontMetrics() {
        labelView = createTestLabelView();
        FontMetrics fontMetrics = labelView.getFontMetrics();
        assertTrue(propertiesFromAttributesSetter.isOccurred());
        propertiesFromAttributesSetter.setOccurred(false);
        fontMetrics = labelView.getFontMetrics();
        assertFalse(propertiesFromAttributesSetter.isOccurred());
        propertiesFromAttributesSetter.setOccurred(false);
        assertEquals(labelView.getFont(), fontMetrics.getFont());
        labelView = createTestLabelView();
        labelView.getFont();
        assertTrue(propertiesFromAttributesSetter.isOccurred());
        assertFalse(fontMetricsGetter.isOccurred());
        assertFalse(containerFontMetricsGetter.isOccurred());
        clearMarkers();
        labelView.getFontMetrics();
        assertFalse(propertiesFromAttributesSetter.isOccurred());
        assertTrue(containerFontMetricsGetter.isOccurred());
    }

    public void testGetForeground() {
        Color defaultColor;
        defaultColor = labelView.getForeground();
        assertNotNull(defaultColor);
        StyleConstants.setForeground(attrs, Color.RED);
        setLabelViewAttributeSet(attrs);
        assertEquals(defaultColor, labelView.getForeground());
        assertEquals(defaultColor, labelView.getForeground());
        labelView.setPropertiesFromAttributes();
        assertEquals(Color.RED, labelView.getForeground());
        StyleConstants.setForeground(attrs, Color.BLUE);
        setLabelViewAttributeSet(attrs);
        assertEquals(Color.RED, labelView.getForeground());
        assertEquals(Color.RED, labelView.getForeground());
        labelView.setPropertiesFromAttributes();
        assertEquals(Color.BLUE, labelView.getForeground());
        labelView = createTestLabelView();
        labelView.getForeground();
        assertTrue(propertiesFromAttributesSetter.isOccurred());
        clearMarkers();
        labelView.getForeground();
        assertFalse(propertiesFromAttributesSetter.isOccurred());
    }

    public void testGetBackground() {
        Color defaultColor = labelView.getBackground();
        assertNull(defaultColor);
        StyleConstants.setBackground(attrs, Color.RED);
        setLabelViewAttributeSet(attrs);
        assertEquals(defaultColor, labelView.getBackground());
        assertEquals(defaultColor, labelView.getBackground());
        labelView.setPropertiesFromAttributes();
        assertEquals(Color.RED, labelView.getBackground());
        StyleConstants.setBackground(attrs, Color.BLUE);
        setLabelViewAttributeSet(attrs);
        assertEquals(Color.RED, labelView.getBackground());
        assertEquals(Color.RED, labelView.getBackground());
        labelView.setPropertiesFromAttributes();
        assertEquals(Color.BLUE, labelView.getBackground());
        labelView = createTestLabelView();
        labelView.getBackground();
        assertTrue(propertiesFromAttributesSetter.isOccurred());
        clearMarkers();
        labelView.getBackground();
        assertFalse(propertiesFromAttributesSetter.isOccurred());
    }

    public void testSetBackground_WithNull() {
        labelView.getBackground();
        labelView.setBackground(null);
        assertNull(labelView.getBackground());
    }

    public void testSetBackground_WithValidValues() {
        labelView.getBackground();
        labelView.setBackground(Color.RED);
        assertEquals(Color.RED, labelView.getBackground());
        StyleConstants.setBackground(attrs, Color.RED);
        assertEquals(Color.RED, labelView.getBackground());
        labelView.setBackground(Color.BLUE);
        assertEquals(Color.BLUE, labelView.getBackground());
        labelView = createTestLabelView();
        labelView.getBackground();
        assertTrue(propertiesFromAttributesSetter.isOccurred());
    }

    public void testSetPropertiesFromAttributes() {
        // SuperscriptReSync
        StyleConstants.setSuperscript(attrs, true);
        assertFalse(labelView.isSuperscript());
        setLabelViewAttributeSet(attrs);
        labelView.setPropertiesFromAttributes();
        assertTrue(labelView.isSuperscript());
        // BackgroundReSync
        StyleConstants.setBackground(attrs, Color.BLUE);
        assertNull(labelView.getBackground());
        setLabelViewAttributeSet(attrs);
        assertNull(labelView.getBackground());
        labelView.setPropertiesFromAttributes();
        StyleConstants.setBackground(attrs, Color.RED);
        assertSame(Color.BLUE, labelView.getBackground());
        setLabelViewAttributeSet(attrs);
        labelView.setPropertiesFromAttributes();
        assertSame(Color.RED, labelView.getBackground());
        labelView = createTestLabelView();
        labelView.setPropertiesFromAttributes();
        assertFalse(fontMetricsGetter.isOccurred());
        if (isHarmony()) {
            assertTrue(backgroundColorSetterMarker.isOccurred());
        } else {
            assertFalse(backgroundColorSetterMarker.isOccurred());
        }
        assertTrue(strikeThroughSetterMarker.isOccurred());
        assertTrue(subscriptSetterMarker.isOccurred());
        assertTrue(superscriptSetterMarker.isOccurred());
        assertTrue(underlineSetterMarker.isOccurred());
    }

    private void setLabelViewAttributeSet(MutableAttributeSet attrs) {
        styledDoc.setCharacterAttributes(labelView.getStartOffset(), labelView.getEndOffset()
                - labelView.getStartOffset(), attrs, true);
        assertSame(labelView.getElement(), styledDoc.getDefaultRootElement().getElement(0)
                .getElement(0));
    }

    private void clearMarkers() {
        backgroundColorSetterMarker.setOccurred(false);
        strikeThroughSetterMarker.setOccurred(false);
        subscriptSetterMarker.setOccurred(false);
        superscriptSetterMarker.setOccurred(false);
        underlineSetterMarker.setOccurred(false);
        propertiesFromAttributesSetter.setOccurred(false);
        fontMetricsGetter.setOccurred(false);
        containerFontMetricsGetter.setOccurred(false);
    }

    private TestLabelView createTestLabelView() {
        clearMarkers();
        return new TestLabelView(styledDoc.getDefaultRootElement().getElement(0).getElement(0));
    }
}