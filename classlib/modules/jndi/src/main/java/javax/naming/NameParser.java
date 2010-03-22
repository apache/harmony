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

package javax.naming;

/**
 * A <code>NameParser</code> is used to validate and decompose a name from a
 * particular namespace. It is implemented by classes provided in SPI
 * implementations.
 */
public interface NameParser {

    /**
     * Takes a name in a <code>String s</code> and validates it according to
     * the rules for the namespace. (See <code>CompoundName</code> for the
     * guidelines on name format and system parameters which affect the
     * translation of a name.) The name is then decomposed into its elements and
     * returned as a <code>Name</code>.
     * 
     * @param s
     *            the name to be examined - cannot be null
     * @return a <code>Name</code> instance, cannot be null.
     * @throws InvalidNameException
     *             when the supplied string violates format rules
     * @throws NamingException
     */
    public Name parse(String s) throws NamingException;

}
