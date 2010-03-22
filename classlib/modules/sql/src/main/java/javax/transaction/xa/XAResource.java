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

/**
 * An interface which provides a mapping of the X/Open CAE Specification (for
 * Distributed Transaction processing) into Java. The XAResource interface is
 * used as the interface between a Transaction Manager and a Resource Manager
 * for handling distributed transaction processing.
 * <p>
 * Typically, a JDBC driver or a JMS driver will implement the XAResource
 * interface to support Global Transactions on a database or on a message
 * connection.
 */
public interface XAResource {

    /**
     * Flag to end a recovery scan
     */
    public static final int TMENDRSCAN = 0x800000;

    /**
     * Flag to indicate that the caller is dissociation from a transaction
     * branch and that it should be marked rollback only
     */
    public static final int TMFAIL = 0x20000000;

    /**
     * Flag to indicate that the caller is joining sn existing transaction
     * branch.
     */
    public static final int TMJOIN = 0x200000;

    /**
     * Flag that indicates that no flags options are selected. (ie a null flag)
     */
    public static final int TMNOFLAGS = 0;

    /**
     * Flag that indicates the caller is using one-phase commit optimization
     */
    public static final int TMONEPHASE = 0x40000000;

    /**
     * Flag that indicates the caller is resuming association with a suspended
     * transaction branch
     */
    public static final int TMRESUME = 0x8000000;

    /**
     * Flag that indicates the start of a recovery scan
     */
    public static final int TMSTARTRSCAN = 0x1000000;

    /**
     * Flag that indicates the caller is dissociating from a transaction branch
     */
    public static final int TMSUCCESS = 0x4000000;

    /**
     * Flag that indicates that the caller is suspending (not terminating) its
     * association with a transaction branch.
     */
    public static final int TMSUSPEND = 0x2000000;

    /**
     * Flag that indicates that transaction work has been read only and has been
     * committed normally
     */
    public static final int XA_RDONLY = 3;

    /**
     * Flag that indicates that transaction work has been Prepared normally
     */
    public static final int XA_OK = 0;

    /**
     * Commits a global transaction.
     * 
     * @param xid
     *            the XID which identifies the global transaction
     * @param onePhase
     *            true if the resource manager should use a one-phase commit
     *            protocol to commit the transaction
     * @throws XAException
     *             if an error occurred.
     *             <p>
     *             Possible errors are identified by the errorcode in the
     *             XAException and include: XA_HEURHAZ, XA_HEURCOM, XA_HEURRB,
     *             XA_HEURMIX, XAER_RMERR, XAER_RMFAIL, XAER_NOTA, XAER_INVAL,
     *             or XAER_PROTO. In addition, one of the XA_RB* errors can
     *             occur if the transaction was not committed and
     *             <code>onePhase</code> was set to true. On completion of
     *             this method, the Resource Manager has rolled back the
     *             transaction and released resources held by the transaction.
     */
    public void commit(Xid xid, boolean onePhase) throws XAException;

    /**
     * Ends the work done for a transaction branch. The Resource Manager
     * disconnects the XA resource from the transaction branch and allows the
     * transaction to complete.
     * 
     * @param xid
     *            the XID which identifies the global transaction. Should have
     *            previously been used as the parameter to a <code>start</code>
     *            method.
     * @param flags
     *            a flags integer - one of: XAResource.TMSUCCESS,
     *            XAResource.TMFAIL, or XAResource.TMSUSPEND.
     *            <p>
     *            TMSUCCESS means that this section of work completed
     *            successfully.
     *            <p>
     *            TMFAIL means that this section of work failed. The Resource
     *            Manager can mark the transaction for rollback only.
     *            <p>
     *            TMSUSPEND means that this section of work is suspended and not
     *            yet complete. The associated transaction context is also
     *            suspended and must be restarted with a call to
     *            {@link #start(Xid, int)} with the <code>TMRESUME</code>
     *            flag.
     * @throws XAException
     *             if an error occurs. Possible error identified in the
     *             errorcode include: XAER_RMERR, XAER_RMFAIL, XAER_NOTA,
     *             XAER_INVAL, XAER_PROTO, or XA_RB*.
     */
    public void end(Xid xid, int flags) throws XAException;

    /**
     * Informs the Resource Manager that it can forget about a specified
     * transaction branch.
     * 
     * @param xid
     *            the XID which identifies the global transaction.
     * @throws XAException
     *             if an error occurs. Possible error identified in the
     *             errorcode include: XAER_RMERR, XAER_RMFAIL, XAER_NOTA,
     *             XAER_INVAL, or XAER_PROTO.
     */
    public void forget(Xid xid) throws XAException;

    /**
     * Gets the transaction timeout value for this XAResource. The default
     * timeout value is the default timeout value set for the Resource Manager.
     * 
     * @return the transaction timeout value for this XAResource in seconds.
     * @throws XAException
     *             if an error occurs. Possible error identified in the
     *             errorcode include: XAER_RMERR and XAER_RMFAIL.
     */
    public int getTransactionTimeout() throws XAException;

    /**
     * Returns true if the ResourceManager for this XAResource is the same as
     * the Resource Manager for a supplied XAResource.
     * 
     * @param theXAResource
     *            an XAResource object
     * @return true if the Resource Manager for this XAResource is the same as
     *         the Resource Manager for <code>theXAResource</code>.
     * @throws XAException
     *             if an error occurs. Possible error identified in the
     *             errorcode include: XAER_RMERR and XAER_RMFAIL.
     */
    public boolean isSameRM(XAResource theXAResource) throws XAException;

    /**
     * Requests the Resource manager to prepare to commit a specified
     * transaction.
     * 
     * @param xid
     *            the XID which identifies the global transaction.
     * @return an integer: XA_RDONLY or XA_OK. XA_OK implies that the
     *         transaction work has been prepared normally, XA_RDONLY implies
     *         that the transaction branch is read only and has been committed.
     *         If there is a failure which requires a rollback, an XAException
     *         is raised.
     * @throws XAException
     *             if an error occurs. Possible error identified in the
     *             errorcode include: XA_RB*, XAER_RMERR, XAER_RMFAIL,
     *             XAER_NOTA, XAER_INVAL, or XAER_PROTO.
     */
    public int prepare(Xid xid) throws XAException;

    /**
     * Get a list of prepared transaction branches.
     * <p>
     * Typically used by a transaction manager during recovery to find
     * transaction branches that are in prepared or heuristically completed
     * states.
     * 
     * @param flag
     *            an integer. Must be one of: XAResource.TMSTARTRSCAN,
     *            XAResource.TMENDRSCAN, XAResource.TMNOFLAGS.
     * @return an array of zero or more XIDs identifying the transaction
     *         branches in the prepared or heuristically completed states.
     * @throws XAException
     *             if an error occurs. Possible error identified in the
     *             errorcode include: XAER_RMERR, XAER_RMFAIL, XAER_INVAL, and
     *             XAER_PROTO.
     */
    public Xid[] recover(int flag) throws XAException;

    /**
     * Requests the Resource Manager to rollback a specified transaction branch.
     * 
     * @param xid
     *            the XID which identifies the transaction branch.
     * @throws XAException
     *             if an error occurs.
     */
    public void rollback(Xid xid) throws XAException;

    /**
     * Sets the transaction timeout value for this XAResource. If the value is
     * set to 0, the default timeout value for the Resource Manager is used.
     * 
     * @param seconds
     *            the new Timeout value in seconds
     * @return true if the transaction timeout value has been updated, false
     *         otherwise.
     * @throws XAException
     *             if an error occurs. Possible error identified in the
     *             errorcode include: XAER_RMERR, XAER_RMFAIL, or XAER_INVAL.
     */
    public boolean setTransactionTimeout(int seconds) throws XAException;

    /**
     * Starts work for a specified transaction branch.
     * 
     * @param xid
     *            the XID which identifies the transaction branch.
     * @param flags
     *            an integer. Must be one of XAResource.TMNOFLAGS,
     *            XAResource.TMJOIN, or XAResource.TMRESUME.
     *            <p>
     *            TMJOIN implies that the start applies to joining a transaction
     *            previously passed to the Resource Manager.
     *            <p>
     *            TMRESUME implies that the start applies to a suspended
     *            transaction that should be restarted.
     *            <p>
     *            If TMNOFLAGS is specified, then if the transaction has been
     *            previously seen by the Resource Manager, an XAException is
     *            raised with the code XAER_DUPID.
     * @throws XAException
     *             if an error occurs. Possible error identified in the
     *             errorcode include: XA_RB*, XAER_RMERR, XAER_RMFAIL,
     *             XAER_DUPID, XAER_OUTSIDE, XAER_NOTA, XAER_INVAL, or
     *             XAER_PROTO.
     */
    public void start(Xid xid, int flags) throws XAException;
}
