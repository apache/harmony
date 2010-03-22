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
 * @author Vitaly A. Provodin
 */

/**
 * Created on 26.01.2005
 */
package org.apache.harmony.jpda.tests.framework;

import java.util.HashMap;

/**
 * This class provides access to options for running JPDA tests.
 * <p>
 * The settings are presented as a set of getters and setters for test options,
 * which can be implemented in different ways. In this implementation test
 * options are implemented via VM system properties, which can be specified
 * using option '-D' in VM command line.
 * <p>
 * The following options are currently recognized:
 * <ul>
 * <li><code>jpda.settings.debuggeeJavaHome</code>
 *   - path to Java bundle to run debuggee on
 * <li><code>jpda.settings.debuggeeJavaExec</code>
 *   - name of Java executable to run debuggee on
 * <li><code>jpda.settings.debuggeeJavaPath</code>
 *   - full path to Java executable to run debuggee on
 * <li><code>jpda.settings.debuggeeAgentName</code>
 *   - name of agent native library
 * <li><code>jpda.settings.debuggeeAgentExtraOptions</code>
 *   - extra options for agent
 * <li><code>jpda.settings.debuggeeClassName</code>
 *   - full name of class to run debuggee with
 * <li><code>jpda.settings.debuggeeVMExtraOptions</code>
 *   - extra options to run debuggee with
 * <li><code>jpda.settings.debuggeeSuspend</code>
 *   - debuggee suspend mode ("y"|"n")
 * <li><code>jpda.settings.transportWrapperClass</code>
 *   - class name of TransportWrapper implementation
 * <li><code>jpda.settings.transportAddress</code>
 *   - address for JDWP connection
 * <li><code>jpda.settings.connectorKind</code>
 *   - type of JDWP connection (attach or listen)
 * <li><code>jpda.settings.syncPort</code>
 *   - port number for sync connection
 * <li><code>jpda.settings.timeout</code>
 *   - timeout used in JPDA tests
 * <li><code>jpda.settings.waitingTime</code>
 *   - timeout for waiting events
 * </ul>
 * <li><code>jpda.settings.verbose</code>
 *   - flag that disables (default) or enables writing messages to the log
 * </ul>
 * All options have default values, if they are not specified.
 *  
 */
public class TestOptions {

    /** Default timeout value for various operations. */
    public static final int DEFAULT_TIMEOUT = 1 * 60 * 1000; // 1 minute

    /** Default time interval for waiting for various events. */
    public static final int DEFAULT_WAITING_TIME = DEFAULT_TIMEOUT;
    
    /** Default static address for transport connection. */
    public static final String DEFAULT_ATTACHING_ADDRESS = "127.0.0.1:9898";

    /** Default port number for sync connection. */
    public static final String DEFAULT_STATIC_SYNC_PORT = "9797";

    /** Default port number for sync connection. */
    public static final int DEFAULT_SYNC_PORT = 0;

    /** Default class name for transport wrapper. */
    public static final String DEFAULT_TRANSPORT_WRAPPER 
        = "org.apache.harmony.jpda.tests.framework.jdwp.SocketTransportWrapper";

    /** Default aclass name for debuggee application. */
    public static final String DEFAULT_DEBUGGEE_CLASS_NAME
        = "org.apache.harmony.jpda.tests.jdwp.share.debuggee.HelloWorld";

    // current waiting time value (negative means using default value)
    private long waitingTime = -1;

    // current timeout value (negative means using default value)
    private long timeout = -1;

    // internally set property values
    private HashMap internalProperties = new HashMap();

    /**
     * Constructs an instance of this class.
     */
    public TestOptions() {
        super();
    }

    /**
     * Returns path to Java bundle to run debuggee on.
     * 
     * @return option "jpda.settings.debuggeeJavaHome" or system property
     *         "java.home" by default.
     */
    public String getDebuggeeJavaHome() {
        return getProperty("jpda.settings.debuggeeJavaHome", getProperty("java.home", null));
    }

    /**
     * Returns name of Java executable to run debuggee on.
     * 
     * @return option "jpda.settings.debuggeeJavaExec" or "java" by default.
     */
    public String getDebuggeeJavaExec() {
        return getProperty("jpda.settings.debuggeeJavaExec", "java");
    }

    /**
     * Returns full path to Java executable to run debuggee on.
     * 
     * @return option "jpda.settings.debuggeeJavaPath" or construct path from
     *         getDebuggeeJavaHome() and getDebuggeeJavaExec() by default.
     */
    public String getDebuggeeJavaPath() {
        return getProperty("jpda.settings.debuggeeJavaPath",
                getDebuggeeJavaHome() + "/bin/" + getDebuggeeJavaExec());
    }

    /**
     * Returns class name of TransportWrapper implementation.
     * 
     * @return option "jpda.settings.transportWrapperClass" or
     *         DEFAULT_TRANSPORT_WRAPPER by default.
     */
    public String getTransportWrapperClassName() {
        return getProperty("jpda.settings.transportWrapperClass",
                DEFAULT_TRANSPORT_WRAPPER);
    }

    /**
     * Returns address for JDWP connection or null for dynamic address.
     * 
     * @return option "jpda.settings.transportAddress" or null by default.
     */
    public String getTransportAddress() {
        return getProperty("jpda.settings.transportAddress", null);
    }
    
    /**
     * Sets address to attach to debuggee.
     * 
     * @param address to attach
     */
    public void setTransportAddress(String address) {
        setProperty("jpda.settings.transportAddress", address);
    }

    /**
     * Returns name of JDWP agent library.
     * 
     * @return option "jpda.settings.debuggeeAgentName" or "jdwp" by default
     */
    public String getDebuggeeAgentName() {
        return getProperty("jpda.settings.debuggeeAgentName", "jdwp");
    }

    /**
     * Returns string with extra options for agent.
     * 
     * @return option "jpda.settings.debuggeeAgentExtraOptions" or "" by default
     */
    public String getDebuggeeAgentExtraOptions() {
        return getProperty("jpda.settings.debuggeeAgentExtraOptions", "");
    }

    /**
     * Returns string with all options for agent including specified connection
     * address.
     *  
     * @param address - address to attach
     * @param isDebuggerListen - true if debugger is listening for connection
     * 
     * @return string with all agent options
     */
    public String getDebuggeeAgentOptions(String address, boolean isDebuggerListen) {
        String serv;
        if (isDebuggerListen) {
            serv = "n";
        } else {
            serv = "y";
        }

        // add ',' to agent extra options if required 
        String agentExtraOptions = getDebuggeeAgentExtraOptions();
        if (agentExtraOptions.length() > 0 && agentExtraOptions.charAt(0) != ',') {
            agentExtraOptions = "," + agentExtraOptions;
        }

        return getProperty("jpda.settings.debuggeeAgentOptions",
                "transport=dt_socket,address=" + address + ",server=" + serv
                + ",suspend=" + getDebuggeeSuspend() + agentExtraOptions);
    }

    /**
     * Returns string with all options for agent including specified connection
     * address (only for debugger in listening mode). It just calls
     * <ul>
     * <li><code>getDebuggeeAgentOptions(address, true)</code></li>
     * </ul>
     *  
     * @deprecated This method is used as workaround for old tests and will be removed soon. 
     *  
     * @param address - address to attach
     * 
     * @return string with all agent options
     */
    public String getDebuggeeAgentOptions(String address) {
        return getDebuggeeAgentOptions(address, true);
    }
    
    /**
     * Returns VM classpath value to run debuggee with.
     * 
     * @return system property "java.class.path" by default.
     */
    public String getDebuggeeClassPath() {
        return getProperty("java.class.path", null);
    }

    /**
     * Returns full name of the class to start debuggee with.
     * 
     * @return option "jpda.settings.debuggeeClassName" or
     *         "org.apache.harmony.jpda.tests.jdwp.share.debuggee.HelloWorld" by default
     */
    public String getDebuggeeClassName() {
        return getProperty("jpda.settings.debuggeeClassName", DEFAULT_DEBUGGEE_CLASS_NAME);
    }

    /**
     * Sets full name of the class to start debuggee with.
     * 
     * @param className
     *            full class name
     */
    public void setDebuggeeClassName(String className) {
        setProperty("jpda.settings.debuggeeClassName", className);
    }

    /**
     * Returns string with extra options to start debuggee with.
     * 
     * @return option "jpda.settings.debuggeeVMExtraOptions" or "" by default
     */
    public String getDebuggeeVMExtraOptions() {
        String extOpts = getProperty("jpda.settings.debuggeeVMExtraOptions", "");
        extOpts = extOpts + " -Djpda.settings.verbose=" + isVerbose();
        return extOpts;
    }

    /**
     * Returns debuggee suspend mode ("y"|"n").
     * 
     * @return option "jpda.settings.debuggeeSuspend" or "y" by default
     */
    public String getDebuggeeSuspend() {
        return getProperty("jpda.settings.debuggeeSuspend", "y");
    }

    /**
     * Returns debuggee suspend mode ("y"|"n").
     * 
     * @param mode
     *            suspend mode
     */
    public void setDebuggeeSuspend(String mode) {
        setProperty("jpda.settings.debuggeeSuspend", mode);
    }

    /**
     * Checks if debuggee is launched in suspend mode.
     * 
     * @return true if debuggee is launched in suspend mode
     */
    public boolean isDebuggeeSuspend() {
        return getDebuggeeSuspend().equals("y");
    }

    /**
     * Returns string representation of TCP/IP port for synchronization channel.
     * 
     * @return string with port number or null
     */
    public String getSyncPortString() {
        return getProperty("jpda.settings.syncPort", null);
    }

    /**
     * Returns type of connection with debuggee.
     * 
     * @return system property "jpda.settings.connectorKind" or "listen" by default.
     */
    public String getConnectorKind() {
        return getProperty("jpda.settings.connectorKind", "listen");
    }

    /**
     * Checks if attach connection with debuggee.
     * 
     * @return true, if attach connection, false otherwise.
     */
    public boolean isAttachConnectorKind() {
        return ((getConnectorKind()).equals("attach"));

    }

    /**
     * Checks if listen connection with debuggee.
     * 
     * @return true, if listen connection, false otherwise.
     */
    public boolean isListenConnectorKind() {
        return (getConnectorKind().equals("listen"));
    }

    /**
     * Sets connectorKind to attach to debuggee.
     */
    public void setAttachConnectorKind() {
        setConnectorKind("attach");
    }

    /**
     * Sets connectorKind to listen connection from debuggee.
     */
    public void setListenConnectorKind() {
        setConnectorKind("listen");
    }
    
    /**
     * Sets kind of connector (attach or listen).
     */
    public void setConnectorKind(String kind) {
        setProperty("jpda.settings.connectorKind", kind);
    }

    /**
     * Returns kind of launching debuggee VM, which can be "auto" or "manual".
     * 
     * @return option "jpda.settings.debuggeeLaunchKind" or "auto" by default.
     */
    public String getDebuggeeLaunchKind() {
        return getProperty("jpda.settings.debuggeeLaunchKind", "auto");
    }

    /**
     * Returns TCP/IP port for synchronization channel.
     * 
     * @return string with port number or null
     */
    public int getSyncPortNumber() {
        String buf = getSyncPortString();
        if (buf == null) {
            return DEFAULT_SYNC_PORT;
        }

        try {
            return Integer.parseInt(buf);
        } catch (NumberFormatException e) {
            throw new TestErrorException(e);
        }
    }

    /**
     * Returns timeout for JPDA tests in milliseconds.
     * 
     * @return option "jpda.settings.timeout" or DEFAULT_TIMEOUT by default.
     */
    public long getTimeout() {
        if (timeout < 0) {
            timeout = DEFAULT_TIMEOUT;
            String buf = getProperty("jpda.settings.timeout", null);
            if (buf != null) {
                try {
                    timeout = Long.parseLong(buf);
                } catch (NumberFormatException e) {
                    throw new TestErrorException(e);
                }
            }
        }
        return timeout;
    }

    /**
     * Sets timeout for JPDA tests in milliseconds.
     * 
     * @param timeout
     *            timeout to be set
     */
    public void setTimeout(long timeout) {
        if (timeout < 0) {
            throw new TestErrorException("Cannot set negative timeout value: "
                    + timeout);
        }
        this.timeout = timeout;
    }

    /**
     * Returns waiting time for events in milliseconds.
     * 
     * @return waiting time
     */
    public long getWaitingTime() {
        if (waitingTime < 0) {
            waitingTime = DEFAULT_WAITING_TIME;
            String buf = getProperty("jpda.settings.waitingTime", null);
            if (buf != null) {
                try {
                    waitingTime = Long.parseLong(buf);
                } catch (NumberFormatException e) {
                    throw new TestErrorException(e);
                }
            }
        }
        return waitingTime;
    }

    /**
     * Sets waiting time for events in milliseconds.
     * 
     * @param waitingTime
     *            waiting time to be set
     */
    public void setWaitingTime(long waitingTime) {
        this.waitingTime = waitingTime;
    }
    
    /**
     * Returns whether print to log is enabled.
     * 
     * @return false (default) if log is disabled or true otherwise.
     */
    public boolean isVerbose() {
        return isTrue(getProperty("jpda.settings.verbose", "true"));
    }

    /**
     * Converts text to boolean.
     * 
     * @param str string representing boolean value
     * @return boolean
     */
    static public boolean isTrue(String str) {
        return str != null && (
            str.equalsIgnoreCase("true") ||
            str.equalsIgnoreCase("yes") ||
            str.equalsIgnoreCase("on") ||
            str.equals("1"));
    }

    /**
     * Returns value of given property if it was set internally or specified in system properties.
     *
     * @param name
     *           property name
     * @param defaultValue
     *           default value for given property
     * @return string value of given property or default value if no such property found 
     */
    protected String getProperty(String name, String defaultValue) {
        String value = (String)internalProperties.get(name);
        if (value != null) {
            return value;
        }
        return System.getProperty(name, defaultValue);
    }

    /**
     * Sets internal value of given property to override corresponding system property.
     *
     * @param name
     *           proparty name
     * @param value
     *           value for given property
     */
    protected void setProperty(String name, String value) {
        internalProperties.put(name, value);
    }

}
