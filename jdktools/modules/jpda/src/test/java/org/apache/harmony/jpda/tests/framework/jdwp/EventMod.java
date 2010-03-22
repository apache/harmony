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
 * Created on 11.29.2004
 */
package org.apache.harmony.jpda.tests.framework.jdwp;

import org.apache.harmony.jpda.tests.framework.jdwp.Location;

/**
 * This class provides specific event modifiers for event request.
 */
public class EventMod {

    public class ModKind {
        public static final byte Count         = 1;

        public static final byte Conditional   = 2;

        public static final byte ThreadOnly    = 3;

        public static final byte ClassOnly     = 4;

        public static final byte ClassMatch    = 5;

        public static final byte ClassExclude  = 6;

        public static final byte LocationOnly  = 7;

        public static final byte ExceptionOnly = 8;

        public static final byte FieldOnly     = 9;

        public static final byte Step          = 10;

        public static final byte InstanceOnly  = 11;
        
        // new case for Java 6
        public static final byte SourceNameMatch = 12;
    }

    public byte modKind;
    public int  count;
    public int  exprID;

    // threadID
    public long     thread;

    // referenceTypeID
    public long     clazz;

    public String   classPattern;
    
    public String   sourceNamePattern;

    public Location loc;

    // referenceTypeID
    public long     exceptionOrNull;

    public boolean  caught;

    public boolean  uncaught;

    // referenceTypeID
    public long     declaring;

    // fieldID
    public long     fieldID;

    public int      size;

    public int      depth;

    // objectID
    public long     instance;

    /**
     * Creates new instance with empty data.
     */
    public EventMod() {
        modKind = 0;
        count = -1;
        exprID = -1;
        // threadID
        thread = -1;
        // referenceTypeID
        clazz = -1;
        classPattern = new String();
        sourceNamePattern = new String();
        loc = new Location();
        // referenceTypeID
        exceptionOrNull = -1;
        caught = false;
        uncaught = false;
        // referenceTypeID
        declaring = -1;
        // fieldID
        fieldID = -1;
        size = -1;
        depth = -1;
        // objectID
        instance = -1;
    }
}
