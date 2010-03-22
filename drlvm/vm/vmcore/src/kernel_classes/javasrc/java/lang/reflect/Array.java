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
 * @author Evgueni Brevnov
 */

package java.lang.reflect;

/**
 * @com.intel.drl.spec_ref 
 */
public final class Array {
    
    private Array(){
    }
    
    /**
     * @com.intel.drl.spec_ref 
     */
    public static Object get(Object array, int index)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        try {
            return ((Object[])array)[index];
        } catch (ClassCastException e) {
            if (array instanceof int[]) {
                return new Integer( ((int[])array)[index]);
            }
            if (array instanceof boolean[]) {
                return ((boolean[])array)[index] ? Boolean.TRUE : Boolean.FALSE;
            }
            if (array instanceof float[]) {
                return new Float( ((float[])array)[index]);
            }
            if (array instanceof char[]) {
                return new Character( ((char[])array)[index]);
            }
            if (array instanceof double[]) {
                return new Double( ((double[])array)[index]);
            }
            if (array instanceof long[]) {
                return new Long( ((long[])array)[index]);
            }
            if (array instanceof short[]) {
                return new Short( ((short[])array)[index]);
            }
            if (array instanceof byte[]) {
                return new Byte( ((byte[])array)[index]);
            }
        }
        throw new IllegalArgumentException("Specified argument is not an array");
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static boolean getBoolean(Object array, int index)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        try {
            return ((boolean[])array)[index];
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static byte getByte(Object array, int index)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        try {
            return ((byte[])array)[index];
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static char getChar(Object array, int index)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        try {
            return ((char[])array)[index];
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static double getDouble(Object array, int index)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof double[]) {
            return ((double[])array)[index];
        }
        return getFloat(array, index);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static float getFloat(Object array, int index)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof float[]) {
            return ((float[])array)[index];
        }
        return getLong(array, index);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static int getInt(Object array, int index)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof int[]) {
            return ((int[])array)[index];
        }
        if (array instanceof char[]) {
            return ((char[])array)[index];
        }
        return getShort(array, index);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static int getLength(Object array) throws IllegalArgumentException {
        try {
            return ((Object[])array).length;
        } catch (ClassCastException e) {
            if (array instanceof int[]) {
                return ((int[])array).length;
            }
            if (array instanceof boolean[]) {
                return ((boolean[])array).length;
            }
            if (array instanceof float[]) {
                return ((float[])array).length;
            }
            if (array instanceof char[]) {
                return ((char[])array).length;
            }
            if (array instanceof double[]) {
                return ((double[])array).length;
            }
            if (array instanceof long[]) {
                return ((long[])array).length;
            }
            if (array instanceof short[]) {
                return ((short[])array).length;
            }
            if (array instanceof byte[]) {
                return ((byte[])array).length;
            }
        }
        throw new IllegalArgumentException("Specified argument is not an array");
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static long getLong(Object array, int index)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof long[]) {
            return ((long[])array)[index];
        }
        return getInt(array, index);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static short getShort(Object array, int index)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof short[]) {
            return ((short[])array)[index];
        }
        return getByte(array, index);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static Object newInstance(Class<?> componentType, int length)
        throws NegativeArraySizeException {
        return newInstance(componentType, new int[] { length });
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static Object newInstance(Class<?> componentType, int[] dimensions)
        throws IllegalArgumentException, NegativeArraySizeException {
        if (componentType == null) {
            throw new NullPointerException();
        }
        if (componentType == Void.TYPE || dimensions.length == 0) {
            throw new IllegalArgumentException(
                "Can not create new array instance for the specified arguments");
        }
        return VMReflection.newArrayInstance(componentType, dimensions);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static void set(Object array, int index, Object value)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
    	
    	if (array == null) {
    		throw new NullPointerException();
    	}
    	
        try {
            ((Object[])array)[index] = value;
            return;
        } catch (ClassCastException e) {
            if (value instanceof Number) {
                if (value instanceof Integer) {
                    setInt(array, index, ((Integer)value).intValue());
                    return;
                } else if (value instanceof Float) {
                    setFloat(array, index, ((Float)value).floatValue());
                    return;
                } else if (value instanceof Double) {
                    setDouble(array, index, ((Double)value).doubleValue());
                    return;
                } else if (value instanceof Long) {
                    setLong(array, index, ((Long)value).longValue());
                    return;
                } else if (value instanceof Short) {
                    setShort(array, index, ((Short)value).shortValue());
                    return;
                } else if (value instanceof Byte) {
                    setByte(array, index, ((Byte)value).byteValue());
                    return;
                }
            } else if (value instanceof Boolean) {
                setBoolean(array, index, ((Boolean)value).booleanValue());
                return;
            } else if (value instanceof Character) {
                setChar(array, index, ((Character)value).charValue());
                return;
            }
        } catch (ArrayStoreException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        throw new IllegalArgumentException(
            "Can not assign the specified value to the specified array component");
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static void setBoolean(Object array, int index, boolean value)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        try {
            ((boolean[])array)[index] = value;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static void setByte(Object array, int index, byte value)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof byte[]) {
            ((byte[])array)[index] = value;
            return;
        }
        setShort(array, index, value);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static void setChar(Object array, int index, char value)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof char[]) {
            ((char[])array)[index] = value;
            return;
        }
        setInt(array, index, value);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static void setDouble(Object array, int index, double value)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        try {
            ((double[])array)[index] = value;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static void setFloat(Object array, int index, float value)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof float[]) {
            ((float[])array)[index] = value;
            return;
        }
        setDouble(array, index, value);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static void setInt(Object array, int index, int value)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof int[]) {
            ((int[])array)[index] = value;
            return;
        }
        setLong(array, index, value);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static void setLong(Object array, int index, long value)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof long[]) {
            ((long[])array)[index] = value;
            return;
        }
        setFloat(array, index, value);
    }

    /**
     * @com.intel.drl.spec_ref 
     */
    public static void setShort(Object array, int index, short value)
        throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        if (array instanceof short[]) {
            ((short[])array)[index] = value;
            return;
        }
        setInt(array, index, value);
    }
}

