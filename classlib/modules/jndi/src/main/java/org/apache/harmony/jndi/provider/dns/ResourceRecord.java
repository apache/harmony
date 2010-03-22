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

import java.util.StringTokenizer;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * Represents domain protocol Resource Record
 * 
 * @see RFC 1035
 */
public class ResourceRecord {

    /** a domain name */
    private String name;

    /** resource record type */
    private int rrType;

    /** resource record class */
    private int rrClass;

    /** time to live */
    private long ttl;

    /** resource data length */
    // private int rdLength;
    /** resource data itself */
    private Object rData;

    /** empty constructor */
    public ResourceRecord() {
    }

    /**
     * Constructs new ResourceRecord object from given values.
     * 
     * @param name
     *            a domain name
     * @param rrType
     *            resource record type
     * @param rrClass
     *            resource record class
     * @param ttl
     *            time to live
     * @param rdLength
     *            resource data length
     * @param rData
     *            resource data itself
     */
    public ResourceRecord(String name, int rrType, int rrClass, long ttl,
    /* int rdLength, */
    Object rData) {
        this.name = name;
        this.rrType = rrType;
        this.rrClass = rrClass;
        this.ttl = ttl;
        // this.rdLength = rdLength;
        this.rData = rData;
    }

    /**
     * Creates the sequence of bytes that represents the current resource
     * record.
     * 
     * @param buffer
     *            the buffer to write the bytes into
     * @param startIdx
     *            starting index
     * @return updated index
     * @throws DomainProtocolException
     *             if something went wrong
     * @throws ArrayIndexOutOfBoundsException
     *             if the buffer border unpredictably encountered
     */
    public int writeBytes(byte[] buffer, int startIdx)
            throws DomainProtocolException {
        int idx = startIdx;

        // basic checking
        if (buffer == null) {
            // jndi.32=buffer is null
            throw new DomainProtocolException(Messages.getString("jndi.32")); //$NON-NLS-1$
        }
        // NAME
        idx = ProviderMgr.writeName(name, buffer, idx);
        // TYPE
        idx = ProviderMgr.write16Int(rrType, buffer, idx);
        // CLASS
        idx = ProviderMgr.write16Int(rrClass, buffer, idx);
        // TTL
        idx = ProviderMgr.write32Int(ttl, buffer, idx);
        // RDLENGTH & RDATA
        if (rrType == ProviderConstants.NS_TYPE
                || rrType == ProviderConstants.CNAME_TYPE
                || rrType == ProviderConstants.PTR_TYPE) {
            int idx0 = idx;

            idx += 2;
            // RDATA
            idx = ProviderMgr.writeName((String) rData, buffer, idx);
            // RDLENGTH
            ProviderMgr.write16Int(idx - 2 - idx0, buffer, idx0);
        } else if (rrType == ProviderConstants.A_TYPE) {
            byte[] ipBytes = ProviderMgr.parseIpStr((String) rData);

            // RDLENGTH
            idx = ProviderMgr.write16Int(ipBytes.length, buffer, idx);
            for (byte element : ipBytes) {
                buffer[idx++] = element;
            }
        } else if (rrType == ProviderConstants.SOA_TYPE) {
            StringTokenizer st = new StringTokenizer((String) rData, " "); //$NON-NLS-1$
            String token;
            int idx0 = idx; // saving RDLENGTH position

            if (st.countTokens() != 7) {
                // jndi.35=Invalid number of fields while parsing SOA record
                throw new DomainProtocolException(Messages.getString("jndi.35")); //$NON-NLS-1$
            }
            idx += 2; // skip RDLENGTH for now
            // RDATA
            // MNAME
            token = st.nextToken();
            idx = ProviderMgr.writeName(token, buffer, idx);
            // RNAME
            token = st.nextToken();
            idx = ProviderMgr.writeName(token, buffer, idx);
            // SERIAL
            // REFRESH
            // RETRY
            // EXPIRE
            // MINIMUM
            try {
                for (int i = 0; i < 5; i++) {
                    token = st.nextToken();
                    idx = ProviderMgr.write32Int(Long.parseLong(token), buffer,
                            idx);
                }
            } catch (NumberFormatException e) {
                // jndi.36=Error while parsing SOA record
                throw new DomainProtocolException(
                        Messages.getString("jndi.36"), e); //$NON-NLS-1$
            }
            // RDLENGTH
            ProviderMgr.write16Int(idx - 2 - idx0, buffer, idx0);
        } else if (rrType == ProviderConstants.MX_TYPE) {
            StringTokenizer st = new StringTokenizer((String) rData, " "); //$NON-NLS-1$
            String token;
            int idx0 = idx; // saving RDLENGTH position

            if (st.countTokens() != 2) {
                // jndi.37=Invalid number of fields while parsing MX record
                throw new DomainProtocolException(Messages.getString("jndi.37")); //$NON-NLS-1$
            }
            idx += 2; // skip RDLENGTH for now
            // PREFERENCE
            token = st.nextToken();
            try {
                ProviderMgr.write16Int(Integer.parseInt(token), buffer, idx);
            } catch (NumberFormatException e) {
                // jndi.38=Error while parsing MX record
                throw new DomainProtocolException(
                        Messages.getString("jndi.38"), e); //$NON-NLS-1$
            }
            // EXCHANGE
            token = st.nextToken();
            idx = ProviderMgr.writeName(token, buffer, idx);
            // RDLENGTH
            ProviderMgr.write16Int(idx - 2 - idx0, buffer, idx0);
        } else if (rrType == ProviderConstants.HINFO_TYPE) {
            StringTokenizer st = new StringTokenizer((String) rData, " "); //$NON-NLS-1$
            String token;
            int idx0 = idx; // saving RDLENGTH position

            if (st.countTokens() != 2) {
                // jndi.39=Invalid number of fields while parsing HINFO record
                throw new DomainProtocolException(Messages.getString("jndi.39")); //$NON-NLS-1$
            }
            idx += 2; // skip RDLENGTH for now
            // CPU
            // OS
            for (int i = 0; i < 2; i++) {
                token = st.nextToken();
                idx = ProviderMgr.writeCharString(token, buffer, idx);
            }
            // RDLENGTH
            ProviderMgr.write16Int(idx - 2 - idx0, buffer, idx0);
        } else if (rrType == ProviderConstants.TXT_TYPE) {
            // character string with preceding length octet
            int idx0 = idx;
            StringTokenizer st = new StringTokenizer((String) rData, " "); //$NON-NLS-1$

            idx += 2;
            // RDATA
            while (st.hasMoreTokens()) {
                String token = st.nextToken();

                if (token.getBytes().length > 255) {
                    // jndi.3A=The length of character string exceed 255 octets
                    throw new DomainProtocolException(Messages
                            .getString("jndi.3A")); //$NON-NLS-1$
                }
                idx = ProviderMgr.writeCharString(token, buffer, idx);
            }
            if (idx - 2 - idx0 > 65535) {
                // jndi.3B=Length of TXT field exceed 65535
                throw new DomainProtocolException(Messages.getString("jndi.3B")); //$NON-NLS-1$
            }
            // RDLENGTH
            ProviderMgr.write16Int(idx - 2 - idx0, buffer, idx0);
        } else if (rrType == ProviderConstants.SRV_TYPE) {
            StringTokenizer st = new StringTokenizer((String) rData, " "); //$NON-NLS-1$
            String token;
            int idx0 = idx; // saving RDLENGTH position

            idx += 2;
            if (st.countTokens() != 4) {
                // jndi.3C=Invalid number of fields while parsing SRV record
                throw new DomainProtocolException(Messages.getString("jndi.3C")); //$NON-NLS-1$
            }
            // RDATA

            // PRIORITY
            // WEIGHT
            // PORT
            try {
                for (int i = 0; i < 3; i++) {
                    token = st.nextToken();
                    idx = ProviderMgr.write16Int(Integer.parseInt(token),
                            buffer, idx);
                }
            } catch (NumberFormatException e) {
                // jndi.3D=Error while parsing SRV record
                throw new DomainProtocolException(
                        Messages.getString("jndi.3D"), e); //$NON-NLS-1$
            }
            // TARGET
            token = st.nextToken();
            idx = ProviderMgr.writeName(token, buffer, idx);
            // RDLENGTH
            ProviderMgr.write16Int(idx - 2 - idx0, buffer, idx0);
        }
        // TODO add more Resource Record types here
        else {
            byte[] bytes;

            if (!(rData instanceof byte[])) {
                // jndi.3E=RDATA for unknown record type {0} should have value
                // of byte[] type
                throw new DomainProtocolException(Messages.getString(
                        "jndi.3E", rrType)); //$NON-NLS-1$
            }
            bytes = (byte[]) rData;
            // RDLENGTH
            idx = ProviderMgr.write16Int(bytes.length, buffer, idx);
            for (byte element : bytes) {
                buffer[idx++] = element;
            }
        }
        return idx;
    }

    /**
     * Parses given sequence of bytes and constructs a resource record from it.
     * 
     * @param mesBytes
     *            the byte array that should be parsed
     * @param startIdx
     *            an index of <code>mesBytes</code> array to start the parsing
     *            at
     * @param resultRR
     *            an object the result of the operation will be stored into
     * @return updated index of <code>mesBytes</code> array
     * @throws DomainProtocolException
     *             if something went wrong
     * @throws ArrayIndexOutOfBoundsException
     *             if the array border unpredictably encountered
     */
    public static int parseRecord(byte[] mesBytes, int startIdx,
            ResourceRecord resultRR) throws DomainProtocolException {
        int idx = startIdx;
        StringBuffer nameSB = new StringBuffer();
        int rrType;
        int rdLen;
        Object rDat = null;

        if (resultRR == null) {
            // jndi.3F=Given resultRR is null
            throw new NullPointerException(Messages.getString("jndi.3F")); //$NON-NLS-1$
        }
        // NAME
        idx = ProviderMgr.parseName(mesBytes, idx, nameSB);
        resultRR.setName(ProviderMgr.normalizeName(nameSB.toString()));
        // TYPE
        rrType = ProviderMgr.parse16Int(mesBytes, idx);
        resultRR.setRRType(rrType);
        idx += 2;
        // CLASS
        resultRR.setRRClass(ProviderMgr.parse16Int(mesBytes, idx));
        idx += 2;
        // TTL
        resultRR.setTtl(ProviderMgr.parse32Int(mesBytes, idx));
        idx += 4;
        // RDLENGTH
        rdLen = ProviderMgr.parse16Int(mesBytes, idx);
        idx += 2;
        // RDATA
        if (rrType == ProviderConstants.NS_TYPE
                || rrType == ProviderConstants.CNAME_TYPE
                || rrType == ProviderConstants.PTR_TYPE) {
            // let's parse the domain name
            StringBuffer name = new StringBuffer();

            idx = ProviderMgr.parseName(mesBytes, idx, name);
            rDat = ProviderMgr.normalizeName(name.toString());
        } else if (rrType == ProviderConstants.A_TYPE) {
            // let's parse the 32 bit Internet address
            byte tmpArr[] = new byte[4];

            for (int i = 0; i < 4; i++) {
                tmpArr[i] = mesBytes[idx + i];
            }
            rDat = ProviderMgr.getIpStr(tmpArr);
            idx += 4;
        } else if (rrType == ProviderConstants.MX_TYPE) {
            // 16 bit integer (preference) followed by domain name
            int preference;
            StringBuffer name = new StringBuffer();

            preference = ProviderMgr.parse16Int(mesBytes, idx);
            idx += 2;
            idx = ProviderMgr.parseName(mesBytes, idx, name);
            rDat = "" + preference + " " + //$NON-NLS-1$ //$NON-NLS-2$
                    ProviderMgr.normalizeName(name.toString());
        } else if (rrType == ProviderConstants.SOA_TYPE) {
            StringBuffer mName = new StringBuffer();
            StringBuffer rName = new StringBuffer();
            long serial;
            long refresh;
            long retry;
            long expire;
            long minimum;

            idx = ProviderMgr.parseName(mesBytes, idx, mName);
            idx = ProviderMgr.parseName(mesBytes, idx, rName);
            serial = ProviderMgr.parse32Int(mesBytes, idx);
            idx += 4;
            refresh = ProviderMgr.parse32Int(mesBytes, idx);
            idx += 4;
            retry = ProviderMgr.parse32Int(mesBytes, idx);
            idx += 4;
            expire = ProviderMgr.parse32Int(mesBytes, idx);
            idx += 4;
            minimum = ProviderMgr.parse32Int(mesBytes, idx);
            idx += 4;
            rDat = ProviderMgr.normalizeName(mName.toString()) + " " + //$NON-NLS-1$
                    ProviderMgr.normalizeName(rName.toString()) + " " + //$NON-NLS-1$
                    serial + " " + refresh + " " + retry + " " + expire + " " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                    minimum;
        } else if (rrType == ProviderConstants.TXT_TYPE) {
            StringBuilder sbuf = new StringBuilder();
            int idx0 = idx;

            while (true) {
                int len11 = ProviderMgr.parse8Int(mesBytes, idx++);

                if (idx - idx0 + len11 > rdLen) {
                    idx--;
                    break;
                }
                if (sbuf.length() > 0) {
                    sbuf.append(' ');
                }
                sbuf.append(new String(mesBytes, idx, len11));
                idx += len11;
            }
            rDat = sbuf.toString();
        } else if (rrType == ProviderConstants.HINFO_TYPE) {
            // two character strings with preceding length octets
            StringBuffer res = new StringBuffer();

            idx = ProviderMgr.parseCharString(mesBytes, idx, res);
            res.append(" "); //$NON-NLS-1$
            idx = ProviderMgr.parseCharString(mesBytes, idx, res);
            rDat = res.toString();
        } else if (rrType == ProviderConstants.SRV_TYPE) {
            int priority;
            int weight;
            int port;
            StringBuffer name = new StringBuffer();

            priority = ProviderMgr.parse16Int(mesBytes, idx);
            idx += 2;
            weight = ProviderMgr.parse16Int(mesBytes, idx);
            idx += 2;
            port = ProviderMgr.parse16Int(mesBytes, idx);
            idx += 2;
            idx = ProviderMgr.parseName(mesBytes, idx, name);
            rDat = "" + priority + " " + weight + " " + port + " " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                    ProviderMgr.normalizeName(name.toString());
        }
        // TODO add more Resource Record types here
        else {
            // copy bytes since the retrieved bytes
            // could contain unknown binary data
            rDat = new byte[rdLen];
            for (int i = 0; i < rdLen; i++) {
                ((byte[]) rDat)[i] = mesBytes[idx++];
            }
        }
        resultRR.setRData(rDat);
        return idx;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append(" "); //$NON-NLS-1$
        sb.append(ProviderConstants.rrTypeNames[rrType]);
        sb.append(" "); //$NON-NLS-1$
        sb.append(rrClass);
        sb.append(" "); //$NON-NLS-1$
        sb.append("TTL=" + ttl); //$NON-NLS-1$
        sb.append(" "); //$NON-NLS-1$
        sb.append(rData.toString());
        return sb.toString();
    }

    // getters and setters

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the rData.
     */
    public Object getRData() {
        return rData;
    }

    /**
     * @param data
     *            The rData to set.
     */
    public void setRData(Object data) {
        rData = data;
    }

    /**
     * @return Returns the rdLength.
     */
    // public int getRDLength() {
    // return rdLength;
    // }
    /**
     * @param rdLength
     *            The rdLength to set.
     */
    // public void setRDLength(int rdLength) {
    // this.rdLength = rdLength;
    // }
    /**
     * @return Returns the rrClass.
     */
    public int getRRClass() {
        return rrClass;
    }

    /**
     * @param rrClass
     *            The rrClass to set.
     */
    public void setRRClass(int rrClass) {
        this.rrClass = rrClass;
    }

    /**
     * @return Returns the rrType.
     */
    public int getRRType() {
        return rrType;
    }

    /**
     * @param rrType
     *            The rrType to set.
     */
    public void setRRType(int rrType) {
        this.rrType = rrType;
    }

    /**
     * @return Returns the TTL.
     */
    public long getTtl() {
        return ttl;
    }

    /**
     * @param ttl
     *            The TTL to set.
     */
    public void setTtl(long ttl) {
        this.ttl = ttl;
    }
}
