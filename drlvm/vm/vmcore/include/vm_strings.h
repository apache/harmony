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
// This describes the VM internal interface for manipulating strings

#ifndef _VM_STRINGS_H_
#define _VM_STRINGS_H_

#include "open/types.h"
#include "object_handles.h"
#include "String_Pool.h"

/*
 * Exported functons.
 */

VMEXPORT // temporary solution for interpreter unplug
Java_java_lang_String *vm_instantiate_cp_string_resolved(String*);


/*
 * VM functons.
 */
unsigned get_utf8_length_of_unicode(const uint16 *str, unsigned unicode_length);
int get_unicode_length_of_utf8(const char *utf8);
unsigned get_utf8_length_of_8bit(const U_8* chars, size_t length);
void pack_utf8(char *utf8_string, const uint16 *unicode, unsigned unicode_length);
void utf8_from_8bit(char* utf8_string, const U_8* chars, size_t length);
void unpack_utf8(uint16 *unicode, const char *utf8);

Java_java_lang_String *vm_instantiate_cp_string_slow(Class*, unsigned cp_index);

jstring String_to_interned_jstring(String* str);

//***** New Interface

// GC must be disabled, but at a GC safe point
ManagedObject* string_create_from_utf8(const char* buf, unsigned length);
ManagedObject* string_create_from_unicode(const uint16* buf, unsigned length);
ManagedObject* string_create_blank(unsigned length);  // Create a string of given length (in characters) with arbitrary characters

// GC must be disabled
// returns length in characters
unsigned string_get_length(ManagedObject*);

// GC must be disabled
// returns the length of the UTF8 encoding of the string
unsigned string_get_utf8_length(ManagedObject* str);

// GC must be disabled
// result is zero terminated 
// Caller should free the result
const uint16* string_get_unicode_chars(ManagedObject* string);

// GC must be disabled
// result is zero terminated
// Caller should free the result
const char* string_get_utf8_chars(ManagedObject* string);

// GC must be disabled
// Copy the characters offset..offset+count-1 into res
void string_get_unicode_region(ManagedObject* str, unsigned offset, unsigned count, uint16* res);

// GC must be disabled
// Encode characters offset..offset+count-1 into UTF8 and place in res
void string_get_utf8_region(ManagedObject* str, unsigned offset, unsigned count, char* res);

//*** Handle versions

ObjectHandle string_create_from_utf8_h(const char* buf, unsigned length);
ObjectHandle string_create_from_unicode_h(const uint16* buf, unsigned length);

// returns length in characters
unsigned string_get_length_h(ObjectHandle str);

// returns the length of the UTF8 encoding of the string
unsigned string_get_utf8_length_h(ObjectHandle str);

// Caller should free the result
const char* string_get_utf8_chars_h(ObjectHandle string);

// Copy the characters offset..offset+count-1 into buf
void string_get_unicode_region_h(ObjectHandle str, unsigned offset, unsigned count, uint16* buf);

// Encode characters offset..offset+count-1 into UTF8 and place in buf
void string_get_utf8_region_h(ObjectHandle str, unsigned offset, unsigned count, char* buf);

#endif //!_VM_STRINGS_H_
