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
 * @author  Vasily Zakharov
 */
package org.apache.harmony.rmi.compiler;

import java.io.File;

import org.apache.harmony.rmi.common.RMIUtil;
import org.apache.harmony.rmi.internal.nls.Messages;


/**
 * Utility functions for RMI compiler.
 *
 * This class cannot be instantiated.
 *
 * @author  Vasily Zakharov
 */
final class RmicUtil implements RmicConstants {

    /**
     * This class cannot be instantiated.
     */
    private RmicUtil() {}

    /**
     * Returns suitable name for a parameter based on its class name and number.
     *
     * @param   cls
     *          Parameter class.
     *
     * @param   number
     *          Parameter number.
     *
     * @return  Suitable name for a parameter.
     */
    static String getParameterName(Class cls, int number) {
        StringBuilder buffer = new StringBuilder(paramPrefix);

        while (cls.isArray()) {
            buffer.append(arrayPrefix);
            cls = cls.getComponentType();
        }

        buffer.append(RMIUtil.getShortName(cls) + '_' + number);

        return buffer.toString();
    }

    /**
     * Creates source code fragment for method parameter,
     * wrapping primitive types into respective Object types.
     *
     * @param   cls
     *          Parameter class.
     *
     * @param   varName
     *          Parameter variable name.
     *
     * @return  Source code fragment for method parameter,
     *          for Object types it's just <code>varName</code>,
     *          for primitive types (e. g. <code>int</code>)
     *          it's a string like this:
     *          <code>"new java.lang.Integer(varName)"</code>.
     */
    static String getObjectParameterString(Class cls, String varName) {
        return (cls.isPrimitive()
                ? ("new " + RMIUtil.getWrappingClass(cls).getName() //$NON-NLS-1$
                + '(' + varName + ')') : varName);
    }

    /**
     * Returns string with capitalized first letter.
     *
     * @param   str
     *          String.
     *
     * @return  String with capitalized first letter,
     *          or string itself if the string is empty.
     */
    static String firstLetterToUpperCase(String str) {
        int length = str.length();

        if (length < 1) {
            return str;
        }

        char[] array = new char[length];
        str.getChars(0, length, array, 0);
        array[0] = Character.toUpperCase(array[0]);
        return String.copyValueOf(array);
    }

    /**
     * Returns name of the class with capitalized first letter
     * for primitive classes and <code>Object</code> for non-primitive classes.
     *
     * @param   cls
     *          Class.
     *
     * @return  Returns name of the class with capitalized first letter
     *          for primitive classes and <code>Object</code> for non-primitive
     *          classes.
     */
    static String getHandlingType(Class cls) {
        return (cls.isPrimitive()
                ? firstLetterToUpperCase(cls.getName()) : "Object"); //$NON-NLS-1$
    }

    /**
     * Creates source code fragment for reading object from a stream,
     * correctly handling primitive types.
     *
     * @param   cls
     *          Class of object being read.
     *
     * @param   streamName
     *          Name of stream to read variable from.
     *
     * @return  Source code fragment for reading object,
     *          for Object class it's like
     *          <code>"streamName.readObject()"</code>,
     *          for other Object types (e. g. <code>Vector</code>)
     *          it's a string like this:
     *          <code>"(java.util.Vector) streamName.readObject()"</code>,
     *          for primitive types (e. g. <code>int</code>)
     *          it's a string like this:
     *          <code>"streamName.readInt()"</code>.
     */
    static String getReadObjectString(Class cls, String streamName) {
        // For primitive types, use respective special read method.
        // For non-primitive types, use readObject() and cast (if not Object).
        return (((!cls.isPrimitive() && (cls != Object.class))
                ? ('(' + RMIUtil.getCanonicalName(cls) + ") ") : "") //$NON-NLS-1$ //$NON-NLS-2$
                + streamName + ".read" + getHandlingType(cls) + "()"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Creates source code fragment for writing object to a stream,
     * correctly handling primitive types.
     *
     * @param   cls
     *          Class of object to write.
     *
     * @param   varName
     *          Name of the variable to write.
     *
     * @param   streamName
     *          Name of stream to write variable to.
     *
     * @return  Source code fragment for writing object,
     *          for object types it's like
     *          <code>"streamName.writeObject(varName)"</code>,
     *          for primitive types (e. g. <code>int</code>)
     *          it's a string like this:
     *          <code>"streamName.writeInt(varName)"</code>.
     */
    static String getWriteObjectString(Class cls, String varName,
            String streamName) {
        // For primitive types, use respective special write method.
        // For non-primitive types, use writeObject().
        return (streamName + ".write" + getHandlingType(cls) //$NON-NLS-1$
                + '(' + varName +')');
    }

    /**
     * Creates source code fragment for return object,
     * correctly de-wrapping primitive types.
     *
     * @param   cls
     *          Return class.
     *
     * @param   varName
     *          Return variable name.
     *
     * @return  Source code fragment for return object,
     *          for {@link Object} class it's just <code>varName</code>,
     *          for other Object types (e. g. <code>Vector</code>)
     *          it's a string like this:
     *          <code>"((java.util.Vector) varName)"</code>,
     *          for primitive types (e. g. <code>int</code>)
     *          it's a string like this:
     *          <code>"((java.lang.Integer) varName).intValue()"</code>.
     */
    static String getReturnObjectString(Class cls, String varName) {
        // For Object return type, do nothing.
        if (cls == Object.class) {
            return varName;
        }

        // For all other types, create the respective cast statement.
        StringBuilder buffer = new StringBuilder("(("); //$NON-NLS-1$
        buffer.append(RMIUtil.getCanonicalName(RMIUtil.getWrappingClass(cls)));
        buffer.append(") " + varName + ')'); //$NON-NLS-1$

        // For primitive types, include case to primitive type.
        if (cls.isPrimitive()) {
            buffer.append('.' + cls.getName() + "Value()"); //$NON-NLS-1$
        }

        return buffer.toString();
    }

    /**
     * Creates a file object for a directory created basing on the specified
     * base directory and the package name for subdirectory.
     *
     * @param   base
     *          Base directory.
     *
     * @param   packageName
     *          Package name (for subdirectory).
     *
     * @return  File object for a directory like this:
     *          <code>baseDir/my/java/package</code>.
     *
     * @throws  RMICompilerException
     *          If directory cannot be created.
     */
    static File getPackageDir(String base, String packageName)
            throws RMICompilerException {
        File dir = new File(base, (packageName != null) ? packageName : ""); //$NON-NLS-1$

        if (dir.exists() ? !dir.isDirectory() : !dir.mkdirs()) {
            // rmi.4E=Can't create target directory: {0}
            throw new RMICompilerException(Messages.getString("rmi.4E", dir)); //$NON-NLS-1$
        }

        return dir;
    }

    /**
     * Creates a file object for a file created basing on the specified
     * directory and the name of file itself.
     *
     * @param   dir
     *          Directory to create the file in.
     *
     * @param   fileName
     *          Name of file itself.
     *
     * @return  File object for a file name like this:
     *          <code>dir/fileName</code>.
     *
     * @throws  RMICompilerException
     *          If file cannot be created.
     */
    static File getPackageFile(File dir, String fileName)
            throws RMICompilerException {
        File file = new File(dir, fileName);

        if (file.exists()
                ? !(file.isFile() && file.canWrite()) : !dir.canWrite()) {
            // rmi.4F=Can't create file: {0}
            throw new RMICompilerException(Messages.getString("rmi.4F", file)); //$NON-NLS-1$
        }

        return file;
    }
}
