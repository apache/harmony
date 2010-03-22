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

import java.awt.Rectangle;

public interface AccessibleExtendedText {
    static final int LINE = 4;
    static final int ATTRIBUTE_RUN = 5;
    String getTextRange(int startIndex, int endIndex);
    AccessibleTextSequence getTextSequenceAt(int part, int index);
    AccessibleTextSequence getTextSequenceAfter(int part, int index);
    AccessibleTextSequence getTextSequenceBefore(int part, int index);
    Rectangle getTextBounds(int startIndex, int endIndex);
}
