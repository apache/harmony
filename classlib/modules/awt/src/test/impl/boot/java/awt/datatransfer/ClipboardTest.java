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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.awt.*;

import junit.framework.TestCase;

public class ClipboardTest extends TestCase {

    private boolean calledBack;
    private boolean listenerCalled;

    public final void testClipboard() {
        assertNull(new Clipboard(null).getName());
        assertEquals(new Clipboard("Clipboard").getName(), "Clipboard");
    }

    public final void testGetName() {
        testClipboard();
    }

    public final void testAddFlavorListener() {
        Clipboard c = new Clipboard("");
        FlavorListener l = new FlavorListener() {
            public void flavorsChanged(FlavorEvent e) {
            }
        };

        c.addFlavorListener(null);
        assertTrue(c.getFlavorListeners().length == 0);

        c.addFlavorListener(l);
        assertTrue(c.getFlavorListeners().length > 0);
        assertTrue(Arrays.asList(c.getFlavorListeners()).contains(l));
    }

    public final void testRemoveFlavorListener() {
        Clipboard c = new Clipboard("");
        FlavorListener l = new FlavorListener() {
            public void flavorsChanged(FlavorEvent e) {
            }
        };

        c.addFlavorListener(l);
        c.removeFlavorListener(null);
        assertTrue(c.getFlavorListeners().length == 1);

        c.removeFlavorListener(l);
        assertTrue(c.getFlavorListeners().length == 0);

        c.removeFlavorListener(l);
        assertTrue(c.getFlavorListeners().length == 0);
    }

    public final void testGetFlavorListeners() {
        Clipboard c = new Clipboard("");
        FlavorListener l = new FlavorListener() {
            public void flavorsChanged(FlavorEvent e) {
            }
        };

        assertTrue(c.getFlavorListeners().length == 0);

        c.addFlavorListener(l);
        assertTrue(c.getFlavorListeners().length > 0);
        assertTrue(Arrays.asList(c.getFlavorListeners()).contains(l));

        c.removeFlavorListener(l);
        assertTrue(c.getFlavorListeners().length == 0);
    }

    public final void testSetContents() throws InterruptedException, InvocationTargetException {
        Clipboard c = new Clipboard("");
        FlavorListener l = new FlavorListener() {
            public void flavorsChanged(FlavorEvent e) {
                listenerCalled = true;
            }
        };
        StringSelection t = new StringSelection("") {
            @Override
            public void lostOwnership(Clipboard clipboard, Transferable contents) {
                calledBack = true;
            }
        };

        c.addFlavorListener(l);
        c.setContents(t, t);
        listenerCalled = calledBack = false;
        c.setContents(new Transferable() {
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                return null;
            }
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return false;
            }
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[0];
            }}, new ClipboardOwner() {
            public void lostOwnership(Clipboard clipboard, Transferable contents) {
            }
        });
        EventQueue.invokeAndWait(new Runnable() {public void run() {}});
        assertTrue(listenerCalled);
        assertTrue(calledBack);
    }

    public final void testGetContents() {
        Clipboard c = new Clipboard("");
        StringSelection s = new StringSelection("");

        c.setContents(s, s);
        assertEquals(c.getContents(null), s);
    }

    @SuppressWarnings("deprecation")
    public final void testIsDataFlavorAvailable() {
        Clipboard c = new Clipboard("");
        StringSelection s = new StringSelection("");

        c.setContents(s, s);
        assertTrue(c.isDataFlavorAvailable(DataFlavor.stringFlavor));
        assertTrue(c.isDataFlavorAvailable(DataFlavor.plainTextFlavor));
        assertFalse(c.isDataFlavorAvailable(new DataFlavor(Rectangle.class, "")));
    }

    @SuppressWarnings("deprecation")
    public final void testGetAvailableDataFlavors() {
        Clipboard c = new Clipboard("");
        StringSelection s = new StringSelection("");

        c.setContents(s, s);
        assertTrue(Arrays.asList(c.getAvailableDataFlavors()).contains(DataFlavor.stringFlavor));
        assertTrue(Arrays.asList(c.getAvailableDataFlavors()).contains(DataFlavor.plainTextFlavor));
        assertFalse(Arrays.asList(c.getAvailableDataFlavors()).contains(new DataFlavor(Rectangle.class, "")));
    }

    public final void testGetData() {
        Clipboard c = new Clipboard("");
        StringSelection s = new StringSelection("");
        boolean string = false;
        boolean unsupported = false;

        c.setContents(s, s);

        try {
            string = c.getData(DataFlavor.stringFlavor).equals("");
            c.getData(new DataFlavor(Rectangle.class, "")).equals("");
        } catch (Exception e) {
            unsupported = true;
        }
        assertTrue(string);
        assertTrue(unsupported);
    }
    
    public final void testNullContent() throws NullPointerException {
        // Regression for HARMONY-2067
        Clipboard c = new Clipboard(""); 
        c.setContents(null, null); 
    }

}
