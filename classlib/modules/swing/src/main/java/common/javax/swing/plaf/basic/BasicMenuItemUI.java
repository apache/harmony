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
package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuDragMouseEvent;
import javax.swing.event.MenuDragMouseListener;
import javax.swing.event.MenuKeyListener;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.MenuItemUI;

import org.apache.harmony.x.swing.ButtonCommons;
import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class BasicMenuItemUI extends MenuItemUI {
    protected class MouseInputHandler implements MouseInputListener {
        public void mouseEntered(final MouseEvent e) {
            final MenuSelectionManager manager = MenuSelectionManager.defaultManager();
            if (openMenuTimer != null) {
                openMenuTimer.stop();
            }
            manager.setSelectedPath(Utilities.addToPath(manager.getSelectedPath(), menuItem));
        }

        public void mouseExited(final MouseEvent e) {
            final MenuSelectionManager manager = MenuSelectionManager.defaultManager();
            manager.setSelectedPath(Utilities.removeFromPath(manager.getSelectedPath(), menuItem));
        }

        public void mouseReleased(final MouseEvent e) {
            MenuSelectionManager defaultManager = MenuSelectionManager.defaultManager();
            if (defaultManager.componentForPoint(e.getComponent(), e.getPoint()) == menuItem
                    || Utilities.isEmptyArray(defaultManager.getSelectedPath())) {

                doClick(defaultManager);
            } else {
                defaultManager.processMouseEvent(e);
            }
        }

        public void mousePressed(final MouseEvent e) {
            MenuSelectionManager.defaultManager().processMouseEvent(e);
        }

        public void mouseClicked(final MouseEvent e) {
            MenuSelectionManager.defaultManager().processMouseEvent(e);
        }

        public void mouseDragged(final MouseEvent e) {
            MenuSelectionManager.defaultManager().processMouseEvent(e);
        }

        public void mouseMoved(final MouseEvent e) {
            MenuSelectionManager.defaultManager().processMouseEvent(e);
        }
    }

    private class MenuItemHandler implements MenuDragMouseListener,
            FocusListener, ChangeListener, PropertyChangeListener {

        public void stateChanged(final ChangeEvent e) {
            menuItem.revalidate();
            menuItem.repaint();
        }

        public void propertyChange(final PropertyChangeEvent event) {
            menuItem.revalidate();
            menuItem.repaint();
        }
        public void menuDragMouseReleased(final MenuDragMouseEvent e) {
            doClick(MenuSelectionManager.defaultManager());
        }

        public void menuDragMouseDragged(final MenuDragMouseEvent e) {
        }

        public void menuDragMouseEntered(final MenuDragMouseEvent e) {
        }

        public void menuDragMouseExited(final MenuDragMouseEvent e) {
        }

        public void focusGained(final FocusEvent e) {
        }

        public void focusLost(final FocusEvent e) {
        }
    }

    protected JMenuItem menuItem;

    protected Icon arrowIcon;
    protected Icon checkIcon;

    protected Color selectionBackground;
    protected Color selectionForeground;
    protected Color disabledForeground;
    protected Color acceleratorForeground;
    protected Color acceleratorSelectionForeground;
    protected int defaultTextIconGap;
    protected Font acceleratorFont;

    protected boolean oldBorderPainted;

    protected MouseInputListener mouseInputListener;
    protected MenuDragMouseListener menuDragMouseListener;
    protected MenuKeyListener menuKeyListener;

    private MenuItemHandler menuItemHandler;

    private static final String PROPERTY_PREFIX = "MenuItem";

    private final Rectangle viewR = new Rectangle();
    private final Rectangle iconR = new Rectangle();
    private final Rectangle textR = new Rectangle();

    private static final Action MNEMONIC_ACTION = new AbstractAction () {
        public void actionPerformed(final ActionEvent e) {
            MenuSelectionManager.defaultManager().clearSelectedPath();
            final JMenuItem item = (JMenuItem)e.getSource();
            item.doClick(0);
        }
    };

    private String acceleratorDelimiter = "";

    static Timer openMenuTimer;

    public static ComponentUI createUI(final JComponent c) {
        return new BasicMenuItemUI();
    }

    protected String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

    public void installUI(final JComponent c) {
        menuItem = (JMenuItem)c;
        installDefaults();
        installComponents(menuItem);
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(final JComponent c) {
        // Fix for HARMONY-2704, for compatibility with RI
        JMenuItem jMenuItem = (JMenuItem) c;

        uninstallKeyboardActions();
        uninstallListeners();
        uninstallComponents(menuItem);
        uninstallDefaults();
        menuItem = null;
    }

    protected void installDefaults() {
        LookAndFeel.installColorsAndFont(menuItem, getPropertyPrefix() + ".background",
                                         getPropertyPrefix() + ".foreground",
                                         getPropertyPrefix() + ".font");
        LookAndFeel.installBorder(menuItem, getPropertyPrefix() + ".border");
        menuItem.setMargin(UIManager.getInsets(getPropertyPrefix() + ".margin"));

        LookAndFeel.installProperty(menuItem, "opaque", Boolean.TRUE);
        arrowIcon = UIManager.getIcon(getPropertyPrefix() + ".arrowIcon");
        checkIcon = UIManager.getIcon(getPropertyPrefix() + ".checkIcon");

        selectionBackground = UIManager.getColor(getPropertyPrefix() + ".selectionBackground");
        selectionForeground = UIManager.getColor(getPropertyPrefix() + ".selectionForeground");
        disabledForeground = UIManager.getColor(getPropertyPrefix() + ".disabledForeground");
        acceleratorForeground = UIManager.getColor(getPropertyPrefix() + ".acceleratorForeground");
        acceleratorSelectionForeground = UIManager.getColor(getPropertyPrefix() + ".acceleratorSelectionForeground");

        acceleratorFont = UIManager.getFont(getPropertyPrefix() + ".acceleratorFont");
        String delim = UIManager.getString(getPropertyPrefix() + ".acceleratorDelimiter");
        acceleratorDelimiter = (delim != null) ? delim : "+";

        defaultTextIconGap = 4;
        oldBorderPainted = UIManager.getBoolean(getPropertyPrefix() + ".borderPainted");
    }

    protected void uninstallDefaults() {
        LookAndFeel.uninstallBorder(menuItem);
        Utilities.uninstallColorsAndFont(menuItem);
        if (Utilities.isUIResource(menuItem.getMargin())) {
            menuItem.setMargin(null);
        }
    }

    protected void installComponents(final JMenuItem menuItem) {
    }

    protected void uninstallComponents(final JMenuItem menuItem) {
    }

    protected void installListeners() {
        if (menuItem == null) {
            return;
        }

        menuDragMouseListener = createMenuDragMouseListener(menuItem);
        menuKeyListener = createMenuKeyListener(menuItem);
        mouseInputListener = createMouseInputListener(menuItem);
        menuItem.addMenuDragMouseListener(menuDragMouseListener);
        menuItem.addMenuKeyListener(menuKeyListener);
        menuItem.addMouseListener(mouseInputListener);
        menuItem.addMouseMotionListener(mouseInputListener);

        menuItemHandler = new MenuItemHandler();
        menuItem.addFocusListener(menuItemHandler);
        menuItem.addPropertyChangeListener(menuItemHandler);
        menuItem.addChangeListener(menuItemHandler);
    }

    protected void uninstallListeners() {
        if (menuItem == null) {
            return;
        }

        menuItem.removeMenuDragMouseListener(menuDragMouseListener);
        menuItem.removeMenuKeyListener(menuKeyListener);
        menuItem.removeMouseListener(mouseInputListener);
        menuItem.removeMouseMotionListener(mouseInputListener);
        menuItem.removePropertyChangeListener(menuItemHandler);
        menuItem.removeChangeListener(menuItemHandler);
        menuItem.removeFocusListener(menuItemHandler);

        menuItemHandler = null;
        menuDragMouseListener = null;
        menuKeyListener = null;
        mouseInputListener = null;
    }

    protected void installKeyboardActions() {
        ActionMap actionMap = new ActionMapUIResource();
        actionMap.put(StringConstants.MNEMONIC_ACTION, MNEMONIC_ACTION);
        actionMap.setParent(((BasicLookAndFeel)UIManager.getLookAndFeel()).getAudioActionMap());
        SwingUtilities.replaceUIActionMap(menuItem, actionMap);
    }

    protected void uninstallKeyboardActions() {
        SwingUtilities.replaceUIActionMap(menuItem, null);
    }

    protected MouseInputListener createMouseInputListener(final JComponent c) {
        return new MouseInputHandler();
    }

    protected MenuDragMouseListener createMenuDragMouseListener(final JComponent c) {
        return (menuItemHandler != null) ? menuItemHandler : (menuItemHandler = new MenuItemHandler());
    }

    protected MenuKeyListener createMenuKeyListener(final JComponent c) {
        return null;
    }

    public Dimension getMinimumSize(final JComponent c) {
        if (c == null) {
            throw new NullPointerException(Messages.getString("swing.03","component")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return null;
    }

    public Dimension getPreferredSize(final JComponent c) {
        return getPreferredMenuItemSize(c, checkIcon, arrowIcon, defaultTextIconGap);
    }

    public Dimension getMaximumSize(final JComponent c) {
        if (c == null) {
            throw new NullPointerException(Messages.getString("swing.03","component")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return null;
    }

    protected Dimension getPreferredMenuItemSize(final JComponent c,
                                                 final Icon check,
                                                 final Icon arrow,
                                                 final int textIconGap) {

        final JMenuItem item = (JMenuItem)c;
        Dimension result = ButtonCommons.getPreferredSize(item, null, textIconGap);
        if (arrow != null && isArrowToBeDrawn()) {
            result.width += arrow.getIconWidth() + textIconGap;
            result.height = Math.max(result.height, arrow.getIconHeight());
        }
        if(check != null) {
            result.width += check.getIconWidth() + textIconGap;
            result.height = Math.max(result.height, check.getIconHeight());
        }
        final KeyStroke accelerator = item.getAccelerator();
        if (accelerator != null) {
            String accelText = Utilities.getAcceleratorText(accelerator,
                                                            acceleratorDelimiter);
            FontMetrics fm = item.getFontMetrics(acceleratorFont);
            result.width += fm.stringWidth(accelText) + 3*textIconGap;
        }

        result.width += 1 + 2*textIconGap;
        result.height += 1;

        return result;
    }

    public void update(final Graphics g, final JComponent c) {
        paint(g, c);
    }

    public void paint(final Graphics g,
                      final JComponent c) {
        final JMenuItem item = (JMenuItem)c;
        Color bgColor = (isPaintArmed() && item.isEnabled()) ? selectionBackground : item.getBackground();
        Color fgColor = !item.isEnabled() ? disabledForeground :
            (!item.isArmed() ? item.getForeground() : selectionForeground);
        paintMenuItem(g, c, checkIcon, arrowIcon, bgColor, fgColor, defaultTextIconGap);
    }

    protected void paintMenuItem(final Graphics g,
                                 final JComponent c,
                                 final Icon check,
                                 final Icon arrow,
                                 final Color background,
                                 final Color foreground,
                                 final int textIconGap) {
        final Color oldColor = g.getColor();
        final JMenuItem item = (JMenuItem)c;
        Icon icon = item.getIcon();
        final boolean isLTR = item.getComponentOrientation().isLeftToRight();
        final int arrowInset = getArrowInset(arrow, textIconGap, item);
        final int checkInset = getCheckInset(check, textIconGap, item);
        final int leftInset = isLTR ? checkInset : arrowInset;
        final int rightInset = isLTR ? arrowInset : checkInset;

        viewR.setBounds(0, 0, 0, 0);
        ButtonCommons.getPaintingParameters(item, viewR, iconR, textR, icon, leftInset, rightInset);

        paintBackground(g, item, background);
        if (arrow != null && isArrowToBeDrawn()) {
            int arrowX = viewR.x + textIconGap + (isLTR ? viewR.width : -arrowInset);
            int arrowY = viewR.y  + (viewR.height - arrow.getIconHeight())/2;
            arrow.paintIcon(c, g, arrowX, arrowY);
        }
        if (check != null) {
            int checkX = viewR.x + textIconGap + (isLTR ? -checkInset : viewR.width);
            int checkY = viewR.y  + (viewR.height - check.getIconHeight())/2;
            check.paintIcon(c, g, checkX, checkY);
        }

        icon = ButtonCommons.getCurrentIcon(item);
        if (icon != null) {
            icon.paintIcon(c, g, iconR.x, iconR.y);
        }
        paintText(g, item, textR, item.getText());
        paintAccelerator(g, viewR, iconR, textR, textIconGap, isLTR);
        g.setColor(oldColor);
    }

    private int getCheckInset(final Icon check, final int textIconGap, final JMenuItem item) {
        int checkInset;
        if (check != null ) {
            checkInset = check.getIconWidth() + 2*textIconGap;
        } else {
            checkInset = !Utilities.isEmptyString(item.getText()) ? textIconGap : 0;
        }
        return checkInset;
    }

    private int getArrowInset(final Icon arrow, final int textIconGap, final JMenuItem item) {
        int arrowInset;
        if (arrow != null && isArrowToBeDrawn()) {
            arrowInset = arrow.getIconWidth() + 2*textIconGap;
        } else {
            arrowInset = !Utilities.isEmptyString(item.getText()) ? textIconGap : 0;
        }
        return arrowInset;
    }

    private void paintAccelerator(final Graphics g, final Rectangle viewR, final Rectangle iconR, final Rectangle textR, final int textIconGap, final boolean isLTR) {
        final KeyStroke accel = menuItem.getAccelerator();
        if (accel == null || viewR.width <= iconR.width + textR.width + 3*textIconGap) {
            return;
        }

        final String acceleratorText = Utilities.getAcceleratorText(accel, acceleratorDelimiter);
        final FontMetrics fm = menuItem.getFontMetrics(acceleratorFont);
        final int acceleratorWidth = fm.stringWidth(acceleratorText);
        if (viewR.width < iconR.width + textR.width + 3*textIconGap + acceleratorWidth) {
            return;
        }

        final Color color = menuItem.isEnabled() ? (menuItem.isArmed()
                ? acceleratorSelectionForeground : acceleratorForeground)
                : getDisabledTextForeground();
        textR.setSize(acceleratorWidth, fm.getHeight());
        textR.x = viewR.x + (isLTR ? viewR.width - textR.width - textIconGap : textIconGap);
        ButtonCommons.paintText(g, fm, acceleratorText, -1,
                                textR, acceleratorText, color);
    }

    protected void paintBackground(final Graphics g,
                                   final JMenuItem menuItem,
                                   final Color bgColor) {
        if (menuItem.isOpaque() || isPaintArmed()) {
            g.setColor(bgColor);
            g.fillRect(0, 0, menuItem.getWidth(), menuItem.getHeight());
        }
    }

    protected void paintText(final Graphics g,
                             final JMenuItem menuItem,
                             final Rectangle textRect,
                             final String text) {
        if (Utilities.isEmptyString(text)) {
            return;
        }

        FontMetrics fm = menuItem.getFontMetrics(menuItem.getFont());
        String clippedText = Utilities.clipString(fm, text, textRect.width);

        final Color color = menuItem.isEnabled() ? (isPaintArmed()
                ? selectionForeground : menuItem.getForeground())
                : getDisabledTextForeground();

        ButtonCommons.paintText(g, fm, clippedText,
                                menuItem.getDisplayedMnemonicIndex(),
                                textRect, clippedText, color);
    }

    public MenuElement[] getPath() {
        MenuElement[] selectedPath = MenuSelectionManager.defaultManager().getSelectedPath();
        if (Utilities.isEmptyArray(selectedPath)) {
            return new MenuElement[0];
        }

        MenuElement lastElement = selectedPath[selectedPath.length - 1];
        boolean pathContainsItem = lastElement == menuItem;
        if (!pathContainsItem && lastElement != menuItem.getParent()) {
            return new MenuElement[] {menuItem};
        }
        int resultLength = selectedPath.length + (pathContainsItem ? 0 : 1);
        MenuElement[] result = new MenuElement[resultLength];
        if (!pathContainsItem) {
            result[resultLength - 1] = menuItem;
        }
        System.arraycopy(selectedPath, 0, result, 0, selectedPath.length);

        return result;
    }

    protected void doClick(final MenuSelectionManager msm) {
        MenuSelectionManager manager = (msm != null) ? msm : MenuSelectionManager.defaultManager();
        manager.clearSelectedPath();
        final BasicLookAndFeel lookAndFeel = (BasicLookAndFeel)UIManager.getLookAndFeel();
        lookAndFeel.fireSoundAction(menuItem, getPropertyPrefix() + ".commandSound");
        menuItem.doClick(0);
    }

    boolean isArrowToBeDrawn() {
        return arrowIcon != null;
    }

    boolean isPaintArmed() {
        return menuItem.isArmed();
    }

    private Color getDisabledTextForeground() {
        return (disabledForeground != null) ? disabledForeground : menuItem.getBackground().darker();
    }
}
