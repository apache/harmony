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
package org.apache.harmony.vm;

/**
 * 
 * 
 * @author Alexey V. Varlamov
 */
public class ClassFormat {
    
    public static final int ACC_BRIDGE = 0x0040;
    
    public static final int ACC_VARARGS = 0x0080;
    
    public static final int ACC_SYNTHETIC = 0x1000;
    
    public static final int ACC_ENUM = 0x4000;

    public static final int ACC_ANNOTATION = 0x2000;
    
    public static final int ACC_INTERFACE = 0x0200;
}
