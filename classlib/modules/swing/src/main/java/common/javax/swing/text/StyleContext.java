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
package javax.swing.text;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class StyleContext
    implements Serializable, AbstractDocument.AttributeContext {

    public class NamedStyle implements Style, Serializable {

        protected transient ChangeEvent changeEvent;
        protected EventListenerList listenerList = new EventListenerList();

        /**
         * This is an instance of AttributeSet. It contains all attributes
         * including style name and resolve parent. StyleContext is used
         * for serialization of it. Named style must use StyleContext's methods
         * for any mutation of it.
         */
        private transient AttributeSet attrs = StyleContext.this.getEmptySet();

        public NamedStyle() {
            this(null, null);
        }

        public NamedStyle(final String name, final Style parent) {
            setName(name);
            setResolveParent(parent);
        }

        public NamedStyle(final Style parent) {
            this(null, parent);
        }

        public void addAttribute(final Object attrName, final Object value) {
            attrs = StyleContext.this.addAttribute(attrs, attrName, value);
            fireStateChanged();
        }

        public void addAttributes(final AttributeSet as) {
            attrs = StyleContext.this.addAttributes(attrs, as);
            fireStateChanged();
        }

        public void addChangeListener(final ChangeListener l) {
            listenerList.add(ChangeListener.class, l);
        }

        public boolean containsAttribute(final Object attrName,
                                         final Object value) {
            return attrs.containsAttribute(attrName, value);
        }

        public boolean containsAttributes(final AttributeSet as) {
            return attrs.containsAttributes(as);
        }

        public AttributeSet copyAttributes() {
            NamedStyle copy = new NamedStyle();
            copy.attrs = attrs.copyAttributes();
            return copy;
        }

        public Object getAttribute(final Object attrName) {
            return attrs.getAttribute(attrName);
        }

        public int getAttributeCount() {
            return attrs.getAttributeCount();
        }

        public Enumeration<?> getAttributeNames() {
            return attrs.getAttributeNames();
        }

        public ChangeListener[] getChangeListeners() {
            return (ChangeListener[])listenerList.getListeners(
                    ChangeListener.class);
        }

        public <T extends EventListener> T[] getListeners(final Class<T> listenerType) {
            return listenerList.getListeners(listenerType);
        }

        public String getName() {
            if (attrs.isDefined(AttributeSet.NameAttribute)) {
                Object styleName =
                    attrs.getAttribute(AttributeSet.NameAttribute);
                return styleName != null ? styleName.toString() : null;
            }

            return null;
        }

        public AttributeSet getResolveParent() {
            return attrs.getResolveParent();
        }

        public boolean isDefined(final Object attrName) {
            return attrs.isDefined(attrName);
        }

        public boolean isEqual(final AttributeSet attr) {
            return attrs.isEqual(attr);
        }

        public void removeAttribute(final Object attrName) {
            attrs = StyleContext.this.removeAttribute(attrs, attrName);
            fireStateChanged();
        }

        public void removeAttributes(final AttributeSet as) {
            attrs = StyleContext.this.removeAttributes(this.attrs, as);
            fireStateChanged();
        }

        public void removeAttributes(final Enumeration<?> names) {
            attrs = StyleContext.this.removeAttributes(attrs, names);
            fireStateChanged();
        }

        public void removeChangeListener(final ChangeListener l) {
            listenerList.remove(ChangeListener.class, l);
        }

        public void setName(final String name) {
            if (name != null) {
                addAttribute(AttributeSet.NameAttribute, name);
            }
        }

        public void setResolveParent(final AttributeSet parent) {
            if (parent == null) {
                removeAttribute(AttributeSet.ResolveAttribute);
            } else {
                addAttribute(AttributeSet.ResolveAttribute, parent);
            }
        }

        public String toString() {
            String str = new String();
            str += "NamedStyle:" + getName() + " ";
            str += attrs.toString();
            return str;
        }

        protected void fireStateChanged() {
            ChangeListener[] changeListeners = getChangeListeners();

            if (changeEvent == null && changeListeners.length > 0) {
                changeEvent = new ChangeEvent(this);
            }
            for (int i = 0; i < changeListeners.length; i++) {
                changeListeners[i].stateChanged(changeEvent);
            }
        }

        private void readObject(final ObjectInputStream in)
            throws IOException, ClassNotFoundException {

            in.defaultReadObject();
            attrs = StyleContext.this.getEmptySet();
            StyleContext.readAttributeSet(in, this);
        }

        private void writeObject(final ObjectOutputStream out)
            throws IOException {

            out.defaultWriteObject();
            StyleContext.writeAttributeSet(out, this);
        }

    }

    public class SmallAttributeSet implements AttributeSet {

        private final Object[] attributes;

        public SmallAttributeSet(final AttributeSet attrs) {
            attributes = new Object[attrs.getAttributeCount() * 2];
            int i = 0;
            for (Enumeration e = attrs.getAttributeNames();
                 e.hasMoreElements();) {

                Object key = e.nextElement();
                attributes[i++] = key;
                attributes[i++] = attrs.getAttribute(key);
            }
        }

        public SmallAttributeSet(final Object[] attributes) {
            this.attributes = new Object[attributes.length];
            System.arraycopy(attributes, 0, this.attributes, 0,
                             attributes.length);
        }

        public Object clone() {
            return this;
        }

        public boolean containsAttribute(final Object key, final Object value) {
            Object attrValue = getAttribute(key);
            return attrValue != null && value.equals(attrValue);
        }

        public boolean containsAttributes(final AttributeSet attrs) {
            for (Enumeration e = attrs.getAttributeNames();
                 e.hasMoreElements();) {

                Object key = e.nextElement();
                if (!containsAttribute(key, attrs.getAttribute(key))) {
                    return false;
                }
            }
            return true;
        }

        public AttributeSet copyAttributes() {
            return this;
        }

        public boolean equals(final Object obj) {
            if (obj instanceof AttributeSet) {
                return isEqual((AttributeSet)obj);
            }
            return false;
        }

        public Object getAttribute(final Object key) {
            Object value = null;
            for (int i = 0; i < attributes.length && value == null; i += 2) {
                if (attributes[i].equals(key)) {
                    value = attributes[i + 1];
                }
            }

            if (value == null && !key.equals(AttributeSet.ResolveAttribute)) {
                AttributeSet parent = getResolveParent();
                if (parent != null) {
                    value = parent.getAttribute(key);
                }
            }

            return value;
        }

        public int getAttributeCount() {
            return attributes.length / 2;
        }

        public Enumeration<?> getAttributeNames() {
            return new Enumeration() {

                private int count = 0;

                public boolean hasMoreElements() {
                    return count < attributes.length;
                }

                public Object nextElement() {
                    if (count >= attributes.length) {
                        new NoSuchElementException(Messages.getString("swing.9A")); //$NON-NLS-1$
                    }

                    Object next = attributes[count++];
                    count++;
                    return next;
                }
            };
        }

        public AttributeSet getResolveParent() {
            return (AttributeSet)getAttribute(AttributeSet.ResolveAttribute);
        }

        public int hashCode() {
            // Xors name with value and sums all result together.
            // Hash codes of equal attribute sets must be equal.
            // So we must return hash codes that will be equal to
            // SimpleAttributeSet's hash codes. It means that we must calculate
            // hash code as it's calculated for hashtable.
            int hc = 0;
            for (int i = 0; i < attributes.length; i += 2) {
                hc += attributes[i].hashCode() ^ attributes[i + 1].hashCode();
            }
            return hc;
        }

        public boolean isDefined(final Object key) {
            for (int i = 0; i < attributes.length; i += 2) {
                if (attributes[i].equals(key)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isEqual(final AttributeSet attr) {
            if (attr.getAttributeCount() != getAttributeCount()) {
                return false;
            }
            return containsAttributes(attr);
        }

        public String toString() {
            String str = new String("{");
            for (int i = 0; i < attributes.length; i += 2) {
                str += attributes[i].toString() + "=";
                Object value = attributes[i + 1];
                if (value instanceof AttributeSet) {
                    str += "AttributeSet";
                } else {
                    str += value.toString();
                }
                str += ",";
            }
            return str += "}";
        }

    }

    /**
     * Class that stores not only a weak reference to the object,
     * but the key for the cache (in the HashMap), so that
     * the referent could be removed from the cache.
     */
    private class CacheReference extends WeakReference {
        /**
         * The key for cache.
         */
        private final Object key;

        /**
         * Creates a new weak reference with the key and value pair specified,
         * registering this reference in the queue specified.
         *
         * @param key the key in cache
         * @param value value in cache that corresponds to the key
         * @param queue the queue where the reference should be registered
         */
        public CacheReference(final Object key, final Object value,
                              final ReferenceQueue queue) {
            super(value, queue);
            this.key = key;
        }

        /**
         * Returns the key that corresponds to the referent (value)
         *
         * @return the key in cache
         */
        public Object getKey() {
            return key;
        }
    }

    public static final String DEFAULT_STYLE = "default";

    /**
     * Maximum number of attributes that stored as an array
     * (in SmallAttributeSet).
     */
    private static final int COMPRESSION_THRESHOLD = 9;
    /**
     * Default style context which is returned with getDefaultStyleContext.
     */
    private static final StyleContext defaultContext = new StyleContext();
    /**
     * Static keys registered with registerStaticKey.
     */
    private static Hashtable staticKeys;

    // Static initializer
    static {
        // Create static key store
        staticKeys = new Hashtable();

        // Key to be registered as static
        Object[] keys = {
            StyleConstants.Alignment,
            StyleConstants.Background,
            StyleConstants.BidiLevel,
            StyleConstants.Bold,
            StyleConstants.ComponentAttribute,
            StyleConstants.ComposedTextAttribute,
            StyleConstants.FirstLineIndent,
            StyleConstants.FontFamily,
            StyleConstants.FontSize,
            StyleConstants.Foreground,
            StyleConstants.IconAttribute,
            StyleConstants.Italic,
            StyleConstants.LeftIndent,
            StyleConstants.LineSpacing,
            StyleConstants.ModelAttribute,
            StyleConstants.NameAttribute,
            StyleConstants.Orientation,
            StyleConstants.ResolveAttribute,
            StyleConstants.RightIndent,
            StyleConstants.SpaceAbove,
            StyleConstants.SpaceBelow,
            StyleConstants.StrikeThrough,
            StyleConstants.Subscript,
            StyleConstants.Superscript,
            StyleConstants.Underline,
            StyleConstants.TabSet
        };

        for (int i = 0; i < keys.length; i++) {
            StyleContext.registerStaticAttributeKey(keys[i]);
        }
    }
    /**
     * Used temporary to construct new attribute sets in all methods
     * that modify sets.
     */
    private transient SimpleAttributeSet attrSet =
        new SimpleAttributeSet();

    private transient Map cache;
    private transient Map fontCache;
    private transient ReferenceQueue queue;

    private transient NamedStyle styles;

    public StyleContext() {
        initTransientFields();
        addStyle(DEFAULT_STYLE, null);
    }

    public synchronized AttributeSet addAttribute(final AttributeSet old,
                                                  final Object name,
                                                  final Object value) {
        attrSet.addAttributes(old);
        attrSet.addAttribute(name, value);

        AttributeSet result;
        if (attrSet.getAttributeCount() <= getCompressionThreshold()) {
            result = cacheAttributeSet(attrSet);
        } else {
            result = createLargeAttributeSet(attrSet);
        }

        attrSet.removeAll();

        return result;
    }

    public synchronized AttributeSet addAttributes(final AttributeSet old,
                                                   final AttributeSet add) {
        attrSet.addAttributes(old);
        attrSet.addAttributes(add);

        AttributeSet result;
        if (attrSet.getAttributeCount() <= getCompressionThreshold()) {
            result = cacheAttributeSet(attrSet);
        } else {
            result = createLargeAttributeSet(attrSet);
        }

        attrSet.removeAll();

        return result;
    }

    public void addChangeListener(final ChangeListener l) {
        styles.addChangeListener(l);
    }

    public Style addStyle(final String name, final Style parent) {
        NamedStyle style = new NamedStyle(name, parent);
        if (name != null) {
            styles.addAttribute(name, style);
        }
        return style;
    }

    public Color getBackground(final AttributeSet as) {
        return StyleConstants.getBackground(as);
    }

    public ChangeListener[] getChangeListeners() {
        return styles.getChangeListeners();
    }

    public AttributeSet getEmptySet() {
        return EmptyAttributeSet.EMPTY;
    }

    public Font getFont(final AttributeSet as) {
        final boolean bold   = StyleConstants.isBold(as);
        final boolean italic = StyleConstants.isItalic(as);
        int style = Font.PLAIN;
        if (bold && italic) {
            style = Font.BOLD | Font.ITALIC;
        } else if (bold) {
            style = Font.BOLD;
        } else if (italic) {
            style = Font.ITALIC;
        }

        return getFont(StyleConstants.getFontFamily(as), style,
                       StyleConstants.getFontSize(as));
    }

    /**
     * Checks if the specified font is cached, and returns the cached instance,
     * or creates a new font and caches it.
     * <p>
     * We use a string with font parameters as key in the font cache.
     */
    public Font getFont(final String family, final int style, final int size) {
        collectGarbageInCache();

        String key = family + "-" +  style + "-" + size;

        Reference cr   = (Reference)fontCache.get(key);
        Font      font = null;
        if (cr != null) {
            font = (Font)cr.get();
        }

        if (font == null) {
            font = new Font(family, style, size);
            fontCache.put(key, new CacheReference(key, font, queue));
        }

        return font;
    }

    public FontMetrics getFontMetrics(final Font font) {
        return Toolkit.getDefaultToolkit().getFontMetrics(font);
    }

    public Color getForeground(final AttributeSet as) {
        return StyleConstants.getForeground(as);
    }

    public Style getStyle(final String name) {
        return (Style)styles.getAttribute(name);
    }

    public Enumeration<?> getStyleNames() {
        return styles.getAttributeNames();
    }

    public void readAttributes(final ObjectInputStream ois,
                               final MutableAttributeSet mas)
        throws ClassNotFoundException, IOException {

        StyleContext.readAttributeSet(ois, mas);
    }

    /**
     * This methods just does nothing because we use
     * <code>WeakReference</code>s to remove unused attribute sets from cache.
     */
    public void reclaim(final AttributeSet a) {
    }

    public synchronized AttributeSet removeAttribute(final AttributeSet old,
                                                     final Object name) {
        attrSet.addAttributes(old);
        attrSet.removeAttribute(name);

        AttributeSet result;
        final int count = attrSet.getAttributeCount();
        if (count == 0) {
            result = EmptyAttributeSet.EMPTY;
        } else if (count <= getCompressionThreshold()) {
            result = cacheAttributeSet(attrSet);
        } else {
            result = createLargeAttributeSet(attrSet);
        }

        attrSet.removeAll();

        return result;
    }

    public synchronized AttributeSet removeAttributes(final AttributeSet old,
                                                      final AttributeSet rem) {
        attrSet.addAttributes(old);
        attrSet.removeAttributes(rem);

        AttributeSet result;
        final int count = attrSet.getAttributeCount();
        if (count == 0) {
            result = EmptyAttributeSet.EMPTY;
        } else if (count <= getCompressionThreshold()) {
            result = cacheAttributeSet(attrSet);
        } else {
            result = createLargeAttributeSet(attrSet);
        }

        attrSet.removeAll();

        return result;
    }

    public synchronized AttributeSet removeAttributes(final AttributeSet old,
                                                      final Enumeration<?> names) {
        attrSet.addAttributes(old);
        attrSet.removeAttributes(names);

        AttributeSet result;
        final int count = attrSet.getAttributeCount();
        if (count == 0) {
            result = EmptyAttributeSet.EMPTY;
        } else if (count <= getCompressionThreshold()) {
            result = cacheAttributeSet(attrSet);
        } else {
            result = createLargeAttributeSet(attrSet);
        }

        attrSet.removeAll();

        return result;
    }

    public void removeChangeListener(final ChangeListener l) {
        styles.removeChangeListener(l);
    }

    public void removeStyle(final String name) {
        styles.removeAttribute(name);
    }

    /**
     * Returns a string representing all cached attribute sets.
     */
    public String toString() {
        String         str = new String();
        Iterator       keys;
        Object         key;

        keys = cache.keySet().iterator();
        while (keys.hasNext()) {
            key = keys.next();

            // Key is instance of AttributeSet (ALWAYS TRUE), print it
            str += key.toString() + "\n";
        }

        return str;
    }

    public void writeAttributes(final ObjectOutputStream oos,
                                final AttributeSet as)
        throws IOException {

        StyleContext.writeAttributeSet(oos, as);
    }

    public static final StyleContext getDefaultStyleContext() {
        return StyleContext.defaultContext;
    }

    /**
     * Returns the object that registered this key by its corresponding
     * string representation.
     */
    public static Object getStaticAttribute(final Object key) {
        return staticKeys.get(key);
    }

    /**
     * Returns the string representing this key.
     * <p>
     * It is composed of class name, dot and string presentation of the
     * <code>key</code> (returned by <code>toString</code>).
     */
    public static Object getStaticAttributeKey(final Object key) {
        return key.getClass().getName() + "." + key.toString();
    }

    public static void readAttributeSet(final ObjectInputStream ois,
                                        final MutableAttributeSet mas)
        throws ClassNotFoundException, IOException {

        Object key;
        Object value;
        Object staticKey;
        int    count;

        key = ois.readObject();
        if (key instanceof Integer) {
            count = ((Integer)key).intValue();
        } else {
            throw new IOException(Messages.getString("swing.9B")); //$NON-NLS-1$
        }

        while (count-- > 0) {
            key = ois.readObject();

            staticKey = key instanceof String
                        ? StyleContext.getStaticAttribute(key)
                        : null;

            if (staticKey != null) {
                key = staticKey;
            }

            value = ois.readObject();

            mas.addAttribute(key, value);
        }
    }

    /**
     * Stores the <code>key</code> in the <code>staticKeys</code> hash table
     * with table key returned by <code>getStaticAttributeKey</code>
     *
     * @see StyleContext#getStaticAttributeKey(Object)
     */
    public static void registerStaticAttributeKey(final Object key) {
        staticKeys.put(StyleContext.getStaticAttributeKey(key), key);
    }

    public static void writeAttributeSet(final ObjectOutputStream oos,
                                         final AttributeSet as)
        throws IOException {

        Enumeration keys = as.getAttributeNames();
        Object      key;
        Object      value;
        Object      staticKey;

        oos.writeObject(new Integer(as.getAttributeCount()));

        while (keys.hasMoreElements()) {
            key = keys.nextElement();

            staticKey = StyleContext.getStaticAttributeKey(key);
            if (staticKeys.containsKey(staticKey)) {
                oos.writeObject(staticKey);
            } else {
                oos.writeObject(key);
            }

            value = as.getAttribute(key);
            oos.writeObject(value);
        }
    }

    protected MutableAttributeSet
        createLargeAttributeSet(final AttributeSet a) {

        return (MutableAttributeSet)a.copyAttributes();
    }

    protected SmallAttributeSet
        createSmallAttributeSet(final AttributeSet aSet) {

        return new SmallAttributeSet(aSet);
    }

    protected int getCompressionThreshold() {
        return COMPRESSION_THRESHOLD;
    }

    /**
     * Checks if the requested attribute set has already been cached, and
     * returns the cached instance. If not, creates a new SmallAttributeSet
     * instance using <code>createSmallAttributeSet</code> method and adds
     * it into the cache.
     * <p>
     * We use the attribute set itself as a key in the cache. WeakHashMap
     * automatically removes unused (unreferenced) objects from the cache.
     * <p>
     * Also removes garbage from the font cache.
     *
     * @param aSet an attribute set to cache
     * @return cached attribute set, or newly created one and put in the cache
     */
    private SmallAttributeSet
        cacheAttributeSet(final AttributeSet aSet) {

        collectGarbageInCache();

        Reference r = (Reference)cache.get(aSet);
        SmallAttributeSet as = null;
        if (r != null) {
            as = (SmallAttributeSet)r.get();
        }

        if (as == null) {
            as = createSmallAttributeSet(aSet);
            cache.put(as, new WeakReference(as));
        }

        return as;
    }

    /**
     * Removes objects stored in the font cache which were garbage collected.
     * <code>AttributeSet</code> objects are removed automatically by
     * <code>WeakHashMap</code>.
     */
    private void collectGarbageInCache() {
        CacheReference r;
        while ((r = (CacheReference)queue.poll()) != null) {
            fontCache.remove(r.getKey());
        }
    }

    private void initTransientFields() {
        cache     = new WeakHashMap();
        fontCache = new HashMap();
        queue     = new ReferenceQueue();
        attrSet   = new SimpleAttributeSet();
        styles    = new NamedStyle();
    }

    /**
     * Deserializes the object. The state of the StyleContext is fully restored,
     * even the cache of attributes.
     *
     * @param ois
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(final ObjectInputStream ois)
        throws IOException, ClassNotFoundException {

        ois.defaultReadObject();
        initTransientFields();
        StyleContext.readAttributeSet(ois, styles);
    }

    /**
     * Serializes the state of StyleContext. All the styles stored in the
     * context is also serialized (with their attributes).
     *
     * @param oos
     * @throws IOException
     */
    private void writeObject(final ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        StyleContext.writeAttributeSet(oos, styles);
    }

}


