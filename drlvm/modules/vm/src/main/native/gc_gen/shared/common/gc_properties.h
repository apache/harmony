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

#ifndef _GC_PROPERTIES
#define _GC_PROPERTIES

enum GC_CAUSE{
  GC_CAUSE_NIL,
  GC_CAUSE_NOS_IS_FULL,
  GC_CAUSE_LOS_IS_FULL,
  GC_CAUSE_MOS_IS_FULL,
  GC_CAUSE_RUNTIME_FORCE_GC,
  GC_CAUSE_CONCURRENT_GC
};

extern unsigned int GC_PROP;

/* ============================================================================ */
/* legal collection kinds:
   for ALGO_HAS_NOS:
     MINOR (i.e., !ALGO_MAJOR)

       ALGO_COPY_SEMISPACE
       ALGO_COPY_FORWARD
        
     ALGO_MAJOR_FALLBACK
     ALGO_MAJOR_NORMAL

       ALGO_MS_NORMAL
       ALGO_MS_COMPACT
       ALGO_COMPACT_SLIDE
       ALGO_COMPACT_MOVE

  for !ALGO_HAS_NOS (and actually !ALGO_HAS_LOS)
     ALGO_COMPACT_MOVE
     ALGO_MS_NORMAL
     ALGO_MS_COMPACT
*/

enum GC_Property{
  ALGO_HAS_NOS          = 0x1,
  ALGO_HAS_LOS          = 0x2,
  ALGO_IS_UNIQUE      = 0x4,
  ALGO_IS_GEN           = 0x8,
  
  ALGO_COPY_FORWARD     = 0x10,
  ALGO_COPY_SEMISPACE   = 0x20, 
  
  ALGO_COMPACT_MOVE     = 0x40,
  ALGO_COMPACT_SLIDE    = 0x80,
  ALGO_COMPACT_MASK     = 0xc0,
  
  ALGO_MARKSWEEP        = 0x100,
  ALGO_MS_NORMAL        = 0x300,  /* ALGO_MARKSWEEP|0x200 */
  ALGO_MS_COMPACT       = 0x500,  /* ALGO_MARKSWEEP|0x400 */
  ALGO_MARKSWEEP_MASK   = 0x700,
        
  ALGO_WORK_STEAL       = 0x1000,
  ALGO_TASK_PUSH        = 0x2000,
  ALGO_POOL_SHARE       = 0x4000,
  
  ALGO_BREADTH_FIRST    = 0x10000,
  ALGO_DEPTH_FIRST      = 0x20000,
  ALGO_REORDERING       = 0x40000,

  ALGO_MAJOR            = 0x100000,
  ALGO_MAJOR_NORMAL     = 0x300000,  /* ALGO_MAJOR|0x200000 */
  ALGO_MAJOR_FALLBACK   = 0x500000,  /* ALGO_MAJOR|0x400000 */
  ALGO_MAJOR_MASK       = 0x700000,

  ALGO_CON              = 0x1000000,
  ALGO_CON_MARK         = 0x3000000,  /* ALGO_CON|0x2000000 */
  ALGO_CON_SWEEP        = 0x5000000,  /* ALGO_CON|0x4000000 */
  ALGO_CON_ENUM         = 0x9000000,  /* ALGO_CON|0x8000000 */

  ALGO_CON_OTF_OBJ      = 0x10000000,
  ALGO_CON_OTF_REF      = 0x20000000,
  ALGO_CON_MOSTLY       = 0x40000000,
  ALGO_CON_MASK         = 0x70000000,
};

FORCE_INLINE Boolean gc_is_kind(unsigned int kind)
{
  return (Boolean)((GC_PROP & kind) == kind);
}

FORCE_INLINE void gc_set_gen_flag()
{
  GC_PROP |= ALGO_IS_GEN;  
}

FORCE_INLINE void gc_clear_gen_flag()
{
  GC_PROP &= ~ALGO_IS_GEN;  
}

FORCE_INLINE Boolean gc_is_unique_space()
{
  return gc_is_kind(ALGO_IS_UNIQUE);
}

FORCE_INLINE Boolean gc_is_unique_move_compact()
{
  return gc_is_kind(ALGO_IS_UNIQUE) && gc_is_kind(ALGO_COMPACT_MOVE);
}

FORCE_INLINE Boolean gc_is_unique_mark_sweep()
{
  return gc_is_kind(ALGO_IS_UNIQUE) && gc_is_kind(ALGO_MS_NORMAL);
}

FORCE_INLINE Boolean gc_is_gen_mode()
{
  return gc_is_kind(ALGO_IS_GEN);
}

FORCE_INLINE Boolean gc_has_los()
{
  return gc_is_kind(ALGO_HAS_LOS);
}

FORCE_INLINE Boolean gc_has_nos()
{
  return gc_is_kind(ALGO_HAS_NOS);
}

FORCE_INLINE Boolean collect_is_major()
{
  return gc_is_kind(ALGO_MAJOR);
}

FORCE_INLINE Boolean collect_is_minor()
{
  return gc_has_nos() && !collect_is_major();
}

FORCE_INLINE Boolean collect_is_major_normal()
{
  return gc_is_kind(ALGO_MAJOR_NORMAL);
}

FORCE_INLINE void collect_set_major_normal()
{
  GC_PROP &= ~ALGO_MAJOR_MASK;
  GC_PROP |= ALGO_MAJOR_NORMAL;
}

FORCE_INLINE void collect_set_minor()
{
  assert( gc_has_nos());
  GC_PROP &= ~ALGO_MAJOR_MASK;
}

FORCE_INLINE Boolean collect_is_fallback()
{
  return gc_is_kind(ALGO_MAJOR_FALLBACK);
}

FORCE_INLINE Boolean major_is_marksweep()
{
  return gc_is_kind(ALGO_MARKSWEEP|ALGO_HAS_NOS);
}

FORCE_INLINE Boolean major_is_compact_move()
{
  return gc_is_kind(ALGO_COMPACT_MOVE|ALGO_HAS_NOS);
}

FORCE_INLINE void major_set_compact_move()
{
  GC_PROP &= ~ALGO_COMPACT_MASK;
  GC_PROP |= ALGO_COMPACT_MOVE;  
}

FORCE_INLINE Boolean major_is_compact_slide()
{
  return gc_is_kind(ALGO_COMPACT_SLIDE|ALGO_HAS_NOS);
}

FORCE_INLINE void major_set_compact_slide()
{
  GC_PROP &= ~ALGO_COMPACT_MASK;
  GC_PROP |= ALGO_COMPACT_SLIDE;  
}

FORCE_INLINE Boolean minor_is_semispace()
{
  return gc_is_kind(ALGO_COPY_SEMISPACE|ALGO_HAS_NOS);  
}

FORCE_INLINE Boolean minor_is_forward()
{
  return gc_is_kind(ALGO_COPY_FORWARD|ALGO_HAS_NOS);  
}

FORCE_INLINE Boolean collect_move_object()
{
 if(gc_has_nos())
   return collect_is_minor() || 
         (collect_is_major() && !gc_is_kind(ALGO_MS_NORMAL));
 else
   return !gc_is_kind(ALGO_MS_NORMAL);  
}

FORCE_INLINE Boolean collect_is_compact_move()
{
  if(gc_has_nos())
    return collect_is_major() && gc_is_kind(ALGO_COMPACT_MOVE);
  else
    return gc_is_kind(ALGO_COMPACT_MOVE);    
}

FORCE_INLINE Boolean collect_is_ms_compact()
{
  if(gc_has_nos())
    return collect_is_major() && gc_is_kind(ALGO_MS_COMPACT);
  else
    return gc_is_kind(ALGO_MS_COMPACT);    
}

FORCE_INLINE void collect_set_ms_normal()
{
  GC_PROP &= ~ALGO_MARKSWEEP_MASK;
  GC_PROP |= ALGO_MS_NORMAL;

}

/* This is to distinct from the case of non-moving or trace-moving, where root slots
   either are updated on-the-fly, or need not updating. The kind below needs to update
   root slots after collection in an extra phase. i.e., collect_mark_and_move */
FORCE_INLINE Boolean collect_need_update_repset()
{
  return (gc_is_kind(ALGO_MAJOR) || gc_is_kind(ALGO_MS_COMPACT) || !gc_has_nos());
}

#endif /* #ifndef _GC_PROPERTIES */
