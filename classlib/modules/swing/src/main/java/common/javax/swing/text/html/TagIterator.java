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
* @author Alexander T. Simbirtsev
*/
package javax.swing.text.html;

import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLDocument.Iterator;

class TagIterator extends Iterator {

    private final Tag tag;
    private final ElementIterator it;
    private Element current;

    public TagIterator(final Tag tag, final Document document) {
        this.tag = tag;
        it = new ElementIterator(document);
        next();
    }

    public AttributeSet getAttributes() {
        return (current != null) ? current.getAttributes() : null;
    }

    public int getEndOffset() {
        return (current != null) ? current.getEndOffset() : -1;
    }

    public int getStartOffset() {
        return (current != null) ? current.getStartOffset() : -1;
    }

    public Tag getTag() {
        return tag;
    }

    public boolean isValid() {
        return (current != null);
    }

    public void next() {
        current = it.next();
        while (current != null) {
            if (tag.toString().equals(current.getName())) {
                break;
            }
            current = it.next();
        }
    }
}
