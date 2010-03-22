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

//
// exceptions that can be thrown during class resolution:
//
//  (0) LinkageError exceptions during loading and linking
//  (1) ExceptionInInitializerError: class initializer completes
//      abruptly by throwing an exception
//  (2) IllegalAccessError: current class or interface does not have
//      permission to access the class or interface being resolved.
//
//  class resolution can occur indirectly via resolution of other
//  constant pool entries (Fieldref, Methodref, InterfaceMethodref),
//  or directly by the following byte codes:
//
//  (0) anewarray       (pg 162)
//  (1) checkcast       (pg 174)
//  (2) instanceof      (pg 256)
//  (3) multianewarray  (pg 316) 
//      - also throws the linking exception, IllegalAccessError, if 
//      the current class does have permission to access the *base* 
//      class of the resolved array class.
//  (4) new             (pg 318) 
//      - also throws the linking exception, InstantiationError, if 
//      the resolved class is an abstract class or an interface.
//
//  resolution of constants occurs directly by the following byte codes:
//  (0) ldc     (pg 291)
//  (1) ldc_w   (pg 292)
//  (2) ldc2_w  (pg 294)
//
//  of these ldc byte codes, only the ldc and ldc_w can cause exceptions
//  and only when they refer to CONSTANT_String entries.  The only exception
//  possible seems to be the VirtualMachineError exception that happens
//  when the VM runs out of internal resources.
//
//  exceptions that can be thrown during field and method resolution:
//
//  (0) any of the exceptions for resolving classes
//  (1) NoSuchFieldError: referenced field does not exist in specified 
//      class or interface.
//  (2) IllegalAccessError: the current class does not have permission
//      to access the referenced field.
//
//  During resolution of methods that are declared as native, if the code
//  for the native method cannot be found, then the VM throws an 
//  UnsatisfiedLinkError.  Note, that this causes a problem because the
//  code for native methods are usually loaded in by the static initializer
//  code of a class.  Therefore, this condition can only be checked at
//  run-time, when the native method is first called.
//
//  In addition, the byte codes that refer to the constant pool entries
//  can throw the following exceptions:
//
//  (0) IncompatibleClassChangeError: thrown by getfield and putfield 
//      if the field reference is resolved to a static field.  
//  (1) IncompatibleClassChangeError: thrown by getstatic and putstatic
//      if the field reference is resolved to a non-static field.
//  (2) IncompatibleClassChangeError: thrown by invokespecial, 
//      invokeinterface and invokevirtual if the method reference is 
//      resolved to a static method.
//  (3) AbstractMethodError: thrown by invokespecial, invokeinterface
//      and invokevirtual if the method reference is resolved to an 
//      abstract method.
//  (4) IncompatibleClassChangeError: thrown by invokestatic if the
//      method reference is resolved to a non-static method.
//
//  Invokeinterface throws an IncompatibleClassChangeError if no method
//  matching the resolved name and description can be found in the class
//  of the object that is being invoked, or if the method being invoked is
//  a class (static) method.
//

#define LOG_DOMAIN LOG_CLASS_INFO
#include "cxxlog.h"

#include "Class.h"
#include "classloader.h"
#include "environment.h"
#include "compile.h"
#include "exceptions.h"
#include "interpreter.h"

#include "open/bytecodes.h"
#include "open/vm_class_manipulation.h"
#include "open/vm_ee.h"


static void class_report_failure(Class* target, uint16 cp_index, jthrowable exn)
{
    ConstantPool& cp = target->get_constant_pool();
    assert(cp.is_valid_index(cp_index));
    assert(hythread_is_suspend_enabled());
    assert(exn);

    tmn_suspend_disable();
    target->lock();
    if (!cp.is_entry_in_error(cp_index)) {
        // vvv - This should be atomic change
        cp.resolve_as_error(cp_index, exn);
        // ^^^
    }
    target->unlock();
    tmn_suspend_enable();
}


static void class_report_failure(Class* target, uint16 cp_index, 
                                 const char* exnname, std::stringstream& exnmsg)
{
    TRACE2("resolve.testing", "class_report_failure: " << exnmsg.str().c_str());
    jthrowable exn = exn_create(exnname, exnmsg.str().c_str());
    // ppervov: FIXME: should throw OOME
    class_report_failure(target, cp_index, exn);
}


#define CLASS_REPORT_FAILURE(target, cp_index, exnclass, exnmsg)    \
{                                                                   \
    std::stringstream ss;                                           \
    ss << exnmsg;                                                   \
    class_report_failure(target, cp_index, exnclass, ss);           \
}


Class* Class::_resolve_class(Global_Env* env,
                             unsigned cp_index)
{
    assert(hythread_is_suspend_enabled());
    ConstantPool& cp = m_const_pool;

    lock();
    if(cp.is_entry_in_error(cp_index)) {
        TRACE2("resolve.testing", "Constant pool entry " << cp_index << " already contains error.");
        unlock();
        return NULL;
    }

    if(cp.is_entry_resolved(cp_index)) {
        unlock();
        return cp.get_class_class(cp_index);
    }

    const String* classname = cp.get_utf8_string(cp.get_class_name_index(cp_index));
    unlock();

    // load the class in
    Class* other_clss = m_class_loader->LoadVerifyAndPrepareClass(env, classname);
    if(other_clss == NULL)
    {
        // FIXMECL should find out if this is VirtualMachineError
        assert(exn_raised());
        class_report_failure(this, cp_index, exn_get());
        exn_clear();
        return NULL;
    }

    // Check access control:
    //   referenced class should be public,
    //   or referenced class & declaring class are the same,
    //   or referenced class & declaring class are in the same runtime package,
    //   or declaring class not checked
    //   (the last case is needed for certain magic classes,
    //   eg, reflection implementation)
    if(m_can_access_all
        || other_clss->is_public()
        || other_clss == this
        || m_package == other_clss->m_package)
    {
        lock();
        cp.resolve_entry(cp_index, other_clss);
        unlock();
        return other_clss;
    }

    // Check access control for inner classes:
    //   access control checks is the same as for members
    if(strrchr(other_clss->get_name()->bytes, '$') != NULL
        && can_access_inner_class(env, other_clss))
    {
        lock();
        cp.resolve_entry(cp_index, other_clss);
        unlock();
        return other_clss;
    }

    CLASS_REPORT_FAILURE(this, cp_index, "java/lang/IllegalAccessError",
        "from " << get_name()->bytes << " to " << other_clss->get_name()->bytes);
    // IllegalAccessError
    return NULL;
} // Class::_resolve_class


static bool class_can_instantiate(Class* clss, bool _throw)
{
    ASSERT_RAISE_AREA;
    bool fail = clss->is_abstract();
    if(fail && _throw) {
        exn_raise_by_name("java/lang/InstantiationError", clss->get_name()->bytes);
    }
    return !fail;
}


Class* resolve_class_new_env(Global_Env* env, Class* clss,
                          unsigned cp_index, bool raise_exn)
{
    ASSERT_RAISE_AREA;

    Class* new_clss = clss->_resolve_class(env, cp_index);
    if (!new_clss) return NULL;
    bool can_instantiate = class_can_instantiate(new_clss, false);

    if(new_clss && !can_instantiate) {
        return NULL;
    }
    return new_clss;
} // _resolve_class_new

// Can "other_clss" access the field or method "member"?
bool Class::can_access_member(Class_Member *member)
{
    Class* member_clss = member->get_class();
    // check access permissions
    if(member->is_public() || (this == member_clss)) {
        // no problemo
        return true;
    } else if(member->is_private()) {
        // IllegalAccessError
        return false;
    } else if(member->is_protected()) {
        // When a member is protected, it can be accessed by classes
        // in the same runtime package
        if(m_package == member_clss->m_package)
            return true;
        // Otherwise, when this class is not in the same package,
        // the class containing the member (member_clss) must be
        // a superclass of this class
        // ppervov: FIXME: this can be made a method of struct Class
        // smth. like:
        //if(!is_extending_class(member_clss)) {
        //    // IllegalAccessError
        //    return false;
        //}
        //return true;
        Class* c;
        for(c = get_super_class(); c != NULL; c = c->get_super_class()) {
            if(c == member_clss)
                break;
        }
        if(c == NULL) {
            // IllegalAccessError
            return false;
        }
        return true;
    } else {
        // When a member has default (or package private) access,
        // it can only be accessed by classes in the same package
        if(m_package == member_clss->m_package)
            return true;
        return false;
    }
} // Class::can_access_member

inline static bool
is_class_extended_class( Class *super_clss, 
                         Class *check_clss)
{
    for(; super_clss != NULL; super_clss = super_clss->get_super_class())
    {
        if( super_clss->get_class_loader() == check_clss->get_class_loader()
            && super_clss->get_name() == check_clss->get_name() )
        {
            return true;
        }
    }
    return false;
} // is_class_extended_class

inline static Class*
get_enclosing_class( Global_Env *env,
                     Class *klass )
{
    Class *encl_clss = NULL;

    if( strrchr( klass->get_name()->bytes, '$') != NULL )
    {   // it is anonymous class
        // search "this$..." in fields and look for enclosing class
        unsigned index;
        Field *field;
        for( index = 0, field = klass->get_field(index);
             index < klass->get_number_of_fields();
             index++, field = klass->get_field(index) )
        {
            if( strncmp( field->get_name()->bytes, "this$", 5 ) 
                || !(field->get_access_flags() & ACC_FINAL)
                || !field->is_synthetic() ) 
            {
                continue;
            }
            // found self, get signature of enclosing class
            const String* desc = field->get_descriptor();
            // get name of enclosing class
            String* name = env->string_pool.lookup(&desc->bytes[1], desc->len - 2);
            // loading enclosing class
            encl_clss = klass->get_class_loader()->LoadVerifyAndPrepareClass(env, name);
            break;
        }
    }
    return encl_clss;
} // get_enclosing_class

bool Class::can_access_inner_class(Global_Env* env, Class* inner_clss)
{
    // check access permissions
    if (inner_clss->is_public() || (this == inner_clss)) {
        // no problemo
        return true;
    } else if (inner_clss->is_private()) {
        // IllegalAccessError
        return false;
    } else if (inner_clss->is_protected()) {
        // When inner class is protected, it can be accessed by classes 
        // in the same runtime package. 
        if(m_package == inner_clss->m_package)
            return true;

        // array type has the same access as base type
        if (inner_clss->is_array()) {
            inner_clss = inner_clss->get_array_base_class();
        }
        
        // Otherwise, when other_clss is not in the same package, 
        // inner_clss must be a superclass of other_clss.
        for(Class *decl_other_clss = this; decl_other_clss != NULL;)
        {
            for(Class *decl_inner_clss = inner_clss; decl_inner_clss != NULL;)
            {
                if(is_class_extended_class( decl_other_clss, decl_inner_clss ) ) {
                    return true;
                }
                if( !decl_inner_clss->is_inner_class() ) {
                    // class "decl_inner_clss" isn't inner class
                    break;
                } else {
                    // loading declaring class
                    if(Class* decl_inner_clss_res = decl_inner_clss->resolve_declaring_class(env)) {
                        decl_inner_clss = decl_inner_clss_res;
                    } else {
                        break;
                    }
                }
            }
            if( !decl_other_clss->is_inner_class() )
            {
                // class "decl_other_clss" isn't inner class
                decl_other_clss = get_enclosing_class(env, decl_other_clss);
                continue;
            } else {
                // loading declaring class
                if(Class* decl_other_clss_res =
                    decl_other_clss->resolve_declaring_class(env))
                {
                    decl_other_clss = decl_other_clss_res;
                    continue;
                }
            }
            break;
        }
        // IllegalAccessError
        return false;
    } else {
        // When a member has default (or package private) access,
        // it can only be accessed by classes in the same runtime package.
        if(m_package == inner_clss->m_package)
            return true;
        return false;
    }
} // Class::can_access_inner_class


Field* Class::_resolve_field(Global_Env *env, unsigned cp_index)
{
    lock();
    if(m_const_pool.is_entry_in_error(cp_index)) {
        TRACE2("resolve.testing", "Constant pool entry " << cp_index << " already contains error.");
        unlock();
        return NULL;
    }

    if(m_const_pool.is_entry_resolved(cp_index)) {
        unlock();
        return m_const_pool.get_ref_field(cp_index);
    }

    //
    // constant pool entry hasn't been resolved yet
    //
    unsigned other_index = m_const_pool.get_ref_class_index(cp_index);
    unlock();

    //
    // check error condition from resolve class
    //
    Class* other_clss = _resolve_class(env, other_index);
    if(!other_clss) {
        if(m_const_pool.is_entry_in_error(other_index)) {
            class_report_failure(this, cp_index,
                m_const_pool.get_error_cause(other_index));
        } else {
            assert(exn_raised());
        }
        return NULL;
    }

    uint16 name_and_type_index = m_const_pool.get_ref_name_and_type_index(cp_index);
    String* name = m_const_pool.get_name_and_type_name(name_and_type_index);
    String* desc = m_const_pool.get_name_and_type_descriptor(name_and_type_index);
    Field* field = other_clss->lookup_field_recursive(name, desc);
    if(field == NULL)
    {
        //
        // NoSuchFieldError
        //
        CLASS_REPORT_FAILURE(this, cp_index, "java/lang/NoSuchFieldError",
            other_clss->get_name()->bytes << "." << name->bytes
            << " of type " << desc->bytes
            << " while resolving constant pool entry at index "
            << cp_index << " in class " << get_name()->bytes);
        return NULL;
    }

    //
    // check access permissions
    //
    if(!can_access_member(field))
    {
        //
        // IllegalAccessError
        //
        CLASS_REPORT_FAILURE(this, cp_index, "java/lang/IllegalAccessError",
            other_clss->get_name()->bytes << "." << name->bytes
            << " of type " << desc->bytes
            << " while resolving constant pool entry at index "
            << cp_index << " in class " << get_name()->bytes);
        return NULL;
    }
    lock();
    m_const_pool.resolve_entry(cp_index, field);
    unlock();

    return field;
} // Class::_resolve_field


bool field_can_link(Class* clss, Field* field, bool _static, bool putfield, bool _throw)
{
    ASSERT_RAISE_AREA;
    if(_static?(!field->is_static()):(field->is_static())) {
        if(_throw) {
            exn_raise_by_name("java/lang/IncompatibleClassChangeError",
                field->get_class()->get_name()->bytes);
        }
        return false;
    }
    if(putfield && field->is_final()) {
        for(int fn = 0; fn < clss->get_number_of_fields(); fn++) {
            if(clss->get_field(fn) == field) {
                return true;
            }
        }
        if(_throw) {
            unsigned buf_size = clss->get_name()->len +
                field->get_class()->get_name()->len +
                field->get_name()->len + 15;
            char* buf = (char*)STD_ALLOCA(buf_size);
            memset(buf, 0, buf_size);
            sprintf(buf, " from %s to %s.%s", clss->get_name()->bytes,
                field->get_class()->get_name()->bytes,
                field->get_name()->bytes);
            jthrowable exc_object = exn_create("java/lang/IllegalAccessError", buf);
            exn_raise_object(exc_object);
        }
        return false;
    }
    return true;
}

static bool CAN_LINK_FROM_STATIC = true; // can link from putstatic/getstatic
static bool CAN_LINK_FROM_FIELD = false; // can link from putfield/getfield
static bool LINK_WRITE_ACCESS   = true;  // link from putfield/putstatic
static bool LINK_READ_ACCESS = false;    // link from getfield/getstatic
static bool LINK_THROW_ERRORS = true;    // should throw linking exception on error

Field* resolve_static_field_env(Global_Env *env,
                                Class *clss,
                                unsigned cp_index,
                                bool putfield, 
                                bool is_runtume)
 {
    ASSERT_RAISE_AREA;
 
    Field *field = clss->_resolve_field(env, cp_index);
    if(!field) {
        assert(clss->get_constant_pool().is_entry_in_error(cp_index));
        if (is_runtume) {
            exn_raise_object(clss->get_constant_pool().get_error_cause(cp_index));
        }
        return NULL;
     }
    if(!field_can_link(clss, field, CAN_LINK_FROM_STATIC, putfield, is_runtume)) {
        return NULL;
    }
     return field;
 }


Field* resolve_nonstatic_field_env(Global_Env* env,
                                    Class* clss,
                                    unsigned cp_index,
                                    unsigned putfield, 
                                    bool raise_exn)
 {
     ASSERT_RAISE_AREA;
 
    Field *field = clss->_resolve_field(env, cp_index);
    if(!field) {
        assert(clss->get_constant_pool().is_entry_in_error(cp_index));
        if (raise_exn) {
            exn_raise_object(clss->get_constant_pool().get_error_cause(cp_index));
        }
        return NULL;
    }
    if(!field_can_link(clss, field, CAN_LINK_FROM_FIELD, putfield, raise_exn)) {
        return NULL;
    }
    return field;
 } 



Method* Class::_resolve_method(Global_Env* env, unsigned cp_index)
{
    lock();
    if(m_const_pool.is_entry_in_error(cp_index)) {
        TRACE2("resolve.testing", "Constant pool entry " << cp_index << " already contains error.");
        unlock();
        return NULL;
    }

    if(m_const_pool.is_entry_resolved(cp_index)) {
        unlock();
        return m_const_pool.get_ref_method(cp_index);
    }

    //
    // constant pool entry hasn't been resolved yet
    //
    unsigned other_index;
    other_index = m_const_pool.get_ref_class_index(cp_index);
    unlock();

    //
    // check error condition from resolve class
    //
    Class* other_clss = _resolve_class(env, other_index);
    if(!other_clss) {
        if(m_const_pool.is_entry_in_error(other_index)) {
            class_report_failure(this, cp_index, 
                m_const_pool.get_error_cause(other_index));
        } else {
            assert(exn_raised());
        }
        return NULL;
    }

    uint16 name_and_type_index = m_const_pool.get_ref_name_and_type_index(cp_index);
    String* name = m_const_pool.get_name_and_type_name(name_and_type_index);
    String* desc = m_const_pool.get_name_and_type_descriptor(name_and_type_index);

    // CONSTANT_Methodref must refer to a class, not an interface, and
    // CONSTANT_InterfaceMethodref must refer to an interface (vm spec 4.4.2)
    if(m_const_pool.is_methodref(cp_index) && other_clss->is_interface()) {
        CLASS_REPORT_FAILURE(this, cp_index, "java/lang/IncompatibleClassChangeError",
            other_clss->get_name()->bytes
            << " while resolving constant pool entry " << cp_index
            << " in class " << m_name->bytes);
        return NULL;
    }

    if(m_const_pool.is_interfacemethodref(cp_index) && !other_clss->is_interface()) {
        CLASS_REPORT_FAILURE(this, cp_index, "java/lang/IncompatibleClassChangeError",
            other_clss->get_name()->bytes
            << " while resolving constant pool entry " << cp_index
            << " in class " << m_name->bytes);
        return NULL;
    }

    Method* method = class_lookup_method_recursive(other_clss, name, desc);
    if(method == NULL) {
        // NoSuchMethodError
        CLASS_REPORT_FAILURE(this, cp_index, "java/lang/NoSuchMethodError",
            other_clss->get_name()->bytes << "." << name->bytes << desc->bytes
            << " while resolving constant pool entry at index " << cp_index
            << " in class " << m_name->bytes);
        return NULL;
    }

    if(method->is_abstract() && !other_clss->is_abstract()) {
        // AbstractMethodError
        CLASS_REPORT_FAILURE(this, cp_index, "java/lang/AbstractMethodError",
            other_clss->get_name()->bytes << "." << name->bytes << desc->bytes
            << " while resolving constant pool entry at index " << cp_index
            << " in class " << m_name->bytes);
        return NULL;
    }

    //
    // check access permissions
    //
    if(!can_access_member(method)) {
        // IllegalAccessError
        CLASS_REPORT_FAILURE(this, cp_index, "java/lang/IllegalAccessError",
            other_clss->get_name()->bytes << "." << name->bytes << desc->bytes
            << " while resolving constant pool entry at index " << cp_index
            << " in class " << m_name->bytes);
        return NULL; 
    }

    lock();
    m_const_pool.resolve_entry(cp_index, method);
    unlock();

    return method;
} //_resolve_method


static bool method_can_link_static(Class* clss, unsigned index, Method* method, bool _throw) {
    ASSERT_RAISE_AREA;

    if (!method->is_static()) {
        if(_throw) {
            exn_raise_by_name("java/lang/IncompatibleClassChangeError",
                method->get_class()->get_name()->bytes);
        }
        return false;
    }
    return true;
}

Method* resolve_static_method_env(Global_Env *env,
                                Class *clss,
                                unsigned cp_index, 
                                bool raise_exn)
{
    ASSERT_RAISE_AREA;
 
    Method* method = clss->_resolve_method(env, cp_index);
    if(!method) {
        assert(clss->get_constant_pool().is_entry_in_error(cp_index));
        if (raise_exn) {
            exn_raise_object(clss->get_constant_pool().get_error_cause(cp_index));
        }
        return NULL;
    }

    if (!method_can_link_static(clss, cp_index, method, raise_exn)) {
        return NULL;
    }
    return method;
}


static bool method_can_link_virtual(Class* clss, unsigned cp_index, Method* method, bool _throw)
{
    ASSERT_RAISE_AREA;

    if(method->is_static()) {
        if(_throw) {
            exn_raise_by_name("java/lang/IncompatibleClassChangeError",
                method->get_class()->get_name()->bytes);
        }
        return false;
    }
    if(method->get_class()->is_interface()) {
        if(_throw) {
            char* buf = (char*)STD_ALLOCA(clss->get_name()->len
                + method->get_name()->len + method->get_descriptor()->len + 2);
            sprintf(buf, "%s.%s%s", clss->get_name()->bytes,
                method->get_name()->bytes, method->get_descriptor()->bytes);
            jthrowable exc_object = exn_create("java/lang/AbstractMethodError", buf);
            exn_raise_object(exc_object);
        }
        return false;
    }
    return true;
}


Method* resolve_virtual_method_env(Global_Env *env,
                                    Class *clss,
                                    unsigned cp_index,
                                    bool raise_exn)
{
    Method* method = clss->_resolve_method(env, cp_index);
    if(!method) {
        assert(clss->get_constant_pool().is_entry_in_error(cp_index));
        if (raise_exn) {
            exn_raise_object(clss->get_constant_pool().get_error_cause(cp_index));
        }
        return NULL;
    }

    if (!method_can_link_virtual(clss, cp_index, method, raise_exn)) {
        return NULL;
    }
    return method;
}


static bool method_can_link_interface(Class* clss, unsigned cp_index, Method* method, bool _throw) {
    return true;
}


Method* resolve_interface_method_env(Global_Env *env,
                                    Class *clss,
                                    unsigned cp_index, 
                                    bool raise_exn)
 {
    Method* method = clss->_resolve_method(env, cp_index);
    if (!method) {
        assert(clss->get_constant_pool().is_entry_in_error(cp_index));
        if (raise_exn) {
            exn_raise_object(clss->get_constant_pool().get_error_cause(cp_index));
        }
        return NULL;
     }
    if (!method_can_link_interface(clss, cp_index, method, raise_exn)) {
        return NULL;
    }
    return method;
}


//
// resolve constant pool reference to a non-static field
// used for getfield and putfield
//
Field_Handle resolve_nonstatic_field(Compile_Handle h,
                                     Class_Handle c,
                                     unsigned index,
                                     unsigned putfield)
{
    return resolve_nonstatic_field_env(compile_handle_to_environment(h), c, index, putfield, false);
} //resolve_nonstatic_field


//
// resolve constant pool reference to a static field
// used for getstatic and putstatic
//
Field_Handle resolve_static_field(Compile_Handle h,
                                  Class_Handle c,
                                  unsigned index,
                                  unsigned putfield)
{
    return resolve_static_field_env(compile_handle_to_environment(h), c, index, putfield, false);
} //resolve_static_field


Method_Handle resolve_method(Compile_Handle h, Class_Handle ch, unsigned idx)
{
    return ch->_resolve_method(compile_handle_to_environment(h), idx);
}


//
// resolve constant pool reference to a virtual method
// used for invokevirtual
//
Method_Handle resolve_virtual_method(Compile_Handle h,
                                     Class_Handle c,
                                     unsigned index)
{
    return resolve_virtual_method_env(compile_handle_to_environment(h), c, index, false);
} //resolve_virtual_method


static bool method_can_link_special(Class* clss, unsigned index, Method* method, bool _throw)
{
    ASSERT_RAISE_AREA;

    ConstantPool& cp = clss->get_constant_pool();
    unsigned class_idx = cp.get_ref_class_index(index);
    unsigned class_name_idx = cp.get_class_name_index(class_idx);
    String* ref_class_name = cp.get_utf8_string(class_name_idx);

    if(method->get_name() == VM_Global_State::loader_env->Init_String
        && method->get_class()->get_name() != ref_class_name)
    {
        if(_throw) {
            exn_raise_by_name("java/lang/NoSuchMethodError",
                method->get_name()->bytes);
        }
        return false;
    }
    if(method->is_static())
    {
        if(_throw) {
            exn_raise_by_name("java/lang/IncompatibleClassChangeError",
                method->get_class()->get_name()->bytes);
        }
        return false;
    }
    if(method->is_abstract())
    {
        if(_throw) {
            tmn_suspend_enable();
            unsigned buf_size = clss->get_name()->len +
                method->get_name()->len + method->get_descriptor()->len + 5;
            char* buf = (char*)STD_ALLOCA(buf_size);
            memset(buf, 0, buf_size);
            sprintf(buf, "%s.%s%s", clss->get_name()->bytes,
                method->get_name()->bytes, method->get_descriptor()->bytes);
            jthrowable exc_object = exn_create("java/lang/AbstractMethodError", buf);
            exn_raise_object(exc_object);
            tmn_suspend_disable();
        }
        return false;
    }
    return true;
}

//
// resolve constant pool reference to a method
// used for invokespecial
//
Method* resolve_special_method_env(Global_Env *env,
                                    Class_Handle curr_clss,
                                    unsigned index,
                                    bool raise_exn)
{
    ASSERT_RAISE_AREA;

    Method* method = curr_clss->_resolve_method(env, index);
    if(!method) {
        assert(curr_clss->get_constant_pool().is_entry_in_error(index));
        if (raise_exn) {
            exn_raise_object(curr_clss->get_constant_pool().get_error_cause(index));
        }
        return NULL;
    }
    if(curr_clss->is_super()
        && is_class_extended_class(curr_clss->get_super_class(), method->get_class())
        && method->get_name() != env->Init_String)
    {
        Method* result_meth;
        for(Class* clss = curr_clss->get_super_class(); clss; clss = clss->get_super_class())
        {
            result_meth = clss->lookup_method(method->get_name(), method->get_descriptor());
            if(result_meth) {
                method = result_meth;
                break;
            }
        }
    }
    if(method && !method_can_link_special(curr_clss, index, method, raise_exn))
        return NULL;
    return method;
} //resolve_special_method_env


//
// resolve constant pool reference to a method
// used for invokespecial
//
Method_Handle resolve_special_method(Compile_Handle h,
                                     Class_Handle c,
                                     unsigned index)
{
    return resolve_special_method_env(compile_handle_to_environment(h), c, index, false);
} //resolve_special_method



//
// resolve constant pool reference to a static method
// used for invokestatic
//
Method_Handle resolve_static_method(Compile_Handle h,
                                    Class_Handle c,
                                    unsigned index) 
{
    return resolve_static_method_env(compile_handle_to_environment(h), c, index, false);
} //resolve_static_method



//
// resolve constant pool reference to a method
// used for invokeinterface
//
Method_Handle resolve_interface_method(Compile_Handle h,
                                       Class_Handle c,
                                       unsigned index) 
{
    return resolve_interface_method_env(compile_handle_to_environment(h), c, index, false);
} //resolve_interface_method


//
// resolve constant pool reference to a class
// used for
//      (1) new 
//              - InstantiationError exception if resolved class is abstract
//      (2) anewarray
//      (3) checkcast
//      (4) instanceof
//      (5) multianewarray
//
// resolve_class_new is used for resolving references to class entries by the
// the new byte code.
//
Class_Handle resolve_class_new(Compile_Handle h,
                               Class_Handle c,
                               unsigned index) 
{
    return resolve_class_new_env(compile_handle_to_environment(h), c, index, false);
} //resolve_class_new



//
// resolve_class is used by all the other byte codes that reference classes.
//
Class_Handle resolve_class(Compile_Handle h,
                           Class_Handle c,
                           unsigned index) 
{
    return c->_resolve_class(compile_handle_to_environment(h), index);
} //resolve_class

BOOLEAN class_cp_is_entry_resolved(Class_Handle clazz, U_16 cp_index) {
    ConstantPool& cp = clazz->get_constant_pool();
    bool res = cp.is_entry_resolved(cp_index);
    if (!res) {
        unsigned char tag = cp.get_tag(cp_index);
        //during the loading of a class not all items in it's constant pool are updated
        if (tag == CONSTANT_Fieldref || tag == CONSTANT_Methodref  
            || tag == CONSTANT_InterfaceMethodref || tag == CONSTANT_Class)
        {
            uint16 typeIndex = tag == CONSTANT_Class ? cp_index : cp.get_ref_class_index(cp_index);
            res = cp.is_entry_resolved(typeIndex);
            if (!res) {
                // the type is not marked as loaded in local constant pool
                // ask classloader directly
                uint16 nameIdx = cp.get_class_name_index(typeIndex);
                String* typeName = cp.get_utf8_string(nameIdx);
                assert(typeName!=NULL);
                Class* type = clazz->get_class_loader()->LookupClass(typeName);
                if (type) {
                    /*TODO: uncommenting this code lead to a crash in StressLoader test
                    clazz->lock();
                    cp.resolve_entry(typeIndex, type);
                    clazz->unlock();*/
                    res = true;
                }

                //if array of primitives -> return true;
                if (*typeName->bytes=='[' && !strchr(typeName->bytes, 'L')) {
                    return true;
                }
            }

        } 
    }
    return res;
}


void class_throw_linking_error(Class_Handle ch, unsigned index, unsigned opcode)
{
    ASSERT_RAISE_AREA;
    tmn_suspend_enable();

    ConstantPool& cp = ch->get_constant_pool();
    if(cp.is_entry_in_error(index)) {
        exn_raise_object(cp.get_error_cause(index));
        tmn_suspend_disable();
        return; // will return in interpreter mode
    }

    switch(opcode) {
        case OPCODE_NEW:
            class_can_instantiate(cp.get_class_class(index), LINK_THROW_ERRORS);
            break;
        case OPCODE_PUTFIELD:
            field_can_link(ch, cp.get_ref_field(index),
                CAN_LINK_FROM_FIELD, LINK_WRITE_ACCESS, LINK_THROW_ERRORS);
            break;
        case OPCODE_GETFIELD:
            field_can_link(ch, cp.get_ref_field(index),
                CAN_LINK_FROM_FIELD, LINK_READ_ACCESS, LINK_THROW_ERRORS);
            break;
        case OPCODE_PUTSTATIC:
            field_can_link(ch, cp.get_ref_field(index),
                CAN_LINK_FROM_STATIC, LINK_WRITE_ACCESS, LINK_THROW_ERRORS);
            break;
        case OPCODE_GETSTATIC:
            field_can_link(ch, cp.get_ref_field(index),
                CAN_LINK_FROM_STATIC, LINK_READ_ACCESS, LINK_THROW_ERRORS);
            break;
        case OPCODE_INVOKEINTERFACE:
            method_can_link_interface(ch, index, cp.get_ref_method(index),
                LINK_THROW_ERRORS);
            break;
        case OPCODE_INVOKESPECIAL:
            method_can_link_special(ch, index, cp.get_ref_method(index),
                LINK_THROW_ERRORS);
            break;
        case OPCODE_INVOKESTATIC:
            method_can_link_static(ch, index, cp.get_ref_method(index),
                LINK_THROW_ERRORS);
            break;
        case OPCODE_INVOKEVIRTUAL:
            method_can_link_virtual(ch, index, cp.get_ref_method(index),
                LINK_THROW_ERRORS);
            break;
        default:
            // FIXME Potentially this can be any RuntimeException or Error
            // The most probable case is OutOfMemoryError.
            LWARN(5, "**Java exception occured during resolution under compilation");
            exn_raise_object(VM_Global_State::loader_env->java_lang_OutOfMemoryError);
            //ASSERT(0, "Unexpected opcode: " << opcode);
            break;
    }
    tmn_suspend_disable();
}

Class* resolve_class_array_of_class(Global_Env* env, Class* cc)
{
    // If the element type is primitive, return one of the preloaded
    // classes of arrays of primitive types.
    if (cc->is_primitive()) {
        if (cc == env->Boolean_Class) {
            return env->ArrayOfBoolean_Class;
        } else if (cc == env->Byte_Class) {
            return env->ArrayOfByte_Class;
        } else if (cc == env->Char_Class) {
            return env->ArrayOfChar_Class;
        } else if (cc == env->Short_Class) {
            return env->ArrayOfShort_Class;
        } else if (cc == env->Int_Class) {
            return env->ArrayOfInt_Class;
        } else if (cc == env->Long_Class) {
            return env->ArrayOfLong_Class;
        } else if (cc == env->Float_Class) {
            return env->ArrayOfFloat_Class;
        } else if (cc == env->Double_Class) {
            return env->ArrayOfDouble_Class;
        }
    }

    char *array_name = (char *)STD_MALLOC(cc->get_name()->len + 5);
    if(cc->get_name()->bytes[0] == '[') {
        sprintf(array_name, "[%s", cc->get_name()->bytes);
    } else {
        sprintf(array_name, "[L%s;", cc->get_name()->bytes);
    }
    String *arr_str = env->string_pool.lookup(array_name);
    STD_FREE(array_name);

    Class* arr_clss = cc->get_class_loader()->LoadVerifyAndPrepareClass(env, arr_str);

    return arr_clss;
} // resolve_class_array_of_class

//
// Given a class handle cl construct a class handle of the type
// representing array of cl.
//
Class_Handle class_get_array_of_class(Class_Handle cl)
{
    Global_Env *env = VM_Global_State::loader_env;
    Class *arr_clss = resolve_class_array_of_class(env, cl);
    assert(arr_clss || exn_raised());

    return arr_clss;
} // class_get_array_of_class


static bool resolve_const_pool_item(Global_Env* env, Class* clss, unsigned cp_index)
{
    ConstantPool& cp = clss->get_constant_pool();

    if(cp.is_entry_resolved(cp_index))
        return true;

    switch(cp.get_tag(cp_index)) {
        case CONSTANT_Class:
            return clss->_resolve_class(env, cp_index);
        case CONSTANT_Fieldref:
            return clss->_resolve_field(env, cp_index);
        case CONSTANT_Methodref:
            return clss->_resolve_method(env, cp_index);
        case CONSTANT_InterfaceMethodref:
            return clss->_resolve_method(env, cp_index);
        case CONSTANT_NameAndType: // fall through
        case CONSTANT_Utf8:
            return true;
        case CONSTANT_String: // fall through
        case CONSTANT_Float: // fall through
        case CONSTANT_Integer:
            return true;
        case CONSTANT_Double: // fall through
        case CONSTANT_Long:
        case CONSTANT_UnusedEntry:
            return true;
    }
    return false;
}


/**
 * Resolve whole constant pool
 */
int resolve_const_pool(Global_Env& env, Class *clss) {
    ConstantPool& cp = clss->get_constant_pool();

    // It's possible that cp is null when defining class on the fly
    if(!cp.available()) return 0;

    unsigned cp_size = cp.get_size();
    for (unsigned i = 1; i < cp_size; i++) {
        if(!resolve_const_pool_item(&env, clss, i)) {
            return i;
        }
    }
    return 0;
} //resolve_const_pool
