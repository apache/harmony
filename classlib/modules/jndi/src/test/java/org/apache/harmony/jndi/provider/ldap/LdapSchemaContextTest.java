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
import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidSearchFilterException;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SchemaViolationException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import junit.framework.TestCase;

public class LdapSchemaContextTest extends TestCase {
    private LdapSchemaContextImpl schema;

    private Hashtable<String, Object> schemaTable;

    private LdapContextImpl context;

    Name name;

    @Override
    public void setUp() throws Exception {
        // Construct the schema table.
        schemaTable = new Hashtable<String, Object>();

        Hashtable<String, Object> subSchema = new Hashtable<String, Object>();
        subSchema
                .put(
                        "javaclass",
                        "( 1.3.6.1.4.1.18060.0.4.1.3.8 name 'javaclass' sup top "
                                + "structural must ( fullyqualifiedjavaclassname $ javaclassbytecode ) x-schema 'apache' )");

        subSchema
                .put(
                        "extensibleobject",
                        "( 1.3.6.1.4.1.1466.101.120.111 name 'extensibleobject' "
                                + "desc 'rfc2252: extensible object' sup top auxiliary x-schema 'system' )");
        subSchema
                .put(
                        "prefnode",
                        "( 1.3.6.1.4.1.18060.0.4.1.3.1 name "
                                + "'prefnode' sup top structural must prefnodename x-schema 'apache' )");
        schemaTable.put("objectclasses", subSchema);

        subSchema = new Hashtable<String, Object>();
        subSchema
                .put(
                        "integerorderingmatch",
                        "( 2.5.13.15 name 'integerorderingmatch'  syntax 1.3.6.1.4.1.1466.115.121.1.27 x-schema 'system' )");

        subSchema
                .put(
                        "caseexactmatch",
                        "( 2.5.13.5 name 'caseexactmatch'  syntax 1.3.6.1.4.1.1466.115.121.1.15 x-schema 'system' )");
        schemaTable.put("matchingrules", subSchema);

        subSchema = new Hashtable<String, Object>();
        subSchema
                .put(
                        "1.3.6.1.4.1.1466.115.121.1.19",
                        "( 1.3.6.1.4.1.1466.115.121.1.19  x-schema 'system' x-is-human-readable 'true' )");
        schemaTable.put("ldapsyntaxes", subSchema);

        subSchema = new Hashtable<String, Object>();
        subSchema
                .put(
                        "dsaquality",
                        "( 0.9.2342.19200300.100.1.49 name 'dsaquality' desc 'rfc1274: dsa quality'  syntax 1.3.6.1.4.1.1466.115.121.1.19 single-value usage userapplications x-schema 'cosine' )");
        schemaTable.put("attributetypes", subSchema);

        context = new LdapContextImpl(new MockLdapClient(), null, "");
        name = new CompositeName("");
        schema = new LdapSchemaContextImpl(context, null, name, schemaTable,
                LdapSchemaContextImpl.SCHEMA_ROOT_LEVEL);
    }

    public void testList_NullPara() throws Exception {
        String nullString = null;
        Name nullName = null;
        try {
            schema.list(nullString);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // Expected.
        }

        try {
            schema.list(nullName);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // Expected.
        }
    }

    public void testList() throws Exception {
        // "" as parameter.
        NamingEnumeration<NameClassPair> namingEnum = schema.list("");
        NameClassPair pair;
        ArrayList<String> verifyList = new ArrayList<String>();
        verifyList.add("classdefinition");
        verifyList.add("attributedefinition");
        verifyList.add("matchingrule");
        verifyList.add("syntaxdefinition");
        while (namingEnum.hasMore()) {
            pair = namingEnum.next();
            assertTrue(verifyList.remove(pair.getName().toLowerCase()));
        }
        assertEquals(0, verifyList.size());

        // "matchingrule" as parameter.
        namingEnum = schema.list("matchingrule");
        int count = 0;
        while (namingEnum.hasMore()) {
            pair = namingEnum.next();
            count++;
        }
        assertEquals(2, count);

        // "syntaxdefinition" as parameter.
        namingEnum = schema.list("syntaxdefinition");
        count = 0;
        while (namingEnum.hasMore()) {
            pair = namingEnum.next();
            count++;
        }
        assertEquals(1, count);

        // "classdefinition" as parameter.
        namingEnum = schema.list("classdefinition");
        count = 0;
        while (namingEnum.hasMore()) {
            pair = namingEnum.next();
            count++;
        }
        assertEquals(3, count);

        // "attributedefinition" as parameter.
        namingEnum = schema.list("attributedefinition");
        count = 0;
        while (namingEnum.hasMore()) {
            pair = namingEnum.next();
            count++;
        }
        assertEquals(1, count);

        // "classdefinition/javaClass" as parameter.
        namingEnum = schema.list("classdefinition/javaClass");
        assertFalse(namingEnum.hasMore());
    }

    public void testList_Exception() throws Exception {
        NamingEnumeration<NameClassPair> namingEnum;
        try {
            namingEnum = schema.list("invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            namingEnum = schema.list("invalid/invalid/invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            namingEnum = schema.list("invalid/invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            namingEnum = schema.list("classdefinition/invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            namingEnum = schema.list("classdefinition/javaClass/name");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            namingEnum = schema.list("classdefinition/javaClass/invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
        
        try {
            namingEnum = schema.list("objectclasses");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void testList_Address() throws Exception {
        NamingEnumeration<NameClassPair> namingEnum;
        try {
            namingEnum = schema.list("ldap://localhost:10389/");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            namingEnum = schema
                    .list("ldap://localhost:10389/dc=example,dc=com");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void testGetSchema() throws Exception {
        try {
            schema.getSchema("");
            fail("Should throw OperationNotSupportedException.");
        } catch (OperationNotSupportedException e) {
            // Expected.
        }

        try {
            schema.getSchema(new CompositeName());
            fail("Should throw OperationNotSupportedException.");
        } catch (OperationNotSupportedException e) {
            // Expected.
        }

        try {
            schema.getSchema(new CompositeName("invalid"));
            fail("Should throw OperationNotSupportedException.");
        } catch (OperationNotSupportedException e) {
            // Expected.
        }

        try {
            String nullString = null;
            schema.getSchema(nullString);
            fail("Should throw OperationNotSupportedException.");
        } catch (OperationNotSupportedException e) {
            // Expected.
        }

        try {
            Name nullName = null;
            schema.getSchema(nullName);
            fail("Should throw NullPointerException.");
        } catch (OperationNotSupportedException e) {
            // Expected.
        }

        try {
            schema.getSchema(new CompositeName("ldap://invalid"));
            fail("Should throw OperationNotSupportedException.");
        } catch (OperationNotSupportedException e) {
            // Expected.
        }
    }

    public void testGetSchemaClassDefinition() throws Exception {
        try {
            schema.getSchemaClassDefinition("");
            fail("Should throw OperationNotSupportedException.");
        } catch (OperationNotSupportedException e) {
            // Expected.
        }

        try {
            schema.getSchemaClassDefinition(new CompositeName());
            fail("Should throw OperationNotSupportedException.");
        } catch (OperationNotSupportedException e) {
            // Expected.
        }

        try {
            schema.getSchemaClassDefinition(new CompositeName("invalid"));
            fail("Should throw OperationNotSupportedException.");
        } catch (OperationNotSupportedException e) {
            // Expected.
        }

        try {
            String nullString = null;
            schema.getSchemaClassDefinition(nullString);
            fail("Should throw OperationNotSupportedException.");
        } catch (OperationNotSupportedException e) {
            // Expected.
        }

        try {
            Name nullName = null;
            schema.getSchemaClassDefinition(nullName);
            fail("Should throw NullPointerException.");
        } catch (OperationNotSupportedException e) {
            // Expected.
        }

        try {
            schema
                    .getSchemaClassDefinition(new CompositeName(
                            "ldap://invalid"));
            fail("Should throw OperationNotSupportedException.");
        } catch (OperationNotSupportedException e) {
            // Expected.
        }
    }

    public void testGetAttributes() throws Exception {
        // "" as parameter.
        Attributes attrs = schema.getAttributes("");
        NamingEnumeration<? extends Attribute> namingEnum = attrs.getAll();
        assertFalse(namingEnum.hasMore());

        attrs = schema.getAttributes("classdefinition/javaclass");

        Attributes attrs2 = schema.getAttributes("classdefinition/javaclass");
        assertNotSame(attrs, attrs2);
        assertEquals(attrs.toString(), attrs2.toString());
        namingEnum = attrs.getAll();

        Attribute attr;
        NamingEnumeration<?> attrEnum;
        int count = 0;
        while (namingEnum.hasMore()) {
            attr = namingEnum.next();
            attrEnum = attr.getAll();
            while (attrEnum.hasMore()) {
                Object o = attrEnum.next();
                count++;
            }

        }
        assertEquals(7, count);

        // "matchingrule/integerorderingmatch" as parameter.
        attrs = schema.getAttributes("matchingrule/integerorderingmatch");
        namingEnum = attrs.getAll();

        count = 0;
        while (namingEnum.hasMore()) {
            attr = namingEnum.next();
            attrEnum = attr.getAll();
            while (attrEnum.hasMore()) {
                Object o = attrEnum.next();
                count++;
            }

        }
        assertEquals(4, count);

        // "matchingrule" as parameter.
        attrs = schema.getAttributes("matchingrule");
        namingEnum = attrs.getAll();

        count = 0;
        while (namingEnum.hasMore()) {
            attr = namingEnum.next();
            attrEnum = attr.getAll();
            while (attrEnum.hasMore()) {
                Object o = attrEnum.next();
                count++;
            }

        }
        assertEquals(1, count);

        // "syntaxdefinition/1.3.6.1.4.1.1466.115.121.1.19" as parameter.
        attrs = schema
                .getAttributes("syntaxdefinition/1.3.6.1.4.1.1466.115.121.1.19");
        namingEnum = attrs.getAll();

        count = 0;
        while (namingEnum.hasMore()) {
            attr = namingEnum.next();
            attrEnum = attr.getAll();
            while (attrEnum.hasMore()) {
                Object o = attrEnum.next();
                count++;
            }

        }
        assertEquals(3, count);
    }

    public void testGetAttributes_Exception() throws Exception {
        Attributes attrs;
        Attribute attr;
        NamingEnumeration<? extends Attribute> namingEnum;
        NamingEnumeration<?> attrEnum;

        try {
            attrs = schema.getAttributes("classdefinition/rfc822localpart/may");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        // Invalid format.
        try {
            attrs = schema.getAttributes("classdefinition\rfc822localpart");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            attrs = schema.getAttributes("invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            attrs = schema.getAttributes("invalid/invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            attrs = schema.getAttributes("classdefinition/invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            attrs = schema
                    .getAttributes("ldap://localhost:10389/dc=example,dc=com");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            attrs = schema.getAttributes("ldap://localhost:10389");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void testGetAttributes_NullPara() throws Exception {
        Attributes attrs;
        String nullString = null;
        Name nullName = null;

        try {
            attrs = schema.getAttributes(nullString);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // Expected.
        }
        try {
            attrs = schema.getAttributes(nullName);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // Expected.
        }
    }

    public void testListBindings() throws Exception {
        // "" as parameter.
        NamingEnumeration<Binding> bindings = schema.listBindings("");
        Binding binding;
        ArrayList<String> verifyList = new ArrayList<String>();
        verifyList.add("classdefinition");
        verifyList.add("attributedefinition");
        verifyList.add("matchingrule");
        verifyList.add("syntaxdefinition");
        while (bindings.hasMore()) {
            binding = bindings.next();
            assertTrue(verifyList.remove(binding.getName().toLowerCase()));
        }
        assertEquals(0, verifyList.size());

        // "matchingrule" as parameter.
        bindings = schema.listBindings("matchingrule");
        int count = 0;
        while (bindings.hasMore()) {
            binding = bindings.next();
            count++;
        }
        assertEquals(2, count);

        // "syntaxdefinition" as parameter.
        bindings = schema.listBindings("syntaxdefinition");
        count = 0;
        while (bindings.hasMore()) {
            binding = bindings.next();
            count++;
        }
        assertEquals(1, count);

        // "classdefinition" as parameter.
        bindings = schema.listBindings("classdefinition");
        count = 0;
        while (bindings.hasMore()) {
            binding = bindings.next();
            count++;
        }
        assertEquals(3, count);

        // "attributedefinition" as parameter.
        bindings = schema.listBindings("attributedefinition");
        count = 0;
        while (bindings.hasMore()) {
            binding = bindings.next();
            count++;
        }
        assertEquals(1, count);

        // "classdefinition/javaClass" as parameter.
        bindings = schema.listBindings("classdefinition/javaClass");
        assertFalse(bindings.hasMore());
    }

    public void testListBindings_Exception() throws Exception {
        NamingEnumeration<Binding> bindings;
        try {
            bindings = schema.listBindings("invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            bindings = schema.listBindings("invalid/invalid/invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            bindings = schema.listBindings("invalid/invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            bindings = schema.listBindings("classdefinition/invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            bindings = schema.listBindings("classdefinition/javaClass/name");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            bindings = schema.listBindings("classdefinition/javaClass/invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void testListBindings_NullPara() throws Exception {
        String nullString = null;
        Name nullName = null;
        try {
            schema.listBindings(nullString);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // Expected.
        }

        try {
            schema.listBindings(nullName);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // Expected.
        }
    }

    public void testLookup() throws Exception {
        DirContext subSchema = (DirContext) schema.lookup("");
        assertSame(schema, subSchema);

        subSchema = (DirContext) schema.lookup("classdefinition");
        assertNotSame(schema, subSchema);
        NamingEnumeration<NameClassPair> namingEnum = subSchema.list("");
        NameClassPair pair;
        int count = 0;
        while (namingEnum.hasMore()) {
            pair = namingEnum.next();
            count++;
        }
        assertEquals(3, count);

        assertSame(subSchema, schema.lookup("classdefinition"));

        DirContext oldSchema = subSchema;
        subSchema = (DirContext) schema.lookup("syntaxdefinition");
        assertNotSame(oldSchema, subSchema);
        namingEnum = subSchema.list("");
        count = 0;
        while (namingEnum.hasMore()) {
            pair = namingEnum.next();
            count++;
        }
        assertEquals(1, count);

        subSchema = (DirContext) schema.lookup("classdefinition/jAvaClass");
        namingEnum = subSchema.list("");
        count = 0;
        while (namingEnum.hasMore()) {
            pair = namingEnum.next();
            count++;
        }
        assertEquals(0, count);

    }

    public void testLookup_Exception() throws Exception {
        try {
            schema.lookup("invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.lookup("invalid/invalid/invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.lookup("invalid/invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.lookup("classdefinition/invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.lookup("classdefinition/javaClass/name");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.lookup("classdefinition/javaClass/invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void testAttributes_SubSchema() throws Exception {
        DirContext subSchema = (DirContext) schema
                .lookup("classdefinition/javaClass");
        Attributes attrs = subSchema.getAttributes("");
        NamingEnumeration<? extends Attribute> namingEnum = attrs.getAll();

        Attribute attr;
        NamingEnumeration<?> attrEnum;
        int count = 0;
        while (namingEnum.hasMore()) {
            attr = namingEnum.next();
            attrEnum = attr.getAll();
            while (attrEnum.hasMore()) {
                Object o = attrEnum.next();
                count++;
            }

        }
        assertEquals(7, count);

        subSchema = (DirContext) schema.lookup("classdefinition");
        attrs = subSchema.getAttributes("");
        namingEnum = attrs.getAll();

        count = 0;
        assertTrue(namingEnum.hasMore());
        while (namingEnum.hasMore()) {
            attr = namingEnum.next();
            assertEquals("objectclass", attr.getID());
            attrEnum = attr.getAll();
            while (attrEnum.hasMore()) {
                Object o = attrEnum.next();
                assertEquals("classdefinition", o.toString().toLowerCase());
                count++;
            }

        }
        assertEquals(1, count);
    }

    public void testRemoveAttributes_DESC() throws NamingException {
        // Creates a new schema.
        Attributes attrs = new BasicAttributes(true); // Ignore case
        attrs.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.88.11");
        attrs.put("NAME", "MMObjectClass");
        attrs.put("DESC", "for test");
        attrs.put("SUP", "top");
        attrs.put("STRUCTURAL", "true");
        Attribute must = new BasicAttribute("MUST", "cn");
        must.add("objectclass");
        attrs.put(must);

        DirContext subSchema = schema.createSubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"), attrs);

        Attributes newAttributes = new BasicAttributes();
        newAttributes.put("DESC", "for test");

        schema.modifyAttributes("ClassDefinition/MMObjectClass",
                DirContext.REMOVE_ATTRIBUTE, newAttributes);

        attrs = schema.getAttributes("ClassDefinition/MMObjectClass");
        assertNull(attrs.get("DESC"));
        schema.destroySubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"));
        try {
            schema.list("ClassDefinition/MMObjectClass");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void testRemoveAttributes_DESC2() throws NamingException {
        // Creates a new schema.
        Attributes attrs = new BasicAttributes(true); // Ignore case
        attrs.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.88.11");
        attrs.put("NAME", "MMObjectClass");
        attrs.put("DESC", "for test");
        attrs.put("SUP", "top");
        attrs.put("STRUCTURAL", "true");
        Attribute must = new BasicAttribute("MUST", "cn");
        must.add("objectclass");
        attrs.put(must);

        DirContext subSchema = schema.createSubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"), attrs);

        Attributes newAttributes = new BasicAttributes();
        newAttributes.put("DESC", "different desc");

        schema.modifyAttributes("ClassDefinition/MMObjectClass",
                DirContext.REMOVE_ATTRIBUTE, newAttributes);

        attrs = schema.getAttributes("ClassDefinition/MMObjectClass");
        Attribute descAttr = attrs.get("DESC");

        assertEquals("DESC", descAttr.getID());
        assertEquals("for test", descAttr.get());
        schema.destroySubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"));
        try {
            schema.list("ClassDefinition/MMObjectClass");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void testRemoveAttributes_Name() throws NamingException {
        // Creates a new schema.
        Attributes attrs = new BasicAttributes(true); // Ignore case
        attrs.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.88.11");
        attrs.put("NAME", "MMObjectClass");
        attrs.put("DESC", "for test");
        attrs.put("SUP", "top");
        attrs.put("STRUCTURAL", "true");
        Attribute must = new BasicAttribute("MUST", "cn");
        must.add("objectclass");
        attrs.put(must);

        DirContext subSchema = schema.createSubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"), attrs);
        Attributes newAttributes = new BasicAttributes();
        newAttributes.put("NAME", "MMObjectClass");

        schema.modifyAttributes("ClassDefinition/MMObjectClass",
                DirContext.REMOVE_ATTRIBUTE, newAttributes);

        attrs = schema.getAttributes("ClassDefinition/MMObjectClass");
        assertNull(attrs.get("NAME"));
        schema.destroySubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"));
        try {
            schema.list("ClassDefinition/MMObjectClass");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void testRemoveAttributes_NUMERICOID() throws NamingException {
        // Creates a new schema.
        Attributes attrs = new BasicAttributes(true); // Ignore case
        attrs.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.88.11");
        attrs.put("NAME", "MMObjectClass");
        attrs.put("DESC", "for test");
        attrs.put("SUP", "top");
        attrs.put("STRUCTURAL", "true");
        Attribute must = new BasicAttribute("MUST", "cn");
        must.add("objectclass");
        attrs.put(must);

        DirContext subSchema = schema.createSubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"), attrs);
        Attributes newAttributes = new BasicAttributes();
        newAttributes.put("NUMERICOID", "test");

        schema.modifyAttributes("ClassDefinition/MMObjectClass",
                DirContext.REMOVE_ATTRIBUTE, newAttributes);

        attrs = schema.getAttributes("ClassDefinition/MMObjectClass");
        assertNotNull(attrs.get("NUMERICOID"));
        schema.destroySubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"));
        try {
            schema.list("ClassDefinition/MMObjectClass");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void testRemoveAttributes_Multiple() throws NamingException {
        // Creates a new schema.
        Attributes attrs = new BasicAttributes(true); // Ignore case
        attrs.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.88.11");
        attrs.put("NAME", "MMObjectClass");
        attrs.put("DESC", "for test");
        attrs.put("SUP", "top");
        attrs.put("STRUCTURAL", "true");
        Attribute must = new BasicAttribute("MUST", "cn");
        must.add("objectclass");
        attrs.put(must);

        DirContext subSchema = schema.createSubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"), attrs);

        Attributes newAttributes = new BasicAttributes();
        newAttributes.put("DESC", "for test");
        newAttributes.put("SUP", "top");
        schema.modifyAttributes("ClassDefinition/MMObjectClass",
                DirContext.REMOVE_ATTRIBUTE, newAttributes);

        attrs = schema.getAttributes("ClassDefinition/MMObjectClass");
        assertNull(attrs.get("DESC"));
        assertNull(attrs.get("SUP"));
        schema.destroySubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"));
        try {
            schema.list("ClassDefinition/MMObjectClass");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void testRemoveAttributes_MultipleValue() throws NamingException {
        // Creates a new schema.
        Attributes attrs = new BasicAttributes(true); // Ignore case
        attrs.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.88.11");
        attrs.put("NAME", "MMObjectClass");
        attrs.put("DESC", "for test");
        attrs.put("SUP", "top");
        attrs.put("STRUCTURAL", "true");
        Attribute must = new BasicAttribute("MUST", "cn");
        must.add("objectclass");
        attrs.put(must);

        DirContext subSchema = schema.createSubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"), attrs);
        Attributes newAttributes = new BasicAttributes();
        newAttributes.put("MUST", "cn");
        schema.modifyAttributes("ClassDefinition/MMObjectClass",
                DirContext.REMOVE_ATTRIBUTE, newAttributes);

        attrs = schema.getAttributes("ClassDefinition/MMObjectClass");
        Attribute attr = attrs.get("must");
        Enumeration enu = attr.getAll();
        assertEquals("objectclass", enu.nextElement());
        assertFalse(enu.hasMoreElements());
        schema.destroySubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"));
        try {
            schema.list("ClassDefinition/MMObjectClass");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void testRemoveAttributes_MultipleValue2() throws NamingException {
        // Creates a new schema.
        Attributes attrs = new BasicAttributes(true); // Ignore case
        attrs.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.88.11");
        attrs.put("NAME", "MMObjectClass");
        attrs.put("DESC", "for test");
        attrs.put("SUP", "top");
        attrs.put("STRUCTURAL", "true");
        Attribute must = new BasicAttribute("MUST", "cn");
        must.add("objectclass");
        attrs.put(must);

        DirContext subSchema = schema.createSubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"), attrs);
        Attributes newAttributes = new BasicAttributes();
        newAttributes.put(must);
        schema.modifyAttributes("ClassDefinition/MMObjectClass",
                DirContext.REMOVE_ATTRIBUTE, newAttributes);

        attrs = schema.getAttributes("ClassDefinition/MMObjectClass");
        assertNull(attrs.get("must"));
        schema.destroySubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"));
        try {
            schema.list("ClassDefinition/MMObjectClass");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void testAddAttributes_DESC() throws NamingException {
        // Creates a new schema.
        Attributes attrs = new BasicAttributes(true); // Ignore case
        attrs.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.88.11");
        attrs.put("NAME", "MMObjectClass");
        attrs.put("SUP", "top");
        attrs.put("STRUCTURAL", "true");
        Attribute must = new BasicAttribute("MUST", "cn");
        must.add("objectclass");
        attrs.put(must);

        DirContext subSchema = schema.createSubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"), attrs);
        Attributes newAttributes = new BasicAttributes();
        newAttributes.put("DESC", "for test");
        schema.modifyAttributes("ClassDefinition/MMObjectClass",
                DirContext.ADD_ATTRIBUTE, newAttributes);

        attrs = schema.getAttributes("ClassDefinition/MMObjectClass");
        Attribute descAttr = attrs.get("DESC");

        assertEquals("DESC", descAttr.getID());
        assertEquals("for test", descAttr.get());
        schema.destroySubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"));
        try {
            schema.list("ClassDefinition/MMObjectClass");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void testAddAttributes_MultipleMust() throws NamingException {
        // Creates a new schema.
        Attributes attrs = new BasicAttributes(true); // Ignore case
        attrs.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.88.11");
        attrs.put("NAME", "MMObjectClass");
        attrs.put("DESC", "for test");
        attrs.put("SUP", "top");
        attrs.put("STRUCTURAL", "true");
        Attribute must = new BasicAttribute("MUST", "cn");
        attrs.put(must);

        DirContext subSchema = schema.createSubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"), attrs);
        Attributes newAttributes = new BasicAttributes();
        newAttributes.put("MUST", "objectclass");

        schema.modifyAttributes("ClassDefinition/MMObjectClass",
                DirContext.ADD_ATTRIBUTE, newAttributes);

        attrs = schema.getAttributes("ClassDefinition/MMObjectClass");
        Attribute mustAttr = attrs.get("MUST");
        assertEquals(2, mustAttr.size());
        schema.destroySubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"));
        try {
            schema.list("ClassDefinition/MMObjectClass");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void testAddAttributes_DuplicateDESC() throws NamingException {
        // Creates a new schema.
        Attributes attrs = new BasicAttributes(true); // Ignore case
        attrs.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.88.11");
        attrs.put("NAME", "MMObjectClass");
        attrs.put("DESC", "for test");
        attrs.put("SUP", "top");
        attrs.put("STRUCTURAL", "true");
        Attribute must = new BasicAttribute("MUST", "cn");
        must.add("objectclass");
        attrs.put(must);

        DirContext subSchema = schema.createSubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"), attrs);
        Attributes newAttributes = new BasicAttributes();
        newAttributes.put("DESC", "for test");

        schema.modifyAttributes("ClassDefinition/MMObjectClass",
                DirContext.ADD_ATTRIBUTE, newAttributes);

        attrs = schema.getAttributes("ClassDefinition/MMObjectClass");
        Attribute descAttr = attrs.get("DESC");
        assertEquals(1, descAttr.size());
        schema.destroySubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"));
        try {
            schema.list("ClassDefinition/MMObjectClass");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void testReplaceAttributes() throws NamingException {
        // Creates a new schema.
        Attributes attrs = new BasicAttributes(true); // Ignore case
        attrs.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.88.11");
        attrs.put("NAME", "MMObjectClass");
        attrs.put("DESC", "for test");
        attrs.put("SUP", "top");
        attrs.put("STRUCTURAL", "true");
        Attribute must = new BasicAttribute("MUST", "cn");
        must.add("objectclass");
        attrs.put(must);

        DirContext subSchema = schema.createSubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"), attrs);
        Attributes newAttributes = new BasicAttributes();
        newAttributes.put("DESC", "modifed test desc");
        schema.modifyAttributes("ClassDefinition/MMObjectClass",
                DirContext.REPLACE_ATTRIBUTE, newAttributes);
        attrs = schema.getAttributes("ClassDefinition/MMObjectClass");
        Attribute descAttr = attrs.get("DESC");

        assertEquals("DESC", descAttr.getID());
        assertEquals("modifed test desc", descAttr.get());

        schema.destroySubcontext(new CompositeName(
                "ClassDefinition/MMObjectClass"));
        try {
            schema.list("ClassDefinition/MMObjectClass");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void testModifyAttributes_WatchSubSchema() throws NamingException {
        // Creates the attributes.
        Attributes attrs = new BasicAttributes(false); // Ignore case
        attrs.put("NAME", "ListObjectClass");
        attrs.put("SUP", "top");
        attrs.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.88.77");
        attrs.put("DESC", "for test");
        attrs.put("STRUCTURAL", "fds");

        Attribute must = new BasicAttribute("MUST", "cn");
        must.add("objectclass");
        attrs.put(must);

        DirContext dir = schema.createSubcontext(new CompositeName(
                "ClassDefinition/ListObjectClass"), attrs);

        Attributes newAttrs = new BasicAttributes(false);
        newAttrs.put("NAME", "Modified");
        newAttrs.put("SUP", "top");
        newAttrs.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.88.77");
        newAttrs.put("DESC", "for test");
        newAttrs.put("STRUCTURAL", "fds");

        schema.modifyAttributes("ClassDefinition/ListObjectClass",
                DirContext.REPLACE_ATTRIBUTE, newAttrs);

        Attributes subSchemaAttrs = dir.getAttributes("");
        assertEquals("Modified", subSchemaAttrs.get("NAME").get());
    }

    public void testModifyAttributes_Exception() throws NamingException {
        Attributes attrs = new BasicAttributes(true); // Ignore case
        attrs.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.88.11");
        attrs.put("NAME", "MMObjectClass");
        attrs.put("DESC", "for test");
        attrs.put("SUP", "top");
        attrs.put("STRUCTURAL", "true");
        Attribute must = new BasicAttribute("MUST", "cn");
        must.add("objectclass");
        attrs.put(must);

        try {
            schema.modifyAttributes("invalid", null);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.modifyAttributes("invalid/invalid/invalid", null);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.modifyAttributes("invalid/invalid", null);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.modifyAttributes("classdefinition/invalid", null);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.modifyAttributes("classdefinition/javaClass/name", null);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.modifyAttributes("classdefinition/javaClass/invalid", null);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.modifyAttributes("", new ModificationItem[] {});
            fail("Should throw SchemaViolationException");
        } catch (SchemaViolationException e) {
            // expected
        }

        try {
            schema.modifyAttributes(new CompositeName(""),
                    new ModificationItem[] {});
            fail("Should throw SchemaViolationException");
        } catch (SchemaViolationException e) {
            // expected
        }
    }

    public void testCreateAndDeleteSubContext() throws NamingException {
        // Creates the attributes.
        Attributes attrs = new BasicAttributes(false); // Ignore case
        attrs.put("NAME", "ListObjectClass");
        attrs.put("SUP", "top");
        attrs.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.88.77");
        attrs.put("DESC", "for test");
        attrs.put("STRUCTURAL", "fds");

        Attribute must = new BasicAttribute("MUST", "cn");
        must.add("objectclass");
        attrs.put(must);

        DirContext dir = schema.createSubcontext(new CompositeName(
                "ClassDefinition/ListObjectClass"), attrs);

        Attributes createdAttrs = schema
                .getAttributes("ClassDefinition/ListObjectClass");

        NamingEnumeration<? extends Attribute> enumeration = createdAttrs
                .getAll();

        int count = 0;
        while (enumeration.hasMore()) {
            Attribute att = enumeration.next();
            count++;
        }
        assertEquals(6, count);

        schema.destroySubcontext("ClassDefinition/ListObjectClass");
        try {
            schema.getAttributes("ClassDefinition/ListObjectClass");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void testDestroySubContext_Exception() throws NamingException {
        // No Exception.
        schema.destroySubcontext("invalid");

        try {
            schema.destroySubcontext("invalid/invalid/invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.destroySubcontext("invalid/invalid");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        // No Exception.
        schema.destroySubcontext("classdefinition/invalid");

        // No Exception.
        schema.destroySubcontext("classdefinition/javaClass/name");

        // No Exception.
        schema.destroySubcontext("classdefinition/javaClass/invalid");

        try {
            schema.destroySubcontext("");
            fail("Should throw ArrayIndexOutOfBoundsException.");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Expected.
        }
        try {
            schema.destroySubcontext("classdefinition");
            fail("Should throw SchemaViolationException.");
        } catch (SchemaViolationException e) {
            // Expected.
        }
    }

    public void testCreateSubContext_Exception() throws NamingException {
        // Creates the attributes.
        Attributes attrs = new BasicAttributes(false); // Ignore case
        attrs.put("NAME", "ListObjectClass");
        attrs.put("SUP", "top");
        attrs.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.88.77");
        attrs.put("DESC", "for test");
        attrs.put("STRUCTURAL", "fds");

        Attribute must = new BasicAttribute("MUST", "cn");
        must.add("objectclass");
        attrs.put(must);

        Attributes invalidAttrs = new BasicAttributes();

        try {
            schema.createSubcontext(new CompositeName("invalid"), attrs);
            fail("Should throw SchemaViolationException.");
        } catch (SchemaViolationException e) {
            // Expected.
        }

        try {
            schema.createSubcontext(new CompositeName("invalid"), invalidAttrs);
            fail("Should throw SchemaViolationException.");
        } catch (SchemaViolationException e) {
            // Expected.
        }

        try {
            schema.createSubcontext(
                    new CompositeName("invalid/invalid/invalid"), attrs);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.createSubcontext(
                    new CompositeName("invalid/invalid/invalid"), invalidAttrs);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema
                    .createSubcontext(new CompositeName("Classdefinition"),
                            attrs);
            fail("Should throw SchemaViolationException.");
        } catch (SchemaViolationException e) {
            // Expected.
        }
    }

    public void testSubContext_OnSubSchema() throws NamingException {
        DirContext subSchema = (DirContext) schema.lookup("classdefinition");

        // Creates the attributes.
        Attributes attrs = new BasicAttributes(false); // Ignore case
        attrs.put("NAME", "ListObjectClass");
        attrs.put("SUP", "top");
        attrs.put("NUMERICOID", "1.3.6.1.4.1.42.2.27.4.2.3.1.88.77");
        attrs.put("DESC", "for test");
        attrs.put("STRUCTURAL", "fds");

        Attribute must = new BasicAttribute("MUST", "cn");
        must.add("objectclass");
        attrs.put(must);

        DirContext dir = subSchema.createSubcontext(new CompositeName(
                "ListObjectClass"), attrs);

        Attributes createdAttrs = dir.getAttributes("");

        NamingEnumeration<? extends Attribute> enumeration = createdAttrs
                .getAll();

        int count = 0;
        while (enumeration.hasMore()) {
            Attribute att = enumeration.next();
            count++;
        }
        assertEquals(6, count);

        subSchema.destroySubcontext("ListObjectClass");

        try {
            schema.getAttributes("ClassDefinition/ListObjectClass");
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void test_CreateSubcontext_LName_LAttributes()
            throws NamingException {
        try {
            schema.createSubcontext((Name) null, new BasicAttributes());
            fail("Should throw NPE");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            schema.createSubcontext(new CompositeName(""), null);
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
        try {
            schema.createSubcontext(new CompositeName("test"), null);
            fail("Should throw SchemaViolationException");
        } catch (SchemaViolationException e) {
            // expected
        }
        try {
            schema.createSubcontext(new CompositeName("test"),
                    new BasicAttributes());
            fail("Should throw SchemaViolationException");
        } catch (SchemaViolationException e) {
            // expected
        }
    }

    public void test_modifyAttributes_LString_LModificationItem()
            throws NamingException {
        try {
            schema.modifyAttributes((String) null, new ModificationItem[] {});
            fail("Should throw NPE");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            schema.modifyAttributes("AttributeDefinition/dsaquality", null);
            fail("Should throw NPE");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void test_search_LString_LAttributes_LString()
            throws NamingException {
        try {
            schema
                    .search((String) null, new BasicAttributes(),
                            new String[] {});
            fail("Should throw NPE");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testDestroySubcontextString() throws NamingException {
        try {
            schema.destroySubcontext((String) null);
            fail("Should throw NPE");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void test_getNameInNamespace() throws NamingException {
        try {
            schema.getNameInNamespace();
            fail("Should throw OperationNotSupportedException");
        } catch (OperationNotSupportedException e) {
            // expected
        }
    }

    public void test_getSchemaClassDefinition() throws NamingException {
        try {
            schema.getSchemaClassDefinition(new CompositeName(""));
            fail("Should throw OperationNotSupportedException");
        } catch (OperationNotSupportedException e) {
            // expected
        }
    }

    public void testSimpleSearch() throws NamingException {
        Attributes matchAttrs = new BasicAttributes();

        // "" as parameter.
        NamingEnumeration<SearchResult> ne = schema.search("", matchAttrs);

        ArrayList<String> verifyList = new ArrayList<String>();
        verifyList.add("ClassDefinition");
        verifyList.add("AttributeDefinition");
        verifyList.add("MatchingRule");
        verifyList.add("SyntaxDefinition");

        SearchResult result;
        int count = 0;
        int attributeCount = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertTrue(verifyList.remove(result.getName()));
            attributeCount += result.getAttributes().size();
        }
        assertEquals(4, count);
        assertEquals(4, attributeCount);

        ne = schema.search("", null);
        count = 0;
        attributeCount = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            attributeCount += result.getAttributes().size();
        }
        assertEquals(4, count);
        assertEquals(4, attributeCount);

        ne = schema.search("classdefinition", matchAttrs);

        count = 0;
        attributeCount = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            attributeCount += result.getAttributes().size();
        }
        assertEquals(3, count);
        assertEquals(18, attributeCount);

        ne = schema.search("classdefinition/javaClass", matchAttrs);
        assertFalse(ne.hasMore());
    }

    public void testSearchException() throws NamingException {
        String nullString = null;
        Name nullName = null;
        try {
            schema.search(nullString, null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // Expected.
        }

        try {
            schema.search(nullName, null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // Expected.
        }

        try {
            schema.search("invalid", null);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.search("invalid/invalid/invalid", null);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.search("invalid/invalid", null);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.search("classdefinition/invalid", null);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.search("classdefinition/javaClass/name", null);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.search("classdefinition/javaClass/invalid", null);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    public void testSearch_Filter() throws NamingException {
        SearchControls controls = new SearchControls();
        NamingEnumeration<SearchResult> ne = schema.search("",
                "(objectclass=classdefinition)", controls);

        SearchResult result;
        int count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
        }
        assertEquals(1, count);

        ne = schema.search("", "(!(objectclass=classdefinition))", controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
        }
        assertEquals(3, count);

        ne = schema.search("",
                "(|(objectclass=classdefinition)(objectclass=matchingrule))",
                controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
        }
        assertEquals(2, count);

        controls.setSearchScope(SearchControls.OBJECT_SCOPE);
        controls.setCountLimit(5);
        ne = schema.search("classdefinition", "(objectclass~=classdefinition)",
                controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
        }
        assertEquals(1, count);
    }

    public void testSearch_matchAttributes() throws NamingException {
        Attributes matchAttrs = new BasicAttributes();
        NamingEnumeration<SearchResult> ne = schema.search("", matchAttrs);

        Attributes returnedAttributes;
        SearchResult result;
        int count = 0;

        // TODO
        // The problem of ldap-jndi conversion
        // There are too many places to handle the case-sensitive problem.
        matchAttrs.put("obJectclass", "ClassDefinition");
        ne = schema.search("", matchAttrs);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
        }
        assertEquals(1, count);

        matchAttrs = new BasicAttributes(true);
        matchAttrs.put("obJectclass", "ClasSDefinition");
        ne = schema.search("", matchAttrs);
        assertFalse(ne.hasMore());

        matchAttrs.put("invalid", "ClassDefinition");
        ne = schema.search("", matchAttrs);
        assertFalse(ne.hasMore());
    }

    public void testSearch_AttributesToReturn() throws NamingException {
        String[] attributesToReturn = new String[] { "objecTClass" };
        NamingEnumeration<SearchResult> ne = schema.search("", null,
                attributesToReturn);

        Attributes returnedAttributes;
        SearchResult result;
        int count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            returnedAttributes = result.getAttributes();
            assertEquals(1, returnedAttributes.size());
            count++;
        }
        assertEquals(4, count);

        attributesToReturn = new String[] { "objecTClass", "invalid" };
        ne = schema.search("", null, attributesToReturn);

        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            returnedAttributes = result.getAttributes();
            assertEquals(1, returnedAttributes.size());
            count++;
        }
        assertEquals(4, count);

        attributesToReturn = new String[] { "invalid", "invalid2" };
        ne = schema.search("", null, attributesToReturn);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            returnedAttributes = result.getAttributes();
            assertEquals(0, returnedAttributes.size());
            // System.out.println(result);
            count++;
        }
        assertEquals(4, count);

        attributesToReturn = new String[] {};
        ne = schema.search("", null, attributesToReturn);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            returnedAttributes = result.getAttributes();
            assertEquals(0, returnedAttributes.size());
            count++;
        }
        assertEquals(4, count);

        attributesToReturn = new String[] { "name" };
        ne = schema.search("classdefinition", null, attributesToReturn);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            returnedAttributes = result.getAttributes();
            assertEquals(1, returnedAttributes.size());
            count++;
        }
        assertEquals(3, count);

        attributesToReturn = new String[] { "name" };
        ne = schema.search("classdefinition/javaClass", null,
                attributesToReturn);
        assertFalse(ne.hasMore());

        attributesToReturn = new String[] { "objecTClass", "invalid", null };
        ne = schema.search("", null, attributesToReturn);

        count = 0;

        try {
            // No-bug difference.
            // RI will throw NullPointerException.
            while (ne.hasMore()) {
                result = ne.next();
                returnedAttributes = result.getAttributes();
                assertEquals(1, returnedAttributes.size());
                count++;
            }
            assertEquals(4, count);
        }

        catch (NullPointerException e) {
            // Expected.
        }
    }

    public void testSearch_Filter2() throws NamingException {
        ArrayList<String> verifyList = new ArrayList<String>();
        SearchControls controls = new SearchControls();
        NamingEnumeration<SearchResult> ne = schema.search("",
                "(objectclass=classdefinition)", controls);

        SearchResult result;
        int count = 0;
        verifyList.add("ClassDefinition");
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertTrue(verifyList.remove(result.getName()));
        }
        assertEquals(1, count);

        verifyList.add("SyntaxDefinition");
        verifyList.add("AttributeDefinition");
        verifyList.add("MatchingRule");
        ne = schema.search("", "(!(objectclass=classdefinition))", controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertTrue(verifyList.remove(result.getName()));
        }
        assertEquals(3, count);

        verifyList.add("MatchingRule");
        verifyList.add("ClassDefinition");
        ne = schema.search("",
                "(|(objectclass=classdefinition)(objectclass=matchingrule))",
                controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertTrue(verifyList.remove(result.getName()));
        }
        assertEquals(2, count);

        verifyList.add("ClassDefinition");
        ne = schema
                .search(
                        "",
                        "(&(objectclass=classdefinition)(!(objectclass=matchingrule)))",
                        controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertTrue(verifyList.remove(result.getName()));
        }
        assertEquals(1, count);

        ne = schema.search("", "(objectclass=*)", controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
        }
        assertEquals(4, count);

        verifyList.add("SyntaxDefinition");
        verifyList.add("ClassDefinition");
        ne = schema.search("", "(objectclass=*s*defi*)", controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertTrue(verifyList.remove(result.getName()));
        }
        assertEquals(2, count);

        verifyList.add("SyntaxDefinition");
        ne = schema.search("", "(objectclass=s*defi*)", controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertTrue(verifyList.remove(result.getName()));
        }
        assertEquals(1, count);

        ne = schema.search("", "(objectclass~=sdefi)", controls);
        assertFalse(ne.hasMore());

        ne = schema.search("", "(objectclass<=a)", controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
        }
        assertEquals(4, count);

        verifyList.add("MatchingRule");
        verifyList.add("SyntaxDefinition");
        ne = schema.search("", "(objectclass>=M)", controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertTrue(verifyList.remove(result.getName()));
        }
        assertEquals(2, count);
        
        ne = schema.search("",
                "(|(!(objectclass=classdefinition))(!(objectclass=matchingrule)))",
                controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
        }
        assertEquals(4, count);
    }

    public void testSearch_Subtree() throws NamingException {
        addMoreSchemaData();

        ArrayList<String> verifyList = new ArrayList<String>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        NamingEnumeration<SearchResult> ne = schema.search("", "(must=cn)",
                controls);

        SearchResult result;
        int count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertTrue(result.getName().startsWith("ClassDefinition"));
            assertTrue(result.getAttributes().get("must").contains("cn"));
        }
        assertEquals(3, count);

        ne = schema.search("", "(x-schema=*)", controls);

        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertTrue(result.getName().contains("/"));
            assertNotNull(result.getAttributes().get("x-schema"));
        }
        assertEquals(10, count);

        // Nonexist attributename;
        ne = schema.search("", "(schema=*)", controls);
        assertFalse(ne.hasMore());
    }

    public void testSearch_ReturnAttributes() throws NamingException {
        addMoreSchemaData();
        ArrayList<String> verifyList = new ArrayList<String>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(new String[] {});
        NamingEnumeration<SearchResult> ne = schema.search("", "(must=cn)",
                controls);

        SearchResult result;
        int count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertEquals(0, result.getAttributes().size());
        }
        assertEquals(3, count);

        controls.setReturningAttributes(new String[] { "may" });
        ne = schema.search("", "(&(mUst=cn)(maY=*))", controls);

        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertEquals(1, result.getAttributes().size());
            assertNotNull(result.getAttributes().get("MAY"));
        }
        assertEquals(3, count);
    }

    public void testFilterSearchException() throws NamingException {
        SearchControls controls = new SearchControls();
        try {
            schema.search("", "", controls);
            fail("Should throw StringIndexOutOfBoundsException");
        } catch (InvalidSearchFilterException e) {
            // Excpected.
        } catch (StringIndexOutOfBoundsException e) {
            // RI's problem.
        }

        try {
            schema.search("invalid", "invalid", controls);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.search("invalid/invalid/invalid", "invalid", controls);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.search("invalid/invalid", "invalid", controls);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.search("classdefinition/invalid", "invalid", controls);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema
                    .search("classdefinition/javaClass/name", "invalid",
                            controls);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        try {
            schema.search("classdefinition/javaClass/invalid", "invalid",
                    controls);
            fail("Should throw NameNotFoundException.");
        } catch (NameNotFoundException e) {
            // Expected.
        }
    }

    private void addMoreSchemaData() throws InvalidNameException {
        // Add more schema data.
        Hashtable subschemaTable = (Hashtable) schemaTable.get("objectclasses");

        subschemaTable
                .put(
                        "applicationprocess",
                        "( 2.5.6.11 name 'applicationprocess' "
                                + "desc 'rfc2256: an application process' "
                                + "sup top structural "
                                + "must cn may ( seealso $ ou $ l $ description ) x-schema 'core' )");

        subschemaTable
                .put(
                        "documentseries",
                        "( 0.9.2342.19200300.100.4.9 name 'documentseries' "
                                + "sup top structural must cn "
                                + "may ( description $ seealso $ telephonenumber $ l $ o $ ou ) "
                                + "x-schema 'cosine' )");

        subschemaTable
                .put(
                        "groupofuniquenames",
                        "( 2.5.6.17 name 'groupofuniquenames' "
                                + "desc 'rfc2256: a group of unique names (dn and unique identifier)' "
                                + "sup top structural must ( uniquemember $ cn ) "
                                + "may ( businesscategory $ seealso $ owner $ ou $ o $ description ) x-schema 'core' )");
        schema = new LdapSchemaContextImpl(context, null, name, schemaTable,
                LdapSchemaContextImpl.SCHEMA_ROOT_LEVEL);
    }

    public void testSearch_FilterWithArgs() throws NamingException {
        ArrayList<String> verifyList = new ArrayList<String>();
        SearchControls controls = new SearchControls();
        Object[] filterArgs = new Object[] { "ClassDeFInition" };
        NamingEnumeration<SearchResult> ne = schema.search("",
                "(objectclass={0})", filterArgs, controls);

        SearchResult result;
        int count = 0;
        verifyList.add("ClassDefinition");
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertTrue(verifyList.remove(result.getName()));
        }
        assertEquals(1, count);

        verifyList.add("SyntaxDefinition");
        verifyList.add("AttributeDefinition");
        verifyList.add("MatchingRule");
        filterArgs = new Object[] { "ClassDeFInition" };
        ne = schema.search("", "(!(objectclass={0}))", filterArgs, controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertTrue(verifyList.remove(result.getName()));
        }
        assertEquals(3, count);

        verifyList.add("MatchingRule");
        verifyList.add("ClassDefinition");
        filterArgs = new Object[] { "ClassDeFInition", "matchingrule" };
        ne = schema.search("", "(|(objectclass={0})(objectclass={1}))",
                filterArgs, controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertTrue(verifyList.remove(result.getName()));
        }
        assertEquals(2, count);

        verifyList.add("ClassDefinition");
        filterArgs = new Object[] { "ClassDeFInition", "matchingrule" };
        ne = schema.search("", "(&(objectclass={0})(!(objectclass={1})))",
                filterArgs, controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertTrue(verifyList.remove(result.getName()));
        }
        assertEquals(1, count);

        ne = schema.search("", "(objectclass=*)", controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
        }
        assertEquals(4, count);

        verifyList.add("SyntaxDefinition");
        verifyList.add("ClassDefinition");
        filterArgs = new Object[] { "defi" };
        ne = schema.search("", "(objectclass=*s*{0}*)", filterArgs, controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertTrue(verifyList.remove(result.getName()));
        }
        assertEquals(2, count);

        verifyList.add("SyntaxDefinition");
        filterArgs = new Object[] { "defi" };
        ne = schema.search("", "(objectclass=s*{0}*)", filterArgs, controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertTrue(verifyList.remove(result.getName()));
        }
        assertEquals(1, count);

        filterArgs = new Object[] { "sdefi" };
        ne = schema.search("", "(objectclass~={0})", filterArgs, controls);
        assertFalse(ne.hasMore());

        filterArgs = new Object[] { "a" };
        ne = schema.search("", "(objectclass<={0})", filterArgs, controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
        }
        assertEquals(4, count);

        verifyList.add("MatchingRule");
        verifyList.add("SyntaxDefinition");
        filterArgs = new Object[] { "M" };
        ne = schema.search("", "(objectclass>={0})", filterArgs, controls);
        count = 0;
        while (ne.hasMore()) {
            result = ne.next();
            count++;
            assertTrue(verifyList.remove(result.getName()));
        }
        assertEquals(2, count);
    }

    public void testRename() throws NamingException {
        Name name1 = new CompositeName("test1");
        Name name2 = new CompositeName("/");
        Name invalidName1 = new CompositeName("");
        try {
            schema.rename(name1, name2);
            fail("Should throw SchemaViolationException");
        } catch (SchemaViolationException e) {
            // Expected.
        }

        try {
            schema.rename(invalidName1, name2);
            fail("Should throw InvalidNameException");
        } catch (InvalidNameException e) {
            // Expected.
        }

        try {
            schema.rename(name2, invalidName1);
            fail("Should throw InvalidNameException");
        } catch (InvalidNameException e) {
            // Expected.
        }

        try {
            schema.rename("test1", "test2");
            fail("Should throw SchemaViolationException");
        } catch (SchemaViolationException e) {
            // Expected.
        }

        try {
            schema.rename("", "test2");
            fail("Should throw InvalidNameException");
        } catch (InvalidNameException e) {
            // Expected.
        }

        try {
            schema.rename("test1", "");
            fail("Should throw InvalidNameException");
        } catch (InvalidNameException e) {
            // Expected.
        }

        try {
            schema.rename("classdefinition/javaClass", "test");
            fail("Should throw InvalidNameException");
        } catch (InvalidNameException e) {
            // Expected.
        }

        try {
            schema.rename("classdefinition\\javaClass", "test");
            fail("Should throw SchemaViolationException");
        } catch (SchemaViolationException e) {
            // Expected.
        }

        try {
            schema.rename(new CompositeName("classdefinition/javaClass"),
                    new CompositeName("test"));
            fail("Should throw InvalidNameException");
        } catch (InvalidNameException e) {
            // Expected.
        }

        try {
            schema.rename(new CompositeName("classdefinition\\javaClass"),
                    new CompositeName("test"));
            fail("Should throw SchemaViolationException");
        } catch (SchemaViolationException e) {
            // Expected.
        }

        try {
            schema.rename("classdefinition/javaClass", "");
            fail("Should throw InvalidNameException");
        } catch (InvalidNameException e) {
            // Expected.
        }

        try {
            schema.rename("", "classdefinition/javaClass");
            fail("Should throw InvalidNameException");
        } catch (InvalidNameException e) {
            // Expected.
        }
    }

    public void testRename_Exception() throws NamingException {
        Name name = new CompositeName("test");
        Name nullName = null;
        String nullString = null;
        try {
            schema.rename(nullName, name);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected.
        }

        try {
            schema.rename(name, nullName);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected.
        }

        try {
            schema.rename(nullName, nullName);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected.
        }

        try {
            schema.rename(nullString, "test");
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected.
        }

        try {
            schema.rename("test", nullString);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected.
        }

        try {
            schema.rename(nullString, nullString);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected.
        }

        try {
            schema.rename("\\", nullString);
            fail("Should throw InvalidNameException");
        } catch (InvalidNameException e) {
            // Expected.
        }

        try {
            schema.rename("/", nullString);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected.
        }

        try {
            schema.rename(null, "");
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // Expected.
        }
    }

    public void testClassDefinition() throws NamingException {
        MockLdapSchemaContext mockSchema = new MockLdapSchemaContext(context,
                null, name, schemaTable,
                LdapSchemaContextImpl.SCHEMA_ROOT_LEVEL);
        Attribute attribute = new BasicAttribute("objectClass", "javaClass");
        attribute.add("extensibleobject");
        attribute.add("prefNode");

        DirContext classDefSchema = mockSchema.getClassDefinition(attribute);
        ArrayList<String> verifyList = new ArrayList<String>();
        verifyList.add("javaclass");
        verifyList.add("prefnode");
        verifyList.add("extensibleobject");
        NamingEnumeration<NameClassPair> ne = classDefSchema.list("");
        NameClassPair pair;
        int count = 0;
        while (ne.hasMore()) {
            pair = ne.next();
            count++;
            assertTrue(verifyList.remove(pair.getName().toLowerCase()));
        }
        assertEquals(3, count);

        ne = classDefSchema.list("prefnode");
        assertFalse(ne.hasMore());

        ne = classDefSchema.list("extensibleobject");
        assertFalse(ne.hasMore());
    }

    public void testAttributeDefinition() throws NamingException {
        addMoreAttributeData();
        MockLdapContext mockContext = new MockLdapContext(context, null, "");
        Attribute attr = new LdapAttribute("objectclass", mockContext);

        DirContext attributeDefinition = attr.getAttributeDefinition();
        NamingEnumeration<NameClassPair> ne = attributeDefinition.list("");
        assertFalse(ne.hasMore());

        try {
            ne = attributeDefinition.list("invalid");
            fail("Should throw NameNotFoundException");
        } catch (NameNotFoundException e) {
            // Expected.
        }

        Attributes schemaAttributes = attributeDefinition.getAttributes("");
        assertEquals(7, schemaAttributes.size());
        assertEquals("1.3.6.1.4.1.1466.115.121.1.38", schemaAttributes.get(
                "syntax").get());
        assertEquals("objectClass", schemaAttributes.get("name").get());
        assertEquals("2.5.4.0", schemaAttributes.get("numericoid").get());
        assertEquals("userApplications", schemaAttributes.get("usage").get());
        assertEquals("objectIdentifierMatch", schemaAttributes.get("equality")
                .get());
    }

    public void testSyntaxDefinition() throws NamingException {
        addMoreAttributeData();
        MockLdapContext mockContext = new MockLdapContext(context, null, "");
        Attribute attr = new LdapAttribute("objectclass", mockContext);
        DirContext attributeDefinition = attr.getAttributeSyntaxDefinition();
        NamingEnumeration<NameClassPair> ne = attributeDefinition.list("");
        assertFalse(ne.hasMore());

        try {
            ne = attributeDefinition.list("invalid");
            fail("Should throw NameNotFoundException");
        } catch (NameNotFoundException e) {
            // Expected.
        }
        Attributes schemaAttributes = attributeDefinition.getAttributes("");
        assertEquals(3, schemaAttributes.size());
        assertEquals("system", schemaAttributes.get("x-schema").get());
        assertEquals("true", schemaAttributes.get("x-is-human-readable").get());
        assertEquals("1.3.6.1.4.1.1466.115.121.1.38", schemaAttributes.get(
                "numericoid").get());
    }

    private void addMoreAttributeData() throws InvalidNameException {
        // Add more schema data.
        Hashtable subschemaTable = (Hashtable) schemaTable
                .get("attributetypes");

        subschemaTable
                .put(
                        "objectclass",
                        "( 2.5.4.0 NAME 'objectClass' "
                                + "DESC 'RFC2256: object classes of the entity'  "
                                + "EQUALITY objectIdentifierMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.38 "
                                + "USAGE userApplications X-SCHEMA 'system' )");

        subschemaTable = (Hashtable) schemaTable.get("ldapsyntaxes");
        subschemaTable
                .put(
                        "1.3.6.1.4.1.1466.115.121.1.38",
                        "( 1.3.6.1.4.1.1466.115.121.1.38  X-SCHEMA 'system' X-IS-HUMAN-READABLE 'true' )");
        schema = new LdapSchemaContextImpl(context, null, name, schemaTable,
                LdapSchemaContextImpl.SCHEMA_ROOT_LEVEL);
    }

    public class MockLdapContext extends LdapContextImpl {
        public MockLdapContext(LdapContextImpl context,
                Hashtable<Object, Object> environment, String dn)
                throws InvalidNameException {
            super(context, environment, dn);
        }

        public DirContext getSchema(String name) {
            return schema;
        }

        public Attributes getAttributes(Name name, String returningAttributes[])
                throws NamingException {
            Attribute attribute = new BasicAttribute("objectClass", "javaClass");
            attribute.add("extensibleobject");
            attribute.add("prefNode");
            Attributes attributes = new BasicAttributes(true);
            attributes.put(attribute);

            return attributes;
        }
    }

    public class MockLdapSchemaContext extends LdapSchemaContextImpl {

        public MockLdapSchemaContext(LdapContextImpl ctx,
                Hashtable<Object, Object> env, Name dn,
                Hashtable<String, Object> schemaTable, int level)
                throws InvalidNameException {
            super(ctx, env, dn, schemaTable, level);
        }

        public DirContext getClassDefinition(Attribute attr)
                throws NamingException {
            return super.getClassDefinition(attr);
        }
    }
}
