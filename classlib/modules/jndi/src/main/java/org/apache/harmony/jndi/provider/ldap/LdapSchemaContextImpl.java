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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.ReferralException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SchemaViolationException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.harmony.jndi.internal.nls.Messages;
import org.apache.harmony.jndi.provider.ldap.parser.SchemaParser;

public class LdapSchemaContextImpl extends LdapContextImpl {

    public static final String CLASS_DEFINITION = "ClassDefinition"; //$NON-NLS-1$

    public static final String ATTRIBUTE_DEFINITION = "AttributeDefinition"; //$NON-NLS-1$

    public static final String SYNTAX_DEFINITION = "SyntaxDefinition"; //$NON-NLS-1$

    public static final String MATCHING_RULE = "MatchingRule"; //$NON-NLS-1$

    public static final String OBJECT_CLASSES = "objectclasses"; //$NON-NLS-1$

    public static final String ATTRIBUTE_TYPES = "attributetypes"; //$NON-NLS-1$

    public static final String LDAP_SYNTAXES = "ldapsyntaxes"; //$NON-NLS-1$

    public static final String MATCHING_RULES = "matchingrules"; //$NON-NLS-1$

    public static final int SCHEMA_ROOT_LEVEL = 3;

    public static final int DEFINITION_LEVEL = 2;

    final private static Hashtable<String, String> schemaJndi2Ldap = new Hashtable<String, String>();
    static {
        schemaJndi2Ldap.put(CLASS_DEFINITION.toLowerCase(), OBJECT_CLASSES);
        schemaJndi2Ldap
                .put(ATTRIBUTE_DEFINITION.toLowerCase(), ATTRIBUTE_TYPES);
        schemaJndi2Ldap.put(SYNTAX_DEFINITION.toLowerCase(), LDAP_SYNTAXES);
        schemaJndi2Ldap.put(MATCHING_RULE.toLowerCase(), MATCHING_RULES);
    }

    final private static Hashtable<String, String> schemaLdap2Jndi = new Hashtable<String, String>();
    static {
        schemaLdap2Jndi.put(OBJECT_CLASSES, CLASS_DEFINITION);
        schemaLdap2Jndi.put(ATTRIBUTE_TYPES, ATTRIBUTE_DEFINITION);
        schemaLdap2Jndi.put(LDAP_SYNTAXES, SYNTAX_DEFINITION);
        schemaLdap2Jndi.put(MATCHING_RULES, MATCHING_RULE);
    }

    private LdapContextImpl ldapContext;

    private Hashtable<String, Object> schemaTable;

    private Name rdn = null;

    private int level;

    public LdapSchemaContextImpl(LdapContextImpl ctx,
            Hashtable<Object, Object> env, Name dn,
            Hashtable<String, Object> schemaTable, int level)
            throws InvalidNameException {
        super(ctx, env, dn.getPrefix(0).toString());
        ldapContext = ctx;
        rdn = dn;
        this.schemaTable = schemaTable;
        this.level = level;
    }

    @Override
    public DirContext getSchema(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public DirContext getSchema(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public DirContext getSchemaClassDefinition(Name name)
            throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public DirContext getSchemaClassDefinition(String name)
            throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public String getNameInNamespace() throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public DirContext createSubcontext(Name name, Attributes attributes)
            throws NamingException {
        int size = name.size();
        Hashtable<String, Object> subSchemaTree = doLookup(name
                .getPrefix(size - 1), size - 1);

        if (null == attributes || attributes.size() == 0) {
            // jndi.8D=Must supply attributes describing schema
            throw new SchemaViolationException(Messages.getString("jndi.8D")); //$NON-NLS-1$
        }

        if (level - size == 2) {
            // jndi.8E=Cannot create new entry under schema root
            throw new SchemaViolationException(Messages.getString("jndi.8E")); //$NON-NLS-1$
        }

        String subSchemaType = name.getSuffix(size - 1).toString();

        if (subSchemaTree.get(subSchemaType.toLowerCase()) != null) {
            throw new NameAlreadyBoundException(subSchemaType);
        }

        String schemaLine = SchemaParser.format(attributes);

        ModifyOp op = new ModifyOp(ldapContext.subschemasubentry);
        Name modifySchemaName = name.getPrefix(size - 1).addAll(rdn);
        BasicAttribute schemaEntry = new LdapAttribute(new BasicAttribute(
                jndi2ldap(modifySchemaName.toString()), schemaLine), ldapContext);
        op.addModification(OperationJndi2Ldap[DirContext.ADD_ATTRIBUTE],
                new LdapAttribute(schemaEntry, ldapContext));
        try {
            doBasicOperation(op);
            subSchemaTree.put(subSchemaType.toLowerCase(), schemaLine);
        } catch (ReferralException e) {
            // TODO
        }

        return (DirContext) lookup(name);
    }

    @Override
    public DirContext createSubcontext(String name, Attributes attributes)
            throws NamingException {
        return createSubcontext(new CompositeName(name), attributes);
    }

    @Override
    public Attributes getAttributes(Name name) throws NamingException {
        int size = name.size();

        Hashtable<String, Object> attributesTable = doLookup(name, size);

        BasicAttributes schemaAttributes = new BasicAttributes(true);

        switch (level - size) {
        case 1:
            Set<String> keyset = attributesTable.keySet();
            for (Iterator<String> i = keyset.iterator(); i.hasNext();) {
                String id = i.next();
                if (id.equals("orig")) { //$NON-NLS-1$
                    continue;
                }
                Object value = attributesTable.get(id);
                BasicAttribute basicAttr = new BasicAttribute(id);

                if (value instanceof List) {
                    List<Object> list = (List<Object>) value;
                    for (int j = 0; j < list.size(); j++) {
                        basicAttr.add(list.get(j));
                    }
                } else {
                    basicAttr.add(value);
                }
                schemaAttributes.put(basicAttr);
            }
            break;

        case 2:
            BasicAttribute basicAttr = new BasicAttribute("objectclass"); //$NON-NLS-1$
            Name allName = name.addAll(rdn);
            basicAttr.add(allName.toString());
            schemaAttributes.put(basicAttr);
            break;

        default:
            // Do nothing.
        }

        return schemaAttributes;
    }

    @Override
    public Attributes getAttributes(Name name, String[] as)
            throws NamingException {
        Attributes attrs = getAttributes(name);
        Attribute attr = null;
        Attributes filteredAttrs = new BasicAttributes(true);
        for (int i = 0; i < as.length; i++) {
            attr = attrs.get(as[i]);
            if (attr != null) {
                filteredAttrs.put(attr);
            }
        }
        return filteredAttrs;
    }

    @Override
    public Attributes getAttributes(String name) throws NamingException {
        return getAttributes(new CompositeName(name));
    }

    @Override
    public Attributes getAttributes(String name, String[] as)
            throws NamingException {
        return getAttributes(new CompositeName(name), as);
    }

    @Override
    public void modifyAttributes(Name name, int i, Attributes attributes)
            throws NamingException {
        if (name == null) {
            // jndi.2E=The name is null
            throw new NullPointerException(Messages.getString("jndi.2E")); //$NON-NLS-1$
        }

        if (attributes == null) {
            // jndi.13=Non-null attribute is required for modification
            throw new NullPointerException(Messages.getString("jndi.13")); //$NON-NLS-1$
        }

        if (i != DirContext.ADD_ATTRIBUTE && i != DirContext.REMOVE_ATTRIBUTE
                && i != DirContext.REPLACE_ATTRIBUTE) {
            /*
             * jndi.14=Modification code {0} must be one of
             * DirContext.ADD_ATTRIBUTE, DirContext.REPLACE_ATTRIBUTE and
             * DirContext.REMOVE_ATTRIBUTE
             */
            throw new IllegalArgumentException(Messages.getString("jndi.14", i)); //$NON-NLS-1$
        }

        NamingEnumeration<? extends Attribute> enu = attributes.getAll();
        ModificationItem[] items = new ModificationItem[attributes.size()];
        int index = 0;
        while (enu.hasMore()) {
            items[index++] = new ModificationItem(i, enu.next());
        }

        modifyAttributes(name, items);
    }

    // Mapping from DirContext's attribute operation code to server's operation
    // code.
    private static final int OperationJndi2Ldap[] = { -1, 0, 2, 1, };

    @Override
    public void modifyAttributes(Name name, ModificationItem[] modificationItems)
            throws NamingException {
        // First get the old schema.
        int size = name.size();

        if (size < 1) {
            // ldap.38=Can't modify schema root
            throw new SchemaViolationException(Messages.getString("ldap.38")); //$NON-NLS-1$
        }

        Hashtable<String, Object> subSchemaTree = doLookup(name
                .getPrefix(size - 1), size - 1);

        String subSchemaType = name.getSuffix(size - 1).toString()
                .toLowerCase();

        Object schema = subSchemaTree.get(jndi2ldap(subSchemaType));
        if (schema == null) {
            throw new NameNotFoundException(name.toString());
        }

        if (level - size == 2) {
            // ldap.38=Can't modify schema root
            throw new SchemaViolationException(Messages.getString("ldap.38")); //$NON-NLS-1$
        }

        if (modificationItems.length == 0) {
            return;
        }

        String schemaLine = schema.toString();
        if (schema instanceof Hashtable) {
            Hashtable<String, Object> table = (Hashtable<String, Object>) schema;
            schemaLine = table.get(SchemaParser.ORIG).toString();
        }

        // Construct the new schema.
        Attributes attributes = getAttributes(name);
        int modifyOperation;
        Attribute modifyAttribute;
        Attribute attribute;
        NamingEnumeration<?> enu;
        for (int i = 0; i < modificationItems.length; i++) {
            modifyOperation = modificationItems[i].getModificationOp();
            modifyAttribute = modificationItems[i].getAttribute();

            switch (modifyOperation) {
            case DirContext.ADD_ATTRIBUTE:
                attribute = attributes.get(modifyAttribute.getID());
                if (attribute == null) {
                    attributes.put(modifyAttribute);
                } else {
                    enu = modifyAttribute.getAll();
                    while (enu.hasMoreElements()) {
                        attribute.add(enu.nextElement());
                    }
                    attributes.put(attribute);
                }
                break;

            case DirContext.REMOVE_ATTRIBUTE:
                attribute = attributes.get(modifyAttribute.getID());
                enu = modifyAttribute.getAll();
                while (enu.hasMoreElements()) {
                    attribute.remove(enu.nextElement());
                }
                if (attribute.size() == 0) {
                    attributes.remove(modifyAttribute.getID());
                }
                break;

            case DirContext.REPLACE_ATTRIBUTE:
                attributes.remove(modifyAttribute.getID());
                attributes.put(modifyAttribute);
                break;
            default:
                // Never reach here.
            }
        }
        String newSchemaLine = SchemaParser.format(attributes);

        // Remove old schema, then add new schema.
        ModifyOp op = new ModifyOp(ldapContext.subschemasubentry);
        Name modifySchemaName = name.getPrefix(size - 1).addAll(rdn);
        BasicAttribute schemaEntry = new LdapAttribute(new BasicAttribute(
                jndi2ldap(modifySchemaName.toString()), schemaLine),
                ldapContext);
        op.addModification(OperationJndi2Ldap[DirContext.REMOVE_ATTRIBUTE],
                new LdapAttribute(schemaEntry, ldapContext));
        BasicAttribute addSchemaEntry = new LdapAttribute(new BasicAttribute(
                jndi2ldap(modifySchemaName.toString()), newSchemaLine),
                ldapContext);
        op.addModification(OperationJndi2Ldap[DirContext.ADD_ATTRIBUTE],
                new LdapAttribute(addSchemaEntry, ldapContext));

        doBasicOperation(op);

        // Modify the hashtable to reflect the modification.
        Object subSchema = subSchemaTree.get(subSchemaType);
        if (subSchema instanceof String) {
            subSchemaTree.remove(subSchemaType);
            subSchemaTree.put(subSchemaType, newSchemaLine);
        } else {
            /*
             * Here we can only change the content of subSchemaTable, instead of
             * change the reference. Because in other ldapSchemaContext, there
             * may be reference to this table. And they should also reflect the
             * changes.
             */
            Hashtable<String, Object> subSchemaTable = (Hashtable<String, Object>) subSchema;
            subSchemaTable.clear();
            Hashtable<String, Object> parsedTable = SchemaParser
                    .parseValue(newSchemaLine);
            Iterator<Entry<String, Object>> it = parsedTable.entrySet()
                    .iterator();
            while (it.hasNext()) {
                Entry<String, Object> entry = it.next();
                subSchemaTable.put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void modifyAttributes(String name, int i, Attributes attributes)
            throws NamingException {
        modifyAttributes(new CompositeName(name), i, attributes);
    }

    @Override
    public void modifyAttributes(String name,
            ModificationItem[] modificationItems) throws NamingException {
        modifyAttributes(new CompositeName(name), modificationItems);
    }

    @Override
    public Context createSubcontext(Name name) throws NamingException {
        return createSubcontext(name, null);
    }

    @Override
    public Context createSubcontext(String name) throws NamingException {
        return createSubcontext(new CompositeName(name));
    }

    @Override
    public void destroySubcontext(Name name) throws NamingException {
        int size = name.size();
        Hashtable<String, Object> subSchemaTree = doLookup(name
                .getPrefix(size - 1), size - 1);

        String subSchemaType = name.getSuffix(size - 1).toString()
                .toLowerCase();

        Object schema = subSchemaTree.get(jndi2ldap(subSchemaType));
        if (schema == null) {
            // Return silently.
            return;
        }

        if (level - size == 2) {
            // ldap.37=Can't delete schema root
            throw new SchemaViolationException(Messages.getString("ldap.37")); //$NON-NLS-1$
        }

        if (level == size) {
            // Return silently.
            return;
        }

        String schemaLine = schema.toString();
        if (schema instanceof Hashtable) {
            Hashtable<String, Object> table = (Hashtable<String, Object>) schema;
            schemaLine = table.get(SchemaParser.ORIG).toString();
        }

        ModifyOp op = new ModifyOp(ldapContext.subschemasubentry);
        Name modifySchemaName = name.getPrefix(size - 1).addAll(rdn);
        BasicAttribute schemaEntry = new LdapAttribute(new BasicAttribute(
                jndi2ldap(modifySchemaName.toString()), schemaLine), ldapContext);
        op.addModification(OperationJndi2Ldap[DirContext.REMOVE_ATTRIBUTE],
                new LdapAttribute(schemaEntry, ldapContext));
        try {
            doBasicOperation(op);
            subSchemaTree.remove(subSchemaType);
        } catch (ReferralException e) {
            // TODO
        }
    }

    @Override
    public void destroySubcontext(String name) throws NamingException {
        destroySubcontext(new CompositeName(name));
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name name)
            throws NamingException {
        int size = name.size();

        Hashtable<String, Object> tempSchema = doLookup(name, size);

        if (size == level - 1) {
            return new LdapNamingEnumeration<NameClassPair>(null, null);
        }

        Iterator<String> keys = tempSchema.keySet().iterator();

        List<NameClassPair> list = new ArrayList<NameClassPair>();
        while (keys.hasNext()) {
            list.add(new NameClassPair(ldap2jndi(keys.next()), this.getClass()
                    .getName()));
        }

        return new LdapNamingEnumeration<NameClassPair>(list, null);
    }

    @Override
    protected Name convertFromStringToName(String s)
            throws InvalidNameException {
        CompositeName name = new CompositeName(s);
        return name;
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name)
            throws NamingException {
        return list(new CompositeName(name));
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name)
            throws NamingException {
        int size = name.size();

        Hashtable<String, Object> tempSchema = doLookup(name, size);

        if (size == level - 1) {
            return new LdapNamingEnumeration<Binding>(null, null);
        }

        Iterator<String> keys = tempSchema.keySet().iterator();

        List<Binding> list = new ArrayList<Binding>();
        while (keys.hasNext()) {
            list.add(new Binding(ldap2jndi(keys.next()), this.getClass()
                    .getName()));
        }

        return new LdapNamingEnumeration<Binding>(list, null);
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name)
            throws NamingException {
        return listBindings(new CompositeName(name));
    }

    private Hashtable<Name, LdapSchemaContextImpl> cachedSubSchemas = new Hashtable<Name, LdapSchemaContextImpl>();

    @Override
    public Object lookup(Name name) throws NamingException {
        // If cached, directly return cached one.
        Name targetDN = name;
        LdapSchemaContextImpl cachedSchema = cachedSubSchemas.get(targetDN);
        if (cachedSchema != null) {
            return cachedSchema;
        }
        int size = targetDN.size();
        if (size == 0) {
            return this;
        }

        Hashtable<String, Object> newSchemaTable = doLookup(name, size);

        cachedSchema = new LdapSchemaContextImpl(ldapContext, env, targetDN,
                newSchemaTable, level - size);
        cachedSubSchemas.put(targetDN, cachedSchema);

        return cachedSchema;
    }

    // Find the subtree of schematree corresponding to the name.
    private Hashtable<String, Object> doLookup(Name name, int size)
            throws NamingException {
        Name targetDN = name;
        if (size >= level) {
            throw new NameNotFoundException(name.toString());
        }

        Hashtable<String, Object> tempSchema = schemaTable;
        Object tempValue;
        for (int i = 0; i < size; i++) {
            String key = targetDN.get(i);
            tempValue = tempSchema.get(jndi2ldap(key));
            if (tempValue == null) {
                throw new NameNotFoundException(name.toString());
            }

            if (tempValue instanceof String) {
                Hashtable<String, Object> attributesTable = SchemaParser
                        .parseValue(tempValue.toString());
                tempSchema.put(jndi2ldap(key).toLowerCase(), attributesTable);
                tempSchema = attributesTable;
            } else {
                tempSchema = (Hashtable<String, Object>) tempValue;
            }
        }

        return tempSchema;
    }

    @Override
    public Object lookup(String name) throws NamingException {
        return lookup(new CompositeName(name));
    }

    @Override
    public void rename(Name nOld, Name nNew) throws NamingException {
        if (nOld == null || nNew == null) {
            throw new NullPointerException();
        }

        if (nOld.size() == 0 || nNew.size() == 0) {
            // ldap.3A=Can't rename empty name
            throw new InvalidNameException(Messages.getString("ldap.3A")); //$NON-NLS-1$
        }

        if (nOld.size() > 1 || nNew.size() > 1) {
            // ldap.3B=Can't rename across contexts
            throw new InvalidNameException(Messages.getString("ldap.3B")); //$NON-NLS-1$
        }
        // ldap.39=Can't rename schema
        throw new SchemaViolationException(Messages.getString("ldap.39")); //$NON-NLS-1$
    }

    @Override
    public void rename(String sOld, String sNew) throws NamingException {
        rename(new CompositeName(sOld), new CompositeName(sNew));
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name,
            Attributes attributes) throws NamingException {
        return search(name, attributes, null);
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name,
            Attributes matchingAttributes, String[] attributesToReturn)
            throws NamingException {
        int size = name.size();

        Hashtable<String, Object> subschemaTable = doLookup(name, size);

        SearchResult searchResult;
        Attributes schemaAttributes;
        String schemaName;
        Set<String> keyset;

        if (level - size > 1) {
            keyset = subschemaTable.keySet();
            List<SearchResult> list = new ArrayList<SearchResult>();
            for (Iterator<String> i = keyset.iterator(); i.hasNext();) {
                schemaName = ldap2jndi(i.next());
                Name tempName = (Name) name.clone();
                schemaAttributes = getAttributes(tempName.add(schemaName));

                if (isMatch(schemaAttributes, matchingAttributes)) {
                    schemaAttributes = filterAttributes(schemaAttributes,
                            attributesToReturn);
                    searchResult = new SearchResult(ldap2jndi(schemaName), this
                            .getClass().getName(), null, schemaAttributes);
                    list.add(searchResult);
                }
            }
            return new LdapNamingEnumeration<SearchResult>(list, null);
        }
        return new LdapNamingEnumeration<SearchResult>(null, null);
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name, String filter,
            SearchControls searchControls) throws NamingException {
        return search(name, filter, null, searchControls);
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name, String filter,
            Object[] filterArgs, SearchControls searchControls)
            throws NamingException {

        HashSet<SearchResult> searchResults = new HashSet<SearchResult>();
        Iterator<SearchResult> iterator;
        SearchResult searchResult;

        Attributes schemaAttributes;

        // Default search scope is ONELEVEL_SCOPE.
        if (searchControls == null
                || searchControls.getSearchScope() == SearchControls.ONELEVEL_SCOPE) {
            searchResults = doSimpleSearch(name, false);

        }
        // SearchControls.SUBTREE_SCOPE
        else if (searchControls.getSearchScope() == SearchControls.SUBTREE_SCOPE) {
            searchResults = doSimpleSearch(name, true);
        }

        // SearchControls.OBJECT_SCOPE.
        else {
            schemaAttributes = getAttributes(name);
            searchResult = new SearchResult(ldap2jndi(name.toString()), this
                    .getClass().getName(), null, schemaAttributes);
            searchResults.add(searchResult);
        }

        LdapSchemaFilter schemaFilter = new LdapSchemaFilter(filter, filterArgs);
        searchResults = schemaFilter.filter(searchResults);

        if (searchControls != null
                && searchControls.getReturningAttributes() != null) {
            String[] attributesToReturn = searchControls
                    .getReturningAttributes();
            // Take the 0 as special case to improve performance.
            if (attributesToReturn.length > 0) {
                iterator = searchResults.iterator();
                while (iterator.hasNext()) {
                    searchResult = iterator.next();
                    schemaAttributes = filterAttributes(searchResult
                            .getAttributes(), attributesToReturn);
                    searchResult.setAttributes(schemaAttributes);
                }
            } else {
                iterator = searchResults.iterator();
                while (iterator.hasNext()) {
                    searchResult = iterator.next();
                    searchResult.setAttributes(new BasicAttributes(true));
                }
            }
        }

        iterator = searchResults.iterator();
        List<SearchResult> list = new ArrayList<SearchResult>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }

        return new LdapNamingEnumeration<SearchResult>(list, null);
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, Attributes attributes, String[] as) throws NamingException {
        return search(new CompositeName(name), attributes, as);
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, Attributes attributes) throws NamingException {
        return search(new CompositeName(name), attributes);
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, String filter, Object[] objs, SearchControls searchControls) throws NamingException {
        return search(new CompositeName(name), filter, objs, searchControls);
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls searchControls) throws NamingException {
        return search(new CompositeName(name), filter, searchControls);
    }

    protected DirContext getClassDefinition(Attribute objectclassAttr)
            throws NamingException {
        Hashtable<String, Object> definitionTable = new Hashtable<String, Object>();
        Hashtable<String, Object> allDefinitionTable = (Hashtable<String, Object>) schemaTable
                .get(OBJECT_CLASSES);

        if (objectclassAttr != null) {
            NamingEnumeration<?> ne = objectclassAttr.getAll();
            String attributeType;
            while (ne.hasMore()) {
                attributeType = ne.next().toString().toLowerCase();
                definitionTable.put(attributeType, allDefinitionTable
                        .get(attributeType));
            }
        }

        return new LdapSchemaContextImpl(this, env, new CompositeName(
                OBJECT_CLASSES), definitionTable,
                LdapSchemaContextImpl.DEFINITION_LEVEL);
    }

    private HashSet<SearchResult> doSimpleSearch(Name name,
            boolean searchSubTree) throws NamingException {
        int size = name.size();
        Hashtable<String, Object> subschemaTable = doLookup(name, size);

        HashSet<SearchResult> searchResults = new HashSet<SearchResult>();
        HashSet<SearchResult> tempResults;
        SearchResult searchResult;
        Attributes schemaAttributes;
        String schemaName;
        Set<String> keyset;

        keyset = subschemaTable.keySet();
        for (Iterator<String> i = keyset.iterator(); i.hasNext();) {
            schemaName = ldap2jndi(i.next());
            Name tempName = (Name) name.clone();
            tempName = tempName.add(schemaName);
            if (tempName.size() < level) {
                schemaAttributes = getAttributes(tempName);
                searchResult = new SearchResult(tempName.toString(), this
                        .getClass().getName(), null, schemaAttributes);
                searchResults.add(searchResult);

                if (searchSubTree) {
                    tempResults = doSimpleSearch(tempName, searchSubTree);
                    searchResults.addAll(tempResults);
                }
            }
        }
        return searchResults;
    }

    private Attributes filterAttributes(Attributes attributes,
            String[] attributesToReturn) {
        if (attributesToReturn == null) {
            return attributes;
        }

        Attribute attribute;
        Attributes filteredAttrs = new BasicAttributes(true);
        for (int i = 0; i < attributesToReturn.length; i++) {
            if (attributesToReturn[i] != null) {
                attribute = attributes.get(attributesToReturn[i]);
                if (attribute != null) {
                    filteredAttrs.put(attribute);
                }
            }
        }

        return filteredAttrs;
    }

    private boolean isMatch(Attributes schemaAttributes,
            Attributes matchingAttributes) throws NamingException {
        if (matchingAttributes == null) {
            return true;
        }

        NamingEnumeration<? extends Attribute> enumeration = matchingAttributes
                .getAll();
        Attribute matchAttribute;
        Attribute schemaAttribute;
        String id;
        while (enumeration.hasMore()) {
            matchAttribute = enumeration.next();
            id = matchAttribute.getID();
            schemaAttribute = schemaAttributes.get(id);
            if (schemaAttribute == null) {
                return false;
            }

            NamingEnumeration<?> singleEnu = matchAttribute.getAll();
            while (singleEnu.hasMore()) {
                if (!schemaAttribute.contains(singleEnu.next())) {
                    return false;
                }
            }
        }

        return true;
    }

    // Convert ldap name to jndi name.
    private String ldap2jndi(String jndiName) {
        String ldapName = schemaLdap2Jndi.get(jndiName);
        if (null == ldapName) {
            ldapName = jndiName;
        }

        return ldapName;
    }

    // Convert jndi name to ldap name.
    private String jndi2ldap(String jndiName) {
        // If the parameter indeed is ldapName, convert it to jndiName to avoid
        // confusion.
        String ldapName = schemaLdap2Jndi.get(jndiName);
        if (null != ldapName) {
            return ldapName;
        }

        ldapName = schemaJndi2Ldap.get(jndiName.toLowerCase());
        if (null == ldapName) {
            ldapName = jndiName;
        }

        return ldapName.toLowerCase();
    }
}
