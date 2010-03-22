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
package java.applet;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

import org.apache.harmony.applet.AudioClipImpl;
import org.apache.harmony.awt.AWTPermissionCollection;
import org.apache.harmony.awt.FieldsAccessor;


public class Applet extends Panel {
    
    private static final long serialVersionUID = -5836846270535785031L;

    private transient AppletStub stub;
    
    private transient AccessibleContext accessibleContext;

    /**  monitor for applet's private data */
    private final transient Object appletLock = new Object(); 
    protected class AccessibleApplet extends Panel.AccessibleAWTPanel {

        private static final long serialVersionUID = 8127374778187708896L;
        
        protected AccessibleApplet() {
            super();
        }
        
        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.FRAME;
        }
        
        @Override
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet set = super.getAccessibleStateSet();
            Window owner = null;
            for (Component c = Applet.this; c != null; c = c.getParent()) {
                if (c instanceof Window) {
                    owner = (Window)c;
                    break;
                }
            }
            
            if (owner != null) {
                if (owner.isActive()) {
                    set.add(AccessibleState.ACTIVE);
                }
            }
            return set;
        }
    }

    public Applet() throws HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
    }

    public void init() {
        // do nothing by specification
    }

    public void start() {
        // do nothing by specification
    }

    public void destroy() {
        // do nothing by specification
    }

    public void stop() {
        // do nothing by specification
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public void resize(int width, int height) {
        synchronized(appletLock) {
            stub.appletResize(width, height);
        }
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public void resize(Dimension d) {
        synchronized(appletLock) {
            stub.appletResize(d.width, d.height);
        }
    }

    public URL getCodeBase() {
        synchronized(appletLock) {
            return stub.getCodeBase();
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        synchronized(appletLock) {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleApplet();
            }
            return accessibleContext;
        }
    }

    public AppletContext getAppletContext() {
        synchronized(appletLock) {
            return stub.getAppletContext();
        }
    }

    public String getAppletInfo() {
        // return null by specification
        return null;
    }

    public AudioClip getAudioClip(URL url, String name) {
        return getAppletContext().getAudioClip(appendURL(url, name));
    }

    public AudioClip getAudioClip(URL url) {
        return getAppletContext().getAudioClip(url);
    }

    public URL getDocumentBase() {
        synchronized(appletLock) {
            return stub.getDocumentBase();
        }
    }

    public Image getImage(URL url) {
        return getAppletContext().getImage(url);
    }

    public Image getImage(URL url, String name) {
        return getAppletContext().getImage(appendURL(url, name));
    }

    @Override
    public Locale getLocale() {
        return super.getLocale();
    }

    public String getParameter(String name) {
        synchronized(appletLock) {
            return stub.getParameter(name);
        }
    }

    public String[][] getParameterInfo() {
        // return null by specification
        return null;
    }

    public boolean isActive() {
        synchronized(appletLock) {
            return stub.isActive();
        }
    }

    public static final AudioClip newAudioClip(URL url) {
        return new AudioClipImpl(url);
    }

    public void play(URL url, String name) {
        AudioClip clip = getAudioClip(url, name);
        clip.play();
    }

    public void play(URL url) {
        AudioClip clip = getAudioClip(url);
        clip.play();
    }

    public final void setStub(AppletStub stub) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(AWTPermissionCollection.SET_APPLET_STUB_PERMISSION);
        }
        synchronized(appletLock) {
            this.stub = stub;
        }
    }

    public void showStatus(String msg) {
        synchronized(appletLock) {
            stub.getAppletContext().showStatus(msg);
        }
    }

    private static URL appendURL(URL url, String name) {
        try {
            return new URL(url, name);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private void readObject(ObjectInputStream stream) 
            throws IOException, ClassNotFoundException {

        stream.defaultReadObject();
        FieldsAccessor accessor = new FieldsAccessor(Applet.class, this);
        accessor.set("appletLock", new Object()); 
    }

}
