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
 
package org.apache.harmony.applet;

import java.net.URL;

public class PluginCallback implements Callback {
    private final long pluginInstance;
    
    public PluginCallback(long pluginInstance) {
        this.pluginInstance = pluginInstance;
    }
    
    public void showDocument(int documentId, URL url, String target) {
        showDocument(pluginInstance, documentId, url, target);
    }

    public void showStatus(int documentId, String status) {
        showStatus(pluginInstance, documentId, status);
    }

    public void appletResize(int appletId, int width, int height) {
        appletResize(pluginInstance, appletId, width, height);
    }

    private native void showDocument(long pluginInstance, int documentId, URL url, String target);

    private native void showStatus(long pluginInstance, int documentId, String status);

    private native void appletResize(long pluginInstance, int appletId, int width, int height);
}