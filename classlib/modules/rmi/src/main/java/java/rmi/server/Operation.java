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


/**
 * @com.intel.drl.spec_ref
 *
 * @author  Mikhail A. Markov
 * @deprecated This class was used by the previous versions of the RMI
 *  framework. It is not required since Java v1.2.
 */
@Deprecated
public class Operation {

    private String op;

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public Operation(String op) {
        this.op = op;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public String toString() {
        return op;
    }

    /**
     * @com.intel.drl.spec_ref
     */
    @Deprecated
    public String getOperation() {
        return op;
    }
}
