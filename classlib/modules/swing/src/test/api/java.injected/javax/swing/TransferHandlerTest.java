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
 */
package javax.swing;

import java.awt.AWTPermission;
import java.awt.Color;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.security.Permission;


public class TransferHandlerTest extends SwingTestCase {
    TransferHandler transferHandler;

    JButton button;

    SimpleTransferHandler trH;

    TransferHandler insetsTransferHandler;

    class SimpleTransferHandler extends TransferHandler {
        private static final long serialVersionUID = 1L;

        public SimpleTransferHandler(final String s) {
            super(s);
        }

        boolean wasCallExportToClipBoard;

        int parameter = -3;

        boolean wasCallImportData;

        Clipboard clipboard;

        JComponent component;

        Transferable transferable;

        @Override
        public void exportToClipboard(final JComponent comp, final Clipboard clip,
                final int action) {
            component = comp;
            clipboard = clip;
            parameter = action;
            wasCallExportToClipBoard = true;
        }

        @Override
        public boolean importData(final JComponent comp, final Transferable t) {
            component = comp;
            transferable = t;
            wasCallImportData = true;
            return super.importData(comp, t);
        }

        @Override
        protected Transferable createTransferable(final JComponent c) {
            Transferable t = super.createTransferable(c);
            return t;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        transferHandler = new TransferHandler("text");
        trH = new SimpleTransferHandler("text");
        button = new JButton();
        button.setTransferHandler(trH);
        insetsTransferHandler = new TransferHandler("insets");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testConstants() {
        assertEquals(0, TransferHandler.NONE);
        assertEquals(1, TransferHandler.COPY);
        assertEquals(2, TransferHandler.MOVE);
        assertEquals(3, TransferHandler.COPY_OR_MOVE);
    }

    public void testTransferHandlerString() {
    }

    public void testTransferHandler() {
    }

    public void testGetVisualRepresentation() {
        Transferable transferable = new StringSelection("***");
        assertNull(transferHandler.getVisualRepresentation(null));
        assertNull(transferHandler.getVisualRepresentation(transferable));
    }

    public void testExportAsDrag() {
        MouseEvent e = new MouseEvent(button, MouseEvent.MOUSE_PRESSED, 0,
                InputEvent.BUTTON1_DOWN_MASK, 50, 50, 0, true);
        trH.exportAsDrag(button, e, TransferHandler.COPY_OR_MOVE);
    }

    public void testImportData() {
        TransferHandler handler = new TransferHandler("background");
        JTextArea textArea = new JTextArea();
        textArea.setBackground(Color.RED);
        Transferable transferable = handler.createTransferable(textArea);
        try {
            assertEquals(Color.RED, transferable.getTransferData(transferable
                    .getTransferDataFlavors()[0]));
        } catch (UnsupportedFlavorException e) {
            assertFalse("Unexpected exception: " + e.getMessage(), true);
        } catch (IOException e) {
            assertFalse("Unexpected exception: " + e.getMessage(), true);
        }
        JTextArea distArea = new JTextArea();
        distArea.setBackground(Color.GREEN);
        handler.importData(distArea, transferable);
        assertEquals(Color.RED, distArea.getBackground());
    }

    public void testCreateTransferable() {
        Transferable transferable = insetsTransferHandler.createTransferable(button);
        transferable.getTransferDataFlavors();
        DataFlavor[] flavors = transferable.getTransferDataFlavors();
        try {
            assertEquals(button.getInsets(), transferable.getTransferData(flavors[0]));
        } catch (IOException e) {
            assertFalse("Unexpected exception :" + e.getMessage(), true);
        } catch (UnsupportedFlavorException e) {
            assertFalse("Unexpected exception :" + e.getMessage(), true);
        }
        assertEquals(1, flavors.length);
        DataFlavor flavor = flavors[0];
        assertEquals(DataFlavor.javaJVMLocalObjectMimeType, flavor.getHumanPresentableName());
    }

    public void testCanImport() {
        //no text property
        assertFalse(transferHandler.canImport(new JPanel(), null));
        DataFlavor[] flavors = new DataFlavor[] { DataFlavor.imageFlavor,
                DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor };
        assertFalse(transferHandler.canImport(button, flavors));
        DataFlavor flavor;
        try {
            flavor = new DataFlavor("application/x-java-jvm-local-objectref;"
                    + "class=java.lang.String");
            assertTrue(transferHandler.canImport(button, new DataFlavor[] { flavor }));
        } catch (ClassNotFoundException e) {
            assertFalse("Unexpected exception : " + e.getMessage(), true);
        }
    }

    public void testExportToClipboard_text() {
        JTextArea textArea = new JTextArea("ABCD");
        Clipboard clip = new Clipboard("non system");
        transferHandler.exportToClipboard(textArea, clip, TransferHandler.COPY);
        DataFlavor flavor = null;
        try {
            flavor = new DataFlavor("application/x-java-jvm-local-objectref;"
                    + "class=java.lang.String");
        } catch (ClassNotFoundException e) {
            assertFalse("Unexpected exception : " + e.getMessage(), true);
        }
        try {
            Transferable trans = clip.getContents(null);
            Object obj = trans.getTransferData(flavor);
            assertEquals("ABCD", obj);
        } catch (IOException e) {
        } catch (UnsupportedFlavorException e) {
        }
    }

    public void testExportToClipboard_background() {
        TransferHandler handler = new TransferHandler("background");
        JTextArea textArea = new JTextArea("ABCD");
        textArea.setBackground(Color.RED);
        Clipboard clip = new Clipboard("non system");
        handler.exportToClipboard(textArea, clip, TransferHandler.COPY);
        DataFlavor flavor = null;
        try {
            flavor = new DataFlavor("application/x-java-jvm-local-objectref;"
                    + "class=java.awt.Color");
        } catch (ClassNotFoundException e) {
            assertFalse("Unexpected exception : " + e.getMessage(), true);
        }
        try {
            Transferable trans = clip.getContents(null);
            Object obj = trans.getTransferData(flavor);
            assertEquals(Color.RED, obj);
        } catch (IOException e) {
        } catch (UnsupportedFlavorException e) {
        }
    }

    public void testGetSourceActions() {
        assertEquals(TransferHandler.COPY, transferHandler.getSourceActions(new JButton()));
        assertEquals(TransferHandler.NONE, transferHandler.getSourceActions(new JPanel()));
        assertEquals(TransferHandler.COPY, transferHandler.getSourceActions(new JLabel()));
        assertEquals(TransferHandler.NONE, transferHandler.getSourceActions(new JLayeredPane()));
    }

    public void testGetPasteAction() {
        // TODO: uncomment when System clipboard is properly supported
        //        StringSelection data = new StringSelection("TEST");
        //        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(data, null);
        //        Action action = TransferHandler.getPasteAction();
        //        ActionEvent event = new ActionEvent(button,
        //                                            ActionEvent.ACTION_PERFORMED, "");
        //        action.actionPerformed(event);
        //        assertTrue(trH.wasCallImportData);
        //        assertEquals(button, trH.component);
        //        assertEquals("paste", action.getValue(Action.NAME));
        //        assertTrue(action == TransferHandler.getPasteAction());
    }

    public void testGetCutAction() {
        // TODO: uncomment when System clipboard is properly supported
        //        Action action = TransferHandler.getCutAction();
        //        ActionEvent event = new ActionEvent(button,
        //                                            ActionEvent.ACTION_PERFORMED, "");
        //        action.actionPerformed(event);
        //        assertTrue(trH.wasCallExportToClipBoard);
        //        assertEquals(TransferHandler.MOVE, trH.parameter);
        //        assertEquals(Toolkit.getDefaultToolkit().getSystemClipboard(),
        //                     trH.clipboard);
        //        assertEquals(button, trH.component);
        //        assertEquals("cut", action.getValue(Action.NAME));
        //        assertTrue(action == TransferHandler.getCutAction());
    }

    public void testGetCopyAction() {
        // TODO: uncomment when System clipboard is properly supported
        //        Action action = TransferHandler.getCopyAction();
        //        ActionEvent event = new ActionEvent(button,
        //                                            ActionEvent.ACTION_PERFORMED, "");
        //        action.actionPerformed(event);
        //        assertTrue(trH.wasCallExportToClipBoard);
        //        assertEquals(TransferHandler.COPY, trH.parameter);
        //        assertEquals(Toolkit.getDefaultToolkit().getSystemClipboard(),
        //                     trH.clipboard);
        //        assertEquals(button, trH.component);
        //        assertEquals("copy", action.getValue(Action.NAME));
        //        assertTrue(action == TransferHandler.getCopyAction());
    }

    public void testAccessSystemClipboard() {
        // Regression test for HARMONY-3479

        class TestSecurityManager extends SecurityManager {

            public boolean flag;

            public void checkPermission(Permission perm) {
                if ((perm instanceof AWTPermission)
                        && "accessClipboard".equals(perm.getName())) {
                    flag = true;
                    throw new SecurityException("test");
                }
            }
        }

        SecurityManager oldSecurityManager = System.getSecurityManager();
        TestSecurityManager testSecurityManager = new TestSecurityManager();
        System.setSecurityManager(testSecurityManager);

        try {
            ActionEvent event = new ActionEvent(new JPanel(), 0, "");
            Action action;

            testSecurityManager.flag = false;
            TransferHandler.getCopyAction().actionPerformed(event);
            assertTrue(testSecurityManager.flag);

            testSecurityManager.flag = false;
            TransferHandler.getCutAction().actionPerformed(event);
            assertTrue(testSecurityManager.flag);

            testSecurityManager.flag = false;
            TransferHandler.getPasteAction().actionPerformed(event);
            assertTrue(testSecurityManager.flag);
        } finally {
            System.setSecurityManager(oldSecurityManager);
        }
    }
}
