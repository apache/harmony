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

package javax.sql.rowset.spi;

import javax.sql.RowSetReader;
import javax.sql.RowSetWriter;

import org.apache.harmony.sql.internal.nls.Messages;

class ProviderImpl extends SyncProvider {

    private String className;

    private String vendor;

    private String version;

    private SyncProvider impl;

    private String errMsg;

    public ProviderImpl() {
        // default constructor
    }

    public ProviderImpl(String name) {
        className = name;

        try {
            Class<?> implClass = Class.forName(className, true, Thread
                    .currentThread().getContextClassLoader());
            impl = (SyncProvider) implClass.newInstance();
        } catch (ClassNotFoundException e) {
            errMsg = Messages.getString("sql.40", className); //$NON-NLS-1$
        } catch (Exception e) {
            // ignore
        }
    }

    public ProviderImpl(String name, String vendor, String version) {
        this(name);

        this.vendor = vendor;
        this.version = version;
    }

    public SyncProvider getImpl() throws SyncFactoryException {
        if (null == impl) {
            throw new SyncFactoryException(Messages.getString(
                    "sql.40", className)); //$NON-NLS-1$
        }
        return impl;
    }

    @Override
    public int getDataSourceLock() throws SyncProviderException {
        checkClassNameValid();
        return impl.getDataSourceLock();
    }

    @Override
    public int getProviderGrade() {
        return impl == null ? 0 : impl.getProviderGrade();
    }

    @Override
    public String getProviderID() {
        return impl == null ? className : impl.getProviderID();
    }

    @Override
    public RowSetReader getRowSetReader() {
        return impl == null ? null : impl.getRowSetReader();
    }

    @Override
    public RowSetWriter getRowSetWriter() {
        return impl == null ? null : impl.getRowSetWriter();
    }

    @Override
    public String getVendor() {
        return impl == null ? vendor : impl.getVendor();
    }

    @Override
    public String getVersion() {
        return impl == null ? version : impl.getVersion();
    }

    @Override
    public void setDataSourceLock(int dataSourceLock)
            throws SyncProviderException {
        checkClassNameValid();
        impl.setDataSourceLock(dataSourceLock);
    }

    @Override
    public int supportsUpdatableView() {
        return impl == null ? 0 : impl.supportsUpdatableView();
    }

    private void checkClassNameValid() throws SyncProviderException {
        if (null == impl) {
            throw new SyncProviderException(errMsg);
        }
    }

}
