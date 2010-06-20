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
#include <assert.h>
#include <stdio.h>

#include "open/vm_field_access.h"
#include "open/vm_method_access.h"
#include "class_interface.h"

#include "tpool.h"
#include "context_base.h"

vf_TypePool::vf_TypePool(SharedClasswideData *_classwide, Class_Handle _klass, unsigned table_incr)
    : classwide(_classwide), tableIncr(table_incr),
    validTypesTableMax(0), validTypesTableSz(1), validTypes(0)
{
    const_object = const_class = const_string = const_throwable = const_arrayref_of_bb =
        const_arrayref_of_char = const_arrayref_of_double = const_arrayref_of_float =
        const_arrayref_of_integer = const_arrayref_of_long = const_arrayref_of_short = 
        const_arrayref_of_object = const_this = SM_NONE;

    k_class = _klass;
    k_cp_length = class_cp_get_size( k_class );
}

/*
* Returns name length if it is array of boolean or 0
*/
inline int vf_TypePool::is_bool_array_conv_needed(const char *type_name, int length) {
    return length > 1 && type_name[length - 1] == 'Z' && type_name[length - 2] == '[';
}

/*
* Get SmConstant by type name.
*/
SmConstant vf_TypePool::get_ref_type(const char *type_name, int length) {
    //TODO: this assert raise false alarms when type name starts with 'L'
    //assert(type_name[0] != 'L');

    // find type in hash
    vf_HashEntry_t *entry = hash.NewHashEntry( type_name, length );
    unsigned index = entry->data_index;
    if( !index ) {
        //convert array of booleans to array of bytes
        if( is_bool_array_conv_needed(type_name, length) ) {
            char *new_name = (char*)classwide->mem.malloc(length+1);
            tc_memcpy(new_name, type_name, length);
            new_name[length-1] = 'B';
            index = get_ref_type(new_name, length).getReferenceIdx();
            classwide->mem.dealloc_last(new_name, length+1);
        }
        // Get next free table entry index
        if( !index ) {
            index = check_table();
            validTypes[index].cls = 0;
            validTypes[index].name = entry->key;
        }
        entry->data_index = index;
    }

    assert(index < validTypesTableSz);
    return SmConstant::getReference(index);
}

SmConstant vf_TypePool::get_primitive_type(const char type_char) {
    switch( type_char ) {
    case 'I':
    case 'B':
    case 'Z':
    case 'C':
    case 'S':
        return SM_INTEGER;
    case 'J':
        return SM_LONG;
    case 'F':
        return SM_FLOAT;
    case 'V':
        return SM_BOGUS;
    case 'D':
        return SM_DOUBLE;
    default:
        assert(0);
        return SM_BOGUS;
    }
}

SmConstant vf_TypePool::get_type(const char *type_name, int name_len) {
    if( name_len > 1 ) {
        if( type_name[0] == '[' ) {
            return get_ref_type(type_name, name_len);
        }

        if( type_name[0] == 'L' ) {
            return get_ref_type(type_name + 1, name_len - 2);
        }

        return SM_BOGUS;
    } else {
        return get_primitive_type(type_name[0]);
    }
}


SmConstant vf_TypePool::get_ref_from_array(SmConstant element) {
    if( element == SM_NULL ) return SM_NULL;

    assert(element.isReference());
    assert(sm_get_refname(element)[0] == '[');
    return get_type(sm_get_refname(element) + 1);
}


int vf_TypePool::mustbe_assignable(SmConstant from, SmConstant to) {
    if( from == to || to == SM_NONE || to == SM_BOGUS ) return true;
    if( from == SM_BOGUS ) return false;

    if( to.isReference() ) {
        if( from == SM_NULL ) return true;

        if( from.isReference() ) {
            return ref_mustbe_assignable(from, to);
        }

        return false;
    }

    if( !to.isPrimitive() ) {
        assert( to != from ); // checked above
        return false;
    }

    //migth have to change switch below if merging is done with constants
    assert( from != SM_NONE );
    assert( from != SM_LOW_WORD );
    assert( from != SM_REF_OR_UNINIT_OR_RETADR );
    assert( from != SM_REF_OR_UNINIT );
    assert( from != SM_ANYARRAY );
    assert( from != SM_BOGUS );

    switch ( to.c ) {
    case SM_LOW_WORD:
        return from != SM_HIGH_WORD;

    case SM_REF_OR_UNINIT_OR_RETADR:
        return from == SM_NULL || from == SM_THISUNINIT || !from.isPrimitive();

    case SM_REF_OR_UNINIT:
        return from == SM_NULL || from == SM_THISUNINIT || from.isNewObject() || from.isReference();

    case SM_ANYARRAY:
        return from == SM_NULL || from.isReference() && sm_get_refname(from)[0] == '[';

    case SM_NULL:
        assert(0);
        return false;

    case SM_THISUNINIT:
    case SM_HIGH_WORD:
    case SM_INTEGER:
    case SM_FLOAT:
    case SM_LONG:
    case SM_DOUBLE:
    case SM_BOGUS:
        return false;
    default:
        assert(0);
        return false;
    }
}

int vf_TypePool::ref_mustbe_assignable(SmConstant from, SmConstant to) {
    if( to == sm_get_const_object() ) return true;

    vf_ValidType *to_type = getVaildType(to.getReferenceIdx());
    vf_ValidType *from_type = getVaildType(from.getReferenceIdx());

    const char *to_name = to_type->name;
    const char *from_name = from_type->name;

    int to_array = to_name[0] == '[';
    int from_array = from_name[0] == '[';

    if( to_array && !from_array ) {
        return false;
    } else if( to_array && from_array ) {
        int dim = 0;
        while( to_name[dim] == '[' && from_name[dim] == '[' ) dim++;

        if( from_name[dim] != 'L' && from_name[dim] != '[' ) {
            //primitive type
            dim--;
        }

        if( to_name[dim] != 'L' ) return false;

        if( from_name[dim] == 'L' ) {
            //merge refs
            return ref_mustbe_assignable(get_type(from_name + dim), get_type(to_name + dim) );
        } else {
            //to must be Object or an interface
            return ref_mustbe_assignable(sm_get_const_object(), get_type(to_name + dim) );
        }
    } else if( from_array ) {
        //from is an array, to is not an array

        //must be checked before
        assert( from != to );

        return to == sm_get_const_object() || !strcmp(to_name, "java/lang/Cloneable") || 
                !strcmp(to_name, "java/io/Serializable");
    } else {
        //both not arrays

        //check whether TO class is loaded
        if( !to_type->cls ) {
            to_type->cls = vf_resolve_class(k_class, to_type->name, false);
            if( !to_type->cls ) to_type->cls = CLASS_NOT_LOADED;
        }

        if( to_type->cls && to_type->cls != CLASS_NOT_LOADED ) {
            //if to is loaded and it is an interface, treat it as an object
            if(class_is_interface(to_type->cls)) {
                return true;
            }
        } else {
            NewConstraint(from_type->name, to_type->name);
            return true;
        }

        //check whether FROM class is loaded
        if( !from_type->cls ) {
            from_type->cls = vf_resolve_class(k_class, from_type->name, false);
            if( !from_type->cls ) from_type->cls = CLASS_NOT_LOADED;
        }

        if( from_type->cls && from_type->cls != CLASS_NOT_LOADED ) {
            return vf_is_extending(from_type->cls, to_type->cls);
        } else {
            NewConstraint(from_type->name, to_type->name);
            return 1;
        }
    }
}

//check if expected_ref is a super class of 'this', its package differs
int vf_TypePool::checkSuperAndPackage(SmConstant expected_ref) {
    //check that expected ref is a super of 'this'
    vf_ValidType *expected_type = getVaildType(expected_ref.getReferenceIdx());
    Class_Handle& referred = expected_type->cls;

    if( !referred ) {
        //try to get class
        referred = vf_resolve_class(k_class, expected_type->name, false);

        if( !referred ) {
            referred = CLASS_NOT_LOADED;
        }
    }

    if( referred == CLASS_NOT_LOADED ) {
        //referred class can't be resolved ==> still might be a super class
        Class_Handle k = class_get_extended_class(k_class, expected_type->name);

        if( k ) {
            referred = k;
        } else {
            return false;
        }
    }

    return !class_is_same_package(k_class, referred) && vf_is_extending(k_class, referred);
}

//check if expected_ref is a super class of 'this', its package differs, and it's protected
int vf_TypePool::checkVirtualAccess(SmConstant expected_ref, unsigned short method_idx) {
    //check if expected_ref is a super class of 'this', its package differs
    if( !checkSuperAndPackage(expected_ref) ) {
        return _FALSE;
    }

    //check further
    //check that they are in different packages and method is protected
    Method_Handle method = class_resolve_method(k_class, method_idx);

    if (!method || method_is_static(method)) {
        //it's not a VerifyError
        return _FALSE;
    }
    
    // for arrays function clone is public
    return !method_is_protected(method) ? _FALSE : !memcmp(method_get_name(method), "clone", 6) ? _CLONE : _TRUE;
}

//check if expected_ref is a super class of 'this', its package differs, and it's protected
int vf_TypePool::checkFieldAccess(SmConstant expected_ref, unsigned short field_idx) {
    //check if expected_ref is a super class of 'this', its package differs
    if( !checkSuperAndPackage(expected_ref) ) {
        return _FALSE;
    }

    //check further
    //check that they are in different packages and method is protected
    Field_Handle field = class_resolve_nonstatic_field(k_class, field_idx);

    if (!field) {
        //it's not a VerifyError
        return _FALSE;
    }
    
    // for arrays function clone is public
    return field_is_protected(field) ? _TRUE : _FALSE;
}


void vf_TypePool::NewConstraint(const char *available,
    const char *required)
{
    vf_TypeConstraint *constraint;

    // lookup constraint
    for( constraint = classwide->class_constraints; constraint; constraint = constraint->next) {
        if( constraint->target == required && constraint->source == available ) {
            // this constraint is already present
            return;
        }
    }

    // set constraint
    constraint = (vf_TypeConstraint*)classwide->constraintPool.malloc(sizeof(vf_TypeConstraint));
    constraint->target = required;
    constraint->source = available;
    constraint->next = classwide->class_constraints;
    classwide->class_constraints = constraint;

    return;
}

SmConstant vf_TypePool::cpool_get_ldcarg(unsigned short cp_idx) {
    if( cp_idx >= k_cp_length || !cp_idx ) return SM_BOGUS;

    switch (class_cp_get_tag( k_class, cp_idx ) ) {
    case _CONSTANT_String: 
        return sm_get_const_string();

    case _CONSTANT_Integer: 
        return SM_INTEGER;

    case _CONSTANT_Float:
        return SM_FLOAT;

    case _CONSTANT_Class:
        //check if it's a 1.5 class (major version is 49)
        return classwide->k_major < 49 ? SM_BOGUS : sm_get_const_class();

    default:
        return SM_BOGUS;
    }
}


SmConstant vf_TypePool::cpool_get_ldc2arg(unsigned short cp_idx) {
    if( cp_idx >= k_cp_length || !cp_idx ) return SM_BOGUS;

    switch (class_cp_get_tag( k_class, cp_idx ) ) {
    case _CONSTANT_Double: 
        return SM_DOUBLE;

    case _CONSTANT_Long: 
        return SM_LONG;

    default:
        return SM_BOGUS;
    }
}


int vf_TypePool::cpool_is_reftype(unsigned short cp_idx) {
    return cp_idx && cp_idx < k_cp_length && class_cp_get_tag( k_class, cp_idx ) == _CONSTANT_Class;
}


int vf_TypePool::cpool_get_class(unsigned short cp_idx, SmConstant *ref, int expected_dim) {
    if( !cpool_is_reftype(cp_idx) ) return false;

    unsigned short name_idx = class_cp_get_class_name_index(k_class, cp_idx);
    if( name_idx >= k_cp_length ) return false;

    const char* name = class_cp_get_utf8_bytes( k_class, name_idx );

    //validate dimensions
    int ptr = 0;
    while (name[ptr] == '[' ) {
        ptr++;
    }
    //'name' already contains final '[', so max dimension of class 'name' is 255
    if( ptr <  expected_dim || ptr > 255 ) return false;

    //array is not allowed here
    if( ptr && expected_dim == -1 ) return false;


    //TODO: do we need to resolve if we don't need SmConstant?
    //e.g. do we need to resolve class of a static variable?
    //constantpool validation should be done whereever else
    if( ref ) *ref = get_ref_type(name, (int)strlen(name));

    return true;
}

int vf_TypePool::cpool_get_array(unsigned short cp_idx, SmConstant *ref) {
    assert(ref);
    if( !cpool_is_reftype(cp_idx) ) return false;

    unsigned short name_idx = class_cp_get_class_name_index(k_class, cp_idx);
    if( name_idx >= k_cp_length ) return false;

    const char* name = class_cp_get_utf8_bytes( k_class, name_idx );
    int len = (int)strlen(name);


    //validate dimensions
    int ptr = 0;
    while (name[ptr] == '[' ) {
        ptr++;
    }

    //'name' does not contain final '[', so max dimension of class 'name' is 254
    if( ptr > 254 ) return false;



    char* arr_name = (char*)classwide->mem.malloc(len + 4);
    arr_name[0] = '[';

    if( name[0] == '[' ) {
        tc_memcpy(arr_name + 1, name, len);
        *ref = get_ref_type(arr_name, len + 1);
    } else {
        arr_name[1] = 'L';
        tc_memcpy(arr_name + 2, name, len);
        arr_name[len + 2] = ';';
        *ref = get_ref_type(arr_name, len + 3);
    }

    classwide->mem.dealloc_last(arr_name, len + 4);
    return true;
}


int vf_TypePool::cpool_get_field(unsigned short cp_idx, SmConstant *ref, SmConstant *value) {
    //check it is a field
    if( !cp_idx || cp_idx >= k_cp_length || class_cp_get_tag( k_class, cp_idx ) != _CONSTANT_Fieldref ) {
        return false;
    }

    unsigned short class_idx = class_cp_get_ref_class_index( k_class, cp_idx );
    if( !cpool_get_class(class_idx, ref) ) return false;

    unsigned short name_and_type_idx = class_cp_get_ref_name_and_type_index( k_class, cp_idx );
    if( !name_and_type_idx || name_and_type_idx >= k_cp_length || class_cp_get_tag( k_class, name_and_type_idx ) != _CONSTANT_NameAndType ) return false;

    //TODO: do we need this check?
    //unsigned short name_idx = class_get_cp_name_index( k_class, name_and_type_idx );
    //if( !name_idx || name_idx >= k_cp_length || class_cp_get_tag( k_class, name_idx ) != _CONSTANT_Utf8 ) return false;

    //get filed type
    unsigned short type_idx = class_cp_get_descriptor_index( k_class, name_and_type_idx );
    if( !type_idx || type_idx >= k_cp_length || class_cp_get_tag( k_class, type_idx ) != _CONSTANT_Utf8 ) return false;

    const char *type = class_cp_get_utf8_bytes( k_class, type_idx );
    *value = get_type(type);

    return true;
}

int vf_TypePool::cpool_method_start(unsigned short cp_idx, const char **state, SmConstant *objectref, 
    unsigned short *name_idx, int opcode) {

    ClassConstantPoolTags expected_tag = opcode == OP_INVOKEINTERFACE ? _CONSTANT_InterfaceMethodref : _CONSTANT_Methodref;

    //check it is a method
    if( !cp_idx || cp_idx >= k_cp_length || class_cp_get_tag( k_class, cp_idx ) != expected_tag ) {
        return false;
    }

    unsigned short class_idx = class_cp_get_ref_class_index( k_class, cp_idx );
    if( opcode == OP_INVOKEVIRTUAL || opcode == OP_INVOKESPECIAL ) {
        if( !cpool_get_class(class_idx, objectref) ) return false;
    } else {
        if( !cpool_get_class(class_idx, 0) ) return false;
        (*objectref) = sm_get_const_object();
    }

    unsigned short name_and_type_idx = class_cp_get_ref_name_and_type_index( k_class, cp_idx );
    if( !name_and_type_idx || name_and_type_idx >= k_cp_length || class_cp_get_tag( k_class, name_and_type_idx ) != _CONSTANT_NameAndType ) return false;

    *name_idx = class_cp_get_name_index( k_class, name_and_type_idx );
    //TODO: do we need this check?
    if( !(*name_idx) || *name_idx >= k_cp_length || class_cp_get_tag( k_class, *name_idx ) != _CONSTANT_Utf8 ) return false;

    //get filed type or function args & rettype
    unsigned short type_idx = class_cp_get_descriptor_index( k_class, name_and_type_idx );
    if( !type_idx || type_idx >= k_cp_length || class_cp_get_tag( k_class, type_idx ) != _CONSTANT_Utf8 ) return false;

    (*state) = class_cp_get_utf8_bytes( k_class, type_idx );
    return true;
}


//find return type and count number of arguments
int vf_TypePool::cpool_method_get_rettype(const char **state, SmConstant *rettype, int *args_sz) {
    const char *name = (*state);
    if( name[0] != '(' ) return 0;

    int i = 1;
    //count number of args: skip '['s and all between 'L' and ';'
    (*args_sz) = 0;
    bool on = true;
    char current;
    while ( (current = name[i]) != ')' ) {
        if( !current ) return 0;
        if( current == 'L' ) on = false;
        if( current == ';' ) on = true;
        if( on && current != '[') {
            (*args_sz)++;
            //it is long or double and not an array of long or double
            if ((current == 'J' || current == 'D') && name[i-1] != '[') {
                (*args_sz)++;
            }
        }
        i++;
    }
    (*state) = i == 1 ? 0 : name + 1;
    name = name + i + 1;

    *rettype = get_type(name);
    return 1;
}

int vf_TypePool::cpool_method_next_arg(const char **state, SmConstant *argument) {
    const char *name = (*state);

    //define name length
    int len = 0;
    //skip '['s
    if ((*state)[len] == '[') {
        while( (*state)[++len] == '[' ) ;//len++;
    }
    //skip up to ';'
    if( (*state)[len] == 'L' ) {
        while( (*state)[++len] != ';' ) ;
        // already checked for '\0' by cpool_method_getrettype
        //        {
        //            assert((*state)[len]);
        //            if( !(*state)[len] ) return false;
        //            len++;
        //        }
    }
    len++;

    *argument = get_type((*state), len);

    (*state) = (*state)[len] == ')' ? 0 : (*state) + len;
    return true;
}

int vf_TypePool::cpool_method_is_constructor_call(unsigned short name_idx) {
    return !strcmp(class_cp_get_utf8_bytes( k_class, name_idx ), "<init>");
}

/* FIXME: deserve separate file probably */
void vf_release_memory(void* error)
{
            tc_free(error);
}

/**
 *  * Creates error message to be reported with verify error
 *   */
void vf_create_error_message(Method_Handle method, vf_Context_Base context, char
        ** error_msg)
{
    char pass[12], instr[12];
    sprintf(pass, "%d", context.pass);
    sprintf(instr, "%d", context.processed_instruction);
    const char* cname = class_get_name(method_get_class(method));
    const char* mname = method_get_name(method);
    const char* mdesc = method_get_descriptor(method);
    const char *format = "%s.%s%s, pass: %s, instr: %s, reason: %s";
    unsigned msg_len = strlen(cname) + strlen(mname) + strlen(mdesc)
        + strlen(pass) + strlen(instr) + strlen(context.error_message)
        + strlen(format);
    *error_msg = (char*)tc_malloc(msg_len);
    if(*error_msg != NULL) {
        sprintf(*error_msg, format,
            cname, mname, mdesc, pass, instr, context.error_message);
    }
}

void vf_create_error_message(Class_Handle klass, vf_TypeConstraint* constraint,
        char** error_msg)
{
    const char* cname = class_get_name(klass);
    const char *format = "constraint check failed, class: %s, source: %s, target: %s";
    unsigned msg_len = strlen(cname) +
        + strlen(constraint->source)
        + strlen(constraint->target) + strlen(format);
    *error_msg = (char*)tc_malloc(msg_len);
    if(*error_msg != NULL) {
        sprintf(*error_msg, format,
            cname, constraint->source, constraint->target);
    }
}

