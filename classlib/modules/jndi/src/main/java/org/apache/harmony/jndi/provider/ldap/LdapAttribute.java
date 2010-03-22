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

package org.apache.harmony.jndi.provider.ldap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;

import org.apache.harmony.jndi.internal.nls.Messages;
import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Decodable;
import org.apache.harmony.jndi.provider.ldap.asn1.ASN1Encodable;
import org.apache.harmony.jndi.provider.ldap.asn1.Utils;

/**
 * This class add supports to <code>getAttributeDefinition()</code> and
 * <code>getAttributeSyntaxDefinition()</code> methods.
 * 
 */
public class LdapAttribute extends BasicAttribute implements ASN1Decodable,
        ASN1Encodable {

    private static final long serialVersionUID = -6492847268062616321L;

    private LdapContextImpl context = null;

    private static HashSet<String> BINARY_ATTRIBUTE = new HashSet<String>();
    static {
        BINARY_ATTRIBUTE.add("photo".toLowerCase()); //$NON-NLS-1$
        BINARY_ATTRIBUTE.add("personalSignature".toLowerCase()); //$NON-NLS-1$
        BINARY_ATTRIBUTE.add("audio".toLowerCase()); //$NON-NLS-1$
        BINARY_ATTRIBUTE.add("jpegPhoto".toLowerCase()); //$NON-NLS-1$
        BINARY_ATTRIBUTE.add("javaSerializedData".toLowerCase()); //$NON-NLS-1$
        BINARY_ATTRIBUTE.add("thumbnailPhoto".toLowerCase()); //$NON-NLS-1$
        BINARY_ATTRIBUTE.add("thumbnailLogo".toLowerCase()); //$NON-NLS-1$
        BINARY_ATTRIBUTE.add("userPassword".toLowerCase()); //$NON-NLS-1$
        BINARY_ATTRIBUTE.add("userCertificate".toLowerCase()); //$NON-NLS-1$
        BINARY_ATTRIBUTE.add("cACertificate".toLowerCase()); //$NON-NLS-1$
        BINARY_ATTRIBUTE.add("authorityRevocationList".toLowerCase()); //$NON-NLS-1$
        BINARY_ATTRIBUTE.add("certificateRevocationList".toLowerCase()); //$NON-NLS-1$
        BINARY_ATTRIBUTE.add("crossCertificatePair".toLowerCase()); //$NON-NLS-1$
        BINARY_ATTRIBUTE.add("x500UniqueIdentifier".toLowerCase()); //$NON-NLS-1$
    }

    /**
     * constructor for decode
     * 
     */
    public LdapAttribute() {
        super("", false); //$NON-NLS-1$
    }

    public LdapAttribute(String id, LdapContextImpl ctx) {
        super(id, false);
        context = ctx;
    }

    void setContext(LdapContextImpl ctx) {
        context = ctx;
    }

    /**
     * Constructs instance from already existing <code>Attribute</code>
     * 
     * @param attr
     *            may never be <code>null</code>
     * @throws NamingException
     */
    public LdapAttribute(Attribute attr, LdapContextImpl ctx)
            throws NamingException {
        super(attr.getID(), attr.isOrdered());
        NamingEnumeration<?> enu = attr.getAll();
        while (enu.hasMore()) {
            Object value = enu.next();
            add(value);
        }
        context = ctx;
    }

    @SuppressWarnings("unchecked")
    public void decodeValues(Object[] vs) {
        byte[] type = (byte[]) vs[0];
        attrID = Utils.getString(type);
        Collection<byte[]> list = (Collection<byte[]>) vs[1];
        for (byte[] bs : list) {
            add(bs);
        }
    }

    public void encodeValues(Object[] vs) {
        vs[0] = Utils.getBytes(attrID);

        List<Object> list = new ArrayList<Object>(this.values.size());

        for (Object object : this.values) {
            if (object instanceof String) {
                String str = (String) object;
                object = Utils.getBytes(str);
            }

            list.add(object);
        }
        vs[1] = list;
    }

    @Override
    public DirContext getAttributeDefinition() throws NamingException {
        DirContext schema = context.getSchema(""); //$NON-NLS-1$

        return (DirContext) schema
                .lookup(LdapSchemaContextImpl.ATTRIBUTE_DEFINITION
                        + "/" + getID()); //$NON-NLS-1$
    }

    @Override
    public DirContext getAttributeSyntaxDefinition() throws NamingException {
        DirContext schema = context.getSchema(""); //$NON-NLS-1$
        DirContext attrDef = (DirContext) schema
                .lookup(LdapSchemaContextImpl.ATTRIBUTE_DEFINITION + "/" //$NON-NLS-1$
                        + getID());

        Attribute syntaxAttr = attrDef.getAttributes("").get("syntax"); //$NON-NLS-1$ //$NON-NLS-2$

        if (syntaxAttr == null || syntaxAttr.size() == 0) {
            // jndi.90={0} does not have a syntax associated with it
            throw new NameNotFoundException(Messages.getString("jndi.90", //$NON-NLS-1$
                    getID()));
        }

        String syntaxName = (String) syntaxAttr.get();

        // look in the schema tree for the syntax definition
        return (DirContext) schema
                .lookup(LdapSchemaContextImpl.SYNTAX_DEFINITION + "/" //$NON-NLS-1$
                        + syntaxName);

    }

    public void convertValueToString() {
        // values can't be null
        if (values.size() == 0) {
            return;
        }

        Vector<Object> newValues = new Vector<Object>(values.size());
        for (Iterator<Object> iter = values.iterator(); iter.hasNext();) {
            Object value = iter.next();
            newValues.add(Utils.getString(value));
        }

        values.clear();
        values = newValues;
    }

    public static boolean isBinary(String name, String[] attrs) {
        if (BINARY_ATTRIBUTE.contains(name.toLowerCase())
                || name.endsWith(";binary")) { //$NON-NLS-1$
            return true;
        }

        if (attrs != null) {
            for (int i = 0; i < attrs.length; i++) {
                if (name.equalsIgnoreCase(attrs[i])) {
                    return true;
                }
            }
        }
        return false;
    }
}
