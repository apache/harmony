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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/**
 * 
 * {@link Element}s content representation. That's unary or binary expression.
 * Operands can be null (matched with null object), instance of {@link Element},
 * instance of {@link ContentModel}.
 * <p>
 * Valid operations can be unary operations (types):
 * <ol>
 * <li> '+' - (e+) - e must occur one or more times;
 * <li> '*' - (e*) - e must occur zero or more times;
 * <li> '?' - (e?) - e must occur zero or one time;
 * <li> (0) - (e) - e must occur one time only
 * </ol>
 * <p>
 * and binary operations (types):
 * <ol>
 * <li> '|' - (e1|e2) means either e1 or e2 must occur, but not both;
 * <li> ',' - (e1,e2) means both A and B must occur, in that order;
 * <li> '&' - (e1 & e2) means both e1 and e2 must occur, in any order;
 * </ol>
 * (Operation interpretation corresponds to HTML 4.01 Specification (3.3.3))
 * <p>
 * As content model is using for dtd presentation here is some ambiguity what
 * null object can be matched with. So null shouldn't be passed to constructor.
 * <p>
 * No recursion is allowed.
 * <p>
 * Content, next, type fields has the following limitation:
 * <ol>
 * <li> if type is one from {'+', '*', '?'} content hasn't to be null and next
 * can be not null, if a binary operation is applyed to them;
 * <li> if type is one from {'|', ',', '&'} content hasn't to be null and next
 * must be null;
 * <li> content can be null, instance of Element or instance of ContentModel;
 * <li> type can be one from the following '*', '+', '?', '|', '&', ','.
 * </ol>
 * 
 * <p>
 * The structure of a {@link ContentModel} is represented by a relation through
 * its {@link ContentModel#content} and {@link ContentModel#next} fields. Using
 * these fields and the information stored in the {@link ContentModel#type}
 * field a {@link ContentModel} can be represented as a binary tree.
 * <p>
 * From now on, in the examples that will follow, we will consider the left
 * branch and the one belonging to the {@link ContentModel#content} field and
 * the right branch the {@link ContentModel#next} field.
 * <p>
 * Depending on the {@link ContentModel#type} of a {@link ContentModel}, the
 * following cases may arise:
 * <p>
 * <b>CASE 1: A binary relation over some {@link ContentModel}s:</b>
 * 
 * <pre>
 *                                 B
 *                                / \ 
 *                              C1  NULL
 *                             /  \
 *                                C2
 *                               /  \
 *                                  ...
 *                                 /  \
 *                                    Cn
 *                                   /  \
 *                                     NULL
 * </pre>
 * 
 * Where the binary operation <b>B</b> is applyed to all the
 * {@link ContentModel}s C1, C2, ..., Cn (that is a sequence of
 * {@link ContentModel} related by the {@link ContentModel#next} field,
 * finishing in a null value). This means that this {@link ContentModel}
 * represents the content model:
 * 
 * <pre>
 *                         (C1 B C2 B ... B Cn)
 * </pre>
 * 
 * Here, obviously the <b>B</b> operator can be one of:
 * <ul>
 * <li> |
 * <li> &
 * <li> ,
 * </ul>
 * 
 * <b>CASE 2: A unary relation applied to one {@link ContentModel}</b>
 * 
 * <pre>
 *                                 U
 *                                / \
 *                              C1  NULL   
 * </pre>
 * 
 * Where the unary operator <b>U</b> is only applyed to the
 * {@link ContentModel} C1. This means that this {@link ContentModel} represents
 * the content model:
 * 
 * <pre>
 *                                 C1 U
 * </pre>
 * 
 * Here, obviously the <b>U</b> operator can be one of:
 * <ul>
 * <li> +
 * <li> *
 * <li> ?
 * <li> 0
 * </ul>
 * 
 * <b>CASE 3: An element</b>
 * 
 * <pre>
 * ELEM
 * </pre>
 * 
 * Where this is only an instance of a {@link Element} class and obviosly
 * denotes a {@link Element} of the {@link ContentModel}. An important fact to
 * remark is that a {@link ContentModel} may not be just an {@link Element}, it
 * must be applyed to at least one unary operator, usually the 0 operator.
 * <p>
 * This means that if we want to represent the <code>body</code>
 * {@link ContentModel}, the {@link ContentModel} will be denoted by:
 * 
 * <pre>
 *                                  0
 *                            +-----+-----+
 *                            |           |              
 *                          BODY         NULL
 * </pre>
 * 
 * <b>CASE 4: A null value</b>
 * 
 * <pre>
 * NULL
 * </pre>
 * 
 * The empty or null {@link ContentModel} is denoted by this value. It is also
 * used to denote the end of a sequence of {@link ContentModel} (as seen in the
 * CASE 1).
 * 
 * <p>
 * As an example, if we want to represent the content model denoted by the
 * expression:
 * 
 * <pre>
 *                          ((E1? , E2)* &amp; E3)+
 * </pre>
 * 
 * The {@link ContentModel} will be denoted by:
 * 
 * <pre>
 *   
 *  
 *                                                          '+'
 *                                                 +---------+---------+
 *                                                 |                   |
 *                                                '&amp;'                 NULL
 *                                       +---------+---------+
 *                                       |                   |
 *                                      '*'                 NULL
 *                             +---------+---------+
 *                             |                   |
 *                            '|'                 '0'
 *                      +------+------+     +------+------+
 *                      |             |     |             |
 *                     '?'           NULL   E4           NULL
 *            +---------+---------+
 *            |                   |
 *           '0'                 '0'
 *     +------+------+     +------+------+
 *     |             |     |             |
 *     E1           NULL   E2           '+'
 *                                +------+------+
 *                                |             |
 *                               '0'           NULL
 *                          +-----+-----+
 *                          |           |
 *                          E3         NULL
 * </pre>
 * 
 */

public final class ContentModel implements Serializable {

    /**
     * The serialization UID value.
     */
    private static final long serialVersionUID = -1130825523866321257L;
    
    /**
     * The type of the content model. It should be '*', '+', '?', '|', '&', ','
     * or 0.
     */
    public int type;

    /**
     * The content of the ContentModel.
     */
    public Object content;

    /**
     * The next ContentModel in the ContentModel structure.
     */
    public ContentModel next;

    /**
     * The symbols representing an obligatory single occurence of an expression.
     */
    private static final char DEFAULT_TYPE = 0;

    /**
     * The symbols representing that an expression must occur at least one time.
     */
    private static final char PLUS_TYPE = '+';

    /**
     * The symbols representing that an expression can occur zero or more times.
     */
    private static final char STAR_TYPE = '*';

    /**
     * The symbols representing that an expression must occur zero or one time.
     */
    private static final char QUESTION_TYPE = '?';

    /**
     * The symbols representing that any of the expressions related by this
     * operator must occur.
     */
    private static final char LINE_TYPE = '|';

    /**
     * The symbols representing that all the expressions in the given order must
     * occur.
     */
    private static final char COMMA_TYPE = ',';

    /**
     * The symbols representing that all the expressions in any order must
     * occur.
     */
    private static final char AMPER_TYPE = '&';

    /**
     * Returns a new {@link ContentModel}. The {@link ContentModel#type},
     * {@link ContentModel#content} and {@link ContentModel#next} fields are
     * filled with the information given as argument.
     */
    public ContentModel(final int type, final Object content,
            final ContentModel next) {
        this.type = type;
        this.content = content;
        this.next = next;
    }

    /**
     * Returns a new {@link ContentModel}. The {@link ContentModel#type} and
     * {ContentModel#content} fields are filled with the information given
     * throough the arguments. The {@link ContentModel#next} field is set to
     * null.
     */
    public ContentModel(final int type, final ContentModel content) {
        this.type = type;
        this.content = content;
    }

    /**
     * That content model will be mathed with exactly one element. Type will be
     * 0. {@link Element} can be equals to null. In such case
     * {@link ContentModel} will be matched with an empty input stream.
     */
    public ContentModel(final Element content) {
        this.content = content;
    }

    /**
     * Returns a {@link ContentModel} with its {@link ContentModel#type} field
     * set to 0 and its {@link ContentModel#content} and
     * {@link ContentModel#next} fields set to null.
     */
    public ContentModel() {
    }

    /**
     * Returns a representation of the {@link ContentModel} converted to a
     * string.
     * 
     * @return a String representing the {@link ContentModel}
     */
    public String toString() {
        String str = new String();

        try {
            if (type == DEFAULT_TYPE && content instanceof Element) {
                    str = str + ((Element) content).getName();
            } else {
                if (type == PLUS_TYPE || type == STAR_TYPE || type == QUESTION_TYPE) {
                    str = content + String.valueOf((char) type);
                } else if (isBinaryOperator(type)) {
                    ContentModel auxModel = (ContentModel) content;
                    while (auxModel != null) {
                        str = str + auxModel;
                        if (auxModel.next != null) {
                            str = str + " " + String.valueOf((char) type) + " ";
                        }
                        auxModel = auxModel.next;
                    }
                    str = "(" + str + ")";
                } else {
                    str = content.toString();
                }
            }
            return str;
        } catch (ClassCastException e) {
            throw new ClassCastException (content.getClass().getName());
        }
    }

    private boolean isBinaryOperator(final int type) {
        return (type == COMMA_TYPE || type == LINE_TYPE || type == AMPER_TYPE);
    }

    /**
     * Returns the {@link Element} that must be first in the
     * {@link ContentModel}.
     * 
     * @return The first element that may appear in the {@link ContentModel}.
     *         Null if there is more than one possible {@link Element}.
     */
    public Element first() {
        Element element;
        
        try {
            if (type == STAR_TYPE || type == QUESTION_TYPE || type == LINE_TYPE
                    || type == AMPER_TYPE) {
                element = null;
            } else if (type == PLUS_TYPE || type == COMMA_TYPE) {
                element = ((ContentModel) content).first();
            } else {
                element = (Element) content;
            }
            return element;
        } catch (ClassCastException e) {
            throw new ClassCastException (content.getClass().getName());
        }
    }

    /**
     * Returns if a given token can occur as first elements in a
     * {@link ContentModel}
     * 
     * @param token
     *            the element we are interested in determining whether it can
     *            occur as the first element of a {@link ContentModel}
     * 
     * @return
     * <ul>
     * <li> if type equals to 0, returns true if and only if token equals to
     * content.
     * <li> if type is one from the unary types returns true if and only if one
     * of the following conditions is true:
     * <ol>
     * <li> content is instance of {@link Element}, token is instance of
     * {@link Element} and token equals to content
     * <li> content is instance of {@link ContentModel}, token is instance of
     * {@link Element} and content.first(token) returns true;
     * </ol>
     * <li> if type is one from binary types then:
     * <ol>
     * <li> if content instance of {@link Element} and content equals to token
     * returns true;
     * <li> if content instance of {@link ContentModel} and content.first(token)
     * equals to true, then returns true;
     * <li> if type equals to ',' returns true if and only if content is
     * instance of {@link ContentModel} and:
     * <ul>
     * <li> for at least one of the {@link ContentModel} related by the ',',
     * first(token) is true and,
     * <li> for all the {@link ContentModel}s that preceded it, empty() is
     * true.
     * </ul>
     * <li> if type equals to '| or '&', it returns true if and only if at least
     * one of {@link ContentModel}s related by the '|' or '&' operator
     * satisfies that first(token) is true.
     * </ol>
     * </ul>
     */ 
    public boolean first(Object token) {
        boolean found = false;
        boolean maybeNext = true;
        ContentModel auxModel;

        if (type == COMMA_TYPE) {
            auxModel = (ContentModel) content;
            while (auxModel != null && maybeNext) {
                found = auxModel.first(token);
                maybeNext = !found && auxModel.empty();
                auxModel = auxModel.next;
            }
        } else if (type == LINE_TYPE || type == AMPER_TYPE) {
            found = ((Element) token).equals(content);
            auxModel = (ContentModel) content;
            while (auxModel != null && !found) {
                found = token.equals(content) || auxModel.first((Element)token);
                auxModel = auxModel.next;
            }

        } else if (type == PLUS_TYPE || type == STAR_TYPE
                || type == QUESTION_TYPE) {
            found = ((ContentModel) content).first(token);
        } else {
            found = content == token;
        }

        return found;
    }

    /**
     * Adds all elements of this contentModel to elemVec ignoring operations
     * between elements. For instance, for ((a+)| ((b*),(c?))) elements a,b,c
     * will be added to the elemVec. The argument elemVec should not be null.
     * If content is null, nothing will be added to elemVec.
     * 
     * @param elemVec
     *            the vector where the {@link Element}s of the
     *            {@link ContentModel} will be added to.
     * 
     */
    public void getElements(final Vector<Element> elemVec) {
        try {

            if (type == LINE_TYPE || type == AMPER_TYPE || type == COMMA_TYPE) {
                ContentModel auxModel = (ContentModel)content;
                while (auxModel != null) {
                    auxModel.getElements(elemVec);
                    auxModel = auxModel.next;
                }
            } else if (type == PLUS_TYPE || type == STAR_TYPE || type == QUESTION_TYPE) {
                ((ContentModel) content).getElements(elemVec);
            } else {
                elemVec.add((Element)content);
            }                     

        } catch (ClassCastException e) {
            throw new ClassCastException (content.getClass().getName());
        }
    }

    /**
     * Checks if the {@link ContentModel} can match an empty expression.
     * 
     * @return true if and only if some of the conditions is true:
     *         <ul>
     *         <li> type equals to '*' or '?';
     *         <li> type equals to '|' and one of the {@link ContentModel}s
     *         related by the binary operator can be empty.
     *         <li> type equals to '&' or ',' and all the {@link ContentModel}s
     *         related by the binary operation can be empty.
     *         </ul>
     *         <p>
     *         If the type equals '+', then it returns true if the
     *         {@link ContentModel} applied to this operator can be empty;
     *         <p>
     *         If the type equals '0' then it returns false.
     */
    public boolean empty() {
        boolean found = false;
        ContentModel auxModel;

        try {
            if (type == PLUS_TYPE || type == LINE_TYPE) {
                auxModel = (ContentModel) content;
                while (auxModel != null && !found) {
                    found = auxModel.empty();
                    auxModel = auxModel.next;
                }
            } else if (type == AMPER_TYPE || type == COMMA_TYPE) {
                auxModel = (ContentModel) content;       
                found = true;
                while (auxModel != null && found) {
                    found = auxModel.empty();
                    auxModel = auxModel.next;
                }
            } else {
                found = (type == STAR_TYPE || type == QUESTION_TYPE);
            }
            return found;
        } catch (ClassCastException e) {
            throw new ClassCastException (content.getClass().getName());
        }
    }
    
    
    /**
     * Determines the sequence of {@link Element} needed to be implied to
     * insert an {@link Element}.
     * 
     * @param e
     *            the {@link Element} found in the document are for which the
     *            implication is needed.
     * @param parsed
     *            the {@link ArrayList} of {@link Element}s found previosly to
     *            the {@link Element} e.
     * @param many
     *            a value specifyng whether the given {@link Element} may appear
     *            more than once in the {@link ContentModel}
     * @param depth
     *            the depth level been searched in the {@link ContentModel}
     * @param path
     *            a possible path of implied {@link Element}s that may leed to
     *            the {@link Element} e.
     * @return 
     *          <ol>
     *          <li> null, if an implication path to the element e could not be found.
     *          <li> an empty {@link List} if the element may be inserted
     *               in the actual position, with no implication of other elements needed.
     *          <li> a non empty {@link List} if some {@link Element}s need to be implied.
     *               In such case, the {@link List} is formed by a pair. The first
     *               component defines the {@link Element} needed to be implied. The
     *               second component defines if the {@link Element} must be opened and
     *               closed (if true) or just opened (false). 
     *          </ol>
     */
    List<Pair<Element,Boolean>> implication(
            final Element e,
            final List<Element> parsed,
            final boolean many,
            final int depth,
            final LinkedList<Pair<Element, Boolean>> path) {
        
        ContentModel auxModel;
        List <Pair<Element,Boolean>> implied = null;

        if (content instanceof Element) {
            Element currentElement =(Element)content;
            if (e.equals(currentElement) && (!parsed.contains(e) || many)) {
                implied = new LinkedList<Pair<Element,Boolean>>(path);
            } else if (currentElement.inclusions != null
                        && e.getIndex() > 0
                        && currentElement.inclusions.get(e.getIndex())) {
                implied = new LinkedList<Pair<Element,Boolean>>(path);
                implied.add(new Pair<Element,Boolean>(currentElement, Boolean.FALSE));
            } else if (depth > 1
                    && !currentElement.hasRequiredAttributes()
                    && !currentElement.isEmpty()
                    && !currentElement.isScript()
                    && !parsed.contains(currentElement)
                    && currentElement.getContent() != null) {
                LinkedList<Pair<Element, Boolean>> newPath = (LinkedList)path.clone();
                newPath.add(new Pair<Element, Boolean>(currentElement, new Boolean(false)));
                implied = currentElement.getContent().implication(e, parsed, many, depth-1, newPath);
            }
        } else if (type == STAR_TYPE || type == PLUS_TYPE) {
            if (content != null) {
                implied = ((ContentModel)content).implication(e, parsed, true, depth, path);
            }
        } else if (type == COMMA_TYPE) {
            auxModel = (ContentModel) content;
            while (auxModel != null && implied == null) {
                implied = auxModel.implication(e, parsed, many, depth, path);                   
                if (implied == null && !auxModel.empty()) {
                    path.add(new Pair<Element, Boolean>(auxModel.first(),
                            new Boolean(true)));
                }
                auxModel = auxModel.next;
            }
        } else if (type == LINE_TYPE || type == AMPER_TYPE) {
            auxModel = (ContentModel) content;
            while (auxModel != null && implied == null) {
                implied = auxModel.implication(e, parsed, many, depth, path);
                auxModel = auxModel.next;
            }
        } else {
            if (content != null) {
                implied = ((ContentModel)content).implication(e, parsed, many, depth, path);
            }
        }

        return implied;
    }      
}
