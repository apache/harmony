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

package java.rmi.activation;

import java.io.Serializable;
import java.rmi.server.UID;

public class ActivationGroupID implements Serializable {
    private static final long serialVersionUID = -1648432278909740833L;

    private final UID uid;

    private final ActivationSystem system;

    public ActivationGroupID(ActivationSystem system) {
        super();
        this.system = system;
        uid = new UID();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ActivationGroupID) {
            ActivationGroupID id = (ActivationGroupID) obj;
            return (uid.equals(id.uid) && system.equals(id.system));
        }
        return false;
    }

    public ActivationSystem getSystem() {
        return system;
    }

    @Override
    public int hashCode() {
        return uid.hashCode();
    }

    @Override
    public String toString() {
        return "ActivationGroupID[" + uid + "; " + system + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
