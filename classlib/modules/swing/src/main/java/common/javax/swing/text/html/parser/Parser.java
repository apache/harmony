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
import java.io.Reader;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.swing.text.ChangedCharSetException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Tag;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * This class attempts to read and parse an HTML file, which it gets via an
 * Input Stream. The parsing is based on a Document Type Definition
 * ({@link DTD}), and calls various methods (such as handleError,
 * handleStartTag, etc.) when it finds tags or data. This methods should be
 * overriden in a subclass in order to use the parser.
 */
public class Parser implements DTDConstants {

    /**
     * A reference to the class that dialogates with CUP
     */
    private ParserHandler handler;

    /**
     * An instance of {@link MutableAttributeSet } 
     * returned by {@link Parser#getAttributes() }
     *       method
     */
    private SimpleAttributeSet attributes;

    /**
     * The current position into the document been parsed.
     */
    private int currentStartPos;

    /**
     * The current position into the document been parsed.
     */
    private int currentEndPos;
    
    /**
     * The current line, where the document is been parsed.
     */
    private int currentLine;


    /**
     * The actual {@link DTD} used to parsed the document.
     */
    protected DTD dtd;

    /**
     * Defines whether the parsing of the document is strict or not.
     */
    protected boolean strict;

    /**
     * The key word that identifies a Markup declaration in which the document
     * type is defined.
     */
    private final String DOCTYPE_DECL = "DOCTYPE";

    /**
     * A reference to the Reader used by the parser method to parse a file.
     */
    private Reader file;

    /**
     * An instance of the last Markup declaration found by the parser.
     */
    private HTMLMarkup LastMarkupDecl;

    /**
     * Indicates whether the HTMLTagType is
     * {@link HTMLTagType#SIMPLE}. Is used to give the same behaviour of the RI
     * in the methods {@link Parser#startTag(TagElement)} and
     * {@link Parser#endTag(boolean)}
     */
    private boolean isCurrentTagSimple;

    /*
     * ********************************************************************
     * Public and Protected Methods/Constructor
     * ********************************************************************
     */

    /**
     * Construct a new {@link Parser} using the information stored in a
     * {@link DTD}
     * 
     * @param dtd the {@link DTD} where the information is stored.
     */
    public Parser(final DTD dtd) {
        this.dtd = dtd;
        handler = new ParserHandlerImpl();
    }

    /**
     * Calls method that reports that a closing tag has been found.
     * @param omitted determines whether the end tag may be omitted or not.
     */
    protected void endTag(final boolean omitted) {
        throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
        // XXX: Perhaps depending on the boolean value, an endtag.missing
        // error may be thrown
        //handleEndTag(currentTag);
    }

    /**
     * Reports an error message with only one information field.
     * 
     * @param err the error message.
     */
    protected void error(final String err) {
        error(err,
                HTMLErrorType.DEF_ERROR.toString(),
                HTMLErrorType.DEF_ERROR.toString(),
                HTMLErrorType.DEF_ERROR.toString());
    }

    private void error(final HTMLErrorType errorType) {
        error(errorType.toString(), 
                HTMLErrorType.DEF_ERROR.toString(),
                HTMLErrorType.DEF_ERROR.toString(),
                HTMLErrorType.DEF_ERROR.toString());
    }
    
    /**
     * Reports an error message with two information field
     *  
     * @param err the first part of the message.
     * @param arg1 the second part of the message.
     */
    protected void error(final String err, final String arg1) {
        error(err, 
                arg1, 
                HTMLErrorType.DEF_ERROR.toString(),
                HTMLErrorType.DEF_ERROR.toString());
    }

    /**
     * Reports an error message with two information field
     *  
     * @param errorType the type of the error.
     * @param arg1 the second part of the message.
     */
    private void error(final HTMLErrorType errorType, final String arg1) {
        error(errorType.toString(), 
                arg1, 
                HTMLErrorType.DEF_ERROR.toString(),
                HTMLErrorType.DEF_ERROR.toString());
    }
    
    /**
     * Reports an error message with three information field
     * 
     * @param err the first part of the message.
     * @param arg1 the second part of the message.
     * @param arg2 the third part of the message.
     */
    protected void error(final String err, final String arg1, final String arg2) {
        error(err, 
                arg1, 
                arg2, 
                HTMLErrorType.DEF_ERROR.toString());
    }
    
    /**
     * Reports an error message with three information field
     * 
     * @param errorType The type of the error.
     * @param arg1 the first part of the message.
     * @param arg2 the second part of the message.
     */
    private void error(final HTMLErrorType errorType, final String arg1, final String arg2) {
        error(errorType.toString(), 
                arg1, 
                arg2,
                HTMLErrorType.DEF_ERROR.toString());
    }    

    /**
     * Reports an error message with four information field
     * 
     * @param err the first part of the message.
     * @param arg1 the second part of the message.
     * @param arg2 the third part of the message.
     * @param arg3 the forth part of the message.
     */
    protected void error(final String err, final String arg1,
            final String arg2, final String arg3) {
        handleError(currentStartPos, 
                err + arg1 + arg2 + arg3);
    }

    /**
     * Reports an error message with four information field
     * 
     * @param errorType The type of the error.
     * @param arg1 the first part of the message.
     * @param arg2 the second part of the message.
     * @param arg3 the third part of the message.
     */
    private void error(final HTMLErrorType errorType, final String arg1,
            final String arg2, final String arg3) {
        error(errorType.toString(), 
                arg1, 
                arg2, 
                arg3);
    }
    
    /**
     * Cleans the information stored in the attribute's stack.
     *
     */
    protected void flushAttributes() {
        attributes = new SimpleAttributeSet();
    }

    /**
     * Returns the attributes stored in the attribute's stack.
     * 
     * @return the attributes of the actual attribute's stack.
     */
    protected SimpleAttributeSet getAttributes() {
        return attributes;
    }

    /**
     * Reports the line number where the parser is scanning the parsed file.
     * 
     * @return the actual line number in the document.
     */
    protected int getCurrentLine() {
        return currentLine;
    }

    /**
     * Reports the current position that is being parsed on the document.
     * 
     * @return the actual position into the parsed file.
     */
    protected int getCurrentPos() {
        return currentStartPos;
    }

    /**
     * This method is called when a comment is found in the parsed file.
     * 
     * @param text the text found as comment.
     */
    protected void handleComment(final char[] text) {
    }

    /**
     * This method is called when a simple or empty tag is found in the parsed
     * file.
     * 
     * @param tag the {@link TagElement} that contains the information of the
     *            parsed opening tag.
     * @throws ChangedCharSetException
     */
    protected void handleEmptyTag(final TagElement tag)
            throws ChangedCharSetException {
    }

    /**
     * This method is called when a closing tag is found in the parsed file.
     * 
     * @param tag the {@link TagElement} that contains the information of the
     *            parsed opening tag.
     */
    protected void handleEndTag(final TagElement tag) {
    }

    /**
     * This method is called when the end of the parsed file is found inside
     * a comment.
     *
     */
    protected void handleEOFInComment() {
        throw new UnsupportedOperationException(Messages.getString("swing.9F")); //$NON-NLS-1$
    }

    /**
     * This method is called when an error is found in the parsed file. 
     * 
     * @param ln the line number where the error was found.
     * @param msg an appropiate message for the found error.
     */
    protected void handleError(final int ln, final String msg) {

    }

    /**
     * This method is called when an opening tag, that is not simple or empty,
     * is found in the parsed file.
     * 
     * @param tag the {@link TagElement} that contains the information of the
     *            parsed opening tag.
     */
    protected void handleStartTag(final TagElement tag) {
    }

    /**
     * This method is called when a piece of text is found in the parsed file.
     * 
     * @param text the piece of text found in the document.
     */    
    protected void handleText(final char[] text) {
    }

    /**
     * This method is called when a title is found in the parsed file.
     * 
     * @param text the piece of text found as part of the title of the parsed
     *             file.
     */
    protected void handleTitle(final char[] text) {
    }

    /**
     * Construct a new {@link TagElement} with the information stored into a
     * {@link Element}.
     * 
     * @param elem the {@link Element} that constains the information.
     * @return a new {@link TagElement} that encapsulates the {@link Element}
     *         received as argument. The fictional value is set to false.
     */
    protected TagElement makeTag(final Element elem) {
        return new TagElement(elem);
    }

    /**
     * Construct a new {@link TagElement} with the information stored into a
     * {@link Element}.
     * 
     * @param elem the {@link Element} that constains the information.
     * @param fictional the value stored in the fictional field of the
     * {@link TagElement}.
     * @return a new {@link TagElement} that encapsulates the {@link Element}
     *         received as argument.
     */
    protected TagElement makeTag(final Element elem, final boolean fictional) {
        return new TagElement(elem, fictional);
    }

    /**
     * It marks the first occurence of an element inside a document.
     * @param elem the {@link Element} whose first occurence wants to be
     *             marked.
     */
    protected void markFirstTime(final Element elem) {
        // TODO review this
        throw new UnsupportedOperationException(Messages.getString("swing.9F")); //$NON-NLS-1$
    }

    /**
     * It parses a HTML document. <br>
     * During the parsing process, this method invokes the handlers for text,
     * tags, comment, ... that posses this class. In this way, the user may be
     * notified about all the information found in the parsed file.
     *  
     * @param in a reader from where the document to be parsed will be extract
     *           during the parsing process.
     * @throws IOException
     */
    public synchronized void parse(final Reader in) throws IOException {
        if (in == null) {
            // same as RI
            throw new NullPointerException();
        }
        file = in;
        handler.parse(file);
    }

    /**
     * Obtains the information of the last parsed markup declaration in the
     * parsed file. 
     * @return the information stored in the last parsed markup declaration.
     * @throws IOException
     */
    public String parseDTDMarkup() throws IOException {
        file.ready(); // To satisfy RI behavior
        return LastMarkupDecl.getDeclaration().substring(DOCTYPE_DECL.length(),
                LastMarkupDecl.getDeclaration().length())
                + LastMarkupDecl.getContent();
    }

    
    protected boolean parseMarkupDeclarations(final StringBuffer strBuff)
            throws IOException {
        boolean isDeclaration = strBuff.toString().toUpperCase().startsWith(
                DOCTYPE_DECL);
        if (isDeclaration) {
            parseDTDMarkup();
        }
        return isDeclaration;
    }

    
    protected void startTag(final TagElement tag)
            throws ChangedCharSetException {

        if (isCurrentTagSimple) {
            handleEmptyTag(tag);
        } else {
            handleStartTag(tag);
        }

    }

    // Invoked by DocumentParser
    String getEOLString() {
        return handler.getEOLString();
    }

    /*
     * ********************************************************************
     * Inner class
     * ********************************************************************
     */

    class ParserHandlerImpl implements ParserHandler {
        
        /**
         * The CUP parser used to parse a file.
         */
        private ParserCup cup;

        /**
         * The LEXER generated by JFlex specification
         */
        private Lexer lexer;

        /**
         * The last information retrieved from the CUP Parser.
         */
        private HTMLText htmlText2flush;
        
        boolean trailingSpaceAppended;
        
        /**
         * Defines the maximun depth searched into a {@link ContentModel} when
         * looking for an implication chain of {@link Element}s.
         */
        private static final int MAX_DEPTH = 2;
        
        private static final String CLASS_ATTR = "class";

        /**
         * Is used in order to handle any text as comment when is 
         * inside a script tag
         */
        private int scriptDepth;
        
        /**
         * The actual element in the parsing tree.
         */
        private DefaultMutableTreeNode currentElemNode;

        private DefaultMutableTreeNode lastElemSeen;

        public ParserHandlerImpl() {
            // TODO COMPLETE ME ?
        }

        /**
         * Starts the parse of a reader.
         * 
         * @param in
         *            The Reader to be parsed.
         * @throws IOException
         *             When the Reader could not be read.
         */
        public void parse(Reader in) throws IOException {
            lexer = new Lexer(in);
            lexer.setDTD(dtd);
            cup = new ParserCup(lexer);
            cup.setCallBack(this);
            lexer.setCup(cup);
            lexer.setStrict(strict);
            try {
                cup.parse();
                flushHtmlText(true);
                reportRemainingElements();
            } catch(ClassCastException e){
                cup.done_parsing();
            } catch (ChangedCharSetException e){
            	throw e;        	
            } catch (Exception e) {
                // FIXME : CALL HANDLE ERROR HERE ?
                throw new IOException(e.toString());
            }
        }

        
        /*
         * ********************************************************************
         * handle methods
         * ********************************************************************
         */
        
        /**
         * This method is called when the lexer finds a token that looks like 
         * an opening tag.
         * <BR>
         * Among other things, this method analyzes:
         * <ol>
         * <li> If the name of the found tag is a valid one or not.
         * <li> If all of its attributes are valid, or there are some of them
         * that were not part of the preestablished attributes for an specific
         * tag.
         * <li> The tag type. This means whether it is a simple (empty) one, or
         * a common opening tag (that may requiere a matching closing tag).
         * </ol> 
         * 
         * @param htmlTag a {@link HTMLTag} element that contains all the
         * information refered to the tag found in the document.
         * @throws ChangedCharSetException 
         */
        public void iHaveNewStartTag(HTMLTag htmlTag) throws ChangedCharSetException {
            flushAttributes();
            currentLine = htmlTag.getLine() + 2;
            currentStartPos = htmlTag.getOffset(); 
            currentEndPos =  htmlTag.getEndPos();
            
            String tagName = htmlTag.getName();
            
            Element element = dtd.elementHash.get(tagName);
            
            TagElement currentTag;
            HTMLTagType tagType;
            if (element != null) {
                tagType = getType(htmlTag, element);    
            } else {
                element = new Element(
                        -1, tagName, false, false, null, null, -1, null, null, null);
                handleUnrecognizedError(htmlTag);
                /* Before any flush of strInfo we report the error (same as RI) */
                tagType = HTMLTagType.SIMPLE;
            }
            handleUnsupportedJavaScript(element);
            
            /* flush text */
            currentTag = new TagElement(element);
            boolean breaksFlowAfter = currentTag.breaksFlow();
            flushHtmlText(breaksFlowAfter);
            
            /* handle attributes */
            handleTagAttr(htmlTag, element);

            /* impply */
            boolean mustBeReported = 
                manageStartElement(element, htmlTag.getEndPos());

            if (mustBeReported) {
                if (tagType == HTMLTagType.START) {
                    isCurrentTagSimple = false;
                } else if (tagType == HTMLTagType.SIMPLE) {
                    isCurrentTagSimple = true;
                    lastElemSeen = new DefaultMutableTreeNode(currentTag);
                } else {
                    // this should not happen
                    throw new AssertionError();
                }
                startTag(currentTag);
            }
            if (isCurrentTagSimple) {
                flushAttributes();
                levelUp();
            }
        }

        /**
         * This method is called by the lexer, when a token that looks like
         * a <em>Markup Declaration</em> is found in the stream been parsed.
         * 
         * @param htmlMarkup a {@link HTMLMarkup} element that contains all
         * the information needed to manage a Markup Declaration.
         * @throws IOException 
         */
        public void iHaveNewMarkup(HTMLMarkup htmlMarkup) throws IOException {
            flushHtmlText(true);
            currentStartPos = htmlMarkup.getOffset();
            LastMarkupDecl = htmlMarkup;
            parseMarkupDeclarations(
                    new StringBuffer(htmlMarkup.getDeclaration()));
        }
        
        /**
         * This method is called by the lexer, when a token that looks like
         * a closing tag is found in the parsed stream.
         * 
         * @param htmlTag a {@link HTMLTag} element that contains all the
         * information related to the closing tag, that was found in the parsed
         * stream.
         * 
         */
        public void iHaveNewEndTag(final HTMLTag htmlTag) {
            currentLine = htmlTag.getLine() + 1;
            String tagName = htmlTag.getName();
            Element element = dtd.elementHash.get(tagName);
            TagElement currentTag;
            if (element != null) {
                currentTag = new TagElement(element);
                flushHtmlText(currentTag.breaksFlow());
            } else {
                handleUnrecognizedError(htmlTag);
                element = new Element(
                        -1, tagName, false, false, null, null, -1, null, null, null);
                currentTag = new TagElement(element);
                flushHtmlText(currentTag.breaksFlow());
                try {
                    attributes.addAttribute("endtag", Boolean.TRUE);
                    handleEmptyTag(currentTag);
                } catch (ChangedCharSetException e) {
                    // this shouldn't happen
                    throw new AssertionError();
                }
            }
            
            currentStartPos = htmlTag.getOffset();
            currentEndPos = htmlTag.getEndPos();
            
            boolean mustBeReported = 
                manageEndElement(element);
            
            if (mustBeReported) {
                handleEndTag(currentTag);
            }
        }

        /**
         * This method is called by the lexer, when a new piece of text is
         * found in the parsed stream.
         * 
         * @param htmlText a {@link HTMLText} element that contains all the
         * information related to the piece of text found in the stream. 
         */
        public void iHaveNewText(final HTMLText htmlText) {
            // flush any remaing text...
            flushHtmlText(false);
            currentStartPos = htmlText.getOffset();
            
            if (scriptDepth > 0) {
                handleComment(getText(htmlText, false, false));
            } else {
                htmlText2flush = htmlText;
            }
        }
        
        /**
         * This method is called by the lexer, when a new comment is found
         * in the parsed stream.
         * 
         * @param htmlComment a {@link HTMLComment} element that contains all
         * the information related with the found comment.
         */
        public void iHaveNewComment(HTMLComment htmlComment) {
            flushHtmlText(false);
            currentLine = htmlComment.getLine() + 1;
            currentStartPos = htmlComment.getOffset();
            handleComment(htmlComment.getText().toCharArray());
        }
       
        /**
         * This method is called by the lexer when an error is found in the
         * stream that is being parsed.
         * 
         * @param errMsgType an appropiate error message according to the found
         *               error.
         * @param attr1 the second part of the message.
         * @param attr2 the third part of the message.
         * @param attr3 the fourth part of the message.
         */
        public void iHaveNewError(HTMLErrorType errMsgType, String attr1, String attr2, String attr3) {         
            error(errMsgType, 
                    attr1==null ? HTMLErrorType.DEF_ERROR.toString() : attr1,
                    attr2==null ? HTMLErrorType.DEF_ERROR.toString() : attr2,
                    attr3==null ? HTMLErrorType.DEF_ERROR.toString() : attr3);            
        }
        
        /**
         * It reports the tags that remains open after the end of a document was
         * reached. This is equivalent to think that some tags remains in the
         * parsing stack. 
         */
        public void reportRemainingElements() {
            if (currentElemNode != null) {
                flushHtmlText(getNodeTagElement(currentElemNode).breaksFlow());
            }
            currentStartPos = currentEndPos + 1; // same as RI
            while (currentElemNode != null) {
                TagElement tag = (TagElement) currentElemNode.getUserObject();
                reportImpliedEndTag(tag.getElement());
                levelUp();
            }
        }
        
        /**
         * Reports which has been the line terminator that most appear in the
         * parsed stream.
         * <BR>
         * The String returned can be any of the following ones:
         * <ol>
         * <li> "\n" (Linux)
         * <li> "\r\n" (Windows)
         * <li> "\r" (Mac)
         * </ol>
         */
        public String getEOLString() {
            return lexer.getEOLString();
        }
        
        /*
         * ********************************************************************
         * Auxiliar methods
         * ********************************************************************
         */
        
        private boolean breaksFlowBefore() {
            return lastElemSeen == null ? 
                    false : getNodeTag(lastElemSeen).breaksFlow();
        }
        
        private void flushHtmlText(boolean breaksFlowAfter) {
            if (htmlText2flush != null) {
                boolean breaksFlowBefore = breaksFlowBefore();
                char[] s = getText(htmlText2flush, breaksFlowBefore, breaksFlowAfter);
                if (s.length != 0) {
                   Tag tag =getNodeTag(currentElemNode); 
                    if (tag != null && tag.equals(Tag.TITLE)) {
                        handleTitle(s);
                   }
                   manageStartElement(dtd.pcdata, htmlText2flush.getOffset());
                   currentStartPos = htmlText2flush.getOffset();
                   handleText(s);        
                }
                htmlText2flush = null;
            }
        }
        
        // FIXME review strict mode
        private char[] getText(HTMLText htmlText, 
                boolean breaksFlowBefore, boolean breaksFlowAfter) {
            String str = htmlText.getText();
           
            if (htmlText.hasLeadingSpaces() && !trailingSpaceAppended && !breaksFlowBefore) {
                str = " " + str;
            }
            if (htmlText.hasTrailingSpaces() && !breaksFlowAfter) {
                str += " ";
                trailingSpaceAppended = true;
            } else {
                trailingSpaceAppended = false;
            }
            return str.toCharArray();
        }
        
        
        private HTMLTagType getType(HTMLTag tag, Element element) {
            HTMLTagType tagType;
            if (element.isEmpty()) {
                // in the case we found a lexical start tag but is defined
                // in the DTD as SIMPLE (17)
                tagType = HTMLTagType.SIMPLE;
            } else {
                tagType = tag.getType();
            }
            return tagType;
        }

        /*
         * If the element is not defined in the dtd (unrecognized) the RI
         * reports a SimpleTag
         */
        private void handleUnrecognizedError(HTMLTag tag) {
            String tagName = tag.getName();
            int actualPos = currentStartPos;
            if (tag.getType() == HTMLTagType.END) {
                currentStartPos = tag.getEndPos() + 2; // to match RI
                error(HTMLErrorType.ERR_END_UNRECOGNIZED, tagName);
            } else {
                if (tag.getAttributes().isEmpty()) {
                    currentStartPos = tag.getEndPos() + 1; // to match RI
                }
                error(HTMLErrorType.ERR_TAG_UNRECOGNIZED, tagName);
            }
            currentStartPos = actualPos;
        }
        
        private void handleUnsupportedJavaScript(Element element) {
            if (element.isScript()) {
                error(HTMLErrorType.ERR_JAVASCRIPT_UNSUPPORTED);
            }
        }
        
        private void handleTagAttr(HTMLTag htmlTag, Element element) {
            String tagName = htmlTag.getName();
            HTMLAttributeList attList = htmlTag.getHtmlAttributeList();
            while (attList != null) {
                String currentAttListName = 
                    attList.getAttributeName();
                
                /* Assign the attribute name (attr): object */
                Object attr = HTML.getAttributeKey(currentAttListName);
                if (attr == null) {
                    attr = currentAttListName;
                }

                /* Report ERR_INVALID_TAG_ATTR */
                AttributeList currentAttList = 
                    element.getAttribute(currentAttListName);
                if (currentAttList == null) {
                    // if the tag is not defined in the current element => invalid
                    error(HTMLErrorType.ERR_INVALID_TAG_ATTR, 
                            currentAttListName, tagName);
                }
               
                /*
                 * Sets the attribute value (attrValue)
                 * 
                 * If the value is null then the value #DEFAULT is assigned.
                 * If it's a valid one, the case of the value is respected. Otherwise, 
                 * the value is moved to lower case  (same as RI)
                 */
                
                String attrValue;
                if (attList.getAttributeValue() == null) {
                    attrValue = 
                        currentAttList != null && currentAttList.getValue() != null ? 
                                currentAttList.getValue() : HTMLTag.DEF_ATTR_VAL;
                } else {
                    // FIXME: This seems to be a special case of RI.
                    if (currentAttListName.equalsIgnoreCase(CLASS_ATTR)) {
                        attrValue = attList.getAttributeValue().toString().toLowerCase();
                    } else if (currentAttList != null) {
                        attrValue = normalizeAttrVal(
                                attList.getAttributeValue().toString(),
                                currentAttList.getType());
                    } else {
                        attrValue = attList.getAttributeValue().toString();
                    }
                }

                /* Reports ERR_MULTI_TAG_ATTR */
                if (attributes.isDefined(currentAttListName)) {
                    error(HTMLErrorType.ERR_MULTI_TAG_ATTR,                             
                            currentAttListName, tagName);
                }
                // Overrides the value of an attribute if it was defined previously (same as RI)
                attributes.addAttribute(attr, attrValue);
                
                attList = attList.getNext();
            }
     
            for (Object attrName : element.getRequiredAttributes()) {
                if (!attributes.isDefined(attrName)) {
                    error(HTMLErrorType.ERR_REQ_ATT, attrName.toString(), tagName);
                }
            }
     
            currentStartPos = htmlTag.getOffset();            
        }

        private String normalizeAttrVal(String attrVal, int type) {
            String str = attrVal;
            switch (type) {
                case DTDConstants.NMTOKEN:
                case DTDConstants.ID:
                case DTDConstants.NUTOKEN:
                case DTDConstants.NUMBER:
                    str = str.toLowerCase();
            }
                
            return str;
        }
        
        /*
         * ********************************************************************
         * Implication methods
         * ********************************************************************
         */

        /**
         * Makes the report of a {@link TagElement} as implied. Setting its
         * attributes and prompting an appropiated error message.
         * 
         * @param tag the {@link TagElement} been implied.
         */
        private void reportImpliedTag(TagElement tag) {
            SimpleAttributeSet backup = attributes;
            attributes = new SimpleAttributeSet();
            attributes.addAttribute("_implied_", Boolean.TRUE);
            handleStartTag(tag);
            if (!tag.getElement().omitStart()) {
                error(HTMLErrorType.ERR_START_MISSING, tag.getElement()
                        .getName());
            }
            attributes = backup;
        }

        /**
         * It makes the report of an {@link Element} that was implied.
         * 
         * @param elem the {@link Element} to be reported.
         */
        private void reportImpliedEndTag(Element elem) {
            if (!elem.isEmpty()) {
                if (!elem.omitEnd()) {
                    error(HTMLErrorType.ERR_END_MISSING, elem.getName());
                }
                handleEndTag(new TagElement(elem));
            }
        }
        
        
        /**
         * Adds a {@link TagElement} at the "top" of the parsing stack. This means
         * that it converts it into the new <code>currentElem</code>
         * 
         * @param elem the {@link Element} to be added at the top of the stack.
         *             If the {@link Element} is pcdata, then it is not added to
         *             the parsing stack.
         */
        private void addAsCurrentElem (Element elem) {
            if (!elem.equals(dtd.pcdata)) {
                DefaultMutableTreeNode newNode = 
                    new DefaultMutableTreeNode(new TagElement(elem));
                if (elem.isScript()) {
                    scriptDepth++;
                }
                if (currentElemNode != null) {
                    currentElemNode.add(newNode);
                }
                currentElemNode = newNode;
                lastElemSeen = currentElemNode;
            }
        }
        
        /**
         * Sets the currentElem to the father of the actual currentElem. 
         *
         */
        private void levelUp () {
            lastElemSeen = currentElemNode;
            if (currentElemNode != null && 
                    getNodeElement(currentElemNode).isScript()) {
                scriptDepth--;
            }
            currentElemNode = (DefaultMutableTreeNode) currentElemNode.getParent();
        }

        
        /**
         * Gets the {@link Element} stored in a {@link DefaultMutableTreeNode}
         * 
         * @param node the {@link DefaultMutableTreeNode} that is consulted.
         * @return the {@link Element} that is stored in the node, if node
         *             is not null. If node in null, then null is returned.
         */
        private Element getNodeElement(DefaultMutableTreeNode node) {
            TagElement tag = getNodeTagElement(node);
            Element e = null;
            if (tag != null) {
                e = tag.getElement();
            }
            return e;
        }

        /**
         * Gets the {@link TagElement} stored in a
         * {@link DefaultMutableTreeNode}
         * 
         * @param node the {@link DefaultMutableTreeNode} that is consulted.
         * @return the {@link TagElement} that is stored in the node, if node
         *             is not null. If node in null, then null is returned.
         */
        private TagElement getNodeTagElement(DefaultMutableTreeNode node) {
            return node==null ? null : (TagElement) node.getUserObject();
        }
           
        /**
         * Gets the {@link Tag} stored in a {@link DefaultMutableTreeNode}
         * 
         * @param node the {@link DefaultMutableTreeNode} that is consulted.
         * @return the {@link Tag} that is stored in the node, if node
         *             is not null. If node in null, then null is returned.
         */
        private Tag getNodeTag(DefaultMutableTreeNode node) {
            TagElement tag = getNodeTagElement(node);
            Tag t = null;
            if (tag != null) {
                t = tag.getHTMLTag();
            }
            return t;
        }

        /**
         * 
         * @param e1 The element to search in e2's inclusions
         * @param e2 The element whose inclusions will be used to search for e1
         * @return true if e1 is present in e2's inclusions
         */
        private boolean isIncluded(Element e1, Element e2){     
            boolean isIncluded = false;
            if (e1 != null && e2 != null) { 
                BitSet bs = e2.inclusions;
                if (bs!=null && 0 <= e1.getIndex()) {
                    isIncluded = e2.inclusions.get(e1.getIndex());
                }
            }
            return isIncluded;
        }
        
        /**
         * 
         * @param e1 The element to search in e2's exclusions
         * @param e2 The element whose exclusions will be used to search for e1
         * @return true if e1 is present in e2's exclusions
         */
        private boolean isExcluded(Element e1, Element e2){     
            boolean isExcluded = false;
            if (e1 != null && e2 != null) {
                BitSet bs = e2.exclusions;
                
                if (bs!=null && 0 <= e1.getIndex()) {
                    isExcluded = e2.exclusions.get(e1.getIndex());
                }
            }
            return isExcluded;
        }
        
        
        /**
         * Takes the neccesary steps to impply the required elements when in non
         * strict mode parsing.
         * 
         * @param e
         *            the {@link Element} for which implication is required.
         * @param endPos
         *            the position of the last character that conforms the
         *            actaul parsing element in the file.
         * @return true if the parsed element must be reported as an opening
         *         tag. Otherwise it returns false.
         */
        private boolean nonStrictModeStartImplication(Element e, int endPos) {
            boolean mustBeTreated = true;
            int actualPos = currentStartPos;
            Element actualElem = getNodeElement(currentElemNode);
            List<Pair<Element,Boolean>> impliedElements;
            boolean implicationMade;

            // The element is in the exclusions list.
            if (isExcluded(e, actualElem)) {
                // We report the close of its father.
                reportImpliedEndTag(actualElem);
                levelUp();
                manageStartElement(e, endPos);
            } else if (isIncluded(e, actualElem)) {
                addAsCurrentElem(e);
            } else if (actualElem != null && actualElem.getContent() == null) {
                // The content model of the current element is null
                if (dtd.isRead() && !e.equals(dtd.pcdata)) {
                    error(HTMLErrorType.ERR_TAG_UNEXPECTED, e.getName());
                }
                addAsCurrentElem(e);
            } else {
                List<Element> parsed = loadParsedElems(currentElemNode);
                Pair<List<Pair<Element,Boolean>>,Boolean> impliedInfo;
                impliedInfo = imply (actualElem, e, false, parsed);
                impliedElements = impliedInfo.getFirst();
                implicationMade = !impliedElements.isEmpty();
                updateParsingStack(impliedElements);
               
                actualElem = getNodeElement(currentElemNode);
                if (Boolean.FALSE.equals(impliedInfo.getSecond())) {
                    if (e.omitStart()) {
                        error(HTMLErrorType.ERR_TAG_IGNORE, e.getName());
                        mustBeTreated = false;
                    } else if (currentElemNode != null
                                && actualElem != null
                                && !actualElem.equals(dtd.body)
                                && actualElem.omitEnd()) {
                        handleEndTag(new TagElement(actualElem, true));
                        levelUp();
                        nonStrictModeStartImplication(e, endPos);
                        mustBeTreated = false;
                    } else if (!e.equals(dtd.pcdata) && isDefined(e)) {
                        currentStartPos = endPos;
                        error(HTMLErrorType.ERR_TAG_UNEXPECTED, e.getName());
                        currentStartPos = actualPos;
                    }
                }               
                
                if (implicationMade && e.equals(dtd.pcdata)) {
                    currentStartPos++;   // to match RI
                    error(HTMLErrorType.ERR_START_MISSING,
                            getNodeElement(currentElemNode).getName());
                }
                if (mustBeTreated) {
                	addAsCurrentElem(e);
                }
                
            }
            return mustBeTreated;
        }
        
        
        
        /**
         * Takes the neccesary steps to impply the required elements when in 
         * strict mode parsing.
         *  
         * @param e
         *            the {@link Element} for which implication is required.
         * @return true if the parsed element must be reported as an opening
         *         tag. Otherwise it returns false.
         */
        private boolean strictModeStartImplication (Element e) {
            Element ce = getNodeElement(currentElemNode);
            Pair<List<Pair<Element,Boolean>>, Boolean> implied;
            List<Element> parsed = loadParsedElems(currentElemNode);
                        
            implied = imply (getNodeElement(currentElemNode), e, true, parsed);
            
            updateParsingStack(implied.getFirst());
                        
            if (!e.equals(dtd.pcdata) && (isExcluded(e, ce) || implied.getSecond().equals(Boolean.FALSE))) {
                error (HTMLErrorType.ERR_TAG_UNEXPECTED, e.getName());
            }
            addAsCurrentElem(e);

            return true;
        }
        
        
        /**
         * Loads the already been parsed elements for a specific node in the
         * parsing tree into a {@link List}
         * 
         * @param node the node of the parsing tree that stores the
         *             {@link TagElement} element for which the already been
         *             parsed sub-elements want to be consulted.
         * 
         * @return a {@link List} with all the elements that has already been
         *         parsed for the element that is actually on the top of the
         *         parsing stack.
         */
        private List<Element> loadParsedElems (DefaultMutableTreeNode node) {
            List<Element> parsed = new LinkedList<Element>();
            if (node != null) {
                Enumeration itr = node.children();
                while (itr.hasMoreElements()) {
                    parsed.add(getNodeElement((DefaultMutableTreeNode)itr.nextElement()));
                }
            }
            return parsed;
        }
        
        
        
        /**
         * It makes the managent of implication when an opening tag is found.
         * 
         * @param e
         *            the {@link Element} that has been found in the document.
         * @param endPos
         *            the final position of the opening tag in the parsed file.
         * @return true if the <code>handleStartTag</code> method must be
         *         called over the current tag. Otherwise it returns false.
         */       
        private boolean manageStartElement(Element e, int endPos) {

            boolean mustBeReported;      
            
            if (strict) {
                strictModeStartImplication(e);
                mustBeReported = true;
            } else {
                mustBeReported = nonStrictModeStartImplication(e, endPos);                    
            }
                
            return mustBeReported;
        }
        

        /**
         * It determines whether an {@link Element} was defined in the path that
         * takes from the <code>rootElem</code> to the <code>currentElem</code> in
         * the parsed tree.
         * 
         * @param elem the {@link Element} been searched.
         * @return true if the {@link Element} could be found in the path.
         *         Otherwise, false.
         */
        private boolean isInActualPath(Element elem) {
            boolean found = false;

            Object[] path = currentElemNode.getUserObjectPath();
            for (Object obj : path) {
                found = found
                        || ((TagElement) obj).getElement().getName()
                                .equalsIgnoreCase(elem.getName());
            }

            return found;
        }
        
        
        /**
         * Determines if a {@link TagElement} contains an {@link Element} that is 
         * defined in the current {@link DTD}
         *  
         * @param elem the {@link Element} been searched.
         * @return true if the {@link Element} is defined in the current
         *         {@link DTD}. Otherwise it returns false.         
         */
        private boolean isDefined (Element elem) {
            return dtd.elementHash.containsKey(
                    elem.getName().toLowerCase());
        }
        

        /**
         * It manages the implication behaviour when a closing tag is found.
         * 
         * @param e the {@link Element} that was found in the document.
         * @return true if the handleEndTag must be reported over the current tag,
         *         of false otherwise.
         */
        private boolean manageEndElement(Element e) {
            boolean mustBeReported = false;
            int actualPos = currentStartPos;

            if (currentElemNode == null) {
                // FIXME corroborate this position...
                currentStartPos = currentEndPos + 1; //to match RI 
                error(HTMLErrorType.ERR_END_EXTRA_TAG, e.getName());
            } else {            
                if (isInActualPath(e)) {
                    Element actualElem = getNodeElement(currentElemNode);
                    if (!e.equals(actualElem) 
                            && !actualElem.omitEnd() 
                            && !e.omitEnd()) {
                        error(HTMLErrorType.ERR_TAG_IGNORE, e.getName());
                    } else {
                        while (!e.getName().equalsIgnoreCase(actualElem.getName())) {
                            reportImpliedEndTag(actualElem);
                            //  the endTag should only be reported if the current 
                            // Element is not simple (EMPTY)
                            levelUp();
                            actualElem = getNodeElement(currentElemNode);
                        }
                        mustBeReported = !e.isEmpty();
                        levelUp();     
                    }
                } else if (isDefined(e)){               
                    error(HTMLErrorType.ERR_UNMATCHED_END_TAG,
                            e.getName());
                }

            }
            currentStartPos = actualPos;
            return mustBeReported;
        }
        
        
        /**
         * Gets the sequence of {@link Element} that may be implied to reach the
         * {@link Element} e.
         * 
         * @param actualElem
         *            the last known {@link Element}, and the one from which the
         *            implication should take place. It also defines the first
         *            {@link ContentModel} where the {@link Element} "e" should be
         *            searched.
         * @param e
         *            the {@link Element} been searched.
         * @param isSimple
         *            defines if the implication should take in consideration only
         *            the <code>HTML</code>, <code>HEAD</code> and <code>BODY</code>
         *            elements.
         * @param parsed
         *            the sequence of previosly found {@link Element} inside the
         *            {@link Element} "actualElem", but before the ocurrence of the
         *            {@link Element} "e".
         * @return a pair that contains the sequence of elements to be impplied and
         *         a {@link Boolean} that specifies if the {@link Element} could be
         *         really be place in such place, or it just the only possible solution
         *         to the implication problem.
         */
        private Pair<List<Pair<Element, Boolean>>,Boolean> imply(
                Element actualElem,
                Element e,
                boolean isSimple,
                List<Element> parsed) {
            
            Pair<List<Pair<Element, Boolean>>,Boolean> impliedInfo = null;
            List<Pair<Element, Boolean>> implied = null;
            LinkedList<Pair<Element, Boolean>> path = new LinkedList<Pair<Element, Boolean>>();
            int depth = 1;
            boolean searchCompleted = false;
            Boolean found = Boolean.TRUE;

            if (actualElem != null) {                
                if (actualElem.getContent() == null) {
                    if (e.getType() == DTDConstants.MODEL) {
                        reportImpliedEndTag(actualElem);
                        levelUp();
                        parsed = loadParsedElems(currentElemNode);
                        impliedInfo = imply (getNodeElement(currentElemNode), e, false, parsed);
                    }
                } else {
                    implied = actualElem.getContent().implication(e, parsed, false, 1, path);
                    if (implied == null) {
                        Element father = null;
                        if (currentElemNode != null) {                            
                            DefaultMutableTreeNode fatherNode = (DefaultMutableTreeNode)currentElemNode.getParent();
                            if (fatherNode != null) {
                                father = getNodeElement(fatherNode);
                                ContentModel fatherModel = father.getContent();
                                if (fatherModel != null) {
                                    parsed = loadParsedElems(fatherNode);
                                    implied = fatherModel.implication(e, parsed, false, 1, new LinkedList<Pair<Element, Boolean>>());
                                }
                            }
                        }
                        parsed = loadParsedElems(currentElemNode);
                        if (implied != null || isIncluded(e, father)) {
                            reportImpliedEndTag(actualElem);
                            levelUp();
                            impliedInfo = imply (getNodeElement(currentElemNode), e, false, parsed);
                        } else {
                            while (depth <= MAX_DEPTH && !searchCompleted) {
                                // implied also checks if the element is in the
                                // inclusions of a potential implication path
                                implied = actualElem.getContent().implication(e, parsed, false, depth, path);
                                depth++;
                                path.clear();
                                searchCompleted = (implied != null || isSimple);
                            }
                        }
                    }
                    found = impliedInfo != null ? impliedInfo.getSecond() : implied!=null;
                }
                
                if (found.equals(Boolean.FALSE)) {
                    if (actualElem.equals(dtd.html) && !e.omitStart()) {
                        implied = implied == null ? new LinkedList<Pair<Element,Boolean>>() : implied;
                        implied.add(new Pair<Element,Boolean>(dtd.head, Boolean.TRUE));
                        implied.add(new Pair<Element,Boolean>(dtd.body, Boolean.FALSE));
                    } else if (actualElem.equals(dtd.head) && !e.omitStart()) {
                        implied = implied == null ? new LinkedList<Pair<Element,Boolean>>() : implied;
                        reportImpliedEndTag(dtd.head);
                        levelUp();
                        implied.add(new Pair<Element,Boolean>(dtd.body, Boolean.FALSE));
                    }
                    found = Boolean.FALSE;
                }

                if (implied != null && !implied.isEmpty()){
                    Element elem = (Element)((Pair)implied.get(0)).getFirst();
                    while (!implied.isEmpty() && parsed.contains(elem)) {
                        implied.remove(0);
                        parsed = parsed.subList(parsed.indexOf(elem)+1, parsed.size());
                        if (!implied.isEmpty()) {
                            elem = (Element)((Pair)implied.get(0)).getFirst();
                        }
                    }
                }
            } else {
                found = Boolean.valueOf(e.equals(dtd.html));
            }
            
            if (actualElem == null && !found) {
                TagElement html = new TagElement(dtd.html, true);
                reportImpliedTag(html);
                addAsCurrentElem(dtd.html);
                impliedInfo = imply (dtd.html, e, false, parsed);
            } else {
                if (implied == null) {
                    implied = new LinkedList<Pair<Element,Boolean>>();
                }
                impliedInfo = new Pair<List<Pair<Element,Boolean>>,Boolean>(implied,found);
            }
            return impliedInfo;
        }        
        
        
        
        private void updateParsingStack(List<Pair<Element,Boolean>> implied) {
            while (!implied.isEmpty()) {
                Pair<Element,Boolean> impliedPair = implied.remove(0);
                TagElement impliedTag = new TagElement (impliedPair.getFirst(), true);
                reportImpliedTag(impliedTag);
                Element element = impliedTag.getElement();
                addAsCurrentElem(element);
                if (impliedPair.getSecond().equals(Boolean.TRUE)
                        && !element.isEmpty()) {
                    handleEndTag(impliedTag);
                    levelUp();
                }
            }
        }        
    }
}
