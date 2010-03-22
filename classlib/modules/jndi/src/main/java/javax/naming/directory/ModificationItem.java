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

import java.io.Serializable;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * This class is a combination of a modification code and attribute.
 * <p>
 * It is used by exception reporting (see
 * <code>AttributeModificationException</code> for an example).
 * </p>
 * <p>
 * The class is not thread-safe.
 * </p>
 */
public class ModificationItem implements Serializable {

    /*
     * This constant is used during deserialization to check the version which
     * created the serialized object
     */
    private static final long serialVersionUID = 0x69199e89ac11aae2L;

    /**
     * Contains the modification to be performed.
     * 
     * @serial
     * @see DirContext
     */
    private int mod_op;

    /**
     * The Attribute or value that is the source of the modification.
     * 
     * @serial
     */
    private Attribute attr;

    /**
     * Constructs a <code>ModificationItem</code> instance with all
     * parameters.
     * 
     * @param operation
     *            an operation code chosen from
     *            <code>DirContext.ADD_ATTRIBUTE</code>,
     *            <code>DirContext.REPLACE_ATTRIBUTE</code>,
     *            <code>DirContext.REMOVE_ATTRIBUTE</code>
     * @param attribute
     *            the <code>Attribute</code> or value that is the source of
     *            the modification
     */
    public ModificationItem(int operation, Attribute attribute) {
        if (null == attribute) {
            // jndi.13=Non-null attribute is required for modification
            throw new IllegalArgumentException(Messages.getString("jndi.13")); //$NON-NLS-1$
        }
        if (!(DirContext.ADD_ATTRIBUTE == operation
                || DirContext.REPLACE_ATTRIBUTE == operation || DirContext.REMOVE_ATTRIBUTE == operation)) {
            // jndi.14=Modification code {0} must be one of
            // DirContext.ADD_ATTRIBUTE, DirContext.REPLACE_ATTRIBUTE and
            // DirContext.REMOVE_ATTRIBUTE
            throw new IllegalArgumentException(Messages.getString(
                    "jndi.14", operation)); //$NON-NLS-1$
        }
        this.mod_op = operation;
        this.attr = attribute;
    }

    /**
     * Gets the <code>Attribute</code> or value that is the source of the
     * modification.
     * 
     * @return the <code>Attribute</code> or value that is the source of the
     *         modification
     */
    public Attribute getAttribute() {
        return this.attr;
    }

    /**
     * Gets the operation code.
     * 
     * @return an operation code chosen from <code>
     *                      DirContext.ADD_ATTRIBUTE</code>,
     *         <code>
     *                      DirContext.REPLACE_ATTRIBUTE</code>, <code>
     *                      DirContext.REMOVE_ATTRIBUTE</code>
     */
    public int getModificationOp() {
        return this.mod_op;
    }

    /**
     * Returns string representations of this <code>ModificationItem</code>
     * instance.
     * 
     * @return a concatenation of string values for the operation and the
     *         attribute
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        switch (mod_op) {
            case DirContext.ADD_ATTRIBUTE:
                sb.append("Operation is add attribute: "); //$NON-NLS-1$
                break;
            case DirContext.REMOVE_ATTRIBUTE:
                sb.append("Operation is remove attribute: "); //$NON-NLS-1$
                break;
            case DirContext.REPLACE_ATTRIBUTE:
                sb.append("Operation is replace attribute: "); //$NON-NLS-1$
                break;
        }
        return sb.append(attr.toString()).toString();
    }
}
