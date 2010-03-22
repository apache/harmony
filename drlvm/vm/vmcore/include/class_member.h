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
#ifndef __CLASS_MEMBER_H__
#define __CLASS_MEMBER_H__

#include "open/rt_types.h"
#include "annotation.h"
#include "Class.h"
#include "vm_java_support.h"

typedef std::vector<Method*> MethodSet;

struct String;
class ByteReader;
class JIT;
class CodeChunkInfo;
struct Global_Env;

///////////////////////////////////////////////////////////////////////////////
// class file attributes
///////////////////////////////////////////////////////////////////////////////
enum Attributes {
    ATTR_SourceFile,            // Class (no more than 1 in each class file)
    ATTR_InnerClasses,          // Class
    ATTR_ConstantValue,         // Field (no more than 1 for each field)
    ATTR_Code,                  // Method
    ATTR_Exceptions,            // Method
    ATTR_LineNumberTable,       // Code
    ATTR_LocalVariableTable,    // Code
    ATTR_Synthetic,             // Class/Field/Method
    ATTR_Deprecated,            // Class/Field/Method
    ATTR_SourceDebugExtension,  // Class (no more than 1 in each class file)
    ATTR_Signature,             // Class/Field/Method (spec does not limit number???)
    ATTR_EnclosingMethod,       // Class (1 at most)
    ATTR_LocalVariableTypeTable,    // Code
    ATTR_RuntimeVisibleAnnotations,             // Class/Field/Method (at most 1 per entity)
    ATTR_RuntimeInvisibleAnnotations,           // Class/Field/Method
    ATTR_RuntimeVisibleParameterAnnotations,    // Method
    ATTR_RuntimeInvisibleParameterAnnotations,  // Method
    ATTR_AnnotationDefault,     // Method (spec does not limit number???)
    ATTR_StackMapTable,         // Method
    N_ATTR,
    ATTR_UNDEF,
    ATTR_ERROR
};

///////////////////////////////////////////////////////////////////////////////
// A class' members are its fields and methods.  Class_Member is the base
// class for Field and Method, and factors out the commonalities in these
// two classes.
///////////////////////////////////////////////////////////////////////////////
class Class_Member {
public:
    //
    // access modifiers
    //
    bool is_public()            {return (_access_flags&ACC_PUBLIC)?true:false;}
    bool is_private()           {return (_access_flags&ACC_PRIVATE)?true:false;}
    bool is_protected()         {return (_access_flags&ACC_PROTECTED)?true:false;}
    bool is_package_private()   {return !(is_public()||is_protected()||is_public())?true:false;}
    bool is_static()            {return (_access_flags&ACC_STATIC)?true:false;}
    bool is_final()             {return (_access_flags&ACC_FINAL)?true:false;}
    bool is_strict()            {return (_access_flags&ACC_STRICT)?true:false;}
    bool is_synthetic()         {return (_access_flags&ACC_SYNTHETIC)?true:_synthetic;}
    bool is_deprecated()        {return _deprecated;}
    unsigned get_access_flags() {return _access_flags;}

    //
    // field get/set methods
    //
    unsigned get_offset() const {return _offset;}
    Class *get_class() const    {return _class;}
    String *get_name() const    {return _name;}

    // Get the type descriptor (Sec. 4.3.2)
    String *get_descriptor() const {return _descriptor;}
    String *get_signature() const {return _signature;}

    AnnotationTable* get_declared_annotations() const {return _annotations;}
    AnnotationTable* get_declared_invisible_annotations() const {
        return _invisible_annotations;
    }

    friend void assign_offsets_to_class_fields(Class *);
    friend void add_new_fake_method(Class *clss, Class *example, unsigned *next);
    friend void add_any_fake_methods(Class *);

    /**
     * Allocate a memory from a class loader pool using the class
     * loader lock.
     */
    void* Alloc(size_t size);

protected:
    Class_Member()
    {
        _access_flags = 0;
        _class = NULL;
        _offset = 0;
#ifdef VM_STATS
        num_accesses = 0;
        num_slow_accesses = 0;
#endif
        _synthetic = _deprecated = false;
        _annotations = NULL;
        _invisible_annotations = NULL;
        _signature = NULL;
    }

    // offset of class member; 
    //   for virtual  methods, the method's offset within the vtable
    //   for static   methods, not used, always zero
    //   for instance data,    offset within the instance's data block
    //   for static   data,    offset within the class' static data block
    unsigned _offset;

    bool _synthetic;
    bool _deprecated;
    AnnotationTable* _annotations;
    AnnotationTable* _invisible_annotations;

    uint16 _access_flags;
    String* _name;
    String* _descriptor;
    String* _signature;
    Class* _class;

    bool parse(Class* clss, ByteReader& cfs);

public:
#ifdef VM_STATS
    uint64 num_accesses;
    uint64 num_slow_accesses;
#endif
}; // Class_Member



///////////////////////////////////////////////////////////////////////////////
// Fields within Class structures.
///////////////////////////////////////////////////////////////////////////////
struct Field : public Class_Member{
public:
    //-----------------------

    // For all fields
    bool is_offset_computed() { return (_offset_computed != 0); }
    void set_offset(unsigned off) { _offset = off; _offset_computed = 1; }

    // For static fields
    void* get_address();

    // Return the type of this field.
    Java_Type get_java_type() {
        return (Java_Type)(get_descriptor()->bytes[0]);
    };

    Const_Java_Value get_const_value() { return const_value; };
    uint16 get_const_value_index() { return _const_value_index; };

    //-----------------------

    Field() {
        _const_value_index = 0;
        _field_type_desc = 0;
        _offset_computed = 0;
        _is_injected = 0;
        _is_magic_type = 0;
        track_access = 0;
        track_modification = 0;
    }

    void Reset() { }

    void set(Class *cl, String* name, String* desc, unsigned short af) {
        _class = cl; _access_flags = af; _name = name; _descriptor = desc;
    }
    Field& operator = (const Field& fd) {
        // copy Class_Member fields
        _access_flags = fd._access_flags;
        _class = fd._class;
        _offset = fd._offset;
        _name = fd._name;
        _descriptor = fd._descriptor;
        _deprecated = fd._deprecated;
        _synthetic = fd._synthetic;
        _annotations = fd._annotations;
        _signature = fd._signature;

        // copy Field fields
        _const_value_index = fd._const_value_index;
        _field_type_desc = fd._field_type_desc;
        _is_injected = fd._is_injected;
        _is_magic_type = fd._is_magic_type;
        _offset_computed = fd._offset_computed;
        const_value = fd.const_value;
        track_access = fd.track_access;
        track_modification = fd.track_modification;
        
        return *this;
    }
    //
    // access modifiers
    //
    unsigned is_volatile()  {return (_access_flags&ACC_VOLATILE);} 
    unsigned is_transient() {return (_access_flags&ACC_TRANSIENT);} 
    bool is_enum()          {return (_access_flags&ACC_ENUM)?true:false;} 
    
    bool parse(Global_Env& env, Class* clss, ByteReader& cfs, bool is_trusted_cl);

    unsigned calculate_size();

    TypeDesc* get_field_type_desc() { return _field_type_desc; }
    void set_field_type_desc(TypeDesc* td) { _field_type_desc = td; }

    Boolean is_injected() {return _is_injected;}
    void set_injected() { _is_injected = 1; }

    Boolean is_magic_type()    {return  _is_magic_type;}
    
    void set_track_access(bool value) {
        track_access = value ? 1 : 0 ;
    }

    void set_track_modification(bool value) {
        track_modification = value ? 1 : 0 ;
    }

    void get_track_access_flag(char** address, char* mask) {
        *address = &track_access;
        *mask = TRACK_ACCESS_MASK;
    }

    void get_track_modification_flag(char** address, char* mask) {
        *address = &track_modification;
        *mask = TRACK_MODIFICATION_MASK;
    }

private:
    //
    // The initial values of static fields.  This is defined by the
    // ConstantValue attribute in the class file.
    //
    // If there was not ConstantValue attribute for that field then _const_value_index==0
    //
    uint16 _const_value_index;
    Const_Java_Value const_value;
    TypeDesc* _field_type_desc;
    unsigned _is_injected : 1;
    unsigned _is_magic_type : 1;
    unsigned _offset_computed : 1;

    /** Turns on sending FieldAccess events on access to this field */
    char track_access;
    const static char TRACK_ACCESS_MASK = 1;

    /** Turns on sending FieldModification events on modification of this field */
    char track_modification;
    const static char TRACK_MODIFICATION_MASK = 1;

    //union {
    //    char bit_flags;
    //    struct {

    //        /** Turns on sending FieldAccess events on access to this field */
    //        char track_access : 1;
    //        const static char TRACK_ACCESS_MASK = 4;

    //        /** Turns on sending FieldModification events on modification of this field */
    //        char track_modification : 1;
    //        const static char TRACK_MODIFICATION_MASK = 8;
    //    };
    //};
}; // Field



///////////////////////////////////////////////////////////////////////////////
// Handler represents a catch block in a method's code array
///////////////////////////////////////////////////////////////////////////////
class Handler {
public:
    Handler();
    bool parse(Class* clss, unsigned code_length, ByteReader& cfs, Method* method);
    uint16 get_start_pc() {return _start_pc;}
    uint16 get_end_pc() {return _end_pc;}
    uint16 get_handler_pc() {return _handler_pc;}
    uint16 get_catch_type_index() {return _catch_type_index;}

private:
    uint16 _start_pc;
    uint16 _end_pc;
    uint16 _handler_pc;
    uint16 _catch_type_index;
    String* _catch_type;
}; //Handler



// Representation of target handlers in the generated code.
class Target_Exception_Handler {
public:
    Target_Exception_Handler(NativeCodePtr start_ip, NativeCodePtr end_ip, NativeCodePtr handler_ip, Class_Handle exn_class, bool exn_is_dead);

    NativeCodePtr get_start_ip();
    NativeCodePtr get_end_ip();
    NativeCodePtr get_handler_ip();
    Class_Handle  get_exc();
    bool          is_exc_obj_dead();

    bool is_in_range(NativeCodePtr eip, bool is_ip_past);
    bool is_assignable(Class_Handle exn_class);

    void update_catch_range(NativeCodePtr new_start_ip, NativeCodePtr new_end_ip);
    void update_handler_address(NativeCodePtr new_handler_ip);

private:
    NativeCodePtr _start_ip;
    NativeCodePtr _end_ip;
    NativeCodePtr _handler_ip;
    Class_Handle _exc;
    bool _exc_obj_is_dead;
}; //Target_Exception_Handler

typedef class Target_Exception_Handler* Target_Exception_Handler_Ptr;

#define MAX_VTABLE_PATCH_ENTRIES 10

class VTable_Patches {
public:
    void *patch_table[MAX_VTABLE_PATCH_ENTRIES];
    VTable_Patches *next;
};

// Used to notify interested JITs whenever a method is changed: overwritten, recompiled,
// or initially compiled.
struct Method_Change_Notification_Record {
    Method *caller;
    JIT    *jit;
    void   *callback_data;
    Method_Change_Notification_Record *next;

    inline bool equals(JIT *jit_, void *callback_data_) {
        if ((callback_data == callback_data_) &&
            (jit == jit_)) {
            return true;
        }
        return false;
    }
};


// 20020222 This is only temporary to support the new JIT interface.
// We will reimplement the signature support.
struct Method_Signature {
public:
    TypeDesc* return_type_desc;
    unsigned num_args;
    TypeDesc** arg_type_descs;
    Method *method;
    String *sig;


    void initialize_from_method(Method *method);
    void reset();

private:
    void initialize_from_java_method(Method *method);
};


///////////////////////////////////////////////////////////////////////////////
// Methods defined in a class.
///////////////////////////////////////////////////////////////////////////////

struct Line_Number_Entry {
    uint16 start_pc;
    uint16 line_number;
};

struct Line_Number_Table {
    uint16 length;
    Line_Number_Entry table[1];
};

struct Local_Var_Entry {
    uint16 start_pc;
    uint16 length;
    uint16 index;
    String* name;
    String* type;
    String* generic_type;
};

struct Local_Var_Table {
    uint16 length;
    Local_Var_Entry table[1];
};

class InlineInfo;

struct Method : public Class_Member {
    friend void add_new_fake_method(Class* clss, Class* example, unsigned* next);
    friend void add_any_fake_methods(Class* clss);
    //-----------------------
public:
    //
    // state of this method
    //
    enum State {
        ST_NotCompiled,                 // initial state
        ST_NotLinked = ST_NotCompiled,  // native not linked to implementation
        ST_Compiled,                    // compiled by JIT
        ST_Linked = ST_Compiled         // native linked to implementation
    };
    State get_state()                   {return _state;}
    void set_state(State st)            {_state=st;}
    
    struct LocalVarOffset{
        int value;
        LocalVarOffset* next;
    }; 

    // "Bytecode" exception handlers, i.e., those from the class file
    unsigned num_bc_exception_handlers() const { return _n_handlers; }
    Handler* get_bc_exception_handler_info(unsigned eh_number) {
        assert(eh_number < _n_handlers);
        return _handlers + eh_number;
    }

    // "Target" exception handlers, i.e., those in the code generated by the JIT.
    void set_num_target_exception_handlers(JIT *jit, unsigned n);
    unsigned get_num_target_exception_handlers(JIT *jit);

    // Arguments:
    //  ...
    //  catch_clss  -- class of the exception or null (for "catch-all")
    //  ...
    void set_target_exception_handler_info(JIT *jit,
                                           unsigned eh_number,
                                           void *start_ip,
                                           void *end_ip,
                                           void *handler_ip,
                                           Class *catch_clss,
                                           bool exc_obj_is_dead = false);

    Target_Exception_Handler_Ptr get_target_exception_handler_info(JIT *jit, unsigned eh_num);

    unsigned num_exceptions_method_can_throw();
    String *get_exception_name (int n);

    // Address of the memory block containing bytecodes.  For best performance
    // the bytecodes should not be destroyed even after the method has been
    // jitted to allow re-compilation.  However the interface allows for such
    // deallocation.  The effect would be that re-optimizing JITs would not
    // show their full potential, but that may be acceptable for low-end systems
    // where memory is at a premium.
    // The value returned by getByteCodeAddr may be NULL in which case the
    // bytecodes are not available (presumably they have been garbage collected by VM).
    const U_8*   get_byte_code_addr()   {return _byte_codes;}
    unsigned     get_byte_code_size()   {return _byte_code_length;}

    // From the class file (Sec. 4.7.4)
    unsigned get_max_stack()                       { return _max_stack; }
    unsigned get_max_locals()                      { return _max_locals; }

    // Returns an iterator for the argument list.
    Arg_List_Iterator get_argument_list();

    // Returns number of bytes of arguments pushed on the stack.
    // This value depends on the descriptor and the calling convention.
    unsigned get_num_arg_slots() const { return _arguments_slot_num; }

    // Returns number of arguments.  For non-static methods, the this pointer
    // is included in this number
    unsigned get_num_args();

    // Number of arguments which are references.
    unsigned get_num_ref_args();

    // Return the return type of this method.
    Java_Type get_return_java_type() {
        const char *descr = get_descriptor()->bytes;
        while(*descr != ')') descr++;
        return (Java_Type)*(descr + 1);
    }

    // For non-primitive types (i.e., classes) get the class type information.
    Class *get_return_class_type();

    // Address of the memory location containing the address of the code.
    // Used for static and special methods which have been resolved but not jitted.
    // The call would be:
    //      call dword ptr [addr]
    void *get_indirect_address()                   { return &_code; }

    // Entry address of the method.  Points to an appropriate stub or directly
    // to the code if no stub is necessary.
    void *get_code_addr()                          { return _code; }
    void set_code_addr(void *code_addr)            { _code = code_addr; }

    void add_vtable_patch(void *);
    void apply_vtable_patches();

    NativeCodePtr get_registered_native_func() 
            { return _registered_native_func; }

    void set_registered_native_func(NativeCodePtr native_func) 
            { _registered_native_func = native_func; }

    /**
     * This returns a block for jitted code. It is not used for native methods.
     * It is safe to call this function from multiple threads.
     */
    void *allocate_code_block_mt(size_t size, size_t alignment, JIT *jit, unsigned heat,
        int id, Code_Allocation_Action action);

    void *allocate_rw_data_block(size_t size, size_t alignment, JIT *jit);

    // The JIT can store some information in a JavaMethod object.
    void *allocate_jit_info_block(size_t size, JIT *jit);

    // JIT-specific data blocks.
    // Access should be protected with _lock.
    // FIXME
    // Think about moving lock aquisition inside public methods.
    void *allocate_JIT_data_block(size_t size, JIT *jit, size_t alignment);
    CodeChunkInfo *get_first_JIT_specific_info()   { return _jits; };
    CodeChunkInfo *get_JIT_specific_info_no_create(JIT *jit);
    /**
     * Find a chunk info for specific JIT. If no chunk exist for this JIT,
     * create and return one. This method is safe to call
     * from multiple threads.
     */
    CodeChunkInfo *get_chunk_info_mt(JIT *jit, int id);

    /**
     * Find a chunk info for specific JIT, or <code>NULL</code> if
     * no chunk info is created for this JIT. This method is safe to call
     * from multiple threads.
     */
    CodeChunkInfo *get_chunk_info_no_create_mt(JIT *jit, int id);

    /**
     * Allocate a new chunk info. This method is safe to call
     * from multiple threads.
     */
    CodeChunkInfo *create_code_chunk_info_mt();

    // Notify JITs whenever this method is recompiled or initially compiled.
    void register_jit_recompiled_method_callback(JIT *jit_to_be_notified, Method* caller, void *callback_data);
    void do_jit_recompiled_method_callbacks();
    void unregister_jit_recompiled_method_callbacks(const Method* caller);

    Method_Side_Effects get_side_effects()         { return _side_effects; }
    void set_side_effects(Method_Side_Effects mse) { _side_effects = mse; }

    Method_Signature *get_method_sig()             { return _method_sig; }
    void set_method_sig(Method_Signature *msig)    { _method_sig = msig; }

    /// Sets index in vtable and offset from the base of vtable for this method
    /// @param index - index in vtable
    /// @param offset - for instance methods: offset from the base of vtable
    void set_position_in_vtable(unsigned index, unsigned offset) {
        assert(!is_static());
        _index = index;
        _offset = offset;
    }
private:
    State _state;
    void *_code;
    VTable_Patches *_vtable_patch;

    NativeCodePtr _counting_stub;

    CodeChunkInfo *_jits;

    Method_Side_Effects _side_effects;
    Method_Signature *_method_sig;

    /** set by JNI RegisterNatives() funcs */
    NativeCodePtr _registered_native_func;

public:
    Method();
    // destructor should be instead of this function, but it's not allowed to use it because copy for Method class is
    // done with memcpy, and old value is destroyed with delete operator.
    void MethodClearInternals();
    void NotifyUnloading();

    //
    // access modifiers
    //
    bool is_synchronized()  {return (_access_flags&ACC_SYNCHRONIZED)?true:false;} 
    bool is_native()        {return (_access_flags&ACC_NATIVE)?true:false;} 
    bool is_abstract()      {return (_access_flags&ACC_ABSTRACT)?true:false;} 
    bool is_varargs()       {return (_access_flags&ACC_VARARGS)?true:false;} 
    bool is_bridge()        {return (_access_flags&ACC_BRIDGE)?true:false;} 

    // method flags
    bool is_init()          {return _flags.is_init?true:false;}
    bool is_clinit()        {return _flags.is_clinit?true:false;}
    bool is_finalize()      {return _flags.is_finalize?true:false;}
    bool is_overridden()    {return _flags.is_overridden?true:false;}
    Boolean  is_nop()       {return _flags.is_nop;}

    unsigned get_index()    {return _index;}

    // Fake methods are interface methods inherited by an abstract class that are not (directly or indirectly)
    // implemented by that class. They are added to the class to ensure they have thecorrect vtable offset.
    // These fake methods point to the "real" interface method for which they are surrogates; this information
    // is used by reflection methods.
    bool is_fake_method()           {return (_intf_method_for_fake_method != NULL);}
    Method *get_real_intf_method()  {return _intf_method_for_fake_method;}

    bool parse(Global_Env& env, Class* clss, ByteReader& cfs, bool is_trusted_cl);

    void calculate_arguments_slot_num();
    
    unsigned calculate_size() {
        unsigned size = sizeof(Class_Member) + sizeof(Method);
        if(_local_vars_table)
            size += sizeof(uint16) + _local_vars_table->length*sizeof(Local_Var_Entry);
        if(_line_number_table)
            size += sizeof(uint16) + _line_number_table->length*sizeof(Line_Number_Entry);
        size += _n_exceptions*sizeof(String*);
        size += _n_handlers*sizeof(Handler);
        size += _byte_code_length;
        return size;
    }

    unsigned get_num_param_annotations() {return _num_param_annotations;}
    AnnotationTable * get_param_annotations(unsigned index) {
        return index < _num_param_annotations ? _param_annotations[index] : NULL;
    }
    unsigned get_num_invisible_param_annotations() {
        return _num_invisible_param_annotations;
    }
    AnnotationTable * get_invisible_param_annotations(unsigned index) {
        return index < _num_invisible_param_annotations ?
                        _invisible_param_annotations[index] : NULL;
    }
    
    AnnotationValue * get_default_value() {return _default_value; }

private:
    U_8 _num_param_annotations;
    AnnotationTable ** _param_annotations;
    U_8 _num_invisible_param_annotations;
    AnnotationTable ** _invisible_param_annotations; 
    
    AnnotationValue * _default_value;

    unsigned _index;                // index in method table
    unsigned _arguments_slot_num;   // number of slots for method arguments
    uint16 _max_stack;
    uint16 _max_locals;
    uint16 _n_exceptions;           // num exceptions method can throw
    uint16 _n_handlers;             // num exception handlers in byte codes
    String** _exceptions;          // array of exceptions method can throw
    U_32 _byte_code_length;       // num bytes of byte code
    U_8*   _byte_codes;           // method's byte codes
    Handler *_handlers;             // array of exception handlers in code
    Method *_intf_method_for_fake_method;
    struct {
        unsigned is_init        : 1;
        unsigned is_clinit      : 1;
        unsigned is_finalize    : 1;    // is finalize() method
        unsigned is_overridden  : 1;    // has this virtual method been overridden by a loaded subclass?
        unsigned is_nop         : 1;
    } _flags;

    //
    // private methods for parsing methods
    //
    bool _parse_code(Global_Env& env, ConstantPool& cp, unsigned code_attr_len, ByteReader &cfs);

    bool _parse_line_numbers(unsigned attr_len, ByteReader &cfs);

    bool _parse_exceptions(ConstantPool& cp, unsigned attr_len, ByteReader &cfs);

    void _set_nop();

    //
    // debugging info
    //
    Line_Number_Table *_line_number_table;
    Local_Var_Table *_local_vars_table;

    bool _parse_local_vars(Local_Var_Table* table, LocalVarOffset* offset_list,
        Global_Env& env, ConstantPool& cp, ByteReader &cfs, const char* attr_name, Attributes attr);

    // This is the number of breakpoints which should be set in the
    // method when it is compiled. This number does not reflect
    // multiple breakpoints that are set in the same location by
    // different environments, it counts only unique locations
    U_32 pending_breakpoints;

    /** Information about methods inlined to this. */
    InlineInfo* _inline_info;
    

    MethodSet* _recompilation_callbacks;
public:

    /**
     * Gets inlined methods information.
     * @return InlineInfo object pointer.
     */
    InlineInfo* get_inline_info() {
        return _inline_info;
    }

    /**
     * Adds information about inlined method.
     * @param[in] method - method which is inlined
     * @param[in] codeSize - size of inlined code block
     * @param[in] codeAddr - size of inlined code block
     * @param[in] mapLength - number of AddrLocation elements in addrLocationMap
     * @param[in] addrLocationMap - native address to bytecode location
     * correspondence table
     */
    void add_inline_info_entry(Method* method, U_32 codeSize, void* codeAddr,
            U_32 mapLength, AddrLocation* addrLocationMap);

    /**
     * Sends JVMTI_EVENT_COMPILED_METHOD_LOAD event for every inline method 
     * recorded in this InlineInfo object.
     * @param[in] method - outer method this InlineInfo object belogs to.
     */
    void send_inlined_method_load_events(Method *method);

    unsigned get_line_number_table_size() {
        return (_line_number_table) ? _line_number_table->length : 0;
    }

    bool get_line_number_entry(unsigned index, jlong* pc, jint* line);

    unsigned get_local_var_table_size() {
        return (_local_vars_table) ? _local_vars_table->length : 0;
    }

    bool get_local_var_entry(unsigned index, jlong* pc, 
        jint* length, jint* slot, String** name, String** type, 
        String** generic_type);

    // XXX
    //bool get_local_var_entry(unsigned index, jlong* pc,
    //    jint* length, jint* slot, String** name, String** type);


    // Returns number of line in the source file, to which the given bytecode offset
    // corresponds, or -1 if it is unknown.
    int get_line_number(uint16 bc) {
        if(!_line_number_table) return -1;
        Line_Number_Table* lnt = _line_number_table;
        for(int i = 0; i < lnt->length - 1; i++) {
            if(bc >= lnt->table[i].start_pc && bc < lnt->table[i+1].start_pc)
                return lnt->table[i].line_number;
        }
        if(bc >= lnt->table[lnt->length-1].start_pc && bc < _byte_code_length)
            return lnt->table[lnt->length-1].line_number;
        return -1;
    }

    void method_was_overridden();
    // Records JITs to be notified when a method is recompiled or initially compiled.
    Method_Change_Notification_Record *_notify_recompiled_records;

    void lock();
    void unlock();

    U_32 get_pending_breakpoints()
    {
        return pending_breakpoints;
    }

    void insert_pending_breakpoint()
    {
        pending_breakpoints++;
    }

    void remove_pending_breakpoint()
    {
        pending_breakpoints--;
    }


    U_8* m_stackmap;
public:
    U_8* get_stackmap() {
        return m_stackmap;
    }


}; // Method

struct _jmethodID : public Method
{
    // Empty declaration to make jmethodID
    // autoconvertable to struct Method*
};

#endif
