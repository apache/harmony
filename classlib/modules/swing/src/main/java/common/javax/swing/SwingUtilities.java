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
 */
package javax.swing;

import java.applet.Applet;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleStateSet;
import javax.swing.plaf.UIResource;

import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class SwingUtilities implements SwingConstants {

    private static final Rectangle auxRect = new Rectangle();

    private SwingUtilities() {
    }

    public static String layoutCompoundLabel(final JComponent c, final FontMetrics fm, final String text, final Icon icon,
            final int verticalAlignment, final int horizontalAlignment,
            final int verticalTextPosition, final int horizontalTextPosition,
            final Rectangle viewR, final Rectangle iconR, final Rectangle textR,
            final int textIconGap) {

        final int hTextPos = Utilities.convertLeadTrail(horizontalTextPosition, c);
        final int hPos = Utilities.convertLeadTrail(horizontalAlignment, c);

        return doLayoutCompoundLabel(fm, text, icon, verticalAlignment, hPos,
            verticalTextPosition, hTextPos, viewR, iconR, textR, textIconGap);
    }

    public static String layoutCompoundLabel(final FontMetrics fm, final String text, final Icon icon,
            final int verticalAlignment, final int horizontalAlignment,
            final int verticalTextPosition, final int horizontalTextPosition,
            final Rectangle viewR, final Rectangle iconR, final Rectangle textR,
            final int textIconGap) {

        final int hTextPos = defaultLeadTrail(horizontalTextPosition, RIGHT);
        final int hPos = defaultLeadTrail(horizontalAlignment, CENTER);

        return doLayoutCompoundLabel(fm, text, icon, verticalAlignment, hPos,
            verticalTextPosition, hTextPos, viewR, iconR, textR, textIconGap);
    }

    public static MouseEvent convertMouseEvent(final Component source, final MouseEvent event, final Component destination) {
        Point convertedPoint = convertPoint(source, event.getPoint(), destination);
        return new MouseEvent((destination != null) ? destination : source,
                                                   event.getID(), event.getWhen(),
                                                   event.getModifiersEx(), convertedPoint.x, convertedPoint.y,
                                                   event.getClickCount(), event.isPopupTrigger(),
                                                   event.getButton());
    }

    public static Rectangle convertRectangle(final Component source, final Rectangle rect, final Component destination) {
        Rectangle convertedRect = new Rectangle(rect);
        convertedRect.setLocation(convertPoint(source, rect.x, rect.y, destination));
        return convertedRect;
    }

    public static Point convertPoint(final Component source, final Point point, final Component destination) {
        return (point != null) ? convertPoint(source, point.x, point.y, destination)
                               : null;
    }

    public static Point convertPoint(final Component source, final int x, final int y, final Component destination) {
        Point convertedPoint = new Point(x, y);
        if (source != null && destination != null) {
            convertPointToScreen(convertedPoint, source);
            convertPointFromScreen(convertedPoint, destination);
        } else {
            translateRelatedPoint(convertedPoint, source, 1, true);
            translateRelatedPoint(convertedPoint, destination, -1, true);
        }

        return convertedPoint;
    }

    public static void convertPointToScreen(final Point point, final Component component) {
        if (component == null) {
            throw new NullPointerException(Messages.getString("swing.61")); //$NON-NLS-1$
        }
        translateRelatedPoint(point, component, 1, false);
    }

    public static void convertPointFromScreen(final Point point, final Component component) {
        if (component == null) {
            throw new NullPointerException(Messages.getString("swing.62")); //$NON-NLS-1$
        }
        translateRelatedPoint(point, component, -1, false);
    }

    public static Rectangle[] computeDifference(final Rectangle rect1, final Rectangle rect2) {
        if (rect1 == null || rect1.isEmpty() || rect2 == null || rect2.isEmpty()) {
            return new Rectangle[0];
        }
        Rectangle isection = rect1.intersection(rect2);
        if (isection.isEmpty()) {
            return new Rectangle[0];
        }
        ArrayList reminders = new ArrayList(4);
        substract(rect1, isection, reminders);
        return (Rectangle[])reminders.toArray(new Rectangle[0]);
    }

    private static void substract(final Rectangle rect, final Rectangle isection, final ArrayList remainders) {
        int isectionRight = isection.x + isection.width;
        int rectRight = rect.x + rect.width;
        int isectionBottom = isection.y + isection.height;
        int rectBottom = rect.y + rect.height;

        if (isection.y > rect.y) {
            remainders.add(new Rectangle(rect.x, rect.y, rect.width, isection.y - rect.y));
        }
        if (isection.x > rect.x) {
            remainders.add(new Rectangle(rect.x, isection.y, isection.x - rect.x, isection.height));
        }
        if (isectionRight < rectRight) {
            remainders.add(new Rectangle(isectionRight, isection.y, rectRight - isectionRight, isection.height));
        }
        if (isectionBottom < rectBottom) {
            remainders.add(new Rectangle(rect.x, isectionBottom, rect.width, rectBottom - isectionBottom));
        }
    }

    public static Rectangle calculateInnerArea(final JComponent component, final Rectangle rect) {
        if (component == null) {
            return null;
        }

        Insets insets = component.getInsets();
        Rectangle bounds = component.getBounds(rect);
        bounds.setRect(insets.left, insets.top,
                       bounds.width - insets.right - insets.left,
                       bounds.height - insets.top - insets.bottom);
        return bounds;
    }

    public static final boolean isRectangleContainingRectangle(final Rectangle r1, final Rectangle r2) {
        return r1.contains(r2);
    }

    public static Rectangle computeUnion(final int x, final int y, final int width, final int height, final Rectangle rect) {
        auxRect.setBounds(x, y, width, height);
        Rectangle2D.union(auxRect, rect, rect);
        return rect;
    }

    public static Rectangle computeIntersection(final int x, final int y, final int width, final int height, final Rectangle rect) {
        auxRect.setBounds(x, y, width, height);
        Rectangle2D.intersect(auxRect, rect, rect);
        if (rect.height < 0 || rect.width < 0) {
            rect.setBounds(0, 0, 0, 0);
        }
        return rect;
    }

    public static Rectangle getLocalBounds(final Component component) {
        return new Rectangle(0, 0, component.getWidth(), component.getHeight());
    }


    public static Container getAncestorNamed(final String name, final Component component) {
        Component ancestor = null;
        if (component != null && name != null) {
            for(ancestor = Utilities.getNotWindowParent(component); !((ancestor == null) || name.equals(ancestor.getName()));
                ancestor = Utilities.getNotWindowParent(ancestor));
        }
        return (Container)ancestor;
    }

    public static Container getAncestorOfClass(final Class<?> wantedClass, final Component component) {
        if (component == null || wantedClass == null) {
            return null;
        }

        Component ancestor = null;
        for(ancestor = component.getParent(); !((ancestor == null) || (wantedClass.isAssignableFrom(ancestor.getClass())));
            ancestor = ancestor.getParent()) {
        }
        return (Container)ancestor;
    }

    public static JRootPane getRootPane(final Component component) {
        if (component == null) {
            return null;
        }
        if (component instanceof JRootPane) {
            return (JRootPane)component;
        }
        if (component instanceof RootPaneContainer) {
            return ((RootPaneContainer)component).getRootPane();
        }
        return (JRootPane)getAncestorOfClass(JRootPane.class, component);
    }

    public static Window windowForComponent(final Component component) {
        return (Window)getAncestorOfClass(Window.class, component);
    }

    public static Window getWindowAncestor(final Component component) {
        return windowForComponent(component);
    }

    public static boolean isDescendingFrom(final Component child, final Component parent) {
        if (parent == child) {
            return true;
        }

        for(Component ancestor = Utilities.getNotWindowParent(child); ancestor != null;
            ancestor = Utilities.getNotWindowParent(ancestor)) {

            if (ancestor == parent) {
                return true;
            }
        }

        return false;
    }


    public static void paintComponent(final Graphics graphics, final Component component, final Container container, final int x, final int y, final int width, final int height) {
        Container auxContainer = Utilities.getNotWindowParent(component);
        if (auxContainer instanceof CellRendererPane) {
            if (Utilities.getNotWindowParent(auxContainer) != container) {
                container.add(auxContainer);
            }
        } else {
            auxContainer = new CellRendererPane();
            container.add(auxContainer);
        }
        ((CellRendererPane)auxContainer).paintComponent(graphics, component, container,
                                                        x, y, width, height, false);
    }

    public static void paintComponent(final Graphics graphics, final Component component, final Container container, final Rectangle rect) {
        paintComponent(graphics, component, container, rect.x, rect.y, rect.width, rect.height);
    }


    private static InputMap getUIInputMapChild(final InputMap inputMap) {
        if (inputMap == null) {
            return null;
        }

        InputMap result = inputMap;
        InputMap parent = inputMap.getParent();
        while (parent != null && !(parent instanceof UIResource)) {
            result = parent;
            parent = result.getParent();
        }

        return result;
    }


    public static InputMap getUIInputMap(final JComponent component, final int condition) {
        InputMap inputMap = getUIInputMapChild(component.getInputMap(condition));
        return (inputMap != null) ? inputMap.getParent() : null;
    }

    public static void replaceUIInputMap(final JComponent component, final int condition, final InputMap uiInputMap) {
        InputMap inputMap = component.getInputMap(condition);

        if (inputMap == null) {
            component.setInputMap(condition, uiInputMap);
        } else {
            if (uiInputMap != null) {
                inputMap = getUIInputMapChild(inputMap);
                inputMap.setParent(uiInputMap);
            } else {
                while (inputMap != null) {
                    if (inputMap.getParent() instanceof UIResource) {
                        inputMap.setParent(inputMap.getParent().getParent());
                    } else {
                        inputMap = inputMap.getParent();
                    }
                }
            }
        }
    }

    private static ActionMap getUIActionMapChild(final ActionMap actionMap) {
        if (actionMap == null) {
            return null;
        }

        ActionMap result = actionMap;
        ActionMap parent = actionMap.getParent();
        while (parent != null && !(parent instanceof UIResource)) {
            result = parent;
            parent = result.getParent();
        }

        return result;
    }

    public static ActionMap getUIActionMap(final JComponent component) {
        ActionMap actionMap = getUIActionMapChild(component.getActionMap());
        return (actionMap != null) ? actionMap.getParent() : null;
    }

    public static void replaceUIActionMap(final JComponent component, final ActionMap uiActionMap) {
        ActionMap actionMap = component.getActionMap();

        if (actionMap == null) {
            component.setActionMap(uiActionMap);
        } else {
            if (uiActionMap != null) {
                actionMap = getUIActionMapChild(actionMap);
                actionMap.setParent(uiActionMap);
            } else {
                while (actionMap != null) {
                    if (actionMap.getParent() instanceof UIResource) {
                        actionMap.setParent(actionMap.getParent().getParent());
                    } else {
                        actionMap = actionMap.getParent();
                    }
                }
            }
        }
    }

    public static void updateComponentTreeUI(final Component component) {
        updateComponentTreeUILevel(component);

        component.invalidate();
        component.validate();
        component.repaint();
    }

    private static void updateComponentTreeUILevel(final Component parent) {
        if (parent instanceof JComponent) {
            ((JComponent)parent).updateUI();
        }
        Component[] children = null;
        if (parent instanceof JMenu) {
            children = ((JMenu)parent).getMenuComponents();
        } else if (parent instanceof Container) {
            children = ((Container)parent).getComponents();
        }
        if (children == null) {
            return;
        }
        for(int iChild = 0; iChild < children.length; iChild++) {
            updateComponentTreeUILevel(children[iChild]);
        }
    }

    public static int computeStringWidth(final FontMetrics metrics, final String str) {
        return metrics.stringWidth(str);
    }

    public static Component getDeepestComponentAt(final Component component, final int x, final int y) {
        if (!component.contains(x, y)) {
            return null;
        }

        Component parent = component;
        Component child = parent;
        while (child != null && child instanceof Container) {
            parent = child;
            child = parent.getComponentAt(x, y);
            if (child == parent) {
                break;
            }
        }

        return (child != null) ? child : parent;
    }

    public static Component getRoot(final Component component) {
        Component result = getWindowOrAppletAncestor(component);
        Component topRoot = result;
        while (topRoot instanceof Applet) {
            result = topRoot;
            topRoot = getWindowOrAppletAncestor(topRoot.getParent());
        }
        return result;
    }

    @Deprecated
    public static Component findFocusOwner(final Component component) {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    }

    public static void invokeLater(final Runnable r) {
        EventQueue.invokeLater(r);
    }

    public static void invokeAndWait(final Runnable r) throws InterruptedException, InvocationTargetException {
        EventQueue.invokeAndWait(r);
    }

    public static boolean isEventDispatchThread() {
        return EventQueue.isDispatchThread();
    }

    public static boolean isRightMouseButton(final MouseEvent event) {
        return isMouseButtonDown(event, InputEvent.BUTTON3_DOWN_MASK, MouseEvent.BUTTON3);
    }

    public static boolean isMiddleMouseButton(final MouseEvent event) {
        return isMouseButtonDown(event, InputEvent.BUTTON2_DOWN_MASK, MouseEvent.BUTTON2);
    }

    public static boolean isLeftMouseButton(final MouseEvent event) {
        return isMouseButtonDown(event, InputEvent.BUTTON1_DOWN_MASK, MouseEvent.BUTTON1);
    }

    public static boolean processKeyBindings(final KeyEvent event) {
        if (event == null || event.isConsumed()) {
            return false;
        }

        Component source = event.getComponent();
        return JComponent.processKeyBindings(event, source);
    }

    public static boolean notifyAction(final Action action, final KeyStroke keyStroke, final KeyEvent keyEvent, final Object sender, final int modifiers) {
        if (action == null || !action.isEnabled()) {
            return false;
        }
        Object command = action.getValue(Action.ACTION_COMMAND_KEY);
        if (command == null && !(action instanceof ActionProxy)) {
            char keyChar = keyEvent.getKeyChar();
            if (keyChar != KeyEvent.CHAR_UNDEFINED) {
                command = String.valueOf(keyChar);
            }
        }

        action.actionPerformed(new ActionEvent(sender, ActionEvent.ACTION_PERFORMED, (String)command, keyEvent.getWhen(), modifiers));

        return true;
    }

    static boolean processKeyEventOnChildren(final Container parent, final KeyEvent event) {
        for (int iChild = 0; iChild < parent.getComponentCount(); iChild++) {
            final Component child = parent.getComponent(iChild);
            if (child instanceof AbstractButton) {
                if (((AbstractButton)child).processMnemonics(event)) {
                    return true;
                }
            }
            if (processKeyEventOnComponent(child, event)) {
                return true;
            }
            if (child instanceof Container) {
                if (processKeyEventOnChildren((Container)child, event)) {
                    return true;
                }
            }
        }

        return false;
    }

    static boolean processKeyEventOnComponent(final Component target, final KeyEvent event) {
        if (target instanceof JComponent) {
            final boolean pressed = (event.getID() == KeyEvent.KEY_PRESSED);
            final KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(event);
            if (((JComponent)target).processKeyBinding(keyStroke, event, JComponent.WHEN_IN_FOCUSED_WINDOW, pressed)) {
                return true;
            }
        }

        return false;
    }

    public static Accessible getAccessibleAt(final Component component, final Point point) {
        Accessible result = null;

        if (component == null) {
            return null;
        }
        AccessibleContext context = component.getAccessibleContext();
        if (context == null) {
            return null;
        }
        AccessibleComponent accessibleComponent = context.getAccessibleComponent();
        if (accessibleComponent == null) {
            return null;
        }

        return accessibleComponent.getAccessibleAt(point);
    }

    public static AccessibleStateSet getAccessibleStateSet(final Component component) {
        AccessibleContext context = component.getAccessibleContext();
        if (context == null) {
            return null;
        }

        return context.getAccessibleStateSet();
    }

    public static Accessible getAccessibleChild(final Component component, final int index) {
        AccessibleContext context = component.getAccessibleContext();
        if (context == null) {
            return null;
        }

        return context.getAccessibleChild(index);
    }

    public static int getAccessibleIndexInParent(final Component component) {
        return component.getAccessibleContext().getAccessibleIndexInParent();
    }

    public static int getAccessibleChildrenCount(final Component component) {
        return component.getAccessibleContext().getAccessibleChildrenCount();
    }

    /**
     * method that actually implements functionality of both public
     * layoutCompoundLabel() methods
     */
    private static String doLayoutCompoundLabel(final FontMetrics fm, final String text, final Icon icon,
            final int verticalAlignment, final int horizontalAlignment,
            final int verticalTextPosition, final int horizontalTextPosition,
            final Rectangle viewR, final Rectangle iconR, final Rectangle textR,
            final int textIconGap) {

        final int gap = icon != null && !Utilities.isEmptyString(text) ? textIconGap : 0;

        if (icon != null) {
            iconR.setSize(icon.getIconWidth(), icon.getIconHeight());
        } else {
            iconR.setSize(0, 0);
        }

        String clippedText = "";
        if (!Utilities.isEmptyString(text)) {
            final int adjust = horizontalTextPosition != CENTER ? iconR.width + gap : 0;
            final int availableLength = viewR.width - adjust;
            clippedText = Utilities.clipString(fm, text, availableLength);
            textR.setSize(Utilities.getStringSize(clippedText, fm));
        } else {
            textR.setSize(0, 0);
        }

        layoutRects(verticalAlignment, horizontalAlignment,
                verticalTextPosition, horizontalTextPosition, viewR, iconR,
                textR, gap);

        return clippedText;
    }

    private static void layoutRects(final int verticalAlignment, final int horizontalAlignment,
            final int vTextPos, final int hTextPos,
            final Rectangle viewR, final Rectangle iconR, final Rectangle textR,
            final int gap) {

        boolean horizontal = hTextPos != CENTER;
        boolean vertical = vTextPos != CENTER;

        int hIconPos = hTextPos;
        int vIconPos = vTextPos;
        int width = Math.max(iconR.width, textR.width);
        int height = Math.max(iconR.height, textR.height);

        if (horizontal) {
            hIconPos = hTextPos != LEFT ? LEFT : RIGHT;
            width = iconR.width + textR.width + gap;
        } else if (vertical) {
            vIconPos = vTextPos != TOP ? TOP : BOTTOM;
            height = iconR.height + textR.height + gap;
        }

        Rectangle labelR = new Rectangle(width, height);

        Utilities.alignRect(labelR, viewR, horizontalAlignment, verticalAlignment);
        Utilities.alignRect(textR, labelR, hTextPos, vTextPos);
        Utilities.alignRect(iconR, labelR, hIconPos, vIconPos);
    }

    private static int defaultLeadTrail(final int anchor, final int value) {
        if (anchor == LEADING || anchor == TRAILING) {
            return value;
        }
        return anchor;
    }

    private static Component getWindowOrAppletAncestor(final Component component) {
        Component result = component;
        while (result != null && !(result instanceof Window || result instanceof Applet)) {
            result = Utilities.getNotWindowParent(result);
        }
        return result;
    }

    private static void translateRelatedPoint(final Point point, final Component c, final int direction,
            final boolean stopAtRootPane) {
        Component currentComponent = c;
        while ((currentComponent != null) &&
                (!stopAtRootPane || stopAtRootPane && !(currentComponent instanceof JRootPane))) {
            Point componentLocation = currentComponent.getLocation();
            point.x += direction * componentLocation.x;
            point.y += direction * componentLocation.y;
            currentComponent = Utilities.getNotWindowParent(currentComponent);
        }
    }

    private static boolean isMouseButtonDown(final MouseEvent e, final int downMask, final int button) {
        return ((e.getModifiersEx() & downMask) != 0)
                || ((e.getID() == MouseEvent.MOUSE_PRESSED
                     || e.getID() == MouseEvent.MOUSE_RELEASED
                     || e.getID() == MouseEvent.MOUSE_CLICKED)
                    && e.getButton() == button);
    }

}
