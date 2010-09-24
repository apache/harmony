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
 * @author Pavel Dolgov
 */
package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.apache.harmony.awt.FieldsAccessor;


public final class SystemColor extends Color implements Serializable {

    private static final long serialVersionUID = 4503142729533789064L;

    public static final int DESKTOP = 0;

    public static final int ACTIVE_CAPTION = 1;

    public static final int ACTIVE_CAPTION_TEXT = 2;

    public static final int ACTIVE_CAPTION_BORDER = 3;

    public static final int INACTIVE_CAPTION = 4;

    public static final int INACTIVE_CAPTION_TEXT = 5;

    public static final int INACTIVE_CAPTION_BORDER = 6;

    public static final int WINDOW = 7;

    public static final int WINDOW_BORDER = 8;

    public static final int WINDOW_TEXT = 9;

    public static final int MENU = 10;

    public static final int MENU_TEXT = 11;

    public static final int TEXT = 12;

    public static final int TEXT_TEXT = 13;

    public static final int TEXT_HIGHLIGHT = 14;

    public static final int TEXT_HIGHLIGHT_TEXT = 15;

    public static final int TEXT_INACTIVE_TEXT = 16;

    public static final int CONTROL = 17;

    public static final int CONTROL_TEXT = 18;

    public static final int CONTROL_HIGHLIGHT = 19;

    public static final int CONTROL_LT_HIGHLIGHT = 20;

    public static final int CONTROL_SHADOW = 21;

    public static final int CONTROL_DK_SHADOW = 22;

    public static final int SCROLLBAR = 23;

    public static final int INFO = 24;

    public static final int INFO_TEXT = 25;

    public static final int NUM_COLORS = 26;

    public static final SystemColor desktop = new SystemColor(DESKTOP);

    public static final SystemColor activeCaption = new SystemColor(ACTIVE_CAPTION);

    public static final SystemColor activeCaptionText = new SystemColor(ACTIVE_CAPTION_TEXT);

    public static final SystemColor activeCaptionBorder = new SystemColor(ACTIVE_CAPTION_BORDER);

    public static final SystemColor inactiveCaption = new SystemColor(INACTIVE_CAPTION);

    public static final SystemColor inactiveCaptionText = new SystemColor(INACTIVE_CAPTION_TEXT);

    public static final SystemColor inactiveCaptionBorder = new SystemColor(INACTIVE_CAPTION_BORDER);

    public static final SystemColor window = new SystemColor(WINDOW);

    public static final SystemColor windowBorder = new SystemColor(WINDOW_BORDER);

    public static final SystemColor windowText = new SystemColor(WINDOW_TEXT);

    public static final SystemColor menu = new SystemColor(MENU);

    public static final SystemColor menuText = new SystemColor(MENU_TEXT);

    public static final SystemColor text = new SystemColor(TEXT);

    public static final SystemColor textText = new SystemColor(TEXT_TEXT);

    public static final SystemColor textHighlight = new SystemColor(TEXT_HIGHLIGHT);

    public static final SystemColor textHighlightText = new SystemColor(TEXT_HIGHLIGHT_TEXT);

    public static final SystemColor textInactiveText = new SystemColor(TEXT_INACTIVE_TEXT);

    public static final SystemColor control = new SystemColor(CONTROL);

    public static final SystemColor controlText = new SystemColor(CONTROL_TEXT);

    public static final SystemColor controlHighlight = new SystemColor(CONTROL_HIGHLIGHT);

    public static final SystemColor controlLtHighlight = new SystemColor(CONTROL_LT_HIGHLIGHT);

    public static final SystemColor controlShadow = new SystemColor(CONTROL_SHADOW);

    public static final SystemColor controlDkShadow = new SystemColor(CONTROL_DK_SHADOW);

    public static final SystemColor scrollbar = new SystemColor(SCROLLBAR);

    public static final SystemColor info = new SystemColor(INFO);

    public static final SystemColor infoText = new SystemColor(INFO_TEXT);

    private final transient Toolkit toolkit = Toolkit.getDefaultToolkit();
    
    private int getHeadlessSystemColorARGB(int index) {
        switch(index) {
        // Grey-blue
        case SystemColor.ACTIVE_CAPTION:
            return 0xff336699;
        case SystemColor.ACTIVE_CAPTION_BORDER:
            return 0xffcccccc;
        case SystemColor.ACTIVE_CAPTION_TEXT:
            return 0xffffffff;
        // Light grey
        case SystemColor.CONTROL:
            return 0xffdddddd;
        // Dark grey
        case SystemColor.CONTROL_DK_SHADOW:
            return 0xff777777;
        // Almost white
        case SystemColor.CONTROL_HIGHLIGHT:
            return 0xffeeeeee;
        // White
        case SystemColor.CONTROL_LT_HIGHLIGHT:
            return 0xffffffff;
        // Grey
        case SystemColor.CONTROL_SHADOW:
            return 0xff999999;
        // Black
        case SystemColor.CONTROL_TEXT:
            return 0xff000000;
        // Dark blue
        case SystemColor.DESKTOP:
            return 0xff224466;
        // Another grey
        case SystemColor.INACTIVE_CAPTION:
            return 0xff888888;
        // Grey
        case SystemColor.INACTIVE_CAPTION_BORDER:
            return 0xffcccccc;
        // Light grey
        case SystemColor.INACTIVE_CAPTION_TEXT:
            return 0xffdddddd;
        // Almost white
        case SystemColor.INFO:
            return 0xffeeeeee;
        case SystemColor.INFO_TEXT:
            return 0xff222222;
        // Light grey
        case SystemColor.MENU:
            return 0xffdddddd;
        // Black
        case SystemColor.MENU_TEXT:
            return 0xff000000;
        // Grey
        case SystemColor.SCROLLBAR:
            return 0xffcccccc;
        // White
        case SystemColor.TEXT:
            return 0xffffffff;
        // Grey blue
        case SystemColor.TEXT_HIGHLIGHT:
            return 0xff336699;
        // White
        case SystemColor.TEXT_HIGHLIGHT_TEXT:
            return 0xffffffff;
        // Almost white
        case SystemColor.TEXT_INACTIVE_TEXT:
            return 0xffdddddd;
        // Black
        case SystemColor.TEXT_TEXT:
            return 0xff000000;
        // White
        case SystemColor.WINDOW:
            return 0xffffffff;
        // Black
        case SystemColor.WINDOW_BORDER:
            return 0xff000000;
        // Black
        case SystemColor.WINDOW_TEXT:
            return 0xff000000;
        }
        // Black
        return 0xFF000000;
    }

    private final int index;
    @Override
    public String toString() {
        return getClass().getName() + "[index=" + index + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public int getRGB() {
        return value = getARGB();
    }

    @Override
    public int hashCode() {
        return (index * 37) + value;
    }

    private SystemColor(int index) {
        super(0, 0, 0);
        this.index = index;
        value = getRGB();
    }

    private int getARGB() {        
        return GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance() ?
                getHeadlessSystemColorARGB(index) : 
                toolkit.getWTK().getSystemProperties().getSystemColorARGB(index);
    }

    @Override
    public PaintContext createContext(ColorModel cm, Rectangle r, Rectangle2D r2d, AffineTransform at, RenderingHints rh) {
        return new Color.ColorPaintContext(getRGB());
    }

    private void readObject(ObjectInputStream stream)
                throws IOException, ClassNotFoundException {

        stream.defaultReadObject();

        FieldsAccessor accessor = new FieldsAccessor(Component.class, this);
        accessor.set("toolkit", Toolkit.getDefaultToolkit()); //$NON-NLS-1$
    }

}

