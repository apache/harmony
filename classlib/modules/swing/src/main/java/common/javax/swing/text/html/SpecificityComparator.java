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
package javax.swing.text.html;

import java.util.Comparator;

final class SpecificityComparator implements Comparator {
    public static final SpecificityComparator compator =
        new SpecificityComparator();

    private SpecificityComparator() {
    }

    public int compare(final Selector arg0, final Selector arg1) {
        return -arg0.specificity.compareTo(arg1.specificity);
    }

    public int compare(final Object arg0, final Object arg1) {
        return compare((Selector)arg0, (Selector)arg1);
    }
}
