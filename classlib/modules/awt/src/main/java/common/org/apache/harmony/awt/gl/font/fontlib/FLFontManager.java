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
package org.apache.harmony.awt.gl.font.fontlib;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.peer.FontPeer;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.harmony.awt.gl.font.CompositeFont;
import org.apache.harmony.awt.gl.font.FontManager;
import org.apache.harmony.awt.gl.font.FontPeerImpl;
import org.apache.harmony.awt.internal.nls.Messages;

public class FLFontManager extends FontManager {
    
    private List<Font> allFonts = new ArrayList<Font>(); 
    
    static {
        org.apache.harmony.awt.Utils.loadLibrary("FL");
    }

    public FLFontManager() {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                Runtime.getRuntime().addShutdownHook(new DisposeNativeHook());
                return null;
            }
        });

        initManager();
        
        addPath(new File(org.apache.harmony.awt.Utils.getSystemProperty("java.home") + "/lib/fonts/"));
        // TODO this doesn't seem very portable - perhaps we should check WINDIR
        addPath(new File("C:\\WINNT\\Fonts"));
        addPath(new File("/usr/X11R6/lib/X11/fonts/Type1/"));
        addPath(new File("/usr/X11R6/lib/X11/fonts/truetype/"));
        
        Font[] nativeFonts = getAllFontsNative();
        
        if (nativeFonts != null)
        for(int i = 0; i < nativeFonts.length; i ++) {
            allFonts.add(nativeFonts[i]);
        }
        
        allFamilies = getAllFamilies();
    }
    
    class FLFilenameFilter implements FilenameFilter {

        public boolean accept(File dir, String str) {     
            String suffix = str.substring(str.length() - 3).toLowerCase();
            return suffix.equals("pfb") || suffix.equals("pfa") || suffix.equals("ttf");
        }
        
    }
    
    FilenameFilter filter = new FLFilenameFilter();
    
    private void addPath(File path){
        if (!path.canRead() || !path.isDirectory()) return;
        
        String[] strMas = path.list(filter);
        
        String dir = path.getAbsolutePath();
        
        Font newFont;
        
        for (int i = 0; i < strMas.length; i++) {
            String str = strMas[i].substring(strMas[i].length() - 3).toLowerCase();
            newFont = addFont(dir + "/" + strMas[i], str.equals("ttf") ? Font.TRUETYPE_FONT : Font.TYPE1_FONT);
            
            if (newFont != null) {
                allFonts.add(newFont);
            }
        }
    }

    @Override
    public FontPeer createPhysicalFontPeer(String name, int style, int size) {
        FontPeerImpl peer = null;        
        
        if (isFontExistInList(name, style)){         
            try {
                peer = new FLFontPeer(name, style, size);
                
                peer.setFamily(name);
            } catch(NullPointerException e) {
                peer = new FLFontPeer(DEFAULT_NAME, style, size);
                
                peer.setFamily(DEFAULT_NAME);
            }
        } else {
            peer = new FLFontPeer(DEFAULT_NAME, style, size);
            
            peer.setFamily(DEFAULT_NAME);
        }
        
        return peer;
    }
    
    private boolean isFontExistInList(String name, int style) {
        for (Font font : allFonts) {
            if (font.getStyle() == style && font.getName().equals(name)) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public FontPeer createDefaultFont(int style, int size) {
        //return getFontPeer(DIALOG_NAME, style, size);
        return createPhysicalFontPeer(DEFAULT_NAME, style, size);
    }
    
    /**
     * Initializes LCID table
     */
    @Override
    public void initLCIDTable(){
        
        Hashtable<String, Short> ht = tableLCID;

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


    @Override
    public String[] getAllFamilies() {        
        if (allFamilies != null) {            
            return allFamilies;
        }
        if (allFonts == null) {
            return null;
        }        
        
        ArrayList<String> al = new ArrayList<String>();        
        String str;
        
        for (Font font : allFonts) {
            str = font.getName();
            if (!al.contains(str)) {
                al.add(str);
            }
        }
        
        allFamilies = new String[al.size()];
        al.toArray(allFamilies);
        
//        if (allFamilies != null)
//            for (int i = 0; i < allFamilies.length; i ++) {            
//                System.out.println(allFamilies[i]);
//            }
        
        return allFamilies;
    }

    @Override
    public Font[] getAllFonts() {        
        return allFonts.toArray(new Font[0]);        
    }
    
    public Font embedFont(String path, int type) throws FontFormatException, IOException {
        Font newFont = addFont(path, type);
        
        if (newFont == null) {
            if ((new File(path)).canRead()) {
                //awt.9B=Can't create font - bad font data
                throw new FontFormatException ( Messages.getString("awt.9B") ); //$NON-NLS-1$                        
            }
            throw new IOException();
        }
        
        allFonts.add(newFont);
        
        allFamilies = getAllFamilies();
        
        return newFont;
    }
    
    private native Font addFont(String path, int type);
    
    private native void initManager();
    
    private native Font[] getAllFontsNative();
    
    private native void dispose();    
    
    /**
     * Returns platform-dependent Font peer created from the specified 
     * Font object from the table with cached FontPeers instances.
     * 
     * Note, this method checks whether FontPeer with specified parameters 
     * exists in the table with cached FontPeers' instances. If there is no needed 
     * instance - it is created and cached.
     * 
     * @param fontName name of the font 
     * @param _fontStyle style of the font 
     * @param size font size
     * 
     * @return platform dependent FontPeer implementation created from 
     * the specified parameters
     */
    @Override
    public FontPeer getFontPeer(String fontName, int _fontStyle, int size) {
        
        //updateFontsTable();
        
        FontPeer peer = null;
        String key; 
        String name;
        int fontStyle = _fontStyle;
        
        int logicalIndex = getLogicalFaceIndex(fontName);
        
        if (logicalIndex != -1){
            name = getLogicalFaceFromFont(fontStyle, logicalIndex);
            fontStyle = getStyleFromLogicalFace(name);
            key = name.concat(String.valueOf(size));
        } else {
            name = fontName;
            key = name.concat(String.valueOf(fontStyle)).
                    concat(String.valueOf(size));
        }
        
        HashMapReference hmr = fontsTable.get(key);
        if (hmr != null) {
            peer = hmr.get();
        }

        if (peer == null) {
            peer = createFontPeer(name, fontStyle, size, logicalIndex);
            if (peer == null){
                peer = getFontPeer(DIALOG_NAME, fontStyle, size);
            } else if (logicalIndex == -1) {
                fontsTable.put(key, new HashMapReference(key, peer, queue));
            }
        }        
        
        return peer;
    }
    
    /**
     * Returns default font peer class with "Default" name that is usually 
     * used when font with specified font names and style doesn't exsist 
     * on a system. 
     * 
     * @param style style of the font
     * @param size size of the font
     */
    @Override
    public FontPeer getDefaultFont(int style, int size){
        
        FontPeer peer = null;
        String key = DEFAULT_NAME.concat(String.valueOf(style)).
                    concat(String.valueOf(size));
        
        HashMapReference hmr   = fontsTable.get(key);
        if (hmr != null) {
            peer = hmr.get();
        }

        if (peer == null) {
            peer = createDefaultFont(style, size);
            
            ((FontPeerImpl)peer).setFamily(DEFAULT_NAME);
            ((FontPeerImpl)peer).setPSName(DEFAULT_NAME);
            ((FontPeerImpl)peer).setFontName(DEFAULT_NAME);

            fontsTable.put(key, new HashMapReference(key, peer, queue));
        }

        return peer;
    }
    
    public void removeFontFromHash(FontPeerImpl font) {
        fontsTable.remove(font.getName().concat(String.valueOf(font.getStyle())).concat(String.valueOf(font.getSize())));        
    }
    
    /**
     * Freeing native resources. This hook is used to avoid 
     * sudden application exit and to free resources created in native code.
     */
    private class DisposeNativeHook extends Thread {

        @Override
        public void run() {            
            try{
                /* Disposing native font peer's resources */
                Enumeration<String> kEnum = fontsTable.keys();

                while(kEnum.hasMoreElements()){
                    Object key = kEnum.nextElement();
                    HashMapReference hmr = fontsTable.get(key);
                    FontPeerImpl delPeer = (FontPeerImpl)hmr.get();
                    
                    if ((delPeer != null) && (delPeer.getClass() != CompositeFont.class)){
                        // there's nothing to dispose in CompositeFont objects
                        
                        delPeer.dispose();
                    }
                }
                
                dispose();
                
            } catch (Throwable t){
                throw new RuntimeException(t);
            }
        }
    }
}
