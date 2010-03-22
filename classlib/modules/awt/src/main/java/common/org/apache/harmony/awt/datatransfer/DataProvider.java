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


/**
 * Unified representation of transferable data,
 * either obtained from or prepared for native clipboard and/or drag&drop
 */
public interface DataProvider {
    /**
     * More information about MIME types and Drag&Drop can be found at  
     * http://java.sun.com/j2se/1.5.0/docs/api/java/awt/datatransfer/DataFlavor.html
     * http://java.sun.com/j2se/1.5.0/docs/guide/dragndrop/spec/dnd1.html
     */
    
    public static final String FORMAT_TEXT = "text/plain"; //$NON-NLS-1$
    public static final String FORMAT_FILE_LIST = "application/x-java-file-list"; //$NON-NLS-1$
    public static final String FORMAT_URL = "application/x-java-url"; //$NON-NLS-1$
    public static final String FORMAT_HTML = "text/html"; //$NON-NLS-1$
    public static final String FORMAT_IMAGE = "image/x-java-image"; //$NON-NLS-1$

    public static final String TYPE_IMAGE = "image/x-java-image"; //$NON-NLS-1$
    public static final String TYPE_SERIALIZED = 
                                    "application/x-java-serialized-object"; //$NON-NLS-1$
    public static final String TYPE_PLAINTEXT = "text/plain"; //$NON-NLS-1$
    public static final String TYPE_HTML = "text/html"; //$NON-NLS-1$
    public static final String TYPE_URL = "application/x-java-url"; //$NON-NLS-1$
    public static final String TYPE_TEXTENCODING = 
                                    "application/x-java-text-encoding"; //$NON-NLS-1$
    public static final String TYPE_FILELIST = "application/x-java-file-list"; //$NON-NLS-1$
    public static final String TYPE_URILIST = "text/uri-list"; //$NON-NLS-1$

    public static final DataFlavor urlFlavor = 
        new DataFlavor("application/x-java-url;class=java.net.URL", "URL"); //$NON-NLS-1$ //$NON-NLS-2$
    
    public static final DataFlavor uriFlavor = 
        new DataFlavor("text/uri-list", "URI"); //$NON-NLS-1$ //$NON-NLS-2$

    
    public String[] getNativeFormats();
    
    public boolean isNativeFormatAvailable(String nativeFormat);
    
    public String getText();
    
    public String[] getFileList();
    
    public String getURL();

    public String getHTML();
    
    public RawBitmap getRawBitmap();
    
    public byte[] getSerializedObject(Class<?> clazz);
}
