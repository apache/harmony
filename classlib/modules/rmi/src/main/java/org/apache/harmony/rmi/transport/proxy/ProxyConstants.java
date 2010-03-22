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
 * @author  Vasily Zakharov
 */
package org.apache.harmony.rmi.transport.proxy;

import org.apache.harmony.rmi.common.RMILog;
import org.apache.harmony.rmi.common.RMIProperties;


/**
 * Contains constants usable by RMI HTTP proxy access classes.
 *
 * @author  Vasily Zakharov
 */
interface ProxyConstants extends RMIProperties {

    /**
     * HTTP end-of-line string.
     */
    String EOLN = "\r\n"; //$NON-NLS-1$

    /**
     * HTTP POST request signature.
     */
    String HTTP_REQUEST_SIGNATURE = "POST "; //$NON-NLS-1$

    /**
     * Length of {@link #HTTP_REQUEST_SIGNATURE} string.
     */
    int HTTP_REQUEST_SIGNATURE_LENGTH = HTTP_REQUEST_SIGNATURE.length();

    /**
     * HTTP response header signature.
     */
    String HTTP_RESPONSE_HEADER_SIGNATURE = "HTTP/1.0 200 "; //$NON-NLS-1$

    /**
     * HTTP response header.
     */
    String HTTP_RESPONSE_HEADER = HTTP_RESPONSE_HEADER_SIGNATURE + "OK"; //$NON-NLS-1$

    /**
     * Content-Length header string signature.
     */
    String CONTENT_LENGTH_SIGNATURE = "Content-Length:"; //$NON-NLS-1$

    /**
     * Length of {@link #CONTENT_LENGTH_SIGNATURE} string.
     */
    int CONTENT_LENGTH_SIGNATURE_LENGTH = CONTENT_LENGTH_SIGNATURE.length();

    /**
     * Log for logging proxy connections activities.
     */
    RMILog proxyTransportLog = RMILog.getProxyTransportLog();
}
