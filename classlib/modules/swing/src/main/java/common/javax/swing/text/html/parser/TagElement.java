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

import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Tag;

public class TagElement {
    private HTML.Tag tag;
    private Element element;
    private boolean isFictional;

    public TagElement(final Element element,
                      final boolean fictional) {
        this.element = element;
        isFictional = fictional;
        
        // HTML.getTag() creates an element if it is not defined in 
        // HTML.getAllTags(). Because of this we have to look up at Tag[] array 
        Tag[] tags = HTML.getAllTags();
        boolean found = false;
        for (int i = 0; i < tags.length; i++) {
            if (tags[i].toString().equals(element.name)) {
                tag = HTML.getTag(element.name);
                found = true;
            }
        }
        if (!found) {
            tag = new HTML.UnknownTag(element.name);
        }
    }


    public TagElement(final Element element) {
        this(element, false);
    }


    public boolean fictional() {
        return isFictional;
    }


    public HTML.Tag getHTMLTag() {
        return tag;
    }


    public Element getElement() {
        return element;
    }


    public boolean isPreformatted() {
        return tag.isPreformatted();
    }


    public boolean breaksFlow() {
        return tag.breaksFlow();
    }
}

