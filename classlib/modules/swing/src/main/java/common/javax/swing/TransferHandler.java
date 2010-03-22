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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.harmony.awt.text.ActionNames;


public class TransferHandler implements Serializable {
    public static final int NONE = ActionNames.NONE;
    public static final int COPY = ActionNames.COPY;
    public static final int MOVE = ActionNames.MOVE;
    public static final int COPY_OR_MOVE = ActionNames.COPY_OR_MOVE;
    private static final String MIME_PREFIX =
        DataFlavor.javaJVMLocalObjectMimeType + ";class=";

    private static final String CUT_ACTION_NAME = "cut";
    private static final String COPY_ACTION_NAME = "copy";
    private static final String PASTE_ACTION_NAME = "paste";


    private static final Action CUT_ACTION =
            new AbstractAction(CUT_ACTION_NAME) {
        public void actionPerformed(final ActionEvent e) {
            Object source = e.getSource();
            TransferHandler transferHandler = getTransferHandler(source);
            Clipboard clipboard = getSystemClipboard();

            if ((transferHandler != null) && (clipboard != null)) {
                transferHandler.exportToClipboard(
                        (JComponent) source, clipboard, MOVE);
            }
        }
    };

    private static final Action COPY_ACTION =
            new AbstractAction(COPY_ACTION_NAME) {
        public void actionPerformed(final ActionEvent e) {
            Object source = e.getSource();
            TransferHandler transferHandler = getTransferHandler(source);
            Clipboard clipboard = getSystemClipboard();

            if ((transferHandler != null) && (clipboard != null)) {
                transferHandler.exportToClipboard(
                        (JComponent) source, clipboard, COPY);
            }
        }
    };

    private static final Action PASTE_ACTION =
            new AbstractAction(PASTE_ACTION_NAME) {
        public void actionPerformed(final ActionEvent e) {
            Object source = e.getSource();
            TransferHandler transferHandler = getTransferHandler(source);
            Clipboard clipboard = getSystemClipboard();

            if ((transferHandler != null) && (clipboard != null)) {
               Transferable t = clipboard.getContents(this);

               if (t != null) {
                   transferHandler.importData((JComponent) source, t);
               }
            }
        }
    };

    private class Data implements Transferable {
        DataFlavor[] flavors;
        JComponent component;
        Object value;

        public Data(final JComponent c) {
            component = c;
            PropertyDescriptor descriptor = getPropertyDescriptor(c);
            if (descriptor == null) {
                return;
            }

            Method reader = descriptor.getReadMethod();
            try {
                 value = reader.invoke(component, (Object[])null);
            } catch (InvocationTargetException e) {
            } catch (IllegalAccessException e) {
            }
            String mimeType = MIME_PREFIX
                 + descriptor.getPropertyType().getName();
            DataFlavor flavor  = null;
            try {
                flavor = new DataFlavor(mimeType);
            } catch (ClassNotFoundException e) {

            }
            flavors = new DataFlavor[]{flavor};
        }

        public Object getTransferData(final DataFlavor flavor)
                throws UnsupportedFlavorException, IOException {
            return value;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        public boolean isDataFlavorSupported(final DataFlavor flavor) {
            for (int i = 0; i < flavors.length; i++) {
                if (flavors[i].equals(flavor)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static TransferHandler getTransferHandler(final Object src) {
         Object source = src;
         return (source instanceof JComponent)
             ? ((JComponent)source).getTransferHandler() : null;
    }

    private static Clipboard getSystemClipboard() {
        try {
            return Toolkit.getDefaultToolkit().getSystemClipboard();
        } catch (SecurityException e) {
            // we need to catch this exception in order to be compatible with RI
            // see HARMONY-3479
            return null;
        }
    }

    private PropertyDescriptor getPropertyDescriptor(final JComponent c) {
        PropertyDescriptor result = null;
        try {
            result = new PropertyDescriptor(propertyName, c.getClass());
        } catch (IntrospectionException e) {
            try {
                BeanInfo bi = Introspector.getBeanInfo(c.getClass());
                PropertyDescriptor pds[] = bi.getPropertyDescriptors();
                for (int i = 0; i < pds.length; i++) {
                    if (pds[i].getName().equals(propertyName)) {
                        return pds[i];
                    }
                }
            } catch (IntrospectionException e1) {
            }
        }
        return result;
    }


    private String propertyName;


    public TransferHandler(final String property) {
        propertyName = property;
    }

    protected TransferHandler() {
        this(null);
    }

    public Icon getVisualRepresentation(final Transferable t) {
        return null;
    }

    public void exportAsDrag(final JComponent c,
                             final InputEvent e,
                             final int action) {
        // TODO: implement
        return;
    }

    protected void exportDone(final JComponent c,
                              final Transferable t,
                              final int action) {
    }

    private DataFlavor getPrefferedFlavor(final Transferable t,
                                          final Class propertyType) {
        DataFlavor[] flavors = t.getTransferDataFlavors();
        DataFlavor result = null;
        for(int i = 0; i < flavors.length; i ++) {
            result = flavors[i];
            if (propertyType.isAssignableFrom(result
                                              .getRepresentationClass())) {
                return result;
            }
        }
        return null;
    }

    public boolean importData(final JComponent c,
                              final Transferable t) {
        PropertyDescriptor descriptor = getPropertyDescriptor(c);
        if (descriptor == null) {
            return false;
        }
        Class propertyType = descriptor.getPropertyType();
        DataFlavor flavor = getPrefferedFlavor(t, propertyType);
        if (flavor == null) {
            return false;
        }

        try {
            Object value = t.getTransferData(flavor);
            Method writer = descriptor.getWriteMethod();
            writer.invoke(c, new Object[]{value});
            return true;
        } catch (UnsupportedFlavorException e) {
        } catch (IOException e) {
        } catch (InvocationTargetException e) {
        } catch (IllegalAccessException e) {
        }
        return false;
    }

    protected Transferable createTransferable(final JComponent c) {
        return new Data(c);
    }

    public boolean canImport(final JComponent c,
                             final DataFlavor[] flavors) {
        PropertyDescriptor descriptor = getPropertyDescriptor(c);
        if (descriptor == null || descriptor.getWriteMethod() == null
                || flavors == null) {
            return false;
        }

        int length = flavors.length;
        Class propertyType = descriptor.getPropertyType();
        for (int i = 0; i < length; i++) {
            DataFlavor flavor = flavors[i];
            if (DataFlavor.javaJVMLocalObjectMimeType.equals(flavor
                                 .getHumanPresentableName())
                    && propertyType.equals(flavor.getRepresentationClass())) {
                return true;
            }
        }
        return false;
    }

    public void exportToClipboard(final JComponent c,
                                  final Clipboard clipboard,
                                  final int action) {
        Transferable t = createTransferable(c);
        if (t == null) {
            return;
        }
        int actionId = action & getSourceActions(c);
        if (actionId == NONE) {
            exportDone(c, t, actionId);
            return;
        }
        try {
            clipboard.setContents(t, null);
        } catch (IllegalStateException e) {
        }
        exportDone(c, t, actionId);
    }

    public int getSourceActions(final JComponent c) {
        BeanInfo beanInfo = null;
        try {
           beanInfo = Introspector.getBeanInfo(c.getClass());
        } catch (IntrospectionException e) {
        }
        
        PropertyDescriptor[] list = beanInfo.getPropertyDescriptors(); 
        for (int i = 0; i < list.length; i++) {
            String name = list[i].getName();
            if (name.equals(propertyName)) {
                return COPY;
            }
        }
        return NONE;
    }

    public static Action getPasteAction() {
        return PASTE_ACTION;
    }

    public static Action getCutAction() {
        return CUT_ACTION;
    }

    public static Action getCopyAction() {
        return COPY_ACTION;
    }
}



