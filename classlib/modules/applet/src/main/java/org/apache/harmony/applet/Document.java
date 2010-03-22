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
package org.apache.harmony.applet;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Representation of browser's (or other host app's) document that contains applets
 */
final class Document {

    final URL docBase;
    final int id;
    private final Set<DocumentSlice> slices = new HashSet<DocumentSlice>();
    final Factory factory;

    Document(Factory factory, URL url, int id) {
        docBase = url;
        this.id = id;
        this.factory = factory;
    }
    
    void add(DocumentSlice ds) {
        synchronized (slices) {
            slices.add(ds);
        }
    }
    
    void remove(DocumentSlice ds) {
        synchronized(slices) {
            slices.remove(ds);
            if (slices.isEmpty()) {
                factory.remove(this);
            }
        }
    }
}
