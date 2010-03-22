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

import java.net.MalformedURLException;


/**
 * @com.intel.drl.spec_ref
 *
 * @author  Mikhail A. Markov
 */
public abstract class RMIClassLoaderSpi {

    /**
     * @com.intel.drl.spec_ref
     */
    public RMIClassLoaderSpi() {
    }

    /**
     * @com.intel.drl.spec_ref
     */
    public abstract Class<?> loadProxyClass(String codebase,
                                         String[] interf,
                                         ClassLoader defaultCl)
            throws MalformedURLException, ClassNotFoundException;

    /**
     * @com.intel.drl.spec_ref
     */
    public abstract Class<?> loadClass(String codebase,
                                    String name,
                                    ClassLoader defaultCl)
            throws MalformedURLException, ClassNotFoundException;

    /**
     * @com.intel.drl.spec_ref
     */
    public abstract String getClassAnnotation(Class<?> cl);

    /**
     * @com.intel.drl.spec_ref
     */
    public abstract ClassLoader getClassLoader(String name)
            throws MalformedURLException;
}
