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

package java.awt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.EventListener;
import java.util.LinkedList;

import org.apache.harmony.awt.internal.nls.Messages;

public class AWTEventMulticaster implements ComponentListener, ContainerListener,
        FocusListener, KeyListener, MouseListener, MouseMotionListener, WindowListener,
        WindowFocusListener, WindowStateListener, ActionListener, ItemListener,
        AdjustmentListener, TextListener, InputMethodListener, HierarchyListener,
        HierarchyBoundsListener, MouseWheelListener {

    protected final EventListener a;

    protected final EventListener b;

    protected static EventListener addInternal(EventListener a, EventListener b) {
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        } else {
            return new AWTEventMulticaster(a, b);
        }
    }

    protected static EventListener removeInternal(EventListener l, EventListener oldl) {
        if ((l == oldl) || (l == null)) {
            return null;
        } else if (l instanceof AWTEventMulticaster) {
            return ((AWTEventMulticaster) l).remove(oldl);
        } else {
            return l;
        }
    }

    protected static void save(ObjectOutputStream s, String k, EventListener l)
            throws IOException {
        s.writeChars(k);
        s.writeObject(l);
    }

    @SuppressWarnings("unchecked")
    public static <T extends EventListener> T[] getListeners(EventListener l,
            Class<T> listenerType) throws ClassCastException {
        if (l == null) {
            return (T[]) Array.newInstance(listenerType, 0);
        }
        return addListeners(l, listenerType, new LinkedList<T>()).toArray(
                (T[]) Array.newInstance(listenerType, 0));
    }

    @SuppressWarnings("unchecked")
    private static <T extends EventListener> LinkedList<T> addListeners(EventListener l,
            Class<T> listenerType, LinkedList<T> list) {
        if (l instanceof AWTEventMulticaster) {
            AWTEventMulticaster ml = (AWTEventMulticaster) l;

            addListeners(ml.a, listenerType, list);
            addListeners(ml.b, listenerType, list);
        } else {
            if (l.getClass().isAssignableFrom(listenerType)) {
                list.add((T) l);
            }
        }

        return list;
    }

    public static ActionListener add(ActionListener a, ActionListener b) {
        return (ActionListener) addInternal(a, b);
    }

    public static ComponentListener add(ComponentListener a, ComponentListener b) {
        return (ComponentListener) addInternal(a, b);
    }

    public static ContainerListener add(ContainerListener a, ContainerListener b) {
        return (ContainerListener) addInternal(a, b);
    }

    public static WindowStateListener add(WindowStateListener a, WindowStateListener b) {
        return (WindowStateListener) addInternal(a, b);
    }

    public static FocusListener add(FocusListener a, FocusListener b) {
        return (FocusListener) addInternal(a, b);
    }

    public static WindowListener add(WindowListener a, WindowListener b) {
        return (WindowListener) addInternal(a, b);
    }

    public static HierarchyBoundsListener add(HierarchyBoundsListener a,
            HierarchyBoundsListener b) {
        return (HierarchyBoundsListener) addInternal(a, b);
    }

    public static WindowFocusListener add(WindowFocusListener a, WindowFocusListener b) {
        return (WindowFocusListener) addInternal(a, b);
    }

    public static HierarchyListener add(HierarchyListener a, HierarchyListener b) {
        return (HierarchyListener) addInternal(a, b);
    }

    public static TextListener add(TextListener a, TextListener b) {
        return (TextListener) addInternal(a, b);
    }

    public static InputMethodListener add(InputMethodListener a, InputMethodListener b) {
        return (InputMethodListener) addInternal(a, b);
    }

    public static MouseWheelListener add(MouseWheelListener a, MouseWheelListener b) {
        return (MouseWheelListener) addInternal(a, b);
    }

    public static ItemListener add(ItemListener a, ItemListener b) {
        return (ItemListener) addInternal(a, b);
    }

    public static MouseMotionListener add(MouseMotionListener a, MouseMotionListener b) {
        return (MouseMotionListener) addInternal(a, b);
    }

    public static KeyListener add(KeyListener a, KeyListener b) {
        return (KeyListener) addInternal(a, b);
    }

    public static MouseListener add(MouseListener a, MouseListener b) {
        return (MouseListener) addInternal(a, b);
    }

    public static AdjustmentListener add(AdjustmentListener a, AdjustmentListener b) {
        return (AdjustmentListener) addInternal(a, b);
    }

    public static MouseListener remove(MouseListener l, MouseListener oldl) {
        return (MouseListener) removeInternal(l, oldl);
    }

    public static ItemListener remove(ItemListener l, ItemListener oldl) {
        return (ItemListener) removeInternal(l, oldl);
    }

    public static MouseMotionListener remove(MouseMotionListener l, MouseMotionListener oldl) {
        return (MouseMotionListener) removeInternal(l, oldl);
    }

    public static InputMethodListener remove(InputMethodListener l, InputMethodListener oldl) {
        return (InputMethodListener) removeInternal(l, oldl);
    }

    public static MouseWheelListener remove(MouseWheelListener l, MouseWheelListener oldl) {
        return (MouseWheelListener) removeInternal(l, oldl);
    }

    public static HierarchyListener remove(HierarchyListener l, HierarchyListener oldl) {
        return (HierarchyListener) removeInternal(l, oldl);
    }

    public static TextListener remove(TextListener l, TextListener oldl) {
        return (TextListener) removeInternal(l, oldl);
    }

    public static HierarchyBoundsListener remove(HierarchyBoundsListener l,
            HierarchyBoundsListener oldl) {
        return (HierarchyBoundsListener) removeInternal(l, oldl);
    }

    public static WindowFocusListener remove(WindowFocusListener l, WindowFocusListener oldl) {
        return (WindowFocusListener) removeInternal(l, oldl);
    }

    public static FocusListener remove(FocusListener l, FocusListener oldl) {
        return (FocusListener) removeInternal(l, oldl);
    }

    public static WindowListener remove(WindowListener l, WindowListener oldl) {
        return (WindowListener) removeInternal(l, oldl);
    }

    public static ContainerListener remove(ContainerListener l, ContainerListener oldl) {
        return (ContainerListener) removeInternal(l, oldl);
    }

    public static WindowStateListener remove(WindowStateListener l, WindowStateListener oldl) {
        return (WindowStateListener) removeInternal(l, oldl);
    }

    public static ComponentListener remove(ComponentListener l, ComponentListener oldl) {
        return (ComponentListener) removeInternal(l, oldl);
    }

    public static ActionListener remove(ActionListener l, ActionListener oldl) {
        return (ActionListener) removeInternal(l, oldl);
    }

    public static AdjustmentListener remove(AdjustmentListener l, AdjustmentListener oldl) {
        return (AdjustmentListener) removeInternal(l, oldl);
    }

    public static KeyListener remove(KeyListener l, KeyListener oldl) {
        return (KeyListener) removeInternal(l, oldl);
    }

    protected AWTEventMulticaster(EventListener a, EventListener b) {
        // awt.74=Input parameters a and b should not be null
        assert (a != null) && (b != null) : Messages.getString("awt.74"); //$NON-NLS-1$

        this.a = a;
        this.b = b;
    }

    protected EventListener remove(EventListener oldl) {
        if (b == oldl) {
            return a;
        } else if (a == oldl) {
            return b;
        } else {
            return this;
        }
    }

    protected void saveInternal(ObjectOutputStream s, String k) throws IOException {
        s.writeChars(k);
        s.writeObject(a);
        s.writeObject(b);
    }

    public void actionPerformed(ActionEvent e) {
        if ((a != null) && (a instanceof ActionListener)) {
            ((ActionListener) a).actionPerformed(e);
        }
        if ((b != null) && (b instanceof ActionListener)) {
            ((ActionListener) b).actionPerformed(e);
        }
    }

    public void adjustmentValueChanged(AdjustmentEvent e) {
        if ((a != null) && (a instanceof AdjustmentListener)) {
            ((AdjustmentListener) a).adjustmentValueChanged(e);
        }
        if ((b != null) && (b instanceof AdjustmentListener)) {
            ((AdjustmentListener) b).adjustmentValueChanged(e);
        }
    }

    public void ancestorMoved(HierarchyEvent e) {
        if ((a != null) && (a instanceof HierarchyBoundsListener)) {
            ((HierarchyBoundsListener) a).ancestorMoved(e);
        }
        if ((b != null) && (b instanceof HierarchyBoundsListener)) {
            ((HierarchyBoundsListener) b).ancestorMoved(e);
        }
    }

    public void ancestorResized(HierarchyEvent e) {
        if ((a != null) && (a instanceof HierarchyBoundsListener)) {
            ((HierarchyBoundsListener) a).ancestorResized(e);
        }
        if ((b != null) && (b instanceof HierarchyBoundsListener)) {
            ((HierarchyBoundsListener) b).ancestorResized(e);
        }
    }

    public void caretPositionChanged(InputMethodEvent e) {
        if ((a != null) && (a instanceof InputMethodListener)) {
            ((InputMethodListener) a).caretPositionChanged(e);
        }
        if ((b != null) && (b instanceof InputMethodListener)) {
            ((InputMethodListener) b).caretPositionChanged(e);
        }
    }

    public void componentAdded(ContainerEvent e) {
        if ((a != null) && (a instanceof ContainerListener)) {
            ((ContainerListener) a).componentAdded(e);
        }
        if ((b != null) && (b instanceof ContainerListener)) {
            ((ContainerListener) b).componentAdded(e);
        }
    }

    public void componentHidden(ComponentEvent e) {
        if ((a != null) && (a instanceof ComponentListener)) {
            ((ComponentListener) a).componentHidden(e);
        }
        if ((b != null) && (b instanceof ComponentListener)) {
            ((ComponentListener) b).componentHidden(e);
        }
    }

    public void componentMoved(ComponentEvent e) {
        if ((a != null) && (a instanceof ComponentListener)) {
            ((ComponentListener) a).componentMoved(e);
        }
        if ((b != null) && (b instanceof ComponentListener)) {
            ((ComponentListener) b).componentMoved(e);
        }
    }

    public void componentRemoved(ContainerEvent e) {
        if ((a != null) && (a instanceof ContainerListener)) {
            ((ContainerListener) a).componentRemoved(e);
        }
        if ((b != null) && (b instanceof ContainerListener)) {
            ((ContainerListener) b).componentRemoved(e);
        }
    }

    public void componentResized(ComponentEvent e) {
        if ((a != null) && (a instanceof ComponentListener)) {
            ((ComponentListener) a).componentResized(e);
        }
        if ((b != null) && (b instanceof ComponentListener)) {
            ((ComponentListener) b).componentResized(e);
        }
    }

    public void componentShown(ComponentEvent e) {
        if ((a != null) && (a instanceof ComponentListener)) {
            ((ComponentListener) a).componentShown(e);
        }
        if ((b != null) && (b instanceof ComponentListener)) {
            ((ComponentListener) b).componentShown(e);
        }
    }

    public void focusGained(FocusEvent e) {
        if ((a != null) && (a instanceof FocusListener)) {
            ((FocusListener) a).focusGained(e);
        }
        if ((b != null) && (b instanceof FocusListener)) {
            ((FocusListener) b).focusGained(e);
        }
    }

    public void focusLost(FocusEvent e) {
        if ((a != null) && (a instanceof FocusListener)) {
            ((FocusListener) a).focusLost(e);
        }
        if ((b != null) && (b instanceof FocusListener)) {
            ((FocusListener) b).focusLost(e);
        }
    }

    public void hierarchyChanged(HierarchyEvent e) {
        if ((a != null) && (a instanceof HierarchyListener)) {
            ((HierarchyListener) a).hierarchyChanged(e);
        }
        if ((b != null) && (b instanceof HierarchyListener)) {
            ((HierarchyListener) b).hierarchyChanged(e);
        }
    }

    public void inputMethodTextChanged(InputMethodEvent e) {
        if ((a != null) && (a instanceof InputMethodListener)) {
            ((InputMethodListener) a).inputMethodTextChanged(e);
        }
        if ((b != null) && (b instanceof InputMethodListener)) {
            ((InputMethodListener) b).inputMethodTextChanged(e);
        }
    }

    public void itemStateChanged(ItemEvent e) {
        if ((a != null) && (a instanceof ItemListener)) {
            ((ItemListener) a).itemStateChanged(e);
        }
        if ((b != null) && (b instanceof ItemListener)) {
            ((ItemListener) b).itemStateChanged(e);
        }
    }

    public void keyPressed(KeyEvent e) {
        if ((a != null) && (a instanceof KeyListener)) {
            ((KeyListener) a).keyPressed(e);
        }
        if ((b != null) && (b instanceof KeyListener)) {
            ((KeyListener) b).keyPressed(e);
        }
    }

    public void keyReleased(KeyEvent e) {
        if ((a != null) && (a instanceof KeyListener)) {
            ((KeyListener) a).keyReleased(e);
        }
        if ((b != null) && (b instanceof KeyListener)) {
            ((KeyListener) b).keyReleased(e);
        }
    }

    public void keyTyped(KeyEvent e) {
        if ((a != null) && (a instanceof KeyListener)) {
            ((KeyListener) a).keyTyped(e);
        }
        if ((b != null) && (b instanceof KeyListener)) {
            ((KeyListener) b).keyTyped(e);
        }
    }

    public void mouseClicked(MouseEvent e) {
        if ((a != null) && (a instanceof MouseListener)) {
            ((MouseListener) a).mouseClicked(e);
        }
        if ((b != null) && (b instanceof MouseListener)) {
            ((MouseListener) b).mouseClicked(e);
        }
    }

    public void mouseDragged(MouseEvent e) {
        if ((a != null) && (a instanceof MouseMotionListener)) {
            ((MouseMotionListener) a).mouseDragged(e);
        }
        if ((b != null) && (b instanceof MouseMotionListener)) {
            ((MouseMotionListener) b).mouseDragged(e);
        }
    }

    public void mouseEntered(MouseEvent e) {
        if ((a != null) && (a instanceof MouseListener)) {
            ((MouseListener) a).mouseEntered(e);
        }
        if ((b != null) && (b instanceof MouseListener)) {
            ((MouseListener) b).mouseEntered(e);
        }
    }

    public void mouseExited(MouseEvent e) {
        if ((a != null) && (a instanceof MouseListener)) {
            ((MouseListener) a).mouseExited(e);
        }
        if ((b != null) && (b instanceof MouseListener)) {
            ((MouseListener) b).mouseExited(e);
        }
    }

    public void mouseMoved(MouseEvent e) {
        if ((a != null) && (a instanceof MouseMotionListener)) {
            ((MouseMotionListener) a).mouseMoved(e);
        }
        if ((b != null) && (b instanceof MouseMotionListener)) {
            ((MouseMotionListener) b).mouseMoved(e);
        }
    }

    public void mousePressed(MouseEvent e) {
        if ((a != null) && (a instanceof MouseListener)) {
            ((MouseListener) a).mousePressed(e);
        }
        if ((b != null) && (b instanceof MouseListener)) {
            ((MouseListener) b).mousePressed(e);
        }
    }

    public void mouseReleased(MouseEvent e) {
        if ((a != null) && (a instanceof MouseListener)) {
            ((MouseListener) a).mouseReleased(e);
        }
        if ((b != null) && (b instanceof MouseListener)) {
            ((MouseListener) b).mouseReleased(e);
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        if ((a != null) && (a instanceof MouseWheelListener)) {
            ((MouseWheelListener) a).mouseWheelMoved(e);
        }
        if ((b != null) && (b instanceof MouseWheelListener)) {
            ((MouseWheelListener) b).mouseWheelMoved(e);
        }
    }

    public void textValueChanged(TextEvent e) {
        if ((a != null) && (a instanceof TextListener)) {
            ((TextListener) a).textValueChanged(e);
        }
        if ((b != null) && (b instanceof TextListener)) {
            ((TextListener) b).textValueChanged(e);
        }
    }

    public void windowActivated(WindowEvent e) {
        if ((a != null) && (a instanceof WindowListener)) {
            ((WindowListener) a).windowActivated(e);
        }
        if ((b != null) && (b instanceof WindowListener)) {
            ((WindowListener) b).windowActivated(e);
        }
    }

    public void windowClosed(WindowEvent e) {
        if ((a != null) && (a instanceof WindowListener)) {
            ((WindowListener) a).windowClosed(e);
        }
        if ((b != null) && (b instanceof WindowListener)) {
            ((WindowListener) b).windowClosed(e);
        }
    }

    public void windowClosing(WindowEvent e) {
        if ((a != null) && (a instanceof WindowListener)) {
            ((WindowListener) a).windowClosing(e);
        }
        if ((b != null) && (b instanceof WindowListener)) {
            ((WindowListener) b).windowClosing(e);
        }
    }

    public void windowDeactivated(WindowEvent e) {
        if ((a != null) && (a instanceof WindowListener)) {
            ((WindowListener) a).windowDeactivated(e);
        }
        if ((b != null) && (b instanceof WindowListener)) {
            ((WindowListener) b).windowDeactivated(e);
        }
    }

    public void windowDeiconified(WindowEvent e) {
        if ((a != null) && (a instanceof WindowListener)) {
            ((WindowListener) a).windowDeiconified(e);
        }
        if ((b != null) && (b instanceof WindowListener)) {
            ((WindowListener) b).windowDeiconified(e);
        }
    }

    public void windowGainedFocus(WindowEvent e) {
        if ((a != null) && (a instanceof WindowFocusListener)) {
            ((WindowFocusListener) a).windowGainedFocus(e);
        }
        if ((b != null) && (b instanceof WindowFocusListener)) {
            ((WindowFocusListener) b).windowGainedFocus(e);
        }
    }

    public void windowIconified(WindowEvent e) {
        if ((a != null) && (a instanceof WindowListener)) {
            ((WindowListener) a).windowIconified(e);
        }
        if ((b != null) && (b instanceof WindowListener)) {
            ((WindowListener) b).windowIconified(e);
        }
    }

    public void windowLostFocus(WindowEvent e) {
        if ((a != null) && (a instanceof WindowFocusListener)) {
            ((WindowFocusListener) a).windowLostFocus(e);
        }
        if ((b != null) && (b instanceof WindowFocusListener)) {
            ((WindowFocusListener) b).windowLostFocus(e);
        }
    }

    public void windowOpened(WindowEvent e) {
        if ((a != null) && (a instanceof WindowListener)) {
            ((WindowListener) a).windowOpened(e);
        }
        if ((b != null) && (b instanceof WindowListener)) {
            ((WindowListener) b).windowOpened(e);
        }
    }

    public void windowStateChanged(WindowEvent e) {
        if ((a != null) && (a instanceof WindowStateListener)) {
            ((WindowStateListener) a).windowStateChanged(e);
        }
        if ((b != null) && (b instanceof WindowStateListener)) {
            ((WindowStateListener) b).windowStateChanged(e);
        }
    }
}
