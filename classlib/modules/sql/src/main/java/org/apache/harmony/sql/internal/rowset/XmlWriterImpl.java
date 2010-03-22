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

package org.apache.harmony.sql.internal.rowset;

import java.io.Writer;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;

import javax.sql.rowset.WebRowSet;
import javax.sql.rowset.spi.XmlWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlWriterImpl extends CachedRowSetWriter implements XmlWriter {

    public void writeXML(WebRowSet caller, Writer writer) throws SQLException {
        if (writer == null || caller == null || caller.getMetaData() == null) {
            throw new NullPointerException();
        }
        caller.beforeFirst();
        DocumentBuilder docBuidler = null;
        Document doc = null;
        try {
            docBuidler = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder();
            doc = docBuidler.newDocument();
        } catch (ParserConfigurationException e) {
            SQLException ex = new SQLException();
            ex.initCause(e);
            throw ex;
        }

        /*
         * root element: webRowSet
         */
        Element rootElement = doc.createElement("webRowSet"); //$NON-NLS-1$
        rootElement.setAttribute("xmlns", "http://java.sun.com/xml/ns/jdbc"); //$NON-NLS-1$ //$NON-NLS-2$
        rootElement.setAttribute("xmlns:xsi", //$NON-NLS-1$
                "http://www.w3.org/2001/XMLSchema-instance"); //$NON-NLS-1$
        rootElement
                .setAttribute("xsi:schemaLocation", //$NON-NLS-1$
                        "http://java.sun.com/xml/ns/jdbc http://java.sun.com/xml/ns/jdbc/webrowset.xsd"); //$NON-NLS-1$

        /*
         * Element: properties
         */
        Element prop = doc.createElement("properties"); //$NON-NLS-1$
        rootElement.appendChild(prop);
        writeProperties(doc, prop, caller);

        /*
         * Element: metadata
         */
        ResultSetMetaData rsmd = caller.getMetaData();
        Element metadataEle = doc.createElement("metadata"); //$NON-NLS-1$
        // column-count
        int colCount = rsmd.getColumnCount();
        Element colCountEle = doc.createElement("column-count"); //$NON-NLS-1$
        colCountEle.setTextContent(Integer.toString(colCount));
        metadataEle.appendChild(colCountEle);
        // add each column definition
        for (int i = 1; i <= colCount; i++) {
            writeMetadataByCol(doc, metadataEle, rsmd, i);
        }
        rootElement.appendChild(metadataEle);

        /*
         * Element: data
         */
        Element data = doc.createElement("data"); //$NON-NLS-1$
        rootElement.appendChild(data);

        writeRowSetData(doc, data, caller);

        /*
         * add root element to Document
         */
        doc.appendChild(rootElement);

        try {
            Transformer transformer = TransformerFactory.newInstance()
                    .newTransformer();
            DOMSource domSrc = new DOMSource(doc);
            StreamResult streamResult = new StreamResult(writer);
            transformer.transform(domSrc, streamResult);
        } catch (TransformerConfigurationException e) {
            SQLException ex = new SQLException();
            ex.initCause(e);
            throw ex;
        } catch (TransformerFactoryConfigurationError e) {
            SQLException ex = new SQLException();
            ex.initCause(e);
            throw ex;
        } catch (TransformerException e) {
            SQLException ex = new SQLException();
            ex.initCause(e);
            throw ex;
        }
    }

    private void writeRowSetData(Document doc, Element data, WebRowSet caller)
            throws SQLException {
        boolean preShowDelted = caller.getShowDeleted();
        caller.setShowDeleted(true);

        caller.beforeFirst();

        while (caller.next()) {
            wirteRow(doc, data, caller);
        }

        caller.setShowDeleted(preShowDelted);
    }

    private void wirteRow(Document doc, Element data, WebRowSet caller)
            throws SQLException {
        Element row = null;
        if (caller.rowDeleted()) {
            row = doc.createElement("deleteRow"); //$NON-NLS-1$
        } else if (caller.rowInserted()) {
            row = doc.createElement("insertRow"); //$NON-NLS-1$
        } else if (caller.rowUpdated()) {
            row = doc.createElement("modifyRow"); //$NON-NLS-1$
        } else {
            row = doc.createElement("currentRow"); //$NON-NLS-1$
        }

        data.appendChild(row);
        for (int i = 1; i <= caller.getMetaData().getColumnCount(); ++i) {
            if (caller.columnUpdated(i)) {
                ResultSet result = caller.getOriginalRow();
                result.next();
                Object originalValue = result.getObject(i);
                Object value = caller.getObject(i);
                if (originalValue != null) {
                    int columnType = caller.getMetaData().getColumnType(i);

                    switch (columnType) {
                    case Types.DATE:
                        originalValue = Long.valueOf(result.getDate(i)
                                .getTime());
                        value = Long.valueOf(caller.getDate(i).getTime());
                        break;
                    case Types.TIME:
                        originalValue = Long.valueOf(result.getTime(i)
                                .getTime());
                        value = Long.valueOf(caller.getTime(i).getTime());
                        break;
                    case Types.TIMESTAMP:
                        originalValue = Long.valueOf(result.getTime(i)
                                .getTime());
                        value = Long.valueOf(caller.getTimestamp(i).getTime());
                        break;
                    }

                    // leave to blank when it's binary or struct data type
                    if (columnType == Types.ARRAY || columnType == Types.BINARY
                            || columnType == Types.BLOB
                            || columnType == Types.CLOB
                            || columnType == Types.REF
                            || columnType == Types.STRUCT
                            || columnType == Types.DISTINCT) {
                        originalValue = ""; //$NON-NLS-1$
                        value = ""; //$NON-NLS-1$
                    }

                }

                appendElement(row, doc, "columnValue", originalValue); //$NON-NLS-1$
                appendElement(row, doc, "updateValue", value); //$NON-NLS-1$

            } else {

                Object value = caller.getObject(i);
                if (value != null) {
                    int columnType = caller.getMetaData().getColumnType(i);
                    switch (columnType) {
                    case Types.DATE:
                        value = Long.valueOf(caller.getDate(i).getTime());
                        break;
                    case Types.TIME:
                        value = Long.valueOf(caller.getTime(i).getTime());
                        break;
                    case Types.TIMESTAMP:
                        value = Long.valueOf(caller.getTime(i).getTime());
                        break;
                    }
                    // leave to blank when it's binary or struct data type
                    if (columnType == Types.ARRAY || columnType == Types.BINARY
                            || columnType == Types.BLOB
                            || columnType == Types.CLOB
                            || columnType == Types.REF
                            || columnType == Types.STRUCT
                            || columnType == Types.DISTINCT) {
                        value = ""; //$NON-NLS-1$
                    }
                }

                appendElement(row, doc, "columnValue", value); //$NON-NLS-1$
            }
        }
    }

    @SuppressWarnings("boxing")
    private void writeProperties(Document doc, Element prop, WebRowSet caller)
            throws SQLException {

        appendElement(prop, doc, "command", caller.getCommand()); //$NON-NLS-1$
        appendElement(prop, doc, "concurrency", caller.getConcurrency()); //$NON-NLS-1$
        appendElement(prop, doc, "datasource", caller.getDataSourceName()); //$NON-NLS-1$
        appendElement(prop, doc, "escape-processing", caller //$NON-NLS-1$
                .getEscapeProcessing());
        appendElement(prop, doc, "fetch-direction", caller.getFetchDirection()); //$NON-NLS-1$
        appendElement(prop, doc, "fetch-size", caller.getFetchSize()); //$NON-NLS-1$
        appendElement(prop, doc, "isolation-level", caller //$NON-NLS-1$
                .getTransactionIsolation());

        // write key columns
        Element keyColumns = doc.createElement("key-columns"); //$NON-NLS-1$
        prop.appendChild(keyColumns);

        int[] indexes = caller.getKeyColumns();
        for (int i = 0; i < indexes.length; ++i) {
            appendElement(keyColumns, doc, "column", indexes[i]); //$NON-NLS-1$
        }

        // wirte type map
        Element typeMap = doc.createElement("map"); //$NON-NLS-1$
        prop.appendChild(typeMap);
        if (caller.getTypeMap() != null) {
            for (Iterator<String> iter = caller.getTypeMap().keySet()
                    .iterator(); iter.hasNext();) {
                String key = iter.next();
                appendElement(typeMap, doc, "type", key); //$NON-NLS-1$
                appendElement(typeMap, doc, "class", caller.getTypeMap().get( //$NON-NLS-1$
                        key).getName());
            }
        }

        appendElement(prop, doc, "max-field-size", caller.getMaxFieldSize()); //$NON-NLS-1$
        appendElement(prop, doc, "max-rows", caller.getMaxRows()); //$NON-NLS-1$
        appendElement(prop, doc, "query-timeout", caller.getQueryTimeout()); //$NON-NLS-1$
        appendElement(prop, doc, "read-only", caller.isReadOnly()); //$NON-NLS-1$

        String rowsetType = null;
        switch (caller.getType()) {
        case ResultSet.TYPE_FORWARD_ONLY:
            rowsetType = "ResultSet.TYPE_FORWARD_ONLY"; //$NON-NLS-1$
            break;
        case ResultSet.TYPE_SCROLL_INSENSITIVE:
            rowsetType = "ResultSet.TYPE_SCROLL_INSENSITIVE"; //$NON-NLS-1$
            break;
        case ResultSet.TYPE_SCROLL_SENSITIVE:
            rowsetType = "ResultSet.TYPE_SCROLL_SENSITIVE"; //$NON-NLS-1$
            break;

        }

        appendElement(prop, doc, "rowset-type", rowsetType); //$NON-NLS-1$

        appendElement(prop, doc, "show-deleted", caller.getShowDeleted()); //$NON-NLS-1$
        appendElement(prop, doc, "table-name", caller.getTableName()); //$NON-NLS-1$
        appendElement(prop, doc, "url", caller.getUrl()); //$NON-NLS-1$

        Element provider = doc.createElement("sync-provider"); //$NON-NLS-1$
        prop.appendChild(provider);

        appendElement(provider, doc, "sync-provider-name", caller //$NON-NLS-1$
                .getSyncProvider().getProviderID());
        appendElement(provider, doc, "sync-provider-vendor", caller //$NON-NLS-1$
                .getSyncProvider().getVendor());
        appendElement(provider, doc, "sync-provider-version", caller //$NON-NLS-1$
                .getSyncProvider().getVersion());
        appendElement(provider, doc, "sync-provider-grade", caller //$NON-NLS-1$
                .getSyncProvider().getProviderGrade());
        appendElement(provider, doc, "data-source-lock", caller //$NON-NLS-1$
                .getSyncProvider().getDataSourceLock());

    }

    private Element createElement(Document doc, String name, Object value) {
        Element element = doc.createElement(name);
        if (value == null) {
            element.appendChild(doc.createElement("null")); //$NON-NLS-1$
        } else {
            element.setTextContent(value.toString());
        }

        return element;
    }

    private void appendElement(Element root, Document doc, String name,
            Object value) {
        Element child = createElement(doc, name, value);
        root.appendChild(child);
    }

    @SuppressWarnings("boxing")
    private void writeMetadataByCol(Document doc, Element ele,
            ResultSetMetaData rsmd, int colIndex) throws SQLException {
        Element colDefEle = doc.createElement("column-definition"); //$NON-NLS-1$
        appendElement(colDefEle, doc, "column-index", colIndex); //$NON-NLS-1$
        appendElement(colDefEle, doc, "auto-increment", rsmd //$NON-NLS-1$
                .isAutoIncrement(colIndex));
        appendElement(colDefEle, doc, "case-sensitive", rsmd //$NON-NLS-1$
                .isCaseSensitive(colIndex));
        appendElement(colDefEle, doc, "currency", rsmd.isCurrency(colIndex)); //$NON-NLS-1$
        appendElement(colDefEle, doc, "nullable", rsmd.isNullable(colIndex)); //$NON-NLS-1$
        appendElement(colDefEle, doc, "signed", rsmd.isSigned(colIndex)); //$NON-NLS-1$
        appendElement(colDefEle, doc, "searchable", rsmd.isSearchable(colIndex)); //$NON-NLS-1$
        appendElement(colDefEle, doc, "column-display-size", rsmd //$NON-NLS-1$
                .getColumnDisplaySize(colIndex));
        appendElement(colDefEle, doc, "column-label", rsmd //$NON-NLS-1$
                .getColumnLabel(colIndex));
        appendElement(colDefEle, doc, "column-name", rsmd //$NON-NLS-1$
                .getColumnName(colIndex));
        appendElement(colDefEle, doc, "schema-name", rsmd //$NON-NLS-1$
                .getSchemaName(colIndex));
        appendElement(colDefEle, doc, "column-precision", rsmd //$NON-NLS-1$
                .getPrecision(colIndex));
        appendElement(colDefEle, doc, "column-scale", rsmd.getScale(colIndex)); //$NON-NLS-1$
        appendElement(colDefEle, doc, "table-name", rsmd.getTableName(colIndex)); //$NON-NLS-1$
        appendElement(colDefEle, doc, "catalog-name", rsmd //$NON-NLS-1$
                .getCatalogName(colIndex));
        appendElement(colDefEle, doc, "column-type", rsmd //$NON-NLS-1$
                .getColumnType(colIndex));
        appendElement(colDefEle, doc, "column-type-name", rsmd //$NON-NLS-1$
                .getColumnTypeName(colIndex));
        ele.appendChild(colDefEle);
    }
}
