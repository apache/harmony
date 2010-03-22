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
package org.apache.harmony.awt.wtk.linux;

import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.PaintEvent;
import java.awt.event.WindowEvent;

import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.nativebridge.CLongPointer;
import org.apache.harmony.awt.nativebridge.Int32Pointer;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.PointerPointer;
import org.apache.harmony.awt.nativebridge.linux.X11;
import org.apache.harmony.awt.nativebridge.linux.X11Defs;
import org.apache.harmony.awt.wtk.NativeEvent;

/**
 * Information about X11 events can be found at http://www.xfree86.org/current/manindex3.html
 */

public final class LinuxEvent extends NativeEvent {
    private static final X11 x11 = X11.getInstance();
    private static final NativeBridge bridge = NativeBridge.getInstance();
    private final LinuxWindowFactory factory;

    private static final int MOUSE_BUTTONS_MASK =
        (X11Defs.Button1Mask | X11Defs.Button2Mask | X11Defs.Button3Mask);

    private static long utcOffset = X11Defs.CurrentTime;

    private final LinuxEventQueue nativeQueue;
    /** Clip area of paint event */
    private MultiRectArea clipRects;

    /** X event type */
    private int nativeEventId;
    /** Invisible auxlitary window for receiving service events */
    private final long javaWindowId;

    private Insets insets = new Insets(0, 0, 0, 0);


    public static long getUTCOffset() {
        return utcOffset;
    }

    LinuxEvent(LinuxWindowFactory factory, 
               LinuxEventQueue nativeQueue,
                          X11.XEvent event) {
        this.javaWindowId = factory.getJavaWindow();
        this.nativeQueue = nativeQueue;
        this.factory = factory;
        setEvent(event);
    }

    public MultiRectArea getClipRects() {
        return new MultiRectArea(clipRects);
    }

    public Rectangle getClipBounds() {
        return clipRects.getBounds();
    }

    public Insets getInsets() {
        return insets;
    }

    public boolean getTrigger() {
        return ((eventId == MouseEvent.MOUSE_PRESSED)
                && (mouseButton == MouseEvent.BUTTON3));
    }

    private void setEvent(X11.XEvent event) {

        windowId = event.get_xany().get_window();
        nativeEventId = event.get_type();
        eventId = ID_PLATFORM;

        if (windowId == javaWindowId) {
            return;
        }
        if (!factory.validWindowId(windowId) &&
                   windowId != factory.getRootWindow()) {
            return;
        }

        switch (nativeEventId) {
        case X11Defs.KeyPress:
        case X11Defs.KeyRelease:
            X11.XKeyEvent keyEvent = event.get_xkey();
            validateUTCOffset(keyEvent.get_time());
            processKeyEvent(keyEvent);
            break;
        case X11Defs.ButtonPress:
        case X11Defs.ButtonRelease:
            X11.XButtonEvent buttonEvent = event.get_xbutton();
            validateUTCOffset(buttonEvent.get_time());
            processButtonEvent(buttonEvent);
            break;
        case X11Defs.MotionNotify:
            X11.XMotionEvent motionEvent = event.get_xmotion();
            validateUTCOffset(motionEvent.get_time());
            processMotionEvent(motionEvent);
            break;
        case X11Defs.FocusIn:
        case X11Defs.FocusOut:
            X11.XFocusChangeEvent focusChangeEvent = event.get_xfocus();
            processFocusChangeEvent(focusChangeEvent);
            break;
        case X11Defs.Expose:
            X11.XExposeEvent exposeEvent = event.get_xexpose();
            processExposeEvent(exposeEvent);
            break;
        case X11Defs.ConfigureNotify:
            X11.XConfigureEvent configureEvent = event.get_xconfigure();
            processConfigureEvent(configureEvent);
            break;
        case X11Defs.DestroyNotify:
            X11.XDestroyWindowEvent destroyWindowEvent = event.get_xdestroywindow();
            processDestroyWindowEvent(destroyWindowEvent);
            break;
        case X11Defs.ClientMessage:
            X11.XClientMessageEvent clientMessageEvent = event.get_xclient();
            processClientMessageEvent(clientMessageEvent);
            break;
        case X11Defs.PropertyNotify:
            X11.XPropertyEvent propertyEvent = event.get_xproperty();
            validateUTCOffset(propertyEvent.get_time());
            processPropertyEvent(propertyEvent);
            break;
        case X11Defs.EnterNotify:
        case X11Defs.LeaveNotify:
            X11.XCrossingEvent crossingEvent = event.get_xcrossing();
            validateUTCOffset(crossingEvent.get_time());
            processCrossingEvent(crossingEvent);
            break;
        case X11Defs.ReparentNotify:
            X11.XReparentEvent reparentEvent = event.get_xreparent();
            processReparentEvent(reparentEvent);
            break;
        default:
            eventId = ID_PLATFORM;
         /*//Other events may be useful in future
        case X11Defs.UnmapNotify:
            X11.XUnmapEvent unmapEvent = x11.new XUnmapEvent(event
                    .get_xunmap_ptr());
            processUnmapEvent(unmapEvent);
            break;
        case X11Defs.MapNotify:
            X11.XMapEvent mapEvent = x11.new XMapEvent(event.get_xmap_ptr());
            processMapEvent(mapEvent);
            break;
         case X11Defs.GraphicsExpose:
         X11.XGraphicsExposeEvent graphicsExposeEvent = x11.new XGraphicsExposeEvent(event.get_xgraphicsexpose());
         processGraphicsExposeEvent(graphicsExposeEvent);
         break;
         case X11Defs.NoExpose:
         X11.XNoExposeEvent noExposeEvent = x11.new XNoExposeEvent(event.get_xnoexpose());
         processNoExposeEventEvent(noExposeEvent);
         break;
         case X11Defs.VisibilityNotify:
         X11.XVisibilityEvent visibilityEvent = x11.new XVisibilityEvent(event.get_xvisibility());
         processVisibilityEvent(visibilityEvent);
         break;
         case X11Defs.MapRequest:
         X11.XMapRequestEvent mapRequestEvent = x11.new XMapRequestEvent(event.get_xmaprequest());
         processMapRequestEvent(mapRequestEvent);
         break;
         case X11Defs.ConfigureRequest:
         X11.XConfigureRequestEvent configureRequestEvent = x11.new XConfigureRequestEvent(event.get_xconfigurerequest());
         processConfigureRequestEvent(configureRequestEvent);
         break;
         case X11Defs.GravityNotify:
         X11.XGravityEvent gravityEvent = x11.new XGravityEvent(event.get_xgravity());
         processGravityEvent(gravityEvent);
         break;
         case X11Defs.ResizeRequest:
         X11.XResizeRequestEvent resizeRequestEvent = x11.new XResizeRequestEvent(event.get_xresizerequest());
         processResizeRequestEvent(resizeRequestEvent);
         break;
         case X11Defs.CirculateNotify:
         X11.XCirculateEvent circulateEvent = x11.new XCirculateEvent(event.get_xcirculate());
         processCirculateEvent(circulateEvent);
         break;
         case X11Defs.CirculateRequest:
         X11.XCirculateRequestEvent circulateRequestEvent = x11.new XCirculateRequestEvent(event.get_xcirculaterequest());
         processCirculateRequestEvent(circulateRequestEvent);
         break;
         case X11Defs.SelectionClear:
         X11.XSelectionClearEvent selectionClearEvent = x11.new XSelectionClearEvent(event.get_xselectionclear());
         processSelectionClearEvent(selectionClearEvent);
         break;
         case X11Defs.SelectionRequest:
         X11.XSelectionRequestEvent selectionRequestEvent = x11.new XSelectionRequestEvent(event.get_xselectionrequest());
         processSelectionRequestEvent(selectionRequestEvent);
         break;
         case X11Defs.SelectionNotify:
         X11.XSelectionEvent selectionEvent = x11.new XSelectionEvent(event.get_xselection());
         processSelectionEvent(selectionEvent);
         break;
         case X11Defs.ColormapNotify:
         X11.XColormapEvent colormapEvent = x11.new XColormapEvent(event.get_xcolormap());
         processColormapEvent(colormapEvent);
         break;
         case X11Defs.MappingNotify:
         X11.XMappingEvent mappingEvent = x11.new XMappingEvent(event.get_xmapping());
         processMappingEvent(mappingEvent);
         break;
         case X11Defs.KeymapNotify:
         X11.XKeymapEvent keymapEvent = x11.new XKeymapEvent(event.get_xkeymap());
         processKeymapEvent(keymapEvent);
         break;
         default:
         //>X11Defs.LASTEvent => undefined event
         //<2 => error or reply (XErrorEvent xerror);*/
        }
    }

    private void processReparentEvent(X11.XReparentEvent reparentEvent) {
        processInsetsChange(null);
    }

    private void processKeyEvent(X11.XKeyEvent event) {
        eventId = (nativeEventId == X11Defs.KeyPress) ?
                KeyEvent.KEY_PRESSED :
                KeyEvent.KEY_RELEASED;

        time = event.get_time() + utcOffset;
        translateModifiers(event.get_state());

        keyInfo = KeyCodeTranslator.translateEvent(event);
        forwardToContent();
    }

    private void processButtonEvent(X11.XButtonEvent event) {
        int button = event.get_button();

        if (button > X11Defs.Button3) {
            eventId = MouseEvent.MOUSE_WHEEL;
            wheelRotation = (button == X11Defs.Button4) ? -1 : 1;
        } else {
            eventId = (nativeEventId == X11Defs.ButtonPress) ?
                    MouseEvent.MOUSE_PRESSED : MouseEvent.MOUSE_RELEASED;
            mouseButton = button - X11Defs.Button1 + MouseEvent.BUTTON1;
        }

        time = event.get_time() + utcOffset;
        localPos = new Point(event.get_x(), event.get_y());
        screenPos = new Point(event.get_x_root(), event.get_y_root());
        translateButtonModifiers(event.get_state());
        if (!updateGrabState(event)) {
            eventId = ID_MOUSE_GRAB_CANCELED;
        }
    }

    private void processMotionEvent(X11.XMotionEvent event) {
        LinuxWindow win = (LinuxWindow) factory.getWindowById(windowId);
        
        if (win == null) {
            return;
        }

        localPos = new Point(event.get_x(), event.get_y());
        if (!localPos.equals(win.queryPointer())) {
            return;
        }
        screenPos = new Point(event.get_x_root(), event.get_y_root());
        if ((event.get_state() & MOUSE_BUTTONS_MASK) > 0) {
            eventId = MouseEvent.MOUSE_DRAGGED;
        } else {
            eventId = MouseEvent.MOUSE_MOVED;
        }
        time = event.get_time() + utcOffset;
        translateModifiers(event.get_state());
    }

    private void processCrossingEvent(X11.XCrossingEvent event) {
        eventId = (nativeEventId == X11Defs.EnterNotify) ?
                MouseEvent.MOUSE_ENTERED : MouseEvent.MOUSE_EXITED;
        localPos = new Point(event.get_x(), event.get_y());
        screenPos = new Point(event.get_x_root(), event.get_y_root());
        time = event.get_time() + utcOffset;
        translateModifiers(event.get_state());
    }

    private void processFocusChangeEvent(X11.XFocusChangeEvent event) {
        int detail = event.get_detail();
        //ignore events sent to windows other than source or destination
        if (detail == X11Defs.NotifyNonlinearVirtual
                || detail == X11Defs.NotifyVirtual) {
            return;
        }
        boolean gainedFocus = (nativeEventId == X11Defs.FocusIn);
        WindowManager wm = factory.wm;
        long focusedWindow = wm.getFocusedWindow();
        long serverFocusedWindow = wm.getInputFocus();
        long newFocusedWindow = gainedFocus ? event.get_window() :
            serverFocusedWindow;
        forwardToContent();
        eventId = gainedFocus ? FocusEvent.FOCUS_GAINED
                : FocusEvent.FOCUS_LOST;

        otherWindowId = gainedFocus ? focusedWindow : newFocusedWindow;
        //have to check if this window belongs to our Java application
        if (!factory.validWindowId(otherWindowId)) {
            otherWindowId = 0;
            //if lost focus to window in some other app, clear focusedWindow:
            if (!gainedFocus) {
                wm.setFocusedWindow(0);
            }
        } else {
            otherWindowId = getContentId(otherWindowId);
        }
        if (gainedFocus) {
            wm.setFocusedWindow(newFocusedWindow);
        }
    }

    private void processExposeEvent(X11.XExposeEvent event) {
        MultiRectArea clip = nativeQueue.getAccumulatedClip(windowId);

        clip.add(new Rectangle(event.get_x(), event.get_y(),
                event.get_width(), event.get_height()));

        if (event.get_count() != 0) {
            eventId = ID_PLATFORM;
        } else {
            eventId = PaintEvent.PAINT;

            clipRects = clip;
            nativeQueue.resetAccumulatedClip(windowId);
        }
    }

    private void processConfigureEvent(X11.XConfigureEvent event) {
        LinuxWindow win = (LinuxWindow)factory.getWindowById(windowId);
        
        if (win == null) {
            return;
        }
        
        boolean child = win.isChild();
        boolean isSynthetic = event.get_send_event() != 0;

        windowRect = new Rectangle(event.get_x(), event.get_y(),
                event.get_width(), event.get_height());

        //in real configure events new position is reported relative to parent
        //so have to translate it for top-level(managed by WM)windows
        //to get root-related coords
        if (!child && !isSynthetic) {
            Int32Pointer x = bridge.createInt32Pointer(1, false);
            Int32Pointer y = bridge.createInt32Pointer(1, false);
            CLongPointer childWindow = bridge.createCLongPointer(1, false);

            long parentId = win.getParentID();
            x11.XTranslateCoordinates(win.getDisplay(), parentId,
                                      factory.getRootWindow(),
                                      windowRect.x, windowRect.y,
                                      x, y, childWindow);
            windowRect.setLocation(x.get(0), y.get(0));
        }

        boolean isContent = (win instanceof ContentWindow);

        if (!isContent && child && !windowRect.equals(win.getBounds())) {
            eventId = ID_PLATFORM;
            return;
        }

        eventId = ID_BOUNDS_CHANGED;
        Rectangle oldRect = win.onBoundsChange(windowRect);
        if (isContent) {
            ContentWindow content = (ContentWindow) win;
            windowRect.setLocation(content.getBounds().getLocation());
        }
        if (windowRect.equals(oldRect)) {
            eventId = /*insetsChanged ? ID_INSETS_CHANGED : */ID_PLATFORM;
        }

        LinuxWindow content = win.getContentWindow();
        if (content != null) {
            if (win.resized) {
                Insets ins = win.getInsets();
                int dw = ins.left + ins.right;
                int dh = ins.top + ins.bottom;
                x11.XResizeWindow(win.getDisplay(), content.getId(),
                        windowRect.width + dw, windowRect.height + dh);
                x11.XFlush(win.getDisplay());
            }
            if (isSynthetic && win.moved) {
                windowId = content.getId();
                windowRect.setBounds(content.getBounds());
            }

        }
    }

    private void processDestroyWindowEvent(X11.XDestroyWindowEvent event) {
        eventId = WindowEvent.WINDOW_CLOSED;
        factory.onWindowDispose(windowId);
    }

    private void processPropertyEvent(X11.XPropertyEvent event) {
        long atom = event.get_atom();
        if (atom == factory.wm.NET_WM_STATE) {
            processWindowStateEvent();
        }  else if (atom == factory.wm.NET_FRAME_EXTENTS ||
                atom == factory.wm.KDE_NET_WM_FRAME_STRUT) {
            processFrameExtentsEvent(atom);
        }
    }

    /**
     * @param property - could be NET_FRAME_EXTENTS or KDE_NET_WM_FRAME_STRUT
     */
    private void processFrameExtentsEvent(long property) {
        insets = factory.wm.getNativeInsets(windowId, property);
    }

    /**
     *
     */
    private void processInsetsChange(Insets newInsets) {
        LinuxWindow win = (LinuxWindow) factory.getWindowById(windowId);
        
        if ((win == null) || win.isChild() || win.isUndecorated()) {
            return;
        }
        
        eventId = ID_INSETS_CHANGED;

        if (newInsets != null) {
            win.setInsets(newInsets);
        } else {
            insets = win.updateInsets();
        }
        forwardToContent();

    }

    private void processWindowStateEvent() {
        CLongPointer actualTypeReturn = bridge.createCLongPointer(1, false);
        Int32Pointer actualFormatReturn = bridge.createInt32Pointer(1, false);
        CLongPointer nitemsReturn = bridge.createCLongPointer(1, false);
        CLongPointer bytesAfterReturn = bridge.createCLongPointer(1, false);
        PointerPointer propReturn = bridge.createPointerPointer(1, false);

        x11.XGetWindowProperty(factory.getDisplay(), windowId,
                factory.wm.NET_WM_STATE, 0, Integer.MAX_VALUE, X11Defs.FALSE,
                X11Defs.AnyPropertyType, actualTypeReturn, actualFormatReturn,
                nitemsReturn, bytesAfterReturn, propReturn);

        int count = (int)nitemsReturn.get(0);
        if (count == 0) {
            return;
        }
        if (actualFormatReturn.get(0) == 32) {
            CLongPointer types = bridge.createCLongPointer(propReturn.get(0));
            deriveNewWindowState(count, types);
        } else {
            // awt.10=Only 32-bit format is supported for window state operations.
            throw new RuntimeException(Messages.getString("awt.10")); //$NON-NLS-1$
        }
    }

    private void deriveNewWindowState(int typesCount, CLongPointer types) {
        LinuxWindow win = (LinuxWindow) factory.getWindowById(windowId);

        if (win == null) {
            return;
        }
        
        int oldState = win.getState();
        int newState = 0;
        boolean oldAlwaysOnTop = win.alwaysOnTop;
        boolean newAlwaysOnTop = false;

        for (int i=0; i<typesCount; i++) {
            long type = types.get(i);
            if (type == factory.wm.NET_WM_STATE_MAXIMIZED_HORZ) {
                newState |= Frame.MAXIMIZED_HORIZ;
            } else if (type == factory.wm.NET_WM_STATE_MAXIMIZED_VERT) {
                newState |= Frame.MAXIMIZED_VERT;
            } else if (type == factory.wm.NET_WM_STATE_HIDDEN) {
                newState |= Frame.ICONIFIED;
            } else if (type == factory.wm.NET_WM_STATE_ABOVE) {
                newAlwaysOnTop = true;
            }
        }

        win.setMaximized((newState & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH);
        win.setIconified((newState & Frame.ICONIFIED) == Frame.ICONIFIED);
        if (newState != oldState) {
            windowState = newState;
            eventId = WindowEvent.WINDOW_STATE_CHANGED;
        }
        if (oldAlwaysOnTop != oldAlwaysOnTop) {
            win.alwaysOnTop = newAlwaysOnTop;
        }
        forwardToContent();
    }

    private void processClientMessageEvent(X11.XClientMessageEvent event) {
        if (event.get_message_type() == factory.wm.WM_PROTOCOLS) {
            CLongPointer data = event.get_l();
            long protocol = data.get(0);
            if (protocol == factory.wm.WM_DELETE_WINDOW) {
                LinuxWindow lw = (LinuxWindow)factory.getWindowById(windowId);
                LinuxWindow cw = lw.getContentWindow();
                if (lw.isInputAllowed()) {
                    eventId = WindowEvent.WINDOW_CLOSING;
                    if (cw != null) {
                        // forward closing event to the content window:
                        windowId = cw.getId();
                    }
                }
            }
            // TODO: Process other Window Manager's events here
        }
    }

    private void translateButtonModifiers(int state) {
        translateModifiers(state);
        
        final int m = (mouseButton == MouseEvent.BUTTON1)
            ? InputEvent.BUTTON1_DOWN_MASK
            : (mouseButton == MouseEvent.BUTTON2) ? InputEvent.BUTTON2_DOWN_MASK
            : (mouseButton == MouseEvent.BUTTON3) ? InputEvent.BUTTON3_DOWN_MASK 
            : 0;

        if (eventId == MouseEvent.MOUSE_PRESSED) {
            modifiers |= m;
        } else if (eventId == MouseEvent.MOUSE_RELEASED) {
            modifiers &= ~m;
        }
    }

    private void translateModifiers(int state) {
        if ((state & X11Defs.Mod1Mask) > 0) {
            modifiers |= InputEvent.ALT_DOWN_MASK;
        }
        if ((state & X11Defs.Mod5Mask) > 0) {
            modifiers |= InputEvent.ALT_GRAPH_DOWN_MASK;
        }
        if ((state & X11Defs.ControlMask) > 0) {
            modifiers |= InputEvent.CTRL_DOWN_MASK;
        }
        if ((state & X11Defs.ShiftMask) > 0) {
            modifiers |= InputEvent.SHIFT_DOWN_MASK;
        }
        if ((state & X11Defs.Button1Mask) > 0) {
            modifiers |= InputEvent.BUTTON1_DOWN_MASK;
        }
        if ((state & X11Defs.Button2Mask) > 0) {
            modifiers |= InputEvent.BUTTON2_DOWN_MASK;
        }
        if ((state & X11Defs.Button3Mask) > 0) {
            modifiers |= InputEvent.BUTTON3_DOWN_MASK;
        }
    }

    private boolean updateGrabState(X11.XButtonEvent event) {
        int mouseState = (event.get_state() & MOUSE_BUTTONS_MASK);

        switch (event.get_type()) {
        case X11Defs.ButtonPress:
            // Only one mouse button is pressed
            if (mouseState == X11Defs.Button1Mask ||
                    mouseState == X11Defs.Button2Mask ||
                    mouseState == X11Defs.Button3Mask) {

                if (!LinuxWindow.startAutoGrab(event.get_window(), event.get_x(), event.get_y())) {
                    return false;
                }
            }
            break;
        case X11Defs.ButtonRelease:
            // All mouse buttons were actually released
            if (mouseState == 0) {
                LinuxWindow.endAutoGrab();
            }
            break;
        }

        return true;
    }

    private void validateUTCOffset(long time) {
        if (utcOffset == X11Defs.CurrentTime) {
            utcOffset = System.currentTimeMillis() - time;
        }
    }

    public String getCompositionString() {
        return new String();
    }

    public int getCompCursorPos() {
        return 0;
    }

    private long getContentId(long winId) {
        LinuxWindow win = (LinuxWindow) factory.getWindowById(winId);
        
        if (win != null) {
            LinuxWindow content = win.getContentWindow();
            
            if (content != null) {
                long contentId = content.getId();
                
                if (factory.validWindowId(contentId)) {
                    return contentId;
                }
            }
        }
        
        return winId;
    }

    private void forwardToContent() {
        windowId = getContentId(windowId);
    }
    
       public String toString() {
               return "window=0x" + Long.toHexString(windowId) + ", event=" + eventId; //$NON-NLS-1$ //$NON-NLS-2$
       }
}
