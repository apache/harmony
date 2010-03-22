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
 * @author Xiao-Feng Li, 2006/10/05
 */
 
#include <open/vm_class_info.h>
#include <open/vm_class_manipulation.h>
#include "../gen/gen.h"
#include "../thread/mutator.h"
#include "gc_for_barrier.h"
#include "../mark_sweep/wspace_mark_sweep.h"
#include "../common/gc_concurrent.h"
#include "../common/gc_common.h"
#include "../finalizer_weakref/finalizer_weakref.h"
#include "../verify/verify_live_heap.h"


/* All the write barrier interfaces need cleanup */

volatile unsigned int write_barrier_function;

void allocator_object_write_barrier(Partial_Reveal_Object* p_object, Collector* allocator) 
{
  if( addr_belongs_to_nos(p_object)) return;

  REF* p_slot; 
  /* scan array object */
  if (object_is_array((Partial_Reveal_Object*)p_object)) {
    Partial_Reveal_Object* array = p_object;
    assert(!obj_is_primitive_array(array));

    I_32 array_length = vector_get_length((Vector_Handle) array);
    for (int i = 0; i < array_length; i++) {
      p_slot = (REF *)vector_get_element_address_ref((Vector_Handle)array, i);
      if( read_slot(p_slot) != NULL && addr_belongs_to_nos(read_slot(p_slot))){
        collector_remset_add_entry(allocator, (Partial_Reveal_Object**)p_slot);
      }
    }   
    return;
  }

  /* scan non-array object */
  Partial_Reveal_Object* p_obj =  (Partial_Reveal_Object*)p_object;   
  unsigned int num_refs = object_ref_field_num(p_obj);
  int *ref_iterator = object_ref_iterator_init(p_obj);
            
  for(unsigned int i=0; i<num_refs; i++){
    p_slot = object_ref_iterator_get(ref_iterator+i, p_obj);        
    if( addr_belongs_to_nos(read_slot(p_slot))){
      collector_remset_add_entry(allocator, (Partial_Reveal_Object**)p_slot);
    }
  }

  return;
}

static void mutator_rem_obj(Managed_Object_Handle p_obj_written)
{
  if( obj_is_remembered((Partial_Reveal_Object*)p_obj_written))
    return;

  Partial_Reveal_Object* p_obj = (Partial_Reveal_Object*)p_obj_written;
  Obj_Info_Type info = get_obj_info_raw(p_obj);
  Obj_Info_Type new_info = info | OBJ_REM_BIT;
  while ( info != new_info) {
    Obj_Info_Type temp =
      atomic_casptrsz((volatile POINTER_SIZE_INT*)get_obj_info_addr(p_obj), new_info, info);
    if (temp == info) break;
    info = get_obj_info_raw(p_obj);
    new_info = info | OBJ_REM_BIT;
  }
  if(info == new_info) return; /* remembered by other */
    
  Mutator *mutator = (Mutator *)gc_get_tls();            
  mutator_remset_add_entry(mutator, (REF*)p_obj);
  return;
}

static void gen_write_barrier_rem_obj(Managed_Object_Handle p_obj_holding_ref, 
                      Managed_Object_Handle p_target) 
{
  if(p_target >= nos_boundary && p_obj_holding_ref < nos_boundary)
    mutator_rem_obj(p_obj_holding_ref);

  return;
}

/* The implementations are only temporary */
static void gen_write_barrier_rem_slot(Managed_Object_Handle *p_slot, 
                      Managed_Object_Handle p_target) 
{
  if(p_target >= nos_boundary && p_slot < nos_boundary){

    Mutator *mutator = (Mutator *)gc_get_tls();
    assert( addr_belongs_to_nos(p_target) && !addr_belongs_to_nos(p_slot)); 
            
    mutator_remset_add_entry(mutator, (REF*)p_slot);
  }
  return;
}

static void write_barrier_rem_source_obj(Managed_Object_Handle p_obj_holding_ref)
{
  if(obj_need_remember((Partial_Reveal_Object*)p_obj_holding_ref)){
    Mutator *mutator = (Mutator *)gc_get_tls();

    //FIXME: Release lock.
    obj_dirty_in_table((Partial_Reveal_Object *) p_obj_holding_ref);
    mutator_dirtyset_add_entry(mutator, (Partial_Reveal_Object*)p_obj_holding_ref);		
  }
}

static void gc_object_write_barrier(Managed_Object_Handle p_object) 
{
  
  if( addr_belongs_to_nos(p_object)) return;

  Mutator *mutator = (Mutator *)gc_get_tls();
  
  REF* p_slot; 
  /* scan array object */
  if (object_is_array((Partial_Reveal_Object*)p_object)) {
    Partial_Reveal_Object* array = (Partial_Reveal_Object*)p_object;
    assert(!obj_is_primitive_array(array));
    
    I_32 array_length = vector_get_length((Vector_Handle) array);
    for (int i = 0; i < array_length; i++) {
      p_slot = (REF*)vector_get_element_address_ref((Vector_Handle) array, i);
      if( read_slot(p_slot) != NULL && addr_belongs_to_nos(read_slot(p_slot))){
        mutator_remset_add_entry(mutator, p_slot);
      }
    }   
    return;
  }

  /* scan non-array object */
  Partial_Reveal_Object* p_obj =  (Partial_Reveal_Object*)p_object;   
  unsigned int num_refs = object_ref_field_num(p_obj);
  int *ref_iterator = object_ref_iterator_init(p_obj);
            
  for(unsigned int i=0; i<num_refs; i++){
    p_slot = object_ref_iterator_get(ref_iterator+i, p_obj);        
    if( addr_belongs_to_nos(read_slot(p_slot))){
      mutator_remset_add_entry(mutator, p_slot);
    }
  }

  return;
}


/*This function is for concurrent mark.*/
static void write_barrier_rem_obj_snapshot(Managed_Object_Handle p_obj_holding_ref)
{
  Mutator *mutator = (Mutator *)gc_get_tls();
  REF* p_obj_slot; 
  if(obj_need_take_snapshot((Partial_Reveal_Object*)p_obj_holding_ref)){
    if (object_is_array((Partial_Reveal_Object*)p_obj_holding_ref)) {
      Partial_Reveal_Object* array = (Partial_Reveal_Object*)p_obj_holding_ref;
      assert(!obj_is_primitive_array(array));

      Partial_Reveal_Object* obj_to_snapshot; 
      
      I_32 array_length = vector_get_length((Vector_Handle) array);
      for (int i = 0; i < array_length; i++) {
        p_obj_slot = (REF*)vector_get_element_address_ref((Vector_Handle) array, i);
        obj_to_snapshot = (Partial_Reveal_Object*)read_slot(p_obj_slot);
        if (obj_to_snapshot != NULL)  
          mutator_dirtyset_add_entry(mutator, obj_to_snapshot);
      }   
    }else{
      /* scan non-array object */
      Partial_Reveal_Object* p_obj =  (Partial_Reveal_Object*)p_obj_holding_ref;   
      unsigned int num_refs = object_ref_field_num(p_obj);
      int *ref_iterator = object_ref_iterator_init(p_obj);
      
      Partial_Reveal_Object* obj_to_snapshot; 

      for(unsigned int i=0; i<num_refs; i++){
        p_obj_slot = object_ref_iterator_get(ref_iterator+i, p_obj);        
        obj_to_snapshot = (Partial_Reveal_Object*)read_slot(p_obj_slot);
        if (obj_to_snapshot != NULL)  
          mutator_dirtyset_add_entry(mutator, obj_to_snapshot);
      }

      if(is_reference_obj(p_obj)){
        REF* p_referent_field = obj_get_referent_field(p_obj);
        obj_to_snapshot = (Partial_Reveal_Object*)read_slot(p_referent_field);
        if (obj_to_snapshot != NULL)  
          mutator_dirtyset_add_entry(mutator, obj_to_snapshot);
      }
    }
    obj_mark_gray_in_table((Partial_Reveal_Object *) p_obj_holding_ref);  // now, the black-only obj (no gray bit been set) will also be scaned by marker, here mark it to gray to prevent this, just a workaround
    obj_mark_black_in_table((Partial_Reveal_Object *) p_obj_holding_ref, mutator);
    obj_dirty_in_table((Partial_Reveal_Object *) p_obj_holding_ref);
  }
}

static void write_barrier_rem_slot_oldvar(Managed_Object_Handle* p_slot)
{
  Mutator *mutator = (Mutator *)gc_get_tls();  
  REF* p_obj_slot = (REF*) p_slot ;
  Partial_Reveal_Object* p_obj = (Partial_Reveal_Object*)read_slot(p_obj_slot);
  if(p_obj && obj_need_remember_oldvar(p_obj)){
    mutator->dirty_obj_num++;
    mutator_dirtyset_add_entry(mutator, p_obj);
  }
}

/*
static void write_barrier_for_check(Managed_Object_Handle p_obj_holding_ref,Managed_Object_Handle *p_slot, Managed_Object_Handle p_target)
{
  //Mutator *mutator = (Mutator *)gc_get_tls();  
  
  Partial_Reveal_Object* src_obj = (Partial_Reveal_Object*)p_obj_holding_ref;
  Partial_Reveal_Object* sub_obj = (Partial_Reveal_Object*)read_slot((REF*) p_slot);
  Partial_Reveal_Object* target_obj = (Partial_Reveal_Object*)p_target;
  
  if(src_obj && (!obj_is_mark_black_in_table(src_obj))){
     INFO2("gc.verifier", "[write_barrier_for_check] [Src]");
     analyze_bad_obj(src_obj);
     RAISE_ERROR;
  }

  if(sub_obj && (!obj_is_mark_black_in_table(sub_obj))){
     INFO2("gc.verifier", "[write_barrier_for_check] [Sub]");
     analyze_bad_obj(sub_obj);
     INFO2("gc.verifier", "[source object]");
     analyze_bad_obj(src_obj);
     //RAISE_ERROR;
     return;
  }

  if(target_obj && (!obj_is_mark_black_in_table(target_obj))){
     INFO2("gc.verifier", "[write_barrier_for_check] [Target]");
     analyze_bad_obj(target_obj);
     RAISE_ERROR;
  }

  *p_slot = p_target;
}
*/
//===========================================

/* The following routines were supposed to be the only way to alter any value in gc heap. */
void gc_heap_write_ref (Managed_Object_Handle p_obj_holding_ref, unsigned offset, Managed_Object_Handle p_target) 
{  assert(0); }


Boolean gc_heap_copy_object_array(Managed_Object_Handle src_array, unsigned int src_start, Managed_Object_Handle dst_array, unsigned int dst_start, unsigned int length)
{


    GC_VTable_Info *src_gcvt = obj_get_gcvt((Partial_Reveal_Object*)src_array);
    GC_VTable_Info *dst_gcvt = obj_get_gcvt((Partial_Reveal_Object*)dst_array);

    Class_Handle src_class = src_gcvt->gc_clss;
    Class_Handle dst_class = dst_gcvt->gc_clss;

   
       //element size of src should be same as element size of dst
       assert(src_gcvt->array_elem_size == dst_gcvt->array_elem_size);
       unsigned int elem_size = src_gcvt->array_elem_size;
	unsigned int src_first_elem_offset = array_first_element_offset((Partial_Reveal_Array*)src_array);
	unsigned int dst_first_elem_offset = array_first_element_offset((Partial_Reveal_Array*)dst_array);
       /*
	#ifdef COMPRESS_REFERENCE
          COMPRESSED_REFERENCE *src_copy_body = (COMPRESSED_REFERENCE *)((POINTER_SIZE_INT)src_array + src_first_elem_offset + elem_size*src_start);
          COMPRESSED_REFERENCE *dst_copy_body = (COMPRESSED_REFERENCE *)((POINTER_SIZE_INT)dst_array + dst_first_elem_offset + elem_size*dst_start);
       #else
       #endif 
       */
          REF* src_copy_body = (REF*)((POINTER_SIZE_INT)src_array + src_first_elem_offset + elem_size*src_start);
          REF* dst_copy_body = (REF*)((POINTER_SIZE_INT)dst_array + dst_first_elem_offset + elem_size*dst_start);	  
	
	
       if(class_is_instanceof(src_class, dst_class)) {
         //rem obj before is for OTF GC barriers
         if(WB_REM_OLD_VAR == write_barrier_function) {
            for (unsigned int count = 0; count < length; count++) {
	        write_barrier_rem_slot_oldvar((Managed_Object_Handle *)dst_copy_body+count);
            }   
         } else if(WB_REM_OBJ_SNAPSHOT == write_barrier_function) {
            write_barrier_rem_obj_snapshot(dst_array);
         }

	  memmove(dst_copy_body, src_copy_body, length * elem_size);

       } else { //for the condition src is not the type of dst
          Class_Handle dst_elem_clss = class_get_array_element_class(dst_class);
	   if(WB_REM_OBJ_SNAPSHOT == write_barrier_function) {
            write_barrier_rem_obj_snapshot(dst_array);
          }
	   
          for (unsigned int count = 0; count < length; count++) {
             // 1, null elements copy direct
             if (src_copy_body[count] == NULL) {
		   if(WB_REM_OLD_VAR == write_barrier_function) {
	               write_barrier_rem_slot_oldvar((Managed_Object_Handle *)dst_copy_body+count);
                 }
		   dst_copy_body[count] = NULL;
		   continue;
		}

             // 2, For non-null elements check if types are compatible.		
/* 
#ifdef COMPRESS_REFERENCE
	      ManagedObject *src_elem = (ManagedObject *)uncompress_compressed_reference(src_elem_offset);
             Class_Handle src_elem_clss = src_elem->vt()->clss;
#else
#endif
*/
             Class_Handle src_elem_clss = obj_get_gcvt(ref_to_obj_ptr(src_copy_body[count]))->gc_clss;

             if (!class_is_instanceof(src_elem_clss, dst_elem_clss)) {
                  if(WB_REM_SOURCE_OBJ == write_barrier_function) {
	               write_barrier_rem_source_obj(dst_array);
	           }
                  return FALSE;
             }

	      if(WB_REM_OLD_VAR == write_barrier_function) {
	          write_barrier_rem_slot_oldvar((Managed_Object_Handle *)dst_copy_body+count);
             }
	       dst_copy_body[count] = src_copy_body[count];
        }
      }

    //rem obj after is for mostly concurrent
    if(WB_REM_SOURCE_OBJ == write_barrier_function) {
	 write_barrier_rem_source_obj(dst_array);
    }

    return TRUE;
}


void gc_heap_wrote_object (Managed_Object_Handle p_obj_written )
{

  if( !gc_is_gen_mode() || !object_has_ref_field((Partial_Reveal_Object*)p_obj_written)) 
    return;

  /* for array copy and object clone */
#ifdef USE_REM_SLOTS
  gc_object_write_barrier(p_obj_written); 
#else
  if( p_obj_written >= nos_boundary ) return;

  mutator_rem_obj( p_obj_written );  
#endif 
  return;
}

/* FIXME:: this is not the right interface for write barrier */
void gc_heap_slot_write_ref (Managed_Object_Handle p_obj_holding_ref,Managed_Object_Handle *p_slot, Managed_Object_Handle p_target)
{ 
  switch(write_barrier_function){
    case WB_REM_NIL:
      *p_slot = p_target;
      break;
    case WB_REM_SOURCE_REF:
      *p_slot = p_target;
#ifdef USE_REM_SLOTS
      gen_write_barrier_rem_slot(p_slot, p_target); 
#else /* USE_REM_OBJS */
      gen_write_barrier_rem_obj(p_obj_holding_ref, p_target);
#endif
      break;      
    case WB_REM_SOURCE_OBJ:
      *p_slot = p_target;
      write_barrier_rem_source_obj(p_obj_holding_ref);
      break;
    case WB_REM_OBJ_SNAPSHOT:
      write_barrier_rem_obj_snapshot(p_obj_holding_ref);
      *p_slot = p_target;
      break;
    case WB_REM_OLD_VAR:
      write_barrier_rem_slot_oldvar(p_slot);      
      *p_slot = p_target;
      break;
    //just debugging
    /*
    case WB_CON_DEBUG:
	write_barrier_for_check(p_obj_holding_ref, p_slot, p_target);
       //*p_slot = p_target;
	break;
    */
    default:
      assert(0);
      return;
  }
  return;
}

/* this is used for global object update, e.g., strings. */
void gc_heap_write_global_slot(Managed_Object_Handle *p_slot,Managed_Object_Handle p_target)
{
  /*Concurrent Mark & Generational Mode: 
      Global objects are roots. After root set enumeration, this objects will be touched by GC. No barrier here.
    */

  *p_slot = p_target;
}
