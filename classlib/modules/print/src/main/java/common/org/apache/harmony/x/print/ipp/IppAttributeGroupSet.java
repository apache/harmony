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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/*
 * Set of IppAttributeGroup
 */
public class IppAttributeGroupSet extends Hashtable {
    
    private static final long serialVersionUID = -1273600082773438491L;

    static protected void sortAttributes(Vector va) {
        Object v1, v2;
        IppAttributeGroup g1, g2;
        boolean the_end = false;

        while (!the_end) {
            the_end = true;
            for (int ii = va.size(), i = 0; i < (ii - 1); i++) {
                v1 = va.get(i);
                v2 = va.get(i + 1);
                if (v1 instanceof Vector) {
                    g1 = (IppAttributeGroup) ((Vector) v1).get(0);
                } else {
                    g1 = (IppAttributeGroup) v1;
                }
                if (v2 instanceof Vector) {
                    g2 = (IppAttributeGroup) ((Vector) v2).get(0);
                } else {
                    g2 = (IppAttributeGroup) v2;
                }
                if (g1 != null && g2 != null && g1.agroupid > g2.agroupid) {
                    the_end = false;
                    va.set(i, v2);
                    va.set(i + 1, v1);
                }
            }
        }
    }

    public boolean setAttribute(String aname, Object avalue) {
        Integer gtag;
        byte vtag;
        IppAttributeGroup group;

        if (avalue == null || aname == null || aname.equals("")) {
            return false;
        }

        gtag = new Integer(IppDefs.getAttributeGtag(aname));
        vtag = IppDefs.getAttributeVtag(aname);
        if (avalue instanceof IppAttribute) {
            if (((IppAttribute) avalue).getTag() != vtag
                    || !aname.equals(new String(
                            ((IppAttribute) avalue).getName()))) {
                return false;
            }
        }

        group = (IppAttributeGroup) get(gtag);
        if (group == null) {
            put(gtag, new IppAttributeGroup(gtag));
            group = (IppAttributeGroup) get(gtag);
        }
        if (group == null) {
            return false;
        }

        int aindex = -1;

        if (avalue instanceof IppAttribute) {
            aname = new String(((IppAttribute) avalue).getName());
            aindex = group.findAttribute(aname);
            if (aindex >= 0) {
                group.set(aindex, avalue);
            } else {
                group.add(avalue);
            }
            return true;
        }

        switch (vtag) {
        case IppAttribute.TAG_INTEGER:
        case IppAttribute.TAG_BOOLEAN:
        case IppAttribute.TAG_ENUM:
            avalue = new IppAttribute(vtag, aname,
                    ((Integer) avalue).intValue());
            aindex = group.findAttribute(aname);
            if (aindex >= 0) {
                group.set(aindex, avalue);
            } else {
                group.add(avalue);
            }
            return true;
        case IppAttribute.TAG_OCTETSTRINGUNSPECIFIEDFORMAT:
        case IppAttribute.TAG_DATETIME:
        case IppAttribute.TAG_RESOLUTION:
        case IppAttribute.TAG_RANGEOFINTEGER:
        case IppAttribute.TAG_TEXTWITHLANGUAGE:
        case IppAttribute.TAG_NAMEWITHLANGUAGE:
            avalue = new IppAttribute(vtag, aname, (byte[]) avalue);
            aindex = group.findAttribute(aname);
            if (aindex >= 0) {
                group.set(aindex, avalue);
            } else {
                group.add(avalue);
            }
            return true;
        case IppAttribute.TAG_TEXTWITHOUTLANGUAGE:
        case IppAttribute.TAG_NAMEWITHOUTLANGUAGE:
        case IppAttribute.TAG_KEYWORD:
        case IppAttribute.TAG_URI:
        case IppAttribute.TAG_URISCHEME:
        case IppAttribute.TAG_CHARSET:
        case IppAttribute.TAG_NATURAL_LANGUAGE:
        case IppAttribute.TAG_MIMEMEDIATYPE:
        case IppAttribute.TAG_UNSUPPORTED:
            avalue = new IppAttribute(vtag, aname, (String) avalue);
            aindex = group.findAttribute(aname);
            if (aindex >= 0) {
                group.set(aindex, avalue);
            } else {
                group.add(avalue);
            }
            return true;
        case IppAttribute.TAG_UNKNOWN:
        case IppAttribute.TAG_NO_VALUE:
            break;
        default:
            break;
        }

        return false;
    }

    public byte[] getBytes() throws IppException{
        ByteArrayOutputStream ab = new ByteArrayOutputStream();
        DataOutputStream ba;
        Iterator ai = values().iterator();
        Object v;
        IppAttributeGroup ag;
        byte[] b;
        Vector av = new Vector();

        for (; ai.hasNext();) {
            av.add(ai.next());
        }
        sortAttributes(av);

        ba = new DataOutputStream(ab);

        try {
            for (int ii = av.size(), i = 0; i < ii; i++) {
                v = av.get(i);
                if (v != null && v instanceof Vector) {
                    for (int j = 0, jj = ((Vector) v).size(); j < jj; j++) {
                        ag = (IppAttributeGroup) ((Vector) v).get(j);
                        b = ag.getBytes();
                        ba.write(b);
                    }
                } else {
                    ag = (IppAttributeGroup) v;
                    b = ag.getBytes();
                    ba.write(b);
                }
            }
            ba.write(IppAttributeGroup.TAG_END_OF_ATTRIBUTES);
            ba.flush();
            ba.close();
        } catch (IOException e) {
            // e.printStackTrace();
            throw new IppException(e.getMessage());
        }

        return ab.toByteArray();

    }
}
