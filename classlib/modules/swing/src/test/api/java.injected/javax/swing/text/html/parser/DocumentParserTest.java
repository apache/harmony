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
 * @author Evgeniya G. Maenkova
 */
package javax.swing.text.html.parser;

import java.io.IOException;

import junit.framework.TestCase;

public class DocumentParserTest extends TestCase {
    Utils.ExtDocumentParser dp;

    protected void setUp() throws Exception {
        DTD dtd = Utils.getDefaultDTD();
        dp = new Utils.ExtDocumentParser(dtd);
    }

    //Looks liek these methods tesing is no of use... (as they are calles
    //only from by
    //parse method)... Otherwise, NPE will be thrown (no callback...)
/*
    public void testHandleError() {
        dp.handleError(5, "That's an error message");
    }

    public void testHandleEndTag() {
    }

    public void testHandleStartTag() {
    }

    public void testHandleEmptyTag() {
    }

    public void testHandleComment() {
    }

    public void testHandleText() {
    }


    public void testDocumentParser() {
    }

    */
    public void testParseReaderParserCallbackBoolean() {
        Utils.ParserCallback cb = new Utils.ParserCallback();
        cb.checkArguments = true;
        cb.setParser(dp);
        try {
               dp.parse(Utils.getReader("test41"), cb, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
