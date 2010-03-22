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

#include "gc_common.h"
#include "open/vm_field_access.h"
#include "open/vm_class_manipulation.h"
#include "../finalizer_weakref/finalizer_weakref.h"

/* Setter functions for the gc class property field. */
void gc_set_prop_alignment_mask (GC_VTable_Info *gcvt, unsigned int the_mask)
{
  gcvt->gc_class_properties |= the_mask;
}
void gc_set_prop_non_ref_array (GC_VTable_Info *gcvt)
{
  gcvt->gc_class_properties |= CL_PROP_NON_REF_ARRAY_MASK;
}
void gc_set_prop_array (GC_VTable_Info *gcvt)
{
  gcvt->gc_class_properties |= CL_PROP_ARRAY_MASK;
}
void gc_set_prop_pinned (GC_VTable_Info *gcvt)
{
  gcvt->gc_class_properties |= CL_PROP_PINNED_MASK;
}
void gc_set_prop_finalizable (GC_VTable_Info *gcvt)
{
  gcvt->gc_class_properties |= CL_PROP_FINALIZABLE_MASK;
}
void gc_set_prop_reference(GC_VTable_Info *gcvt, WeakReferenceType type)
{
  gcvt->gc_class_properties |= (unsigned int)type << CL_PROP_REFERENCE_TYPE_SHIFT;
}


/* A comparison function for qsort() called below to order offset slots. */
static int intcompare(const void *vi, const void *vj)
{
  const int *i = (const int *) vi;
  const int *j = (const int *) vj;
  if (*i > *j)
    return 1;
  if (*i < *j)
    return -1;
  return 0;
}

static unsigned int class_num_ref_fields(Class_Handle ch)
{
  WeakReferenceType is_reference = class_is_reference(ch);
  unsigned num_ref_fields = 0;
  unsigned num_fields = class_num_instance_fields_recursive(ch);

  unsigned idx;
  for(idx = 0; idx < num_fields; idx++) {
    Field_Handle fh = class_get_instance_field_recursive(ch, idx);
    if(field_is_reference(fh)) {
      num_ref_fields++;
    }
  }

#ifndef BUILD_IN_REFERENT
  if (is_reference != NOT_REFERENCE) {
    unsigned int offset = class_get_referent_offset(ch);
    unsigned int gc_referent_offset = get_gc_referent_offset();
    if (gc_referent_offset == 0) {
      set_gc_referent_offset(offset);
    } else {
      assert(gc_referent_offset == offset);
    }

    num_ref_fields--;
  }
#endif

  return num_ref_fields;
}

static void build_ref_offset_array(Class_Handle ch, GC_VTable_Info *gcvt)
{     
  unsigned num_fields = class_num_instance_fields_recursive(ch);
  WeakReferenceType is_reference = class_is_reference(ch);
  unsigned int gc_referent_offset = get_gc_referent_offset();
  
  int *new_ref_array = gcvt->gc_ref_offset_array;
  int *result = new_ref_array;
  for(unsigned int idx = 0; idx < num_fields; idx++) {
    Field_Handle fh = class_get_instance_field_recursive(ch, idx);
    if(field_is_reference(fh)) {
      int offset = field_get_offset(fh);
#ifndef BUILD_IN_REFERENT
      if(is_reference && (offset == gc_referent_offset)) continue;
#endif
      *new_ref_array = field_get_offset(fh);
      new_ref_array++;
    }
  }

  /* ref array is NULL-terminated */
  *new_ref_array = 0;

  unsigned int num_ref_fields = gcvt->gc_number_of_ref_fields;
  /* offsets were built with idx, may not be in order. Let's sort it anyway.
     FIXME: verify_live_heap depends on ordered offset array. */
  qsort(gcvt->gc_ref_offset_array, num_ref_fields, sizeof(int), intcompare);
  
  return;
}

void gc_class_prepared (Class_Handle ch, VTable_Handle vth) 
{
  GC_VTable_Info *gcvt;  
  assert(ch);
  assert(vth);

  Partial_Reveal_VTable *vt = (Partial_Reveal_VTable *)vth;

  unsigned int num_ref_fields = class_num_ref_fields(ch);
  unsigned int gcvt_size = sizeof(GC_VTable_Info);
  if(num_ref_fields){
    gcvt_size += num_ref_fields * sizeof(unsigned int);  
  }
  
  gcvt_size = (gcvt_size + GCVT_ALIGN_MASK) & ~GCVT_ALIGN_MASK;
  gcvt = (GC_VTable_Info*) class_alloc_via_classloader(ch, gcvt_size);
  assert(gcvt);
  assert(!((POINTER_SIZE_INT)gcvt % GCVT_ALIGNMENT));

  memset((void *)gcvt, 0, gcvt_size);
  gcvt->gc_clss = ch;
  gcvt->gc_class_properties = 0;
  gc_set_prop_alignment_mask(gcvt, class_get_alignment(ch));

  if(num_ref_fields){
    gcvt->gc_number_of_ref_fields = num_ref_fields;
    /* Build the offset array */
    build_ref_offset_array(ch, gcvt);
  }

  if(class_is_array(ch)) {
    Class_Handle array_element_class = class_get_array_element_class(ch);
    gc_set_prop_array(gcvt);
    
        gcvt->array_elem_size = class_get_array_element_size(ch);
    unsigned int the_offset = vector_first_element_offset_unboxed(array_element_class);
    gcvt->array_first_elem_offset = the_offset;
  
    if (class_is_non_ref_array (ch)) {
      gc_set_prop_non_ref_array(gcvt);
    }else{
      gcvt->gc_number_of_ref_fields = 1;
    }
  }
  
  if (class_is_finalizable(ch)) {
    gc_set_prop_finalizable(gcvt);
  }

  WeakReferenceType type = class_is_reference(ch);
  gc_set_prop_reference(gcvt, type);
  
    unsigned int size = class_get_object_size(ch);
  gcvt->gc_allocated_size = size;
  
  gcvt->gc_class_name = class_get_name(ch);
  assert (gcvt->gc_class_name);

  /* these should be set last to use the gcvt pointer */
  if(gcvt->gc_number_of_ref_fields)
    gcvt = (GC_VTable_Info*)((POINTER_SIZE_INT)gcvt | GC_CLASS_FLAG_REFS);
  
  if(class_is_array(ch))
    gcvt = (GC_VTable_Info*)((POINTER_SIZE_INT)gcvt | GC_CLASS_FLAG_ARRAY);
    
  if(class_is_finalizable(ch))
    gcvt = (GC_VTable_Info*)((POINTER_SIZE_INT)gcvt | GC_CLASS_FLAG_FINALIZER);

  vtable_set_gcvt(vt, gcvt);

  return;
}  /* gc_class_prepared */






