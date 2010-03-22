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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Applet context factory
 */
final class Factory {
    
    private final Callback callback;
    private final Map<URL, CodeBase> codeBases = Collections.synchronizedMap(new HashMap<URL, CodeBase>());
    private final Map<Integer, Proxy> allProxies = Collections.synchronizedMap(new HashMap<Integer, Proxy>());
    private final Map<Integer, Document> documents = Collections.synchronizedMap(new HashMap<Integer, Document>());
    
    Factory(Callback callback) {
        this.callback = callback;
    }
    
    CodeBase getCodeBase(URL url) {
        synchronized(codeBases) {
            CodeBase cb = codeBases.get(url);
            if (cb == null) {
                cb = new CodeBase(url, this);
                codeBases.put(url, cb);
            }
            return cb;
        }
    }
    
    void remove(CodeBase codeBase) {
        codeBases.remove(codeBase.codeBase);
    }

    void remove(Document doc) {
        documents.remove(Integer.valueOf(doc.id));
    }

    void dispose(int id) {
        Proxy p = allProxies.get(Integer.valueOf(id));
        if (p == null) {
            return;
        }
        p.docSlice.remove(p);
        p.dispose();
        
    }
    
    Document getDocument(URL docBase, int docId) {
        synchronized(documents) {
            Document doc;
            Integer objDocId = Integer.valueOf(docId);
            doc = documents.get(objDocId);
            if (doc == null) {
                doc = new Document(this, docBase, docId);
                documents.put(objDocId, doc);
            }
            return doc;
        }
    }
    
    void createAndRun(Parameters params) {
        
        CodeBase codeBase = getCodeBase(params.codeBase);
        Document doc = getDocument(params.documentBase, params.documentId);
        DocumentSlice ds = codeBase.getDocumentSlice(doc);
        doc.add(ds);
        
        Proxy p = new Proxy(ds, params);
        p.create();
    }

    void createAndRun(int id, long parentWindowId, URL documentBase,
                int documentId, URL codeBase, String className,
                String []paramStrings, String name, Object container) {
        Parameters params = new Parameters(id, parentWindowId, documentBase, documentId, codeBase, className, paramStrings, name, container);
        createAndRun(params);
    }
    
    void start(int id) {
        Proxy p = allProxies.get(Integer.valueOf(id));
        if (p != null) {
            p.start();
        }
    }
    
    void stop(int id) {
        Proxy p = allProxies.get(Integer.valueOf(id));
        if (p != null) {
            p.stop();
        }
    }
    
    void init(int id) {
        Proxy p = allProxies.get(Integer.valueOf(id));
        if (p != null) {
            p.init();
        }
    }
    
    void destroy(int id) {
        Proxy p = allProxies.get(Integer.valueOf(id));
        if (p != null) {
            p.destroy();
        }
    }
    
    void appletResize(Proxy p, int width, int height) {
        callback.appletResize(p.params.id, width, height);
    }
    
    void showStatus(DocumentSlice ds, String status) {
        callback.showStatus(ds.document.id, status);
    }
    
    void showDocument(DocumentSlice ds, URL url, String target) {
        callback.showDocument(ds.document.id, url, target);
    }

    void add(Proxy p) {
        allProxies.put(Integer.valueOf(p.params.id), p);
    }
    
    void remove(Proxy p) {
        allProxies.remove(Integer.valueOf(p.params.id));
    }
    
    void dump() {
        for (Proxy p : allProxies.values()) {
            System.err.println("app " + p.params.id + " " + 
                    " cb " + p.docSlice.codeBase.hashCode() + " " +
                    " doc " + p.params.documentId + " " +
                    p.params.codeBase + p.params.className + " " + 
                    (p.isActive() ? "active" : "stopped"));
        }
        for (CodeBase cb : codeBases.values()) {
            System.err.println("cb " + cb.hashCode() + " " + cb.threadGroup);
        }
    }
}

