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

package javax.naming.directory;

import javax.naming.NamingException;

/**
 * Thrown when a caller attempts to make an attribute modification that is not
 * permitted.
 * <p>
 * Modifications such as addition, removal, and change of value to an entry's
 * attributes are made via calls to the API on a <code>DirContext</code>.
 * Where the modification is invalid by reference to the attributes' schema an
 * <code>AttributeModificationException</code> is thrown describing the
 * attributes that were unmodified.
 * </p>
 * <p>
 * The list of attributes that were not modified are returned in the same order
 * as the original modification request. If the list is returned as
 * <code>null</code> then all modification requests failed.
 * </p>
 * <p>
 * The class is not thread-safe.
 * </p>
 */
public class AttributeModificationException extends NamingException {

    private static final long serialVersionUID = 0x6fdd462d96b0fdaaL;

    /* Array of ModificationItems that were not applied. */
    private ModificationItem unexecs[] = null;

    /**
     * Default constructor.
     * <p>
     * All fields are initialized to null.
     * </p>
     */
    public AttributeModificationException() {
        super();
    }

    /**
     * Constructs an <code>AttributeModificationException</code> instance
     * using the supplied text of the message.
     * <p>
     * All fields are initialized to null.
     * </p>
     * 
     * @param s
     *            message about the problem
     */
    public AttributeModificationException(String s) {
        super(s);
    }

    /**
     * Gets <code>ModificationItems</code> that were not executed.
     * 
     * @return an array of <code>ModificationItems</code> that were not
     *         executed, in the same order they were requested in. Null is a
     *         special return value meaning none of the requested modifications
     *         were done.
     */
    public ModificationItem[] getUnexecutedModifications() {
        return unexecs;
    }

    /**
     * Sets <code>ModificationItems</code> that were not executed.
     * 
     * @param amodificationitem
     *            an array of <code>ModificationItems</code> that were not
     *            executed, in the same order they were requested in. Null is a
     *            special return value meaning none of the requested
     *            modifications were done.
     */
    public void setUnexecutedModifications(ModificationItem[] amodificationitem) {
        this.unexecs = amodificationitem;
    }

    /**
     * Returns string representation of this exception.
     * 
     * @return text detailing the exception location and the failing
     *         modification.
     */
    @Override
    public String toString() {
        return toStringImpl(false);
    }

    /**
     * Returns string representation of this exception.
     * 
     * @param flag
     *            Indicates if the resolved object need to be returned.
     * @return text detailing the exception location and the failing
     *         modification.
     */
    @Override
    public String toString(boolean flag) {
        return toStringImpl(flag);
    }

    private String toStringImpl(boolean flag) {
        StringBuilder sb = new StringBuilder(super.toString(flag));
        if (null != unexecs && unexecs.length > 0) {
            sb.append(". The unexecuted modification items are: \""); //$NON-NLS-1$
            for (ModificationItem element : unexecs) {
                sb.append(element.toString()).append(";"); //$NON-NLS-1$
            }
            sb.append("\""); //$NON-NLS-1$
        }
        return sb.toString();
    }

}
