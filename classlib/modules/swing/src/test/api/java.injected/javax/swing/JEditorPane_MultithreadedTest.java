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
package javax.swing;

import junit.framework.TestCase;

public class JEditorPane_MultithreadedTest extends TestCase {
    JEditorPane jep;

    JFrame jf;

    SetTextMaster masterSetText = new SetTextMaster();

    MakeSelectionMaster masterMakeSelection = new MakeSelectionMaster();

    UnsupportedOperationException ex;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ex = null;
        jep = new JEditorPane();
        jf = new JFrame();
        jf.getContentPane().add(jep);
        jf.setSize(200, 300);
        jf.pack();
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    final class MakeSelectionMaster implements Runnable {
        int dot;

        int mark;

        public void setDotAndMark(final int d, final int m) {
            dot = d;
            mark = m;
        }

        public void setDot(final int i) {
            setDotAndMark(i, i);
        }

        public void run() {
            jep.getCaret().setDot(mark);
            jep.getCaret().moveDot(dot);
        }
    }

    final class SetTextMaster implements Runnable {
        String text;

        public void setText(final String s) {
            text = s;
        }

        public void run() {
            jep.setText(text);
        }
    }

    public void testReplaceSelection() throws Exception {
        masterSetText.setText("testReplaceSelection");
        masterMakeSelection.setDotAndMark(7, 4);
        SwingUtilities.invokeAndWait(masterSetText);
        SwingUtilities.invokeAndWait(masterMakeSelection);
        jep.replaceSelection("XXX");
        assertEquals("testXXXlaceSelection", jep.getText());
        masterMakeSelection.setDotAndMark(4, 2);
        SwingUtilities.invokeAndWait(masterMakeSelection);
        jep.replaceSelection(null);
        assertEquals("teXXXlaceSelection", jep.getText());
        masterMakeSelection.setDotAndMark(2, 0);
        SwingUtilities.invokeAndWait(masterMakeSelection);
        jep.replaceSelection(null);
        assertEquals("XXXlaceSelection", jep.getText());
        masterMakeSelection.setDotAndMark(3, 3);
        SwingUtilities.invokeAndWait(masterMakeSelection);
        jep.replaceSelection("YYY");
        assertEquals("XXXYYYlaceSelection", jep.getText());
    }

    public void testReplaceSelectionNotEditable() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                jep.setText("replaceSelectionNotEditable");
                jep.setEditable(false);
                jep.setSelectionStart(3);
                jep.setSelectionEnd(5);
            }
        });
        jep.replaceSelection("AAAA");
        assertEquals("la", jep.getSelectedText());
        assertEquals("replaceSelectionNotEditable", jep.getText());
    }

    //temporary commented: DefaultStyledDocument not implemented
    /*public void testReplaceSelectionWithAttributes() throws Exception {
     final AbstractDocument doc = new DefaultStyledDocument();
     SimpleAttributeSet as1 = new SimpleAttributeSet();
     as1.addAttribute("key1", "value1");
     SimpleAttributeSet as2 = new SimpleAttributeSet();
     as2.addAttribute("key2", "value2");

     try {
     doc.insertString(0, "testReplaceSelection", as1);
     doc.insertString(4, "INSERT", as2);
     } catch (final BadLocationException e) {
     assertFalse("unexpected exception :" + e.getMessage(), true);
     }
     SwingUtilities.invokeAndWait(new Runnable() {
     public void run() {
     try {
     jep.setEditorKit(new RTFEditorKit());
     jep.setDocument(doc);
     } catch(UnsupportedOperationException e){
     ex = e;
     }
     }
     });
     if (ex != null)
     return;
     masterMakeSelection.setDotAndMark(7, 6);
     SwingUtilities.invokeAndWait(masterMakeSelection);

     jep.replaceSelection("YYY");

     for (int i = 0; i < doc.getLength(); i++) {
     AttributeSet as = getAttributeSetByIndex(doc, i);
     if (i > 3 && i < 12)
     assertEquals(as2, as);
     else
     assertEquals(as1, as);
     }
     }*/
    public void testSetGetTextPlain() throws Exception {
        jep.setText(JEditorPaneTest.plainString);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                assertEquals("plain", JEditorPaneTest.plainString, jep.getText());
            }
        });
    };

    public void testSetGetTextRtf() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    jep.setContentType("text/rtf");
                } catch (UnsupportedOperationException e) {
                    ex = e;
                }
            }
        });
        if (ex != null) {
            return;
        }
        jep.setText(JEditorPaneTest.rtfString);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                assertEquals("blablabla\n", JEditorPaneTest.getDocContent(jep.getDocument()));
            }
        });
    };

    public void testSetGetTextHTML() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    jep.setContentType("text/html");
                } catch (UnsupportedOperationException e) {
                    ex = e;
                }
            }
        });
        if (ex != null) {
            return;
        }
        jep.setText(JEditorPaneTest.htmlString);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                assertEquals(JEditorPaneTest.removeMeta(JEditorPaneTest.htmlString),
                        JEditorPaneTest.removeMeta(jep.getText().replaceAll("\n", "")));
            }
        });
    };
}