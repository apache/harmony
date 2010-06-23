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

package java.beans;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.harmony.beans.internal.nls.Messages;

public class Statement {
    private static final Object[] EMPTY_ARRAY = new Object[0];

    private Object target;

    private String methodName;

    private Object[] arguments;
    
    // cache used methods of specified target class to accelerate method search
    private static WeakHashMap<Class<?>, Method[]> cache = new WeakHashMap<Class<?>, Method[]>();
    
    // the special method name donating constructors
    static final String CONSTRUCTOR_NAME = "new"; //$NON-NLS-1$

    // the special method name donating array "get"
    static final String ARRAY_GET = "get"; //$NON-NLS-1$

    // the special method name donating array "set"
    static final String ARRAY_SET = "set"; //$NON-NLS-1$

    public Statement(Object target, String methodName, Object[] arguments) {
        this.target = target;
        this.methodName = methodName;
        if (arguments != null) {
            this.arguments = arguments;
        } else {
            this.arguments = EMPTY_ARRAY;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Object theTarget = getTarget();
        String theMethodName = getMethodName();
        Object[] theArguments = getArguments();
        String targetVar = theTarget != null ? convertClassName(theTarget.getClass()) : "null"; //$NON-NLS-1$
        sb.append(targetVar);
        sb.append('.');
        sb.append(theMethodName);
        sb.append('(');
        if (theArguments != null) {
            for (int i = 0; i < theArguments.length; ++i) {
                if (i > 0) {
                    sb.append(", "); //$NON-NLS-1$
                }
                if (theArguments[i] == null) {
                    sb.append("null"); //$NON-NLS-1$
                } else if (theArguments[i] instanceof String) {
                    sb.append('"');
                    sb.append(theArguments[i].toString());
                    sb.append('"');
                } else {
                    sb.append(convertClassName(theArguments[i].getClass()));
                }
            }
        }
        sb.append(')');
        sb.append(';');
        return sb.toString();
    }

    public String getMethodName() {
        return methodName;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public Object getTarget() {
        return target;
    }

    public void execute() throws Exception {
        invokeMethod();
    }

    Object invokeMethod() throws Exception {
        Object result = null;
        try {
            Object theTarget = getTarget();
            String theMethodName = getMethodName();
            Object[] theArguments = getArguments();
            if (theTarget.getClass().isArray()) {
                Method method = findArrayMethod(theMethodName, theArguments);
                Object[] args = new Object[theArguments.length + 1];
                args[0] = theTarget;
                System.arraycopy(theArguments, 0, args, 1, theArguments.length);
                result = method.invoke(null, args);
            } else if (theMethodName.equals("newInstance") //$NON-NLS-1$
                    && theTarget == Array.class) {
                Class<?> componentType = (Class<?>) theArguments[0];
                int length = ((Integer) theArguments[1]).intValue();
                result = Array.newInstance(componentType, length);
            } else if (theMethodName.equals("new") //$NON-NLS-1$
                    || theMethodName.equals("newInstance")) { //$NON-NLS-1$
                if (theTarget instanceof Class<?>) {
                    Constructor<?> constructor = findConstructor((Class<?>)theTarget, theArguments);
                    result = constructor.newInstance(theArguments);
                } else {
                    if ("new".equals(theMethodName)) { //$NON-NLS-1$
                        throw new NoSuchMethodException(this.toString());
                    }
                    // target class declares a public named "newInstance" method
                    Method method = findMethod(theTarget.getClass(),
                            theMethodName, theArguments, false);
                    result = method.invoke(theTarget, theArguments);
                }
            } else if (theMethodName.equals("newArray")) {//$NON-NLS-1$
                // create a new array instance without length attribute
                int length = theArguments.length;
                Class<?> clazz = (Class<?>) theTarget;

                // check the element types of array
                for (int i = 0; i < length; i++) {
                    boolean isNull = theArguments[i] == null;
                    boolean isPrimitiveWrapper = isNull ? false
                            : isPrimitiveWrapper(theArguments[i].getClass(),
                                    clazz);
                    boolean isAssignable = isNull ? false : clazz
                            .isAssignableFrom(theArguments[i].getClass());
                    if (!isNull && !isPrimitiveWrapper && !isAssignable) {
                        throw new IllegalArgumentException(Messages
                                .getString("beans.63")); //$NON-NLS-1$
                    }
                }
                result = Array.newInstance(clazz, length);
                if (clazz.isPrimitive()) {
                    // Copy element according to primitive types
                    arrayCopy(clazz, theArguments, result, length);
                } else {
                    // Copy element of Objects
                    System.arraycopy(theArguments, 0, result, 0, length);
                }
                return result;
            } else if (theTarget instanceof Class<?>) {
                Method method = null;
                boolean found = false;
                try {
                    /*
                     * Try to look for a static method of class described by the
                     * given Class object at first process only if the class
                     * differs from Class itself
                     */
                    if (theTarget != Class.class) {
                        method = findMethod((Class<?>) theTarget, theMethodName, theArguments, true);
                        result = method.invoke(null, theArguments);
                        found = true;
                    }
                } catch (NoSuchMethodException e) {
                    // expected
                }
                if (!found) {
                    // static method was not found
                    // try to invoke method of Class object
                    if (theMethodName.equals("forName") //$NON-NLS-1$
                            && theArguments.length == 1 && theArguments[0] instanceof String) {
                        // special handling of Class.forName(String)
                        try {
                            result = Class.forName((String) theArguments[0]);
                        } catch (ClassNotFoundException e2) {
                            result = Class.forName((String) theArguments[0], true, Thread
                                    .currentThread().getContextClassLoader());
                        }
                    } else {
                        method = findMethod(theTarget.getClass(), theMethodName, theArguments, false);
                        result = method.invoke(theTarget, theArguments);
                    }
                }
            } else if (theTarget instanceof Iterator<?>){
            	final Iterator<?> iterator = (Iterator<?>) theTarget;
				final Method method = findMethod(theTarget.getClass(), theMethodName,
						theArguments, false);
				if (iterator.hasNext()) {
					PrivilegedAction<Object> action = new PrivilegedAction<Object>() {

						public Object run() {
							try {
								method.setAccessible(true);
								return (method.invoke(iterator, new Object[0]));
							} catch (Exception e) {
								// ignore
							} 
							return null;
						}
						
					};
					result = action.run();
				}
            } else {
                Method method = findMethod(theTarget.getClass(), theMethodName,
                        theArguments, false);
                method.setAccessible(true);
                result = method.invoke(theTarget, theArguments);
            }
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getCause();
            throw (t != null) && (t instanceof Exception) ? (Exception) t : ite;
        }
        return result;
    }
    
    private void arrayCopy(Class<?> type, Object[] src, Object dest, int length) {
        if (type == boolean.class) {
            boolean[] destination = (boolean[]) dest;
            for (int i = 0; i < length; i++) {
                destination[i] = ((Boolean) src[i]).booleanValue();
            }
        } else if (type == short.class) {
            short[] destination = (short[]) dest;
            for (int i = 0; i < length; i++) {
                destination[i] = ((Short) src[i]).shortValue();
            }
        } else if (type == byte.class) {
            byte[] destination = (byte[]) dest;
            for (int i = 0; i < length; i++) {
                destination[i] = ((Byte) src[i]).byteValue();
            }
        } else if (type == char.class) {
            char[] destination = (char[]) dest;
            for (int i = 0; i < length; i++) {
                destination[i] = ((Character) src[i]).charValue();
            }
        } else if (type == int.class) {
            int[] destination = (int[]) dest;
            for (int i = 0; i < length; i++) {
                destination[i] = ((Integer) src[i]).intValue();
            }
        } else if (type == long.class) {
            long[] destination = (long[]) dest;
            for (int i = 0; i < length; i++) {
                destination[i] = ((Long) src[i]).longValue();
            }
        } else if (type == float.class) {
            float[] destination = (float[]) dest;
            for (int i = 0; i < length; i++) {
                destination[i] = ((Float) src[i]).floatValue();
            }
        } else if (type == double.class) {
            double[] destination = (double[]) dest;
            for (int i = 0; i < length; i++) {
                destination[i] = ((Double) src[i]).doubleValue();
            }
        }
    }

    private Method findArrayMethod(String theMethodName, Object[] theArguments) throws NoSuchMethodException {
        // the code below reproduces exact RI exception throwing behavior
        if (!theMethodName.equals("set") && !theMethodName.equals("get")) { //$NON-NLS-1$ //$NON-NLS-2$
            throw new NoSuchMethodException(Messages.getString("beans.3C")); //$NON-NLS-1$
        } else if (theArguments.length > 0 && theArguments[0].getClass() != Integer.class) {
            throw new ClassCastException(Messages.getString("beans.3D")); //$NON-NLS-1$
        } else if (theMethodName.equals("get") && (theArguments.length != 1)) { //$NON-NLS-1$
            throw new ArrayIndexOutOfBoundsException(Messages.getString("beans.3E")); //$NON-NLS-1$
        } else if (theMethodName.equals("set") && (theArguments.length != 2)) { //$NON-NLS-1$
            throw new ArrayIndexOutOfBoundsException(Messages.getString("beans.3F")); //$NON-NLS-1$
        }
        if (theMethodName.equals("get")) { //$NON-NLS-1$
            return Array.class.getMethod("get", new Class[] { Object.class, //$NON-NLS-1$
                    int.class });
        }
        return Array.class.getMethod("set", new Class[] { Object.class, //$NON-NLS-1$
                int.class, Object.class });
    }

    private Constructor<?> findConstructor(Class<?> targetClass, Object[] theArguments) throws NoSuchMethodException {
        Class<?>[] argClasses = getClasses(theArguments);
        Constructor<?> result = null;
        Constructor<?>[] constructors = targetClass.getConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == argClasses.length) {
                boolean found = true;
                for (int j = 0; j < parameterTypes.length; ++j) {
                    boolean argIsNull = argClasses[j] == null;
                    boolean argIsPrimitiveWrapper = isPrimitiveWrapper(argClasses[j],
                            parameterTypes[j]);
                    boolean paramIsPrimitive = parameterTypes[j].isPrimitive();
                    boolean paramIsAssignable = argIsNull ? false : parameterTypes[j]
                            .isAssignableFrom(argClasses[j]);
                    if (!argIsNull && !paramIsAssignable && !argIsPrimitiveWrapper || argIsNull
                            && paramIsPrimitive) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    if (result == null) {
                        // first time, set constructor
                        result = constructor;
                        continue;
                    }
                    // find out more suitable constructor
                    Class<?>[] resultParameterTypes = result
                            .getParameterTypes();
                    boolean isAssignable = true;
                    for (int j = 0; j < parameterTypes.length; ++j) {
                        if (theArguments[j] != null
                                && !(isAssignable &= resultParameterTypes[j]
                                        .isAssignableFrom(parameterTypes[j]))) {
                            break;
                        }
                        if (theArguments[j] == null
                                && !(isAssignable &= parameterTypes[j]
                                        .isAssignableFrom(resultParameterTypes[j]))) {
                            break;
                        }
                    }
                    if (isAssignable) {
                        result = constructor;
                    }
                }
            }
        }
        if (result == null) {
            throw new NoSuchMethodException(Messages.getString(
                    "beans.40", targetClass.getName())); //$NON-NLS-1$
        }
        return result;
    }

    /**
     * Searches for best matching method for given name and argument types.
     */
    static Method findMethod(Class<?> targetClass, String methodName, Object[] arguments,
            boolean methodIsStatic) throws NoSuchMethodException {
        Class<?>[] argClasses = getClasses(arguments);
        Method[] methods = null;
        
        if(cache.containsKey(targetClass)){
            methods = cache.get(targetClass);
        }else{
            methods = targetClass.getMethods();
            cache.put(targetClass, methods);
        }
        
        ArrayList<Method> foundMethods = new ArrayList<Method>();
        Method[] foundMethodsArr;
        for (Method method : methods) {
            int mods = method.getModifiers();
            if (method.getName().equals(methodName)
                    && (methodIsStatic ? Modifier.isStatic(mods) : true)) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == argClasses.length) {
                    boolean found = true;
                    for (int j = 0; j < parameterTypes.length; ++j) {
                        boolean argIsNull = (argClasses[j] == null);
                        boolean argIsPrimitiveWrapper = isPrimitiveWrapper(argClasses[j],
                                parameterTypes[j]);
                        boolean paramIsAssignable = argIsNull ? false : parameterTypes[j]
                                .isAssignableFrom(argClasses[j]);
                        if (!argIsNull && !paramIsAssignable && !argIsPrimitiveWrapper){
                            found = false;
                            break;
                        }
                    }
                    if (found) {
                        foundMethods.add(method);
                    }
                }
            }
        }
        if (foundMethods.size() == 0) {
            throw new NoSuchMethodException(Messages.getString("beans.41", methodName)); //$NON-NLS-1$
        }
        if(foundMethods.size() == 1){
            return foundMethods.get(0);
        }
        foundMethodsArr = foundMethods.toArray(new Method[foundMethods.size()]);
        //find the most relevant one
        MethodComparator comparator = new MethodComparator(methodName, argClasses);
        Method chosenOne = foundMethodsArr[0];
        for (int i = 1; i < foundMethodsArr.length; i++) {
            int difference = comparator.compare(chosenOne, foundMethodsArr[i]);
            //if 2 methods have same relevance, throw exception
            if (difference == 0) {
                // if 2 methods have the same signature, check their return type
                Class<?> oneReturnType = chosenOne.getReturnType();
                Class<?> foundMethodReturnType = foundMethodsArr[i]
                        .getReturnType();
                if (oneReturnType.equals(foundMethodReturnType)) {
                    // if 2 methods have the same signature and return type,
                    // throw NoSuchMethodException
                    throw new NoSuchMethodException(Messages.getString(
                            "beans.62", methodName)); //$NON-NLS-1$
                }

                if (oneReturnType.isAssignableFrom(foundMethodReturnType)) {
                    // if chosenOne is super class or interface of
                    // foundMethodReturnType, set chosenOne to foundMethodArr[i]
                    chosenOne = foundMethodsArr[i];
                }
            }
            if(difference > 0){
                chosenOne = foundMethodsArr[i];
            }
        }
        return chosenOne;
    }

    static boolean isStaticMethodCall(Statement stmt) {
        Object target = stmt.getTarget();
        String mName = stmt.getMethodName();
        if (!(target instanceof Class<?>)) {
            return false;
        }
        try {
            Statement.findMethod((Class<?>) target, mName, stmt.getArguments(), true);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /*
     * The list of "method signatures" used by persistence delegates to create
     * objects. Not necessary reflects to real methods.
     */
    private static final String[][] pdConstructorSignatures = {
            { "java.lang.Class", "new", "java.lang.Boolean", "", "", "" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            { "java.lang.Class", "new", "java.lang.Byte", "", "", "" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            { "java.lang.Class", "new", "java.lang.Character", "", "", "" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            { "java.lang.Class", "new", "java.lang.Double", "", "", "" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            { "java.lang.Class", "new", "java.lang.Float", "", "", "" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            { "java.lang.Class", "new", "java.lang.Integer", "", "", "" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            { "java.lang.Class", "new", "java.lang.Long", "", "", "" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            { "java.lang.Class", "new", "java.lang.Short", "", "", "" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            { "java.lang.Class", "new", "java.lang.String", "", "", "" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            { "java.lang.Class", "forName", "java.lang.String", "", "", "" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            { "java.lang.Class", "newInstance", "java.lang.Class", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    "java.lang.Integer", "", "" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            { "java.lang.reflect.Field", "get", "null", "", "", "" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            { "java.lang.Class", "forName", "java.lang.String", "", "", "" } //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
    };

    static boolean isPDConstructor(Statement stmt) {
        Object target = stmt.getTarget();
        String methodName = stmt.getMethodName();
        Object[] args = stmt.getArguments();
        String[] sig = new String[pdConstructorSignatures[0].length];
        if (target == null || methodName == null || args == null || args.length == 0) {
            // not a constructor for sure
            return false;
        }
        sig[0] = target.getClass().getName();
        sig[1] = methodName;
        for (int i = 2; i < sig.length; i++) {
            if (args.length > i - 2) {
                sig[i] = args[i - 2] != null ? args[i - 2].getClass().getName() : "null"; //$NON-NLS-1$
            } else {
                sig[i] = ""; //$NON-NLS-1$
            }
        }
        for (String[] element : pdConstructorSignatures) {
            if (Arrays.equals(sig, element)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPrimitiveWrapper(Class<?> wrapper, Class<?> base) {
        return (base == boolean.class) && (wrapper == Boolean.class) || (base == byte.class)
                && (wrapper == Byte.class) || (base == char.class)
                && (wrapper == Character.class) || (base == short.class)
                && (wrapper == Short.class) || (base == int.class)
                && (wrapper == Integer.class) || (base == long.class)
                && (wrapper == Long.class) || (base == float.class) && (wrapper == Float.class)
                || (base == double.class) && (wrapper == Double.class);
    }

    private static Class<?> getPrimitiveWrapper(Class<?> base) {
        Class<?> res = null;
        if (base == boolean.class) {
            res = Boolean.class;
        } else if (base == byte.class) {
            res = Byte.class;
        } else if (base == char.class) {
            res = Character.class;
        } else if (base == short.class) {
            res = Short.class;
        } else if (base == int.class) {
            res = Integer.class;
        } else if (base == long.class) {
            res = Long.class;
        } else if (base == float.class) {
            res = Float.class;
        } else if (base == double.class) {
            res = Double.class;
        }
        return res;
    }

    static String convertClassName(Class<?> type) {
        StringBuilder clazzNameSuffix = new StringBuilder();
        Class<?> componentType = null;
        Class<?> clazzType = type;
        while ((componentType = clazzType.getComponentType()) != null) {
            clazzNameSuffix.append("Array"); //$NON-NLS-1$
            clazzType = componentType;
        }
        String clazzName = clazzType.getName();
        int k = clazzName.lastIndexOf('.');
        if (k != -1 && k < clazzName.length()) {
            clazzName = clazzName.substring(k + 1);
        }
        if (clazzNameSuffix.length() == 0 && "String".equals(clazzName)) { //$NON-NLS-1$
            return "\"\""; //$NON-NLS-1$
        }
        return clazzName + clazzNameSuffix.toString();
    }

    private static Class<?>[] getClasses(Object[] arguments) {
        Class<?>[] result = new Class[arguments.length];
        for (int i = 0; i < arguments.length; ++i) {
            result[i] = (arguments[i] == null) ? null : arguments[i].getClass();
        }
        return result;
    }

    /**
     * Comparator to determine which of two methods is "closer" to the reference
     * method.
     */
    static class MethodComparator implements Comparator<Method> {
        static int INFINITY = Integer.MAX_VALUE;

        private String referenceMethodName;

        private Class<?>[] referenceMethodArgumentTypes;

        private final Map<Method, Integer> cache;

        public MethodComparator(String refMethodName, Class<?>[] refArgumentTypes) {
            this.referenceMethodName = refMethodName;
            this.referenceMethodArgumentTypes = refArgumentTypes;
            cache = new HashMap<Method, Integer>();
        }

        public int compare(Method m1, Method m2) {
            Integer norm1 = cache.get(m1);
            Integer norm2 = cache.get(m2);
            if (norm1 == null) {
                norm1 = Integer.valueOf(getNorm(m1));
                cache.put(m1, norm1);
            }
            if (norm2 == null) {
                norm2 = Integer.valueOf(getNorm(m2));
                cache.put(m2, norm2);
            }
            return (norm1.intValue() - norm2.intValue());
        }

        /**
         * Returns the norm for given method. The norm is the "distance" from
         * the reference method to the given method.
         * 
         * @param m
         *            the method to calculate the norm for
         * @return norm of given method
         */
        private int getNorm(Method m) {
            String methodName = m.getName();
            Class<?>[] argumentTypes = m.getParameterTypes();
            int totalNorm = 0;
            if (!referenceMethodName.equals(methodName)
                    || referenceMethodArgumentTypes.length != argumentTypes.length) {
                return INFINITY;
            }
            for (int i = 0; i < referenceMethodArgumentTypes.length; i++) {
                if (referenceMethodArgumentTypes[i] == null) {
                    // doesn't affect the norm calculation if null
                    continue;
                }
                if (referenceMethodArgumentTypes[i].isPrimitive()) {
                    referenceMethodArgumentTypes[i] = getPrimitiveWrapper(referenceMethodArgumentTypes[i]);
                }
                if (argumentTypes[i].isPrimitive()) {
                    argumentTypes[i] = getPrimitiveWrapper(argumentTypes[i]);
                }
                totalNorm += getDistance(referenceMethodArgumentTypes[i], argumentTypes[i]);
            }
            return totalNorm;
        }

        /**
         * Returns a "hierarchy distance" between two classes.
         * 
         * @param clz1
         * @param clz2
         *            should be superclass or superinterface of clz1
         * @return hierarchy distance from clz1 to clz2, Integer.MAX_VALUE if
         *         clz2 is not assignable from clz1.
         */
        private static int getDistance(Class<?> clz1, Class<?> clz2) {
            Class<?> superClz;
            int superDist = INFINITY;
            if (!clz2.isAssignableFrom(clz1)) {
                return INFINITY;
            }
            if (clz1.getName().equals(clz2.getName())) {
                return 0;
            }
            superClz = clz1.getSuperclass();
            if (superClz != null) {
                superDist = getDistance(superClz, clz2);
            }
            if (clz2.isInterface()) {
                Class<?>[] interfaces = clz1.getInterfaces();
                int bestDist = INFINITY;
                for (Class<?> element : interfaces) {
                    int curDist = getDistance(element, clz2);
                    if (curDist < bestDist) {
                        bestDist = curDist;
                    }
                }
                if (superDist < bestDist) {
                    bestDist = superDist;
                }
                return (bestDist != INFINITY ? bestDist + 1 : INFINITY);
            }
            return (superDist != INFINITY ? superDist + 1 : INFINITY);
        }
    }
}
