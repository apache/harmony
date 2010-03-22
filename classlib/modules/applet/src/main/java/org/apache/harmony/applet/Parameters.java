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
import java.util.Map;
import java.util.HashMap;

/**
 * Applet's startup parameters
 */
final class Parameters {

    /** applet parameters provided by &lt;param&gt; tags */
    final Map<String, String> parameters;
    /** the location the document comes from */
    final URL documentBase;
    /** document's id from the host application */
    final int documentId;
    /** the location applet comes from */
    final URL codeBase;
    /** applet's id from host application */
    final int id;
    /** applet's class name (without suffix '.class') */
    final String className;
    /** applet's name in the document */
    final String name;
    /** host app's window to put applet into */
    final long parentWindowId;
    /** Java app's Container to put applet into */
    final Object container;

    Parameters( int id,
                long parentWindowId,
                URL documentBase,
                int documentId,
                URL codeBase,
                String className,
                Map<String, String> parameters,
                String name,
                Object container) {

        this.id = id;
        this.parentWindowId = parentWindowId;
        this.parameters = parameters;
        this.documentBase = documentBase;
        this.documentId = documentId;
        this.codeBase = codeBase;
        this.className = className;
        this.name = name;
        this.container = container;
    }

    Parameters(int id,
                long parentWindowId,
                URL documentBase,
                int documentId,
                URL codeBase,
                String className,
                String []paramStrings,
                String name,
                Object container) {

        this.id = id;
        this.parentWindowId = parentWindowId;
        this.documentBase = documentBase;
        this.documentId = documentId;
        this.codeBase = codeBase;
        this.className = className;
        this.name = name;
        this.container = container;
        
        this.parameters = new HashMap<String, String>(paramStrings.length/2);
        for (int i = 0; i+1 < paramStrings.length; i += 2)
            parameters.put(paramStrings[i], paramStrings[i+1]);
    }

    String getParameter(String name) {
        return parameters.get(name);
    }

}
