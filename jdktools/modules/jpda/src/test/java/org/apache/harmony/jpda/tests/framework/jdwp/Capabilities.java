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
 * Created on 12.01.2004
 */
package org.apache.harmony.jpda.tests.framework.jdwp;

/**
 * This class defines capabilities from JDWP specifications.
 */
public class Capabilities {
    // Target VM Capabilities
    public boolean canWatchFieldModification        = false;

    public boolean canWatchFieldAccess              = false;

    public boolean canGetBytecodes                  = false;

    public boolean canGetSyntheticAttribute         = false;

    public boolean canGetOwnedMonitorInfo           = false;

    public boolean canGetCurrentContendedMonitor    = false;

    public boolean canGetMonitorInfo                = false;

    public boolean canRedefineClasses               = false;

    public boolean canAddMethod                     = false;

    public boolean canUnrestrictedlyRedefineClasses = false;

    public boolean canPopFrames                     = false;

    public boolean canUseInstanceFilters            = false;

    public boolean canGetSourceDebugExtension       = false;

    public boolean canRequestVMDeathEvent           = false;

    public boolean canSetDefaultStratum             = false;

    public boolean canGetInstanceInfo               = false;

    public boolean reserved17                       = false;

    public boolean canGetMonitorFrameInfo           = false;

    public boolean canUseSourceNameFilters          = false;

    public boolean canGetConstantPool               = false;

    public boolean canForceEarlyReturn              = false;

    public boolean reserved22                       = false;

    public boolean reserved23                       = false;

    public boolean reserved24                       = false;

    public boolean reserved25                       = false;

    public boolean reserved26                       = false;

    public boolean reserved27                       = false;

    public boolean reserved28                       = false;

    public boolean reserved29                       = false;

    public boolean reserved30                       = false;

    public boolean reserved31                       = false;

    public boolean reserved32                       = false;
}