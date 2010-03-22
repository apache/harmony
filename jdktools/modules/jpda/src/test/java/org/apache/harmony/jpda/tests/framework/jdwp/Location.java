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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Aleksey V. Yantsen
 */

/**
 * Created on 10.25.2004
 */
package org.apache.harmony.jpda.tests.framework.jdwp;

import org.apache.harmony.jpda.tests.framework.jdwp.JDWPConstants;

/**
 * This class represents Location value in JDWP packet.
 */
public class Location {
    public byte tag;
    public long classID;
    public long methodID;
    public long index;

    /**
     * Creates new Location value with empty data.
     */
    public Location() {
        tag = JDWPConstants.Tag.NO_TAG;
        classID = 0;
        methodID = 0;
        index = 0;
    }

    /**
     * Creates new Location value with specified data.
     */
    Location(byte tag, long classID, long methodID, long index) {
        this.tag = tag;
        this.classID = classID;
        this.methodID = methodID;
        this.index = index;
    }

    /**
     * Converts Location to string format for printing.
     */
    public String toString() {
        return "Location: tag="+tag+", classID="+classID+", methodID="+methodID+", index="+index;
    }
    
    /**
     * Compares this with other Location object.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Location))
            return false;
        Location loc = (Location )obj;
        return classID == loc.classID && methodID == loc.methodID
                    && index == loc.index;
        
    }
}