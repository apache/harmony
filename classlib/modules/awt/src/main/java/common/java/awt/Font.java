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

package java.awt;

import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.font.TransformAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.peer.FontPeer;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.CharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.harmony.awt.gl.font.CommonGlyphVector;
import org.apache.harmony.awt.gl.font.FontManager;
import org.apache.harmony.awt.gl.font.FontPeerImpl;
import org.apache.harmony.awt.gl.font.fontlib.FLFontManager;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.misc.HashCode;


public class Font implements Serializable {
    private static final long serialVersionUID = -4206021311591459213L;

    // Identity Transform attribute
    private static final TransformAttribute IDENTITY_TRANSFORM = new TransformAttribute(new AffineTransform());

    public static final int PLAIN = 0;

    public static final int BOLD = 1;

    public static final int ITALIC = 2;

    public static final int ROMAN_BASELINE = 0;

    public static final int CENTER_BASELINE = 1;

    public static final int HANGING_BASELINE = 2;

    public static final int TRUETYPE_FONT = 0;
    
    public static final int TYPE1_FONT = 1;

    public static final int LAYOUT_LEFT_TO_RIGHT = 0;

    public static final int LAYOUT_RIGHT_TO_LEFT = 1;

    public static final int LAYOUT_NO_START_CONTEXT = 2;

    public static final int LAYOUT_NO_LIMIT_CONTEXT = 4;
    
    static final Font DEFAULT_FONT = new Font("Dialog", Font.PLAIN, 12); //$NON-NLS-1$

    protected String name;

    protected int style;

    protected int size;

    protected float pointSize;

    // Flag if the Font object transformed
    private boolean transformed;
    
    // Set of font attributes 
    private Hashtable<Attribute, Object> fRequestedAttributes;

    // font peer object corresponding to this Font
    private transient FontPeerImpl fontPeer;

    // number of glyphs in this Font    
    private transient int numGlyphs = -1;

    // code for missing glyph for this Font     
    private transient int missingGlyphCode = -1;

    /**
     * Writes object to ObjectOutputStream.
     * 
     * @param out ObjectOutputStream
     * @throws IOException
     */
    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException{
        out.defaultWriteObject();
    }

    /**
     * Reads object from ObjectInputStream object and set native platform
     * dependent fields to default values.
     * 
     * @param in ObjectInputStream object
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException{
        in.defaultReadObject();

        numGlyphs = -1;
        missingGlyphCode = -1;

    }

    public Font(Map<? extends Attribute, ?> attributes) {
        Object currAttr;

        // Default values are taken from the documentation of the Font class. 
        // See Font constructor, decode and getFont sections.
  
        this.name = "default"; //$NON-NLS-1$
        this.size = 12;
        this.pointSize = 12;
        this.style = Font.PLAIN;

        if (attributes != null){

            fRequestedAttributes = new Hashtable<Attribute, Object>(attributes);

            currAttr = attributes.get(TextAttribute.SIZE);
            if ( currAttr != null){
                this.pointSize = ((Float)currAttr).floatValue();
                this.size = (int)Math.ceil(this.pointSize);
            }

            currAttr = attributes.get(TextAttribute.POSTURE);
            if ( currAttr != null && 
                    currAttr.equals(TextAttribute.POSTURE_OBLIQUE)){
                this.style |= Font.ITALIC;
            }

            currAttr = attributes.get(TextAttribute.WEIGHT);
            if ( (currAttr != null) &&
                 (((Float)currAttr).floatValue() >= (TextAttribute.WEIGHT_BOLD).floatValue()) ){
                this.style |= Font.BOLD;
            }

            currAttr = attributes.get(TextAttribute.FAMILY);
            if ( currAttr != null){
                this.name = (String)currAttr;
            }

            currAttr = attributes.get(TextAttribute.TRANSFORM);
            if ( currAttr != null){
                if (currAttr instanceof TransformAttribute) {
                    this.transformed = !((TransformAttribute)currAttr).getTransform().isIdentity();
                } else if (currAttr instanceof AffineTransform) {
                    this.transformed = !((AffineTransform)currAttr).isIdentity();
                }
            }

        } else {
            fRequestedAttributes = new Hashtable<Attribute, Object>(5);
            fRequestedAttributes.put(TextAttribute.TRANSFORM, IDENTITY_TRANSFORM);

            this.transformed = false;

            fRequestedAttributes.put(TextAttribute.FAMILY, name);

            fRequestedAttributes.put(TextAttribute.SIZE, new Float(this.size));

            if ((this.style & Font.BOLD) != 0){
                fRequestedAttributes.put(TextAttribute.WEIGHT, 
                        TextAttribute.WEIGHT_BOLD);
            } else {
                fRequestedAttributes.put(TextAttribute.WEIGHT, 
                        TextAttribute.WEIGHT_REGULAR);
            }
            if ((this.style & Font.ITALIC) != 0){
                fRequestedAttributes.put(TextAttribute.POSTURE, 
                        TextAttribute.POSTURE_OBLIQUE);
            } else {
                fRequestedAttributes.put(TextAttribute.POSTURE, 
                        TextAttribute.POSTURE_REGULAR);
            }

        }
    }

    public Font(String name, int style, int size) {
        this.name = (name != null) ? name : "Default"; //$NON-NLS-1$
        this.size = (size >= 0) ? size : 0;
        this.style = (style & ~0x03) == 0 ? style : Font.PLAIN;
        this.pointSize = this.size;

        fRequestedAttributes = new Hashtable<Attribute, Object>(5);

        fRequestedAttributes.put(TextAttribute.TRANSFORM, IDENTITY_TRANSFORM);

        this.transformed = false;

        fRequestedAttributes.put(TextAttribute.FAMILY, this.name);
        fRequestedAttributes.put(TextAttribute.SIZE, new Float(this.size));

        if ((this.style & Font.BOLD) != 0){
            fRequestedAttributes.put(TextAttribute.WEIGHT,
                    TextAttribute.WEIGHT_BOLD);
        } else {
            fRequestedAttributes.put(TextAttribute.WEIGHT,
                    TextAttribute.WEIGHT_REGULAR);
        }
        if ((this.style & Font.ITALIC) != 0){
            fRequestedAttributes.put(TextAttribute.POSTURE,
                    TextAttribute.POSTURE_OBLIQUE);
        } else {
            fRequestedAttributes.put(TextAttribute.POSTURE,
                    TextAttribute.POSTURE_REGULAR);
        }
    }


    public boolean canDisplay(char c) {
        FontPeerImpl peer = (FontPeerImpl)this.getPeer();
        return peer.canDisplay(c);
    }
    
    public boolean canDisplay(int i) {
        if (!Character.isValidCodePoint(i)) {
            throw new IllegalArgumentException();
        }
        
        if (!FontManager.IS_FONTLIB) {
            //TODO implement true code point support 
            return canDisplay((char)i);
        }
        FontPeerImpl peer = (FontPeerImpl)this.getPeer();
        return peer.canDisplay(peer.getUnicodeByIndex(i));
    }

    public int canDisplayUpTo(char[] text, int start, int limit) {
        int st = start;
        int result;
        while ((st < limit) && canDisplay(text[st])) {
            st++;
        }

        if (st == limit) {
            result = -1;
        } else {
            result = st;
        }

        return result;
    }

    public int canDisplayUpTo(CharacterIterator iter, int start, int limit) {
        int st = start;
        char c = iter.setIndex(start);
        int result;

        while ((st < limit) && (canDisplay(c))){
            st++;
            c = iter.next();
        }
        if (st == limit) {
            result = -1;
        } else {
            result = st;
        }

        return result;
    }

    public int canDisplayUpTo(String str) {
        char[] chars = str.toCharArray();
        return canDisplayUpTo(chars, 0, chars.length);
    }


    public GlyphVector createGlyphVector(FontRenderContext frc, char[] chars) {
        return new CommonGlyphVector(chars, frc, this, 0);
    }


    public GlyphVector createGlyphVector(FontRenderContext frc,
            CharacterIterator iter) {
        char[] chars;
        int index = 0;
        if (iter.getEndIndex() != -1){
            chars = new char[iter.getEndIndex()];
        } else {
            return null;
        }
        for(char c = iter.first(); c != CharacterIterator.DONE; c = iter.next(),index++) {
            chars[index] = c;
        }

        return this.createGlyphVector(frc, chars);
    }

    public GlyphVector createGlyphVector(FontRenderContext frc, int[] glyphCodes){
        int length = glyphCodes.length;        
        char[] chars = new char[length];        
        FontPeerImpl peer = (FontPeerImpl) getPeer();
        
        for (int i = 0; i < length; i ++) {
            chars[i] = peer.getUnicodeByIndex(glyphCodes[i]); 
        }
        
        return new CommonGlyphVector(chars, frc, this, 0);
    }


    public GlyphVector createGlyphVector(FontRenderContext frc, String str) {
        return new CommonGlyphVector(str.toCharArray(), frc, this, 0);
    }

    /**
     * Returns font style constant value corresponding to one of the font style 
     * names ("BOLD", "ITALIC", "BOLDITALIC"). Method returns Font.PLAIN if there 
     * was no coincidence with predefined style names.
     * 
     * @param fontStyleName font style name
     * @return font style constant value according to the font style name specified.
     */
    private static int getFontStyle(String fontStyleName){
        int result = Font.PLAIN;

        if (fontStyleName.toUpperCase().equals("BOLDITALIC")) { //$NON-NLS-1$
            result = Font.BOLD | Font.ITALIC;
        } else if (fontStyleName.toUpperCase().equals("BOLD")) { //$NON-NLS-1$
            result = Font.BOLD;
        } else if (fontStyleName.toUpperCase().equals("ITALIC")) { //$NON-NLS-1$
            result = Font.ITALIC;
        }

        return result;
    }

    public static Font decode(String str) {
        // XXX: Documentation doesn't describe all cases, e.g. fonts face names with 
        // symbols that are suggested as delimiters in the documentation. 
        // In this decode implementation only ***-***-*** format is used with '-' 
        // as the delimiter to avoid unexpected parse results of font face names 
        // with spaces.
        
        if (str == null) {
            return DEFAULT_FONT;
        }
        
        StringTokenizer strTokens;
        String delim = "-"; //$NON-NLS-1$
        String substr;

        int fontSize = DEFAULT_FONT.size;
        int fontStyle = DEFAULT_FONT.style;
        String fontName = DEFAULT_FONT.name;

        strTokens = new StringTokenizer(str.trim(), delim);

        // Font Name
        if (strTokens.hasMoreTokens()){
            fontName = strTokens.nextToken(); // first token is the font name
        }

        // Font Style or Size (if the style is undefined)
        if (strTokens.hasMoreTokens()){
            substr = strTokens.nextToken();

            try {
                // if second token is the font size
                fontSize = Integer.valueOf(substr).intValue();
            } catch (NumberFormatException e) {
                // then second token is the font style
                fontStyle = getFontStyle(substr);
            }

        }

        // Font Size
        if (strTokens.hasMoreTokens()){
            try {
                fontSize = Integer.valueOf(strTokens.nextToken()).intValue();
            } catch (NumberFormatException e) {
            }
        }

        return new Font(fontName, fontStyle, fontSize);
    }

    @SuppressWarnings("unchecked")
    public Font deriveFont(AffineTransform trans) {

        if (trans == null) {
            // awt.94=transform can not be null
            throw new IllegalArgumentException(Messages.getString("awt.94")); //$NON-NLS-1$
        }

        Hashtable<Attribute, Object> derivefRequestedAttributes = (Hashtable<Attribute, Object>)fRequestedAttributes.clone();

        derivefRequestedAttributes.put(TextAttribute.TRANSFORM,
                new TransformAttribute(trans));

        return new Font(derivefRequestedAttributes);

    }

    @SuppressWarnings("unchecked")
    public Font deriveFont(float size) {
        Hashtable<Attribute, Object> derivefRequestedAttributes = (Hashtable<Attribute, Object>)fRequestedAttributes.clone();
        derivefRequestedAttributes.put(TextAttribute.SIZE, new Float(size));
        return new Font(derivefRequestedAttributes);
    }

    @SuppressWarnings("unchecked")
    public Font deriveFont(int style) {
        Hashtable<Attribute, Object> derivefRequestedAttributes = (Hashtable<Attribute, Object>)fRequestedAttributes.clone();

        if ((style & Font.BOLD) != 0){
            derivefRequestedAttributes.put(TextAttribute.WEIGHT,
                    TextAttribute.WEIGHT_BOLD);
        } else if (derivefRequestedAttributes.get(TextAttribute.WEIGHT) != null){
            derivefRequestedAttributes.remove(TextAttribute.WEIGHT);
        }

        if ((style & Font.ITALIC) != 0){
            derivefRequestedAttributes.put(TextAttribute.POSTURE,
                    TextAttribute.POSTURE_OBLIQUE);
        } else if (derivefRequestedAttributes.get(TextAttribute.POSTURE) != null){
            derivefRequestedAttributes.remove(TextAttribute.POSTURE);
        }

        return new Font(derivefRequestedAttributes);
    }

    @SuppressWarnings("unchecked")
    public Font deriveFont(int style, AffineTransform trans) {

        if (trans == null) {
            // awt.94=transform can not be null
            throw new IllegalArgumentException(Messages.getString("awt.94")); //$NON-NLS-1$
        }
        Hashtable<Attribute, Object> derivefRequestedAttributes = (Hashtable<Attribute, Object>)fRequestedAttributes.clone();

        if ((style & BOLD) != 0){
            derivefRequestedAttributes.put(TextAttribute.WEIGHT,
                    TextAttribute.WEIGHT_BOLD);
        } else if (derivefRequestedAttributes.get(TextAttribute.WEIGHT) != null){
            derivefRequestedAttributes.remove(TextAttribute.WEIGHT);
        }

        if ((style & ITALIC) != 0){
            derivefRequestedAttributes.put(TextAttribute.POSTURE,
                    TextAttribute.POSTURE_OBLIQUE);
        } else if (derivefRequestedAttributes.get(TextAttribute.POSTURE) != null){
            derivefRequestedAttributes.remove(TextAttribute.POSTURE);
        }
        derivefRequestedAttributes.put(TextAttribute.TRANSFORM,
                new TransformAttribute(trans));

        return new Font(derivefRequestedAttributes);
    }

    @SuppressWarnings("unchecked")
    public Font deriveFont(int style, float size) {
        Hashtable<Attribute, Object> derivefRequestedAttributes = (Hashtable<Attribute, Object>)fRequestedAttributes.clone();

        if ((style & BOLD) != 0){
            derivefRequestedAttributes.put(TextAttribute.WEIGHT,
                    TextAttribute.WEIGHT_BOLD);
        } else if (derivefRequestedAttributes.get(TextAttribute.WEIGHT) != null){
            derivefRequestedAttributes.remove(TextAttribute.WEIGHT);
        }

        if ((style & ITALIC) != 0){
            derivefRequestedAttributes.put(TextAttribute.POSTURE,
                    TextAttribute.POSTURE_OBLIQUE);
        } else if (derivefRequestedAttributes.get(TextAttribute.POSTURE) != null){
            derivefRequestedAttributes.remove(TextAttribute.POSTURE);
        }

        derivefRequestedAttributes.put(TextAttribute.SIZE, new Float(size));
        return new Font(derivefRequestedAttributes);

    }


    @SuppressWarnings("unchecked")
    public Font deriveFont(Map<? extends Attribute, ?> attributes) {
        Attribute[] avalAttributes = this.getAvailableAttributes();

        Hashtable<Attribute, Object> derivefRequestedAttributes = (Hashtable<Attribute, Object>)fRequestedAttributes.clone();
        Object currAttribute;
        for (Attribute element : avalAttributes) {
            currAttribute = attributes.get(element);
            if (currAttribute != null){
                derivefRequestedAttributes.put(element, currAttribute);
            }
        }
        return new Font(derivefRequestedAttributes);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){
            return true;
        }

        if (obj != null) {
          try {
            Font font = (Font)obj;

            return  ((this.style == font.style)
                    && (this.size == font.size)
                    && this.name.equals(font.name)
                    && (this.pointSize == font.pointSize)
                    && (this.getTransform()).equals(font.getTransform()));
          } catch (ClassCastException e) {
          }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public Map<TextAttribute, ?> getAttributes() {
        return (Map<TextAttribute, ?>)fRequestedAttributes.clone();
    }

    public Attribute[] getAvailableAttributes() {
        Attribute[] attrs = { TextAttribute.FAMILY,
                             TextAttribute.POSTURE,
                             TextAttribute.SIZE,
                             TextAttribute.TRANSFORM,
                             TextAttribute.WEIGHT,
                             TextAttribute.SUPERSCRIPT,
                             TextAttribute.WIDTH};
        return attrs;
    }

    public byte getBaselineFor(char c) {
        // TODO: implement using TT BASE table data
        return 0;
    }


    public String getFamily() {
        FontPeerImpl peer = (FontPeerImpl)this.getPeer();
        return peer.getFamily();


    }


    public String getFamily(Locale l) {
        if (l == null) {
            // awt.01='{0}' parameter is null
            throw new NullPointerException(Messages.getString(
                    "awt.01", "Locale")); //$NON-NLS-1$ //$NON-NLS-2$ 
        }

        FontPeerImpl peer = (FontPeerImpl) this.getPeer();
        return peer.getFamily(l);
    }


    public static Font getFont(Map<? extends Attribute, ?> attributes) {
        Font fnt = (Font)attributes.get(TextAttribute.FONT);
        if (fnt != null){
            return fnt;
        }
        return new Font(attributes);
    }

    public static Font getFont(String sp, Font f) {
        String pr = org.apache.harmony.awt.Utils.getSystemProperty(sp);
        if (pr == null) {
            return f;
        }
        return decode(pr);
    }

    public static Font getFont(String sp) {
        return getFont(sp, null);
    }


    public String getFontName() {        
        FontPeerImpl peer = (FontPeerImpl)this.getPeer();
        return peer.getFontName();
    }

    public String getFontName(Locale l) {
        FontPeerImpl peer = (FontPeerImpl)this.getPeer();
        return peer.getFontName(l);
    }

    public LineMetrics getLineMetrics(char[] chars, int start, int end,
            FontRenderContext frc) {
        
        if (frc == null) {
            // awt.00=FontRenderContext is null
            throw new NullPointerException(Messages.getString("awt.00")); //$NON-NLS-1$
        }

        FontPeerImpl peer = (FontPeerImpl) this.getPeer();

        return peer.getLineMetrics((new String(chars)).substring(start, end),
                frc, this.getTransform());
    }

    public LineMetrics getLineMetrics(CharacterIterator iter, int start,
            int end, FontRenderContext frc) {

        if (frc == null){
            // awt.00=FontRenderContext is null
            throw new NullPointerException(Messages.getString("awt.00")); //$NON-NLS-1$
        }

        String resultString;
        int iterCount;

        iterCount = end - start;
        if (iterCount < 0){
            resultString = ""; //$NON-NLS-1$
        } else{
            char[] chars = new char[iterCount];
            int i = 0;
            for (char c = iter.setIndex(start);
                    c != CharacterIterator.DONE && (i < iterCount);
                    c = iter.next()) {
                    chars[i] = c;
                    i++;
            }
            resultString = new String(chars);
        }
        return this.getLineMetrics(resultString, frc);
    }

    public LineMetrics getLineMetrics(String str, FontRenderContext frc) {
        
        if (frc == null){
            // awt.00=FontRenderContext is null
            throw new NullPointerException(Messages.getString("awt.00")); //$NON-NLS-1$
        }
        
        FontPeerImpl peer = (FontPeerImpl)this.getPeer();
        return peer.getLineMetrics(str, frc, getTransform());
    }

    public LineMetrics getLineMetrics(String str, int start, int end,
            FontRenderContext frc) {
        return this.getLineMetrics(str.substring(start, end), frc);
    }

    public Rectangle2D getStringBounds(CharacterIterator ci, int start,
            int end, FontRenderContext frc) {
        int first = ci.getBeginIndex();
        int finish = ci.getEndIndex();
        char[] chars;

        if (start < first) {
            // awt.95=Wrong start index: {0}
            throw new IndexOutOfBoundsException(Messages.getString("awt.95", start)); //$NON-NLS-1$
        }
        if ( end > finish) {
            // awt.96=Wrong finish index: {0}
            throw new IndexOutOfBoundsException(Messages.getString("awt.96", end)); //$NON-NLS-1$
        }
        if (start > end) {
            // awt.97=Wrong range length: {0}
            throw new IndexOutOfBoundsException(Messages.getString("awt.97", //$NON-NLS-1$
                    (end - start)));
        }

        if (frc == null) {
            throw new NullPointerException(Messages.getString("awt.00")); //$NON-NLS-1$
        }

        chars = new char[end - start];

        ci.setIndex(start);
        for(int i = 0; i < chars.length; i++) {
            chars[i] = ci.current();
            ci.next();
        }

        return this.getStringBounds(chars,0,chars.length,frc);

    }

    public Rectangle2D getStringBounds(String str, FontRenderContext frc) {
        char[] chars = str.toCharArray();
        return this.getStringBounds(chars, 0, chars.length, frc);

    }

    public Rectangle2D getStringBounds(String str, int start, int end,
            FontRenderContext frc) {

        return this.getStringBounds((str.substring(start, end)), frc);
    }

    public Rectangle2D getStringBounds(char[] chars, int start, int end,
            FontRenderContext frc) {
        if (start < 0) {
            // awt.95=Wrong start index: {0}
            throw new IndexOutOfBoundsException(Messages.getString("awt.95", start)); //$NON-NLS-1$
        }
        if ( end > chars.length) {
            // awt.96=Wrong finish index: {0}
            throw new IndexOutOfBoundsException(Messages.getString("awt.96", end)); //$NON-NLS-1$
        }
        if (start > end) {
            // awt.97=Wrong range length: {0}
            throw new IndexOutOfBoundsException(Messages.getString("awt.97", //$NON-NLS-1$
                    (end - start)));
        }

        if (frc == null) {
            throw new NullPointerException(Messages.getString("awt.00")); //$NON-NLS-1$
        }

        FontPeerImpl peer = (FontPeerImpl) this.getPeer();

        final int TRANSFORM_MASK = AffineTransform.TYPE_GENERAL_ROTATION
                | AffineTransform.TYPE_GENERAL_TRANSFORM;
        Rectangle2D bounds;

        AffineTransform transform = getTransform();
        AffineTransform frcTransform = frc.getTransform();

        // XXX: for transforms where an angle between basis vectors is not 90
        // degrees Rectanlge2D class doesn't fit as Logical bounds.
        if ((transform.getType() & TRANSFORM_MASK) == 0) {
            int width = 0;
            for (int i = start; i < end; i++) {
                width += peer.charWidth(chars[i]);
            }
            LineMetrics nlm = peer.getLineMetrics();
            bounds = transform.createTransformedShape(
                    new Rectangle2D.Float(0, -nlm.getAscent(), width, nlm
                            .getHeight())).getBounds2D();
        } else {
            int len = end - start;
            char[] subChars = new char[len];
            System.arraycopy(chars, start, subChars, 0, len);
            bounds = createGlyphVector(frc, subChars).getLogicalBounds();
        }
        
        if (!(frcTransform.isIdentity()))
            return frcTransform.createTransformedShape(bounds).getBounds2D();
        
        return bounds;
    }

    public Rectangle2D getMaxCharBounds(FontRenderContext frc) {
        if (frc == null){
            // awt.00=FontRenderContext is null
            throw new NullPointerException(Messages.getString("awt.00")); //$NON-NLS-1$ 
        }

        FontPeerImpl peer = (FontPeerImpl)this.getPeer();

        Rectangle2D bounds = peer.getMaxCharBounds(frc);
        AffineTransform transform = getTransform();
        // !! Documentation doesn't describe meaning of max char bounds 
        // for the fonts that have rotate transforms. For all transforms
        // returned bounds are the bounds of transformed maxCharBounds 
        // Rectangle2D that corresponds to the font with identity transform.
        // TODO: resolve this issue to return correct bounds
        bounds = transform.createTransformedShape(bounds).getBounds2D();

        return bounds;
    }

    public GlyphVector layoutGlyphVector(FontRenderContext frc, char[] chars,
                                            int start, int count, int flags) {
        // TODO: implement method for bidirectional text.
        // At the moment only LTR and RTL texts supported.
        if (start < 0) {
            // awt.95=Wrong start index: {0}
            throw new ArrayIndexOutOfBoundsException(Messages.getString("awt.95", //$NON-NLS-1$
                    start));
        }

        if (count < 0) {
            // awt.98=Wrong count value, can not be negative: {0}
            throw new ArrayIndexOutOfBoundsException(Messages.getString("awt.98", //$NON-NLS-1$
                    count));
        }

        if ( start + count > chars.length) {
            // awt.99=Wrong [start + count] is out of range: {0}
            throw new ArrayIndexOutOfBoundsException(Messages.getString("awt.99", //$NON-NLS-1$
                    (start + count)));
        }

        char[] out = new char[count];
        System.arraycopy(chars, start, out, 0, count);

        return new CommonGlyphVector(out, frc, this, flags);
    }

    @Override
    public String toString() {
        String stl = "plain"; //$NON-NLS-1$
        String result;

        if (this.isBold() && this.isItalic()){
            stl = "bolditalic"; //$NON-NLS-1$
        }
        if (this.isBold() && !this.isItalic()){
            stl = "bold"; //$NON-NLS-1$
        }

        if (!this.isBold() && this.isItalic()){
            stl = "italic"; //$NON-NLS-1$
        }

        result = this.getClass().getName() +
                "[family=" + this.getFamily() + //$NON-NLS-1$
                ",name="+ this.name + //$NON-NLS-1$
                ",style=" + stl + //$NON-NLS-1$
                ",size=" + this.size + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        return result;
    }

    public String getPSName() {
        FontPeerImpl peer = (FontPeerImpl)this.getPeer();
        return peer.getPSName();
    }

    public String getName() {
        return (this.name);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public FontPeer getPeer() {        
        if (fontPeer == null){
            fontPeer = (FontPeerImpl) FontManager.getInstance().getFontPeer(
                    this.getName(), 
                    this.getStyle(), 
                    this.getSize()
                    );
             
        }
        return fontPeer;
    }

    public AffineTransform getTransform() {
        Object transform = fRequestedAttributes.get(TextAttribute.TRANSFORM);

        if (transform != null) {
            if (transform instanceof TransformAttribute) {
                return ((TransformAttribute) transform).getTransform();
            }
            if (transform instanceof AffineTransform) {
                return new AffineTransform((AffineTransform) transform);
            }
        } else {
            transform = new AffineTransform();
        }
        return (AffineTransform) transform;

    }

    public boolean isTransformed() {
        return this.transformed;
    }

    public boolean isPlain() {
        return (this.style  == PLAIN);
    }

    public boolean isItalic() {
        return (this.style & ITALIC) != 0;
    }

    public boolean isBold() {
        return (this.style & BOLD) != 0;
    }

    public boolean hasUniformLineMetrics() {
        FontPeerImpl peer = (FontPeerImpl)this.getPeer();
        return peer.hasUniformLineMetrics();
    }

    @Override
    public int hashCode() {
        HashCode hash = new HashCode();

        hash.append(this.name);
        hash.append(this.style);
        hash.append(this.size);

        return hash.hashCode();
    }

    public int getStyle() {
        return this.style;
    }

    public int getSize() {
        return this.size;
    }

    public int getNumGlyphs() {
        if (numGlyphs == -1){
            FontPeerImpl peer = (FontPeerImpl)this.getPeer();
            this.numGlyphs =  peer.getNumGlyphs();
        }
        return this.numGlyphs;
    }

    public int getMissingGlyphCode() {
        if (missingGlyphCode == -1){
            FontPeerImpl peer = (FontPeerImpl)this.getPeer();
            this.missingGlyphCode =  peer.getMissingGlyphCode();
        }
        return this.missingGlyphCode;
    }

    public float getSize2D() {
        return this.pointSize;
    }

    public float getItalicAngle() {
        FontPeerImpl peer = (FontPeerImpl)this.getPeer();
        return peer.getItalicAngle();
    }

    public static Font createFont(int fontFormat, File fontFile) throws FontFormatException,
            IOException {
        if (fontFile == null) throw new NullPointerException();
        
        if (FontManager.IS_FONTLIB) {
            if (fontFormat != TRUETYPE_FONT && fontFormat != TYPE1_FONT) {
                // awt.9A=Unsupported font format
                throw new IllegalArgumentException ( Messages.getString("awt.9A") ); //$NON-NLS-1$
            }
            
            return ((FLFontManager)FontManager.getInstance()).embedFont(fontFile.getAbsolutePath(), fontFormat);
        }
        InputStream is = new FileInputStream(fontFile);
        try {
            return createFont(fontFormat, is);
        } finally {
            is.close();
        }
    }
    
    public static Font createFont(int fontFormat, InputStream fontStream)
            throws FontFormatException, IOException {

        BufferedInputStream buffStream;
        int bRead = 0;
        int size = 8192;  // memory page size, for the faster reading
        byte buf[] = new byte[size];

        if (fontFormat != TRUETYPE_FONT && !FontManager.IS_FONTLIB) {
            // awt.9A=Unsupported font format
            throw new IllegalArgumentException ( Messages.getString("awt.9A") ); //$NON-NLS-1$
        }

        /* Get font file in system-specific directory */
        File fontFile = FontManager.getInstance().getTempFontFile();
//        File fontFile = Toolkit.getDefaultToolkit().getGraphicsFactory().getFontManager().getTempFontFile();


        buffStream = new BufferedInputStream ( fontStream );
        FileOutputStream fOutStream = new FileOutputStream(fontFile);

        bRead = buffStream.read ( buf, 0, size );

        while ( bRead != -1 ) {
            fOutStream.write ( buf, 0, bRead );
            bRead = buffStream.read ( buf, 0, size );
        }

        buffStream.close();
        fOutStream.close();

        Font font = null;
        
        if (FontManager.IS_FONTLIB) {
            font = ((FLFontManager)FontManager.getInstance()).embedFont(fontFile.getAbsolutePath(), fontFormat);
        } else {
            font = Toolkit.getDefaultToolkit().getGraphicsFactory().embedFont(fontFile.getAbsolutePath());
        }
        if ( font == null ) {
            // awt.9B=Can't create font - bad font data
            throw new FontFormatException ( Messages.getString("awt.9B") ); //$NON-NLS-1$
        }
        return font;
    }

}


