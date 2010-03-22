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

package org.apache.harmony.jndi.provider.dns;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * Represents DNS resolver's SLIST - the structure to keep the collected
 * information about active DNS servers and zones they contain information
 * about.
 * 
 * @see RFC 1034 TODO some methods can be optimized
 */
class SList {

    public static int NETWORK_FAILURE = Integer.MAX_VALUE - 3;

    public static int TIMEOUT = Integer.MAX_VALUE - 2;

    public static int SERVER_FAILURE = Integer.MAX_VALUE - 1;

    public static int UNKNOWN = 0;

    // Hash with vectors; one vector of server entries per zone
    private Hashtable<String, Vector<Entry>> zones;

    // the array with known DNS servers information
    private Vector<Server> servers;

    /**
     * @see #getInstance()
     */
    private SList() {
        zones = new Hashtable<String, Vector<Entry>>();
        servers = new Vector<Server>();
    }

    private static SList instance = new SList();

    /**
     * <code>SList</code> is a singleton class.
     * 
     * @return instance of <code>SList</code>
     */
    static SList getInstance() {
        return instance;
    }

    /**
     * Updates existent SLIST entry or creates a new one. S-List will be sorted
     * according the response time. Entries with bigger response will be placed
     * father from the beginning of the list.
     * 
     * @param zone
     *            the name of DNS zone
     * @param server
     *            the server that is known to have the information about given
     *            zone
     * @param responseTime
     *            response time for server for this particular DNS zone
     */
    void updateEntry(String zone, Server server, int responseTime) {
        String normZoneName = ProviderMgr.normalizeName(zone);
        Vector<Entry> vect;
        Entry entryToAdd = new Entry(normZoneName, getServerNum(server),
                responseTime);

        synchronized (zones) {
            vect = zones.get(ProviderMgr.normalizeName(normZoneName));
            if (vect == null) {
                vect = new Vector<Entry>();
                vect.addElement(entryToAdd);
                zones.put(normZoneName, vect);
            } else {
                boolean added = false;

                // delete previous occurrence of given server
                for (int i = 0; i < vect.size(); i++) {
                    Entry curEntry = vect.elementAt(i);

                    if (server.equals(serverAtNum(curEntry.getServerNum()))) {
                        vect.removeElementAt(i);
                        break;
                    }
                }

                // and insert a new one with updated response time
                for (int i = 0; i < vect.size(); i++) {
                    Entry curEntry = vect.elementAt(i);

                    if (responseTime < curEntry.getResponseTime()) {
                        vect.insertElementAt(entryToAdd, i);
                        added = true;
                        break;
                    }
                }
                // append to the end of list if not found
                if (!added) {
                    vect.addElement(entryToAdd);
                }
            }
        } // synchronized block
    }

    /**
     * Returns the best guess about that DNS server should be chosen to send the
     * request concerning the particular DNS zone.
     * 
     * @param zone
     *            the name of DNS zone
     * @return best guess - a <code>SList.Server</code> object;
     *         <code>null</code> if the information is not found
     */
    Server getBestGuess(String zone, Hashtable<Server, ?> serversToIgnore) {
        Vector<Entry> vect;

        synchronized (zones) {
            vect = zones.get(ProviderMgr.normalizeName(zone));
            if (vect != null && vect.size() > 0) {
                for (int i = 0; i < vect.size(); i++) {
                    Entry entry = vect.elementAt(i);

                    if (serversToIgnore != null) {
                        if (serversToIgnore.get(serverAtNum(entry
                                .getServerNum())) != null) {
                            continue;
                        }
                    }
                    return serverAtNum(entry.getServerNum());
                }
            }
        }
        return null;
    }

    /**
     * Removes occurrence of given server related to given zone from the SLIST.
     * 
     * @param zone
     *            DNS zone
     * @param server
     *            the server to remove
     */
    void dropServer(String zone, Server server) {
        Vector<Entry> vect;

        synchronized (zones) {
            vect = zones.get(ProviderMgr.normalizeName(zone));
            if (vect != null) {
                for (int i = 0; i < vect.size(); i++) {
                    Entry entry = vect.elementAt(i);

                    if (server.equals(serverAtNum(entry.getServerNum()))) {
                        vect.removeElementAt(i);
                        break;
                    }
                }
            }
        }
    }

    /**
     * @param zone
     *            the name of zone
     * @param server
     *            DNS server
     * @return <code>true</code> if SList has information about specified
     *         server & zone combination; <code>false</code> otherwise
     */
    boolean hasServer(String zone, Server server) {
        Vector<Entry> vect;

        synchronized (zones) {
            vect = zones.get(ProviderMgr.normalizeName(zone));
            if (vect != null) {
                for (int i = 0; i < vect.size(); i++) {
                    Entry entry = vect.elementAt(i);

                    if (server.equals(serverAtNum(entry.getServerNum()))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @param zone
     *            the name DNS zone
     * @param srvName
     *            the name of the server
     * @param srvPort
     *            the port of the server
     * @return <code>Server</code> object with specified attributes
     */
    Server getServerByName(String zone, String name, int port) {
        Vector<Entry> vect;

        synchronized (zones) {
            vect = zones.get(ProviderMgr.normalizeName(zone));
            if (vect != null) {
                for (int i = 0; i < vect.size(); i++) {
                    Entry entry = vect.elementAt(i);

                    if (ProviderMgr.namesAreEqual(name, serverAtNum(
                            entry.getServerNum()).getName())
                            && port == serverAtNum(entry.getServerNum())
                                    .getPort()) {
                        return serverAtNum(entry.getServerNum());
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param zone
     *            name of DNS zone
     * @param srvIP
     *            IPv4 address of server
     * @param srvPort
     *            port on server
     * @return <code>Server</code> object with specified attributes
     */
    Server getServerByIP(String zone, String ip, int port) {
        Vector<Entry> vect;

        synchronized (zones) {
            vect = zones.get(ProviderMgr.normalizeName(zone));
            if (vect != null) {
                for (int i = 0; i < vect.size(); i++) {
                    Entry entry = vect.elementAt(i);

                    if (ip.equals(serverAtNum(entry.getServerNum()).getIP())
                            && port == serverAtNum(entry.getServerNum())
                                    .getPort()) {
                        return serverAtNum(entry.getServerNum());
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param zone
     *            the name of DNS zone to query SLIST with
     * @param server
     *            the server to compare with
     * @return first <code>Server</code> object from SLIST that equals to
     *         specified server in terms of <code>equals()</code> method;
     *         <code>null</code> if not found.
     * @see SList.Server#equals(SList.Server)
     */
    Server getServerByServer(String zone, Server server) {
        Vector<Entry> vect;

        synchronized (zones) {
            vect = zones.get(ProviderMgr.normalizeName(zone));

            if (vect != null) {
                for (int i = 0; i < vect.size(); i++) {
                    Entry entry = vect.elementAt(i);
                    if (server.equals(serverAtNum(entry.getServerNum()))) {
                        return serverAtNum(entry.getServerNum());
                    }
                }
            }
        }
        return null;
    }

    // --- managing local list of servers ---

    // since the list of servers is add-only entity a write synchronization
    // should be enough

    /**
     * @return number of given server in the internal array of servers; add the
     *         server if not found
     */
    private int getServerNum(Server server) {
        if (servers.contains(server)) {
            return servers.indexOf(server);
        }
        synchronized (servers) {
            servers.addElement(server);
            return servers.size() - 1;
        }
    }

    /**
     * @param num
     *            internal number of server
     * @return <code>Server</code> object found at specified index
     */
    private Server serverAtNum(int num) {
        if (num < servers.size()) {
            return servers.elementAt(num);
        }
        return null;
    }

    /**
     * Checks if given server is present in the internal list of known servers.
     * 
     * @param hostname
     *            host name of server
     * @return <code>true</code> or <code>false</code>
     */
    boolean hasServer(String hostname) {
        return servers.contains(new Server(hostname, null,
                ProviderConstants.DEFAULT_DNS_PORT));
    }

    /**
     * Returns all occurrences of server with specified
     * 
     * @param name
     *            hostname
     * @return found server object or <code>null</code> if not found
     */
    Enumeration<Server> getServersByName(String name) {
        Vector<Server> result = new Vector<Server>();

        if (name == null) {
            // jndi.34=hostname is null
            throw new NullPointerException(Messages.getString("jndi.34")); //$NON-NLS-1$
        }
        for (int i = 0; i < servers.size(); i++) {
            Server curServ = servers.get(i);

            if (curServ.getName() != null
                    && ProviderMgr.namesAreEqual(name, curServ.getName())) {
                result.addElement(curServ);
            }
        }
        return result.elements();
    }

    /**
     * Add IP information of server in list. Affects only servers with IP set to
     * <code>null</code>.
     * 
     * @param hostname
     *            hostname of server
     * @param newIP
     *            new IP
     */
    void setServerIP(String hostname, String newIP) {
        String nameNorm = ProviderMgr.normalizeName(hostname);

        for (int i = 0; i < servers.size(); i++) {
            SList.Server serv = servers.elementAt(i);

            if (nameNorm.equals(serv.getName()) && serv.getIP() == null) {
                serv.setIP(newIP);
                break;
            }
        }
    }

    // --- additional classes ---

    /**
     * Represents an SLIST entry.
     */
    static class Entry {

        private String zoneName;

        private int serverNum;

        private int responseTime;

        /**
         * Creates new SLIST entry.
         * 
         * @param zoneName
         * @param server
         * @param respTime
         */
        public Entry(String zoneName, int serverNum, int respTime) {
            this.zoneName = zoneName;
            this.serverNum = serverNum;
            this.responseTime = respTime;
        }

        /**
         * @return Returns the responseTime.
         */
        public int getResponseTime() {
            return responseTime;
        }

        /**
         * @param responseTime
         *            The responseTime to set.
         */
        public void setResponseTime(int responseTime) {
            this.responseTime = responseTime;
        }

        /**
         * @return Returns the server.
         */
        public int getServerNum() {
            return serverNum;
        }

        /**
         * @param server
         *            The server to set.
         */
        public void setServerNum(int serverNum) {
            this.serverNum = serverNum;
        }

        /**
         * @return Returns the zoneName.
         */
        public String getZoneName() {
            return zoneName;
        }

        /**
         * @param zoneName
         *            The zoneName to set.
         */
        public void setZoneName(String zoneName) {
            this.zoneName = zoneName;
        }
    }

    /**
     * Represents a DNS server.
     */
    static class Server {

        private String serverName;

        private String serverIP;

        private int serverPort;

        /**
         * Constructs new <code>Server</code> object with given parameters.
         * 
         * @param serverName
         *            the name of the server
         * @param serverIP
         *            IP address of the server
         * @param serverPort
         *            a port number
         */
        public Server(String serverName, String serverIP, int serverPort) {
            this.serverName = ProviderMgr.normalizeName(serverName);
            this.serverIP = serverIP;
            this.serverPort = serverPort;
        }

        /**
         * Returns <code>true</code> if two servers are equal,
         * <code>false</code> otherwise.
         * 
         * @param obj
         *            a <code>Server</code> object to compare with
         * @return <code>true</code> or <code>false</code>
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SList.Server)) {
                return false;
            }

            SList.Server srv = (SList.Server) obj;
            if (serverIP == null || srv.getIP() == null) {
                if (this.getName() == null || srv.getName() == null) {
                    return false;
                }
                return ProviderMgr.namesAreEqual(this.getName(), srv.getName())
                        && this.getPort() == srv.getPort();
            }
            return this.getIP().equals(srv.getIP())
                    && this.getPort() == srv.getPort();
        }
        
        /**
         * Returns the hash code of the receiver.
         */
        @Override
        public int hashCode() {
            return getIP().hashCode() + getPort();
        }

        /**
         * @return Returns the serverIP.
         */
        public String getIP() {
            return serverIP;
        }

        /**
         * @return Returns the serverName.
         */
        public String getName() {
            return serverName;
        }

        /**
         * @return Returns the serverPort.
         */
        public int getPort() {
            return serverPort;
        }

        /**
         * @param serverIP
         *            The serverIP to set.
         */
        public void setIP(String serverIP) {
            this.serverIP = serverIP;
        }

        /**
         * @param serverName
         *            The serverName to set.
         */
        public void setName(String serverName) {
            this.serverName = ProviderMgr.normalizeName(serverName);
        }

        /**
         * @param serverPort
         *            The serverPort to set.
         */
        public void setPort(int serverPort) {
            this.serverPort = serverPort;
        }

        @Override
        public String toString() {
            if (this.serverName != null) {
                return serverName + ":" + serverPort; //$NON-NLS-1$
            }
            return serverIP + ":" + serverPort; //$NON-NLS-1$
        }
    }

}
