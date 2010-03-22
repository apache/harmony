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

package org.apache.harmony.sql.internal.rowset;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.sql.RowSetMetaData;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.WebRowSet;
import javax.sql.rowset.spi.XmlReader;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.harmony.sql.internal.nls.Messages;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XmlReaderImpl extends CachedRowSetReader implements XmlReader {

    public void readXML(WebRowSet caller, Reader reader) throws SQLException {
        SAXParser parser = null;
        try {
            parser = SAXParserFactory.newInstance().newSAXParser();
            XmlHandler xmlHandler = new XmlHandler(caller);
            parser.parse(new InputSource(reader), xmlHandler);
        } catch (ParserConfigurationException e) {
            SQLException ex = new SQLException();
            ex.initCause(e);
            throw ex;
        } catch (SAXException e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            }
            SQLException ex = new SQLException();
            ex.initCause(e);
            throw ex;
        } catch (IOException e) {
            SQLException ex = new SQLException();
            ex.initCause(e);
            throw ex;
        }
    }

    private class XmlHandler extends DefaultHandler {
        private WebRowSet webRs;

        private static final int READ_PROPERTIES = 1;

        private static final int READ_METADATA = 2;

        private static final int READ_DATA = 3;

        private int state = 0;

        private String currentTagName;

        private String currentValue;

        private int colIndex;

        private ArrayList<CachedRow> rows;

        private CachedRow currentRow;

        private int columnCount;

        private ArrayList<Object> columnData;

        private ArrayList<Object> updateData;

        private ArrayList<Integer> updateColIndex;

        private ArrayList<Integer> keyCols;

        private Map<String, Class<?>> map;

        private String type;

        private String className;

        public XmlHandler(WebRowSet webRs) {
            this.webRs = webRs;
            keyCols = new ArrayList<Integer>();
            map = new HashMap<String, Class<?>>();
        }

        @Override
        public void startDocument() throws SAXException {
            rows = new ArrayList<CachedRow>();
        }

        @Override
        public void endDocument() throws SAXException {
            /*
             * set keyCols, map
             */
            int[] iKeyCols = new int[keyCols.size()];
            for (int i = 0; i < keyCols.size(); i++) {
                iKeyCols[i] = keyCols.get(i).intValue();
            }
            try {
                webRs.setKeyColumns(iKeyCols);
                webRs.setTypeMap(map);
            } catch (SQLException e) {
                SAXException ex = new SAXException();
                ex.initCause(e);
                throw ex;
            }
            /*
             * set rows
             */
            ((CachedRowSetImpl) webRs).setRows(rows, columnCount);
        }

        @Override
        public void startElement(String namespaceURI, String localName,
                String qName, Attributes attr) throws SAXException {
            if (qName.equals("null")) { //$NON-NLS-1$
                currentValue = null;
                return;
            }

            if (qName.equals("webRowSet")) { //$NON-NLS-1$
                // TODO
                return;
            }

            if (qName.equals("properties")) { //$NON-NLS-1$
                state = READ_PROPERTIES;
            } else if (qName.equals("metadata")) { //$NON-NLS-1$
                state = READ_METADATA;
            } else if (qName.equals("data")) { //$NON-NLS-1$
                state = READ_DATA;
            }

            currentTagName = qName;
            currentValue = ""; //$NON-NLS-1$

            if (state == READ_DATA) {
                initRow();
            }
        }

        @Override
        public void endElement(String namespaceURI, String localName,
                String qName) throws SAXException {
            try {
                if (state == READ_DATA) {
                    insertRow(qName);
                }
                if (qName.equals("null") || !qName.equals(currentTagName)) { //$NON-NLS-1$
                    return;
                }

                switch (state) {
                case READ_PROPERTIES:
                    readProperties();
                    break;
                case READ_METADATA:
                    readMetadata();
                    break;
                case READ_DATA:
                    readData();
                    break;
                }
            } catch (SQLException e) {
                SAXException ex = new SAXException();
                ex.initCause(e);
                throw ex;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            currentValue = new String(ch, start, length);
        }

        private void readProperties() throws SQLException {
            if ("command".equals(currentTagName)) { //$NON-NLS-1$
                webRs.setCommand(currentValue);
            } else if ("concurrency".equals(currentTagName)) { //$NON-NLS-1$
                webRs.setConcurrency(parseInt(currentValue));
            } else if ("datasource".equals(currentTagName)) { //$NON-NLS-1$
                webRs.setDataSourceName(currentValue);
            } else if ("escape-processing".equals(currentTagName)) { //$NON-NLS-1$
                webRs.setEscapeProcessing(parseBoolean(currentValue));
            } else if ("fetch-direction".equals(currentTagName)) { //$NON-NLS-1$
                webRs.setFetchDirection(parseInt(currentValue));
            } else if ("fetch-size".equals(currentTagName)) { //$NON-NLS-1$
                webRs.setFetchSize(parseInt(currentValue));
            } else if ("isolation-level".equals(currentTagName)) { //$NON-NLS-1$
                webRs.setTransactionIsolation(parseInt(currentValue));
            } else if ("column".equals(currentTagName)) { //$NON-NLS-1$
                keyCols.add(Integer.valueOf(parseInt(currentValue)));
            } else if ("type".equals(currentTagName)) { //$NON-NLS-1$
                type = currentValue;
            } else if ("class".equals(currentTagName)) { //$NON-NLS-1$
                className = currentValue;
                try {
                    map.put(type, Class.forName(className));
                } catch (ClassNotFoundException e) {
                    SQLException ex = new SQLException();
                    ex.initCause(e);
                    throw ex;
                }
                type = null;
                className = null;
            } else if ("max-field-size".equals(currentTagName)) { //$NON-NLS-1$
                webRs.setMaxFieldSize(parseInt(currentValue));
            } else if ("max-rows".equals(currentTagName)) { //$NON-NLS-1$
                webRs.setMaxRows(parseInt(currentValue));
            } else if ("query-timeout".equals(currentTagName)) { //$NON-NLS-1$
                webRs.setQueryTimeout(parseInt(currentValue));
            } else if ("read-only".equals(currentTagName)) { //$NON-NLS-1$
                webRs.setReadOnly(parseBoolean(currentValue));
            } else if ("rowset-type".equals(currentTagName)) { //$NON-NLS-1$
                webRs.setType(getType(currentValue));
            } else if ("show-deleted".equals(currentTagName)) { //$NON-NLS-1$
                webRs.setShowDeleted(parseBoolean(currentValue));
            } else if ("table-name".equals(currentTagName)) { //$NON-NLS-1$
                // currentValue can't be null. Or else it would throw
                // SQLException.
                if (currentValue != null) {
                    webRs.setTableName(currentValue);
                }
            } else if ("url".equals(currentTagName)) { //$NON-NLS-1$
                webRs.setUrl(currentValue);
            } else if ("sync-provider-name".equals(currentTagName)) { //$NON-NLS-1$
                webRs.setSyncProvider(currentValue);
            }
        }

        private void readMetadata() throws SQLException {
            RowSetMetaData md = (RowSetMetaData) webRs.getMetaData();
            if (md == null) {
                md = new RowSetMetaDataImpl();
                webRs.setMetaData(md);
            }

            if (currentTagName.equals("column-count")) { //$NON-NLS-1$
                columnCount = parseInt(currentValue);
                md.setColumnCount(columnCount);
            } else if (currentTagName.equals("column-index")) { //$NON-NLS-1$
                colIndex = parseInt(currentValue);
            } else if (currentTagName.equals("auto-increment")) { //$NON-NLS-1$
                md.setAutoIncrement(colIndex, parseBoolean(currentValue));
            } else if (currentTagName.equals("case-sensitive")) { //$NON-NLS-1$
                md.setCaseSensitive(colIndex, parseBoolean(currentValue));
            } else if (currentTagName.equals("currency")) { //$NON-NLS-1$
                md.setCurrency(colIndex, parseBoolean(currentValue));
            } else if (currentTagName.equals("nullable")) { //$NON-NLS-1$
                md.setNullable(colIndex, parseInt(currentValue));
            } else if (currentTagName.equals("signed")) { //$NON-NLS-1$
                md.setSigned(colIndex, parseBoolean(currentValue));
            } else if (currentTagName.equals("searchable")) { //$NON-NLS-1$
                md.setSearchable(colIndex, parseBoolean(currentValue));
            } else if (currentTagName.equals("column-display-size")) { //$NON-NLS-1$
                md.setColumnDisplaySize(colIndex, parseInt(currentValue));
            } else if (currentTagName.equals("column-label")) { //$NON-NLS-1$
                md.setColumnLabel(colIndex, currentValue);
            } else if (currentTagName.equals("column-name")) { //$NON-NLS-1$
                md.setColumnName(colIndex, currentValue);
            } else if (currentTagName.equals("schema-name")) { //$NON-NLS-1$
                md.setSchemaName(colIndex, currentValue);
            } else if (currentTagName.equals("column-precision")) { //$NON-NLS-1$
                md.setPrecision(colIndex, parseInt(currentValue));
            } else if (currentTagName.equals("column-scale")) { //$NON-NLS-1$
                md.setScale(colIndex, parseInt(currentValue));
            } else if (currentTagName.equals("table-name")) { //$NON-NLS-1$
                md.setTableName(colIndex, currentValue);
            } else if (currentTagName.equals("catalog-name")) { //$NON-NLS-1$
                md.setCatalogName(colIndex, currentValue);
            } else if (currentTagName.equals("column-type")) { //$NON-NLS-1$
                md.setColumnType(colIndex, parseInt(currentValue));
            } else if (currentTagName.equals("column-type-name")) { //$NON-NLS-1$
                md.setColumnTypeName(colIndex, currentValue);
            }

        }

        private void readData() throws SQLException {
            if ("columnValue".equals(currentTagName)) { //$NON-NLS-1$
                colIndex++;
                columnData.add(parseObject(currentValue));
            } else if ("updateValue".equals(currentTagName)) { //$NON-NLS-1$
                updateData.add(parseObject(currentValue));
                updateColIndex.add(Integer.valueOf(colIndex));
            }
        }

        @SuppressWarnings("boxing")
        private Object parseObject(String value) throws SQLException {
            if (value == null) {
                return null;
            }

            Object obj = null;
            ResultSetMetaData rsmd = webRs.getMetaData();
            int colType = rsmd.getColumnType(colIndex);
            switch (colType) {
            case Types.BIGINT:
                obj = parseLong(value);
                break;
            case Types.BIT:
            case Types.BOOLEAN:
                obj = parseBoolean(value);
                break;
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                obj = value;
                break;
            case Types.DATE:
                obj = parseDate(value);
                break;
            case Types.FLOAT:
            case Types.DOUBLE:
                obj = parseDouble(value);
                break;
            case Types.NUMERIC:
            case Types.DECIMAL:
                obj = parseBigDecimal(value);
                break;
            case Types.REAL:
                obj = parseFloat(value);
                break;
            case Types.TIME:
                obj = parseTime(value);
                break;
            case Types.TIMESTAMP:
                obj = parseTimestamp(value);
                break;
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                obj = parseInt(value);
                break;
            }
            return obj;
        }

        private int getType(String type) throws SQLException {
            type = type.trim();
            if ("ResultSet.TYPE_FORWARD_ONLY".equals(type)) { //$NON-NLS-1$
                return ResultSet.TYPE_FORWARD_ONLY;
            } else if ("ResultSet.TYPE_SCROLL_INSENSITIVE".equals(type)) { //$NON-NLS-1$
                return ResultSet.TYPE_SCROLL_INSENSITIVE;
            } else if ("ResultSet.TYPE_SCROLL_SENSITIVE".equals(type)) { //$NON-NLS-1$
                return ResultSet.TYPE_SCROLL_SENSITIVE;
            }
            // rowset.27=Illegal input string "{0}"
            throw new SQLException(Messages.getString("rowset.27", type)); //$NON-NLS-1$
        }

        private void initRow() {
            if ("currentRow".equals(currentTagName) //$NON-NLS-1$
                    || "deleteRow".equals(currentTagName) //$NON-NLS-1$
                    || "insertRow".equals(currentTagName) //$NON-NLS-1$
                    || "modifyRow".equals(currentTagName)) { //$NON-NLS-1$
                columnData = new ArrayList<Object>();
                updateData = new ArrayList<Object>();
                updateColIndex = new ArrayList<Integer>();
                colIndex = 0;
            }
        }

        private void insertRow(String tagName) throws SQLException {
            boolean isInsertRow = false;
            if ("currentRow".equals(tagName)) { //$NON-NLS-1$
                currentRow = new CachedRow(columnData.toArray());
                isInsertRow = true;
            } else if ("deleteRow".equals(tagName)) { //$NON-NLS-1$
                currentRow = new CachedRow(columnData.toArray());
                currentRow.setDelete();
                isInsertRow = true;
            } else if ("insertRow".equals(tagName)) { //$NON-NLS-1$
                currentRow = new CachedRow(columnData.toArray());
                currentRow.setInsert();
                isInsertRow = true;
            } else if ("modifyRow".equals(tagName)) { //$NON-NLS-1$
                currentRow = new CachedRow(columnData.toArray());
                currentRow.setUpdate();
                isInsertRow = true;
            }
            if (isInsertRow) {
                for (int i = 0; i < updateData.size(); i++) {
                    Object updateValue = updateData.get(i);
                    Integer updateCol = updateColIndex.get(i);
                    currentRow.updateObject(updateCol.intValue(), updateValue);
                }
                rows.add(currentRow);
                currentRow = null;
            }
        }

        private int parseInt(String value) throws SQLException {
            if (value == null) {
                throw new SQLException(Messages.getString("rowset.27", value)); //$NON-NLS-1$
            }

            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                // rowset.27=Illegal input string "{0}"
                SQLException ex = new SQLException(Messages.getString(
                        "rowset.27", value)); //$NON-NLS-1$
                ex.initCause(e);
                throw ex;
            }
        }

        private long parseLong(String value) throws SQLException {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                // rowset.27=Illegal input string "{0}"
                SQLException ex = new SQLException(Messages.getString(
                        "rowset.27", value)); //$NON-NLS-1$
                ex.initCause(e);
                throw ex;
            }
        }

        private float parseFloat(String value) throws SQLException {
            try {
                return Float.parseFloat(value.trim());
            } catch (NumberFormatException e) {
                // rowset.27=Illegal input string "{0}"
                SQLException ex = new SQLException(Messages.getString(
                        "rowset.27", value)); //$NON-NLS-1$
                ex.initCause(e);
                throw ex;
            }
        }

        private double parseDouble(String value) throws SQLException {
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException e) {
                // rowset.27=Illegal input string "{0}"
                SQLException ex = new SQLException(Messages.getString(
                        "rowset.27", value)); //$NON-NLS-1$
                ex.initCause(e);
                throw ex;
            }
        }

        private Date parseDate(String value) throws SQLException {
            try {
                return new Date(Long.parseLong(value.trim()));
            } catch (NumberFormatException e) {
                // rowset.27=Illegal input string "{0}"
                SQLException ex = new SQLException(Messages.getString(
                        "rowset.27", value)); //$NON-NLS-1$
                ex.initCause(e);
                throw ex;
            }
        }

        private Time parseTime(String value) throws SQLException {
            try {
                return new Time(Long.parseLong(value.trim()));
            } catch (NumberFormatException e) {
                // rowset.27=Illegal input string "{0}"
                SQLException ex = new SQLException(Messages.getString(
                        "rowset.27", value)); //$NON-NLS-1$
                ex.initCause(e);
                throw ex;
            }
        }

        private Timestamp parseTimestamp(String value) throws SQLException {
            try {
                return new Timestamp(Long.parseLong(value.trim()));
            } catch (NumberFormatException e) {
                // rowset.27=Illegal input string "{0}"
                SQLException ex = new SQLException(Messages.getString(
                        "rowset.27", value)); //$NON-NLS-1$
                ex.initCause(e);
                throw ex;
            }
        }

        private boolean parseBoolean(String value) {
            if (value == null) {
                return false;
            }

            return Boolean.parseBoolean(value.trim());
        }

        private BigDecimal parseBigDecimal(String value) throws SQLException {
            try {
                return new BigDecimal(value.trim());
            } catch (NumberFormatException e) {
                // rowset.27=Illegal input string "{0}"
                SQLException ex = new SQLException(Messages.getString(
                        "rowset.27", value)); //$NON-NLS-1$
                ex.initCause(e);
                throw ex;
            }
        }
    }
}
