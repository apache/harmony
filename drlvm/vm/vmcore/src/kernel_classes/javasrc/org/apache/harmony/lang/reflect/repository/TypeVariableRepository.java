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
package org.apache.harmony.lang.reflect.repository;

import java.lang.reflect.TypeVariable;
import java.lang.reflect.GenericDeclaration;

/**
 * @author Serguei S. Zapreyev
 */

/**
 * (This all should be considered as an experimental approach which could be changed on
 * java.lang.ref using)
 * 
 * TypeVariableRepository keeps iformation about type variables.
 * 
 * For now the following approach for keeping is realized.
 * A concreet TV instance is included into repository if somebody do the *first* request for
 * the reflect's method like Method.getGenericReturnType()->Class.MethodData.initGenericReturnType()
 * (Field.getGenericType()->Field.FieldData.initGenericType()
 *  Method.getGenericExceptionTypes()->Class.MethodData.initGenericExceptionTypes()
 *  ...
 *  Constructor.getGenericParameterTypes()->Class.ConstructorData.initGenericParameterTypes()
 *  ...
 * ). If the identical TV is accessed anyhow then the pointer from repository is used. However,
 * further it can be removed from the repository if the cache is full under the new instance including.
 * Then if reflect's request for such TV using the earlier used interface methods then the pointers
 * to such instance kept within the Method/Constructor/Field/Class instances are returned just if it is
 * absent within repository. But if we use other Method/Constructor/Field/Class instances to access
 * to some instance of the same TV type which has been removed from repository then the new
 * exemplar of instance is included into repository and the pointer to it is replicated if it needs
 * into the new Method/Constructor/Field/Class instances.
 * Another way to recreate some TV entry within repository is the AuxiliaryFinder.findTypeVariable() method
 * which can recreate the removed instance getting it within Method/Constructor/Field/Class instances
 * which were used to acces to the mentioned TV earlier.
 * 
 * Maybe, it would be more convinient to realize another repository implementation
 * where the accumulative approach will be used.
 * 
 */
public final class TypeVariableRepository {

	static final class TypeVariableCache {
	
    /**
     * This class realizes the TypeVariable repository which
     * is just a cache. When the cache is exceeded at that time this experimental original cache algorithm (which, if
     * it will justify hopes, should be combined in one for all type repositories here) rewrites the worst entity
     * which can't be copied any more only recreated (as equal, of course). If the cache has an entity which
     * the reflect supporting algorithm supposes to include there than the cache's algorithm copies the existing entity.
     * If the cache is not full and an entity does not exist than there the cache's algorithm
     * creates preliminary an entity and inserts it there.
     * So, an earlier created type variable exists while the cache's algorithm has pointer[s] of it
     * within the cache (entity has not removed yet during cleanings)
     * or user's java code keeps pointer[s] of this type variable.
     */

			private static class TVSSynchro {
			};

			private static int cacheLenght = 2048;
			private static TypeVariable cache[] = new TypeVariable[cacheLenght];
			private static int counter = 0;
			private static int worth[] = new int[cacheLenght];

			/**
			 *   To delete the worst entry.
			 */
			static int deleteTypeVariable() {
				int min = -1;
				int min2 = -1;

				synchronized (TVSSynchro.class) {
					float minWorth = ((worth[0] >> 16) > 0 ? (worth[0] & 0xFFFF) / worth[0] >> 16 : worth[0]);
					int minWorth2 = worth[0];
					float tmp;
					int i = 0;
					for (; i < cacheLenght; i++) {
						try {
							if ((tmp = (worth[i] & 0xFFFF) / worth[i] >> 16) <= minWorth) {
								min = i;
								minWorth = tmp;
							}
						} catch (ArithmeticException _) {
							if ((tmp = worth[i]) <= minWorth2) {
								min2 = i;
								minWorth2 = (int) tmp;
							}
						}
					}
					if (i == cacheLenght && min == -1) {
						min = min2;
					}
					cache[min] = null;
					worth[min] = 0;
				}
				return min;
			}

			/**
			 *  To create new cache entry
			 */
			static int insertTypeVariable(TypeVariable typeVariable, int ip) {
				synchronized (TVSSynchro.class) {
					if (ip < 0) {
						for (int ind = 0; ind < cacheLenght ; ind++) {
							if (null == cache[ind]) {
								cache[ind] = typeVariable;
								worth[ind] = 1;
								return ind;
							}
						}
						return insertTypeVariable(typeVariable, deleteTypeVariable());
					} else {
						cache[ip] = typeVariable;

						boolean flg = false;
						short sv = (short) (worth[ip] & 0xFFFF);
						if (sv == Short.MAX_VALUE - 1){
							flg = true; // to reduce
						}
						worth[ip] = (sv + 1) | (worth[ip] & 0xFFFF0000);
						if (flg) {
							try {
								for (int ind = 0; ind < cacheLenght ; ind++) { // reducing
									short sv1 = (short) (worth[ind] >> 16);
									short sv2 = (short) (worth[ind] & 0xFFFF);
									worth[ind] = (sv2 == 1 ? 1 : sv2 >> 2) | (((sv1 == 1 ? 1 : sv1 >> 2) + 1) << 16);
								}
							} catch (NullPointerException _) {
							}
						}
					}
				}
				return ip;
			}

			/**
			 *  To return TypeVariable of cache.
			 */
			static TypeVariable valueTypeVariable(String typeVariableName,  GenericDeclaration decl) {
				synchronized (TVSSynchro.class) {
					boolean flg = false;
					if (counter == cacheLenght) { // Do smoke, it's time for reordering
						try {
							for (int ind = 0; ind < cacheLenght ; ind++) {
								short sv1 = (short) (worth[ind] >> 16);
								if (sv1 == Short.MAX_VALUE - 1){
									flg = true; // to reduce
								}
								worth[ind] = worth[ind] & 0xFFFF | ((sv1 + 1) << 16);
							}
						} catch(NullPointerException _) {
						}
						if (flg) {
							try {
								for (int ind = 0; ind < cacheLenght ; ind++) { // reducing
									short sv1 = (short) (worth[ind] >> 16);
									short sv2 = (short) (worth[ind] & 0xFFFF);
									worth[ind] = (sv2 == 1 ? 1 : sv2 >> 2) | (((sv1 == 1 ? 1 : sv1 >> 2) + 1) << 16);
								}
							} catch (NullPointerException _) {
							}
						}
						counter = 0;
					} else {
						counter++;
					}
				}
				try {
					for (int ind = 0; ind < cacheLenght ; ind++) {
						if (typeVariableName.equals(cache[ind].getName()) && decl.equals(cache[ind].getGenericDeclaration())) {
							return cache[ind];
						}
					}
				} catch (NullPointerException _) {
				}
				return null;
			}
		}
	
    /**
     * This method returns a registered type variable if it exists within the repository.
     * 
     * @param typeVariableName a name of a type variable.
     * @param startPoint an instance of the Class, Method, Constructor or Field type to start the search
     *        of a type variable declaration place.
     * @return the found type variable or null if it does not exist within repository.
     */
	public static TypeVariable findTypeVariable(String typeVariableName, Object startPoint) {
		return TypeVariableCache.valueTypeVariable(typeVariableName, (GenericDeclaration) startPoint);
	}
	
    /**
     * This method registers a type variable within the repository.
     * 
     * @param typeVariable a type variable.
     * @param typeVariableName a name of a type variable.
     * @param startPoint an instance of the Class, Method, Constructor or Field type to start the search
     *        of a type variable declaration place.
     */
	public static void registerTypeVariable(TypeVariable typeVariable, String typeVariableName, Object startPoint) {
		TypeVariableCache.insertTypeVariable(typeVariable, -1);		
	}
}
