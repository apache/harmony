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
 * @author Evgeniya G. Maenkova
 */
package javax.swing.text;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.SwingTestCase;

public class StyledEditorKit_StyledTextActionTest extends SwingTestCase {
    StyledEditorKit.StyledTextAction action;

    boolean bWasException;

    String message;

    JEditorPane jep;

    StyledEditorKit kit;

    class StyledTextAction extends StyledEditorKit.StyledTextAction {
        private static final long serialVersionUID = 1L;

        public StyledTextAction(final String name) {
            super(name);
        }

        public void actionPerformed(final ActionEvent e) {
        }
    }

    @Override
    protected void setUp() throws Exception {
        jep = new JEditorPane();
        setIgnoreNotImplemented(true);
        action = new StyledTextAction("testName");
        message = null;
        bWasException = false;
        kit = new StyledEditorKit();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testActionPerformed() {
    }

    public void testStyledTextAction() {
        assertEquals("testName", action.getValue(Action.NAME));
        assertTrue(action.isEnabled());
    }

    public void testGetEditor() {
        jep = new JEditorPane();
        assertEquals(jep, action.getEditor(new ActionEvent(jep, 1, "...")));
        //Really this needs to check NullPointerException, but ...
    }

    public void testSetParagraphAttributes() {
        DefaultStyledDocument doc = new DefaultStyledDocument();
        jep.setEditorKit(kit);
        jep.setDocument(doc);
        jep.setText("ABCD\nEFGH\nIJKL\nMNOP\nQRST\n");
        jep.setCaretPosition(2);
        SimpleAttributeSet sas = createAttributeSet(2);
        action.setParagraphAttributes(jep, sas, false);
        checkParagraphAttributes(doc, -1, 5, sas, 0);
        jep.setSelectionStart(10);
        jep.setSelectionEnd(13);
        SimpleAttributeSet sas2 = createAttributeSet(3);
        action.setParagraphAttributes(jep, sas2, false);
        checkParagraphAttributes(doc, 9, 15, sas2, 6);
        jep.setCaretPosition(18);
        SimpleAttributeSet sas3 = createAttributeSet(4);
        action.setParagraphAttributes(jep, sas3, true);
        for (int i = 0; i < doc.getLength(); i++) {
            AttributeSet as = doc.getParagraphElement(i).getAttributes();
            if (i < 15) {
                sas.addAttribute(AttributeSet.ResolveAttribute, as
                        .getAttribute(AttributeSet.ResolveAttribute));
                sas2.addAttribute(AttributeSet.ResolveAttribute, as
                        .getAttribute(AttributeSet.ResolveAttribute));
            }
            if (i < 5) {
                assertEquals(sas, as);
            } else if (i < 10) {
                assertNotNull(as.getAttribute(AttributeSet.ResolveAttribute));
                assertEquals(1, as.getAttributeCount());
            } else if (i < 15) {
                assertEquals(sas2, as);
            } else if (i < 20) {
                assertEquals(sas3, as);
            } else {
                assertNotNull(as.getAttribute(AttributeSet.ResolveAttribute));
                assertEquals(1, as.getAttributeCount());
            }
        }
    }

    public void testGetStyledDocument() {
        jep = new JEditorPane();
        try {
            action.getStyledDocument(jep);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertTrue(message.endsWith("'StyledDocument'"));
        StyledEditorKit styledKit = new StyledEditorKit();
        jep.setEditorKit(styledKit);
        assertEquals(jep.getDocument(), action.getStyledDocument(jep));
        bWasException = false;
        jep.setDocument(new PlainDocument());
        try {
            action.getStyledDocument(jep);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertTrue(message.endsWith("'StyledDocument'"));
    }

    public void testGetStyledEditorKit() {
        jep = new JEditorPane();
        try {
            action.getStyledEditorKit(jep);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertTrue(message.endsWith("'StyledEditorKit'"));
        StyledEditorKit styledKit = new StyledEditorKit();
        jep.setEditorKit(styledKit);
        assertEquals(styledKit, action.getStyledEditorKit(jep));
        jep.setDocument(new PlainDocument());
        assertEquals(styledKit, action.getStyledEditorKit(jep));
    }

    public void testSetCharacterAttributes() {
        DefaultStyledDocument doc = new DefaultStyledDocument();
        jep.setEditorKit(kit);
        jep.setDocument(doc);
        jep.setText("ABCDEFGHIGKLMNOPQA\n");
        //no selection
        AttributeSet attrs = doc.getCharacterElement(4).getAttributes();
        jep.setCaretPosition(4);
        SimpleAttributeSet sas = createAttributeSet(1);
        action.setCharacterAttributes(jep, sas, false);
        assertEquals(sas, kit.getInputAttributes());
        assertEquals(attrs, doc.getCharacterElement(4).getAttributes());
        SimpleAttributeSet sas2 = createAttributeSet(2);
        action.setCharacterAttributes(jep, sas2, false);
        assertEquals(sas2, kit.getInputAttributes());
        assertEquals(attrs, doc.getCharacterElement(4).getAttributes());
        SimpleAttributeSet sas3 = createAttributeSet(3);
        action.setCharacterAttributes(jep, sas3, false);
        assertEquals(sas3, kit.getInputAttributes());
        assertEquals(attrs, doc.getCharacterElement(4).getAttributes());
        //there is selection
        jep.setSelectionStart(3);
        jep.setSelectionEnd(5);
        kit.getInputAttributes().removeAttributes(kit.getInputAttributes());
        attrs = kit.getInputAttributes();
        SimpleAttributeSet sas4 = createAttributeSet(4);
        action.setCharacterAttributes(jep, sas4, false);
        checkAttributes(doc, 3, 6, sas4, true);
        assertEquals(attrs, kit.getInputAttributes());
        SimpleAttributeSet sas5 = createAttributeSet(5);
        SimpleAttributeSet sas6 = new SimpleAttributeSet();
        sas6.addAttribute("&&", "$$$");
        action.setCharacterAttributes(jep, sas6, true);
        assertEquals(attrs, kit.getInputAttributes());
        action.setCharacterAttributes(jep, sas5, false);
        sas5.addAttribute("&&", "$$$");
        checkAttributes(doc, 3, 6, sas5, true);
        assertEquals(attrs, kit.getInputAttributes());
    }

    static final void checkAttributes(final Document doc, final int start, final int end,
            final MutableAttributeSet set, final boolean ignoreResolveAttribute) {
        for (int i = 0; i < doc.getLength(); i++) {
            AttributeSet as = getAttributeSetByOffset(doc, i);
            if (!ignoreResolveAttribute) {
                set.addAttribute(AttributeSet.ResolveAttribute, as
                        .getAttribute(AttributeSet.ResolveAttribute));
            }
            if (i > start && i < end) {
                assertEquals(set, as);
            } else {
                assertEquals(0, as.getAttributeCount());
            }
        }
    }

    final void checkParagraphAttributes(final AbstractDocument doc, final int start,
            final int end, final MutableAttributeSet set, final int from) {
        for (int i = from; i < doc.getLength(); i++) {
            AttributeSet as = doc.getParagraphElement(i).getAttributes();
            set.addAttribute(AttributeSet.ResolveAttribute, as
                    .getAttribute(AttributeSet.ResolveAttribute));
            if (i > start && i < end) {
                assertSame(as, doc.getParagraphElement(i));
                assertEquals(set.getAttributeCount(), as.getAttributeCount());
                assertEquals(set, as);
            } else {
                assertEquals(1, as.getAttributeCount());
            }
        }
    }

    final SimpleAttributeSet createAttributeSet(final int i) {
        SimpleAttributeSet sas = new SimpleAttributeSet();
        Integer value = new Integer(i);
        sas.addAttribute(AbstractDocument.ParagraphElementName, value);
        sas.addAttribute(AbstractDocument.ContentElementName, value);
        sas.addAttribute(AbstractDocument.SectionElementName, value);
        sas.addAttribute(StyleConstants.ComponentElementName, value);
        sas.addAttribute(StyleConstants.IconElementName, value);
        sas.addAttribute(StyleConstants.IconAttribute, value);
        sas.addAttribute("##", value);
        return sas;
    }

    static final AttributeSet getAttributeSetByOffset(final Document doc, final int offset) {
        Element elem = StyledEditorKitTest.getElementByOffset(doc, offset);
        return (elem == null) ? null : elem.getAttributes();
    }
}
