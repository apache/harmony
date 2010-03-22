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

package javax.swing.text.html.parser;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import org.apache.harmony.security.asn1.ASN1Boolean;
import org.apache.harmony.security.asn1.ASN1Implicit;
import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1SetOf;
import org.apache.harmony.security.asn1.ASN1StringType;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;

/**
 * It implements ASN.1 codification tools for
 * <code>javax.swing.text.html.parser.Element</code>. Given an
 * <code>Element</code>, its values are codified in ASN.1 according to the
 * following rule:
 * 
 * <pre>
 *  HTMLElement ::= SEQUENCE {
 *      index INTEGER, 
 *      name UTF8String,
 *      type INTEGER,
 *      oStart BOOLEAN,
 *      oEnd BOOLEAN,
 *      exclusions [0] IMPLICIT SET OF INTEGER OPTIONAL,
 *      inclusions [1] IMPLICIT SET OF INTEGER OPTIONAL,
 *      attributes SET OF HTMLElementAttributes OPTIONAL,
 *      contentModel HTMLContentModel
 * }
 * </pre>
 * 
 * The class can be used to obtain a byte array representing the codification of
 * an <code>Element</code>, as well as an <code>Element</code> from a byte
 * array (previously obtained codifying an <code>Element</code>). In fact, it
 * serves as a wrapper for the codification and the <code>Element</code>
 * itself.
 */
class Asn1Element {

    /**
     * It stores the definition of an <code>Element</code> as an ASN.1 valid
     * type according to the ASN.1 framework.
     */
    private static ASN1Type ASN1_ELEMENT;

    /**
     * The definition of the ASN1_ELEMENT type according to the rule defined for
     * an <code>Element</code>. It also defines a custom encoder/decoder for
     * this type.
     */
    static {
        ASN1_ELEMENT = new ASN1Sequence(new ASN1Type[] {
                ASN1Integer.getInstance(), // 0 Index
                ASN1StringType.UTF8STRING, // 1 Name
                ASN1Integer.getInstance(), // 2 Type
                ASN1Boolean.getInstance(), // 3 OStart
                ASN1Boolean.getInstance(), // 4 OEnd
                new ASN1Implicit(0, new ASN1SetOf(ASN1Integer.getInstance())), // 5 Exclusions
                new ASN1Implicit(1, new ASN1SetOf(ASN1Integer.getInstance())), // 6 Inclusions
                new ASN1SetOf(Asn1Attributes.getInstance()), // 7 Attributes
                Asn1ContentModel.getInstance() // 8 ContentModel
        }) {

            {
                setOptional(5); // Exclusions Optional
                setOptional(6); // Inclusions Optional
                setOptional(7); // Attributes Optional
            }

            /**
             * Overrided method used to decodified the information that
             * represents an <code>Element</code>. It makes a completely new
             * <code>Element</code> with the information interpreted from the
             * stream.
             * <p>
             * Note that the data field is ignored, and thus always set to null.
             * 
             * @param in
             *            The <code>BerInputStream</code> where the
             *            codificated information will be read from.
             * @return An <code>Element</code> filled with the information
             *         read from the stream.
             */
            public Object getDecodedObject(BerInputStream in) {
                Object values[] = (Object[]) in.content;

                int index = new BigInteger((byte[]) values[0]).intValue();
                String name = (String) values[1];
                int type = new BigInteger((byte[]) values[2]).intValue();
                boolean oStart = ((Boolean) values[3]).booleanValue();
                boolean oEnd = ((Boolean) values[4]).booleanValue();
                BitSet exclusions = values[5] != null ? list2bitset(((List) values[5]))
                        : null;
                BitSet inclusions = values[6] != null ? list2bitset(((List) values[6]))
                        : null;
                AttributeList att = values[7] != null ? list2att((List) values[7])
                        : null;
                ContentModel model = values[8] == null ? null
                        : ((ContentModel) values[8]);

                // XXX data is always null, we ignore it
                return new Element(index, name, oStart, oEnd, exclusions,
                        inclusions, type, model, att, null);
            }

            /**
             * Overrided method used to codify the information stored in an
             * <code>Element</code> into an array of bytes, according to its
             * ASN.1 specification.
             * <p>
             * Note that the <code>data</code> field is not taken in
             * consideration.
             * 
             * @param object
             *            The object where the information to be codified is
             *            stored. It actually consists of an
             *            <code>Asn1Element</code> object which contains the
             *            <code>Element</code> to be codified.
             * 
             * @param values
             *            An array of objects where the element's index, name,
             *            type, omitting properties, attribute list, element
             *            inclusions and exclusions and its content model
             *            information will be stored, ready for codification.
             */
            public void getValues(Object object, Object values[]) {
                Asn1Element asn1 = (Asn1Element) object;

                try {
                    values[0] = BigInteger
                            .valueOf(asn1.getElement().getIndex())
                            .toByteArray(); // Index
                    values[1] = asn1.getElement().getName(); // Name
                    values[2] = BigInteger.valueOf(asn1.getElement().getType())
                            .toByteArray(); // Type
                    values[3] = Boolean.valueOf(asn1.getElement().omitStart()); // OStart
                    values[4] = Boolean.valueOf(asn1.getElement().omitEnd()); // OEnd
                    values[5] = asn1.getElement().exclusions != null ? // Exclusions
                            bitset2list(asn1.getElement().exclusions) : new ArrayList();
                    values[6] = asn1.getElement().inclusions != null ? // Inclusions
                            bitset2list(asn1.getElement().inclusions) : new ArrayList();
                    values[7] = asn1.getElement().getAttributes() != null ? // AttributeList
                            att2list(asn1.getElement().getAttributes()) : null;
                    values[8] = new Asn1ContentModel(asn1.getElement()
                            .getContent());

                } catch (IOException e) {
                    throw new AssertionError(e); // this should not happen
                }
            }
        };
    }

    /**
     * It returns an <code>ASN1Type</code> value that contains the ASN.1
     * codification rules for an <code>Element</code> with its encoder and
     * decoder.
     * <p>
     * Among other things, this method can be used to declare the types of new
     * fields in other structures, as for example, in an <code>Asn1DTD</code>.
     * 
     * @return The value that defines an ASN.1 <code>Element</code>
     *         representation with its encoder/decoder.
     */
    static ASN1Type getInstance() {
        return ASN1_ELEMENT;
    }

    /**
     * An internal copy of the <code>Element</code> to be codified.
     */
    private Element element;

    /**
     * An internal copy of the byte array which contains the codification of an
     * <code>Element</code>. From this variable, the information used to
     * decodify an <code>Element</code> is read from.
     */
    private byte[] encoded;

    /**
     * Constructs a new instance of an <code>Asn1Element</code> class from a
     * byte array. The byte array received as argument can be later decodified
     * into an <code>Element</code>.
     * 
     * @param encoded
     *            A byte array containing the codified information of an
     *            <code>Element</code>.
     */
    public Asn1Element(byte[] encoded) {
        byte[] copy = new byte[encoded.length];
        System.arraycopy(encoded, 0, copy, 0, copy.length);
        this.encoded = copy;
    }

    /**
     * Constructs a new instance of an <code>Asn1Element</code> class from an
     * <code>Element</code>. The <code>Element</code> received as argument
     * can be then codified into a byte array.
     * <p>
     * The value received as argument should not be null. If so, a
     * <code>NullPointerException</code> is thrown.
     * 
     * @param element
     *            The <code>Element</code> to be codified.
     */
    public Asn1Element(Element element) {
        if (element == null) {
            throw new NullPointerException();
        }
        this.element = element;
    }

    /**
     * Returns the representation of an <code>Element</code> in ASN.1
     * codification.
     * <p>
     * If the <code >Asn1Element</code> object was created with an
     * <code>Element</code>, then the <code>Element</code> is codified and
     * returned as a byte array. On the other hand, if the instance was created
     * using a byte array, then no codification process is made.
     * 
     * @return If at construction time an <code>Element</code> was given, its
     *         representation in ASN.1 codification. If at construction time a
     *         byte array was given, a copy of the same array is returned.
     */
    public byte[] getEncoded() {
        if (encoded == null) {
            return ASN1_ELEMENT.encode(element);
        } else {
            return encoded;
        }
    }

    /**
     * Returns the <code>Element</code> obtained from the decodification of an
     * ASN.1 codified byte array.
     * <p>
     * If the <code>Asn1Element</code> was created giving an
     * <code>Element</code>, a reference to the same <code>Element</code>
     * is obtained. Otherwise, the byte array given at construction time is
     * decodificated into a new <code>Element</code> object.
     * 
     * @return If at construction time an <code>Element</code> was given, the
     *         same <code>Element</code> is returned. If at construction time
     *         a byte array was given, an <code>Element</code> constructed
     *         with the information stored in that byte array is returned.
     * @throws IOException
     *             If the decodification process could not be carried out
     *             properly.
     */
    public Element getElement() throws IOException {
        if (element == null) {
            return (Element) ASN1_ELEMENT.decode(encoded);
        } else {
            return element;
        }
    }

    /**
     * Converts an <code>AttributeList</code> into a <code>List</code>.
     * 
     * @param att
     *            The <code>AttributeList</code> to be converted.
     * @return A <code>List</code> with the same nodes that composed the
     *         <code>AttributeList</code>, ordered by the
     *         <code>AttributeList</code>'s next field.
     */
    private static List att2list(AttributeList att) {
        ArrayList<Asn1Attributes> lstAttribute = new ArrayList<Asn1Attributes>();
        for (AttributeList attNext = att; attNext != null; attNext = attNext
                .getNext()) {
            lstAttribute.add(new Asn1Attributes(attNext));
        }
        return lstAttribute;
    }

    /**
     * Converts a <code>List</code> into an <code>AttributeList</code>
     * 
     * @param lst
     *            The <code>List</code> to be converted.
     * @return An <code>AttributeList</code> made of the nodes contained in
     *         the <code>List</code> given as argument. The
     *         <code>AttributeList</code>'s <code>next</code> field is set
     *         according to <code>List</code> order.
     */
    private static AttributeList list2att(List lst) {
        AttributeList attReturn;
        attReturn = (AttributeList) lst.get(0);
        for (int i = 0; i < lst.size(); i++) {
            AttributeList first = (AttributeList) lst.get(i);
            if (i != lst.size() - 1) { // last element ?
                AttributeList second = (AttributeList) lst.get(i + 1);
                first.next = second;
            }
        }
        return attReturn;
    }

    /**
     * Converts a <code>BitSet</code> into a <code>List</code> of
     * <code>byte[]</code>. The elements that compose the <code>List</code>
     * are actually byte arrays that represents the values of the
     * <code>BitSet</code> which were set.
     * <p>
     * For example, if the <code>BitSet</code> has its <code>i</code>
     * position set, then the <code>List</code> will contain as element the
     * value: <code>BigInteger.valueOf(i).toByteArray()</code>.
     * 
     * @param bs
     *            The <code>BitSet</code> to be converted.
     * @return A <code>List</code> with each of the <code>BitSet</code>
     *         values which were set, codified as a byte array.
     */
    private static List bitset2list(BitSet bs) {
        ArrayList<byte[]> lst = new ArrayList<byte[]>();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            lst.add(BigInteger.valueOf(i).toByteArray());
        }
        return lst;
    }

    /**
     * Converts a <code>List</code> of <code>byte[]</code> into a
     * <code>BitSet</code>.
     * <p>
     * The byte array contained into the <code>List</code> are representations
     * of the values which should be set in the <code>BitSet</code>.
     * 
     * @param lst
     *            The <code>List</code> of <code>byte[]</code> to be
     *            converted.
     * @return A <code>BitSet</code> with the values specified by the nodes of
     *         the <code>List</code>.
     */
    private static BitSet list2bitset(List lst) {
        BitSet bs = null;
        if (!lst.isEmpty()) { 
            bs = new BitSet();
        }
        for (Iterator iter = lst.iterator(); iter.hasNext();) {
            byte[] element = (byte[]) iter.next();
            BigInteger bi = new BigInteger(element);
            bs.set(bi.intValue());
        }
        return bs;
    }
}
