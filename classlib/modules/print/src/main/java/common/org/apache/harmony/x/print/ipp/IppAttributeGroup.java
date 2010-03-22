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
package org.apache.harmony.x.print.ipp;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/*
 * class IppAttributeGroup stores IPP attributes group
 */
public class IppAttributeGroup extends Vector {
    
    private static final long serialVersionUID = -2197671478629444252L;
    
    /* According to RFC2910 (http://ietf.org/rfc/rfc2910.txt?number=2910):
     *
     * Each "attribute-group" field MUST be encoded with the "begin-
     * attribute-group-tag" field followed by zero or more "attribute" sub-
     * fields.
     * 
     * The table below maps the model document group name to value of the
     * "begin-attribute-group-tag" field:
     * 
     * Model Document Group "begin-attribute-group-tag" field values
     * 
     * Operation Attributes "operations-attributes-tag" 
     * Job Template Attributes "job-attributes-tag" 
     * Job Object Attributes "job-attributes-tag" 
     * Requested Attributes "job-attributes-tag" (Get-Job-Attributes) 
     * Requested Attributes "printer-attributes-tag" (Get-Printer-Attributes) 
     * Unsupported Attributes * "unsupported-attributes-tag" 
     * Document Content in a special position as described above
     * 
     * 0x00 reserved for definition in a future IETF standards track document
     * 0x01 "operation-attributes-tag" 
     * 0x02 "job-attributes-tag" 
     * 0x03 "end-of-attributes-tag" 
     * 0x04 "printer-attributes-tag" 
     * 0x05 "unsupported-attributes-tag" 
     * 0x06-0x0f reserved for future delimiters in
     * IETF standards track documents
     */
    public static final byte TAG_RESERVED = 0x00;
    public static final byte TAG_OPERATION_ATTRIBUTES = 0x01;
    public static final byte TAG_JOB_ATTRIBUTES = 0x02;
    public static final byte TAG_JOB_TEMPLATE_ATTRIBUTES = TAG_JOB_ATTRIBUTES;
    public static final byte TAG_JOB_OBJECT_ATTRIBUTES = TAG_JOB_ATTRIBUTES;
    public static final byte TAG_GET_JOB_ATTRIBUTES = TAG_JOB_ATTRIBUTES;
    public static final byte TAG_END_OF_ATTRIBUTES = 0x03;
    public static final byte TAG_PRINTER_ATTRIBUTES = 0x04;
    public static final byte TAG_GET_PRINTER_ATTRIBUTES = TAG_PRINTER_ATTRIBUTES;
    public static final byte TAG_UNSUPPORTED_ATTRIBUTES = 0x05;

    /*
     * @return Returns the name of the group.
     */
    public static String getGname(int agid) {
        switch (agid) {
        case TAG_RESERVED:
            return IppResources.getString("IppAttributesGroup.RESERVED");
        case TAG_OPERATION_ATTRIBUTES:
            return IppResources.getString("IppAttributesGroup.OPERATION_ATTRIBUTES");
        case TAG_JOB_ATTRIBUTES:
            return IppResources.getString("IppAttributesGroup.JOB_TEMPLATE_ATTRIBUTES");
        case TAG_PRINTER_ATTRIBUTES:
            return IppResources.getString("IppAttributesGroup.GET_PRINTER_ATTRIBUTES");
        case TAG_END_OF_ATTRIBUTES:
            return IppResources.getString("IppAttributesGroup.END_OF_ATTRIBUTES");
        case TAG_UNSUPPORTED_ATTRIBUTES:
            return IppResources.getString("IppAttributesGroup.UNSUPPORTED_ATTRIBUTES");
        }
        return "UNKNOWN_ATTRIBUTES";
    }

    protected int agroupid;

    public IppAttributeGroup(int agid) {
        super();
        this.agroupid = agid;
    }

    public IppAttributeGroup(Integer agid) {
        super();
        this.agroupid = agid.byteValue();
    }

    /*
     * change/add attributes in this group by attributes from parameter
     */
    public int set(IppAttributeGroup ag) {
        if (ag != null && agroupid == ag.agroupid && ag.size() > 0) {
            int i1;
            IppAttribute a;

            for (int ii = ag.size(), i = 0; i < ii; i++) {
                a = (IppAttribute) ag.get(i);
                i1 = findAttribute(new String(a.aname));
                if (i1 >= 0) {
                    set(i1, a);
                } else {
                    add(a);
                }
            }
        }

        return size();
    }

    public int findAttribute(String name) {
        for (int ii = size(), i = 0; i < ii; i++) {
            if (name.equals(new String(((IppAttribute) get(i)).aname))) {
                return i;
            }
        }

        return -1;
    }

    /*
     * return array of byte presentation of group
     */
    public byte[] getBytes() {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        IppAttribute a;
        byte[] b;

        ba.write((byte) agroupid);
        for (int ii = size(), i = 0; i < ii; i++) {
            a = (IppAttribute) get(i);
            b = a.getBytes();
            ba.write(b, 0, b.length);
        }
        return ba.toByteArray();
    }

    /*
     * human readable form of group
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        ByteArrayOutputStream ab = new ByteArrayOutputStream();
        DataOutputStream ba = new DataOutputStream(ab);
        IppAttribute a;
        String s;

        try {
            ba.writeBytes("---- Attributes group: 0x"
                    + Integer.toHexString(agroupid) + "(" + getGname(agroupid)
                    + ")" + "\n");
            for (int ii = size(), i = 0; i < ii; i++) {
                a = (IppAttribute) get(i);
                s = a.toString();
                ba.writeBytes(s + "\n");
            }
        } catch (IOException e) {
            // IGNORE exception
            e.printStackTrace();
        }

        return ab.toString();
    }
}
