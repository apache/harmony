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

package javax.swing.plaf.synth;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.Icon;

/**
 * XMLSynthStyle class represents SynthStyle, generated from XMLSynthParser. The
 * setters used in parser to configure style. The getters used in UI to draw the
 * component.
 */
class XMLSynthStyle extends SynthStyle implements Cloneable {

    /**
     * Style properties
     */
    private final HashMap<String, Object> propertiesMap = new HashMap<String, Object>();

    /**
     * Painters manager defines the correct painter to draw the UI under the
     * style
     */
    private final PaintersManager paintersManager = new PaintersManager();

    /**
     * Insets used in UI under the style
     */
    private Insets insets = new Insets(0, 0, 0, 0);

    /**
     * GraphicsUtils used in UI
     */
    private SynthGraphicsUtils gUtils = new SynthGraphicsUtils();

    /**
     * isOpaque property for UI (used in painters)
     */
    private boolean isOpaque = false;

    /**
     * FontInfo contains all the registered fonts
     */
    private List<FontInfo> fonts = new ArrayList<FontInfo>();

    /**
     * contains all the registered icons
     */
    private List<IconInfo> icons = new ArrayList<IconInfo>();

    /**
     * ColorInfo contains all the registered colors according to ColorTypes
     */
    private List<ColorInfo> colors = new ArrayList<ColorInfo>();

    @Override
    public Object get(@SuppressWarnings("unused")
    SynthContext context, Object key) {
        return propertiesMap.get(key);
    }

    /**
     * According to spec, graphicsUitils stated for style - not for context.
     * That's why context is unused.
     */
    @Override
    @SuppressWarnings("unused")
    public SynthGraphicsUtils getGraphicsUtils(SynthContext context) {
        return gUtils;
    }

    @Override
    public Insets getInsets(SynthContext context, Insets modified) {
    
        if (modified == null) {
    
            return (Insets) this.insets.clone();
        }
    
        modified.set(insets.top, insets.left, insets.bottom, insets.right);
    
        return modified;
    }

    @Override
    @SuppressWarnings("unused")
    public SynthPainter getPainter(SynthContext context) {
        return paintersManager;
    }

    @Override
    @SuppressWarnings("unused")
    public boolean isOpaque(SynthContext context) {
        return isOpaque;
    }

    @Override
    public Icon getIcon(SynthContext context, Object key) {
        Icon result = null;
    
        int resultState = 0;
        int componentState = context.getComponentState();
        for (IconInfo info : icons) {
            if (info.getKey().equalsIgnoreCase((String) key)) {
                if (info.isStateFits(componentState)) {
                    if ((result == null) || info.getState() >= resultState) {
    
                        resultState = info.getState();
                        result = info.getIcon();
    
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected Font getFontForState(SynthContext context) {
        Font result = null;
        int resultState = 0;
        int componentState = context.getComponentState();
        for (FontInfo info : fonts) {
            if (info.isStateFits(componentState)) {
                if ((result == null) || (info.getState() >= resultState)) {
                    resultState = info.getState();
                    result = info.getFont();
                }
            }
        }
        return result;
    }

    /**
     * Should note that RI's behavior depends on the order, described in the xml
     * file (for example either state=ENABLED before state=FOCUSED or after) and
     * the (unspecified) order of UI's state verification (for example,
     * if(ButtonModel.isFocused) before Button.isEnabled in SynthButtonUI).
     * 
     * I've construct the getColorForState, getFontForState and
     * PainterManager.findPainter methods according to the RI's black box
     * testing (although, the RI's behavior seems incorrect for me)
     * 
     * Anyway, if you will to deal with using coloring, fonts or painters order
     * you should see the following: UI: getComponentState method,
     * XMLSynthParser: processColorElement, processFontElement,
     * processPainterElement, processImagePainterElement (generating the lists),
     * PainterManager(for painters) and this class
     */
    @Override
    protected Color getColorForState(SynthContext context, ColorType type) {
        Color result = null;
        int resultState = 0;
        int componentState = context.getComponentState();
        for (ColorInfo info : colors) {
            if (info.getColorType() == type) {
                if (info.isStateFits(componentState)) {
                    if ((result == null) || componentState > resultState) {
    
                        resultState = info.getState();
                        result = info.getColor();
    
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /** Method used in parser only */
    void addFont(Font f, int state) {
        fonts.add(new FontInfo(f, state));
    }

    /** Method used in parser only */
    void addColor(Color c, int state, ColorType type) {
        colors.add(new ColorInfo(c, type, state));
    }

    /**
     * add painter to paintersManager. Method used in parser only
     */
    void addPainter(SynthPainter p, int state, String method, int direction) {
        paintersManager.setPainter(p, state, method, direction);
    }

    void addIcon(Icon icon, int SynthState, String key) {
        icons.add(new IconInfo(icon, SynthState, key));
    }

    /**
     * Method used in parser only
     */
    void setGraphicsUtils(SynthGraphicsUtils proposed) {
        this.gUtils = proposed;
    }

    /** Method used in parser only */
    void setProperty(String key, Object value) {
        propertiesMap.put(key, value);
    }

    /** Method used in parser only */
    void setInsets(Insets insets) {
        this.insets = insets;
    }

    /** Method used in parser only */
    void setOpaque(boolean isOpaque) {
        this.isOpaque = isOpaque;
    }

    /**
     * ColorInfo class used in SynthStyle for correct search Colors in List of
     * colors.
     */
    private static class ColorInfo {
    
        private final ColorType type;
    
        private final int state;
    
        private final Color color;
    
        ColorInfo(Color color, ColorType type, int state) {
            this.color = color;
            this.type = type;
            this.state = state;
        }
    
        ColorType getColorType() {
            return type;
        }
    
        Color getColor() {
            return color;
        }
    
        int getState() {
            return state;
        }
    
        boolean isStateFits(int comparedState) {
            return (((~comparedState) & (this.state)) == 0);
        }
    
    }

    /**
     * ColorInfo class used in SynthStyle for correct search Fonts in List
     */
    private static class FontInfo {
    
        private final int state;
    
        private final Font font;
    
        FontInfo(Font font, int state) {
            this.font = font;
            this.state = state;
        }
    
        Font getFont() {
            return font;
        }
    
        int getState() {
            return state;
        }
    
        boolean isStateFits(int comparedState) {
            return (((~comparedState) & (this.state)) == 0);
        }
    
    }

    /**
     * IconInfo class used in SynthStyle for correct search Icons in List
     */
    private static class IconInfo {
    
        private final int state;
    
        private final Icon icon;
    
        private final String key;
    
        IconInfo(Icon icon, int state, String key) {
            this.icon = icon;
            this.state = state;
            this.key = key;
        }
    
        Icon getIcon() {
            return icon;
        }
    
        int getState() {
            return state;
        }
    
        String getKey() {
            return key;
        }
    
        boolean isStateFits(int comparedState) {
            return (((~comparedState) & (this.state)) == 0);
        }
    
    }

}
