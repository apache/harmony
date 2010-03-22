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
 * @author Pavel Dolgov
 */
package org.apache.harmony.awt.datatransfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.SystemFlavorMap;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * TextFlavor
 */
public class TextFlavor {

    public static final Class<?>[] unicodeTextClasses = { 
            String.class, Reader.class, CharBuffer.class, char[].class };

    public static final Class<?>[] charsetTextClasses = { 
            InputStream.class, ByteBuffer.class, byte[].class };
    
    public static void addUnicodeClasses(SystemFlavorMap fm,
                                         String nat,
                                         String subType) {
        for (int i = 0; i < unicodeTextClasses.length; i++) {
            String type = "text/" + subType; //$NON-NLS-1$
            String params = ";class=\"" +  //$NON-NLS-1$
                    unicodeTextClasses[i].getName() + "\""; //$NON-NLS-1$
            DataFlavor f = new DataFlavor(type + params, type);
            fm.addFlavorForUnencodedNative(nat, f);
            fm.addUnencodedNativeForFlavor(f, nat);
        }
    }

    public static void addCharsetClasses(SystemFlavorMap fm, 
                                         String nat,
                                         String subType, 
                                         String charset) {
        for (int i = 0; i < charsetTextClasses.length; i++) {
            String type = "text/" + subType; //$NON-NLS-1$
            String params = ";class=\"" +  //$NON-NLS-1$
                    charsetTextClasses[i].getName() + "\"" +  //$NON-NLS-1$
                    ";charset=\"" + charset + "\""; //$NON-NLS-1$ //$NON-NLS-2$
            DataFlavor f = new DataFlavor(type + params, type);
            fm.addFlavorForUnencodedNative(nat, f);
            fm.addUnencodedNativeForFlavor(f, nat);
        }
    }
}