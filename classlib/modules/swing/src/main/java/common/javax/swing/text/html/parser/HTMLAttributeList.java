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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an attribute of a html tag. Contains a reference to the 
 * next attribute. 
 */
class HTMLAttributeList {

    /**
     * The attribute name
     */
    private String attrName;
    
    /**
     * The attribute value
     */
    private Object attrValue;
    
    /**
     * The first position of the attribute 
     */
    private int pos;

    /**
     * The final position of the attribute 
     */
    private int endPos;
    
    /**
     * Stores a list of lexical errors 
     */
    private List<String> error = null;
    
    /**
     * A reference to the next attribute
     */
    private HTMLAttributeList next; 
    
    HTMLAttributeList (final String attrName, final Object attrValue, 
            final int pos, final int endPos, final HTMLAttributeList next) {        
        this(attrName,attrValue,pos,endPos,next,null);        
    }	
    
    
    HTMLAttributeList (final String attrName, final Object attrValue, 
            final int pos, final int endPos, 
            final HTMLAttributeList next,List<String> error) {
    	this.attrName = attrName.toLowerCase();
        this.attrValue = attrValue;
        this.pos = pos;
        this.endPos = endPos;
        this.next = next;
        this.error=error;                  
    }
    
    List<String> getError(){
    	return error;
    }
    
    String getAttributeName () {
        return attrName;
    }
    
    Object getAttributeValue () {
        return attrValue;
    }
    
    int getPos () {
        return pos;
    }
    
    int getEndPos () {
        return endPos;
    }
    
    HTMLAttributeList getNext () {
        return next;
    }
    
    void setNext (HTMLAttributeList nextAttr) {
        this.next = nextAttr;
    }
    
}
