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
 * @author Dmitry A. Durnev, Pavel Dolgov
 */
package org.apache.harmony.awt.wtk.linux;

import java.awt.Frame;
import java.awt.Insets;

import org.apache.harmony.awt.wtk.CreationParams;

import org.apache.harmony.awt.nativebridge.CLongPointer;
import org.apache.harmony.awt.nativebridge.Int32Pointer;
import org.apache.harmony.awt.nativebridge.Int8Pointer;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.PointerPointer;
import org.apache.harmony.awt.nativebridge.VoidPointer;
import org.apache.harmony.awt.nativebridge.linux.X11;
import org.apache.harmony.awt.nativebridge.linux.X11Defs;



/**
 * Window manager specific actions
 *
 */
class WindowManager {

    private static final X11 x11 = X11.getInstance();
    private static final NativeBridge bridge = NativeBridge.getInstance();

    private final LinuxWindowFactory factory;
    private final long display;

    //atom which defines window property type "ATOM"
    private final long XA_ATOM;
    private final long XA_CARDINAL;


    // WM hints
    public final long WM_PROTOCOLS;
    public final long WM_DELETE_WINDOW;
    public final long WM_TAKE_FOCUS;

    // _NET extended hints
    private final long NET_WM_WINDOW_TYPE;
    private final long NET_WM_WINDOW_TYPE_DIALOG;
    private final long NET_WM_WINDOW_TYPE_NORMAL;
    private final long NET_REQUEST_FRAME_EXTENTS;
    public final long NET_WM_STATE;
    public final long NET_WM_STATE_HIDDEN;
    public final long NET_WM_STATE_MAXIMIZED_VERT;
    public final long NET_WM_STATE_MAXIMIZED_HORZ;
    public final long NET_WM_STATE_ABOVE;
    public final long NET_WM_STATE_STAYS_ON_TOP; // same as ABOVE, only for KDE
    public final long NET_FRAME_EXTENTS;

    // KDE specific hints
    private final long KDE_NET_WM_WINDOW_TYPE_OVERRIDE;
    public final long KDE_NET_WM_FRAME_STRUT;

    // Motif hints (for Gnome)
    private final long XA_MOTIF_WM_HINTS;

    private long focusedWindow;

    WindowManager(LinuxWindowFactory factory) {
        this.factory = factory;
        display = factory.getDisplay();


        XA_ATOM = internAtom("ATOM"); //$NON-NLS-1$
        XA_CARDINAL = internAtom("CARDINAL"); //$NON-NLS-1$

        // WM hints
        WM_PROTOCOLS = internAtom("WM_PROTOCOLS"); //$NON-NLS-1$
        WM_DELETE_WINDOW = internAtom("WM_DELETE_WINDOW"); //$NON-NLS-1$
        WM_TAKE_FOCUS = internAtom("WM_TAKE_FOCUS"); //$NON-NLS-1$

        // _NET extended hints
        NET_WM_WINDOW_TYPE = internAtom("_NET_WM_WINDOW_TYPE"); //$NON-NLS-1$
        NET_WM_WINDOW_TYPE_DIALOG = internAtom("_NET_WM_WINDOW_TYPE_DIALOG"); //$NON-NLS-1$
        NET_WM_WINDOW_TYPE_NORMAL = internAtom("_NET_WM_WINDOW_TYPE_NORMAL"); //$NON-NLS-1$
        NET_REQUEST_FRAME_EXTENTS = internAtom("_NET_REQUEST_FRAME_EXTENTS"); //$NON-NLS-1$
        NET_WM_STATE = internAtom("_NET_WM_STATE"); //$NON-NLS-1$
        NET_WM_STATE_HIDDEN = internAtom("_NET_WM_STATE_HIDDEN"); //$NON-NLS-1$
        NET_WM_STATE_MAXIMIZED_VERT = internAtom("_NET_WM_STATE_MAXIMIZED_VERT"); //$NON-NLS-1$
        NET_WM_STATE_MAXIMIZED_HORZ = internAtom("_NET_WM_STATE_MAXIMIZED_HORZ"); //$NON-NLS-1$
        NET_WM_STATE_ABOVE = internAtom("_NET_WM_STATE_ABOVE"); //$NON-NLS-1$
        NET_WM_STATE_STAYS_ON_TOP = internAtom("_NET_WM_STATE_STAYS_ON_TOP"); //$NON-NLS-1$
        NET_FRAME_EXTENTS = internAtom("_NET_FRAME_EXTENTS"); //$NON-NLS-1$

        // KDE specific hints
        KDE_NET_WM_WINDOW_TYPE_OVERRIDE = internAtom("_KDE_NET_WM_WINDOW_TYPE_OVERRIDE"); //$NON-NLS-1$
        KDE_NET_WM_FRAME_STRUT = internAtom("_KDE_NET_WM_FRAME_STRUT"); //$NON-NLS-1$

        // Motif hints (for Gnome)
        XA_MOTIF_WM_HINTS = internAtom("_MOTIF_WM_HINTS"); //$NON-NLS-1$
    }

    long internAtom(String atomName) {
        return factory.internAtom(atomName);
    }

    String getAtomName(int atom) {
        return factory.getAtomName(atom);
    }

    void changeWindowProperty(long winId, long property,
                              long type, long values[]) {
        int count = values.length;
        Int8Pointer dataPtr = null;

        if (count > 0) {
            CLongPointer data = bridge.createCLongPointer(count, false);
            data.set(values, 0, count);
            dataPtr = bridge.createInt8Pointer(data);
        }

        x11.XChangeProperty(display, winId, property, type, 32,
                X11Defs.PropModeReplace, dataPtr, count);
    }

    void setDecorType(long winId, int decorType, boolean undecorated) {

        long styles[] = null;
        if(undecorated || decorType == CreationParams.DECOR_TYPE_UNDECOR) {
            styles = new long[] {
                    KDE_NET_WM_WINDOW_TYPE_OVERRIDE,
                    NET_WM_WINDOW_TYPE_NORMAL };

        } else if (decorType == CreationParams.DECOR_TYPE_FRAME) {
            styles = new long[] { NET_WM_WINDOW_TYPE_NORMAL };

        } else if (decorType == CreationParams.DECOR_TYPE_DIALOG) {
            styles = new long[] { NET_WM_WINDOW_TYPE_DIALOG };
        }

        if (styles != null) {
            changeWindowProperty(winId, NET_WM_WINDOW_TYPE, XA_ATOM, styles);
        }

        if (undecorated || decorType == CreationParams.DECOR_TYPE_UNDECOR) {
            long hint[] = new long[] { 2, 0, 0, 0, 0 };
            changeWindowProperty(winId, XA_MOTIF_WM_HINTS, XA_MOTIF_WM_HINTS, hint);
        }
    }

    /**
     * Set non-geometry WM Hints, such as keyboard input,
     * window group leader, icon, etc.
     */
    void setWMHints(int winId, int ownerID) {
        long wmHintsPtr = x11.XAllocWMHints();
        X11.XWMHints wmHints = x11.createXWMHints(wmHintsPtr);
        int flags = X11Defs.InputHint;
        wmHints.set_input(1);
        if (ownerID != 0) {
            flags |= X11Defs.WindowGroupHint;
            wmHints.set_window_group(ownerID);//set group leader ID to owner
        }
        wmHints.set_flags(flags);
        x11.XSetWMHints(display, winId, wmHintsPtr);
        x11.XFree(wmHintsPtr);
    }

    void setWMProtocols(long windowID) {
        CLongPointer protocols = bridge.createCLongPointer(2, false);
        protocols.set(0, WM_DELETE_WINDOW);
        protocols.set(1, WM_TAKE_FOCUS);
        x11.XSetWMProtocols(display, windowID, protocols, 2);
    }

    void setInputAllowed(long winId, boolean allow) {
        long wmHintsPtr = x11.XGetWMHints(display, winId);
        if (wmHintsPtr == 0) {
            wmHintsPtr = x11.XAllocWMHints();
        }
        X11.XWMHints wmHints = x11.createXWMHints(wmHintsPtr);
        wmHints.set_input(allow ? 1 : 0);
        wmHints.set_flags(wmHints.get_flags() | X11Defs.InputHint);
        x11.XSetWMHints(display, winId, wmHintsPtr);

        x11.XFree(wmHintsPtr);
        x11.XFlush(display);
    }

    boolean isInputAllowed(long winId) {
        long wmHintsPtr = x11.XGetWMHints(display, winId);
        if (wmHintsPtr == 0) {
            return true;
        }
        X11.XWMHints wmHints = x11.createXWMHints(wmHintsPtr);
        long flags = wmHints.get_flags();
        int input = wmHints.get_input();
        x11.XFree(wmHintsPtr);
        if ((flags & X11Defs.InputHint) == 0) {
            return true;
        }
        return input != 0;
    }

    /**
     * @return
     */
    long getInputFocus() {
        CLongPointer window = bridge.createCLongPointer(1, false);
        Int32Pointer revertStatus = bridge.createInt32Pointer(1, false);
        x11.XGetInputFocus(display, window, revertStatus);
        return window.get(0);
    }

    long getFocusedWindow() {
        return focusedWindow;
    }

    void setFocusedWindow(long windowId) {
        focusedWindow = windowId;
    }

    /**
     *
     * @param winId - the window ID
     * @param resizable - the window should be resizable
     * @param width - width to set if the window isn't resizable
     * @param height - height to set if the window isn't resizable
     */
    void setResizableHint(long winId, boolean resizable, int x, int y,
                          int width, int height) {
        setResizableHint(winId, resizable, false, x, y, width, height);
    }

    void setResizableHint(long winId, boolean resizable, boolean zoomed,
                          int x, int y, int width, int height) {
        final int MAX = Integer.MAX_VALUE;
        boolean setMaxSize = (!resizable ||
                              (zoomed && (width < MAX) && (height < MAX)));
        int flags = X11Defs.PMinSize | X11Defs.PSize | X11Defs.PPosition |
         X11Defs.USSize | X11Defs.USPosition;
        long sizeHintsPtr = x11.XAllocSizeHints();
        X11.XSizeHints sizeHints = x11.createXSizeHints(sizeHintsPtr);
        sizeHints.set_flags(flags);

        //save old hints settings
        x11.XGetWMNormalHints(display, winId, sizeHintsPtr, sizeHintsPtr);
        //sizeHints = x11.new XSizeHints(sizeHintsPtr);
        if (!zoomed) {
            sizeHints.set_min_height(!resizable ? height
                                               : LinuxWindow.MIN_WINDOW_HEIGHT);
            sizeHints.set_min_width(!resizable ? width
                                              : LinuxWindow.MIN_WINDOW_WIDTH);

            // Dummy values for obsolete fields
            sizeHints.set_x(x);
            sizeHints.set_y(y);
            sizeHints.set_width(width);
            sizeHints.set_height(height);
        }

        if (setMaxSize) {
            flags |= X11Defs.PMaxSize;
            sizeHints.set_max_height(height);
            sizeHints.set_max_width(width);
        }

        sizeHints.set_flags(flags);
        x11.XSetWMNormalHints(display, winId, sizeHintsPtr);
        x11.XFree(sizeHintsPtr);

    }
    /** register a window to receive notifications when WM frame extents
     * (i. e. native insets) change
     * @param winId - id of a window which wants to receive WM notifications
     */
    void requestFrameExtents(long winId) {
        X11.XEvent event =
            createClientMessage(winId, NET_REQUEST_FRAME_EXTENTS);
        sendClientMessage(event);
    }

    int sendClientMessage(X11.XEvent event) {
        int mask = (X11Defs.SubstructureNotifyMask |
                    X11Defs.SubstructureRedirectMask);
        int status = x11.XSendEvent(display, factory.getRootWindow(),
                                    X11Defs.False, mask, event);
        return status;
    }

    X11.XEvent createClientMessage(long winId, long msgType) {
        X11.XEvent retEvent = x11.createXEvent(false);
        X11.XClientMessageEvent cme = retEvent.get_xclient();
        cme.set_display(display);
        cme.set_window(winId);
        cme.set_message_type(msgType);
        cme.set_type(X11Defs.ClientMessage);
        cme.set_format(32);
        return retEvent;
    }

    int changeWindowState(LinuxWindow wnd, int action, long[] properties) {
        int mapState = wnd.getMapState();
        long winId = wnd.getId();
        if (mapState == X11Defs.IsUnmapped) {

            long[] props = getStateProps(wnd);
            if (props != null) {
                changeWindowProperty(winId, NET_WM_STATE, XA_ATOM, props);
            }
            return 0;
        }
        X11.XEvent clientEvent = createClientMessage(winId, NET_WM_STATE);
        CLongPointer data = clientEvent.get_xclient().get_l();
        data.set(0, action);  // add/remove/toggle
        for (int i = 0; i < properties.length; i++) {
            data.set(i + 1, properties[i]);
        }
        return sendClientMessage(clientEvent);
    }


    /**
     * @param wnd
     * @return
     */
    private long[] getStateProps(LinuxWindow wnd) {
        long props[] = new long[]{0l, 0l, 0l, 0l};
        int state = wnd.getCurrentState();
        int k = 0;
        if ((state & Frame.MAXIMIZED_HORIZ) != 0) {
            props[k++] = NET_WM_STATE_MAXIMIZED_HORZ;
        }
        if ((state & Frame.MAXIMIZED_VERT) != 0) {
            props[k++] = NET_WM_STATE_MAXIMIZED_VERT;
        }
        if ((state & Frame.ICONIFIED) != 0) {
            props[k++] = NET_WM_STATE_HIDDEN;
        }
        if (wnd.alwaysOnTop) {
            props[k++] = NET_WM_STATE_ABOVE;
        }
        long[] ret = new long[k];
        System.arraycopy(props, 0, ret, 0, k);
        return ret;
    }

    long[] getSupportedHints() {
        final long NET_SUPPORTED = internAtom("_NET_SUPPORTED"); //$NON-NLS-1$
        long root = factory.getRootWindow();
        long [] supportedHints = getWindowProperty(root, NET_SUPPORTED);
        return supportedHints;
    }

    private long[] getWindowProperty(long winId, final long propertyAtom) {
        CLongPointer type = bridge.createCLongPointer(1, false);
        Int32Pointer formatPtr = bridge.createInt32Pointer(1, false);
        CLongPointer nItemsPtr = bridge.createCLongPointer(1, false);
        CLongPointer bytesRemaining = bridge.createCLongPointer(1, false);
        PointerPointer data = bridge.createPointerPointer(1, false);

        final int anyType = X11Defs.AnyPropertyType;
        x11.XGetWindowProperty(display, winId, propertyAtom, 0, 1,
                               anyType, X11Defs.False, type,
                               formatPtr, nItemsPtr,
                               bytesRemaining, data);
        VoidPointer dataPtr = data.get(0);
        if (dataPtr == null) {
            return null;
        }
        x11.XFree(dataPtr);
        long nBytes = bytesRemaining.get(0);
        long typeAtom = type.get(0);
        if (typeAtom == X11Defs.None) {
            // the property doesn't exist
            return null;
        }
        int bitFormat = formatPtr.get(0);
        long nItems = (nBytes + 4) * 8 / bitFormat;
        long n32bitItems = nItems / (32 / bitFormat);
        x11.XGetWindowProperty(display, winId, propertyAtom, 0,
                               n32bitItems, anyType, X11Defs.False,
                               type, formatPtr, nItemsPtr,
                               bytesRemaining, data);

        nBytes = bytesRemaining.get(0);
        assert nBytes == 0;
        //read the data:
        int itemSize = bitFormat / 8;
        CLongPointer dataArray = bridge.createCLongPointer(data.get(0));
        long[] props = new long [(int)nItems];
        for (int i = 0; i < nItems; i++) {
            int item = 0;
            if (itemSize == 4) {
                item = (int)dataArray.get(i);
            } else if (itemSize == 2) {
                item = (short)dataArray.get(i);
            } else if (itemSize == 1) {
                item = (byte)dataArray.get(i);
            }
            props[i] = item;
        }
        x11.XFree(dataPtr);

        return props;
    }

    /**
     * @return array of child window id's
     * (for a top-level this maybe a WM frame window)
     */
    long[] getChildrenIDs(long windowID) {
        if (windowID == 0) {
            return new long[0];
        }

        CLongPointer root = bridge.createCLongPointer(1, false);
        CLongPointer parent = bridge.createCLongPointer(1, false);
        PointerPointer childrenArray = bridge.createPointerPointer(1, false);
        Int32Pointer childrenCount = bridge.createInt32Pointer(1, false);;
        x11.XQueryTree(display, windowID, root, parent,
                       childrenArray, childrenCount);

        int count = childrenCount.get(0);
        CLongPointer children = bridge.createCLongPointer(childrenArray.get(0));
        if (children == null) {
            return new long[0];
        }
        long[] result = new long[count];
        children.get(result, 0, count);
        x11.XFree(children);
        return result;
    }

    int setIcon(long windowID, long pixmap, int mask) {
        // save old WM hints
        long wmHintsPtr = x11.XGetWMHints(display, windowID);
        if (wmHintsPtr == 0) {
            wmHintsPtr = x11.XAllocWMHints();
        }
        X11.XWMHints wmHints = x11.createXWMHints(wmHintsPtr);
        int flags = (int) wmHints.get_flags() | X11Defs.IconPixmapHint;
        wmHints.set_icon_pixmap(pixmap);
        if (mask != 0) {
            flags |= X11Defs.IconMaskHint;
            wmHints.set_icon_mask(mask);
        }
        wmHints.set_flags(flags);
        int result = x11.XSetWMHints(display, windowID, wmHintsPtr);
        x11.XFree(wmHintsPtr);
        return result;
    }

    /**
     *
     * @param windowId - native window ID
     * @param property - could be NET_FRAME_EXTENTS or KDE_NET_WM_FRAME_STRUT
     * @return native insets set by Window Manager, or null if Window manager
     * doesn't support this property or property has unexpected formats
     */
    Insets getNativeInsets(long windowId, long property) {
        Insets insets = null;

        CLongPointer actualTypeReturn = bridge.createCLongPointer(1, false);
        Int32Pointer actualFormatReturn = bridge.createInt32Pointer(1, false);
        CLongPointer nitemsReturn = bridge.createCLongPointer(1, false);
        CLongPointer bytesAfterReturn = bridge.createCLongPointer(1, false);
        PointerPointer propReturn = bridge.createPointerPointer(1, false);

        int result = x11.XGetWindowProperty(factory.getDisplay(), windowId,
                property, 0, 4, X11Defs.FALSE,
                X11Defs.AnyPropertyType, actualTypeReturn, actualFormatReturn,
                nitemsReturn, bytesAfterReturn, propReturn);

        if (result == X11Defs.Success) {
            long nItems = nitemsReturn.get(0);
            long actualType = actualTypeReturn.get(0);
            int actualFormat = actualFormatReturn.get(0);
            CLongPointer ptrData = bridge.createCLongPointer(propReturn.get(0));
            if (ptrData == null) {
                return insets;
            }

            if ((nItems == 4) && (actualType == XA_CARDINAL)
                    && (actualFormat == 32)) {

                insets = new Insets(0, 0, 0, 0);
                insets.left = (int)ptrData.get(0);
                insets.right = (int)ptrData.get(1);
                insets.top = (int)ptrData.get(2);
                insets.bottom = (int)ptrData.get(3);
            }
            x11.XFree(ptrData);
        }

        return insets;
    }
}
