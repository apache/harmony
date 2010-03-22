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

package org.apache.harmony.beans.tests.support.mock;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;

/**
 * test VetoableChangeSupport
 */
public class NonSerializedVCListener implements VetoableChangeListener {

    String propertyName;

    public NonSerializedVCListener(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NonSerializedVCListener)) {
            return false;
        }

        NonSerializedVCListener other = (NonSerializedVCListener) o;
        return (this.propertyName == null ? other.propertyName == null
                : this.propertyName.equals(other.propertyName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.beans.VetoableChangeListener#vetoableChange(java.beans.PropertyChangeEvent)
     */
    public void vetoableChange(PropertyChangeEvent event)
            throws PropertyVetoException {
        // TODO Auto-generated method stub

    }

}
