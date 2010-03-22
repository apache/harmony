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
package javax.swing.text;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import org.apache.harmony.awt.text.TextUtils;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * <p>
 * <i>StyledEditorKit</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.
 * <em>This applies to nested classes as well</em>.</li>
 * </ul>
 */
public class StyledEditorKit extends DefaultEditorKit {

    public static class AlignmentAction extends StyledTextAction {
        private static final long serialVersionUID = 1L;

        private final Object defaultValue;

        public AlignmentAction(final String name, final int alignment) {
            super(name);
            defaultValue = new Integer(alignment);
        }

        public void actionPerformed(final ActionEvent event) {
            JEditorPane pane = getEditorPane(event);
            if (pane == null) {
                return;
            }
            MutableAttributeSet attr = new SimpleAttributeSet();
            Object newValue = null;
            if (event != null) {
                try {
                    newValue = new Integer(event.getActionCommand());
                } catch (NumberFormatException e) {
                }
            }
            newValue = (newValue != null) ? newValue : defaultValue;
            attr.addAttribute(StyleConstants.Alignment, newValue);
            setParagraphAttributes(pane, attr, false);
        }
    }

    public static class BoldAction extends StyledTextAction {
        private static final long serialVersionUID = 1L;

        public BoldAction() {
            super("font-bold");
        }

        public void actionPerformed(final ActionEvent event) {
            performAction(event, StyleConstants.Bold, null, null, null, true);
        }
    }

    public static class FontFamilyAction extends StyledTextAction {
        private static final long serialVersionUID = 1L;

        private final Object defaultValue;

        public FontFamilyAction(final String name, final String family) {
            super(name);
            defaultValue = family;
        }

        public void actionPerformed(final ActionEvent event) {
            Object newValue = null;
            if (event != null) {
                try {
                    newValue = event.getActionCommand();
                } catch (NumberFormatException e) {
                }
            }
            performAction(event, StyleConstants.FontFamily, null, defaultValue,
                          newValue, false);
        }
    }

    public static class FontSizeAction extends StyledTextAction {
        private static final long serialVersionUID = 1L;

        private final Object defaultValue;

        public FontSizeAction(final String name, final int size) {
            super(name);
            defaultValue = new Integer(size);
        }

        public void actionPerformed(final ActionEvent event) {
            Object newValue = null;
            if (event != null) {
                try {
                    newValue = new Integer(event.getActionCommand());
                } catch (NumberFormatException e) {
                }
            }
            performAction(event, StyleConstants.FontSize, null, defaultValue,
                          newValue, false);
        }

    }

    public static class ForegroundAction extends StyledTextAction {
        private static final long serialVersionUID = 1L;

        private final Object defaultValue;

        public ForegroundAction(final String name, final Color color) {
            super(name);
            defaultValue = color;
        }

        public void actionPerformed(final ActionEvent event) {
            Object newValue = null;
            if (event !=  null) {
                try {
                    newValue = Color.decode(event.getActionCommand());
                } catch (NumberFormatException e) {
                }
            }
            performAction(event, StyleConstants.Foreground, null, defaultValue,
                          newValue, false);
        }
    }

    public static class ItalicAction extends StyledTextAction {
        private static final long serialVersionUID = 1L;

        public ItalicAction() {
            super("font-italic");
        }

        public void actionPerformed(final ActionEvent event) {
            performAction(event, StyleConstants.Italic, null, null, null, true);
        }
    }

    public abstract static class StyledTextAction extends TextAction {
        private static final String documentExceptionMessage =
            Messages.getString("swing.48","StyledDocument"); //$NON-NLS-1$ //$NON-NLS-2$
        private static final String editorKitExceptionMessage =
            Messages.getString("swing.49","StyledEditorKit"); //$NON-NLS-1$ //$NON-NLS-2$

        public StyledTextAction(final String name) {
            super(name);
        }

        protected final JEditorPane getEditor(final ActionEvent event) {
            if (event == null) {
                return getFocusedEditorPane();
            }
            Object source = event.getSource();
            if (source instanceof JEditorPane) {
                return (JEditorPane)source;
            } else {
                return getFocusedEditorPane();
            }
        }

        final JEditorPane getFocusedEditorPane() {
            JTextComponent textComponent = getFocusedComponent();
            return (textComponent instanceof JEditorPane)
                ?  (JEditorPane)textComponent : null;

        }

        final JEditorPane getEditorPane(final ActionEvent e) {
            JEditorPane pane = getEditor(e);
            return (pane != null && pane.isEditable()) ? pane : null;
        }

        protected final void setCharacterAttributes(final JEditorPane c,
                final AttributeSet set,
                final boolean replace) {
            int selectionStart = c.getSelectionStart();
            int selectionEnd = c.getSelectionEnd();
            getStyledDocument(c).setCharacterAttributes(selectionStart,
                                       selectionEnd - selectionStart,
                                       set, replace);

            MutableAttributeSet atts = getStyledEditorKit(c).getInputAttributes();
            if (replace) {
                atts.removeAttributes(atts.getAttributeNames());
            }
            atts.addAttributes(set);
        }

        final AttributeSet getAttributeSetByOffset(final Document doc,
                                                   final int offset) {
            Element elem = getElementByOffset(doc, offset);
            return (elem == null) ? null : elem.getAttributes();
        }

        final Boolean getNewValue(final JEditorPane pane,
                                  final Object key) {
            StyledEditorKit kit = getStyledEditorKit(pane);
            Object oldValue = kit.getInputAttributes().getAttribute(key);
            return (oldValue instanceof Boolean)
                    ? Boolean.valueOf(!((Boolean)oldValue).booleanValue())
                    : Boolean.TRUE;

        }

        protected final StyledDocument getStyledDocument(final JEditorPane c) {
            Document doc = c.getDocument();
            if (!(doc instanceof StyledDocument)) {
                throw new IllegalArgumentException(documentExceptionMessage);
            }
            return (StyledDocument)doc;
        }

        protected final StyledEditorKit getStyledEditorKit(final JEditorPane
                                                           c) {
            EditorKit kit = c.getEditorKit();
            if (!(kit instanceof StyledEditorKit)) {
                throw new IllegalArgumentException(editorKitExceptionMessage);
            }
            return (StyledEditorKit)kit;
        }

        final void performAction(final ActionEvent event,
                                 final Object attribute,
                                 final MutableAttributeSet set,
                                 final Object defaultValue,
                                 final Object newValue,
                                 final boolean isToggleAction) {
            JEditorPane pane = getEditorPane(event);
            if (pane == null) {
                return;
            }
            MutableAttributeSet attr = (set == null) ? new SimpleAttributeSet()
                    : set;
            Object value;
            if (isToggleAction) {
                value = getNewValue(pane, attribute);
            } else {
                value = (newValue != null) ? newValue : defaultValue;
            }
            attr.addAttribute(attribute, value);
            setCharacterAttributes(pane, attr, false);
        }

        protected final void setParagraphAttributes(final JEditorPane c,
                final AttributeSet set,
                final boolean replace) {

            TextUtils.setParagraphAttributes(set, replace, c,
                                             getStyledDocument(c));
        }


    }

    public static class UnderlineAction extends StyledTextAction {
        private static final long serialVersionUID = 1L;

        public UnderlineAction() {
            super("font-underline");
        }

        public void actionPerformed(final ActionEvent event) {
            performAction(event, StyleConstants.Underline, null, null, null,
                          true);
        }
    }

    static final class ViewFactoryImpl implements ViewFactory {
        public View create(final Element element) {
//            if (true) {
//                throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
//            }
//            return null;
            //This code committed-out temporarily (while these views is not
            //implemented)
            String name = element.getName();
            if (AbstractDocument.ParagraphElementName.equals(name)) {
                return new ParagraphView(element);
            } else if (AbstractDocument.SectionElementName.equals(name)) {
                return new BoxView(element, View.Y_AXIS);
            } else if (StyleConstants.ComponentElementName.equals(name)) {
                return new ComponentView(element);
            } else if (StyleConstants.IconElementName.equals(name)) {
                return new IconView(element);
            } else {
                return new LabelView(element);
            }
        }
    }

    private class CaretListenerImpl implements CaretListener {
        int dot;
        public void caretUpdate(final CaretEvent ce) {
            int newDot = ce.getDot();
            if (newDot != dot) {
                dot = newDot;
                updateInputAttributes(dot);
            }
        }
        final void componentChanged() {
            dot = editorPane.getCaretPosition();
            updateInputAttributes(dot);
        }
    }

    private static final long serialVersionUID = 1L;

    private static final ViewFactory factory = new ViewFactoryImpl();

    private static Action[] actions;

    private JEditorPane editorPane;

    private CaretListenerImpl caretListener;

    private final MutableAttributeSet inputAttributes = new SimpleAttributeSet();


    public StyledEditorKit() {
        createStaticActions();
    }

    @Override
    public Action[] getActions() {
        return actions.clone();
    }

    @Override
    public Document createDefaultDocument() {
        return new DefaultStyledDocument();
    }

    protected void createInputAttributes(final Element element,
                                         final MutableAttributeSet set) {
        AttributeSet as = element.getAttributes();
        set.removeAttributes(set);
        for (Enumeration keys = as.getAttributeNames();
             keys.hasMoreElements();) {
            Object key = keys.nextElement();
            if (!StyleConstants.IconAttribute.equals(key)
                && !StyleConstants.ComponentAttribute.equals(key)
                && !AbstractDocument.ElementNameAttribute.equals(key)) {
                set.addAttribute(key, as.getAttribute(key));
            }
        }
    }

    @Override
    public void deinstall(final JEditorPane component) {
        if (component == editorPane) {
            if (editorPane != null && caretListener != null) {
                editorPane.removeCaretListener(caretListener);
            }
            editorPane = null;
        }
    }

    private Action[] createStaticActions() {
        if (actions == null) {
            Action[] styledActions = getDefaultActions();
            int styledActionsCount = styledActions.length;
            Action[] superActions = super.getActions();
            int superActionsCount = superActions.length;
            actions = new Action[styledActionsCount + superActionsCount];
            System.arraycopy(styledActions, 0, actions, 0, styledActionsCount);
            System.arraycopy(superActions, 0, actions, styledActionsCount,
                             superActionsCount);
        }
        return actions;
    }

    public Element getCharacterAttributeRun() {
        if (editorPane == null) {
            return null;
        }
        return getElement();
    }

    private Action[] getDefaultActions() {
        return new Action[] {
                new StyledEditorKit.FontSizeAction("font-size-48", 48),
                new StyledEditorKit.FontSizeAction("font-size-36", 36),
                new StyledEditorKit.FontSizeAction("font-size-24", 24),
                new StyledEditorKit.FontSizeAction("font-size-18", 18),
                new StyledEditorKit.FontSizeAction("font-size-16", 16),
                new StyledEditorKit.FontSizeAction("font-size-14", 14),
                new StyledEditorKit.FontSizeAction("font-size-12", 12),
                new StyledEditorKit.FontSizeAction("font-size-10", 10),
                new StyledEditorKit.FontSizeAction("font-size-8", 8),
                new StyledEditorKit.FontFamilyAction("font-family-SansSerif",
                                                     "SansSerif"),
                new StyledEditorKit.FontFamilyAction("font-family-Serif",
                                                     "Serif"),
                new StyledEditorKit.FontFamilyAction("font-family-Monospaced",
                                                     "Monospaced"),
                new StyledEditorKit.BoldAction(),
                new StyledEditorKit.UnderlineAction(),
                new StyledEditorKit.ItalicAction(),
                new StyledEditorKit.AlignmentAction("right-justify",
                                                    StyleConstants.ALIGN_RIGHT),
                new StyledEditorKit.AlignmentAction("left-justify",
                                                    StyleConstants.ALIGN_LEFT),
                new StyledEditorKit.AlignmentAction("center-justify",
                                                    StyleConstants.
                                                    ALIGN_CENTER),
                new StyledEditorKit.ForegroundAction("font-foreground",
                                                     Color.BLACK)};
    }

    public MutableAttributeSet getInputAttributes() {
        return inputAttributes;
    }

    private Element getElement() {
        int dot = editorPane.getCaretPosition();
        return getElementByOffset(editorPane.getDocument(), dot);
    }

    private static Element getElementByOffset(final Document doc,
                                              final int offset) {
        if (doc == null) {
            return null;
        }
        int pos = offset;
        Element elem = doc.getDefaultRootElement();
        while (elem.getElementCount() > 0) {
            Element tmp = elem.getElement(elem.getElementIndex(pos));
            elem = tmp.getElementCount() > 0 ? tmp :
                elem.getElement(elem.getElementIndex(Math.max(0,pos - 1)));
        }
        return elem;
    }

    @Override
    public ViewFactory getViewFactory() {
        return factory;
    }

    @Override
    public void install(final JEditorPane component) {
        editorPane = component;
        if (caretListener == null) {
            caretListener = new CaretListenerImpl();
        }
        if (editorPane != null) {
            editorPane.addCaretListener(caretListener);
            caretListener.componentChanged();
        }
    }

    private void updateInputAttributes(final int dot) {
        inputAttributes.removeAttributes(inputAttributes);
        Element element = getElementByOffset(editorPane.getDocument(), dot);
        createInputAttributes(element, inputAttributes);
    }

    static final void writeLock(final Document doc) {
        if (doc instanceof AbstractDocument) {
            ((AbstractDocument)doc).writeLock();
        }
    }

    static final void writeUnlock(final Document doc) {
        if (doc instanceof AbstractDocument) {
            ((AbstractDocument)doc).writeUnlock();
        }
    }
}



