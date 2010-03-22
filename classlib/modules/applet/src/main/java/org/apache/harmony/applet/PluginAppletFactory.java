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
import java.util.HashMap;

public class PluginAppletFactory {

    private static HashMap<Long, Factory> factories = new HashMap<Long, Factory>();
    
    private static Factory getFactory(long pluginInstance) {
        Long inst = Long.valueOf(pluginInstance);
        Factory f = factories.get(inst);
        if (f == null) {
            f = new Factory(new PluginCallback(pluginInstance));
            factories.put(inst, f);
        }
        
        return f;
    }
    
    public static void createAndRun(long pluginInstance, int id, long parentWindowId, URL documentBase,
                int documentId, URL codeBase, String className,
                String []paramStrings, String name, Object container) {
        Factory f = getFactory(pluginInstance);
        f.createAndRun(id, parentWindowId, documentBase, documentId, codeBase, className, paramStrings, name, container);
    }    
}
