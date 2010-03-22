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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Binding;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;

import org.apache.harmony.jndi.provider.ldap.asn1.LdapASN1Constant;
import org.apache.harmony.jndi.provider.ldap.asn1.Utils;
import org.apache.harmony.jndi.provider.ldap.asn1.ASN1ChoiceWrap.ChosenValue;

public class LdapSearchResult {

    /**
     * All search result entries
     */
    protected HashMap<String, Attributes> entries = new HashMap<String, Attributes>();

    /**
     * SearchResultReference from server
     */
    protected List<String> refURLs;

    private LdapResult result;

    private NamingException ex;

    private String address;

    private String[] binaryAttributes;

    /**
     * batch size for this search operation
     */
    private int batchSize;

    /**
     * NamingEnumeration for this LdapSearchResult. One LdapSearchResult
     * instance can only has one corresponding NamingEnumeration instance.
     */
    private LdapNamingEnumeration<Object> enumeration;

    /**
     * Currently there are three different typs of LdapNamingEnumeration:
     *
     * <code>
     * 1    NameClassPair
     * 2    Binding
     * 3    SearchResult
     * </code>
     */
    private int enumerationType;

    private String baseDN;

    private LdapContextImpl context;

    private Name name;

    /**
     * whether received all search result from server
     */
    private boolean isFinished;

    private static int ENUMERATION_NAME_CLASS_PAIR = 1;

    private static int ENUMERATION_BINDING = 2;

    private static int ENUMERATION_SEARCH_RESULT = 3;

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void decodeSearchResponse(Object[] values) {
        ChosenValue chosen = (ChosenValue) values[0];
        switch (chosen.getIndex()) {
        case LdapASN1Constant.OP_SEARCH_RESULT_ENTRY:
            decodeEntry(chosen.getValue());
            break;
        case LdapASN1Constant.OP_SEARCH_RESULT_REF:
            decodeRef(chosen.getValue());
            break;
        case LdapASN1Constant.OP_SEARCH_RESULT_DONE:
            decodeDone(chosen.getValue());
            break;
        }
    }

    protected void decodeDone(Object value) {
        result = new LdapResult();
        result.decodeValues((Object[]) value);
        isFinished = true;
        addToEnumeration();
    }

    protected void decodeRef(Object value) {
        if (refURLs == null) {
            refURLs = new ArrayList<String>();
        }

        Collection<byte[]> list = (Collection<byte[]>) value;
        for (byte[] bs : list) {
            refURLs.add(Utils.getString(bs));
        }
    }

    protected void decodeEntry(Object value) {
        Object[] values = (Object[]) value;
        String name = Utils.getString(values[0]);

        if (address != null) {
            name = address + name;
        }

        Attributes attrs = null;
        synchronized (entries) {
            if (entries.containsKey(name)) {
                attrs = entries.get(name);
            } else {
                attrs = new BasicAttributes(true);
                entries.put(name, attrs);
            }
        }

        Collection<Object[]> list = (Collection<Object[]>) values[1];
        for (Object[] objects : list) {
            LdapAttribute attr = new LdapAttribute();
            attr.decodeValues(objects);
            if (!LdapAttribute.isBinary(attr.getID(), binaryAttributes)) {
                attr.convertValueToString();
            }
            attrs.put(attr);
        }
        addToEnumeration();
    }

    public Map<String, Attributes> getEntries() {
        return entries;
    }

    public List<String> getRefURLs() {
        return refURLs;
    }

    public LdapResult getResult() {
        return result;
    }

    public NamingException getException() {
        return ex;
    }

    public void setException(Exception ex) {
        if (ex == null) {
            this.ex = null;
            return;
        }
        if (!(ex instanceof NamingException)) {
            this.ex = new NamingException(ex.getMessage());
            this.ex.initCause(ex);
        } else {
            this.ex = (NamingException) ex;
        }
    }

    public boolean isEmpty() {
        return entries.size() == 0 && (refURLs == null || refURLs.size() == 0);
    }

    public void setRefURLs(List<String> refURLs) {
        this.refURLs = refURLs;
    }

    public String[] getBinaryAttributes() {
        return binaryAttributes;
    }

    public void setBinaryAttributes(String[] binaryAttributes) {
        this.binaryAttributes = binaryAttributes;
    }

    public NamingEnumeration<NameClassPair> toNameClassPairEnumeration(
            String baseDN) {
        enumerationType = ENUMERATION_NAME_CLASS_PAIR;
        enumeration = new LdapNamingEnumeration<Object>(null, null);
        this.baseDN = baseDN;
        addToEnumeration();
        return (NamingEnumeration) enumeration;
    }

    public NamingEnumeration<Binding> toBindingEnumeration(
            LdapContextImpl context, Name name) throws NamingException {
        enumerationType = ENUMERATION_BINDING;
        enumeration = new LdapNamingEnumeration<Object>(null, null);
        this.context = context;
        this.name = name;

        Name tempName = new LdapName(context.getNameInNamespace());
        tempName.addAll(name);
        baseDN = tempName.toString();

        addToEnumeration();

        return (NamingEnumeration) enumeration;
    }

    public NamingEnumeration<SearchResult> toSearchResultEnumeration(
            String baseDN) {
        enumerationType = ENUMERATION_SEARCH_RESULT;
        enumeration = new LdapNamingEnumeration<Object>(null, null);
        this.baseDN = baseDN;

        addToEnumeration();

        return (NamingEnumeration) enumeration;
    }

    private void addToEnumeration() {
        if (enumeration == null || entries == null || entries.isEmpty()) {
            return;
        }

        List<Object> list = new ArrayList<Object>();
        HashMap<String, Attributes> tempEntries = null;

        synchronized (entries) {
            tempEntries = (HashMap<String, Attributes>) entries.clone();
            entries.clear();
        }

        try {
            for (String dn : tempEntries.keySet()) {
                String relativeName = LdapUtils.convertToRelativeName(dn,
                        baseDN);
                Attributes attrs = tempEntries.get(dn);
                Attribute attrClass = attrs.get("javaClassName"); //$NON-NLS-1$
                String className = null;
                switch (enumerationType) {
                case 1:
                    if (attrClass != null) {
                        className = (String) attrClass.get(0);
                    } else {
                        className = DirContext.class.getName();
                    }
                    NameClassPair pair = new NameClassPair(relativeName,
                            className, true);
                    pair.setNameInNamespace(dn);
                    list.add(pair);
                    break;
                case 2:
                    Object bound = null;

                    if (attrClass != null) {
                        className = (String) attrClass.get(0);
                        bound = context.lookup(name);
                    } else {
                        className = DirContext.class.getName();
                        bound = new LdapContextImpl(context, baseDN);
                    }

                    Binding binding = new Binding(relativeName, className,
                            bound);
                    binding.setNameInNamespace(dn);
                    list.add(binding);
                    break;
                case 3:
                    SearchResult sr = null;
                    if (dn.startsWith("ldap://")) {
                        sr = new SearchResult(dn, null, attrs, false);
                        int index = dn.indexOf("/", 7);
                        sr.setNameInNamespace(dn.substring(index + 1, dn
                                .length()));
                    } else {
                        sr = new SearchResult(relativeName, null, attrs);
                        sr.setNameInNamespace(dn);
                    }
                    list.add(sr);
                    break;
                }
            }
        } catch (NamingException e) {
            ex = e;
            isFinished = true;
        }

        enumeration.setException(ex);
        enumeration.add(list, isFinished);
    }
}
