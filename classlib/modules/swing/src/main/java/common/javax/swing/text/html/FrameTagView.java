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
package javax.swing.text.html;

import java.awt.Component;
import java.awt.Insets;
import java.awt.Shape;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.ViewFactory;


class FrameTagView extends ComponentView {
    private JEditorPane editorPane;
    private JScrollPane scrollPane;
    private Border scrollPaneBorder;

    public FrameTagView(final Element elem) {
        super(elem);
    }

    protected Component createComponent() {
        editorPane = new JEditorPane();
        scrollPane = new JScrollPane(editorPane);

        loadContent(editorPane);
        updateAttributes();

        return scrollPane;
    }

    public void changedUpdate(final DocumentEvent e, final Shape s,
                              final ViewFactory f) {
        super.changedUpdate(e, s, f);

        updateAttributes();
    }

    private void updateAttributes() {
        if (scrollPane == null) {
            return;
        }

        installBorder();
        installMargin();
        installScrollBarPolicy();

        editorPane.setEditable(((JTextComponent)getContainer()).isEditable());
    }

    private void installBorder() {
        if (getFrameBorderAttr()) {
            if (scrollPane.getBorder() == null && scrollPaneBorder != null) {
                scrollPane.setBorder(scrollPaneBorder);
            }
        } else {
            scrollPaneBorder = scrollPane.getBorder();
            scrollPane.setBorder(null);
        }
    }

    private void installMargin() {
        int marginWidth = getMarginWidthAttr();
        int marginHeight = getMarginHeightAttr();
        editorPane.setMargin(new Insets(marginHeight, marginWidth,
                                        marginHeight, marginWidth));
    }

    private void installScrollBarPolicy() {
        String scrolling = getScrollingAttr();
        if ("yes".equals(scrolling)) {
            scrollPane.setHorizontalScrollBarPolicy(
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
            scrollPane.setVerticalScrollBarPolicy(
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        } else if ("no".equals(scrolling)) {
            scrollPane.setHorizontalScrollBarPolicy(
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setVerticalScrollBarPolicy(
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        }
    }

    private void loadContent(final JEditorPane pane) {
        try {
            pane.setPage(getSourceURL());
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private URL getSourceURL() {
        try {
            return new URL(((HTMLDocument)getDocument()).getBase(), getSrcAttr());
        } catch (final MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getSrcAttr() {
        return getAttributes().getAttribute(HTML.Attribute.SRC).toString();
    }

    private String getScrollingAttr() {
        Object value = getAttributes().getAttribute(HTML.Attribute.SCROLLING);
        return value == null ? "auto" : value.toString();
    }

    private boolean getFrameBorderAttr() {
        Object value = getAttributes().getAttribute(HTML.Attribute.FRAMEBORDER);
        return !"0".equals(value);
    }

    private int getMarginWidthAttr() {
        Object value = getAttributes().getAttribute(HTML.Attribute.MARGINWIDTH);
        return value == null ? 2 : ((Integer)value).intValue();
    }

    private int getMarginHeightAttr() {
        Object value = getAttributes().getAttribute(HTML.Attribute.MARGINHEIGHT);
        return value == null ? 0 : ((Integer)value).intValue();
    }
}
