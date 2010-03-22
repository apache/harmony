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

import java.text.ParseException;
import javax.swing.SwingTestCase;
import javax.swing.JFormattedTextField;

public class MaskFormatterTest extends SwingTestCase {
    MaskFormatter formatter;

    boolean bWasException;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        formatter = new MaskFormatter();
        bWasException = false;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testMaskFormatter() {
        assertNull(formatter.getMask());
        checkMainProperties();
    }

    private void checkMainProperties() {
        assertEquals(' ', formatter.getPlaceholderCharacter());
        assertNull(formatter.getInvalidCharacters());
        assertNull(formatter.getValidCharacters());
        assertNull(formatter.getPlaceholder());
        assertTrue(formatter.getValueContainsLiteralCharacters());
        assertFalse(formatter.getAllowsInvalid());
        assertTrue(formatter.getOverwriteMode());
        assertFalse(formatter.getCommitsOnValidEdit());
        assertNull(formatter.getValueClass());
    }

    public void testMaskFormatterString() {
        String mask = "#`ULA?*H";
        try {
            formatter = new MaskFormatter(mask);
            assertEquals(mask, formatter.getMask());
            checkMainProperties();
            mask = "#`ula?*h";
            formatter = new MaskFormatter(mask);
            assertEquals(mask, formatter.getMask());
            checkMainProperties();
        } catch (ParseException e) {
            assertFalse("Unexpected exception: " + e.getMessage(), true);
        }
        //API says that's it's possible ParseException in this constructor.
        //I don't know when it's possible.
    }

    public void testSetGetMask() {
        String mask = "0xH-H";
        try {
            formatter.setMask(mask);
            assertEquals(mask, formatter.getMask());
            assertEquals("0xa-B", formatter.stringToValue("0xa-B"));
        } catch (ParseException e) {
            assertFalse("Unexpected exception: " + e.getMessage(), true);
        }
        try {
            formatter.stringToValue("ddd");
        } catch (ParseException e) {
            bWasException = true;
        }
        assertTrue(bWasException);
    }

    public void testSetGetInvalidCharacters() {
        try {
            formatter.setMask("*****");
            formatter.setInvalidCharacters("#2");
            assertEquals("#2", formatter.getInvalidCharacters());
            formatter.stringToValue("rrrr5");
        } catch (ParseException e) {
            assertFalse("Unexpected exception: " + e.getMessage(), true);
        }
        try {
            formatter.stringToValue("#rra");
        } catch (ParseException e) {
            bWasException = true;
        }
        assertTrue(bWasException);
    }

    public void testSetGetPlaceholder() {
        try {
            formatter.setMask("HHH$%$HHH^^^HHH");
            formatter.setPlaceholder("fff$%$fff^^^fff");
            assertEquals("fff$%$fff^^^fff", formatter.getPlaceholder());
            assertEquals("111$%$fff^^^fff", formatter.valueToString("111"));
            formatter.setPlaceholderCharacter('*');
            assertEquals("111$%$fff^^^fff", formatter.valueToString("111"));
            formatter.setPlaceholder("123$%$abc");
            assertEquals("123$%$abc", formatter.getPlaceholder());
            assertEquals("AAA$%$abc^^^***", formatter.valueToString("AAA"));
        } catch (ParseException e) {
            assertFalse("Unexpected exception: " + e.getMessage(), true);
        }
    }

    public void testSetGetPlaceholderCharacter() {
        try {
            formatter.setMask("HHH$%$HHH^^^HHH");
            formatter.setPlaceholderCharacter('&');
            assertEquals('&', formatter.getPlaceholderCharacter());
            assertEquals("456$%$6&&^^^&&&", formatter.valueToString("456$%$6"));
            formatter.setPlaceholder("123$%$abc");
            assertEquals("456$%$6bc^^^&&&", formatter.valueToString("456$%$6"));
        } catch (ParseException e) {
            assertFalse("Unexpected exception: " + e.getMessage(), true);
        }
    }

    public void testSetGetValidCharacters() {
        try {
            formatter.setMask("****");
            formatter.setValidCharacters("012345678abc");
            assertEquals("012345678abc", formatter.getValidCharacters());
            formatter.stringToValue("acb4");
        } catch (ParseException e) {
            assertFalse("Unexpected exception: " + e.getMessage(), true);
        }
        try {
            formatter.stringToValue("rra");
        } catch (ParseException e) {
            bWasException = true;
        }
        assertTrue(bWasException);
    }

    public void testSetGetValidInvalidCharacters() {
        try {
            formatter.setMask("****");
            formatter.setInvalidCharacters("a");
            formatter.setValidCharacters("a");
            formatter.stringToValue("a");
        } catch (ParseException e) {
            bWasException = true;
        }
        assertTrue(bWasException);
    }

    public void testSetGetValueContainsLiteralCharacters() {
        try {
            formatter.setMask("(###) ###-####");
            String text = "(415) 555-1212";
            assertEquals(text, formatter.stringToValue(text));
            formatter.setValueContainsLiteralCharacters(false);
            assertEquals("4155551212", formatter.stringToValue(text));
        } catch (ParseException e) {
            assertFalse("Unexpected exception: " + e.getMessage(), true);
        }
    }

    public void testSetIncorrectPlaceHolder() {
        try {
            formatter.setMask("##-##-##");
            String placeholder = "asdkahsdkjahs";
            formatter.setPlaceholder(placeholder);
            assertEquals(placeholder, formatter.getPlaceholder());
            assertEquals("26-ka-sd", formatter.valueToString("26"));
        } catch (ParseException e) {
            assertFalse("Unexpected exception: " + e.getMessage(), true);
        }
    }

    public void testSetPlaceHolderWithInvalidCharacters() {
        try {
            formatter.setMask("##-##-##");
            formatter.setValidCharacters("12");
            String placeholder = "asdkahsdkjahs";
            formatter.setPlaceholder(placeholder);
            assertEquals(placeholder, formatter.getPlaceholder());
            assertEquals("21-ka-sd", formatter.valueToString("21"));
        } catch (ParseException e) {
            assertFalse("Unexpected exception: " + e.getMessage(), true);
        }
    }

    public void testValueToString_InvalidValue() {
        try {
            formatter.setMask("-^##-##-##");
            formatter.valueToString("23");
        } catch (ParseException e) {
            bWasException = true;
        }
        checkException();
    }

    public void testValueToString_ValueWithoutLiterals() {
        try {
            formatter.setMask("-^##-##-##");
            formatter.setValueContainsLiteralCharacters(false);
            formatter.setPlaceholderCharacter('*');
            assertEquals("-^23-**-**", formatter.valueToString("23"));
        } catch (ParseException e) {
            assertFalse("Unexpected exception: " + e.getMessage(), true);
        }
    }

    public void testSetMask_Escape_StringToValue() {
        try {
            formatter.setMask("'Uw''et'67H'H");
            formatter.stringToValue("Uw'et678H");
        } catch (ParseException e) {
            assertFalse("Unexpected exception: " + e.getMessage(), true);
        }
    }

    public void testSetMask_Escape_AtTheEnd() {
        try {
            formatter.setMask("567'");
            assertEquals("567'", formatter.getMask());
            assertEquals("567", formatter.stringToValue("567"));
        } catch (ParseException e) {
            assertFalse("Unexpected exception: " + e.getMessage(), true);
        }
        try {
            formatter.stringToValue("567'");
        } catch (ParseException e) {
            bWasException = true;
        }
        checkException();
    }

    public void testValueToString_ESCAPE() {
        try {
            formatter.setMask("'U456H'''H");
            assertEquals("U4567'H", formatter.valueToString("U4567'H"));
            formatter.setValueContainsLiteralCharacters(false);
            assertEquals("U4567'H", formatter.valueToString("7"));
        } catch (ParseException e) {
            assertFalse("Unexpected exception: " + e.getMessage(), true);
        }
    }

    public void testValueToString_ESCAPE_ParseException() {
        try {
            formatter.setMask("'U456H'''H");
            formatter.valueToString("4567'H");
        } catch (ParseException e) {
            bWasException = true;
        }
        checkException();
        try {
            formatter.setValueContainsLiteralCharacters(false);
            formatter.valueToString("y3");
        } catch (ParseException e) {
            bWasException = true;
        }
        checkException();
    }

    private void checkException() {
        assertTrue(bWasException);
        bWasException = false;
    }

    public void testGetNavigationFilter() {
        assertNotNull(formatter.getNavigationFilter());
    }

    public void testStringToValue_InvalidPlaceHolderChar() {
        try {
            formatter.setMask("UUU-UUU");
            formatter.setPlaceholder("aaaaa");
            formatter.setPlaceholderCharacter('x');
        } catch (ParseException e) {
            assertTrue("Unexpected exception: " + e.getMessage(), false);
        }
    }
   
    public void testValueToString_Object() throws ParseException{
    	// Regression for HARMONY-1742 
    	MaskFormatter obj = new MaskFormatter();
        obj.valueToString(new Object());
    } 
    public void testInstall_JFormattedTextField() {
    	// Regression for HARMONY-1742 
        MaskFormatter obj = new MaskFormatter();
        obj.install(new JFormattedTextField());
    }
}
