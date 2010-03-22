/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/** 
 * @author Igor A. Pyankov 
 */ 

package org.apache.harmony.x.print.ipp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

/**
 * This class represents request encoding
 * described in RFC 2910 (http://ietf.org/rfc/rfc2910.txt?number=2910)
 */

/*
 *
 * <pre>
 * 
 *  
 *            An operation request or response is encoded as follows:
 *         
 *            -----------------------------------------------
 *            |                  version-number             |   2 bytes  - required
 *            -----------------------------------------------
 *            |               operation-id (request)        |
 *            |                      or                     |   2 bytes  - required
 *            |               status-code (response)        |
 *            -----------------------------------------------
 *            |                   request-id                |   4 bytes  - required
 *            -----------------------------------------------
 *            |                 attribute-group             |   n bytes - 0 or more
 *            -----------------------------------------------
 *            |              end-of-attributes-tag          |   1 byte   - required
 *            -----------------------------------------------
 *            |                     data                    |   q bytes  - optional
 *            -----------------------------------------------
 *   
 *  
 * </pre>
 */
public class IppRequest {
    static int requestcount;

    static protected void sortAttributes(Vector vaa) {
        IppAttributeGroup g1, g2;
        boolean the_end = false;

        while (!the_end) {
            the_end = true;
            for (int ii = vaa.size(), i = 0; i < (ii - 1); i++) {
                g1 = (IppAttributeGroup) vaa.get(i);
                g2 = (IppAttributeGroup) vaa.get(i + 1);
                if (g1.agroupid > g2.agroupid) {
                    the_end = false;
                    vaa.set(i, g2);
                    vaa.set(i + 1, g1);
                }
            }
        }
    }

    protected byte[] ippversion = { 1, 1 };
    protected short operationid;
    protected int requestid;
    protected IppAttributeGroupSet agroups;
    protected Object document;

    public IppRequest() {
        requestid = ++requestcount;
        agroups = new IppAttributeGroupSet();
    }

    public IppRequest(int version_major, int version_minor, short opid,
            String charset, String natural_language) {
        IppAttributeGroup agroup;

        requestid = ++requestcount;
        agroups = new IppAttributeGroupSet();

        this.setVersion(version_major, version_minor);
        this.setOperationId(opid);
        agroup = this.newGroup(IppAttributeGroup.TAG_OPERATION_ATTRIBUTES);
        agroup.add(new IppAttribute(IppAttribute.TAG_CHARSET,
                "attributes-charset", charset));
        agroup.add(new IppAttribute(IppAttribute.TAG_NATURAL_LANGUAGE,
                "attributes-natural-language", natural_language));
    }

    public IppRequest(byte[] request) throws Exception {
        ByteArrayInputStream ab = new ByteArrayInputStream(request);
        DataInputStream ba = new DataInputStream(ab);

        boolean the_end = false;
        byte tag;
        IppAttributeGroup agroup = null;
        IppAttribute attr;
        short name_length;
        String name = "";
        short value_length;
        Vector value = new Vector();

        agroups = new IppAttributeGroupSet();
        ba.read(ippversion, 0, 2);
        operationid = ba.readShort();
        requestid = ba.readInt();

        while (!the_end) {
            tag = ba.readByte();
            if (isAttributeGroupTag(tag)) {
                switch (tag) {
                case IppAttributeGroup.TAG_END_OF_ATTRIBUTES:
                    the_end = true;
                    continue;
                case IppAttributeGroup.TAG_OPERATION_ATTRIBUTES:
                case IppAttributeGroup.TAG_JOB_ATTRIBUTES:
                case IppAttributeGroup.TAG_PRINTER_ATTRIBUTES:
                case IppAttributeGroup.TAG_UNSUPPORTED_ATTRIBUTES:
                default:
                    agroup = newGroup(tag, -1);
                    break;
                }
            } else {
                name_length = ba.readShort();
                byte[] bname = new byte[name_length];
                if (name_length != 0) {
                    ba.read(bname, 0, name_length);
                    name = new String(bname);
                    value = new Vector();
                    attr = new IppAttribute(tag, name, value);
                    agroup.add(attr);
                }
                value_length = ba.readShort();

                switch (tag) {
                case IppAttribute.TAG_INTEGER:
                    value.add(new Integer(ba.readInt()));
                    break;
                case IppAttribute.TAG_BOOLEAN:
                    value.add(new Integer(ba.readByte()));
                    break;
                case IppAttribute.TAG_ENUM:
                    value.add(new Integer(ba.readInt()));
                    break;
                case IppAttribute.TAG_OCTETSTRINGUNSPECIFIEDFORMAT:
                case IppAttribute.TAG_DATETIME:
                case IppAttribute.TAG_RESOLUTION:
                case IppAttribute.TAG_RANGEOFINTEGER:
                case IppAttribute.TAG_TEXTWITHLANGUAGE:
                case IppAttribute.TAG_NAMEWITHLANGUAGE: {
                    byte[] b = new byte[value_length];
                    ba.read(b, 0, value_length);
                    value.add(b);
                }
                    break;
                case IppAttribute.TAG_TEXTWITHOUTLANGUAGE:
                case IppAttribute.TAG_NAMEWITHOUTLANGUAGE:
                case IppAttribute.TAG_KEYWORD:
                case IppAttribute.TAG_URI:
                case IppAttribute.TAG_URISCHEME:
                case IppAttribute.TAG_CHARSET:
                case IppAttribute.TAG_NATURAL_LANGUAGE:
                case IppAttribute.TAG_MIMEMEDIATYPE:
                case IppAttribute.TAG_UNSUPPORTED:
                case IppAttribute.TAG_UNKNOWN:
                case IppAttribute.TAG_NO_VALUE: {
                    byte[] b = new byte[value_length];
                    ba.read(b, 0, value_length);
                    value.add(b);
                }
                    break;
                default: {
                    byte[] b = new byte[value_length];
                    ba.read(b, 0, value_length);
                    value.add(b);
                }
                    break;
                }
            }
        }
    }

    public static boolean isAttributeGroupTag(byte tag) {
        return (tag < 0x10);
    }

    public void setVersion(int major, int minor) {
        ippversion[0] = (byte) major;
        ippversion[1] = (byte) minor;
    }

    public byte[] getVersion() {
        return ippversion;
    }

    public void setOperationId(short opid) {
        operationid = opid;
    }

    public short getOperationId() {
        return operationid;
    }

    public short getStatusCode() {
        return getOperationId();
    }

    public void setRequestId(int rid) {
        requestid = rid;
    }

    public int getRequestId() {
        return requestid;
    }

    public IppAttributeGroupSet getAgroups() {
        return agroups;
    }

    public IppAttributeGroup newGroup(int agid) {
        Vector v = (Vector) agroups.get(new Integer(agid));
        IppAttributeGroup g = new IppAttributeGroup(agid);

        if (v == null) {
            v = new Vector();
            agroups.put(new Integer(agid), v);
            v.add(g);
        } else {
            v.set(0, g);
        }

        return g;
    }

    public IppAttributeGroup newGroup(int agid, int index) {
        Vector v = (Vector) agroups.get(new Integer(agid));
        IppAttributeGroup g = new IppAttributeGroup(agid);

        if (v == null) {
            v = new Vector();
            agroups.put(new Integer(agid), v);
            v.add(g);
        } else {
            if (index < 0) {
                v.add(g);
            } else {
                v.set(index, g);
            }
        }

        return g;
    }

    public Vector newGroupVector(int agid, Vector v) {
        agroups.put(new Integer(agid), v);

        return getGroupVector(agid);
    }

    public IppAttributeGroup getGroup(int agid) {
        Vector v = (Vector) agroups.get(new Integer(agid));
        if (v != null) {
            return (IppAttributeGroup) v.get(0);
        }
        return null;
    }

    public IppAttributeGroup getGroup(int agid, int gid) {
        Vector v = (Vector) agroups.get(new Integer(agid));
        if (v != null) {
            return (IppAttributeGroup) v.get(gid);
        }
        return null;
    }

    public Vector getGroupVector(int agid) {
        return (Vector) agroups.get(new Integer(agid));
    }

    public void setDocument(Object data) throws IppException {
        if (data instanceof InputStream || data instanceof byte[]
                || data instanceof char[] || data instanceof String
                || data instanceof Reader || data instanceof URL) {
            this.document = data;
        } else {
            throw new IppException("Wrong type for IPP document");
        }
    }

    public Object getDocument() {
        return document;
    }

    public byte[] getBytes() throws Exception {
        ByteArrayOutputStream ab = new ByteArrayOutputStream();
        DataOutputStream ba;
        Iterator ag = agroups.values().iterator();
        IppAttributeGroup aa;
        byte[] b;
        Vector vaa = new Vector();

        for (; ag.hasNext();) {
            vaa.addAll((Vector) ag.next());
        }
        sortAttributes(vaa);

        ba = new DataOutputStream(ab);

        ba.write(ippversion);
        ba.writeShort(operationid);
        ba.writeInt(requestid);
        for (int ii = vaa.size(), i = 0; i < ii; i++) {
            aa = (IppAttributeGroup) vaa.get(i);
            b = aa.getBytes();
            ba.write(b);
        }
        ba.write(IppAttributeGroup.TAG_END_OF_ATTRIBUTES);
        ba.flush();
        ba.close();

        return ab.toByteArray();
    }

    public String toString() {
        ByteArrayOutputStream ab = new ByteArrayOutputStream();
        DataOutputStream ba;
        Iterator ag = agroups.values().iterator();
        IppAttributeGroup aa;
        String s;
        Vector vaa = new Vector();

        try {
            for (; ag.hasNext();) {
                vaa.addAll((Vector) ag.next());
            }

            ba = new DataOutputStream(ab);

            ba.writeBytes("IPP version: " + ippversion[0] + "." + ippversion[1]
                    + "\n");
            ba.writeBytes("Operation id/Status code: 0x"
                    + Integer.toHexString(operationid) + "\n");
            ba.writeBytes("Request id: 0x" + Integer.toHexString(requestid)
                    + "\n");

            for (int ii = vaa.size(), i = 0; i < ii; i++) {
                aa = (IppAttributeGroup) vaa.get(i);
                s = aa.toString();
                ba.writeBytes(s + "\n");
            }
            ba.flush();
            ba.close();
        } catch (IOException e) {
            // IGNORE exception
            e.printStackTrace();
        }

        return ab.toString();
    }

}
