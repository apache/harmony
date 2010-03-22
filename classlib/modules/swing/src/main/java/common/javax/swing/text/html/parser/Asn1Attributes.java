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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1SetOf;
import org.apache.harmony.security.asn1.ASN1StringType;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;

/**
 * It implements ASN.1 codification tools for
 * <code>javax.swing.text.html.parser.AttributeList</code>. Given an
 * <code>AttributeList</code>, its values are codified in ASN.1 according to
 * the following rule:
 *
 * <pre>
 * HTMLElementAttributes ::= SEQUENCE {
 *      Name UTF8String,
 *      Type INTEGER,
 *      Modifier INTEGER,
 *      DefaultValue UTF8String OPTIONAL,
 *      PossibleValues SET OF UTF8String OPTIONAL
 * }
 * </pre>
 *
 * The class can be used to obtain a byte array representing the codification of
 * an <code>AttributeList</code>, as well as an <code>AttributeList</code>
 * from a byte array (previously obtained codifying an <code>AttributeList</code>).
 * In fact, it serves as a wrapper for the codification and the
 * <code>AttributeList</code> itself.
 * 
 */
class Asn1Attributes {

    /**
     * It stores the definition of an <code>AttributeList</code> as an ASN.1
     * valid type according to the ASN.1 framework.
     */
    private static ASN1Type ASN1_ATTRIBUTES;

    /**
     * The definition of the ASN1_ATTRIBUTES type according to the rule defined
     * for an <code>AttributeList</code>. It also defines a custom
     * encoder/decoder for this type.
     */
    static {
        ASN1_ATTRIBUTES = new ASN1Sequence(new ASN1Type[] {
                ASN1StringType.UTF8STRING, // 0 Name
                ASN1Integer.getInstance(), // 1 Type
                ASN1Integer.getInstance(), // 2 Modifier
                ASN1StringType.UTF8STRING, // 3 DefaultValue
                new ASN1SetOf(ASN1StringType.UTF8STRING) // 4 PosibleValues
                }) {

            {
                setOptional(3);
                setOptional(4);
            }

            /**
             * Overrided method used to decodified the information that
             * represents an <code>AttributeList</code>. It makes a completely
             * new <code>AttributeList</code> with the information interpreted
             * from the stream.
             * <p>
             * Note that the information related with the next
             * <code>AttributeList</code> in the chain is managed in the
             * <code>Asn1Element</code> class. For this reason, from this
             * method, the <code>next</code> field is always set to null.
             * 
             * @param in
             *            The <code>BerInputStream</code> where the
             *            codified information will be read from.
             * @return An <code>AttributeList</code> filled with the
             *         information read from the stream.
             */
            public Object getDecodedObject(BerInputStream in) {
                Object values[] = (Object[]) in.content;

                String name = (String) values[0]; // Name
                int type = new BigInteger((byte[]) values[1]).intValue(); // Type
                int modifier = new BigInteger((byte[]) values[2]).intValue(); // Modifier
                String defValue = (String) values[3]; // DefValue
                Vector vecValues = values[4] != null ?
                        lst2vector((List) values[4]) : null;

                // XXX next value modified in Asn1Element...
                return new AttributeList(name, type, modifier, defValue,
                        vecValues, null);
            }

            /**
             * Overrided method used to codify the information stored in an
             * <code>AttributeList</code> into an array of bytes, according to
             * its ASN.1 specification.
             * 
             * @param object
             *            The object where the information to be codified is
             *            stored. It actually consists of an
             *            <code>Asn1Attributes</code> object which contains
             *            the <code>AttributeList</code> to be codified.
             * 
             * @param values
             *            An array of objects where the attributes' name, type,
             *            modifier, default value and possibles allowed values
             *            information will be stored, ready for codification.
             */
            public void getValues(Object object, Object values[]) {
                Asn1Attributes asn1 = (Asn1Attributes) object;

                try {
                    values[0] = asn1.getAttributeList().getName();
                    values[1] = BigInteger.valueOf(
                            asn1.getAttributeList().getType()).toByteArray();
                    values[2] = BigInteger.valueOf(
                            asn1.getAttributeList().getModifier())
                            .toByteArray();
                    values[3] = asn1.getAttributeList().getValue();

                    if (asn1.getAttributeList().values != null) {
                        ArrayList<String> lst = new ArrayList<String>();
                        for (Enumeration e = asn1.getAttributeList()
                                .getValues(); e.hasMoreElements();) {
                            lst.add((String) e.nextElement());
                        }
                        values[4] = lst;
                    }
                } catch (IOException e) {
                    throw new AssertionError(e); // this should not happen
                }
            }
        };
    }

    /**
     * It returns an <code>ASN1Type</code> value that contains the ASN.1
     * codification rules for an <code>AttributeList</code> with its encoder
     * and decoder.
     * <p>
     * Among other things, this method can be used to declare the types of new
     * fields in other structures, as for example, in an
     * <code>Asn1Element</code>.
     * 
     * @return The value that defines an ASN.1 <code>AttributeList</code>
     *         representation with its encoder/decoder.
     */
    static ASN1Type getInstance() {
        return ASN1_ATTRIBUTES;
    }

    /**
     * An internal copy of the <code>AttributeList</code> to be codified.
     */
    private AttributeList att;

    /**
     * An internal copy of the byte array which contains the codification of an
     * <code>AttributeList</code>. From this variable, the information used
     * to decodify an <code>AttributeList</code> is read from.
     */
    private byte[] encoded;

    /**
     * Constructs a new instance of an <code>Asn1Attributes</code> class from
     * a byte array. The byte array received as argument can be later decodified
     * into an <code>AttributeList</code>.
     * 
     * @param encoded
     *            A byte array containing the codified information of an
     *            <code>AttributeList</code>.
     */
    public Asn1Attributes(byte[] encoded) {
        byte[] copy = new byte[encoded.length];
        System.arraycopy(encoded, 0, copy, 0, copy.length);
        this.encoded = copy;
    }

    /**
     * Constructs a new instance of an <code>Asn1Attributes</code> class from
     * an <code>AttributeList</code>. The <code>AttributeList</code>
     * received as argument can be then codified into a byte array.
     * <p>
     * The value received as argument should not be null. If so, a
     * <code>NullPointerException</code> is thrown.
     * 
     * @param att
     *            The <code>AttributeList</code> to be codified.
     */
    public Asn1Attributes(AttributeList att) {
        if (att == null) {
            throw new NullPointerException();
        }
        this.att = att;
    }

    /**
     * Returns the representation of an <code>AttributeList</code> in ASN.1
     * codification.
     * <p>
     * If the <code >Asn1Attributes</code> object was created with an
     * <code>AttributeList</code>, then the <code>AttributeList</code> is
     * codified and returned as a byte array. On the other hand, if the instance
     * was created using a byte array, then no codification process is made.
     * 
     * @return If at construction time an <code>AttributeList</code> was
     *         given, its representation in ASN.1 codification. If at
     *         construction time a byte array was given, a copy of the same
     *         array is returned.
     */
    public byte[] getEncoded() {
        if (encoded == null) {
            return ASN1_ATTRIBUTES.encode(att);
        } else {
            return encoded;
        }
    }

    /**
     * Returns the <code>AttributeList</code> obtained from the decodification
     * of an ASN.1 codified byte array.
     * <p>
     * If the <code>Asn1Attributes</code> was created giving an
     * <code>AttributeList</code>, a reference to the same
     * <code>AttributeList</code> is obtained. Otherwise, the byte array given
     * at construction time is decodificated into a new
     * <code>AttributeList</code> object.
     * 
     * @return If at construction time an <code>AttributeList</code> was
     *         given, the same <code>AttributeList</code> is returned. If at
     *         construction time a byte array was given, an
     *         <code>AttributeList</code> constructed with the information
     *         stored in that byte array is returned.
     * @throws IOException
     *             If the decodification process could not be carried out
     *             properly.
     */
    public AttributeList getAttributeList() throws IOException {
        if (att == null) {
            return (AttributeList) ASN1_ATTRIBUTES.decode(encoded);
        } else {
            return att;
        }
    }

    /**
     * Converts a list of <code>String</code> into a vector of
     * <code>String</code>.
     * 
     * @param lst
     *            The list of <code>String</code> to be converted.
     * @return A <code>Vector</code> containing the <code>String</code>s
     *         stored in the <code>List</code> received as argument.
     */
    private static Vector lst2vector(List lst) {
        Vector<String> vecValues = new Vector<String>();
        for (Iterator iter = lst.iterator(); iter.hasNext();) {
            String element = (String) iter.next();
            vecValues.addElement(element);
        }
        return vecValues;
    }

}
