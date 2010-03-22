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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.rowset.BaseRowSet;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.WebRowSet;
import javax.sql.rowset.spi.SyncFactoryException;

public class WebRowSetImpl extends CachedRowSetImpl implements WebRowSet {

    private static final long serialVersionUID = -1585509574069224797L;

    public WebRowSetImpl() throws SyncFactoryException {
        super();
    }

    public void readXml(Reader reader) throws SQLException {
        new XmlReaderImpl().readXML(this, reader);
    }

    public void readXml(InputStream iStream) throws SQLException, IOException {
        new XmlReaderImpl().readXML(this, new InputStreamReader(iStream));
    }

    public void writeXml(ResultSet rs, Writer writer) throws SQLException {
        super.populate(rs);
        writeXml(writer);
        beforeFirst();
    }

    public void writeXml(ResultSet rs, OutputStream oStream)
            throws SQLException, IOException {
        super.populate(rs);
        writeXml(oStream);
        beforeFirst();
    }

    public void writeXml(Writer writer) throws SQLException {
        new XmlWriterImpl().writeXML(this, writer);
    }

    public void writeXml(OutputStream oStream) throws SQLException, IOException {
        new XmlWriterImpl().writeXML(this, new OutputStreamWriter(oStream));
    }

    @Override
    public CachedRowSet createCopy() throws SQLException {
        WebRowSetImpl webRs = new WebRowSetImpl();
        CachedRowSet copyCrset = super.createCopy();
        copyCrset.beforeFirst();
        webRs.populate(copyCrset);
        webRs.setCommand(copyCrset.getCommand());
        Object[] params = ((CachedRowSetImpl) copyCrset).getParams();
        for (int i = 0; i < params.length; i++) {
            if (params[i] instanceof Object[]) {
                Object[] objs = (Object[]) params[i];
                // character stream
                if (objs.length == 2) {
                    webRs.setCharacterStream(i + 1, (Reader) objs[0],
                            ((Integer) objs[1]).intValue());
                } else {
                    int type = ((Integer) objs[2]).intValue();
                    switch (type) {
                    case BaseRowSet.ASCII_STREAM_PARAM:
                        webRs.setAsciiStream(i + 1, (InputStream) objs[0],
                                ((Integer) objs[1]).intValue());
                        break;
                    case BaseRowSet.BINARY_STREAM_PARAM:
                        webRs.setBinaryStream(i + 1, (InputStream) objs[0],
                                ((Integer) objs[1]).intValue());
                        break;
                    }
                }
            } else {
                webRs.setObject(i + 1, params[i]);
            }
        }
        if (copyCrset.getUrl() != null) {
            webRs.setUrl(copyCrset.getUrl());
            webRs.setUsername(copyCrset.getUsername());
            webRs.setPassword(copyCrset.getPassword());
        } else if (copyCrset.getDataSourceName() != null) {
            webRs.setDataSourceName(copyCrset.getDataSourceName());
        }
        return webRs;
    }
}
