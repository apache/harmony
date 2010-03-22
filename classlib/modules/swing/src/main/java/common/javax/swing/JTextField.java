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

import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import javax.swing.text.TextAction;
import javax.swing.text.View;

import org.apache.harmony.awt.ComponentInternals;
import org.apache.harmony.awt.text.PropertyNames;
import org.apache.harmony.awt.text.TextFieldKit;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * Note: <code>serialVersionUID</code> fields in this class and its inner
 * classes are added as a performance optimization but not as a guarantee of
 * correct deserialization of the classes. 
 */
public class JTextField extends JTextComponent implements SwingConstants {

    protected class AccessibleJTextField extends
            JTextComponent.AccessibleJTextComponent {

        private static final long serialVersionUID = -3980593114771538955L;

        protected AccessibleJTextField() {
            super();
        }

        @Override
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet ass = super.getAccessibleStateSet();
            ass.add(AccessibleState.SINGLE_LINE);
            return ass;
        }
    }

    public static final String notifyAction = "notify-field-accept";

    /**
     * This field is added as a performance optimization but not as
     * a guarantee of correct deserialization of the class. 
     */
    private static final long serialVersionUID = 6111025777502333651L;

    private int columns;

    private int columnWidth;

    private static final String uiClassID = "TextFieldUI";

    private static final TextAction AcceptAction = new NotifyAction(
            notifyAction);

    private transient String actionCommand;

    private transient int horizontalAlignment;

    private transient ActionEvent actionEvent;

    private transient Action action;

    private transient Action oldAction;

    private transient PropertyChangeListener listener;

    private transient int scrollOffset;

    private transient BoundedRangeModel boundedRangeModel;

    transient boolean scrollOffsetWasSet = false;

    final class ActionPropertyChangeListener implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent event) {
            String name = event.getPropertyName();
            Object newValue = event.getNewValue();
            if (name == "enabled") {
                setEnabled(((Boolean) newValue).booleanValue());
            }
            if (name == "ShortDescription") {
                setToolTipText((String) newValue);
            }
        }
    }

    static class NotifyAction extends TextAction {
        private static final long serialVersionUID = 7892443630033381907L;

        public NotifyAction(final String name) {
            super(name);
        }

        public void actionPerformed(final ActionEvent a) {
            final JTextComponent focused = getFocusedComponent();
            if (!(focused instanceof JTextField)) {
                return;
            }

            ((JTextField)focused).postActionEvent();
        }

        @Override
        public boolean isEnabled() {
            final JTextComponent focused = getFocusedComponent();
            if (!(focused instanceof JTextField)) {
                return false;
            }

            JTextField textField = (JTextField)focused;
            return textField.getActionListeners().length > 0;
        }
    }

    class TextFieldKitImpl implements TextFieldKit {
        public int getHorizontalAlignment() {
            return JTextField.this.getHorizontalAlignment();
        }

        public BoundedRangeModel getHorizontalVisibility() {
            return JTextField.this.getHorizontalVisibility();
        }

        public boolean echoCharIsSet() {
            return false;
        }

        public char getEchoChar() {
            return '\0';
        }

        public Insets getInsets() {
            return JTextField.this.getInsets();
        }

        public ComponentOrientation getComponentOrientation() {
            return JTextField.this.getComponentOrientation();
        }
    }

    final class ModelChangeListener implements ChangeListener {
        public void stateChanged(final ChangeEvent e) {
            scrollOffset = boundedRangeModel.getValue();
        }
    }

    public JTextField() {
        this(null, null, 0);
    }

    public JTextField(final int c) {
        this(null, null, c);
    }

    public JTextField(final String text) {
        this(null, text, 0);
    }

    public JTextField(final String text, final int c) {
        this(null, text, c);
    }

    public JTextField(final Document doc, final String text, final int c) {
        super();
        if (c < 0) {
            throw new IllegalArgumentException(Messages.getString("swing.45")); //$NON-NLS-1$
        }
        Document document = doc;
        if (doc == null) {
            document = createDefaultModel();
        }

        setDocument(document);
        if (text != null) {
            try {
                document.remove(0, document.getLength());
                document.insertString(0, text, null);
            } catch (final BadLocationException e) {
            }
        }

        columns = c;
        evaluate(getFont());
        actionCommand = null;
        horizontalAlignment = LEADING;
        action = null;
        oldAction = null;
        installTextKit();
    }

    void installTextKit() {
        ComponentInternals.getComponentInternals()
            .setTextFieldKit(this, new TextFieldKitImpl());
    }

    public void addActionListener(final ActionListener actionListener) {
        listenerList.add(ActionListener.class, actionListener);
    }

    private String alignmentToString(final int alignment) {
        switch (alignment) {
        case LEFT:
            return "LEFT";
        case CENTER:
            return "CENTER";
        case RIGHT:
            return "RIGHT";
        case LEADING:
            return "LEADING";
        case TRAILING:
            return "TRAILING";
        default:
            return null;
        }
    }

    protected void configurePropertiesFromAction(final Action a) {
        if (a == null) {
            setEnabled(true);
            setToolTipText(null);
            return;
        }
        setEnabled(a.isEnabled());
        String toolTipText = (String) a.getValue(Action.SHORT_DESCRIPTION);
        setToolTipText(toolTipText);
    }

    protected PropertyChangeListener createActionPropertyChangeListener(
                                                              final Action a) {
        return new ActionPropertyChangeListener();
    }

    final BoundedRangeModel createBoundedRangeModel() {
        int prefWidth = (int)getUI().getRootView(this).
            getPreferredSpan(View.X_AXIS);
        int value = getMaxScrollOffset();
        int max = Math.max(prefWidth, value);
        return new DefaultBoundedRangeModel(value, max - value, 0, max);
    }

    protected Document createDefaultModel() {
        return new PlainDocument();
    }

    private void evaluate(final Font f) {
        if (f != null) {
            FontMetrics fm = getFontMetrics(f);
            columnWidth = fm.charWidth('m');
        } else {
            columnWidth = 0;
        }
    }

    protected void fireActionPerformed() {
        String command = actionCommand == null ? getText() : actionCommand;
        actionEvent = new ActionEvent(this,
                ActionEvent.ACTION_PERFORMED, command);
        ActionListener[] listeners = getActionListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].actionPerformed(actionEvent);
        }

    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJTextField();
        }
        return accessibleContext;
    }

    public Action getAction() {
        return action;
    }

    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    @Override
    public Action[] getActions() {
        Action[] editorKitActions = ((TextUI) ui).getEditorKit(this)
                .getActions();
        int length = editorKitActions.length;
        Action[] actions = new Action[length + 1];
        System.arraycopy(editorKitActions, 0, actions, 0, length);
        actions[length] = AcceptAction;
        return actions;
    }

    public int getColumns() {
        return columns;
    }

    protected int getColumnWidth() {
        return columnWidth;
    }

    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public BoundedRangeModel getHorizontalVisibility() {
        if (boundedRangeModel == null) {
            boundedRangeModel = createBoundedRangeModel();
            boundedRangeModel.addChangeListener(new ModelChangeListener());
        }
        return boundedRangeModel;
    }

    final int getMaxScrollOffset() {
        int prefWidth = getPreferredSize().width;
        int width = getWidth();
        int diff = prefWidth - width;
        return (diff >= 0) ? diff + 1 : 0;
    }

    @Override
    public Dimension getPreferredSize() {
        int widthColumns = columns * columnWidth;
        Dimension dim = super.getPreferredSize();
        int width = (columns == 0) ? dim.width : widthColumns;
        return new Dimension(width, dim.height);
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    @Override
    public String getUIClassID() {
        return uiClassID;
    }

    @Override
    public boolean isValidateRoot() {
        Container parent = getParent();
        return parent == null || !(parent instanceof JViewport);
    }

    /*
     * The format of the string is based on 1.5 release behavior
     * which can be revealed using the following code:
     *
     *     Object obj = new JTextField();
     *     System.out.println(obj.toString());
     */
    @Override
    protected String paramString() {
        return super.paramString() + "," + "columns=" + getColumns() + ","
                + "columnWidth=" + getColumnWidth() + "," + "command="
                + actionCommand + "," + "horizontalAlignment="
                + alignmentToString(getHorizontalAlignment());
    }

    public void postActionEvent() {
        fireActionPerformed();
    }

    public void removeActionListener(final ActionListener actionListener) {
        listenerList.remove(ActionListener.class, actionListener);
    }

    @Override
    public void scrollRectToVisible(final Rectangle r) {
        int x = r.x;
        Insets insets = getInsets();
        BoundedRangeModel brm = getHorizontalVisibility();
        int oldValue = brm.getValue();
        int width = getVisibleRect().width;

        if (x > width - insets.right) {
            brm.setValue(oldValue + (x - width + insets.right) + 2);
            repaint();
        }
        if (x < insets.left) {
            brm.setValue(oldValue - (insets.left - x) - 2);
            repaint();
        }
    }

    public void setAction(final Action a) {
        oldAction = action;
        action = a;
        configurePropertiesFromAction(a);
        if (oldAction != null) {
            oldAction.removePropertyChangeListener(listener);
        }
        if (a != null) {
            listener = createActionPropertyChangeListener(a);
            a.addPropertyChangeListener(listener);
        }

        ActionListener[] listeners = getActionListeners();
        boolean isNew = true;
        int length = listeners.length;

        removeActionListener(oldAction);
        if (a == null) {
            return;
        }
        for (int i = 0; i < length; i++) {
            if (listeners[i] == a) {
                isNew = false;
                break;
            }
        }
        if (isNew) {
            addActionListener(a);
        }
    }

    public void setActionCommand(final String command) {
        actionCommand = command;

    }

    public void setColumns(final int c) {
        if (c < 0) {
            throw new IllegalArgumentException(Messages.getString("swing.45")); //$NON-NLS-1$
        }
        columns = c;
        invalidate();

    }

    @Override
    public void setDocument(final Document doc) {
        super.setDocument(doc);
        if (doc != null) {
            doc.putProperty(PropertyNames.FILTER_NEW_LINES, Boolean.TRUE);
        }
    }

    @Override
    public void setFont(final Font f) {
        super.setFont(f);
        evaluate(f);
        revalidate();
    }

    public void setHorizontalAlignment(final int alignment) {
        if (alignment != LEFT && alignment != RIGHT && alignment != CENTER
                && alignment != LEADING && alignment != TRAILING) {
            throw new IllegalArgumentException("horizontalAlignment"); //$NON-NLS-1$
        }
        int old = horizontalAlignment;
        horizontalAlignment = alignment;
        LookAndFeel.markPropertyNotInstallable(this, "horizontalAlignment");
        firePropertyChange("horizontalAlignment", old, horizontalAlignment);
    }

    public void setScrollOffset(final int scrOffset) {
        scrollOffsetWasSet = true;
        LookAndFeel.markPropertyNotInstallable(this, "scrollOffset");
        scrollOffset = Math.min(Math.max(0, scrOffset), getMaxScrollOffset());
        getHorizontalVisibility().setValue(scrollOffset);
    }

}
