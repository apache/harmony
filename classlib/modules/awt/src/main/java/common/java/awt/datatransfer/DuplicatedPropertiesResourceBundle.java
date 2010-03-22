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
 * @author Michael Danilov
 */
package java.awt.datatransfer;

import java.util.*;
import java.io.*;

final class DuplicatedPropertiesResourceBundle extends ResourceBundle {

    private final Properties properties;

    public DuplicatedPropertiesResourceBundle(InputStream stream) throws IOException {
        properties = new Properties() {
            private static final long serialVersionUID = -4869518800983843099L;

            @SuppressWarnings("unchecked")
            @Override
            public Object put(Object key, Object value) {
                Object oldValue = get(key);

                if (oldValue == null) {
                    return super.put(key, value);
                }
                List<Object> list;

                if (oldValue instanceof String) {
                    list = new LinkedList<Object>();
                    list.add(oldValue);
                } else {
                    list = (List<Object>) oldValue;
                }
                list.add(value);

                return super.put(key, list);
            }
        };
        properties.load(stream);
    }

    @Override
    public Object handleGetObject(String key) {
        return properties.get(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<String> getKeys() {
        Enumeration<String> result = (Enumeration<String>)properties.propertyNames();

        if (parent == null) {
            return result;
        }

        ArrayList<String> keys = Collections.list(result);
        Enumeration<String> e = parent.getKeys();

        while (e.hasMoreElements()) {
            String key = e.nextElement();

            if (!keys.contains(key)) {
                keys.add(key);
            }
        }

        return Collections.enumeration(keys);
    }
}
