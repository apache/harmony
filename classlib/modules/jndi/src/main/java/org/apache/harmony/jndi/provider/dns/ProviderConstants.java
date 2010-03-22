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

import javax.naming.InvalidNameException;

/**
 * Contains various constants that are used in the provider classes.
 *
 * @see RFC 1035
 */
public class ProviderConstants {

    // Constants that should be used as possible values for the header QR field 
    public static final boolean QR_QUERY = false;
    public static final boolean QR_RESPONSE = true;
    
    // Message header masks
    public static final int QR_MASK = 0x8000;
    public static final int OPCODE_MASK = 0x7800;
    public static final int AA_MASK = 0x400;
    public static final int TC_MASK = 0x200;
    public static final int RD_MASK = 0x100;
    public static final int RA_MASK = 0x80;
    public static final int Z_MASK = 0x70;
    public static final int RCODE_MASK = 0xf;

    // Message header fields shifts
    public static final int OPCODE_SHIFT = 11;
    public static final int RCODE_SHIFT = 0;
    
    // Constants that can be used as OPCODEs in the header
    public static final int QUERY = 0;
    public static final int IQUERY = 1;
    public static final int STATUS = 2;

    // Constants that can be used as RCODEs in the header
    public static final int NO_ERROR = 0;
    public static final int FORMAT_ERROR = 1;
    public static final int SERVER_FAILURE = 2;
    public static final int NAME_ERROR = 3;
    public static final int NOT_IMPLEMENTED = 4;
    public static final int REFUSED = 5;

    // Possible TYPE values
    public static final int A_TYPE = 1;
    public static final int NS_TYPE = 2;
    public static final int MD_TYPE = 3;
    public static final int MF_TYPE = 4;
    public static final int CNAME_TYPE = 5;
    public static final int SOA_TYPE = 6;
    public static final int MB_TYPE = 7;
    public static final int MG_TYPE = 8;
    public static final int MR_TYPE = 9;
    public static final int NULL_TYPE = 10;
    public static final int WKS_TYPE = 11;
    public static final int PTR_TYPE = 12;
    public static final int HINFO_TYPE = 13;
    public static final int MINFO_TYPE = 14;
    public static final int MX_TYPE = 15;
    public static final int TXT_TYPE = 16;
    public static final int AAAA_TYPE = 28;
    public static final int SRV_TYPE = 33;
    
    // Possible QTYPE values
    public static final int AXFR_QTYPE = 252;    
    public static final int MAILB_QTYPE = 253;    
    public static final int MAILA_QTYPE = 254;    
    public static final int ANY_QTYPE = 255;   
    
    public static String[] rrTypeNames;
    public static String[] rrClassNames;
    
    // Possible CLASS values
    public static final int IN_CLASS = 1;
    public static final int CS_CLASS = 2;
    public static final int CH_CLASS = 3;
    public static final int HS_CLASS = 4;

    // Possible QCLASS values
    public static final int ANY_QCLASS = 255;

    // Maximum lengths
    public static final int LABEL_MAX_CHARS = 63;
    public static final int NAME_MAX_CHARS = 255;

    // default DNS port
    public static final int DEFAULT_DNS_PORT = 53;

    // Resolver settings
    public static final int DEFAULT_INITIAL_TIMEOUT = 1000;
    public static final int DEFAULT_TIMEOUT_RETRIES = 4;
    public static final boolean DEFAULT_AUTHORITATIVE = false;
    public static final boolean DEFAULT_RECURSION = true;
    public static final int DEFAULT_LOOKUP_ATTR_TYPE = TXT_TYPE;
    public static final int DEFAULT_LOOKUP_ATTR_CLASS = IN_CLASS;
    public static final int DEFAULT_MAX_THREADS = 7;
    
    public static final DNSName ROOT_ZONE_NAME_OBJ;
    
    // public static final String LOGGER_NAME =
    //        "org.apache.harmony.jndi.provider.dns";

    
    static {
        // Resource Record types
        // commented out types are not supported
        rrTypeNames = new String[256];
        for (int i = 0; i < 256; i++) {
            rrTypeNames[i] = String.valueOf(i);
        }
        rrTypeNames[A_TYPE] = "A"; //$NON-NLS-1$
        rrTypeNames[NS_TYPE] = "NS"; //$NON-NLS-1$
        //rrTypeNames[MD_TYPE] = "MD";
        //rrTypeNames[MF_TYPE] = "MF";
        rrTypeNames[CNAME_TYPE] = "CNAME"; //$NON-NLS-1$
        rrTypeNames[SOA_TYPE] = "SOA"; //$NON-NLS-1$
        //rrTypeNames[MB_TYPE] = "MB";
        //rrTypeNames[MG_TYPE] = "MG";
        //rrTypeNames[MR_TYPE] = "MR";
        //rrTypeNames[NULL_TYPE] = "NULL";
        //rrTypeNames[WKS_TYPE] = "WKS";
        rrTypeNames[PTR_TYPE] = "PTR"; //$NON-NLS-1$
        rrTypeNames[HINFO_TYPE] = "HINFO"; //$NON-NLS-1$
        //rrTypeNames[MINFO_TYPE] = "MINFO";
        rrTypeNames[MX_TYPE] = "MX"; //$NON-NLS-1$
        rrTypeNames[TXT_TYPE] = "TXT"; //$NON-NLS-1$
        rrTypeNames[AAAA_TYPE] = "AAAA"; //$NON-NLS-1$
        rrTypeNames[SRV_TYPE] = "SRV"; //$NON-NLS-1$
        //rrTypeNames[AXFR_QTYPE] = "AXFR";
        //rrTypeNames[MAILB_QTYPE] = "MAILB";
        //rrTypeNames[MAILA_QTYPE] = "MAILA";
        rrTypeNames[ANY_QTYPE] = "*"; //$NON-NLS-1$

        // Resource Record classes
        rrClassNames = new String[256];
        for (int i = 0; i < 256; i++) {
            rrClassNames[i] = String.valueOf(i);
        }
        rrClassNames[IN_CLASS] = "IN"; //$NON-NLS-1$
        rrClassNames[HS_CLASS] = "HS"; //$NON-NLS-1$
        rrClassNames[ANY_QCLASS] = "*"; //$NON-NLS-1$

        // Root zone name
        DNSName root = null;
        try {
            root = (DNSName) ((new DNSNameParser()).parse(".")); //$NON-NLS-1$
        } catch (InvalidNameException e) {
            // ignore
        }
        ROOT_ZONE_NAME_OBJ = root;
    }
}
