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
 * @author Vadim L. Bogdanov
 */

package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Rectangle;

import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import javax.swing.border.EmptyBorder;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.TitlePaneInternals;
import org.apache.harmony.x.swing.Utilities;


public class BasicInternalFrameTitlePane extends JComponent {

    public class CloseAction extends AbstractAction {
        public CloseAction() {
            putValue(SHORT_DESCRIPTION, closeTooltipText);
            putValue(SMALL_ICON, closeIcon);
        }

        public void actionPerformed(final ActionEvent e) {
            if (frame.isClosable()) {
                try {
                    frame.setClosed(true);
                } catch (final PropertyVetoException v) {
                }
            }
        }
    }

    public class IconifyAction extends AbstractAction {
        public IconifyAction() {
            putValue(SHORT_DESCRIPTION, minimizeTooltipText);
            putValue(SMALL_ICON, iconIcon);
        }

        public void actionPerformed(final ActionEvent e) {
            if (frame.isIconifiable()) {
                try {
                    frame.setIcon(true);
                } catch (final PropertyVetoException v) {
                }
            }
        }
    }

    public class MaximizeAction extends AbstractAction {
        public MaximizeAction() {
            putValue(SHORT_DESCRIPTION, maximizeTooltipText);
            putValue(SMALL_ICON, maxIcon);
        }

        public void actionPerformed(final ActionEvent e) {
            if (frame.isMaximizable()) {
                try {
                    if (frame.isIcon()) {
                        frame.setIcon(false);
                    }
                    frame.setMaximum(true);
                } catch (final PropertyVetoException v) {
                }
            }
        }
    }

    public class RestoreAction extends AbstractAction {
        public RestoreAction() {
            putValue(SHORT_DESCRIPTION, restoreTooltipText);
            putValue(SMALL_ICON, minIcon);
        }

        public void actionPerformed(final ActionEvent e) {
            if (frame.isIcon()) {
                if (frame.isIconifiable()) {
                    try {
                        frame.setIcon(false);
                    } catch (final PropertyVetoException v) {
                    }
                }
            } else if (frame.isMaximum()) {
                if (frame.isMaximizable()) {
                    try {
                        frame.setMaximum(false);
                    } catch (final PropertyVetoException v) {
                    }
                }
            }
        }
    }

    public class MoveAction extends AbstractAction {
        private static final String MOVE_ACTION_NAME = "Move";
        
        public MoveAction() {
            putValue(Action.NAME, MOVE_ACTION_NAME);
        }
        
        public void actionPerformed(final ActionEvent e) {
            if (e == null) {
                return;
            }
            
            ((BasicDesktopPaneUI)frame.getDesktopPane().getUI()).
                frameOperation = BasicDesktopPaneUI.DRAGGING;
        }
    }

    public class SizeAction extends AbstractAction {
        private static final String SIZE_ACTION_NAME = "Size";
        
        public SizeAction() {
            putValue(Action.NAME, SIZE_ACTION_NAME);
        }
        
        public void actionPerformed(final ActionEvent e) {
            if (e == null) {
                return;
            }
            ((BasicDesktopPaneUI)frame.getDesktopPane().getUI()).
                frameOperation = BasicDesktopPaneUI.RESIZING;
        }
    }

    public class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent e) {
            // Note: could optimize work with buttons
            // may be we should always do the same work (update buttons,
            // enableActions, etc.)?
            if (JInternalFrame.IS_MAXIMUM_PROPERTY.equals(e.getPropertyName())
                || JInternalFrame.IS_ICON_PROPERTY.equals(e.getPropertyName())) {
                setButtonIcons();
                enableActions();
            } else if (e.getPropertyName().
                    equals(StringConstants.INTERNAL_FRAME_ICONABLE_PROPERTY)) {
                updateButton(iconButton, frame.isIconifiable());
            } else if (e.getPropertyName().
                    equals(StringConstants.INTERNAL_FRAME_MAXIMIZABLE_PROPERTY)) {
                updateButton(maxButton, frame.isMaximizable());
            } else if (e.getPropertyName().
                    equals(StringConstants.INTERNAL_FRAME_CLOSABLE_PROPERTY)) {
                updateButton(closeButton, frame.isClosable());
            } else if (e.getPropertyName().
                    equals(StringConstants.INTERNAL_FRAME_RESIZABLE_PROPERTY)) {
                enableActions();
            } else if (e.getPropertyName().equals("ancestor")) {
                // to enable sizeAction, moveAction
                enableActions();
                revalidate();
            } else if (JInternalFrame.FRAME_ICON_PROPERTY.
                    equals(e.getPropertyName())) {
                internals.setFrameIcon(frame.getFrameIcon());
                revalidate();
                repaint();
            } else if (JInternalFrame.TITLE_PROPERTY.
                    equals(e.getPropertyName())) {
                revalidate();
                repaint();
            }
        }

        private void updateButton(final JButton button, final boolean add) {
            if (add) {
                add(button);
            } else {
                remove(button);
            }
            enableActions();
            revalidate();
            repaint();
        }
    }

    public class SystemMenuBar extends JMenuBar {
        public SystemMenuBar() {
            setFocusable(false);
            setBorder(new EmptyBorder(0, 0, 0, 0));
        }

        public void paint(final Graphics g) {
            paintChildren(g);
        }
    }

    public class TitlePaneLayout implements LayoutManager {
        public Dimension preferredLayoutSize(final Container c) {
            return minimumLayoutSize(c);
        }

        public Dimension minimumLayoutSize(final Container c) {
            Dimension size = Utilities.getCompoundLabelSize(
                BasicInternalFrameTitlePane.this,
                getMinimumString(internals.getWindowTitle()),
                internals.getFrameIcon(),
                SwingConstants.LEFT,
                SwingConstants.LEFT,
                internals.gapX
                );

            if (closeButton.getParent() == BasicInternalFrameTitlePane.this) {
                size.width += internals.gapX + closeIcon.getIconWidth();
            }
            if (maxButton.getParent() == BasicInternalFrameTitlePane.this) {
                size.width += internals.gapX + maxIcon.getIconWidth();
            }
            if (iconButton.getParent() == BasicInternalFrameTitlePane.this) {
                size.width += internals.gapX + iconIcon.getIconWidth();
            }

            size.width += 2 * internals.gapX;

            return Utilities.addInsets(size, BasicInternalFrameTitlePane.this
                    .getInsets());
        }

        private String getMinimumString(final String str) {
            if (str == null) {
                return "";
            }
            return str.substring(0, Math.min(3, str.length())) + "...";
        }

        public void addLayoutComponent(final String name, final Component c) {
        }

        public void layoutContainer(final Container c) {
            final int width = internals.getUIFrameIcon().getIconWidth();
            final int height = internals.getUIFrameIcon().getIconHeight();

            Rectangle inner = SwingUtilities.calculateInnerArea(
                BasicInternalFrameTitlePane.this, null);
            int y = inner.y + (inner.height - height) / 2;
            boolean isLTR = getComponentOrientation().isLeftToRight();
            int buttonOffsetX = isLTR ? getWidth() : -width;
            int sign = isLTR ? 1 : -1;
            Rectangle buttonBounds = new Rectangle(buttonOffsetX, y, width, height);

            // calculate bounds of closeButton
            if (closeButton.getParent() == BasicInternalFrameTitlePane.this) {
                buttonBounds.x -= sign * (width + internals.gapX);
                closeButton.setBounds(buttonBounds);
            }

            // calculate bounds of maxButton
            if (maxButton.getParent() == BasicInternalFrameTitlePane.this) {
                buttonBounds.x -= sign * (width + internals.gapX);
                maxButton.setBounds(buttonBounds);
            }

            // calculate bounds of iconButton
            if (iconButton.getParent() == BasicInternalFrameTitlePane.this) {
                buttonBounds.x -= sign * (width + internals.gapX);
                iconButton.setBounds(buttonBounds);
            }

            int iconTextOffsetX = isLTR ? internals.gapX
                    : buttonBounds.x + width + internals.gapX;
            int reminderWidth = isLTR ? buttonBounds.x - 2 * internals.gapX
                    : getWidth() - iconTextOffsetX - internals.gapX;
            Rectangle viewR = new Rectangle(iconTextOffsetX, 0, reminderWidth, getHeight());

            paintedTitle = layoutCompoundLabel(BasicInternalFrameTitlePane.this,
                internals.getWindowTitle(),
                internals.hasMenuBar ? internals.getFrameIcon() : null,
                viewR,
                menuBarBounds,
                textR);
            if (menuBar != null) {
                menuBar.setBounds(menuBarBounds);
            }

            // calculate bounds of possible decoration
            if (internals.decorationR != null) {
                int decorX = isLTR ? textR.x + textR.width + internals.gapX
                        : buttonBounds.x + width + internals.gapX;
                int decorWidth = isLTR ? viewR.x + viewR.width - decorX
                        : textR.x - viewR.x - internals.gapX;

                internals.decorationR.setBounds(decorX, textR.y,  decorWidth, textR.height);
            }
        }

        private String layoutCompoundLabel(final JComponent c,
                                           final String text,
                                           final Icon icon,
                                           final Rectangle viewR,
                                           final Rectangle iconR,
                                           final Rectangle textR) {
            boolean isEmptyTitle = Utilities.isEmptyString(text);
            String fixedText = isEmptyTitle ? " ": text;

            String result = SwingUtilities.layoutCompoundLabel(c,
                Utilities.getFontMetrics(c),
                fixedText,
                icon,
                SwingConstants.CENTER,
                SwingConstants.LEADING,
                SwingConstants.CENTER,
                SwingConstants.TRAILING,
                viewR,
                iconR,
                textR,
                internals.gapX
            );

            if (isEmptyTitle) {
                result = "";
                textR.width = -internals.gapX;
            }
            return result;
        }

        public void removeLayoutComponent(final Component comp) {
        }
    }

    protected static final String ICONIFY_CMD = new String("Iconify");
    protected static final String MAXIMIZE_CMD = new String("Maximize");
    protected static final String RESTORE_CMD = new String("Restore");
    protected static final String CLOSE_CMD = new String("Close");
    protected static final String MOVE_CMD = new String("Move");
    protected static final String SIZE_CMD = new String("Size");

    protected JMenuBar menuBar;
    protected JMenu windowMenu;
    protected JInternalFrame frame;

    protected Color selectedTitleColor;
    protected Color notSelectedTitleColor;
    protected Color selectedTextColor;
    protected Color notSelectedTextColor;

    protected JButton iconButton;
    protected JButton maxButton;
    protected JButton closeButton;

    protected Icon iconIcon;
    protected Icon maxIcon;
    protected Icon minIcon;
    protected Icon closeIcon;

    protected Action iconifyAction;
    protected Action maximizeAction;
    protected Action restoreAction;
    protected Action closeAction;
    protected Action moveAction;
    protected Action sizeAction;

    protected PropertyChangeListener propertyChangeListener;

    // tooltips text
    private String maximizeTooltipText;
    private String minimizeTooltipText;
    private String closeTooltipText;
    private String restoreTooltipText;

    // system menu items text
    private String closeButtonText;
    private String maxButtonText;
    private String minButtonText;
    private String moveButtonText;
    private String restoreButtonText;
    private String sizeButtonText;

    private String paintedTitle;
    private Rectangle textR = new Rectangle();
    private Rectangle menuBarBounds = new Rectangle();

    private TitlePaneInternals internals;

    public BasicInternalFrameTitlePane(final JInternalFrame frame) {
        if (frame == null) {
            throw new NullPointerException();  
        } 

        this.frame = frame;
        setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
        installInternals();
        installTitlePane(); 
    }

    protected JMenuBar createSystemMenuBar() {
        return new SystemMenuBar();
    }

    protected void assembleSystemMenu() {
        windowMenu = createSystemMenu();
        internals.setWindowMenu(windowMenu);
        addSystemMenuItems(windowMenu);

        menuBar = createSystemMenuBar();
        menuBar.add(windowMenu);
    }

    protected void addSystemMenuItems(final JMenu menu) {
        menu.add(TitlePaneInternals.createMenuItem(restoreAction,
                                                   restoreButtonText, 'R'));
        menu.add(TitlePaneInternals.createMenuItem(moveAction,
                                                   moveButtonText, 'M'));
        menu.add(TitlePaneInternals.createMenuItem(sizeAction,
                                                   sizeButtonText, 'S'));
        menu.add(TitlePaneInternals.createMenuItem(iconifyAction,
                                                   minButtonText, 'N'));
        menu.add(TitlePaneInternals.createMenuItem(maximizeAction,
                                                   maxButtonText, 'X'));

        menu.addSeparator();
        menu.add(TitlePaneInternals.createMenuItem(closeAction,
                                                   closeButtonText, 'C'));
    }

    protected JMenu createSystemMenu() {
        JMenu menu = new JMenu();
        menu.setBorder(new EmptyBorder(0, 0, 0, 0));
        menu.setOpaque(false);
        return menu;
    }

    protected void postClosingEvent(final JInternalFrame frame) {
        // this method seems to be unused
        Object[] listeners = frame.getListeners(InternalFrameListener.class);
        InternalFrameEvent e = null;

        for (int i = 0; i < listeners.length; i++) {
            if (e == null) {
                e = new InternalFrameEvent(
                    frame, InternalFrameEvent.INTERNAL_FRAME_CLOSING);
            }
            ((InternalFrameListener)listeners[i]).internalFrameClosing(e);
        }
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler();
    }

    protected LayoutManager createLayout() {
        return new TitlePaneLayout();
    }

    protected String getTitle(final String text, final FontMetrics fm,
                              final int availableWidth) {
        return Utilities.clipString(fm, text, availableWidth);
    }

    protected void paintTitleBackground(final Graphics g) {
        g.setColor(internals.isSelected()
                   ? selectedTitleColor
                   : notSelectedTitleColor);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    public void paintComponent(final Graphics g) {
        paintTitleBackground(g);

        if (menuBar == null) {
            Icon frameIcon = internals.getFrameIcon();
            if (frameIcon != null) {
                frameIcon.paintIcon(this, g, menuBarBounds.x, menuBarBounds.y);
            }
        }

        g.setColor(internals.isSelected()
                   ? selectedTextColor
                   : notSelectedTextColor);

        FontMetrics fm = getFontMetrics(g.getFont());
        g.drawString(paintedTitle, textR.x,
                     Utilities.getTextY(fm, textR));
    }

    protected void installListeners() {
        if (propertyChangeListener == null) {
            propertyChangeListener = createPropertyChangeListener();
        }
        frame.addPropertyChangeListener(propertyChangeListener);

        // propertyChangeListener will be uninstalled
        // when title pane is removed from internal frame
        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(final HierarchyEvent e) {
                if (e.getChanged() == BasicInternalFrameTitlePane.this
                        && (e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0
                        && !frame.isAncestorOf(BasicInternalFrameTitlePane.this)) {
                    uninstallListeners();
                }
            }
        });
    }

    protected void uninstallListeners() {
        // Warning: there is no method like uninstallTitlePane(); this method
        // is called directly from HierarchyListener.hierarchyChanged();
        frame.removePropertyChangeListener(propertyChangeListener);
    }

    protected void installDefaults() {
        // tooltips
        maximizeTooltipText = UIManager.getString(
                "InternalFrame.maxButtonToolTip");
        minimizeTooltipText = UIManager.getString(
                "InternalFrame.iconButtonToolTip");
        closeTooltipText = UIManager.getString(
                "InternalFrame.closeButtonToolTip");
        restoreTooltipText = UIManager.getString(
                "InternalFrame.restoreButtonToolTip");

        // window menu labels
        closeButtonText = UIManager.getString(
                "InternalFrameTitlePane.closeButtonText");
        maxButtonText = UIManager.getString(
                "InternalFrameTitlePane.maximizeButtonText");
        minButtonText = UIManager.getString(
                "InternalFrameTitlePane.minimizeButtonText");
        moveButtonText = UIManager.getString(
                "InternalFrameTitlePane.moveButtonText");
        restoreButtonText = UIManager.getString(
                "InternalFrameTitlePane.restoreButtonText");
        sizeButtonText = UIManager.getString(
                "InternalFrameTitlePane.sizeButtonText");

        // icons
        closeIcon = UIManager.getIcon("InternalFrame.closeIcon");
        maxIcon = UIManager.getIcon("InternalFrame.maximizeIcon");
        minIcon = UIManager.getIcon("InternalFrame.minimizeIcon");
        iconIcon = UIManager.getIcon("InternalFrame.iconifyIcon");
        // "InternalFrame.icon" is loaded in BasicInternalFrameUI

        // colors
        selectedTitleColor =
            UIManager.getColor("InternalFrame.activeTitleBackground");
        selectedTextColor =
            UIManager.getColor("InternalFrame.activeTitleForeground");
        notSelectedTitleColor =
            UIManager.getColor("InternalFrame.inactiveTitleBackground");
        notSelectedTextColor =
            UIManager.getColor("InternalFrame.inactiveTitleForeground");

        // font
        setFont(UIManager.getFont("InternalFrame.titleFont"));
    }

    protected void uninstallDefaults() {
        // there is no need to uninstall anything because
        // the title pane is replaced while changing L&F
    }

    /**
     * This function is used in
     * <code>BasicInternalFrameUI.installKeyboardActions</code> to implement
     * menu opening with keyboard.
     */
    protected void showSystemMenu() {
        windowMenu.doClick(0);
    }

    protected void setButtonIcons() {
        if (frame.isIcon()) {
            iconButton.setAction(restoreAction);
        } else {
            iconButton.setAction(iconifyAction);
        }

        if (frame.isMaximum() && !frame.isIcon()) {
            maxButton.setAction(restoreAction);
        } else {
            maxButton.setAction(maximizeAction);
        }
    }

    protected void installTitlePane() {
        installDefaults();
        installListeners();
        createActions();
        setLayout(createLayout());
        createButtons();
        assembleSystemMenu();
        addSubComponents();
        enableActions();
    }

    protected void enableActions() {
        iconifyAction.setEnabled(!frame.isIcon() && frame.isIconifiable());
        closeAction.setEnabled(frame.isClosable());
        maximizeAction.setEnabled(frame.isMaximizable()
                                  && (!frame.isMaximum()
                                      || frame.isIcon()
                                      && frame.isIconifiable()));

        restoreAction.setEnabled(frame.isMaximum() && frame.isMaximizable()
                                 || frame.isIcon() && frame.isIconifiable());

        moveAction.setEnabled(frame.getDesktopPane() != null
            && !frame.isMaximum()
            && frame.getDesktopPane().getUI() instanceof BasicDesktopPaneUI);

        sizeAction.setEnabled(moveAction.isEnabled()
                              && frame.isResizable() && !frame.isIcon());
    }

    protected void createButtons() {
        maxButton = createTitlePaneButton(maximizeAction);
        iconButton = createTitlePaneButton(iconifyAction);
        closeButton = createTitlePaneButton(closeAction);

        setButtonIcons();
    }

    private JButton createTitlePaneButton(final Action action) {
        JButton button = new JButton(action);
        button.setFocusable(false);
        return button;
    }

    protected void createActions() {
        closeAction = new CloseAction();
        iconifyAction = new IconifyAction();
        maximizeAction = new MaximizeAction();
        moveAction = new MoveAction();
        restoreAction = new RestoreAction();
        sizeAction = new SizeAction();
    }

    protected void addSubComponents() {
        if (menuBar != null) {
            add(menuBar);
        }

        if (frame.isIconifiable()) {
            add(iconButton);
        }
        if (frame.isMaximizable()) {
            add(maxButton);
        }
        if (frame.isClosable()) {
            add(closeButton);
        }
    }

    private void installInternals() {
        internals = new TitlePaneInternals(frame);
        putClientProperty("internals", internals);
    }
}
