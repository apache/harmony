/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Alexei Y. Zakharov
 */

package org.apache.harmony.jndi.provider.dns;


import java.lang.reflect.Field;

/**
 * Contains several utility methods. Is used from unit tests.
 * @author Alexei Zakharov
 */
public class TestMgr {
    
    /**
     * Retrieves the field information and set accessible to
     * <code>true</code>. 
     * 
     * @return retrieved <code>Field</code> object
     * @throws SecurityException is such operation is prohibited by system
     * security manager
     * @throws NoSuchFieldException if the filed with such name has not been
     * found
     */
    static Field getField(Object obj, String fieldName)
            throws SecurityException, NoSuchFieldException
    {
        Field field = obj.getClass().getDeclaredField(fieldName);

        field.setAccessible(true);
        return field;
    }

    /**
     * Retrieves the value of an integer field regardless to it's access
     * modifier.  
     * @param obj object to retrieve the field value from
     * @param fieldName name of the field
     * @return retrieved value of the field
     * @throws SecurityException is such operation is prohibited by system
     * security manager
     * @throws NoSuchFieldException if the filed with such name has not been
     * found
     */
    static int getIntField(Object obj, String fieldName)
            throws SecurityException, NoSuchFieldException,
                   IllegalArgumentException
    {
        Field field = getField(obj, fieldName);
        int n = -1;

        try {
            n = field.getInt(obj);
        }
        catch (IllegalAccessException e) {
            // FIXME
            e.printStackTrace();
        }
        return n;
    }

    /**
     * Retrieves the value of a boolean field regardless to it's access
     * modifier.  
     * @param obj object to retrieve the field value from
     * @param fieldName name of the field
     * @return retrieved value of the field
     * @throws SecurityException is such operation is prohibited by system
     * security manager
     * @throws NoSuchFieldException if the filed with such name has not been
     * found
     */
    static boolean getBoolField(Object obj, String fieldName)
            throws SecurityException, NoSuchFieldException,
                   IllegalArgumentException
    {
        Field field = getField(obj, fieldName);
        boolean  b = false;

        try {
            b = field.getBoolean(obj);
        }
        catch (IllegalAccessException e) {
            // FIXME
            e.printStackTrace();
        }
        return b;
    }
}
