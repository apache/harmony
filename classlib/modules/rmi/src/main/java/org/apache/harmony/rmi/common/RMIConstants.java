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
 * @author  Victor A. Martynov
 */
package org.apache.harmony.rmi.common;

import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * The constants of the RMI and Activation framework.
 * @author  Victor A. Martynov
 */
public interface RMIConstants {
    /*
     * -------------------------------------------------------------------------
     * proxy constants
     * -------------------------------------------------------------------------
     */

    /**
     * Default HTTP proxy port number to use
     * if {@link RMIProperties#PROXY_PORT_PROP} is not set.
     */
    public static final int HTTP_DEFAULT_PORT = 80;

    /*
     * ************************************************************************
     * Default values for RMI Activation.
     * ************************************************************************
     */
    /**
     * The default filename to store snapshots of the RMID's state.
     */
    public static final String DEFAULT_SNAPSHOT_FILE = "snapshot.rmid"; //$NON-NLS-1$

    /**
     * The default filename to store deltas of the RMID's state between snapshots.
     */
    public static final String DEFAULT_DELTA_FILE = "delta.rmid"; //$NON-NLS-1$

    /**
     * The default folder to store rmid log (snapshot and delta).
     */
    public static final String DEFAULT_LOG_FOLDER = "log"; //$NON-NLS-1$
    /**
     * "Well-Known" ObjID for Activation System.
     */
    public static final int ACTIVATION_SYSTEM_ID = 4;

    /**
     * The usage of RMID tool.
     *
     * rmi.console.0E=\nUsage: rmid <options>
     * rmi.console.0F=\n\nwhere <options> include:
     * rmi.console.10=\n-port <port>        Specify port for rmid to use
     * rmi.console.11=\n-log <directory>    Specify directory in which rmid writes log
     * rmi.console.12=\n-stop               Stop current invocation of rmid (for specified port)
     * rmi.console.13=\n-C<runtime flag>    Pass argument to each child process (activation group)
     * rmi.console.14=\n-J<runtime flag>    Pass argument to the java interpreter
     * rmi.console.15=\n-help               Prints this help message
     * rmi.console.16=\n------------------ Nonstandard options ------------------
     * rmi.console.17=\n-monitor            Starts rmid with monitoring on
     */
    public static final String RMID_USAGE =
        Messages.getString("rmi.console.0E")+ //$NON-NLS-1$
        Messages.getString("rmi.console.0F")+ //$NON-NLS-1$
        Messages.getString("rmi.console.10")+ //$NON-NLS-1$
        Messages.getString("rmi.console.11")+ //$NON-NLS-1$
        Messages.getString("rmi.console.12")+ //$NON-NLS-1$
        Messages.getString("rmi.console.13")+ //$NON-NLS-1$
        Messages.getString("rmi.console.14")+ //$NON-NLS-1$
        Messages.getString("rmi.console.15")+ //$NON-NLS-1$
        Messages.getString("rmi.console.16")+ //$NON-NLS-1$
        Messages.getString("rmi.console.17"); //$NON-NLS-1$

    /**
     * The default timeout that is given to the ActivationGroup VM to
     * start(milliseconds).
     */
    public static final long DEFAULT_ACTIVATION_EXECTIMEOUT = 30000;

    /**
     * The state of RMID (as a database for ActivationGroups and
     * ActivatableObjects) is saved on the harddrive and this operation is
     * known as "snapshot". This variable represents the interval between
     * the snapshots of the RMID current state.
     */
    public static final long DEFAULT_SNAPSHOTINTERVAL = 200;

    /**
     * The amount of activation groups that may be started concurrently.
     */
    public static final long MAX_CONCURRENT_STARTING_GROUPS = 3;

    /**
     * The default class name of the RMID monitor.
     *
     * @see org.apache.harmony.rmi.common.RMIProperties#ACTIVATION_MONITOR_CLASS_NAME_PROP
     * @see org.apache.harmony.rmi.activation.RmidMonitorAdapter
     */
    public static final String DEFAULT_ACTIVATION_MONITOR_CLASS_NAME =
        "org.apache.harmony.rmi.activation.RmidMonitorAdapter"; //$NON-NLS-1$

    /**
     * The default value of the activation group VM executable.
     *
     * @see RMIProperties#ACTIVATION_VM_EXECUTABLE_PROP
     */
    public static final String DEFAULT_ACTIVATION_VM_EXECUTABLE = "java"; //$NON-NLS-1$
}
