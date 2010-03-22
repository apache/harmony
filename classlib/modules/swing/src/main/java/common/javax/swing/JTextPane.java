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
 * @author Roman I. Chernyatchik
 */
package javax.swing;

import java.awt.Component;
import java.awt.MenuContainer;
import java.awt.image.ImageObserver;
import java.io.Serializable;

import javax.accessibility.Accessible;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;

import org.apache.harmony.awt.text.TextUtils;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class JTextPane extends JEditorPane implements ImageObserver,
        MenuContainer, Serializable, Accessible, Scrollable {

    private static final String uiClassID = "TextPaneUI";

    public JTextPane() {
        setEditorKit(new StyledEditorKit());
    }

    public JTextPane(final StyledDocument doc) {
        this();

        if (doc == null) {
            throw new NullPointerException();
        } 
    }

    public String getUIClassID() {
        return uiClassID;
    }

    public void setDocument(final Document doc) {
        if (doc instanceof StyledDocument) {
            super.setDocument(doc);
        } else {
            throw new IllegalArgumentException(Messages.getString("swing.48", "StyledDocument")); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    public void setStyledDocument(final StyledDocument doc) {
        super.setDocument(doc);
    }

    public StyledDocument getStyledDocument() {
        return (StyledDocument)getDocument();
    }

    /**
     * This method differs from JEditorPane.replaceSelection only in one case.
     * If there is selection the replacement text should have not the attributes
     * currently defined for input, but the attributes of the first selected
     * symbol.
     */
    public void replaceSelection(final String content) {
        if (!isEditable()) {
            new DefaultEditorKit.BeepAction().actionPerformed(null);
            return;
        }

        final int start = getSelectionStart();
        final int end = getSelectionEnd();
        final StyledDocument doc = getStyledDocument();

        AttributeSet attrs;

        try {
            if (start != end) {
                attrs = doc.getCharacterElement(start).getAttributes();

                doc.remove(start, end - start);

                if (StyleConstants.getIcon(attrs) != null) {
                    final MutableAttributeSet newAttrs =
                        new SimpleAttributeSet(attrs);
                    newAttrs.removeAttribute(StyleConstants.IconAttribute);
                    newAttrs.removeAttribute(AbstractDocument
                                             .ElementNameAttribute);
                    attrs = newAttrs;
                }
                if (StyleConstants.getComponent(attrs) != null) {
                    final MutableAttributeSet newAttrs =
                        new SimpleAttributeSet(attrs);
                    newAttrs.removeAttribute(StyleConstants.ComponentAttribute);
                    newAttrs.removeAttribute(AbstractDocument
                                             .ElementNameAttribute);
                    attrs = newAttrs;
                }

            } else {
                attrs = getInputAttributes();
            }

            if (content != null) {
                doc.insertString(start, content, attrs);
            }
        } catch (BadLocationException e) {
        }
    }

   /**
    * To insert component we should insert in the document whitespace with
    * special attribute StyleConstants.ComponentAttribute
    */
    public synchronized void insertComponent(final Component c) {
        final MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setComponent(attrs, c);

        replaceObject(getStyledDocument(),
                     getSelectionStart(),
                     getSelectionEnd(),
                     attrs);
    }

    /**
     * To insert icon we should insert in the document whitespace with
     * special attribute StyleConstants.IconAttribute
     */
    public synchronized void insertIcon(final Icon g) {
        final MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setIcon(attrs, g);

        replaceObject(getStyledDocument(),
                getSelectionStart(),
                getSelectionEnd(),
                attrs);
    }

    public Style addStyle(final String styleName, final Style parent) {
        return getStyledDocument().addStyle(styleName, parent);
    }

    public void removeStyle(final String styleName) {
        getStyledDocument().removeStyle(styleName);
    }

    public Style getStyle(final String styleName) {
        return getStyledDocument().getStyle(styleName);
    }

    public void setLogicalStyle(final Style s) {
        getStyledDocument().setLogicalStyle(getCaretPosition(), s);
    }

    public Style getLogicalStyle() {
        final int position = getCaretPosition();
        if (position < 0) {
            return null;
        }
        return getStyledDocument().getLogicalStyle(position);
    }

    public AttributeSet getCharacterAttributes() {
        final int position = getCaretPosition();
        if (position < 0) {
            return null;
        }
        return this.getStyledDocument().getCharacterElement(position)
                .getAttributes();
    }

    public synchronized void setCharacterAttributes(final AttributeSet attr,
            final boolean replace) {

         TextUtils.setCharacterAttributes(attr, replace, this,
                                          getStyledDocument(),
                                          getInputAttributes());
    }

    public AttributeSet getParagraphAttributes() {
        final int position = getCaretPosition();
        if (position < 0) {
            return null;
        }
        return getStyledDocument().getParagraphElement(position)
                .getAttributes();
    }

    public synchronized void setParagraphAttributes(final AttributeSet attr,
            final boolean replace) {

        TextUtils.setParagraphAttributes(attr, replace, this,
                                         getStyledDocument());
    }

    public final void setEditorKit(final EditorKit kit) {
        if (kit instanceof StyledEditorKit) {
            super.setEditorKit(kit);
        } else {
            throw new IllegalArgumentException(Messages.getString("swing.49","StyledEditorKit")); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    public MutableAttributeSet getInputAttributes() {
        return getStyledEditorKit().getInputAttributes();
    }

    protected EditorKit createDefaultEditorKit() {
        return new StyledEditorKit();
    }

    protected final StyledEditorKit getStyledEditorKit() {
        return (StyledEditorKit) getEditorKit();
    }

    private void replaceObject(final StyledDocument doc, final int selectStart,
            final int selectEnd, final MutableAttributeSet attrs) {
        try {
            if (selectStart != selectEnd) {
                doc.remove(selectStart, selectEnd - selectStart);
            }
            doc.insertString(selectStart, " ", attrs);
        } catch (BadLocationException e) {
        }
    }
}
