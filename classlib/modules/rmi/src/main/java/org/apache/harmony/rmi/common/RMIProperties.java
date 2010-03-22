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
 * @author  Mikhail A. Markov, Vasily Zakharov
 */
package org.apache.harmony.rmi.common;


/**
 * Interface containing names of all supported RMI properties.
 *
 * @author  Mikhail A. Markov, Vasily Zakharov
 */
public interface RMIProperties {

    /*
     * -------------------------------------------------------------------------
     * java.rmi.* supported properties
     * -------------------------------------------------------------------------
     */
    String ACTIVATIONPORT_PROP = "java.rmi.activation.port"; //$NON-NLS-1$
    String DGCLEASEVALUE_PROP = "java.rmi.dgc.leaseValue"; //$NON-NLS-1$
    String CODEBASE_PROP = "java.rmi.server.codebase"; //$NON-NLS-1$
    String HOSTNAME_PROP = "java.rmi.server.hostname"; //$NON-NLS-1$
    String LOGSERVER_PROP = "java.rmi.server.logCalls"; //$NON-NLS-1$
    String RANDOMIDS_PROP = "java.rmi.server.randomIDs"; //$NON-NLS-1$
    String USECODEBASEONLY_PROP = "java.rmi.server.usecodebaseOnly"; //$NON-NLS-1$
    String USELOCALHOSTNAME_PROP = "java.rmi.server.useLocalHostname"; //$NON-NLS-1$
    String DISABLEHTTP_PROP = "java.rmi.server.disableHttp"; //$NON-NLS-1$
    String IGNORESTUBCLASSES_PROP = "java.rmi.server.ignoreStubClasses"; //$NON-NLS-1$

    /*
     * -------------------------------------------------------------------------
     * harmony.rmi.* supported properties
     * -------------------------------------------------------------------------
     */

    // Server properties.
    String DGCACKTIMEOUT_PROP = "harmony.rmi.dgc.ackTimeout"; //$NON-NLS-1$
    String DGCCHECKINTERVAL_PROP = "harmony.rmi.dgc.checkInterval"; //$NON-NLS-1$
    String DGCLOGLEVEL_PROP = "harmony.rmi.dgc.logLevel"; //$NON-NLS-1$
    String LOADERLOGLEVEL_PROP = "harmony.rmi.loader.logLevel"; //$NON-NLS-1$
    String EXCEPTIONTRACE_PROP = "harmony.rmi.server.exceptionTrace"; //$NON-NLS-1$
    String SUPPRESSSTACKTRACES_PROP = "harmony.rmi.server.suppressStackTraces"; //$NON-NLS-1$
    String TRANSPORTLOGLEVEL_PROP = "harmony.rmi.transport.logLevel"; //$NON-NLS-1$
    String LOCALHOSTNAMETIMEOUT_PROP = "harmony.rmi.transport.tcp.localHostNameTimeOut"; //$NON-NLS-1$
    String TRANSPORTTCPLOGLEVEL_PROP = "harmony.rmi.transport.tcp.logLevel"; //$NON-NLS-1$
    String READTIMEOUT_PROP = "harmony.rmi.transport.tcp.readTimeout"; //$NON-NLS-1$

    // Client properties.
    String LOGCLIENT_PROP = "harmony.rmi.client.logCalls"; //$NON-NLS-1$
    String DGCCLEANINTERVAL_PROP = "harmony.rmi.dgc.cleanInterval"; //$NON-NLS-1$
    String SERVERLOGLEVEL_PROP = "harmony.rmi.server.logLevel"; //$NON-NLS-1$
    String CLIENTLOGLEVEL_PROP = "harmony.rmi.client.logLevel"; //$NON-NLS-1$
    String CONNECTIONTIMEOUT_PROP = "harmony.rmi.transport.connectionTimeout"; //$NON-NLS-1$
    String CONNECTTIMEOUT_PROP = "harmony.rmi.transport.proxy.connectTimeout"; //$NON-NLS-1$
    String EAGERHTTPFALLBACK_PROP = "harmony.rmi.transport.proxy.eagerHttpFallback"; //$NON-NLS-1$
    String TRANSPORTPROXYLOGLEVEL_PROP = "harmony.rmi.transport.proxy.logLevel"; //$NON-NLS-1$
    String HANDSHAKETIMEOUT_PROP = "harmony.rmi.transport.tcp.handshakeTimeout"; //$NON-NLS-1$

    // Activation properties.
    String ACTIVATIONLOGLEVEL_PROP = "harmony.rmi.activation.logLevel"; //$NON-NLS-1$
    String ACTIVATION_EXECTIMEOUT_PROP = "harmony.rmi.activation.execTimeout"; //$NON-NLS-1$
    String MAXSTARTGROUP_PROP = "harmony.rmi.activation.groupThrottle"; //$NON-NLS-1$
    String ACTIVATION_SNAPSHOTINTERVAL_PROP = "harmony.rmi.activation.snapshotInterval"; //$NON-NLS-1$
    String ACTIVATION_LOG_DEBUG_PROP = "harmony.rmi.log.debug"; //$NON-NLS-1$
    String ACTIVATION_DEBUGEXEC_PROP = "harmony.rmi.server.activation.debugExec"; //$NON-NLS-1$

    /*
     * -------------------------------------------------------------------------
     * Additional proxy properties
     * -------------------------------------------------------------------------
     */

    /**
     * Name of the system property containing HTTP proxy host name.
     */
    String PROXY_HOST_PROP = "http.proxyHost"; //$NON-NLS-1$

    /**
     * Name of the system property containing HTTP proxy port number.
     */
    String PROXY_PORT_PROP = "http.proxyPort"; //$NON-NLS-1$

    /**
     * Name of the property allowing to disable direct socket connections.
     */
    String DISABLE_DIRECT_SOCKET_PROP =
            "org.apache.harmony.rmi.transport.disableDirectSocket"; //$NON-NLS-1$

    /**
     * Name of the property allowing to enable direct HTTP connections.
     */
    String ENABLE_DIRECT_HTTP_PROP =
            "org.apache.harmony.rmi.transport.proxy.enableDirectHTTP"; //$NON-NLS-1$

    /**
     * Name of the property allowing to disable plain HTTP connections
     * (and force CGI instead).
     */
    String DISABLE_PLAIN_HTTP_PROP =
            "org.apache.harmony.rmi.transport.proxy.disablePlainHTTP"; //$NON-NLS-1$

    /*
     * -------------------------------------------------------------------------
     * Additional Activation properties
     * -------------------------------------------------------------------------
     */

    /**
     * @see org.apache.harmony.rmi.common.RMIConstants#DEFAULT_ACTIVATION_MONITOR_CLASS_NAME
     */
    String ACTIVATION_MONITOR_CLASS_NAME_PROP =
            "org.apache.harmony.rmi.activation.monitor"; //$NON-NLS-1$

    /*
     * -------------------------------------------------------------------------
     * RMI Compiler properties
     * -------------------------------------------------------------------------
     */

    /**
     * Property specifying the compiler class to use.
     */
    String JAVA_COMPILER_CLASS_PROPERTY =
            "org.apache.harmony.rmi.compiler.class"; //$NON-NLS-1$

    /**
     * Property specifying the compiler class method to use.
     */
    String JAVA_COMPILER_METHOD_PROPERTY =
            "org.apache.harmony.rmi.compiler.method"; //$NON-NLS-1$

    /**
     * Property specifying the compiler executable to use.
     */
    String JAVA_COMPILER_EXECUTABLE_PROPERTY =
            "org.apache.harmony.rmi.compiler.executable"; //$NON-NLS-1$
}
