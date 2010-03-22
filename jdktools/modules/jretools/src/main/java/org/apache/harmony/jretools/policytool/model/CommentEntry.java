/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.jretools.policytool.model;

/**
 * Represents a comment entry which does not modify the access granted by the policy text which this belongs to.<br>
 * It may exist to:
 * <ul>
 *     <li>separate, tag the policy text
 *     <li>hold informations, comments for the author or the user of the policy text
 * </ul>
 */
public class CommentEntry extends PolicyEntry {

    /** Comment entries are not tokenized just holds the entry text "as is". */
    private String entryText;

    /**
     * Creates a new CommentEntry.
     * @param entryText policy entry text of the entry
     */
    public CommentEntry( final String entryText ) {
        setText( entryText );
    }

    @Override
    public String getText() {
        return entryText;
    }

    /**
     * Sets the text of the comment entry.
     * @param entryText
     */
    public void setText( final String entryText ) {
        this.entryText = entryText;
    }

}
