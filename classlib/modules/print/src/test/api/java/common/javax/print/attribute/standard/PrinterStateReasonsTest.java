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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;


public class PrinterStateReasonsTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PrinterStateReasonsTest.class);
    }

    static {
        System.out.println("PrinterStateReasons testing...");
    }

    PrinterStateReasons reasons;

    /*
     * PrinterStateReasons() constructor testing. 
     */
    public final void testPrinterStateReasons() {
        reasons = new PrinterStateReasons();
        assertEquals(0, reasons.size());
    }

    /*
     * PrinterStateReasons(int initialCapacity) constructor testing. 
     */
    public final void testPrinterStateReasonsint() {
        try {
            reasons = new PrinterStateReasons(-1);
            fail("Constructor doesn't throw IllegalArgumentException if " +
            "initialCapacity < 0");
        } catch (IllegalArgumentException e) {
        }
    }

    /*
     * PrinterStateReasons(Map map) constructor testing. 
     */
    public final void testPrinterStateReasonsMap() {
        HashMap map = new HashMap();
        map.put(PrinterStateReason.TONER_LOW, Severity.WARNING);
        map.put(PrinterStateReason.DEVELOPER_LOW, Severity.WARNING);
        map.put(PrinterStateReason.MEDIA_LOW, Severity.ERROR);
        reasons = new PrinterStateReasons(map);

        assertEquals(3, reasons.size());
        assertTrue(reasons.containsKey(PrinterStateReason.TONER_LOW));
        assertTrue(reasons.containsKey(PrinterStateReason.DEVELOPER_LOW));
        assertTrue(reasons.containsKey(PrinterStateReason.MEDIA_LOW));

        try {
            map = new HashMap();
            map.put(PrinterStateReason.MEDIA_LOW, PrinterState.IDLE);
            reasons = new PrinterStateReasons(map);
            fail("Constructor doesn't throw ClassCastException if " +
                    "some value in the map isn't Severity");
        } catch (ClassCastException e) {
        }

        try {
            map = new HashMap();
            map.put(PrinterState.IDLE, Severity.ERROR);
            reasons = new PrinterStateReasons(map);
            fail("Constructor doesn't throw ClassCastException if " +
                    "some key in the map isn't PrinterStateReason");
        } catch (ClassCastException e) {
        }

        try {
            map = new HashMap();
            Severity severity = null;
            map.put(PrinterStateReason.COVER_OPEN, severity);
            reasons = new PrinterStateReasons(map);
            fail("Constructor doesn't throw NullPointerException if " +
                    "some key in the map is null");
        } catch (NullPointerException e) {
        }

        try {
            map.put(null, Severity.REPORT);
            reasons = new PrinterStateReasons(map);
            fail("Constructor doesn't throw NullPointerException if " +
                    "some value in the map is null");
        } catch (NullPointerException e) {
        }

        try {
            map = null;
            reasons = new PrinterStateReasons(map);
            fail("Constructor doesn't throw NullPointerException if " +
                    "map is null");
        } catch (NullPointerException e) {
        }

    }

    /*
     * getCategory() method testing.
     */
    public final void testGetCategory() {
        reasons = new PrinterStateReasons();
        assertEquals(PrinterStateReasons.class, reasons.getClass());
    }

    /*
     * getName() method testing.
     */
    public final void testGetName() {
        reasons = new PrinterStateReasons();
        assertEquals("printer-state-reasons", reasons.getName());
    }

    /*
     * printerStateReasonSet(Severity severity) method testing.
     */
    public final void testPrinterStateReasonSet() {
        reasons = new PrinterStateReasons();
        reasons.put(PrinterStateReason.MEDIA_LOW, Severity.ERROR);
        HashSet set = new HashSet();
        set.add(PrinterStateReason.MEDIA_LOW);
        assertEquals(set, reasons.printerStateReasonSet(Severity.ERROR));
        set = new HashSet();
        assertEquals(set, reasons.printerStateReasonSet(Severity.REPORT));
    }

    /*
     * printerStateReasonSet(Severity severity) method testing.
     */
    public final void testPrinterStateReasonSet1() {
        reasons = new PrinterStateReasons();
        reasons.put(PrinterStateReason.COVER_OPEN, Severity.ERROR);
        reasons.put(PrinterStateReason.MEDIA_LOW, Severity.WARNING);
        reasons.put(PrinterStateReason.DOOR_OPEN, Severity.ERROR);
        reasons.put(PrinterStateReason.INPUT_TRAY_MISSING, Severity.ERROR);

        Set set = reasons.printerStateReasonSet(Severity.ERROR);
        try {
            set.iterator().remove();
            fail("Unmodifiable set was changed");
        } catch (UnsupportedOperationException e) {
        }

        try {
            set.add(PrinterStateReason.COVER_OPEN);
            fail("Unmodifiable set was changed");
        } catch (UnsupportedOperationException e) {

        }
    }
}
