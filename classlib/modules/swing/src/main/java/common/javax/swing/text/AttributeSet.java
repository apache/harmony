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
 * @author Alexey A. Ivanov
 */
package javax.swing.text;

import java.util.Enumeration;

public interface AttributeSet {

    interface CharacterAttribute {
    }

    interface ColorAttribute {
    }

    interface FontAttribute {
    }

    interface ParagraphAttribute {
    }

    Object NameAttribute = StyleConstants.NameAttribute;

    Object ResolveAttribute = StyleConstants.ResolveAttribute;

    boolean containsAttribute(Object key, Object value);

    boolean containsAttributes(AttributeSet attrSet);

    AttributeSet copyAttributes();

    Object getAttribute(Object key);

    int getAttributeCount();

    Enumeration<?> getAttributeNames();

    AttributeSet getResolveParent();

    boolean isDefined(Object key);

    boolean isEqual(AttributeSet attrSet);

}

