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
 * @author Michael Danilov
 */

package java.awt.datatransfer;

import java.util.*;

import junit.framework.TestCase;

public class SystemFlavorMapTest extends TestCase {

    public final void testIsJavaMIMEType() {
        assertTrue(SystemFlavorMap.isJavaMIMEType("org.apache.harmony.awt.datatransfer:"));
        assertFalse(SystemFlavorMap.isJavaMIMEType("JAVA"));
        assertFalse(SystemFlavorMap.isJavaMIMEType(null));
    }

    public final void testEncodeJavaMIMEType() {
        assertEquals(SystemFlavorMap.encodeJavaMIMEType("z"), 
                "org.apache.harmony.awt.datatransfer:z");
        assertNull(SystemFlavorMap.encodeJavaMIMEType(null));
    }

    public final void testDecodeJavaMIMEType() {
    }

    public final void testEncodeDataFlavor() {
        assertEquals(SystemFlavorMap.encodeDataFlavor(DataFlavor.stringFlavor),
                "org.apache.harmony.awt.datatransfer:" +
                "application/x-java-serialized-object;" +
                " class=\"java.lang.String\"");
        assertNull(SystemFlavorMap.encodeDataFlavor(null));
    }

    public final void testDecodeDataFlavor() {
        try {
            assertEquals(SystemFlavorMap.decodeDataFlavor(
                    "org.apache.harmony.awt.datatransfer:" +
                    "application/x-java-serialized-object;" +
                    " class=\"java.lang.String\""),
                    DataFlavor.stringFlavor);
            assertNull(SystemFlavorMap.decodeDataFlavor(null));
            assertNull(SystemFlavorMap.decodeDataFlavor("z"));
        } catch (ClassNotFoundException e) {
            fail();
        }

        try {
            assertEquals(SystemFlavorMap.decodeDataFlavor(
                    "org.apache.harmony.awt.datatransfer" +
                    ":application/x-java-serialized-object; class=Ing"),
                    DataFlavor.stringFlavor);
            fail();
        } catch (ClassNotFoundException e) {
            assertTrue(true);
        }
    }

    public final void testGetDefaultFlavorMap() {
        FlavorMap map = SystemFlavorMap.getDefaultFlavorMap();

        assertTrue(map.getFlavorsForNatives(null).size() > 0);
        assertTrue(map.getNativesForFlavors(null).size() > 0);
    }

    public final void testGetFlavorsForNative() {
        SystemFlavorMap map = (SystemFlavorMap) SystemFlavorMap.getDefaultFlavorMap();
        List<DataFlavor> list;

        map.addFlavorForUnencodedNative("1nat1", new DataFlavor("1data1/flavor", "flav1"));
        map.addFlavorForUnencodedNative("1nat1", new DataFlavor("1data2/flavor", "flav2"));
        map.addFlavorForUnencodedNative("1nat2", new DataFlavor("1data3/flavor", "flav3"));
        map.addFlavorForUnencodedNative("1nat2", new DataFlavor("1data4/flavor", "flav4"));

        list = map.getFlavorsForNative("1nat1");
        assertTrue(list.size() == 2);
        assertTrue(list.contains(new DataFlavor("1data1/flavor", "flav1")));
        assertTrue(list.contains(new DataFlavor("1data2/flavor", "flav2")));

        list = map.getFlavorsForNative(null);
        assertTrue(list.size() >= 4);
        assertTrue(list.contains(new DataFlavor("1data1/flavor", "flav1")));
        assertTrue(list.contains(new DataFlavor("1data2/flavor", "flav2")));
        assertTrue(list.contains(new DataFlavor("1data3/flavor", "flav3")));
        assertTrue(list.contains(new DataFlavor("1data4/flavor", "flav4")));
    }

    public final void testGetNativesForFlavor() {
        SystemFlavorMap map = (SystemFlavorMap) SystemFlavorMap.getDefaultFlavorMap();
        List<String> list;

        map.addUnencodedNativeForFlavor(new DataFlavor("2data1/flavor", "flav1"), "2nat1");
        map.addUnencodedNativeForFlavor(new DataFlavor("2data1/flavor", "flav1"), "2nat2");
        map.addUnencodedNativeForFlavor(new DataFlavor("2data2/flavor", "flav2"), "2nat3");
        map.addUnencodedNativeForFlavor(new DataFlavor("2data2/flavor", "flav2"), "2nat4");

        list = map.getNativesForFlavor(new DataFlavor("2data1/flavor", "flav1"));
        assertTrue(list.size() == 2);
        assertTrue(list.contains("2nat1"));
        assertTrue(list.contains("2nat2"));

        list = map.getNativesForFlavor(null);
        assertTrue(list.size() >= 4);
        assertTrue(list.contains("2nat1"));
        assertTrue(list.contains("2nat2"));
        assertTrue(list.contains("2nat3"));
        assertTrue(list.contains("2nat4"));
    }

    public final void testGetFlavorsForNatives() {
        SystemFlavorMap map = (SystemFlavorMap) SystemFlavorMap.getDefaultFlavorMap();
        Map<String, DataFlavor> submap;

        map.addFlavorForUnencodedNative("3nat1", new DataFlavor("3data1/flavor", "flav1"));
        map.addFlavorForUnencodedNative("3nat1", new DataFlavor("3data2/flavor", "flav2"));
        map.addFlavorForUnencodedNative("3nat2", new DataFlavor("3data3/flavor", "flav3"));
        map.addFlavorForUnencodedNative("3nat2", new DataFlavor("3data4/flavor", "flav4"));

        submap = map.getFlavorsForNatives(new String[] {"3nat1"});
        assertTrue(submap.keySet().size() == 1);
        assertEquals(submap.get("3nat1"), new DataFlavor("3data1/flavor", "flav1"));

        submap = map.getFlavorsForNatives(null);
        assertTrue(submap.keySet().size() >= 2);
        assertEquals(submap.get("3nat1"), new DataFlavor("3data1/flavor", "flav1"));
        assertEquals(submap.get("3nat2"), new DataFlavor("3data3/flavor", "flav3"));
    }

    public final void testGetNativesForFlavors() {
        SystemFlavorMap map = (SystemFlavorMap) SystemFlavorMap.getDefaultFlavorMap();
        Map<DataFlavor, String> submap;

        map.addUnencodedNativeForFlavor(new DataFlavor("4data1/flavor", "flav1"), "4nat1");
        map.addUnencodedNativeForFlavor(new DataFlavor("4data1/flavor", "flav1"), "4nat2");
        map.addUnencodedNativeForFlavor(new DataFlavor("4data2/flavor", "flav2"), "4nat3");
        map.addUnencodedNativeForFlavor(new DataFlavor("4data2/flavor", "flav2"), "4nat4");

        submap = map.getNativesForFlavors(new DataFlavor[] {new DataFlavor("4data1/flavor", "flav1")});
        assertTrue(submap.keySet().size() == 1);
        assertEquals(submap.get(new DataFlavor("4data1/flavor", "4flav1")), "4nat1");

        submap = map.getNativesForFlavors(null);
        assertTrue(submap.keySet().size() >= 2);
        assertEquals(submap.get(new DataFlavor("4data1/flavor", "flav1")), "4nat1");
        assertEquals(submap.get(new DataFlavor("4data2/flavor", "flav2")), "4nat3");
    }

    public final void testAddUnencodedNativeForFlavor() {
        SystemFlavorMap map = (SystemFlavorMap) SystemFlavorMap.getDefaultFlavorMap();
        List<String> list;

        map.addUnencodedNativeForFlavor(new DataFlavor("5data1/flavor", "flav1"), "5nat1");
        map.addUnencodedNativeForFlavor(new DataFlavor("5data1/flavor", "flav1"), "5nat2");
        map.addUnencodedNativeForFlavor(new DataFlavor("5data1/flavor", "flav1"), "5nat2");

        list = map.getNativesForFlavor(new DataFlavor("5data1/flavor", "flav1"));
        assertTrue(list.size() == 2);
        assertTrue(list.contains("5nat1"));
        assertTrue(list.contains("5nat2"));
    }

    public final void testAddFlavorForUnencodedNative() {
        SystemFlavorMap map = (SystemFlavorMap) SystemFlavorMap.getDefaultFlavorMap();
        List<DataFlavor> list;

        map.addFlavorForUnencodedNative("6nat1", new DataFlavor("6data1/flavor", "flav1"));
        map.addFlavorForUnencodedNative("6nat1", new DataFlavor("6data2/flavor", "flav2"));
        map.addFlavorForUnencodedNative("6nat1", new DataFlavor("6data2/flavor", "flav2"));

        list = map.getFlavorsForNative("6nat1");
        assertTrue(list.size() == 2);
        assertTrue(list.contains(new DataFlavor("6data1/flavor", "flav1")));
        assertTrue(list.contains(new DataFlavor("6data2/flavor", "flav2")));
    }

    public final void testSetNativesForFlavor() {
        SystemFlavorMap map = (SystemFlavorMap) SystemFlavorMap.getDefaultFlavorMap();
        List<String> list;

        map.addUnencodedNativeForFlavor(new DataFlavor("7data1/flavor", "flav1"), "77nat1");
        map.setNativesForFlavor(new DataFlavor("7data1/flavor", "flav1"), new String[] {"7nat10", "7nat11"});

        list = map.getNativesForFlavor(new DataFlavor("7data1/flavor", "flav1"));
        assertTrue(list.size() == 2);
        assertTrue(list.contains("7nat10"));
        assertTrue(list.contains("7nat11"));
    }

    public final void testSetFlavorsForNative() {
        SystemFlavorMap map = (SystemFlavorMap) SystemFlavorMap.getDefaultFlavorMap();
        List<DataFlavor> list;

        map.addFlavorForUnencodedNative("8nat1", new DataFlavor("88data1/flavor", "flav1"));
        map.setFlavorsForNative("8nat1", new DataFlavor[] {new DataFlavor("8data10/flavor", "flav1"),
                new DataFlavor("8data11/flavor", "flav1")});

        list = map.getFlavorsForNative("8nat1");
        assertTrue(list.size() == 2);
        assertTrue(list.contains(new DataFlavor("8data10/flavor", "flav1")));
        assertTrue(list.contains(new DataFlavor("8data11/flavor", "flav1")));
    }

}
