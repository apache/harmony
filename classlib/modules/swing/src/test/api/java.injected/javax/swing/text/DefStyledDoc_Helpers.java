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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Vector;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import junit.framework.Assert;

/**
 * Contains "complex" <code>assert</code> methods as well as logging versions
 * of <code>DefaultStyledDocument</code> and
 * <code>DefaultStyledDocument.ElementBuffer</code> which facilitate testing
 * and debugging.
 *
 */
public final class DefStyledDoc_Helpers extends Assert {
    public static boolean logging = false;

    /**
     * The version of <code>DefaultStyledDocument</code> which logs parameters
     * passed to some methods to <code>stdout</code>.
     */
    public static class DefStyledDocWithLogging extends DefaultStyledDocument {
        private static final long serialVersionUID = 1L;

        public DefStyledDocWithLogging() {
            super();
        }

        public DefStyledDocWithLogging(final Content content) {
            super(content, new StyleContext());
        }

        @Override
        public void insertString(int offset, String text, AttributeSet attrs)
                throws BadLocationException {
            if (logging) {
                System.out.println(">>>doc.insertString(" + offset + ", '" + text + "', "
                        + attrs + ")");
            }
            super.insertString(offset, text, attrs);
            if (logging) {
                System.out.println("<<<doc.insertString");
            }
        }

        @Override
        public void remove(int offset, int length) throws BadLocationException {
            if (logging) {
                System.out.println(">>>doc.remove(" + offset + ", " + length + ")");
            }
            super.remove(offset, length);
            if (logging) {
                System.out.println("<<<doc.remove");
            }
        }

        @Override
        protected void create(ElementSpec[] spec) {
            if (logging) {
                System.out.println(">>>doc.create");
                printElementSpecs(spec);
            }
            super.create(spec);
            if (logging) {
                System.out.println("<<<doc.create");
            }
        }

        @Override
        protected void insert(int offset, ElementSpec[] spec) throws BadLocationException {
            if (logging) {
                System.out.println(">>>doc.insert(" + offset + ", " + ")");
                printElementSpecs(spec);
            }
            super.insert(offset, spec);
            if (logging) {
                System.out.println("<<<doc.insert");
            }
        }

        @Override
        protected void insertUpdate(DefaultDocumentEvent event, AttributeSet attrs) {
            if (logging) {
                System.out.println(">>>doc.insertUpdate(" + ", " + attrs + ")");
            }
            super.insertUpdate(event, attrs);
            if (logging) {
                System.out.println("<<<doc.insertUpdate");
            }
        }

        @Override
        protected void removeUpdate(DefaultDocumentEvent event) {
            if (logging) {
                System.out.println(">>>doc.removeUpdate");
            }
            super.removeUpdate(event);
            if (logging) {
                System.out.println("<<<doc.removeUpdate");
            }
        }

        @Override
        protected void styleChanged(Style style) {
            if (logging) {
                System.out.println(">>>styleChanged(" + style + ")");
            }
            super.styleChanged(style);
            if (logging) {
                System.out.println("<<<styleChanged(" + style + ")");
            }
        }

        @Override
        protected Element createBranchElement(Element parent, AttributeSet as) {
            if (logging) {
                System.out.println("createBranch("
                        + parent.getName()
                        + "["
                        + (parent.getElementCount() <= 0 ? "N/A" : (parent.getStartOffset()
                                + ", " + parent.getEndOffset())) + "], "
                        + (as == null ? "null" : new SimpleAttributeSet(as).toString()) + ")");
            }
            return super.createBranchElement(parent, as);
        }

        @Override
        protected Element createLeafElement(Element parent, AttributeSet as, int start, int end) {
            if (logging) {
                System.out.println("createLeaf("
                        + parent.getName()
                        + "["
                        + (parent.getElementCount() <= 0 ? "N/A" : (parent.getStartOffset()
                                + ", " + parent.getEndOffset())) + "], "
                        + (as == null ? "null" : new SimpleAttributeSet(as).toString()) + ", "
                        + start + ", " + end + ")");
            }
            return super.createLeafElement(parent, as, start, end);
        }
    }

    /**
     * The version of <code>DefaultStyledDocument.ElementBuffer</code>
     * which logs parameters passed to some methods to <code>stdout</code>.
     */
    public static class ElementBufferWithLogging extends DefaultStyledDocument.ElementBuffer {
        private static final long serialVersionUID = 1L;

        public ElementBufferWithLogging(final DefaultStyledDocument doc, final Element root) {
            doc.super(root);
        }

        @Override
        public void change(int offset, int length, DefaultDocumentEvent event) {
            if (logging) {
                System.out.println("->buf.change(" + offset + ", " + length + ", \n\t" + event
                        + ")");
            }
            super.change(offset, length, event);
            if (logging) {
                System.out.println("<-buf.change");
            }
        }

        @Override
        protected void changeUpdate() {
            if (logging) {
                System.out.println("->buf.changeUpdate");
            }
            super.changeUpdate();
            if (logging) {
                System.out.println("<-buf.changeUpdate");
            }
        }

        @Override
        public Element clone(Element parent, Element clonee) {
            if (logging) {
                System.out.println("clone("
                        + parent.getName()
                        + "["
                        + (parent.getElementCount() <= 0 ? "N/A" : (parent.getStartOffset()
                                + ", " + parent.getEndOffset()))
                        + "], "
                        + ", "
                        + clonee.getName()
                        + "["
                        + (!clonee.isLeaf() && clonee.getElementCount() <= 0 ? "N/A" : (clonee
                                .getStartOffset()
                                + ", " + clonee.getEndOffset())) + "])");
            }
            return super.clone(parent, clonee);
        }

        @Override
        public void insert(int offset, int length, ElementSpec[] spec,
                DefaultDocumentEvent event) {
            if (logging) {
                System.out
                        .println("->buf.insert(" + offset + ", " + length + ", " + ", " + ")");
                printElementSpecs(spec);
                System.out.println(event);
            }
            super.insert(offset, length, spec, event);
            if (logging) {
                System.out.println(event);
                System.out.println("<-buf.insert");
            }
        }

        @Override
        protected void insertUpdate(ElementSpec[] spec) {
            if (logging) {
                System.out.println("->buf.insertUpdate");
                printElementSpecs(spec);
            }
            super.insertUpdate(spec);
            if (logging) {
                System.out.println("<-buf.insertUpdate");
            }
        }

        @Override
        public void remove(int offset, int length, DefaultDocumentEvent event) {
            if (logging) {
                System.out.println("->buf.remove(" + offset + ", " + length + ")");
            }
            super.remove(offset, length, event);
            if (logging) {
                System.out.println("<-buf.remove");
            }
        }

        @Override
        protected void removeUpdate() {
            if (logging) {
                System.out.println("->buf.removeUpdate");
            }
            super.removeUpdate();
            if (logging) {
                System.out.println("<-buf.removeUpdate");
            }
        }
    }

    public static final AttributeSet bold;

    public static final AttributeSet italic;
    static {
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setBold(attrs, true);
        bold = attrs;
        attrs = new SimpleAttributeSet();
        StyleConstants.setItalic(attrs, true);
        italic = attrs;
    }

    /**
     * Asserts that the expected number of children was removed and/or added
     * to an element.
     *
     * @param object <code>ElementChange</code> implementation which stores
     *               the information.
     * @param element the element modified.
     * @param removed the number of children removed.
     * @param added the number of children added
     */
    public static void assertChange(final Object object, final Element element,
            final int removed, final int added) {
        ElementChange change = (ElementChange) object;
        assertSame("change.element", element, change.getElement());
        assertEquals("change.removed.length", removed, change.getChildrenRemoved().length);
        assertEquals("change.added.length", added, change.getChildrenAdded().length);
    }

    /**
     * Asserts that the removed and/or added children have the expected offsets.
     *
     * @param change the changes performed to an element.
     * @param removedOffsets the offsets of children removed in the form
     *                       <code>{start1, end1, start2, end2, ...}</code>.
     * @param addedOffsets the offsets of children added in the same form as
     *                     <code>removedOffsets</code>.
     */
    public static void assertChange(final ElementChange change, final int[] removedOffsets,
            final int[] addedOffsets) {
        final Element[] removed = change.getChildrenRemoved();
        assertEquals("change.removed.length", removedOffsets.length / 2, removed.length);
        for (int i = 0, j = 0; i < removed.length; i++, j += 2) {
            assertEquals("change.removed[" + i + "].start", removedOffsets[j], removed[i]
                    .getStartOffset());
            assertEquals("change.removed[" + i + "].end", removedOffsets[j + 1], removed[i]
                    .getEndOffset());
        }
        final Element[] added = change.getChildrenAdded();
        assertEquals("change.added.length", addedOffsets.length / 2, added.length);
        for (int i = 0, j = 0; i < added.length; i++, j += 2) {
            assertEquals("change.added[" + i + "].start", addedOffsets[j], added[i]
                    .getStartOffset());
            assertEquals("change.added[" + i + "].end", addedOffsets[j + 1], added[i]
                    .getEndOffset());
        }
    }

    /**
     * Asserts that ElementChange has expected field values.
     *
     * @param change the change
     * @param element element where the change occurred
     * @param index index where the change occurred
     * @param removed offsets of the elements removed
     * @param added offsets of the elements added
     */
    public static void assertChange(final Object change, final Element element,
            final int index, final int[] removed, final int[] added) {
        assertSame("change.element", element, ((ElementChange) change).getElement());
        assertEquals("change.index", index, ((ElementChange) change).getIndex());
        assertChange((ElementChange) change, removed, added);
    }

    /**
     * Asserts that an element has children with the expected offsets.
     *
     * @param element the element where to check children.
     * @param offsets the expected offsets in the form
     *                <code>{start1, end1, start2, end2, ...}</code>.
     */
    public static void assertChildren(final Element element, final int[] offsets) {
        final int count = element.getElementCount();
        assertEquals("element.children.length", offsets.length / 2, count);
        for (int i = 0, j = 0; i < count; i++, j += 2) {
            final Element child = element.getElement(i);
            assertEquals("element.children[" + i + "].start", offsets[j], child
                    .getStartOffset());
            assertEquals("element.children[" + i + "].end", offsets[j + 1], child
                    .getEndOffset());
        }
    }

    /**
     * Asserts that an element has children with the expected offsets and
     * attributes.
     *
     * @param element the element where to check children.
     * @param offsets the expected offsets.
     * @param attributes the expected attributes; <code>null</code> if no
     *                   attributes expected.
     */
    public static void assertChildren(final Element element, final int[] offsets,
            final AttributeSet[] attributes) {
        assertChildren(element, offsets);
        assertEquals("element.attributes.length", attributes.length, element.getElementCount());
        for (int i = 0; i < attributes.length; i++) {
            final Element child = element.getElement(i);
            if (attributes[i] == null) {
                assertEquals("element.children[" + i + "].attributes.count", 0, child
                        .getAttributes().getAttributeCount());
            } else {
                assertTrue("element.child[" + i + "].attributes", child.getAttributes()
                        .isEqual(attributes[i]));
            }
        }
    }

    /**
     * Asserts that an element spec is as expected.
     *
     * @param spec the spec to check.
     * @param type the type of the spec.
     * @param direction the direction of the spec.
     * @param offset the offset of the spec.
     * @param length the length of the spec.
     */
    public static void assertSpec(final ElementSpec spec, final short type,
            final short direction, final int offset, final int length, final boolean isNullArray) {
        assertEquals("spec.type", type, spec.getType());
        assertEquals("spec.direction", direction, spec.getDirection());
        assertEquals("spec.offset", offset, spec.getOffset());
        assertEquals("spec.length", length, spec.getLength());
        assertEquals("spec.array", isNullArray, spec.getArray() == null);
    }

    /**
     * Asserts that an element spec is as expected.
     *
     * @param spec the spec to check.
     * @param type the type of the spec.
     * @param direction the direction of the spec.
     * @param offset the offset of the spec.
     * @param length the length of the spec.
     */
    public static void assertSpec(final ElementSpec spec, final short type,
            final short direction, final int offset, final int length) {
        assertSpec(spec, type, direction, offset, length, true);
    }

    /**
     * Compares two <code>ElementSpec</code> objects.
     *
     * @param expected the expected spec.
     * @param actual the actual spec.
     */
    public static void assertSpec(final ElementSpec expected, final ElementSpec actual) {
        assertSpec(actual, expected.getType(), expected.getDirection(), expected.getOffset(),
                expected.getLength());
    }

    /**
     * Compares two arrays of <code>ElementSpec</code> objects.
     *
     * @param expected array with expected specs.
     * @param actual array with the actual specs.
     */
    public static void assertSpecs(final ElementSpec[] expected, final ElementSpec[] actual) {
        assertEquals("specs.length", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertSpec(expected[i], actual[i]);
        }
    }

    /**
     * Returns the list of edits stored in <code>DefaultDocumentEvent</code>
     * object.
     *
     * @param event the event from which to extract the field.
     * @return returns the <code>edits</code> from
     *         <code>javax.swing.undo.CompoundEdit</code>, or <code>null</code>
     *         if something goes wrong.
     */
    public static Vector<?> getEdits(final DefaultDocumentEvent event) {
        try {
            Class<?> eventSuperClass = event.getClass().getSuperclass();
            Field f = eventSuperClass.getDeclaredField("edits");
            f.setAccessible(true);
            return (Vector<?>) (f.get(event));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Dumps the changes stored in <code>change</code>.
     *
     * @param change the change to print removed and added children from.
     */
    public static void printChange(ElementChange change) {
        System.out.print("@ " + change.getIndex() + ": " + change.getElement());
        System.out.println("    <<< Removed:");
        printElements(change.getChildrenRemoved());
        System.out.println("    >>> Added:");
        printElements(change.getChildrenAdded());
    }

    /**
     * Prints all the changes to elements performed.
     *
     * @param edits the list extracted from a <code>DefaultDocumentEvet</code>
     *              object.
     */
    public static void printChanges(List<?> edits) {
        for (int i = 0; i < edits.size(); i++) {
            Object edit = edits.get(i);
            if (edit instanceof ElementChange) {
                printChange((ElementChange) edit);
                System.out.println();
            }
        }
    }

    /**
     * Prints elements contained in an array.
     *
     * @param elems the array containing elements of interest.
     */
    public static void printElements(Element[] elems) {
        for (int i = 0; i < elems.length; i++) {
            System.out.print("        [" + i + "]" + elems[i]);
        }
    }

    /**
     * Prints the specs contained in an array.
     *
     * @param spec the array containing specs of interest.
     */
    public static void printElementSpecs(ElementSpec[] spec) {
        for (int i = 0; i < spec.length; i++) {
            System.out.println("\t" + spec[i] + (i != spec.length - 1 ? "," : ""));
        }
    }

    private DefStyledDoc_Helpers() {
    }
}
