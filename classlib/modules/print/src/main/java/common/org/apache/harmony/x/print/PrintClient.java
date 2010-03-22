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

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.PrintException;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttributeSet;

public interface PrintClient {
    
    /**
     * Prints document.
     * @param doc - document to print
     * @param set - set of printing request attributes.
     */
    public void print(Doc doc, PrintRequestAttributeSet has) throws PrintException;
    
    /**
     * Retrieves a list of doc flavors which are supported by
     * particular client.
     * @return an array of DocFlavor instances.
     */
    public DocFlavor[] getSupportedDocFlavors();
    
    /**
     * Retrieves attributes of client's print service.
     * @return set of print service attributes.
     */
    public PrintServiceAttributeSet getAttributes();

    /**
     * Retrieves categories of print request attributes
     * supported by client.
     * @return an array of print request attribute categories. 
     */
    public Class[] getSupportedAttributeCategories();

    /**
     * Retrieves default value of print request attribute
     * supported by client.
     * @param category category of print request attribute.
     * @return instance of print request attribute of specified
     * category, which describes its default value.
     */
    public Object getDefaultAttributeValue(Class category);

    /**
     * Checks whether print request attribute value is supported
     * by client in combination with doc's flavor and set of 
     * other attributes.
     * @param attribute print request attribute to check.
     * @param flavor flavor of document, for which check should
     * be performed.
     * @param attributes set of doc's print request attributes.
     * @return true if attribute value is supported, and otherwise
     * false.
     */
    boolean isAttributeValueSupported(Attribute attribute,
            DocFlavor flavor, AttributeSet attributes);
    
    /**
     * Retrieves all supported print request attribute values of
     * specified category in combination with specified doc's
     * flavor and set of print request attributes.
     * @param category category for which values should be
     * returned.
     * @param flavor flavor of document, for which check should
     * be performed.
     * @param attributes set of doc's print request attributes.
     * @return
     */    
    public Object getSupportedAttributeValues(Class category, DocFlavor flavor,
            AttributeSet attributes);
}
