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
 * @author Roman I. Chernyatchik
 */
package org.apache.harmony.x.swing.text.html;

import java.io.Serializable;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class HTMLIconFactory implements Serializable {
    private static HTMLIconFactory instance;

    private static Icon loadingFailedIcon;
    private static Icon loadingImageIcon;
    private static Icon noImageIcon;

    public static Icon getLoadingFailedIcon() {

        if (loadingFailedIcon == null) {
            loadingFailedIcon = loadIcon("icons/loadingFailedIcon.gif");
        }
        return loadingFailedIcon;
    }

    public static Icon getLoadingImageIcon() {
        if (loadingImageIcon == null) {
            loadingImageIcon = loadIcon("icons/loadingImageIcon.gif");
        }
        return loadingImageIcon;
    }

    public static Icon getNoImageIcon() {
        if (noImageIcon == null) {
            noImageIcon = loadIcon("icons/noImageIcon.gif");
        }
        return noImageIcon;
    }

    protected HTMLIconFactory() {
    }

    private static URL getResource(final String resource) {
        if (instance == null) {
            instance = new HTMLIconFactory();
        }
        return instance.getClass().getResource(resource);
    }

    private static Icon loadIcon(final String resource) {
        return new ImageIcon(getResource(resource));
    }
}
