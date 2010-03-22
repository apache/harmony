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

/**
 * Represents the error types reported by the handle methods of 
 * the class {@link Parser}.  
 */
enum HTMLErrorType {
    /*
     * Notice the final space in some error constants: to behave as the RI 
     */
    ERR_TAG_UNRECOGNIZED("tag.unrecognized "),
    ERR_END_UNRECOGNIZED("end.unrecognized"),
    ERR_START_MISSING("start.missing"),
    ERR_END_MISSING("end.missing"),
    ERR_INVALID_TAG_ATTR("invalid.tagatt"),
    ERR_MULTI_TAG_ATTR("multi.tagatt"),
    ERR_REQ_ATT("req.att "),
    ERR_INVALID_TAG_CHAR("invalid.tagchar"),
    ERR_TAG_IGNORE("tag.ignore"),
    ERR_TAG_UNEXPECTED("tag.unexpected"),
    ERR_END_EXTRA_TAG("end.extra.tag"),
    ERR_UNMATCHED_END_TAG("unmatched.endtag"),
    ERR_THROWABLE("java.lang.IOException"),
    ERR_ATTVALERR("attvalerr"),
    ERR_EXPECTED("expected"),
    ERR_EXPECTED_TAGNAME("expected.tagname"),
    ERR_EOF_LITERAL("eof.literal"),
    ERR_JAVASCRIPT_UNSUPPORTED("javascript.unsupported"), 
    DEF_ERROR("?");
    
    /**
     * Encapsulates the current error type 's value
     */
    private String value;
    
    /**
     * Constructor
     * 
     * @param value Represents the error type 's value   
     */
    private HTMLErrorType(String value) {
        this.value = value;
    }
    
    /**
     * Get the current error type 's value    
     */
    public String toString() {
        return value;
    }
}
