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
 * @author Pavel Dolgov
 */
package java.awt;

import java.util.NoSuchElementException;

import junit.framework.TestCase;

public class MenuRTest extends TestCase {

    public void testInsertNullMenuItem() {

        boolean npe = false;
        Menu m = new Menu();
        try {
            MenuItem mi = null;
            m.insert(mi, 1);
        } catch (NullPointerException e) {
            npe = true;
        }
        assertTrue(npe);
    }

    public void testInsertIndexLessThanZero() {

        boolean iae = false;
        Menu m = new Menu();
        try {
            MenuItem mi = new MenuItem();
            m.insert(mi, -1);
        } catch (IllegalArgumentException e) {
            iae = true;
        }
        assertTrue(iae);
    }

    public void testInsertIndexTooBig() {

        boolean nse = false;
        Menu m = new Menu();
        try {
            MenuItem mi = new MenuItem();
            m.insert(mi, 1);
        } catch (NoSuchElementException e) {
            nse = true;
        }
        assertFalse(nse);
    }

    public void testRemoveNull() {

        boolean npe = false;
        Menu m = new Menu();
        try {
            m.remove(null);
        } catch (NullPointerException e) {
            npe = true;
        }
        assertFalse(npe);
    }

}
