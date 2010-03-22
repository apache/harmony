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
 * @author Anton V. Karnachuk
 */

/**
 * Created on 18.03.2005
 */
package org.apache.harmony.jpda.tests.framework.jdwp;

/**
 * This class represents JDWP event packet, which is special kind of command packet.
 */
public class EventPacket extends CommandPacket {
    
    /**
     * Creates EventPacket from array of bytes including header and data sections.
     * 
     * @param p the JDWP packet, given as array of bytes.
     */
    public EventPacket(byte[] p) {
        super(p);
    }
}
