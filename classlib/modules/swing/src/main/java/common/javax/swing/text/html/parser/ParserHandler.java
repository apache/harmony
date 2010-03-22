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

interface ParserHandler {
    void parse(Reader in) throws IOException;
    
    // called by DocumentParser at the end
    String getEOLString();
    
    void iHaveNewStartTag(final HTMLTag htmlTag) throws ChangedCharSetException;
        
    void iHaveNewMarkup(final HTMLMarkup htmlMarkup) throws IOException;
    
    void iHaveNewEndTag(final HTMLTag htmlTag);
    
    void iHaveNewText(final HTMLText htmlText);
    
    void iHaveNewComment(final HTMLComment htmlComment); 
    
    void iHaveNewError(final HTMLErrorType errMsgType, final String attr1,final String attr2,final String attr3);
    
    void reportRemainingElements();
}