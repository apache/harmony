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

import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.RowSet;
import javax.sql.rowset.FilteredRowSet;
import javax.sql.rowset.JdbcRowSet;
import javax.sql.rowset.JoinRowSet;
import javax.sql.rowset.WebRowSet;

public class JoinRowSetTestCase extends CachedRowSetTestCase {

    protected JoinRowSet jrs;

    public void setUp() throws Exception {
        super.setUp();
        createBookTable();
        createNewTable();
        jrs = newJoinRowSet();
    }

    public void insertDataToCustomerTable() throws Exception {
        st.executeUpdate("delete from CUSTOMER_INFO");
        st
                .executeUpdate("insert into CUSTOMER_INFO(ID,NAME) values (1111,'customer_one')");
        st
                .executeUpdate("insert into CUSTOMER_INFO(ID,NAME) values (5555,'customer_two')");
        st
                .executeUpdate("insert into CUSTOMER_INFO(ID,NAME) values (3,'test3')");
        st
                .executeUpdate("insert into CUSTOMER_INFO(ID,NAME) values (4,'test4')");
    }

    public void createBookTable() throws Exception {
        st = conn.createStatement();
        rs = conn.getMetaData().getTables(null, "APP", "BOOKS", null);
        String createTableSQL = "create table BOOKS (AUTHORID INTEGER NOT NULL,SN VARCHAR(30) NOT NULL,NAME VARCHAR(30))";

        if (rs.next()) {
            st.execute("drop table BOOKS");
        }
        st.execute(createTableSQL);

        st
                .executeUpdate("insert into BOOKS(AUTHORID,SN,NAME) values (1,'sn1-1','test1')");
        st
                .executeUpdate("insert into BOOKS(AUTHORID,SN,NAME) values (1,'sn1-2','hermit')");
        st
                .executeUpdate("insert into BOOKS(AUTHORID,SN,NAME) values (2,'sn2-1','test')");
        st
                .executeUpdate("insert into BOOKS(AUTHORID,SN,NAME) values (3,'sn3-1','update3')");
        st
                .executeUpdate("insert into BOOKS(AUTHORID,SN,NAME) values (3,'sn3-1','test3')");
        st
                .executeUpdate("insert into BOOKS(AUTHORID,SN,NAME) values (4,'sn4-1','test4')");
        st
                .executeUpdate("insert into BOOKS(AUTHORID,SN,NAME) values (5,'sn5-1','test5')");
    }

    public void insertNullDataToBooks() throws Exception {
        st = conn.createStatement();
        st
                .executeUpdate("insert into BOOKS(AUTHORID,SN,NAME) values (1,'sn1-1','null')");

        st
                .executeUpdate("insert into BOOKS(AUTHORID,SN,NAME) values (2,'sn1-2','null')");
        st

                .executeUpdate("insert into BOOKS(AUTHORID,SN,NAME) values (3,'sn4-1','null')");

    }

    public void addUnsortableToBooksTable() throws Exception {
        st = conn.createStatement();
        st.executeUpdate("alter table BOOKS add VARCHAR_FOR_BIT_T VARCHAR(100) FOR BIT DATA");
        
        String insertSQL = "INSERT INTO BOOKS(AUTHORID, SN, NAME, VARCHAR_FOR_BIT_T) VALUES(?, ?, ?, ?)";
        PreparedStatement preStmt = conn.prepareStatement(insertSQL);

        byte[] bs = new byte[] { 1, 2, 3, 4, 5 };
        preStmt.setInt(1, 10);
        preStmt.setString(2, "sn10");
        preStmt.setString(3, "test10");
        preStmt.setBytes(4, bs);
        preStmt.executeUpdate();
        
        byte[] bs2 = new byte[] { 2, 3, 4, 5, 6, 7};
        
        preStmt.setInt(1, 11);
        preStmt.setString(2, "sn11");
        preStmt.setString(3, "test11");
        preStmt.setBytes(4, bs2);
        preStmt.executeUpdate();
        
        preStmt.setInt(1, 12);
        preStmt.setString(2, "sn12");
        preStmt.setString(3, "test12");
        preStmt.setBytes(4, bs);
        preStmt.executeUpdate();
        
        preStmt.setInt(1, 13);
        preStmt.setString(2, "sn13");
        preStmt.setString(3, "test13");
        preStmt.setBytes(4, bs2);
        preStmt.executeUpdate();
        
        
        if (preStmt != null) {
            preStmt.close();
        }
    }
    
    public void insertNullDataToUserInfo() throws Exception {
        st = conn.createStatement();
        st
                .executeUpdate("insert into USER_INFO(ID,NAME) values (235,'null')");
        st
                .executeUpdate("insert into USER_INFO(ID,NAME) values (357,'null')");
 
    }

    protected JoinRowSet newJoinRowSet() throws Exception {
        JoinRowSet jrs = null;
        try {
            jrs = (JoinRowSet) Class.forName("com.sun.rowset.JoinRowSetImpl")
                    .newInstance();
        } catch (ClassNotFoundException e) {
            jrs = (JoinRowSet) Class.forName(
                    "org.apache.harmony.sql.internal.rowset.JoinRowSetImpl")
                    .newInstance();
        }
        return jrs;
    }

    protected JdbcRowSet newJdbcRowSet() throws Exception {
        try {
            return (JdbcRowSet) Class.forName("com.sun.rowset.JdbcRowSetImpl")
                    .newInstance();
        } catch (ClassNotFoundException e) {
            return (JdbcRowSet) Class.forName(
                    "org.apache.harmony.sql.internal.rowset.JdbcRowSetImpl")
                    .newInstance();
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

    protected FilteredRowSet newFilterRowSet() throws Exception {
        try {
            return (FilteredRowSet) Class.forName(
                    "com.sun.rowset.FilteredRowSetImpl").newInstance();
        } catch (ClassNotFoundException e) {
            return (FilteredRowSet) Class
                    .forName(
                            "org.apache.harmony.sql.internal.rowset.FilteredRowSetImpl")
                    .newInstance();
        }
    }

    /*
     * Return the position of the first occurence of value in rowset on column,
     * based from 1. return -1 if not found.
     */
    protected int findValue(Object value, RowSet rowset, int column)
            throws SQLException {
        int index = 1;

        rowset.beforeFirst();

        while (rowset.next()) {
            if (value != null) {
                if (value.equals(rowset.getObject(column))) {
                    return index;
                }
            } else {
                if (rowset.getObject(column) == null) {
                    return index;
                }
            }
            index++;
        }
        return -1;
    }
    
    
    
    

    protected boolean dataEqualsIgnoreOrder(RowSet rowSet1, RowSet rowSet2)
            throws SQLException {
        // First decides if the column num are equal.
        int columnNum = rowSet1.getMetaData().getColumnCount();
        if (rowSet2.getMetaData().getColumnCount() != columnNum) {
            return false;
        }

        // Then decides if the row num are equal.
        rowSet1.beforeFirst();
        rowSet2.beforeFirst();

        while (rowSet1.next()) {
            rowSet2.next();
        }
        if (!rowSet2.isLast()) {
            return false;
        }

        // For each row in rowSet1, try to find an equal row in rowSet2.
        // If not found, return false.
        rowSet1.beforeFirst();

        while (rowSet1.next()) {
            boolean isRowFound = false;

            rowSet2.beforeFirst();
            while (rowSet2.next()) {
                int i = 1;
                for (i = 1; i <= columnNum; i++) {
                    if (rowSet1.getObject(i) == null) {
                        if (rowSet2.getObject(i) != null) {
                            break;
                        }
                    } else if (!rowSet1.getObject(i).equals(
                            rowSet2.getObject(i))) {
                        break;
                    }
                }

                if (i == columnNum + 1) {
                    // A equal row has found
                    isRowFound = true;
                    break;
                }
            }

            if (!isRowFound) {
                return false;
            }
        }

        // Since we have proved that for each row in rowSet1, we can find an
        // equal row in rowSet2, so the data in these to rowSet2 are equal,
        // ignoring order.
        return true;
    }
    
    

    protected boolean dataEqualsIgnoreOrderAndNullInR2(RowSet rowSet1, RowSet rowSet2, int nullColumn)
            throws SQLException {
        // First decides if the column num are equal.
        int columnNum = rowSet1.getMetaData().getColumnCount();
        if (rowSet2.getMetaData().getColumnCount() != columnNum) {
            return false;
        }

        // Then decides if the row num are equal.
        rowSet1.beforeFirst();
        rowSet2.beforeFirst();

        int notNullRowNum = 0;
        
        while (rowSet1.next()) {
              notNullRowNum++;
          
        }
        
        while (rowSet2.next()) {
            if (! "null".equals(rowSet2.getObject(nullColumn))) {
                notNullRowNum--;
            }
        }
        if (notNullRowNum != 0) {
            return false;
        }

        // For each row in rowSet1, try to find an equal row in rowSet2.
        // If not found, return false.
        rowSet1.beforeFirst();

        while (rowSet1.next()) {
            boolean isRowFound = false;

            rowSet2.beforeFirst();
            while (rowSet2.next()) {
                int i = 1;
                for (i = 1; i <= columnNum; i++) {
                    if (rowSet1.getObject(i) == null) {
                        if (rowSet2.getObject(i) != null) {
                            break;
                        }
                    } else if (!rowSet1.getObject(i).equals(
                            rowSet2.getObject(i))) {
                        break;
                    }
                }

                if (i == columnNum + 1) {
                    // A equal row has found
                    isRowFound = true;
                    break;
                }
            }

            if (!isRowFound) {
                return false;
            }
        }

        // Since we have proved that for each row in rowSet1, we can find an
        // equal row in rowSet2, so the data in these to rowSet2 are equal,
        // ignoring order.
        return true;
    }

}
