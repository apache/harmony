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

/**
 * This class represents pointer to memory for working with native functions.
 * It is an analogue of <it>void*</it> C type. Every pointed object can be direct
 * native object or Java object. In both cases NativeBridge provides correct
 * passing addresses to native functions, native address is passed as is, Java
 * object will be implicitly locked and address of lock will be passed to native.
 * NULL pointer can't be wrapped by this object, NULL is represents by Java
 * null value.
 * @see org.apache.harmony.misc.accessors.MemoryAccessor
 * @see org.apache.harmony.misc.accessors.ArrayAccessor
 */
public abstract class VoidPointer {

    public ByteBase byteBase;
    boolean explicitlyLocked = false;

    VoidPointer(int size, boolean direct) {
        byteBase = new ByteBase(size, direct);
    }

    VoidPointer(byte[] arr, int offset, int size) {
        byteBase = new ByteBase(arr, offset, size);
    }

    VoidPointer(long addr) {
        byteBase = new ByteBase(addr);
    }

    VoidPointer(VoidPointer p) {
        byteBase = p.byteBase;
    }

    VoidPointer(ByteBase base) {
        this.byteBase = base;
    }

    public boolean isDirect() {
        return byteBase.isDirect();
    }

    public abstract int size();

    /**
     * Frees memory represented by this pointer. In case of direct object -
     * the native memory is deallocated. In case of locked Java memory - it is
     * released (back copying values to Java is not performed).
     * @see org.apache.harmony.misc.accessors.LockedArray#releaseNoCopy()
     */
    public void free() {
        byteBase.free();
        byteBase = null;
    }


    /**
     * Returns the memory address for this pointer. Performs the explicit lock
     * of the pointed memory.
     * If this object is direct pointer to native memory - returns native address,
     * If this object reference to Java object, then Java object is locked and
     * memory address for locked Java object is returned.
     * Explicitly locked object should be released after usage.
     * @see #release()
     * @see #free()  h
     */
    public final long lock() {
        explicitlyLocked = true;
        return byteBase.longLockPointer();
    }

    /**
     * Performs the explicit release of Java object. This method does nothing
     * if object is direct native pointer or non-locked Java memory.
     */
    public final void release() {
        explicitlyLocked = false;
        byteBase.unlock();
    }

    public void unlock() {
        byteBase.unlock();
    }

    /**
     * Performs the explicit releaseNoCopy of Java object. This method does nothing
     * if object is direct native pointer or non-locked Java memory.
     */
    public final void releaseNoCopy() {
        explicitlyLocked = false;
        byteBase.unlockNoCopy();
    }

    public long longLockPointer() {
        explicitlyLocked = true;
        return byteBase.longLockPointer();
    }

    /** performs short lock */
    public long shortLockPointer() {
        explicitlyLocked = true;
        return byteBase.shortLockPointer();
    }

    /**
     * Internal long lock. Used in NativeBridge wrappers for native functions.
     * @return - memory address.
     */
    final long internalLongLock() {
        return byteBase.longLockPointer();
    }

    /**
     * Internal short lock. Used in NativeBridge wrappers for native functions.
     * @return - memory address.
     */
    final long internalShortLock() {
        return byteBase.shortLockPointer();
    }

    /**
     * Internal release. Unlocks objects if it was implicitly locked. This
     * methods don't release exlicitly locked Java objects.
     */
    final void internalRelease() {
        if(!explicitlyLocked) {
            byteBase.unlock();
        }
    }

    /**
     * Internal release. Unlocks objects if it was implicitly locked. This
     * methods don't release exlicitly locked Java objects. This methods gives
     * a hint, that values should not be copied back to Java object. Used for
     * "in" parameters of functions.
     */
    final void internalReleaseNoCopy() {
        if(!explicitlyLocked) {
            byteBase.unlockNoCopy();
        }
    }

    /**
     * Copies <code>length</code> bytes from src VoidPointer to this.
     * Offset in a destination is dstOffset. Starting offset in source
     * is always 0;
     *
     * @param src source byte base
     * @param dstOffset destination
     * @param length
     */
    public void copy(VoidPointer src, int dstOffset, int length) {
        byteBase.copy(src.byteBase, dstOffset, length);
    }
}