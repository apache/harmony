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

package javax.print;

import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.event.PrintServiceAttributeListener;

public interface PrintService {
    void addPrintServiceAttributeListener(PrintServiceAttributeListener listener);

    DocPrintJob createPrintJob();

    boolean equals(Object object);

    <T extends PrintServiceAttribute> T getAttribute(Class<T> category);

    PrintServiceAttributeSet getAttributes();

    Object getDefaultAttributeValue(Class<? extends Attribute> category);

    String getName();

    ServiceUIFactory getServiceUIFactory();

    Class<?>[] getSupportedAttributeCategories();

    Object getSupportedAttributeValues(Class<? extends Attribute> category, DocFlavor flavor,
            AttributeSet attributes);

    DocFlavor[] getSupportedDocFlavors();

    AttributeSet getUnsupportedAttributes(DocFlavor flavor, AttributeSet attributes);

    int hashCode();

    boolean isAttributeCategorySupported(Class<? extends Attribute> category);

    boolean isAttributeValueSupported(Attribute attrval, DocFlavor flavor,
            AttributeSet attributes);

    boolean isDocFlavorSupported(DocFlavor flavor);

    void removePrintServiceAttributeListener(PrintServiceAttributeListener listener);
}
