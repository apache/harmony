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

package javax.sql.rowset.serial;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Ref;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLOutput;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.harmony.sql.internal.nls.Messages;

public class SQLOutputImpl implements SQLOutput {
    private Vector attributes;

    private Map map;

    /**
     * Constructs a new SQLOutputImpl object using a list of attributes and a
     * custom name-type map. JDBC drivers will use this map to identify which
     * SQLData.writeSQL will be invoked.
     * 
     * @param attributes -
     *            the list of given attribute objects.
     * @param map -
     *            the UDT(user defined type) name-type map
     * @throws SQLException -
     *             if the attributes or the map is null
     */
    public SQLOutputImpl(Vector<?> attributes, Map<String, ?> map)
            throws SQLException {
        if (null == attributes || null == map) {
            throw new SQLException(Messages.getString("sql.33")); //$NON-NLS-1$
        }
        this.attributes = attributes;
        this.map = map;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeArray(Array)
     */
    @SuppressWarnings("unchecked")
    public void writeArray(Array theArray) throws SQLException {
        if (theArray != null) {
            SerialArray serialArray = new SerialArray(theArray, map);
            attributes.addElement(serialArray);
        } else {
            attributes.addElement(theArray);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeAsciiStream(InputStream)
     */
    @SuppressWarnings("unchecked")
    public void writeAsciiStream(InputStream theStream) throws SQLException {
        BufferedReader br = new BufferedReader(new InputStreamReader(theStream));
        StringBuilder stringBuffer = new StringBuilder();
        String line;
        try {
            line = br.readLine();
            while (line != null) {
                stringBuffer.append(line);
                line = br.readLine();
            }
            attributes.addElement(stringBuffer.toString());
        } catch (IOException e) {
            throw new SQLException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeBigDecimal(BigDecimal)
     */
    @SuppressWarnings("unchecked")
    public void writeBigDecimal(BigDecimal theBigDecimal) throws SQLException {
        attributes.addElement(theBigDecimal);
    }

    /**
     * {@inheritDoc}
     * 
     * FIXME So far NO difference has been detected between writeBinaryStream
     * and writeAsciiStream in RI. Keep their implementation same temporarily
     * until some bug is found.
     * 
     * @see java.sql.SQLOutput#writeBinaryStream(InputStream)
     */
    @SuppressWarnings("unchecked")
    public void writeBinaryStream(InputStream theStream) throws SQLException {
        writeAsciiStream(theStream);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeBlob(Blob)
     */
    @SuppressWarnings("unchecked")
    public void writeBlob(Blob theBlob) throws SQLException {
        if (theBlob != null) {
            SerialBlob serialBlob = new SerialBlob(theBlob);
            attributes.addElement(serialBlob);
        } else {
            attributes.addElement(theBlob);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeBoolean(boolean)
     */
    @SuppressWarnings( { "boxing", "unchecked" })
    public void writeBoolean(boolean theFlag) throws SQLException {
        attributes.addElement(theFlag);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeByte(byte)
     */
    @SuppressWarnings( { "boxing", "unchecked" })
    public void writeByte(byte theByte) throws SQLException {
        attributes.addElement(theByte);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeBytes(byte[])
     */
    @SuppressWarnings( { "boxing", "unchecked" })
    public void writeBytes(byte[] theBytes) throws SQLException {
        attributes.addElement(theBytes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeCharacterStream(Reader)
     */
    @SuppressWarnings("unchecked")
    public void writeCharacterStream(Reader theStream) throws SQLException {
        BufferedReader br = new BufferedReader(theStream);
        StringBuilder stringBuffer = new StringBuilder();
        String line;
        try {
            line = br.readLine();
            while (line != null) {
                stringBuffer.append(line);
                line = br.readLine();
            }
            attributes.addElement(stringBuffer.toString());
        } catch (IOException e) {
            throw new SQLException();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeClob(Clob)
     */
    @SuppressWarnings("unchecked")
    public void writeClob(Clob theClob) throws SQLException {
        if (theClob != null) {
            SerialClob serialClob = new SerialClob(theClob);
            attributes.addElement(serialClob);
        } else {
            attributes.addElement(theClob);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeDate(Date)
     */
    @SuppressWarnings("unchecked")
    public void writeDate(Date theDate) throws SQLException {
        attributes.addElement(theDate);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeDouble(double)
     */
    @SuppressWarnings( { "boxing", "unchecked" })
    public void writeDouble(double theDouble) throws SQLException {
        attributes.addElement(theDouble);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeFloat(float)
     */
    @SuppressWarnings( { "boxing", "unchecked" })
    public void writeFloat(float theFloat) throws SQLException {
        attributes.addElement(theFloat);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeInt(int)
     */
    @SuppressWarnings( { "boxing", "unchecked" })
    public void writeInt(int theInt) throws SQLException {
        attributes.addElement(theInt);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeLong(long)
     */
    @SuppressWarnings( { "boxing", "unchecked" })
    public void writeLong(long theLong) throws SQLException {
        attributes.addElement(theLong);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeObject(SQLData)
     */
    @SuppressWarnings("unchecked")
    public void writeObject(SQLData theObject) throws SQLException {
        if (theObject == null) {
            attributes.addElement(null);
        } else {
            attributes
                    .addElement(new SerialStruct(theObject, new HashMap(map)));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeRef(Ref)
     */
    @SuppressWarnings("unchecked")
    public void writeRef(Ref theRef) throws SQLException {
        if (theRef != null) {
            SerialRef serialRef = new SerialRef(theRef);
            attributes.addElement(serialRef);
        } else {
            attributes.addElement(theRef);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeShort(short)
     */
    @SuppressWarnings( { "boxing", "unchecked" })
    public void writeShort(short theShort) throws SQLException {
        attributes.addElement(theShort);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeString(String)
     */
    @SuppressWarnings("unchecked")
    public void writeString(String theString) throws SQLException {
        attributes.addElement(theString);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeStruct(Struct)
     */
    @SuppressWarnings("unchecked")
    public void writeStruct(Struct theStruct) throws SQLException {
        if (theStruct != null) {
            SerialStruct serialStruct = new SerialStruct(theStruct, map);
            attributes.addElement(serialStruct);
        } else {
            attributes.addElement(theStruct);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeTime(Time)
     */
    @SuppressWarnings("unchecked")
    public void writeTime(Time theTime) throws SQLException {
        attributes.addElement(theTime);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeTimestamp(Timestamp)
     */
    @SuppressWarnings("unchecked")
    public void writeTimestamp(Timestamp theTimestamp) throws SQLException {
        attributes.addElement(theTimestamp);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.SQLOutput#writeURL(URL)
     */
    @SuppressWarnings("unchecked")
    public void writeURL(URL theURL) throws SQLException {
        if (theURL != null) {
            SerialDatalink serialDatalink = new SerialDatalink(theURL);
            attributes.addElement(serialDatalink);
        } else {
            attributes.addElement(theURL);
        }
    }
}
