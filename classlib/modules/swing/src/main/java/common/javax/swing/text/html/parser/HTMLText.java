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


class HTMLText extends HTMLObject {

    /**
     * The text that was found in the text
     */
    private String text;

    /**
     * Determines whether a normal text had spaces at the begining of it (and were
     * removed)
     */
    private boolean hasLeadingSpaces;

    /**
     * Determines whether a normal text had spaces at the end of it (and were
     * removed)
     */
    private boolean hasTrailingSpaces;

    /**
     * Makes a new {@link HTMLText} object
     * 
     * @param text
     *            The parsed text
     * @param pos
     *            The position where the text was found
     * @param hasLeadingSpaces
     *            Determines whether the parser text had spaces at the begining
     *            of it.
     * @param hasTrailingSpaces
     *            Determines whether the parser text had spaces at the end
     *            of it.
     */
    public HTMLText(String text, int pos, boolean hasLeadingSpaces, boolean hasTrailingSpaces) {
        super(pos);
        this.text = text;
        this.hasLeadingSpaces = hasLeadingSpaces;
        this.hasTrailingSpaces = hasTrailingSpaces;
    }

    /**
     * Returns the parsed text contained by this object.
     * 
     * @return the parsed text.
     */
    public String getText() {
        return text;
    }

    /**
     * Tells if the parsed text (stored in this object) has spaces at the 
     * begining of it.
     * 
     * @return True if the parsed text had spaces at the begining of it. Otherwise it
     *         returns False.
     */
    public boolean hasLeadingSpaces() {
        return hasLeadingSpaces;
    }
    
    /**
     * Tells if the parsed text (stored in this object) has spaces at the end of
     * it.
     * 
     * @return True if the parsed text had spaces at the end of it. Otherwise it
     *         returns False.
     */
    public boolean hasTrailingSpaces() {
        return hasTrailingSpaces;
    }    

    public String toString() {
    	return text;    	
    }
    

}
