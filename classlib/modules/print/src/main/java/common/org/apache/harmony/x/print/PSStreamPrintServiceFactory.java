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
 * @author Igor A. Pyankov 
 */ 

package org.apache.harmony.x.print;

import java.io.OutputStream;

import javax.print.DocFlavor;
import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;

/*
 * PSStreamPrintServiceFactory  
 */
public class PSStreamPrintServiceFactory extends StreamPrintServiceFactory {

    private static final String mimeType = "application/postscript";
    private static final DocFlavor supportedDocFlavors[] = {
            DocFlavor.SERVICE_FORMATTED.PRINTABLE,
            DocFlavor.SERVICE_FORMATTED.PAGEABLE,
            DocFlavor.BYTE_ARRAY.GIF, 
            DocFlavor.INPUT_STREAM.GIF, 
            DocFlavor.URL.GIF,
            DocFlavor.BYTE_ARRAY.JPEG, 
            DocFlavor.INPUT_STREAM.JPEG, 
            DocFlavor.URL.JPEG,
            DocFlavor.BYTE_ARRAY.PNG, 
            DocFlavor.INPUT_STREAM.PNG, 
            DocFlavor.URL.PNG};

    public String getOutputFormat() {
        return mimeType;
    }
    
    public DocFlavor[] getSupportedDocFlavors() {
        DocFlavor copy_supportedDocFlavors[] 
                               = new DocFlavor[supportedDocFlavors.length];
        for (int i = 0; i < supportedDocFlavors.length; i++) {
            copy_supportedDocFlavors[i] = supportedDocFlavors[i];
        }
        return copy_supportedDocFlavors;
    }

    public StreamPrintService getPrintService(OutputStream outputstream) {     
        return new All2PSStreamPrintService(outputstream, this);       
    }
}


