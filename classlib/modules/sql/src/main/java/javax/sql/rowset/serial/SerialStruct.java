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

import java.io.Serializable;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLOutput;
import java.sql.Struct;
import java.util.Map;
import java.util.Vector;

/**
 * A struct class for serialization.
 */
public class SerialStruct implements Struct, Serializable, Cloneable {
    // required by serialized form
    @SuppressWarnings("unused")
    private static final long serialVersionUID = -8322445504027483372L;

    // required by serialized form
    private String SQLTypeName;

    // required by serialized form
    private Object[] attribs;

    /**
     * Constructs this serializable struct from an instance of SQLData. Use the
     * mapping defined in the map for UDT.
     * 
     * @param in
     *            an instance of SQLData.
     * @param map
     *            an user defined mapping for UDT.
     * @throws SerialException
     *             if there is something wrong.
     */
    public SerialStruct(SQLData in, Map<String, Class<?>> map)
            throws SerialException {
        try {
            SQLTypeName = in.getSQLTypeName();
        } catch (SQLException e) {
            throw new SerialException(e.getMessage());
        }
        Vector v = new Vector();
        try {
            SQLOutput out = new SQLOutputImpl(v, map);
            in.writeSQL(out);
        } catch (SQLException e) {
            throw new SerialException(e.getMessage());
        }
        attribs = v.toArray();
    }

    /**
     * Constructs this serializable struct from an instance of Struct. Use the
     * mapping defined in the map for UDT.
     * 
     * @param in
     *            an instance of SQLData.
     * @param map
     *            an user defined mapping for UDT.
     * @throws SerialException
     *             if there is something wrong.
     */
    public SerialStruct(Struct in, Map<String, Class<?>> map)
            throws SerialException {
        try {
            SQLTypeName = in.getSQLTypeName();
            attribs = in.getAttributes(map);
        } catch (SQLException e) {
            throw new SerialException(e.getMessage());
        }
    }

    /**
     * Returns all the attributes as an array of Object.
     */
    public Object[] getAttributes() throws SerialException {
        return attribs;
    }

    /**
     * Returns all the attributes as an array of Object, with the mapping
     * defined by user.
     */
    public Object[] getAttributes(Map<String, Class<?>> map)
            throws SerialException {
        // TODO handle the map.
        return attribs;
    }

    public String getSQLTypeName() throws SerialException {
        return SQLTypeName;
    }

}
