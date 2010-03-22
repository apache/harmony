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

import java.io.Serializable;

import javax.sql.RowSetReader;
import javax.sql.RowSetWriter;
import javax.sql.rowset.spi.SyncProvider;
import javax.sql.rowset.spi.SyncProviderException;

public class HYOptimisticProvider extends SyncProvider implements Serializable {

    private static final long serialVersionUID = -4275201032064821711L;

    private final static String providerID = "Apache Harmony"; //$NON-NLS-1$

    private final static int providerGrade = SyncProvider.GRADE_CHECK_MODIFIED_AT_COMMIT;

    private final static String vendor = "Apache Harmony"; //$NON-NLS-1$

    private final static String version = ""; //$NON-NLS-1$

    @Override
    public int getDataSourceLock() throws SyncProviderException {
        return DATASOURCE_NO_LOCK;
    }

    @Override
    public int getProviderGrade() {
        return providerGrade;
    }

    @Override
    public String getProviderID() {
        return providerID;
    }

    @Override
    public RowSetReader getRowSetReader() {
        return new CachedRowSetReader();
    }

    @Override
    public RowSetWriter getRowSetWriter() {
        return new CachedRowSetWriter();
    }

    @Override
    public String getVendor() {
        return vendor;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setDataSourceLock(int dataSourceLock)
            throws SyncProviderException {
        if (dataSourceLock != DATASOURCE_NO_LOCK) {
            // rowset.23=Locking classification is not supported
            throw new SyncProviderException("rowset.23"); //$NON-NLS-1$
        }
    }

    @Override
    public int supportsUpdatableView() {
        return NONUPDATABLE_VIEW_SYNC;
    }

}
