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
package java.awt;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.harmony.awt.internal.nls.Messages;

public class AWTKeyStroke implements Serializable {
    private static final long serialVersionUID = -6430539691155161871L;

    private static final Map<AWTKeyStroke, AWTKeyStroke> cache = new HashMap<AWTKeyStroke, AWTKeyStroke>(); //Map<AWTKeyStroke, ? extends AWTKeyStroke>
    private static final Map<Integer, String> keyEventTypesMap = new HashMap<Integer, String>(); //Map<int, String>
    private static Constructor<?> subConstructor;
    
    static {
        keyEventTypesMap.put(new Integer(KeyEvent.KEY_PRESSED), "pressed"); //$NON-NLS-1$
        keyEventTypesMap.put(new Integer(KeyEvent.KEY_RELEASED), "released"); //$NON-NLS-1$
        keyEventTypesMap.put(new Integer(KeyEvent.KEY_TYPED), "typed"); //$NON-NLS-1$
    }

    private char keyChar;
    private int keyCode;
    private int modifiers;
    private boolean onKeyRelease;
    
    protected AWTKeyStroke(char keyChar, int keyCode, int modifiers,
            boolean onKeyRelease)
    {
       setAWTKeyStroke(keyChar, keyCode, modifiers, onKeyRelease);
    }

    private void setAWTKeyStroke( char keyChar, int keyCode, int modifiers,
            boolean onKeyRelease)
    {
        this.keyChar = keyChar;
        this.keyCode = keyCode;
        this.modifiers = modifiers;
        this.onKeyRelease = onKeyRelease;
    }
    protected AWTKeyStroke() {
        this(KeyEvent.CHAR_UNDEFINED, KeyEvent.VK_UNDEFINED, 0, false);
    }

    @Override
    public int hashCode() {
        return modifiers + ( keyCode != KeyEvent.VK_UNDEFINED ?
                keyCode : keyChar) + (onKeyRelease ? -1 : 0);
    }

    public final int getModifiers() {
        return modifiers;
    }

    @Override
    public final boolean equals(Object anObject) {
        if (anObject instanceof AWTKeyStroke) {
            AWTKeyStroke key = (AWTKeyStroke)anObject;
            return ((key.keyCode == keyCode) && (key.keyChar == keyChar) &&
                    (key.modifiers == modifiers) &&
                    (key.onKeyRelease == onKeyRelease));
        }
        return false;
    }

    @Override
    public String toString() {
        int type = getKeyEventType();
        return InputEvent.getModifiersExText(getModifiers()) + " " + //$NON-NLS-1$
            keyEventTypesMap.get(new Integer(type)) +  " " + //$NON-NLS-1$
            (type == KeyEvent.KEY_TYPED ? new String(new char[] {keyChar}) :
                                          KeyEvent.getKeyText(keyCode));
    }

    public final int getKeyCode() {
        return keyCode;
    }

    public final char getKeyChar() {
        return keyChar;
    }

    private static AWTKeyStroke getAWTKeyStroke(char keyChar, int keyCode,
                                                int modifiers,
                                                boolean onKeyRelease) {
        AWTKeyStroke key = newInstance(keyChar, keyCode, modifiers, onKeyRelease);

        AWTKeyStroke value = cache.get(key);
        if (value == null) {
            value = key;
            cache.put(key, value);
        }
        return value;
    }

    private static AWTKeyStroke newInstance(char keyChar, int keyCode,
                                            int modifiers,
                                            boolean onKeyRelease) {
        AWTKeyStroke key;
        if (subConstructor == null) {
            key = new AWTKeyStroke();
        } else {
            try {
                key = (AWTKeyStroke) subConstructor.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        int allModifiers = getAllModifiers(modifiers);
        key.setAWTKeyStroke(keyChar, keyCode, allModifiers, onKeyRelease);
        return key;
    }

    private static int addMask(int mod, int mask) {
        return ((mod & mask) != 0) ? (mod | mask) : mod;
    }

    /**
     * return all (old & new) modifiers corresponding to
     * @param mod old or new modifiers
     * @return old and new modifiers together
     */
    static int getAllModifiers(int mod) {
        int allMod = mod;
        int shift = (InputEvent.SHIFT_MASK | InputEvent.SHIFT_DOWN_MASK);
        int ctrl = (InputEvent.CTRL_MASK | InputEvent.CTRL_DOWN_MASK);
        int meta = (InputEvent.META_MASK | InputEvent.META_DOWN_MASK);
        int alt = (InputEvent.ALT_MASK | InputEvent.ALT_DOWN_MASK);
        int altGr = (InputEvent.ALT_GRAPH_MASK | InputEvent.ALT_GRAPH_DOWN_MASK);
        // button modifiers are not converted between old & new

        allMod = addMask(allMod, shift);
        allMod = addMask(allMod, ctrl);
        allMod = addMask(allMod, meta);
        allMod = addMask(allMod, alt);
        allMod = addMask(allMod, altGr);

        return allMod;
    }

    public static AWTKeyStroke getAWTKeyStroke(String s) {
        if (s == null) {
            // awt.65=null argument
            throw new IllegalArgumentException(Messages.getString("awt.65")); //$NON-NLS-1$
        }

        StringTokenizer tokenizer = new StringTokenizer(s);

        Boolean release = null;
        int modifiers = 0;
        int keyCode = KeyEvent.VK_UNDEFINED;
        char keyChar = KeyEvent.CHAR_UNDEFINED;
        boolean typed = false;
        long modifier = 0;
        String token = null;
        do {
            token = getNextToken(tokenizer);
            modifier = parseModifier(token);
            modifiers |= modifier;
        } while (modifier > 0);

        typed = parseTypedID(token);

        if (typed) {
            token = getNextToken(tokenizer);
            keyChar = parseTypedKey(token);

        }
        if (keyChar == KeyEvent.CHAR_UNDEFINED) {
            release = parsePressedReleasedID(token);
            if (release != null) {
                token = getNextToken(tokenizer);
            }
            keyCode = parseKey(token);
        }
        if (tokenizer.hasMoreTokens()) {
            // awt.66=Invalid format
            throw new IllegalArgumentException(Messages.getString("awt.66")); //$NON-NLS-1$
        }

        return getAWTKeyStroke(keyChar, keyCode, modifiers,
                               release == Boolean.TRUE);
    }

    private static String getNextToken(StringTokenizer tokenizer) {
        try {
            return tokenizer.nextToken();
        } catch (NoSuchElementException exception) {
            // awt.66=Invalid format
            throw new IllegalArgumentException(Messages.getString("awt.66")); //$NON-NLS-1$
        }
    }

    static int getKeyCode(String s) {
        try {
            Field vk = KeyEvent.class.getField("VK_" + s); //$NON-NLS-1$
            return vk.getInt(null);
        } catch (Exception e) {
            if (s.length() != 1) {
                // awt.66=Invalid format
                throw new IllegalArgumentException(Messages.getString("awt.66")); //$NON-NLS-1$
            }
            return KeyEvent.VK_UNDEFINED;
        }
    }

    public static AWTKeyStroke getAWTKeyStroke(char keyChar) {
        return getAWTKeyStroke(keyChar, KeyEvent.VK_UNDEFINED, 0, false);
    }

    public static AWTKeyStroke getAWTKeyStroke(int keyCode, int modifiers,
                                               boolean onKeyRelease) {
        return getAWTKeyStroke(KeyEvent.CHAR_UNDEFINED, keyCode, modifiers,
                               onKeyRelease);
    }

    public static AWTKeyStroke getAWTKeyStroke(Character keyChar, int modifiers) {
        if (keyChar == null) {
            // awt.01='{0}' parameter is null
            throw new IllegalArgumentException(Messages.getString("awt.01", "keyChar")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return getAWTKeyStroke(keyChar.charValue(), KeyEvent.VK_UNDEFINED,
                               modifiers, false);
    }

    public static AWTKeyStroke getAWTKeyStroke(int keyCode, int modifiers) {
        return getAWTKeyStroke(keyCode, modifiers, false);
    }

    public static AWTKeyStroke getAWTKeyStrokeForEvent(KeyEvent anEvent) {
        int id = anEvent.getID();
        char undef = KeyEvent.CHAR_UNDEFINED;
        char keyChar = (id == KeyEvent.KEY_TYPED ? anEvent.getKeyChar() :
                                                   undef);
        int keyCode = (keyChar == undef ? anEvent.getKeyCode() :
                                          KeyEvent.VK_UNDEFINED);
        return getAWTKeyStroke(keyChar, keyCode, anEvent.getModifiersEx(),
                               id == KeyEvent.KEY_RELEASED);
    }

    public final int getKeyEventType() {
        if (keyCode == KeyEvent.VK_UNDEFINED) {
            return KeyEvent.KEY_TYPED;
        }
        return (onKeyRelease ? KeyEvent.KEY_RELEASED : KeyEvent.KEY_PRESSED);
    }

    public final boolean isOnKeyRelease() {
        return onKeyRelease;
    }

    protected Object readResolve() throws ObjectStreamException {
        return getAWTKeyStroke(this.keyChar, this.keyCode,
                               this.modifiers, this.onKeyRelease);
    }

    protected static void registerSubclass(Class<?> subclass) {
        if (subclass == null) {
            // awt.01='{0}' parameter is null
            throw new IllegalArgumentException(Messages.getString("awt.01", "subclass")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (! AWTKeyStroke.class.isAssignableFrom(subclass)) {
            // awt.67=subclass is not derived from AWTKeyStroke
            throw new ClassCastException(Messages.getString("awt.67")); //$NON-NLS-1$
        }
        try {
            subConstructor = subclass.getDeclaredConstructor();
            subConstructor.setAccessible(true);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            // awt.68=subclass could not be instantiated
            throw new IllegalArgumentException(Messages.getString("awt.68")); //$NON-NLS-1$
        }
        cache.clear(); //flush the cache
    }

    private static long parseModifier(String strMod) {
        long modifiers = 0l;
        if (strMod.equals("shift")) { //$NON-NLS-1$
            modifiers |= InputEvent.SHIFT_DOWN_MASK;
        } else if (strMod.equals("control") || strMod.equals("ctrl")) { //$NON-NLS-1$ //$NON-NLS-2$
            modifiers |= InputEvent.CTRL_DOWN_MASK;
        } else if (strMod.equals("meta")) { //$NON-NLS-1$
            modifiers |= InputEvent.META_DOWN_MASK;
        } else if (strMod.equals("alt")) { //$NON-NLS-1$
            modifiers |= InputEvent.ALT_DOWN_MASK;
        } else if (strMod.equals("altGraph")) { //$NON-NLS-1$
            modifiers |= InputEvent.ALT_GRAPH_DOWN_MASK;
        } else if (strMod.equals("button1")) { //$NON-NLS-1$
            modifiers |= InputEvent.BUTTON1_DOWN_MASK;
        } else if (strMod.equals("button2")) { //$NON-NLS-1$
            modifiers |= InputEvent.BUTTON2_DOWN_MASK;
        } else if (strMod.equals("button3")) { //$NON-NLS-1$
            modifiers |= InputEvent.BUTTON3_DOWN_MASK;
        }
        return modifiers;
    }

    private static boolean parseTypedID(String strTyped) {
        if (strTyped.equals("typed")) { //$NON-NLS-1$
            return true;
        }

        return false;
    }

    private static char parseTypedKey(String strChar) {
        char keyChar = KeyEvent.CHAR_UNDEFINED;

        if (strChar.length() != 1) {
            // awt.66=Invalid format
            throw new IllegalArgumentException(Messages.getString("awt.66")); //$NON-NLS-1$
        }
        keyChar = strChar.charAt(0);
        return keyChar;
    }

    private static Boolean parsePressedReleasedID(String str) {

        if (str.equals("pressed")) { //$NON-NLS-1$
            return Boolean.FALSE;
        } else if (str.equals("released")) { //$NON-NLS-1$
            return Boolean.TRUE;
        }
        return null;
    }

    private static int parseKey(String strCode) {
        int keyCode = KeyEvent.VK_UNDEFINED;

        keyCode = getKeyCode(strCode);

        if (keyCode == KeyEvent.VK_UNDEFINED) {
            // awt.66=Invalid format
            throw new IllegalArgumentException(Messages.getString("awt.66")); //$NON-NLS-1$
        }
        return keyCode;
    }
}

