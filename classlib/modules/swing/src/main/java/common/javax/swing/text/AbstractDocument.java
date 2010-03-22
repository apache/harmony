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
package javax.swing.text;

import java.awt.font.TextAttribute;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.text.Bidi;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.tree.TreeNode;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

import org.apache.harmony.awt.text.PropertyNames;
import org.apache.harmony.awt.text.TextUtils;
import org.apache.harmony.x.swing.StringConstants;

import org.apache.harmony.x.swing.internal.nls.Messages;


public abstract class AbstractDocument implements Document, Serializable {

    public abstract class AbstractElement
        implements Element, MutableAttributeSet, Serializable, TreeNode {

        private static final String INDENT_LEVEL = "  ";
        private static final int MAX_LEAF_CONTENT_LENGTH = 40;

        private transient AttributeSet attrs;

        private final Element parent;

        public AbstractElement(final Element parent,
                               final AttributeSet attributes) {
            this.parent = parent;

            this.attrs  = context.getEmptySet();

            if (attributes != null) {
                addAttributes(attributes);
            }
        }

        public abstract boolean getAllowsChildren();

        public abstract boolean isLeaf();

        public abstract Enumeration children();

        public abstract Element getElement(final int index);

        public abstract int getElementCount();

        public abstract int getElementIndex(final int offset);

        public abstract int getStartOffset();

        public abstract int getEndOffset();

        public void addAttribute(final Object key, final Object value) {
            checkWriteLock();

            attrs = context.addAttribute(attrs, key, value);
        }

        public void addAttributes(final AttributeSet attrSet) {
            checkWriteLock();

            attrs = context.addAttributes(attrs, attrSet);
        }

        public boolean containsAttribute(final Object key, final Object value) {
            return attrs.containsAttribute(key, value);
        }

        public boolean containsAttributes(final AttributeSet attrSet) {
            return attrs.containsAttributes(attrSet);
        }

        public AttributeSet copyAttributes() {
            return attrs.copyAttributes();
        }

        /**
         * Prints the element name and lists all attributes.
         */
        public void dump(final PrintStream ps, final int indent) {
            final String name = getName();

            String indentation = "";
            for (int i = indent; i > 0; i--) {
                indentation += INDENT_LEVEL;
            }

            if (getAttributeCount() == 0) {
                ps.println(indentation + "<" + name + ">");
                indentation += INDENT_LEVEL;
            } else {
                ps.println(indentation + "<" + name);
                final String closing = indentation + ">";

                indentation += INDENT_LEVEL;

                Enumeration keys = getAttributeNames();
                while (keys.hasMoreElements()) {
                    Object key   = keys.nextElement();
                    Object value = getAttribute(key);
                    ps.println(indentation + key + "=" + value);
                }
                ps.println(closing);
            }

            if (isLeaf()) {
                try {
                    String text = content.getString(getStartOffset(),
                                                    getEndOffset()
                                                    - getStartOffset());
                    if (text.length() > MAX_LEAF_CONTENT_LENGTH) {
                        text = text.substring(0, MAX_LEAF_CONTENT_LENGTH);
                        text += "...";
                    }
                    ps.println(indentation
                               + "[" + getStartOffset() + ","
                               + getEndOffset() + "]["
                               + text + "]");
                } catch (final NullPointerException e) {
                } catch (final BadLocationException e) {
                }
            } else {
                Enumeration leaves = children();
                if (leaves == null) {
                    return;
                }

                while (leaves.hasMoreElements()) {
                    Object child = leaves.nextElement();
                    if (child instanceof AbstractElement) {
                        ((AbstractElement)child).dump(ps, indent + 1);
                    }
                }
            }
        }

        public Object getAttribute(final Object key) {
            Object value = attrs.getAttribute(key);
            AttributeSet resolver = getResolveParent();
            if (value == null && resolver != null) {
                value = resolver.getAttribute(key);
            }
            return value;
        }

        public int getAttributeCount() {
            return attrs.getAttributeCount();
        }

        public Enumeration<?> getAttributeNames() {
            return attrs.getAttributeNames();
        }

        public AttributeSet getAttributes() {
            return this;
        }

        public TreeNode getChildAt(final int index) {
            return (TreeNode)getElement(index);
        }

        public int getChildCount() {
            return getElementCount();
        }

        public Document getDocument() {
            return AbstractDocument.this;
        }

        public int getIndex(final TreeNode node) {
            if (getAllowsChildren()) {
                final int count = getChildCount();
                for (int i = 0; i < count; i++) {
                    if (getChildAt(i) == node) {
                        return i;
                    }
                }
            }

            return -1;
        }

        public String getName() {
            return isDefined(ElementNameAttribute)
            ? (String)getAttribute(ElementNameAttribute)
            : null;
        }

        public TreeNode getParent() {
            final Element parentElement = getParentElement();
            return parentElement instanceof TreeNode ? (TreeNode)parentElement
                                                     : null;
        }

        public Element getParentElement() {
            return parent;
        }

        public AttributeSet getResolveParent() {
            AttributeSet resolver = attrs.getResolveParent();
            if (resolver == null && parent != null) {
                resolver = parent.getAttributes();
            }
            return resolver;
        }

        public boolean isDefined(final Object key) {
            return attrs.isDefined(key);
        }

        public boolean isEqual(final AttributeSet attrSet) {
            return attrs.isEqual(attrSet);
        }

        public void removeAttribute(final Object key) {
            checkWriteLock();

            attrs = context.removeAttribute(attrs, key);
        }

        public void removeAttributes(final AttributeSet attrSet) {
            checkWriteLock();

            attrs = context.removeAttributes(attrs, attrSet);
        }

        public void removeAttributes(final Enumeration<?> attrNames) {
            checkWriteLock();

            attrs = context.removeAttributes(attrs, attrNames);
        }

        public void setResolveParent(final AttributeSet resolveParent) {
            checkWriteLock();

            attrs = context.addAttribute(attrs, AttributeSet.ResolveAttribute,
                                         resolveParent);
        }

        final void checkWriteLock() {
            if (AbstractDocument.this.getCurrentWriter()
                != Thread.currentThread()) {

                throw new Error(Messages.getString("swing.err.0F")); //$NON-NLS-1$
            }
        }

        private void readObject(final ObjectInputStream ois)
            throws IOException, ClassNotFoundException {

            ois.defaultReadObject();

            MutableAttributeSet mas = new SimpleAttributeSet();
            StyleContext.readAttributeSet(ois, mas);

            attrs = context.getEmptySet();
            if (mas.getAttributeCount() > 0) {
                writeLock();
                addAttributes(mas);
                writeUnlock();
            }
        }

        private void writeObject(final ObjectOutputStream oos)
            throws IOException {

            oos.defaultWriteObject();
            StyleContext.writeAttributeSet(oos, attrs);
        }

    }

    public static interface AttributeContext {

        AttributeSet addAttribute(AttributeSet old, Object key, Object value);

        AttributeSet addAttributes(AttributeSet old, AttributeSet toAdd);

        AttributeSet getEmptySet();

        void reclaim(AttributeSet attrs);

        AttributeSet removeAttribute(AttributeSet old, Object key);

        AttributeSet removeAttributes(AttributeSet old, AttributeSet toDelete);

        AttributeSet removeAttributes(AttributeSet old, Enumeration<?> names);

    }

    @SuppressWarnings("serial")
    public class BranchElement extends AbstractElement {
        Element[] elements =  new Element[0];

        private final SearchElement searchElem = new SearchElement();
        private final Comparator<Object> comparator = new ElementComparator();

        public BranchElement(final Element parent,
                             final AttributeSet attributes) {
            super(parent, attributes);
        }

        @Override
        public Enumeration children() {
            if (elements == null || elements.length == 0) {
                return null;
            } else {
                return new Enumeration() {
                    private int index = 0;

                    public boolean hasMoreElements() {
                        return index < elements.length;
                    }

                    public Object nextElement() {
                        return elements[index++];
                    }
                };
            }
        }

        @Override
        public boolean getAllowsChildren() {
            return true;
        }

        @Override
        public Element getElement(final int index) {
            if (0 <= index && index < elements.length) {
                return elements[index];
            } else {
                return null;
            }
        }

        @Override
        public int getElementCount() {
            return elements.length;
        }

        @Override
        public int getElementIndex(final int offset) {
            if (elements.length <= 1) {
                return 0;
            }

            searchElem.offset = offset;
            int result = Arrays.binarySearch(elements,
                                             searchElem,
                                             comparator);
            if (result < 0) {
                result = -result - 1;
            }
            if (result >= elements.length) {
                result = elements.length - 1;
            }

            return result;
        }

        @Override
        public int getEndOffset() {
            if (elements.length == 0) {
                throw new NullPointerException();
            }
            return elements[elements.length - 1].getEndOffset();
        }

        @Override
        public String getName() {
            final String inherited = super.getName();
            return inherited != null ? inherited : ParagraphElementName;
        }

        @Override
        public int getStartOffset() {
            if (elements.length == 0) {
                throw new NullPointerException();
            }
            return elements[0].getStartOffset();
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        public Element positionToElement(final int offset) {
            if (offset < getStartOffset() || offset > getEndOffset() - 1) {
                return null;
            }
            return elements[getElementIndex(offset)];
        }

        public void replace(final int index, final int length,
                            final Element[] newElements) {
            Element[] newArray;

            if (index < 0 || length < 0 || elements.length < index + length) {
                throw new ArrayIndexOutOfBoundsException(); 
            }

            newArray = new Element[
                    elements.length - length + newElements.length];

            System.arraycopy(elements, 0, newArray, 0, index);
            System.arraycopy(newElements, 0, newArray, index,
                             newElements.length);
            System.arraycopy(elements, index + length, newArray,
                             index + newElements.length,
                             elements.length - (index + length));

            elements = newArray;
        }

        @Override
        public String toString() {
            return "BranchElement(" + getName() + ") "
                    + getStartOffset() + "," + getEndOffset() + "\n";
        }

    }

    public static interface Content {

        Position createPosition(int offset) throws BadLocationException;

        void getChars(int offset, int length, Segment text)
            throws BadLocationException;

        String getString(int offset, int length)
            throws BadLocationException;

        UndoableEdit insertString(int offset, String str)
            throws BadLocationException;

        int length();

        UndoableEdit remove(int offset, int length)
            throws BadLocationException;

    }

    @SuppressWarnings("serial")
    public class DefaultDocumentEvent extends CompoundEdit
        implements DocumentEvent {

        private static final int THRESHOLD = 10;

        private HashMap<Element, ElementChange> changes;
        private int       length;
        private int       offset;
        private EventType type;

        public DefaultDocumentEvent(final int offset, final int length,
                                    final DocumentEvent.EventType type) {
            this.offset  = offset;
            this.length  = length;
            this.type    = type;
        }

        @Override
        public boolean addEdit(final UndoableEdit anEdit) {
            boolean result = super.addEdit(anEdit);

            if (result && edits.size() > THRESHOLD) {
                if (changes == null) {
                    changes = new HashMap<Element, ElementChange>();

                    for (UndoableEdit edit : edits) {
                        if (edit instanceof ElementChange) {
                            ElementChange change = (ElementChange)edit;
                            changes.put(change.getElement(), change);
                        }
                    }
                } else {
                    if (anEdit instanceof ElementChange) {
                        ElementChange change = (ElementChange)anEdit;
                        changes.put(change.getElement(), change);
                    }
                }
            }

            return result;
        }

        public ElementChange getChange(final Element element) {
            if (changes != null) {
                return changes.get(element);
            }

            for (UndoableEdit edit : edits) {
                if (edit instanceof ElementChange) {
                    ElementChange change = (ElementChange)edit;
                    if (change.getElement() == element) {
                        return change;
                    }
                }
            }
            return null;
        }

        public Document getDocument() {
            return AbstractDocument.this;
        }

        public int getLength() {
            return length;
        }

        public int getOffset() {
            return offset;
        }

        @Override
        public String getPresentationName() {
            if (type == EventType.INSERT) {
                return getLocalizedString("AbstractDocument.additionText");
            } else if (type == EventType.REMOVE) {
                return getLocalizedString("AbstractDocument.deletionText");
            } else if (type == EventType.CHANGE) {
                return getLocalizedString("AbstractDocument.styleChangeText");
            }

            assert false : "Valid values are DocumentEvent.EventType.INSERT, "
                            + "REMOVE, CHANGE";
            return null;
        }

        @Override
        public String getRedoPresentationName() {
            return getLocalizedRedoName() + " " + getPresentationName();
        }

        public EventType getType() {
            return type;
        }

        @Override
        public String getUndoPresentationName() {
            return getLocalizedUndoName() + " " + getPresentationName();
        }

        @Override
        public boolean isSignificant() {
            return true;
        }

        @Override
        public void redo() {
            writeLock();
            try {
                super.redo();

                fireEvent(false);
            } finally {
                writeUnlock();
            }
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            for (UndoableEdit edit : edits) {
                result.append(", ")
                      .append(edit.toString());
            }
            result.replace(0, 2, "[")
                  .append("]");
            return result.toString();
        }

        @Override
        public void undo() {
            writeLock();
            try {
                super.undo();

                fireEvent(true);
            } finally {
                writeUnlock();
            }
        }

        private void fireEvent(final boolean undone) {
            final EventType eventType = type;
            if (type == EventType.CHANGE) {
                fireChangedUpdate(this);
            } else {
                if (type == EventType.INSERT && undone) {
                    type = EventType.REMOVE;
                    fireRemoveUpdate(this);
                } else {
                    type = EventType.INSERT;
                    fireInsertUpdate(this);
                }
                type = eventType;
            }
        }

        private String getLocalizedUndoName() {
            return UIManager.getString("AbstractDocument.undoText");
        }

        private String getLocalizedRedoName() {
            return UIManager.getString("AbstractDocument.redoText");
        }
    }

    @SuppressWarnings("serial")
    public static class ElementEdit extends AbstractUndoableEdit
        implements ElementChange {

        private Element[] added;
        private Element   element;
        private int       index;
        private Element[] removed;

        public ElementEdit(final Element element, final int index,
                           final Element[] removed, final Element[] added) {
            this.element = element;
            this.index   = index;
            this.removed = removed;
            this.added   = added;
        }

        public Element[] getChildrenAdded() {
            return added;
        }

        public Element[] getChildrenRemoved() {
            return removed;
        }

        public Element getElement() {
            return element;
        }

        public int getIndex() {
            return index;
        }

        @Override
        public void redo() {
            super.redo();
            Element[] temp = added;
            added = removed;
            removed = temp;
            ((BranchElement)element).replace(index, removed.length, added);
        }

        @Override
        public void undo() {
            super.undo();
            ((BranchElement)element).replace(index, added.length, removed);
            Element[] temp = added;
            added = removed;
            removed = temp;
        }

    }

    @SuppressWarnings("serial")
    public class LeafElement extends AbstractElement {
        private transient Position end;
        private transient Position start;

        public LeafElement(final Element parent, final AttributeSet attributes,
                           final int startOffset, final int endOffset) {
            super(parent, attributes);

            int adjustedStartOffset = startOffset;
            int adjustedEndOffset   = endOffset;

            if (adjustedStartOffset < 0) {
                adjustedStartOffset = 0;
            }

            if (adjustedEndOffset < adjustedStartOffset) {
                adjustedEndOffset = adjustedStartOffset;
            }

            initPositions(adjustedStartOffset, adjustedEndOffset);
        }

        @Override
        public Enumeration children() {
            return null;
        }

        @Override
        public boolean getAllowsChildren() {
            return false;
        }

        @Override
        public Element getElement(final int offset) {
            return null;
        }

        @Override
        public int getElementCount() {
            return 0;
        }

        @Override
        public int getElementIndex(final int offset) {
            return -1;
        }

        @Override
        public int getEndOffset() {
            return end.getOffset();
        }

        @Override
        public String getName() {
            final String inherited = super.getName();
            return inherited != null ? inherited : ContentElementName;
        }

        @Override
        public int getStartOffset() {
            return start.getOffset();
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

        @Override
        public String toString() {
            return "LeafElement(" + getName() + ") " + getStartOffset()
                    + "," + getEndOffset() + "\n";
        }

        private void initPositions(final int startOffset, final int endOffset) {
            try {
                start = createPosition(startOffset);
                end   = createPosition(endOffset);
            } catch (final BadLocationException e) { }
        }

        private void readObject(final ObjectInputStream ois)
            throws IOException, ClassNotFoundException {

            ois.defaultReadObject();

            final int startOffset = ois.readInt();
            final int endOffset = ois.readInt();
            initPositions(startOffset, endOffset);
        }

        private void writeObject(final ObjectOutputStream oos)
            throws IOException {

            oos.defaultWriteObject();

            oos.writeInt(start.getOffset());
            oos.writeInt(end.getOffset());
        }

    }

    @SuppressWarnings("serial")
    private class BidiElement extends LeafElement {
        public BidiElement(final AttributeSet attributes,
                           final int startOffset,
                           final int endOffset) {
            super(getBidiRootElement(), attributes, startOffset, endOffset);
        }

        @Override
        public String getName() {
            return BidiElementName;
        }
    }

    @SuppressWarnings("serial")
    private class BidiRoot extends BranchElement {
        public BidiRoot() {
            super(null, null);
        }

        @Override
        public String getName() {
            return "bidi root";
        }
    }

    private static class ReadWriteLock {
        private boolean            callingListeners;
        private int                writeAcquestCounter;
        private int                waitingWritersCounter;
        private Thread             activeWriter;
        private final List<Thread> activeReaders = new ArrayList<Thread>();
        private final Object       lock          = new Object();

        public final Thread getCurrentWriter() {
            return activeWriter;
        }

        public final void readLock() {
            final Thread thread = Thread.currentThread();

            if (thread == activeWriter) {
                return;
            }

            synchronized (lock) {
                while ((activeWriter != null) || (waitingWritersCounter > 0)) {
                    try {
                        lock.wait();
                    } catch (final InterruptedException e) {
                        return;
                    }
                }

                activeReaders.add(thread);
            }
        }

        public final void readUnlock() {
            final Thread thread = Thread.currentThread();

            if (thread == activeWriter) {
                return;
            }

            synchronized (lock) {
                if (!activeReaders.remove(thread)) {
                    throw new Error(Messages.getString("swing.err.10")); //$NON-NLS-1$
                }

                lock.notifyAll();
            }
        }

        public final void setCallingListeners(final boolean flag) {
            callingListeners = flag;
        }

        public final void writeLock() {
            if (callingListeners) {
                throw new IllegalStateException(Messages.getString("swing.7E")); //$NON-NLS-1$
            }

            final Thread thread = Thread.currentThread();

            if (thread == activeWriter) {
                writeAcquestCounter++;
                return;
            }

            synchronized (lock) {
                if ((activeReaders.size() > 0) || (activeWriter != null)) {

                    waitingWritersCounter++;

                    while ((activeReaders.size() > 0) || (activeWriter != null)) {
                        try {
                            lock.wait();
                        } catch (final InterruptedException e) {
                            waitingWritersCounter--;
                            return;
                        }
                    }

                    waitingWritersCounter--;
                }

                writeAcquestCounter++;
                activeWriter = thread;
            }
        }

        public final void writeUnlock() {
            if (activeWriter != Thread.currentThread()) {
                throw new Error(Messages.getString("swing.err.11")); //$NON-NLS-1$            }
            }

            if (--writeAcquestCounter == 0) {
                synchronized (lock) {
                    activeWriter = null;
                    lock.notifyAll();
                }
            }
        }
    }

    /**
     * Object of this class serves as key when searching for element index.
     * @see BranchElement#getElementIndex(int)
     */
    @SuppressWarnings("serial")
    private static final class SearchElement implements Serializable {
        /**
         * Indicates the offset of interest.
         */
        public transient int offset;

        @Override
        public String toString() {
            return "SearchElement(none) " + offset + ", " + offset + "\n";
        }
    }

    /**
     * The comparator to compare <code>Element</code>s when searching for
     * element index.
     * <p><em><strong>Note:</strong> This comparator imposes ordering that
     * is inconsistent with equals.</em> Most of <code>Element</code>s
     * does not override <code>equals</code>.
     * @see BranchElement#getElementIndex(int)
     */
    @SuppressWarnings("serial")
    private static final class ElementComparator
        implements Comparator<Object>, Serializable {

        /**
         * It is expected that the first argument is the <code>Element</code>
         * to compare with and the second argument is the key containing the
         * offset of interest.
         *
         * @param item element to compare with.
         *             Must be of type <code>Element</code>.
         * @param key the search key. Object of type <code>SearchElement</code>
         *            which contains the desired offset.
         * @return 0, -1 or +1
         */
        public int compare(final Object item, final Object key) {
            Element e = (Element)item;
            int offset = ((SearchElement)key).offset;
            if (e.getStartOffset() <= offset
                && offset < e.getEndOffset()) {

                return 0;
            } else if (e.getEndOffset() <= offset) {
                return -1;
            } else {
                return +1;
            }
        }
    }

    public static final String BidiElementName      = "bidi level";
    public static final String ContentElementName   = "content";
    public static final String ElementNameAttribute = "$ename";
    public static final String ParagraphElementName = "paragraph";
    public static final String SectionElementName   = "section";

    protected static final String BAD_LOCATION = "document location failure";

    protected transient EventListenerList listenerList;

    private transient BranchElement bidiRoot;
    private transient Content content;
    private transient AttributeContext context;
    private transient Position docEnd;
    private DocumentFilter docFilter;
    private Dictionary<Object, Object> docProperties = new Hashtable<Object, Object>();
    private transient Position docStart;
    private transient DocumentFilter.FilterBypass filterBypasser;

    private UIDefaults uiDefaults = null;

    private transient ReadWriteLock lock;

    protected AbstractDocument(final Content content) {
        this(content, StyleContext.getDefaultStyleContext());
    }

    protected AbstractDocument(final Content content,
                               final AttributeContext context) {

        this.content = content;
        this.context = context;

        putProperty(StringConstants.BIDI_PROPERTY, Boolean.FALSE);

        initTransientFields();

        writeLock();
        bidiRoot = new BidiRoot();
        bidiRoot.replace(0, 0, new Element[] {
            new BidiElement(context.addAttribute(context.getEmptySet(),
                    StyleConstants.BidiLevel, new Integer(0)),
                            docStart.getOffset(),
                            docEnd.getOffset())
        });
        writeUnlock();
    }

    public abstract Element getDefaultRootElement();

    public abstract Element getParagraphElement(final int offset);

    public void addDocumentListener(final DocumentListener listener) {
        listenerList.add(DocumentListener.class, listener);
    }

    public void addUndoableEditListener(final UndoableEditListener listerner) {
        listenerList.add(UndoableEditListener.class, listerner);
    }

    public synchronized Position createPosition(final int offset)
        throws BadLocationException {

        return content.createPosition(offset);
    }

    /**
     * Dumps the array returned by getRootElements.
     */
    public void dump(final PrintStream ps) {
        Element[] roots = getRootElements();

        for (Element root : roots) {
            if (root instanceof AbstractElement) {
                ((AbstractElement)root).dump(ps, 0);
            }
        }
    }

    public int getAsynchronousLoadPriority() {
        Object value = getProperty(PropertyNames.LOAD_PRIORITY);
        return value instanceof Integer ? ((Integer)value).intValue() : -1;
    }

    public Element getBidiRootElement() {
        return bidiRoot;
    }

    public DocumentFilter getDocumentFilter() {
        return docFilter;
    }

    public DocumentListener[] getDocumentListeners() {
        return listenerList.getListeners(DocumentListener.class);
    }

    public Dictionary<Object, Object> getDocumentProperties() {
        return docProperties;
    }

    public final Position getEndPosition() {
        return docEnd;
    }

    public int getLength() {
        return content.length() - 1;
    }

    public <T extends EventListener> T[] getListeners(final Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    public final Object getProperty(final Object key) {
        return getDocumentProperties().get(key);
    }

    public Element[] getRootElements() {
        return new Element[] {getDefaultRootElement(), getBidiRootElement()};
    }

    public final Position getStartPosition() {
        return docStart;
    }

    public String getText(final int offset, final int length)
        throws BadLocationException {

        return content.getString(offset, length);
    }

    public void getText(final int offset, final int length, final Segment text)
        throws BadLocationException {

        content.getChars(offset, length, text);
    }

    public UndoableEditListener[] getUndoableEditListeners() {
        return listenerList.getListeners(UndoableEditListener.class);
    }

    public void insertString(final int offset,
                             final String text,
                             final AttributeSet attrs)
        throws BadLocationException {

        if (org.apache.harmony.x.swing.Utilities.isEmptyString(text)) {
            return;
        }

        writeLock();
        try {
            if (docFilter != null) {
                docFilter.insertString(filterBypasser, offset, text, attrs);
            } else {
                doInsert(offset, text, attrs);
            }
        } finally {
            writeUnlock();
        }
    }

    public final void putProperty(final Object key, final Object value) {
        if (value == null) {
            getDocumentProperties().remove(key);
        } else {
            getDocumentProperties().put(key, value);
        }
    }

    public final void readLock() {
        lock.readLock();
    }

    public final void readUnlock() {
        lock.readUnlock();
    }

    public void remove(final int offset, final int length)
        throws BadLocationException {

        writeLock();
        try {
            if (docFilter != null) {
                docFilter.remove(filterBypasser, offset, length);
            } else {
                doRemove(offset, length);
            }
        } finally {
            writeUnlock();
        }
    }

    public void removeDocumentListener(final DocumentListener listener) {
        listenerList.remove(DocumentListener.class, listener);
    }

    public void removeUndoableEditListener(final
                                           UndoableEditListener listener) {
        listenerList.remove(UndoableEditListener.class, listener);
    }

    public void render(final Runnable r) {
        readLock();
        try {
            r.run();
        } finally {
            readUnlock();
        }
    }

    public void replace(final int offset,
                        final int length,
                        final String newText,
                        final AttributeSet attrs)
        throws BadLocationException {

        writeLock();

        try {
            if (docFilter != null) {
                docFilter.replace(filterBypasser, offset, length, newText,
                                  attrs);
            } else {
                doReplace(offset, length, newText, attrs);
            }
        } finally {
            writeUnlock();
        }
    }

    public void setAsynchronousLoadPriority(final int priority) {
        putProperty(PropertyNames.LOAD_PRIORITY, new Integer(priority));
    }

    public void setDocumentFilter(final DocumentFilter filter) {
        this.docFilter = filter;
    }

    public void setDocumentProperties(final Dictionary<Object, Object> properties) {
        this.docProperties = properties;
    }

    protected Element createBranchElement(final Element parent,
                                          final AttributeSet as) {
        return new BranchElement(parent, as);
    }

    protected Element createLeafElement(final Element parent,
                                        final AttributeSet as,
                                        final int start,
                                        final int end) {
        return new LeafElement(parent, as, start, end);
    }

    protected void fireChangedUpdate(final DocumentEvent event) {
        lock.setCallingListeners(true);

        try {
            DocumentListener[] listeners = getDocumentListeners();
            for (DocumentListener listener : listeners) {
                listener.changedUpdate(event);
            }
        } finally {
            lock.setCallingListeners(false);
        }
    }

    protected void fireInsertUpdate(final DocumentEvent event) {
        lock.setCallingListeners(true);

        try {
            DocumentListener[] listeners = getDocumentListeners();
            for (DocumentListener listener : listeners) {
                listener.insertUpdate(event);
            }
        } finally {
            lock.setCallingListeners(false);
        }
    }

    protected void fireRemoveUpdate(final DocumentEvent event) {
        lock.setCallingListeners(true);

        try {
            DocumentListener[] listeners = getDocumentListeners();
            for (DocumentListener listener : listeners) {
                listener.removeUpdate(event);
            }
        } finally {
            lock.setCallingListeners(false);
        }
    }

    protected void fireUndoableEditUpdate(final UndoableEditEvent event) {
        lock.setCallingListeners(true);

        try {
            UndoableEditListener[] listeners = getUndoableEditListeners();
            for (UndoableEditListener listener : listeners) {
                listener.undoableEditHappened(event);
            }
        } finally {
            lock.setCallingListeners(false);
        }
    }

    protected final AttributeContext getAttributeContext() {
        return context;
    }

    protected final Content getContent() {
        return content;
    }

    protected final Thread getCurrentWriter() {
        return lock.getCurrentWriter();
    }

    protected void insertUpdate(final DefaultDocumentEvent event,
                                final AttributeSet attrs) {
        final int offset = event.getOffset();
        final int length = event.getLength();

        final Segment text = new Segment();

        try {
            content.getChars(offset, length, text);
        } catch (final BadLocationException e) { }

        if (!bidiUpdateProperty(offset, text)) {
            return;
        }

        final List<Element> added = new ArrayList<Element>();
        Element par;
        int nextOffset = offset;
        do {
            par = getParagraphElement(nextOffset);
            bidiParseParagraph(added, par, text);

            nextOffset = par.getEndOffset();
        } while (par.getEndOffset() < offset + length);

        bidiUpdateStructure(event, added);
    }

    protected void postRemoveUpdate(final DefaultDocumentEvent event) {
        if (!hasBidiInfo()) {
            return;
        }

        final List<Element> added = new ArrayList<Element>();

        bidiParseParagraph(added,
                           getParagraphElement(event.getOffset()),
                           new Segment());


        bidiUpdateStructure(event, added);
    }

    protected void removeUpdate(final DefaultDocumentEvent event) {
    }

    protected final void writeLock() {
        lock.writeLock();
    }

    protected final void writeUnlock() {
        lock.writeUnlock();
    }

    void doInsert(final int offset, final String text,
                  final AttributeSet attrs) throws BadLocationException {

        final DefaultDocumentEvent event =
            new DefaultDocumentEvent(offset, text.length(), EventType.INSERT);

        final UndoableEdit contentEdit = content.insertString(offset, text);
        if (contentEdit != null) {
            event.addEdit(contentEdit);
        }

        insertUpdate(event, attrs);

        event.end();

        fireInsertUpdate(event);
        if (contentEdit != null) {
            fireUndoableEditUpdate(new UndoableEditEvent(this, event));
        }
    }

    final void doRemove(final int offset, final int length)
        throws BadLocationException {

        if (length == 0) {
            return;
        }

        if (offset < 0 || offset > getLength()) {
            throw new BadLocationException(Messages.getString("swing.7F"), offset); //$NON-NLS-1$
        }

        if (offset + length > getLength()) {
            throw new BadLocationException(Messages.getString("swing.80"), //$NON-NLS-1$
                                           offset + length);
        }

        final DefaultDocumentEvent event =
            new DefaultDocumentEvent(offset, length, EventType.REMOVE);

        removeUpdate(event);

        final UndoableEdit contentEdit = content.remove(offset, length);
        if (contentEdit != null) {
            event.addEdit(contentEdit);
        }

        postRemoveUpdate(event);

        event.end();

        fireRemoveUpdate(event);
        if (contentEdit != null) {
            fireUndoableEditUpdate(new UndoableEditEvent(this, event));
        }
    }

    final void doReplace(final int offset, final int length,
                         final String newText, final AttributeSet attrs)
        throws BadLocationException {

        if (length > 0) {
            doRemove(offset, length);
        }

        if (!org.apache.harmony.x.swing.Utilities.isEmptyString(newText)) {
            doInsert(offset, newText, attrs);
        }
    }

    final boolean isLeftToRight(final int offset) {
        final Element e = bidiRoot.getElement(bidiRoot.getElementIndex(offset));
        return TextUtils.isLTR(getBidiLevel(e));
    }

    private int bidiAdjustFirstElement(final List<Element> added, final Element par) {
        int prevParIndex = bidiRoot.getElementIndex(par.getStartOffset() - 1);
        final Element prevParBidi = bidiRoot.getElement(prevParIndex);
        final Element firstAdded = added.get(0);

        if (getBidiLevel(prevParBidi) == getBidiLevel(firstAdded)) {
            // Combine these two
            added.remove(0);
            added.add(0,
                      new BidiElement(firstAdded.getAttributes(),
                                      prevParBidi.getStartOffset(),
                                      firstAdded.getEndOffset()));
        } else if (prevParBidi.getStartOffset() < firstAdded.getStartOffset()
                   && prevParBidi.getEndOffset()
                      > firstAdded.getStartOffset()) {
            // Divide prevParBidi
            added.add(0,
                      new BidiElement(prevParBidi.getAttributes(),
                                      prevParBidi.getStartOffset(),
                                      firstAdded.getStartOffset()));
        } else if (prevParBidi.getStartOffset()
                   != firstAdded.getStartOffset()) {
            // prevParBidi don't need to be deleted
            prevParIndex++;
        }

        return prevParIndex;
    }

    private int bidiAdjustLastElement(final List<Element> added) {
        final Element lastAdded = added.get(added.size() - 1);
        int nextParIndex  =
                bidiRoot.getElementIndex(lastAdded.getEndOffset() + 1);
        final Element nextParBidi = bidiRoot.getElement(nextParIndex);
        if (nextParBidi == null) {
            return nextParIndex;
        }

        if (getBidiLevel(nextParBidi) == getBidiLevel(lastAdded)) {
            // Combine these two
            added.remove(added.size() - 1);
            added.add(new BidiElement(lastAdded.getAttributes(),
                                      lastAdded.getStartOffset(),
                                      nextParBidi.getEndOffset()));
        } else if (lastAdded.getEndOffset() > nextParBidi.getStartOffset()
                   && lastAdded.getEndOffset() < nextParBidi.getEndOffset()) {
            // Divide nextParBidi
            added.add(new BidiElement(nextParBidi.getAttributes(),
                                      lastAdded.getEndOffset(),
                                      nextParBidi.getEndOffset()));
        } else if (nextParBidi.getEndOffset() != lastAdded.getEndOffset()) {
            // nextParBidi is left unchanged
                nextParIndex--;
        }

        return nextParIndex;
    }

    private void bidiParseParagraph(final List<Element> added, final Element par,
                                    final Segment text) {
        final int parStart = par.getStartOffset();
        final int parEnd   = par.getEndOffset();
        final int parLen   = parEnd - parStart;

        try {
            content.getChars(parStart, parLen, text);
        } catch (final BadLocationException e) { }

        Bidi bidi = new Bidi(text.array, text.offset, null, 0, text.count,
                             getDefaultDirection(par));

        final int runCount = bidi.getRunCount();
        for (int i = 0; i < runCount; i++) {
            int level = bidi.getRunLevel(i);

            if (i == 0 && added.size() > 0) {
                Element prevBidi = added.get(added.size() - 1);
                if (getBidiLevel(prevBidi) == level) {
                    added.remove(added.size() - 1);
                    added.add(new BidiElement(prevBidi.getAttributes(),
                                              prevBidi.getStartOffset(),
                                              parStart + bidi.getRunLimit(i)));
                    continue;
                }
            }

            added.add(
                new BidiElement(context.addAttribute(context.getEmptySet(),
                                                     StyleConstants.BidiLevel,
                                                     new Integer(level)),
                                parStart + bidi.getRunStart(i),
                                parStart + bidi.getRunLimit(i)));
        }

    }

    private boolean bidiUpdateProperty(final int offset, final Segment text) {
        final boolean hasBidiInfo = hasBidiInfo();

        if (!hasBidiInfo && TextUtils.isLTR(getDefaultDirection(offset))
            && !Bidi.requiresBidi(text.array, text.offset,
                                  text.offset + text.count)) {

            return false;
        }

        final Bidi bidi = new Bidi(text.array, text.offset, null, 0, text.count,
                                   getDefaultDirection(offset));

        if (hasBidiInfo && !bidi.isMixed()
            && isLeftToRight(offset) == bidi.isLeftToRight()) {

            return false;
        }

        if (!hasBidiInfo) {
            putProperty(StringConstants.BIDI_PROPERTY, Boolean.TRUE);
        }
        return true;
    }

    private void bidiUpdateStructure(final DefaultDocumentEvent event,
                                     final List<Element> added) {
        Element par = getParagraphElement(event.getOffset());

        int prevParIndex = bidiAdjustFirstElement(added, par);

        int nextParIndex  = bidiAdjustLastElement(added);
        if (nextParIndex == -1) {
            nextParIndex = bidiRoot.getElementCount() - 1;
        }

        int removedLength = nextParIndex - prevParIndex + 1;
        Element[] removed = new Element[removedLength];
        System.arraycopy(bidiRoot.elements, prevParIndex, removed, 0,
                         removedLength);

        Element[] addedElements = added.toArray(new Element[added.size()]);
        bidiRoot.replace(prevParIndex, removedLength, addedElements);

        ElementEdit edit = new ElementEdit(bidiRoot, prevParIndex, removed,
                                           addedElements);
        event.addEdit(edit);
    }

    private int getDefaultDirection(final Element par) {
        Object runDirection = null;

        if (par != null) {
            runDirection =
                par.getAttributes().getAttribute(TextAttribute.RUN_DIRECTION);

            if (runDirection != null) {
                return getDefaultDirection(runDirection);
            }
        }

        runDirection = getProperty(TextAttribute.RUN_DIRECTION);
        if (runDirection != null) {
            return getDefaultDirection(runDirection);
        } else {
            return Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT;
        }
    }

    private int getDefaultDirection(final int offset) {
        return getDefaultDirection(getParagraphElement(offset));
    }

    private boolean hasBidiInfo() {
        final Object docProperty = getProperty(StringConstants.BIDI_PROPERTY);
        return Boolean.TRUE.equals(docProperty);
    }

    private UIDefaults getUIDefaults() {
        if (uiDefaults == null) {
            uiDefaults = new UIDefaults();
        }

        return uiDefaults;
    }

    private String getLocalizedString(final String key) {
        Boolean isAWTDoc = (Boolean)getProperty(PropertyNames.AWT_DOCUMENT);
        if (isAWTDoc != null && isAWTDoc.booleanValue()) {
            return getUIDefaults().getString(key);
        } else {
            return UIManager.getString(key);
        }
    }

    private void initTransientFields() {
        try {
            docStart = content.createPosition(0);
            docEnd   = content.createPosition(content.length());
        } catch (final BadLocationException e) { }

        lock = new ReadWriteLock();

        filterBypasser = new DocumentFilter.FilterBypass() {

            @Override
            public Document getDocument() {
                return AbstractDocument.this;
            }

            @Override
            public void insertString(final int offset,
                                     final String text,
                                     final AttributeSet attrs)
                throws BadLocationException {

                doInsert(offset, text, attrs);
            }

            @Override
            public void remove(final int offset, final int length)
                throws BadLocationException {

                doRemove(offset, length);
            }

            @Override
            public void replace(final int offset,
                                final int length,
                                final String newText,
                                final AttributeSet attrs)
                throws BadLocationException {

                doReplace(offset, length, newText, attrs);
            }

        };

        listenerList = new EventListenerList();
    }


    private void readObject(final ObjectInputStream ois)
        throws IOException, ClassNotFoundException {

        ois.defaultReadObject();

        context = (AttributeContext)ois.readObject();
        content = (Content)ois.readObject();

        initTransientFields();

        bidiRoot = (BranchElement)ois.readObject();
    }

    private void writeObject(final ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();

        oos.writeObject(context);
        oos.writeObject(content);
        oos.writeObject(bidiRoot);
    }

    private static int getBidiLevel(final Element e) {
        return StyleConstants.getBidiLevel(e.getAttributes());
    }

    private static int getDefaultDirection(final Object runDirection) {
        return TextAttribute.RUN_DIRECTION_LTR.equals(runDirection)
               ? Bidi.DIRECTION_LEFT_TO_RIGHT
               : Bidi.DIRECTION_RIGHT_TO_LEFT;
    }

}
