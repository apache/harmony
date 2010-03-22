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

package org.apache.harmony.tools.appletviewer;

import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AudioClip;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

class ViewerAppletContext implements AppletContext {
    private static final HashMap<String, Applet> namedApplets = new HashMap<String, Applet>();
    private static final Vector<Applet> applets = new Vector<Applet>(1, 1);
    private static final HashMap<URL, ViewerClassLoader> loaders = 
        new HashMap<URL, ViewerClassLoader>();

    private final AppletInfo appletInfo;

    static Applet loadApplet(AppletInfo appletInfo) throws Exception {

        String width = appletInfo.getParameter("WIDTH");
        if(width != null) {
            appletInfo.setWidth(width);
        } else {
            System.err.println("Warning: <" + appletInfo.getTag() +"> tag requires width attribute.");
            System.exit(-1);
        }

        String heigth = appletInfo.getParameter("HEIGHT");
        if(heigth != null) {
            appletInfo.setHeight(heigth);
        } else {
            System.err.println("Warning: <" + appletInfo.getTag() +"> tag requires height attribute.");
            System.exit(-1);
        }
  
        String code = appletInfo.getParameter("CODE");
        if(code == null || code.equals("")){
            System.err.println("Warning: <" + appletInfo.getTag() +"> tag requires code attribute.");
            System.exit(0);
        }

        code = (code.endsWith(".class"))?code.substring(0, code.length()-6):code;
        
        appletInfo.setCodeBase(appletInfo.getParameter("CODEBASE"));

        URL codeBase = appletInfo.getCodeBase();

        ViewerClassLoader loader = loaders.get(codeBase);
        if(loader == null){
            loader = new ViewerClassLoader();
            loaders.put(codeBase, loader);
        }

        loader.addURLs(appletInfo.getClassLoaderURLs());
        Class clz = loader.loadClass(code);
        Applet applet = (Applet)clz.newInstance();

        applets.add(applet);

        String name = appletInfo.getParameter("NAME");
        if(name != null && name != "") namedApplets.put(name, applet);

        applet.setStub(new ViewerAppletStub(applet, appletInfo));

        return applet;
    }

    public ViewerAppletContext(AppletInfo appletInfo) {
        this.appletInfo = appletInfo;
    }

    public Applet getApplet(String name) {
        return namedApplets.get(name);
    }

    public Enumeration<Applet> getApplets() {
        return applets.elements();
    }

    public AudioClip getAudioClip(URL url) {
        return new ViewerAudioClip(url);
    }

    public Image getImage(URL url) {
        return Toolkit.getDefaultToolkit().createImage(url);
    }

    public InputStream getStream(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    public Iterator<String> getStreamKeys() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setStream(String key, InputStream stream) throws IOException {
        // TODO Auto-generated method stub

    }

    public void showDocument(URL url) {
        // TODO Auto-generated method stub

    }

    public void showDocument(URL url, String target) {
        // TODO Auto-generated method stub

    }

    public void showStatus(String status) {
        appletInfo.setStatus(status);
    }

    void remove(Applet applet){
        String name = applet.getParameter("NAME");
        synchronized(namedApplets){
            namedApplets.remove(name);
        }

        synchronized(applets){
            applets.remove(applet);
        }
    }

    private static class ViewerClassLoader extends URLClassLoader{

        public ViewerClassLoader(){
            super(new URL[0]);
        }

        public void addURL(URL url){
            URL[] urls = getURLs();
            boolean exists = false;
            for(int i = 0; i < urls.length; i++){
                if(urls[i].equals(url)){
                    exists = true;
                    break;
                }
            }
            if(!exists) super.addURL(url);
        }

        public void addURLs(URL[] urls){
            if(urls == null) return;
            for(int i = 0; i < urls.length; i++){
                addURL(urls[i]);
            }
        }

    }
}
