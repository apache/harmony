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

package org.apache.harmony.sql.tests.internal.rowset;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringWriter;

import javax.sql.rowset.WebRowSet;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.w3c.dom.Document;

public class XmlReaderTest extends TestCase {

    public final String XML_SRC_URL_RI = "resources/org/apache/harmony/sql/internal/rowset/XmlReaderTest_RI.xml";

    public final String XML_SRC_URL_HY = "resources/org/apache/harmony/sql/internal/rowset/XmlReaderTest_HY.xml";

    public final String XML_SRC_URL_INVALID_HEADER = "resources/org/apache/harmony/sql/internal/rowset/XmlFile_InvalidHeader.xml";

    public String currentUrl;

    public void testReaderXml_Header() throws Exception {
        /*
         * when run on RI, the Attribute xmlns can be empty. And the
         * xsi:schemaLocation also can be empty. However, The value of the
         * attribute "prefix="xmlns",localpart="xsi" can't be empty. No matter
         * what the value of these attributes are, the output xml's header of
         * WebRowSet keeps the same.
         */
        WebRowSet webRs = newWebRowSet();
        Reader fileReader = new FileReader(XML_SRC_URL_INVALID_HEADER);
        webRs.readXml(fileReader);

        StringWriter strWriter = new StringWriter();
        webRs.writeXml(strWriter);
        assertFalse(-1 == strWriter.toString().indexOf(
                "http://java.sun.com/xml/ns/jdbc"));
        assertFalse(-1 == strWriter.toString().indexOf(
                "http://www.w3.org/2001/XMLSchema-instance"));
    }

    public void testReaderXml_Reader() throws Exception {
        WebRowSet webRs = newWebRowSet();
        webRs.readXml(new FileReader(currentUrl));

        /*
         * TODO A row is marked as delete in XML. The row isn't marked as delete
         * any more after populate to WebRowSet.
         */
        if (!"true".equals(System.getProperty("Testing Harmony"))) {
            assertTrue(webRs.absolute(3));
            assertEquals(3, webRs.getInt(1));
            assertFalse(webRs.rowDeleted());
            webRs.deleteRow();
        }

        Document srcDoc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().parse(currentUrl);
        XmlWriterTest.assertProperties(srcDoc, webRs);
        XmlWriterTest.assertMetadata(srcDoc, webRs);
        XmlWriterTest.assertData(srcDoc, webRs);
    }

    public void testReaderXml_InputStream() throws Exception {
        WebRowSet webRs = newWebRowSet();
        webRs.readXml(new FileInputStream(currentUrl));

        /*
         * TODO A row is marked as delete in XML. The row isn't marked as delete
         * any more after populating to WebRowSet.
         */
        if (!"true".equals(System.getProperty("Testing Harmony"))) {
            assertTrue(webRs.absolute(3));
            assertEquals(3, webRs.getInt(1));
            assertFalse(webRs.rowDeleted());
            webRs.deleteRow();
        }

        Document srcDoc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().parse(currentUrl);
        XmlWriterTest.assertProperties(srcDoc, webRs);
        XmlWriterTest.assertMetadata(srcDoc, webRs);
        XmlWriterTest.assertData(srcDoc, webRs);
    }

    public void setUp() throws Exception {
        try {
            Class.forName("com.sun.rowset.WebRowSetImpl");
            currentUrl = XML_SRC_URL_RI;
        } catch (ClassNotFoundException e) {
            System.setProperty("Testing Harmony", "true");
            Class
                    .forName("org.apache.harmony.sql.internal.rowset.WebRowSetImpl");
            currentUrl = XML_SRC_URL_HY;
        }
    }

    protected WebRowSet newWebRowSet() throws Exception {
        try {
            return (WebRowSet) Class.forName("com.sun.rowset.WebRowSetImpl")
                    .newInstance();
        } catch (ClassNotFoundException e) {
            return (WebRowSet) Class.forName(
                    "org.apache.harmony.sql.internal.rowset.WebRowSetImpl")
                    .newInstance();
        }
    }
}
