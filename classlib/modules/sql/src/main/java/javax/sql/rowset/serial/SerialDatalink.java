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
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.harmony.sql.internal.nls.Messages;

public class SerialDatalink implements Serializable, Cloneable {
    private static final long serialVersionUID = 2826907821828733626L;

    private URL url;

    @SuppressWarnings("unused")
    // Required by serialized form
    private int baseType;

    @SuppressWarnings("unused")
    // Required by serialized form
    private String baseTypeName;

    /**
     * Constructor.
     * 
     * @param url
     *            The URL to link to.
     * @throws SerialException
     *             if <code>url</code> is null
     */
    public SerialDatalink(URL url) throws SerialException {
        if (url == null) {
            throw new SerialException(Messages.getString("sql.12")); //$NON-NLS-1$
        }
        this.url = url;
    }

    /**
     * Gets a copied url object of this SerialDatalink object.
     * 
     * @return a url object in the java programming language which represents
     *         this SerialDatalink object.
     * @throws SerialException
     *             if <code>url</code> can not be copied.
     */
    public URL getDatalink() throws SerialException {
        URL copyUrl;
        try {
            copyUrl = new URL(url.toString());
        } catch (MalformedURLException e) {
            throw new SerialException();
        }
        return copyUrl;
    }
}
