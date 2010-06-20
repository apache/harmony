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

#include "classloader.h"
#include "environment.h"
#include "open/vm_util.h"
#include "open/types.h"
#include "type.h"
#include "vm_core_types.h"

TypeDesc* type_desc_create_from_java_descriptor(const char* d, ClassLoader* loader)
{
    Global_Env* env = VM_Global_State::loader_env;
    switch (*d) 
    {
    case 'B': return env->bootstrap_class_loader->get_primitive_type(K_S1);
    case 'C': return env->bootstrap_class_loader->get_primitive_type(K_Char);
    case 'D': return env->bootstrap_class_loader->get_primitive_type(K_F8);
    case 'F': return env->bootstrap_class_loader->get_primitive_type(K_F4);
    case 'I': return env->bootstrap_class_loader->get_primitive_type(K_S4);
    case 'J': return env->bootstrap_class_loader->get_primitive_type(K_S8);
    case 'S': return env->bootstrap_class_loader->get_primitive_type(K_S2);
    case 'Z': return env->bootstrap_class_loader->get_primitive_type(K_Boolean);
    case 'V': return env->bootstrap_class_loader->get_primitive_type(K_Void);
    case 'L':
        {
            const char* sn = d+1;
            const char* en = sn;
            while (en[0]!=';') {
                en++;
            }
            unsigned len = (unsigned)(en-sn);
            String* str = env->string_pool.lookup(sn, len);

            assert(loader);
            loader->LockTypesCache();
            TypeDesc** tdres = loader->GetJavaTypes()->Lookup(str);
            if (tdres)
            {
                assert (*tdres);
                loader->UnlockTypesCache();
                return *tdres;
            }
            TypeDesc* td = new TypeDesc(K_Object, NULL, NULL, str, loader, NULL);
            assert(td);
            loader->GetJavaTypes()->Insert(str, td);
            loader->UnlockTypesCache();
            return td;
        }
    case '[':
        {
            // descriptor is checked in recursion
            TypeDesc* et = type_desc_create_from_java_descriptor(d+1, loader);
            if( !et ) {
                return NULL;
            }
            return et->type_desc_create_vector();
        }
    default:
        DIE(("Bad type descriptor"));
        return NULL;
    }
}

TypeDesc* type_desc_create_from_java_class(Class* c)
{
    const String* cname = c->get_name();
    if(c->is_array())
        return type_desc_create_from_java_descriptor(cname->bytes, c->get_class_loader());
    assert(!c->is_primitive());
    
    c->get_class_loader()->LockTypesCache();
    TypeDesc** tdres = c->get_class_loader()->GetJavaTypes()->Lookup(cname);
    if (tdres)
    {
        assert (*tdres);
        c->get_class_loader()->UnlockTypesCache();
        return *tdres;
    }
    TypeDesc* td = new TypeDesc(K_Object, NULL, NULL, cname, c->get_class_loader(), c);
    assert(td);
    c->get_class_loader()->GetJavaTypes()->Insert(cname, td);
    c->get_class_loader()->UnlockTypesCache();
    return td;
}

Class *resolve_class_array_of_class(Global_Env *env, Class *cc);

Class* TypeDesc::load_type_desc()
{
    if (clss) return clss; // class already loaded
    Global_Env* env = VM_Global_State::loader_env;
    Class* element_clss;

    switch (get_kind()) {
    case K_S1: return env->Byte_Class;
    case K_S2: return env->Short_Class;
    case K_S4: return env->Int_Class;
    case K_S8: return env->Long_Class;
    case K_F4: return env->Float_Class;
    case K_F8: return env->Double_Class;
    case K_Boolean: return env->Boolean_Class;
    case K_Char: return env->Char_Class;
    case K_Void: return env->Void_Class;
    case K_Object:
        assert (loader);
        assert (name);
        // FIXME: better to use LoadVerifyAndPrepareClass here - but this results in Recursive resolution collision in StartLoadingClass
        //c = loader->LoadVerifyAndPrepareClass(env, name);
        clss = loader->LoadClass(env, name);
        return clss;
    case K_Vector:
        assert (component_type);
        element_clss = component_type->load_type_desc();
        if (!element_clss) return NULL;
        clss = resolve_class_array_of_class(env, element_clss);
        return clss;
    default:
        // All other types are not Java types, so fail
        LDIE(73, "Unexpected kind");
        return NULL;
    }
}

bool TypeDesc::is_loaded()
{
    if (clss != NULL)
        return true;

    assert(name);
    assert(loader);
    Class* loaded_class = loader->LookupClass(name);

    if (loaded_class)
        clss = loaded_class;

    return (clss != NULL);
}

TypeDesc* TypeDesc::type_desc_create_vector()
{
    TypeDesc* td = get_vector_type();
    if (td)
        return td;
    else
    {
        loader->LockTypesCache();
        td = get_vector_type();
        if (td) // check once again that someone could already insert vector type - like in string_pool
        {
            loader->UnlockTypesCache();
            return td;
        }
        TypeDesc* td = new TypeDesc(K_Vector, this, NULL, NULL, loader, NULL);
        assert(td);
        set_vector_type(td);
        loader->UnlockTypesCache();
        return td;
    }
}


