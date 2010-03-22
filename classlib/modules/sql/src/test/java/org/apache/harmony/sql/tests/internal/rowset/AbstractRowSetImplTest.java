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

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Locale;

import javax.sql.RowSetEvent;
import javax.sql.RowSetListener;

import org.apache.harmony.sql.internal.rowset.AbstractRowSetImpl;

import junit.framework.TestCase;

public class AbstractRowSetImplTest extends TestCase {
    private AbstractRowSetImpl rowset;

    private Object[] params;

    public void setUp() {
        rowset = new AbstractRowSetImpl();
    }

    public void tearDown() {
        rowset = null;
    }

    private class MockRowSetListener implements RowSetListener {
        boolean cursorMoved;

        boolean rowChanged;

        boolean rowsetChanged;

        public void cursorMoved(RowSetEvent theEvent) {
            cursorMoved = cursorMoved ? false : true;

        }

        public void rowChanged(RowSetEvent theEvent) {
            rowChanged = rowChanged ? false : true;
        }

        public void rowSetChanged(RowSetEvent theEvent) {
            rowsetChanged = rowsetChanged ? false : true;
        }

    }

    /**
     * @add tests
     *      {@link javax.sql.rowset.BaseRowSet#addRowSetListener(javax.sql.RowSetListener)}
     * @add tests
     *      {@link javax.sql.rowset.BaseRowSet#removeRowSetListener(RowSetListener)}
     */
    public void test_addRowSetListener_Ljavax_sql_RowsetListener()
            throws SQLException {
        final class MockAbstractRowSetImpl extends AbstractRowSetImpl {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            protected void notifyCursorMoved() throws SQLException {
                super.notifyCursorMoved();
            }

            protected void notifyRowChanged() throws SQLException {
                super.notifyRowChanged();
            }

            protected void notifyRowSetChanged() throws SQLException {
                super.notifyRowSetChanged();
            }

        }
        MockRowSetListener listener = new MockRowSetListener();
        MockAbstractRowSetImpl rowset = new MockAbstractRowSetImpl();
        rowset.addRowSetListener(listener);
        assertFalse(listener.cursorMoved);
        assertFalse(listener.rowChanged);
        assertFalse(listener.rowsetChanged);
        rowset.notifyCursorMoved();
        assertTrue(listener.cursorMoved);
        rowset.notifyRowChanged();
        assertTrue(listener.rowChanged);
        rowset.notifyRowSetChanged();
        assertTrue(listener.rowsetChanged);

        // this call won't make any effect
        rowset.removeRowSetListener(null);
        rowset.notifyCursorMoved();
        rowset.notifyRowChanged();
        rowset.notifyRowSetChanged();
        assertFalse(listener.cursorMoved);
        assertFalse(listener.rowChanged);
        assertFalse(listener.rowsetChanged);

        // this call will remove the registered listener
        rowset.removeRowSetListener(listener);
        rowset.notifyCursorMoved();
        rowset.notifyRowChanged();
        rowset.notifyRowSetChanged();
        assertFalse(listener.cursorMoved);
        assertFalse(listener.rowChanged);
        assertFalse(listener.rowsetChanged);
    }

    /**
     *@add tests {@link javax.sql.rowset.BaseRowSet#getUsername()}.
     */
    public void test_getUsername() {
        assertNull("username should init as null", rowset.getUsername());
    }

    /**
     * @add tests {@link javax.sql.rowset.BaseRowSet#getPassword()}.
     */
    public void test_getPassword() {
        assertNull("password should init as null", rowset.getPassword());
    }

    /**
     * Test method for {@link javax.sql.rowset.BaseRowSet#setInt(int, int)}.
     */
    public void test_setInt_II() throws SQLException {
        try {
            rowset.setInt(0, 0);
            fail("should throw SQLException");
        } catch (SQLException sqlE) {
            // expected
        }
        rowset.setInt(1, 1);
        params = rowset.getParams();
        assertEquals("set int failure", 1, ((Integer) params[0]).intValue());

    }

    /**
     * Test method for {@link javax.sql.rowset.BaseRowSet#setLong(int, long)}.
     */
    public void test_setLong_IJ() throws SQLException {
        try {
            rowset.setLong(0, 0L);
            fail("should throw SQLException");
        } catch (SQLException ex) {
            // expected
        }
        rowset.setLong(1, 1000000L);
        params = rowset.getParams();
        assertEquals("set long failure", 1000000L, ((Long) params[0])
                .longValue());
    }

    /**
     * Test method for {@link javax.sql.rowset.BaseRowSet#setFloat(int, float)}.
     */
    public void test_setFloat_IF() throws SQLException {
        try {
            rowset.setFloat(0, 0f);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        rowset.setFloat(1, 0.5f);
        params = rowset.getParams();
        assertEquals("set float failure", 0.5f, ((Float) params[0])
                .floatValue());
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.BaseRowSet#setDouble(int, double)}.
     * 
     * @throws SQLException
     */
    public void test_setDouble_ID() throws SQLException {
        try {
            rowset.setDouble(0, 0);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        rowset.setDouble(1, 1.5);
        params = rowset.getParams();
        assertEquals("set double failure", 1.5, ((Double) params[0])
                .doubleValue());
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.BaseRowSet#setBigDecimal(int, java.math.BigDecimal)}
     * 
     * @throws SQLException
     */
    public void test_setBigDecimal_ILjava_math_Bigdecimal() throws SQLException {
        BigDecimal decimal = new BigDecimal(1000);
        try {
            rowset.setBigDecimal(0, decimal);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        rowset.setBigDecimal(1, decimal);
        params = rowset.getParams();
        assertEquals("set big decimal failure", decimal, (BigDecimal) params[0]);
    }

    /**
     * Test method for {@link javax.sql.rowset.BaseRowSet#setBytes(int, byte[])}
     * 
     * @throws SQLException
     */
    public void test_setBytes_IB() throws SQLException {
        try {
            rowset.setBytes(0, null);
        } catch (SQLException e) {
            // expected
        }
        byte[] bytes = new byte[] { (byte) 1, (byte) 2 };
        rowset.setBytes(1, bytes);
        params = rowset.getParams();
        assertEquals("set bytes failure", bytes, ((byte[]) params[0]));
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.BaseRowSet#setDate(int, java.sql.Date)}.
     * 
     * @throws SQLException
     */
    public void test_setDate_ILjava_sql_Date() throws SQLException {
        Date date = new Date(100000000000L);
        try {
            rowset.setDate(0, date);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        rowset.setDate(1, date);
        params = rowset.getParams();
        assertEquals("setDate(int,Date) failure", date, (Date) params[0]);
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.BaseRowSet#setTime(int, java.sql.Time)}.
     * 
     * @throws SQLException
     */
    public void test_setTime_ILjava_sql_Time() throws SQLException {
        Time time = new Time(1000000000L);
        try {
            rowset.setTime(0, time);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        rowset.setTime(1, time);
        params = rowset.getParams();
        assertEquals("setTime(int,Time) failure", time, (Time) params[0]);
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.BaseRowSet#setTimestamp(int, java.sql.Timestamp)}
     * 
     * @throws SQLException
     */
    public void test_setTimestamp_ILjava_sql_Timestamp() throws SQLException {
        Timestamp stamp = new Timestamp(10000000000L);
        try {
            rowset.setTimestamp(0, stamp);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        rowset.setTimestamp(1, stamp);
        params = rowset.getParams();
        assertEquals("setTimeStamp(int,Timestamp) failure", stamp,
                (Timestamp) params[0]);
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.BaseRowSet#setAsciiStream(int, java.io.InputStream, int)}
     * 
     * @throws SQLException
     * @throws IOException
     */
    public void test_setAsciiStream_ILjava_io_InputStreamI()
            throws SQLException, IOException {
        InputStream in = new ByteArrayInputStream(new byte[] { (byte) 1,
                (byte) 10 });
        try {
            rowset.setAsciiStream(0, null, 0);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            rowset.setAsciiStream(0, in, 0);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        rowset.setAsciiStream(1, in, 2);
        params = rowset.getParams();
        Object[] array = (Object[]) params[0];
        assertEquals("first element in params " + array[0]
                + " did not equal to Inputstream", in, (InputStream) array[0]);
        in.close();
        assertEquals("second element in params " + array[1]
                + " did not equal to 2", 2, array[1]);
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.BaseRowSet#setBinaryStream(int, java.io.InputStream, int)}
     * 
     * @throws SQLException
     * @throws IOException
     */
    public void test_setBinaryStream_ILjava_io_InputStreamI()
            throws SQLException, IOException {
        InputStream in = new ByteArrayInputStream(new byte[] { (byte) 1,
                (byte) 10 });
        try {
            rowset.setBinaryStream(0, null, 0);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            rowset.setBinaryStream(0, in, 0);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        rowset.setBinaryStream(1, in, 2);
        params = rowset.getParams();
        Object[] array = (Object[]) params[0];
        assertEquals("first element in params " + array[0]
                + " did not equal to Inputstream", in, (InputStream) array[0]);
        in.close();
        assertEquals("second element in params " + array[1]
                + " did not equal to 2", 2, array[1]);
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.BaseRowSet#setUnicodeStream(int, java.io.InputStream, int)}
     * 
     * @throws SQLException
     */
    @SuppressWarnings("deprecation")
    public void test_setUnicodeStream_ILjava_io_InputStreamI()
            throws SQLException {
        InputStream in = new ByteArrayInputStream(new byte[] { (byte) 1,
                (byte) 10 });
        try {
            rowset.setUnicodeStream(0, null, 0);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            rowset.setUnicodeStream(0, in, 0);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        rowset.setUnicodeStream(1, in, 2);
        params = rowset.getParams();
        Object[] array = (Object[]) params[0];
        assertEquals("first element in params " + array[0]
                + " did not equal to Inputstream", in, (InputStream) array[0]);
        assertEquals("second element in params " + array[1]
                + " did not equal to 2", 2, array[1]);
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.BaseRowSet#setCharacterStream(int, java.io.Reader, int)}
     * 
     * @throws SQLException
     */
    public void test_setCharacterStream_ILjava_io_ReaderI() throws SQLException {
        Reader reader = new CharArrayReader(new char[] { 'a', 'b' });
        try {
            rowset.setCharacterStream(0, null, 0);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            rowset.setCharacterStream(0, reader, 0);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        rowset.setCharacterStream(1, reader, 2);
        // rowset.getCharacterStream(1);
        params = rowset.getParams();
        Object[] array = (Object[]) params[0];
        assertEquals("first element in params " + array[0]
                + " did not equal to Inputstream", reader, (Reader) array[0]);
        assertEquals("second element in params " + array[1]
                + " did not equal to 2", 2, array[1]);
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.BaseRowSet#setObject(int, java.lang.Object, int, int)}
     * 
     * @throws SQLException
     */
    public void test_setObject_ILjava_lang_ObjectII() throws SQLException {
        try {
            rowset.setObject(0, null, 0, 0);
            fail("should throw SQLExceptions");
        } catch (SQLException e) {
            // expected
        }
        rowset.setObject(1, null, 0, 0);// should pass
        rowset.setObject(2, new Float(0.55f), Types.NUMERIC, 0);// should pass
        rowset.setObject(3, new Double(100.0003), Types.DECIMAL, 4);
        params = rowset.getParams();
        Object[] array1 = (Object[]) params[0];
        assertNull(array1[0]);
        Object[] array2 = (Object[]) params[1];
        assertEquals(0.55f, ((Float) array2[0]).floatValue());
        assertEquals(Types.NUMERIC, array2[1]);
        Object[] array3 = (Object[]) params[2];
        assertEquals(100.0003, ((Double) array3[0]).doubleValue());
        assertEquals(Types.DECIMAL, array3[1]);
        assertEquals(4, array3[2]);
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.BaseRowSet#setObject(int, java.lang.Object, int)}
     * 
     * @throws SQLException
     */
    public void test_setObject_ILjava_lang_ObjectI() throws SQLException {
        try {
            rowset.setObject(0, null, 0);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        rowset.setObject(1, null, 0);// should pass
        rowset.setObject(1, new Integer(10), Types.VARCHAR);// should pass
        rowset.setObject(1, new Integer(10), Types.INTEGER);
        params = rowset.getParams();
        Object[] array = (Object[]) params[0];
        assertEquals(10, ((Integer) array[0]).intValue());
        assertEquals(Types.INTEGER, array[1]);
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.BaseRowSet#setObject(int, java.lang.Object)}.
     * 
     * @throws SQLException
     */
    public void test_setObject_IntLjava_lang_Object() throws SQLException {
        try {
            rowset.setObject(0, null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        rowset.setObject(1, new String("test"));
        params = rowset.getParams();
        assertEquals("test", (String) params[0]);
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.BaseRowSet#setTime(int, java.sql.Time, java.util.Calendar)}
     * 
     * @throws SQLException
     */
    public void test_setTime_ILjava_sql_TimeLjava_util_Calendar()
            throws SQLException {
        try {
            rowset.setTimestamp(0, null, null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        rowset.setTime(1, null, null);// should pass
        Time t = new Time(100000L);
        Calendar cal = Calendar.getInstance(Locale.CHINA);
        rowset.setTime(2, t, cal);
        params = rowset.getParams();
        Object[] array1 = (Object[]) params[0];
        assertNull(array1[0]);
        assertNull(array1[1]);
        Object[] array2 = (Object[]) params[1];
        assertEquals(t, array2[0]);
        assertEquals(cal, array2[1]);
    }

    /**
     * Test method for
     * {@link javax.sql.rowset.BaseRowSet#setTimestamp(int, java.sql.Timestamp, java.util.Calendar)}
     * 
     * @throws SQLException
     */
    public void test_setTimestamp_ILjava_sql_TimestampLjava_util_Calendar()
            throws SQLException {
        try {
            rowset.setTimestamp(0, null, null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        Timestamp ts = new Timestamp(1000000000000000L);
        Calendar cal = Calendar.getInstance(Locale.CHINA);
        rowset.setTimestamp(1, null, null);
        rowset.setTimestamp(2, ts, cal);
        params = rowset.getParams();
        Object[] array1 = (Object[]) params[0];
        Object[] array2 = (Object[]) params[1];
        assertNull(array1[0]);
        assertNull(array1[1]);
        assertEquals(ts, array2[0]);
        assertEquals(cal, array2[1]);
    }

    /**
     * @throws SQLException
     * @add tests
     *      {@link javax.sql.rowset.BaseRowSet#setDate(int, Date, Calendar)}
     */
    public void test_setDate_ILjava_sql_DateLjava_util_Calendar()
            throws SQLException {
        try {
            rowset.setDate(0, null, null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        Date date = new Date(10000000000L);
        Calendar cal = Calendar.getInstance(Locale.CHINA);
        rowset.setDate(1, null, null);
        rowset.setDate(2, null, cal);
        rowset.setDate(3, date, null);
        rowset.setDate(4, date, cal);
        params = rowset.getParams();
        Object[] array1 = (Object[]) params[0];
        Object[] array2 = (Object[]) params[1];
        Object[] array3 = (Object[]) params[2];
        Object[] array4 = (Object[]) params[3];
        assertNull(array1[0]);
        assertNull(array1[1]);
        assertNull(array2[0]);
        assertEquals(cal, array2[1]);
        assertEquals(date, array3[0]);
        assertNull(array3[1]);
        assertEquals(date, array4[0]);
        assertEquals(cal, array4[1]);
    }

    /**
     * @throws SQLException
     * @add tests {@link javax.sql.rowset.BaseRowSet#setQueryTimeout(int)}
     */
    public void test_setQueryTimeOut_I() throws SQLException {
        try {
            rowset.setQueryTimeout(-1);
        } catch (SQLException e) {
            // expected
        }
        rowset.setQueryTimeout(10);
        assertEquals(10, rowset.getQueryTimeout());
    }

}
