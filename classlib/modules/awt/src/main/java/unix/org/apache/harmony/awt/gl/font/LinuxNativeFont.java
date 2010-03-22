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
 * @author Ilya S. Okomin
 */
package org.apache.harmony.awt.gl.font;

import java.awt.Font;
import java.io.IOException;

import java.util.*;

import org.apache.harmony.awt.gl.font.FontPeerImpl;
import org.apache.harmony.awt.internal.nls.Messages;

/**
 *
 *  Library wrapper of native linux font functions.
 */
public class LinuxNativeFont {
    
    public static final int FC_SLANT_ROMAN = 0;
    public static final int FC_SLANT_ITALIC = 100;
    public static final int FC_SLANT_OBLIQUE = 110;
    public static final int FC_WEIGHT_MEDIUM = 100;

    /**
     * Returns array of Strings that represents list of all font families names
     * available on the system.  
     */
    public synchronized static native String[] getFontFamiliesNames();

    /**
     * Returns true if the new font was added to the system, false
     * otherwise.  Methods checks if the number of system fonts
     * changed after font configuration was rebuilt.
     *   
     * @param absolutePath absolute path to the font.
     */
    public synchronized static native String embedFontNative(String absolutePath);

    /**
     * Initiailzes native Xft font object from specified parameters and returns 
     * font handle, also sets font type to the font peer parameter. 
     * NullPointerException is thrown if there are errors in native code. 
     * 
     * @param linFont LinuxFont instance
     * @param family font family name
     * @param style style of the font
     * @param size size of the font
     * @param styleName style name of the font
     */
    public synchronized static native long initializeFont(LinuxFont linFont, String family, int style, int size, String styleName);

    /** 
     * Returns number of glyphs in specified XftFont if success. 
     * 
     * @param hndFont XftFont handle
     */

    public synchronized static native int getNumGlyphsNative(long hndFont);

    /**
     * Returns true, if XftFont object can display specified char.
     * 
     * @param hndFont XftFont handle
     * @param c specified char
     */
    // !! Instead of this method getGlyphCode can be used
    // TODO: implement method and find out if this method faster than getGlyphCode 
    // usage 
    public synchronized static native boolean canDisplayCharNative(long hndFont, char c);

    /**
     * Returns family name of the XftFont object.
     * 
     * @param hndFont XftFont handle
     */
    public synchronized static native String getFamilyNative(long hndFont);

    /**
     * Returns face name of the XftFont object.
     * 
     * @param hndFont XftFont handle
     */
    public synchronized static native String getFontNameNative(long hndFont);

    /**
     * Returns XftFont's postscript name.
     * Returned value is the name of the font in system default locale or
     * for english langid if there is no results for default locale settings. 
     * 
     * @param fnt XftFont handle
     */
    public synchronized static native String getFontPSNameNative(long fnt);

    /**
     * Getting antialiasing font from existing font
     */
    public static native long getAntialiasedFont(long font, long display, boolean isAntialiasing);

    /**
     * Disposing XftFont object.
     * 
     * @param hndFont XftFont handle
     * @param display Display handle
     */
    public synchronized static native void pFontFree(long hndFont, long display);

    /**
     * Returns tangent of Italic angle of given Font.
     * Returned value is null and NullPointerException is thrown if there is Xft error.
     * 
     * @param hndFont XftFont handle
     * @param fontType type of the font
     */
    public synchronized static native float getItalicAngleNative(long hndFont, int fontType);

    /** 
     * Returns an array of available system fonts names.
     * In case of errors in native code NullPointerException is thrown.
     */
    public synchronized static native String[] getFonts();

    /**
     * Returns array of values of font metrics corresponding to the given XftFont 
     * font object. NullPointerException is thrown in case errors in native code.
     * 
     * @param hFont XftFont handle
     * @param fontSize size of the font
     * @param isAntialiased parameter true if antialiased metrics required
     * @param usesFractionalMetrics true if results calculated using fractional metrics
     * @param fontType type of the specified font
     */
    public synchronized static native float[] getNativeLineMetrics(long hFont, int fontSize,
            boolean isAntialiased, boolean usesFractionalMetrics, int fontType);

    /** 
     * Returns array of glyph metrics values for the specified character
     * null is returned and NullPointerException is thrown in case of FreeType 
     * errors.
     * 
     * @param pFnt XftFont handle
     * @param c specified char
     */
    public synchronized static native float[] getGlyphInfoNative(long pFnt, char c,
            int fontSize);

    /** 
     * Returns array of glyph metrics values in pixels for the specified character
     * null is returned and NullPointerException is thrown in case of FreeType errors.
     * 
     * @param pFnt XftFont handle
     * @param c specified char
     */
    public synchronized static native int[] getGlyphPxlInfoNative(long display, long pFnt, char c);

    /**
     * Returns glyphs code corresponding to the characters in String specified, null 
     * is returned if failure. NullPointerException is thrown in case of Display 
     * is null.
     * 
     * @param fnt XftFont handle
     * @param uChar specified char
     * @param display Display handle
     */
    // TODO: implement native call
    public synchronized static native int[] getGlyphCodesNative(long fnt, String str, int len);

    /**
     * Returns glyph code corresponding to the specified character, null is 
     * returned if failure. NullPointerException is thrown in case of Display is null.
     * 
     * @param fnt XftFont handle
     * @param uChar specified char
     * @param display Display handle
     */
    public synchronized static native int getGlyphCodeNative(long fnt, char uChar, long display);

    /**
     * Updates specified folder where temporary font created from InputStream stored.
     * This method used in LinuxFont dispose method, it re-caches ~/.fonts
     * directory, after temporary font file is deleted.
     *   
     * @param tempFontFileName directory that is being re-cached name.
     * @return not null value if success, 0 otherwise
     */
    public synchronized static native int RemoveFontResource(String tempFontFileName);

    /**
     * Draws text on XftDraw with specified parameters using Xft library.
     * 
     * @param xftDraw XftDraw handle
     * @param display Display handle
     * @param colormap Colormap handle
     * @param font XftFont handle
     * @param x X coordinate where to draw at
     * @param y Y coordinate where to draw at
     * @param chars array of chars to draw
     * @param len length of the array of chars
     * @param xcolor XColor handle, the color of the text
     */
    public synchronized static native void drawStringNative(long xftDraw, long display, long colormap, long font, int x, int y, char[] chars, int len, long xcolor);
    
// FreeType routines
    
    /**
     * Returns pointer to GlyphBitmap structure that represents bitmap
     * with parameters of the character specified or 0 if failures 
     * in native code.
     * 
     * @param fnt XftFont handle
     * @param chr specified char
     */
    public synchronized static native long NativeInitGlyphBitmap(long fnt, char chr);
    
    /**
     * Disposes memory block that is used by FreeType FT_Bitmap structure
     * by pointer specified.
     * 
     * @param ptr specified pointer to the memory block
     */    
    public synchronized static native void NativeFreeGlyphBitmap(long bitmap);

    /**
     * Returns pointer to the FreeType FT_Outline structure. 
     * 
     * @param pFont XFT font handle
     * @param c specified character
     */
    public synchronized static native long getGlyphOutline(long pFont, char c);
    
    /**
     * Disposes memory block that is used by FreeType FT_Outline structure
     * by pointer specified.
     * 
     * @param ptr specified pointer to the memory block
     */
    public synchronized static native void freeGlyphOutline(long ptr);

    /**
     * Returns an array of pairs of coordinates [x1, y1, x2, y2...] from 
     * FreeType FT_Vector structure.  
     * 
     * @param ft_vector pointer to the memory block with FT_Vector structure
     * @param size number of elements in FT_Vector structure
     */
    public synchronized static native float[] getPointsFromFTVector(long ft_vector, int size);

// Xft routines
    
    /**
     * Returns XftDraw handle created from specified parameters using Xft library.
     * 
     * @param display Display handle 
     * @param drawable Drawable handle
     * @param visual Visual handle
     */
    public synchronized static native long createXftDrawNative(long display, long drawable, long visual);

    /**
     * Destroys XftDraw object.
     * @param xftDraw XftDraw handle 
     */
    public synchronized static native void freeXftDrawNative(long xftDraw);

    /**
     * Set new subwindow mode to XftDraw object
     *  
     * @param xftDraw XftDraw handle 
     * @param mode new mode
     */
    public static native void xftDrawSetSubwindowModeNative(long xftDraw, int mode);

    /**
     * Sets clipping rectangles in Xft drawable to the specified clipping rectangles. 
     * 
     * @param xftDraw XftDraw handle
     * @param xOrigin x position to start
     * @param yOrigin y position to start
     * @param rects handle to the memory block representing XRectangles array
     * @param n number of rectangles
     * 
     * @return result true if success in native call, false otherwise 
     */
    public static native boolean XftDrawSetClipRectangles(long xftDraw, int xOrigin,
            int yOrigin, long rects, int n);

//  public static native boolean isCharExists(char chr);

    /**
     * Returns an array of extrametrics of the font:<p>
     *  elem[0] - the average width of characters in the font (Type1 - 0.0f)<p>
     *  elem[1] - horizontal size for subscripts (Type1 - 0.7f * fontHeight)<p> 
     *  elem[2] - vertical size for subscripts (Type1 - 0.65f * fontHeight)<p> 
     *  elem[3] - horizontal offset for subscripts (Type1 - 0.0f)<p>
     *  elem[4] - vertical offset value for subscripts(Type1 - 0.15f * fontHeight)<p>
     *  elem[5] - horizontal size for superscripts (Type1 - 0.7f * fontHeight)<p>
     *  elem[6] - vertical size for superscripts (Type1 - 0.65f * fontHeight)<p>
     *  elem[7] - horizontal offset for superscripts (Type1 - 0.0f)<p> 
     *  elem[8] - vertical offset for superscripts (Type1 - 0.45f * fontHeight)<p> 
     * For TrueType fonts metrics are taken from OS2 table, for Type1 fonts
     * metrics are calculated using coefficients (read FontExtraMetrics comments).
     * OS2 table can be found at http://www.freetype.org/freetype2/docs/reference/ft2-truetype_tables.html#TT_OS2
     * 
     * @param hFont XFT font handle
     * @param fontSize font size
     * @param fontType type of the font
     */
    public synchronized static native float[] getExtraMetricsNative(long hFont, int fontSize, int fontType);


    /**
     * Initializes LCID table
     */
    public static void initLCIDsTable(Hashtable ht){

            /*
             *  Language records with LCID values (0x04**).
             */
             ht.put(new String("ar"), new Short((short)0x0401)); // ar-dz //$NON-NLS-1$
             ht.put(new String("bg"), new Short((short)0x0402)); //$NON-NLS-1$
             ht.put(new String("ca"), new Short((short)0x0403)); //$NON-NLS-1$
             ht.put(new String("zh"), new Short((short)0x0404)); // zh-tw //$NON-NLS-1$
             ht.put(new String("cs"), new Short((short)0x0405)); //$NON-NLS-1$
             ht.put(new String("da"), new Short((short)0x0406)); //$NON-NLS-1$
             ht.put(new String("de"), new Short((short)0x0407)); // de-de //$NON-NLS-1$
             ht.put(new String("el"), new Short((short)0x0408)); //$NON-NLS-1$
             ht.put(new String("fi"), new Short((short)0x040b)); //$NON-NLS-1$
             ht.put(new String("fr"), new Short((short)0x040c)); // fr-fr //$NON-NLS-1$
             ht.put(new String("iw"), new Short((short)0x040d)); // "he" //$NON-NLS-1$
             ht.put(new String("hu"), new Short((short)0x040e)); //$NON-NLS-1$
             ht.put(new String("is"), new Short((short)0x040f)); //$NON-NLS-1$
             ht.put(new String("it"), new Short((short)0x0410)); // it-it //$NON-NLS-1$
             ht.put(new String("ja"), new Short((short)0x0411)); //$NON-NLS-1$
             ht.put(new String("ko"), new Short((short)0x0412)); //$NON-NLS-1$
             ht.put(new String("nl"), new Short((short)0x0413)); // nl-nl //$NON-NLS-1$
             ht.put(new String("no"), new Short((short)0x0414)); // no_no //$NON-NLS-1$
             ht.put(new String("pl"), new Short((short)0x0415)); //$NON-NLS-1$
             ht.put(new String("pt"), new Short((short)0x0416)); // pt-br //$NON-NLS-1$
             ht.put(new String("rm"), new Short((short)0x0417)); //$NON-NLS-1$
             ht.put(new String("ro"), new Short((short)0x0418)); //$NON-NLS-1$
             ht.put(new String("ru"), new Short((short)0x0419)); //$NON-NLS-1$
             ht.put(new String("hr"), new Short((short)0x041a)); //$NON-NLS-1$
             ht.put(new String("sk"), new Short((short)0x041b)); //$NON-NLS-1$
             ht.put(new String("sq"), new Short((short)0x041c)); //$NON-NLS-1$
             ht.put(new String("sv"), new Short((short)0x041d)); // sv-se //$NON-NLS-1$
             ht.put(new String("th"), new Short((short)0x041e)); //$NON-NLS-1$
             ht.put(new String("tr"), new Short((short)0x041f)); //$NON-NLS-1$
             ht.put(new String("ur"), new Short((short)0x0420)); //$NON-NLS-1$
             ht.put(new String("in"), new Short((short)0x0421)); // "id" //$NON-NLS-1$
             ht.put(new String("uk"), new Short((short)0x0422)); //$NON-NLS-1$
             ht.put(new String("be"), new Short((short)0x0423)); //$NON-NLS-1$
             ht.put(new String("sl"), new Short((short)0x0424)); //$NON-NLS-1$
             ht.put(new String("et"), new Short((short)0x0425)); //$NON-NLS-1$
             ht.put(new String("lv"), new Short((short)0x0426)); //$NON-NLS-1$
             ht.put(new String("lt"), new Short((short)0x0427)); //$NON-NLS-1$
             ht.put(new String("fa"), new Short((short)0x0429)); //$NON-NLS-1$
             ht.put(new String("vi"), new Short((short)0x042a)); //$NON-NLS-1$
             ht.put(new String("hy"), new Short((short)0x042b)); //$NON-NLS-1$
             ht.put(new String("eu"), new Short((short)0x042d)); //$NON-NLS-1$
             ht.put(new String("sb"), new Short((short)0x042e)); //$NON-NLS-1$
             ht.put(new String("mk"), new Short((short)0x042f)); //$NON-NLS-1$
             ht.put(new String("sx"), new Short((short)0x0430)); //$NON-NLS-1$
             ht.put(new String("ts"), new Short((short)0x0431)); //$NON-NLS-1$
             ht.put(new String("tn"), new Short((short)0x0432)); //$NON-NLS-1$
             ht.put(new String("xh"), new Short((short)0x0434)); //$NON-NLS-1$
             ht.put(new String("zu"), new Short((short)0x0435)); //$NON-NLS-1$
             ht.put(new String("af"), new Short((short)0x0436)); //$NON-NLS-1$
             ht.put(new String("fo"), new Short((short)0x0438)); //$NON-NLS-1$
             ht.put(new String("hi"), new Short((short)0x0439)); //$NON-NLS-1$
             ht.put(new String("mt"), new Short((short)0x043a)); //$NON-NLS-1$
             ht.put(new String("gd"), new Short((short)0x043c)); //$NON-NLS-1$
             ht.put(new String("yi"), new Short((short)0x043d)); //$NON-NLS-1$
             ht.put(new String("sw"), new Short((short)0x0441)); //$NON-NLS-1$
             ht.put(new String("tt"), new Short((short)0x0444)); //$NON-NLS-1$
             ht.put(new String("ta"), new Short((short)0x0449)); //$NON-NLS-1$
             ht.put(new String("mr"), new Short((short)0x044e)); //$NON-NLS-1$
             ht.put(new String("sa"), new Short((short)0x044f)); //$NON-NLS-1$

            /*
             *  Language-country records.
             */
             ht.put(new String("ar_SA"), new Short((short)0x401)); //$NON-NLS-1$
             ht.put(new String("bg_BG"), new Short((short)0x402)); //$NON-NLS-1$
             ht.put(new String("ca_ES"), new Short((short)0x403)); //$NON-NLS-1$
             ht.put(new String("zh_TW"), new Short((short)0x404)); //$NON-NLS-1$
             ht.put(new String("cs_CZ"), new Short((short)0x405)); //$NON-NLS-1$
             ht.put(new String("da_DK"), new Short((short)0x406)); //$NON-NLS-1$
             ht.put(new String("de_DE"), new Short((short)0x407)); //$NON-NLS-1$
             ht.put(new String("el_GR"), new Short((short)0x408)); //$NON-NLS-1$
             ht.put(new String("en_US"), new Short((short)0x409)); //$NON-NLS-1$
             ht.put(new String("es_ES"), new Short((short)0x40a)); //$NON-NLS-1$
             ht.put(new String("fi_FI"), new Short((short)0x40b)); //$NON-NLS-1$
             ht.put(new String("fr_FR"), new Short((short)0x40c)); //$NON-NLS-1$
             ht.put(new String("he_IL"), new Short((short)0x40d)); //$NON-NLS-1$
             ht.put(new String("hu_HU"), new Short((short)0x40e)); //$NON-NLS-1$
             ht.put(new String("is_IS"), new Short((short)0x40f)); //$NON-NLS-1$
             ht.put(new String("it_IT"), new Short((short)0x410)); //$NON-NLS-1$
             ht.put(new String("ja_JP"), new Short((short)0x411)); //$NON-NLS-1$
             ht.put(new String("ko_KR"), new Short((short)0x412)); //$NON-NLS-1$
             ht.put(new String("nl_NL"), new Short((short)0x413)); //$NON-NLS-1$
             ht.put(new String("nb_NO"), new Short((short)0x414)); //$NON-NLS-1$
             ht.put(new String("pl_PL"), new Short((short)0x415)); //$NON-NLS-1$
             ht.put(new String("pt_BR"), new Short((short)0x416)); //$NON-NLS-1$
             ht.put(new String("ro_RO"), new Short((short)0x418)); //$NON-NLS-1$
             ht.put(new String("ru_RU"), new Short((short)0x419)); //$NON-NLS-1$
             ht.put(new String("hr_HR"), new Short((short)0x41a)); //$NON-NLS-1$
             ht.put(new String("sk_SK"), new Short((short)0x41b)); //$NON-NLS-1$
             ht.put(new String("sq_AL"), new Short((short)0x41c)); //$NON-NLS-1$
             ht.put(new String("sv_SE"), new Short((short)0x41d)); //$NON-NLS-1$
             ht.put(new String("th_TH"), new Short((short)0x41e)); //$NON-NLS-1$
             ht.put(new String("tr_TR"), new Short((short)0x41f)); //$NON-NLS-1$
             ht.put(new String("ur_PK"), new Short((short)0x420)); //$NON-NLS-1$
             ht.put(new String("id_ID"), new Short((short)0x421)); //$NON-NLS-1$
             ht.put(new String("uk_UA"), new Short((short)0x422)); //$NON-NLS-1$
             ht.put(new String("be_BY"), new Short((short)0x423)); //$NON-NLS-1$
             ht.put(new String("sl_SI"), new Short((short)0x424)); //$NON-NLS-1$
             ht.put(new String("et_EE"), new Short((short)0x425)); //$NON-NLS-1$
             ht.put(new String("lv_LV"), new Short((short)0x426)); //$NON-NLS-1$
             ht.put(new String("lt_LT"), new Short((short)0x427)); //$NON-NLS-1$
             ht.put(new String("fa_IR"), new Short((short)0x429)); //$NON-NLS-1$
             ht.put(new String("vi_VN"), new Short((short)0x42a)); //$NON-NLS-1$
             ht.put(new String("hy_AM"), new Short((short)0x42b)); //$NON-NLS-1$
             ht.put(new String("az_AZ"), new Short((short)0x42c)); //$NON-NLS-1$
             ht.put(new String("eu_ES"), new Short((short)0x42d)); //$NON-NLS-1$
             ht.put(new String("mk_MK"), new Short((short)0x42f)); //$NON-NLS-1$
             ht.put(new String("af_ZA"), new Short((short)0x436)); //$NON-NLS-1$
             ht.put(new String("ka_GE"), new Short((short)0x437)); //$NON-NLS-1$
             ht.put(new String("fo_FO"), new Short((short)0x438)); //$NON-NLS-1$
             ht.put(new String("hi_IN"), new Short((short)0x439)); //$NON-NLS-1$
             ht.put(new String("ms_MY"), new Short((short)0x43e)); //$NON-NLS-1$
             ht.put(new String("kk_KZ"), new Short((short)0x43f)); //$NON-NLS-1$
             ht.put(new String("ky_KG"), new Short((short)0x440)); //$NON-NLS-1$
             ht.put(new String("sw_KE"), new Short((short)0x441)); //$NON-NLS-1$
             ht.put(new String("uz_UZ"), new Short((short)0x443)); //$NON-NLS-1$
             ht.put(new String("tt_TA"), new Short((short)0x444)); //$NON-NLS-1$
             ht.put(new String("pa_IN"), new Short((short)0x446)); //$NON-NLS-1$
             ht.put(new String("gu_IN"), new Short((short)0x447)); //$NON-NLS-1$
             ht.put(new String("ta_IN"), new Short((short)0x449)); //$NON-NLS-1$
             ht.put(new String("te_IN"), new Short((short)0x44a)); //$NON-NLS-1$
             ht.put(new String("kn_IN"), new Short((short)0x44b)); //$NON-NLS-1$
             ht.put(new String("mr_IN"), new Short((short)0x44e)); //$NON-NLS-1$
             ht.put(new String("sa_IN"), new Short((short)0x44f)); //$NON-NLS-1$
             ht.put(new String("mn_MN"), new Short((short)0x450)); //$NON-NLS-1$
             ht.put(new String("gl_ES"), new Short((short)0x456)); //$NON-NLS-1$
             ht.put(new String("ko_IN"), new Short((short)0x457)); //$NON-NLS-1$
             ht.put(new String("sy_SY"), new Short((short)0x45a)); //$NON-NLS-1$
             ht.put(new String("di_MV"), new Short((short)0x465)); //$NON-NLS-1$
             ht.put(new String("ar_IQ"), new Short((short)0x801)); //$NON-NLS-1$
             ht.put(new String("zh_CN"), new Short((short)0x804)); //$NON-NLS-1$
             ht.put(new String("de_CH"), new Short((short)0x807)); //$NON-NLS-1$
             ht.put(new String("en_GB"), new Short((short)0x809)); //$NON-NLS-1$
             ht.put(new String("es_MX"), new Short((short)0x80a)); //$NON-NLS-1$
             ht.put(new String("fr_BE"), new Short((short)0x80c)); //$NON-NLS-1$
             ht.put(new String("it_CH"), new Short((short)0x810)); //$NON-NLS-1$
             ht.put(new String("nl_BE"), new Short((short)0x813)); //$NON-NLS-1$
             ht.put(new String("nn_NO"), new Short((short)0x814)); //$NON-NLS-1$
             ht.put(new String("pt_PT"), new Short((short)0x816)); //$NON-NLS-1$
             ht.put(new String("sr_SP"), new Short((short)0x81a)); //$NON-NLS-1$
             ht.put(new String("sv_FI"), new Short((short)0x81d)); //$NON-NLS-1$
             ht.put(new String("az_AZ"), new Short((short)0x82c)); //$NON-NLS-1$
             ht.put(new String("ms_BN"), new Short((short)0x83e)); //$NON-NLS-1$
             ht.put(new String("uz_UZ"), new Short((short)0x843)); //$NON-NLS-1$
             ht.put(new String("ar_EG"), new Short((short)0xc01)); //$NON-NLS-1$
             ht.put(new String("zh_HK"), new Short((short)0xc04)); //$NON-NLS-1$
             ht.put(new String("de_AT"), new Short((short)0xc07)); //$NON-NLS-1$
             ht.put(new String("en_AU"), new Short((short)0xc09)); //$NON-NLS-1$
             ht.put(new String("es_ES"), new Short((short)0xc0a)); //$NON-NLS-1$
             ht.put(new String("fr_CA"), new Short((short)0xc0c)); //$NON-NLS-1$
             ht.put(new String("sr_SP"), new Short((short)0xc1a)); //$NON-NLS-1$
             ht.put(new String("ar_LY"), new Short((short)0x1001)); //$NON-NLS-1$
             ht.put(new String("zh_SG"), new Short((short)0x1004)); //$NON-NLS-1$
             ht.put(new String("de_LU"), new Short((short)0x1007)); //$NON-NLS-1$
             ht.put(new String("en_CA"), new Short((short)0x1009)); //$NON-NLS-1$
             ht.put(new String("es_GT"), new Short((short)0x100a)); //$NON-NLS-1$
             ht.put(new String("fr_CH"), new Short((short)0x100c)); //$NON-NLS-1$
             ht.put(new String("ar_DZ"), new Short((short)0x1401)); //$NON-NLS-1$
             ht.put(new String("zh_MO"), new Short((short)0x1404)); //$NON-NLS-1$
             ht.put(new String("de_LI"), new Short((short)0x1407)); //$NON-NLS-1$
             ht.put(new String("en_NZ"), new Short((short)0x1409)); //$NON-NLS-1$
             ht.put(new String("es_CR"), new Short((short)0x140a)); //$NON-NLS-1$
             ht.put(new String("fr_LU"), new Short((short)0x140c)); //$NON-NLS-1$
             ht.put(new String("ar_MA"), new Short((short)0x1801)); //$NON-NLS-1$
             ht.put(new String("en_IE"), new Short((short)0x1809)); //$NON-NLS-1$
             ht.put(new String("es_PA"), new Short((short)0x180a)); //$NON-NLS-1$
             ht.put(new String("fr_MC"), new Short((short)0x180c)); //$NON-NLS-1$
             ht.put(new String("ar_TN"), new Short((short)0x1c01)); //$NON-NLS-1$
             ht.put(new String("en_ZA"), new Short((short)0x1c09)); //$NON-NLS-1$
             ht.put(new String("es_DO"), new Short((short)0x1c0a)); //$NON-NLS-1$
             ht.put(new String("ar_OM"), new Short((short)0x2001)); //$NON-NLS-1$
             ht.put(new String("en_JM"), new Short((short)0x2009)); //$NON-NLS-1$
             ht.put(new String("es_VE"), new Short((short)0x200a)); //$NON-NLS-1$
             ht.put(new String("ar_YE"), new Short((short)0x2401)); //$NON-NLS-1$
             ht.put(new String("en_CB"), new Short((short)0x2409)); //$NON-NLS-1$
             ht.put(new String("es_CO"), new Short((short)0x240a)); //$NON-NLS-1$
             ht.put(new String("ar_SY"), new Short((short)0x2801)); //$NON-NLS-1$
             ht.put(new String("en_BZ"), new Short((short)0x2809)); //$NON-NLS-1$
             ht.put(new String("es_PE"), new Short((short)0x280a)); //$NON-NLS-1$
             ht.put(new String("ar_JO"), new Short((short)0x2c01)); //$NON-NLS-1$
             ht.put(new String("en_TT"), new Short((short)0x2c09)); //$NON-NLS-1$
             ht.put(new String("es_AR"), new Short((short)0x2c0a)); //$NON-NLS-1$
             ht.put(new String("ar_LB"), new Short((short)0x3001)); //$NON-NLS-1$
             ht.put(new String("en_ZW"), new Short((short)0x3009)); //$NON-NLS-1$
             ht.put(new String("es_EC"), new Short((short)0x300a)); //$NON-NLS-1$
             ht.put(new String("ar_KW"), new Short((short)0x3401)); //$NON-NLS-1$
             ht.put(new String("en_PH"), new Short((short)0x3409)); //$NON-NLS-1$
             ht.put(new String("es_CL"), new Short((short)0x340a)); //$NON-NLS-1$
             ht.put(new String("ar_AE"), new Short((short)0x3801)); //$NON-NLS-1$
             ht.put(new String("es_UY"), new Short((short)0x380a)); //$NON-NLS-1$
             ht.put(new String("ar_BH"), new Short((short)0x3c01)); //$NON-NLS-1$
             ht.put(new String("es_PY"), new Short((short)0x3c0a)); //$NON-NLS-1$
             ht.put(new String("ar_QA"), new Short((short)0x4001)); //$NON-NLS-1$
             ht.put(new String("es_BO"), new Short((short)0x400a)); //$NON-NLS-1$
             ht.put(new String("es_SV"), new Short((short)0x440a)); //$NON-NLS-1$
             ht.put(new String("es_HN"), new Short((short)0x480a)); //$NON-NLS-1$
             ht.put(new String("es_NI"), new Short((short)0x4c0a)); //$NON-NLS-1$
             ht.put(new String("es_PR"), new Short((short)0x500a)); //$NON-NLS-1$
    }

    /**
     * List of font faces names of system fonts supported by a system.
     */
    public static String[] faces;
    
    /**
     * List of font style names of system fonts supported by a system 
     * corresponding to faces indexing.
     */
    public static String[] styleNames;
    
    /**
     * List of family indexes in families array corresponding to the faces 
     * indexing.
     */
    public static int[] famIndices;

    /**
     * List of font styles of system fonts supported by a system 
     * corresponding to faces indexing.
     */
    public static int[] fontStyles;

    /**
     * The number of different fonts installed onto the system.
     */
    public static int facesCount;
    
    /**
     * Set of all unique families installed onto the system.
     */
    public static Vector fams = new Vector();

    /**
     * Returns family name that corresponds to the face with specified 
     * face index.
     * 
     * @param faceIndex index of the face in faces array
     */
    public static String getFamilyFromFaceIndex(int faceIndex){
        return (String)fams.get(famIndices[faceIndex]);
    }

    /**
     * Returns font style name of the font with face having specified index.
     * 
     * @param faceIndex specified index of the face in faces array
     */
    public static String getFontStyleName(int faceIndex){
        return styleNames[faceIndex];
    }

    /**
     * Returns font style of the font with face having specified index.
     * 
     * @param faceIndex specified index of the face in faces array
     */
    public static int getFontStyle(int faceIndex){
        return fontStyles[faceIndex];
    }

    /**
     * Returns array of Strings that represent face names of all fonts
     * supported by a system. 
     */
    public static String[] getFaces(){
        if (faces == null)
            initFaces();
        return faces;
    }

    /**
     * Initializes famIndices, styles, style names and faces arrays 
     * according to the font information available on the system. 
     */
    public static void initFaces(){
        if (facesCount == 0){
            String[] fontNames = getFonts();
            facesCount = fontNames.length;
            faces = new String[facesCount];
            styleNames = new String[facesCount];
            famIndices = new int[facesCount];
            fontStyles = new int[facesCount];

            for (int i =0; i < facesCount; i++){
                initFace(i, fontNames[i]);
            }
        }
    }

    /**
     * Initializes specified elements with index specified of famIndices, styles and 
     * faces arrays according to the given faceString. faceString has format 
     * "family name"-"style name"-style.
     * 
     * @param index index of element to identify
     * @param faceString String defining family name, style name and style in 
     * special format
     */
    public static void initFace(int index, String faceString){
        String delim = "-"; //$NON-NLS-1$
        int pos;
        if (faceString == null) {
            return;
        }

        String str = faceString;
        pos = str.lastIndexOf(delim);

        // get style value
        int style = Integer.parseInt(str.substring(pos+1));
        str = str.substring(0, pos);

        pos = str.lastIndexOf(delim);

        // get family name
        String family = str.substring(0, pos);
        int famIndex = fams.indexOf(family);
        if(famIndex == -1){
            fams.add(family);
            famIndex = fams.size() - 1;
        }
        famIndices[index] = famIndex;

        styleNames[index] = str.substring(pos+1);
        
        fontStyles[index] = style;

        faces[index] = family + " " + styleNames[index]; //$NON-NLS-1$
    }

    /** Returns the list of system font families names. */
    public static String[] getFamilies() {
        initFaces();

        int size = fams.size();
        String[] names = new String[size];
        for(int i=0; i < size; i++){
            names[i] = (String)fams.get(i);
        }
        return names;
    }

    /**
     * Returns an array of instances of 1 pt. sized plain Font objects
     * corresponding to fonts supported by a system. 
     */
    public static Font[] getAllFonts() {
        initFaces();

        Font[] fonts = new Font[faces.length];
        for (int i =0; i < fonts.length;i++){
            fonts[i] = new Font(faces[i], Font.PLAIN, 1);
        }
        return fonts;
    }

    /**
     * Adds new plain font with 1 pt. size from font resource file to the 
     * system if similar font wasn't into the system before. Method returns 
     * font object, corresponding to the specified resource. 
     *  
     * @param absolutePath absolute path to the font resource file
     */
    public static Font embedFont(String absolutePath) throws IOException {
        String familyName = embedFontNative(absolutePath);
        if (familyName == null)
            throw new IOException(Messages.getString("awt.299"));

        return new Font(familyName, Font.PLAIN, 1);
    }

    /** flag, returns true if native linuxfont was loaded */
    private static boolean isLibLoaded = false;

    static void loadLibrary() {
        if (!isLibLoaded) {
            java.security.AccessController
                    .doPrivileged(new java.security.PrivilegedAction() {
                        public Object run() {
                            org.apache.harmony.awt.Utils.loadLibrary("linuxfont"); //$NON-NLS-1$
                            return null;
                        }
                    });
            isLibLoaded = true;
        }
    }

    /* load native Font library */
    static {
        loadLibrary();
    }

}

