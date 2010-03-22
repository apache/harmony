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
 * @author Dennis Ushakov
 */
package javax.swing;

@SuppressWarnings("unchecked")
public class SizeSequenceTest extends BasicSwingTestCase {
    private SizeSequence sizeSequence;

    private int[] sizes;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sizes = new int[] { 1, 2, 3, 4, 5 };
        sizeSequence = new SizeSequence(sizes);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        sizeSequence = null;
        sizes = null;
    }

    public void testSizeSequence() throws Exception {
        sizeSequence = new SizeSequence();
        assertEquals(0, sizeSequence.getSizes().length);
        sizeSequence = new SizeSequence(10);
        assertEquals(10, sizeSequence.getSizes().length);
        assertEquals(10, sizeSequence.getIndex(0));
        for (int i = 0; i < 10; i++) {
            assertEquals(0, sizeSequence.getSizes()[i]);
        }
        sizeSequence = new SizeSequence(10, 13);
        assertEquals(10, sizeSequence.getSizes().length);
        for (int i = 0; i < 10; i++) {
            assertEquals(13, sizeSequence.getSizes()[i]);
        }
        int[] sizes = new int[] { 1, 2, 3, 4, 5 };
        sizeSequence = new SizeSequence(sizes);
        assertEquals(5, sizeSequence.getSizes().length);
        assertNotSame(sizes, sizeSequence.getSizes());
    }

    public void testGetIndex() {
        assertEquals(2, sizeSequence.getIndex(3));
        assertEquals(3, sizeSequence.getIndex(6));
        assertEquals(4, sizeSequence.getIndex(11));
        assertEquals(5, sizeSequence.getIndex(100));
        assertEquals(0, sizeSequence.getIndex(-100));
    }

    public void testGetPosition() {
        assertEquals(0, sizeSequence.getPosition(0));
        assertEquals(1, sizeSequence.getPosition(1));
        assertEquals(3, sizeSequence.getPosition(2));
        assertEquals(6, sizeSequence.getPosition(3));
        assertEquals(10, sizeSequence.getPosition(4));
        assertEquals(15, sizeSequence.getPosition(100));
        assertEquals(0, sizeSequence.getPosition(-100));
        sizeSequence = new SizeSequence();
        assertEquals(0, sizeSequence.getPosition(5));
    }

    public void testGetSetSize() {
        assertEquals(1, sizeSequence.getSize(0));
        sizeSequence.setSize(0, 10);
        assertEquals(10, sizeSequence.getSize(0));
        assertEquals(10, sizeSequence.getPosition(1));
        assertEquals(12, sizeSequence.getPosition(2));
        assertEquals(24, sizeSequence.getPosition(5));
        assertEquals(1, sizeSequence.getIndex(11));
        assertEquals(3, sizeSequence.getIndex(15));
        sizeSequence.setSize(0, -10);
        assertEquals(-10, sizeSequence.getSize(0));
        assertEquals(-10, sizeSequence.getPosition(1));
        sizeSequence.setSize(10, 1);
        assertEquals(0, sizeSequence.getSize(10));
    }

    public void testGetSetSizes() {
        int[] first = sizeSequence.getSizes();
        assertNotSame(first, sizeSequence.getSizes());
        sizes[0] = 5;
        assertEquals(1, sizeSequence.getSizes()[0]);
        sizes = new int[] { 1, 2, 3, 4, 5, 6 };
        sizeSequence.setSizes(sizes);
        assertEquals(sizes.length, sizeSequence.getSizes().length);
        assertNotSame(sizes, sizeSequence.getSizes());
        for (int i = 0; i < sizes.length; i++) {
            assertEquals(sizes[i], sizeSequence.getSizes()[i]);
            assertEquals(sizes[i], sizeSequence.getSize(i));
        }
    }

    public void testInsertRemoveEntries() {
        sizeSequence.removeEntries(0, 3);
        assertEquals(2, sizeSequence.getSizes().length);
        assertEquals(5, sizeSequence.getSize(1));
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                sizeSequence.removeEntries(10, 11);
            }

            @Override
            public Class expectedExceptionClass() {
                return NegativeArraySizeException.class;
            }
        });
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                sizeSequence.removeEntries(10, -1);
            }

            @Override
            public Class expectedExceptionClass() {
                return ArrayIndexOutOfBoundsException.class;
            }
        });
        sizeSequence = new SizeSequence(sizes);
        sizeSequence.insertEntries(2, 3, 2);
        assertEquals(3 + sizes.length, sizeSequence.getSizes().length);
        assertEquals(2, sizeSequence.getSize(2));
        assertEquals(2, sizeSequence.getIndex(3));
        assertEquals(5, sizeSequence.getSize(7));
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                sizeSequence.insertEntries(10, 11, 0);
            }

            @Override
            public Class expectedExceptionClass() {
                return ArrayIndexOutOfBoundsException.class;
            }
        });
        testExceptionalCase(new ExceptionalCase() {
            @Override
            public void exceptionalAction() throws Exception {
                sizeSequence.insertEntries(0, -1, 0);
            }

            @Override
            public Class expectedExceptionClass() {
                return ArrayIndexOutOfBoundsException.class;
            }
        });
    }
}
