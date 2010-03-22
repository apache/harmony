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

import java.lang.reflect.ParameterizedType;

import org.apache.harmony.lang.reflect.parser.*;
import org.apache.harmony.lang.reflect.support.AuxiliaryFinder;

/**
 * @author Serguei S. Zapreyev
 */

/**
 * (This all should be considered as an experimental approach which could be changed on
 * java.lang.ref using)
 * 
 * ParameterizedTypeRepository provides the keeping iformation about a parameterized types.
 * 
 * For now the following approach for keeping is realized.
 * A concreet PT instance is included into repository if somebody do the *first* request for
 * the reflect's method like Class.getGenericSuperclass()->Class.ReflectionData.initGenericSuperclass()
 * (Field.getGenericType()->Field.FieldData.initGenericType()
 *  Method.getGenericReturnType()->Class.MethodData.initGenericReturnType()
 *  ...
 *  Constructor.getGenericParameterTypes()->Class.ConstructorData.initGenericParameterTypes()
 *  ...
 * ). If the identical PT is accessed anyhow then the pointer from repository is used. However,
 * further it can be removed from the repository if the cache is full under the new instance including.
 * Then if reflect's request for such PT using the earlier used interface methods then the pointers
 * to such instance kept within the Method/Constructor/Field/Class instances are returned just if it is
 * absent within repository. But if we use other Method/Constructor/Field/Class instances to access
 * to some instance of the same PT type which has been removed from repository then the new
 * exemplar of instance is included into repository and the pointer to it is replicated if it needs
 * into the new Method/Constructor/Field/Class instances.
 * 
 * Maybe, it would be more convinient to realize another repository implementation
 * where the accumulative approach will be used.
 * It might be realized using just java.lang.ref.
 * 
 * Note1. The inserted on 01.26.06 cache3 may significantly slow the algorithm but
 * it needs to provide the functional completeness of repository because we should distinguish
 * the entries with equals signatures if there are TVARs which are only homonym ones, i.e.
 * they are parameters of different generic declarations. 
 */
public final class ParameterizedTypeRepository {

	static final class ParameterizedTypeCache {
	
    /**
     * This class realizes the ParameterizedType repository which
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
			private static ParameterizedType cache2[] = new ParameterizedType[cacheLenght];
			private static InterimParameterizedType cache3[] = new InterimParameterizedType[cacheLenght];
			private static Object cache4[] = new Object[cacheLenght];
			private static int counter = 0;
			private static int worth[] = new int[cacheLenght];

			/**
			 *   To delete the worst entry.
			 */
			static int deleteParameterizedType() {
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
			static int insertParameterizedType(ParameterizedType parameterizedType, InterimParameterizedType prsrdParameterizedType, String signature, Object startPoint, int ip) {
				synchronized (TVSSynchro.class) {
					if (ip < 0) {
						for (int ind = 0; ind < cacheLenght ; ind++) {
							if (null == cache[ind]) {
								cache[ind] = signature;
								cache2[ind] = parameterizedType;
								cache3[ind] = prsrdParameterizedType;
								cache4[ind] = startPoint;
								worth[ind] = 1;
								return ind;
							}
						}
						return insertParameterizedType(parameterizedType, prsrdParameterizedType, signature, startPoint, deleteParameterizedType());
					} else {
						cache[ip] = signature;
						cache2[ip] = parameterizedType;
						cache3[ip] = prsrdParameterizedType;
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
			 *  To return ParameterizedType of cache.
			 */
			static ParameterizedType valueParameterizedType(InterimParameterizedType parameterizedType, String signature, Object startPoint) {
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
					try {
						for (int ind = 0; ind < cacheLenght ; ind++) {
							if (signature.equals(cache[ind]) && arePTEqual(cache3[ind], cache4[ind], parameterizedType, startPoint)) {
								return cache2[ind];
							}
						}
					} catch (NullPointerException _) {
					}
				}
				return null;
			}
	}
	
    /**
     * This method provides the comparing of two InterimParameterizedType entities.
     * 
     * @param parameterizedType1 a InterimParameterizedType entity.
     * @param startPoint1 a generic declaration which the seeking of any type variable definition used within parameterizedType1 should be started from.
     * @param parameterizedType2 another InterimParameterizedType entity.
     * @param startPoint2 a generic declaration which the seeking of any type variable definition used within parameterizedType2 should be started from.
     */
	static boolean arePTEqual(InterimParameterizedType parameterizedType1, Object startPoint1, InterimParameterizedType parameterizedType2, Object startPoint2) {
		// Remember that the signatures for being compared InterimParameterizedType-s are equal!
		
		// So, we need to compare only the rests:
		InterimType params1[] = parameterizedType1.parameters;
		InterimType params2[] = parameterizedType2.parameters;
		for (int i = 0; i < params1.length; i++) {
			if (params1[i] instanceof InterimTypeVariable && !areTVEqual((InterimTypeVariable)params1[i], startPoint1, (InterimTypeVariable)params2[i], startPoint2)) {
				return false;
			}
			if (params1[i] instanceof InterimParameterizedType && !arePTEqual((InterimParameterizedType)params1[i], startPoint1, (InterimParameterizedType)params2[i], startPoint2)) {
				return false;
			}
			if (params1[i] instanceof InterimWildcardType && !WildcardTypeRepository.areWCEqual((InterimWildcardType)params1[i], startPoint1, (InterimWildcardType)params2[i], startPoint2)) {
				return false;
			}
		}
		if (parameterizedType1.ownerType != null && parameterizedType1.ownerType instanceof InterimParameterizedType) {
			return arePTEqual((InterimParameterizedType)parameterizedType1.ownerType, startPoint1, (InterimParameterizedType)parameterizedType2.ownerType, startPoint2);
		}
		return true;
	}
	
    /**
     * This method provides the comparing of two InterimTypeVariable entities.
     * 
     * @param typeVariable1 a InterimTypeVariable entity.
     * @param startPoint1 a generic declaration which the seeking of typeVariable1 type variable definition should be started from.
     * @param typeVariable2 another InterimTypeVariable entity.
     * @param startPoint2 a generic declaration which the seeking of typeVariable2 type variable definition should be started from.
     */
	static boolean areTVEqual(InterimTypeVariable typeVariable1, Object startPoint1, InterimTypeVariable typeVariable2, Object startPoint2) {
		if (AuxiliaryFinder.findTypeVariable(typeVariable1.typeVariableName, startPoint1) == AuxiliaryFinder.findTypeVariable(typeVariable2.typeVariableName, startPoint2)) {
			return true;
		}
		return false;
	}
	
    /**
     * This method returns a registered type variable if it exists within the repository.
     * 
     * @param ParameterizedTypeName a parameterized type.
     * @param startPoint an instance of the Class, Method, Constructor or Field type to start the search
     *        of a type variable declaration place.
     * @return the found type variable or null if it does not exist within repository.
     */
	public static ParameterizedType findParameterizedType(InterimParameterizedType parameterizedType, Object startPoint) {
		return ParameterizedTypeCache.valueParameterizedType(parameterizedType, parameterizedType.signature, startPoint);
	}
	
    /**
     * This method returns a registered type variable if it exists within the repository.
     * 
     * @param ParameterizedTypeName a parameterized type.
     * @param signature a signature of a parameterized type.
     * @param startPoint an instance of the Class, Method, Constructor or Field type to start the search
     *        of a type variable declaration place.
     * @return the found type variable or null if it does not exist within repository.
     */
	public static ParameterizedType findParameterizedType(InterimParameterizedType parameterizedType, String signature, Object startPoint) {
		return ParameterizedTypeCache.valueParameterizedType(parameterizedType, signature, startPoint);
	}
	
    /**
     * This method registers a parameterized type within the repository.
     * 
     * @param ParameterizedType a type variable.
     * @param ParameterizedTypeName a name of a type variable.
     * @param startPoint an instance of the Class, Method, Constructor or Field type to start the search
     *        of a type variable declaration place.
     */
	public static void registerParameterizedType(ParameterizedType parameterizedType, InterimParameterizedType prsrdParameterizedType, String signature, Object startPoint) {
		ParameterizedTypeCache.insertParameterizedType(parameterizedType, prsrdParameterizedType, signature, startPoint, -1);		
	}
}
