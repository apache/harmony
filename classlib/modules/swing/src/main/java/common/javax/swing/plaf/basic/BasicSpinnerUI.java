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
 * @author Dennis Ushakov
 */
package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.LookAndFeel;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SpinnerUI;

import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;


public class BasicSpinnerUI extends SpinnerUI {

    private class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent event) {
            String changedProperty = event.getPropertyName();

            if (StringConstants.EDITOR_PROPERTY_CHANGED.equals(changedProperty)) {
                replaceEditor((JComponent)event.getOldValue(), (JComponent)event.getNewValue());
            }
        }
    }

    private class ArrowButtonHandler implements ActionListener, FocusListener, MouseListener {
        private int dir;
        private Timer scrollTimer;
        private static final int SCROLL_DELAY = 50;
        private static final int PRE_SCROLL_DELAY = 300;
        private static final int NEXT = 0;
        private static final int PREVIOUS = 1;

        public void actionPerformed(final ActionEvent e) {
            e.setSource(spinner);
            if (dir == NEXT) {
                BasicSpinnerKeyboardActions.incrementAction.actionPerformed(e);
            }
            if (dir == PREVIOUS) {
                BasicSpinnerKeyboardActions.decrementAction.actionPerformed(e);
            }
        }

        public void focusGained(final FocusEvent e) {
            editor.requestFocus();
        }

        public void focusLost(final FocusEvent e) {
        }


        public void mousePressed(final MouseEvent e) {
            if (e.getSource() == nextButton) {
                dir = NEXT;
            } else if (e.getSource() == previousButton) {
                dir = PREVIOUS;
            }
            if (scrollTimer == null) {
                scrollTimer = new Timer(SCROLL_DELAY, this);
                scrollTimer.setInitialDelay(PRE_SCROLL_DELAY);
            }
            scrollTimer.restart();
        }

        public void mouseReleased(final MouseEvent e) {
            if (scrollTimer != null) {
                scrollTimer.stop();
            }
        }

        public void mouseClicked(final MouseEvent e) {
        }

        public void mouseEntered(final MouseEvent e) {
        }

        public void mouseExited(final MouseEvent e) {
        }
    }

    private class SpinnerLayout implements LayoutManager {

        public void addLayoutComponent(final String name, final Component comp) {
        }

        public void layoutContainer(final Container parent) {
            int left = parent.getInsets().left;
            int top = parent.getInsets().top;
            int right = parent.getInsets().right;
            int bottom = parent.getInsets().bottom;

            Dimension parentSize = parent.getSize();
            int width = parentSize.width;
            int height = parentSize.height;
            int nextWidth = 0;
            int previousWidth = 0;

            if (nextButton != null) {
                nextButton.setSize(buttonSize);
                nextWidth = nextButton.getWidth();
            }
            if (previousButton != null) {
                previousButton.setSize(buttonSize);
                previousWidth = previousButton.getWidth();
            }

            int rightMargin = Math.max(nextWidth, previousWidth);
            int leftMargin = 0;

            if (spinner.getComponentOrientation().isLeftToRight()) {
                if (editor != null) {
                    leftMargin = width - rightMargin;
                    editor.setBounds(left, top, leftMargin - left, height - bottom - top);
                }
                if (nextButton != null) {
                    nextButton.setBounds(leftMargin, buttonInsets.top, 
                                         nextButton.getWidth() - buttonInsets.right, height / 2 - buttonInsets.top);
                }
                if (previousButton != null) {
                    previousButton.setBounds(leftMargin, height / 2, previousButton.getWidth() - buttonInsets.right,
                                             height / 2 - buttonInsets.bottom);
                }
            }  else {
                if (editor != null) {
                    leftMargin = width - rightMargin;
                    editor.setBounds(rightMargin, top, leftMargin - right, height - bottom - top);
                }
                if (nextButton != null) {
                    nextButton.setBounds(buttonInsets.left, buttonInsets.top, 
                                         nextButton.getWidth() - buttonInsets.left,
                                         height / 2 - buttonInsets.top);
                }
                if (previousButton != null) {
                    previousButton.setBounds(buttonInsets.left, height / 2, 
                                             previousButton.getWidth() - buttonInsets.left,
                                             height / 2 - buttonInsets.bottom);
                }
            }
        }

        public Dimension minimumLayoutSize(final Container parent) {
            Dimension editorSize = editor != null ? editor.getMinimumSize() : new Dimension();
            Dimension result = new Dimension(editorSize.width + buttonSize.width, Math.max(editorSize.height, 2 * buttonSize.height));
            return Utilities.addInsets(result, parent.getInsets());
        }

        public Dimension preferredLayoutSize(final Container parent) {
            Dimension editorSize = editor != null ? editor.getPreferredSize() : new Dimension();
            Dimension result = new Dimension(editorSize.width + buttonSize.width, Math.max(editorSize.height, 2 * buttonSize.height));
            return Utilities.addInsets(result, parent.getInsets());
        }

        public void removeLayoutComponent(final Component comp) {
        }
    }

    private static final String EDITOR_FIELD = "Editor";
    private static final String NEXT_FIELD = "Next";
    private static final String PREVIOUS_FIELD = "Previous";

    private Component editor;
    private Component nextButton;
    private Component previousButton;

    protected JSpinner spinner;
    private PropertyChangeListener changeListener;
    private ArrowButtonHandler buttonHandler = new ArrowButtonHandler();
    private Dimension buttonSize;
    private Insets buttonInsets;
    private boolean paintEditorBorder;

    public static ComponentUI createUI(final JComponent c) {
        return new BasicSpinnerUI();
    }

    public void installUI(final JComponent c) {
        spinner = (JSpinner)c;
        installDefaults();
        installListeners();
        installKeyboardActions();

        spinner.setLayout(createLayout());

        replaceEditor(null, createEditor());

        nextButton = createNextButton();
//        nextButton.setMinimumSize(buttonSize);
        spinner.add(NEXT_FIELD, nextButton);

        previousButton = createPreviousButton();
//        previousButton.setMinimumSize(buttonSize);
        spinner.add(PREVIOUS_FIELD, previousButton);
    }

    public void uninstallUI(final JComponent c) {
        uninstallKeyboardActions();
        uninstallListeners();
        uninstallDefaults();
        spinner.remove(editor);
        spinner.remove(nextButton);
        spinner.remove(previousButton);
        spinner = null;
    }

    protected void installListeners() {
        changeListener = createPropertyChangeListener();
        spinner.addPropertyChangeListener(changeListener);
    }

    protected void uninstallListeners() {
        spinner.removePropertyChangeListener(changeListener);
    }

    protected void installDefaults() {
        LookAndFeel.installBorder(spinner, "Spinner.border");
        LookAndFeel.installColorsAndFont(spinner, "Spinner.background", "Spinner.foreground", "Spinner.font");
        LookAndFeel.installProperty(spinner, "opaque", Boolean.TRUE);

        paintEditorBorder = UIManager.getBoolean("Spinner.editorBorderPainted");
        buttonSize = UIManager.getDimension("Spinner.arrowButtonSize");
        buttonInsets = UIManager.getInsets("Spinner.arrowButtonInsets");
    }

    protected void uninstallDefaults() {
        LookAndFeel.uninstallBorder(spinner);
        Utilities.uninstallColorsAndFont(spinner);
    }

    protected void installNextButtonListeners(final Component c) {
        if (!(c instanceof JButton)) {
            return;
        }
        JButton next = (JButton)c;
        next.addMouseListener(buttonHandler);
        next.addActionListener(buttonHandler);
        next.addFocusListener(buttonHandler);
    }

    protected void installPreviousButtonListeners(final Component c) {
        if (!(c instanceof JButton)) {
            return;
        }
        JButton previous = (JButton)c;
        previous.addMouseListener(buttonHandler);
        previous.addActionListener(buttonHandler);
        previous.addFocusListener(buttonHandler);
    }

    protected LayoutManager createLayout() {
        return new SpinnerLayout();
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler();
    }

    protected Component createPreviousButton() {
        BasicArrowButton previous = new BasicArrowButton(BasicArrowButton.SOUTH);
        installPreviousButtonListeners(previous);
        return previous;
    }

    protected Component createNextButton() {
        BasicArrowButton next = new BasicArrowButton(BasicArrowButton.NORTH);
        installNextButtonListeners(next);
        return next;
    }

    protected JComponent createEditor() {
        return spinner.getEditor();
    }

    protected void replaceEditor(final JComponent oldEditor, final JComponent newEditor) {
        if (oldEditor != null) {
            spinner.remove(oldEditor);
        }

        editor = newEditor;

        if (!paintEditorBorder && editor instanceof DefaultEditor) {
            ((DefaultEditor)editor).getTextField().setBorder(null);
        }
        spinner.add(newEditor, EDITOR_FIELD);
    }

    protected void installKeyboardActions() {
        BasicSpinnerKeyboardActions.installKeyboardActions(spinner);
    }

    private void uninstallKeyboardActions() {
        BasicSpinnerKeyboardActions.uninstallKeyboardActions(spinner);
    }
}
