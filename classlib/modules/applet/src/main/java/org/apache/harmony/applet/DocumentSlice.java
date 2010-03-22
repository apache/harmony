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

import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AudioClip;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Collection of applets running in one document and loaded from the same code base,
 * implementation of <b>AppletContext</b> interface
 */
final class DocumentSlice implements AppletContext {

    final CodeBase codeBase;
    final Document document;
    
    private final List<Proxy> proxies = new ArrayList<Proxy>();
    private final Map<String, InputStream> streams = new HashMap<String, InputStream>();
    
    
    DocumentSlice(Document doc, CodeBase codeBase) {
        this.document = doc;
        this.codeBase = codeBase;
    }
    
    void add(Proxy p) {
        synchronized (proxies) {
            proxies.add(p);
        }
        codeBase.factory.add(p);
    }
    
    void remove(Proxy p) {
        codeBase.factory.remove(p);

        boolean empty;
        synchronized (proxies) {
            proxies.remove(p);
            empty = proxies.isEmpty();
        }
        if (empty) {
            codeBase.remove(this);
            document.remove(this);
        }
    }

    public Applet getApplet(String name) {
        
        synchronized (proxies) {
            for (Proxy p : proxies) {
                if (p.params.name.equals(name)) {
                    return p.getApplet();
                }
            }
            return null;
        }
    }

    public Enumeration<Applet> getApplets() {
        
        synchronized (proxies) {
            ArrayList<Applet> applets = new ArrayList<Applet>();
            for (Proxy p : proxies) {
                Applet a = p.getApplet();
                if (a != null) {
                    applets.add(a);
                }
            }
            return Collections.enumeration(applets);
        }
    }

    public AudioClip getAudioClip(URL url) {
        return new AudioClipImpl(url);
    }

    public Image getImage(URL url) {
        return Toolkit.getDefaultToolkit().getImage(url);
    }

    public InputStream getStream(String key) {

        synchronized (streams) {
            return streams.get(key);
        }
    }

    public Iterator<String> getStreamKeys() {

        synchronized (streams) {
            ArrayList<String> keys = new ArrayList<String>();
            for (String string : streams.keySet()) {
                keys.add(string);
            }
            return keys.iterator();
        }
    }

    public void setStream(String key, InputStream stream) throws IOException {

        synchronized (streams) {
            streams.put(key, stream);
        }
    }

    public void showDocument(URL url, String target) {
        codeBase.factory.showDocument(this, url, target);
    }

    public void showDocument(URL url) {
        this.showDocument(url, null);
    }

    public void showStatus(String status) {
        codeBase.factory.showStatus(this, status);
    }
}
