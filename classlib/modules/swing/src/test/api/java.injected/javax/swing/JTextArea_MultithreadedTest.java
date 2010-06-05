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

import java.awt.Container;
import java.awt.GridLayout;
import junit.framework.TestCase;

public class JTextArea_MultithreadedTest extends TestCase {
    JFrame jf;

    ExtJTextArea jta;

    JTextArea bidiJta;

    JTextArea jt;

    String sLTR = "aaaa";

    String tmp;

    String sRTL = "\u05dc" + "\u05dc" + "\u05dc" + "\u05dc";

    String content = "Edison accumul\tator, Edison base: Edison battery"
            + " Edison cap, \tEdison effect\n"
            + "Edison screw, Edison screw cap, Edison screw \n"
            + "holder, Edison screw lampholder, Edison screw " + "plug\n"
            + "Edison screw terminal, Edison storage battery" + "Edison storage \t\tcell";

    String bidiContent = sLTR + sRTL + sRTL + " \t" + sLTR + sRTL + sLTR + "\n" + sRTL + "."
            + sLTR + sRTL + "\t" + sRTL + "\n" + sLTR + sLTR + sRTL + sRTL + sRTL + sLTR + sLTR
            + sLTR + sRTL + sLTR + sRTL + sLTR;

    String str1 = "jazz band";

    String str2 = "jazz model";

    boolean bWasException;

    String message;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        bWasException = false;
        message = null;
        jf = new JFrame();
        bidiJta = new JTextArea(bidiContent);
        jta = new ExtJTextArea(content);
        jt = new JTextArea(bidiContent);
        Container cont = jf.getContentPane();
        cont.setLayout(new GridLayout(1, 2, 4, 4));
        cont.add(jta);
        cont.add(bidiJta);
        jf.setLocation(200, 300);
        jf.setSize(350, 400);
        jf.pack();
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    String replaceRange(final String s1, final String s2, final int start, final int end) {
        String tmp = s2 == null ? "" : s2;
        return s1.substring(0, start) + tmp + s1.substring(end, s1.length());
    }

    public void testReplaceRange() throws Exception {
        jta.replaceRange(str1, 5, 10);
        tmp = replaceRange(content, str1, 5, 10);
        assertEquals(tmp, jta.getText());
        jta.replaceRange(null, 5, 10);
        tmp = replaceRange(tmp, null, 5, 10);
        assertEquals(tmp, jta.getText());
        jta.replaceRange("", 5, 10);
        tmp = replaceRange(tmp, "", 5, 10);
        assertEquals(tmp, jta.getText());
        try {
            jta.replaceRange(str2, -1, 5);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("Invalid remove", message);
        bWasException = false;
        message = null;
        try {
            jta.replaceRange(str2, 1, tmp.length() + 1);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("Invalid remove", message);
        bWasException = false;
        message = null;
        try {
            jta.replaceRange(str2, 10, 5);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("end before start", message);
    }

    String insertString(final String s1, final String s2, final int index) {
        return s1.substring(0, index) + s2 + s1.substring(index, s1.length());
    }

    public void testInsert() throws Exception {
        jta.insert(str1, 5);
        tmp = insertString(content, str1, 5);
        assertEquals(tmp, jta.getText());
        jta.insert(null, 5);
        assertEquals(tmp, jta.getText());
        jta.insert("", 5);
        assertEquals(tmp, jta.getText());
        try {
            jta.insert(str2, -1);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("Invalid insert", message);
        bWasException = false;
        message = null;
        try {
            jta.insert(str2, tmp.length() + 1);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("Invalid insert", message);
        bWasException = false;
        message = null;
        try {
            jta.insert(null, tmp.length() + 1);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertFalse(bWasException);
        assertNull(message);
    }

    public void testAppend() throws Exception {
        jta.append(str1);
        tmp = content + str1;
        assertEquals(tmp, jta.getText());
        jta.append(null);
        assertEquals(tmp, jta.getText());
        jta.append("");
        assertEquals(tmp, jta.getText());
    }
}