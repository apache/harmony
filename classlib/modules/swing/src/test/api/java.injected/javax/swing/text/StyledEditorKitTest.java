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

import java.util.Enumeration;

import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.SwingTestCase;
import javax.swing.event.ChangeListener;

public class StyledEditorKitTest extends SwingTestCase {
    StyledEditorKit kit;

    class SimpleElement implements Element {
        String name;

        AttributeSet set = new SimpleAttributeSet();

        public SimpleElement(final String s) {
            name = s;
        }

        public AttributeSet getAttributes() {
            return set;
        }

        public Document getDocument() {
            return new PlainDocument();
        }

        public Element getElement(final int arg0) {
            return null;
        }

        public int getElementCount() {
            return 0;
        }

        public int getElementIndex(final int arg0) {
            return 0;
        }

        public int getEndOffset() {
            return 0;
        }

        public String getName() {
            return name;
        }

        public Element getParentElement() {
            return null;
        }

        public int getStartOffset() {
            return 0;
        }

        public boolean isLeaf() {
            return false;
        }

        final void setAttributeSet(final AttributeSet as) {
            set = as;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        kit = new StyledEditorKit();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testStyledEditorKit() {
    }

    public void testClone() {
        Object kit1 = kit.clone();
        assertNotSame(kit1, kit);
        assertTrue(kit1 instanceof StyledEditorKit);
    }

    public void testGetActions() {
        Action[] actions = kit.getActions();
        Action[] superActions = new DefaultEditorKit().getActions();
        Action[] newActions = new Action[] {
                new StyledEditorKit.FontSizeAction("font-size-48", 48),
                new StyledEditorKit.FontSizeAction("font-size-36", 36),
                new StyledEditorKit.FontSizeAction("font-size-24", 24),
                new StyledEditorKit.FontSizeAction("font-size-18", 18),
                new StyledEditorKit.FontSizeAction("font-size-16", 16),
                new StyledEditorKit.FontSizeAction("font-size-14", 14),
                new StyledEditorKit.FontSizeAction("font-size-12", 12),
                new StyledEditorKit.FontSizeAction("font-size-10", 10),
                new StyledEditorKit.FontSizeAction("font-size-8", 8),
                new StyledEditorKit.FontFamilyAction("font-family-SansSerif", "SansSerif"),
                new StyledEditorKit.FontFamilyAction("font-family-Serif", "Serif"),
                new StyledEditorKit.FontFamilyAction("font-family-Monospaced", "Monospaced"),
                new StyledEditorKit.BoldAction(),
                new StyledEditorKit.UnderlineAction(),
                new StyledEditorKit.ItalicAction(),
                new StyledEditorKit.AlignmentAction("right-justify", StyleConstants.ALIGN_RIGHT),
                new StyledEditorKit.AlignmentAction("left-justify", StyleConstants.ALIGN_LEFT),
                new StyledEditorKit.AlignmentAction("center-justify",
                        StyleConstants.ALIGN_CENTER)
        //,
        //new StyledEditorKit.StyledInsertBreakAction("insert-break")
        };
        //There are some problems
        //1) no Foreground action in StyledEditorKit.getActions
        //2) DefaultEditorKit.InsertBreakAction is replaced by
        // StyledEditorKit.StyledInsertBreakAction
        for (int i = 0; i < superActions.length; i++) {
            boolean was = false;
            if (superActions[i].getClass().getName().equals(
                    "javax.swing.text.DefaultEditorKit$InsertBreakAction")) {
                continue;
            }
            for (int j = 0; j < actions.length; j++) {
                if (superActions[i] == actions[j]) {
                    was = true;
                    break;
                }
            }
            assertTrue("action missed: " + superActions[i], was);
        }
        for (int i = 0; i < newActions.length; i++) {
            boolean was = false;
            for (int j = 0; j < actions.length; j++) {
                if (newActions[i].getClass().getName().equals(actions[j].getClass().getName())
                        && newActions[i].getValue(Action.NAME).equals(
                                actions[j].getValue(Action.NAME))) {
                    was = true;
                    break;
                }
            }
            assertTrue("action missed: " + superActions[i], was);
        }
    }

    public void testCreateDefaultDocument() {
        Document doc1 = kit.createDefaultDocument();
        Document doc2 = kit.createDefaultDocument();
        String className = "javax.swing.text.DefaultStyledDocument";
        assertEquals(className, doc1.getClass().getName());
        assertEquals(className, doc2.getClass().getName());
        assertNotSame(doc1, doc2);
    }

    final SimpleAttributeSet createAttributeSet() {
        SimpleAttributeSet sas = new SimpleAttributeSet();
        sas.addAttribute(AbstractDocument.ParagraphElementName, "p");
        sas.addAttribute(AbstractDocument.ContentElementName, "c");
        sas.addAttribute(AbstractDocument.SectionElementName, "s");
        sas.addAttribute(StyleConstants.ComponentElementName, "1");
        sas.addAttribute(StyleConstants.IconElementName, "2");
        sas.addAttribute(StyleConstants.IconElementName, "3");
        sas.addAttribute(StyleConstants.IconAttribute, "4");
        sas.addAttribute(AbstractDocument.ElementNameAttribute, "7");
        sas.addAttribute("##", "5");
        return sas;
    }

    static final void removeObsolete(final MutableAttributeSet as) {
        as.removeAttribute(StyleConstants.IconAttribute);
        as.removeAttribute(StyleConstants.ComponentAttribute);
        as.removeAttribute(AbstractDocument.ElementNameAttribute);
    }

    public void testCreateInputAttributes() {
        SimpleAttributeSet sas = createAttributeSet();
        SimpleElement element = new SimpleElement("");
        element.setAttributeSet(sas);
        SimpleAttributeSet set = new SimpleAttributeSet();
        set.addAttribute(StyleConstants.ComponentAttribute, "3");
        set.addAttribute(AbstractDocument.ContentElementName, "~");
        set.addAttribute("^^$$$", "%%%%");
        kit.createInputAttributes(element, set);
        removeObsolete(sas);
        assertEquals(sas, set);
    }

    public void testDeinstallJEditorPane() {
    }

    public void testGetCharacterAttributeRun() {
        SimpleAttributeSet set = createAttributeSet();
        JEditorPane jep = new JEditorPane();
        DefaultStyledDocument doc = new DefaultStyledDocument();
        jep.setEditorKit(kit);
        jep.setDocument(doc);
        jep.setText("ABCDEDFGHIGKLMN\n");
        doc.setCharacterAttributes(3, 8, set, true);
        int length = doc.getLength();
        removeObsolete(set);
        for (int i = 0; i < length; i++) {
            jep.setCaretPosition(i);
            assertEquals(getElementByOffset(doc, i), kit.getCharacterAttributeRun());
        }
    }

    public void testGetInputAttributes() {
        SimpleAttributeSet set = createAttributeSet();
        JEditorPane jep = new JEditorPane();
        DefaultStyledDocument doc = new DefaultStyledDocument();
        jep.setEditorKit(kit);
        jep.setDocument(doc);
        jep.setText("ABCDEDFGHIGKLMN\n");
        doc.setCharacterAttributes(3, 8, set, true);
        int length = doc.getLength();
        removeObsolete(set);
        for (int i = 0; i < length; i++) {
            jep.setCaretPosition(i);
            if (i > 3 && i < 12) {
                assertEquals(set, kit.getInputAttributes());
            } else {
                assertEquals(new SimpleAttributeSet(), kit.getInputAttributes());
            }
        }
    }
    
    public void testGetInputAttributesNoComponent() throws Exception {
        final AttributeSet as = kit.getInputAttributes();
        assertNotNull(as);
        assertEquals(0, as.getAttributeCount());
    }

    static final Element getElementByOffset(final Document doc, final int offset) {
        int pos = (offset == 0) ? 0 : offset - 1;
        Element elem = doc.getDefaultRootElement();
        while (elem.getElementCount() > 0) {
            elem = elem.getElement(elem.getElementIndex(pos));
        }
        return elem;
    }

    public void testGetViewFactory() {
        ViewFactory factory = kit.getViewFactory();
        Element element = new SimpleElement(AbstractDocument.ContentElementName);
        assertEquals("javax.swing.text.LabelView", factory.create(element).getClass().getName());
        element = new SimpleElement(AbstractDocument.ParagraphElementName);
        assertEquals("javax.swing.text.ParagraphView", factory.create(element).getClass()
                .getName());
        element = new SimpleElement(AbstractDocument.SectionElementName);
        View v = factory.create(element);
        assertEquals("javax.swing.text.BoxView", v.getClass().getName());
        assertEquals(View.Y_AXIS, ((BoxView) v).getAxis());
        element = new SimpleElement(StyleConstants.ComponentElementName);
        assertEquals("javax.swing.text.ComponentView", factory.create(element).getClass()
                .getName());
        element = new SimpleElement(StyleConstants.IconElementName);
        assertEquals("javax.swing.text.IconView", factory.create(element).getClass().getName());
        element = new SimpleElement("something");
        assertEquals("javax.swing.text.LabelView", factory.create(element).getClass().getName());
        ViewFactory factory1 = kit.getViewFactory();
        assertEquals(factory, factory1);
    }

    public void testInstallJEditorPane() {
    }
    
    
    
    /**
     * Regression test for HARMONY-2594
     * */
    public void testcreateInputAttributes() {
        MyStyledEditorKit msek = new MyStyledEditorKit();
        MutableAttributeSet set = new Style() {
            public void removeChangeListener(ChangeListener p0) {
                return;
            }
            public void addChangeListener(ChangeListener p0) {
                return;
            }
            public String getName() {
                return "AA";
            }
            public void setResolveParent(AttributeSet p0) {
                return;
            }
            public void removeAttributes(AttributeSet p0) {
                return;
            }
            public void removeAttributes(Enumeration p0) {
                return;
            }
            public void removeAttribute(Object p0) {
                return;
            }
            public void addAttributes(AttributeSet p0) {
                return;
            }
            public void addAttribute(Object p0, Object p1) {
                return;
            }
            public AttributeSet getResolveParent() {
                return null;
            }
            public boolean containsAttributes(AttributeSet p0) {
                return false;
            }
            public boolean containsAttribute(Object p0, Object p1) {
                return false;
            }
            public Enumeration getAttributeNames() {
                return null;
            }
            public Object getAttribute(Object p0) {
                return null;
            }
            public AttributeSet copyAttributes() {
                return null;
            }
            public boolean isEqual(AttributeSet p0) {
                return false;
            }
            public boolean isDefined(Object p0) {
                return false;
            }
            public int getAttributeCount() {
                return 0;
            }
        };
        try {
            msek.createInputAttributes(null, set);
            fail("NPE not thrown when Element is null!");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    /**
     * Regression test for HARMONY-2594
     * */
    public void testCreateInputAttributes2() {
        MyStyledEditorKit msek = new MyStyledEditorKit();
        try {
            msek.createInputAttributes(new SimpleElement(""), null);
            fail("NPE not thrown when MutableAttributeSet is null!");
        } catch (NullPointerException npe) {
            // expected
        }
    }
    
    class MyStyledEditorKit extends StyledEditorKit {
        public MyStyledEditorKit() {
            super();
        }

        public void createInputAttributes(Element element, MutableAttributeSet set) {
            super.createInputAttributes(element, set);
        }
    }
    
}
