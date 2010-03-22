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
 * @author Elena V. Sayapina 
 */ 

package javax.print.attribute.standard;

import javax.print.attribute.HashAttributeSet;

import junit.framework.TestCase;

public class MediaTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(MediaTest.class);
    }

    static {
        System.out.println("Media testing...");
    }

    /*
     * equals(Object object) method testing.
     */
    public final void testEqualsObject() {
        Media name = MediaName.NA_LETTER_WHITE;
        Media sizename = MediaSizeName.INVOICE;
        assertTrue(name.equals(MediaName.NA_LETTER_WHITE));
        assertFalse(name.equals(sizename));

        name = new mediaName(1);
        Media tray = new mediaTray(1);
        assertFalse(name.equals(tray));

        sizename = null;
        assertFalse(name.equals(sizename));
    }

    /*
     * getCategory() method testing.
     */
    public final void testGetCategory() {
        Media m = new media(1);
        assertEquals(Media.class, m.getCategory());
    }

    /*
     * getName() method testing.
     */
    public final void testGetName() {
        Media m = new media(1);
        assertEquals("media", m.getName());
    }


    /*
     * Test that MadiaName, MediaSizeName, MediaTray are
     * fall into the Media attribute Category
     */
    public final void testMediaCategory() {

        HashAttributeSet aset = new HashAttributeSet();

        MediaSizeName msn = MediaSizeName.ISO_A3;
        aset.add(msn);
        assertEquals(msn, aset.get(Media.class));
        assertNull(aset.get(MediaSizeName.class));

        MediaTray mt = MediaTray.BOTTOM;
        aset.add(mt);
        assertEquals(mt, aset.get(Media.class));
        assertNull(aset.get(MediaTray.class));

        MediaName mn = MediaName.ISO_A4_WHITE;
        aset.add(mn);
        assertEquals(mn, aset.get(Media.class));
        assertNull(aset.get(MediaName.class));
    }


    /*
     * Auxiliary class
     */
    public class media extends Media {

        public media(int value) {
            super(value);
        }
    }

    /*
     * Auxiliary class
     */
    public class mediaName extends MediaName {

        public mediaName(int value) {
            super(value);
        }
    }

    /*
     * Auxiliary class
     */
    public class mediaTray extends MediaTray {

        public mediaTray(int value) {
            super(value);
        }
    }
}
