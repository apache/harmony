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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.beans.XMLDecoder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.apache.harmony.x.swing.internal.nls.Messages;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XMLSynthParser creates a set of SynthStyles that satisfied XML description of
 * the look and feel. SynthLookAndFeel can access to the styles through
 * SynthFactory
 */
class XMLSynthParser extends DefaultHandler {

    /* All the known elements */
    private static final String STYLE_ELEMENT = "style"; //$NON-NLS-1$

    private static final String STATE_ELEMENT = "state"; //$NON-NLS-1$

    private static final String FONT_ELEMENT = "font"; //$NON-NLS-1$

    private static final String COLOR_ELEMENT = "color"; //$NON-NLS-1$

    private static final String G_UTILS_ELEMENT = "graphicsUtils"; //$NON-NLS-1$

    private static final String DEFAULTS_ELEMENT = "defaultsProperty"; //$NON-NLS-1$

    private static final String INSETS_ELEMENT = "insets"; //$NON-NLS-1$

    private static final String OPAQUE_ELEMENT = "opaque"; //$NON-NLS-1$

    private static final String PROPERTY_ELEMENT = "property"; //$NON-NLS-1$

    private static final String BIND_ELEMENT = "bind"; //$NON-NLS-1$

    private static final String IM_PAINTER_ELEMENT = "imagePainter"; //$NON-NLS-1$

    private static final String PAINTER_ELEMENT = "painter"; //$NON-NLS-1$

    private static final String IMAGE_ICON_ELEMENT = "imageIcon"; //$NON-NLS-1$

    private static final String SYNTH_ELEMENT = "synth"; //$NON-NLS-1$

    /**
     * BeansAdapter to process unknown elements
     */
    private final BeansAdapter adapter = new BeansAdapter();

    /**
     * The base used for correct URL creating
     */
    private final Class<?> base;

    /**
     * The marking for default state. Used in parser to mark state as default
     * and indirectly in XMLSynthStyle in findColorForState, findFontForState
     * methods.
     */
    private final int DEFAULT_STATE = 0;

    /**
     * The XMLStyleFactory to add configured styles
     */
    private final DefaultStyleFactory styleFactory = new DefaultStyleFactory();

    /**
     * Map used for correct references processing. Key is Element id in xml
     * style. Value is the element to be inserted in the style
     */
    private final Map<String, Object> namedElements = new HashMap<String, Object>();

    /**
     * Map used for correct references processing. Key is the complex key (see
     * private class below). Value is the SynthStyle to be value from
     * namedElements be inserted
     */
    private final Map<XMLSynthKey, XMLSynthStyle> namedReferences = new HashMap<XMLSynthKey, XMLSynthStyle>();

    /**
     * The style under modification
     */
    private XMLSynthStyle currentStyle;

    /**
     * State marking
     */
    private int currentState;

    /**
     * The colors, that can be obtained from the string description
     */
    enum Colors {
        RED(Color.RED), GREEN(Color.GREEN), BLUE(Color.BLUE), CYAN(Color.CYAN), BLACK(
                Color.BLACK), WHITE(Color.WHITE), GRAY(Color.GRAY), DARK_GRAY(
                Color.DARK_GRAY), ORANGE(Color.ORANGE), YELLOW(Color.YELLOW);

        final Color color;

        Colors(Color color) {

            this.color = color;
        }
    }

    /**
     * The directions used in Painters, that can be obtained from the string
     * description
     */
    enum Directions {
        EAST(SwingConstants.EAST), NORTH(SwingConstants.NORTH), SOUTH(
                SwingConstants.SOUTH), WEST(SwingConstants.WEST), TOP(
                SwingConstants.TOP), BOTTOM(SwingConstants.BOTTOM), LEFT(
                SwingConstants.LEFT), RIGHT(SwingConstants.RIGHT), HORIZONTAL(
                SwingConstants.HORIZONTAL), VERTICAL(SwingConstants.VERTICAL), HORIZONTAL_SPLIT(
                JSplitPane.HORIZONTAL_SPLIT), VERTICAL_SPLIT(
                JSplitPane.VERTICAL_SPLIT);

        final int direction;

        Directions(int direction) {

            this.direction = direction;
        }
    }

    XMLSynthParser(Class<?> resourseBase) {
        this.base = resourseBase;
    }

    /**
     * Process all the known tags
     */
    @Override
    public void startElement(String namespaceURI, String localName,
            String qName, Attributes attrs) throws SAXException {

        String element = ("".equals(localName)) ? qName.intern() : //$NON-NLS-1$ 
                localName.intern();

        if (element == STYLE_ELEMENT) {

            processStyleElement(attrs);

        } else if (element == STATE_ELEMENT) {

            currentState = computeStateValue(attrs.getValue("value")); //$NON-NLS-1$

        } else if (element == FONT_ELEMENT) {

            processFontElement(attrs);

        } else if (element == COLOR_ELEMENT) {

            processColorElement(attrs);

        } else if (element == G_UTILS_ELEMENT) {

            processGraphicsUtilsElement(attrs);

        } else if (element == DEFAULTS_ELEMENT) {

            processDefaultsProperyElement(attrs);

        } else if (element == INSETS_ELEMENT) {

            processInsetsElement(attrs);

        } else if (element == OPAQUE_ELEMENT) {

            processOpaqueElement(attrs);

        } else if (element == PROPERTY_ELEMENT) {

            processProperyElement(attrs);

        } else if (element == BIND_ELEMENT) {

            processBindElement(attrs);

        } else if (element == IM_PAINTER_ELEMENT) {

            processImagePainterElement(attrs);

        } else if (element == PAINTER_ELEMENT) {

            processPainterElement(attrs);

        } else if (element == IMAGE_ICON_ELEMENT) {

            processIconElement(attrs);

        } else if (element == SYNTH_ELEMENT) {

            // Do nothing

        } else {
            adapter.pushStartTag(element, attrs);
        }
    }

    /**
     * Characters need to beansPersistance entities only
     */
    @Override
    public void characters(char[] ch, int start, int size) throws SAXException {
        adapter.pushCharacters(ch, start, size);
    }

    @Override
    @SuppressWarnings("unused")
    public void endElement(String namespaceURI, String sName, String qName) {

        String element = ("".equals(sName)) ? qName.intern() : sName //$NON-NLS-1$
                .intern();

        if ((STYLE_ELEMENT == element)) {
            currentStyle = null;
        } else if (STATE_ELEMENT == element) {
            currentState = DEFAULT_STATE;
        } else if ((!(BIND_ELEMENT == element))
                && (!(COLOR_ELEMENT == element))
                && (!(G_UTILS_ELEMENT == element))
                && (!(DEFAULTS_ELEMENT == element))
                && (!(INSETS_ELEMENT == element))
                && (!(OPAQUE_ELEMENT == element))
                && (!(PROPERTY_ELEMENT == element))
                && (!(IM_PAINTER_ELEMENT == element))
                && (!(PAINTER_ELEMENT == element))
                && (!(IMAGE_ICON_ELEMENT == element))
                && (!(SYNTH_ELEMENT == element))
                && (!(FONT_ELEMENT == element))) {
            adapter.pushEndTag(element);
        }

    }

    /**
     * After the xml document parsing process all the references in
     * nameReferences map
     * 
     * All the RuntimeExceptions are internal synth exceptions
     */
    @Override
    public void endDocument() throws SAXException {

        adapter.putAdaptedBeansToMap(namedElements);

        fillReferences();

        SynthLookAndFeel.setStyleFactory(styleFactory);
    }

    /**
     * Called in endDocument
     * 
     * Process all the found references (idref in xml document and
     * namedReferences Map in this class) and binds it with exiting objects (id
     * in xml document and namedElements Map in this class)
     */
    private void fillReferences() {

        for (Map.Entry<XMLSynthKey, XMLSynthStyle> entry : namedReferences
                .entrySet()) {

            XMLSynthKey key = entry.getKey();
            String element = key.getElementName().intern();

            if (element == INSETS_ELEMENT) {
                // Just put the insets with given idref to style it (idref)
                // contains
                entry.getValue().setInsets(
                        (Insets) namedElements.get(key.getReference()));

            } else if (element == FONT_ELEMENT) {
                // put the font with given idref to style it (idref)
                // contains, according to staieId obtained from key (key
                // filling can be found in )
                entry.getValue().addFont(
                        (Font) namedElements.get(key.getReference()),
                        key.getState());

            } else if (element == G_UTILS_ELEMENT) {

                entry.getValue().setGraphicsUtils(
                        (SynthGraphicsUtils) namedElements.get(key
                                .getReference()));

            } else if (element == COLOR_ELEMENT) {

                entry.getValue().addColor(
                        (Color) namedElements.get(key.getReference()),
                        key.getState(),
                        ColorType.calculateColorType(key.getFirstInfo()));

            } else if (element == PROPERTY_ELEMENT) {
                if (key.getAllInfo().length == 2) {
                    entry.getValue().addIcon(
                            (Icon) namedElements.get(key.getReference()),
                            key.getState(), key.getFirstInfo());
                } else {
                    entry.getValue().setProperty(key.getFirstInfo(),
                            namedElements.get(key.getReference()));
                }

            } else if (element == DEFAULTS_ELEMENT) {

                UIManager.put(key.getFirstInfo(), namedElements.get(key
                        .getReference()));

            } else if (element == PAINTER_ELEMENT) {

                String directionAttr = key.getAllInfo()[1];
                int direction = (directionAttr != null) ? Integer
                        .parseInt(directionAttr) : PaintersManager.NO_DIRECTION;

                entry.getValue().addPainter(
                        (SynthPainter) namedElements.get(key.getReference()),
                        key.getState(), key.getAllInfo()[0], direction);

            } else {
                // impossible case because namedReferences filled by known
                // Elements only
                assert false : "Error in parser code"; //$NON-NLS-1$
            }
        }
    }

    private void processStyleElement(Attributes attrs) throws SAXException {

        String cloneAttr = attrs.getValue("clone"); //$NON-NLS-1$
        String styleName = attrs.getValue("id"); //$NON-NLS-1$

        if (cloneAttr == null) {

            currentStyle = new XMLSynthStyle();
            namedElements.put(styleName, currentStyle);

        } else {
            try {
                currentStyle = (XMLSynthStyle) ((XMLSynthStyle) namedElements
                        .get(cloneAttr)).clone();

            } catch (CloneNotSupportedException e) {
                throw new SAXException(e);
            }

            namedElements.put(styleName, currentStyle);
        }

        currentState = DEFAULT_STATE;
    }

    @SuppressWarnings("nls")
    private void processFontElement(Attributes attrs) throws SAXException {

        String idref = attrs.getValue("idref");
        String id = attrs.getValue("id");
        String name = attrs.getValue("name");
        String styleAttr = attrs.getValue("style");

        int size = 0;
        int style = Font.PLAIN;

        if (idref != null) {
            namedReferences.put(new XMLSynthKey(FONT_ELEMENT, currentState,
                    idref, new String[0]), currentStyle);
            return;
        }

        try {
            size = Integer.parseInt(attrs.getValue("size"));
        } finally {
            if (size == 0) {
                throw new SAXException("Invalid font size:"
                        + attrs.getValue("size"));
            }
        }

        if (styleAttr != null) {
            StringTokenizer tk = new StringTokenizer(styleAttr);
            while (tk.hasMoreTokens()) {
                String st = tk.nextToken();
                if (st.equals("BOLD")) {
                    style |= Font.BOLD;
                } else if (st.equals("ITALIC")) {
                    style |= Font.ITALIC;
                }
            }
        }

        if (name == null) {
            throw new SAXException(Messages.getString("swing.err.1E"));
        }

        Font currentFont = new Font(name, style, size);

        if (currentStyle != null) {
            currentStyle.addFont(currentFont, currentState);
        }

        if (id != null) {
            namedElements.put(id, currentFont);
        }
    }

    private void processColorElement(Attributes attrs) throws SAXException {

        String colorTypeAttr = attrs.getValue("type"); //$NON-NLS-1$
        String idRefAttr = attrs.getValue("idref"); //$NON-NLS-1$
        String colorValueAttr = attrs.getValue("value"); //$NON-NLS-1$
        String idAttr = attrs.getValue("id"); //$NON-NLS-1$

        if ((idRefAttr != null) && (colorTypeAttr != null)) {
            namedReferences.put(new XMLSynthKey(COLOR_ELEMENT, currentState,
                    idRefAttr, colorTypeAttr), currentStyle);
            return;
        }

        Color currentColor;

        try {
            if (colorValueAttr.length() == 7) {

                currentColor = Color.decode(colorValueAttr);

            } else if (colorValueAttr.length() == 9) {

                currentColor = new Color(Integer.parseInt(colorValueAttr, 16),
                        true);

            } else {

                currentColor = Colors.valueOf(colorValueAttr.toUpperCase()).color;

            }
        } catch (Exception e) {
            throw new SAXException("Color value is incorrect", e); //$NON-NLS-1$
        }

        if (idAttr != null) {

            namedElements.put(idAttr, currentColor);

        } else {

            ColorType currentColorType = ColorType
                    .calculateColorType(colorTypeAttr);

            if (currentColorType == null) {
                try {
                    Class.forName(colorTypeAttr).newInstance();
                } catch (InstantiationException e) {
                    throw new SAXException(Messages.getString("swing.err.1C") //$NON-NLS-1$
                            + colorTypeAttr);
                } catch (IllegalAccessException e) {
                    throw new SAXException(Messages.getString("swing.err.1C") //$NON-NLS-1$
                            + colorTypeAttr);
                } catch (ClassNotFoundException e) {
                    throw new SAXException(Messages.getString("swing.err.1C") //$NON-NLS-1$
                            + colorTypeAttr);
                }
            }

            currentStyle.addColor(currentColor, currentState, currentColorType);
        }
    }

    @SuppressWarnings( { "nls", "boxing" })
    private void processProperyElement(Attributes attrs) throws SAXException {

        String propertyType = attrs.getValue("type");
        String keyAttr = attrs.getValue("key");

        if ("idref".equals(propertyType) || propertyType == null) {

            if ((keyAttr.endsWith("icon")) || (keyAttr.endsWith("Icon"))) {
                // Markup that it is icon property is XMLSynthKey.info array
                // length
                namedReferences.put(new XMLSynthKey(PROPERTY_ELEMENT,
                        currentState, attrs.getValue("value"), new String[] {
                                keyAttr, "" }), currentStyle);
            } else {
                namedReferences.put(new XMLSynthKey(PROPERTY_ELEMENT,
                        currentState, attrs.getValue("value"), keyAttr),
                        currentStyle);
            }

        } else if ("boolean".equalsIgnoreCase(propertyType)) {

            currentStyle.setProperty(keyAttr, new Boolean(attrs
                    .getValue("value")));

        } else if ("dimension".equalsIgnoreCase(propertyType)) {

            String size = attrs.getValue("value");
            int width = Integer.parseInt(size.substring(0, size.indexOf(" ")));
            int height = Integer
                    .parseInt(size.substring(size.indexOf(" ") + 1));

            currentStyle.setProperty(keyAttr, new Dimension(width, height));

        } else if ("insets".equalsIgnoreCase(propertyType)) {

            currentStyle.setProperty(keyAttr, getInsetsFromString(attrs
                    .getValue("value")));

        } else if ("integer".equalsIgnoreCase(propertyType)) {

            currentStyle.setProperty(keyAttr, Integer.parseInt(attrs
                    .getValue("value")));

        } else {
            throw new SAXException(Messages.getString("swing.err.1F",
                    propertyType));
        }
    }

    @SuppressWarnings( { "nls", "boxing" })
    private void processDefaultsProperyElement(Attributes attrs)
            throws SAXException {

        String propertyType = attrs.getValue("type");

        if ("idref".equals(propertyType) || propertyType == null) {
            namedReferences.put(new XMLSynthKey(DEFAULTS_ELEMENT, currentState,
                    attrs.getValue("value"), attrs.getValue("key")),
                    currentStyle);

        } else if ("boolean".equalsIgnoreCase(propertyType)) {

            UIManager.put(attrs.getValue("key"), new Boolean(attrs
                    .getValue("value")));

        } else if ("dimension".equalsIgnoreCase(propertyType)) {

            String size = attrs.getValue("value");
            int width = Integer.parseInt(size.substring(0, size.indexOf(" ")));
            int height = Integer
                    .parseInt(size.substring(size.indexOf(" ") + 1));

            UIManager.put(attrs.getValue("key"), new Dimension(width, height));

        } else if ("insets".equalsIgnoreCase(propertyType)) {

            UIManager.put(attrs.getValue("key"), getInsetsFromString(attrs
                    .getValue("value")));

        } else if ("integer".equalsIgnoreCase(propertyType)) {

            UIManager.put(attrs.getValue("key"), Integer.parseInt(attrs
                    .getValue("value")));

        } else {
            throw new SAXException(Messages.getString("swing.err.1F",
                    propertyType));
        }
    }

    @SuppressWarnings("nls")
    private void processBindElement(Attributes attrs) throws SAXException {
        /*
         * This method works with previously defined in parsed xml SynthStyles.
         * It's compatible with RI.
         */
        String bindType = attrs.getValue("type");

        if ("region".equalsIgnoreCase(bindType)) {
            styleFactory.putStyle(bindType, attrs.getValue("key"),
                    (XMLSynthStyle) namedElements.get(attrs.getValue("style")));
        } else if ("name".equalsIgnoreCase(bindType)) {
            styleFactory.putStyle(bindType, attrs.getValue("key"),
                    (XMLSynthStyle) namedElements.get(attrs.getValue("style")));
        } else {
            throw new SAXException(Messages.getString("swing.err.22", bindType,
                    attrs.getValue("style")));
        }
    }

    private void processImagePainterElement(Attributes attrs)
            throws SAXException {

        String sourceInsetsAttr = attrs.getValue("sourceInsets"); //$NON-NLS-1$
        String pathAttr = attrs.getValue("path"); //$NON-NLS-1$
        String destinationInsetsAttr = attrs.getValue("destinationInsets"); //$NON-NLS-1$
        String methodAttr = attrs.getValue("method").toLowerCase(); //$NON-NLS-1$
        String directionAttr = attrs.getValue("direction"); //$NON-NLS-1$
        String idAttr = attrs.getValue("id"); //$NON-NLS-1$

        boolean paintCenter = "false".equalsIgnoreCase(attrs //$NON-NLS-1$
                .getValue("paintCenter")) ? false : true; //$NON-NLS-1$
        boolean stretch = "false".equalsIgnoreCase(attrs.getValue("stretch")) ? false : true; //$NON-NLS-1$ //$NON-NLS-2$

        Insets sourseInsets;

        // Compute SourceInsets
        if (sourceInsetsAttr != null && pathAttr != null) {
            sourseInsets = getInsetsFromString(sourceInsetsAttr);
        } else {
            throw new SAXException(Messages.getString("swing.err.23")); //$NON-NLS-1$
        }

        // Compute Direction.
        int direction = PaintersManager.NO_DIRECTION;
        if (directionAttr != null) {
            try {
                direction = Directions.valueOf(directionAttr.toUpperCase()).direction;
            } catch (IllegalArgumentException e) {
                throw new SAXException(Messages.getString("swing.err.20") //$NON-NLS-1$
                        + directionAttr, e);
            }
        }

        Insets destinationInsets = destinationInsetsAttr == null ? sourseInsets
                : getInsetsFromString(destinationInsetsAttr);

        ImagePainter currentPainer;
        try {
            currentPainer = new ImagePainter(pathAttr, sourseInsets,
                    destinationInsets, paintCenter, stretch, base);

            if (idAttr != null) {
                namedElements.put(idAttr, currentPainer);
            }
            if (currentStyle != null) {
                if (methodAttr == null) {
                    currentStyle.addPainter(currentPainer, currentState,
                            "default", direction); //$NON-NLS-1$
                } else {

                    if ((methodAttr.contains("border"))) { //$NON-NLS-1$
                        currentStyle.setProperty(
                                getBorgerPaintedPropertyKey(methodAttr),
                                Boolean.TRUE);
                    }
                    currentStyle.addPainter(currentPainer, currentState,
                            methodAttr, direction);
                }
            }

        } catch (IOException e) {
            // Exception trows in ImageIo.read method and all the comments are
            // in e stack
            throw new SAXException(e);
        }
    }

    @SuppressWarnings("nls")
    private void processPainterElement(Attributes attrs) {
        String method = attrs.getValue("method").toLowerCase();

        if ((method != null) && (method.contains("border"))) {
            currentStyle.setProperty(getBorgerPaintedPropertyKey(method),
                    Boolean.TRUE);

        }

        namedReferences.put(new XMLSynthKey(PAINTER_ELEMENT, currentState,
                attrs.getValue("idref"), new String[] { method,
                        attrs.getValue("direction") }), currentStyle);
    }

    /**
     * The name of the method used as a key to let an UI know is paint border or
     * not
     */
    static String getBorgerPaintedPropertyKey(String method) {
        return "Synth" + method; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void processIconElement(Attributes attrs) {
        URL imageURL = base.getResource(attrs.getValue("path"));
        namedElements.put(attrs.getValue("id"), new ImageIcon(imageURL));
    }

    private void processOpaqueElement(Attributes attrs) {
        currentStyle.setOpaque(Boolean.parseBoolean(attrs.getValue("value"))); //$NON-NLS-1$
    }

    private void processInsetsElement(Attributes attrs) throws SAXException {

        String idref = attrs.getValue("idref"); //$NON-NLS-1$
        if (idref != null) {
            namedReferences.put(new XMLSynthKey(
                    "insets", currentState, idref, new String[0]), //$NON-NLS-1$
                    currentStyle);
            return;
        }

        try {

            Insets insets = new Insets(0, 0, 0, 0);
            String top = attrs.getValue("top"); //$NON-NLS-1$
            String bottom = attrs.getValue("bottom"); //$NON-NLS-1$
            String left = attrs.getValue("left"); //$NON-NLS-1$
            String right = attrs.getValue("right"); //$NON-NLS-1$
            if (top != null) {
                insets.top = Integer.parseInt(top);
            }
            if (bottom != null) {

                insets.bottom = Integer.parseInt(bottom);
            }
            if (left != null) {

                insets.left = Integer.parseInt(left);
            }
            if (right != null) {

                insets.right = Integer.parseInt(right);
            }

            currentStyle.setInsets(insets);
            String id = attrs.getValue("id"); //$NON-NLS-1$

            if (id != null) {

                namedElements.put(id, insets);
            }

        } catch (NumberFormatException e) {

            throw new SAXException(e);
        }
    }

    @SuppressWarnings("nls")
    private Insets getInsetsFromString(String source) {

        int top = Integer.parseInt(source.substring(0, source.indexOf(" ")));
        source = source.substring(source.indexOf(" ") + 1);
        int bottom = Integer.parseInt(source.substring(0, source.indexOf(" ")));
        source = source.substring(source.indexOf(" ") + 1);
        int left = Integer.parseInt(source.substring(0, source.indexOf(" ")));
        source = source.substring(source.indexOf(" ") + 1);
        int right = Integer.parseInt(source.substring(source.indexOf(" ") + 1));

        return new Insets(top, left, bottom, right);
    }

    /**
     * GraphicsUtils that can be obtained according to beansAdapter only.
     */
    private void processGraphicsUtilsElement(Attributes attrs) {

        namedReferences.put(new XMLSynthKey(G_UTILS_ELEMENT, currentState,
                attrs.getValue("idref"), new String[0]), currentStyle); //$NON-NLS-1$
    }

    private int computeStateValue(String stateName) throws SAXException {

        if (stateName == null) {

            return DEFAULT_STATE;

        } else if ("ENABLED".equals(stateName)) { //$NON-NLS-1$

            return SynthConstants.ENABLED;

        } else if ("DISABLED".equals(stateName)) { //$NON-NLS-1$

            return SynthConstants.DISABLED;

        } else if ("MOUSE_OVER".equals(stateName)) { //$NON-NLS-1$

            return SynthConstants.MOUSE_OVER;

        } else if ("PRESSED".equals(stateName)) { //$NON-NLS-1$

            return SynthConstants.PRESSED;

        } else if ("DEFAULT".equals(stateName)) { //$NON-NLS-1$

            return SynthConstants.DEFAULT;

        } else if ("FOCUSED".equals(stateName)) { //$NON-NLS-1$

            return SynthConstants.FOCUSED;

        } else if ("SELECTED".equals(stateName)) { //$NON-NLS-1$

            return SynthConstants.SELECTED;
        }

        int indexOfAND = stateName.indexOf(" AND "); //$NON-NLS-1$

        if (indexOfAND != -1) {

            return computeStateValue(stateName.substring(0, indexOfAND))
                    + computeStateValue(stateName.substring(indexOfAND + 5));

        }

        throw new SAXException(Messages.getString("swing.err.21", stateName)); //$NON-NLS-1$
    }

    /**
     * Beans adapter processing the unknown elements and obtains objects from
     * elements known to beansAdapter. All the obtained objects should be added
     * to namedElements map (parser do it in endDocument() method)
     */
    private static class BeansAdapter {

        private final String lsp = System.getProperty("line.separator"); //$NON-NLS-1$

        private final StringBuffer xmlLine = new StringBuffer();

        private final List<String> idList = new LinkedList<String>();

        /**
         * The nesting depth of xml tags
         */
        private int validatingCounter = 0;

        @SuppressWarnings("nls")
        public BeansAdapter() {
            xmlLine.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            xmlLine.append(lsp);
            xmlLine.append("<java>");
            xmlLine.append(lsp);
        }

        @SuppressWarnings("nls")
        public void pushStartTag(String element, Attributes attrs) {

            String id = attrs.getValue("id");

            if (id != null && validatingCounter == 0) {
                idList.add(id);
            }

            xmlLine.append("<");
            xmlLine.append(element);

            for (int i = 0; i < attrs.getLength(); i++) {
                String localName = attrs.getLocalName(i);
                xmlLine.append(" ");
                xmlLine.append(("".equals(localName)) ? attrs.getQName(i)
                        : localName);
                xmlLine.append("=\"");
                xmlLine.append(attrs.getValue(i));
                xmlLine.append("\" ");
            }

            xmlLine.append(">");
            xmlLine.append(lsp);

            validatingCounter++;

        }

        public void pushCharacters(char[] ch, int start, int end) {

            if (validatingCounter > 0) {
                xmlLine.append(ch, start, end);
            }

        }

        @SuppressWarnings("nls")
        public void pushEndTag(String element) {

            xmlLine.append("</");
            xmlLine.append(element);
            xmlLine.append(">");
            xmlLine.append(lsp);

            validatingCounter--;
        }

        private XMLDecoder createDecoder() {
            xmlLine.append("</java>"); //$NON-NLS-1$
            return new XMLDecoder(new ByteArrayInputStream(xmlLine.toString()
                    .getBytes()));
        }

        public void putAdaptedBeansToMap(Map<String, Object> elementsMap) {
            XMLDecoder d = createDecoder();
            for (String id : idList) {
                elementsMap.put(id, d.readObject());
            }
            d.close();

        }
    }

    /**
     * This class is used to represent correct keys in namedReferences table
     * which helps to unambiguously insert named element instead of reference in
     * the concluding stage of parsing
     */
    private static class XMLSynthKey {

        /**
         * This field represents the name of element to refer. By convention (in
         * this class) the type defines the additional info needed (i.e. the
         * Color element needed in colorType info and stateID, Insets element
         * not needed in info at all, etc)
         */
        private final String elementName;

        /**
         * This field represents the key to find in the namedElements table
         * (idref in xml file)
         */
        private final String reference;

        /**
         * This field provides information about concerned Objects necessary to
         * correct insertion to style (such as ColorType, method attribute for
         * painters etc)
         */
        private final String[] info;

        /**
         * StateID is also additional info, but because it is integer it taken
         * out from info array for performance reasons
         */
        private final int stateID;

        XMLSynthKey(String elementName, int stateID, String reference,
                String[] info) {
            this.elementName = elementName;
            this.reference = reference;
            this.info = info;
            this.stateID = stateID;
        }

        XMLSynthKey(String type, int stateID, String reference, String info) {
            this.elementName = type;
            this.reference = reference;
            this.info = new String[] { info };
            this.stateID = stateID;
        }

        String getElementName() {
            return elementName;
        }

        String getReference() {
            return reference;
        }

        String[] getAllInfo() {
            return info;
        }

        int getState() {
            return stateID;
        }

        /**
         * This method used if info array contains one element
         * 
         * @return the first element in the info array
         */
        String getFirstInfo() {
            return info != null ? info[0] : null;
        }

        @Override
        public boolean equals(Object key) {

            XMLSynthKey _key = (XMLSynthKey) key;
            boolean result;
            result = (_key.elementName == null) ? (this.elementName == null)
                    : _key.elementName.equals(this.elementName);
            result &= (this.stateID == _key.stateID);
            result &= (_key.reference == null) ? this.reference == null
                    : _key.reference.equals(this.reference);
            result &= Arrays.equals(_key.info, this.info);

            return result;
        }

        @Override
        public int hashCode() {
            return elementName.hashCode() + reference.hashCode()
                    + info.hashCode();
        }
    }

}
