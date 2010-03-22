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

import java.lang.ref.WeakReference;
import javax.swing.SerializableTestCase;

public class GapContent_SerializeTest extends SerializableTestCase {
    private GapContent content;

    @Override
    protected void setUp() throws Exception {
        toSave = content = new GapContent();
        content.insertString(0, "01234");
        content.insertString(5, "abcde");
        content.insertString(5, "!");
        super.setUp();
    }

    @Override
    public void testSerializable() throws Exception {
        GapContent restored = (GapContent) toLoad;
        assertEquals(content.length(), restored.length());
        assertEquals(content.getArrayLength(), restored.getArrayLength());
        assertEquals(content.getGapStart(), restored.getGapStart());
        assertEquals(content.getGapEnd(), restored.getGapEnd());
        assertEquals(content.getString(0, content.length()), restored.getString(0, restored
                .length()));
        // Test that position handling works the way it is supposed
        Position pos = restored.createPosition(5);
        WeakReference<Position> wr = new WeakReference<Position>(pos);
        assertEquals(5, pos.getOffset());
        restored.insertString(0, "aStr");
        assertEquals(9, pos.getOffset());
    }
}
