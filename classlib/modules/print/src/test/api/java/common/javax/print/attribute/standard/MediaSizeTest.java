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

import org.apache.harmony.x.print.attributes.PPDMediaSizeName;

import junit.framework.TestCase;

@SuppressWarnings("static-access")
public class MediaSizeTest extends TestCase {

    /*
     * MediaSize constructor testing.
     */
    public final void testMediaSize() {
        MediaSize  ms = new MediaSize(200, 300, 1000, MediaSizeName.A);
        assertEquals(200, (int) ms.getX(1000));
        assertEquals(300, (int) ms.getY(1000));

        assertFalse(200 == (int) MediaSize.Engineering.A.getX(1000));
        assertFalse(300 == (int) MediaSize.Engineering.A.getY(1000));
    }

    /*
     * equals(Object object) method testing.
     */
    public final void testEquals() {

        MediaSize ms1 = MediaSize.ISO.B0;
        MediaSize ms2 = MediaSize.ISO.B0;
        assertTrue(ms1.equals(ms1));
        ms2 = MediaSize.JIS.B0;
        assertFalse(ms1.equals(ms2));
        ms2 = null;
        assertFalse(ms1.equals(ms2));
    }


    /*
     * findMedia(float x, float y, int units) method testing.
     * Tests that findMedia(..) gives correct results on big figures.
     */
    public final void testFindMedia() {

        MediaSizeName msn1 = new mediaSizeName(111);
        MediaSizeName msn2 = new mediaSizeName(112);
        MediaSizeName msn3 = new mediaSizeName(113);
        MediaSize ms1 = new MediaSize(Integer.MAX_VALUE / 15,
                Integer.MAX_VALUE / 15, 1, msn1);
        
        MediaSize ms2 = new MediaSize(Integer.MAX_VALUE / 5,
                Integer.MAX_VALUE / 5, 1, msn2);
        MediaSize ms3 = new MediaSize(Integer.MAX_VALUE, Integer.MAX_VALUE, 1,
                msn3);
        assertEquals(msn2,
            MediaSize.findMedia(Integer.MAX_VALUE/4, Integer.MAX_VALUE/4, 1));
        assertEquals(msn3,
            MediaSize.findMedia(Float.MAX_VALUE, Float.MAX_VALUE, 1));
    }

    /*
     * findMedia(float x, float y, int units) method testing.
     * Tests that JIS_B0 is found as the biggest media from all 
     * standard media sheets and ISO_A10 is found as the smallest 
     * media from all standard media sheets. 
     */
    public final void testFindMedia1() {

        assertTrue((MediaSizeName.JIS_B0 ==
                MediaSize.findMedia(5000, 5000, MediaSize.MM)) ||
                (PPDMediaSizeName.B0 ==
                MediaSize.findMedia(5000, 5000, MediaSize.MM)));

        assertTrue((MediaSizeName.ISO_A10 ==
            MediaSize.findMedia(20, 20, MediaSize.MM)) ||
            (PPDMediaSizeName.A10 ==
            MediaSize.findMedia(20, 20, MediaSize.MM)));
    }

    /*
     * findMedia(float x, float y, int units) method testing. 
     * Simple check on standard media sheets.
     */
    public final void testFindMedia2() {

        assertEquals(MediaSizeName.EXECUTIVE,
            MediaSize.findMedia(7.25f, 10.5f, MediaSize.INCH));

        MediaSizeName found = MediaSize.findMedia(8.5f, 11f, MediaSize.INCH);
        if(found != MediaSizeName.A && found != MediaSizeName.NA_LETTER) {
            fail("MediaSize.findMedia(8.5f, 11f, MediaSize.INCH) = " +
                    MediaSize.findMedia(8.5f, 11f, MediaSize.INCH));
        }

        assertEquals(MediaSizeName.JAPANESE_DOUBLE_POSTCARD,
                MediaSize.findMedia(148, 200, MediaSize.MM));

        found = MediaSize.findMedia(11, 17, MediaSize.INCH);
        if(found != MediaSizeName.TABLOID && found != MediaSizeName.LEDGER
                                                && found != MediaSizeName.B) {
            fail("MediaSize.findMedia(11, 17, MediaSize.INCH) = " +
                    MediaSize.findMedia(8.5f, 11f, MediaSize.INCH));
        }

        assertEquals(MediaSizeName.NA_5X7,
                MediaSize.findMedia(5, 7, MediaSize.INCH));

    }

    /*
     * findMedia(float x, float y, int units) method testing.
     * Tests that findMedia(..) finds the closest media sheet. 
     */
    public final void testFindMedia3() {

        assertEquals(MediaSizeName.ISO_A4,
            MediaSize.findMedia(MediaSize.ISO.A4.getX(MediaSize.MM)+1,
                                MediaSize.ISO.A4.getY(MediaSize.MM)+1,
                                MediaSize.MM));

        MediaSizeName msn1 = new mediaSizeName(91);
        MediaSizeName msn2 = new mediaSizeName(92);
        MediaSize  ms1 = new MediaSize(0.01f, 0.01f, 100, msn1);
        MediaSize  ms2 = new MediaSize(0.02f, 0.02f, 10, msn2);

        assertEquals(msn2,
            MediaSize.findMedia(0.00017f, 0.00017f, 1));
    }

    /*
     * getCategory() method testing.
     */
    public final void testGetCategory() {
        MediaSize ms = MediaSize.ISO.A0;
        assertEquals(MediaSize.class, ms.getCategory());
    }

    /*
     * getMediaSizeForName(MediaSizeName media) method testing.
     * Tests that getMediaSizeForName(..) did mapping between MediaSize
     * and MediaSizeName, but standard media sizes didn't remove.
     */
    public final void testGetMediaSizeForName() {

        //new mapping
        MediaSize  ms1 = new MediaSize(1, 1, 100000, MediaSizeName.A);

        assertEquals(ms1,
            MediaSize.getMediaSizeForName(MediaSizeName.A));


        assertEquals(MediaSizeName.A,
            MediaSize.findMedia(1, 1, 100000));
        //System.out.println("findMedia(1, 1, 100000) " +
        //MediaSize.findMedia(1, 1 , 100000));

        MediaSizeName found = MediaSize.findMedia(8.5f, 11f, MediaSize.INCH);
        //System.out.println("findMedia(8.5, 11, MediaSize.INCH) = " +
        //MediaSize.findMedia(8.5f, 11f, MediaSize.INCH));
        if(found != MediaSizeName.A && found != MediaSizeName.NA_LETTER) {
            fail("findMedia(8.5, 11, MediaSize.INCH) = " +
                    MediaSize.findMedia(8.5f, 11f, MediaSize.INCH));
        }

        assertEquals(MediaSize.NA.LEGAL,
                MediaSize.getMediaSizeForName(MediaSizeName.NA_LEGAL));
        assertEquals(MediaSize.Other.LEDGER,
                MediaSize.getMediaSizeForName(MediaSizeName.LEDGER));

    }

    /*
     * getMediaSizeName() method testing.
     */
    public final void testGetMediaSizeName() {
        MediaSize ms = MediaSize.ISO.A0;
        assertEquals(MediaSizeName.ISO_A0, ms.getMediaSizeName());
        ms = MediaSize.JIS.B0;
        assertEquals(MediaSizeName.JIS_B0, ms.getMediaSizeName());

        ms = new MediaSize(30, 30, MediaSize.INCH);
        assertNull(ms.getMediaSizeName());
    }

    /*
     * getName() method testing.
     */
    public final void testGetName() {
        MediaSize ms = MediaSize.ISO.A0;
        assertEquals("media-size", ms.getName());
    }


    /*
     * Auxiliary class
     */
    @SuppressWarnings("serial")
    public class mediaSizeName extends MediaSizeName {

        public mediaSizeName(int value) {
            super(value);
        }
    }

}
