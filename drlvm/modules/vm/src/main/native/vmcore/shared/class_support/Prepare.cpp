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
 * @author Pavel Pervov
 */  


#define LOG_DOMAIN LOG_CLASS_INFO
#include "cxxlog.h"

#include <assert.h>
#include "String_Pool.h"
#include "Class.h"
#include "classloader.h"
#include "nogc.h"
#include "Package.h"
#include "vtable.h"
#include "vm_strings.h"
#include "open/vm_util.h"
#include "open/gc.h"
#include "environment.h"
#include "compile.h"
#include "interpreter_exports.h"
#include "interpreter.h"
#include "lil.h"
#include "lil_code_generator.h"
#include "vm_stats.h"

#include <algorithm>

#ifdef _DEBUG
#include "jni.h"
#include "jvmti_direct.h"
#endif

#include "dump.h"


static unsigned get_magic_type_size(Field* field) {
    //all magic sizes use size==machine word today
    return sizeof(char*);
}

// For Java currently, fields are not packed: an int16 occupies a full 32 bit word.
// "do_field_compaction" is true, e.g., for packed ("sequential") layout.
// If the "clss" pointer is non-NULL, the type must be that of an instance field and any
// "padding" bytes for the field are added to the class's total number of field padding bytes.
// If "clss" is NULL, no padding information is gathered.
static unsigned sizeof_field_type(Field *field, bool do_field_compaction)
{
    unsigned sz = 0;
    unsigned pad_bytes = 0;

    const String *descriptor = field->get_descriptor();
    switch (descriptor->bytes[0]) {
    case 'B':
    case 'Z':
        if (do_field_compaction) {
            sz = 1;
        } else {
            sz = 4;
            pad_bytes = 3;
        }
        break;
    case 'C':
    case 'S':
        if (do_field_compaction) {
            sz = 2;
        } else {
            sz = 4;
            pad_bytes = 2;
        }
        break;
    case 'F':
    case 'I':
        sz = 4;
        break;
    case 'L': 
    case '[': 
        {
            if (field->is_magic_type()) {
                sz = get_magic_type_size(field);
            } else {
                sz = REF_SIZE;
            }
        }
        break;
    case 'D':
    case 'J':
        sz = 8;
        break;
    default:
        DIE(("Invalid type descriptor"));
    }

    return sz;
} // sizeof_field_type


// Given a class that is a primitive array returns the size
// of an element in an instance of the class
// NOTE: the class is not fully formed at this time
unsigned shift_of_primitive_array_element(Class *p_class) 
{
    const String *elt_type = p_class->get_name();
    char elt = elt_type->bytes[1];
    unsigned int sz;
    switch (elt) {
    case 'C':
        sz = 1;
        break;
    case 'B':
        sz = 0;
        break;
    case 'D':
        sz = 3;
        break;
    case 'F':
        sz = 2;
        break;
    case 'I':
        sz = 2;
        break;
    case 'J':
        sz = 3;
        break;
    case 'S':
        sz = 1;
        break;
    case 'Z': // boolean
        sz = 0;
        break;
    case '[':
        sz = 2; // This works for the H32, V64, R32 version.
        assert(OBJECT_REF_SIZE == 4);
        break;
    default:
        DIE(("Unexpected type descriptor"));
        return 0;
    }
    return sz;
} //shift_of_primitive_array_element


//
// Is this array class a one dimensional array (vector) with a primitive component type.
//
inline bool
is_vector_of_primitives(Class* p_class)
{
    // I parse the following character of the class name
    // to see if it is an array of arrays.
    if(p_class->get_name()->bytes[1] == '[') // An array of array
        return false;
    if(p_class->get_name()->bytes[1] == 'L') // An array of objects
        return false;
    if(!p_class->is_array_of_primitives()) // base type is not primitive
        return false;
    if(p_class->is_array())
        return true;
    LDIE(65, "Should never be called unless p_class is an array");
    return true;
}



void Class::assign_offset_to_instance_field(Field *field, bool do_field_compaction)
{
    if(!field->is_static() && !field->is_offset_computed()) {
        int sz = sizeof_field_type(field, do_field_compaction);
        int offset = m_unpadded_instance_data_size;
        // We must continue to align fields on natural boundaries:
        // e.g., Java ints on a 4 byte boundary.
        // This is required for IPF and can improve IA32 performance.
        int inc = sz;
        int delta = offset % sz;
        if (delta != 0) {
            int pad_bytes = (sz - delta);
            offset += pad_bytes;
            inc    += pad_bytes;
            m_num_field_padding_bytes += pad_bytes;
        }
        field->set_offset(offset);
        m_unpadded_instance_data_size += inc;

        char c_type = *(field->get_descriptor()->bytes);
        if ((c_type == '[') || (c_type == 'L')) {
            m_num_instance_refs += 1;
        }
    }
} // Class::assign_offset_to_instance_field


// "field_ptrs" is an array of pointers to the class's fields.
void Class::assign_offsets_to_instance_fields(Field** field_ptrs, bool do_field_compaction)
{
    int i, sz;
    if(m_num_fields == 0) return;

    // Try to align the first field on a 4 byte boundary. It might not be if
    // -compact_fields was specified on the command line. See whether there are
    // any short instance fields towards the end of the field array (since that
    // is where -XX:+vm.sort_fields puts them) and try to fill in some bytes before
    // the "first" field.
    if(VM_Global_State::loader_env->sort_fields
        && VM_Global_State::loader_env->compact_fields)
    {
        if((m_unpadded_instance_data_size % 4) != 0) {
            int delta = (m_unpadded_instance_data_size % 4);
            int pad_bytes = (4 - delta);   // the number of bytes remaining to fill in
            int last_field = (m_num_fields - 1);
            while(pad_bytes > 0) {
                // Find a field to allocate
                int field_to_allocate = -1;
                for(i = last_field; i >= get_number_of_static_fields(); i--) {
                    Field* field = field_ptrs[i];
                    if(!field->is_static() && !field->is_offset_computed()) {
                        sz = sizeof_field_type(field, do_field_compaction);
                        if(sz > pad_bytes) {
                            break; // field is too big
                        }
                        field_to_allocate = i;
                        break;
                    }
                }
                // Now allocate that field, if one was found
                if (field_to_allocate == -1) {
                    // No field could be found to fill in.
                    // "pad_bytes" is the number of padding bytes to insert.
                    m_unpadded_instance_data_size += pad_bytes;
                    m_num_field_padding_bytes += pad_bytes;
                    break;
                } else {
                    last_field = (i - 1);
                    Field* victim_field = field_ptrs[field_to_allocate];
                    assign_offset_to_instance_field(victim_field, do_field_compaction);
                    delta = (m_unpadded_instance_data_size % 4);
                    pad_bytes = ((delta > 0)? (4 - delta) : 0);
                }
            }
        }
    }

    // Place the remaining instance fields.
    for(i = get_number_of_static_fields(); i < m_num_fields; i++) {
        assign_offset_to_instance_field(field_ptrs[i], do_field_compaction);
    }
} // Class::assign_offsets_to_instance_fields


// "field_ptrs" is an array of pointers to the class's fields.
void Class::assign_offsets_to_static_fields(Field** field_ptrs, bool do_field_compaction)
{
    for(int i = 0; i < get_number_of_static_fields(); i++) {
        Field* field = field_ptrs[i];
        assert(field->is_static());

        // static (i.e., class) data field
        // is this needed for interface static constants?
        int field_size;
        field_size = sizeof_field_type(field, do_field_compaction);

        // Align the static field if necessary.
#ifdef POINTER64
        if (field_size==8) {
#else  // not POINTER64
        if (field->get_descriptor()->bytes[0] == 'D') {
#endif // not POINTER64
            if((m_static_data_size%8)!=0) {
                m_static_data_size += 4;
                assert((m_static_data_size%8)==0);
            }
        }

        field->set_offset(m_static_data_size);
        m_static_data_size += field_size;
    }
} // Class::assign_offsets_to_static_fields


// Return the field's size before any padding: e.g., 1 for a Byte, 2 for a Char.
static int field_size(Field *field, Class * UNREF clss, bool doing_instance_flds) {
    if (doing_instance_flds && field->is_static()) {
        return 0x7FFFFFFF; // INT_MAX
    }

    int sz;
    sz = sizeof_field_type(field, /*do_field_compaction*/ true);
    return sz;
} //field_size


static bool is_greater(Field *f1, Field *f2, Class *clss, bool doing_instance_flds) {
    int f1_size = field_size(f1, clss, doing_instance_flds);
    int f2_size = field_size(f2, clss, doing_instance_flds);
    if (f1_size > f2_size) {
        return true;
    }
    if (f1_size < f2_size) {
        return false;
    }
    // f1 and f2 have the same size. If f1 is a reference, and f2 is not, then f1 is greater than f2.
    char f1_kind = f1->get_descriptor()->bytes[0];
    char f2_kind = f2->get_descriptor()->bytes[0];
    bool f1_is_ref = (f1_kind == 'L') || (f1_kind == '[');
    bool f2_is_ref = (f2_kind == 'L') || (f2_kind == '[');
    if (f1_is_ref && !f2_is_ref) {
        return true;
    }
    return false;
} //is_greater


// Sort in decending order by size
static void bubbleSort(Field* field[], int l, int r, Class *clss, bool doing_instance_flds) {
    bool isChanged = true;
    Field *temp;

    while(isChanged) {
        isChanged = false;
        for(int c = l; c <= r-1; c++) {
            if ( is_greater(field[c+1], field[c], clss, doing_instance_flds) ) {
                temp = field[c+1];
                field[c+1] = field[c];
                field[c] = temp;
                isChanged = true;
            }
        }
    }
}

void Class::assign_offsets_to_fields()
{
    assert(m_state != ST_InstanceSizeComputed);
    bool do_field_compaction = VM_Global_State::loader_env->compact_fields;
    bool do_field_sorting    = VM_Global_State::loader_env->sort_fields;

    // Create a temporary array of pointers to the class's fields. We do this to support sorting the fields
    // by size if the command line option "-XX:+vm.sort_fields" is given, and because elements of the clss->fields array
    // cannot be rearranged without copying their entire Field structure.
    Field** field_ptrs = new Field*[m_num_fields];
    for(int i = 0; i < m_num_fields; i++) {
        field_ptrs[i] = &m_fields[i];
    }

    assert(m_state < ST_InstanceSizeComputed);
    if(m_state != ST_InstanceSizeComputed) {
        // Sort the instance fields by size before allocating their offsets. But not if doing sequential layout!
        // Note: we must sort the instance fields separately from the static fields since for some classes the offsets
        // of statics can only be determined after the offsets of instance fields are found.
        if(do_field_sorting && (m_num_fields > 0)) {
            bubbleSort(field_ptrs, m_num_static_fields,
                (m_num_fields - 1), this,
                /*doing_instance_flds:*/ true);
        }

        // We have to assign offsets to a type's instance fields first because
        // a static field of that type needs the instance size if it's a value type.
        assign_offsets_to_instance_fields(field_ptrs, do_field_compaction);

#ifdef DEBUG_FIELD_SORTING
        if (do_field_sorting) {
            printf("\nInstance fields for %s, size=%d\n", m_name->bytes, m_unpadded_instance_data_size);
            for(int i = 0; i < m_num_fields; i++) {
                Field* field = field_ptrs[i];
                if(!field->is_static()) {
                    const String* typeDesc = field->get_descriptor();
                    int sz = field_size(field, this, /*doing_instance_flds:*/ true);
                    printf("   %40s  %c %4d %4d\n", field->get_name()->bytes, typeDesc->bytes[0], sz, field->get_offset());
                    fflush(stdout);
                }
            }
        }
#endif // DEBUG_FIELD_SORTING

        // Set class to ST_InstanceSizeComputed state.
        m_state = ST_InstanceSizeComputed;
    }

    // Sort the static fields by size before allocating their offsets.
    if(do_field_sorting && (m_num_static_fields > 0)) {
        bubbleSort(field_ptrs, 0, m_num_static_fields - 1,
            this, /*doing_instance_flds:*/ false);
    }
    assign_offsets_to_static_fields(field_ptrs, do_field_compaction);

#ifdef DEBUG_FIELD_SORTING
    if (do_field_sorting) {
        printf("Static fields for %s, size=%d\n", m_name->bytes, m_static_data_size);
        for(int i = 0; i < m_num_fields; i++) {
            Field* field = field_ptrs[i];
            if(field->is_static()) {
                const String *typeDesc = field->get_descriptor();
                int sz = field_size(field, this, /*doing_instance_flds:*/ false);
                printf("   %40s  %c %4d %4d\n", field->get_name()->bytes, typeDesc->bytes[0], sz, field->get_offset());
                fflush(stdout);
            }
        }
    }
#endif // DEBUG_FIELD_SORTING
    delete[] field_ptrs;
} // Class::assign_offsets_to_fields


// Required for reflection. See class_prepare STEP20 for further explanation.
bool assign_values_to_class_static_final_fields(Class *clss)
{
    ASSERT_RAISE_AREA;
    assert(!hythread_is_suspend_enabled());
    bool do_field_compaction = VM_Global_State::loader_env->compact_fields;

    for(int i = 0; i < clss->get_number_of_fields(); i++) {
        Field* field = clss->get_field(i);
        if(field->is_static()) {
            Java_Type field_type = field->get_java_type();
            void* field_addr = field->get_address();

            // If static field is constant it should be initialized by its constant value,...
            if(field->get_const_value_index()) {
                Const_Java_Value cvalue = field->get_const_value();
                switch(field_type) {
                case '[':
                case 'L':
                {
                    // compress static reference fields.
                    if (cvalue.object == NULL) { //cvalue.string == NULL
                        // We needn't deal with this case, because the object field must be set in static initializer.
                        // initialize the field explicitly.
#ifdef REFS_RUNTIME_OR_COMPRESSED
                        REFS_RUNTIME_SWITCH_IF
                            REF_INIT_BY_ADDR(field_addr, 0); // i.e., null
                        REFS_RUNTIME_SWITCH_ENDIF
#endif // REFS_RUNTIME_OR_COMPRESSED
                        break;
                    }
                    static const String* jlstring_desc_string =
                        VM_Global_State::loader_env->
                            string_pool.lookup("Ljava/lang/String;");
                    if (field->get_descriptor() == jlstring_desc_string) {
                        // ------------------------------------------------------------vv
                        Java_java_lang_String *str = vm_instantiate_cp_string_resolved(cvalue.string);

                        if (!str) {
                            assert(exn_raised());
                            tmn_suspend_enable();
                            TRACE2("classloader.prepare", "failed instantiate final field : " 
                                << clss->get_name()->bytes << "." << field->get_name()->bytes);
                            return false;
                        }
                        STORE_GLOBAL_REFERENCE(field_addr, str);
                        // ------------------------------------------------------------^^
                    } else {
                        DIE(("Unexpected type descriptor"));
                    }
                    break;
                }
                default:
                    int field_size = sizeof_field_type(field, do_field_compaction);
                    memmove(field_addr, (void*)&cvalue, field_size);
                }

                // ... if field isn't constant it will be initialized by 0, and <clinit> shall initialize it in future.
            } else {
                if ((field_type == '[') || (field_type == 'L')) {
                    // initialize the field explicitly.
#ifdef REFS_RUNTIME_OR_COMPRESSED
                    REFS_RUNTIME_SWITCH_IF
                        REF_INIT_BY_ADDR(field_addr, 0); // i.e., null
                    REFS_RUNTIME_SWITCH_ENDIF
#endif // REFS_RUNTIME_OR_COMPRESSED
                }
            }
        } // end if static field
    }
    return true;
} //assign_values_to_class_static_final_fields


class class_comparator{
public:
    bool operator() (Class* c1, Class* c2) { 
        if (c1==c2) return false;
        int c1Weight = (c1->get_number_of_methods() << 16) + c1->get_id();
        int c2Weight = (c2->get_number_of_methods() << 16) + c2->get_id();
        return !(c1Weight < c2Weight); //descending compare
    }
};

//
// create_intfc_table
//
Intfc_Table *create_intfc_table(Class* clss, unsigned n_entries)
{
    unsigned size = INTFC_TABLE_OVERHEAD + (n_entries * sizeof(Intfc_Table_Entry));
    Intfc_Table *table = (Intfc_Table*) clss->get_class_loader()->Alloc(size);
    memset(table,0,size);
    table->n_entries = n_entries;
    return table;
}


static void build_interface_table_descriptors(Class* cls, std::vector<Class*>& result, int depth)
{
    // fill in intfc_table_descriptors with the descriptors from the superclass and the superinterfaces
    if(depth == 0 && cls->has_super_class()) {
        Intfc_Table* superIntfcTable = cls->get_super_class()->get_vtable()->intfc_table;
        for(unsigned i = 0; i < superIntfcTable->n_entries; i++) {
            result.push_back(superIntfcTable->entry[i].intfc_class);
        }
    }
    uint16 num_superinterfaces = cls->get_number_of_superinterfaces();
    for(unsigned k = 0; k < num_superinterfaces; k++) {
        Class* intfc = cls->get_superinterface(k);
        build_interface_table_descriptors(intfc, result, depth+1);
    }
    // if this class is an interface, add it to the interface table
    if(cls->is_interface()) {
        result.push_back(cls);
    }
    if (depth==0) {
        // sort the interfaces eliminating duplicate entries
        std::sort(result.begin(), result.end(), class_comparator());
        std::vector<Class*>::iterator newEnd = std::unique(result.begin(), result.end());
        result.erase(newEnd, result.end());
    }
}


// Returns the method matching the Signature "sig" that is implemented directly or indirectly by "clss", or NULL if not found.
Method *find_method_impl_by_class(Class *clss, const String* name, const String* desc)
{
    assert(clss);
    Method *m = NULL;
    for(;  ((clss != NULL) && (m == NULL));  clss = clss->get_super_class()) {
        m = clss->lookup_method(name, desc);
    }
    return m;
} //find_method_impl_by_class

// Add the new fake methods to class.
void inline add_new_fake_method( Class *clss, Class *example, unsigned *next)
{
    for (unsigned i = 0;  i < clss->get_number_of_superinterfaces();  i++) {
        Class *intf_clss = clss->get_superinterface(i);
        add_new_fake_method(intf_clss, example, next);
        for(unsigned k = 0;  k < intf_clss->get_number_of_methods();  k++) {
            Method* intf_method = intf_clss->get_method(k);
            if(intf_method->is_clinit()) {
                continue;
            }
            // See if the interface method "intf_method" is implemented by clss. 
            const String* intf_name = intf_method->get_name();
            const String* intf_desc = intf_method->get_descriptor();
            Method* impl_method = find_method_impl_by_class(example, intf_name, intf_desc);

            if (impl_method == NULL) {
#ifdef DEBUG_FAKE_METHOD_ADDITION
                printf("**    Adding fake method to class %s for unimplemented method %s of interface %s.\n", 
                    example->name->bytes, intf_name->bytes, intf_clss->name->bytes);
#endif
                Method* fake_method = example->get_method(*next);
                (*next)++;

                fake_method->_class = example;
                // 20021119 Is this the correct signature?
                fake_method->_name = (String*)intf_name; 
                fake_method->_descriptor = (String*)intf_desc; 
                fake_method->_state = Method::ST_NotCompiled;
                fake_method->_access_flags = (ACC_PUBLIC | ACC_ABSTRACT);
                // Setting its "_intf_method_for_fake_method" field marks the method as being fake.
                fake_method->_intf_method_for_fake_method = intf_method;
                fake_method->_arguments_slot_num = intf_method->_arguments_slot_num;
                // The rest of the method's fields were zero'd above 
            }
        }
    }
    return;
} // add_new_fake_method

// Count the fake methods.
// These are the interface methods that are not implemented by the class.
unsigned inline count_fake_interface_method( Class *clss, Class *example ) 
{
    unsigned count = 0;
    for (unsigned i = 0;  i < clss->get_number_of_superinterfaces();  i++) {
        Class *intf_clss = clss->get_superinterface(i);
        count += count_fake_interface_method(intf_clss, example);
        for(unsigned k = 0;  k < intf_clss->get_number_of_methods();  k++) {
            Method* intf_method = intf_clss->get_method(k);
            if(intf_method->is_clinit()) {
                continue;
            }
            // See if the interface method "intf_method" is implemented by clss. 
            const String *intf_name = intf_method->get_name();
            const String *intf_desc = intf_method->get_descriptor();
            Method *impl_method = find_method_impl_by_class(example, intf_name, intf_desc);
            if (impl_method == NULL) {
                count++;
            }
        }
    }
    return count;
} // count_fake_interface_method


void Class::add_any_fake_methods()
{
    assert(is_abstract());

    // First, count the fake methods. These are the interface methods that are not implemented by the class.
    unsigned num_fake_methods = count_fake_interface_method(this, this);

    // If no fake methods are needed, just return.
    if (num_fake_methods == 0) {
        return;
    }

    // Reallocate the class's method storage block, creating a new method area with room for the fake methods.
#ifdef DEBUG_FAKE_METHOD_ADDITION
    printf("\n** %u fake methods needed for class %s \n", num_fake_methods, clss->name->bytes);
#endif
    unsigned new_num_methods = (m_num_methods + num_fake_methods);
    Method* new_meth_array = new Method[new_num_methods];
    if(m_methods != NULL) {
        memcpy(new_meth_array, m_methods, (m_num_methods * sizeof(Method)));
    }
    unsigned next_fake_method_idx = m_num_methods;
    memset(&(new_meth_array[next_fake_method_idx]), 0, (num_fake_methods * sizeof(Method)));

    // Regenerate the existing compile-me/delegate/unboxer stubs and redirect
    // the class's static_initializer and default_constructor fields since
    // they refer to the old method block. We regenerate the stubs
    // because any code to update the addresses in the existing stubs would be
    // very fragile, fake methods very rarely need to be added, and the stubs
    // are small.
    // Note that this is still the old number of methods.
    for(unsigned i = 0;  i < m_num_methods;  i++) {
        Method* m = &m_methods[i];
        Method* m_copy = &new_meth_array[i];
        if(m_copy->get_method_sig())
        {
            m_copy->get_method_sig()->method = m_copy;
        }
        if(m->is_clinit())
        {
            m_static_initializer = m_copy;
        }
        if(m->get_name() == VM_Global_State::loader_env->Init_String
            && m->get_descriptor() == VM_Global_State::loader_env->VoidVoidDescriptor_String)
        {
            m_default_constructor = m_copy;
        }
    }

    // Free the old storage area for the class's methods, and have the class point to the new method storage area.
    if(m_methods != NULL) {
        delete[] m_methods;
    }
    m_methods = new_meth_array;
    m_num_methods = (uint16)new_num_methods;

    // Add the new fake methods.
    add_new_fake_method( this, this, &next_fake_method_idx );
    // some methods could be counted several times as "fake" methods (count_fake_interface_method())
    // however they are added only once. So we adjust the number of added methods.
    assert(next_fake_method_idx <= new_num_methods);
    m_num_methods = (uint16)next_fake_method_idx;
} //add_any_fake_methods


void Class::assign_offsets_to_methods(Global_Env* env)
{
    // At this point we have an array of the interfaces implemented by
    // this class. We also know the number of methods in the interface part
    // of the vtable. We now need to find the number of virtual methods
    // that are in the virtual method part of the vtable, before we can
    // allocate _vtable and _vtable_descriptors.

    // First, if the class is abstract, add any required "fake" methods:
    // these are abstract methods inherited by an abstract class that are
    // not implemented by that class or any superclass.
    if(is_abstract() && !is_interface()) {
        add_any_fake_methods();
    }

    Method** super_vtable_descriptors = NULL;
    unsigned n_super_virtual_method_entries = 0;
    if(has_super_class()) {
        super_vtable_descriptors = get_super_class()->m_vtable_descriptors;
        n_super_virtual_method_entries =
            get_super_class()->m_num_virtual_method_entries;
    }
    // Offset of the next entry in the vtable to use.
#ifdef POINTER64
    unsigned next_vtable_offset = m_num_virtual_method_entries << 3;
#else
    unsigned next_vtable_offset = m_num_virtual_method_entries << 2;
#endif
    if(!is_interface()) {
        // Classes have an additional overhead for the class pointer and interface table.
        next_vtable_offset += VTABLE_OVERHEAD;
    }
    unsigned i, j;
    if (!interpreter_enabled())
        for(i = 0;  i < m_num_methods;  i++) {
            Method& method = m_methods[i];

            // check if the method hasn't already been initialized or even compiled
            assert(method.get_code_addr() == NULL);
            // initialize method's code address
            method.set_code_addr((char*)compile_gen_compile_me(&method));
        }

    for(i = 0;  i < m_num_methods;  i++) {
        Method& method = m_methods[i];
        if(!method.is_static()) {
            // A virtual method. Look it up in virtual method tables of the
            // super classes; if not found, then assign a new offset.

            // Ignore initializers.
            if (method.is_init()) {
                continue;
            }

            if (method.is_finalize()) {
                if(get_name() != env->JavaLangObject_String) {
                    m_has_finalizer = 1;
                }
#ifdef REMOVE_FINALIZE_FROM_VTABLES
                // skip over finalize() method, but remember it
                finalize_method = &method;
                continue;
#endif
            }
            unsigned off   = 0;
            unsigned index = 0;
            if (super_vtable_descriptors != NULL) {
                const String *name = method.get_name();
                const String *desc = method.get_descriptor();
                for (j = 0;  j < n_super_virtual_method_entries;  j++) {
                    Method *m = super_vtable_descriptors[j];
                    if (name == m->get_name() && desc == m->get_descriptor()) {
                        if(m->is_final()) {
                            if(m->is_private()
                                || (m->is_package_private() 
                                    && m->get_class()->get_package() != method.get_class()->get_package()))
                            {
                                // We allow to override private final and
                                // default (package private) final methods
                                // from superclasses since they are not accessible
                                // from descendants.
                                // Note: for package private methods this statement
                                // is true only for classes from different packages
                            } else {
                                REPORT_FAILED_CLASS_CLASS(get_class_loader(), this,
                                    "java/lang/VerifyError",
                                    "An attempt is made to override final method "
                                    << m->get_class()->get_name()->bytes << "."
                                    << m->get_name()->bytes << m->get_descriptor()->bytes);
                                m_state = ST_Error;
                                return;
                            }
                        }
                        // method doesn't override m if method has package access
                        // and is in a different runtime package than m.
                        if(m_package == m->get_class()->get_package()
                            || m->is_public()
                            || m->is_protected()
                            || m->is_private())
                        {
                            off   = m->get_offset();
                            index = m->get_index();
                            // mark superclass' method "m" as being overridden
                            m->method_was_overridden();
                        }
                        break;
                    }
                }
            }
            if (off == 0 || is_interface()) {
                // Didn't find a matching signature in any super class;
                // add a new entry to this class' vtable.
                off = next_vtable_offset;
                index = m_num_virtual_method_entries;
#ifdef POINTER64
                next_vtable_offset += 8;
#else
                next_vtable_offset += 4;
#endif
                m_num_virtual_method_entries++;
            }
            method.set_position_in_vtable(index, off);
        }
    }

    // Figure out which methods don't do anything
    // ppervov: suspending this check, as it only detects empty constructors
    //for (i = 0;  i < n_methods;  i++) {
    //    Method& method = methods[i];
    //    method._set_nop();
    //}

    // Decide whether it is possible to allocate instances of this class using a fast inline sequence containing
    // no calls to other routines. This means no calls to raise exceptions or to invoke constructors. It will also be
    // necessary that the allocation itself can be done without needing to call a separate function.
    bool is_not_instantiable = (     // if true, will raise java_lang_InstantiationException
        (m_default_constructor == NULL)
        || (is_primitive() || is_array() || is_interface() || is_abstract())
        || (this == VM_Global_State::loader_env->Void_Class));
    if(!is_not_instantiable && m_default_constructor->is_nop()) {
        m_is_fast_allocation_possible = 1;
    }
} // Class::assign_offsets_to_methods


bool Class::initialize_static_fields_for_interface()
{
    ASSERT_RAISE_AREA;
    tmn_suspend_disable();
    m_state = ST_Prepared;
    // Initialize static fields
    if(!assign_values_to_class_static_final_fields(this)) {
        tmn_suspend_enable();
        return false;
    }
    tmn_suspend_enable();

#ifndef NDEBUG
    for(uint16 i = 0; i < m_num_methods; i++) {
        if(m_methods[i].is_clinit()) {
            assert(m_static_initializer == &(m_methods[i]));
        }
    }
#endif
    TRACE2("classloader.prepare", "interface " << m_name->bytes << " prepared");

    return true;
} // Class::initialize_static_fields_for_interface


void Class::create_vtable(unsigned n_vtable_entries)
{
    unsigned vtable_size = VTABLE_OVERHEAD + n_vtable_entries * sizeof(void *);

    // Always allocate vtable data from vtable_data_pool
    void *p_gc_hdr = m_class_loader->VTableAlloc(vtable_size, 16, CAA_Allocate);

#ifdef VM_STATS
    // For allocation statistics, include any rounding added to make each
    // item aligned (current alignment is to the next 16 byte boundary).
    unsigned num_bytes = (vtable_size + 15) & ~15;
    // 20020923 Total number of allocations and total number of
    // bytes for class-related data structures.
    VM_Statistics::get_vm_stats().num_vtable_allocations++;
    VM_Statistics::get_vm_stats().total_vtable_bytes += num_bytes;
#endif
    assert(p_gc_hdr);
    memset(p_gc_hdr, 0, vtable_size);

    VTable* vtable = (VTable*)p_gc_hdr;

    if(has_super_class()) {
        m_depth = get_super_class()->m_depth + 1;
        memcpy(&vtable->superclasses,
               &get_super_class()->m_vtable->superclasses,
               sizeof(vtable->superclasses));
        for(unsigned i = 0; i < vm_max_fast_instanceof_depth(); i++) {
            if(vtable->superclasses[i] == NULL) {
                vtable->superclasses[i] = this;
                break;
            }
        }
    }
    if(m_depth > 0
        && m_depth < vm_max_fast_instanceof_depth()
        && !is_array()
        && !is_interface())
    {
        m_is_suitable_for_fast_instanceof = 1;
    }

    Global_Env* env = VM_Global_State::loader_env;
    if (!env->InBootstrap()){
        tmn_suspend_disable();
        vtable->jlC = *get_class_handle(); 
        tmn_suspend_enable();
    }
    else {
        // for BootStrap mode jlC is set in create_instance_for_class
        // class_handle is NULL in bootstrap mode 
        assert (!get_class_handle());
        vtable->jlC = NULL;
    }
    
    m_vtable = vtable;
} // Class::create_vtable


void Class::populate_vtable_descriptors_table_and_override_methods(const std::vector<Class*>& intfc_table_entries)
{
    // Populate _vtable_descriptors first with _n_virtual_method_entries
    // from super class
    if(has_super_class()) {
        for(unsigned i = 0; i < get_super_class()->m_num_virtual_method_entries; i++) {
            m_vtable_descriptors[i] = get_super_class()->m_vtable_descriptors[i];
        }
    }
    // NOW OVERRIDE with this class' methods
    unsigned i;
    for(i = 0; i < m_num_methods; i++) {
        Method* method = &(m_methods[i]);
        if(method->is_clinit()) {
            assert(m_static_initializer == method);
        } 
        if(method->is_static()
            || method->is_init()
#ifdef REMOVE_FINALIZE_FROM_VTABLES
            || method->is_finalize()
#endif
            )
            continue;
        m_vtable_descriptors[method->get_index()] = method;
    }
    // finally, the interface methods
    unsigned index = m_num_virtual_method_entries;
    for(i = 0; i < intfc_table_entries.size();  i++) {
        Class* intfc = intfc_table_entries[i];
        for(unsigned k = 0; k < intfc->get_number_of_methods(); k++) {
            if(intfc->get_method(k)->is_clinit()) {
                continue;
            }

            // Find method with matching signature and replace
            const String* sig_name = intfc->get_method(k)->get_name();
            const String* sig_desc = intfc->get_method(k)->get_descriptor();
            Method* method = NULL;
            for(unsigned j = 0; j < m_num_virtual_method_entries; j++) {
                if(m_vtable_descriptors[j]->get_name() == sig_name
                    && m_vtable_descriptors[j]->get_descriptor() == sig_desc)
                {
                    method = m_vtable_descriptors[j];
                    break;      // a match!
                }
            }
            if(method == NULL && !is_abstract()) {
                // There're many cases VM will run apps built on previous JDK
                // version, and without implementations of newly added methods
                // for specific interfaces, we allow them to continue to run
                TRACE2("classloader.prepare", "No implementation in class "
                    << get_name()->bytes << " for method "
                    << sig_name->bytes << " of interface "
                    << intfc->get_name()->bytes
                    << ". \n\nCheck whether you used another set of class library.\n");
            }
            m_vtable_descriptors[index] = method;
            index++;
        }
    }
} // Class::populate_vtable_descriptors_table_and_override_methods


void Class::point_vtable_entries_to_stubs()
{
    for (unsigned i = 0; i < m_num_virtual_method_entries; i++) {
        assert(m_vtable_descriptors[i]);
        Method* method = m_vtable_descriptors[i];
        assert(!method->is_static());
        if(!method->is_static()) {
            unsigned meth_idx = method->get_index();
            // There are several assumptions in the code that the width
            // of each method pointer is the same as void*.
            assert((method->get_offset() - VTABLE_OVERHEAD)/sizeof(void*) == method->get_index());
            m_vtable->methods[meth_idx] =
                (unsigned char*)method->get_code_addr();
            method->add_vtable_patch(&(m_vtable->methods[meth_idx]));
            assert(method->is_fake_method() || interpreter_enabled() || method->get_code_addr());
        }
    }
}

extern bool dump_stubs;

// It's a rutime helper. So should be named as rth_prepare_throw_abstract_method_error
void prepare_throw_abstract_method_error(Class_Handle clss, Method_Handle method)
{
    char* buf = (char*)STD_ALLOCA(clss->get_name()->len + method->get_name()->len
        + method->get_descriptor()->len + 2); // . + \0
    sprintf(buf, "%s.%s%s", clss->get_name()->bytes,
        method->get_name()->bytes, method->get_descriptor()->bytes);
    tmn_suspend_enable();

    // throw exception here because it's a helper
    exn_throw_by_name("java/lang/AbstractMethodError", buf);
    tmn_suspend_disable();
}

NativeCodePtr prepare_gen_throw_abstract_method_error(Class_Handle clss, Method_Handle method)
{
    NativeCodePtr addr = NULL;
    void (*p_throw_ame)(Class_Handle, Method_Handle) =
        prepare_throw_abstract_method_error;
    LilCodeStub* cs = lil_parse_code_stub("entry 0:stdcall::void;"
        "push_m2n 0, 0;"
        "m2n_save_all;"
        "out platform:pint,pint:void;"
        "o0=%0i:pint;"
        "o1=%1i:pint;"
        "call.noret %2i;",
        clss, method, p_throw_ame);
    assert(cs && lil_is_valid(cs));
    addr = LilCodeGenerator::get_platform()->compile(cs);
    
    DUMP_STUB(addr, "prepare_throw_abstract_method_error", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);

    return addr;
}

// It's a rutime helper. So should be named as rth_prepare_throw_illegal_access_error
void prepare_throw_illegal_access_error(Class_Handle to, Method_Handle from)
{
    char* buf = (char*)STD_ALLOCA(from->get_class()->get_name()->len
        + to->get_name()->len + from->get_name()->len
        + from->get_descriptor()->len + 12); // from + to + . + \0
    sprintf(buf, "from %s to %s.%s%s", from->get_class()->get_name()->bytes,
        to->get_name()->bytes,
        from->get_name()->bytes, from->get_descriptor()->bytes);
    tmn_suspend_enable();

    // throw exception here because it's a helper
    exn_throw_by_name("java/lang/IllegalAccessError", buf);
    tmn_suspend_disable();
}

NativeCodePtr prepare_gen_throw_illegal_access_error(Class_Handle to, Method_Handle from)
{
    NativeCodePtr addr = NULL;
    void (*p_throw_iae)(Class_Handle, Method_Handle) =
        prepare_throw_illegal_access_error;
    LilCodeStub* cs = lil_parse_code_stub("entry 0:stdcall::void;"
        "push_m2n 0, 0;"
        "m2n_save_all;"
        "out platform:pint,pint:void;"
        "o0=%0i:pint;"
        "o1=%1i:pint;"
        "call.noret %2i;",
        to, from, p_throw_iae);
    assert(cs && lil_is_valid(cs));
    addr = LilCodeGenerator::get_platform()->compile(cs);
    
    DUMP_STUB(addr, "rth_throw_linking_exception", lil_cs_get_code_size(cs));

    lil_free_code_stub(cs);

    return addr;
}

Intfc_Table* Class::create_and_populate_interface_table(const std::vector<Class*>& intfc_table_entries)
{
    // shouldn't it be called vtable_index?
    Intfc_Table* intfc_table = create_intfc_table(this,
        (unsigned)intfc_table_entries.size());
    if(intfc_table_entries.size() != 0) {
        unsigned vtable_offset = m_num_virtual_method_entries;
        for (unsigned i = 0; i < intfc_table_entries.size(); i++) {
            Class* intfc = intfc_table_entries[i];
            intfc_table->entry[i].intfc_class = intfc;
            intfc_table->entry[i].table = &m_vtable->methods[vtable_offset];
            vtable_offset += intfc->get_number_of_methods();
            if(intfc->m_static_initializer) {
                // Don't count static initializers of interfaces.
                vtable_offset--;
            }
        }
        // Set the vtable entries to point to the code address.
        unsigned meth_idx = m_num_virtual_method_entries;
        for (unsigned i = 0; i < intfc_table_entries.size(); i++) {
            Class* intfc = intfc_table_entries[i];
            for(unsigned k = 0; k < intfc->get_number_of_methods(); k++) {
                if (intfc->get_method(k)->is_clinit()) {
                    continue;
                }
                Method* method = m_vtable_descriptors[meth_idx];
                if(method == NULL || method->is_abstract()) {
                    TRACE2("classloader.prepare.ame", "Inserting Throw_AbstractMethodError stub for method\n\t"
                        << m_name->bytes << "."
                        << intfc->get_method(k)->get_name()->bytes
                        << intfc->get_method(k)->get_descriptor()->bytes);
                    m_vtable->methods[meth_idx] =
                        (unsigned char*)prepare_gen_throw_abstract_method_error(this, intfc->get_method(k));
                } else if(method->is_public()) {
                    m_vtable->methods[meth_idx] =
                        (unsigned char *)method->get_code_addr();
                    method->add_vtable_patch(&(m_vtable->methods[meth_idx]));
                } else {
                    TRACE2("classloader.prepare.iae", "Inserting Throw_IllegalAccessError stub for method\n\t"
                        << method->get_class()->get_name()->bytes << "."
                        << method->get_name()->bytes << method->get_descriptor()->bytes);
                    m_vtable->methods[meth_idx] =
                        (unsigned char*)prepare_gen_throw_illegal_access_error(intfc, method);
                }
                meth_idx++;
            }
        }
    }
    return intfc_table;
} // Class::create_and_populate_interface_table


bool Class::prepare(Global_Env* env)
{
    ASSERT_RAISE_AREA;
    //
    // STEP 1 ::: SIMPLY RETURN IF already prepared, initialized, or currently initializing.
    //
    if(is_at_least_prepared() || in_error()) // try fast path
        return true;

    LMAutoUnlock autoUnlocker(m_lock);

    if(is_at_least_prepared() || in_error()) // try slow path
        return true;

    TRACE2("classloader.prepare", "BEGIN class prepare, class name = " << m_name->bytes);
    assert(m_state == ST_BytecodesVerified);

    //
    // STEP 2 ::: PREPARE SUPER-INTERFACES
    //
    unsigned i;
    for(i = 0; i < m_num_superinterfaces; i++) {
        assert(m_superinterfaces[i].clss->is_interface());
        if(!m_superinterfaces[i].clss->prepare(env)) {
            REPORT_FAILED_CLASS_CLASS(m_class_loader, this,
                VM_Global_State::loader_env->JavaLangNoClassDefFoundError_String->bytes,
                m_name->bytes << ": error preparing superinterface "
                << m_superinterfaces[i].clss->get_name()->bytes);
            return false;
        }
    }

    //
    // STEP 3 ::: PREPARE SUPERCLASS if needed
    //
    if(!is_interface() && has_super_class())
    {
        // Regular class with super-class
        if(!get_super_class()->prepare(env)) {
            REPORT_FAILED_CLASS_CLASS(m_class_loader, this,
                VM_Global_State::loader_env->JavaLangNoClassDefFoundError_String->bytes,
                m_name->bytes << ": error preparing superclass "
                << get_super_class()->get_name()->bytes);
            return false;
        }
    }

    //
    // STEP 4 ::: setup selected class properties
    //
    if(!is_interface()) {
        if(has_super_class()) {
            if(get_super_class()->has_finalizer()) {
                m_has_finalizer = 1;
            }
            // Copy over instance size, instance refs, static fields #,
            // and num_field_padding_bytes from the super class.
            if(m_name == env->JavaLangClass_String) {
                // calculate unpadded instance data size
                // for java/lang/Class separately
                m_unpadded_instance_data_size =
                    (((unsigned)ManagedObject::get_size() + (GC_OBJECT_ALIGNMENT - 1))
                    / GC_OBJECT_ALIGNMENT)
                    * GC_OBJECT_ALIGNMENT;
            } else {
                m_unpadded_instance_data_size =
                    get_super_class()->m_unpadded_instance_data_size;
            }
            m_num_instance_refs = get_super_class()->m_num_instance_refs;
            m_num_field_padding_bytes =
                get_super_class()->m_num_field_padding_bytes;
            // Copy over number of virtual methods
            // and interface methods of super class
            m_num_virtual_method_entries =
                get_super_class()->m_num_virtual_method_entries;
        } else {
            // this is java/lang/Object
            // FIXME: primitive classes also get here, but this assignment
            // has no effect on them really
            m_unpadded_instance_data_size = (unsigned)ManagedObject::get_size();
        }
    }

    //
    // STEP 5 ::: ASSIGN OFFSETS to the class and instance data FIELDS.
    //            This SETs class to ST_InstanceSizeComputed state.
    //
    assign_offsets_to_fields();
    assert(m_state == ST_InstanceSizeComputed);

    //
    // STEP 6 ::: Calculate # of INTERFACES METHODS and build interface table DESCRIPTORS for C
    //
    std::vector<Class*> intfc_table_entries;
    build_interface_table_descriptors(this, intfc_table_entries, 0);

    //
    // STEP 7 ::: ASSIGN OFFSETS to the class and virtual METHODS
    //
    assign_offsets_to_methods(env);
    if(exn_raised())
        return false;

    //
    // STEP 8 ::: Create the static field block
    //
    m_static_data_block = (char*)m_class_loader->Alloc(m_static_data_size);
    memset(m_static_data_block, 0, m_static_data_size);

#ifdef VM_STATS
    // Total number of allocations and total number of bytes for class-related data structures.
    // This includes any rounding added to make each item aligned (current alignment is to the next 16 byte boundary).
    unsigned num_bytes = (m_static_data_size + 15) & ~15;
    VM_Statistics::get_vm_stats().num_statics_allocations++;
    if(m_static_data_size > 0) {
        VM_Statistics::get_vm_stats().num_nonempty_statics_allocations++;
    }
    VM_Statistics::get_vm_stats().total_statics_bytes += num_bytes;
#endif
    assert(m_static_data_block);
    // block must be on a 8 byte boundary
    assert((((POINTER_SIZE_INT)(m_static_data_block)) % 8) == 0);

    //
    // STEP 9 ::: For INTERFACES initialize static fields and return.
    //
    if(is_interface()) {
        bool init_fields = initialize_static_fields_for_interface();
        if(!env->InBootstrap())
        {
            autoUnlocker.ForceUnlock();
            assert(hythread_is_suspend_enabled());
            if (init_fields
                && jvmti_should_report_event(JVMTI_EVENT_CLASS_PREPARE))
            {
                jvmti_send_class_prepare_event(this);
            }
        }
        // DONE for interfaces
        TRACE2("classloader.prepare", "END class prepare, class name = "
            << m_name->bytes);
        return init_fields;
    }

    //
    // STEP 10 ::: COMPUTE number of interface method entries.
    //
    for(i = 0; i < intfc_table_entries.size(); i++) {
        Class* intfc = intfc_table_entries[i];
        m_num_intfc_method_entries += intfc->get_number_of_methods();
        if(intfc->m_static_initializer) {
            // Don't count static initializers of interfaces.
            m_num_intfc_method_entries--;
        }
    }

    //
    // STEP 11 ::: ALLOCATE the Vtable descriptors array
    //
    unsigned n_vtable_entries =
        m_num_virtual_method_entries + m_num_intfc_method_entries;
    if(n_vtable_entries != 0) {
        m_vtable_descriptors = new Method*[n_vtable_entries];
        // ppervov: FIXME: should throw OOME
    }

    //
    // STEP 12 ::: POPULATE with interface descriptors and virtual method descriptors
    //             Also, OVERRIDE superclass' methods with those of this one's
    //
    populate_vtable_descriptors_table_and_override_methods(intfc_table_entries);

    //
    // STEP 13 ::: CREATE VTABLE and set the Vtable entries to point to the
    //             code address (a stub or jitted code)
    //
    create_vtable(n_vtable_entries);
    assert(m_vtable);
    for(i = 0; i < n_vtable_entries; i++) {
        // need to populate with pointers to stubs or compiled code
        m_vtable->methods[i] = NULL;    // for now
    }

    if(vm_is_vtable_compressed())
    {
        m_allocation_handle =
            (Allocation_Handle)((UDATA)m_vtable - (UDATA)vm_get_vtable_base_address());
    }
    else
    {
        m_allocation_handle = (Allocation_Handle)m_vtable;
    }
    m_vtable->clss = this;

    // Set the vtable entries to point to the code address (a stub or jitted code)
    point_vtable_entries_to_stubs();

    //
    // STEP 14 ::: CREATE and POPULATE the CLASS INTERFACE TABLE
    //
    m_vtable->intfc_table = create_and_populate_interface_table(intfc_table_entries);
    
    //cache first values
    if (m_vtable->intfc_table->n_entries >= 1 ) {
        m_vtable->intfc_class_0 = m_vtable->intfc_table->entry[0].intfc_class;
        m_vtable->intfc_table_0 = m_vtable->intfc_table->entry[0].table;
    }
    if (m_vtable->intfc_table->n_entries >= 2 ) {
        m_vtable->intfc_class_1 = m_vtable->intfc_table->entry[1].intfc_class;
        m_vtable->intfc_table_1 = m_vtable->intfc_table->entry[1].table;
    }
    if (m_vtable->intfc_table->n_entries >= 3 ) {
        m_vtable->intfc_class_2 = m_vtable->intfc_table->entry[2].intfc_class;
        m_vtable->intfc_table_2 = m_vtable->intfc_table->entry[2].table;
    }

    //
    // STEP 15 ::: HANDLE JAVA CLASS CLASS separately
    //
    // Make sure no one hasn't prematurely set these fields since all calculations
    // up to this point should be based on clss->unpadded_instance_data_size.
    assert(m_instance_data_size == 0);
    assert(m_allocated_size == 0);
    // Add any needed padding including the OBJECT_HEADER which is used to hold
    // things like gc forwarding pointers, mark bits, hashes and locks..
    m_allocated_size = 
        (((m_unpadded_instance_data_size + (GC_OBJECT_ALIGNMENT - 1)) 
          / GC_OBJECT_ALIGNMENT) * GC_OBJECT_ALIGNMENT) + OBJECT_HEADER_SIZE;
    // Move the size to the vtable.
    m_vtable->allocated_size = m_allocated_size;
    m_instance_data_size = m_allocated_size;
    TRACE2("class.size", "class " << this << " allocated size "
            << m_allocated_size);

    //
    // STEP 16 :::: HANDLE PINNING and Class PROPERTIES if needed.
    //
    if(has_super_class()
        && (get_super_class()->m_vtable->class_properties & CL_PROP_PINNED_MASK) != 0)
    {
        // If the super class is pinned then this class is pinned
        m_vtable->class_properties |= CL_PROP_PINNED_MASK;
        set_instance_data_size_constraint_bit();
    }

    // Set up the class_properties field.
    if(is_array()) {
        m_array_element_size = (vm_is_heap_compressed()
            ? sizeof(COMPRESSED_REFERENCE) : sizeof(RAW_REFERENCE));
        m_array_element_shift = m_array_element_size == 8 ? 3 : 2;
        m_vtable->class_properties |= CL_PROP_ARRAY_MASK;
        if(is_vector_of_primitives(this)) {
            m_array_element_shift = shift_of_primitive_array_element(this);
            m_array_element_size = 1 << m_array_element_shift;
            m_vtable->class_properties |= CL_PROP_NON_REF_ARRAY_MASK;
        }
        m_vtable->array_element_size = (unsigned short)m_array_element_size;
        switch(m_vtable->array_element_size)
        {
        case 1:
            m_vtable->array_element_shift = 0;
            break;
        case 2:
            m_vtable->array_element_shift = 1;
            break;
        case 4:
            m_vtable->array_element_shift = 2;
            break;
        case 8:
            m_vtable->array_element_shift = 3;
            break;
        default:
            m_vtable->array_element_shift = 65535;
            LDIE(66, "Unexpected array element size: {0}" << m_vtable->array_element_size);
            break;
        }
    }

#ifndef POINTER64
    if(!strcmp("[D", m_name->bytes)) {
        // In IA32, Arrays of Doubles need to be eight byte aligned to improve 
        // performance. In IPF all objects (arrays, class data structures, heap objects)
        // get aligned on eight byte boundaries. So, this special code is not needed.
        m_alignment = ((GC_OBJECT_ALIGNMENT<8)?8:GC_OBJECT_ALIGNMENT);;

        // align doubles on 8, clear alignment field and put in 8.
        m_vtable->class_properties |= 8;
        // Set high bit in size so that gc knows there are constraints
        set_instance_data_size_constraint_bit();
    }
#endif

    //
    // STEP 17 ::: HANDLE ALIGNMENT and Class FINALIZER if needed.
    //
    if(m_alignment) {
        if(m_alignment != GC_OBJECT_ALIGNMENT) { 
            // The GC will align on 4 byte boundaries by default on IA32....
#ifdef POINTER64
            LDIE(67, "Alignment is supposed to be appropriate");
#endif
            // Make sure it is a legal mask.
            assert((m_alignment & CL_PROP_ALIGNMENT_MASK) <= CL_PROP_ALIGNMENT_MASK);
            m_vtable->class_properties |= m_alignment;
            set_instance_data_size_constraint_bit();
            // make sure constraintbit was set.
            assert(get_instance_data_size() != m_instance_data_size);
        }
    }

    if(has_finalizer()) {
        m_vtable->class_properties |= CL_PROP_FINALIZABLE_MASK;
        set_instance_data_size_constraint_bit();
    }

    //
    // STEP 18 ::: SET Class ALLOCATED SIZE to INSTANCE SIZE
    //
    // Finally set the allocated size field.
    m_allocated_size = get_instance_data_size();
 
    //
    // STEP 18a: Determine if class should have special access check treatment.
    //
    static const char* reflect = "java/lang/reflect/";
    static const size_t reflect_len = strlen(reflect);
    if(strncmp(m_name->bytes, reflect, reflect_len) == 0)
        m_can_access_all = 1;

    //
    // STEP 19 :::: SET class to ST_Prepared state.
    //
    gc_class_prepared(this, m_vtable);
    assert(m_state == ST_InstanceSizeComputed);
    m_state = ST_Prepared;
    if (is_array()) {
        m_state = ST_Initialized;
    }
    TRACE2("classloader.prepare","class " << m_name->bytes << " prepared");

    //
    // STEP 20 ::: ASSIGN VALUE to static final fields
    //
    // Generally speaking final value is inlined, so we wouldn’t need to worry
    // about the initialization of those static final fields. But when we use
    // reflection mechanisms - Field.getXXX() - to access them, we got
    // null values. Considering this, we must initialize those static
    // final fields. Also related to this is Binary Compatibility chapter
    // section 13.4.8 of the JLS
    //
    tmn_suspend_disable();
    if(!assign_values_to_class_static_final_fields(this))
    { 
        //OOME happened
        tmn_suspend_enable();
        return false;
    }
    tmn_suspend_enable();

    //
    // STEP 21 ::: Link java.lang.Class to struct Class
    //
    // VM adds an extra field, 'vm_class', to all instances of
    // java.lang.Class (see an entry in vm_extra_fields).
    // This field is set to point to the corresponding struct Class.
    //
    // The code below stores the offset to that field in the VM environment.
    //
    if(m_name == env->JavaLangClass_String) {
        String* name = env->string_pool.lookup("vm_class");
        String* desc = env->string_pool.lookup("J");
        Field* vm_class_field = lookup_field(name, desc);
        assert(vm_class_field != NULL);
        env->vm_class_offset = vm_class_field->get_offset();
    }

    assert(m_allocated_size == m_vtable->allocated_size);
    assert(m_array_element_size == m_vtable->array_element_size);

    if(!env->InBootstrap())
    {
        autoUnlocker.ForceUnlock();
        assert(hythread_is_suspend_enabled());
        if(jvmti_should_report_event(JVMTI_EVENT_CLASS_PREPARE)) {
            jvmti_send_class_prepare_event(this);
        }
    }
    TRACE2("classloader.prepare", "END class prepare, class name = " << m_name->bytes);

    return true;
} // Class::prepare
