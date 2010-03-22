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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1SetOf;
import org.apache.harmony.security.asn1.ASN1StringType;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;

/**
 * It implements ASN.1 codification tools for
 * <code>javax.swing.text.html.parser.DTD</code>. Given an <code>DTD</code>,
 * its values are codified in ASN.1 according to the following rule:
 * 
 * <pre>
 * BDTD ::= SEQUENCE {
 *      Name UTF8String,    
 *      Entity SET OF HTMLEntity,
 *      Element SET OF HTMLElement
 * }
 * </pre>
 * 
 * The class can be used to obtain a byte array representing the codification of
 * a <code>DTD</code>, as well as a <code>DTD</code> from a byte array
 * (previously obtained codifying a <code>DTD</code>). In fact, it serves as a
 * wrapper for the codification and the <code>DTD</code> itself.
 * 
 */
class Asn1Dtd {

    /**
     * It stores the definition of a <code>DTD</code> as an ASN.1 valid type
     * according to the ASN.1 framework.
     */
    private static ASN1Type ASN1_DTD;

    /**
     * The definition of the ASN1_DTD type according to the rule defined for a
     * <code>DTD</code>. It also defines a custom encoder/decoder for this
     * type.
     */
    static {
        ASN1_DTD = new ASN1Sequence(new ASN1Type[] { ASN1StringType.UTF8STRING, // NAME
                new ASN1SetOf(Asn1Entity.getInstance()), // ENTITY
                new ASN1SetOf(Asn1Element.getInstance()) // ELEMENT
                }) {

            /**
             * Overrided method used to decodified the information that
             * represents a <code>DTD</code>. It makes a completely new
             * <code>DTD</code> with the information interpreted from the
             * stream.
             * 
             * @param in
             *            The <code>BerInputStream</code> where the
             *            codificated information will be read from.
             * @return A <code>DTD</code> filled with the information read
             *         from the stream.
             */
            protected Object getDecodedObject(BerInputStream in) {
                Object values[] = (Object[]) in.content;
                DTD dtd = new DTD("");
                dtd.setReading(true);

                // Name
                dtd.name = String.valueOf(values[0]);

                // Entities
                List lstEntities = (ArrayList) values[1];
                Entity entity;
                for (int i=0; i<lstEntities.size(); i++) {
                    entity = (Entity)lstEntities.get(i);
                    dtd.defineEntity(entity.getName(), entity.type, entity
                            .getData());
                }

                // Elements
                List lstElements = (ArrayList) values[2];
                Element element;
                for (int i=0; i<lstElements.size(); i++) {
                    element = (Element) lstElements.get(i);
                    dtd.defineElement(element.getName(), element.getType(),
                            element.omitStart(), element.omitEnd(), element
                                    .getContent(), element.exclusions,
                            element.inclusions, element.getAttributes());
                }
                return dtd;
            }

            /**
             * Overrided method used to codify the information stored in a
             * <code>DTD</code> into an array of bytes, according to its ASN.1
             * specification.
             * 
             * @param object
             *            The object where the information to be codified is
             *            stored. It actually consists of a <code>DTD</code>.
             * 
             * @param values
             *            An array of objects where the dtd's name, entities and
             *            elements information will be stored, ready for
             *            codification.
             */
            protected void getValues(Object object, Object[] values) {
                DTD dtd = (DTD) object;

                // Name
                values[0] = dtd.getName();

                // Entities
                ArrayList<Asn1Entity> lstEntity = new ArrayList<Asn1Entity>();
                Iterator itr = dtd.entityHash.values().iterator();
                while (itr.hasNext()) {
                    lstEntity.add(new Asn1Entity((Entity) itr.next()));
                }
                values[1] = lstEntity;

                // Elements
                ArrayList<Asn1Element> lstElement = new ArrayList<Asn1Element>();
                itr = dtd.elements.iterator();
                while (itr.hasNext()) {
                    lstElement.add(new Asn1Element((Element) itr.next()));
                }
                values[2] = lstElement;
            }

        };
    }

    /**
     * It returns an <code>ASN1Type</code> value that contains the ASN.1
     * codification rules for a <code>DTD</code> with its encoder and decoder.
     * 
     * @return The value that defines an ASN.1 <code>DTD</code> representation
     *         with its encoder/decoder.
     */
    static ASN1Type getInstance() {
        return ASN1_DTD;
    }

    /**
     * An internal copy of the <code>DTD</code> to be codified.
     */
    private DTD dtd;

    /**
     * An internal copy of the byte array which contains the codification of a
     * <code>DTD</code>. From this variable, the information used to decodify
     * a <code>DTD</code> is read from.
     */
    private byte[] encoded;
    
    
    /**
     * An internal copy of the {@link DTD} on which the encoded information
     * will be extracted to. 
     */
    private DTD refDTD;

    /**
     * Constructs a new instance of a <code>Asn1Dtd</code> class from a byte
     * array. The byte array received as argument can be later decodified into a
     * <code>DTD</code>.
     * 
     * @param encoded
     *            A byte array containing the codified information of a
     *            <code>DTD</code>.
     */
    public Asn1Dtd(byte[] encoded) {
        byte[] copy = new byte[encoded.length];
        System.arraycopy(encoded, 0, copy, 0, encoded.length);
        this.encoded = copy;
    }

    /**
     * Constructs a new instance of an <code>Asn1Dtd</code> class from a
     * <code>DTD</code>. The <code>DTD</code> received as argument can be
     * then codified into a byte array.
     * <p>
     * The value received as argument should not be null. If so, a
     * <code>NullPointerException</code> is thrown.
     * 
     * @param dtd
     *            The <code>DTD</code> to be codified.
     */
    public Asn1Dtd(DTD dtd) {
        if (dtd == null) {
            throw new NullPointerException();
        }
        this.dtd = dtd;
    }

    /**
     * Returns the representation of a <code>DTD</code> in ASN.1 codification.
     * <p>
     * If the <code >Asn1Dtd</code> object was created with a <code>DTD</code>,
     * then the <code>DTD</code> is codified and returned as a byte array. On
     * the other hand, if the instance was created using a byte array, then no
     * codification process is made.
     * 
     * @return If at construction time a <code>DTD</code> was given, its
     *         representation in ASN.1 codification. If at construction time a
     *         byte array was given, a copy of the same array is returned.
     */
    public byte[] getEncoded() {
        if (encoded == null) {
            return ASN1_DTD.encode(dtd);
        } else {
            return encoded;
        }
    }

    /**
     * Returns the <code>DTD</code> obtained from the decodification of an
     * ASN.1 codified byte array.
     * <p>
     * If the <code>Asn1Dtd</code> was created giving a <code>DTD</code>, a
     * reference to the same <code>DTD</code> is obtained. Otherwise, the byte
     * array given at construction time is decodificated into a new
     * <code>DTD</code> object.
     * 
     * @return If at construction time a <code>DTD</code> was given, the same
     *         <code>DTD</code> is returned. If at construction time a byte
     *         array was given, a <code>DTD</code> constructed with the
     *         information stored in that byte array is returned.
     * @throws IOException
     *             If the decodification process could not be carried out
     *             properly.
     */
    public DTD getDTD(DTD refDTD) throws IOException {
        this.refDTD = refDTD;
        if (dtd == null) {
            return restoreElements(updateInfo((DTD) ASN1_DTD.decode(encoded), refDTD));
        } else {
            return dtd;
        }
    }

    /**
     * Updates the information of the <code>Element</code>'s reference stored
     * in the <code>ContentModel</code>s.
     * <p>
     * When the <code>ContentModel</code> is reconstructed from its ASN.1
     * codification, no information about other <code>Element</code> is
     * available at that moment, so information about them must be updated
     * before returning the whole <code>DTD</code>.
     * 
     * @param dtd
     *            The <code>DTD</code> whose
     *            <code>Element<code>'s <code>ContentModel</code>s information
     *            must be updated.
     * @return The <code>DTD</code> with the information updated.
     */
    private DTD restoreElements(DTD dtd) {
        ContentModel model = null;
        Element tmpElem = null;
        ArrayList<ContentModel> queue = new ArrayList<ContentModel>();
        Enumeration itr = dtd.elements.elements();
        Element e;
        while (itr.hasMoreElements()) {
            e = (Element)itr.nextElement();
            model = e.getContent();
            if (model != null) {
                queue.add(model);
                while (!queue.isEmpty()) {
                    model = queue.remove(0);
                    if (model.content instanceof ContentModel) {
                        queue.add((ContentModel) model.content);
                    } else {
                        tmpElem = dtd.elements.get(
                                ((Element)model.content).getIndex());
                        model.content = tmpElem;
                    }
                    if (model.next != null) {
                        queue.add(model.next);
                    }
                }
            }
        }
        return dtd;
    }
    
    
    /**
     * Updates the information of a {@link DTD} using the information stored in
     * the previosly decoded {@link DTD}
     * 
     * @param readDTD the decoded {@link DTD}
     * @param refDTD the {@link DTD} where the decoded information will be
     *               updated.
     * @return a reference to the updated {@link DTD}
     */
    private DTD updateInfo (DTD readDTD, DTD refDTD) {
        refDTD.name = readDTD.name;
        refDTD.entityHash = readDTD.entityHash;
        
        for (Element e : readDTD.elements) {
            if (refDTD.elementHash.containsKey(e.getName())) {
                Element modifyElem = refDTD.elementHash.get(e.getName());
                modifyElem.updateElement(e.getIndex(),
                        e.getName(), e.omitStart(), e.omitEnd(),
                        e.exclusions, e.inclusions, e.getType(),
                        e.getContent(), e.getAttributes(), e.data);
            } else {
                refDTD.elementHash.put(e.getName(), e);
                refDTD.elements.add(e);
            }
        }
        
        return refDTD;
    }
}
