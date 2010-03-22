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
package javax.swing.text;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Shape;
import java.awt.Toolkit;

import javax.swing.event.DocumentEvent;

public class LabelView extends GlyphView implements TabableView {
    private boolean outOfSync = true;
    private Font font;

    private Color background;
    private Color foreground;

    private boolean strikeThrough;
    private boolean subscript;
    private boolean superscript;
    private boolean underline;

    public LabelView(final Element element) {
        super(element);
    }

    public void changedUpdate(final DocumentEvent event, final Shape allocation,
                              final ViewFactory factory) {
        outOfSync = true;
        super.changedUpdate(event, allocation, factory);
    }

    public boolean isSuperscript() {
        lazySync();
        return superscript;
    }

    public boolean isSubscript() {
        lazySync();
        return subscript;
    }

    public boolean isStrikeThrough() {
        lazySync();
        return strikeThrough;
    }

    public boolean isUnderline() {
        lazySync();
        return underline;
    }

    public Font getFont() {
        lazySync();
        return font;
    }

    public Color getForeground() {
        lazySync();
        return foreground;
    }

    public Color getBackground() {
        lazySync();
        return background;
    }

    @Deprecated
    protected FontMetrics getFontMetrics() {
        lazySync();
        Component component = getComponent();
        if (component == null) {
            return Toolkit.getDefaultToolkit().getFontMetrics(font);
        }
        return component.getFontMetrics(font);
    }

    protected void setPropertiesFromAttributes() {
        outOfSync = false;

        font = super.getFont();
        foreground = super.getForeground();
        setBackground(super.getBackground());

        setStrikeThrough(super.isStrikeThrough());
        setSubscript(super.isSubscript());
        setSuperscript(super.isSuperscript());
        setUnderline(super.isUnderline());
    }

    protected void setBackground(final Color background) {
        this.background = background;
    }

    protected void setSubscript(final boolean subscript) {
        this.subscript = subscript;
    }

    protected void setSuperscript(final boolean superscript) {
        this.superscript = superscript;
    }

    protected void setStrikeThrough(final boolean strikeThrough) {
        this.strikeThrough = strikeThrough;
    }

    protected void setUnderline(final boolean underline) {
        this.underline = underline;
    }

    private void lazySync() {
        if (outOfSync) {
            setPropertiesFromAttributes();
        }
    }
}
