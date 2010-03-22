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
 * @author Aleksei V. Ivaschenko 
 */ 

package org.apache.harmony.x.print;

import java.io.OutputStream;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.ServiceUIFactory;
import javax.print.StreamPrintService;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.ColorSupported;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.Sides;
import javax.print.event.PrintServiceAttributeListener;

/*
 * Image2PSStreamPrintService
 */
public class All2PSStreamPrintService extends StreamPrintService {
 
    private static final String outputFormat = "application/postscript";
    private static final MediaSizeName mediaSizes[] = {
            MediaSizeName.ISO_A3,
            MediaSizeName.ISO_A4,
            MediaSizeName.ISO_A5};

    private static final Class supportedAttributeCategories[] = {
            ColorSupported.class,
            Copies.class, 
            JobName.class, 
            Media.class, 
            MediaPrintableArea.class,  
            Sides.class};

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
                        
    public All2PSStreamPrintService(OutputStream outputstream,
            StreamPrintServiceFactory factory) {
        super(outputstream);
        if (factory == null) {
            throw new NullPointerException("factory is null");
        }   
    }

     public String getOutputFormat() {
        return outputFormat;
    }

    public Class[] getSupportedAttributeCategories() {
        Class copy_supportedAttrCats[] 
                     = new Class[supportedAttributeCategories.length];
        for (int i = 0; i < supportedAttributeCategories.length; i++) {
            copy_supportedAttrCats[i] = supportedAttributeCategories[i];
        }
        return copy_supportedAttrCats;
    }

    public boolean isAttributeCategorySupported(Class category) {
        if (category == null) {
            throw new NullPointerException("Argument category is null");
        }
        if (!(javax.print.attribute.Attribute.class).isAssignableFrom(category)) {
            throw new IllegalArgumentException(category.toString()
                    + " is not an Attribute");
        }

        for (int i = 0; i < supportedAttributeCategories.length; i++) {
            if (category.equals(supportedAttributeCategories[i]))
                return true;
        }
        return false;
    }
                    
        
    public String getName() {
        return "Convert source to Postscript language";
    }

    public DocFlavor[] getSupportedDocFlavors() {
        DocFlavor copy_supportedDocFlavors[] 
                       = new DocFlavor[supportedDocFlavors.length];
        for (int i = 0; i < supportedDocFlavors.length; i++) {
            copy_supportedDocFlavors[i] = supportedDocFlavors[i];
        }
        return copy_supportedDocFlavors;
    }
        
    public boolean isDocFlavorSupported(DocFlavor flavor) {
        if (flavor == null) {
            throw new NullPointerException("Argument flavor is null");
        }    
            
        for(int i = 0; i < supportedDocFlavors.length; i++){
            if(flavor.equals(supportedDocFlavors[i]))
                return true; 
            } 
        return false;
    }

    public DocPrintJob createPrintJob() {        
        return new All2PSDocPrintJob(this);
    }

    public int hashCode() {
        return getName().hashCode();
    }

    public boolean equals(Object obj) {
        return obj == this || (obj instanceof All2PSStreamPrintService)
            && ((All2PSStreamPrintService) obj).getName().equals(getName());
    }    

    public ServiceUIFactory getServiceUIFactory() {
        return null;
    }
    /* methods below this line must be completed */

    public PrintServiceAttributeSet getAttributes() {
        return null;
    }

    public void addPrintServiceAttributeListener(
            PrintServiceAttributeListener arg0) {
    }

    public void removePrintServiceAttributeListener(
            PrintServiceAttributeListener arg0) {
    }

    public Object getDefaultAttributeValue(Class arg0) {
        return null;
    }

    public PrintServiceAttribute getAttribute(Class arg0) {
        return null;
    }

    public boolean isAttributeValueSupported(Attribute arg0, DocFlavor arg1,
            AttributeSet arg2) {
        return false;
    }

    public AttributeSet getUnsupportedAttributes(DocFlavor arg0,
            AttributeSet arg1) {
        return arg1;
    }

    public Object getSupportedAttributeValues(Class arg0, DocFlavor arg1,
            AttributeSet arg2) {
        return null;
    }
        
}


