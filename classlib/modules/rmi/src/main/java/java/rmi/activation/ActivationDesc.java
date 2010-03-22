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
import java.rmi.MarshalledObject;
import org.apache.harmony.rmi.internal.nls.Messages;

public final class ActivationDesc implements Serializable {
    private static final long serialVersionUID = 7455834104417690957L;

    /**
     * @serial The ActivationGroupID of the object.
     */
    private ActivationGroupID groupID;

    /**
     * @serial The className of the object.
     */
    private String className;

    /**
     * @serial The location(<i>codebase/URLs</i>) from which the class of the object can be loaded.
     */
    private String location;

    /**
     * @serial MarshalledObject that contain object-specific initialization data used during each activation.
     */
    private MarshalledObject<?> data;

    /**
     * @serial If the object requires restart service, restart should be true. If restart is false, the object is simply activated upon demand.
     */
    private boolean restart;

    public ActivationDesc(String className, String location, MarshalledObject<?> data)
            throws ActivationException {
        ActivationGroupID currentGID = ActivationGroup.currentGroupID();
        if (currentGID == null) {
            // rmi.0D=The default group for this JVM is inactive.
            throw new ActivationException(Messages.getString("rmi.0D")); //$NON-NLS-1$
        }
        this.groupID = currentGID;
        this.className = className;
        this.location = location;
        this.data = data;
        this.restart = false;
    }

    public ActivationDesc(String className, String location, MarshalledObject<?> data,
            boolean restart) throws ActivationException {
        ActivationGroupID currentGID = ActivationGroup.currentGroupID();
        if (currentGID == null) {
            // rmi.0D=The default group for this JVM is inactive.
            throw new ActivationException(Messages.getString("rmi.0D")); //$NON-NLS-1$
        }
        this.groupID = currentGID;
        this.className = className;
        this.location = location;
        this.data = data;
        this.restart = restart;
    }

    public ActivationDesc(ActivationGroupID groupID, String className, String location,
            MarshalledObject<?> data) {
        if (groupID == null) {
            // rmi.10=The groupID can't be null.
            throw new IllegalArgumentException(Messages.getString("rmi.10")); //$NON-NLS-1$
        }
        this.groupID = groupID;
        this.className = className;
        this.location = location;
        this.data = data;
        this.restart = false;
    }

    public ActivationDesc(ActivationGroupID groupID, String className, String location,
            MarshalledObject<?> data, boolean restart) {
        if (groupID == null) {
            // rmi.10=The groupID can't be null.
            throw new IllegalArgumentException(Messages.getString("rmi.10")); //$NON-NLS-1$
        }
        this.groupID = groupID;
        this.className = className;
        this.location = location;
        this.data = data;
        this.restart = restart;
    }

    public ActivationGroupID getGroupID() {
        return groupID;
    }

    public MarshalledObject<?> getData() {
        return data;
    }

    public String getLocation() {
        return location;
    }

    public String getClassName() {
        return className;
    }

    public boolean getRestartMode() {
        return restart;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ActivationDesc) {
            ActivationDesc objCasted = (ActivationDesc) obj;
            boolean p0, p1, p2, p3, p4;
            p0 = (groupID == null) ? objCasted.groupID == null : groupID
                    .equals(objCasted.groupID);
            p1 = (className == null) ? objCasted.className == null : className
                    .equals(objCasted.className);
            p2 = (location == null) ? objCasted.location == null : location
                    .equals(objCasted.location);
            p3 = (data == null) ? objCasted.data == null : data.equals(objCasted.data);
            p4 = (restart == objCasted.restart);
            return p0 && p1 && p2 && p3 && p4;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int groupID_Hash = (groupID == null) ? 0 : groupID.hashCode();
        int className_Hash = (className == null) ? 0 : className.hashCode();
        int location_Hash = (location == null) ? 0 : location.hashCode();
        int data_Hash = (data == null) ? 0 : data.hashCode();
        int restart_Hash = (restart == false) ? 0 : 1;
        int hashCode = groupID_Hash ^ className_Hash ^ location_Hash ^ data_Hash ^ restart_Hash;
        return hashCode;
    }
}
