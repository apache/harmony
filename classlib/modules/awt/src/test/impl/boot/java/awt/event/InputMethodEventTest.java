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
 * @author Michael Danilov
 */
package java.awt.event;

import java.awt.Button;
import java.text.AttributedCharacterIterator;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;

public class InputMethodEventTest extends TestCase {

    public final void testInputMethodEventComponentintTextHitInfoTextHitInfo() {
        Button button = new Button();
        InputMethodEvent event = new InputMethodEvent(button,
                InputMethodEvent.CARET_POSITION_CHANGED,
                null, null);

        assertEquals(event.getSource(), button);
        assertEquals(event.getID(), InputMethodEvent.CARET_POSITION_CHANGED);
        assertNull(event.getText());
        assertEquals(event.getWhen(), 0);
        assertNull(event.getCaret());
        assertEquals(event.getCommittedCharacterCount(), 0);
        assertNull(event.getVisiblePosition());
        assertFalse(event.isConsumed());

        boolean wrongID = false;
        try {
            event = new InputMethodEvent(button,
                    InputMethodEvent.CARET_POSITION_CHANGED + 1024,
                    null, null);
        } catch (IllegalArgumentException e) {
            wrongID = true;
        }
        assertTrue(wrongID);
    }

    public final void testInputMethodEventComponentintAttributedCharacterIteratorintTextHitInfoTextHitInfo() {
        Button button = new Button();
        InputMethodEvent event = new InputMethodEvent(button,
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                text, 0, null, null);

        assertEquals(event.getSource(), button);
        assertEquals(event.getID(), InputMethodEvent.INPUT_METHOD_TEXT_CHANGED);
        assertEquals(event.getText(), text);
        assertEquals(event.getWhen(), 0);
        assertNull(event.getCaret());
        assertEquals(event.getCommittedCharacterCount(), 0);
        assertNull(event.getVisiblePosition());
        assertFalse(event.isConsumed());

        boolean wrongID = false;
        try {
            event = new InputMethodEvent(button,
                    InputMethodEvent.INPUT_METHOD_TEXT_CHANGED + 1024,
                    text, 0, null, null);
        } catch (IllegalArgumentException e) {
            wrongID = true;
        }
        assertTrue(wrongID);

        boolean nullText = false;
        try {
            event = new InputMethodEvent(button,
                    InputMethodEvent.CARET_POSITION_CHANGED,
                    text, 0, null, null);
        } catch (IllegalArgumentException e) {
            nullText = true;
        }
        assertTrue(nullText);

        boolean wrongCount = false;
        try {
            event = new InputMethodEvent(button,
                    InputMethodEvent.CARET_POSITION_CHANGED,
                    text, 10, null, null);
        } catch (IllegalArgumentException e) {
            wrongCount = true;
        }
        assertTrue(wrongCount);
    }

    public final void testInputMethodEventComponentintlongAttributedCharacterIteratorintTextHitInfoTextHitInfo() {
        Button button = new Button();
        InputMethodEvent event = new InputMethodEvent(button,
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, 1000000000,
                text, 0, null, null);

        assertEquals(event.getSource(), button);
        assertEquals(event.getID(), InputMethodEvent.INPUT_METHOD_TEXT_CHANGED);
        assertEquals(event.getText(), text);
        assertEquals(event.getWhen(), 1000000000);
        assertNull(event.getCaret());
        assertEquals(event.getCommittedCharacterCount(), 0);
        assertNull(event.getVisiblePosition());
        assertFalse(event.isConsumed());

        boolean wrongID = false;
        try {
            event = new InputMethodEvent(button,
                    InputMethodEvent.CARET_POSITION_CHANGED + 1024, 1000000000,
                    text, 0, null, null);
        } catch (IllegalArgumentException e) {
            wrongID = true;
        }
        assertTrue(wrongID);

        boolean nullText = false;
        try {
            event = new InputMethodEvent(button,
                    InputMethodEvent.CARET_POSITION_CHANGED, 1000000000,
                    text, 0, null, null);
        } catch (IllegalArgumentException e) {
            nullText = true;
        }
        assertTrue(nullText);

        boolean wrongCount = false;
        try {
            event = new InputMethodEvent(button,
                    InputMethodEvent.CARET_POSITION_CHANGED, 1000000000,
                    text, 10, null, null);
        } catch (IllegalArgumentException e) {
            wrongCount = true;
        }
        assertTrue(wrongCount);
    }


    public final void testIsConsuming() {
        Button button = new Button("Button");
        InputMethodEvent event = new InputMethodEvent(button,
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, 1000000000,
                text, 0, null, null);

        assertFalse(event.isConsumed());
        event.consume();
        assertTrue(event.isConsumed());
    }

    public final void testParamString() {
        Button button = new Button("Button");
        InputMethodEvent event = new InputMethodEvent(button,
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, 1000000000,
                text, 0, null, null);

        assertTrue(event.paramString().indexOf("INPUT_METHOD_TEXT_CHANGED,text=java.awt.event.InputMethodEventTest") != -1);
        assertTrue(event.paramString().indexOf(",commitedCharCount=0,caret=null,visiblePosition=null") != -1);
    }

    static final AttributedCharacterIterator text = new AttributedCharacterIterator() {
        public int getRunStart() {
            return 0;
        }

        public int getRunStart(Attribute arg0) {
            return 0;
        }

        public int getRunStart(Set<? extends Attribute> arg0) {
            return 0;
        }

        public int getRunLimit() {
            return 0;
        }

        public int getRunLimit(Attribute arg0) {
            return 0;
        }

        public int getRunLimit(Set<? extends Attribute> arg0) {
            return 0;
        }

        public Set<Attribute> getAllAttributeKeys() {
            return null;
        }

        public Object getAttribute(Attribute arg0) {
            return null;
        }

        public Map<Attribute, Object> getAttributes() {
            return null;
        }

        public char first() {
            return 0;
        }

        public char last() {
            return 0;
        }

        public char current() {
            return 0;
        }

        public char next() {
            return 0;
        }

        public char previous() {
            return 0;
        }

        public char setIndex(int arg0) {
            return 0;
        }

        public int getBeginIndex() {
            return 0;
        }

        public int getEndIndex() {
            return 0;
        }

        public int getIndex() {
            return 0;
        }

        @Override
        public Object clone() {
            return null;
        }
    };
}
