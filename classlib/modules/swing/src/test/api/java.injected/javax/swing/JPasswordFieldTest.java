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

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

public class JPasswordFieldTest extends SwingTestCase {
    JPasswordField pf;

    boolean bWasException;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        pf = new JPasswordField();
        bWasException = false;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCopy() {
        // TODO: uncomment when System clipboard is properly supported
        //        pf.setText("abcdef");
        //        setContentInSystemClipboard("1234");
        //        pf.copy();
        //        assertEquals("1234", getSystemClipboardContent());
    }

    public void testCut() {
        // TODO: uncomment when System clipboard is properly supported
        //        pf.setText("abcdef");
        //        setContentInSystemClipboard("1234");
        //        pf.cut();
        //        assertEquals("1234", getSystemClipboardContent());
        //        assertEquals("abcdef", pf.getText());
    }

    public void testJPasswordField() {
        assertTrue(pf.getDocument() instanceof PlainDocument);
        assertEquals(0, pf.getColumns());
        assertEquals("", pf.getText());
    }

    public void testJPasswordFieldString() {
        pf = new JPasswordField(null);
        assertTrue(pf.getDocument() instanceof PlainDocument);
        assertEquals(0, pf.getColumns());
        assertEquals("", pf.getText());
        pf = new JPasswordField("abc");
        assertTrue(pf.getDocument() instanceof PlainDocument);
        assertEquals(0, pf.getColumns());
        assertEquals("abc", pf.getText());
    }

    public void testJPasswordFieldStringint() {
        pf = new JPasswordField("abc", 5);
        assertTrue(pf.getDocument() instanceof PlainDocument);
        assertEquals(5, pf.getColumns());
        assertEquals("abc", pf.getText());
    }

    public void testJPasswordFieldint() {
        pf = new JPasswordField(5);
        assertTrue(pf.getDocument() instanceof PlainDocument);
        assertEquals(5, pf.getColumns());
        assertEquals("", pf.getText());
    }

    public void testJPasswordFieldDocumentStringint() {
        Document doc = new PlainDocument();
        pf = new JPasswordField(doc, "abc", 5);
        assertEquals(doc, pf.getDocument());
        assertEquals(5, pf.getColumns());
        assertEquals("abc", pf.getText());
    }

    public void testEchoCharIsSet() {
        assertTrue(pf.echoCharIsSet());
        pf.setEchoChar('\0');
        assertFalse(pf.echoCharIsSet());
    }

    public void testGetAccessibleContext() {
        AccessibleContext context = pf.getAccessibleContext();
        assertTrue(context instanceof JPasswordField.AccessibleJPasswordField);
        assertEquals(AccessibleRole.PASSWORD_TEXT, context.getAccessibleRole());
    }

    public void testSetGetEchoChar() {
        char echoChar = '*';
        assertEquals(echoChar, pf.getEchoChar());
        echoChar = 'j';
        pf.setEchoChar(echoChar);
        assertEquals(echoChar, pf.getEchoChar());
        echoChar = '\0';
        pf.setEchoChar(echoChar);
        assertEquals(echoChar, pf.getEchoChar());
    }

    public void testGetPassword() {
        String content = "abcd";
        pf.setText("abcd");
        char[] pwd = pf.getPassword();
        for (int i = 0; i < pwd.length; i++) {
            assertEquals(content.charAt(i), pwd[i]);
        }
    }

    public void testGetText() {
        pf.setText("abcd");
        assertEquals("abcd", pf.getText());
    }

    private void checkException() {
        assertTrue(bWasException);
        bWasException = false;
    }

    public void testGetTextintint() {
        pf.setText("abcd");
        try {
            assertEquals("", pf.getText(1, 0));
            assertEquals("c", pf.getText(2, 1));
        } catch (BadLocationException e) {
            assertTrue("Unexpected exception: ", false);
        }
        try {
            pf.getText(100, 3);
        } catch (BadLocationException e) {
            bWasException = true;
        }
        checkException();
        try {
            pf.getText(1, -63);
        } catch (BadLocationException e) {
            bWasException = true;
        }
        checkException();
    }

    public void testGetUIClassID() {
        assertEquals("PasswordFieldUI", pf.getUIClassID());
    }
}
