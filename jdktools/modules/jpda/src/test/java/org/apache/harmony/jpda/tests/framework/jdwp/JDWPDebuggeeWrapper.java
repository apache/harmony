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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Aleksey V. Yantsen
 */

/**
 * Created on 10.25.2004
 */
package org.apache.harmony.jpda.tests.framework.jdwp;

import org.apache.harmony.jpda.tests.framework.DebuggeeWrapper;
import org.apache.harmony.jpda.tests.framework.LogWriter;
import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.framework.TestOptions;
import org.apache.harmony.jpda.tests.framework.jdwp.TransportWrapper;
import org.apache.harmony.jpda.tests.framework.jdwp.VmMirror;

/**
 * This class represents specific kind of <code>DebuggeeWrapper</code> for JDWP tests.
 * It encapsulates JDWP connection and communicates with debuggee using 
 * command, reply, and event packets.
 */
public abstract class JDWPDebuggeeWrapper extends DebuggeeWrapper {

    public VmMirror vmMirror;

    /**
     * Creates an instance of JDWPDebuggeeWrapper.
     * 
     * @param settings test run options
     * @param logWriter logWriter for messages
     */
    public JDWPDebuggeeWrapper(TestOptions settings, LogWriter logWriter) {
        super(settings, logWriter);
        vmMirror = createVmMirror(settings, logWriter);
    }

    /**
     * Creates new instance of appropriate TransportWrapper.
     * 
     * @return new instance of TransportWrapper
     */
    public TransportWrapper createTransportWrapper() {
        String name = settings.getTransportWrapperClassName();
        try {
            Class cls = Class.forName(name);
            return (TransportWrapper) cls.newInstance();
        } catch (Exception e) {
            throw new TestErrorException(e);
        }
    }

    /**
     * Creates new instance of VmMirror.
     * 
     * @return new instance of VmMirror
     */
    protected VmMirror createVmMirror(TestOptions settings, LogWriter logWriter) {
        return new VmMirror(settings, logWriter);
    }

    /**
     * Returns opened JDWP connection or null.
     * 
     * @return JDWP connection or null
     */
    public TransportWrapper getConnection() {
        return vmMirror.getConnection();
    }

    /**
     * Sets opened JDWP connection.
     * 
     * @param connection to set
     */
    public void setConnection(TransportWrapper connection) {
        vmMirror.setConnection(connection);
    }
   /**
     * Resumes debuggee VM.
     */
    public void resume() {
        vmMirror.resume();
    }

    /**
     * Disposes debuggee VM.
     */
    public void dispose() {
        vmMirror.dispose();
    }

    /**
     * Exit target Virtual Machine
     */
    public void exit(int exitStatus) {
        vmMirror.exit(exitStatus);
    }
    
   
}
