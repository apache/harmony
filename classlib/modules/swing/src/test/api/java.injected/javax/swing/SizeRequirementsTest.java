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
 * @author Alexander T. Simbirtsev
 * Created on 23.11.2004
 * * Window - Preferences - Java - Code Style - Code Templates
 *
 */
package javax.swing;

import java.io.IOException;

public class SizeRequirementsTest extends SwingTestCase {
    /*
     * Class under test for void SizeRequirements(int, int, int, float)
     */
    public void testSizeRequirementsintintintfloat() {
        int min = 100;
        int pref = 10;
        int max = 1;
        float align = 0.3f;
        SizeRequirements requirements = new SizeRequirements(min, pref, max, align);
        assertEquals("Minimum ", min, requirements.minimum);
        assertEquals("Preferred ", pref, requirements.preferred);
        assertEquals("Maximum ", max, requirements.maximum);
        assertEquals("Alignmnent ", align, requirements.alignment, 1e-5);
    }

    /*
     * Class under test for void SizeRequirements(float)
     */
    public void testSizeRequirementsfloat() {
        if (!isHarmony()) {
            return;
        }
        int min = 0;
        int pref = 0;
        int max = 0;
        float align = 0.3f;
        SizeRequirements requirements = new SizeRequirements(0, 0, 0, align);
        assertEquals("Minimum ", min, requirements.minimum);
        assertEquals("Preferred ", pref, requirements.preferred);
        assertEquals("Maximum ", max, requirements.maximum);
        assertEquals("Alignmnent ", align, requirements.alignment, 1e-5);
    }

    /*
     * Class under test for void SizeRequirements()
     */
    public void testSizeRequirements() {
        int min = 0;
        int pref = 0;
        int max = 0;
        float align = 0.5f;
        SizeRequirements requirements = new SizeRequirements();
        assertEquals("Minimum ", min, requirements.minimum);
        assertEquals("Preferred ", pref, requirements.preferred);
        assertEquals("Maximum ", max, requirements.maximum);
        assertEquals("Alignmnent ", align, requirements.alignment, 1e-5);
    }

    /*
     * Class under test for String toString()
     */
    public void testToString() {
        SizeRequirements requirements1 = new SizeRequirements(10, 20, 30, 0.1f);
        SizeRequirements requirements2 = new SizeRequirements(110, 240, 530, 0.9f);
        SizeRequirements requirements3 = new SizeRequirements();
        assertNotNull(requirements1.toString());
        assertNotNull(requirements2.toString());
        assertNotNull(requirements3.toString());
    }

    public void testGetTiledSizeRequirements() {
        SizeRequirements[] reqs = new SizeRequirements[] {
                new SizeRequirements(10, 20, 30, 1f), new SizeRequirements(20, 30, 40, 1f) };
        SizeRequirements res = SizeRequirements.getTiledSizeRequirements(reqs);
        assertEquals("Minimum ", 30, res.minimum);
        assertEquals("Preferred ", 50, res.preferred);
        assertEquals("Maximum ", 70, res.maximum);
        assertEquals("Alignmnent ", 0.5f, res.alignment, 1e-5);
        reqs = new SizeRequirements[] { new SizeRequirements(10, 20, 30, 2f),
                new SizeRequirements(20, 30, 40, 3f), new SizeRequirements(30, 40, 50, -100f) };
        res = SizeRequirements.getTiledSizeRequirements(reqs);
        assertEquals("Minimum ", 60, res.minimum);
        assertEquals("Preferred ", 90, res.preferred);
        assertEquals("Maximum ", 120, res.maximum);
        assertEquals("Alignmnent ", 0.5f, res.alignment, 1e-5);
        reqs = new SizeRequirements[0];
        res = SizeRequirements.getTiledSizeRequirements(reqs);
        assertEquals("Minimum ", 0, res.minimum);
        assertEquals("Preferred ", 0, res.preferred);
        assertEquals("Maximum ", 0, res.maximum);
        assertEquals("Alignmnent ", 0.5f, res.alignment, 1e-5);
        reqs = new SizeRequirements[] {
                new SizeRequirements(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                        0f),
                new SizeRequirements(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                        1f), new SizeRequirements(30, 20, 10, 1f) };
        res = SizeRequirements.getTiledSizeRequirements(reqs);
        assertEquals("Minimum ", Integer.MAX_VALUE, res.minimum);
        assertEquals("Preferred ", Integer.MAX_VALUE, res.preferred);
        assertEquals("Maximum ", Integer.MAX_VALUE, res.maximum);
        assertEquals("Alignmnent ", 0.5f, res.alignment, 1e-5);
    }

    public void testGetAlignedSizeRequirements() {
        SizeRequirements[] reqs = new SizeRequirements[] {
                new SizeRequirements(10, 20, 30, 0.2f), new SizeRequirements(20, 30, 40, .7f) };
        SizeRequirements res = SizeRequirements.getAlignedSizeRequirements(reqs);
        if (isHarmony()) {
            assertEquals("Minimum ", 22, res.minimum);
            assertEquals("Preferred ", 37, res.preferred);
            assertEquals("Maximum ", 52, res.maximum);
            assertEquals("Alignmnent ", 0.6363636f, res.alignment, 1e-5);
        }
        reqs = new SizeRequirements[] { new SizeRequirements(10, 20, 30, 0f),
                new SizeRequirements(20, 30, 40, 0.2f), new SizeRequirements(30, 20, 10, 1f) };
        res = SizeRequirements.getAlignedSizeRequirements(reqs);
        assertEquals("Minimum ", 46, res.minimum);
        assertEquals("Preferred ", 44, res.preferred);
        assertEquals("Maximum ", 42, res.maximum);
        assertEquals("Alignmnent ", 0.65217394f, res.alignment, 1e-5);
        reqs = new SizeRequirements[0];
        res = SizeRequirements.getAlignedSizeRequirements(reqs);
        assertEquals("Minimum ", 0, res.minimum);
        assertEquals("Preferred ", 0, res.preferred);
        assertEquals("Maximum ", 0, res.maximum);
        assertEquals("Alignmnent ", 0.0f, res.alignment, 1e-5);
        reqs = new SizeRequirements[] {
                new SizeRequirements(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                        0f),
                new SizeRequirements(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                        0.2f), new SizeRequirements(30, 20, 10, 1f) };
        res = SizeRequirements.getAlignedSizeRequirements(reqs);
        assertEquals("Minimum ", Integer.MAX_VALUE, res.minimum);
        assertEquals("Preferred ", Integer.MAX_VALUE, res.preferred);
        assertEquals("Maximum ", Integer.MAX_VALUE, res.maximum);
        assertEquals("Alignmnent ", 0.2f, res.alignment, 1e-5);
    }

    /*
     * Class under test for void calculateTiledPositions(int, SizeRequirements, SizeRequirements[], int[], int[], boolean)
     */
    public void testCalculateTiledPositionsintSizeRequirementsSizeRequirementsArrayintArrayintArrayboolean() {
        SizeRequirements total = null;
        SizeRequirements[] children = new SizeRequirements[] {
                new SizeRequirements(10, 40, 10, 0.9f),
                new SizeRequirements(20, 80, 100, 0.3f), new SizeRequirements(80, 70, 20, 0.6f) };
        int[] offsets = new int[3];
        int[] spans = new int[3];
        SizeRequirements.calculateTiledPositions(50, total, children, offsets, spans, true);
        assertEquals("Offsets coinside ", 0, offsets[0]);
        assertEquals("Offsets coinside ", 10, offsets[1]);
        assertEquals("Offsets coinside ", 30, offsets[2]);
        assertEquals("Spans coinside ", 10, spans[0]);
        assertEquals("Spans coinside ", 20, spans[1]);
        assertEquals("Spans coinside ", 80, spans[2]);
        SizeRequirements.calculateTiledPositions(100, total, children, offsets, spans, true);
        assertEquals("Offsets coinside ", 0, offsets[0]);
        assertEquals("Offsets coinside ", 10, offsets[1]);
        assertEquals("Offsets coinside ", 30, offsets[2]);
        assertEquals("Spans coinside ", 10, spans[0]);
        assertEquals("Spans coinside ", 20, spans[1]);
        assertEquals("Spans coinside ", 80, spans[2]);
        SizeRequirements.calculateTiledPositions(250, total, children, offsets, spans, true);
        assertEquals("Offsets coinside ", 0, offsets[0]);
        assertEquals("Offsets coinside ", 10, offsets[1]);
        assertEquals("Offsets coinside ", 110, offsets[2]);
        assertEquals("Spans coinside ", 10, spans[0]);
        assertEquals("Spans coinside ", 100, spans[1]);
        assertEquals("Spans coinside ", 20, spans[2]);
        SizeRequirements.calculateTiledPositions(250, total, children, offsets, spans, false);
        assertEquals("Offsets coinside ", 240, offsets[0]);
        assertEquals("Offsets coinside ", 140, offsets[1]);
        assertEquals("Offsets coinside ", 120, offsets[2]);
        assertEquals("Spans coinside ", 10, spans[0]);
        assertEquals("Spans coinside ", 100, spans[1]);
        assertEquals("Spans coinside ", 20, spans[2]);
        SizeRequirements.calculateTiledPositions(100, total, children, offsets, spans, false);
        assertEquals("Offsets coinside ", 90, offsets[0]);
        assertEquals("Offsets coinside ", 70, offsets[1]);
        assertEquals("Offsets coinside ", -10, offsets[2]);
        assertEquals("Spans coinside ", 10, spans[0]);
        assertEquals("Spans coinside ", 20, spans[1]);
        assertEquals("Spans coinside ", 80, spans[2]);
        SizeRequirements.calculateTiledPositions(80, total, children, offsets, spans, false);
        assertEquals("Offsets coinside ", 70, offsets[0]);
        assertEquals("Offsets coinside ", 50, offsets[1]);
        assertEquals("Offsets coinside ", -30, offsets[2]);
        assertEquals("Spans coinside ", 10, spans[0]);
        assertEquals("Spans coinside ", 20, spans[1]);
        assertEquals("Spans coinside ", 80, spans[2]);
        children = new SizeRequirements[] { new SizeRequirements(10, 100, 1000, 0.9f),
                new SizeRequirements(20, 200, 2000, 0.3f),
                new SizeRequirements(30, 300, 3000, 0.6f) };
        SizeRequirements.calculateTiledPositions(1000, total, children, offsets, spans, true);
        assertEquals("Offsets coinside ", 0, offsets[0]);
        assertEquals("Offsets coinside ", 166, offsets[1]);
        assertEquals("Offsets coinside ", 499, offsets[2]);
        assertEquals("Spans coinside ", 166, spans[0]);
        assertEquals("Spans coinside ", 333, spans[1]);
        assertEquals("Spans coinside ", 500, spans[2]);
        offsets = new int[1];
        spans = new int[1];
        children = new SizeRequirements[] { new SizeRequirements(30, 300, Integer.MAX_VALUE,
                0.6f) };
        SizeRequirements.calculateTiledPositions(350, total, children, offsets, spans, true);
        assertEquals("Offsets coinside ", 0, offsets[0]);
        assertEquals("Spans coinside ", 350, spans[0]);
        offsets = new int[2];
        spans = new int[2];
        children = new SizeRequirements[] {
                new SizeRequirements(30, 50, Integer.MAX_VALUE, 0.0f),
                new SizeRequirements(30, 50, Integer.MAX_VALUE, 0.0f) };
        SizeRequirements.calculateTiledPositions(300, total, children, offsets, spans, true);
        assertEquals("Offsets coinside ", 0, offsets[0]);
        assertEquals("Offsets coinside ", 150, offsets[1]);
        assertEquals("Spans coinside ", 150, spans[0]);
        assertEquals("Spans coinside ", 150, spans[1]);
        if (isHarmony()) {
            offsets = new int[2];
            spans = new int[2];
            children = new SizeRequirements[] {
                    new SizeRequirements(100, Integer.MAX_VALUE, Integer.MAX_VALUE, 0.0f),
                    new SizeRequirements(100, Integer.MAX_VALUE, Integer.MAX_VALUE, 0.0f) };
            SizeRequirements
                    .calculateTiledPositions(300, total, children, offsets, spans, true);
            assertEquals("Offsets coinside ", 0, offsets[0]);
            assertEquals("Offsets coinside ", 150, offsets[1]);
            assertEquals("Spans coinside ", 150, spans[0]);
            assertEquals("Spans coinside ", 150, spans[1]);
        }
    }

    /*
     * This function is being tested by:
     * testCalculateTiledPositionsintSizeRequirementsSizeRequirementsArrayintArrayintArrayboolean()
     */
    public void testCalculateTiledPositionsintSizeRequirementsSizeRequirementsArrayintArrayintArray() {
    }

    /*
     * Class under test for void calculateAlignedPositions(int, SizeRequirements, SizeRequirements[], int[], int[], boolean)
     */
    public void testCalculateAlignedPositionsintSizeRequirementsSizeRequirementsArrayintArrayintArrayboolean() {
        SizeRequirements total = new SizeRequirements(0, 0, 0, 0.4f);
        SizeRequirements[] children = new SizeRequirements[] {
                new SizeRequirements(10, 40, 10, 0.1f),
                new SizeRequirements(40, 80, 100, 0.2f), new SizeRequirements(60, 70, 20, 1f) };
        int[] offsets = new int[3];
        int[] spans = new int[3];
        SizeRequirements.calculateAlignedPositions(50, total, children, offsets, spans, true);
        assertEquals("Offsets coinside ", 19, offsets[0]);
        assertEquals("Offsets coinside ", 0, offsets[1]);
        assertEquals("Offsets coinside ", 0, offsets[2]);
        assertEquals("Spans coinside ", 10, spans[0]);
        assertEquals("Spans coinside ", 50, spans[1]);
        assertEquals("Spans coinside ", 20, spans[2]);
        SizeRequirements.calculateAlignedPositions(100, total, children, offsets, spans, true);
        assertEquals("Offsets coinside ", 39, offsets[0]);
        assertEquals("Offsets coinside ", 20, offsets[1]);
        assertEquals("Offsets coinside ", 20, offsets[2]);
        assertEquals("Spans coinside ", 10, spans[0]);
        assertEquals("Spans coinside ", 80, spans[1]);
        assertEquals("Spans coinside ", 20, spans[2]);
        SizeRequirements.calculateAlignedPositions(200, total, children, offsets, spans, true);
        assertEquals("Offsets coinside ", 79, offsets[0]);
        assertEquals("Offsets coinside ", 60, offsets[1]);
        assertEquals("Offsets coinside ", 60, offsets[2]);
        assertEquals("Spans coinside ", 10, spans[0]);
        assertEquals("Spans coinside ", 100, spans[1]);
        assertEquals("Spans coinside ", 20, spans[2]);
        total = new SizeRequirements(0, 0, 0, 0.1f);
        SizeRequirements.calculateAlignedPositions(200, total, children, offsets, spans, false);
        assertEquals("Offsets coinside ", 171, offsets[0], 1);
        assertEquals("Offsets coinside ", 100, offsets[1], 1);
        assertEquals("Offsets coinside ", 180, offsets[2], 1);
        assertEquals("Spans coinside ", 10, spans[0]);
        assertEquals("Spans coinside ", 100, spans[1]);
        assertEquals("Spans coinside ", 20, spans[2]);
        SizeRequirements.calculateAlignedPositions(80, total, children, offsets, spans, false);
        assertEquals("Offsets coinside ", 63, offsets[0], 1);
        assertEquals("Offsets coinside ", 0, offsets[1], 1);
        assertEquals("Offsets coinside ", 72, offsets[2], 1);
        assertEquals("Spans coinside ", 10, spans[0], 1);
        assertEquals("Spans coinside ", 80, spans[1], 1);
        assertEquals("Spans coinside ", 8, spans[2], 1);
    }

    /*
     * This function is being tested by:
     * testCalculateAlignedPositionsintSizeRequirementsSizeRequirementsArrayintArrayintArrayboolean()
     */
    public void testCalculateAlignedPositionsintSizeRequirementsSizeRequirementsArrayintArrayintArray() {
    }

    public void testAdjustSizes() {
        SizeRequirements[] reqs = new SizeRequirements[] {
                new SizeRequirements(10, 20, 30, 0.2f), new SizeRequirements(20, 30, 40, .7f) };
        int[] res = SizeRequirements.adjustSizes(1, reqs);
        assertTrue(res.length == 0);
    }

    public void testWriteObject() throws Exception {
        //        SizeRequirements requirements1 = new SizeRequirements(10, 20, 30, 0.1f);
        //        SizeRequirements requirements2 = new SizeRequirements(40, 50, 60, 0.2f);
        //        ByteArrayOutputStream fo = new ByteArrayOutputStream();
        //        ObjectOutputStream so = new ObjectOutputStream(fo);
        //        so.writeObject(requirements1);
        //        so.flush();
        //        so.writeObject(requirements2);
        //        so.flush();
    }

    public void testReadObject() throws IOException, ClassNotFoundException {
        //        SizeRequirements requirements1 = new SizeRequirements(10, 20, 30, 0.1f);
        //        SizeRequirements requirements2 = new SizeRequirements(40, 50, 60, 0.2f);
        //        ByteArrayOutputStream fo = new ByteArrayOutputStream();
        //        ObjectOutputStream so = new ObjectOutputStream(fo);
        //        so.writeObject(requirements1);
        //        so.flush();
        //        InputStream fi = new ByteArrayInputStream(fo.toByteArray());
        //        ObjectInputStream si = new ObjectInputStream(fi);
        //        SizeRequirements resurrectedRequirements = (SizeRequirements)si
        //                .readObject();
        //
        //        assertEquals("Deserialized minimum",
        //                     requirements1.minimum, resurrectedRequirements.minimum);
        //
        //        assertEquals("Deserialized preferred",
        //                     requirements1.preferred, resurrectedRequirements.preferred);
        //
        //        assertEquals("Deserialized maximum",
        //                     requirements1.maximum, resurrectedRequirements.maximum);
        //
        //        assertEquals("Deserialized alignment",
        //                     requirements1.alignment, resurrectedRequirements.alignment, 1e-5);
        //
        //        fo = new ByteArrayOutputStream();
        //        so = new ObjectOutputStream(fo);
        //        so.writeObject(requirements2);
        //        so.flush();
        //
        //        fi = new ByteArrayInputStream(fo.toByteArray());
        //        si = new ObjectInputStream(fi);
        //        resurrectedRequirements = (SizeRequirements)si.readObject();
        //        assertEquals("Deserialized minimum",
        //                     requirements2.minimum, resurrectedRequirements.minimum);
        //
        //        assertEquals("Deserialized preferred",
        //                     requirements2.preferred, resurrectedRequirements.preferred);
        //
        //        assertEquals("Deserialized maximum",
        //                     requirements2.maximum, resurrectedRequirements.maximum);
        //
        //        assertEquals("Deserialized alignment",
        //                     requirements2.alignment, resurrectedRequirements.alignment, 1e-5);
    }
}
