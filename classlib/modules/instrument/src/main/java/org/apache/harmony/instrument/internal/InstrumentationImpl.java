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

package org.apache.harmony.instrument.internal;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import org.apache.harmony.instrument.internal.nls.Messages;

/**
 * Default implementation of Instrumentation
 */
public class InstrumentationImpl implements Instrumentation {
    /*
     * ----------------------------------------------------------------------------
     * Consts
     * ----------------------------------------------------------------------------
     */

    private static final Class[] PREMAIN_SIGNATURE = new Class[] {
            String.class, Instrumentation.class };

    /*
     * ----------------------------------------------------------------------------
     * Variables
     * ----------------------------------------------------------------------------
     */
    private ClassFileTransformer[] transformers = new ClassFileTransformer[0];

    private boolean isRedefineClassesSupported;

    /*
     * ----------------------------------------------------------------------------
     * Constructor
     * ----------------------------------------------------------------------------
     */
    /**
     * Constructs a new instance.
     * 
     * @param isRedefineClassesSupported
     */
    public InstrumentationImpl(boolean isRedefineClassesSupported) {
        this.isRedefineClassesSupported = isRedefineClassesSupported;
    }

    /*
     * ----------------------------------------------------------------------------
     * Methods implemented from Instrumentation
     * ----------------------------------------------------------------------------
     */
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.instrument.Instrumentation#addTransformer(java.lang.instrument.ClassFileTransformer)
     */
    public void addTransformer(ClassFileTransformer transformer) {
        if (null == transformer) {
            throw new NullPointerException();
        }
        int length = transformers.length;
        ClassFileTransformer[] temp = new ClassFileTransformer[length + 1];
        System.arraycopy(transformers, 0, temp, 0, length);
        temp[length] = transformer;
        transformers = temp;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.instrument.Instrumentation#redefineClasses(java.lang.instrument.ClassDefinition[])
     */
    public void redefineClasses(ClassDefinition[] definitions)
            throws ClassNotFoundException, UnmodifiableClassException {
        if (!isRedefineClassesSupported) {
            throw new UnsupportedOperationException(Messages.getString("instrument.3")); //$NON-NLS-1$
        }
        for (int i = 0; i < definitions.length; i++) {
            if (null == definitions[i]) {
                throw new NullPointerException();
            }
        }
        redefineClasses_native(definitions);
    }

    private native void redefineClasses_native(ClassDefinition[] definitions);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.instrument.Instrumentation#removeTransformer(java.lang.instrument.ClassFileTransformer)
     */
    public boolean removeTransformer(ClassFileTransformer transformer) {
        if (null == transformer) {
            throw new NullPointerException();
        }
        int i = 0;
        int length = transformers.length;
        for (i = length-1; i >= 0 && transformers[i] != transformer; i--)
            ;
        if (i == -1) {
            return false;
        }
        ClassFileTransformer[] temp = new ClassFileTransformer[length - 1];
        if (i > 0) {
            System.arraycopy(transformers, 0, temp, 0, i);
        }
        if (i < length - 1) {
            System.arraycopy(transformers, i + 1, temp, i, length - i - 1);
        }
        transformers = temp;
        return true;
    }

    public void clear() {
        transformers = new ClassFileTransformer[0];
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.instrument.Instrumentation#getAllLoadedClasses()
     */
    public native Class[] getAllLoadedClasses();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.instrument.Instrumentation#getInitiatedClasses(java.lang.ClassLoader)
     */
    public native Class[] getInitiatedClasses(ClassLoader loader);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.instrument.Instrumentation#getObjectSize(java.lang.Object)
     */
    public long getObjectSize(Object objectToSize) {
        if (null == objectToSize) {
            throw new NullPointerException();
        }
        return getObjectSize_native(objectToSize);
    }

    private native long getObjectSize_native(Object objectToSize);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.instrument.Instrumentation#isRedefineClassesSupported()
     */
    public boolean isRedefineClassesSupported() {
        return isRedefineClassesSupported;
    }

    /*
     * ----------------------------------------------------------------------------
     * Callback methods for native JVMTI agent
     * ----------------------------------------------------------------------------
     */
    /*
     * ClassFileLoadHook event handler method
     */
    private byte[] transform(ClassLoader loader, byte[] classNameBytes,
            Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {
        byte[] source = classfileBuffer;
        byte[] result = null;
        byte[] trans = null;
        String className = new String(classNameBytes);
        for (ClassFileTransformer t : transformers) {
            try {
                trans = t.transform(loader, className, classBeingRedefined,
                        protectionDomain, source);
                if (null != trans && 0 != trans.length) {
                    result = trans;
                    source = trans;
                }
            } catch (Exception e) {
                // nothing to do, just continue~
            }
        }
        return result;
    }

    /*
     * callback method to execute javaagents' premain method
     */
    private void executePremain(byte[] className, byte[] options) {
        try {
            ClassLoader loader = ClassLoader.getSystemClassLoader();
            Class c = loader.loadClass(new String(className));
            Method method = c.getMethod("premain", PREMAIN_SIGNATURE); //$NON-NLS-1$
            method.invoke(null, new Object[] {
                    null == options ? null : new String(options), this });
        } catch (Exception e) {
            e.printStackTrace();
            System.err
                    .println(Messages.getString("instrument.4")); //$NON-NLS-1$
            System.exit(1);
        }
    }
}
