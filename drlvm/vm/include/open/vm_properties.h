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
#ifndef __PROPERTIES_H__
#define __PROPERTIES_H__

#include <stddef.h>
#include "open/common.h"
#include "open/platform_types.h"


#ifdef __cplusplus
extern "C" {
#endif

typedef enum { VM_PROPERTIES  = 0, JAVA_PROPERTIES = 1 } PropertyTable;

/**
 * Sets the property for <code>table_number</code> property table. <code>NULL</code> value is supported.
 */
DECLARE_OPEN(void, vm_properties_set_value, (const char* key, const char* value, PropertyTable table_number));

/**
 * @return The value of the property from <code>table_number</code> property table if it
 *         has been set by <code>vm_properties_set_value</code> function. Otherwise <code>NULL</code>.
 */
DECLARE_OPEN(char*, vm_properties_get_value, (const char* key, PropertyTable table_number));

/**
 * Safety frees memory of value returned by <code>vm_properties_get_value</code> function.
 */ 
DECLARE_OPEN(void, vm_properties_destroy_value, (char* value));

/**
 * Checks if the property is set. 
 */
DECLARE_OPEN(BOOLEAN, vm_property_is_set, (const char* key, PropertyTable table_number));

/**
 * @return An array of keys from <code>table_number</code> properties table.
 */ 
DECLARE_OPEN(char**, vm_properties_get_keys, (PropertyTable table_number));

/**
 * @return An array of keys which start with specified prefix from 
 *         <code>table_number</code> properties table.
 */ 
DECLARE_OPEN(char**, vm_properties_get_keys_starting_with, (const char* prefix, PropertyTable table_number));

/**
 * Safety frees array of keys memory which returned by <code>vm_properties_get_keys</code>
 * or <code>vm_properties_get_keys_starting_with</code> functions.
 */
DECLARE_OPEN(void, vm_properties_destroy_keys, (char** keys));

/**
 * Tries to interpret property value as <code>Boolean</code> and returns it. 
 * In case of failure returns <code>default_value</code>.
 */
DECLARE_OPEN(BOOLEAN, vm_property_get_boolean, (const char* property, BOOLEAN default_value, PropertyTable table_number));

/**
 * Tries to interpret property value as <code>int</code> and returns it. In case of failure 
 * returns <code>default_value</code>.
 */
DECLARE_OPEN(int, vm_property_get_integer, (const char *property_name, int default_value, PropertyTable table_number));

/**
 * Tries to interpret property value as <code>int</code> and returns it.
 * In case of failure returns <code>default_value</code>.
 * Numbers can include 'm' or 'M' for megabytes, 'k' or 'K' for kilobytes, and 'g' or 'G' for gigabytes
 * (for example, 32k is the same as 32768).
 */
DECLARE_OPEN(size_t, vm_property_get_size, (const char *property_name, size_t default_value, PropertyTable table_number));

#ifdef __cplusplus
}
#endif

#endif
