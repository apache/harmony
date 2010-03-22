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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.transaction.xa;

import java.io.Serializable;

/**
 * An XAException is an exception thrown by a Resource Manager to inform the
 * Transaction Manager of an error which has occurred in relation to a
 * transaction branch. In addition to the usual exception message, an
 * XAException carries an errorCode value which provides information about the
 * error, as defined by the series of integer values defined by the XAException
 * class.
 */
public class XAException extends Exception implements Serializable {

    private static final long serialVersionUID = -8249683284832867751L;

    /**
     * Code which contains the inclusive lower bound of the rollback error codes
     */
    public static final int XA_RBBASE = 100;

    /**
     * Code which means that the rollback occurred for an unspecified reason
     */
    public static final int XA_RBROLLBACK = 100;

    /**
     * Code which means that rollback was caused by a communication failure
     */
    public static final int XA_RBCOMMFAIL = 101;

    /**
     * Code which means that a failure occurred because a deadlock was detected
     */
    public static final int XA_RBDEADLOCK = 102;

    /**
     * Code which means that a condition was detected than implies a violation
     * of the integrity of the resource
     */
    public static final int XA_RBINTEGRITY = 103;

    /**
     * Code which means that the Resource Manager rolled back the transaction
     * branch for a reason not separately listed
     */
    public static final int XA_RBOTHER = 104;

    /**
     * Code which means that a protocol error occurred in the Resource Manager
     */
    public static final int XA_RBPROTO = 105;

    /**
     * Code which means that a transaction branch took too long
     */
    public static final int XA_RBTIMEOUT = 106;

    /**
     * Code which means that the caller may retry the transaction branch
     */
    public static final int XA_RBTRANSIENT = 107;

    /**
     * Code which contains the inclusive upper bound of the rollback error codes
     */
    public static final int XA_RBEND = 107;

    /**
     * Code which means that resumption must occur where the suspension occurred
     */
    public static final int XA_NOMIGRATE = 9;

    /**
     * Code which means that the transaction branch may have been heuristically
     * completed
     */
    public static final int XA_HEURHAZ = 8;

    /**
     * Code which means that the transaction branch has been heuristically
     * committed
     */
    public static final int XA_HEURCOM = 7;

    /**
     * Code which means that the transaction branch has been heuristically
     * rolled back
     */
    public static final int XA_HEURRB = 6;

    /**
     * Code which means that the transaction branch has been heuristically
     * committed and rolled back
     */
    public static final int XA_HEURMIX = 5;

    /**
     * Code which means that the method returned with no effect and can be
     * reissued
     */
    public static final int XA_RETRY = 4;

    /**
     * Code which means that the transaction branch was read only and has been
     * committed
     */
    public static final int XA_RDONLY = 3;

    /**
     * Code which means that there is already an asynchronous operation
     * outstanding
     */
    public static final int XAER_ASYNC = -2;

    /**
     * Code which means that a Resource Manager error has occurred for the
     * transaction branch
     */
    public static final int XAER_RMERR = -3;

    /**
     * Code which means that the XID is not valid
     */
    public static final int XAER_NOTA = -4;

    /**
     * Code which means that invalid arguments were supplied
     */
    public static final int XAER_INVAL = -5;

    /**
     * Code which means that the method was invoked in an improper context
     */
    public static final int XAER_PROTO = -6;

    /**
     * Code which means that the Resource Manager is unavailable
     */
    public static final int XAER_RMFAIL = -7;

    /**
     * Code which means that the XID already exists
     */
    public static final int XAER_DUPID = -8;

    /**
     * Work is being done by the Resource Manager outside the boundaries of a
     * global transaction.
     */
    public static final int XAER_OUTSIDE = -9;

    /**
     * The errorCode which details the error that has occurred
     */
    public int errorCode;

    /**
     * Creates an XAException with no message or error code
     */
    public XAException() {
        super();
    }

    /**
     * Creates an XAException with a supplied message and no error code
     * 
     * @param theMessage
     *            a String containing the exception message
     */
    public XAException(String theMessage) {
        super(theMessage);
    }

    /**
     * Creates an XAException with a specified error code but no message
     * 
     * @param errorCode
     *            an integer containing one of the XAException errorCode values
     */
    public XAException(int errorCode) {
        super();
        this.errorCode = errorCode;
    }
}
