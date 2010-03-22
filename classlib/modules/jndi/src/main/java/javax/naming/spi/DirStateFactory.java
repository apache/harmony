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

package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.Name;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

/**
 * The <code>DirStateFactory</code> interface describes a factory used to get
 * the state of an object to be bound. <code>DirStateFactory</code> is a
 * specific version of <code>StateFactory</code> for
 * <code>DirectoryManager</code>.
 * 
 * @see StateFactory
 * @see DirectoryManager
 */
public interface DirStateFactory extends StateFactory {

    /**
     * Similar to <code>StateFactory.getStateToBind</code> with an additional
     * <code>attributes</code> parameter.
     * 
     * @param o
     *            an object
     * @param n
     *            a name
     * @param c
     *            a context
     * @param envmt
     *            a context environment
     * @param a
     *            some attributes
     * @return the state as a <code>Result</code> instance, containing an
     *         object and associated attributes.
     * @throws NamingException
     *             if an exception occurs
     * @see StateFactory#getStateToBind(Object, Name, Context, Hashtable)
     */
    Result getStateToBind(Object o, Name n, Context c, Hashtable<?, ?> envmt,
            Attributes a) throws NamingException;

    /**
     * Used by the <code>DirectoryManager.getStateToBind</code> method as the
     * returning value.
     */
    public static class Result {

        // the Object returned by DirectoryManager.getStateToBind.
        private Object obj;

        // the Attributes returned by DirectoryManager.getStateToBind.
        private Attributes attrs;

        /**
         * Creates an instance of <code>DirStateFactory.Result</code>
         * 
         * @param o
         *            the object returned by
         *            <code>DirectoryManager.getStateToBind</code>. May be
         *            null.
         * @param a
         *            the attributes returned by
         *            <code>DirectoryManager.getStateToBind</code>. May be
         *            null.
         */
        public Result(Object o, Attributes a) {
            this.obj = o;
            this.attrs = a;
        }

        /**
         * Returns the object associated with this result.
         * 
         * @return the object associated with this result.
         */
        public Object getObject() {
            return this.obj;
        }

        /**
         * Returns the attributes associated with this result.
         * 
         * @return the attributes associated with this result.
         */
        public Attributes getAttributes() {
            return this.attrs;
        }
    }

}
