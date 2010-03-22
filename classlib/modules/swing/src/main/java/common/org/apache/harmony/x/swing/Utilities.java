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
 * @author Alexander T. Simbirtsev, Anton Avtamonov
 */
package org.apache.harmony.x.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.MenuElement;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.text.Position;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * SwingUtilities extension. This class provides utility
 * methods which are widely used in Swing classes.
 *
 */
public class Utilities implements SwingConstants {
    /**
     * This interface allows to access list data
     */
    public interface ListModelAccessor {
        /**
         * Returns the list data element according to specified index
         * 
         * @param index of the element
         * @return element, specified by index
         */
        public Object getElementAt(final int index);
        
        /**
         * Returns the size of the list
         * @return size of the list
         */
        public int getSize();
    }
    
    /**
     * Returns the index of the next element of the list according to specified
     * prefix, start index and bias
     * 
     * @param model of the list
     * @param prefix of the list element
     * @param startIndex index to start search from
     * @param bias
     * @return index of the next element
     */
    public static int getNextMatch(final ListModelAccessor model, final String prefix, 
                            final int startIndex, final Position.Bias bias) {
        if (prefix == null) {
            throw new IllegalArgumentException(Messages.getString("swing.6F")); //$NON-NLS-1$
        }

        if (startIndex < 0 || startIndex >= model.getSize()) {
            throw new IllegalArgumentException(Messages.getString("swing.6D")); //$NON-NLS-1$
        }

        String ucPrefix = prefix.toUpperCase();
        if (Position.Bias.Forward == bias) {
            for (int i = startIndex; i < model.getSize(); i++) {
                String elementAsString = model.getElementAt(i).toString().toUpperCase();
                if (elementAsString.startsWith(ucPrefix)) {
                    return i;
                }
            }
            for (int i = 0; i < startIndex; i++) {
                String elementAsString = model.getElementAt(i).toString().toUpperCase();
                if (elementAsString.startsWith(ucPrefix)) {
                    return i;
                }
            }
        } else if (Position.Bias.Backward == bias) {
            for (int i = startIndex; i >= 0; i--) {
                String elementAsString = model.getElementAt(i).toString().toUpperCase();
                if (elementAsString.startsWith(ucPrefix)) {
                    return i;
                }
            }
            for (int i = model.getSize() - 1; i > startIndex; i--) {
                String elementAsString = model.getElementAt(i).toString().toUpperCase();
                if (elementAsString.startsWith(ucPrefix)) {
                    return i;
                }
            }
        }

        return -1;
    }

    /**
    * Clips string due to the width of an area available for painting the text.
    *
    * @param fm FontMetrics for the text font
    * @param text original (not clipped) text
    * @param width width of the area available for painting the text
    * @return clipped string (ending with "...") or the original string if it fits the available width
    */
    public static String clipString(final FontMetrics fm, final String text, final int width) {
        final String dots = "...";
        final int stringWidth = fm.stringWidth(text);
        if (width >= stringWidth) {
            return text;
        }

        final int dotsWidth = fm.stringWidth(dots);
        if (width <= dotsWidth) {
            return dots;
        }

        return text.substring(0, getSuitableSubstringLength(fm, text, width - dotsWidth)) + dots;
    }

    /**
     * Returns given index if underscored part of the string isn't clipped,
     * <code>-1</code>  otherwise.
     *
     * @param original original text
     * @param clipped clipped text
     * @param underscoreIndex index of the character in <code>text</code> to be
     *  underscored
     * @return underscoreIndex or <code>-1</code> if the position is clipped
     */
    public static int getClippedUnderscoreIndex(final String original, final String clipped, final int underscoreIndex) {
        return insideString(clipped, underscoreIndex)
                && original.charAt(underscoreIndex) == clipped.charAt(underscoreIndex) ? underscoreIndex : -1;
    }

    /**
     * Draws part of 3D-like rectangle with given parameters.
     *
     * @see java.awt.Graphics2D#draw3DRect(int, int, int, int, boolean)
     * @param shadow color for dark parts of the rectangle
     * @param highlight color for light parts of the rectangle
     * @param raised if <code>true</code> left and top sides of rectangle will be drawn light
     */
    public static void draw3DRect(final Graphics g, final int x, final int y, final int w, final int h, final Color shadow, final Color highlight, final boolean raised) {
        final Color oldColor = g.getColor();
        final int bottom = y + h - 1;
        final int right = x + w - 1;
        final Color topLeft = raised ? highlight : shadow;
        final Color bottomRight = raised ? shadow : highlight;
        g.setColor(topLeft);
        g.drawLine(x, y, x, bottom);
        g.drawLine(x, y, right, y);
        g.setColor(bottomRight);
        g.drawLine(right, y, right, bottom);
        g.drawLine(x, bottom, right, bottom);
        g.setColor(oldColor);
    }

    /**
     * Converts keyCode to mnemonic keyChar if possible. Note there is no 1 to 1 conversion.
     * @param keyCode KeyCode to be converted to a char
     * @return char or {@link java.awt.event.KeyEvent#VK_UNDEFINED} if conversion could not be done.
     */
    public static char keyCodeToKeyChar(final int keyCode) {
        if (keyCode < 0 || keyCode > 0xFFFF) {
            return KeyEvent.VK_UNDEFINED;
        }

        return Character.toUpperCase((char)keyCode);
    }

    /**
     * Converts mnemonic keyChar to keyCode.
     *
     * @param keyChar char to be converted to keyCode
     * @return converted keyCode or the <code>int</code> value of <code>keyChar</code>.
     */
    public static int keyCharToKeyCode(final char keyChar) {
        if (keyChar > 127) {
            return keyChar;
        }

        return Character.toUpperCase(keyChar);
    }

    /**
     * Returns optimal location to display popup based on the anchor bounds
     * requested, popup size and location of popup related to anchor. The optimal
     * location fulfills the condition popup fits on screen, not overlaps with
     * anchor and is verticaly or horizontaly positioned relative to anchor. If
     * position is vertical the popup left or right bound is aligned with
     * anchor bound depending on <code>leftToRight</code>.
     *
     * @param anchor anchor bounds relative to
     * @param size requested popup size
     * @param leftToRight horizontal alignment
     * @param horizontal placement relative to anchor
     * @return optimal popup location
     */
    public static Point getPopupLocation(final Rectangle anchor, final Dimension size, final boolean leftToRight, final boolean horizontal, final GraphicsConfiguration gc) {
        Point result = getPopupLocation(anchor, size, leftToRight, horizontal);
        return adjustPopupLocation(result, size, anchor, horizontal, gc);
    }

    /**
     * Gets popup location basing on the invoker position.
     *
     * @param anchor Rectangle which specified the bounds of the popup invoker
     * @param size Dimension of the popup size
     * @param leftToRight boolean value representing ComponentOrientation.isLeftToRight() state
     * @param horizontal boolean value representing ComponentOrientation.isHorizontal() state
     * @return Popup location
     */
    public static Point getPopupLocation(final Rectangle anchor, final Dimension size, final boolean leftToRight, final boolean horizontal) {
        Point result = anchor.getLocation();
        if (horizontal) {
            if (leftToRight) {
                result.x += anchor.width;
            } else {
                result.x -= size.width;
            }
        } else {
            result.y += anchor.height;
            if (!leftToRight) {
                result.x += anchor.width - size.width;
            }
        }

        return result;
    }

    /**
     * Determizes the 'optimal' popup location to provide the entire popup is shown.
     *
     * @param location Point of the proposed location
     * @param size Dimension of the popup size
     * @param anchor Rectangle which specified the bounds of the popup invoker
     * @param horizontal boolean value representing ComponentOrientation.isHorizontal() state
     * @param gc GraphicsConfiguration of the parent Window
     *
     * @return Optimal popup location
     */
    public static Point adjustPopupLocation(final Point location, final Dimension size, final Rectangle anchor, final boolean horizontal, final GraphicsConfiguration gc) {
        final Point result = location;
        final Rectangle screenBounds = getScreenClientBounds(gc);
        if (screenBounds.contains(new Rectangle(result, size))) {
            return result;
        }
        if (horizontal) {
            if (screenBounds.width < result.x + size.width) {
                result.x = anchor.x - size.width;
            }
            if (screenBounds.x > result.x) {
                result.x = anchor.x + anchor.width;
            }
            if (screenBounds.height < result.y + size.height) {
                result.y = anchor.y + anchor.height - size.height;
            }
            if (screenBounds.y > result.y + size.height) {
                result.y = screenBounds.y + 20;
            }
        } else {
            if (screenBounds.height < result.y + size.height) {
                result.y = anchor.y - size.height;
            }
            if (screenBounds.width < result.x + size.width) {
                result.x = screenBounds.width - size.width - 20;
            }
            if (screenBounds.x > result.x) {
                result.x = screenBounds.x + 20;
            }
            if (screenBounds.y > result.y + size.height) {
                result.y = screenBounds.y + 20;
            }
        }

        return result;
    }

    /**
     * If the window is <code>Frame</code> or <code>Dialog</code>, the function
     * returns <code>true</code> the window is resizable. Otherwise
     * <code>false</code> is returned. This function can be used
     * when implementing L&F for <code>JRootPane</code>.
     *
     * @param window the window to determine if it is resizable
     *
     * @return <code>true</code> if the window is <code>Frame</code> or
     *         <code>Dialog</code> and is resizable, otherwise
     *         <code>false</code> is returned
     */
    public static final boolean isResizableWindow(final Window window) {
        if (window instanceof Frame) {
            return ((Frame)window).isResizable();
        } else if (window instanceof Dialog) {
            return ((Dialog)window).isResizable();
        }
        return false;
    }

    /**
     * Returns <code>true</code> if the parameter is a maximized frame.
     * This function can be used when implementing L&F for
     * <code>JRootPane</code>.
     *
     * @param window the window to determine if it is maximized
     *
     * @return <code>true</code> if the window is <code>Frame</code>
     *         and it is maximized
     */
    public static final boolean isMaximumFrame(final Window window) {
        return window instanceof Frame &&
                (((Frame)window).getExtendedState() & Frame.MAXIMIZED_BOTH) != 0;
    }

    /**
     * This function implements evaluation of maximum/minimum/preferred sizes
     * of <code>JRootPane</code>. Depending on the size to be evaluated,
     * the parameters are maximum/minimum/preferred sizes of subcomponents.
     *
     * @param contentPaneSize the maximum/minimum/preferred size of
     *        <code>contentPane</code>
     * @param menuBarSize the maximum/minimum/preferred size of
     *        <code>menuBar</code>; may be <code>null</code>
     * @param titlePaneSize the maximum/minimum/preferred size of
     *        <code>titlePane</code>; may be <code>null</code>
     * @param insets the insets of <code>JRootPane</code>
     *
     * @return the maximum/minimum/preferred size of <code>JRootPane</code>
     */
    public static final Dimension getRootPaneLayoutSize(
            final Dimension contentPaneSize,
            final Dimension menuBarSize,
            final Dimension titlePaneSize,
            final Insets insets) {

        Dimension result = new Dimension(contentPaneSize);

        if (menuBarSize != null) {
            result.height += menuBarSize.height;
            result.width = Math.max(result.width, menuBarSize.width);
        }

        if (titlePaneSize != null) {
            result.height += titlePaneSize.height;
            result.width = Math.max(result.width, titlePaneSize.width);
        }

        return addInsets(result, insets);
    }

    /**
    * Removes colors and font installed by <code>LookAndFeel</code>.
    *
    * @see javax.swing.LookAndFeel#installColorsAndFont(JComponent, String, String, String)
    * @param comp Component for which UI colors and font should be uninstalled
    */
    public static final void uninstallColorsAndFont(final JComponent comp) {
        if (comp.getBackground() instanceof UIResource) {
            comp.setBackground(null);
        }
        if (comp.getForeground() instanceof UIResource) {
            comp.setForeground(null);
        }
        if (comp.getFont() instanceof UIResource) {
            comp.setFont(null);
        }
    }

    /**
     * Enlarges <code>size</code> by insets size. <code>size</code> fields
     * are changed.
     *
     * @param size initial dimension
     * @param insets insets to add
     * @return Enlarged dimension
     */
    public static final Dimension addInsets(final Dimension size, final Insets insets) {
        size.setSize(size.width + insets.left + insets.right,
                     size.height + insets.top + insets.bottom);
        return size;
    }

    /**
     * Reduces width and height of <code>rect</code> by insets
     * and moves its origin. The fields of <code>insets</code>
     * are not changed.
     *
     * @param rect initial rectangle, its fields will be modified
     * @param insets insets to subract
     * @return Area inside the insets
     */
    public static final Rectangle subtractInsets(final Rectangle rect,
                                                 final Insets insets) {
        if (insets == null) {
            return rect;
        }
        rect.setBounds(rect.x + insets.left,
                       rect.y + insets.top,
                       rect.width - insets.left - insets.right,
                       rect.height - insets.top - insets.bottom);
        return rect;
    }

    /**
     * Adds values from second argument to the first one and returns first argument.
     *
     * @param recipient Insets to be enlarged
     * @param addition Instes to be added to the recipient
     *
     * @return Recipient
     */
    public static final Insets addInsets(final Insets recipient, final Insets addition) {
        if (addition == null) {
            return recipient;
        }
        recipient.set(recipient.top + addition.top,
                      recipient.left + addition.left,
                      recipient.bottom + addition.bottom,
                      recipient.right + addition.right);

        return recipient;
    }


    /**
     * Returns string size due to the given font metrics.
     *
     * @param str String which size should be calculated
     * @param fm FontMetrics of the measuring String
     * @return String size
     */
    public static Dimension getStringSize(final String str, final FontMetrics fm) {
        return !isEmptyString(str) ? new Dimension(fm.stringWidth(str), fm.getHeight()) : new Dimension();
    }

    /**
     * Draws string with given font and color.
     *
     * @param g Graphics to draw on
     * @param str String to draw
     * @param x int representing text x-coordinate
     * @param y int representing text y-coordinate
     * @param fm FontMetrics of the text to draw
     * @param c Color to draw with
     * @param underscoreIndex int value representing underscore index to be underlined or -1 if
     *        no underlining is required
     */
    public static void drawString(final Graphics g, final String str,
                                  final int x, final int y, final FontMetrics fm,
                                  final Color c, final int underscoreIndex) {
        if (isEmptyString(str)) {
            return;
        }
        final Color oldColor = g.getColor();
        final Font oldFont = g.getFont();
        g.setColor(c);
        g.setFont(fm.getFont());
        BasicGraphicsUtils.drawStringUnderlineCharAt(g, str, underscoreIndex, x, y);
        g.setColor(oldColor);
        g.setFont(oldFont);
    }

    /**
     * Calculates the baseline for text being rendered on compound label
     * based on the FontMetrics and bounding rectangle <code>y</code> coordinate.
     *
     * @param fm FontMetrics of the text
     * @param textR Rectangle representing text bounds
     *
     * @return Y-coordinate to draw text from
     */
    public static int getTextY(final FontMetrics fm, final Rectangle textR) {
        return textR.y + fm.getAscent();
    }

    /**
     * Calculates size for the label that consists of a text message and an icon.
     *
     * @see javax.swing.SwingUtilities#layoutCompoundLabel(FontMetrics, String, Icon, int, int, int, int, Rectangle, Rectangle, Rectangle, int)
     * @return Preferred size
     */
    public static Dimension getCompoundLabelSize(final JComponent c,
            final String text, final Icon icon,
            final int verticalTextPosition, final int horizontalTextPosition,
            final int iconTextGap) {

        final Dimension result = new Dimension();
        final FontMetrics fm = getFontMetrics(c);
        if (fm == null) {
            return result;
        }
        Rectangle viewR = new Rectangle(Short.MAX_VALUE, Short.MAX_VALUE);
        Rectangle iconR = new Rectangle();
        Rectangle textR = new Rectangle();

        SwingUtilities.layoutCompoundLabel(fm, text, icon, TOP, LEFT,
                                           verticalTextPosition,
                                           horizontalTextPosition,
                                           viewR, iconR, textR, iconTextGap);
        result.width = Math.max(iconR.x + iconR.width, textR.x + textR.width);
        result.height = Math.max(iconR.y + iconR.height, textR.y + textR.height);

        return result;
    }

    /**
     * Checks whether a string is empty, null is considered to be empty string.
     *
     * @param str string to check
     * @return <code>true</code>, if <code>str</code> is <code>null</code>
     *          or is empty.
     */
    public static boolean isEmptyString(final String str) {
        return str == null || str.length() == 0;
    }

    /**
     * Gets component FontMetrics.
     *
     * @param c Component which FontMetrics requested
     * @return Component FontMetrics correspondent to current components font
     */
    public static FontMetrics getFontMetrics(final JComponent c) {
        final Font font = c.getFont();
        return font != null ? c.getFontMetrics(font) : null;
    }

    /**
     * Checks whether an array is empty, null is considered to be empty array.
     *
     * @param arr array to check
     * @return <code>true</code>, if <code>array</code> is <code>null</code>
     *          or contains 0 elements.
     */
    public static boolean isEmptyArray(final Object[] arr) {
        return arr == null || arr.length == 0;
    }

    /**
     * Checks whether an array is empty, null is considered to be empty array.
     *
     * @param arr array to check
     * @return <code>true</code>, if <code>array</code> is <code>null</code>
     *          or contains 0 elements.
     */
    public static boolean isEmptyArray(final int[] arr) {
        return arr == null || arr.length == 0;
    }

    /**
     * Aligns rectangle with the inner bounds of the box taking into account
     * alignments given.
     *
     * @param rect rectangle to align
     * @param box box to align with
     * @param horizontalAlign one of LEFT/RIGHT/CENTER
     * @param verticalAlign one of TOP/BOTTOM/CENTER
     */
    public static void alignRect(final Rectangle rect, final Rectangle box,
            final int horizontalAlign, final int verticalAlign) {
        rect.y = verticallyAlignRect(rect.height, box, verticalAlign);
        rect.x = horizontallyAlignRect(rect.width, box, horizontalAlign);
    }

    /**
     * Installs UI input map to the given component
     * also installs RTL input map if available and needed.
     *
     * @param c component input map should be installed to
     * @param condition condition for the input map
     * @param inputMapKey key of the input map record in the defaults table.
     * @param rtlInputMapKey key of the right-to-left input map record in the defaults table.
     */
    public static void installKeyboardActions(final JComponent c, final int condition, final String inputMapKey, final String rtlInputMapKey) {
        InputMap uiInputMap = (InputMap)UIManager.get(inputMapKey);
        if (rtlInputMapKey != null && !c.getComponentOrientation().isLeftToRight()) {
            final InputMap rtlInputMap = (InputMap)UIManager.get(rtlInputMapKey);
            if (rtlInputMap != null) {
                rtlInputMap.setParent(uiInputMap);
                uiInputMap = rtlInputMap;
            }
        }
        SwingUtilities.replaceUIInputMap(c, condition, uiInputMap);
    }

    /**
     * Uninstalls UI InputMap from the component.
     *
     * @param c component from which UI input map to be removed
     * @param condition condition for which input map should be removed.
     */
    public static void uninstallKeyboardActions(final JComponent c, final int condition) {
        SwingUtilities.replaceUIInputMap(c, condition, null);
    }

    /**
     * Checks if object is installed by UI or <code>null</code>.
     * @param obj Object to be checked if it is an instance of UIResource or not.
     *
     * @return true if the obj instance of UIResource or null, false otherwise
     */
    public static boolean isUIResource(final Object obj) {
        return (obj == null) || (obj instanceof UIResource);
    }

    /**
     * Draws triangle-like arrow (for scrollbars).
     *
     * @param g Grpahics to draw on
     * @param x x-coordinate of the bounding rectangle
     * @param y y-coordinate of the bounding rectangle
     * @param direction of the NORTH, SOUTH, WEST, EAST
     * @param width int representing size of the bounding rectangle
     * @param wide boolean determining if the arrow basis should be wide
     * @param color Color to draw the arrow with
     */
    public static void paintArrow(final Graphics g, final int x, final int y,
                                  final int direction, final int width,
                                  final boolean wide, final Color color) {
        paintArrow(g, x, y, direction, width, wide, color, false);
    }

    /**
     * Draws filled triangle-like arrow (for scrollbars).
     *
     * @param g Grpahics to draw on
     * @param x x-coordinate of the bounding rectangle
     * @param y y-coordinate of the bounding rectangle
     * @param direction of the NORTH, SOUTH, WEST, EAST
     * @param width int representing size of the bounding rectangle
     * @param wide boolean determining if the arrow basis should be wide
     * @param color Color to fill the arrow with
     */
    public static void fillArrow(final Graphics g, final int x, final int y,
                                 final int direction, final int width,
                                 final boolean wide, final Color color) {
        paintArrow(g, x, y, direction, width, wide, color, true);
    }

    /**
     * Returns mouse pointer location in the screen coordinates.
     * If <code>GraphicsEnvironment.isHeadless()</code> returns
     * <code>true</code> or there is no mouse, <code>null</code> is returned.
     * This function should be used to determine mouse displacement
     * when <code>JFrame</code> is dragged or resized with mouse.
     *
     * @return Point of the current mouse pointer location in the screen coordinates
     * @see java.awt.MouseInfo#getPointerInfo()
     */
    public static Point getMousePointerScreenLocation() {
        Point p = null;
        try {
            p = AccessController.doPrivileged(new PrivilegedAction<Point>() {
                public Point run() {
                    return MouseInfo.getPointerInfo().getLocation();
                }
            });
        } catch (final HeadlessException ex) {
        }

        return p;
    }

    /**
     * Returns parent of <code>c</code> if <code>c</code> is not <code>Window</code> decsendant,
     *  <code>null</code> otherwise.
     *
     *  @param c Component which parent is requested
     *  @return Container where c is located or null if specified component is Window
     */
    public static Container getNotWindowParent(final Component c) {
        return !(c instanceof Window) ? c.getParent() : null;
    }

    /**
     * Converts LEADING/TRAILING to LEFT/RIGHT regarding to component orientation.
     * Passes by any other values.
     *
     * @param value to convert
     * @param c component where value is to be applied
     * @return converted value
     */
    public static int convertLeadTrail(final int value, final Component c) {
        final boolean isLTR = (c == null) || c.getComponentOrientation().isLeftToRight();
        if (value == LEADING) {
            return isLTR ? LEFT : RIGHT;
        }
        if (value == TRAILING) {
            return isLTR ? RIGHT : LEFT;
        }
        return value;
    }

    /**
     * Generates accelarator displayed text.
     *
     * @param acc KeyStroke of the accelerator
     * @param delimiter String representation the delimiter of the key in a key sequence
     * @return Accelerator displayed text
     */
    public static String getAcceleratorText(final KeyStroke acc, final String delimiter) {
        if (acc == null) {
            return null;
        }

        String text = InputEvent.getModifiersExText(acc.getModifiers());
        if (!delimiter.equals("+")) {
            text = text.replaceAll("[+]", delimiter);
        }
        if (text.length() > 0) {
            text += delimiter;
        }
        text += acc.getKeyCode() != KeyEvent.VK_UNDEFINED ?
                KeyEvent.getKeyText(acc.getKeyCode()) : "" + acc.getKeyChar();
        return text;
    }

    /**
     * Checks if given key is appropriate constant for vertical alignment, throws IAE otherwise.
     *
     * @param key value to check
     * @param exceptionText text of exception message
     * @return given key
     * @throws IllegalArgumentException with the specified exceptionText if the key is not valid
     */
    public static int checkVerticalKey(final int key, final String exceptionText) {
        switch (key) {
        case TOP:
        case CENTER:
        case BOTTOM:
            return key;
        default:
            throw new IllegalArgumentException(exceptionText);
        }
    }

    /**
     * Checks if given key is appropriate constant for horizontal alignment, throws IAE otherwise.
     *
     * @param key value to check
     * @param exceptionText text of exception message
     * @return given key
     * @throws IllegalArgumentException with the specified exceptionText if the key is not valid
     */
    public static int checkHorizontalKey(final int key, final String exceptionText) {
        switch (key) {
        case RIGHT:
        case LEFT:
        case CENTER:
        case LEADING:
        case TRAILING:
            return key;
        default:
            throw new IllegalArgumentException(exceptionText);
        }
    }

    /**
     * Calculates mnemonic index in the text for the given keyChar.
     *
     * @param text text in which mnemonic index should be found
     * @param mnemonicChar mnemonic char
     * @return index in the text or -1 if there is no index for the specified mnemonic
     */
    public static int getDisplayedMnemonicIndex(final String text, final char mnemonicChar) {
        return text != null ? text.toUpperCase().indexOf(Character.toUpperCase(mnemonicChar)) : -1;
    }


    /**
     * Returns components that implements MenuElement interface.
     *
     * @param menu container among whose components MenuElements will be looked for
     * @return MenuElements array
     */
    public static MenuElement[] getSubElements(final Container menu) {
        final Component[] components = menu.getComponents();
        final ArrayList<MenuElement> result = new ArrayList<MenuElement>(components.length);
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof MenuElement) {
                result.add((MenuElement)components[i]);
            }
        }

        return result.toArray(new MenuElement[result.size()]);
    }

    /**
     * Returns if <code>element</code> is the subElement of <code>container</code>.
     *
     * @param container menu to look for sub element at
     * @param element element to check
     * @return <code>true</code> if <code>element</code> is the subElement of <code>container</code>,
     * <code>false</code> otherwise
     */
    public static boolean isMenuSubElement(final MenuElement container, final MenuElement element) {
        final MenuElement[] subElements = container.getSubElements();
        for (int i = 0; i < subElements.length; i++) {
            if (subElements[i] == element) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns menu path of given menuElement up to the <code>JMenuBar</code>.
     *
     * @param element MenuElement which path is requested
     * @return MenuElement[] representing menu path for the specified element
     */
    public static MenuElement[] getMenuElementPath(final MenuElement element) {
        if (!(element instanceof Container)) {
            return new MenuElement[0];
        }
        final ArrayList<MenuElement> hierarchy = new ArrayList<MenuElement>();
        Container c = (Container)element;
        do {
            hierarchy.add(0, (MenuElement)c);
            if (c instanceof JMenuBar) {
                break;
            }
            if (c instanceof JPopupMenu) {
                c = (Container)((JPopupMenu)c).getInvoker();
            } else {
                c = c.getParent();
            }
        } while (c != null && c instanceof MenuElement);

        return hierarchy.toArray(new MenuElement[hierarchy.size()]);
    }

    /**
     * Adds item to the corresponding place in the menu selection path.
     * Trims path if necessary.
     *
     * @param path old path
     * @param item element being added
     * @return new path including new element
     */
    public static MenuElement[] addToPath(final MenuElement[] path, final MenuElement item) {
        int commonPathLength;
        for (commonPathLength = 0; commonPathLength < path.length; commonPathLength++) {
            if (Utilities.isMenuSubElement(path[commonPathLength], item)) {
                commonPathLength++;
                break;
            }
        }
        final MenuElement[] result = new MenuElement[commonPathLength + 1];
        System.arraycopy(path, 0, result, 0, commonPathLength);
        result[result.length - 1] = item;
        return result;
    }

    /**
     * Gets first visible and enabled item from the list of MenuElements.
     *
     * @param children the list of elements to select from
     * @return visible and enabled element if any, null otherwise
     */
    public static MenuElement getFirstSelectableItem(final MenuElement[] children) {
        if (isEmptyArray(children)) {
            return null;
        }

        for (int i = 0; i < children.length; i++) {
            Component component = children[i].getComponent();
            if (component == null && children[i] instanceof Component) {
                component = (Component)children[i];
            }
            if (component != null && component.isVisible() && component.isEnabled()) {
                return children[i];
            }
        }

        return null;
    }

    /**
     * Checks if the given item is the valid menu path root.
     *
     * @param item MenuItem to be checked
     * @return true if the specified item is valid menu path root, false otherwise
     */
    public static boolean isValidFirstPathElement(final MenuElement item) {
        return (item instanceof JMenuBar) || ((item instanceof JPopupMenu) && !(item instanceof BasicComboPopup));
    }

    /**
     * Removes element from menu selection path.
     * Trims path if necessary.
     *
     * @param path old path
     * @param item element being removed
     * @return updated path
     */
    public static MenuElement[] removeFromPath(final MenuElement[] path, final MenuElement item) {
        if (isEmptyArray(path)) {
            return new MenuElement[0];
        }
        int lastSurvivor = path.length - 1;
        for (int i = path.length - 1; i >= 0; i--) {
            if (path[i] == item) {
                lastSurvivor = i - 1;
                break;
            }
        }
        final MenuElement[] result = new MenuElement[lastSurvivor + 1];
        System.arraycopy(path, 0, result, 0, result.length);
        return result;
    }

    /**
     * Returns value that lies between given bounds.
     *
     * @param x given value
     * @param min bottom bound for x
     * @param max top bound for x
     * @return min if x less than min, max if x larger than max, x otherwise
     */
    public static int range(final int x, final int min, final int max) {
        return Math.max(min, Math.min(x, max));
    }

    /**
     * Returns sum of two integers. This function prevents overflow, and
     * if the result were greater than Integer.MAX_VALUE, the maximum
     * integer would be returned.
     * <p><strong>Note:</strong> this does not prevent underflow.
     *
     * @param item1 the first number
     * @param item2 the second number
     * @return the sum
     */
    public static int safeIntSum(final int item1, final int item2) {
        if (item2 > 0) {
            return (item1 > Integer.MAX_VALUE - item2) ? Integer.MAX_VALUE
                                                       : item1 + item2;
        }
        // TODO Handle negative values correctly: MIN_VALUE - 1 == MIN_VALUE
        return item1 + item2;
    }

    /**
     * Checks if <code>underscoreIndex</code> is inside <code>clipped</code>.
     * @param clipped String which may be clipped (partially replaced with "...")
     * @param underscoreIndex the index to check
     * @return <code>true</code> if <code>underscoreIndex</code> is inside
     *         <code>clipped</code>.
     *
     * @see Utilities#clipString(FontMetrics, String, int)
     */
    public static boolean insideString(final String clipped, final int underscoreIndex) {
        return (-1 < underscoreIndex && underscoreIndex < clipped.length());
    }

    /**
     * Checks if the currently installed Look and feel supports window
     * decorations.
     *
     * @return <code>true</code> if the currently installed Look and feel
     *         is not <code>null</code> and supports window decorations.
     */
    public static boolean lookAndFeelSupportsWindowDecorations() {
        LookAndFeel lnf = UIManager.getLookAndFeel();
        return lnf != null && lnf.getSupportsWindowDecorations();
    }

    /**
     * Calculates the Container from which paint process should be started. The result
     * can differ from the specified component in case component hierarchy contains
     * JComponents with non-optimized drawing (#see javax.swing.JComponent.isOptimizedDrawingEnabled())
     * and specified component is not on the top child hierarchy. Durign the calculation
     * paintRect should be considered to decide if its overlapped with the potentially
     * overlapping hierarchy or not. Is not curretnly used due to performance reasons.
     *
     * @param c JComponent to be painted
     * @param paintRect region of JComponent to painted. Currently is not used due to
     *                    the performance reasons
     *
     * @return Container from which actual painting to be started
     */
    public static Container getDrawingRoot(final JComponent c, final Rectangle paintRect) {
        Container parent = c.getParent();
        Component child = c;
        Container result = c;
        while (parent instanceof JComponent) {
            if (!((JComponent)parent).isOptimizedDrawingEnabled()
                && ((JComponent)parent).getComponentZOrder(child) != 0) {

                result = parent;
            }

            child = parent;
            parent = parent.getParent();
        }

        return result;
    }


    private static int getSuitableSubstringLength(final FontMetrics fm,
                                                  final String text,
                                                  final int width) {
        final int textLength = text.length();
        for (int i = 1; i < textLength; i++) {
            final int substrWidth = fm.stringWidth(text.substring(0, i));
            if (substrWidth > width) {
                return i - 1;
            }
        }

        return textLength;
    }

    private static Rectangle getScreenClientBounds(final GraphicsConfiguration graphConfig) {
        GraphicsConfiguration gc = graphConfig;
        if (gc == null) {
            gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
        }
        final Rectangle screenRect = gc.getBounds();
        final Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        return subtractInsets(screenRect, screenInsets);
    }

    private static void paintArrow(final Graphics g, final int x, final int y,
                                   final int direction, final int size,
                                   final boolean wide, final Color color,
                                   final boolean fill) {

        final int halfHeight = (size + 1) / 2;
        final int height = halfHeight * 2;
        final int width = wide ? halfHeight : height;

        final int[] heights = new int[] {0, halfHeight - 1, height - 2};
        final int[] lWidths = new int[] {width - 1, 0, width - 1};
        final int[] rWidths = new int[] {0, width - 1, 0};
        int[] px = null;
        int[] py = null;
        switch (direction) {
        case NORTH:
            px = heights;
            py = lWidths;
            break;
        case SOUTH:
            px = heights;
            py = rWidths;
            break;
        case WEST:
        case LEFT:
            px = lWidths;
            py = heights;
            break;
        case EAST:
        case RIGHT:
            px = rWidths;
            py = heights;
            break;
        default:
            // do nothing to be compatible with RI
            return;
        }

        final Color oldColor = g.getColor();
        g.setColor(color);
        g.translate(x, y);
        g.drawPolygon(px, py, 3);
        if (fill) {
            g.fillPolygon(px, py, 3);
        }
        g.translate(-x, -y);
        g.setColor(oldColor);
    }

    private static int horizontallyAlignRect(final int rectWidth, final Rectangle box, final int horizontalAlign) {
        int result = 0;
        if (horizontalAlign != LEFT) {
            result = box.width - rectWidth;
            if (horizontalAlign == CENTER) {
                result /= 2;
            }
        }
        result += box.x;
        return result;
    }

    private static int verticallyAlignRect(final int rectHeight, final Rectangle box, final int verticalAlign) {
        int result = 0;
        if (verticalAlign != TOP) {
            result = box.height - rectHeight;
            if (verticalAlign == CENTER) {
                result /= 2;
            }
        }
        result += box.y;
        return result;
    }
}
