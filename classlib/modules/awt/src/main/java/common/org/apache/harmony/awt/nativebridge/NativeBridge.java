/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Sergey V. Kuksenko
 */
package org.apache.harmony.awt.nativebridge;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.harmony.misc.accessors.AccessorFactory;
import org.apache.harmony.misc.accessors.MemoryAccessor;

public class NativeBridge {

    private NativeBridge() {
    }

    private static final MemoryAccessor memAccess = AccessController.doPrivileged(new PrivilegedAction<MemoryAccessor>() {
                                                      public MemoryAccessor run() {
                                                          return AccessorFactory.getMemoryAccessor();
                                                      }
                                                  });

    private static final NativeBridge instance = new NativeBridge();
    public static final int ptrSize = memAccess.getPointerSize();
    public static final boolean is64 = (ptrSize == 8);


    /**
     * Factory method returns the only instance of the NativeBridge
     */
    public static NativeBridge getInstance() {
        return instance;
    }

    /**
     * Int8Pointer factory methods
     */

    /**
     * Creates pointer to 1-byte data type with given size.
     * @param size amount of memory in 1-bytes units that should be allocated.
     * @param direct  <code>true</code> for native
     * memory usage, <code>false</code> - for java array usage
     */
    public Int8Pointer createInt8Pointer(int size, boolean direct) {
        return  new Int8Pointer(size,direct);
    }

    /**
     * Creates modified UTF8 encoded copy of String object in native heap or in java array.
     * depending on <code>direct</code> parameter.
     * @param str - original string
     * @param direct  <code>true</code> to create string in native
     * memory, <code>false</code> - using java array
     */
    public Int8Pointer createStringUTF(String str, boolean direct) {
        return createInt8Pointer(str, direct);
    }

    /**
     * Creates modified UTF8 encoded copy of String object in native heap or in java array.
     * depending on <code>direct</code> parameter.
     * @param str - original string
     * @param direct - <code>true</code> to create string in native
     * memory, <code>false</code> - using java array
     */
    public Int8Pointer createInt8Pointer(String str, boolean direct) {
        Int8Pointer p = new Int8Pointer(str.length() * 3 + 2, direct);
        p.setStringUTF(str);
        return p;
    }

    /**
     * Creates pointer to 1-byte data type.
     * @param addr - native memory address
     */
    public Int8Pointer createInt8Pointer(long addr) {
        if(addr == 0) {
            return null;
        }
        return new Int8Pointer(addr);
    }

    /**
     * Creates pointer to 1-byte data type using some void pointer object.
     * @param elementPointer  pointer object that should be wrapped
     */
    public Int8Pointer createInt8Pointer(VoidPointer elementPointer) {
        return new Int8Pointer(elementPointer);
    }

    /**
     * Int16Pointer factory methods
     */
    /**
     * Creates pointer to 2-byte data type with given size.
     * @param size amount of memory in 2-bytes units that should be allocated.
     * @param direct  <code>true</code> for native
     * memory usage, <code>false</code> - for java array usage
     */
    public Int16Pointer createInt16Pointer(int size, boolean direct) {
        return new Int16Pointer(size, direct);
    }

    /**
     * Creates pointer to 2-byte data type using some void pointer object.
     * @param elementPointer  pointer object that should be wrapped
     */
    public Int16Pointer createInt16Pointer(VoidPointer elementPointer) {
        return  new Int16Pointer(elementPointer);
    }

    /**
     * Creates pointer to 2-byte data type.
     * @param addr - native memory address
     */
    public Int16Pointer createInt16Pointer(long addr) {
        if (addr == 0) {
            return null;
        }
        return new Int16Pointer(addr);
    }

    /**
     * Creates UTF16 (Unicode) copy of String object.
     * @param str - original string
     * @param direct - <code>true</code> to create string in native
     * memory, <code>false</code> - using java array
     */
    public Int16Pointer createInt16Pointer(String str, boolean direct) {
        Int16Pointer s = this.createInt16Pointer(str.length() + 2, direct);
        s.setString(str);
        return s;
    }

    /**
     * Creates UTF16 (Unicode) copy of String object.
     * @param str - original string
     * @param direct - <code>true</code> to create string in native
     * memory, <code>false</code> - using java array
     */
    public Int16Pointer createString(String str, boolean direct) {
        return createInt16Pointer(str, direct);
    }

    /**
     * Int32Pointer factory methods
     */
    /**
     * Creates pointer to 4-byte data type with given size.
     * @param size amount of memory in 4-bytes units that should be allocated.
     * @param direct  <code>true</code> for native
     * memory usage, <code>false</code> - for java array usage
     */
    public Int32Pointer createInt32Pointer(int size, boolean direct) {
        return new Int32Pointer(size, direct);
    }

    /**
     * Creates pointer to 4-byte data type using some void pointer object.
     * @param elementPointer  pointer object that should be wrapped
     */
    public Int32Pointer createInt32Pointer(VoidPointer elementPointer) {
        return new Int32Pointer(elementPointer);
    }

    /**
     * Creates pointer to 4-byte data type.
     * @param addr - native memory address
     */
    public Int32Pointer createInt32Pointer(long addr) {
        if (addr == 0) {
            return null;
        }
        return new Int32Pointer(addr);
    }

    /**
     * Int64Pointer factory methods
     */
    /**
     * Creates pointer to 8-byte data type with given size.
     * @param size amount of memory in 8-bytes units that should be allocated.
     * @param direct  <code>true</code> for native
     * memory usage, <code>false</code> - for java array usage
     */
    public Int64Pointer createInt64Pointer(int size, boolean direct) {
        return new Int64Pointer(size, direct);
    }

    /**
     * Creates pointer to 8-byte data type using some void pointer object.
     * @param elementPointer  pointer object that should be wrapped
     */
    public Int64Pointer createInt64Pointer(VoidPointer elementPointer) {
        return new Int64Pointer(elementPointer);
    }

    /**
     * Creates pointer to 8-byte data type.
     * @param addr - native memory address
     */
    public Int64Pointer createInt64Pointer(long addr) {
        if (addr == 0) {
            return null;
        }
        return new Int64Pointer(addr);
    }

    /**
     * FloatPointer factory methods
     */
    /**
     * Creates pointer to float data type with given size.
     * @param size amount of memory in 4-bytes units that should be allocated.
     * @param direct  <code>true</code> for native
     * memory usage, <code>false</code> - for java array usage
     */
    public FloatPointer createFloatPointer(int size, boolean direct) {
        return new FloatPointer(size, direct);
    }

    /**
     * Creates pointer to float data type using some void pointer object.
     * @param elementPointer  pointer object that should be wrapped
     */
    public FloatPointer createFloatPointer(VoidPointer elementPointer) {
        return new FloatPointer(elementPointer);
    }

    /**
     * Creates pointer to float data type.
     * @param addr - native memory address
     */
    public FloatPointer createFloatPointer(long addr) {
        if (addr == 0) {
            return null;
        }
        return new FloatPointer(addr);
    }

    /**
     * FloatPointer factory methods
     */
    public DoublePointer createDoublePointer(int size, boolean direct) {
        return new DoublePointer(size, direct);
    }

    /**
     * Creates pointer to double data type using some void pointer object.
     * @param elementPointer  pointer object that should be wrapped
     */
    public DoublePointer createDoublePointer(VoidPointer elementPointer) {
        return new DoublePointer(elementPointer);
    }

    /**
     * Creates pointer to double data type.
     * @param addr - native memory address
     */
    public DoublePointer createDoublePointer(long addr) {
        if (addr == 0) {
            return null;
        }
        return new DoublePointer(addr);
    }

    /**
     * PointerPointer factory methods
     */
    public PointerPointer createPointerPointer(int size, boolean direct) {
        return new PointerPointer(size, direct);
    }

    public PointerPointer createPointerPointer(long ptrPtr) {
        return new PointerPointer(ptrPtr);
    }

    /**
     * Creates pointer to <type>pointer using void pointer object.
     * @param p  pointer object that should be wrapped
     * @param direct <code>true</code> for native memory usage
     */
    public PointerPointer createPointerPointer(VoidPointer p, boolean direct) {
        return new PointerPointer(p, direct);
    }

    /**
     * CLongPointer factory methods
     */
    /**
     * Creates pointer C-lang long data type using some void pointer object.
     * @param elementPointer  pointer object that should be wrapped
     */
    public CLongPointer createCLongPointer(VoidPointer elementPointer) {
        return new CLongPointer(elementPointer);
    }

    /**
     * Creates pointer to C-lang long data type with given size.
     * @param size amount of memory in 1-bytes units that should be allocated.
     * @param direct  <code>true</code> for native
     * memory usage, <code>false</code> - for java array usage
     */
    public CLongPointer createCLongPointer(int size, boolean direct) {
        return new CLongPointer(size, direct);
    }

    /**
     * Creates pointer to C-lang long data type.
     * @param addr - native memory address
     */
    public CLongPointer createCLongPointer(long addr) {
        if (addr == 0) {
            return null;
        }
        return new CLongPointer(addr);
    }
}
