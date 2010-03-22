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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class DefaultStyledDocument extends AbstractDocument
    implements StyledDocument {

    public static class AttributeUndoableEdit extends AbstractUndoableEdit {
        protected AttributeSet copy;
        protected Element element;
        protected boolean isReplacing;
        protected AttributeSet newAttributes;

        public AttributeUndoableEdit(final Element element,
                                     final AttributeSet newAttributes,
                                     final boolean isReplacing) {
            this.element = element;
            this.newAttributes = newAttributes;
            this.isReplacing = isReplacing;
            this.copy = element.getAttributes().copyAttributes();
        }

        public void redo() {
            final AbstractElement elem = (AbstractElement)element;
            if (isReplacing) {
                elem.removeAttributes(elem);
            }
            elem.addAttributes(newAttributes);
        }

        public void undo() {
            final AbstractElement elem = (AbstractElement)element;
            elem.removeAttributes(newAttributes);
            elem.addAttributes(copy);
        }
    }

    public class ElementBuffer implements Serializable {
        private final Element root;
        private transient DefaultDocumentEvent event;
        private transient int offset;
        private transient int length;
        private transient ChangeDesc current;

        private transient Stack changeStack;
        private transient List changes;
        private transient boolean create;
        private transient Element tail;

        public ElementBuffer(final Element root) {
            this.root = root;
            initChangeLists();
        }

        public Element getRootElement() {
            return root;
        }

        public void insert(final int offset, final int length,
                           final ElementSpec[] spec,
                           final DefaultDocumentEvent event) {
            prepare(offset, length, event);

            insertUpdate(spec);

            collectEdits();
        }

        public void remove(final int offset, final int length,
                           final DefaultDocumentEvent event) {
            prepare(offset, length, event);

            removeUpdate();

            applyEdits();
            collectEdits();
        }

        public void change(final int offset, final int length,
                           final DefaultDocumentEvent event) {
            prepare(offset, length, event);

            changeUpdate();

            applyEdits();
            collectEdits();
        }

        public Element clone(final Element parent, final Element clonee) {
            if (clonee.isLeaf()) {
                return createLeafElement(parent,
                                         clonee.getAttributes(),
                                         clonee.getStartOffset(),
                                         clonee.getEndOffset());
            }
            BranchElement result =
                (BranchElement)createBranchElement(parent,
                                                   clonee.getAttributes());
            final int count = clonee.getElementCount();
            if (count > 0) {
                Element[] children = new Element[count];
                for (int i = 0; i < count; i++) {
                    children[i] = clone(result, clonee.getElement(i));
                }
                result.replace(0, 0, children);
            }
            return result;
        }

        protected void insertUpdate(final ElementSpec[] spec) {
            // Find the deepest branch
            Element branch = root;
            do {
                changeStack.push(new ChangeDesc(branch));
                branch = branch.getElement(branch.getElementIndex(offset));
            } while (!branch.isLeaf());

            current = (ChangeDesc)changeStack.peek();

            performSpecs(spec);
            leaveParagraph();
        }

        protected void removeUpdate() {
            final int endOffset = offset + length;
            final Element startLeaf = getCharacterElement(offset);
            final Element startBranch = startLeaf.getParentElement();

            final Element endLeaf = endOffset == startLeaf.getEndOffset()
                                    && endOffset < startBranch.getEndOffset()
                                    ? startLeaf
                                    : getCharacterElement(endOffset);
            final Element endBranch = endLeaf.getParentElement();

            if (startLeaf == endLeaf) {
                if (startLeaf.getStartOffset() == offset
                    && endOffset == startLeaf.getEndOffset()) {

                    current = new ChangeDesc(startBranch, offset);
                    current.removed.add(startLeaf);
                    changes.add(current);
                }
            } else if (startBranch == endBranch) {
                final int index = startBranch.getElementIndex(offset);
                current = new ChangeDesc(startBranch);
                for (int i = index; i < startBranch.getElementCount(); i++) {
                    final Element child = startBranch.getElement(i);
                    if (offset <= child.getStartOffset()
                        && child.getEndOffset() <= endOffset) {

                        current.setChildIndex(i);
                        current.removed.add(child);
                    }
                    if (endOffset < child.getEndOffset()) {
                        break;
                    }
                }

                changes.add(current);
            } else {
                final BranchElement parent =
                    (BranchElement)startBranch.getParentElement();
                if (parent != null) {
                    current = new ChangeDesc(parent, offset);
                    
                    BranchElement branch = (BranchElement)createBranchElement(parent,
                                                                startBranch.getAttributes());
                    List children = new LinkedList();
                    
                    // Copy elements from startBranch
                    int index = startBranch.getElementIndex(offset);
                    if (startBranch.getElement(index).getStartOffset() < offset) {
                        ++index;
                    }
                    for (int i = 0; i < index; i++) {
                        children.add(clone(branch, startBranch.getElement(i)));
                    }
                    
                    // Copy elements from endBranch
                    index = endBranch.getElementIndex(endOffset);
                    for (int i = index; i < endBranch.getElementCount(); i++) {
                        children.add(clone(branch, endBranch.getElement(i)));
                    }
                    
                    index = parent.getElementIndex(endOffset);
                    for (int i = current.getChildIndex(); i <= index; i++) {
                        current.removeChildElement(i);
                    }
                    current.added.add(branch);
                    
                    branch.replace(0, 0, listToElementArray(children));
                } else {
                    current = new ChangeDesc(startBranch, offset);
                    
                    // Copy elements from endBranch
                    int index = endBranch.getElementIndex(endOffset);
                    for (int i = index; i < endBranch.getElementCount(); i++) {
                        current.added.add(clone(startBranch, endBranch.getElement(i)));
                    }
                    
                    // Copy elements from startBranch
                    int startIndex = startBranch.getElementIndex(offset);
                    int endIndex = startBranch.getElementIndex(endOffset);
                    for (int i = startIndex; i <= endIndex; i++) {
                        current.removeChildElement(i);
                    }
                    current.setChildIndex(startIndex);
                    
                    current.apply();
                }

                changes.add(current);
            }
        }

        protected void changeUpdate() {
            final int endOffset = offset + length;
            final Element startLeaf = getCharacterElement(offset);
            final Element endLeaf = getCharacterElement(endOffset);

            if (startLeaf.getStartOffset() == offset
                && endOffset == startLeaf.getEndOffset()) {
                return;
            }

            if (startLeaf == endLeaf) {
                current = new ChangeDesc(startLeaf.getParentElement(), offset);
                current.splitLeafElement(startLeaf, offset, endOffset, true, startLeaf.getAttributes());

                changes.add(current);
            } else {
                // Break the startLeaf
                int start = startLeaf.getStartOffset();
                int end = startLeaf.getEndOffset();

                if (start < offset) {
                    current = new ChangeDesc(startLeaf.getParentElement(), offset);
                    current.splitLeafElement(startLeaf, offset);
                    changes.add(current);
                }

                // Break the endLeaf
                start = endLeaf.getStartOffset();
                end = endLeaf.getEndOffset();

                if (start < endOffset && endOffset < end) {
                    final boolean sameParents = current != null
                        && current.element == endLeaf.getParentElement();
                    if (!sameParents) {
                        current = new ChangeDesc(endLeaf.getParentElement(), endOffset);
                    } else {
                        final int endIndex = current.getChildIndexAtOffset(endOffset);
                        for (int i = current.getChildIndex() + 1;
                             i < endIndex; i++) {

                            final Element child = current.getChildElement(i);
                            current.removed.add(child);
                            current.added.add(child);
                        }
                    }

                    current.splitLeafElement(endLeaf, endOffset);

                    if (!sameParents) {
                        changes.add(current);
                    }
                }
            }
        }

        final void create(final ElementSpec[] specs,
                          final DefaultDocumentEvent event) {
            prepare(event.getOffset(), event.getLength(), event);
            create = true;

            // Remove all elements from the only paragraph
            current = new ChangeDesc(getParagraphElement(0));
            current.setChildIndex(0);
            current.createLeafElement(current.getChildElement(0).getAttributes(),
                                      length, length + 1);
            for (int i = 0; i < current.element.getElementCount(); i++) {
                current.removeChildElement(i);
            }
            current.apply();
            changes.add(current);
            current = null;

            performSpecs(specs);
            leaveParagraph();

            collectEdits();
        }

        private void performSpecs(final ElementSpec[] spec) throws Error {
            for (int i = 0; i < spec.length; i++) {
                switch (spec[i].getType()) {
                case ElementSpec.ContentType:
                    insertContent(spec[i]);
                    break;

                case ElementSpec.EndTagType:
                    insertEndTag();
                    break;

                case ElementSpec.StartTagType:
                    insertStartTag(spec[i]);
                    break;

                default:
                    throw new Error(Messages.getString("swing.err.12")); //$NON-NLS-1$
                }
            }
        }

        private void applyEdits() {
            for (int i = 0; i < changes.size(); i++) {
                final ChangeDesc desc = (ChangeDesc)changes.get(i);
                desc.apply();
            }
        }

        private void collectEdits() {
            while (!changeStack.empty()) {
                ChangeDesc desc = (ChangeDesc)changeStack.pop();
                if (!desc.isEmpty()) {
                    changes.add(desc);
                }
            }

            for (int i = 0; i < changes.size(); i++) {
                final ChangeDesc desc = (ChangeDesc)changes.get(i);
                if (!desc.isEmpty()) {
                    event.addEdit(desc.toElementEdit());
                }
            }
            changes.clear();

            clear();
        }

        private void clear() {
            event = null;
            current = null;
        }

        private void insertContent(final ElementSpec spec) {
            switch (spec.getDirection()) {
            case ElementSpec.OriginateDirection:
                insertContentOriginate(spec);
                break;

            case ElementSpec.JoinNextDirection:
                insertContentJoinNext(spec);
                break;

            case ElementSpec.JoinPreviousDirection:
                break;

            case ElementSpec.JoinFractureDirection:
                insertContentOriginate(spec);
                break;
            }
            offset += spec.getLength();
            length -= spec.getLength();
        }

        private void insertContentOriginate(final ElementSpec spec) {
            final AttributeSet specAttr = spec.getAttributes();
            if (current.element.getElementCount() == 0) {
                current.setChildIndex(0);
                current.createLeafElement(specAttr,
                                          offset, offset + spec.length);
            } else {
                current.setChildIndexByOffset(offset);
                final Element leafToRemove = current.getCurrentChild();
                if (offset == 0 && leafToRemove.isLeaf()) {
                    current.removed.add(leafToRemove);
                    current.createLeafElement(specAttr,
                                              offset, offset + spec.length);
                    current.createLeafElement(leafToRemove.getAttributes(),
                                              offset + length, leafToRemove.getEndOffset());
                    tail = current.getLastAddedElement();
                    current.added.remove(tail);
                } else if (offset == event.getOffset()
                        && leafToRemove.getStartOffset() < offset
                        && offset < leafToRemove.getEndOffset()) {
                    if (leafToRemove.isLeaf()) {
                        current.splitLeafElement(leafToRemove, offset,
                                                 offset + spec.length,
                                                 offset + length,
                                                 true, specAttr);
                        tail = current.getLastAddedElement();
                        current.added.remove(tail);
                    } else {
                        tail = splitBranch(leafToRemove);
                        current.createLeafElement(specAttr,
                                                  offset, offset + spec.length);
                        current.childIndex = current.getChildIndex() + 1;
                    }
                } else {
                    current.createLeafElement(specAttr,
                                              offset, offset + spec.length);
                    if (offset >= current.element.getEndOffset()
                        && current.getChildIndex() < current.element
                                .getElementCount()) {
                        current.childIndex = current.getChildIndex() + 1;
                    }
                }
            }
        }

        private void insertContentJoinNext(final ElementSpec spec) {
            current.setChildIndexByOffset(offset);
            final Element leaf = current.getCurrentChild();
            if (leaf.getStartOffset() >= offset) {
                current.removed.add(leaf);
                current.createLeafElement(leaf.getAttributes(),
                                          offset, leaf.getEndOffset());
            } else {
                final Element next =
                    current.getChildElement(current.getChildIndex() + 1);
                current.removed.add(leaf);
                current.removed.add(next);
                current.createLeafElement(leaf.getAttributes(),
                                          leaf.getStartOffset(), offset);
                current.createLeafElement(next.getAttributes(),
                                          offset, next.getEndOffset());
            }
        }

        private void insertStartTag(final ElementSpec spec) {
            switch (spec.getDirection()) {
            case ElementSpec.OriginateDirection:
                insertStartOriginate(spec);
                break;

            case ElementSpec.JoinNextDirection:
                insertStartJoinNext(spec);
                break;

            case ElementSpec.JoinPreviousDirection:
                insertStartJoinPrevious(spec);
                break;

            case ElementSpec.JoinFractureDirection:
                insertStartFracture(spec);
                break;

            default:
                throw new Error(Messages.getString("swing.err.13","ElementSpec")); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        private void insertStartFracture(final ElementSpec spec) {
            final AttributeSet attrs =
                spec.getDirection() == ElementSpec.OriginateDirection
                ? spec.getAttributes()
                : findLastExistedBranch().getAttributes();
            final BranchElement newBranch =
                (BranchElement)createBranchElement(current.element,
                                                   attrs);

            final ChangeDesc lastChange = (ChangeDesc)changes.get(changes.size() - 1);
            int startIndex = lastChange.getChildIndexAtOffset(offset);
            if (lastChange.getChildElement(startIndex).getEndOffset() <= offset) {
                ++startIndex;
            }
            moveChildren(newBranch, lastChange, startIndex);

            current.added.add(newBranch);
            if (current.getChildIndex() == -1) {
                int newIndex = current.getChildIndexAtOffset(offset);
                if (newBranch.getElementCount() > 0
                    && newBranch.getEndOffset()
                       > current.getChildElement(newIndex).getStartOffset()) {
                    ++newIndex;
                }
                current.setChildIndex(newIndex);
            }
            if (current.isApplied()) {
                int replaceIndex = current.getChildIndexAtOffset(offset);
                if (newBranch.getElementCount() > 0
                    && newBranch.getEndOffset()
                       > current.getChildElement(replaceIndex).getStartOffset()) {
                    ++replaceIndex;
                }
                current.element.replace(replaceIndex, 0,
                                        new Element[] {newBranch});
            } else {
                current.apply();
            }

            current = new ChangeDesc(newBranch, true);
            changeStack.push(current);
        }

        private void moveChildren(final BranchElement newParent,
                                  final ChangeDesc sourceDesc,
                                  final int startIndex) {
            // copy all elements from lastBranch to the new one
            final int count = sourceDesc.element.getElementCount();
            final Element[] children = new Element[count - startIndex];
            for (int i = startIndex; i < count; i++) {
                children[i - startIndex] = clone(newParent, sourceDesc.getChildElement(i));
            }
            // Now we need to remove all previously added elements which were
            // copied from added list
            final int i = startIndex - sourceDesc.getChildIndex();
            for (int j = startIndex; j < count; j++) {
                final Object addedElement = sourceDesc.getAddedElement(i);
                final Object existingElement = sourceDesc.getChildElement(j);
                if (addedElement == existingElement) {
                    sourceDesc.added.remove(addedElement);
                } else if (!sourceDesc.justCreated) {
                    sourceDesc.removed.add(existingElement);
                }
            }
            // Complete the removal of elements from source
            if (count - startIndex > 0) {
                sourceDesc.element.replace(startIndex, count - startIndex, new Element[0]);
            }

            // Place copied children into the new parent
            newParent.replace(0, 0, children);
        }

        private void insertStartOriginate(final ElementSpec spec) {
            if (current == null) {
                insertStartJoinPrevious(spec);
            } else if (!create && !changes.isEmpty()) {
                insertStartFracture(spec);
            } else {
                Element branch = createBranchElement(current.element,
                                                     spec.getAttributes());
                current.setChildIndexByOffset(offset);
                current.added.add(branch);
                current = new ChangeDesc(branch, true);
                changeStack.push(current);
            }
        }

        private void insertStartJoinNext(final ElementSpec spec) {
            current = new ChangeDesc(current.getChildAtOffset(offset));
            changeStack.push(current);
        }

        private void insertStartJoinPrevious(final ElementSpec spec) {
            if (current == null) {
                current = new ChangeDesc(getRootElement());
                // TODO are old attributes to be removed?
                final AttributeSet specAttr = spec.getAttributes();
                if (specAttr != null) {
                    ((AbstractElement)getRootElement()).addAttributes(specAttr);
                }
                changeStack.push(current);
            } else {
                current = new ChangeDesc(current.getChildAtOffset(offset));
                changeStack.push(current);
            }
        }

        private void insertEndTag() {
            if (current.isEmpty()) {
                current.setChildIndexByOffset(offset);
                Element leaf = current.getCurrentChild();
                final int start = leaf.getStartOffset();
                final int end = leaf.getEndOffset();
                if (start < offset && offset < end
                    || start < offset + length && offset + length < end) {

                    if (leaf.isLeaf()) {
                        current.splitLeafElement(leaf, offset, offset + length, false, null);
                    } else if (length != 0) {
                        BranchElement rightBranch = splitBranch(leaf);
                        current.added.add(rightBranch);
                        int newIndex = current.getChildIndexAtOffset(offset + length);
                        if (rightBranch.getElementCount() > 0
                                && rightBranch.getEndOffset()
                                > current.getChildElement(newIndex).getStartOffset()) {
                            ++newIndex;
                        }
                        current.childIndex = newIndex;
                    }
                }
            }
            leaveParagraph();
            changes.add(current);
            changeStack.pop();
            current = changeStack.empty() ? null : (ChangeDesc)changeStack.peek();
        }

        private BranchElement splitBranch(final Element branch) {
            BranchElement result = current.createBranchElement(branch.getAttributes());
            final ChangeDesc lastChange = (ChangeDesc)changes.get(changes.size() - 1);
            int startIndex = lastChange.getChildIndexAtOffset(offset + length);
            moveChildren(result, lastChange, startIndex);
            return result;
        }

        private BranchElement findLastExistedBranch() {
            int i = changes.size() - 1;
            ChangeDesc desc = null;
            while (i >= 0 && (desc = (ChangeDesc)changes.get(i)).justCreated) {
                i--;
            }
            return i >= 0 ? desc.element : null;
        }

        private void leaveParagraph() {
            if (current == null || current.isEmpty()) {
                return;
            }

            if (tail != null) {
                current.added.add(tail);
            }
            tail = null;
            current.apply();
        }

        private Element[] listToElementArray(final List list) {
            return (Element[])list.toArray(new Element[list.size()]);
        }

        private void initChangeLists() {
            changeStack = new Stack();
            changes = new ArrayList();
        }

        private void prepare(final int offset, final int length,
                          final DefaultDocumentEvent event) {
            this.offset = offset;
            this.length = length;
            this.event  = event;

            this.changes.clear();
            this.changeStack.clear();
            this.current = null;

            this.create = false;
            this.tail = null;
        }

        private void readObject(final ObjectInputStream ois)
            throws IOException, ClassNotFoundException {

            ois.defaultReadObject();
            initChangeLists();
        }

        private void writeObject(final ObjectOutputStream oos)
            throws IOException {

            oos.defaultWriteObject();
        }
    }

    public static class ElementSpec {
        public static final short ContentType = 3;
        public static final short EndTagType = 2;
        public static final short StartTagType = 1;

        public static final short JoinFractureDirection = 7;
        public static final short JoinNextDirection = 5;
        public static final short JoinPreviousDirection = 4;
        public static final short OriginateDirection = 6;

        private AttributeSet attrs;
        private short type;
        private char[] text;
        private int offset;
        private int length;
        private short direction;

        public ElementSpec(final AttributeSet attrs, final short type) {
            this(attrs, type, null, 0, 0);
        }

        public ElementSpec(final AttributeSet attrs,
                           final short type,
                           final char[] text,
                           final int offset,
                           final int length) {
            this.attrs  = attrs;
            this.type   = type;
            this.text   = text;
            this.offset = offset;
            this.length = length;

            this.direction = OriginateDirection;
        }

        public ElementSpec(final AttributeSet attrs,
                           final short type,
                           final int length) {
            this(attrs, type, null, 0, length);
        }


        public char[] getArray() {
            return text;
        }

        public AttributeSet getAttributes() {
            return attrs;
        }

        public short getDirection() {
            return direction;
        }

        public int getLength() {
            return length;
        }

        public int getOffset() {
            return offset;
        }

        public short getType() {
            return type;
        }

        public void setDirection(final short direction) {
            this.direction = direction;
        }

        public void setType(final short type) {
            this.type = type;
        }

        /*
         * The format of the string is based on 1.5 release behavior
         * which can be revealed using the following code:
         *
         *     Object obj = new DefaultStyledDocument.ElementSpec(null,
         *         DefaultStyledDocument.ElementSpec.ContentType);
         *     System.out.println(obj.toString());
         */
        public String toString() {
            String result;
            switch (type) {
            case StartTagType:
                result = "StartTag:";
                break;
            case ContentType:
                result = "Content:";
                break;
            case EndTagType:
                result = "EndTag:";
                break;
            default:
                result = "??:";
            }

            switch (direction) {
            case OriginateDirection:
                result += "Originate:";
                break;
            case JoinFractureDirection:
                result += "Fracture:";
                break;
            case JoinNextDirection:
                result += "JoinNext:";
                break;
            case JoinPreviousDirection:
                result += "JoinPrevious:";
                break;
            default:
                result += "??:";
            }

            result += length;

            return result;
        }

    }

    protected class SectionElement extends BranchElement {
        public SectionElement() {
            super(null, null);
        }

        public String getName() {
            return AbstractDocument.SectionElementName;
        }
    }

    private final class ChangeDesc {
        public final BranchElement element;
        private int childIndex = -1;
        public final List added = new ArrayList();
        public final List removed = new ArrayList();
        public final boolean justCreated;
        private boolean applied;

        public ChangeDesc(final Element element) {
            this(element, false);
        }

        public ChangeDesc(final Element element,
                          final boolean justCreated) {
            this.element = (BranchElement)element;
            this.justCreated = justCreated;
        }

        public ChangeDesc(final Element element,
                          final int offset) {
            this(element, false);
            setChildIndexByOffset(offset);
        }

        public void setChildIndex(final int index) {
            if (this.childIndex == -1) {
                this.childIndex = index;
            }
        }

        public int getChildIndex() {
            return childIndex;
        }

        public Element[] getChildrenAdded() {
            return (Element[])added.toArray(new Element[added.size()]);
        }

        public Element[] getChildrenRemoved() {
            return (Element[])removed.toArray(new Element[removed.size()]);
        }

        public ElementEdit toElementEdit() {
            return new ElementEdit(element, childIndex,
                                   getChildrenRemoved(),
                                   getChildrenAdded());
        }

        public void apply() {
            if (applied || isEmpty()) {
                return;
            }
            if (childIndex == -1) {
                childIndex = 0;
            }

            applied = true;
            element.replace(childIndex, removed.size(), getChildrenAdded());
        }

        public boolean isEmpty() {
            return removed.size() == 0 && added.size() == 0;
        }

        public boolean isApplied() {
            return applied;
        }

        public void createLeafElement(final AttributeSet attr, final int start,
                                      final int end) {
            added.add(DefaultStyledDocument.this.createLeafElement(element, attr, start, end));
        }

        public BranchElement createBranchElement(final AttributeSet attr) {
            return (BranchElement)DefaultStyledDocument.this.createBranchElement(element, attr);
        }

        public void splitLeafElement(final Element leaf, final int splitOffset) {
            final AttributeSet attrs = leaf.getAttributes();
            createLeafElement(attrs , leaf.getStartOffset(), splitOffset);
            createLeafElement(attrs, splitOffset, leaf.getEndOffset());
            removed.add(leaf);
        }

        public void splitLeafElement(final Element child,
                                     final int splitOffset1,
                                     final int splitOffset2,
                                     final boolean createMiddle,
                                     final AttributeSet middleAttr) {
            splitLeafElement(child, splitOffset1, splitOffset2, splitOffset2, createMiddle, middleAttr);
        }

        public void splitLeafElement(final Element child,
                                     final int splitOffset1,
                                     final int splitOffset2,
                                     final int splitOffset3,
                                     final boolean createMiddle,
                                     final AttributeSet middleAttr) {
            final AttributeSet attrs = child.getAttributes();
            if (child.getStartOffset() < splitOffset1) {
                createLeafElement(attrs, child.getStartOffset(), splitOffset1);
            }
            if (createMiddle) {
                createLeafElement(middleAttr, splitOffset1, splitOffset2);
            }
            if (splitOffset3 < child.getEndOffset()) {
                createLeafElement(attrs, splitOffset3, child.getEndOffset());
            }
            removed.add(child);
        }

        public void setChildIndexByOffset(final int offset) {
            setChildIndex(element.getElementIndex(offset));
        }

        public Element getChildAtOffset(final int offset) {
            return element.getElement(element.getElementIndex(offset));
        }

        public int getChildIndexAtOffset(final int offset) {
            return element.getElementIndex(offset);
        }

        public Element getCurrentChild() {
            return element.getElement(childIndex);
        }

        public Element getChildElement(final int index) {
            return element.getElement(index);
        }

        public void removeChildElement(final int index) {
            removed.add(element.getElement(index));
        }

        public Element getAddedElement(final int i) {
            return (i > 0 && i < added.size()) ? (Element)added.get(i) : null;
        }

        public Element getLastAddedElement() {
            return (Element)added.get(added.size() - 1);
        }
        public Element getLastRemovedElement() {
            return (Element)removed.get(removed.size() - 1);
        }
    }

    public static final int BUFFER_SIZE_DEFAULT = 4096;
    private transient AttributeSet defaultLogicalStyle;

    protected ElementBuffer buffer;

    private ChangeListener styleContextChangeListener;
    private ChangeListener styleChangeListener;

    public DefaultStyledDocument() {
        this(new GapContent(BUFFER_SIZE_DEFAULT), new StyleContext());
    }

    public DefaultStyledDocument(final Content content,
                                 final StyleContext styles) {
        super(content, styles);
        createDefaultLogicalStyle();
        buffer = new ElementBuffer(createDefaultRoot());
    }

    public DefaultStyledDocument(final StyleContext styles) {
        this(new GapContent(BUFFER_SIZE_DEFAULT), styles);
    }

    public Style addStyle(final String name,
                          final Style parent) {
        return getStyleContext().addStyle(name, parent);
    }

    public void removeStyle(final String name) {
        getStyleContext().removeStyle(name);
    }

    public Style getStyle(final String name) {
        return getStyleContext().getStyle(name);
    }

    public Enumeration<?> getStyleNames() {
        return getStyleContext().getStyleNames();
    }

    public Color getForeground(final AttributeSet attrs) {
        return getStyleContext().getForeground(attrs);
    }

    public Color getBackground(final AttributeSet attrs) {
        return getStyleContext().getBackground(attrs);
    }

    public Font getFont(final AttributeSet attrs) {
        return getStyleContext().getFont(attrs);
    }

    public Element getDefaultRootElement() {
        return buffer.getRootElement();
    }

    public Element getCharacterElement(final int offset) {
        final Element paragraph = getParagraphElement(offset);
        return paragraph.getElement(paragraph.getElementIndex(offset));
    }

    public Element getParagraphElement(final int offset) {
        Element branch;
        Element child = getDefaultRootElement();
        do {
            branch = child;
            child = branch.getElement(branch.getElementIndex(offset));
        } while (!child.isLeaf());
        return branch;
    }

    public void setCharacterAttributes(final int offset,
                                       final int length,
                                       final AttributeSet attrs,
                                       final boolean replace) {
        if (checkInvalid(offset, length)) {
            return;
        }

        writeLock();
        try {
            final DefaultDocumentEvent event =
                new DefaultDocumentEvent(offset, length, EventType.CHANGE);

            buffer.change(offset, length, event);

            AbstractElement element;
            int currentOffset = offset;
            final int limit = offset + length;
            while (currentOffset < limit) {
                element = (AbstractElement)getCharacterElement(currentOffset);
                event.addEdit(new AttributeUndoableEdit(element,
                                                        attrs, replace));
                if (replace) {
                    element.removeAttributes(element.getAttributeNames());
                }
                element.addAttributes(attrs);
                currentOffset = element.getEndOffset();
            }

            event.end();
            fireChangedUpdate(event);
            fireUndoableEditUpdate(new UndoableEditEvent(this, event));
        } finally {
            writeUnlock();
        }
    }

    public void setParagraphAttributes(final int offset,
                                       final int length,
                                       final AttributeSet attrs,
                                       final boolean replace) {
        if (checkInvalid(offset, length)) {
            return;
        }

        writeLock();
        try {
            final DefaultDocumentEvent event =
                new DefaultDocumentEvent(offset, length, EventType.CHANGE);

            AbstractElement element;
            int currentOffset = offset;
            final int limit = offset + length;
            while (currentOffset < limit) {
                element = (AbstractElement)getParagraphElement(currentOffset);
                event.addEdit(new AttributeUndoableEdit(element,
                                                        attrs, replace));
                if (replace) {
                    element.removeAttributes(element.getAttributeNames());
                }
                element.addAttributes(attrs);
                currentOffset = element.getEndOffset();
            }

            event.end();
            fireChangedUpdate(event);
            fireUndoableEditUpdate(new UndoableEditEvent(this, event));
        } finally {
            writeUnlock();
        }
    }

    public void setLogicalStyle(final int offset,
                                final Style style) {
        final AbstractElement branch =
            (AbstractElement)getParagraphElement(offset);
        writeLock();
        try {
            branch.setResolveParent(style);
        } finally {
            writeUnlock();
        }
    }

    public Style getLogicalStyle(final int offset) {
        final Element element = getParagraphElement(offset);
        Object resolver = element.getAttributes().getResolveParent();
        return resolver instanceof Style ? (Style)resolver : null;
    }

    public void addDocumentListener(final DocumentListener listener) {
        super.addDocumentListener(listener);
        getStyleContext().addChangeListener(getStyleContextListener());
        addListenerToStyles();
    }

    public void removeDocumentListener(final DocumentListener listener) {
        super.removeDocumentListener(listener);
        if (getDocumentListeners().length == 0) {
            getStyleContext().removeChangeListener(getStyleContextListener());
            removeListenerFromStyles();
        }
    }

    protected AbstractElement createDefaultRoot() {
        final BranchElement result = new SectionElement();
        writeLock();
        try {
            final BranchElement paragraph =
                (BranchElement)createBranchElement(result, null);
            paragraph.setResolveParent(getStyle(StyleContext.DEFAULT_STYLE));
            final Element content =
                createLeafElement(paragraph, null,
                                  getStartPosition().getOffset(),
                                  getEndPosition().getOffset());
            paragraph.replace(0, 0, new Element[] {content});
            result.replace(0, 0, new Element[] {paragraph});
        } finally {
            writeUnlock();
        }
        return result;
    }

    protected void create(final ElementSpec[] specs) {
        final StringBuffer text = appendSpecsText(specs);

        writeLock();
        try {
            if (getLength() > 0) {
                try {
                    remove(0, getLength());
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }

            final int offset = 0;
            UndoableEdit contentInsert = null;
                try {
                    contentInsert =
                        getContent().insertString(offset, text.toString());
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }

            DefaultDocumentEvent event =
                new DefaultDocumentEvent(offset, text.length(),
                                         EventType.INSERT);
            if (contentInsert != null) {
                event.addEdit(contentInsert);
            }
            event.addEdit(
                new AttributeUndoableEdit(buffer.getRootElement(),
                                          getStyleContext().getEmptySet(),
                                          true));
            ((AbstractElement)buffer.getRootElement())
                              .removeAttributes(buffer.getRootElement()
                                                .getAttributes());

            buffer.create(specs, event);

            event.end();
            fireInsertUpdate(event);
            if (contentInsert != null) {
                fireUndoableEditUpdate(new UndoableEditEvent(this, event));
            }
        } finally {
            writeUnlock();
        }
    }

    protected void insert(final int offset, final ElementSpec[] specs)
        throws BadLocationException {

        final StringBuffer text = appendSpecsText(specs);
        writeLock();
        try {
            UndoableEdit contentInsert =
                getContent().insertString(offset, text.toString());

            DefaultDocumentEvent event =
                new DefaultDocumentEvent(offset, text.length(),
                                         EventType.INSERT);
            if (contentInsert != null) {
                event.addEdit(contentInsert);
            }

            buffer.insert(offset, text.length(), specs, event);

            event.end();
            fireInsertUpdate(event);
            if (contentInsert != null) {
                fireUndoableEditUpdate(new UndoableEditEvent(this, event));
            }
        } finally {
            writeUnlock();
        }
    }

    protected void insertUpdate(final DefaultDocumentEvent event,
                                final AttributeSet attrs) {
        final AttributeSet attributes = attrs == null
                                        ? getStyleContext().getEmptySet()
                                        : attrs;

        final List specs = new LinkedList();

        String text = null;
        final int offset = event.getOffset();
        final int length = event.getLength();

        try {
            text = getText(offset, length);
        } catch (final BadLocationException e) { }

        boolean splitPrevParagraph = false;
        try {
            splitPrevParagraph = offset > 0
                                 && getText(offset - 1, 1).charAt(0) == '\n';
        } catch (final BadLocationException e) { }

        final int firstBreak = text.indexOf('\n');
        final int lastBreak = text.lastIndexOf('\n');
        final boolean hasLineBreak = firstBreak != -1;

        Element charElem = getCharacterElement(offset);
        ElementSpec spec = null;
        if (!hasLineBreak) {
            if (splitPrevParagraph) {
                splitBranch(specs, offset, length, charElem,
                            ElementSpec.JoinNextDirection);
                // The direction of the next Content element must be chosen
                // based on attributes of the first Content element
                // in the next paragraph
                charElem = getCharacterElement(offset + length);
            }
            spec = new ElementSpec(attributes, ElementSpec.ContentType, length);
            if (charElem.getAttributes().isEqual(attributes)) {
                spec.setDirection(splitPrevParagraph
                                  ? ElementSpec.JoinNextDirection
                                  : ElementSpec.JoinPreviousDirection);
            }
            specs.add(spec);
        } else {
            int currentOffset = offset;
            int currentIndex = firstBreak;
            int processedLength = 0;

            if (splitPrevParagraph) {
                splitBranch(specs, offset, length, charElem,
                            ElementSpec.OriginateDirection);
            }

            while (currentOffset < offset + length) {
                if (!(currentIndex < 0)) {
                    spec = new ElementSpec(attributes, ElementSpec.ContentType,
                                           currentIndex + 1 - processedLength);
                    currentOffset += spec.getLength();
                    processedLength += spec.getLength();
                    if (specs.size() == 0
                        && charElem.getAttributes().isEqual(attributes)) {

                        spec.setDirection(ElementSpec.JoinPreviousDirection);
                    }
                    specs.add(spec);

                    specs.add(new ElementSpec(null, ElementSpec.EndTagType));

                    spec = new ElementSpec(defaultLogicalStyle,
                                           ElementSpec.StartTagType);
                    if (currentIndex == lastBreak) {
                        spec.setDirection(splitPrevParagraph
                                          ? ElementSpec.JoinNextDirection
                                          : ElementSpec.JoinFractureDirection);
                    }
                    specs.add(spec);

                    currentIndex = text.indexOf('\n', currentIndex + 1);
                } else {
                    spec = new ElementSpec(attributes, ElementSpec.ContentType,
                                           length - processedLength);
                    currentOffset += spec.getLength();
                    processedLength += spec.getLength();
                    if (getCharacterElement(currentOffset)
                        .getAttributes().isEqual(attributes)) {

                        spec.setDirection(ElementSpec.JoinNextDirection);
                    }
                    specs.add(spec);
                }
            }

        }

        final Object[] specArray = specs.toArray(new ElementSpec[specs.size()]);
        buffer.insert(offset, length, (ElementSpec[])specArray, event);

        super.insertUpdate(event, attrs);
    }

    private void splitBranch(final List specs,
                             final int offset, final int length,
                             final Element leaf,
                             final short lastSpecDirection) {
        ElementSpec spec = null;
        Element branch = leaf.getParentElement();
        final int endOffset = offset + length;
        while (branch != null && branch.getEndOffset() == endOffset) {
            specs.add(new ElementSpec(null, ElementSpec.EndTagType));
            branch = branch.getParentElement();
        }

        branch = branch.getElement(branch.getElementIndex(offset) + 1);
        while (branch != null
               && !branch.isLeaf()
               && branch.getStartOffset() == endOffset) {
            spec = new ElementSpec(branch.getAttributes(),
                                   ElementSpec.StartTagType);
            spec.setDirection(ElementSpec.JoinNextDirection);
            specs.add(spec);
            branch = branch.getElement(0);
        }
        spec.setDirection(lastSpecDirection);
    }

    protected void removeUpdate(final DefaultDocumentEvent event) {
        buffer.remove(event.getOffset(), event.getLength(), event);
    }

    protected void styleChanged(final Style style) {
    }

    private StringBuffer appendSpecsText(final ElementSpec[] specs) {
        final StringBuffer result = new StringBuffer();
        for (int i = 0; i < specs.length; i++) {
            if (specs[i].getLength() > 0) {
                result.append(specs[i].getArray(), specs[i].getOffset(),
                            specs[i].getLength());
            }
        }
        return result;
    }

    private void addListenerToStyles() {
        final Enumeration names = getStyleNames();
        while (names.hasMoreElements()) {
            String name = (String)names.nextElement();
            getStyle(name).addChangeListener(getStyleChangeListener());
        }
    }

    private void removeListenerFromStyles() {
        final Enumeration names = getStyleNames();
        while (names.hasMoreElements()) {
            String name = (String)names.nextElement();
            getStyle(name).removeChangeListener(getStyleChangeListener());
        }
    }

    private boolean checkInvalid(final int offset, final int length) {
        return offset < 0 || length <= 0 || offset + length > getLength() + 1;
    }

    private void createDefaultLogicalStyle() {
        final StyleContext styles = getStyleContext();
        defaultLogicalStyle =
            styles.addAttribute(styles.getEmptySet(),
                                AttributeSet.ResolveAttribute,
                                styles.getStyle(StyleContext.DEFAULT_STYLE));
    }

    private ChangeListener getStyleChangeListener() {
        if (styleChangeListener == null) {
            styleChangeListener = new ChangeListener() {
                public void stateChanged(final ChangeEvent e) {
                    styleChanged((Style)e.getSource());
                }
            };
        }
        return styleChangeListener;
    }

    private ChangeListener getStyleContextListener() {
        if (styleContextChangeListener == null) {
            styleContextChangeListener = new ChangeListener() {
                public void stateChanged(final ChangeEvent e) {
                    removeListenerFromStyles();
                    addListenerToStyles();
                }
            };
        }
        return styleContextChangeListener;
    }

    private StyleContext getStyleContext() {
        return (StyleContext)getAttributeContext();
    }

    private void readObject(final ObjectInputStream ois)
        throws IOException, ClassNotFoundException {

        ois.defaultReadObject();
        createDefaultLogicalStyle();
    }

    private void writeObject(final ObjectOutputStream oos)
        throws IOException {

        oos.defaultWriteObject();
    }
}


