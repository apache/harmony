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
 * @author Dennis Ushakov
 */

package javax.accessibility;

import javax.swing.text.AttributeSet;

public interface AccessibleEditableText extends AccessibleText {
    void setTextContents(String s);
    void insertTextAtIndex(int index, String s);
    String getTextRange(int startIndex, int endIndex);
    void delete(int startIndex, int endIndex);
    void cut(int startIndex, int endIndex);
    void paste(int startIndex);
    void replaceText(int startIndex, int endIndex, String s);
    void selectText(int startIndex, int endIndex);
    void setAttributes(int startIndex, int endIndex, AttributeSet as);
}
