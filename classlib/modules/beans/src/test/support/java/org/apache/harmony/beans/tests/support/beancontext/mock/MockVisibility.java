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

package org.apache.harmony.beans.tests.support.beancontext.mock;

import java.beans.Visibility;

/**
 * Test Visibility
 */
public class MockVisibility implements Visibility {

    public boolean avoidingGui;

    public boolean needsGui;

    public boolean okToUseGui;

    public MockVisibility(boolean avoiding, boolean needs) {
        this.avoidingGui = avoiding;
        this.needsGui = needs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.beans.Visibility#avoidingGui()
     */
    public boolean avoidingGui() {
        return avoidingGui;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.beans.Visibility#dontUseGui()
     */
    public void dontUseGui() {
        okToUseGui = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.beans.Visibility#needsGui()
     */
    public boolean needsGui() {
        return needsGui;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.beans.Visibility#okToUseGui()
     */
    public void okToUseGui() {
        okToUseGui = true;
    }

}
