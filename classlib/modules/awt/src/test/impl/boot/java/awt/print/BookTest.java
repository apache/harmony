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

package java.awt.print;

import java.awt.Graphics;

import junit.framework.TestCase;

public class BookTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BookTest.class);
    }

    /**
     * Test method for
     * {@link java.awt.print.Book#setPage(int, java.awt.print.Printable, java.awt.print.PageFormat)}.
     */
    public void testSetPage() {
        // Regression test for HARMONY-2433
        final Book d = new Book();
        final PageFormat pf = new PageFormat();
        final Printable p = new Printable() {
            public int print(Graphics g, PageFormat pf, int i) {
                return PAGE_EXISTS;
            }
        };

        try {
            d.setPage(1, p, pf);
            fail("IndexOutOfBoundsException was not thrown"); //$NON-NLS-1$
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }

        try {
            d.setPage(1, null, pf);
            fail("NullPointerException was not thrown"); //$NON-NLS-1$
        } catch (NullPointerException ex) {
            // expected
        }

        try {
            d.setPage(1, p, null);
            fail("NullPointerException was not thrown"); //$NON-NLS-1$
        } catch (NullPointerException ex) {
            // expected
        }
    }
}
