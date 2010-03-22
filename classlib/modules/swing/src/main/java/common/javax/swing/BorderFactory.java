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

package javax.swing;

import java.awt.Color;
import java.awt.Font;

import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;

public class BorderFactory {

    static final Border sharedRaisedBevel = new BevelBorder(BevelBorder.RAISED);
    static final Border sharedLoweredBevel = new BevelBorder(BevelBorder.LOWERED);
    static final Border sharedEtchedBorder = new EtchedBorder(EtchedBorder.LOWERED);
    static final Border emptyBorder = new EmptyBorder(0, 0, 0, 0);

    private BorderFactory() {
        super();
    }

    public static TitledBorder createTitledBorder(final Border border, final String title, final int titleJustification, final int titlePosition, final Font titleFont, final Color titleColor) {
        return new TitledBorder(border, title, titleJustification, titlePosition, titleFont, titleColor);
    }

    public static Border createBevelBorder(final int type, final Color highlightOuter, final Color highlightInner, final Color shadowOuter, final Color shadowInner) {
        return new BevelBorder(type, highlightOuter, highlightInner, shadowOuter, shadowInner);
    }

    public static TitledBorder createTitledBorder(final Border border, final String title, final int titleJustification, final int titlePosition, final Font titleFont) {
        return new TitledBorder(border, title, titleJustification, titlePosition, titleFont);
    }

    public static TitledBorder createTitledBorder(final Border border, final String title, final int titleJustification, final int titlePosition) {
        return new TitledBorder(border, title, titleJustification, titlePosition);
    }

    public static TitledBorder createTitledBorder(final Border border, final String title) {
        return new TitledBorder(border, title);
    }

    public static CompoundBorder createCompoundBorder(final Border outside, final Border inside) {
        return new CompoundBorder(outside, inside);
    }

    public static Border createEtchedBorder(final Color highlight, final Color shadow) {
        return new EtchedBorder(highlight, shadow);
    }

    public static Border createEtchedBorder(final int type, final Color highlight, final Color shadow) {
        return new EtchedBorder(type, highlight, shadow);
    }

    public static Border createBevelBorder(final int type, final Color highlight, final Color shadow) {
        return new BevelBorder(type, highlight, shadow);
    }

    public static TitledBorder createTitledBorder(final Border border) {
        return new TitledBorder(border);
    }

    public static TitledBorder createTitledBorder(final String title) {
        return new TitledBorder(title);
    }

    public static MatteBorder createMatteBorder(final int top, final int left, final int bottom, final int right, final Icon tileIcon) {
        return new MatteBorder(top, left, bottom, right, tileIcon);
    }

    public static MatteBorder createMatteBorder(final int top, final int left, final int bottom, final int right, final Color color) {
        return new MatteBorder(top, left, bottom, right, color);
    }

    public static Border createLineBorder(final Color color, final int thickness) {
        return new LineBorder(color, thickness);
    }

    public static Border createLineBorder(final Color color) {
        return new LineBorder(color);
    }

    public static CompoundBorder createCompoundBorder() {
        return new CompoundBorder();
    }

    public static Border createEmptyBorder(final int top, final int left, final int bottom, final int right) {
        return new EmptyBorder(top, left, bottom, right);
    }

    public static Border createEtchedBorder(final int type) {
        return new EtchedBorder(type);
    }

    public static Border createBevelBorder(final int type) {
        return new BevelBorder(type);
    }

    public static Border createRaisedBevelBorder() {
        return new BevelBorder(BevelBorder.RAISED);
    }

    public static Border createLoweredBevelBorder() {
        return new BevelBorder(BevelBorder.LOWERED);
    }

    public static Border createEtchedBorder() {
        return new EtchedBorder();
    }

    public static Border createEmptyBorder() {
        return new EmptyBorder(0, 0, 0, 0);
    }
}
