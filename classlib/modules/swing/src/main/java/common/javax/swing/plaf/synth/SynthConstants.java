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

package javax.swing.plaf.synth;

/**
 * The SynthConstants contains possible states for the Component. The states
 * divided into 2 groups: primary (at least one of the states should be present)
 * and additional (component state may also contain this states)
 */
public interface SynthConstants {

    /** The component (a JButton) is marked as default (additional) */
    static int DEFAULT = 1024;

    /** The component is disabled (primary) */
    static int DISABLED = 8;

    /** The component is enabled (primary) */
    static int ENABLED = 1;

    /** The component is focused (additional) */
    static int FOCUSED = 256;

    /** The mouse arrow is over the component (primary) */
    static int MOUSE_OVER = 2;

    /** The component is in pressed state (primary) */
    static int PRESSED = 4;

    /** The component is selected (additional) */
    static int SELECTED = 512;

}
