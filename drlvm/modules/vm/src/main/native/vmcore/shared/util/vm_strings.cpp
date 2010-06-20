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
#define LOG_DOMAIN "vm.accessors"

#include <apr_atomic.h>

#include "cxxlog.h"

#include "vm_strings.h"
#include "environment.h"
#include "vm_stats.h"
#include "exceptions.h"
#include "vm_arrays.h"
#include "port_threadunsafe.h"

/////////////////////////////////////////////////////////////
// begin utf8 support

//
// See JVM Spec, Section 4.4.7
//

// return length of UTF-8 encoded string, or negative value in case of error.
int get_unicode_length_of_utf8(const char *utf8)
{
    int len = 0;
    U_8 ch;
    U_8 ch2;
    U_8 ch3;
    while((ch = *utf8++)) {
        len++;
        if(ch & 0x80) { // 2 or 3 byte encoding
            if (! (ch & 0x40))
                return -1;
            ch2 = *utf8++;
            if(ch & 0x20) { // 3 byte encoding
                ch3 = *utf8++;
                if ((ch  & 0xf0) != 0xe0  ||  // check first byte high bits
                    (ch2 & 0xc0) != 0x80  ||  // check second byte high bits
                    (ch3 & 0xc0) != 0x80)     // check third byte high bits
                    return -1;
            } else {    // 2 byte encoding
                if ((ch2 & 0xc0) != 0x80)     // check second byte high bits
                    return -1;
            }
        } 
    }
    return len;
} //get_unicode_length_of_utf8

unsigned get_utf8_length_of_unicode(const uint16 *unicode, unsigned unicode_length)
{
    unsigned length = 0;
    for(unsigned i = 0; i < unicode_length; i++) {
        uint16 ch = unicode[i];
        if(ch == 0) {
            length += 2;
        } else if(ch < 0x80) {
            length += 1;
        } else if(ch < 0x800) {
            length += 2;
        } else {
            length += 3;
        }
    }
    return length;
} //get_utf8_length_of_unicode

unsigned get_utf8_length_of_8bit(const U_8* chars, size_t length)
{
    unsigned len = 0;
    for(unsigned i=0; i < length; i++)
        if (chars[i]!=0 && chars[i]<0x80)
            len++;
        else
            len += 2;
    return len;
}

void pack_utf8(char *utf8_string, const uint16 *unicode, unsigned unicode_length)
{
    char *s = utf8_string;
    for(unsigned i = 0; i < unicode_length; i++) {
        unsigned ch = unicode[i];
        if(ch == 0) {
            *s++ = (char)0xc0;
            *s++ = (char)0x80;
        } else if(ch < 0x80) {
            *s++ = (char)ch;
        } else if(ch < 0x800) {
            unsigned b5_0 = ch & 0x3f;
            unsigned b10_6 = (ch >> 6) & 0x1f;
            *s++ = (char)(0xc0 | b10_6);
            *s++ = (char)(0x80 | b5_0);
        } else {
            unsigned b5_0 = ch & 0x3f;
            unsigned b11_6 = (ch >> 6) & 0x3f;
            unsigned b15_12 = (ch >> 12) & 0xf;
            *s++ = (char)(0xe0 | b15_12);
            *s++ = (char)(0x80 | b11_6);
            *s++ = (char)(0x80 | b5_0);
        }
    }
    *s = 0;
} //pack_utf8

void utf8_from_8bit(char* utf8_string, const U_8* chars, size_t length)
{
    char* s = utf8_string;
    for(unsigned i=0; i<length; i++) {
        unsigned ch = chars[i];
        if (ch==0) {
            *s++ = (char)0xc0;
            *s++ = (char)0x80;
        } else if(ch < 0x80) {
            *s++ = (char)ch;
        } else {
            unsigned b5_0 = ch & 0x3f;
            unsigned b10_6 = (ch >> 6) & 0x1f;
            *s++ = (char)(0xc0 | b10_6);
            *s++ = (char)(0x80 | b5_0);
        }
    }
    *s = '\0';
}

void unpack_utf8(uint16 *unicode, const char *utf8_string)
{
    const U_8 *utf8 = (const U_8 *)utf8_string;
    unsigned len = 0;
    uint16 ch;
    while((ch = (uint16)*utf8++)) {
        len++;
        if(ch & 0x80) {
            assert(ch & 0x40);
            if(ch & 0x20) {
                uint16 x = ch;
                uint16 y = (uint16)*utf8++;
                uint16 z = (uint16)*utf8++;
                *unicode++ = (uint16)(((0x0f & x) << 12) + ((0x3f & y) << 6) + ((0x3f & z)));
            } else {
                uint16 x = ch;
                uint16 y = (uint16)*utf8++;
                *unicode++ = (uint16)(((0x1f & x) << 6) + (0x3f & y));
            }
        } else {
            *unicode++ = ch;
        }
    }
} //unpack_utf8

// end utf8 support
/////////////////////////////////////////////////////////////

/////////////////////////////////////////////////////////////
// begin strings

// The actual characters of a string might be stored as an array of 16-bit characters or compressed
// as an array of 8-bit characters.  This structure combines this information into one structure.
// If is_compressed then the characters are stored as 8-bit and compressed points to the array,
// otherwise the characters are stored as 16-bit and unicode points to the array.
struct StringBuffer {
    uint16* unicode;
    U_8* compressed;
    bool is_compressed;
};

static void string_get_buffer(ManagedObject* str, StringBuffer* buf);

/////////////////////////////////////////////////////////////
// String creation

// Offset of String fields in ManagedObject.
static unsigned f_count_offset, f_offset_offset, f_value_char_offset, f_value_byte_offset;

static void init_fields() {
    Global_Env *global_env = VM_Global_State::loader_env;
    Class* clss = global_env->JavaLangString_Class;
    Field *f_count = class_lookup_field_recursive(clss, "count", "I");
    Field *f_offset = class_lookup_field_recursive(clss, "offset", "I");
    Field *f_value_byte = class_lookup_field_recursive(clss, "bvalue", "[B");
    Field *f_value_char = class_lookup_field_recursive(clss, "value", "[C");

    assert(f_count);
    assert(f_offset);

    f_count_offset = f_count->get_offset();
    f_offset_offset = f_offset->get_offset();
    f_value_char_offset = f_value_char != 0 ? f_value_char->get_offset(): 0;
    f_value_byte_offset = f_value_byte != 0 ? f_value_byte->get_offset(): 0;
}

static void string_set_fields_separate(ManagedObject* str, unsigned length, unsigned offset, Vector_Handle chars, bool is_byte_array)
{
    if (f_count_offset == 0) {
        init_fields();
    }
    unsigned f_value_offset;
    if (is_byte_array)
        f_value_offset = f_value_byte_offset;
    else
        f_value_offset = f_value_char_offset;

    assert(f_value_offset);

    U_8* str_raw = (U_8*)str;
    *(U_32*)(str_raw+f_count_offset) = length;
    *(U_32*)(str_raw+f_offset_offset) = offset;
    STORE_REFERENCE(str, str_raw+f_value_offset, chars);
}

// GC must be disabled but at a same point
// Create a string with unicode_length characters
// If eight_bit then characters can be compressed to 8 bits
// Return: str gets the string object, buf points to buffer
static void string_create(unsigned unicode_length, bool eight_bit, ManagedObject** str, StringBuffer* buf)
{
    ASSERT_RAISE_AREA;
    assert(!hythread_is_suspend_enabled());

    Global_Env *global_env = VM_Global_State::loader_env;
    Class *clss;
    clss = global_env->ArrayOfChar_Class;
    if (eight_bit)
        clss = global_env->ArrayOfByte_Class;
    assert(clss);

    unsigned sz = clss->calculate_array_size(unicode_length);
    if (sz == 0) {
        // string too long
        *str = NULL;
        exn_raise_object(VM_Global_State::loader_env->java_lang_OutOfMemoryError);
        return;
    }

    Vector_Handle array = vm_alloc_and_report_ti(sz, clss->get_allocation_handle(),
        vm_get_gc_thread_local(), clss);
    if(!array) { // OutOfMemory should be thrown
        *str = NULL;
        exn_raise_object(VM_Global_State::loader_env->java_lang_OutOfMemoryError);
        return;
    }
#ifdef VM_STATS
    clss->instance_allocated(sz);
#endif //VM_STATS
    set_vector_length(array, unicode_length);

    VTable *jls_vtable = VM_Global_State::loader_env->JavaLangString_VTable;

    assert(!hythread_is_suspend_enabled());
    GcFrame gc;
    gc.add_object((ManagedObject**)&array);

    ManagedObject* jls = (ManagedObject*)class_alloc_new_object_using_vtable(jls_vtable);
    if (!jls) { // OutOfMemory is thrown
        *str = NULL;
        return;
    }
    gc.add_object((ManagedObject**)&jls);

    string_set_fields_separate(jls, unicode_length, 0, array, eight_bit);

    *str = jls;
    buf->is_compressed = eight_bit;
    if (eight_bit)
        buf->compressed = (U_8*)get_vector_element_address_int8(array, 0);
    else
        buf->unicode = get_vector_element_address_uint16(array, 0);
}

// return String ManagedObject representing string provided in UTF-8 encoding,
// or NULL in case of error.
// GC must be disabled, but at a GC safe point
ManagedObject* string_create_from_utf8(const char* buf, unsigned length)
{
    ASSERT_RAISE_AREA;
    assert(buf && buf[length]=='\0');
    int unicode_length = get_unicode_length_of_utf8(buf);
    if (unicode_length < 0) // data error
        return NULL;

    ManagedObject* str;
    StringBuffer buf2;
    string_create((unsigned) unicode_length, false, &str, &buf2);
    if (!str) { // if OutOfMemory
        return NULL;
    }
    if (buf2.is_compressed)
        memcpy(buf2.compressed, buf, unicode_length);
    else
        unpack_utf8(buf2.unicode, buf);
    return str;
}

static bool is_compressible_jchar_array(const uint16* unicodeChars, unsigned length)
{
    for(unsigned i=0; i<length; i++)
        if (unicodeChars[i] > 0xff)
            return false;
    return true;
}

// GC must be disabled, but at a GC safe point
ManagedObject* string_create_from_unicode(const uint16* buf, unsigned length)
{
    ASSERT_RAISE_AREA;
    Global_Env *global_env = VM_Global_State::loader_env;
    bool compress = global_env->strings_are_compressed && is_compressible_jchar_array(buf, length);
    ManagedObject* str;
    StringBuffer buf2;
    string_create(length, compress, &str, &buf2);
    if (!str) { // if OutOfMemory
        return NULL;
    }
    if (buf2.is_compressed) {
        for(unsigned i=0; i<length; i++)
            buf2.compressed[i] = (U_8)buf[i];
    } else {
        memcpy(buf2.unicode, buf, sizeof(uint16) * length);
    }
    return str;
}

ObjectHandle string_create_from_utf8_h(const char* buf, unsigned length)
{
    ASSERT_RAISE_AREA;
    assert(hythread_is_suspend_enabled());
    tmn_suspend_disable();
    ObjectHandle res = oh_allocate_local_handle();
    res->object = string_create_from_utf8(buf, length);
    tmn_suspend_enable();
    return res->object ? res : NULL;
}

ObjectHandle string_create_from_unicode_h(const uint16* buf, unsigned length)
{
    ASSERT_RAISE_AREA;
    assert(hythread_is_suspend_enabled());
    tmn_suspend_disable();
    ObjectHandle res = oh_allocate_local_handle();
    res->object = string_create_from_unicode(buf, length);
    tmn_suspend_enable();
    return res;
}

///////////////////////////////////////////////////////////////////
// Getting Length

// GC must be disabled
// returns length in characters
unsigned string_get_length(ManagedObject* str)
{
    assert(!hythread_is_suspend_enabled());
    assert(str);

    if (f_count_offset == 0) init_fields();
    U_8* str_raw = (U_8*)str;
    return *(U_32*)(str_raw+f_count_offset);
}

// returns length in characters
unsigned string_get_length_h(ObjectHandle str)
{
    assert(hythread_is_suspend_enabled());
    tmn_suspend_disable();
    assert(str && str->object);
    unsigned len = string_get_length(str->object);
    tmn_suspend_enable();
    return len;
}

// GC must be disabled
// returns the length of the UTF8 encoding of the string
unsigned string_get_utf8_length(ManagedObject* str)
{
    StringBuffer buf;
    unsigned len = string_get_length(str);
    string_get_buffer(str, &buf);
    if (buf.is_compressed)
        return get_utf8_length_of_8bit(buf.compressed, len);
    else
        return get_utf8_length_of_unicode(buf.unicode, len);
}

// returns the length of the UTF8 encoding of the string
unsigned string_get_utf8_length_h(ObjectHandle str)
{
    assert(hythread_is_suspend_enabled());
    tmn_suspend_disable();
    assert(str && str->object);
    unsigned utf8_len = string_get_utf8_length(str->object);
    tmn_suspend_enable();
    return utf8_len;
}

///////////////////////////////////////////////////////////////////
// Getting Characters

static void string_get_buffer(ManagedObject* str, StringBuffer* buf)
{
    if (f_value_char_offset == 0) init_fields();
    assert(f_value_char_offset);

    U_8* str_raw = (U_8*)str;
    unsigned offset = *(U_32*)(str_raw + f_offset_offset);
    Vector_Handle char_array = get_raw_reference_pointer((ManagedObject**)(str_raw+f_value_char_offset));
    if (char_array) {
        buf->is_compressed = false;
        buf->unicode = get_vector_element_address_uint16(char_array, offset);
    } else {
        buf->is_compressed = true;
        assert(f_value_byte_offset);
        buf->compressed = (U_8*)get_vector_element_address_int8(*(Vector_Handle*)(str_raw+f_value_byte_offset), offset);
    }
}

// GC must be disabled
// result is zero terminated
// Caller should free the result
const uint16* string_get_unicode_chars(ManagedObject* string)
{
    assert(string);
    U_32 unicode_size = string_get_length(string);
    StringBuffer buf;
    string_get_buffer(string, &buf);
    uint16* unicode_chars = (uint16*)STD_MALLOC(sizeof(uint16)*(unicode_size+1));
    if (NULL == unicode_chars)
        return NULL;

    if (buf.is_compressed) {
        for(unsigned i=0; i<unicode_size; i++)
            unicode_chars[i] = buf.compressed[i];
    } else {
        memcpy(unicode_chars, buf.unicode, sizeof(uint16)*unicode_size);
    }
    unicode_chars[unicode_size] = 0;
    return unicode_chars;
}

// GC must be disabled
// result is zero terminated
// Caller should free the result
const char* string_get_utf8_chars(ManagedObject* string)
{
    assert(string);
    U_32 unicode_size = string_get_length(string);
    StringBuffer buf;
    string_get_buffer(string, &buf);
    char* utf_chars;
    size_t sz;
    if (buf.is_compressed) {
        unsigned utf_size = get_utf8_length_of_8bit(buf.compressed, unicode_size);
        sz = utf_size+1;
        utf_chars = (char*)STD_MALLOC(sz);
        assert(utf_chars);
        utf8_from_8bit(utf_chars, buf.compressed, unicode_size);
    } else {
        unsigned utf_size = get_utf8_length_of_unicode(buf.unicode, unicode_size);
        sz = utf_size+1;
        utf_chars = (char*)STD_MALLOC(sz);
        assert(utf_chars);
        pack_utf8(utf_chars, buf.unicode, unicode_size);
    }
    assert(strlen(utf_chars) < sz);
    return utf_chars;
} //string_get_utf8_chars

// Caller should free the result
const char* string_get_utf8_chars_h(ObjectHandle string)
{
    assert(hythread_is_suspend_enabled());
    tmn_suspend_disable();
    assert(string && string->object);
    const char* res = string_get_utf8_chars(string->object);
    tmn_suspend_enable();
    return res;
}

// GC must be disabled
// Copy the characters offset..offset+count-1 into res
void string_get_unicode_region(ManagedObject* str, unsigned offset, unsigned count, uint16* res)
{
    StringBuffer buf;
    string_get_buffer(str, &buf);
    if (buf.is_compressed)
        for(unsigned i=0; i<count; i++)
            res[i] = buf.compressed[i+offset];
    else
        memcpy(res, buf.unicode+offset, count*sizeof(uint16));
}

// Copy the characters offset..offset+count-1 into buf
void string_get_unicode_region_h(ObjectHandle str, unsigned offset, unsigned count, uint16* buf)
{
    assert(hythread_is_suspend_enabled());
    tmn_suspend_disable();
    assert(str && str->object && offset+count<=string_get_length(str->object));
    string_get_unicode_region(str->object, offset, count, buf);
    tmn_suspend_enable();
}

// GC must be disabled
// Encode characters offset..offset+count-1 into UTF8 and place in res
void string_get_utf8_region(ManagedObject* str, unsigned offset, unsigned count, char* res)
{
    StringBuffer buf;
    string_get_buffer(str, &buf);
    if (buf.is_compressed)
        utf8_from_8bit(res, buf.compressed+offset, count);
    else
        pack_utf8(res, buf.unicode+offset, count);
}

// Encode characters offset..offset+count-1 into UTF8 and place in buf
void string_get_utf8_region_h(ObjectHandle str, unsigned offset, unsigned count, char* buf)
{
    assert(hythread_is_suspend_enabled());
    tmn_suspend_disable();
    assert(str && str->object && offset+count<=string_get_length(str->object));
    string_get_utf8_region(str->object, offset, count, buf);
    tmn_suspend_enable();
}

///////////////////////////////////////////////////////////////////
// Old interface


// Given a String, creates its interned Java_java_lang_string from its byte array. GC must be disabled
VMEXPORT // temporary solution for interpreter unplug
Java_java_lang_String *vm_instantiate_cp_string_resolved(String *str)
{
    ASSERT_RAISE_AREA;
    assert(!hythread_is_suspend_enabled());

    REFS_RUNTIME_SWITCH_IF
#ifdef REFS_RUNTIME_OR_COMPRESSED
        if (str->intern.compressed_ref != 0) {
            return uncompress_compressed_reference(str->intern.compressed_ref);
        }
#endif // REFS_RUNTIME_OR_COMPRESSED
    REFS_RUNTIME_SWITCH_ELSE
#ifdef REFS_RUNTIME_OR_UNCOMPRESSED
        if (str->intern.raw_ref != NULL) {
            return str->intern.raw_ref;
        }
#endif // REFS_RUNTIME_OR_UNCOMPRESSED
    REFS_RUNTIME_SWITCH_ENDIF
    return VM_Global_State::loader_env->string_pool.intern(str);
} //vm_instantiate_cp_string_resolved

// Interning of strings

jstring String_to_interned_jstring(String* str)
{
    ASSERT_RAISE_AREA;
    tmn_suspend_disable();
    Java_java_lang_String *jstr = vm_instantiate_cp_string_resolved(str);

    if (jstr == NULL) {
        tmn_suspend_enable();
        assert(exn_raised());
        return NULL;
    }
    ObjectHandle hstr = oh_allocate_local_handle();
    hstr->object = jstr;
    tmn_suspend_enable();

    return (jstring)hstr;
}


Java_java_lang_String*
vm_instantiate_cp_string_slow(Class* c, unsigned cp_index)
{
    ASSERT_THROW_AREA;
#ifdef VM_STATS
    UNSAFE_REGION_START
    VM_Statistics::get_vm_stats().num_instantiate_cp_string_slow++;
    UNSAFE_REGION_END
#endif

    Java_java_lang_String* result;
    ConstantPool& cp = c->get_constant_pool();
    String* str = cp.get_string(cp_index);

    BEGIN_RAISE_AREA;
    result = vm_instantiate_cp_string_resolved(str);
    END_RAISE_AREA;
    exn_rethrow_if_pending();

    return result;
} //vm_instantiate_cp_string_slow

// end strings
/////////////////////////////////////////////////////////////

