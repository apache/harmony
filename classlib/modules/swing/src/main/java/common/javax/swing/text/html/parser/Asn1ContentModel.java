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
import java.util.Collection;

import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1SequenceOf;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;

/**
 * It implements ASN.1 codification tools for
 * <code>javax.swing.text.html.parser.ContentModel</code>. Given a
 * <code>ContentModel</code>, its values are codified in ASN.1 according to
 * the following rule:
 * 
 * <pre>
 * HTMLContentModel ::= SEQUENCE OF SEQUENCE {
 *      Type INTEGER,
 *      Index INTEGER
 * }
 * </pre>
 * 
 * The structure used to store the <code>ContentModel</code> is a sort of
 * binary tree that imitate the internal structure of a
 * <code>ContentModel</code>.
 * <p>
 * As you may know, a <code>ContentModel</code> can be represented by a binary
 * tree where each node denotes one of the following possible values:
 * <ul>
 * <li> A binary relation between its children.
 * <li> A unary relation applied only to its left child.
 * <li> An element.
 * <li> A null value.
 * </ul>
 * <p>
 * So, depending on each of those possible values, a different node is created
 * to summarize that information. We will denote each node as a pair of (type,
 * index). Therefore, according to the representation of a ContentModel,we have
 * the following conversions:
 * <p>
 * 
 * <b>CASE 1: A binary relation between its children</b>
 * 
 * <pre>
 *                    B
 *                   /               =&gt;         [B,-1]
 *                 C1   
 *                  \
 *                   C2
 *                    \
 *                    ...
 *                      \
 *                      Cn
 *                 
 *                 
 * </pre>
 * 
 * 
 * <b>CASE 2: A unary relation applied inly to its left child</b>
 * 
 * <pre>
 *                    U
 *                   /               =&gt;         [U,-1]
 *                 C1    
 * </pre>
 * 
 * 
 * <b>CASE 3: An element</b>
 * 
 * <pre>
 *                   ELEM            =&gt;         [-2, ELEM.getIndex()]
 * </pre>
 * 
 * 
 * <b>CASE 4: A null value</b>
 * 
 * <pre>
 *                   NULL            =&gt;         [-1,-1]  
 * </pre>
 * 
 * 
 * 
 * For example, lets take the <code>ContentModel</code>'s tree for the HEAD
 * <code>Element<code>. The <code>ContentModel</code> is defined as:
 * 
 * <pre>
 *              TITLE &amp; ISINDEX? &amp; BASE?
 * </pre>
 * 
 * And the <code>ContentModel</code> tree for this case is then:
 * 
 * <pre>
 *                                      &amp;
 *                                      |
 *                    +-----------------+-----------------+
 *                    |                                   |
 *                    0                                 NULL
 *          +---------+---------+
 *          |                   |
 *        TITLE                 ?
 *                    +---------+---------+
 *                    |                   |
 *                    0                   ?
 *               +----+----+         +----+----+
 *               |         |         |         |
 *            ISINDEX    NULL        0       NULL
 *                             +-----+-----+
 *                             |           |
 *                            BASE       NULL
 *          
 * </pre>
 * 
 * Then, this representation translated into our tree representation looks like:
 * 
 * 
 * <pre>
 *                                  ['&amp;',-1]
 *                                      |
 *                    +-----------------+-----------------+
 *                    |                                   |
 *                  [0,-1]                             [-1,-1]
 *          +---------+---------+
 *          |                   |
 *      [-2,TITLE]          ['?',-1]
 *                    +---------+---------+
 *                    |                   |
 *                  [0,-1]            ['?',-1]
 *               +----+----+         +----+----+
 *               |         |         |         |
 *          [-2,ISINDEX][-1,-1]    [0,-1]   [-1,-1]
 *                             +-----+-----+
 *                             |           |
 *                         [-2,BASE]    [-1,-1]
 * </pre>   
 * 
 * So then, this simpler tree can be stored as a sequence of pairs (type,index),
 * and reconstructing it again is straightforward if both, the storage and
 * recovery of information, are made is BSF mode.
 * <p>
 * Then, this tree will be represented by the sequence:
 * <p> 
 * ['&',-1] , [0,-1], [-1,-1], [-2,TITLE], ['?',-1], [0,-1], ['?',-1],
 * [-2,ISINDEX], [-1,-1], [0,-1], [-1,-1], [-2,BASE], [-1,-1]
 * <p> 
 * And the relation among nodes can be restored if we consider that if the
 * sequence is numerated from 0, we maintain the number of processed
 * <tt>relation nodes (rn)</tt> and we read the stored nodes in order, for any
 * relational node, its sons are stored ad positions 2*rn+1 and 2*rn+2.
 * 
 * The class can be used to obtain a byte array representing the codification of
 * a <code>ContentModel</code>, as well as a <code>ContentModel</code> from
 * a byte array (previously obtained codifying a <code>ContentModel</code>).
 * In fact, it serves as a wrapper for the codification and the
 * <code>ContentModel</code> itself.
 * 
 */
class Asn1ContentModel {

    /**
     * It stores the definition of a <code>ContentModel</code> as an ASN.1
     * valid type according to the ASN.1 framework.
     */
    private static ASN1Type ASN1_CONTENT_MODEL;

    /**
     * The definition of the ASN1_CONTENT_MODEL type according to the rule
     * defined for a <code>ContentModel</code>. It also defines a custom
     * encoder/decoder for this type.
     */
    static {
        ASN1_CONTENT_MODEL = new ASN1SequenceOf(Asn1ModelElement.getInstance()) {

            /**
             * Overrided method used to decodified the information that
             * represents a <code>ContentModel</code>. It makes a completely
             * new <code>ContentModel</code> with the information interpreted
             * from the stream.
             * <p>
             * As at this point no information about other <code>Element</code>s
             * is available, a partial empty <code>Element</code> is used to
             * denote the presence of other elements in the
             * <code>ContentModel</code>.
             * 
             * @param in
             *            The <code>BerInputStream</code> where the
             *            codificated information will be read from.
             * @return A <code>ContentModel</code> filled with the information
             *         read from the stream.
             */
            public Object getDecodedObject(BerInputStream in) {
                Object values = in.content;

                int pos = 0;
                ArrayList mLst = (ArrayList) values;
                ArrayList<Object> queue = new ArrayList<Object>();
                ContentModel rootModel = (ContentModel)ModelElement.makeContent(
                                            (ModelElement)mLst.get(0));
                ContentModel actualModel = null;
                if (rootModel != null) {
                    queue.add(rootModel);
                }

                while (!queue.isEmpty()) {
                    actualModel = (ContentModel)queue.remove(0);
                    actualModel.content = ModelElement.makeContent(
                                                (ModelElement)mLst
                            .get(pos * 2 + 1));
                    actualModel.next = (ContentModel)ModelElement.makeContent(
                                                (ModelElement)mLst
                            .get(pos * 2 + 2));
                    if ( actualModel.content instanceof ContentModel) {
                        queue.add(actualModel.content);
                    }
                    if (actualModel.next != null) {
                        queue.add(actualModel.next);
                    }
                    pos++;
                }

                return rootModel;
            }

            /**
             * Overrided method used to codify the information stored in an
             * <code>ContentModel</code> into an array of bytes, according to
             * its ASN.1 specification.
             * 
             * @param object
             *            The object where the information to be codified is
             *            stored. It actually consists of an
             *            <code>Asn1ContentModel</code> object which contains
             *            the <code>ContentModel</code> to be codified.
             * 
             * @return A collection with the information of the nodes that
             *         compose the <code>ContentModel</code>, ready to be
             *         codified.
             */
            public Collection getValues(Object object) {
                ArrayList<Object> queue = new ArrayList<Object>();
                ArrayList<ModelElement> mLst = new ArrayList<ModelElement>();
                Asn1ContentModel asn1 = (Asn1ContentModel) object;
                ContentModel model = asn1.getWrappedContentModel();
                
                mLst.add(new ModelElement(model));
                if (model != null) {
                    queue.add(model);
                }
                while (!queue.isEmpty()) {
                    model = (ContentModel)queue.remove(0);
                        mLst.add(new ModelElement(
                                        (model.content)));
                        mLst.add(new ModelElement(model.next));
                        if ((model.content) instanceof ContentModel) {
                            queue.add(model.content);
                        }
                        if (model.next != null) {
                            queue.add(model.next);
                        }
                }
                
                return mLst;
            }
        };
    }

    /**
     * It returns an <code>ASN1Type</code> value that contains the ASN.1
     * codification rules for a <code>ContentModel</code> with its encoder and
     * decoder.
     * <p>
     * Among other things, this method can be used to declare the types of new
     * fields in other structures, as for example, in an
     * <code>Asn1Element</code>.
     * 
     * @return The value that defines an ASN.1 <code>ContentModel</code>
     *         representation with its encoder/decoder.
     */
    static ASN1Type getInstance() {
        return ASN1_CONTENT_MODEL;
    }

    /**
     * An internal copy of the <code>ContentModel</code> to be codified.
     */
    private ContentModel contentModel;

    /**
     * An internal copy of the byte array which contains the codification of an
     * <code>ContentModel</code>. From this variable, the information used to
     * decodify a <code>ContentModel</code> is read from.
     */
    private byte[] encoded;

    /**
     * Constructs a new instance of an <code>Asn1ContentModel</code> class
     * from a byte array. The byte array received as argument can be later
     * decodified into a <code>ContentModel</code>.
     * 
     * @param encoded
     *            A byte array containing the codified information of a
     *            <code>ContentModel</code>.
     */
    public Asn1ContentModel(byte[] encoded) {
        byte[] copy = new byte[encoded.length]; 
        System.arraycopy(encoded, 0, copy, 0, copy.length);
        this.encoded = copy;
    }

    /**
     * Constructs a new instance of an <code>Asn1ContentModel</code> class
     * from a <code>ContentModel</code>. The <code>ContentModel</code>
     * received as argument can be then codified into a byte array.
     * 
     * @param contentModel
     *            The <code>ContentModel</code> to be codified.
     */
    public Asn1ContentModel(ContentModel contentModel) {
        this.contentModel = contentModel;
    }

    /**
     * Returns the representation of a <code>ContentModel</code> in ASN.1
     * codification.
     * <p>
     * If the <code >Asn1ContentModel</code> object was created with a
     * <code>ContentModel</code>, then the <code>ContentModel</code> is
     * codified and returned as a byte array. On the other hand, if the instance
     * was created using a byte array, then no codification process is made.
     * 
     * @return If at construction time a <code>ContentModel</code> was given,
     *         its representation in ASN.1 codification. If at construction time
     *         a byte array was given, a copy of the same array is returned.
     */
    public byte[] getEncoded() {
        if (encoded == null) {
            return ASN1_CONTENT_MODEL.encode(contentModel);
        } else {
            byte[] copy = new byte[encoded.length]; 
            System.arraycopy(encoded, 0, copy, 0, copy.length);
            return copy;
        }
    }

    /**
     * Returns the <code>ContentModel</code> obtained from the decodification
     * of an ASN.1 codified byte array.
     * <p>
     * If the <code>Asn1ContentModel</code> was created giving an
     * <code>ContentModel</code>, a reference to the same
     * <code>ContentModel</code> is obtained. Otherwise, the byte array given
     * at construction time is decodificated into a new
     * <code>ContentModel</code> object.
     * 
     * @return If at construction time a <code>ContentModel</code> was given,
     *         the same <code>ContentModel</code> is returned. If at
     *         construction time a byte array was given, a
     *         <code>ContentModel</code> constructed with the information
     *         stored in that byte array is returned.
     * @throws IOException
     *             If the decodification process could not be carried out
     *             properly.
     */
    public ContentModel getContentModel() throws IOException {
        if (contentModel == null) {
            return (ContentModel) ASN1_CONTENT_MODEL.decode(encoded);
        } else {
            return contentModel;
        }
    }

    /**
     * Returns the <code>ContentModel</code> wrapped into the
     * <code>Asn1ContentModel</code> object.
     * 
     * @return The wrapped <code>ContentModel</code>, if one was used at
     *         construction time, or null if the object was created using a byte
     *         array.
     */
    public ContentModel getWrappedContentModel() {
        return contentModel;
    }
}

/**
 * It implements the elements that compose the sequence that defines a
 * <code>ContentModel</code>.
 * <p>
 * When the ASN.1 representation for the <code>ContentModel</code> was given,
 * it was defined as a "SEQUENCE OF" some structures, thus the purpose of this
 * class is to define the "SEQUENCE" that implements that structures. Such
 * structures will be denoted by the name <code>ModelElement</code>.
 * <p>
 * When defined the codification of a <code>ContentModel</code>, its tree
 * structure was established. This class just defines the node's structure of
 * such tree.
 */
class ModelElement {

    /**
     * It stores the type of the node.
     */
    private int type;

    /**
     * It stores the index of the <code>Element</code> that codify the node.
     */
    private int index;

    /**
     * Sets the values of the node.
     * 
     * @param type
     *            The type set to the node
     * @param index
     *            The index of the <code>Element</code> that the node is
     *            codifying.
     */
    private void setValues(int type, int index) {
        this.type = type;
        this.index = index;
    }

    /**
     * Returns the type of the node.
     * 
     * @return The integer representation of the symbol that denote the unary
     *         ('*', '+', '?') or binary ('|', '&', ',') relation that relates
     *         the two children of this node. 0 if the node represents an
     *         <code>Element</code> or -1 if the node represents a null value.
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the index of the <code>Element</code> that the node is
     * codifying.
     * 
     * @return The index of the codified <code>Element</code> or -1 if no
     *         <code>Element</code> is codified.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Constructs a new <code>ModelElement</code> given a type and index
     * value.
     * 
     * @param type
     *            The node type. Any <code>int</code> value is possible.
     * @param index
     *            The node index. Any <code>int</code> value is possible.
     */
    public ModelElement(final int type, final int index) {
        setValues(type, index);
    }

    /**
     * Constructs a new <code>ModelElement</code> given a
     * <code>ContentModel</code>. The information used to define the
     * <code>ModelElement</code> is retrieved from the
     * <code>ContentModel</code>.
     * 
     * @param obj
     *            The <code>ContentModel</code> from where the information
     *            used to define the <code>ModelElement</code> is retrieved.
     */
    public ModelElement(Object obj) {
        if (obj == null) {
            setValues(-1, -1);
        } else {
            if (obj instanceof Element) {
                setValues(-2, ((Element)obj).index);
            } else {
                setValues(((ContentModel)obj).type, -1);
            }
        }
    }

    /**
     * Constructs a <code>ContentModel</code> using the information stored
     * into a <code>ModelElement</code>. If the <code>ModelElement</code>
     * denoted an <code>Element</code>, an <code>Element</code> with only
     * its index and a suitable name will be returned.
     * 
     * @param me
     *            The <code>ModelElement</code> from where the information to
     *            construct the <code>ContentModel</code> is retrieved.
     * @return A <code>ContentModel</code> or <code>Element</code> 
     *         initialized with the information
     *         stored in the <code>ModelElement<code> given as argument.
     */
    public static Object makeContent(ModelElement me) {
        Object obj;
        switch (me.getType()) {
        case -1:
            obj = null;
            break;
        case -2:
            obj = new Element(me.getIndex(), "ELEMENT "
                    + me.getIndex(), false, false, null, null, 1, null, null,
                    null);
            break;
        default:
            obj = new ContentModel();
            ((ContentModel)obj).type = me.getType();
            break;
        }
        return obj;
    }
}

/**
 * It implements ASN.1 codification tools for the nodes that conforms the
 * <code>ContentModel</code> tree representation.
 * <p>
 * The class can be used to obtain a byte array representing the codification of
 * a <code>ModelElement</code>, as well as a <code>ModelElement</code> from
 * a byte array (previously obtained codifying a <code>ModelElement</code>).
 */
class Asn1ModelElement {

    /**
     * It stores the definition of a <code>ModelElement</code> as an ASN.1
     * valid type according to the ASN.1 framework.
     */
    private static ASN1Type ASN1_MODEL_ELEMENT;

    /**
     * It returns an <code>ASN1Type</code> value that contains the ASN.1
     * codification rules for a <code>ModelElement</code> with its encoder and
     * decoder.
     * <p>
     * Among other things, this method can be used to declare the types of new
     * fields in other structures, as for example, in an
     * <code>Asn1ContentModel</code>.
     * 
     * @return The value that defines an ASN.1 <code>ModelElement</code>
     *         representation with its encoder/decoder.
     */
    static ASN1Type getInstance() {
        return ASN1_MODEL_ELEMENT;
    }

    /**
     * The definition of the ASN1_MODEL_ELEMENT type according to the rule
     * defined for a <code>ModelElement</code>. It also defines a custom
     * encoder/decoder for this type.
     */
    static {
        ASN1_MODEL_ELEMENT = new ASN1Sequence(new ASN1Type[] {
                ASN1Integer.getInstance(), // Type
                ASN1Integer.getInstance() // Index
                }) {

            /**
             * Overrided method used to decodified the information that
             * represents a <code>ModelElement</code>. It makes a completely
             * new <code>ModelElement</code> with the information interpreted
             * from the stream.
             * 
             * @param in
             *            The <code>BerInputStream</code> where the
             *            codificated information will be read from.
             * @return A <code>ModelElement</code> filled with the information
             *         read from the stream.
             */
            public Object getDecodedObject(BerInputStream in) {
                Object values[] = (Object[]) in.content;

                int type = new BigInteger((byte[]) values[0]).intValue();
                int index = new BigInteger((byte[]) values[1]).intValue();

                return new ModelElement(type, index);
            }

            /**
             * Overrided method used to codify the information stored in an
             * <code>ModelElement</code> into an array of bytes, according to
             * its ASN.1 specification.
             * 
             * @param object
             *            The object where the information to be codified is
             *            stored. It actually consists of a
             *            <code>ModelElement</code>.
             * 
             * @param values
             *            An array of objects where the model node's type and
             *            index information will be stored, ready for
             *            codification.
             */
            public void getValues(Object object, Object values[]) {
                ModelElement me = (ModelElement) object;

                values[0] = BigInteger.valueOf(me.getType()).toByteArray();
                values[1] = BigInteger.valueOf(me.getIndex()).toByteArray();
            }
        };
    }
}