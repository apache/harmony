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
 * @author Alexei Y. Zakharov
 */

package org.apache.harmony.jndi.provider.dns;

import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;

import org.apache.harmony.jndi.internal.nls.Messages;
import org.apache.harmony.jndi.provider.dns.SList.Server;

/**
 * This class implements the functionality of a simple DNS resolver.<br>
 * Following DNS Resource Records are supported:
 * <ul>
 * <li>A</li>
 * <li>NS</li>
 * <li>CNAME</li>
 * <li>SOA</li>
 * <li>PTR</li>
 * <li>MX</li>
 * <li>TXT</li>
 * <li>HINFO</li>
 * <li>AAAA (TODO)</li>
 * <li>NAPTR (TODO)</li>
 * <li>SRV</li>
 * </ul>
 * Following DNS classes are supported:
 * <ul>
 * <li>IN</li>
 * <li>HS (TODO)</li>
 * </ul>
 * <br>
 * TODO: do we need broadcasting and IP multicasting to obtain initial name
 * server address? TODO: network-preference feature for choosing most welcome
 * address for multihomed hosts (RFC 1123 point 6.1.3.4) TODO: add general IPv6
 * support and support for AAAA resource record
 */
public class Resolver implements Runnable {

    private static final int MSG_MAX_BYTES = 512;

    /**
     * Entry of the resolver thread list.
     */
    private static class ThreadListEntry {
        Thread thread;

        String serverNameToResolve;

        int dnsClass;
    }

    private static final Random rndGen = new Random();

    // resolver configuration
    private int initialTimeout;

    private int timeoutRetries;

    private boolean authoritativeAnswerDesired;

    private boolean recursionDesired;

    // maximum number of active threads
    private int threadNumberLimit;

    // vector with currently running Resolver threads
    private final ArrayList<ThreadListEntry> resolverThreads = new ArrayList<ThreadListEntry>();

    // the list of host names that should be resolved
    private final ArrayList<ThreadListEntry> hostnamesToResolve = new ArrayList<ThreadListEntry>();

    // semaphore that controls access to both lists above
    private class ThreadListSemaphore {
    }

    private final Object threadListSemaphore = new ThreadListSemaphore();

    /**
     * Constructs a <code>Resolver</code> object with default initial timeout
     * (1 second), default timeout retries (4 times), default recursion desired
     * switch (true) and default authoritative answer desired
     * 
     * @see #Resolver(int, int)
     */
    public Resolver() {
        this(ProviderConstants.DEFAULT_INITIAL_TIMEOUT,
                ProviderConstants.DEFAULT_TIMEOUT_RETRIES,
                ProviderConstants.DEFAULT_MAX_THREADS,
                ProviderConstants.DEFAULT_AUTHORITATIVE,
                ProviderConstants.DEFAULT_RECURSION);
    }

    /**
     * Constructs a <code>Resolver</code> object with given initial timeout
     * and timeout retries. Initially the resolver will try to access DNS
     * servers with timeout set to initial timeout. If none of servers answer it
     * will double the timeout and perform the second round. The process will
     * continue for <code>timeoutRetries</code> rounds. If there is no answer
     * still the resolver will give up.
     * 
     * @param initialTimeout
     *            the initial timeout that is used during the first round (in
     *            milliseconds)
     * @param timeoutRetries
     *            number of rounds the Resolver should perform before giving up
     * @param authoritativeAnswerDesired
     *            do we want to receive only authoritative answers
     * @param recursionDesired
     *            do we want our outgoing packages to have RD but set
     */
    public Resolver(int initialTimeout, int timeoutRetries, int maxThreads,
            boolean authoritativeAnswerDesired, boolean recursionDesired) {
        this.initialTimeout = initialTimeout;
        this.timeoutRetries = timeoutRetries;
        this.threadNumberLimit = maxThreads;
        this.authoritativeAnswerDesired = authoritativeAnswerDesired;
        this.recursionDesired = recursionDesired;
    }

    /**
     * @return Returns the threadNumberLimit.
     */
    public int getThreadNumberLimit() {
        return threadNumberLimit;
    }

    /**
     * @param threadNumberLimit
     *            The threadNumberLimit to set.
     */
    public void setThreadNumberLimit(int threadNumberLimit) {
        this.threadNumberLimit = threadNumberLimit;
    }

    /**
     * @return Returns the authoritativeAnswerDesired.
     */
    public boolean isAuthoritativeAnswerDesired() {
        return authoritativeAnswerDesired;
    }

    /**
     * @param authoritativeAnswerDesired
     *            The authoritativeAnswerDesired to set.
     */
    public void setAuthoritativeAnswerDesired(boolean authoritativeAnswerDesired) {
        this.authoritativeAnswerDesired = authoritativeAnswerDesired;
    }

    /**
     * @return Returns the initialTimeout.
     */
    public int getInitialTimeout() {
        return initialTimeout;
    }

    /**
     * @param initialTimeout
     *            The initialTimeout to set.
     */
    public void setInitialTimeout(int initialTimeout) {
        this.initialTimeout = initialTimeout;
    }

    /**
     * @return Returns the recursionDesired.
     */
    public boolean isRecursionDesired() {
        return recursionDesired;
    }

    /**
     * @param recursionDesired
     *            The recursionDesired to set.
     */
    public void setRecursionDesired(boolean recursionDesired) {
        this.recursionDesired = recursionDesired;
    }

    /**
     * @return Returns the timeoutRetries.
     */
    public int getTimeoutRetries() {
        return timeoutRetries;
    }

    /**
     * @param timeoutRetries
     *            The timeoutRetries to set.
     */
    public void setTimeoutRetries(int timeoutRetries) {
        this.timeoutRetries = timeoutRetries;
    }

    /**
     * Checks available name servers if they have any resource records related
     * to given name & type & class combination. Standard DNS lookup algorithm
     * is used.
     * 
     * @param name
     *            well-formed domain name
     * @param types
     *            an array of types; only records that have such types will be
     *            returned
     * @param classes
     * @return enumeration with found resource records
     * @throws SecurityException
     *             if the resolver is not allowed to use a network subsystem
     * @throws NameNotFoundException
     *             if authoritative server for desired zone was contacted but
     *             given name has not been found in that zone
     * @throws ServiceUnavailableException
     *             if no authoritative server for desired name was found or all
     *             servers are dead or malfunction
     * @throws DomainProtocolException
     *             if some DNS specific error has occurred
     */
    public Enumeration<ResourceRecord> lookup(String name, int[] types,
            int[] classes) throws SecurityException, NameNotFoundException,
            ServiceUnavailableException, DomainProtocolException {

        // Algorithm:
        // 1. Set workZone to the parent of qName; clear queriedServers.
        // 2. Try to get a complete answer for the workZone from the servers
        // currently available in SLIST exclude servers from queriedServers.
        // 3. update queriedServers with "visited servers" info.
        // 4. If the complete answer was received - return it to the user;exit.
        // 5. If the delegation was received:
        // a) If we already have this server & zone pair in SLIST - skip it.
        // b) If we don't have - put it into SLIST
        // c) If we haven't received any new delegations - goto step (7)
        // d) If some new delegation has been received:
        // 1) from delegations: found the zone with the best matching count
        // with qName
        // 2) if this matching count is bigger than matching count between
        // workZone and qName:
        // - set workZone to zone with biggest matching count determined
        // at step (5.d.1)
        // - goto step (2)
        // 3) if it doesn't then goto step 2 with the same workZone
        // 6. If ALIAS was received ...
        // 7. If no answer has been received:
        // a) Check if the workZone is the root zone.
        // b) If so - give up; return empty result to the user.
        // c) If it isn't, set workZone to parent of workZone. Goto step (2).

        // SList slist = SList.getInstance();
        ResolverCache cache = ResolverCache.getInstance();

        Vector<QuestionRecord> questions = new Vector<QuestionRecord>();
        Vector<ResourceRecord> answers = new Vector<ResourceRecord>();

        if (name == null) {
            // jndi.2E=The name is null
            throw new NullPointerException(Messages.getString("jndi.2E")); //$NON-NLS-1$
        }
        if (types == null) {
            // jndi.6B=types is null
            throw new NullPointerException(Messages.getString("jndi.6B")); //$NON-NLS-1$
        }
        if (classes == null) {
            // jndi.6C=classes is null
            throw new NullPointerException(Messages.getString("jndi.6C")); //$NON-NLS-1$
        }
        for (int element : classes) {
            for (int element0 : types) {
                QuestionRecord quest = new QuestionRecord(name, element0,
                        element);
                questions.addElement(quest);
            }
        }
        // iterate over question records
        for (int i = 0; i < questions.size(); i++) {
            QuestionRecord curQuestion = questions.elementAt(i);
            String qName = curQuestion.getQName();
            Message mesToSend = null;
            Message receivedMes = null;
            AnalysisReport report = null;
            String workZone;
            Hashtable<Server, Object> visitedServers = new Hashtable<Server, Object>();

            // if (LogConst.DEBUG) {
            // ProviderMgr.logger.fine("Current question: " +
            // curQuestion.toString());
            // }
            // look in cache
            if (curQuestion.getQType() != ProviderConstants.ANY_QTYPE
                    && curQuestion.getQClass() != ProviderConstants.ANY_QCLASS) {
                Enumeration<ResourceRecord> recEnum = cache.get(curQuestion);

                if (recEnum.hasMoreElements()) {
                    while (recEnum.hasMoreElements()) {
                        answers.addElement(recEnum.nextElement());
                    }
                    // we don't need to query any servers since the information
                    // we want has been found in the local cache
                    // if (LogConst.DEBUG) {
                    // ProviderMgr.logger.fine(
                    // "Information was gathered from cache");
                    // }
                    continue;
                }
            }

            // query remote DNS servers

            // determine work zone
            if (qName != null && !qName.equals(".")) { //$NON-NLS-1$
                workZone = qName;
                // support for SRV-style qNames
                while (workZone.startsWith("_")) { //$NON-NLS-1$
                    workZone = ProviderMgr.getParentName(workZone);
                }
            } else {
                workZone = "."; //$NON-NLS-1$
            }
            // if (LogConst.DEBUG) {
            // ProviderMgr.logger.fine("Lookup: new workZone is " +
            // "\"" + workZone + "\"");
            // }
            // construct request message
            try {
                mesToSend = createMessageForSending(qName, curQuestion
                        .getQType(), curQuestion.getQClass());
                // if (LogConst.DEBUG) {
                // ProviderMgr.logger.finest("Message to send:\n" +
                // mesToSend.toString());
                // }

            } catch (DomainProtocolException e) {
                throw e;
            }
            while (true) {
                boolean noIdea = false;

                try {
                    receivedMes = queryServers(mesToSend, workZone,
                            visitedServers, false);
                    if (receivedMes != null) {
                        report = analyzeAnswer(mesToSend, receivedMes);
                        if (!report.messageWasTruncated) {
                            // Put all extra records into the cache for
                            // future use
                            for (int k = 0; k < report.extraRecords.size(); k++) {
                                ResourceRecord rec = report.extraRecords
                                        .elementAt(k);

                                cache.put(rec);
                            }
                        } else {
                            // Truncated message MUST NOT be cached and later
                            // used in such a way that the fact that they are
                            // truncated is lost (RFC 1123 point 6.1.3.2).
                        }
                        // examine the report
                        if (report.completeAnswerWasReceived) {
                            // complete answer
                            // if (LogConst.DEBUG) {
                            // ProviderMgr.logger.fine(
                            // "Lookup: a complete answer was received");
                            // }
                            for (int k = 0; k < report.records.size(); k++) {
                                ResourceRecord rec = report.records
                                        .elementAt(k);
                                answers.addElement(rec);
                                // we are sure that the answer section has not
                                // been truncated so we can put the record
                                // into the cache
                                cache.put(rec);
                            }
                            // exit the loop
                            break;
                        } else if (report.nameError) {
                            // name error
                            // if (LogConst.DEBUG) {
                            // ProviderMgr.logger.fine("Lookup: name error");
                            // }
                            // jndi.6D=Name {0} was not found
                            throw new NameNotFoundException(Messages.getString(
                                    "jndi.6D", name)); //$NON-NLS-1$
                        } else if (report.aliasInfoWasReceived) {
                            // alias received
                            // QuestionRecord newQuestion = new
                            // QuestionRecord();

                            // if (LogConst.DEBUG) {
                            // ProviderMgr.logger.fine(
                            // "Lookup: an alias was received");
                            // }
                            qName = report.newName;
                            curQuestion.setQName(qName);
                            // look in cache
                            if (curQuestion.getQType() != ProviderConstants.ANY_QTYPE
                                    && curQuestion.getQClass() != ProviderConstants.ANY_QCLASS) {
                                Enumeration<ResourceRecord> recEnum = cache
                                        .get(curQuestion);

                                if (recEnum.hasMoreElements()) {
                                    while (recEnum.hasMoreElements()) {
                                        answers.addElement(recEnum
                                                .nextElement());
                                    }
                                    // We don't need to query any more servers
                                    // since the information we want has been
                                    // found in the local cache.
                                    // Let's switch to next question if any.
                                    break;
                                }
                            }
                            if (qName != null && !qName.equals(".")) //$NON-NLS-1$
                            {
                                workZone = qName;
                            } else {
                                workZone = "."; //$NON-NLS-1$
                            }
                            visitedServers = new Hashtable<Server, Object>();
                            for (int k = 0; k < report.records.size(); k++) {
                                answers.addElement(report.records.elementAt(k));
                            }
                            // construct a new request message
                            try {
                                mesToSend = createMessageForSending(qName,
                                        curQuestion.getQType(), curQuestion
                                                .getQClass());
                            } catch (DomainProtocolException e) {
                                throw e;
                            }
                            // if (LogConst.DEBUG) {
                            // ProviderMgr.logger.fine("Lookup: new name is " +
                            // "\"" + qName + "\"");
                            // ProviderMgr.logger.fine(
                            // "Lookup: new workZone is " +
                            // "\"" + workZone + "\"");
                            // }
                        } else if (report.delegationArrived) {
                            // new delegation, probably need to query once again
                            int k17 = -1;
                            int matchingCount = ProviderMgr.getMatchingCount(
                                    qName, workZone);

                            // if (LogConst.DEBUG) {
                            // ProviderMgr.logger.fine(
                            // "Lookup: delegation arrived");
                            // }
                            for (int k = 0; k < report.delegationZones.size(); k++) {
                                String curZone = report.delegationZones
                                        .elementAt(k);
                                int tmpMatchingCount = ProviderMgr
                                        .getMatchingCount(qName, curZone);

                                if (tmpMatchingCount > matchingCount) {
                                    k17 = k;
                                    matchingCount = tmpMatchingCount;
                                }

                            }
                            if (k17 != -1) {
                                // better delegation was received
                                workZone = report.delegationZones
                                        .elementAt(k17);
                                // if (LogConst.DEBUG) {
                                // ProviderMgr.logger.fine(
                                // "Lookup: better delegation was found");
                                // }
                            } else {
                                // no better delegation
                                // do nothing, just query the next server of
                                // the current workZone
                            }
                        } else {
                            noIdea = true;
                        }
                    } // end of if report != null block
                    else {
                        noIdea = true;
                    }
                    if (noIdea) {
                        // Resolver has no idea how to get info about
                        // desired host while querying master hosts of the
                        // current workZone.
                        // Let's make one step up to the root.
                        // if (LogConst.DEBUG) {
                        // ProviderMgr.logger.fine("Lookup: no idea");
                        // }
                        if (!workZone.equals(".")) { //$NON-NLS-1$
                            workZone = ProviderMgr.getParentName(workZone);
                            // if (LogConst.DEBUG) {
                            // ProviderMgr.logger.fine(
                            // "Lookup: new work zone is " +
                            // "\"" + workZone + "\"");
                            // }
                        } else {
                            // give up
                            break;
                            // throw new ServiceUnavailableException(
                            // "Unable to " +
                            // "contact authoritative server for " +
                            // qName +
                            // " and no other results were found");
                        }
                    }
                } catch (NameNotFoundException e) {
                    throw e;
                } catch (DomainProtocolException e) {
                    throw e;
                }
            } // query servers loop

        } // questions loop
        return answers.elements();
    }

    /**
     * Lists entire DNS zone using zone transfer mechanism.
     * 
     * @param name
     *            DNS zone name
     * @return enumeration with found <code>ResourceRecord</code> objects
     * @throws SecurityException
     *             if the resolver is not allowed to use a network subsystem
     * @throws NameNotFoundException
     *             if authoritative server(s) was not found
     * @throws ServiceUnavailableException
     *             if none of found servers permits zone transfers
     * @throws DomainProtocolException
     *             if some DNS specific error has occured
     */
    public Enumeration<ResourceRecord> list(String name) throws NamingException {
        final int OUT_BUF_SIZE = 512;
        final int IN_BUF_SIZE = 65536;

        Vector<ResourceRecord> answerVect = new Vector<ResourceRecord>();
        Message mesToSend = null;
        Message receivedMes = null;
        Enumeration<ResourceRecord> enum1;
        // String zoneMasterServer = null;
        // Vector authoritativeServerIPs = new Vector();
        HashSet<Object> authoritativeServers = new HashSet<Object>();
        Iterator<Object> authServersIter;
        int qClassArr[] = new int[1];
        byte outBuf[] = new byte[OUT_BUF_SIZE];
        int outLen;
        byte inBuf[] = new byte[IN_BUF_SIZE];
        boolean received = false;
        boolean completeAnswer = false;
        String proto = null;

        ResolverCache cache = ResolverCache.getInstance();
        // SList slist = SList.getInstance();

        if (name == null) {
            // jndi.2E=The name is null
            throw new NullPointerException(Messages.getString("jndi.2E")); //$NON-NLS-1$
        }
        // if given name is SRV style name where domain name is prefixed
        // with _Proto
        if (name.startsWith("_")) { //$NON-NLS-1$
            int n = name.indexOf('.');

            if (n != -1) {
                proto = name.substring(0, n);
                if (name.length() > n) {
                    name = name.substring(n + 1, name.length());
                } else {
                    // nonsense
                    name = "."; //$NON-NLS-1$
                }
            } else {
                // nonsense
                name = "."; //$NON-NLS-1$
            }
        }
        enum1 = lookup(name, new int[] { ProviderConstants.NS_TYPE },
                new int[] { ProviderConstants.ANY_QTYPE });
        mesToSend = createMessageForSending(name, ProviderConstants.AXFR_QTYPE,
                ProviderConstants.ANY_QCLASS);
        outLen = mesToSend.writeBytes(outBuf, 0);
        // determine the list of zone authoritative servers
        while (enum1.hasMoreElements()) {
            ResourceRecord rr = enum1.nextElement();

            if (rr.getRRType() == ProviderConstants.NS_TYPE) {
                authoritativeServers.add(rr.getRData());
                // assertion: all authoritative servers should have the same
                // DNS class
                qClassArr[0] = rr.getRRClass();
            } else if (rr.getRRType() == ProviderConstants.SOA_TYPE) {
                StringTokenizer st = new StringTokenizer(
                        (String) rr.getRData(), " "); //$NON-NLS-1$

                if (st.hasMoreTokens()) {
                    authoritativeServers.add(st.nextToken());
                    qClassArr[0] = rr.getRRClass();
                    break;
                }
            }
        }
        // try to perform a zone transfer
        authServersIter = authoritativeServers.iterator();
        authServersLoop: while (authServersIter.hasNext()) {
            String authServerName = (String) authServersIter.next();
            Enumeration<ResourceRecord> addrEnum = lookup(authServerName,
                    new int[] { ProviderConstants.A_TYPE }, qClassArr);

            while (addrEnum.hasMoreElements()) {
                ResourceRecord curRR = addrEnum.nextElement();
                String ip = (String) curRR.getRData();

                try {
                    // if (LogConst.DEBUG) {
                    // ProviderMgr.logger.fine(
                    // "Initiating zone transfer, IP=" + ip);
                    // }
                    TransportMgr.sendReceiveTCP(ip,
                            ProviderConstants.DEFAULT_DNS_PORT, outBuf, outLen,
                            inBuf, IN_BUF_SIZE, this.initialTimeout
                                    * this.timeoutRetries);
                    received = true;
                } catch (SocketTimeoutException e) {

                    // if (LogConst.DEBUG) {
                    // ProviderMgr.logger.fine("Socket timeout");
                    // }
                } catch (DomainProtocolException e) {
                    // some problem was encountered
                    // ProviderMgr.logger.log(Level.WARNING,
                    // "Connection failure", e);
                }
                if (received) {
                    receivedMes = new Message();
                    try {
                        int rCode;

                        Message.parseMessage(inBuf, 0, receivedMes);
                        rCode = receivedMes.getRCode();
                        switch (rCode) {
                            case ProviderConstants.NO_ERROR:
                                // put all received records into Resolver's
                                // cache
                                for (int k = 0; k < 3; k++) {
                                    switch (k) {
                                        case 0:
                                            enum1 = receivedMes.getAnswerRRs();
                                            break;
                                        case 1:
                                            enum1 = receivedMes
                                                    .getAuthorityRRs();
                                            break;
                                        case 2:
                                            enum1 = receivedMes
                                                    .getAdditionalRRs();
                                            break;
                                    }
                                    while (enum1.hasMoreElements()) {
                                        ResourceRecord rr = enum1.nextElement();

                                        cache.put(rr);
                                        if (k == 0) {
                                            answerVect.addElement(rr);
                                        }
                                    }
                                }
                                completeAnswer = true;
                                break;
                            case ProviderConstants.NAME_ERROR:
                                // jndi.6D=Name {0} was not found
                                throw new NameNotFoundException(Messages
                                        .getString("jndi.6D", name)); //$NON-NLS-1$
                            case ProviderConstants.SERVER_FAILURE:
                            case ProviderConstants.FORMAT_ERROR:
                            case ProviderConstants.NOT_IMPLEMENTED:
                            case ProviderConstants.REFUSED:
                            default:
                        }
                    } catch (DomainProtocolException e) {
                        // ProviderMgr.logger.log(Level.WARNING,
                        // "Error while parsing of DNS message", e);
                    }
                } // if received
                if (completeAnswer) {
                    // if (LogConst.DEBUG) {
                    // ProviderMgr.logger.fine(
                    // "list: Complete answer received");
                    // }
                    break authServersLoop;
                }
            } // address loop
        } // authoritative servers loop

        if (!completeAnswer) {
            // found nothing
            // jndi.6E=Unable to perform zone transfer
            throw new ServiceUnavailableException(Messages.getString("jndi.6E")); //$NON-NLS-1$
        }
        // SRV _Proto prefix support - filter all records that don't have given
        // _Proto field
        if (proto != null) {
            Vector<ResourceRecord> answerVect2 = new Vector<ResourceRecord>();

            for (int i = 0; i < answerVect.size(); i++) {
                ResourceRecord rr = answerVect.elementAt(i);
                StringTokenizer st = new StringTokenizer(rr.getName(), "."); //$NON-NLS-1$
                String token = null;
                boolean valid = false;

                if (st.hasMoreTokens()) {
                    token = st.nextToken();
                    if (token.length() > 0 && token.charAt(0) == '_'
                            && st.hasMoreTokens()) {
                        token = st.nextToken();
                        if (token.equalsIgnoreCase(proto)) {
                            valid = true;
                        }
                    }
                }
                if (valid) {
                    answerVect2.addElement(rr);
                }
            }
            answerVect = answerVect2;
        }
        return answerVect.elements();
    }

    /**
     * Adds initial DNS server the resolver should start with. Trying underlying
     * OS services to determine IP from name.
     * 
     * @param name
     *            server's name
     * @param ip
     *            server's IP address
     * @param port
     *            port on server
     */
    public void addInitialServer(String name, String ip, int port,
            String zoneName) {
        SList.Server server = new SList.Server(name, ip, port);
        SList slist = SList.getInstance();

        if (name == null && ip == null) {
            // jndi.6F=Both name and IP are null
            throw new NullPointerException(Messages.getString("jndi.6F")); //$NON-NLS-1$
        }
        if (zoneName == null) {
            // jndi.70=zoneName is null
            throw new NullPointerException(Messages.getString("jndi.70")); //$NON-NLS-1$
        }
        // if IP is not given and we don't know this server yet
        // try to determine IP from underlying OS services
        if (ip == null && !slist.hasServer(name)) {
            InetAddress addrObj = TransportMgr.getIPByName_OS(name);

            if (addrObj != null) {
                server.setIP(ProviderMgr.getIpStr(addrObj.getAddress()));
            }
        }
        // add given zone <-> server pair
        if (!slist.hasServer(zoneName, server)) {
            slist.updateEntry(zoneName, server, SList.UNKNOWN);
        }
    }

    /**
     * Query available DNS servers for desired information. This method doesn't
     * look into the local cache. Drops all answers that contains "server fail"
     * and "not implemented" answer codes and returns the first "good" answer.
     * 
     * @param request
     *            a DNS message that contains the request record
     * @param workZone
     *            a zone that is closest known (grand-) parent of the desired
     *            name
     * @param visitedServers
     *            the hash list of servers, that should not be examined; this
     *            method also appends to this list all server that have been
     *            visited during execution of this method
     * @param tcpOnly
     *            <code>true</code> if we want to use TCP protocol only;
     *            otherwise UDP will be tried first
     * @return the message received; <code>null</code> if none found
     * @throws DomainProtocolException
     *             some domain protocol related error occured
     * @throws SecurityException
     *             if the resolver doesn't have the permission to use sockets
     */
    Message queryServers(Message request, String workZone,
            Hashtable<Server, Object> visitedServers, boolean tcpOnly)
            throws DomainProtocolException, SecurityException {
        QuestionRecord qRecord;

        SList slist = SList.getInstance();
        SList.Server curServer;
        byte[] outBuf = new byte[MSG_MAX_BYTES];
        int outBufLen;
        byte[] inBuf = new byte[MSG_MAX_BYTES];
        Message receivedMes = null;
        int idx = 0;
        int curTimeout = this.initialTimeout;
        boolean received = false;
        boolean parsed = false;
        boolean correctAnswer = false;
        int rCode = -1;

        // determine a question
        if (!request.getQuestionRecords().hasMoreElements()) {
            // jndi.71=no question record
            throw new IllegalArgumentException(Messages.getString("jndi.71")); //$NON-NLS-1$
        }
        qRecord = request.getQuestionRecords().nextElement();
        // preparing a domain protocol message
        outBufLen = request.writeBytes(outBuf, 0);

        // sending message and trying to receive an answer
        for (int round = 0; round < this.timeoutRetries; round++) {
            Set<Server> queriedServers = new HashSet<Server>();

            // start of round
            while (true) {
                int responseTime = 0;

                received = false;
                parsed = false;
                rCode = -1;
                // get next server
                curServer = slist.getBestGuess(workZone, visitedServers);
                if (curServer == null || queriedServers.contains(curServer)) {
                    // end of round
                    break;
                }
                if (curServer.getIP() == null) {
                    // if we don't know IP lets start background resolving
                    // thread
                    startResolvingThread(curServer.getName(), qRecord
                            .getQClass());
                    slist.updateEntry(workZone, curServer,
                            SList.NETWORK_FAILURE);
                    queriedServers.add(curServer);
                    continue;
                }
                // send the message and receive the answer
                try {
                    // if (LogConst.DEBUG) {
                    // ProviderMgr.logger.fine("Timeout is set to " +
                    // curTimeout);
                    // ProviderMgr.logger.fine("Querying server \"" +
                    // curServer + "\"");
                    // }
                    // timeBeforeSending = System.currentTimeMillis();
                    if (tcpOnly) {
                        TransportMgr.sendReceiveTCP(curServer.getIP(),
                                curServer.getPort(), outBuf, outBufLen, inBuf,
                                inBuf.length, curTimeout);
                    } else {
                        TransportMgr.sendReceiveUDP(curServer.getIP(),
                                curServer.getPort(), outBuf, outBufLen, inBuf,
                                inBuf.length, curTimeout);
                    }
                    // responseTime = (int) (System.currentTimeMillis() -
                    // timeBeforeSending);
                    // ProviderMgr.logger.fine("Answer received in " +
                    // responseTime + " milliseconds");
                    received = true;
                } catch (SocketTimeoutException e) {
                    slist.updateEntry(workZone, curServer, SList.TIMEOUT);
                    // if (LogConst.DEBUG) {
                    // ProviderMgr.logger.fine("Socket timeout");
                    // }
                } catch (DomainProtocolException e) {
                    // problems with receiving the message
                    // skipping this server
                    slist.updateEntry(workZone, curServer,
                            SList.NETWORK_FAILURE);
                    // ProviderMgr.logger.log(Level.WARNING,
                    // "Connection failure", e);
                }
                // parse the message
                if (received) {
                    try {
                        boolean answerSectionIsTruncated = false;

                        receivedMes = new Message();
                        idx = 0;
                        idx = Message.parseMessage(inBuf, idx, receivedMes);

                        // if (LogConst.DEBUG) {
                        // ProviderMgr.logger.finest("Received message:\n" +
                        // receivedMes.toString());
                        // }
                        parsed = true;
                        // handle a truncation
                        if (receivedMes.isTc() && !tcpOnly) {
                            // The Message is truncated.
                            // Let's try to establish a TCP connection
                            // and retransmit the message over that connection.
                            // if (LogConst.DEBUG) {
                            // ProviderMgr.logger.fine("Message is truncated");
                            // ProviderMgr.logger.fine("Trying to establish " +
                            // "a connection over TCP");
                            // }
                            try {
                                Message receivedMesTcp;
                                int idx2;

                                TransportMgr.sendReceiveTCP(curServer.getIP(),
                                        curServer.getPort(), outBuf, outBufLen,
                                        inBuf, inBuf.length, curTimeout);
                                receivedMesTcp = new Message();
                                idx2 = Message.parseMessage(inBuf, 0,
                                        receivedMesTcp);
                                // complete message was received
                                if (!receivedMesTcp.isTc()) {
                                    receivedMes = receivedMesTcp;
                                    idx = idx2;
                                }
                            } catch (Exception e) {
                                // ProviderMgr.logger.log(Level.WARNING,
                                // "Receiving a complete message" +
                                // " over TCP failed", e);
                                // if (LogConst.DEBUG) {
                                // ProviderMgr.logger.fine(
                                // "Parsing the message " +
                                // "previously received over UDP");
                                // }
                            }
                        }
                        // Is the message still truncated?
                        // (It is possible in case if TCP connection failed)
                        if (receivedMes.isTc()) {
                            // check if the ANSWER section is truncated
                            // or not
                            if (!receivedMes.getAuthorityRRs()
                                    .hasMoreElements()
                                    && !receivedMes.getAdditionalRRs()
                                            .hasMoreElements()) {
                                answerSectionIsTruncated = true;
                            }
                        }
                        rCode = receivedMes.getRCode();
                        if (rCode == ProviderConstants.NO_ERROR) {
                            // correct message has been received
                            slist
                                    .updateEntry(workZone, curServer,
                                            responseTime);
                            visitedServers.put(curServer, new Object()); // $NON-LOCK-1$
                            if (!answerSectionIsTruncated) {
                                correctAnswer = true;
                                break;
                            }
                        } else if (rCode == ProviderConstants.SERVER_FAILURE) {
                            // removing server from list
                            // ProviderMgr.logger.warning("Server failure. " +
                            // errMsg);
                            slist.updateEntry(workZone, curServer,
                                    SList.SERVER_FAILURE);
                            visitedServers.put(curServer, new Object()); // $NON-LOCK-1$
                        } else if (rCode == ProviderConstants.FORMAT_ERROR) {
                            // removing server from list
                            // ProviderMgr.logger.warning("Format error. " +
                            // errMsg);
                            slist.updateEntry(workZone, curServer,
                                    SList.SERVER_FAILURE);
                            visitedServers.put(curServer, new Object()); // $NON-LOCK-1$
                        } else if (rCode == ProviderConstants.NAME_ERROR) {
                            // ProviderMgr.logger.warning("Name error. " +
                            // errMsg);
                            if (receivedMes.isAA()) {
                                slist.updateEntry(workZone, curServer,
                                        responseTime);
                                visitedServers.put(curServer, new Object()); // $NON-LOCK-1$
                                correctAnswer = true;
                                // if (LogConst.DEBUG) {
                                // ProviderMgr.logger.fine(
                                // "Return name error to user");
                                // }
                                break;
                            }
                            // This server is not authoritative server for
                            // this zone. It should not answer with a
                            // name error. Probably it is misconfigured.
                            slist.updateEntry(workZone, curServer,
                                    SList.SERVER_FAILURE);
                            visitedServers.put(curServer, new Object()); // $NON-LOCK-1$
                            // if (LogConst.DEBUG) {
                            // ProviderMgr.logger.fine(
                            // "Not authoritative answer. " +
                            // "Skip it.");
                            // }
                        } else if (rCode == ProviderConstants.NOT_IMPLEMENTED) {
                            // ProviderMgr.logger.warning("Not implemented. " +
                            // errMsg);
                            slist.updateEntry(workZone, curServer,
                                    SList.SERVER_FAILURE);
                            visitedServers.put(curServer, new Object()); // $NON-LOCK-1$
                        } else if (rCode == ProviderConstants.REFUSED) {
                            // ProviderMgr.logger.warning("Refused. " +
                            // errMsg);
                            slist.updateEntry(workZone, curServer,
                                    SList.SERVER_FAILURE);
                            visitedServers.put(curServer, new Object()); // $NON-LOCK-1$
                        }
                    } catch (DomainProtocolException e) {
                        // removing this server from SLIST
                        slist.dropServer(workZone, curServer);
                        // ProviderMgr.logger.warning("Unknown error.");
                    } catch (IndexOutOfBoundsException e) {
                        // bad message received
                        slist.dropServer(workZone, curServer);
                        // ProviderMgr.logger.warning("Bad message received: " +
                        // " IndexOutOfBoundsException.");
                    }
                }
                queriedServers.add(curServer);
            }
            // end of round

            if (received & parsed & correctAnswer) {
                // correct answer received
                return receivedMes;
            }
            curTimeout *= 2;
        }
        // give up - no correct message has been received
        return null;
    }

    /**
     * Analyzes the answer message and constructs an analysis report.
     * 
     * @param request
     *            the request has been send to the server
     * @param answer
     *            the answer has been received
     * @param workZone
     *            the current resolver's work zone
     * @return analysis report TODO may be optimized
     */
    AnalysisReport analyzeAnswer(Message request, Message answer)
            throws DomainProtocolException {
        Enumeration<QuestionRecord> questions = request.getQuestionRecords();
        Enumeration<ResourceRecord> answerRRs = answer.getAnswerRRs();
        Enumeration<ResourceRecord> authorityRRs = answer.getAuthorityRRs();
        Enumeration<ResourceRecord> additionalRRs;
        QuestionRecord question;
        Resolver.AnalysisReport report = new AnalysisReport();

        // Check the ID.
        if (request.getId() != answer.getId()) {
            // jndi.72=Request and Answer have different ids
            throw new DomainProtocolException(Messages.getString("jndi.72")); //$NON-NLS-1$
        }

        // Determine a question.
        if (questions.hasMoreElements()) {
            question = questions.nextElement();
        } else {
            // jndi.73=no question record
            throw new IllegalArgumentException(Messages.getString("jndi.73")); //$NON-NLS-1$
        }
        // If name error occurred - no extra processing needed.
        if (answer.getRCode() == ProviderConstants.NAME_ERROR) {
            report.nameError = true;
            return report;
        }
        // check truncation, truncated message should not be cached
        if (answer.isTc()) {
            report.messageWasTruncated = true;
        }
        // Analyze answer section.
        while (answerRRs.hasMoreElements()) {
            ResourceRecord curRec = answerRRs.nextElement();

            if (question.getQClass() == curRec.getRRClass()
                    || question.getQClass() == ProviderConstants.ANY_QCLASS) {
                if (question.getQType() == ProviderConstants.ANY_QTYPE
                        && ProviderMgr.namesAreEqual(curRec.getName(), question
                                .getQName())) {
                    // If we query for ANY record types and the server returns
                    // some record for the SAME domain name we will collect
                    // all of such records and treat
                    // this situation as a complete answer for this query.
                    // We will not perform any more attempts to obtain more
                    // records.

                    report.records.addElement(curRec);
                    // if (LogConst.DEBUG) {
                    // ProviderMgr.logger.fine("Adding " +
                    // ProviderConstants.rrTypeNames[
                    // curRec.getRRType()]);
                    // }
                    if (curRec.getRRType() == ProviderConstants.CNAME_TYPE) {
                        report.aliasInfoWasReceived = true;
                        report.newName = (String) curRec.getRData();
                        // if (LogConst.DEBUG) {
                        // ProviderMgr.logger.fine("Alias \"" +
                        // report.newName + "\" was received");
                        // }
                    } else {
                        // XXX have we received a complete set of records?
                        report.completeAnswerWasReceived = true;
                    }
                } else if (question.getQType() == curRec.getRRType()
                        && ProviderMgr.namesAreEqual(question.getQName(),
                                curRec.getName())) {
                    // This is a situation when we get the record with the
                    // name and type exactly matching to that we have asked for.
                    // We will treat this as a complete answer.

                    report.records.addElement(curRec);
                    // if (LogConst.DEBUG) {
                    // ProviderMgr.logger.fine("Adding " +
                    // ProviderConstants.rrTypeNames[
                    // curRec.getRRType()]);
                    // }
                    report.completeAnswerWasReceived = true;
                } else if (curRec.getRRType() == ProviderConstants.CNAME_TYPE
                        && ProviderMgr.namesAreEqual(curRec.getName(), question
                                .getQName())) {
                    // This is the case of an alias. If we received an alias for
                    // the name we have asked the information for then we need
                    // to change the desired name to this newly received name.
                    // Then we will try to find necessary information for
                    // this new name in the current answer. If we fail then
                    // we will continue our general lookup algorithm with the
                    // new name instead of an old one. We will query servers
                    // from the SLIST with this new name.

                    // TODO this is not effective
                    Enumeration<ResourceRecord> answerRRs2 = answer
                            .getAnswerRRs();
                    Enumeration<ResourceRecord> additionalRRs2 = answer
                            .getAdditionalRRs();

                    report.aliasInfoWasReceived = true;
                    report.newName = (String) curRec.getRData();
                    report.extraRecords.addElement(curRec);
                    // if (LogConst.DEBUG) {
                    // ProviderMgr.logger.fine("Alias \"" + report.newName +
                    // "\" was received");
                    // }
                    // if we find the one of desired records in the
                    // current answer then we will treat the answer as complete
                    while (answerRRs2.hasMoreElements()) {

                        // Try to look for info about newly received name
                        // in ANSWER section.

                        ResourceRecord tmpRec = answerRRs2.nextElement();

                        // if (LogConst.DEBUG) {
                        // ProviderMgr.logger.fine(
                        // "Look for an answer in ANSWER section");
                        // }
                        if (tmpRec.getRRType() == question.getQType()
                                && ProviderMgr.namesAreEqual(tmpRec.getName(),
                                        report.newName)) {
                            // the answer is founded in ANSWER section
                            report.records.addElement(tmpRec);
                            // if (LogConst.DEBUG) {
                            // ProviderMgr.logger.fine("Adding " +
                            // ProviderConstants.rrTypeNames[
                            // tmpRec.getRRType()]);
                            // }
                            report.completeAnswerWasReceived = true;
                        }
                    }
                    while (additionalRRs2.hasMoreElements()) {
                        // Try to look for info about newly received name
                        // in ADDITIONAL section.

                        ResourceRecord tmpRec = additionalRRs2.nextElement();

                        // if (LogConst.DEBUG) {
                        // ProviderMgr.logger.fine("Look for an answer in " +
                        // "ADDITIONAL section");
                        // }
                        if (tmpRec.getRRType() == question.getQType()
                                && ProviderMgr.namesAreEqual(tmpRec.getName(),
                                        report.newName)) {
                            // the answer is founded in ADDITIONAL section
                            report.records.addElement(tmpRec);
                            // if (LogConst.DEBUG) {
                            // ProviderMgr.logger.fine("Adding " +
                            // ProviderConstants.rrTypeNames[
                            // tmpRec.getRRType()]);
                            // }
                            report.completeAnswerWasReceived = true;
                        }
                    }
                    // if (report.completeAnswerWasReceived) {
                    // if (LogConst.DEBUG) {
                    // ProviderMgr.logger.fine("Complete answer received");
                    // }
                    // }
                } else {
                    // We have received some extra records. Let's save it for
                    // future use.

                    // we will treat authoritative answer as a complete answer
                    // and in no case will perform further actions
                    if (answer.isAA()) {
                        report.completeAnswerWasReceived = true;
                    }
                    report.extraRecords.addElement(curRec);
                    // if (LogConst.DEBUG) {
                    // ProviderMgr.logger.fine("Adding additional record " +
                    // ProviderConstants.rrTypeNames[
                    // curRec.getRRType()]);
                    // }
                }
            } else {
                // The record from another DNS class arrived. Just ignore it.
                // if (LogConst.DEBUG) {
                // ProviderMgr.logger.fine("Ignore records from DNS class " +
                // curRec.getRRClass());
                // }
            }
        }

        // analyze authority section
        // 1. Store all info from authority NS records; try to locate NS IPs
        // from additional records in case if it is not present in SLIST;
        // start new background lookup process if not found in additional
        // section
        // TODO current implementation isn't effective
        while (authorityRRs.hasMoreElements()) {
            ResourceRecord curRec = authorityRRs.nextElement();
            SList slist = SList.getInstance();

            // save record for future use
            report.extraRecords.addElement(curRec);
            // analyze
            if (curRec.getRRType() == ProviderConstants.NS_TYPE) {
                String serverName = (String) curRec.getRData();
                SList.Server server2 = new SList.Server(serverName, null,
                        ProviderConstants.DEFAULT_DNS_PORT);
                SList.Server server = slist.getServerByServer(curRec.getName(),
                        server2);

                report.delegationArrived = true;
                if (server == null) {
                    // not found in SLIST
                    slist.updateEntry(curRec.getName(), server2, SList.UNKNOWN);
                    report.delegationZones.addElement(curRec.getName());
                    server = server2;
                }
                if (server != null && server.getIP() == null) {
                    // try to search additional records to obtain server's IP
                    additionalRRs = answer.getAdditionalRRs();
                    while (additionalRRs.hasMoreElements()) {
                        ResourceRecord addRec = additionalRRs.nextElement();

                        if (ProviderMgr.namesAreEqual(addRec.getName(),
                                serverName)
                                && addRec.getRRType() == ProviderConstants.A_TYPE) {
                            server.setIP((String) addRec.getRData());
                        }
                    }
                    if (server.getIP() == null) {
                        // IP was not found in additional section
                        // start resolving process in the background
                        this.startResolvingThread(server.getName(), curRec
                                .getRRClass());
                    }
                }
                // if (LogConst.DEBUG) {
                // ProviderMgr.logger.fine("Delegation \"" + server +
                // "\" arrived");
                // }
            } // end of NS type analysis
        } // end of authority section analysis

        // analyze additional section
        additionalRRs = answer.getAdditionalRRs();
        while (additionalRRs.hasMoreElements()) {
            ResourceRecord addRec = additionalRRs.nextElement();

            report.extraRecords.addElement(addRec);
            // if (LogConst.DEBUG) {
            // ProviderMgr.logger.fine("Adding additional record " +
            // ProviderConstants.rrTypeNames[addRec.getRRType()]);
            // }
        }

        // Fixing RRSet TTL issue.
        // If TTL fields in RRSet are not all the same then we need to set
        // all TTLs to lowest found value.
        // See RFC 2181 point 5.2

        // checking report.records and report.extraRecords
        for (int k = 0; k < 2; k++) {
            Vector<ResourceRecord> records = null;
            HashSet<String> processed = new HashSet<String>();

            switch (k) {
                case 0:
                    records = report.records;
                    break;
                case 1:
                    records = report.extraRecords;
                    break;
            }
            for (int i = 0; i < records.size(); i++) {
                ResourceRecord rr = records.elementAt(i);
                String key = rr.getName() + " " + rr.getRRClass() + " " + //$NON-NLS-1$ //$NON-NLS-2$
                        rr.getRRType();
                long ttl = rr.getTtl();
                Vector<ResourceRecord> objToUpdateTTL = new Vector<ResourceRecord>();

                if (processed.contains(key)) {
                    continue;
                }
                objToUpdateTTL.addElement(rr);
                // look forward for records with the same NAME CLASS TYPE
                for (int j = i; j < records.size(); j++) {
                    ResourceRecord rr2 = records.elementAt(j);
                    String key2 = rr2.getName() + " " + rr2.getRRClass() + " " + //$NON-NLS-1$ //$NON-NLS-2$
                            rr2.getRRType();
                    long ttl2 = rr2.getTtl();

                    if (processed.contains(key2)) {
                        continue;
                    }
                    if (key.equals(key2)) {
                        if (ttl > ttl2) {
                            ttl = ttl2;
                        }
                        objToUpdateTTL.addElement(rr2);
                    }
                }
                // update TTL if necessary
                for (int j = 0; j < objToUpdateTTL.size(); j++) {
                    ResourceRecord rr2 = objToUpdateTTL.elementAt(j);

                    if (rr2.getTtl() != ttl) {
                        rr2.setTtl(ttl);
                    }
                }
                // don't process such NAME CLASS TYPE combination any more
                processed.add(key);
            }
        } // fixing RRSet TTL issue

        return report;
    }

    /**
     * Creates a new <code>Message</code> object and fills some of it's
     * standard fields.
     * 
     * @return created <code>Message</code> object
     */
    static Message createMessageForSending(String desiredName, int recType,
            int recClass) throws DomainProtocolException {
        Message mes = new Message();
        QuestionRecord qr = new QuestionRecord();

        mes.setId(rndGen.nextInt() & 0xffff);
        mes.setQR(ProviderConstants.QR_QUERY);
        mes.setOpCode(ProviderConstants.QUERY);
        mes.setRD(true);
        mes.setQDCount(1);
        qr.setQName(desiredName);
        qr.setQType(recType);
        qr.setQClass(recClass);
        mes.addQuestionRecord(qr);
        return mes;
    }

    /**
     * Starts new resolver thread that will be searching for IP of the given
     * hostname.
     * 
     * @param hostname
     *            hostname to resolve
     * @param dnsClass
     *            DNS class of host
     */
    void startResolvingThread(String hostname, int dnsClass) {
        Thread newThread;
        Resolver.ThreadListEntry newEntry;
        int classes[] = new int[1];

        synchronized (threadListSemaphore) {
            // check that no currently running thread looks for this hostname
            for (int i = 0; i < resolverThreads.size(); i++) {
                Resolver.ThreadListEntry entry = resolverThreads.get(i);
                if (ProviderMgr.namesAreEqual(hostname,
                        entry.serverNameToResolve)
                        && entry.dnsClass == dnsClass) {
                    // this hostname is already under investigation
                    // exiting
                    return;
                }
            }
            // check if the hostname is already scheduled for resolving
            for (int i = 0; i < hostnamesToResolve.size(); i++) {
                Resolver.ThreadListEntry entry = hostnamesToResolve.get(i);
                if (ProviderMgr.namesAreEqual(hostname,
                        entry.serverNameToResolve)
                        && entry.dnsClass == dnsClass) {
                    // this hostname is already scheduled for resolving
                    // exiting
                    return;
                }
            }
            // check that the maximum number of threads is not exceeded
            if (resolverThreads.size() >= threadNumberLimit) {
                // maximum possible number of threads is reached already
                return;
            }
            classes[0] = dnsClass;
            newEntry = new Resolver.ThreadListEntry();
            newEntry.serverNameToResolve = hostname;
            newEntry.dnsClass = dnsClass;
            hostnamesToResolve.add(newEntry);
            // starting new thread that should make further updates by itself
            newThread = new Thread(this);
            // if (LogConst.DEBUG) {
            // ProviderMgr.logger.fine("Starting new resolver thread," +
            // " target hostname: " + hostname);
            // }
            newThread.start();
        }
    }

    /**
     * Start background search of the address of next unresolved server hostname
     */
    public void run() {
        SList slist = SList.getInstance();
        Enumeration<ResourceRecord> foundRecords;
        Resolver.ThreadListEntry entryToProcess;
        int[] classes = new int[1];

        // update lists
        synchronized (threadListSemaphore) {
            if (hostnamesToResolve.size() > 0) {
                entryToProcess = hostnamesToResolve.get(0);
                hostnamesToResolve.remove(0);
                entryToProcess.thread = Thread.currentThread();
                resolverThreads.add(entryToProcess);
            } else {
                // ProviderMgr.logger.warning(
                // "Resolver thread: no host name to resolve");
                return;
            }
        }
        // lookup
        try {
            classes[0] = entryToProcess.dnsClass;
            foundRecords = lookup(entryToProcess.serverNameToResolve,
                    new int[] { ProviderConstants.A_TYPE }, classes);
            while (foundRecords != null && foundRecords.hasMoreElements()) {
                // we will take all A records and store all of them in SLIST
                ResourceRecord rr = foundRecords.nextElement();

                if (rr.getRRType() == ProviderConstants.A_TYPE) {
                    slist.setServerIP(entryToProcess.serverNameToResolve,
                            (String) rr.getRData());
                }
            }
        } catch (NamingException e) {
            // just ignore it
        }
        // update resolver threads list, remove info about current thread
        synchronized (threadListSemaphore) {
            for (int i = 0; i < resolverThreads.size(); i++) {
                Resolver.ThreadListEntry entry = resolverThreads.get(i);

                if (ProviderMgr.namesAreEqual(
                        entryToProcess.serverNameToResolve,
                        entry.serverNameToResolve)
                        && entryToProcess.dnsClass == entry.dnsClass) {
                    resolverThreads.remove(i);
                    break;
                }
            }
        }
        // exiting
    }

    /**
     * Analysis report.
     * 
     * @see Resolver#analyzeAnswer(Message, Message)
     */
    static class AnalysisReport {

        boolean completeAnswerWasReceived = false;

        boolean nameError = false;

        boolean delegationArrived = false;

        boolean aliasInfoWasReceived = false;

        boolean messageWasTruncated = false;

        Vector<ResourceRecord> records;

        Vector<String> delegationZones;

        String newName = null;

        Vector<ResourceRecord> extraRecords;

        AnalysisReport() {
            records = new Vector<ResourceRecord>();
            delegationZones = new Vector<String>();
            extraRecords = new Vector<ResourceRecord>();
        }

    }
}
