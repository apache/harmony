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
 * Created on 11.11.2004

 */
package javax.swing.event;

import junit.framework.TestCase;

public class CaretEventTest extends TestCase {
    SimpleCaretEvent sce;

    Object obj;

    class SimpleCaretEvent extends CaretEvent {
        private static final long serialVersionUID = 1L;

        public SimpleCaretEvent(final Object obj) {
            super(obj);
        }

        @Override
        public int getMark() {
            return 0;
        }

        @Override
        public int getDot() {
            return 0;
        }
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        obj = new Object();
        sce = new SimpleCaretEvent(obj);
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCaretEvent() {
        assertNotNull(sce);
        assertEquals(sce.getSource(), obj);
    }
}
