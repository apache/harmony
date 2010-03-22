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
 * @author Oleg V. Khaschansky
 */
/**
 * This file is based on WGL headers.
 */

package org.apache.harmony.awt.nativebridge.windows;

public interface WGLDefs {
    public static final int WGL_DRAW_TO_WINDOW_ARB         = 0x2001;
    public static final int WGL_PIXEL_TYPE_ARB             = 0x2013;
    public static final int WGL_TYPE_RGBA_ARB              = 0x202B;
    public static final int WGL_DRAW_TO_PBUFFER_ARB        = 0x202D;
    public static final int WGL_STENCIL_BITS_ARB           = 0x2023;
    public static final int WGL_ALPHA_BITS_ARB             = 0x201B;
    public static final int WGL_DOUBLE_BUFFER_ARB          = 0x2011;
    public static final int WGL_ACCELERATION_ARB           = 0x2003;
    public static final int WGL_FULL_ACCELERATION_ARB      = 0x2027;
}
