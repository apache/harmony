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
 * @author  Victor A. Martynov
 */
package org.apache.harmony.rmi.activation;


/**
 * Holds the ID of the node object and its state.
 *
 * @author  Victor A. Martynov
 */
public class NodeInfo {

    private static final int REGISTERED_STATE = 0;
    private static final int ACTIVE_STATE = 1;
    private static final int INACTIVE_STATE = 2;

    private static final String []STATES = {
        "(registered)", //$NON-NLS-1$
        "(active)", //$NON-NLS-1$
        "(inactive)" //$NON-NLS-1$
    };

    /**
     * Either ActivationGroupID or ActivationID.
     */
    private Object id;

    /**
     * One of 3 possible states.
     */
    private int state;

    /**
     * Creates NodeInfo object with given ID and <i>"registered"</i> state.
     * @param id <code>ActivationGroupID</code> or <code>ActivationID</code>.
     */
    public NodeInfo(Object id) {
        this.id = id;
        this.state = REGISTERED_STATE;
    }

    /**
     * @return ID of this node.
     */
    public Object getID() {
        return id;
    }
    /**
     * Sets the state of this node to <i>"active"</i>.
     */
    public void active() {
        this.state = ACTIVE_STATE;
    }

    /**
     * Sets the state of this node to <i>"inactive"</i>.
     */
    public void inactive() {
        this.state = INACTIVE_STATE;
    }

    /**
     * @return String representation of this node consisting of its ID and state.
     */
    public String toString() {
        return id + ": " + STATES[state]; //$NON-NLS-1$
    }
}
