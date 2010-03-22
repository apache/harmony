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
package javax.swing.text;

import javax.swing.BasicSwingTestCase;
import javax.swing.SerializableTestCase;
import javax.swing.text.AbstractDocumentTest.DisAbstractedDocument;
import org.apache.harmony.x.swing.StringConstants;

public class AbstractDocument_SerializationTest extends SerializableTestCase {
    private AbstractDocument doc;

    private final String key = "key";

    private final String value = "value";

    private final String text = "text\n" + AbstractDocument_UpdateTest.RTL;

    @Override
    protected void setUp() throws Exception {
        toSave = doc = new DisAbstractedDocument(new GapContent());
        doc.insertString(0, text, null);
        doc.putProperty(key, value);
        doc.setDocumentFilter(new AbstractDocument_FilterTest.Filter());
        super.setUp();
    }

    @Override
    public void testSerializable() throws BadLocationException {
        AbstractDocument restored = (AbstractDocument) toLoad;
        assertEquals(text, restored.getText(0, restored.getLength()));
        assertEquals(value, restored.getProperty(key));
        assertEquals(Boolean.TRUE, restored
                .getProperty(BasicSwingTestCase.isHarmony() ? StringConstants.BIDI_PROPERTY
                        : "i18n"));
        if (BasicSwingTestCase.isHarmony()) {
            AbstractDocument_ListenerTest listener = new AbstractDocument_ListenerTest();
            restored.addDocumentListener(listener);
            restored.addUndoableEditListener(listener);
            restored.insertString(0, "234", null);
            // 12345 must be inserted instead of only 234 'cause of filter
            assertEquals("12345" + text, restored.getText(0, restored.getLength()));
            // Event handlers should be called
            assertNotNull(listener.insert);
            assertNotNull(listener.undo);
            assertEquals(0, restored.getStartPosition().getOffset());
            assertEquals(restored.getLength() + 1, restored.getEndPosition().getOffset());
            // Test readers are not null
            restored.readLock();
            restored.readUnlock();
        }
    }
}