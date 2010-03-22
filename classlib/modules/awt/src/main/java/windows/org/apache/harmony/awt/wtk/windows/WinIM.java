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
 * @author Dmitry A. Durnev
 */
package org.apache.harmony.awt.wtk.windows;

import java.awt.AWTException;
import java.awt.im.spi.InputMethod;
import java.util.HashMap;
import java.util.Locale;

import org.apache.harmony.awt.ContextStorage;
import org.apache.harmony.awt.im.IMManager;
import org.apache.harmony.awt.im.InputMethodContext;
import org.apache.harmony.awt.nativebridge.Int16Pointer;
import org.apache.harmony.awt.nativebridge.Int8Pointer;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.PointerPointer;
import org.apache.harmony.awt.nativebridge.windows.Win32;
import org.apache.harmony.awt.wtk.NativeIM;

import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;

/**
 * Windows-specific native input method
 * functionality
 */
public class WinIM extends NativeIM {
    
    private static final NativeBridge nb = NativeBridge.getInstance();
    private static final Win32 win32 = Win32.getInstance();
    private static final WinWindowFactory wwf = (WinWindowFactory) ContextStorage.getWindowFactory();
    private static final HashMap<Long, Locale> hkl2Locale = new HashMap<Long, Locale>();;
    private static final HashMap<Locale, Long> locale2HKL = new HashMap<Locale, Long>();
    private Locale curLocale; // last locale set by user
    private long hIMC; // private native input context handle
    private long defaultIMC;
    
    
    WinIM() {
        WinEventQueue.Task task = new WinEventQueue.Task() {
            @Override
            public void perform() {
                hIMC = win32.ImmCreateContext();
            }
        };
        wwf.eventQueue.performTask(task);
    }

    @Override
    public Locale[] getAvailableLocales() throws AWTException {
        int nBuff = win32.GetKeyboardLayoutList(0, null);
        PointerPointer buffPtr = nb.createPointerPointer(nBuff, false);
        nBuff = win32.GetKeyboardLayoutList(nBuff, buffPtr);
        for (int i = 0; i < nBuff; i++) {
            long hkl = buffPtr.getElementPointer(i).getAddress(0);
            hkl2Locale(hkl);
        }
        
        return locale2HKL.keySet().toArray(new Locale[0]);
    }
    
    @Override
    public boolean setLocale(final Locale locale) {
        if (getLocale().equals(locale)) {
            curLocale = locale;
            return true;
        }
        WinEventQueue.Task task = new WinEventQueue.Task () {
            @Override
            public void perform() {
                long hkl = locale2HKL(locale);
                int flags = 0;
                boolean res = (win32.ActivateKeyboardLayout(hkl, flags) != 0);
                returnValue = Boolean.valueOf(res);
            }
        };         
        wwf.eventQueue.performTask(task);
        boolean result = ((Boolean)task.returnValue).booleanValue();
        if (result) {
            curLocale = locale;
        }
        return result;
    }
    
    @Override
    public Locale getLocale() {
       
        WinEventQueue.Task task = new WinEventQueue.Task () {
            @Override
            public void perform() {
                returnValue =  new Long(win32.GetKeyboardLayout(0));                
            }
        };         
        wwf.eventQueue.performTask(task);
        Long hkl = (Long) task.returnValue;        
        return hkl2Locale(hkl.longValue());
    }
    
    private static int makeLCID(short langid, short sortid) {
        return ((sortid << 16) | langid);
        
    }
    
    private static String getLocaleInfo(int lcid, int lcType) {
        int size = 6;
        Int16Pointer lpLCData = nb.createInt16Pointer(size, false);
        size = win32.GetLocaleInfoW(lcid, lcType, lpLCData, size);
        return lpLCData.getString();
    }

    /**
     * convert LANGID(16 low-order bits of HKL) into Locale instance
     */
    private static Locale hkl2Locale(long hkl) {
        Long key = new Long(hkl);
        if (hkl2Locale.containsKey(key)) {
            return hkl2Locale.get(key);
        }
        short langid = (short) hkl;
        short sortid = WindowsDefs.SORT_DEFAULT;        
        int lcid = makeLCID(langid, sortid);        
        String country = getLocaleInfo(lcid, WindowsDefs.LOCALE_SISO3166CTRYNAME);
        String language = getLocaleInfo(lcid, WindowsDefs.LOCALE_SISO639LANGNAME);
        Locale locale = new Locale(language, country);
        hkl2Locale.put(key, locale);
        locale2HKL.put(locale, key);
        return locale;
    }
    
  /**
   * convert Locale instance to HKL
   * 
   * @param locale Locale to get HKL for
   * @return HKL identifier of the given locale
   */
    private static long locale2HKL(Locale locale) {
        // there's no native functionality to get
        // lcid from locale name
        // maybe have to call getAvailableLocales()
        // before to update map(?)
        Long hkl = locale2HKL.get(locale);
        if (hkl != null) {
            return hkl.longValue();
        }
        return 0l;
    }
    
    /**
     * Must create new instance of this IM for
     * every instance of input context
     */
    @Override
    public InputMethod createInputMethod() throws Exception {
        return new WinIM();
    }
    
    @Override
    public void activate() {
        // reassociate focused window with
        // default native input context
        // if IME was previously disabled
        WinEventQueue.Task task = new WinEventQueue.Task() {
            @Override
            public void perform() {
                final long hwnd = win32.GetFocus();
                if (hwnd == 0l) {
                    return;
                }
                long curIMC = win32.ImmGetContext(hwnd);
                if ((curIMC != 0) && isActiveClient()) {
                    //  close composition window
                    // opened by passive client
                    win32.ImmSetOpenStatus(curIMC, 0);
                }
                if (curIMC != hIMC) {                    
                    long res = win32.ImmAssociateContext(hwnd, hIMC);
                    
                    if (res != 0l) {
                        defaultIMC = res;
                    }
                    returnValue = new Long(res);
                  

                } else {
                    // have to change input context on every
                    // activation to be able to process IME
                    // messages without showing default composition
                    // window for active clients
                    
                    win32.ImmAssociateContext(hwnd, defaultIMC);
                }
                
                win32.ImmReleaseContext(hwnd, curIMC);
            }
            
        };
        wwf.eventQueue.performTask(task);

        if (curLocale != null)  {
            setLocale(curLocale);
        }
    }    
    
    /**
     * Is called before the IME generates the composition string
     *  as a result of a keystroke.
     * @param hwnd owner of the composition window
     * @return false if the default composition window
     * should be opened, true otherwise
     */
    static boolean onStartComposition(long hwnd) {
        long hIMC = win32.ImmGetContext(hwnd);
        boolean active = isActiveClient();
        
        if ((hIMC != 0l) && !active) {
            setDefaultCompositionWindow(hIMC);
        }
        win32.ImmReleaseContext(hwnd, hIMC);
        return active;
    }
    
    /**
     * Is called when IME composition string changes
     * @param hwnd window where composition occurs
     * @param idx specifies how the composition string changed
     * @return true if message was processed by IM and
     * no default processing is required
     */
    static boolean onComposition(long hwnd, long idx) {
        // TODO: convert composition string change event
        // to Java InputMethodEvent and dispatch it
        // to active client(or post it to the EventQueue?)
        long hIMC = win32.ImmGetContext(hwnd);        
        
        if (hIMC != 0l) {
            if ((idx & WindowsDefs.GCS_COMPATTR) != 0) {
                int size=0;
                size = win32.ImmGetCompositionStringW(hIMC, WindowsDefs.GCS_COMPATTR,
                                                      0l, size);
                size += 2; // 0-terminator

                Int8Pointer lpBuf = nb.createInt8Pointer(size, false);                
                win32.ImmGetCompositionStringW(hIMC, WindowsDefs.GCS_COMPATTR,
                                               lpBuf, size);
                processAttributes(size - 2, lpBuf);
            }
            if ((idx & WindowsDefs.GCS_COMPCLAUSE) != 0) {
            }
            if ((idx & WindowsDefs.GCS_COMPREADATTR) != 0) {
            }
            if ((idx & WindowsDefs.GCS_COMPREADCLAUSE) != 0) {
            }
            if ((idx & WindowsDefs.GCS_COMPREADSTR) != 0) {
            }
            if ((idx & WindowsDefs.GCS_COMPSTR) != 0) {
            }
            if ((idx & WindowsDefs.GCS_CURSORPOS) != 0) {
            }
            if ((idx & WindowsDefs.GCS_DELTASTART) != 0) {
            }
            if ((idx & WindowsDefs.GCS_RESULTCLAUSE) != 0) {
            }
            if ((idx & WindowsDefs.GCS_RESULTREADCLAUSE) != 0) {
            }
            if ((idx & WindowsDefs.GCS_RESULTREADSTR) != 0) {
            }
            if ((idx & WindowsDefs.GCS_RESULTSTR) != 0) {
            }
        }
        win32.ImmReleaseContext(hwnd, hIMC);
        return isActiveClient();
    }

    private static void processAttributes(int size, Int8Pointer lpBuf) {
        // TODO: convert windows IM attributes to 
        // AttributedCharacterIterator attributes
        for (int i=0; i < size; i++) {
            byte attr = lpBuf.get(i);
            String strAttr = ""; //$NON-NLS-1$
            switch(attr) {
            case WindowsDefs.ATTR_INPUT:
                strAttr = "INP"; //$NON-NLS-1$
                break;
            case WindowsDefs.ATTR_INPUT_ERROR:
                strAttr = "IE"; //$NON-NLS-1$
                break;
            case WindowsDefs.ATTR_TARGET_CONVERTED:
                strAttr = "T_CONV"; //$NON-NLS-1$
                break;
            case WindowsDefs.ATTR_CONVERTED:
                strAttr = "CONV"; //$NON-NLS-1$
                break;
            case WindowsDefs.ATTR_TARGET_NOTCONVERTED:
                strAttr = "T_NCONV"; //$NON-NLS-1$
                break;
            case WindowsDefs.ATTR_FIXEDCONVERTED:
                strAttr = "FIX_CONV"; //$NON-NLS-1$
                break;                        
            }
        }
    }

    
    /**
     *  sets IME composition window position/style
     *  for passive clients
     */
    private static int setDefaultCompositionWindow(long hIMC) {
        Win32.COMPOSITIONFORM form = win32.createCOMPOSITIONFORM(false);
        form.set_dwStyle(WindowsDefs.CFS_DEFAULT);
        return win32.ImmSetCompositionWindow(hIMC, form);
    }
    
    @Override
    public void removeNotify() {
        disableIME();
    }
    
    @Override
    public void dispose() {
        WinEventQueue.Task task = new WinEventQueue.Task() {
            @Override
            public void perform() {
                win32.ImmDestroyContext(hIMC);
            }
        };
        wwf.eventQueue.performTask(task);
    }
    
    /**
     * Disables native input method support for the
     * focused window
     */
    @Override
    public void disableIME() {        
        WinEventQueue.Task task = new WinEventQueue.Task () {
            @Override
            public void perform() {
                final long hwnd = win32.GetFocus();
                long curIMC = win32.ImmGetContext(hwnd); 
                if (curIMC != 0l) {
                    win32.ImmAssociateContext(hwnd, 0l);
                }
                win32.ImmReleaseContext(hwnd, curIMC);
                returnValue = new Long(hwnd);
            }
        
        };        
        wwf.eventQueue.performTask(task);
    }
    
    /**
     * Is called when user chooses the new input language via
     * native system interface, i. e. with the hotkey or
     * from the indicator on the system taskbar.
     */
    static void onInputLangChange(long lcid) {
        // remember the new locale as selected in the
        // input context of the focused component
        InputMethodContext imc = IMManager.getLastActiveIMC();
        if (imc == null) {
            return;
        }
        InputMethod im = imc.getInputMethod();
        if (im instanceof NativeIM) {
            im.setLocale(hkl2Locale(lcid));
        }

    }
    
    private static boolean isActiveClient() {
        InputMethodContext imc = IMManager.getLastActiveIMC();
        if ((imc == null) || (imc.getClient() == null)) {
            return false;
        }
        return (imc.getClient().getInputMethodRequests() != null);
    }

    /**
     * Is called when input context is activated.
     * @return false if IME default composition window
     * should be activated for input context, true
     * otherwise
     */
    static boolean onActivateContext(long hwnd) {
        boolean result = isActiveClient();
        return result;
    }
}
