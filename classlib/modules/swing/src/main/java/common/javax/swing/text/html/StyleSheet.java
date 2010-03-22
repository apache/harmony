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
 * @author Alexey A. Ivanov
*/
package javax.swing.text.html;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.View;
import javax.swing.text.html.CSS.Attribute;
import javax.swing.text.html.CSS.BorderColor;
import javax.swing.text.html.CSS.BorderStyle;
import javax.swing.text.html.CSS.ColorProperty;
import javax.swing.text.html.CSS.PropertyValueConverter;
import javax.swing.text.html.CSS.ShorthandPropertyExpander;
import javax.swing.text.html.CSS.TextDecoration;
import javax.swing.text.html.CSS.ViewUpdater;

import org.apache.harmony.x.swing.Utilities;
import org.apache.harmony.x.swing.text.html.cssparser.CSSParser;
import org.apache.harmony.x.swing.text.html.cssparser.ParseException;
import org.apache.harmony.x.swing.text.html.cssparser.metamodel.Property;
import org.apache.harmony.x.swing.text.html.cssparser.metamodel.RuleSet;
import org.apache.harmony.x.swing.text.html.cssparser.metamodel.Sheet;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * Implementation of this class is based on <a href="http://www.w3.org/TR/CSS1">CSS1</a>.
 */
public class StyleSheet extends StyleContext {

    public static class BoxPainter implements Serializable {
        private static final CSS.Attribute[] MARGIN_KEYS = new CSS.Attribute[] {
            CSS.Attribute.MARGIN_TOP,    CSS.Attribute.MARGIN_RIGHT,
            CSS.Attribute.MARGIN_BOTTOM, CSS.Attribute.MARGIN_LEFT
        };
        private static final CSS.Attribute[] PADDING_KEYS = new CSS.Attribute[] {
            CSS.Attribute.PADDING_TOP,    CSS.Attribute.PADDING_RIGHT,
            CSS.Attribute.PADDING_BOTTOM, CSS.Attribute.PADDING_LEFT
        };
        private static final CSS.Attribute[] BORDER_WIDTH_KEYS =
            new CSS.Attribute[] {
                CSS.Attribute.BORDER_TOP_WIDTH,
                CSS.Attribute.BORDER_RIGHT_WIDTH,
                CSS.Attribute.BORDER_BOTTOM_WIDTH,
                CSS.Attribute.BORDER_LEFT_WIDTH
            };

        private View view;
        private final AttributeSet attr;
        private final StyleSheet styleSheet;

        private final float[] margin = new float[4];
        private final float[] borderWidth = new float[4];

        private CSS.BorderStyle borderStyle;
        private CSS.BorderColor borderColor;
        private CSS.ColorProperty backgroundColor;
        private CSS.ImageValue backgroundImage;
        private CSS.BackgroundRepeat backgroundRepeat;
        private Color foreground;
        private ViewUpdater updater;
        private boolean isTableBoxPainter;

        private BoxPainter(final AttributeSet attr,
                           final StyleSheet styleSheet) {
            this.attr = attr;
            this.styleSheet = styleSheet;
        }

        public float getInset(final int side, final View v) {
            final int sideIndex = getSideIndex(side);
            CSS.Length length =
                (CSS.Length)v.getAttributes()
                            .getAttribute(PADDING_KEYS[sideIndex]);
            return margin[sideIndex]
                   + (length != null ? length.floatValue(v) : 0)
                   + getBorderSideWidth(sideIndex, v);
        }

        public void paint(final Graphics g, final float x, final float y,
                          final float w, final float h, final View v) {
            Color oldColor = g.getColor();

            updateProperties();

            if (!(g instanceof Graphics2D)) {
                System.err.println(Messages.getString("swing.err.09")); //$NON-NLS-1$
            }
            Graphics2D g2d = (Graphics2D)g;

            int xx = (int)x;
            int yy = (int)y;
            int ww = (int)w;
            int hh = (int)h;
            if (isTableBoxPainter) {
                int captionHeight = ((TableTagView)view).getCaptionHeight();
                yy += captionHeight;
                hh -= captionHeight;
            }
            xx += margin[CSS.LEFT_SIDE];
            yy += margin[CSS.TOP_SIDE];
            ww -= (margin[CSS.LEFT_SIDE] + margin[CSS.RIGHT_SIDE]);
            hh -= (margin[CSS.TOP_SIDE] + margin[CSS.BOTTOM_SIDE]);


            if (backgroundImage != null
                && backgroundImage != CSS.ImageValue.NONE) {

                backgroundImage.loadImage(styleSheet.getBase());
                final Image image = backgroundImage.getImage();
                if (image != null) {
                    paintBackgroundImage(image, g, xx, yy, ww, hh);
                } else {
                    if (updater == null) {
                        updater = new ViewUpdater() {
                            public void updateView() {
                                Rectangle rc;
                                JTextComponent tc =
                                    (JTextComponent)view.getContainer();
                                if (tc != null) {
                                    try {
                                        rc = tc.modelToView(view
                                                            .getStartOffset());
                                    } catch (BadLocationException e) {
                                        return;
                                    }
                                    tc.repaint(rc.x, rc.y,
                                               ((BoxView)view).getWidth(),
                                               ((BoxView)view).getHeight());
                                }
                            }
                        };
                    }
                    backgroundImage.addListener(updater);
                }
            } else if (backgroundColor != null
                       && backgroundColor.getColor() != null) {
                g.setColor(backgroundColor.getColor());
                g.fillRect(xx, yy, ww, hh);
            }


            if (borderStyle != null) {
                Color bc;
                Stroke saveStroke = g2d.getStroke();
                Stroke bs;

                if (shouldDrawSide(CSS.BOTTOM_SIDE)) {
                    bc = getSideColor(CSS.BOTTOM_SIDE);
                    bs = getSideStroke(CSS.BOTTOM_SIDE);
                    g2d.setColor(bc);
                    g2d.setStroke(bs);
                    g2d.drawLine(xx, yy + hh - 1, xx + ww - 1, yy + hh - 1);
                }
                if (shouldDrawSide(CSS.LEFT_SIDE)) {
                    bc = getSideColor(CSS.LEFT_SIDE);
                    bs = getSideStroke(CSS.LEFT_SIDE);
                    g2d.setColor(bc);
                    g2d.setStroke(bs);
                    g2d.drawLine(xx, yy, xx, yy + hh - 1);
                }
                if (shouldDrawSide(CSS.TOP_SIDE)) {
                    bc = getSideColor(CSS.TOP_SIDE);
                    bs = getSideStroke(CSS.TOP_SIDE);
                    g2d.setColor(bc);
                    g2d.setStroke(bs);
                    g2d.drawLine(xx, yy, xx + ww - 1, yy);
                }
                if (shouldDrawSide(CSS.RIGHT_SIDE)) {
                    bc = getSideColor(CSS.RIGHT_SIDE);
                    bs = getSideStroke(CSS.RIGHT_SIDE);
                    g2d.setColor(bc);
                    g2d.setStroke(bs);
                    g2d.drawLine(xx + ww - 1, yy, xx + ww - 1, yy + hh - 1);
                }

                g2d.setStroke(saveStroke);
            }

            g.setColor(oldColor);
            foreground = null;
        }

        final void setView(final View view) {
            this.view = view;
            isTableBoxPainter = view instanceof TableTagView;
            updateMargin();
        }

        float getTopMargin() {
            return margin[CSS.TOP_SIDE];
        }

        float getRightMargin() {
            return margin[CSS.RIGHT_SIDE];
        }

        float getBottomMargin() {
            return margin[CSS.BOTTOM_SIDE];
        }

        float getLeftMargin() {
            return margin[CSS.LEFT_SIDE];
        }

        private static float getBorderSideWidth(final int side,
                                                final View view,
                                                final AttributeSet attrs) {
            CSS.Length length =
                (CSS.Length)attrs.getAttribute(BORDER_WIDTH_KEYS[side]);
            return length != null
                   ? length.floatValue(view)
                   : CSS.BorderWidthValue.factory.floatValue();
        }

        private float getBorderSideWidth(final int side, final View view) {
            final AttributeSet viewAttr = view.getAttributes();
            CSS.BorderStyle borderStyle =
                (BorderStyle)viewAttr.getAttribute(CSS.Attribute.BORDER_STYLE);
            if (borderStyle == null
                || borderStyle.getSideStyle(side).getIndex()
                   == CSS.BorderStyleValue.NONE) {

                return 0;
            }

            return getBorderSideWidth(side, view, viewAttr);
        }

        private Color getSideColor(final int i) {
            Color result = null;
            if (borderColor != null) {
                result = (Color)borderColor.getSideColor(i).fromCSS();
            }

            if (result == null) {
                if (foreground == null) {
                    ColorProperty cp =
                        (ColorProperty)attr.getAttribute(CSS.Attribute.COLOR);
                    foreground = cp != null ? cp.getColor() : Color.BLACK;
                }
                result = foreground;
            }
            return result;
        }

        private int getSideIndex(final int side) {
            switch (side) {
            case SwingUtilities.TOP:
                return CSS.TOP_SIDE;

            case SwingUtilities.RIGHT:
                return CSS.RIGHT_SIDE;

            case SwingUtilities.BOTTOM:
                return CSS.BOTTOM_SIDE;

            case SwingUtilities.LEFT:
                return CSS.LEFT_SIDE;

            default:
                throw new IllegalArgumentException(Messages.getString("swing.A4", side)); //$NON-NLS-1$
            }
        }

        private Stroke getSideStroke(final int side) {
            return new BasicStroke(borderWidth[side]);
        }

        private boolean shouldDrawSide(final int side) {
            return borderStyle.getSideStyle(side).getIndex() != 0
                   && borderWidth[side] > 0;
        }

        private void paintBackgroundImage(final Image image, final Graphics g,
                                          final int x, final int y,
                                          final int w, final int h) {
            if (image == null) {
                return;
            }

            final int imageWidth = backgroundImage.getWidth();
            final int imageHeight = backgroundImage.getHeight();

            final Image offscreen = view.getContainer().createVolatileImage(w, h);
            final Graphics offG = offscreen.getGraphics();

             if (backgroundColor != null
                 && backgroundColor.getColor() != null) {

                 offG.setColor(backgroundColor.getColor());
                 offG.fillRect(x, y, w, h);
             }

            final int repeat = backgroundRepeat != null
                               ? backgroundRepeat.getIndex() : 0;
            switch (repeat) {
            default:
            case CSS.BackgroundRepeat.REPEAT:
                for (int ix = 0; ix < w; ix += imageWidth) {
                    for (int iy = 0; iy < h; iy += imageHeight) {
                        offG.drawImage(image, ix, iy, null);
                    }
                }
                break;

            case CSS.BackgroundRepeat.REPEAT_X:
                for (int ix = 0; ix < w; ix += imageWidth) {
                    offG.drawImage(image, ix, 0, null);
                }
                break;

            case CSS.BackgroundRepeat.REPEAT_Y:
                for (int iy = 0; iy < h; iy += imageHeight) {
                    offG.drawImage(image, 0, iy, null);
                }
                break;

            case CSS.BackgroundRepeat.NO_REPEAT:
                offG.drawImage(image, 0, 0, null);
                break;
            }

            g.drawImage(offscreen, x, y, null);

            offG.dispose();
            offscreen.flush();
        }

        private void updateMargin() {
            for (int i = 0; i < margin.length; i++) {
                CSS.Length length =
                    (CSS.Length)attr.getAttribute(MARGIN_KEYS[i]);
                margin[i] = length != null ? length.floatValue(view) : 0;
            }
        }

        private void updateBorderWidth() {
            for (int i = 0; i < borderWidth.length; i++) {
                borderWidth[i] = getBorderSideWidth(i, view, attr);
            }
        }

        private void updateProperties() {
            updateMargin();

            borderStyle =
                (BorderStyle)attr.getAttribute(CSS.Attribute.BORDER_STYLE);
            if (borderStyle != null) {
                borderColor =
                    (BorderColor)attr.getAttribute(CSS.Attribute.BORDER_COLOR);
                updateBorderWidth();
            }

            backgroundImage =
                (CSS.ImageValue)attr.getAttribute(CSS.Attribute
                                                       .BACKGROUND_IMAGE);
            backgroundRepeat =
                backgroundImage == null
                ? null
                : (CSS.BackgroundRepeat)attr.getAttribute(CSS.Attribute
                                                          .BACKGROUND_REPEAT);
            backgroundColor =
                (CSS.ColorProperty)attr.getAttribute(CSS.Attribute
                                                     .BACKGROUND_COLOR);
        }
    }

    public static class ListPainter implements Serializable {
        private static final float DECORATOR_MARGIN = 5;
        private static final String DISC = "\u25CF";
        private static final String CIRCLE = "\u25CB";
        private static final String SQUARE = "\u25A0";
        //        private final AttributeSet attr;
//        private final CSS.ListStyleType listStyle;
//        private final CSS.ListStyleImage listImage;


        private ListPainter(final AttributeSet attr) {
//            this.attr = attr;
//            listStyle =
//                (CSS.ListStyleType)attr.getAttribute(Attribute.LIST_STYLE_TYPE);
            // TODO ListPainter: handle listImage
//            attr.getAttribute(Attribute.LIST_STYLE_IMAGE);
        }

        public void paint(final Graphics g, final float x, final float y,
                          final float w, final float h,
                          final View v, final int item) {
            final View child = v.getView(item);
            if (!(child instanceof BlockView)) {
                return;
            }

            final AttributeSet attr = child.getAttributes();
            final StyleSheet ss = ((BlockView)child).getStyleSheet();
            Font font = ss.getFont(attr);
            final Color color = ss.getForeground(attr);
            final CSS.ListStyleType listStyle =
                (CSS.ListStyleType)attr.getAttribute(Attribute.LIST_STYLE_TYPE);

            String decorator = null;

            int index;
            if (listStyle == null) {
                final String name = v.getElement().getName();
                if (HTML.Tag.OL.toString().equals(name)) {
                    index = CSS.ListStyleType.LIST_STYLE_DECIMAL;
                } else if (HTML.Tag.UL.toString().equals(name)) {
                    index = CSS.ListStyleType.LIST_STYLE_DISC;
                } else {
                    index = CSS.ListStyleType.LIST_STYLE_NONE;
                }
            } else {
                index = listStyle.getIndex();
            }

            switch (index) {
            case CSS.ListStyleType.LIST_STYLE_DISC:
                decorator = DISC;
                font = font.deriveFont(font.getSize2D() * 0.7f);
                break;

            case CSS.ListStyleType.LIST_STYLE_CIRCLE:
                decorator = CIRCLE;
                font = font.deriveFont(font.getSize2D() * 0.7f);
                break;

            case CSS.ListStyleType.LIST_STYLE_SQUARE:
                decorator = SQUARE;
                font = font.deriveFont(font.getSize2D() * 0.7f);
                break;


            case CSS.ListStyleType.LIST_STYLE_DECIMAL:
                decorator = Integer.toString(item + 1) + ".";
                break;


            case CSS.ListStyleType.LIST_STYLE_LOWER_ROMAN:
                decorator = Integer.toString(item + 1) + "r.";
                break;
            case CSS.ListStyleType.LIST_STYLE_UPPER_ROMAN:
                decorator = Integer.toString(item + 1) + "R.";
                break;

            case CSS.ListStyleType.LIST_STYLE_LOWER_ALPHA:
                decorator = (char)('a' + item) + ".";
                break;
            case CSS.ListStyleType.LIST_STYLE_UPPER_ALPHA:
                decorator = (char)('A' + item) + ".";
                break;

            case CSS.ListStyleType.LIST_STYLE_NONE:
            default:
            }

            if (decorator == null) {
                return;
            }

            Color oldColor = g.getColor();
            Font oldFont = g.getFont();

            g.setColor(color);
            g.setFont(font);
            FontMetrics metrics = g.getFontMetrics(font);
            int width = metrics.stringWidth(decorator);

            Rectangle pAlloc = new Rectangle((int)x, (int)y, (int)w, (int)h);
            pAlloc = (Rectangle)child.getChildAllocation(0, pAlloc);

            g.drawString(decorator,
                         (int)(x - width - DECORATOR_MARGIN),
                         (int)(pAlloc.y
                               + pAlloc.height * child.getView(0)
                                                 .getAlignment(View.Y_AXIS)
                               + metrics.getHeight() / 2f
                               - metrics.getDescent()));

            g.setFont(oldFont);
            g.setColor(oldColor);
        }
    }

    final class SmallConverterSet extends SmallAttributeSet {
        public SmallConverterSet(final AttributeSet attrs) {
            super(attrs);
        }

        public SmallConverterSet(final Object[] attrs) {
            super(attrs);
        }

        public Object getAttribute(final Object key) {
            Object cssKey = CSS.mapToCSS(key);
            Object result = super.getAttribute(cssKey != null ? cssKey : key);
            if (!(cssKey instanceof Attribute)) {
                if (key == StyleConstants.Underline
                    || key == StyleConstants.StrikeThrough) {

                    return getTextDecoration(this, key);
                }
                return result;
            }
            if (result == null) {
                return super.getAttribute(key);
            }

            return ((PropertyValueConverter)result).fromCSS();
        }

        public boolean isDefined(final Object key) {
            if (key == StyleConstants.Underline
                || key == StyleConstants.StrikeThrough) {

                return super.isDefined(Attribute.TEXT_DECORATION);
            }
            Object cssKey = CSS.mapToCSS(key);
            return super.isDefined(cssKey != null ? cssKey : key);
        }
    }

    final class LargeConverterSet extends SimpleAttributeSet {

        LargeConverterSet(final AttributeSet source) {
            super(source);
        }

        public Object getAttribute(final Object key) {
            Object cssKey = CSS.mapToCSS(key);
            Object result = super.getAttribute(cssKey != null ? cssKey : key);
            if (!(cssKey instanceof Attribute)) {
                if (key == StyleConstants.Underline
                    || key == StyleConstants.StrikeThrough) {

                    return getTextDecoration(this, key);
                }
                return result;
            }
            if (result == null) {
                return super.getAttribute(key);
            }

            return ((PropertyValueConverter)result).fromCSS();
        }

        public boolean isDefined(final Object key) {
            if (key == StyleConstants.Underline
                || key == StyleConstants.StrikeThrough) {

                return super.isDefined(Attribute.TEXT_DECORATION);
            }
            Object cssKey = CSS.mapToCSS(key);
            return super.isDefined(cssKey != null ? cssKey : key);
        }
    }

    private final class ResultStyleHashMap extends HashMap {
        public CascadedStyle get(final String key) {
            return (CascadedStyle)super.get(key);
        }

        public void put(final Style resStyle) {
            super.put(resStyle.getName(), resStyle);
        }
    }

    private final class NameConverterEnumeration implements Enumeration {
        private final List attrKeys = new LinkedList();
        private final Iterator it;

        private final boolean underline;
        private final boolean lineThrough;

        NameConverterEnumeration(final AttributeSet toModify,
                                 final AttributeSet attrSet) {
            final Enumeration keys = attrSet.getAttributeNames();
            boolean ul = false;
            boolean lt = false;
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = attrSet.getAttribute(key);
                if (key == StyleConstants.Underline) {
                    ul = ((Boolean)value).booleanValue();
                } else if (key == StyleConstants.StrikeThrough) {
                    lt = ((Boolean)value).booleanValue();
                } else if (toModify.containsAttribute(key, value)) {
                    attrKeys.add(key);
                }
            }

            it = attrKeys.iterator();

            underline = ul;
            lineThrough = lt;
        }

        NameConverterEnumeration(final Enumeration names) {
            boolean ul = false;
            boolean lt = false;
            while (names.hasMoreElements()) {
                Object key = names.nextElement();
                if (key == StyleConstants.Underline) {
                    ul = true;
                } else if (key == StyleConstants.StrikeThrough) {
                    lt = true;
                } else {
                    attrKeys.add(key);
                }
            }

            it = attrKeys.iterator();

            underline = ul;
            lineThrough = lt;
        }

        public boolean hasMoreElements() {
            return it.hasNext();
        }

        public Object nextElement() {
            Object key = it.next();
            Object cssKey = CSS.mapToCSS(key);
            return cssKey != null ? cssKey : key;
        }

        boolean isUnderline() {
            return underline;
        }

        boolean isLineThrough() {
            return lineThrough;
        }
    }

    private static final Pattern relativePattern =
        Pattern.compile("(?:\\+|-)\\d+");

    private static final int DEFAULT_BASE_FONT_SIZE = 4;

    private static final Map HTML_ATTRIBUTE_TO_CSS = new HashMap();

    private URL base;
    private int baseFontSize = DEFAULT_BASE_FONT_SIZE;

    private CSSParser parser;

    private ResultStyleHashMap resultStyles = new ResultStyleHashMap();
    private List styleSheets = new LinkedList();

    static {
        HTML_ATTRIBUTE_TO_CSS.put(HTML.Attribute.ALIGN,
                                  CSS.Attribute.TEXT_ALIGN);
        HTML_ATTRIBUTE_TO_CSS.put(HTML.Attribute.BACKGROUND,
                                  CSS.Attribute.BACKGROUND_IMAGE);
        HTML_ATTRIBUTE_TO_CSS.put(HTML.Attribute.BGCOLOR,
                                  CSS.Attribute.BACKGROUND_COLOR);
//        HTML_ATTRIBUTE_TO_CSS.put(HTML.Attribute.BORDER,
//                                  CSS.Attribute.BORDER);
        HTML_ATTRIBUTE_TO_CSS.put(HTML.Attribute.TEXT,
                                  CSS.Attribute.COLOR);

        HTML_ATTRIBUTE_TO_CSS.put(HTML.Attribute.COLOR,
                                  CSS.Attribute.COLOR);
        HTML_ATTRIBUTE_TO_CSS.put(HTML.Attribute.FACE,
                                  CSS.Attribute.FONT_FAMILY);

//        HTML_ATTRIBUTE_TO_CSS.put(HTML.Attribute.HALIGN,
//                                  CSS.Attribute.TEXT_ALIGN);
        HTML_ATTRIBUTE_TO_CSS.put(HTML.Attribute.HEIGHT,
                                  CSS.Attribute.HEIGHT);

//        HTML_ATTRIBUTE_TO_CSS.put(HTML.Attribute.NOWRAP,
//                                  CSS.Attribute.WHITE_SPACE);

        HTML_ATTRIBUTE_TO_CSS.put(HTML.Attribute.VALIGN,
                                  CSS.Attribute.VERTICAL_ALIGN);

        HTML_ATTRIBUTE_TO_CSS.put(HTML.Attribute.WIDTH,
                                  CSS.Attribute.WIDTH);

//        HTML_ATTRIBUTE_TO_CSS.put(HTML.Attribute.LINK,
//                                  CSS.Attribute.COLOR);
//        HTML_ATTRIBUTE_TO_CSS.put(HTML.Attribute.ALINK,
//                                  CSS.Attribute.COLOR);
//        HTML_ATTRIBUTE_TO_CSS.put(HTML.Attribute.VLINK,
//                                  CSS.Attribute.COLOR);
    }

    public static int getIndexOfSize(final float size) {
        return CSS.FontSize.sizeValueIndex((int)size) + 1;
    }

    public AttributeSet addAttribute(final AttributeSet old, final Object key,
                                     final Object value) {
        Attribute cssKey = (Attribute)CSS.mapToCSSForced(key);
        if (cssKey == null || cssKey.getConverter() == null) {
            if (key == StyleConstants.Underline
                || key == StyleConstants.StrikeThrough) {

                return super.addAttribute(old, CSS.Attribute.TEXT_DECORATION,
                                          createTextDecoration(old, key, value));
            }
            return super.addAttribute(old, key, value);
        }
        return super.addAttribute(old, cssKey,
                                  value instanceof PropertyValueConverter
                                  ? value : cssKey.getConverter().toCSS(value));
    }

    public AttributeSet addAttributes(final AttributeSet old,
                                      final AttributeSet attr) {
        MutableAttributeSet converted = new SimpleAttributeSet(old);
        Enumeration attrKeys = attr.getAttributeNames();
        while (attrKeys.hasMoreElements()) {
            Object key = attrKeys.nextElement();
            Object value = attr.getAttribute(key);
            Attribute cssKey = (Attribute)CSS.mapToCSSForced(key);
            if (cssKey == null) {
                if (key == StyleConstants.Underline
                    || key == StyleConstants.StrikeThrough) {

                    value = createTextDecoration(converted, key, value);
                    key = CSS.Attribute.TEXT_DECORATION;
                }
                converted.addAttribute(key, value);
            } else {
                if (!(value instanceof CSS.PropertyValueConverter)) {
                    value = cssKey.getConverter().toCSS(value);
                }
                if (value != null) {
                    converted.addAttribute(cssKey, value);
                }
            }
        }
        return super.addAttributes(getEmptySet(), converted);
    }

    public AttributeSet removeAttribute(final AttributeSet old,
                                        final Object key) {
        if (key == StyleConstants.Underline
            || key == StyleConstants.StrikeThrough) {

            TextDecoration td =
                (TextDecoration)old.getAttribute(Attribute.TEXT_DECORATION);
            td = (TextDecoration)td.clone();
            if (key == StyleConstants.Underline && td.isUnderline()) {
                td.setUnderline(false);
            }
            if (key == StyleConstants.StrikeThrough && td.isLineThrough()) {
                td.setLineThrough(false);
            }
            if (td.isNone()) {
                return super.removeAttribute(old, Attribute.TEXT_DECORATION);
            }
            return super.addAttribute(old, Attribute.TEXT_DECORATION, td);
        }

        Attribute cssKey = (Attribute)CSS.mapToCSSForced(key);
        if (cssKey == null) {
            return super.removeAttribute(old, key);
        }
        return super.removeAttribute(old, cssKey);
    }

    public AttributeSet removeAttributes(final AttributeSet old,
                                         final AttributeSet rem) {
        return localRemoveAttributes(old, new NameConverterEnumeration(old, rem));
    }

    public AttributeSet removeAttributes(final AttributeSet old,
                                         final Enumeration<?> names) {
        return localRemoveAttributes(old, new NameConverterEnumeration(names));
    }

    public void addCSSAttribute(final MutableAttributeSet attr,
                                final CSS.Attribute key, final String value) {
        if (key == null || Utilities.isEmptyString(value)) {
            return;
        }
        ShorthandPropertyExpander spe = key.getExpander();
        if (spe != null) {
            spe.parseAndExpandProperty(attr, value);
        } else {
            Object cssValue = key.getConverter().toCSS(value);
            if (cssValue != null) {
                attr.addAttribute(key, cssValue);
            }
        }
    }

    public boolean addCSSAttributeFromHTML(final MutableAttributeSet attr,
                                           final CSS.Attribute key,
                                           final String value) {

        final int count = attr.getAttributeCount();
        final Object attrValue = attr.getAttribute(key);
        if (key == CSS.Attribute.BACKGROUND_IMAGE) {
            addCSSAttribute(attr, key, "url(" + value + ")");
        } else {
            addCSSAttribute(attr, key, value);
        }
        return count != attr.getAttributeCount()
               || attrValue != attr.getAttribute(key);
    }

    public void addRule(final String rule) {
        if (Utilities.isEmptyString(rule)) {
            return;
        }
        initCSSParser(new StringReader(rule));
        parseSheet(false);
    }

    public void addStyleSheet(final StyleSheet ss) {
        if (!styleSheets.contains(ss)) {
            styleSheets.add(0, ss);
            addStyleSheetToCascadedStyles(ss);
        }
    }

    protected MutableAttributeSet
        createLargeAttributeSet(final AttributeSet attr) {

        return new LargeConverterSet(attr);
    }

    protected SmallAttributeSet
        createSmallAttributeSet(final AttributeSet attr) {

        return new SmallConverterSet(attr);
    }

    public BoxPainter getBoxPainter(final AttributeSet attr) {
        return new BoxPainter(attr, this);
    }

    public AttributeSet getDeclaration(final String decl) {
        if (Utilities.isEmptyString(decl)) {
            return new SimpleAttributeSet();
        }

        initCSSParser(new StringReader("htmlTag {" + decl + "}"));
        RuleSet rs = null;
        try {
            rs = parser.parseRuleSet();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        MutableAttributeSet attrs = new SimpleAttributeSet();
        Iterator pi = rs.getProperties();
        while (pi.hasNext()) {
            Property property = (Property)pi.next();
            addCSSAttribute(attrs, CSS.getAttribute(property.getName()),
                            property.getValue());
        }

        return attrs;
    }

    public Font getFont(final AttributeSet attr) {
        Object value;

        value = getCSSProperty(attr, Attribute.FONT_FAMILY);
        final String family = value != null ? (String)value
                                            : CSS.FontFamily.DEFAULT;

        value = attr.getAttribute(Attribute.FONT_SIZE);
        final int size = value != null
                         ? ((CSS.Length)value).intValue(attr)
                         : CSS.FontSize.getDefaultValue().intValue();

        value = getCSSProperty(attr, Attribute.FONT_WEIGHT);
        final boolean bold = value != null ? ((Boolean)value).booleanValue()
                                           : false;

        value = getCSSProperty(attr, Attribute.FONT_STYLE);
        boolean italic = value != null ? ((Boolean)value).booleanValue()
                                       : false;

        int style = Font.PLAIN;
        if (bold) {
            style |= Font.BOLD;
        }
        if (italic) {
            style |= Font.ITALIC;
        }
        return getFont(family, style, size);
    }

    public Color getForeground(final AttributeSet attr) {
        Object fore = getCSSProperty(attr, Attribute.COLOR);
        return fore != null ? (Color)fore
                            : super.getForeground(getEmptySet());
    }

    public Color getBackground(final AttributeSet attr) {
        Object back = getCSSProperty(attr, Attribute.BACKGROUND_COLOR);
        return back != null ? (Color)back : null;
    }

    public ListPainter getListPainter(final AttributeSet attr) {
        return new ListPainter(attr);
    }

    public Style getRule(final String selector) {
        return getCascadedStyle(new Selector(selector).toString());
    }

    public Style getRule(final HTML.Tag tag, final Element element) {
        return getCascadedStyle(CascadedStyle.getElementTreeSelector(element));
    }

    public StyleSheet[] getStyleSheets() {
        return styleSheets.size() > 0
               ? (StyleSheet[])styleSheets
                               .toArray(new StyleSheet[styleSheets.size()])
               : null;
    }

    public AttributeSet getViewAttributes(final View v) {
        return new ViewAttributeSet(this, v);
    }

    public void importStyleSheet(final URL url) {
        // TODO importStyleSheet: use the URL specified to resolve references
        if (url == null) {
            return;
        }

        try {
            initCSSParser(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        parseSheet(true);
    }

    public void loadRules(final Reader in, final URL ref) throws IOException {
        // TODO loadRules: use the URL specified to resolve references
        initCSSParser(in);
        parseSheet(false);
    }

    public void removeStyle(final String name) {
        super.removeStyle(name);
        if (name != null) {
            removeStyleFromCascadedStyles(name);
        }
    }

    public void removeStyleSheet(final StyleSheet ss) {
        if (styleSheets.remove(ss)) {
            removeStyleSheetFromCascadedStyles(ss);
        }
    }

    public void setBase(final URL base) {
        this.base = base;
    }

    public URL getBase() {
        return base;
    }

    public void setBaseFontSize(final int index) {
        baseFontSize = Utilities.range(index, 1, 7);
    }

    public void setBaseFontSize(final String size) {
        setBaseFontSize(getRelativeIndex(size));
    }

    public float getPointSize(final int index) {
        return CSS.FontSize.SIZE_TABLE[Utilities.range(index - 1,
                                                       0, 6)].intValue();
    }

    public float getPointSize(final String size) {
        return getPointSize(getRelativeIndex(size));
    }

    public Color stringToColor(final String colorNameOrHex) {
        return CSS.ColorProperty.stringToColor(colorNameOrHex);
    }

    public AttributeSet translateHTMLToCSS(final AttributeSet htmlAttrSet) {
        final Style result = addStyle(null, null);
//        if (((Element)htmlAttrSet).isLeaf()) {
//            return result;
//        }

        final Enumeration keys = htmlAttrSet.getAttributeNames();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = htmlAttrSet.getAttribute(key);
            if (key instanceof CSS.Attribute) {
                result.addAttribute(key, value);
            } else {
                Object cssKey = HTML_ATTRIBUTE_TO_CSS.get(key);
                if (cssKey != null
                    && ((Attribute)cssKey).getConverter() != null) {

                    if ((key == HTML.Attribute.HEIGHT
                         || key == HTML.Attribute.WIDTH)
                        && value instanceof String
                        && !((String)value).endsWith("%")) {

                        value = ((Attribute)cssKey).getConverter()
                                .toCSS((String)value + /*"px"*/ "pt");
                    } else if (key == HTML.Attribute.BACKGROUND) {
                        value = ((Attribute)cssKey).getConverter()
                                .toCSS("url(" + value + ")");
                    } else {
                        value = ((Attribute)cssKey).getConverter().toCSS(value);
                    }

                    if (value != null) {
                        result.addAttribute(cssKey, value);
                    }
                }
            }
        }
        return result;
    }

    final Boolean getTextDecoration(final AttributeSet attr,
                                    final Object key) {
        TextDecoration value =
            (TextDecoration)attr.getAttribute(Attribute.TEXT_DECORATION);
        if (value == null) {
            return null;
        }
        if (key == StyleConstants.Underline) {
            return Boolean.valueOf(value.isUnderline());
        }
        if (key == StyleConstants.StrikeThrough) {
            return Boolean.valueOf(value.isLineThrough());
        }
        return null;
    }

    private Object createTextDecoration(final AttributeSet old,
                                        final Object key, final Object value) {
        if (value instanceof String) {
            return Attribute.TEXT_DECORATION.getConverter().toCSS(value);
        }
        TextDecoration oldValue =
            (TextDecoration)old.getAttribute(Attribute.TEXT_DECORATION);
        TextDecoration result = oldValue == null
                                ? new TextDecoration()
                                : (TextDecoration)oldValue.clone();
        if (key == StyleConstants.Underline) {
            result.setUnderline(((Boolean)value).booleanValue());
        }
        if (key == StyleConstants.StrikeThrough) {
            result.setLineThrough(((Boolean)value).booleanValue());
        }
        return result;
    }

    private Object getCSSProperty(final AttributeSet attr,
                                  final CSS.Attribute key) {
        Object value = attr.getAttribute(key);
        if (value != null) {
            return ((PropertyValueConverter)value).fromCSS();
        }
        return null;
    }

    private int getRelativeIndex(final String size) {
        if (!relativePattern.matcher(size).matches()) {
            return Integer.parseInt(size);
        }

        int index = Integer.parseInt(size.substring(1));
        if (size.charAt(0) == '-') {
            index = -index;
        }
        return baseFontSize + index;
    }

    private void initCSSParser(final Reader reader) {
        if (parser == null) {
            parser = new CSSParser(reader);
        } else {
            parser.ReInit(reader);
        }
    }

    private void initCSSParser(final InputStream stream) {
        if (parser == null) {
            parser = new CSSParser(stream);
        } else {
            parser.ReInit(stream);
        }
    }

    private void parseSheet(final boolean asResolver) {
//        throw new UnsupportedOperationException(Messages.getString("swing.A5")); //$NON-NLS-1$
        Sheet ss = null;
        try {
            ss = parser.parse();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        addImportedStyleSheets(ss.getImportsIterator(), asResolver);
        addParsedSheet(ss.getRuleSetIterator(), asResolver);
    }

    private void addImportedStyleSheets(final Iterator it,
                                        final boolean asResolver) {
        while (it.hasNext()) {
            importStyleSheet(HTML.resolveURL(extractURL((String)it.next()),
                                             getBase()));
        }
    }

    private void addParsedSheet(final Iterator it, final boolean asResolver) {
        while (it.hasNext()) {
            RuleSet rs = (RuleSet)it.next();
            MutableAttributeSet attrs = new SimpleAttributeSet();
            Iterator pi = rs.getProperties();
            while (pi.hasNext()) {
                Property property = (Property)pi.next();
                addCSSAttribute(attrs, CSS.getAttribute(property.getName()),
                                property.getValue());
            }
            if (attrs.getAttributeCount() == 0) {
                continue;
            }

            Iterator si = rs.getSelectors();
            while (si.hasNext()) {
                String selector = new Selector((String)si.next()).toString();
                Style rr = getStyle(selector);
                if (rr == null) {
                    rr = addStyle(selector, null);
                    addStyleToCascadedStyles(selector);
                }
                if (asResolver) {
                    final AttributeSet resolver = rr.getResolveParent();
                    final MutableAttributeSet importedAttrs =
                        resolver instanceof MutableAttributeSet
                        ? (MutableAttributeSet)resolver
                        : addStyle(null, null);
                    importedAttrs.addAttributes(attrs);
                    rr.setResolveParent(importedAttrs);
                } else {
                    rr.addAttributes(attrs);
                }
            }
        }
    }

    private void addStyleToCascadedStyles(final String styleName) {
        final Iterator it = resultStyles.values().iterator();
        while (it.hasNext()) {
            CascadedStyle rs = (CascadedStyle)it.next();
            rs.addStyle(styleName);
        }
    }

    private void removeStyleFromCascadedStyles(final String styleName) {
        final Iterator it = resultStyles.values().iterator();
        while (it.hasNext()) {
            CascadedStyle rs = (CascadedStyle)it.next();
            rs.removeStyle(styleName);
        }
    }

    private void addStyleSheetToCascadedStyles(final StyleSheet styleSheet) {
        final Iterator it = resultStyles.values().iterator();
        while (it.hasNext()) {
            CascadedStyle rs = (CascadedStyle)it.next();
            rs.addStyleSheet(styleSheet);
        }
    }

    private void removeStyleSheetFromCascadedStyles(final StyleSheet styleSheet) {
        final Iterator it = resultStyles.values().iterator();
        while (it.hasNext()) {
            CascadedStyle rs = (CascadedStyle)it.next();
            rs.removeStyleSheet(styleSheet);
        }
    }

    private Style getCascadedStyle(final String selector) {
        Style result = resultStyles.get(selector);
        if (result != null) {
            return result;
        }
        result = new CascadedStyle(this, selector,
                                   SelectorMatcher.findMatching(getStyleNames(),
                                                                selector),
                                   styleSheets.iterator());
       resultStyles.put(result);
       return result;
    }

    private AttributeSet localRemoveAttributes(final AttributeSet toModify,
                                          final NameConverterEnumeration keys) {
        if (!keys.isUnderline() && !keys.isLineThrough()) {
            return super.removeAttributes(toModify, keys);
        }

        final MutableAttributeSet result = new SimpleAttributeSet(toModify);
        result.removeAttributes(keys);
        TextDecoration td =
            (TextDecoration)result.getAttribute(Attribute.TEXT_DECORATION);
        td = (TextDecoration)td.clone();

        if (keys.isUnderline() && td.isUnderline()) {
            td.setUnderline(false);
        }
        if (keys.isLineThrough() && td.isLineThrough()) {
            td.setLineThrough(false);
        }
        if (td.isNone()) {
            result.removeAttribute(Attribute.TEXT_DECORATION);
        } else {
            result.addAttribute(Attribute.TEXT_DECORATION, td);
        }
        return super.addAttributes(getEmptySet(), result);
    }

    private String extractURL(final String importPath) {
        String result = importPath;
        if (result.startsWith("url(") && result.endsWith(")")) {
            result = result.substring(4, result.length() - 1);
        }
        char c = result.charAt(0);
        if (c == '\'' || c == '"') {
            result = result.substring(1, result.length() - 1);
        }
        return result;
    }
}
