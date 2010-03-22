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
 * @author Anton Avtamonov
 */

package javax.swing.event;

import java.util.EventObject;

public class ListSelectionEvent extends EventObject {
    private final int firstIndex;
    private final int lastIndex;
    private final boolean isAdjusting;

    public ListSelectionEvent(final Object source, final int firstIndex, final int lastIndex, final boolean isAdjusting) {
        super(source);
        this.firstIndex = firstIndex;
        this.lastIndex = lastIndex;
        this.isAdjusting = isAdjusting;
    }

    public int getFirstIndex() {
        return firstIndex;
    }

    public int getLastIndex() {
        return lastIndex;
    }

    public boolean getValueIsAdjusting() {
        return isAdjusting;
    }

    /*
     * The format of the string is based on 1.5 release behavior
     * which can be revealed using the following code:
     *
     *     Object obj = new ListSelectionEvent(new JList(), 0, 1, false);
     *     System.out.println(obj.toString());
     */
    public String toString() {
        return this.getClass().getName() + "[source="+source+" firstIndex= " + firstIndex + " lastIndex= " + lastIndex + " isAdjusting= " + isAdjusting + " ]";
    }
}
