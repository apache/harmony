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

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.AttributeSet;
import javax.swing.text.BoxView;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.View;

import org.apache.harmony.x.swing.Utilities;
import org.apache.harmony.x.swing.internal.nls.Messages;

public class CSS implements Serializable {
    public static final class Attribute {
        public static final Attribute BACKGROUND =
                new Attribute("background", null, false,
                              new BackgroundExpander());
        public static final Attribute BACKGROUND_ATTACHMENT =
                new Attribute("background-attachment", "scroll", false,
                              BackgroundAttachment.factory);
        public static final Attribute BACKGROUND_COLOR =
                new Attribute("background-color", "transparent", false,
                              BackgroundColor.factory);
        public static final Attribute BACKGROUND_IMAGE =
                new Attribute("background-image", "none", false,
                              ImageValue.NONE);
        public static final Attribute BACKGROUND_POSITION =
                new Attribute("background-position", null, false,
                              new BackgroundPosition());
        public static final Attribute BACKGROUND_REPEAT =
                new Attribute("background-repeat", "repeat", false,
                              BackgroundRepeat.factory);
        public static final Attribute BORDER =
                new Attribute("border", null, false,
                              new BorderExpander());
        public static final Attribute BORDER_BOTTOM_WIDTH =
            new Attribute("border-bottom-width", "medium", false,
                          BorderWidthValue.factory);
        public static final Attribute BORDER_BOTTOM =
                new Attribute("border-bottom", null, false,
                              new BorderSideExpander(
                                      BorderSideExpander.BOTTOM_SIDE,
                                      BORDER_BOTTOM_WIDTH));
        public static final Attribute BORDER_COLOR =
                new Attribute("border-color", null, false, new BorderColor());
        public static final Attribute BORDER_LEFT_WIDTH =
            new Attribute("border-left-width", "medium", false,
                          BorderWidthValue.factory);
        public static final Attribute BORDER_LEFT =
                new Attribute("border-left", null, false,
                              new BorderSideExpander(
                                      BorderSideExpander.LEFT_SIDE,
                                      BORDER_LEFT_WIDTH));
        public static final Attribute BORDER_RIGHT_WIDTH =
            new Attribute("border-right-width", "medium", false,
                          BorderWidthValue.factory);
        public static final Attribute BORDER_RIGHT =
                new Attribute("border-right", null, false,
                              new BorderSideExpander(
                                      BorderSideExpander.RIGHT_SIDE,
                                      BORDER_RIGHT_WIDTH));
        public static final Attribute BORDER_STYLE =
                new Attribute("border-style", "none", false, new BorderStyle());
        public static final Attribute BORDER_TOP_WIDTH =
                new Attribute("border-top-width", "medium", false,
                              BorderWidthValue.factory);
        public static final Attribute BORDER_TOP =
            new Attribute("border-top", null, false,
                          new BorderSideExpander(
                                  BorderSideExpander.TOP_SIDE,
                                  BORDER_TOP_WIDTH));
        public static final Attribute BORDER_WIDTH =
                new Attribute("border-width", "medium", false,
                              new SpaceExpander(BORDER_TOP_WIDTH,
                                                BORDER_RIGHT_WIDTH,
                                                BORDER_BOTTOM_WIDTH,
                                                BORDER_LEFT_WIDTH,
                                                BorderWidthValue.factory));
        public static final Attribute CLEAR =
                new Attribute("clear", "none", false, new Clear());
        public static final Attribute COLOR =
                new Attribute("color", null, true, ColorProperty.factory);
        public static final Attribute DISPLAY =
                new Attribute("display", "block", false, new Display());
        public static final Attribute FLOAT =
                new Attribute("float", "none", false, new FloatProperty());
        public static final Attribute FONT =
                new Attribute("font", null, true, new FontExpander());
        public static final Attribute FONT_FAMILY =
                new Attribute("font-family", null, true, new FontFamily());
        public static final Attribute FONT_SIZE =
                new Attribute("font-size", "medium", true, new FontSize());
        public static final Attribute FONT_STYLE =
                new Attribute("font-style", "normal", true, new FontStyle());
        public static final Attribute FONT_VARIANT =
                new Attribute("font-variant", "normal", true,
                              new FontVariant());
        public static final Attribute FONT_WEIGHT =
                new Attribute("font-weight", "normal", true, new FontWeight());
        public static final Attribute HEIGHT =
                new Attribute("height", "auto", false, new Height());
        public static final Attribute LETTER_SPACING =
                new Attribute("letter-spacing", "normal", true,
                              SpacingValue.factory);
        public static final Attribute LINE_HEIGHT =
                new Attribute("line-height", "normal", true,
                              LineHeight.normal);
        public static final Attribute LIST_STYLE =
                new Attribute("list-style", null, true,
                              new ListStyleExpander());
        public static final Attribute LIST_STYLE_IMAGE =
                new Attribute("list-style-image", "none", true,
                              ImageValue.NONE);
        public static final Attribute LIST_STYLE_POSITION =
                new Attribute("list-style-position", "outside", true,
                              ListStylePosition.factory);
        public static final Attribute LIST_STYLE_TYPE =
                new Attribute("list-style-type", "disc", true,
                              ListStyleType.factory);
        public static final Attribute MARGIN_BOTTOM =
                new Attribute("margin-bottom", "0", false, FloatValue.factory);
        public static final Attribute MARGIN_LEFT =
                new Attribute("margin-left", "0", false, FloatValue.factory);
        public static final Attribute MARGIN_RIGHT =
                new Attribute("margin-right", "0", false, FloatValue.factory);
        public static final Attribute MARGIN_TOP =
                new Attribute("margin-top", "0", false, FloatValue.factory);
        public static final Attribute MARGIN =
                new Attribute("margin", null, false,
                              new SpaceExpander(MARGIN_TOP, MARGIN_RIGHT,
                                                MARGIN_BOTTOM, MARGIN_LEFT));
        public static final Attribute PADDING_BOTTOM =
                new Attribute("padding-bottom", "0", false, FloatValue.factory);
        public static final Attribute PADDING_LEFT =
                new Attribute("padding-left", "0", false, FloatValue.factory);
        public static final Attribute PADDING_RIGHT =
                new Attribute("padding-right", "0", false, FloatValue.factory);
        public static final Attribute PADDING_TOP =
                new Attribute("padding-top", "0", false, FloatValue.factory);
        public static final Attribute PADDING =
                new Attribute("padding", null, false,
                              new SpaceExpander(PADDING_TOP, PADDING_RIGHT,
                                                PADDING_BOTTOM, PADDING_LEFT));
        public static final Attribute TEXT_ALIGN =
                new Attribute("text-align", null, true, new TextAlign());
        public static final Attribute TEXT_DECORATION =
                new Attribute("text-decoration", "none", true,
                              new TextDecoration());
        public static final Attribute TEXT_INDENT =
                new Attribute("text-indent", "0", true, FloatValue.factory);
        public static final Attribute TEXT_TRANSFORM =
                new Attribute("text-transform", "none", true,
                              new TextTransform());
        public static final Attribute VERTICAL_ALIGN =
                new Attribute("vertical-align", "baseline", false,
                              new VerticalAlign());
        public static final Attribute WHITE_SPACE =
                new Attribute("white-space", "normal", true,
                              WhiteSpace.factory);
        public static final Attribute WIDTH =
                new Attribute("width", "auto", false, Width.auto);
        public static final Attribute WORD_SPACING =
                new Attribute("word-spacing", "normal", true,
                              SpacingValue.factory);

        private final String name;
        private final String defValue;
        private final boolean inherit;
        private final PropertyValueConverter converter;
        private final ShorthandPropertyExpander expander;

        private Attribute(final String name, final String defValue,
                          final boolean inherit,
                          final PropertyValueConverter converter) {
            this(name, defValue, inherit, converter, null);
        }

        private Attribute(final String name, final String defValue,
                          final boolean inherit,
                          final ShorthandPropertyExpander expander) {
            this(name, defValue, inherit, null, expander);
        }

        private Attribute(final String name, final String defValue,
                          final boolean inherit,
                          final PropertyValueConverter converter,
                          final ShorthandPropertyExpander expander) {
            this.name = name;
            this.defValue = defValue;
            this.inherit = inherit;
            this.converter = converter;
            this.expander = expander;
        }

        public String getDefaultValue() {
            return defValue;
        }

        public boolean isInherited() {
            return inherit;
        }

        public String toString() {
            return name;
        }

        /**
         * Returns the converter to translate property values from
         * {@link StyleConstants} to <code>CSS</code> and vice versa.
         *
         * @return the converter.
         */
        PropertyValueConverter getConverter() {
            return converter;
        }

        /**
         * Returns the expander to convert a shorthand property to its
         * distinct parts.
         *
         * @return the expander.
         */
        ShorthandPropertyExpander getExpander() {
            return expander;
        }
    }

    interface ShorthandPropertyExpander {
        void parseAndExpandProperty(MutableAttributeSet attrs,
                                    String value);
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#background">'background'</a>.
     */
    static final class BackgroundExpander implements ShorthandPropertyExpander {

        public void parseAndExpandProperty(final MutableAttributeSet attrs,
                                           final String value) {
            ColorProperty color = null;
            ImageValue image = null;
            BackgroundRepeat repeat = null;
            BackgroundAttachment attachment = null;
            BackgroundPosition[] position = new BackgroundPosition[2];
            String[] parts = split(value.trim());
            if (parts == null) {
                return;
            }
            for (int i = 0; i < parts.length; i++) {
                if (color == null) {
                    color = (ColorProperty)BackgroundColor.factory
                                                          .toCSS(parts[i]);
                    if (color != null) {
                        continue;
                    }
                }

                if (image == null) {
                    image = (ImageValue)ImageValue.NONE.toCSS(parts[i]);
                    if (image != null) {
                        continue;
                    }
                }

                if (repeat == null) {
                    repeat = (BackgroundRepeat)BackgroundRepeat.factory
                                                               .toCSS(parts[i]);
                    if (repeat != null) {
                        continue;
                    }
                }

                if (attachment == null) {
                    attachment = (BackgroundAttachment)BackgroundAttachment
                                                       .factory.toCSS(parts[i]);
                    if (attachment != null) {
                        continue;
                    }
                }

                if (position[0] == null) {
                    position[0] =
                        (BackgroundPosition)Attribute.BACKGROUND_POSITION
                                            .getConverter().toCSS(parts[i]);
                    if (position[0] != null) {
                        continue;
                    }
                }

                if (position[1] == null) {
                    position[1] =
                        (BackgroundPosition)Attribute.BACKGROUND_POSITION
                                            .getConverter().toCSS(parts[i]);
                    if (position[1] != null) {
                        continue;
                    }
                }

                return;
            }

            final PropertyValueConverter bgPosConverter =
                Attribute.BACKGROUND_POSITION.getConverter();
            // Attribute.BACKGROUND_POSITION.getDefaultValue() must be "0% 0%"
            PropertyValueConverter bgPosition =
                position[0] == null
                ? bgPosConverter.toCSS("0% 0%"/*Attribute.BACKGROUND_POSITION
                                                .getDefaultValue()*/)
                : (position[1] != null
                   ? bgPosConverter.toCSS(position[0] + " " + position[1])
                   : position[0]);

            if (bgPosition == null) {
                return;
            }

            attrs.addAttribute(Attribute.BACKGROUND_COLOR,
                               color != null ? color : BackgroundColor.factory);
            attrs.addAttribute(Attribute.BACKGROUND_IMAGE,
                               image != null ? image : ImageValue.NONE);
            attrs.addAttribute(Attribute.BACKGROUND_REPEAT,
                               repeat != null ? repeat
                                              : BackgroundRepeat.factory);
            attrs.addAttribute(Attribute.BACKGROUND_ATTACHMENT,
                               attachment != null
                               ? attachment : BackgroundAttachment.factory);
            attrs.addAttribute(Attribute.BACKGROUND_POSITION, bgPosition);
        }

        private static String[] split(final String value) {
            if (value.indexOf("url(") != -1) {
                Matcher matcher = ListStyleExpander.URL_PATTERN.matcher(value);
                if (!matcher.find()) {
                    return null;
                }
                final int urlStart = matcher.start();
                final int urlEnd   = matcher.end();
                final String url = value.substring(urlStart, urlEnd);
                final String rest = (value.substring(0, urlStart)
                                     + value.substring(urlEnd)).trim();
                if (rest.length() != 0) {
                    if (rest.indexOf("url(") != -1) {
                        return null;
                    }

                    String[] parts = BorderColor.SPLIT_PATTERN.split(rest);
                    String[] result = new String[parts.length + 1];
                    System.arraycopy(parts, 0, result, 0, parts.length);
                    result[parts.length] = url;

                    return result;
                }

                return new String[] {url};
            }

            return BorderColor.SPLIT_PATTERN.split(value);
        }
    }

    /**
     * Describes converters of attribute values from {@link StyleConstants}
     * notation to CSS and vice versa.
     * <p>
     * The convertion may be not single-valued transformation, i.e.
     * the following expression is generally speaking <code>false</code>:
     * <pre>
     * value.equals(toCSS(value).fromCSS())
     * </pre>
     * <p>
     * The result of convertion to CSS notation is instance of
     * <code>PropertyValueConverter</code>. This object holds values for
     * both notations. It returns its <code>StyleConstants</code> representation
     * from {@link CSS.PropertyValueConverter#fromCSS() fromCSS()} method.
     * The CSS-styled value should be returned from <code>toString()</code>.
     */
    interface PropertyValueConverter {
        /**
         * Converts an attribute value from <code>StyleConstants</code> to
         * <code>CSS</code>.
         *
         * @param value the value to convert.
         * @return wrapper object which holds both values.
         */
        PropertyValueConverter toCSS(Object value);

        /**
         * Returns the <code>StyleConstants</code> representation of a CSS
         * property value stored.
         *
         * @return the converted value.
         */
        Object fromCSS();
    }

    interface RelativeValueResolver {
        Object getComputedValue(View view);
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#background-attachment">'background-attachment'</a>.
     */
    static final class BackgroundAttachment extends FixedSetValues {
        static final BackgroundAttachment factory = new BackgroundAttachment(0);

        private static final String[] VALID_VALUES = {
            "scroll", "fixed"
        };
        private static final BackgroundAttachment[] VALUE_HOLDERS =
            new BackgroundAttachment[VALID_VALUES.length];

        static {
            VALUE_HOLDERS[0] = factory;
        }

        private BackgroundAttachment(final int index) {
            super(index);
        }

        String[] getValidValues() {
            return VALID_VALUES;
        }

        PropertyValueConverter[] getValueHolders() {
            return VALUE_HOLDERS;
        }

        PropertyValueConverter createValueHolder(final int valueIndex) {
            return new BackgroundAttachment(valueIndex);
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#background-color">'background-color'</a>.
     */
    static final class BackgroundColor extends ColorProperty {
        static final BackgroundColor factory = new BackgroundColor();

        private BackgroundColor() {
            super("transparent", null);
        }

        public PropertyValueConverter toCSS(final Object value) {
            return "transparent".equals(value) ? factory : super.toCSS(value);
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#background-image">'background-image'</a>
     * and
     * <a href="http://www.w3.org/TR/CSS1#list-style-image">'list-style-image'</a>.
     */
    static final class ImageValue implements PropertyValueConverter {
        static final ImageValue NONE = new ImageValue();

        private final String value;
        private final String path;
        private BackgroundImageLoader imageLoader;

        private final List listeners = new ArrayList();

        private ImageValue() {
            this("none", null);
        }

        private ImageValue(final String value, final String path) {
            this.value = value;
            this.path  = path;
        }

        public PropertyValueConverter toCSS(final Object value) {
            if ("none".equals(value)) {
                return NONE;
            }

            String path = extractPath((String)value);
            return path != null ? new ImageValue((String)value, path)
                                : null;
        }

        public Object fromCSS() {
            return null;
        }

        public String toString() {
            return value;
        }

        void loadImage(final URL base) {
            if (path != null && imageLoader == null) {

                final URL url = HTML.resolveURL(path, base);
                imageLoader = new BackgroundImageLoader(url, true, -1, -1) {
                    protected void onReady() {
                        super.onReady();
                        notifyViews();
                    }
                };
            }
        }

        int getWidth() {
            return imageLoader != null ? imageLoader.getWidth() : -1;
        }

        int getHeight() {
            return imageLoader != null ? imageLoader.getHeight() : -1;
        }

        Image getImage() {
            return imageLoader != null && imageLoader.isReady()
                   ? imageLoader.getImage() : null;
        }

        synchronized void addListener(final ViewUpdater viewUpdater) {
            if (imageLoader != null
                && !imageLoader.isReady() && !imageLoader.isError()) {

                listeners.add(viewUpdater);
            }
        }

        private static String extractPath(final String url) {
            if (!(url.startsWith("url(") && url.endsWith(")"))) {
                return null;
            }
            char c = url.charAt(4);
            boolean quoted = c == '\'' || c == '"';
            if (quoted && url.charAt(url.length() - 2) != c) {
                return null;
            }
            return url.substring(4 + (quoted ? 1 : 0),
                                 url.length() - 1 - (quoted ? 1 : 0));
        }

        private synchronized void notifyViews() {
            Iterator it = listeners.iterator();
            while (it.hasNext()) {
                ((ViewUpdater)it.next()).updateView();
            }
            listeners.clear();
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#background-position">'background-position'</a>.
     */
    static final class BackgroundPosition implements PropertyValueConverter {
        private static final String[] KEYWORDS = new String[] {
            "top", "center", "bottom", "left", "right"
        };

        private static final String[] KEYWORD_VALUE_STRS =
            new String[] {"0%", "50%", "100%"};
        private static final FloatValue[] KEYWORD_VALUES = new FloatValue[3];

        private static final int KV_0   = 0;
        private static final int KV_50  = 1;
        private static final int KV_100 = 2;

        private String theValue;
        private FloatValue horz;
        private FloatValue vert;

        BackgroundPosition() {
            this(null, null, null);
        }

        private BackgroundPosition(final String theValue,
                                   final int hz,
                                   final int vt) {
            this(theValue, getKeywordValue(hz), getKeywordValue(vt));
        }

        private BackgroundPosition(final String theValue,
                                   final FloatValue horz,
                                   final FloatValue vert) {
            this.theValue = theValue;
            this.horz = horz;
            this.vert = vert;
        }

        public PropertyValueConverter toCSS(final Object value) {
            return parseValue((String)value);
        }

        public Object fromCSS() {
            return null;
        }

        public String toString() {
            return theValue;
        }

        private PropertyValueConverter parseValue(final String value) {
            String[] parts = value.split("\\s+");
            if (parts.length > 2) {
                return null;
            }

            if (parts.length == 1) {
                int index = getKeywordIndex(parts[0]);
                if (index != -1) {
                    switch (index) {
                    case 0: // top
                        horz = getKeywordValue(KV_50);
                        vert = getKeywordValue(KV_0);
                        break;

                    case 1: // center
                        horz = getKeywordValue(KV_50);
                        vert = getKeywordValue(KV_50);
                        break;

                    case 2: // bottom
                        horz = getKeywordValue(KV_50);
                        vert = getKeywordValue(KV_100);
                        break;

                    case 3: // left
                        horz = getKeywordValue(KV_0);
                        vert = getKeywordValue(KV_50);
                        break;

                    case 4: // right
                        horz = getKeywordValue(KV_100);
                        vert = getKeywordValue(KV_50);
                        break;

                    default:
                        return null;
                    }
                    return new BackgroundPosition(value, horz, vert);
                }
                horz = (FloatValue)FloatValue.factory.toCSS(parts[0]);
                return horz == null
                       ? null
                       : new BackgroundPosition(value, horz,
                                                getKeywordValue(KV_50));
            }

            int hzIndex = getKeywordIndex(parts[0]);
            int vtIndex = getKeywordIndex(parts[1]);
            if (hzIndex == -1 && vtIndex != -1
                || hzIndex != -1 && vtIndex == -1) {

                // Combinations of keywords and length or percentage values
                // are not allowed according to CSS1 spec, 'background-position'.
                return null;
            }

            if (hzIndex == -1 && vtIndex == -1) {
                horz = (FloatValue)FloatValue.factory.toCSS(parts[0]);
                vert = (FloatValue)FloatValue.factory.toCSS(parts[1]);
                return horz == null || vert == null
                       ? null
                       : new BackgroundPosition(value, horz, vert);
            }

            if (isHorizontalIndex(hzIndex) && isVerticalIndex(vtIndex)) {
                return new BackgroundPosition(value,
                                              kwToValue(hzIndex),
                                              kwToValue(vtIndex));
            }

            if (isHorizontalIndex(vtIndex) && isVerticalIndex(hzIndex)) {
                return new BackgroundPosition(value,
                                              kwToValue(vtIndex),
                                              kwToValue(hzIndex));
            }

            return null;
        }

        private static int getKeywordIndex(final String kw) {
            for (int i = 0; i < KEYWORDS.length; i++) {
                if (KEYWORDS[i].equals(kw)) {
                    return i;
                }
            }
            return -1;
        }

        private static boolean isHorizontalIndex(final int kwIndex) {
            return kwIndex == 1 || kwIndex == 3 || kwIndex == 4;
        }

        private static boolean isVerticalIndex(final int kwIndex) {
            return kwIndex <= 2;
        }

        private static int kwToValue(final int kwIndex) {
            switch (kwIndex) {
            case 0: // top
            case 3: // left
                return 0;

            case 1: // center
                return 1;

            case 2: // bottom
            case 4: // right
                return 2;

            default:
                return -1;
            }
        }

        private static FloatValue getKeywordValue(final int valueIndex) {
            if (KEYWORD_VALUES[valueIndex] == null) {
                // valueIndex must be in the range 0..2
                KEYWORD_VALUES[valueIndex] =
                    new FloatValue(KEYWORD_VALUE_STRS[valueIndex],
                                   50 * valueIndex,
                                   Length.RELATIVE_UNITS_PERCENTAGE);
            }
            return KEYWORD_VALUES[valueIndex];
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#background-repeat">'background-repeat'</a>.
     */
    static final class BackgroundRepeat extends FixedSetValues {
        static final BackgroundRepeat factory = new BackgroundRepeat(0);

        static final int REPEAT = 0;
        static final int REPEAT_X = 1;
        static final int REPEAT_Y = 2;
        static final int NO_REPEAT = 3;

        private static final String[] VALID_VALUES = {
            "repeat", "repeat-x", "repeat-y", "no-repeat"
        };
        private static final BackgroundRepeat[] VALUE_HOLDERS =
            new BackgroundRepeat[VALID_VALUES.length];

        static {
            VALUE_HOLDERS[0] = factory;
        }

        private BackgroundRepeat(final int index) {
            super(index);
        }

        String[] getValidValues() {
            return VALID_VALUES;
        }

        PropertyValueConverter[] getValueHolders() {
            return VALUE_HOLDERS;
        }

        PropertyValueConverter createValueHolder(final int valueIndex) {
            return new BackgroundRepeat(valueIndex);
        }
    }


    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#border-color">'border-color'</a>.
     */
    static final class BorderColor implements PropertyValueConverter {
        static final Pattern SPLIT_PATTERN =
            Pattern.compile("(?<!,)\\s+");

        private static final PropertyValueConverter converter =
            ColorProperty.factory;

        private final ColorProperty[] colors;

        private String value;

        BorderColor() {
            this(null, null);
        }

        BorderColor(final ColorProperty[] colors) {
            this(colors, getDeclaration(colors));
        }

        BorderColor(final ColorProperty[] colors, final String value) {
            this.colors = colors;
            this.value = value;
        }

        public PropertyValueConverter toCSS(final Object value) {
            final ColorProperty[] cp = new ColorProperty[4];
            final String[] values = SPLIT_PATTERN.split((String)value);
            for (int i = 0; i < cp.length; i++) {
                if (i < values.length) {
                    cp[i] = (ColorProperty)converter.toCSS(values[i]);
                    if (cp[i] == null) {
                        return null;
                    }
                } else {
                    cp[i] = cp[i >= 3 ? 1 : 0];
                }
            }
            return new BorderColor(cp, (String)value);
        }

        public Object fromCSS() {
            return null;
        }

        public String toString() {
            return value;
        }

        void setSideColor(final int sideIndex, final ColorProperty color) {
            colors[sideIndex] = color;
            value = getDeclaration(colors);
        }

        ColorProperty getSideColor(final int sideIndex) {
            return colors[sideIndex];
        }

        private static String getDeclaration(final ColorProperty[] colors) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < colors.length; i++) {
                if (i > 0) {
                    result.append(' ');
                }
                result.append(colors[i] == null ? "white"
                                                : colors[i].toString());
            }
            return result.toString();
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#border-style">'border-style'</a>.
     * <p>This class stores four {@link BorderStyleValue} for each side of the box.
     */
    static final class BorderStyle implements PropertyValueConverter {
        private static final PropertyValueConverter converter =
            BorderStyleValue.factory;

        private final BorderStyleValue[] styles;

        private String value;

        BorderStyle() {
            this(null, null);
        }

        BorderStyle(final BorderStyleValue[] styles) {
            this(styles, getDeclaration(styles));
        }

        BorderStyle(final BorderStyleValue[] styles,
                            final String value) {
            this.styles = styles;
            this.value = value;
        }

        public PropertyValueConverter toCSS(final Object value) {
            final BorderStyleValue[] bs = new BorderStyleValue[4];
            final String[] values = ((String)value).split("\\s+");
            for (int i = 0; i < bs.length; i++) {
                if (i < values.length) {
                    bs[i] = (BorderStyleValue)converter.toCSS(values[i]);
                    if (bs[i] == null) {
                        return null;
                    }
                } else {
                    bs[i] = bs[i >= 3 ? 1 : 0];
                }
            }
            return new BorderStyle(bs, (String)value);
        }

        public Object fromCSS() {
            return null;
        }

        public String toString() {
            return value;
        }

        void setSideStyle(final int sideIndex, final BorderStyleValue style) {
            styles[sideIndex] = style;
            value = getDeclaration(styles);
        }

        BorderStyleValue getSideStyle(final int sideIndex) {
            return styles[sideIndex] != null ? styles[sideIndex]
                                             : (BorderStyleValue)converter;
        }

        private static String getDeclaration(final BorderStyleValue[] styles) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < styles.length; i++) {
                if (i > 0) {
                    result.append(' ');
                }
                result.append(styles[i] == null ? "none"
                                                : styles[i].toString());
            }
            return result.toString();
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#border-style">'border-style'</a>.
     * <p>Defines the particular value for a side of the box.
     */
    static final class BorderStyleValue extends FixedSetValues {
        static final int NONE   = 0;
        static final int SOLID  = 1;
        static final int DOTTED = 2;
        static final int DASHED = 3;
        static final int DOUBLE = 4;
        static final int GROOVE = 5;
        static final int RIDGE  = 6;
        static final int INSET  = 7;
        static final int OUTSET = 8;

        static final BorderStyleValue factory = new BorderStyleValue(0);

        private static final String[] VALID_VALUES = {
            "none", "solid", "dotted", "dashed",
            "double", "groove", "ridge", "inset",
            "outset"
        };
        private static final BorderStyleValue[] VALUE_HOLDERS =
            new BorderStyleValue[VALID_VALUES.length];

        static {
            VALUE_HOLDERS[0] = factory;
        }

        private BorderStyleValue(final int index) {
            super(index);
        }

        String[] getValidValues() {
            return VALID_VALUES;
        }

        PropertyValueConverter[] getValueHolders() {
            return VALUE_HOLDERS;
        }

        PropertyValueConverter createValueHolder(final int valueIndex) {
            return new BorderStyleValue(valueIndex);
        }
    }

    /**
     * The implementation is based on CSS1.
     * It defines the particular value for a side of the box:
     * <ul>
     * <li><a href="http://www.w3.org/TR/CSS1#border-width-top">'border-width-top'</a>
     * <li><a href="http://www.w3.org/TR/CSS1#border-width-right">'border-width-right'</a>
     * <li><a href="http://www.w3.org/TR/CSS1#border-width-bottom">'border-width-bottom'</a>
     * <li><a href="http://www.w3.org/TR/CSS1#border-width-left">'border-width-left'</a>
     * </ul>
     */
    static final class BorderWidthValue extends FloatValue {
        static final BorderWidthValue factory =
            new BorderWidthValue("medium", 3);

        private static final BorderWidthValue[] WIDTHS = {
            new BorderWidthValue("thin", 1),
            factory,
            new BorderWidthValue("thick", 5)
        };

        private BorderWidthValue(final String value, final float theValue) {
            super(value, theValue, RELATIVE_UNITS_UNDEFINED);
        }

        private BorderWidthValue(final String value, final float theValue,
                                 final int rUnits) {
            super(value, theValue, rUnits);
        }

        public PropertyValueConverter toCSS(final Object value) {
            for (int i = 0; i < WIDTHS.length; i++) {
                if (WIDTHS[i].sValue.equals(value)) {
                    return WIDTHS[i];
                }
            }

            return super.toCSS(value);
        }

        PropertyValueConverter create(final String strValue,
                                      final float theValue, final int rUnits) {
            return theValue < 0 || rUnits == RELATIVE_UNITS_PERCENTAGE
                   ? null
                   : new BorderWidthValue(strValue, theValue, rUnits);
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <ul>
     * <li><a href="http://www.w3.org/TR/CSS1#border-top">'border-top'</a>
     * <li><a href="http://www.w3.org/TR/CSS1#border-right">'border-right'</a>
     * <li><a href="http://www.w3.org/TR/CSS1#border-bottom">'border-bottom'</a>
     * <li><a href="http://www.w3.org/TR/CSS1#border-left">'border-left'</a>
     * </ul>
     */
    static final class BorderSideExpander implements ShorthandPropertyExpander {
        static final int TOP_SIDE    = 0;
        static final int RIGHT_SIDE  = 1;
        static final int BOTTOM_SIDE = 2;
        static final int LEFT_SIDE   = 3;

        private final int sideIndex;
        private final Attribute widthKey;

        BorderSideExpander(final int sideIndex, final Attribute widthKey) {
            this.sideIndex = sideIndex;
            this.widthKey  = widthKey;
        }

        public void parseAndExpandProperty(final MutableAttributeSet attrs,
                                           final String value) {
            BorderStyleValue style = null;
            ColorProperty color = null;
            BorderWidthValue width = null;

            final String[] parts = BorderColor.SPLIT_PATTERN.split(value);
            for (int i = 0; i < parts.length; i++) {
                if (style == null) {
                    style = (BorderStyleValue)BorderStyle.converter
                                              .toCSS(parts[i]);
                    if (style != null) {
                        continue;
                    }
                }

                if (color == null) {
                    color = (ColorProperty)ColorProperty.factory
                                           .toCSS(parts[i]);
                    if (color != null) {
                        continue;
                    }
                }

                if (width == null) {
                    width = (BorderWidthValue)BorderWidthValue.factory
                                              .toCSS(parts[i]);
                    if (width != null) {
                        continue;
                    }
                }

                // Contains an unknown value - ignore the entire declaration
                return;
            }

            if (style != null) {
                final BorderStyle styleValue =
                    (BorderStyle)attrs.getAttribute(Attribute.BORDER_STYLE);
                if (styleValue != null) {
                    styleValue.setSideStyle(sideIndex, style);
                } else {
                    BorderStyleValue[] styles = new BorderStyleValue[4];
                    styles[sideIndex] = style;
                    attrs.addAttribute(Attribute.BORDER_STYLE,
                                       new BorderStyle(styles));
                }
            }

            if (color != null) {
                final BorderColor colorValue =
                    (BorderColor)attrs.getAttribute(Attribute.BORDER_COLOR);
                if (colorValue != null) {
                    colorValue.setSideColor(sideIndex, color);
                } else {
                    ColorProperty[] colors = new ColorProperty[4];
                    colors[sideIndex] = color;
                    attrs.addAttribute(Attribute.BORDER_COLOR,
                                       new BorderColor(colors));
                }
            }

            if (width != null) {
                attrs.addAttribute(widthKey, width);
            }
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#border">'border'</a>.
     */
    static final class BorderExpander implements ShorthandPropertyExpander {
        public void parseAndExpandProperty(final MutableAttributeSet attrs,
                                           final String value) {
            BorderStyleValue style = null;
            ColorProperty color = null;
            BorderWidthValue width = null;

            final String[] parts = BorderColor.SPLIT_PATTERN.split(value);
            for (int i = 0; i < parts.length; i++) {
                if (style == null) {
                    style = (BorderStyleValue)BorderStyle.converter
                                              .toCSS(parts[i]);
                    if (style != null) {
                        continue;
                    }
                }

                if (color == null) {
                    color = (ColorProperty)ColorProperty.factory
                                           .toCSS(parts[i]);
                    if (color != null) {
                        continue;
                    }
                }

                if (width == null) {
                    width = (BorderWidthValue)BorderWidthValue.factory
                                              .toCSS(parts[i]);
                    if (width != null) {
                        continue;
                    }
                }

                // Contains an unknown value - ignore the entire declaration
                return;
            }

            if (style != null) {
                attrs.addAttribute(Attribute.BORDER_STYLE,
                                   new BorderStyle(new BorderStyleValue[] {
                                                       style, style,
                                                       style, style
                                                   },
                                                   style.toString()));
            }

            if (color != null) {
                attrs.addAttribute(Attribute.BORDER_COLOR,
                                   new BorderColor(new ColorProperty[] {
                                                       color, color,
                                                       color, color
                                                   },
                                                   color.toString()));
            }

            if (width != null) {
                attrs.addAttribute(Attribute.BORDER_TOP_WIDTH, width);
                attrs.addAttribute(Attribute.BORDER_RIGHT_WIDTH, width);
                attrs.addAttribute(Attribute.BORDER_BOTTOM_WIDTH, width);
                attrs.addAttribute(Attribute.BORDER_LEFT_WIDTH, width);
            }
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#color">'color'</a>.
     * @see <a href="http://www.w3.org/TR/CSS1#color-units">Color Units</a>.
     */
    static class ColorProperty implements PropertyValueConverter {
        static final ColorProperty factory = new ColorProperty();

        private static final char[] zeros = {'0', '0', '0', '0', '0'};
        private static final Map colorMap = new HashMap();
        private static final Pattern RGB_SEPARATOR =
            Pattern.compile(",");

        private final Color color;
        private final String cssValue;

        static {
            Color color;

            color = Color.CYAN;
            colorMap.put("aqua", color);
            colorMap.put("00ffff", color);

            color = Color.BLACK;
            colorMap.put("black", color);
            colorMap.put("000000", color);

            color = Color.BLUE;
            colorMap.put("blue", color);
            colorMap.put("0000ff", color);

            color = Color.MAGENTA;
            colorMap.put("fuchsia", color);
            colorMap.put("ff00ff", color);

            color = Color.GRAY;
            colorMap.put("gray", color);
            colorMap.put("808080", color);

            color = new Color(0, 128, 0);
            colorMap.put("green", color);
            colorMap.put("008000", color);

            color = Color.GREEN;
            colorMap.put("lime", color);
            colorMap.put("00ff00", color);

            color = new Color(128, 0, 0);
            colorMap.put("maroon", color);
            colorMap.put("800000", color);

            color = new Color(0, 0, 128);
            colorMap.put("navy", color);
            colorMap.put("000080", color);

            color = new Color(128, 128, 0);
            colorMap.put("olive", color);
            colorMap.put("808000", color);

            color = new Color(128, 0, 128);
            colorMap.put("purple", color);
            colorMap.put("800080", color);

            color = Color.RED;
            colorMap.put("red", color);
            colorMap.put("ff0000", color);

            color = Color.LIGHT_GRAY;
            colorMap.put("silver", color);
            colorMap.put("c0c0c0", color);

            color = new Color(0, 128, 128);
            colorMap.put("teal", color);
            colorMap.put("008080", color);

            color = Color.WHITE;
            colorMap.put("white", color);
            colorMap.put("ffffff", color);

            color = Color.YELLOW;
            colorMap.put("yellow", color);
            colorMap.put("ffff00", color);
        }

        ColorProperty(final String cssValue, final Color color) {
            this.cssValue = cssValue;
            this.color = color;
        }

        private ColorProperty() {
            this(null, null);
        }

        private ColorProperty(final Color color) {
            this(colorToString(color), color);
        }

        public PropertyValueConverter toCSS(final Object value) {
            if (value instanceof String) {
                final String cssValue = (String)value;
                Color c = parseRGB(cssValue);
                if (c == null) {
                    c = stringToColor(cssValue);
                }
                return c != null ? new ColorProperty(cssValue, c) : null;
            }
            return new ColorProperty((Color)value);
        }

        public Object fromCSS() {
            return color;
        }

        public String toString() {
            return cssValue;
        }

        static Color stringToColor(final String colorName) {
            if (Utilities.isEmptyString(colorName)) {
                return null;
            }

            final String lower = colorName.toLowerCase();
            if (lower.charAt(0) == '#') {
                final StringBuilder name = new StringBuilder(6);
                if (lower.length() == 4) {
                    for (int i = 1; i < 4; i++) {
                        name.append(lower.charAt(i))
                            .append(lower.charAt(i));
                    }
                } else if (lower.length() != 7) {
                    return null;
                } else {
                    name.append(lower.substring(1));
                }

                final Color result = (Color)colorMap.get(name.toString());
                return result != null
                       ? result
                       : new Color(Integer.parseInt(name.toString(), 16));
            } else {
                return (Color)colorMap.get(lower);
            }
        }

        static Color parseRGB(final String rgbColor) {
            if (Utilities.isEmptyString(rgbColor)
                || !rgbColor.startsWith("rgb(") && !rgbColor.endsWith(")")) {

                return null;
            }
            final String[] colorComponents =
                RGB_SEPARATOR.split(rgbColor.substring(4,
                                                       rgbColor.length() - 1));
            if (colorComponents.length < 3) {
                return null;
            }
            final int[] rgb = new int[3];
            boolean percentage = false;
            for (int i = 0; i < colorComponents.length; i++) {
                final String cc = colorComponents[i].trim();
                if (Utilities.isEmptyString(cc)) {
                    return null;
                }
                percentage |= cc.charAt(cc.length() - 1) == '%';
                if (percentage && cc.charAt(cc.length() - 1) != '%') {
                    return null;
                }
                rgb[i] = percentage
                         ? (int)(Double.parseDouble(cc.substring(0,
                                                                 cc.length()
                                                                 - 1))
                                 * 255 / 100)
                         : Integer.parseInt(cc);
                rgb[i] = Utilities.range(rgb[i], 0, 255);
            }
            return new Color(rgb[0], rgb[1], rgb[2]);
        }

        final Color getColor() {
            return color;
        }

        private static String colorToString(final Color color) {
            final StringBuilder result = new StringBuilder(7);
            final String hex = Integer.toHexString(color.getRGB() & 0x00FFFFFF);
            result.append('#').append(zeros, 0, 6 - hex.length()).append(hex);
            return result.toString();
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#clear">'clear'</a>.
     */
    static final class Clear extends FixedSetValues {
        private static final String[] VALID_VALUES = {
            "none", "left", "right", "both"
        };
        private static final Clear[] VALUE_HOLDERS =
            new Clear[VALID_VALUES.length];

        Clear() {
            this(-1);
        }

        private Clear(final int index) {
            super(index);
        }

        String[] getValidValues() {
            return VALID_VALUES;
        }

        PropertyValueConverter[] getValueHolders() {
            return VALUE_HOLDERS;
        }

        PropertyValueConverter createValueHolder(final int valueIndex) {
            return new Clear(valueIndex);
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#display">'display'</a>.
     */
    static final class Display extends FixedSetValues {
        private static final String[] VALID_VALUES = {
            "block", "inline", "list-item", "none"
        };
        private static final Display[] VALUE_HOLDERS =
            new Display[VALID_VALUES.length];

        Display() {
            this(-1);
        }

        private Display(final int index) {
            super(index);
        }

        String[] getValidValues() {
            return VALID_VALUES;
        }

        PropertyValueConverter[] getValueHolders() {
            return VALUE_HOLDERS;
        }

        PropertyValueConverter createValueHolder(final int valueIndex) {
            return new Display(valueIndex);
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#float">'float'</a>.
     */
    static final class FloatProperty extends FixedSetValues {
        private static final String[] VALID_VALUES = {
            "none", "left", "right"
        };
        private static final FloatProperty[] VALUE_HOLDERS =
            new FloatProperty[VALID_VALUES.length];

        FloatProperty() {
            this(-1);
        }

        private FloatProperty(final int index) {
            super(index);
        }

        String[] getValidValues() {
            return VALID_VALUES;
        }

        PropertyValueConverter[] getValueHolders() {
            return VALUE_HOLDERS;
        }

        PropertyValueConverter createValueHolder(final int valueIndex) {
            return new FloatProperty(valueIndex);
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#font-family">'font-family'</a>.
     */
    static final class FontFamily implements PropertyValueConverter {
        private static final String SANS_SERIF_FAMILY = "sans-serif";
        private static final String SERIF_FAMILY = "serif";
        private static final String MONOSPACE_FAMILY = "monospace";

        private static final String SANS_SERIF = "SansSerif";
        private static final String SERIF = "Serif";
        private static final String MONOSPACED = "Monospaced";

        static final String DEFAULT = SANS_SERIF;

        private static final Pattern SPLIT_PATTERN =
            Pattern.compile("\\s*,\\s*");

        private static final String[] fontFamilies;

        static {
            GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
            fontFamilies = ge.getAvailableFontFamilyNames();
            Arrays.sort(fontFamilies);
        }

        private final String value;
        private final String family;

        FontFamily() {
            value = null;
            family = null;
        }

        private FontFamily(final Object value) {
            this.value = (String)value;
            this.family = init();
        }

        public PropertyValueConverter toCSS(final Object value) {
            return new FontFamily(value);
        }

        public Object fromCSS() {
            return family;
        }

        public String toString() {
            return value;
        }

        private String init() {
            final String[] familyList = SPLIT_PATTERN.split(value);
            for (int i = 0; i < familyList.length; i++) {
                if (SANS_SERIF_FAMILY.equals(familyList[i])) {
                    return SANS_SERIF;
                }
                if (SERIF_FAMILY.equals(familyList[i])) {
                    return SERIF;
                }
                if (MONOSPACE_FAMILY.equals(familyList[i])) {
                    return MONOSPACED;
                }
                if (Arrays.binarySearch(fontFamilies, familyList[i]) >= 0) {
                    return familyList[i];
                }
            }
            return SANS_SERIF;
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#font-size">'font-size'</a>.
     */
    static final class FontSize extends Length
        implements RelativeValueResolver {

        static final Integer[] SIZE_TABLE = {
            new Integer(8),  new Integer(10), new Integer(12), new Integer(14),
            new Integer(18), new Integer(24), new Integer(36)
        };

        static final int RELATIVE_UNITS_SMALLER = 10;
        static final int RELATIVE_UNITS_LARGER = 11;

        private static final String[] VALUE_TABLE = {
            "xx-small", "x-small", "small",
            "medium",
            "large", "x-large", "xx-large"
        };

        private final Integer size;

        FontSize() {
            super();
            size = null;
        }

        private FontSize(final String strValue, final int rUnits) {
            super(strValue, rUnits);
            size = getDefaultValue();
        }

        private FontSize(final String strValue, final Integer theSize) {
            super(strValue, RELATIVE_UNITS_UNDEFINED);
            size = theSize;
        }

        private FontSize(final String strValue,
                         final int theSize, final int rUnits) {
            super(strValue, rUnits);
            size = new Integer(theSize);
        }

        public PropertyValueConverter toCSS(final Object value) {
            if (value instanceof String) {
                if ("smaller".equals(value)) {
                    return new FontSize((String)value, RELATIVE_UNITS_SMALLER);
                }
                if ("larger".equals(value)) {
                    return new FontSize((String)value, RELATIVE_UNITS_LARGER);
                }
                Integer theSize = valueToSize((String)value);
                return theSize != null ? new FontSize((String)value, theSize)
                                       : super.toCSS(value);
            }
            if (value instanceof Integer) {
                final int index = sizeValueIndex(((Integer)value).intValue());
                return new FontSize(VALUE_TABLE[index], SIZE_TABLE[index]);
            }
            return null;
        }

        public Object getComputedValue(final View view) {
            if (relativeUnits != RELATIVE_UNITS_UNDEFINED) {
                return new FontSize(sValue,
                                    (Integer)resolveRelativeValue(view));
            }
            return this;
        }

        static Integer getDefaultValue() {
            return SIZE_TABLE[2];
        }

        static int sizeValueIndex(final int size) {
            for (int i = 0; i < SIZE_TABLE.length; i++) {
                if (size <= SIZE_TABLE[i].intValue()) {
                    return i;
                }
            }
            return VALUE_TABLE.length - 1;
        }

        PropertyValueConverter create(final String strValue,
                                      final float theValue,
                                      final int rUnits) {
            if (theValue < 0) {
                return null;
            }
            return new FontSize(strValue, (int)theValue, rUnits);
        }

        Object getValue(final View view) {
            return size;
        }

        Object resolveRelativeValue(final View view) {
            if (view == null) {
                return getDefaultValue();
            }
            final View parent = view.getParent();
            if (parent == null) {
                return getDefaultValue();
            }

            final AttributeSet attr = parent.getAttributes();
            final Object fs = attr.getAttribute(Attribute.FONT_SIZE);
            final int fontSize = fs != null ? ((Length)fs).intValue(parent)
                                            : getDefaultValue().intValue();
            int sizeValueIndex;

            // calculation is defined by CSS1, http://www.w3.org/TR/CSS1#length-units
            switch (relativeUnits) {
            case RELATIVE_UNITS_EM:
                return new Integer(fontSize * size.intValue());

            case RELATIVE_UNITS_EX:
                return new Integer(fontSize * size.intValue() / 2);

            case RELATIVE_UNITS_PERCENTAGE:
                return new Integer(fontSize * size.intValue() / 100);

            case RELATIVE_UNITS_SMALLER:
                sizeValueIndex = sizeValueIndex(fontSize);
                return SIZE_TABLE[sizeValueIndex > 0 ? sizeValueIndex - 1 : 0];

            case RELATIVE_UNITS_LARGER:
                sizeValueIndex = sizeValueIndex(fontSize) + 1;
                return new Integer(fontSize * 120 / 100);

            default:
                System.err.println(Messages.getString("swing.err.07")); //$NON-NLS-1$
            }
            return getDefaultValue();
        }

        private static Integer valueToSize(final String value) {
            for (int i = 0; i < VALUE_TABLE.length; i++) {
                if (VALUE_TABLE[i].equals(value)) {
                    return SIZE_TABLE[i];
                }
            }
            return null;
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#font-style">'font-style'</a>.
     */
    static final class FontStyle implements PropertyValueConverter {
        private static final FontStyle OBLIQUE =
            new FontStyle("oblique", Boolean.FALSE);
        private static final FontStyle NORMAL =
            new FontStyle("normal", Boolean.FALSE);
        private static final FontStyle ITALIC =
            new FontStyle("italic", Boolean.TRUE);

        private final String  cssValue;
        private final Boolean value;

        FontStyle() {
            cssValue = null;
            value = null;
        }

        private FontStyle(final String cssValue, final Boolean value) {
            this.cssValue = cssValue;
            this.value = value;
        }

        public PropertyValueConverter toCSS(final Object value) {
            if (value instanceof Boolean) {
                return ((Boolean)value).booleanValue() ? ITALIC : NORMAL;
            } else if (ITALIC.cssValue.equals(value)) {
                return ITALIC;
            } else if (OBLIQUE.cssValue.equals(value)) {
                return OBLIQUE;
            } else if (NORMAL.cssValue.equals(value)) {
                return NORMAL;
            }

            return null;
        }

        public Object fromCSS() {
            return value;
        }

        public String toString() {
            return cssValue;
        }

    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#font-variant">'font-variant'</a>.
     */
    static final class FontVariant extends FixedSetValues {
        private static final String[] VALID_VALUES = {
            "normal", "small-caps"
        };
        private static final FontVariant[] VALUE_HOLDERS =
            new FontVariant[VALID_VALUES.length];

        FontVariant() {
            this(-1);
        }

        private FontVariant(final int index) {
            super(index);
        }

        String[] getValidValues() {
            return VALID_VALUES;
        }

        PropertyValueConverter[] getValueHolders() {
            return VALUE_HOLDERS;
        }

        PropertyValueConverter createValueHolder(final int valueIndex) {
            return new FontVariant(valueIndex);
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#font-weight">'font-weight'</a>.
     */
    static final class FontWeight implements PropertyValueConverter {
        private static final FontWeight BOLD =
            new FontWeight("bold", Boolean.TRUE);
        private static final FontWeight NORMAL =
            new FontWeight("normal", Boolean.FALSE);

        private static final String[] RELATIVE = {"bolder", "lighter"};
        private static final String[] ABSOLUTE = {
            "100", "200", "300", "400", "500", "600", "700", "800", "900"
        };
        private static final int BOLD_THRESHOLD = 5;

        private final String cssValue;
        private final Boolean value;

        FontWeight() {
            cssValue = null;
            value = null;
        }

        private FontWeight(final String cssValue, final Boolean value) {
            this.cssValue = cssValue;
            this.value = value;
        }

        public PropertyValueConverter toCSS(final Object value) {
            if (value instanceof Boolean) {
                return ((Boolean)value).booleanValue() ? BOLD : NORMAL;
            }

            if (BOLD.cssValue.equals(value)) {
                return BOLD;
            } else if (NORMAL.cssValue.equals(value)) {
                return NORMAL;
            }

            for (int i = 0; i < ABSOLUTE.length; i++) {
                if (ABSOLUTE[i].equals(value)) {
                    return new FontWeight((String)value,
                                          Boolean.valueOf(i >= BOLD_THRESHOLD));
                }
            }

            for (int i = 0; i < RELATIVE.length; i++) {
                if (RELATIVE[i].equals(value)) {
                    return new FontWeight((String)value,
                                          Boolean.valueOf(i == 0));
                }
            }

            return null;
        }

        public Object fromCSS() {
            return value;
        }

        public String toString() {
            return cssValue;
        }

    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#font">'font'</a>.
     */
    static final class FontExpander implements ShorthandPropertyExpander {
        private static final Pattern WS_SPLIT = Pattern.compile("\\s+");

        public void parseAndExpandProperty(final MutableAttributeSet attrs,
                                           final String value) {
            parse(value.trim(), attrs);
        }

        private void parse(final String value,
                           final MutableAttributeSet attrs) {
            final String[] parts = WS_SPLIT.split(value);
            FontStyle style = null;
            FontVariant variant = null;
            FontWeight weight = null;
            FontSize size = null;
            FloatValue lineHeight = null;
            int slashIndex = -1;
            int i;
            for (i = 0; i < parts.length; i++) {
                if (i > 3) {
                    return;
                }

                if ("normal".equals(parts[i])) {
                    continue;
                }

                if (style == null) {
                    style = (FontStyle)Attribute.FONT_STYLE.getConverter()
                                                           .toCSS(parts[i]);
                    if (style != null) {
                        continue;
                    }
                }

                if (variant == null) {
                    variant =
                        (FontVariant)Attribute.FONT_VARIANT.getConverter()
                                                           .toCSS(parts[i]);
                    if (variant != null) {
                        continue;
                    }
                }

                if (weight == null) {
                    weight = (FontWeight)Attribute.FONT_WEIGHT.getConverter()
                                                              .toCSS(parts[i]);
                    if (weight != null) {
                        continue;
                    }
                }

                if (size == null) {
                    slashIndex = parts[i].indexOf('/');
                    final String sizeValue = slashIndex == -1
                                             ? parts[i]
                                             : parts[i].substring(0, slashIndex);
                    size = (FontSize)Attribute.FONT_SIZE.getConverter()
                                                        .toCSS(sizeValue);
                    if (size != null) {
                        break;
                    }
                }

                return;
            }

            if (size == null) {
                return;
            }

            String lhValue;
            if (slashIndex > 0) {
                if (parts[i].length() > slashIndex + 1) {
                    lhValue  = parts[i].substring(slashIndex + 1);
                    i += 1;
                } else {
                    if (parts.length <= i + 1) {
                        return;
                    }
                    lhValue = parts[i + 1];
                    i += 2;
                }
            } else {
                if (parts.length <= i + 1) {
                    return;
                }

                if (parts[i + 1].length() == 1
                    && parts[i + 1].charAt(0) == '/') {

                    if (parts.length <= i + 2) {
                        return;
                    }
                    lhValue = parts[i + 2];
                    i += 3;
                } else if (parts[i + 1].startsWith("/")) {
                    lhValue = parts[i + 1].substring(1);
                    i += 2;
                } else {
                    lhValue = Attribute.LINE_HEIGHT.getDefaultValue();
                    i += 1;
                }
            }
            lineHeight = (FloatValue)Attribute.LINE_HEIGHT.getConverter()
                                                          .toCSS(lhValue);
            if (lineHeight == null) {
                return;
            }

            StringBuilder family = new StringBuilder();
            family.append(parts[i++]);
            for (; i < parts.length; i++) {
                family.append(' ')
                      .append(parts[i]);
            }
            if (family.length() == 0) {
                return;
            }


            if (style == null) {
                style = (FontStyle)Attribute.FONT_STYLE.getConverter()
                            .toCSS(Attribute.FONT_STYLE.getDefaultValue());
            }

            if (variant == null) {
                variant = (FontVariant)Attribute.FONT_VARIANT.getConverter()
                                .toCSS(Attribute.FONT_VARIANT.getDefaultValue());
            }

            if (weight == null) {
                weight = (FontWeight)Attribute.FONT_WEIGHT.getConverter()
                              .toCSS(Attribute.FONT_WEIGHT.getDefaultValue());
            }

//            if (lineHeight == null ) {
//                lineHeight = (FloatValue)Attribute.LINE_HEIGHT.getConverter()
//                                  .toCSS(Attribute.LINE_HEIGHT.getDefaultValue());
//            }

            attrs.addAttribute(Attribute.FONT_STYLE, style);
            attrs.addAttribute(Attribute.FONT_VARIANT, variant);
            attrs.addAttribute(Attribute.FONT_WEIGHT, weight);
            attrs.addAttribute(Attribute.FONT_SIZE, size);
            attrs.addAttribute(Attribute.LINE_HEIGHT, lineHeight);
            attrs.addAttribute(Attribute.FONT_FAMILY, family.toString());
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#text-align">'text-align'</a>.
     */
    static final class TextAlign implements PropertyValueConverter {
        private static final TextAlign LEFT =
            new TextAlign("left", new Integer(StyleConstants.ALIGN_LEFT));
        private static final TextAlign CENTER =
            new TextAlign("center", new Integer(StyleConstants.ALIGN_CENTER));
        private static final TextAlign RIGHT =
            new TextAlign("right", new Integer(StyleConstants.ALIGN_RIGHT));
        private static final TextAlign JUSTIFY =
            new TextAlign("justify",
                          new Integer(StyleConstants.ALIGN_JUSTIFIED));

        private final String align;
        private final Integer justification;

        TextAlign() {
            this.align = null;
            this.justification = null;
        }

        private TextAlign(final String align, final Integer justification) {
            this.align = align;
            this.justification = justification;
        }

        public String toString() {
            return align;
        }

        public PropertyValueConverter toCSS(final Object value) {
            return value instanceof Integer
                   ? convertIntegerValue((Integer)value)
                   : convertStringValue((String)value);
        }

        public Object fromCSS() {
            return justification;
        }

        private PropertyValueConverter convertIntegerValue(final Integer value) {
            switch (value.intValue()) {
            case StyleConstants.ALIGN_LEFT:
                return LEFT;

            case StyleConstants.ALIGN_CENTER:
                return CENTER;

            case StyleConstants.ALIGN_RIGHT:
                return RIGHT;

            case StyleConstants.ALIGN_JUSTIFIED:
                return JUSTIFY;

            default:
                return LEFT;
            }
        }

        private PropertyValueConverter convertStringValue(final String value) {
            if (LEFT.align.equals(value)) {
                return LEFT;
            }
            if (CENTER.align.equals(value)) {
                return CENTER;
            }
            if (RIGHT.align.equals(value)) {
                return RIGHT;
            }
            if (JUSTIFY.align.equals(value)) {
                return JUSTIFY;
            }
            return null;
        }
    }

    /**
     * Storage for
     * <a href="http://www.w3.org/TR/CSS1#length-units">Length Units</a>
     * of float type.
     */
    static class FloatValue extends Length {
        static final FloatValue factory = new FloatValue();

        private static final Float ZERO = new Float(0);

        private final Float theValue;

        FloatValue() {
            super();
            theValue = null;
        }

        protected FloatValue(final String strValue,
                           final float theValue,
                           final int units) {
            this(strValue, new Float(theValue), units);
        }

        protected FloatValue(final String strValue,
                           final Float theValue,
                           final int units) {
            super(strValue, units);
            this.theValue = theValue;
        }

        public PropertyValueConverter toCSS(final Object value) {
            if (value instanceof Float) {
                return new FloatValue(value.toString() + "pt", (Float)value,
                                      RELATIVE_UNITS_UNDEFINED);
            }
            return super.toCSS(value);
        }

        PropertyValueConverter create(final String strValue,
                                      final float theValue,
                                      final int rUnits) {
            return new FloatValue(strValue, theValue, rUnits);
        }

        Object resolveRelativeValue(final View view) {
            if (view == null) {
                return ZERO;
            }

            final AttributeSet attr = view.getAttributes();

            switch (relativeUnits) {
            case RELATIVE_UNITS_EM:
            case RELATIVE_UNITS_EX:
                final Object fs = attr.getAttribute(Attribute.FONT_SIZE);
                final float fontSize = fs != null
                                       ? ((Length)fs).floatValue(view)
                                       : FontSize.getDefaultValue().floatValue();

                float result = fontSize * theValue.floatValue();
                if (relativeUnits == RELATIVE_UNITS_EX) {
                    result /= 2;
                }
                return new Float(result);

            case RELATIVE_UNITS_PERCENTAGE:
                View parent = view.getParent();
                if (!(parent instanceof BoxView)) {
                    return ZERO;
                }
                float width = ((BoxView)parent).getWidth();
                if (width >= Integer.MAX_VALUE) {
                    return ZERO;
                }

                return new Float(width * theValue.floatValue() / 100);

            default:
                System.err.println(Messages.getString("swing.err.07")); //$NON-NLS-1$
            }
            return ZERO;
        }

        Object getValue(final View view) {
            return theValue;
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#width">'width'</a>.
     */
    static class Width extends FloatValue {
        static final Width auto =
            new Width("auto", 0, RELATIVE_UNITS_UNDEFINED);

        Width() {
            super();
        }

        Width(final String strValue, final float theValue, final int units) {
            super(strValue, theValue, units);
        }

        public PropertyValueConverter toCSS(final Object value) {
            if ("auto".equals(value)) {
                return auto;
            }
            return super.toCSS(value);
        }

        PropertyValueConverter create(final String strValue,
                                      final float theValue, final int rUnits) {
            if (theValue < 0) {
                return null;
            }
            return super.create(strValue, theValue, rUnits);
        }
    }

    static final class Height extends Width {
        PropertyValueConverter create(final String strValue,
                                      final float theValue, final int rUnits) {
            if (rUnits == RELATIVE_UNITS_PERCENTAGE) {
                return null;
            }
            return super.create(strValue, theValue, rUnits);
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#line-height">'line-height'</a>.
     */
    static final class LineHeight extends FloatValue
        implements RelativeValueResolver {

        static final int RELATIVE_UNITS_NUMBER = 20;

        static final Length normal = new LineHeight("normal", 1,
                                                    RELATIVE_UNITS_NUMBER);

        private LineHeight(final String strValue,
                           final float theValue,
                           final int units) {
            super(strValue, theValue, units);
        }

        public PropertyValueConverter toCSS(final Object value) {
            if ("normal".equals(value)) {
                return normal;
            }
            if (value instanceof String) {
                final String sValue = (String)value;

                return NUMBER_PATTERN.matcher(sValue).matches()
                       ? create(sValue, Float.parseFloat(sValue),
                                RELATIVE_UNITS_NUMBER)
                       : super.toCSS(value);
            }
            return super.toCSS(value);
        }

        public Object getComputedValue(final View view) {
            if (relativeUnits == RELATIVE_UNITS_UNDEFINED
                || relativeUnits == RELATIVE_UNITS_NUMBER) {

                return this;
            }
            return new FloatValue(sValue,
                                  (Float)resolveRelativeValue(view),
                                  RELATIVE_UNITS_UNDEFINED);
        }

        PropertyValueConverter create(final String strValue,
                                      final float theValue,
                                      final int rUnits) {
            if (theValue < 0) {
                return null;
            }
            return super.create(strValue, theValue, rUnits);
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#list-style-type">'list-style-type'</a>.
     */
    static final class ListStyleType extends FixedSetValues {
        static final ListStyleType factory = new ListStyleType(0);

        static final int LIST_STYLE_DISC = 0;
        static final int LIST_STYLE_CIRCLE = 1;
        static final int LIST_STYLE_SQUARE = 2;
        static final int LIST_STYLE_DECIMAL = 3;

        static final int LIST_STYLE_LOWER_ROMAN = 4;
        static final int LIST_STYLE_UPPER_ROMAN = 5;
        static final int LIST_STYLE_LOWER_ALPHA = 6;
        static final int LIST_STYLE_UPPER_ALPHA = 7;

        static final int LIST_STYLE_NONE = 8;

        private static final String[] VALID_VALUES = {
            "disc", "circle", "square", "decimal",
            "lower-roman", "upper-roman", "lower-alpha", "upper-alpha",
            "none"
        };
        private static final ListStyleType[] VALUE_HOLDERS =
            new ListStyleType[VALID_VALUES.length];

        static {
            VALUE_HOLDERS[0] = factory;
        }

        private ListStyleType(final int index) {
            super(index);
        }

        String[] getValidValues() {
            return VALID_VALUES;
        }

        PropertyValueConverter[] getValueHolders() {
            return VALUE_HOLDERS;
        }

        PropertyValueConverter createValueHolder(final int valueIndex) {
            return new ListStyleType(valueIndex);
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#list-style-position">'list-style-position'</a>.
     */
    static final class ListStylePosition extends FixedSetValues {
        static final ListStylePosition factory = new ListStylePosition(0);

        private static final String[] VALID_VALUES = {
            "outside", "inside"
        };
        private static final ListStylePosition[] VALUE_HOLDERS =
            new ListStylePosition[VALID_VALUES.length];

        static {
            VALUE_HOLDERS[0] = factory;
        }

        private ListStylePosition(final int index) {
            super(index);
        }

        String[] getValidValues() {
            return VALID_VALUES;
        }

        PropertyValueConverter[] getValueHolders() {
            return VALUE_HOLDERS;
        }

        PropertyValueConverter createValueHolder(final int valueIndex) {
            return new ListStylePosition(valueIndex);
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#list-style">'list-style'</a>.
     */
    static final class ListStyleExpander implements ShorthandPropertyExpander {
        private static final Pattern WS_SPLIT = Pattern.compile("\\s+");
        // pattern is based on CSS1, http://www.w3.org/TR/CSS1#url format
        static final Pattern URL_PATTERN =
            Pattern.compile("url\\(\\s*((?:\"|')?+).+?\\1\\s*\\)");

        public void parseAndExpandProperty(final MutableAttributeSet attrs,
                                           final String value) {
            String[] parts = split(value.trim());
            if (parts == null || parts.length > 3) {
                return;
            }

            ListStyleType type = null;
            ListStylePosition position = null;
            ImageValue image = null;

            for (int i = 0; i < parts.length; i++) {
                if (type == null) {
                    type = (ListStyleType)ListStyleType.factory.toCSS(parts[i]);
                    if (type != null) {
                        continue;
                    }
                } else {
                    if ("none".equals(type.toString()) && image == null) {
                        ListStyleType typeTry =
                            (ListStyleType)ListStyleType.factory.toCSS(parts[i]);
                        if (typeTry != null) {
                            type = typeTry;
                            image = ImageValue.NONE;
                            continue;
                        }
                    }
                }

                if (position == null) {
                    position =
                        (ListStylePosition)ListStylePosition.factory
                                                            .toCSS(parts[i]);
                    if (position != null) {
                        continue;
                    }
                }

                if (image == null) {
                    image = (ImageValue)ImageValue.NONE.toCSS(parts[i]);
                    if (image != null) {
                        continue;
                    }
                }

                // An invalid value encountered
                return;
            }

            attrs.addAttribute(Attribute.LIST_STYLE_TYPE,
                               type != null ? type : ListStyleType.factory);
            attrs.addAttribute(Attribute.LIST_STYLE_POSITION,
                               position != null ? position
                                                : ListStylePosition.factory);
            attrs.addAttribute(Attribute.LIST_STYLE_IMAGE,
                               image != null ? image : ImageValue.NONE);
        }

        private static String[] split(final String value) {
            if (value.indexOf("url(") != -1) {
                Matcher matcher = URL_PATTERN.matcher(value);
                if (!matcher.find()) {
                    return null;
                }
                final int urlStart = matcher.start();
                final int urlEnd   = matcher.end();
                final String url = value.substring(urlStart, urlEnd);
                final String rest = (value.substring(0, urlStart)
                                     + value.substring(urlEnd)).trim();
                if (rest.length() != 0) {
                    if (rest.indexOf("url(") != -1) {
                        return null;
                    }

                    String[] parts = WS_SPLIT.split(rest);
                    String[] result = new String[parts.length + 1];
                    System.arraycopy(parts, 0, result, 0, parts.length);
                    result[parts.length] = url;

                    return result;
                }

                return new String[] {url};
            }

            return WS_SPLIT.split(value);
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#text-transform">'text-transform'</a>.
     */
    static final class TextTransform extends FixedSetValues {
        private static final String[] VALID_VALUES = {
            "none", "capitalize", "uppercase", "lowercase"
        };
        private static final TextTransform[] VALUE_HOLDERS =
            new TextTransform[VALID_VALUES.length];

        TextTransform() {
            this(-1);
        }

        private TextTransform(final int index) {
            super(index);
        }

        String[] getValidValues() {
            return VALID_VALUES;
        }

        PropertyValueConverter[] getValueHolders() {
            return VALUE_HOLDERS;
        }

        PropertyValueConverter createValueHolder(final int valueIndex) {
            return new TextTransform(valueIndex);
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#letter-spacing">'letter-spacing'</a>
     * and <a href="http://www.w3.org/TR/CSS1#word-spacing">'word-spacing'</a>.
     */
    static final class SpacingValue extends FloatValue {
        static final SpacingValue factory =
            new SpacingValue("normal", 0, RELATIVE_UNITS_UNDEFINED);

        private SpacingValue(final String strValue,
                             final float theValue,
                             final int units) {
            super(strValue, theValue, units);
        }

        public PropertyValueConverter toCSS(final Object value) {
            if ("normal".equals(value)) {
                return factory;
            }

            return super.toCSS(value);
        }

        PropertyValueConverter create(final String strValue,
                                      final float theValue, final int rUnits) {
            return rUnits == RELATIVE_UNITS_PERCENTAGE
                   ? null : new FloatValue(strValue, theValue, rUnits);
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#vertical-align">'vertical-align'</a>.
     */
    static final class VerticalAlign extends FixedSetValues {
        private static final String[] VALID_VALUES = {
            "baseline", "sub", "super", "top", "text-top", "middle", "bottom",
            "text-bottom"
        };
        private static final VerticalAlign[] VALUE_HOLDERS =
            new VerticalAlign[VALID_VALUES.length];

        VerticalAlign() {
            this(-1);
        }

        private VerticalAlign(final int index) {
            super(index);
        }

        public PropertyValueConverter toCSS(final Object value) {
            // TODO 'vertical-align' - implement support for percentage values
            return super.toCSS(value);
        }

        String[] getValidValues() {
            return VALID_VALUES;
        }

        PropertyValueConverter[] getValueHolders() {
            return VALUE_HOLDERS;
        }

        PropertyValueConverter createValueHolder(final int valueIndex) {
            return new VerticalAlign(valueIndex);
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#white-space">'white-space'</a>.
     */
    static final class WhiteSpace extends FixedSetValues {
        static final WhiteSpace factory = new WhiteSpace(0);

        static final int NORMAL = 0;
        static final int PRE    = 1;
        static final int NOWRAP = 2;

        private static final String[] VALID_VALUES = {
            "normal", "pre", "nowrap"
        };
        private static final WhiteSpace[] VALUE_HOLDERS =
            new WhiteSpace[VALID_VALUES.length];

        static {
            VALUE_HOLDERS[0] = factory;
        }

        private WhiteSpace(final int index) {
            super(index);
        }

        String[] getValidValues() {
            return VALID_VALUES;
        }

        PropertyValueConverter[] getValueHolders() {
            return VALUE_HOLDERS;
        }

        PropertyValueConverter createValueHolder(final int valueIndex) {
            return new WhiteSpace(valueIndex);
        }
    }

    /**
     * The implementation is based on CSS1 spec for
     * <a href="http://www.w3.org/TR/CSS1#text-decoration">'text-decoration'</a>.
     */
    static final class TextDecoration implements Cloneable,
                                                 PropertyValueConverter {
        private static final String[] DECORATIONS = {
            "underline", "line-through", "overline", "blink"
        };

        private final boolean[] values = new boolean[4];

        private String value;

        TextDecoration() {
        }

        private TextDecoration(final String value, final boolean[] values) {
            this.value = value;
            for (int i = 0; i < this.values.length; i++) {
                this.values[i] = values[i];
            }
        }

        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }

        public PropertyValueConverter toCSS(final Object value) {
            return parseValue((String)value, values)
                   ? new TextDecoration((String)value, values)
                   : null;
        }

        public Object fromCSS() {
            return null;
        }

        public String toString() {
            return value;
        }

        void setUnderline(final boolean underline) {
            values[0] = underline;
            updateValue();
        }

        boolean isUnderline() {
            return values[0];
        }

        void setLineThrough(final boolean lineThrough) {
            values[1] = lineThrough;
            updateValue();
        }

        boolean isLineThrough() {
            return values[1];
        }

        void setOverline(final boolean overline) {
            values[2] = overline;
            updateValue();
        }

        boolean isOverline() {
            return values[2];
        }

        void setBlink(final boolean blink) {
            values[3] = blink;
            updateValue();
        }

        boolean isBlink() {
            return values[3];
        }

        boolean isNone() {
            return !isUnderline() && !isLineThrough()
                   && !isOverline() && !isBlink();
        }

        private String getValue() {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                if (values[i]) {
                    if (result.length() > 0) {
                        result.append(' ');
                    }
                    result.append(DECORATIONS[i]);
                }
            }
            return result.length() > 0 ? result.toString() : "none";
        }

        private static boolean parseValue(final String value,
                                          final boolean[] values) {
            for (int i = 0; i < values.length; i++) {
                values[i] = false;
            }
            if ("none".equals(value)) {
                return true;
            }

            final String[] decorators = value.split("\\s+");
            if (decorators.length == 0) {
                return false;
            }

            for (int i = 0; i < decorators.length; i++) {
                int index = getIndex(decorators[i]);
                if (index == -1) {
                    return false;
                }
                values[index] = true;
            }
            return true;
        }

        private void updateValue() {
            value = getValue();
        }

        private static int getIndex(final String decorator) {
            for (int i = 0; i < DECORATIONS.length; i++) {
                if (DECORATIONS[i].equals(decorator)) {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * The storage for
     * <a href="http://www.w3.org/TR/CSS1#length-units">'Length Units'</a>.
     */
    abstract static class Length implements PropertyValueConverter {
        static final int RELATIVE_UNITS_UNDEFINED = -1;
        static final int RELATIVE_UNITS_PERCENTAGE = 0;
        static final int RELATIVE_UNITS_EM = 1;
        static final int RELATIVE_UNITS_EX = 2;

        // Units convertion coefficients
        static final float PX_TO_PT = 1.3f;         // 96 / 72 = 4 / 3 = 1.3(3),
                                                    // which is approx. 1.3
                                                    // 96 - def screen resolution
        static final float MM_TO_PT = 72f / 25.4f;  // 1 in = 25.4 mm
        static final float CM_TO_PT = 72f / 2.54f;  // 1 in = 2.54 cm
        static final float PC_TO_PT = 12f;          // 1 pc = 12 pt
        static final float IN_TO_PT = 72f;          // 1 pt = 1/72 in

        // This is based on CSS1 spec, http://www.w3.org/TR/CSS1#length-units.
        static final Pattern NUMBER_PATTERN =
            Pattern.compile("(?:\\+|-)?(?:\\d+|\\d*\\.\\d+)");
        private static final Pattern LENGTH_PATTERN =
            Pattern.compile("(?:\\+|-)?(?:\\d+|\\d*\\.\\d+)(?:pt|px|mm|cm|pc|in)");

        final String sValue;
        final int relativeUnits;

        Length() {
            sValue = null;
            relativeUnits = RELATIVE_UNITS_UNDEFINED;
        }

        Length(final String sValue, final int rUnits) {
            this.sValue = sValue;
            this.relativeUnits = rUnits;
        }

        abstract PropertyValueConverter create(final String strValue,
                                               final float theValue,
                                               final int rUnits);

        abstract Object resolveRelativeValue(final View view);
        abstract Object getValue(final View view);

        public PropertyValueConverter toCSS(final Object value) {
            if (value instanceof String) {
                String strValue = (String)value;
                int rUnits = getRelativeUnits(strValue);
                float theValue = rUnits != RELATIVE_UNITS_UNDEFINED
                                 ? parseRelativeValue(strValue, rUnits)
                                 : parseAbsoluteValue(strValue);
                if (!Float.isNaN(theValue)) {
                    return create(strValue, theValue, rUnits);
                }
            }
            return null;
        }

        public final Object fromCSS() {
            return getResolvedValue(null);
        }

        public final String toString() {
            return sValue;
        }

        protected static float parseAbsoluteValue(final String value) {
            if ("0".equals(value)) {
                return 0;
            }
            if (LENGTH_PATTERN.matcher(value).matches()) {
                final String units = value.substring(value.length() - 2,
                                                     value.length());
                final float theValue =
                    Float.parseFloat(value.substring(0, value.length() - 2));

                if ("pt".equals(units)) {
                    return theValue;
                } else if ("px".equals(units)) {
                    return theValue * PX_TO_PT;
                } else if ("mm".equals(units)) {
                    return theValue * MM_TO_PT;
                } else if ("cm".equals(units)) {
                    return theValue * CM_TO_PT;
                } else if ("pc".equals(units)) {
                    return theValue * PC_TO_PT;
                } else if ("in".equals(units)) {
                    return theValue * IN_TO_PT;
                }
            }

            return Float.NaN;
        }

        protected static float parseRelativeValue(final String value,
                                                  final int relativeUnits) {
            String number;
            switch (relativeUnits) {
            case RELATIVE_UNITS_PERCENTAGE:
                number = value.substring(0, value.length() - 1);
                break;

            case RELATIVE_UNITS_EM:
            case RELATIVE_UNITS_EX:
                number = value.substring(0, value.length() - 2);
                break;

            default:
                return Float.NaN;
            }

            if (NUMBER_PATTERN.matcher(number).matches()) {
                return Float.parseFloat(number);
            }
            return Float.NaN;
        }

        protected static int getRelativeUnits(final String value) {
            if (Utilities.isEmptyString(value)) {
                return RELATIVE_UNITS_UNDEFINED;
            }

            if (value.endsWith("%")) {
                return RELATIVE_UNITS_PERCENTAGE;
            }
            if (value.endsWith("em")) {
                return RELATIVE_UNITS_EM;
            }
            if (value.endsWith("ex")) {
                return RELATIVE_UNITS_EX;
            }

            return RELATIVE_UNITS_UNDEFINED;
        }

        protected static boolean isRelative(final String value) {
            return getRelativeUnits(value) != RELATIVE_UNITS_UNDEFINED;
        }

        final boolean isRelative() {
            return relativeUnits != RELATIVE_UNITS_UNDEFINED;
        }

        final Object resolveRelativeValue() {
            return resolveRelativeValue(null);
        }

        final Object getValue() {
            return getValue(null);
        }

        final Object getResolvedValue(final View view) {
            if (isRelative()) {
                return resolveRelativeValue(view);
            }
            return getValue(view);
        }

        final float floatValue() {
            return floatValue((View)null);
        }

        final float floatValue(final View view) {
            return ((Number)getResolvedValue(view)).floatValue();
        }

        final float floatValue(final AttributeSet attrs) {
            return attrs instanceof ViewAttributeSet
                   ? floatValue(((ViewAttributeSet)attrs).view)
                   : floatValue();
        }

        final int intValue() {
            return intValue((View)null);
        }

        final int intValue(final View view) {
            return ((Number)getResolvedValue(view)).intValue();
        }

        final int intValue(final AttributeSet attrs) {
            return attrs instanceof ViewAttributeSet
                   ? intValue(((ViewAttributeSet)attrs).view)
                   : intValue();
        }
    }

    /**
     * The storage for all properties with fixed set of values.
     */
    abstract static class FixedSetValues implements PropertyValueConverter {
        abstract String[] getValidValues();
        abstract PropertyValueConverter[] getValueHolders();

        private final int index;

        FixedSetValues(final int index) {
            this.index = index;
        }

        public PropertyValueConverter toCSS(final Object value) {
            if (value instanceof String) {
                final int valueIndex = getValueIndex((String)value);
                return valueIndex >= 0 ? getValueHolder(valueIndex) : null;
            }
            return null;
        }

        public Object fromCSS() {
            return null;
        }

        final int getIndex() {
            return index;
        }

        final int getValueIndex(final String value) {
            final String[] validValues = getValidValues();
            for (int i = 0; i < validValues.length; i++) {
                if (validValues[i].equals(value)) {
                    return i;
                }
            }
            return -1;
        }

        final PropertyValueConverter getValueHolder(final int valueIndex) {
            final PropertyValueConverter[] valueHolders = getValueHolders();
            if (valueHolders[valueIndex] == null) {
                valueHolders[valueIndex] = createValueHolder(valueIndex);
            }
            return valueHolders[valueIndex];
        }

        PropertyValueConverter createValueHolder(final int valueIndex) {
            throw new FixedSetValuesError(valueIndex, getClass().getName());
        }

        public final String toString() {
            return getValidValues()[index];
        }
    }

    static final class FixedSetValuesError extends Error {
        FixedSetValuesError(final int index, final String className) {
            super(Messages.getString("swing.err.07", index, className)); //$NON-NLS-1$
        }
    }

    /**
     * Expands values of shorthand <a href="http://www.w3.org/TR/CSS1#margin">'margin'</a>
     * and <a href="http://www.w3.org/TR/CSS1#padding">'padding'</a> properties into
     * individual properties for each side.
     */
    static final class SpaceExpander implements ShorthandPropertyExpander {
        final Attribute[] keys;
        final PropertyValueConverter factory;

        private Pattern splitPattern;

        SpaceExpander(final Attribute topKey, final Attribute rightKey,
                      final Attribute bottomKey, final Attribute leftKey) {
            this(topKey, rightKey, bottomKey, leftKey, FloatValue.factory);
        }

        SpaceExpander(final Attribute topKey, final Attribute rightKey,
                      final Attribute bottomKey, final Attribute leftKey,
                      final PropertyValueConverter factory) {
            keys = new Attribute[] {
                topKey, rightKey, bottomKey, leftKey
            };
            this.factory = factory;
        }

        public void parseAndExpandProperty(final MutableAttributeSet attrs,
                                           final String value) {
            final String[] values = getSplitPattern().split(value);
            final Object[] parsed = new Object[4];
            for (int i = 0; i < parsed.length; i++) {
                if (i < values.length) {
                    parsed[i] = factory.toCSS(values[i]);
                } else {
                    parsed[i] = parsed[i >= 3 ? 1 : 0];
                    // parsed[1] = parsed[0], i.e. right = top if only one value
                    //                                          is specified
                    // parsed[2] = parsed[0], i.e. bottom = top
                    // parsed[3] = parsed[1], i.e. left = right
                }
                if (parsed[i] == null) {
                    // Even if one portion is not recognized as a valid value,
                    // the whole declaration must be ignored.
                    return;
                }
            }

            for (int i = 0; i < parsed.length; i++) {
                attrs.addAttribute(keys[i], parsed[i]);
            }
        }

        private Pattern getSplitPattern() {
            if (splitPattern == null) {
                splitPattern = Pattern.compile("\\s+");
            }
            return splitPattern;
        }
    }


    interface ViewUpdater {
        void updateView();
    }


    private static final Attribute[] allAttributeKeys = {
        Attribute.BACKGROUND,
        Attribute.BACKGROUND_ATTACHMENT,
        Attribute.BACKGROUND_COLOR,
        Attribute.BACKGROUND_IMAGE,
        Attribute.BACKGROUND_POSITION,
        Attribute.BACKGROUND_REPEAT,
        Attribute.BORDER,
        Attribute.BORDER_BOTTOM,
        Attribute.BORDER_BOTTOM_WIDTH,
        Attribute.BORDER_COLOR,
        Attribute.BORDER_LEFT,
        Attribute.BORDER_LEFT_WIDTH,
        Attribute.BORDER_RIGHT,
        Attribute.BORDER_RIGHT_WIDTH,
        Attribute.BORDER_STYLE,
        Attribute.BORDER_TOP,
        Attribute.BORDER_TOP_WIDTH,
        Attribute.BORDER_WIDTH,
        Attribute.CLEAR,
        Attribute.COLOR,
        Attribute.DISPLAY,
        Attribute.FLOAT,
        Attribute.FONT,
        Attribute.FONT_FAMILY,
        Attribute.FONT_SIZE,
        Attribute.FONT_STYLE,
        Attribute.FONT_VARIANT,
        Attribute.FONT_WEIGHT,
        Attribute.HEIGHT,
        Attribute.LETTER_SPACING,
        Attribute.LINE_HEIGHT,
        Attribute.LIST_STYLE,
        Attribute.LIST_STYLE_IMAGE,
        Attribute.LIST_STYLE_POSITION,
        Attribute.LIST_STYLE_TYPE,
        Attribute.MARGIN,
        Attribute.MARGIN_BOTTOM,
        Attribute.MARGIN_LEFT,
        Attribute.MARGIN_RIGHT,
        Attribute.MARGIN_TOP,
        Attribute.PADDING,
        Attribute.PADDING_BOTTOM,
        Attribute.PADDING_LEFT,
        Attribute.PADDING_RIGHT,
        Attribute.PADDING_TOP,
        Attribute.TEXT_ALIGN,
        Attribute.TEXT_DECORATION,
        Attribute.TEXT_INDENT,
        Attribute.TEXT_TRANSFORM,
        Attribute.VERTICAL_ALIGN,
        Attribute.WHITE_SPACE,
        Attribute.WIDTH,
        Attribute.WORD_SPACING
    };

    static final int TOP_SIDE    = 0;
    static final int RIGHT_SIDE  = 1;
    static final int BOTTOM_SIDE = 2;
    static final int LEFT_SIDE   = 3;

    private static final HashMap nameToAttribute = new HashMap();
    private static final HashMap styleConstantsToCSS = new HashMap();

    static {
        for (int i = 0; i < allAttributeKeys.length; i++) {
            nameToAttribute.put(allAttributeKeys[i].toString(),
                                allAttributeKeys[i]);
            StyleContext.registerStaticAttributeKey(allAttributeKeys[i]);
        }

        styleConstantsToCSS.put(StyleConstants.Background,
                                Attribute.BACKGROUND_COLOR);

        styleConstantsToCSS.put(StyleConstants.Foreground, Attribute.COLOR);

        styleConstantsToCSS.put(StyleConstants.FontFamily,
                                Attribute.FONT_FAMILY);
        styleConstantsToCSS.put(StyleConstants.FontSize,
                                Attribute.FONT_SIZE);
        styleConstantsToCSS.put(StyleConstants.Italic,
                                Attribute.FONT_STYLE);
        styleConstantsToCSS.put(StyleConstants.Bold,
                                Attribute.FONT_WEIGHT);

        styleConstantsToCSS.put(StyleConstants.SpaceAbove,
                                Attribute.MARGIN_TOP);
        styleConstantsToCSS.put(StyleConstants.RightIndent,
                                Attribute.MARGIN_RIGHT);
        styleConstantsToCSS.put(StyleConstants.SpaceBelow,
                                Attribute.MARGIN_BOTTOM);
        styleConstantsToCSS.put(StyleConstants.LeftIndent,
                                Attribute.MARGIN_LEFT);

        styleConstantsToCSS.put(StyleConstants.Alignment, Attribute.TEXT_ALIGN);
        styleConstantsToCSS.put(StyleConstants.FirstLineIndent,
                                Attribute.TEXT_INDENT);
    }

    public CSS() {
    }

    public static Attribute[] getAllAttributeKeys() {
        return allAttributeKeys;
    }

    public static final Attribute getAttribute(final String name) {
        return (Attribute)nameToAttribute.get(name);
    }

    /**
     * Maps a <code>StyleConstants</code> attribute key
     * to {@link CSS.Attribute}.
     *
     * @param attrKey the key to convert.
     * @return the corresponding <code>CSS.Attribute</code>,
     *         or <code>null</code> if no equivalent found.
     */
    static Object mapToCSS(final Object attrKey) {
        return styleConstantsToCSS.get(attrKey);
    }

    /**
     * Maps a <code>StyleConstants</code> attribute key
     * to {@link CSS.Attribute}.
     *
     * @param attrKey the key to convert.
     * @return <code>attrKey</code> if it is of type <code>CSS.Attribute</code>,
     *         the corresponding <code>CSS.Attribute</code>,
     *         or <code>null</code> if no equivalent found.
     */
    static Object mapToCSSForced(final Object attrKey) {
        return attrKey instanceof Attribute ? attrKey : mapToCSS(attrKey);
    }
}
