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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author  Mikhail A. Markov
 */
package java.rmi.server;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StreamCorruptedException;


/**
 * @com.intel.drl.spec_ref
 *
 * @author  Mikhail A. Markov
 * @deprecated This interface is no longer required by RMI framework since v1.2.
 *  It is only used by the deprecated methods of
 *  {@link java.rmi.server.RemoteRef} class.
 */
@Deprecated
public interface RemoteCall {

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public void done() throws IOException;

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public void executeCall() throws Exception;

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public ObjectOutput getResultStream(boolean success)
            throws IOException, StreamCorruptedException;

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public void releaseInputStream() throws IOException;

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public ObjectInput getInputStream() throws IOException;

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public void releaseOutputStream() throws IOException;

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public ObjectOutput getOutputStream() throws IOException;
}
