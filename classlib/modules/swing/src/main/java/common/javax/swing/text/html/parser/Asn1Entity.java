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

import org.apache.harmony.security.asn1.ASN1Boolean;
import org.apache.harmony.security.asn1.ASN1Implicit;
import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1StringType;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;

/**
 * It implements ASN.1 codification tools for
 * <code>javax.swing.text.html.parser.Entity</code>. Given an
 * <code>Entity</code>, its values are codified in ASN.1 according to the
 * following rule:
 * 
 * <pre>
 * HTMLEntity ::= SEQUENCE {
 *     name UTF8String,
 *     value INTEGER,
 *     general [0] IMPLICIT BOOLEAN DEFAULT FALSE,
 *     parameter [1] IMPLICIT BOOLEAN DEFAULT FALSE,
 *     data UTF8String
 * }
 * </pre>
 * 
 * The class can be used to obtain a byte array representing the codification of
 * an <code>Entity</code>, as well as an <code>Entity</code> from a byte
 * array (previously obtained codifying an <code>Entity</code>). In fact, it
 * serves as a wrapper for the codification and the <code>Entity</code>
 * itself.
 * 
 */
class Asn1Entity {

    /**
     * It stores the definition of an <code>Entity</code> as an ASN.1 valid
     * type according to the ASN.1 framework.
     */
    private static ASN1Type ASN1_ENTITY;

    /**
     * The definition of the ASN1_ENTITY type according to the rule defined for
     * an <code>Entity</code>. It also defines a custom encoder/decoder for
     * this type.
     */
    static {
        ASN1_ENTITY = new ASN1Sequence(new ASN1Type[] {
                ASN1StringType.UTF8STRING, // Name
                ASN1Integer.getInstance(), // Value
                new ASN1Implicit(0, ASN1Boolean.getInstance()), // General
                new ASN1Implicit(1, ASN1Boolean.getInstance()), // Parameter
                ASN1StringType.UTF8STRING // UTF8String
                }) {

            {
                setDefault(Boolean.FALSE, 2); // GENERAL default value
                setDefault(Boolean.FALSE, 3); // PARAMETER default value
            }

            /**
             * Overrided method used to decodified the information that
             * represents an <code>Entity</code>. It makes a completely new
             * <code>Entity</code> with the information interpreted from the
             * stream.
             * 
             * @param in
             *            The <code>BerInputStream</code> where the
             *            codificated information will be read from.
             * @return An <code>Entity</code> filled with the information read
             *         from the stream.
             */
            public Object getDecodedObject(BerInputStream in) {
                Object values[] = (Object[]) in.content;

                String name = (String) values[0];
                int type = new BigInteger((byte[]) values[1]).intValue();
                boolean general = ((Boolean) values[2]).booleanValue();
                boolean parameter = ((Boolean) values[3]).booleanValue();
                String data = (String) values[4];

                return new Entity(name, type, data, general, parameter);
            }

            /**
             * Overrided method used to codify the information stored in an
             * <code>Entity</code> into an array of bytes, according to its
             * ASN.1 specification.
             * 
             * @param object
             *            The object where the information to be codified is
             *            stored. It actually consists of an
             *            <code>Asn1Entity</code> object which contains the
             *            <code>Entity</code> to be codified.
             * 
             * @param values
             *            An array of objects where the entity's name, type,
             *            data, and isGeneral and isParameter information will
             *            be stored, ready for codification.
             */
            public void getValues(Object object, Object values[]) {
                Asn1Entity asn1 = (Asn1Entity) object;

                try {
                    values[0] = asn1.getEntity().getName();
                    values[1] = BigInteger.valueOf(asn1.getEntity().getType())
                            .toByteArray();
                    values[2] = Boolean.valueOf(asn1.getEntity().isGeneral());
                    values[3] = Boolean.valueOf(asn1.getEntity().isParameter());
                    values[4] = String.valueOf(asn1.getEntity().getData());
                } catch (IOException e) {
                    throw new AssertionError(e); // this should not happen
                }
            }
        };
    }

    /**
     * It returns an <code>ASN1Type</code> value that contains the ASN.1
     * codification rules for an <code>Entity</code> with its encoder and
     * decoder.
     * <p>
     * Among other things, this method can be used to declare the types of new
     * fields in other structures, as for example, in an <code>Asn1DTD</code>.
     * 
     * @return The value that defines an ASN.1 <code>Entity</code>
     *         representation with its encoder/decoder.
     */
    static ASN1Type getInstance() {
        return ASN1_ENTITY;
    }

    /**
     * An internal copy of the <code>Entity</code> to be codified.
     */
    private Entity entity;

    /**
     * An internal copy of the byte array which contains the codification of an
     * <code>Entity</code>. From this variable, the information used to
     * decodify an <code>Entity</code> is read from.
     */
    private byte[] encoded;

    /**
     * Constructs a new instance of an <code>Asn1Entity</code> class from a
     * byte array. The byte array received as argument can be later decodified
     * into an <code>Entity</code>.
     * 
     * @param encoded
     *            A byte array containing the codified information of an
     *            <code>Entity</code>.
     */
    public Asn1Entity(byte[] encoded) {
        byte[] copy = new byte[encoded.length];
        System.arraycopy(encoded, 0, copy, 0, copy.length);
        this.encoded = copy;
    }

    /**
     * Constructs a new instance of an <code>Asn1Entity</code> class from an
     * <code>Entity</code>. The <code>Entity</code> received as argument
     * can be then codified into a byte array.
     * <p>
     * The value received as argument should not be null. If so, a
     * <code>NullPointerException</code> is thrown.
     * 
     * @param entity
     *            The <code>Entity</code> to be codified.
     */
    public Asn1Entity(Entity entity) {
        if (entity == null) {
            throw new NullPointerException();
        }
        this.entity = entity;
    }

    /**
     * Returns the representation of an <code>Entity</code> in ASN.1
     * codification.
     * <p>
     * If the <code >Asn1Entity</code> object was created with an
     * <code>Entity</code>, then the <code>Entity</code> is codified and
     * returned as a byte array. On the other hand, if the instance was created
     * using a byte array, then no codification process is made.
     * 
     * @return If at construction time an <code>Entity</code> was given, its
     *         representation in ASN.1 codification. If at construction time a
     *         byte array was given, a copy of the same array is returned.
     */
    public byte[] getEncoded() {
        if (encoded == null) {
            return ASN1_ENTITY.encode(entity);
        } else {
            byte[] copy = new byte[encoded.length];
            System.arraycopy(encoded, 0, copy, 0, copy.length);
            return copy;
        }
    }

    /**
     * Returns the <code>Entity</code> obtained from the decodification of an
     * ASN.1 codified byte array.
     * <p>
     * If the <code>Asn1Entity</code> was created giving an
     * <code>Entity</code>, a reference to the same <code>Entity</code> is
     * obtained. Otherwise, the byte array given at construction time is
     * decodificated into a new <code>Entity</code> object.
     * 
     * @return If at construction time an <code>Entity</code> was given, the
     *         same <code>Entity</code> is returned. If at construction time a
     *         byte array was given, an <code>Entity</code> constructed with
     *         the information stored in that byte array is returned.
     * @throws IOException
     *             If the decodification process could not be carried out
     *             properly.
     */
    public Entity getEntity() throws IOException {
        if (entity == null) {
            return (Entity) ASN1_ENTITY.decode(encoded);
        } else {
            return entity;
        }
    }
}
