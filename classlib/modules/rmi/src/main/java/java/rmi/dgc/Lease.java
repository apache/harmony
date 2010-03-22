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
package java.rmi.dgc;

import java.io.Serializable;


/**
 * @com.intel.drl.spec_ref
 *
 * @author  Mikhail A. Markov
 */
public final class Lease implements Serializable {
    private static final long serialVersionUID = -5713411624328831948L;
    private VMID vmid;
    private long value;

    /**
     * @com.intel.drl.spec_ref
     */
    public Lease(VMID vmid, long l) {
        this.vmid = vmid;
        this.value = l;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public VMID getVMID() {
        return vmid;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public long getValue() {
        return value;
    }
}
