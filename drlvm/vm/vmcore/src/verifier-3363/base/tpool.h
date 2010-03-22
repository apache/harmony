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
#ifndef __TPOOL_H_
#define __TPOOL_H_

#include "open/vm_class_manipulation.h"

#include "verifier.h"
#include "stackmap.h"
#include "memory.h"
#include "ver_utils.h"

/**
 * Verification type constraint structure.
 */
struct vf_TypeConstraint {
    const char *source;         // constraint source class name
    const char *target;         // constraint target class name
    vf_TypeConstraint *next;  // next constraint
};
typedef vf_TypeConstraint* vf_TypeConstraint_p;

//verifier's data stored in classloader
typedef struct {
    Memory    *pool;        // constraint memory pool
    vf_Hash *hash;        // constraint hash table
    vf_Hash *string;      // string pool hash table
} vf_ClassLoaderData_t;


struct vf_ValidType {
    Class_Handle cls;      //class handler
    const char*  name;     //name of the class
};

#define CLASS_NOT_LOADED ((Class_Handle)-1)

class SharedClasswideData;

/**
 * Utility class for dealing with Java types, converting object references to SmConstant,
 * parsing constantpool, etc.
 * TODO: remove constant pool parse and verification from the bytecode verifier.
 */
class vf_TypePool {

public:
    vf_TypePool(SharedClasswideData *_classwide, Class_Handle _klass, unsigned table_incr);

    ~vf_TypePool() {
        if( validTypes ) tc_free(validTypes);
    }

    SmConstant cpool_get_ldcarg(unsigned short cp_idx);
    SmConstant cpool_get_ldc2arg(unsigned short cp_idx);
    int cpool_is_reftype(unsigned short cp_idx);
    int cpool_get_class(unsigned short cp_idx, SmConstant *ref, int expected_dim = 0);

    int cpool_get_array(unsigned short cp_idx, SmConstant *ref);
    int cpool_get_field(unsigned short cp_idx, SmConstant *ref, SmConstant *value);
    int cpool_method_start(unsigned short cp_idx, const char **state, SmConstant *objectref,
        unsigned short *name_idx, int opcode);
    int cpool_method_get_rettype(const char **state, SmConstant *rettype, int *args_sz);
    int cpool_method_next_arg(const char **state, SmConstant *argument);
    int cpool_method_is_constructor_call(unsigned short name_idx);


    SmConstant get_type(const char *type_name, int name_len);
    SmConstant get_ref_type(const char *type_name, int name_len);
    SmConstant get_primitive_type(const char type_char);
    SmConstant get_ref_from_array(SmConstant element);


    SmConstant get_type(const char *type_name) {
        return get_type(type_name, (int)strlen(type_name) );
    }

    SmConstant get_ref_type(const char *type_name) {
        return get_ref_type(type_name, (int)strlen(type_name) );
    }

    int mustbe_assignable(SmConstant from, SmConstant to);
    int ref_mustbe_assignable(SmConstant from, SmConstant to);


    vf_ValidType *getVaildType(unsigned index) {
        assert(index && validTypes && index < validTypesTableSz);
        return validTypes + index;
    }

    Class_Handle sm_get_handler(SmConstant type) {
        unsigned index = type.getReferenceIdx();
        return getVaildType(index)->cls;
    }

    const char* sm_get_refname(SmConstant type) {
        unsigned index = type.getReferenceIdx();
        return getVaildType(index)->name;
    }

    //return SmConstant (known verification type) corresponding to 'type_name' and cache result in the 'cache'
    SmConstant sm_get_const_existing(const char* type_name, SmConstant* cache) {
        if( (*cache) == SM_NONE ) {
            //TODO: check asm code for strlen
            (*cache) = get_ref_type(type_name, (int)strlen(type_name));
        }
        return (*cache);
    }

    //return SmConstant (known verification type) corresponding to the super class of the class being verified
    //returned value is cached
    SmConstant sm_get_const_super() {
        const char* _super = class_get_name( class_get_super_class( k_class ));
        return get_ref_type(_super, (int)strlen(_super) );
    }

    //return SmConstant (known verification type) corresponding to the class being verified
    //returned value is cached
    SmConstant sm_get_const_this() {
        return sm_get_const_existing(class_get_name(k_class), &const_this);
    }

    //return SmConstant (known verification type) corresponding to java/lang/Object
    //returned value is cached
    SmConstant sm_get_const_object() {
        return sm_get_const_existing("java/lang/Object", &const_object);
    }

    //return SmConstant (known verification type) corresponding to java/lang/Class
    //returned value is cached
    SmConstant sm_get_const_class() {
        return sm_get_const_existing("java/lang/Class", &const_class);
    }

    //return SmConstant (known verification type) corresponding to java/lang/String
    //returned value is cached
    SmConstant sm_get_const_string() {
        return sm_get_const_existing("java/lang/String", &const_string);
    }

    //return SmConstant (known verification type) corresponding to java/lang/Throwable
    //returned value is cached
    SmConstant sm_get_const_throwable() {
        return sm_get_const_existing("java/lang/Throwable", &const_throwable);
    }

    //return SmConstant (known verification type) corresponding to U_8[]
    //returned value is cached
    SmConstant sm_get_const_arrayref_of_bb() {
        return sm_get_const_existing("[B", &const_arrayref_of_bb);
    }

    //return SmConstant (known verification type) corresponding to char[]
    //returned value is cached
    SmConstant sm_get_const_arrayref_of_char() {
        return sm_get_const_existing("[C", &const_arrayref_of_char);
    }

    //return SmConstant (known verification type) corresponding to double[]
    //returned value is cached
    SmConstant sm_get_const_arrayref_of_double() {
        return sm_get_const_existing("[D", &const_arrayref_of_double);
    }

    //return SmConstant (known verification type) corresponding to float[]
    //returned value is cached
    SmConstant sm_get_const_arrayref_of_float() {
        return sm_get_const_existing("[F", &const_arrayref_of_float);
    }

    //return SmConstant (known verification type) corresponding to int[]
    //returned value is cached
    SmConstant sm_get_const_arrayref_of_integer() {
        return sm_get_const_existing("[I", &const_arrayref_of_integer);
    }

    //return SmConstant (known verification type) corresponding to long[]
    //returned value is cached
    SmConstant sm_get_const_arrayref_of_long() {
        return sm_get_const_existing("[J", &const_arrayref_of_long);
    }

    //return SmConstant (known verification type) corresponding to short[]
    //returned value is cached
    SmConstant sm_get_const_arrayref_of_short() {
        return sm_get_const_existing("[S", &const_arrayref_of_short);
    }

    //return SmConstant (known verification type) corresponding to Object[]
    //returned value is cached
    SmConstant sm_get_const_arrayref_of_object() {
        return sm_get_const_existing("[Ljava/lang/Object;", &const_arrayref_of_object);
    }

    //return SmConstant represented array of specified type
    //the type is specified in the OP_NEWARRAY instruction. See the spec for possible types
    SmConstant sm_get_const_arrayref(U_8 see_spec) {
        switch ( see_spec ) {
    case 4: //T_BOOLEAN
    case 8: //T_BYTE
        return sm_get_const_arrayref_of_bb();
    case 5: //T_CHAR
        return sm_get_const_arrayref_of_char();
    case 6: //T_FLOAT
        return sm_get_const_arrayref_of_float();
    case 7: //T_DOUBLE
        return sm_get_const_arrayref_of_double();
    case 9: //T_SHORT
        return sm_get_const_arrayref_of_short();
    case 10: //T_INT
        return sm_get_const_arrayref_of_integer();
    case 11: //T_LONG
        return sm_get_const_arrayref_of_long();
        }
        assert(0);
        return SM_BOGUS;
    }

    //check if expected_ref is a super class of 'this', its package differs, and it's protected
    enum FieldAndMethodCheck {_FALSE, _CLONE, _TRUE};
    int checkFieldAccess(SmConstant expected_ref, unsigned short method_idx);
    int checkVirtualAccess(SmConstant expected_ref, unsigned short method_idx);
    int checkSuperAndPackage(SmConstant expected_ref);
private:
    //ref to the main class of the verifier
    SharedClasswideData *classwide;

    //class handler of the class being verified
    Class_Handle k_class;

    //constantpool length
    unsigned k_cp_length;

    //hash table storing class names
    vf_Hash hash;
    vf_ValidType *validTypes;
    unsigned tableIncr;
    unsigned validTypesTableMax;
    unsigned validTypesTableSz;

    /*****************/
    //cache for SmConstant constants;
    SmConstant const_object, const_class, const_string, const_throwable, const_arrayref_of_bb, 
        const_arrayref_of_char, const_arrayref_of_double, const_arrayref_of_float, 
        const_arrayref_of_integer, const_arrayref_of_long, const_arrayref_of_short,
        const_arrayref_of_object, const_this;


    void NewConstraint(const char *available, const char *required);

    //Get next free table entry index.
    //Reallocate table if out of free entries.
    unsigned check_table() {
        if( validTypesTableSz + 1 >= validTypesTableMax ) {
            validTypesTableMax += tableIncr;
            validTypes = (vf_ValidType*)tc_realloc(validTypes, sizeof(vf_ValidType) * validTypesTableMax);
        }
        return validTypesTableSz++;
    }

    int is_bool_array_conv_needed(const char *type_name, int length);

};

Class_Handle vf_resolve_class(Class_Handle k_class, const char* name, bool need_load);
int vf_is_extending(Class_Handle from, Class_Handle to);

#endif
