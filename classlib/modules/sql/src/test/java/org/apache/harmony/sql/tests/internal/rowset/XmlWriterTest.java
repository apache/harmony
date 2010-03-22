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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.sql.tests.internal.rowset;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.WebRowSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlWriterTest extends CachedRowSetTestCase {

    private StringWriter strWriter = null;

    public void setUp() throws Exception {
        super.setUp();
        strWriter = new StringWriter();
    }

    public void testWriteXML_Unicode() throws Exception {
        final String unicodeChar = "\u4e2d\u6587";
        String insertSQL = "INSERT INTO USER_INFO(ID, NAME, BIGINT_T, NUMERIC_T, DECIMAL_T, SMALLINT_T, "
                + "FLOAT_T, REAL_T, DOUBLE_T, DATE_T, TIME_T, TIMESTAMP_T) VALUES(?, ?, ?, ?, ?, ?,"
                + "?, ?, ?, ?, ?, ?)";
        PreparedStatement preStmt = conn.prepareStatement(insertSQL);
        preStmt.setInt(1, 10);
        preStmt.setString(2, unicodeChar);
        preStmt.setLong(3, 444423L);
        preStmt.setBigDecimal(4, new BigDecimal(12));
        preStmt.setBigDecimal(5, new BigDecimal(23));
        preStmt.setInt(6, 41);
        preStmt.setFloat(7, 4.8F);
        preStmt.setFloat(8, 4.888F);
        preStmt.setDouble(9, 4.9999);
        preStmt.setDate(10, new Date(965324512));
        preStmt.setTime(11, new Time(452368512));
        preStmt.setTimestamp(12, new Timestamp(874532105));
        preStmt.executeUpdate();
        preStmt.close();

        WebRowSet webRs = newWebRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        webRs.populate(rs);
        webRs.writeXml(strWriter);
        assertTrue(webRs.last());
        assertEquals(unicodeChar, webRs.getString(2));
        assertFalse(-1 == strWriter.toString().indexOf(unicodeChar));

        WebRowSet webRs2 = newWebRowSet();
        webRs2.readXml(new StringReader(strWriter.toString()));
        assertTrue(webRs2.last());
        assertEquals(unicodeChar, webRs2.getString(2));
    }

    public void testWriteXML_Listener() throws Exception {
        /*
         * First, populate WebRowSet using ResultSet; then call WebRowSet's
         * writeXml(), write to StringWriter; call readXml() to read the
         * StringWriter's content again. See what happens: The properties and
         * metadata remains the same. Only four new rows which are the same as
         * the original data in WebRowSet are added.
         */
        WebRowSet webRs = newWebRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        webRs.populate(rs);
        ResultSetMetaData meta = webRs.getMetaData();
        // register listener
        Listener listener = new Listener();
        webRs.addRowSetListener(listener);
        assertNull(listener.getTag());
        // write to StringWriter
        webRs.writeXml(strWriter);
        webRs.beforeFirst();
        // read from StringWriter
        webRs.readXml(new StringReader(strWriter.toString()));
        isMetaDataEquals(meta, webRs.getMetaData());
        webRs.beforeFirst();
        int index = 0;
        while (webRs.next()) {
            index++;
            if (index > 4) {
                assertEquals(index - 4, webRs.getInt(1));
            } else {
                assertEquals(index, webRs.getInt(1));
            }
        }
        // TODO How to solve the difference between RI and Harmony
        // assertEquals(8, index);

        /*
         * Create a new table. Then populate it to WebRowSet. See what happens:
         * The metadata and the row datas come from the new table.
         */
        createNewTable();
        rs = st.executeQuery("SELECT * FROM CUSTOMER_INFO");
        index = 0;
        while (rs.next()) {
            index++;
            if (index == 1) {
                assertEquals(1111, rs.getInt(1));
                assertEquals("customer_one", rs.getString(2));
            } else if (index == 2) {
                assertEquals(5555, rs.getInt(1));
                assertEquals("customer_two", rs.getString(2));
            }
        }
        assertEquals(2, index);
        rs = st.executeQuery("SELECT * FROM CUSTOMER_INFO");
        webRs.beforeFirst();
        listener.clear();
        webRs.populate(rs);
        assertEquals(CachedRowSetListenerTest.EVENT_ROWSET_CHANGED, listener
                .getTag());
        webRs.beforeFirst();
        index = 0;
        /*
         * TODO record the difference between RI and Harmony
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            while (webRs.next()) {
                index++;
                if (index == 1) {
                    assertEquals(1111, webRs.getInt(1));
                    assertEquals("customer_one", webRs.getString(2));
                } else if (index == 2) {
                    assertEquals(5555, webRs.getInt(1));
                    assertEquals("customer_two", webRs.getString(2));
                }
            }
        } else {
            while (webRs.next()) {
                index++;
                if (index == 1) {
                    assertEquals(1, webRs.getInt(1));
                    assertEquals("hermit", webRs.getString(2));
                } else if (index == 2) {
                    assertEquals(2, webRs.getInt(1));
                    assertEquals("test", webRs.getString(2));
                }
            }
        }
        assertEquals(2, index);
    }

    public void testWriteXML() throws Exception {
        WebRowSet webRs = newWebRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        webRs.populate(rs);

        Map<String, Class<?>> map = new HashMap<String, Class<?>>();
        map.put("VARCHAR", String.class);
        map.put("Array", Array.class);
        webRs.setTypeMap(map);

        webRs.setKeyColumns(new int[] { 2, 1 });

        // update a row
        assertTrue(webRs.absolute(4));
        webRs.updateInt(1, 44);
        webRs.updateString(2, "update44");
        webRs.updateRow();

        // update a row but not call updateRow()
        assertTrue(webRs.absolute(2));
        webRs.updateInt(1, 22);

        // delete a row
        assertTrue(webRs.absolute(3));
        webRs.deleteRow();

        // insert a row
        webRs.moveToInsertRow();
        webRs.updateInt(1, 77);
        webRs.updateString(2, "insert77");
        webRs.insertRow();
        webRs.moveToCurrentRow();

        webRs.writeXml(strWriter);

        Document doc = getDocument(strWriter);
        assertProperties(doc, webRs);
        assertMetadata(doc, webRs);
        assertData(doc, webRs);

        webRs = newWebRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        webRs.populate(rs);

        strWriter = new StringWriter();
        webRs.writeXml(strWriter);

        doc = getDocument(strWriter);
        assertProperties(doc, webRs);
        assertMetadata(doc, webRs);
        assertData(doc, webRs);
    }

    public void testWriteXML_Update() throws Exception {
        WebRowSet webRs = newWebRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        webRs.populate(rs);

        webRs.absolute(3);
        webRs.updateString(2, "update3");

        webRs.writeXml(strWriter);

        assertTrue(webRs.isAfterLast());

        Document doc = getDocument(strWriter);

        assertProperties(doc, webRs);
        assertMetadata(doc, webRs);
        assertData(doc, webRs);

        webRs.updateRow();

        strWriter = new StringWriter();
        webRs.writeXml(strWriter);

        assertTrue(webRs.isAfterLast());

        doc = getDocument(strWriter);

        assertProperties(doc, webRs);
        assertMetadata(doc, webRs);
        assertData(doc, webRs);
    }

    public void testWriteXML_Insert() throws Exception {
        WebRowSet webRs = newWebRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        webRs.populate(rs);

        webRs.moveToInsertRow();
        webRs.updateString(2, "update3");
        webRs.updateInt(4, 3);
        webRs.moveToCurrentRow();

        webRs.writeXml(strWriter);

        assertTrue(webRs.isAfterLast());

        Document doc = getDocument(strWriter);

        assertProperties(doc, webRs);
        assertMetadata(doc, webRs);
        assertData(doc, webRs);

        webRs = newWebRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        webRs.populate(rs);

        assertTrue(webRs.absolute(3));
        webRs.moveToInsertRow();
        webRs.updateString(2, "insert5");
        webRs.updateInt(1, 5);
        webRs.insertRow();
        webRs.moveToCurrentRow();
        webRs.next();
        webRs.updateString(2, "update5");
        webRs.updateInt(1, 6);

        strWriter = new StringWriter();
        webRs.writeXml(strWriter);

        assertTrue(webRs.isAfterLast());

        doc = getDocument(strWriter);

        assertProperties(doc, webRs);
        assertMetadata(doc, webRs);
        assertData(doc, webRs);
    }

    public void testWriteXML_Delete() throws Exception {
        WebRowSet webRs = newWebRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        webRs.populate(rs);

        webRs.absolute(3);
        webRs.deleteRow();

        assertEquals(0, webRs.getRow());

        webRs.writeXml(strWriter);

        assertTrue(webRs.isAfterLast());

        Document doc = getDocument(strWriter);

        assertProperties(doc, webRs);
        assertMetadata(doc, webRs);
        assertData(doc, webRs);
    }

    public void testWriteXML_Update_Delete() throws Exception {
        WebRowSet webRs = newWebRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        webRs.populate(rs);

        webRs.absolute(3);
        webRs.updateString(2, "update3");

        webRs.deleteRow();

        webRs.writeXml(strWriter);

        assertTrue(webRs.isAfterLast());

        Document doc = getDocument(strWriter);

        assertProperties(doc, webRs);
        assertMetadata(doc, webRs);
        assertData(doc, webRs);

        webRs = newWebRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        webRs.populate(rs);

        webRs.absolute(3);
        webRs.updateString(2, "update3");
        webRs.updateRow();

        webRs.deleteRow();

        strWriter = new StringWriter();
        webRs.writeXml(strWriter);

        assertTrue(webRs.isAfterLast());

        doc = getDocument(strWriter);

        assertProperties(doc, webRs);
        assertMetadata(doc, webRs);
        assertData(doc, webRs);
    }

    public void testWriteXML_NotInitial() throws Exception {
        WebRowSet webRs = newWebRowSet();

        try {
            webRs.writeXml(strWriter);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        Writer writer = null;
        try {
            webRs.writeXml(writer);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testWriteXML_ResultSet() throws Exception {
        WebRowSet webRs = newWebRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");

        webRs.writeXml(rs, strWriter);

        /*
         * TODO spec says the cursor should move to previous position after
         * writing. However, RI doesn't. Harmony follows spec
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertTrue(webRs.isBeforeFirst());
        } else {
            assertTrue(webRs.isAfterLast());
        }

        Document doc = getDocument(strWriter);

        assertProperties(doc, webRs);
        assertMetadata(doc, webRs);
        assertData(doc, webRs);

        webRs.absolute(2);

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        strWriter = new StringWriter();
        webRs.writeXml(rs, strWriter);

        /*
         * TODO spec says the cursor should move to previous position after
         * writing. However, RI doesn't. Harmony follows spec
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertTrue(webRs.isBeforeFirst());
        } else {
            assertTrue(webRs.isAfterLast());
        }

        doc = getDocument(strWriter);

        assertProperties(doc, webRs);
        assertMetadata(doc, webRs);
        assertData(doc, webRs);
    }

    protected WebRowSet newWebRowSet() throws Exception {
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            return (WebRowSet) Class.forName(
                    "org.apache.harmony.sql.internal.rowset.WebRowSetImpl")
                    .newInstance();
        }
        return (WebRowSet) Class.forName("com.sun.rowset.WebRowSetImpl")
                .newInstance();
    }

    private Document getDocument(StringWriter strWriter) throws Exception {
        StringBuffer buffer = strWriter.getBuffer();
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();
        return docBuilder.parse(new InputSource(new StringReader(buffer
                .toString())));
    }

    public static void assertProperties(Document doc, WebRowSet webRs)
            throws Exception {
        Element element = (Element) doc.getFirstChild();
        Element prop = getElement(element, "properties");

        element = getElement(prop, "command");
        assertNotNull(element);
        if (webRs.getCommand() != null) {
            assertEquals(webRs.getCommand(), element.getTextContent());
        } else {
            assertEquals("null", element.getFirstChild().getNodeName());
            assertEquals("", element.getFirstChild().getTextContent());
        }

        element = getElement(prop, "concurrency");
        assertNotNull(element);
        assertEquals(webRs.getConcurrency(), Integer.valueOf(
                element.getTextContent()).intValue());

        element = getElement(prop, "datasource");
        assertNotNull(element);
        if (webRs.getDataSourceName() != null) {
            assertEquals(webRs.getDataSourceName(), element.getTextContent());
        } else {
            assertEquals("null", element.getFirstChild().getNodeName());
            assertEquals("", element.getFirstChild().getTextContent());
        }

        element = getElement(prop, "escape-processing");
        assertNotNull(element);
        assertEquals(webRs.getEscapeProcessing(), Boolean.valueOf(
                element.getTextContent()).booleanValue());

        element = getElement(prop, "fetch-direction");
        assertNotNull(element);

        assertEquals(webRs.getFetchDirection(), Integer.valueOf(
                element.getTextContent()).intValue());

        element = getElement(prop, "fetch-size");
        assertNotNull(element);
        assertEquals(webRs.getFetchSize(), Integer.valueOf(
                element.getTextContent()).intValue());

        element = getElement(prop, "isolation-level");
        assertNotNull(element);
        assertEquals(webRs.getTransactionIsolation(), Integer.valueOf(
                element.getTextContent()).intValue());

        element = getElement(prop, "key-columns");
        assertNotNull(element);
        NodeList list = element.getChildNodes();

        for (int i = 0, j = 0; i < list.getLength(); ++i) {
            if (list.item(i) instanceof Element) {
                assertEquals(webRs.getKeyColumns()[j], Integer.valueOf(
                        list.item(i).getTextContent()).intValue());
                ++j;
            }
        }

        element = getElement(prop, "map");
        assertNotNull(element);
        list = element.getChildNodes();
        int i = 0;
        if (webRs.getTypeMap() != null) {
            for (Iterator iter = webRs.getTypeMap().keySet().iterator(); iter
                    .hasNext();) {
                String key = (String) iter.next();
                while (!(list.item(i) instanceof Element)) {
                    i++;
                }
                assertEquals("type", list.item(i).getNodeName());
                String docTypeName = list.item(i).getTextContent();
                assertTrue(webRs.getTypeMap().keySet().contains(docTypeName));
                ++i;

                while (!(list.item(i) instanceof Element)) {
                    i++;
                }
                assertEquals("class", list.item(i).getNodeName());
                assertEquals(webRs.getTypeMap().get(docTypeName).getName(),
                        list.item(i).getTextContent());

                ++i;
            }
        }

        element = getElement(prop, "max-field-size");
        assertNotNull(element);
        assertEquals(webRs.getMaxFieldSize(), Integer.valueOf(
                element.getTextContent()).intValue());

        element = getElement(prop, "max-rows");
        assertNotNull(element);
        assertEquals(webRs.getMaxRows(), Integer.valueOf(
                element.getTextContent()).intValue());

        element = getElement(prop, "query-timeout");
        assertNotNull(element);
        assertEquals(webRs.getQueryTimeout(), Integer.valueOf(
                element.getTextContent()).intValue());

        element = getElement(prop, "read-only");
        assertNotNull(element);
        assertEquals(webRs.isReadOnly(), Boolean.valueOf(
                element.getTextContent()).booleanValue());

        element = getElement(prop, "rowset-type");
        assertNotNull(element);
        switch (webRs.getType()) {
        case ResultSet.TYPE_FORWARD_ONLY:
            assertEquals("ResultSet.TYPE_FORWARD_ONLY", element
                    .getTextContent());
            break;

        case ResultSet.TYPE_SCROLL_INSENSITIVE:
            assertEquals("ResultSet.TYPE_SCROLL_INSENSITIVE", element
                    .getTextContent());
            break;

        case ResultSet.TYPE_SCROLL_SENSITIVE:
            assertEquals("ResultSet.TYPE_SCROLL_SENSITIVE", element
                    .getTextContent());
            break;
        }

        element = getElement(prop, "show-deleted");
        assertNotNull(element);
        /*
         * TODO RI would change show deleted to true after writing data, but the
         * value written to xml is right
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertEquals(webRs.getShowDeleted(), Boolean.valueOf(
                    element.getTextContent()).booleanValue());
        }

        element = getElement(prop, "table-name");
        assertNotNull(element);
        if (webRs.getTableName() != null) {
            assertEquals(webRs.getTableName(), element.getTextContent());
        } else {
            assertEquals("null", element.getFirstChild().getNodeName());
            assertEquals("", element.getFirstChild().getTextContent());
        }

        element = getElement(prop, "url");
        assertNotNull(element);
        if (webRs.getUrl() != null) {
            assertEquals(webRs.getUrl(), element.getTextContent());
        } else {
            assertEquals("null", element.getFirstChild().getNodeName());
            assertEquals("", element.getFirstChild().getTextContent());
        }

        Element provider = getElement(prop, "sync-provider");

        element = getElement(provider, "sync-provider-name");
        assertNotNull(element);
        assertEquals(webRs.getSyncProvider().getProviderID(), element
                .getTextContent());

        element = getElement(provider, "sync-provider-vendor");
        assertNotNull(element);
        assertEquals(webRs.getSyncProvider().getVendor(), element
                .getTextContent());

        element = getElement(provider, "sync-provider-version");
        assertNotNull(element);
        assertEquals(webRs.getSyncProvider().getVersion(), element
                .getTextContent());

        element = getElement(provider, "sync-provider-grade");
        assertNotNull(element);
        assertEquals(webRs.getSyncProvider().getProviderGrade(), Integer
                .valueOf(element.getTextContent()).intValue());

        element = getElement(provider, "data-source-lock");
        assertNotNull(element);
        assertEquals(webRs.getSyncProvider().getDataSourceLock(), Integer
                .valueOf(element.getTextContent()).intValue());

    }

    private static Element getElement(Element node, String name) {
        NodeList list = node.getElementsByTagName(name);
        assertEquals(1, list.getLength());
        return (Element) list.item(0);
    }

    public static void assertMetadata(Document doc, WebRowSet webRs)
            throws Exception {
        boolean isArrived = false;
        ResultSetMetaData meta = webRs.getMetaData();
        NodeList nodeList = doc.getFirstChild().getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                Element ele = (Element) node;
                if ("metadata".equals(ele.getTagName())) {
                    isArrived = true;

                    // column-count
                    int colCount_Meta = webRs.getMetaData().getColumnCount();
                    String colCount_Doc = ele.getElementsByTagName(
                            "column-count").item(0).getTextContent();
                    assertEquals(Integer.toString(colCount_Meta), colCount_Doc);

                    // column-definition
                    NodeList colDefList = ele
                            .getElementsByTagName("column-definition");
                    for (int j = 0; j < colDefList.getLength(); j++) {
                        Element colDefEle = (Element) colDefList.item(j);
                        NodeList children = colDefEle.getChildNodes();

                        int columnIndex = 0;
                        String autoIncrement = null;
                        String caseSensitive = null;
                        String currency = null;
                        String nullable = null;
                        String signed = null;
                        String searchable = null;
                        String columnDisplaySize = null;
                        String columnLabel = null;
                        String columnName = null;
                        String schemaName = null;
                        String columnPrecision = null;
                        String columnScale = null;
                        String tableName = null;
                        String catalogName = null;
                        String columnType = null;
                        String columnTypeName = null;

                        int index = 0;
                        for (int k = 0; k < children.getLength(); k++) {
                            if (children.item(k) instanceof Element) {
                                index++;
                                Element child = (Element) children.item(k);
                                String childTag = child.getTagName();
                                String childContent = child.getTextContent();
                                if (index == 1) {
                                    assertEquals("column-index", childTag);
                                    columnIndex = Integer
                                            .parseInt(childContent);
                                } else if (index == 2) {
                                    assertEquals("auto-increment", childTag);
                                    autoIncrement = childContent;
                                } else if (index == 3) {
                                    assertEquals("case-sensitive", childTag);
                                    caseSensitive = childContent;
                                } else if (index == 4) {
                                    assertEquals("currency", childTag);
                                    currency = childContent;
                                } else if (index == 5) {
                                    assertEquals("nullable", childTag);
                                    nullable = childContent;
                                } else if (index == 6) {
                                    assertEquals("signed", childTag);
                                    signed = childContent;
                                } else if (index == 7) {
                                    assertEquals("searchable", childTag);
                                    searchable = childContent;
                                } else if (index == 8) {
                                    assertEquals("column-display-size",
                                            childTag);
                                    columnDisplaySize = childContent;
                                } else if (index == 9) {
                                    assertEquals("column-label", childTag);
                                    columnLabel = childContent;
                                } else if (index == 10) {
                                    assertEquals("column-name", childTag);
                                    columnName = childContent;
                                } else if (index == 11) {
                                    assertEquals("schema-name", childTag);
                                    schemaName = childContent;
                                } else if (index == 12) {
                                    assertEquals("column-precision", childTag);
                                    columnPrecision = childContent;
                                } else if (index == 13) {
                                    assertEquals("column-scale", childTag);
                                    columnScale = childContent;
                                } else if (index == 14) {
                                    assertEquals("table-name", childTag);
                                    tableName = childContent;
                                } else if (index == 15) {
                                    assertEquals("catalog-name", childTag);
                                    catalogName = childContent;
                                } else if (index == 16) {
                                    assertEquals("column-type", childTag);
                                    columnType = childContent;
                                } else if (index == 17) {
                                    assertEquals("column-type-name", childTag);
                                    columnTypeName = childContent;
                                }
                            }
                        }
                        assertEquals(17, index);

                        assertEquals(autoIncrement, Boolean.toString(meta
                                .isAutoIncrement(columnIndex)));
                        assertEquals(caseSensitive, Boolean.toString(meta
                                .isCaseSensitive(columnIndex)));
                        assertEquals(currency, Boolean.toString(meta
                                .isCurrency(columnIndex)));
                        assertEquals(nullable, Integer.toString(meta
                                .isNullable(columnIndex)));
                        assertEquals(signed, Boolean.toString(meta
                                .isSigned(columnIndex)));
                        assertEquals(searchable, Boolean.toString(meta
                                .isSearchable(columnIndex)));
                        assertEquals(columnDisplaySize, Integer.toString(meta
                                .getColumnDisplaySize(columnIndex)));
                        assertEquals(columnLabel, meta
                                .getColumnLabel(columnIndex));
                        assertEquals(columnName, meta
                                .getColumnName(columnIndex));
                        assertEquals(schemaName, meta
                                .getSchemaName(columnIndex));
                        assertEquals(columnPrecision, Integer.toString(meta
                                .getPrecision(columnIndex)));
                        assertEquals(columnScale, Integer.toString(meta
                                .getScale(columnIndex)));
                        assertEquals(tableName, meta.getTableName(columnIndex));
                        assertEquals(catalogName, meta
                                .getCatalogName(columnIndex));
                        assertEquals(columnType, Integer.toString(meta
                                .getColumnType(columnIndex)));
                        assertEquals(columnTypeName, meta
                                .getColumnTypeName(columnIndex));
                    }
                }
            }
        }
        assertTrue(isArrived);
    }

    public static void assertData(Document doc, WebRowSet webRs)
            throws Exception {
        webRs.setShowDeleted(true);
        webRs.beforeFirst();

        Element element = (Element) doc.getFirstChild();
        Element dataEle = getElement(element, "data");
        NodeList nodeList = dataEle.getChildNodes();
        int rowIndex = 0;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                rowIndex++;
                Element rowEle = (Element) node;
                assertRow(rowEle, webRs, rowIndex);
            }
        }
        assertEquals(webRs.size(), rowIndex);
        webRs.setShowDeleted(false);
    }

    private static void assertRow(Element ele, WebRowSet webRs, int rowIndex)
            throws Exception {
        assertTrue(webRs.absolute(rowIndex));
        String rowTag = null;
        if (webRs.rowDeleted()) {
            rowTag = "deleteRow";
        } else if (webRs.rowInserted()) {
            rowTag = "insertRow";
        } else if (webRs.rowUpdated()) {
            /*
             * TODO ri's bug. The tag should be "modifyRow" if a row is marked
             * as update. However, RI still uses "currentRow". The tag should be
             * "updateValue" if a column is updated. However, RI uses
             * "updateRow".
             */
            if ("true".equals(System.getProperty("Testing Harmony"))) {
                rowTag = "modifyRow";
            } else {
                rowTag = "currentRow";
            }
        } else {
            rowTag = "currentRow";
        }
        assertEquals(rowTag, ele.getTagName());

        // the original row
        CachedRowSet originalRow = (CachedRowSet) webRs.getOriginalRow();
        assertTrue(originalRow.next());

        NodeList nodeList = ele.getChildNodes();
        int colIndex = 0;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                String tag = element.getTagName();
                String content = element.getChildNodes().item(0).getNodeValue();
                if ("columnValue".equals(tag)) {
                    colIndex++;
                    if (content == null) {
                        assertNull(originalRow.getObject(colIndex));
                    } else {
                        String columnValue = null;
                        int columnType = webRs.getMetaData().getColumnType(
                                colIndex);
                        if (Types.DATE == columnType) {
                            columnValue = Long.toString(originalRow.getDate(
                                    colIndex).getTime());
                        } else if (Types.TIME == columnType) {
                            columnValue = Long.toString(originalRow.getTime(
                                    colIndex).getTime());
                        } else if (Types.TIMESTAMP == columnType) {
                            columnValue = Long.toString(originalRow
                                    .getTimestamp(colIndex).getTime());
                        } else {
                            columnValue = originalRow.getString(colIndex);
                        }
                        assertEquals(content, columnValue);
                    }
                } else {
                    if (content == null) {
                        assertNull(webRs.getObject(colIndex));
                    } else {
                        String columnValue = null;
                        int columnType = webRs.getMetaData().getColumnType(
                                colIndex);
                        if (Types.DATE == columnType) {
                            columnValue = Long.toString(webRs.getDate(colIndex)
                                    .getTime());
                        } else if (Types.TIME == columnType) {
                            columnValue = Long.toString(webRs.getTime(colIndex)
                                    .getTime());
                        } else if (Types.TIMESTAMP == columnType) {
                            columnValue = Long.toString(webRs.getTimestamp(
                                    colIndex).getTime());
                        } else {
                            columnValue = webRs.getString(colIndex);
                        }
                        assertEquals(content, columnValue);
                    }
                }
            }
        }
        assertEquals(DEFAULT_COLUMN_COUNT, colIndex);
    }
}
