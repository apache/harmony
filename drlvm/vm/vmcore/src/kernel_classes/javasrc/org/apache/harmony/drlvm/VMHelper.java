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
 * @author Mikhail Y. Fursov
 */ 

package org.apache.harmony.drlvm;

import org.vmmagic.unboxed.*;
import org.vmmagic.pragma.*;

/**
    Core class for DRLVM's vmmagic based helpers.
    Resolved and initilized during VM startup
    
    Note: All classes with vmmagic based helpers registred in VM are also resolved and initialized at VM startup
    Note: If you need to initialize another DRLVM's specific utility class related to vmmagic infrastructure
          refer to it from static section of this class: and it will also be automatically initialized
*/
public class VMHelper {

 

    //public constants

    public static final int POINTER_TYPE_SIZE          = getPointerTypeSize();

    public static final boolean COMPRESSED_REFS_MODE   = isCompressedRefsMode();

    public static final boolean COMPRESSED_VTABLE_MODE = isCompressedVTableMode();

    public static final long COMPRESSED_VTABLE_BASE_OFFSET    = getCompressedModeVTableBaseOffset();

    public static final long COMPRESSED_REFS_OBJ_BASE_OFFSET  = getCompressedModeObjectBaseOffset();

    public static final int OBJ_VTABLE_OFFSET = getObjectVtableOffset();

    public static final int VTABLE_GCPRIVATE_OFFSET = 0;

    public static final int VTABLE_CLASS_OFFSET = getVtableClassOffset();

    public static final int OBJ_INFO_OFFSET = 4;

    public static final int CLASS_JLC_HANDLE_OFFSET = getClassJLCHanldeOffset();
    
    // preload @Inline vmmagic class
    static final Class pragmaInline = org.vmmagic.pragma.Inline.class;
    static final Class threadHelper = org.apache.harmony.drlvm.thread.ThreadHelper.class;
    static final Class vmFastPathes = org.apache.harmony.drlvm.VMHelperFastPath.class;

    //Slow path versions of helpers

    //TODO: allocation handle is int only on 32bit OS or (64bit OS && compressed mode)
    public static Address newResolvedUsingAllocHandleAndSize(int objSize, int allocationHandle) {fail(); return null;}
    
    //TODO: allocation handle is int only on 32bit OS or (64bit OS && compressed mode)
    public static Address newVectorUsingAllocHandle(int arrayLen, int allocationHandle) {fail(); return null;}

    public static void monitorEnter(Object obj) {fail();}

    public static void monitorExit(Object obj) {fail();}

    public static void memset0(Address addr, int size) {fail();}

    public static void prefetch(Address addr, int distance, int stride) {fail();}

    public static void writeBarrier(Address objBase, Address objSlot, Address source) {fail();}

    public static int getHashcode(Address p_obj){fail(); return 0;}

    public static Address getInterfaceVTable(Object obj, Address intfTypePtr) {fail(); return null;}
 
    public static void checkCast(Object obj, Address castTypePtr) {fail();}
 
    public static boolean instanceOf(Object obj, Address castTypePtr) {fail(); return false;}




    //utility magics supported by JIT

    public static boolean isVMMagicPackageSupported() {return false;}

    public static Address getTlsBaseAddress() {fail(); return null;}

    public static boolean isArray(Address classHandle) {VMHelper.fail(); return false;}

    public static boolean isInterface(Address classHandle) {VMHelper.fail(); return false;}

    public static boolean isFinal(Address classHandle) {VMHelper.fail(); return false;}

    public static Address getArrayClass(Address elemClassHandle)  {VMHelper.fail(); return null;}

    public static int getAllocationHandle(Address classHandle) {VMHelper.fail(); return 0;}

    public static int getTypeSize(Address classHandle) {VMHelper.fail(); return 0;}

    public static int getArrayElemSize(Address arrayClassHandle) {VMHelper.fail(); return 0;}

    public static int getFastTypeCheckDepth(Address classHandle) {VMHelper.fail(); return 0;}

    protected static void fail() {throw new RuntimeException("Not supported!");}





    //helpers implemented with magics

    @Inline
    public static Address getVTable(Object obj) {
        Address objAddr = ObjectReference.fromObject(obj).toAddress();
        if (COMPRESSED_VTABLE_MODE) {
            int compressedAddr = objAddr.loadInt(Offset.fromIntZeroExtend(OBJ_VTABLE_OFFSET));
            return Address.fromLong(compressedAddr + COMPRESSED_VTABLE_BASE_OFFSET);
        } 
        return objAddr.loadAddress(Offset.fromIntZeroExtend(OBJ_VTABLE_OFFSET));
    }


    @Inline
    public static Address getNativeClass(Object obj) {                
        Address nativeClass = getVTable(obj).loadAddress(Offset.fromIntZeroExtend(VTABLE_CLASS_OFFSET));                
        return nativeClass;
    }
    
    @Inline
    public static Address getManagedClass(Object obj) {                
        //accessing to ManagedObject** m_class_handle field + 1 dereference in Class.h
        Address moAddr = getNativeClass(obj).loadAddress(Offset.fromIntZeroExtend(CLASS_JLC_HANDLE_OFFSET)).loadAddress();                 
        return moAddr;
    } 




    // private area

    private VMHelper() {}

    /** @return vtable offset in managed object structure */
    private static native int getObjectVtableOffset();

    /** @return pointer-type size. 4 or 8 */
    private static native int getPointerTypeSize();

    /** @return true if VM is run in compressed reference mode */
    private static native boolean isCompressedRefsMode();

    /** @return true if VM is run in compressed vtables mode */
    private static native boolean isCompressedVTableMode();

    /** @return vtable base offset if is in compressed-refs mode or -1*/
    private static native long getCompressedModeVTableBaseOffset();

    /** @return object base offset if is in compressed-refs mode or -1*/
    private static native long getCompressedModeObjectBaseOffset();

    /** @return native Class* struct offset in vtable*/
    private static native int getVtableClassOffset();

    /** @return managed object field offset in vtable*/
    private static native int getClassJLCHanldeOffset();

}




