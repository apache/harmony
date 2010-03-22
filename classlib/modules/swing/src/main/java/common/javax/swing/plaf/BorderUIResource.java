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
 * @author Sergey Burlak
 */

package javax.swing.plaf;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.io.Serializable;
import javax.swing.Icon;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class BorderUIResource implements Border, UIResource, Serializable {
    private static Border etched;
    private static Border blackLine;
    private static Border raisedBevel;
    private static Border loweredBevel;

    public static class BevelBorderUIResource extends BevelBorder implements UIResource {

        public BevelBorderUIResource(final int bevelType, final Color highlightOuter, final Color highlightInner, final Color shadowOuter, final Color shadowInner) {
            super(bevelType, highlightOuter, highlightInner, shadowOuter, shadowInner);
        }

        public BevelBorderUIResource(final int bevelType, final Color highlight, final Color shadow) {
            super(bevelType, highlight, shadow);
        }

        public BevelBorderUIResource(final int bevelType) {
            super(bevelType);
        }

    }

    public static class CompoundBorderUIResource extends CompoundBorder implements UIResource {

        public CompoundBorderUIResource(final Border out, final Border in) {
            super(out, in);
        }

    }

    public static class EmptyBorderUIResource extends EmptyBorder implements UIResource {

        public EmptyBorderUIResource(final Insets ins) {
            super(ins);
        }

        public EmptyBorderUIResource(final int top, final int left, final int bottom, final int right) {
            super(top, left, bottom, right);
        }
    }

    public static class EtchedBorderUIResource extends EtchedBorder implements UIResource {

        public EtchedBorderUIResource(final Color highlight, final Color shadow) {
            super(highlight, shadow);
        }

        public EtchedBorderUIResource(final int etchType, final Color highlight, final Color shadow) {
            super(etchType, highlight, shadow);
        }

        public EtchedBorderUIResource(final int etchType) {
            super(etchType);
        }

        public EtchedBorderUIResource() {
        }

    }

    public static class LineBorderUIResource extends LineBorder implements UIResource {

        public LineBorderUIResource(final Color color, final int thick) {
            super(color, thick);
        }

        public LineBorderUIResource(final Color color) {
            super(color);
        }

    }

    public static class MatteBorderUIResource extends MatteBorder implements UIResource {

        public MatteBorderUIResource(final Icon icon) {
            super(icon);
        }

        public MatteBorderUIResource(final int top, final int left, final int bottom, final int right, final Icon icon) {
            super(top, left, bottom, right, icon);
        }

        public MatteBorderUIResource(final int top, final int left, final int bottom, final int right, final Color color) {
            super(top, left, bottom, right, color);
        }

    }

    public static class TitledBorderUIResource extends TitledBorder implements UIResource {

        public TitledBorderUIResource(final Border border, final String title, final int justification, final int position, final Font font, final Color color) {
            super(border, title, justification, position, font, color);
        }

        public TitledBorderUIResource(final Border border, final String title, final int justification, final int position, final Font font) {
            super(border, title, justification, position, font);
        }

        public TitledBorderUIResource(final Border border, final String title, final int justification, final int position) {
            super(border, title, justification, position);
        }

        public TitledBorderUIResource(final Border border, final String title) {
            super(border, title);
        }

        public TitledBorderUIResource(final Border border) {
            super(border);
        }

        public TitledBorderUIResource(final String title) {
            super(title);
        }

    }

    private Border border;

    public BorderUIResource(final Border border) {
        if (border == null) {
            throw new IllegalArgumentException(Messages.getString("swing.6B")); //$NON-NLS-1$
        }
        this.border = border;
    }

    public Insets getBorderInsets(final Component c) {
        return border.getBorderInsets(c);
    }

    public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
        border.paintBorder(c, g, x, y, w, h);
    }

    public boolean isBorderOpaque() {
        return border.isBorderOpaque();
    }

    /**
     * Return raised bevel border
     * @return Border result
     */
    public static Border getRaisedBevelBorderUIResource() {
        if (raisedBevel == null) {
            raisedBevel = new BevelBorderUIResource(0);
        }
        return raisedBevel;
    }

    /**
     * Return lowered bevel border
     * @return Border result
     */
    public static Border getLoweredBevelBorderUIResource() {
        if (loweredBevel == null) {
            loweredBevel = new BevelBorderUIResource(1);
        }
        return loweredBevel;
    }

    /**
     * Return etched border
     * @return Border result
     */
    public static Border getEtchedBorderUIResource() {
        if (etched == null) {
            etched = new EtchedBorderUIResource(1);
        }
        return etched;
    }

    /**
     * Return black line border
     * @return Border result
     */
    public static Border getBlackLineBorderUIResource() {
        if (blackLine == null) {
            blackLine = new LineBorderUIResource(Color.black);
        }
        return blackLine;
    }
}


