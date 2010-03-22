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

package org.apache.harmony.jretools.policytool;

import java.awt.Font;

/**
 * Holds general and application-wide constants.
 */
public class Consts {

    /** Name of the application. */
    public static final String APPLICATION_NAME = "Policytool";

    /** X coordinate of the main frame on startup. */
    public static final int MAIN_FRAME_START_POS_X = 200;
    /** Y coordinate of the main frame on startup. */
    public static final int MAIN_FRAME_START_POS_Y = 100;
    /** Width of the main frame.                   */
    public static final int MAIN_FRAME_WIDTH       = 600;
    /** Height of the main frame.                  */
    public static final int MAIN_FRAME_HEIGHT      = 600;

    /** Font size in the direct editing panel.     */
    public static final int DIRECT_EDITING_FONT_SIZE = 13;
    /** Font in the direct editing panel.          */
    public static final Font DIRECT_EDITING_FONT     = new Font( "Courier New", Font.PLAIN, Consts.DIRECT_EDITING_FONT_SIZE );
    /** Tab size in the direct editing panel.      */
    public static final int DIRECT_EDITING_TAB_SIZE  = 4;

    /** Proper line separator for the running operating system. */
    public static final String NEW_LINE_STRING = System.getProperty( "line.separator" );

}
