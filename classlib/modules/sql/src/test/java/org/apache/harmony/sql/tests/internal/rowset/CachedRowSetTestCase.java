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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;

import javax.sql.rowset.CachedRowSet;

import junit.framework.TestCase;

public class CachedRowSetTestCase extends TestCase {
    public static final String DERBY_URL_Create = "jdbc:derby:resources/TESTDB;create=true";

    public static final String DERBY_URL = "jdbc:derby:resources/TESTDB";

    protected Connection conn = null;

    protected Statement st;

    protected ResultSet rs;

    protected CachedRowSet crset;

    protected CachedRowSet noInitialCrset;

    public final static int DEFAULT_COLUMN_COUNT = 12;

    public final static int DEFAULT_ROW_COUNT = 4;

    public void setUp() throws Exception {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");

        try {
            conn = DriverManager.getConnection(DERBY_URL);
        } catch (SQLException e) {
            conn = DriverManager.getConnection(DERBY_URL_Create);
        }

        st = conn.createStatement();
        rs = conn.getMetaData().getTables(null, "APP", "USER_INFO", null);
        String createTableSQL = "create table USER_INFO (ID INTEGER NOT NULL,NAME VARCHAR(10) NOT NULL, BIGINT_T BIGINT, "
                + "NUMERIC_T NUMERIC, DECIMAL_T DECIMAL, SMALLINT_T SMALLINT, FLOAT_T FLOAT, REAL_T REAL, DOUBLE_T DOUBLE,"
                + "DATE_T DATE, TIME_T TIME, TIMESTAMP_T TIMESTAMP)";
        String alterTableSQL = "ALTER TABLE USER_INFO  ADD CONSTRAINT USER_INFO_PK Primary Key (ID)";

        if (!rs.next()) {
            st.execute(createTableSQL);
            st.execute(alterTableSQL);
        }

        insertData();
        rs = st.executeQuery("select * from USER_INFO");
        try {
            crset = (CachedRowSet) Class.forName(
                    "com.sun.rowset.CachedRowSetImpl").newInstance();
            noInitialCrset = (CachedRowSet) Class.forName(
                    "com.sun.rowset.CachedRowSetImpl").newInstance();
        } catch (ClassNotFoundException e) {

            crset = (CachedRowSet) Class.forName(
                    "org.apache.harmony.sql.internal.rowset.CachedRowSetImpl")
                    .newInstance();
            noInitialCrset = (CachedRowSet) Class.forName(
                    "org.apache.harmony.sql.internal.rowset.CachedRowSetImpl")
                    .newInstance();

            System.setProperty("Testing Harmony", "true");
        }
        crset.populate(rs);
        rs = st.executeQuery("select * from USER_INFO");
        crset.setUrl(DERBY_URL);
    }

    public void tearDown() throws Exception {
        if (rs != null) {
            rs.close();
        }
        if (crset != null) {
            crset.close();
        }
        if (st != null) {
            st.close();
        }
        if (conn != null) {
            /*
             * if doesn't call rollback, ri will throw exception then block
             * java.sql.SQLException: Invalid transaction state.
             */
            conn.rollback();
            conn.close();
        }
    }

    protected void insertData() throws Exception {

        st.executeUpdate("delete from USER_INFO");

        // first row
        st.executeUpdate("insert into USER_INFO(ID,NAME) values (1,'hermit')");
        // second row
        st.executeUpdate("insert into USER_INFO(ID,NAME) values (2,'test')");

        String insertSQL = "INSERT INTO USER_INFO(ID, NAME, BIGINT_T, NUMERIC_T, DECIMAL_T, SMALLINT_T, "
                + "FLOAT_T, REAL_T, DOUBLE_T, DATE_T, TIME_T, TIMESTAMP_T) VALUES(?, ?, ?, ?, ?, ?,"
                + "?, ?, ?, ?, ?, ?)";
        PreparedStatement preStmt = conn.prepareStatement(insertSQL);
        // third row
        preStmt.setInt(1, 3);
        preStmt.setString(2, "test3");
        preStmt.setLong(3, 3333L);
        preStmt.setBigDecimal(4, new BigDecimal("123.6521"));
        preStmt.setBigDecimal(5, new BigDecimal("85.31"));
        preStmt.setInt(6, 13);
        preStmt.setFloat(7, 3.7F);
        preStmt.setFloat(8, 3.888F);
        preStmt.setDouble(9, 3.99999999);
        preStmt.setDate(10, new Date(52365412356663L));
        preStmt.setTime(11, new Time(96655422555551L));
        preStmt.setTimestamp(12, new Timestamp(52365412356663L));
        preStmt.executeUpdate();
        // fourth row
        preStmt.setInt(1, 4);
        preStmt.setString(2, "test4");
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

        if (preStmt != null) {
            preStmt.close();
        }
    }

    protected void insertMoreData(int rows) throws Exception {
        String insertSQL = "INSERT INTO USER_INFO(ID, NAME, BIGINT_T, NUMERIC_T, DECIMAL_T, SMALLINT_T, "
                + "FLOAT_T, REAL_T, DOUBLE_T, DATE_T, TIME_T, TIMESTAMP_T) VALUES(?, ?, ?, ?, ?, ?,"
                + "?, ?, ?, ?, ?, ?)";
        PreparedStatement preStmt = conn.prepareStatement(insertSQL);

        // insert 15 rows
        for (int i = DEFAULT_ROW_COUNT + 1; i < DEFAULT_ROW_COUNT + rows + 1; i++) {
            preStmt.setInt(1, i);
            preStmt.setString(2, "test" + i);
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
        }

        if (preStmt != null) {
            preStmt.close();
        }
    }

    protected void isMetaDataEquals(ResultSetMetaData expected,
            ResultSetMetaData actual) throws SQLException {
        assertEquals(expected.getColumnCount(), actual.getColumnCount());
        isMetaDataEqualsInColCount(expected, 1, expected.getColumnCount(),
                actual, 1);
        assertEquals(expected.getTableName(1), actual.getTableName(1));
    }

    protected void isMetaDataEqualsInColCount(ResultSetMetaData expected,
            int fromIndexInExpected, int toIndexInExpected,
            ResultSetMetaData actual, int fromColInActual) throws SQLException {
        for (int column = fromIndexInExpected; column <= toIndexInExpected; column++) {
            assertEquals(expected.isAutoIncrement(column), actual
                    .isAutoIncrement(fromColInActual));
            assertEquals(expected.isCaseSensitive(column), actual
                    .isCaseSensitive(fromColInActual));
            assertEquals(expected.isCurrency(column), actual
                    .isCurrency(fromColInActual));
            assertEquals(expected.isDefinitelyWritable(column), actual
                    .isDefinitelyWritable(fromColInActual));
            assertEquals(expected.isReadOnly(column), actual
                    .isReadOnly(fromColInActual));
            assertEquals(expected.isSearchable(column), actual
                    .isSearchable(fromColInActual));
            assertEquals(expected.isSigned(column), actual
                    .isSigned(fromColInActual));
            assertEquals(expected.isWritable(column), actual
                    .isWritable(fromColInActual));
            assertEquals(expected.isNullable(column), actual
                    .isNullable(fromColInActual));
            assertEquals(expected.getCatalogName(column), actual
                    .getCatalogName(fromColInActual));
            assertEquals(expected.getColumnClassName(column), actual
                    .getColumnClassName(fromColInActual));
            assertEquals(expected.getColumnDisplaySize(column), actual
                    .getColumnDisplaySize(fromColInActual));
            assertEquals(expected.getColumnLabel(column), actual
                    .getColumnLabel(fromColInActual));
            assertEquals(expected.getColumnName(column), actual
                    .getColumnName(fromColInActual));
            assertEquals(expected.getColumnType(column), actual
                    .getColumnType(fromColInActual));
            assertEquals(expected.getColumnTypeName(column), actual
                    .getColumnTypeName(fromColInActual));
            assertEquals(expected.getPrecision(column), actual
                    .getPrecision(fromColInActual));
            assertEquals(expected.getScale(column), actual
                    .getScale(fromColInActual));
            assertEquals(expected.getSchemaName(column), actual
                    .getSchemaName(fromColInActual));
            fromColInActual++;
        }
    }

    protected CachedRowSet newNoInitialInstance() throws Exception {
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            return (CachedRowSet) Class.forName(
                    "org.apache.harmony.sql.internal.rowset.CachedRowSetImpl")
                    .newInstance();
        }
        return (CachedRowSet) Class.forName("com.sun.rowset.CachedRowSetImpl")
                .newInstance();
    }

    protected void reloadCachedRowSet() throws SQLException {
        rs = st.executeQuery("select * from USER_INFO");
        crset.populate(rs);
        rs = st.executeQuery("select * from USER_INFO");
        crset.setUrl(DERBY_URL);
    }

    public void testTestCase() throws Exception {
        // do nothing
    }

    public void createNewTable() throws Exception {
        st = conn.createStatement();
        rs = conn.getMetaData().getTables(null, "APP", "CUSTOMER_INFO", null);
        String createTableSQL = "create table CUSTOMER_INFO (ID INTEGER NOT NULL,NAME VARCHAR(30) NOT NULL)";
        String alterTableSQL = "ALTER TABLE CUSTOMER_INFO ADD CONSTRAINT CUSTOMER_INFO_PK Primary Key (ID)";

        if (!rs.next()) {
            st.execute(createTableSQL);
            st.execute(alterTableSQL);
        }

        st.executeUpdate("delete from CUSTOMER_INFO");
        st
                .executeUpdate("insert into CUSTOMER_INFO(ID,NAME) values (1111,'customer_one')");
        st
                .executeUpdate("insert into CUSTOMER_INFO(ID,NAME) values (5555,'customer_two')");
    }
}
