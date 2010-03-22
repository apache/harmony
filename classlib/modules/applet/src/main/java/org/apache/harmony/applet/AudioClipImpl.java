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

import java.applet.AudioClip;
import java.io.IOException;
import java.net.URL;

/**
 * Implementation of AudioClip interface
 */
public class AudioClipImpl implements AudioClip {
    
    @SuppressWarnings("unused") // TODO remove once used
    private final URL url;
    
    // TODO: introduce AudioClipPeer when sound API will be available

    public AudioClipImpl(URL url) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkConnect(url.getHost(), url.getPort());
            try {
                sm.checkPermission(url.openConnection().getPermission());
            } catch (IOException e) {
            }
        }
        this.url = url;
    }

    public void stop() {
        // TODO: implement when sound API will be available
        throw new UnsupportedOperationException();
    }

    public void loop() {
        // TODO: implement when sound API will be available
        throw new UnsupportedOperationException();
    }

    public void play() {
        // TODO: implement when sound API will be available
        throw new UnsupportedOperationException();
    }

}
