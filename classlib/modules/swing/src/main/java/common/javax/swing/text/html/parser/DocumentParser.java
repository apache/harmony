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
import javax.swing.text.ChangedCharSetException;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

public class DocumentParser extends Parser {

    private static final HTML.Attribute HTTP_EQUIV = 
        HTML.getAttributeKey("http-equiv");

    private static final String CONTENT_TYPE = "content-type";
    
    private static final HTML.Attribute CONTENT = 
        HTML.getAttributeKey("content");
    
    private static final String CHARSET = "charset";
    
    private HTMLEditorKit.ParserCallback callback;

    private boolean ignoreCharSet;

    public DocumentParser(final DTD dtd) {
        super(dtd);
    }

    protected void handleError(final int ln, final String errorMsg) {
        callback.handleError(errorMsg, ln);
    }

    protected void handleText(final char[] data) {
        callback.handleText(data, getCurrentPos());
    }

    protected void handleEndTag(final TagElement tag) {
        callback.handleEndTag(tag.getHTMLTag(), getCurrentPos());
    }

    protected void handleEmptyTag(final TagElement tag)
            throws ChangedCharSetException {
        if (!ignoreCharSet && (tag.getHTMLTag() == HTML.Tag.META)) {
            String httpEquivValue = (String) getAttributes().getAttribute(HTTP_EQUIV);
            String contentValue = (String) getAttributes().getAttribute(CONTENT);

            if (httpEquivValue != null && contentValue != null &&
                    httpEquivValue.equalsIgnoreCase(CONTENT_TYPE) &&
                    contentValue.toLowerCase().contains(CHARSET)) {
                // notice that always here ignoreCharSet will be false 
                throw new ChangedCharSetException(contentValue, ignoreCharSet); 
            }
        }
        callback.handleSimpleTag(
                tag.getHTMLTag(), getAttributes(), getCurrentPos());
    }

    protected void handleComment(final char[] text) {
        callback.handleComment(text, getCurrentPos());
    }

    protected void handleStartTag(final TagElement tag) {
        callback.handleStartTag(
                tag.getHTMLTag(), getAttributes(), getCurrentPos());
    }
    
    

    public void parse(final Reader in,
            final HTMLEditorKit.ParserCallback callback,
            final boolean ignoreCharSet) throws IOException {
        /*
         * TODO
         * when the Reader in is null handle implied methods are invoked
         * and the handleError report a nullPointerException 
         * 
         * Should be handled in Parser class without calling cup class ?  
         */

        this.callback = callback;
        this.ignoreCharSet = ignoreCharSet;
        super.parse(in);

        callback.handleEndOfLineString(super.getEOLString());    

        // a close invocation flush the remaining bytes
        in.close();  
    }
}