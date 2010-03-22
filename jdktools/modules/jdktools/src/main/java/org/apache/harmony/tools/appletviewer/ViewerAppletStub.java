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

import java.applet.AppletContext;
import java.applet.AppletStub;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.net.URL;

class ViewerAppletStub implements AppletStub {
    private final Component appletPane;
    private final AppletInfo appletInfo;
    private final AppletContext appletContext;

    ViewerAppletStub(Component appletPane, AppletInfo appletInfo) {
        this.appletPane = appletPane;
        this.appletInfo = appletInfo;
        this.appletContext = new ViewerAppletContext(appletInfo);
    }
    
    public boolean isActive() {
        return false;
    }
    
    public URL getDocumentBase() {
        return appletInfo.getDocumentBase();
    }
    
    public URL getCodeBase() {
        return appletInfo.getCodeBase();
    }
    
    public String getParameter(String name) {
        return appletInfo.getParameter(name);
    }
    
    public AppletContext getAppletContext() {
        return appletContext;
    }
    
    public void appletResize(int width, int height) {
        Component cmp = appletPane;

        appletPane.setPreferredSize(new Dimension(width, height));

        while (cmp != null) {
            cmp = cmp.getParent();

            if (cmp instanceof Window) {
                ((Window) cmp).pack();
                break;
            }
        }
    }
}