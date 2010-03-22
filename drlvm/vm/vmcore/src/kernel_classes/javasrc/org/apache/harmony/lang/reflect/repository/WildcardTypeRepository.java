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

import java.lang.reflect.WildcardType;

import org.apache.harmony.lang.reflect.parser.*;

/**
 * @author Serguei S. Zapreyev
 */

/**
 * (This all should be considered as an experimental approach which could be changed on
 * java.lang.ref using)
 * 
 * WildcardTypeRepository keeps iformation about wild cards.
 * 
 * For now the following approach for keeping is realized.
 * A concreet WC instance is included into repository if somebody do the *first* request for
 * the reflect's method like Class.getGenericSuperclass()->Class.ReflectionData.initGenericSuperclass()
 * (Field.getGenericType()->Field.FieldData.initGenericType()
 *  Method.getGenericReturnType()->Class.MethodData.initGenericReturnType()
 *  ...
 *  Constructor.getGenericParameterTypes()->Class.ConstructorData.initGenericParameterTypes()
 *  ...
 * ) and there are ParameterizedTypes with wild cards.
 * The wild card creation and allocation within repository is located within AuxiliaryCreator.createTypeArg method.
 * If the identical WC is accessed anyhow then the pointer from repository is used. However,
 * further it can be removed from the repository if the cache is full under the new instance including.
 * Then if reflect's request for such WC using the earlier used interface methods then the pointers
 * to such instance kept within the Method/Constructor/Field/Class instances are returned just if it is
 * absent within repository. But if we access anyhow to some instance of the same WC type which has 
 * been removed from repository then the new exemplar of instance is created and included into 
 * repository and the pointer to it is replicated if it needs.
 * 
 * Maybe, it would be more convinient to realize another repository implementation
 * where the accumulative approach will be used.
 * 
 * Note1. The inserted on 01.27.06 cache3 may significantly slow the algorithm but
 * it needs to provide the functional completeness of repository because we should distinguish
 * the entries with equals signatures if there are TVARs which are only homonym ones, i.e.
 * they are parameters of different generic declarations. 
 */
public final class WildcardTypeRepository {

		static final class WildcardTypeCache {
	
    /**
     * This class realizes the WildcardType repository which
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
			private static String cache[] = new String[cacheLenght];
			private static WildcardType cache2[] = new WildcardType[cacheLenght];
			private static InterimWildcardType cache3[] = new InterimWildcardType[cacheLenght];
			private static Object cache4[] = new Object[cacheLenght];
			private static int counter = 0;
			private static int worth[] = new int[cacheLenght];

			/**
			 *   To delete the worst entry.
			 */
			static int deleteWildcardType() {
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
					cache2[min] = null;
					cache3[min] = null;
					cache4[min] = null;
					worth[min] = 0;
				}
				return min;
			}

			/**
			 *  To create new cache entry
			 */
			static int insertWildcardType(WildcardType wildcardType, InterimWildcardType prsrdWildcardType, String signature, Object startPoint, int ip) {
				synchronized (TVSSynchro.class) {
					if (ip < 0) {
						for (int ind = 0; ind < cacheLenght ; ind++) {
							if (null == cache[ind]) {
								cache[ind] = signature;
								cache2[ind] = wildcardType;
								cache3[ind] = prsrdWildcardType;
								cache4[ind] = startPoint;
								worth[ind] = 1;
								return ind;
							}
						}
						return insertWildcardType(wildcardType, prsrdWildcardType, signature, startPoint, deleteWildcardType());
					} else {
						cache[ip] = signature;
						cache2[ip] = wildcardType;
						cache3[ip] = prsrdWildcardType;
						cache4[ip] = startPoint;

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
			 *  To return WildcardType of cache.
			 */
			static WildcardType valueWildcardType(InterimWildcardType wildcard, String signature, Object startPoint) {
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
						if (signature.equals(cache[ind]) && areWCEqual(cache3[ind], cache4[ind], wildcard, startPoint)) {
							return cache2[ind];
						}
					}
				} catch (NullPointerException _) {
				}
				return null;
			}
		}
	
    /**
     * This method provides the comparing of two InterimWildcardType entities.
     * 
     * @param wildcard1 a InterimWildcardType entity.
     * @param startPoint1 a generic declaration which the seeking of any type variable definition used within wildcard1 should be started from.
     * @param wildcard2 another InterimWildcardType entity.
     * @param startPoint2 a generic declaration which the seeking of any type variable definition used within wildcard2 should be started from.
     */
	static boolean areWCEqual(InterimWildcardType wildcard1, Object startPoint1, InterimWildcardType wildcard2, Object startPoint2) {
		// Remember that the signatures for being compared InterimWildcardType-s are equal!
		
		// So, we need to compare only the rests:
		InterimType bounds1[] = wildcard1.bounds;
		InterimType bounds2[] = wildcard2.bounds;
		for (int i = 0; i < bounds1.length; i++) {
			if (bounds1[i] instanceof InterimTypeVariable && !ParameterizedTypeRepository.areTVEqual((InterimTypeVariable)bounds1[i], startPoint1, (InterimTypeVariable)bounds2[i], startPoint2)) {
				return false;
			} else if (bounds1[i] instanceof InterimParameterizedType && !ParameterizedTypeRepository.arePTEqual((InterimParameterizedType)bounds1[i], startPoint1, (InterimParameterizedType)bounds1[i], startPoint2)) {
				return false;
			}
		}
		return true;
	}
	
    /**
     * This method returns a registered type variable if it exists within the repository.
     * 
     * @param WildcardTypeName a name of a type variable.
     * @param startPoint an instance of the Class, Method, Constructor or Field type to start the search
     *        of a type variable declaration place.
     * @return the found type variable or null if it does not exist within repository.
     */
	public static WildcardType findWildcardType(InterimWildcardType wildcard, String signature, Object startPoint) {
		return WildcardTypeCache.valueWildcardType(wildcard, signature, startPoint);
	}
	
    /**
     * This method returns a registered type variable if it exists within the repository.
     * 
     * @param WildcardTypeName a name of a type variable.
     * @param startPoint an instance of the Class, Method, Constructor or Field type to start the search
     *        of a type variable declaration place.
     * @return the found type variable or null if it does not exist within repository.
     */
	public static String recoverWildcardSignature(InterimWildcardType wildcard) {
		String res = "";
		res = res+(wildcard.boundsType == true ? "+" : "-");
		if (wildcard.bounds[0] instanceof InterimParameterizedType) {
			res = res+((InterimParameterizedType)wildcard.bounds[0]).signature;
		} else if (wildcard.bounds[0] instanceof InterimTypeVariable) {
			res = res+((InterimTypeVariable) wildcard.bounds[0]).typeVariableName;
		} else { //ClassType
			res = res+((InterimClassType) wildcard.bounds[0]).classTypeName;
		}
		return res;
	}
	
    /**
     * This method registers a type variable within the repository.
     * 
     * @param WildcardType a type variable.
     * @param WildcardTypeName a name of a type variable.
     * @param startPoint an instance of the Class, Method, Constructor or Field type to start the search
     *        of a type variable declaration place.
     */
	public static void registerWildcardType(WildcardType wildcard, InterimWildcardType prsrdWildcardType, String signature, Object startPoint) {
		WildcardTypeCache.insertWildcardType(wildcard, prsrdWildcardType, signature, startPoint, -1);		
	}
}
