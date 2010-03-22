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

import javax.swing.text.SimpleAttributeSet;


class HTMLTag extends HTMLObject {

    private String name;
     
    private HTMLAttributeList attList;
    
    private HTMLTagType type;
    
    private int endPos;

    private int line;
    
    private SimpleAttributeSet attributes;

    /**
     * The dafault value given as attribute value for an attribute whose
     * value was not defined in the parsed document.
     */
    public static final String DEF_ATTR_VAL = "#DEFAULT";
    
	public HTMLTag(HTMLTagType type, String name, HTMLAttributeList attList, int offset, int endPos, int line) {
		super(offset);
		this.type = type;
		this.attList = attList;
        this.name = name.toLowerCase();
        this.endPos = endPos;
        this.line = line;
        this.attributes = new SimpleAttributeSet();
	}

    /*
     * getters
     */
    
	public String getName() {
		return name;
	}

    public HTMLAttributeList getHtmlAttributeList() {
        return attList;
    }
    
    public SimpleAttributeSet getAttributes() {
        return attributes;
    }
    
    public void setAttributes(SimpleAttributeSet attributes) {
        this.attributes = attributes;
    }
    
    public HTMLTagType getType() {
		return type;
	}

    public int getEndPos() {
        return endPos;
    }
    
    public int getLine() {
        return line;
    }
}

