/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
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
 * @file
 * Major interpreter-related definitions.
 */
#include "environment.h"
#include "vm_threads.h"
#include "open/bytecodes.h"
#include "open/vm_util.h"
#include "ini.h"
#include "jvmti_types.h"
#include "vm_log.h"

//#define INTERPRETER_DEEP_DEBUG
/** \def DEBUG(a)
  * \brief Does nothing.*/
#define DEBUG(a)

#ifdef NDEBUG
/** \def DEBUG_BYTECODE(a)
  * \brief If <code>DEBUG</code> is off, does nothing.*/
#  define DEBUG_BYTECODE(a)
#else
/** \def DEBUG_BYTECODE(a)
  * \brief If <code>DEBUG</code> is on, calls TRACE(a).*/
#  define DEBUG_BYTECODE(a) { if (frame.dump_bytecodes)  TRACE(a); }
#endif

/** \def DEBUG_GC(a)
  * \brief Calls <code>TRACE2</code> with the <code>gc_interpreter</code>
  * category.
  */
#define DEBUG_GC(a)             TRACE2("gc_interpreter", a)

/** <code>TRUE</code> if the interpreter enables debug.*/
extern bool interpreter_enable_debug;

/** \def DEBUG_TRACE(a)
  * \brief Calls <code>TRACE2</code> with the <code>folded_interpreter</code>
  * category.*/
#define DEBUG_TRACE(a)          TRACE2("folded_interpreter", a)

/** \def ASSERT_TAGS(a)
  * \brief Does nothing.*/
#define ASSERT_TAGS(a)

/** \def ASSERT_OBJECT(a)
  * \brief Checks the object.*/
#define ASSERT_OBJECT(a)                                       \
    assert((a == 0) ||                                         \
    ( (*((a)->vt()->clss->get_class_handle()))->vt()->clss ==  \
            VM_Global_State::loader_env->JavaLangClass_Class))

#ifndef INTERPRETER_USE_MALLOC_ALLOCATION
/** \def ALLOC_FRAME(sz)
  * \brief Calls <code>alloca(sz)</code>.*/
#define ALLOC_FRAME(sz) alloca(sz)
/** \def FREE_FRAME(ptr)
  * \brief If <code>INTERPRETER_USE_MALLOC_ALLOCATION</code> is on, does
  * nothing.*/
#define FREE_FRAME(ptr)
#else
#define ALLOC_FRAME(sz) m_malloc(sz)
#define FREE_FRAME(ptr) m_free(ptr)
#endif

#if POINTER64
#   define COMPACT_FIELDS
#   define uword uint64
#   define word int64
#else
#   define uword U_32
#   define word I_32
#endif

#if defined(POINTER64) && defined(REFS_USE_COMPRESSED)
#define REF32                // Use compressed references
typedef COMPRESSED_REFERENCE REF;
#else
typedef ManagedObject* REF;  // Use uncompressed references
#endif

#if defined(REF32) || !defined(POINTER64)
#define VAL32 // Value is 32-bit
#endif


// Create uncompressed value
#define MAKEREFVAL(_val_)   (*((ManagedObject**)(&(_val_))))
// Create compressed value
#define MAKECRVAL(_val_)    (*((COMPRESSED_REFERENCE*)(&(_val_))))

// Macros to compress/uncompress references in fiels and arrays
// Note: VM references are references in heap and fields and arrays
//       interpreter references are references in method stack and local vars
#if defined(REFS_USE_COMPRESSED)
// Both VM and interpreter references are compressed
#define STORE_UREF_BY_ADDR(_addr_, _val_)                                    \
    *((COMPRESSED_REFERENCE*)(_addr_)) = compress_reference(_val_)
#define UNCOMPRESS_REF(cref) (uncompress_compressed_reference(MAKECRVAL(cref)))
//----------------------
#elif defined(REFS_USE_UNCOMPRESSED)
// Both VM and interpreter references are uncompressed
#define STORE_UREF_BY_ADDR(_addr_, _val_)                                    \
    *((ManagedObject**)(_addr_)) = (ManagedObject*)(_val_)
#define UNCOMPRESS_REF(cref) ((ManagedObject*)(cref))
//----------------------
#else // for REFS_USE_RUNTIME_SWITCH
// interpreter refs are uncompressed; VM refs can be either
#define STORE_UREF_BY_ADDR(_addr_, _val_)                                   \
    if (REFS_IS_COMPRESSED_MODE) {                                          \
        *((COMPRESSED_REFERENCE*)(_addr_)) = compress_reference(_val_);     \
    } else {                                                                \
        *((ManagedObject**)(_addr_)) = (ManagedObject*)(_val_);             \
    }
#define UNCOMPRESS_REF(cref)    ( REFS_IS_COMPRESSED_MODE ?                 \
            (uncompress_compressed_reference(MAKECRVAL(cref))) :            \
            (MAKEREFVAL(cref)))
//----------------------
#endif

// Macros for compressing/uncompressing referenceses in interpreter's
// method stack and local vars
#ifdef REF32
#define COMPRESS_INTERP(ref)    (compress_reference(ref))
#define UNCOMPRESS_INTERP(cref) (uncompress_compressed_reference(cref))
#define REF_NULL                (MANAGED_NULL)
#else // REF32
#define COMPRESS_INTERP(ref)    ((ManagedObject*)(ref))
#define UNCOMPRESS_INTERP(cref) ((ManagedObject*)(cref))
#define REF_NULL                0
#endif // REF32



/** Defines byte ordering in Value2 in different situations.*/

/** The stack value.
 * @note Values on the java stack are placed in the reversed order, so that the
 *       reversed copy in the function call works correctly.*/
#define s0 1 
#define s1 0 
/** The local value.*/
#define l0 0
/** The local value.*/
#define l1 1
/** The constant value.*/
#define c0 0
/** The constant value.*/
#define c1 1
/** The argument value.*/
#define a0 0
/** The argument value.*/
#define a1 1
/** The array value.*/
#define ar0 0
/** The array value.*/
#define ar1 1
/** The result value.*/
#define res0 1
/** The result value.*/
#define res1 0

/** Holds 32-bit values.*/
union Value {
/** The unsigned integer value.*/
    U_32 u;
/** The integer value.*/
    I_32 i;
/** The float value.*/
    float f;
///** Compressed/uncompressed reference.*/
    REF ref;
};

/** Holds 64-bit values */
union Value2 {
#ifdef VAL32
/** Two 32-bit values */
    Value v[2];
#else
    Value v0;
#endif
/** The long-long value.*/
    int64 i64;
/** The unsigned long-long value */
    uint64 u64;
/** The double value */
    double d;
/** The reference */
    ManagedObject* ref;
};

/** The local variable types.*/
enum {
/** The element of stack or local variables that is not an object.*/
    FLAG_NONE = 0,
/** The container for the return address from a subroutine.*/
    FLAG_RET_ADDR = 2,
/** The containter for an object reference.*/
    FLAG_OBJECT = 3
};

/** The <code>pop_frame</code> states.*/
enum PopFrameState {
/** Indicates that the frame cannot be popped.*/
    POP_FRAME_UNAVAILABLE,
/** Indicates that the frame can be popped.*/
    POP_FRAME_AVAILABLE,
/** Indicates that the frame is being popped.*/
    POP_FRAME_NOW
};

/**
 * @brief %The stack for executing the Java method.
 *
 * This structure contains a set of operations specific for the Java stack.
 */

class Stack {
/** The stack element value.*/
    Value *data;
/** The value to the object reference.*/
    U_8 *refs;
/** The number of elements on the stack.*/
    I_32 index;
/** The stack size.*/
    I_32 size;

    public:
/** The empty constructor.*/
    inline Stack() {}
/** The destructor.*/
    inline ~Stack();

/**
 * Initializes the stack of a method.
 * 
 * @param[in] ptr  - the pointer to the data
 * @param[in] size - the stack size
 */
    inline void init(void *ptr, int size);

/**
 * Returns the reference to the value on the top of the stack.
 * 
 * @param[in] offset - the offset value
 * @return The reference to the value on the top of the stack.
 */
    inline Value& pick(int offset = 0);

/**
 * Sets and resets the value to the object reference.
 * 
 * @param[in] offset - the offset value
 * @return The value to the object reference.
 */
    inline U_8& ref(int offset = 0);
    
/**
 * Only moves the stack pointer.
 * 
 * @param[in] size - the size value
 */
    inline void push(int size = 1);
    
/**
 * Decreases the stack pointer.
 * By default, decreases the pointer by one step or as specified in <i>size</i>.
 *
 * @param[in] size - the required size
 */
    inline void pop(int size = 1);

/**
 * Is similar to pop().
 * Does the same as pop() and clears the type value associated with
 * every cleared stack element via the ref() function.
 *
 * @param[in] size - the required size
 */
    inline void popClearRef(int size = 1);

/**
 * Sets the value of an object of the <code>Long</code> or <code>Double</code> type
 * contained in two adjacent stack elements.
 * 
 * @param[in] idx - the pointer to the stack depth of the <code>Long</code> value
 * @param[in] val - the <code>Long</code> value
 */
    inline void setLong(int idx, Value2 val);

/**
 * Returns the <code>Long</code> value located at the depth specified by <i>idx</i>.
 *
 * @param[in] idx - the value identifier
 * @return The <code>Long</code> value.
 */
    inline Value2 getLong(int idx);

/** Clears the stack.*/
    inline void clear();

/**
 * Returns the size of the allocated stack area by the elements' size.
 * 
 * @param[in] size - the size in elements
 * @return The size of the allocated area.
 */
    static inline int getStorageSize(int size);

/**
 * Returns the number of elements on the stack.
 * 
 * @return The number of elements on the stack.
 */
    inline int getIndex() { return index + 1; }

/**
 * Enumerates references associated with the thread.
 * 
 * @param[in] VM_thread - the pointer to the thread
 */
    friend void interp_enumerate_root_set_single_thread_on_stack(VM_thread*);

/**
 * Enumerates references associated with the thread.
 * 
 * @param[in] ti_env    - the pointer to the jvmti environment
 * @param[in] VM_thread - the pointer to the thread
 */
    friend void interp_ti_enumerate_root_set_single_thread_on_stack(jvmtiEnv* ti_env, VM_thread *thread);
};

/** The storage for local variables of the executed Java method.*/
class Locals {
    // local variable value
    Value *vars;
    // references to the local variable type
    U_8 *refs;
    // locals size
    U_32 varNum;

    public:
/** The empty constructor.*/
    inline Locals() {}
/** The desctructor.*/
    inline ~Locals();

/**
 * Initializes the stack of a method.
 * 
 * @param[in] ptr  - the pointer to the data
 * @param[in] size - the locals size value
 */
    inline void init(void *ptr, U_32 size);

/**
 * Returns the reference to the local variable of the specifie ID.
 * 
 * @param[in] id - the local variable ID
 * @return The reference to the requested local variable.
 */
    inline Value& operator () (U_32 id);

/**
 * Sets the value of an object of the <code>Long</code> or <code>Double</code>
 * type contained in two adjacent elements.
 *
 * @param[in] idx - the local variable number
 * @param[in] val - the local variable value
 */
    inline void setLong(int idx, Value2 val);

/**
 * Returns the value of an object of the <code>Long</code> or <code>Double</code>
 * type contained in two adjacent elements.
 *
 * @param[in] idx - the local variable number
 * @return The requested object value.
 */
    inline Value2 getLong(int idx);

/**
 * Returns the reference to the type of the local variable.
 * 
 * @param[in] idx - the local variable number
 * @return The reference to the local variable type.
 * @sa     FLAG_NONE, FLAG_RET_ADDR, FLAG_OBJECT
 */
    inline U_8& ref(U_32 id);

/**
 * Returns the size of the allocated locals area by its size in elements.
 * 
 * @param[in] size - size of locals area in elements
 * @return The size of the allocated area.
 */
    static inline int getStorageSize(int size);

/**
 * Returns the number of local variables in this object.
 * 
 * @return The number of local variables.*/
    inline U_32 getLocalsNumber() { return varNum; }

/**
 * Enumerates references associated with the thread.
 * 
 * @param[in] VM_thread - the pointer to the thread*/
    friend void interp_enumerate_root_set_single_thread_on_stack(VM_thread*);

/**
 * Enumerates references associated with the thread.
 * 
 * @param[in] ti_env    - the pointer to the jvmti environment
 * @param[in] VM_thread - the pointer to the thread */
    friend void interp_ti_enumerate_root_set_single_thread_on_stack(jvmtiEnv* ti_env, VM_thread *thread);
};

/** The list of functions that listen for the <code>PopFrame</code> event.*/
struct FramePopListener {
/** The pointer to the listener.*/
    void *listener;
/** The next element.*/
    FramePopListener *next;
};

/** The list of monitors locked by this method.*/
struct MonitorList {
/** The pointer to the monitor.*/
    ManagedObject *monitor;
/** The next element.*/
    MonitorList *next;
};

/** The representation of the method being executed.*/
struct StackFrame {
    public:
/** The address of the bytecode being executed.*/
    U_8 *ip;
/** The stack of this method.*/
    Stack stack;
/** The local variables of this method.*/
    Locals locals;
/** The pointer to the structure of this method.*/
    Method *method;
/** The reference to the caller method.*/
    StackFrame *prev;
/** The list of functions listening for the <code>PopFrame</code> event.*/
    FramePopListener *framePopListener;
/** <code>This</code> pointer of the method being executed.*/
    ManagedObject *This;
/** The list of locked monitors.*/
    struct MonitorList *locked_monitors;
/** The auxiliary structure for storing available monitor structures.*/
    struct MonitorList *free_monitors;
/** The method state: whether the JVMTI frame pop can be performed on it.*/
    PopFrameState jvmti_pop_frame;
#ifndef NDEBUG
    bool dump_bytecodes;
#endif
#ifdef INTERPRETER_DEEP_DEBUG
    U_8 last_bytecodes[8];
    int n_last_bytecode;
#endif
/** The <code>Exception</code> object that has been thrown and for which
  * the JVMTI <code>Exception</code> (?) event has been sent.*/
    ManagedObject *exc;
/** The <code>Exception</code> object that has been caught and for which
  * the JVMTI <code>ExceptionCaught</code> (?) event has been sent.*/
    ManagedObject *exc_catch;
};

/**
 * \defgroup Prototypes Prototypes
 */
/*@{*/

/**
 * The function for interpreter breakpoint processing.
 *
 * @param[in] frame - the method ID*/
extern U_8 Opcode_BREAKPOINT(StackFrame& frame);

/**
 * Enumerates references associated with the thread.
 * 
 * @param[in] VM_thread - the pointer to the thread*/
extern void interp_enumerate_root_set_single_thread_on_stack(VM_thread*);

/**
 * Executes the native method.
 *
 * @param[in] method        - the native-method structure pointer
 * @param[out] return_value - the return value pointer
 * @param[in] args          - the method arguments pointer*/
extern void interpreter_execute_native_method(
        Method *method, jvalue *return_value, jvalue *args);

/**
 * Calls the static native method.
 * 
 * @param[in] prevFrame - the previous frame pointer
 * @param[in] frame     - the frame pointer
 * @param[in] method    - the native-method structure pointer*/
extern void interpreterInvokeStaticNative(
        StackFrame& prevFrame, StackFrame& frame, Method *method);

/**
 * Calls the virtual native method.
 * 
 * @param[in] prevFrame - the previous frame pointer
 * @param[in] frame     - the frame pointer
 * @param[in] method    - the method structure pointer*/
extern void interpreterInvokeVirtualNative(
        StackFrame& prevFrame, StackFrame& frame, Method *method, int sz);

/**
 * Executes the method.
 * 
 * @param[in] method        - the method structure pointer
 * @param[out] return_value - the return value pointer
 * @param[in] args          - the method arguments pointer*/
extern void interpreter_execute_method(
        Method *method, jvalue *return_value, jvalue *args);


/**
 * Processes method entry events.
 * 
 * @param[in] method - the method structure pointer*/
void method_entry_callback(Method *method);

/**
 * Processes method exit events.
 * 
 * @param[in] method                  - the method structure pointer
 * @param[in] was_popped_by_exception - if was popped by exception
 * @param[in] ret_val                 - the return value pointer*/
void method_exit_callback(Method *method, bool was_popped_by_exception, jvalue ret_val);

/**
 * Processes method exit events.
 * 
 * @param[in] method - the method structure pointer
 * @param[in] frame  - the frame pointer*/
void method_exit_callback_with_frame(Method *method, StackFrame& frame);

/**
 * Processes the field modification event.
 * 
 * @param[in] field - the field structure pointer
 * @param[in] frame - the frame pointer*/
void putfield_callback(Field *field, StackFrame& frame);

/**
 * Processes the field modification event.
 * 
 * @param[in] field - the field structure pointer
 * @param[in] frame - the frame pointer*/
void getfield_callback(Field *field, StackFrame& frame);

/**
 * Processes the field modification event.
 * 
 * @param[in] field - the field structure pointer
 * @param[in] frame - the frame pointer*/
void putstatic_callback(Field *field, StackFrame& frame);

/**
 * Processes the field modification event.
 * 
 * @param[in] field - the field structure pointer
 * @param[in] frame - the frame pointer*/
void getstatic_callback(Field *field, StackFrame& frame);

/**
 * Processes the frame pop event.
 * 
 * @param[in] l                       - the pointer to the list of functions that
 *                                      listen for the <code>PopFrame</code> event
 * @param[in] method                  - the pointer to the method structure
 * @param[in] was_popped_by_exception - if <code>was_popped_by_exception</code>*/
void frame_pop_callback(FramePopListener *l, Method *method, jboolean was_popped_by_exception);

/**
 * Processes the single step event.
 * 
 * @param[in] frame - the frame pointer*/
void single_step_callback(StackFrame &frame);

/**
 * Finds the exception handler.
 * 
 * @param[in] frame     - the frame pointer
 * @param[in] exception - the exception pointer
 * @param[in] h -       - the pointer to the representation of a catch block in
 *                        a method's code array
 * @return <code>TRUE</code> on success.*/
bool findExceptionHandler(StackFrame& frame, ManagedObject **exception, Handler **h);

/**
 * Loads method handled exceptions.
 * 
 * @param[in] method - the method structure pointer
 * @return <code>TRUE</code> on success.*/
bool load_method_handled_exceptions(Method *m);
/*@}*/

/**
 * \defgroup Inlines Inline Functions
 */
/*@{*/

/**
 * Returns the last stack frame.
 * 
 * @return The last stack frame.*/
static inline StackFrame*
getLastStackFrame() {
    return (StackFrame*)get_thread_ptr()->lastFrame;
}

/**
 * Returns the last stack frame.
 * 
 * @param[in] thread - the thread pointer
 * @return The last stack frame.*/
static inline StackFrame*
getLastStackFrame(VM_thread *thread) {
    return (StackFrame*)(thread->lastFrame);
}
/** The interpreter states.*/
enum interpreter_state {
    INTERP_STATE_STACK_OVERFLOW = 1
};

/**
 * Sets the last stack frame.
 *
 * @param[in] frame - the frame pointer*/
static inline void setLastStackFrame(StackFrame *frame) {
    get_thread_ptr()->lastFrame = frame;
}
/*@}*/
void
Stack::init(void *ptr, int sz) {
    data = (Value*)ptr;
    refs = (U_8*)(data + sz);
    size = sz;
    index = -1;
    for(int i = 0; i < size; i++) refs[i] = 0;
}

Stack::~Stack() {
    FREE_FRAME(data);
}

Locals::~Locals() {
    FREE_FRAME(data);
}

void
Locals::init(void *ptr, U_32 size) {
    vars = (Value*) ptr;
    refs = (U_8*)(vars + size);
    varNum = size;
    for(U_32 i = 0; i < varNum; i++) refs[i] = 0;
}

int
Locals::getStorageSize(int size) {
    return (size * (sizeof(Value) + sizeof(U_8)) + 7) & ~7;
}

Value&
Locals::operator () (U_32 id) {
    assert(id < varNum);
    return vars[id];
}

void
Locals::setLong(int idx, Value2 val) {
#ifdef VAL32
    operator() (idx+l0) = val.v[a0];
    operator() (idx+l1) = val.v[a1];
#else
    operator() (idx+l0) = val.v0;
#endif
}

Value2
Locals::getLong(int idx) {
    Value2 val;
#ifdef VAL32
    val.v[a0] = operator() (idx+l0);
    val.v[a1] = operator() (idx+l1);
#else
    val.v0 = operator() (idx+l0);
#endif
    return val;
}

U_8&
Locals::ref(U_32 id) {
    assert(id < varNum);
    return refs[id];
}

void
Stack::clear() {
    index = -1;
    for(int i = 0; i < size; i++) refs[i] = 0;
}

U_8&
Stack::ref(int offset) {
    assert(index - offset >= 0);
    return refs[index - offset];
}

Value&
Stack::pick(int offset) {
    assert(index - offset >= 0);
    return data[index - offset];
}

void
Stack::setLong(int idx, Value2 val) {
#ifdef VAL32
    pick(idx + s0) = val.v[a0];
    pick(idx + s1) = val.v[a1];
#else
    pick(idx + s0) = val.v0;
#endif
}

Value2
Stack::getLong(int idx) {
    Value2 val;
#ifdef VAL32
    val.v[a0] = pick(idx + s0);
    val.v[a1] = pick(idx + s1);
#else
    val.v0 = pick(idx + s0);
#endif
    return val;
}

void
Stack::pop(int off) {
    index -= off;
    assert(index >= -1);
}

void
Stack::popClearRef(int off) {
    assert(index - off >= -1);
    for(int i = 0; i < off; i++)
        refs[index - i] = FLAG_NONE;
    index -= off;
}

void
Stack::push(int off) {
    index += off;
    assert(index < size);
}

int
Stack::getStorageSize(int size) {
    return (size * (sizeof(Value) + sizeof(U_8)) + 7) & ~7;
}

/** Sets up locals and stack on the C stack.*/
#define SETUP_LOCALS_AND_STACK(frame,method)                   \
    int max_stack = method->get_max_stack();                   \
    frame.stack.init(ALLOC_FRAME(                              \
                Stack::getStorageSize(max_stack)), max_stack); \
    int max_locals = method->get_max_locals();                 \
    frame.locals.init(ALLOC_FRAME(                             \
                Locals::getStorageSize(max_locals)), max_locals)

/** The interpreter jvmti events.*/
enum interpreter_ti_events {
/** The method entry event.*/
    INTERPRETER_TI_METHOD_ENTRY_EVENT = 1,
/** The method exit event.*/
    INTERPRETER_TI_METHOD_EXIT_EVENT  = 2,
/** The single step event.*/
    INTERPRETER_TI_SINGLE_STEP_EVENT  = 4,
/** The pop-frame event.*/
    INTERPRETER_TI_POP_FRAME_EVENT = 8,
/** The field access event.*/
    INTERPRETER_TI_FIELD_ACCESS = 16,
/** The field modification event.*/
    INTERPRETER_TI_FIELD_MODIFICATION = 32,
/** For other events.*/
    INTERPRETER_TI_OTHER = 64 /* EXCEPTION, EXCEPTION_CATCH */
};

/**
 * The global flags' section.
 *
 * Bitwise or of enabled <code>interpreter_ti_events</code>
 */
extern int interpreter_ti_notification_mode;




