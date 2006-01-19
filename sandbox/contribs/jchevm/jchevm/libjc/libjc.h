
/*
 * Copyright 2005 The Apache Software Foundation or its licensors,
 * as applicable.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * $Id$
 */

#ifndef _LIBJC_H_
#define _LIBJC_H_

#include "config.h"

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <sys/wait.h>
#include <sys/time.h>
#include <sys/utsname.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <ucontext.h>
#include <limits.h>
#include <fcntl.h>
#include <unistd.h>
#include <signal.h>
#include <ctype.h>
#include <errno.h>
#include <dlfcn.h>
#include <sched.h>
#include <math.h>
#include <pwd.h>

#include <zlib.h>

#define _JC_VIRTUAL_MACHINE		1	/* for jni.h */

#include "queue.h"
#include "jni.h"
#include "jc_defs.h"

#include "arch_definitions.h"
#include "definitions.h"
#include "arch_structures.h"
#include "structures.h"
#include "arch_libjc.h"

/* array.c */
extern jint		_jc_setup_array_types(_jc_env *env);

/* bootstrap.c */
extern jint		_jc_bootstrap_classes(_jc_env *env);

/* class_bytes.c */
extern _jc_classbytes	*_jc_bootcl_find_classbytes(_jc_env *env,
				const char *name, int *indexp);
extern _jc_classbytes	*_jc_copy_classbytes(_jc_env *env, const void *data,
				size_t len);
extern _jc_classbytes	*_jc_read_classbytes(_jc_env *env, _jc_cpath_entry *ent,
				const char *name);
extern jint		_jc_read_classbytes_dir(_jc_env *env,
				_jc_cpath_entry *ent, const char *name,
				_jc_classbytes **bytesp);
extern jint		_jc_read_classbytes_zip(_jc_env *env,
				_jc_cpath_entry *ent, const char *name,
				_jc_classbytes **bytesp);
extern void		_jc_free_classbytes(_jc_classbytes **bytesp);

/* class_loader.c */
extern _jc_class_loader	*_jc_get_loader(_jc_env *env, _jc_object *cl);
extern _jc_class_loader	*_jc_get_jni_loader(_jc_env *env);
extern _jc_class_loader	*_jc_create_loader(_jc_env *env);
extern void		_jc_destroy_loader(_jc_jvm *vm,
				_jc_class_loader **loaderp);
extern void		_jc_loader_wait(_jc_env *env, _jc_class_loader *loader);
extern jint		_jc_merge_implicit_refs(_jc_env *env,
				const _jc_resolve_info *info);

/* cl_alloc.c */
extern void		_jc_uni_alloc_init(_jc_uni_mem *uni, int min_pages,
				volatile _jc_word *avail_pages);
extern void		_jc_uni_alloc_free(_jc_uni_mem *uni);
extern void		*_jc_uni_alloc(_jc_env *env, _jc_uni_mem *uni,
				size_t size);
extern void		_jc_uni_unalloc(_jc_uni_mem *uni, void *ptrp,
				size_t size);
extern void		*_jc_uni_zalloc(_jc_env *env, _jc_uni_mem *uni,
				size_t size);
extern char		*_jc_uni_strdup(_jc_env *env, _jc_uni_mem *uni,
				const char *s);
extern jboolean		_jc_uni_contains(_jc_uni_mem *uni, const void *ptr);
extern void		*_jc_uni_mark(_jc_uni_mem *uni);
extern void		_jc_uni_reset(_jc_uni_mem *uni, void *mem);
extern void		*_jc_cl_alloc(_jc_env *env, _jc_class_loader *loader,
				size_t size);
extern void		_jc_cl_unalloc(_jc_class_loader *loader, void *ptrp,
				size_t size);
extern void		*_jc_cl_zalloc(_jc_env *env, _jc_class_loader *loader,
				size_t size);
extern char		*_jc_cl_strdup(_jc_env *env, _jc_class_loader *loader,
				const char *s);

/* deps.c */
extern int		_jc_gen_deplist(_jc_env *env, _jc_classfile *cfile,
				_jc_class_ref **arrayp);

/* derive.c */
extern _jc_type		*_jc_derive_type_from_classfile(_jc_env *env,
				_jc_class_loader *loader, const char *name,
				_jc_classbytes *cbytes);
extern _jc_type		*_jc_derive_array_type(_jc_env *env,
				_jc_class_loader *loader, const char *name);
extern void		_jc_initialize_lockword(_jc_env *env, _jc_type *type,
				_jc_type *stype);
extern void		_jc_initialize_bsi(_jc_jvm *vm, _jc_type *type);

/* derive2.c */
extern _jc_type		*_jc_derive_type_interp(_jc_env *env,
				_jc_class_loader *loader,
				_jc_classbytes *cbytes);

/* exception.c */
extern void		_jc_post_exception_object(_jc_env *env,
				_jc_object *obj);
extern void		_jc_post_exception(_jc_env *env, int num);
extern void		_jc_post_exception_msg(_jc_env *env, int num,
				const char *fmt, ...)
#ifdef __FreeBSD__
				__printf0like(3, 4)
#endif
;
extern void		_jc_post_exception_info(_jc_env *env);
extern void		_jc_post_exception_params(_jc_env *env, int num,
				_jc_word *params);
extern void		_jc_throw_exception(_jc_env *env)
				__attribute__ ((noreturn));
extern _jc_object	*_jc_retrieve_exception(_jc_env *env, _jc_type *type);
extern _jc_object	*_jc_retrieve_cross_exception(_jc_env *env);
extern jboolean		_jc_unpost_exception(_jc_env *env, int num);
extern void		_jc_fprint_exception_headline(_jc_env *env,
				FILE *fp, _jc_object *e);

/* gc_final.c */
extern jint		_jc_gc_finalize(_jc_env *env);

/* gc_root.c */
extern int		_jc_root_walk(_jc_env *env, _jc_object ***refsp);

/* gc_scan.c */
extern jint		_jc_gc(_jc_env *env, jboolean urgent);

/* heap.c */
extern jint		_jc_heap_init(_jc_env *env, _jc_jvm *vm);
extern void		_jc_heap_destroy(_jc_jvm *vm);
extern void		*_jc_heap_alloc_small_block(_jc_env *env, int bsi);
extern void		*_jc_heap_alloc_pages(_jc_env *env, int npages);
extern int		_jc_heap_block_size(_jc_jvm *vm, size_t size);
#ifndef NDEBUG
extern void		_jc_heap_check(_jc_jvm *vm);
extern void		_jc_heap_check_object(_jc_jvm *vm, _jc_object *obj,
				int recurse);
#endif

/* init.c */
extern jint		_jc_init(void);

/* initialize.c */
extern jint		_jc_initialize_type(_jc_env *env, _jc_type *type);

/* instance.c */
extern int		_jc_instance_of(_jc_env *env, _jc_object *obj,
				_jc_type *type);
extern int		_jc_assignable_from(_jc_env *env, _jc_type *from,
				_jc_type *to);
extern jboolean		_jc_subclass_of(_jc_object *obj, _jc_type *type);

/* interp.c */
extern jboolean		_jc_interp_z(_jc_env *env, ...);
extern jbyte		_jc_interp_b(_jc_env *env, ...);
extern jchar		_jc_interp_c(_jc_env *env, ...);
extern jshort		_jc_interp_s(_jc_env *env, ...);
extern jint		_jc_interp_i(_jc_env *env, ...);
extern jlong		_jc_interp_j(_jc_env *env, ...);
extern jfloat		_jc_interp_f(_jc_env *env, ...);
extern jdouble		_jc_interp_d(_jc_env *env, ...);
extern _jc_object	*_jc_interp_l(_jc_env *env, ...);
extern void		_jc_interp_v(_jc_env *env, ...);
extern jboolean		_jc_interp_native_z(_jc_env *env, ...);
extern jbyte		_jc_interp_native_b(_jc_env *env, ...);
extern jchar		_jc_interp_native_c(_jc_env *env, ...);
extern jshort		_jc_interp_native_s(_jc_env *env, ...);
extern jint		_jc_interp_native_i(_jc_env *env, ...);
extern jlong		_jc_interp_native_j(_jc_env *env, ...);
extern jfloat		_jc_interp_native_f(_jc_env *env, ...);
extern jdouble		_jc_interp_native_d(_jc_env *env, ...);
extern _jc_object	*_jc_interp_native_l(_jc_env *env, ...);
extern void		_jc_interp_native_v(_jc_env *env, ...);
extern int		_jc_interp_pc_to_jline(_jc_method *method, int index);
extern void		_jc_interp_get_targets(_jc_env *env);
extern const _jc_word	*_jc_interp_targets;

/* invoke.c */
extern jint		_jc_invoke_nonvirtual(_jc_env *env,
				_jc_method *method, _jc_object *this, ...);
extern jint		_jc_invoke_unwrap_nonvirtual(_jc_env *env,
				_jc_method *method, jobject this, ...);
extern jint		_jc_invoke_nonvirtual_v(_jc_env *env,
				_jc_method *method, _jc_object *this,
				va_list args);
extern jint		_jc_invoke_unwrap_nonvirtual_v(_jc_env *env,
				_jc_method *method, jobject this, va_list args);
extern jint		_jc_invoke_nonvirtual_a(_jc_env *env,
				_jc_method *method, _jc_object *this,
				_jc_word *params);
extern jint		_jc_invoke_unwrap_nonvirtual_a(_jc_env *env,
				_jc_method *method, jobject this,
				jvalue *params);
extern jint		_jc_invoke_virtual(_jc_env *env,
				_jc_method *method, _jc_object *this, ...);
extern jint		_jc_invoke_unwrap_virtual(_jc_env *env,
				_jc_method *method, jobject this, ...);
extern jint		_jc_invoke_virtual_v(_jc_env *env,
				_jc_method *method, _jc_object *this,
				va_list args);
extern jint		_jc_invoke_unwrap_virtual_v(_jc_env *env,
				_jc_method *method, jobject this, va_list args);
extern jint		_jc_invoke_virtual_a(_jc_env *env, _jc_method *method,
				_jc_object *this, _jc_word *params);
extern jint		_jc_invoke_unwrap_virtual_a(_jc_env *env,
				_jc_method *method, jobject this,
				jvalue *params);
extern jint		_jc_invoke_static(_jc_env *env,
				_jc_method *method, ...);
extern jint		_jc_invoke_unwrap_static(_jc_env *env,
				_jc_method *method, ...);
extern jint		_jc_invoke_static_v(_jc_env *env,
				_jc_method *method, va_list args);
extern jint		_jc_invoke_unwrap_static_v(_jc_env *env,
				_jc_method *method, va_list args);
extern jint		_jc_invoke_static_a(_jc_env *env, _jc_method *method,
				_jc_word *params);
extern jint		_jc_invoke_unwrap_static_a(_jc_env *env,
				_jc_method *method, jvalue *params);
extern jint		_jc_invoke_v(_jc_env *env, _jc_method *method,
				const void *func, _jc_object *obj, va_list args,
				jboolean jni);
extern jint		_jc_invoke_jcni_a(_jc_env *env, _jc_method *method,
				const void *func, _jc_object *obj,
				_jc_word *params);
extern jint		_jc_invoke_jni_a(_jc_env *env, _jc_method *method,
				const void *func, _jc_object *obj,
				_jc_word *params);

/* jni_invoke.c */
extern const		struct JNIInvokeInterface _jc_invoke_interface;

/* jni_native.c */
extern const		struct JNINativeInterface _jc_native_interface;

/* load.c */
extern _jc_type		*_jc_load_type(_jc_env *env, _jc_class_loader *loader,
				const char *name);
extern _jc_type		*_jc_load_type2(_jc_env *env, _jc_class_loader *loader,
				const char *name, size_t len);
extern _jc_type		*_jc_load_primitive_type(_jc_env *env, int ptype);
extern jint		_jc_create_class_instance(_jc_env *env, _jc_type *type);
extern _jc_type		*_jc_find_type(_jc_env *env, _jc_class_loader *loader,
				const char *name);

/* lock.c */
extern jint		_jc_lock_object(_jc_env *env, _jc_object *obj);
extern jint		_jc_unlock_object(_jc_env *env, _jc_object *obj);
extern jint		_jc_inflate_lock(_jc_env *env, _jc_object *object);
extern void		_jc_lock_contention(_jc_env *env, _jc_object *obj);
extern void		_jc_free_lock(_jc_jvm *vm, _jc_object *obj);
extern void		_jc_destroy_lock(_jc_fat_lock **lockp);
extern jboolean		_jc_lock_held(_jc_env *env, _jc_object *obj);

/* misc.c */
extern void		_jc_fatal_error(_jc_jvm *vm, const char *fmt, ...)
				__attribute__ ((noreturn));
extern void		_jc_fatal_error_v(_jc_jvm *vm, const char *fmt,
				va_list args) __attribute__ ((noreturn));
extern jint		_jc_create_subdir(_jc_env *env, const char *root,
				const char *pathname);
extern size_t		_jc_name_encode(const char *name, char *buf,
				jboolean pass_slash);
extern char		*_jc_name_decode(const char *name, char *buf);
extern const char	*_jc_parse_class_ref(const char *s, _jc_class_ref *rc,
				int cc, u_char *ptype);
extern char		**_jc_parse_searchpath(_jc_env *env, const char *path);
extern jint		_jc_parse_classpath(_jc_env *env, const char *path,
				_jc_cpath_entry **listp, int *lenp);
extern void		_jc_jni_encode(char **bufp, const char *s);
extern size_t		_jc_jni_encode_length(const char *s);
extern int		_jc_field_compare(const void *v1, const void *v2);
extern int		_jc_method_compare(const void *v1, const void *v2);

extern _jc_splay_cmp_t	_jc_class_ref_compare;
extern _jc_splay_cmp_t	_jc_type_cmp;
extern _jc_splay_cmp_t	_jc_node_cmp;

/* mutex.c */
extern jint		_jc_mutex_init(_jc_env *env, pthread_mutex_t *mutex);
extern void		_jc_mutex_destroy(pthread_mutex_t *mutex);
extern jint		_jc_cond_init(_jc_env *env, pthread_cond_t *cond);
extern void		_jc_cond_destroy(pthread_cond_t *cond);

/* native_lib.c */
extern jint		_jc_invoke_native_method(_jc_env *env,
				_jc_method *method, int values, ...);
extern jint		_jc_load_native_library(_jc_env *env,
				_jc_class_loader *loader, const char *name);
extern void		_jc_unload_native_libraries(_jc_jvm *vm,
				_jc_class_loader *loader);
extern jint		_jc_resolve_native_method(_jc_env *env,
				_jc_method *method);

/* native_ref.c */
extern jobject		_jc_new_local_native_ref(_jc_env *env, _jc_object *obj);
extern _jc_object	*_jc_free_local_native_ref(jobject *obj);
extern void		_jc_free_all_native_local_refs(_jc_env *env);
extern jobject		_jc_new_global_native_ref(_jc_env *env,
				_jc_object *obj);
extern _jc_object	*_jc_free_global_native_ref(jobject *obj);
extern void		_jc_free_all_native_global_refs(_jc_jvm *vm);
extern jint		_jc_push_local_native_frame(_jc_env *env, int num_refs);
extern void		_jc_push_stack_local_native_frame(_jc_env *env,
			    _jc_native_frame *frame);
extern jint		_jc_extend_local_native_frame(_jc_env *env,
				int num_refs);
extern jobject		_jc_pop_local_native_frame(_jc_env *env,
				_jc_object *obj);
extern _jc_native_frame	*_jc_add_native_frame(_jc_env *env,
				_jc_native_frame_list *list);

/* new.c */
extern _jc_object	*_jc_new_object(_jc_env *env, _jc_type *type);
extern _jc_object	*_jc_init_object(_jc_env *env, void *mem,
				_jc_type *type);
extern _jc_object	*_jc_initialize_class_object(_jc_env *env, void *mem);
extern _jc_array	*_jc_new_array(_jc_env *env, _jc_type *type, jint len);
extern _jc_array *	_jc_init_array(_jc_env *env, void *mem,
				_jc_type *type, jint len);
extern _jc_array	*_jc_new_multiarray(_jc_env *env, _jc_type *type,
				jint num_sizes, const jint *sizes);
extern _jc_array	*_jc_init_multiarray(_jc_env *env, void *mem,
				_jc_type *type, jint num_sizes,
				const jint *sizes);

/* os_functions.c */
extern int		_jc_num_cpus(_jc_env *env);

/* prepare.c */
extern jint		_jc_prepare_type(_jc_env *env, _jc_type *type);

/* printf.c */
extern int		_jc_printf(_jc_jvm *vm, const char *fmt, ...);
extern int		_jc_eprintf(_jc_jvm *vm, const char *fmt, ...);
extern int		_jc_fprintf(_jc_jvm *vm, FILE *fp,
				const char *fmt, ...);
extern void		_jc_fprint_noslash(_jc_jvm *vm, FILE *fp,
				const char *s);

/* properties.c */
extern jint		_jc_set_system_properties(_jc_env *env);
extern jint		_jc_set_property(_jc_env *env, const char *name,
				const char *value);
extern jint		_jc_set_property2(_jc_env *env, const char *name,
				size_t name_len, const char *value);
extern jint		_jc_digest_properties(_jc_env *vm);
extern void		_jc_destroy_properties(_jc_jvm *vm);

/* reflect.c */
extern void		*_jc_get_vm_pointer(_jc_jvm *vm, _jc_object *obj,
				_jc_field *field);
extern jint		_jc_set_vm_pointer(_jc_env *env, _jc_object *obj,
				_jc_field *field, void *ptr);
extern _jc_field	*_jc_get_declared_field(_jc_env *env, _jc_type *type,
				const char *name, const char *sig,
				int is_static);
extern _jc_method	*_jc_get_declared_method(_jc_env *env, _jc_type *type,
				const char *name, const char *sig,
				int flags_mask, int flags);
extern _jc_object	*_jc_get_reflected_field(_jc_env *env,
				_jc_field *field);
extern _jc_object	*_jc_get_reflected_method(_jc_env *env,
				_jc_method *method);
extern _jc_object	*_jc_get_reflected_constructor(_jc_env *env,
				_jc_method *method);
extern _jc_field	*_jc_get_field(_jc_env *env, _jc_object *obj);
extern _jc_method	*_jc_get_method(_jc_env *env, _jc_object *obj);
extern _jc_method	*_jc_get_constructor(_jc_env *env, _jc_object *obj);
extern jint		_jc_reflect_invoke(_jc_env *env, _jc_method *method,
				_jc_object *this, _jc_object_array *pobjs);
extern _jc_object_array	*_jc_get_parameter_types(_jc_env *env,
				_jc_method *method);
extern _jc_object_array	*_jc_get_exception_types(_jc_env *env,
				_jc_method *method);
extern _jc_object	*_jc_wrap_primitive(_jc_env *env, int type,
				_jc_value *value);
extern int		_jc_unwrap_primitive(_jc_env *env, _jc_object *obj,
				_jc_value *value);
extern jint		_jc_convert_primitive(_jc_env *env, int dtype,
				int stype, _jc_value *value);
extern int		_jc_reflect_accessible(_jc_env *env,
				_jc_type *member_class, _jc_uint16 access,
				_jc_type **calling_classp);

/* resolve.c */
extern jint		_jc_resolve_type(_jc_env *env, _jc_type *type);
extern _jc_method	*_jc_resolve_method(_jc_env *env, _jc_type *type,
				const char *name, const char *sig);
extern _jc_field	*_jc_resolve_field(_jc_env *env, _jc_type *type,
				const char *name, const char *sig,
				int is_static);
extern jint		_jc_resolve_add_ref(_jc_env *env,
				_jc_resolve_info *info, _jc_object *ref);
extern jint		_jc_resolve_add_loader_ref(_jc_env *env,
				_jc_resolve_info *info,
				_jc_class_loader *loader);

/* resolve2.c */
extern jint		_jc_resolve_interp(_jc_env *env, _jc_type *type,
				_jc_resolve_info *info);
extern int		_jc_resolve_signature(_jc_env *env, _jc_method *method,
				_jc_resolve_info *info);
extern _jc_method	*_jc_method_lookup(_jc_type *type, _jc_method *key);

/* signals.c */
extern jint		_jc_init_signals(void);
extern void		_jc_restore_signals(void);

/* splay.c */
extern void		_jc_splay_init(_jc_splay_tree *tree,
				_jc_splay_cmp_t *compare, size_t offset);
extern void		*_jc_splay_find(_jc_splay_tree *tree, void *item);
extern void		_jc_splay_insert(_jc_splay_tree *tree, void *item);
extern void		_jc_splay_remove(_jc_splay_tree *tree, void *item);
extern void		_jc_splay_list(_jc_splay_tree *tree, void **items);

/* stack.c */
extern void		_jc_print_stack_trace(_jc_env *env, FILE *fp);
extern int		_jc_save_stack_frames(_jc_env *env, _jc_env *thread,
				int max, _jc_saved_frame *frames);
extern void		_jc_print_stack_frames(_jc_env *env, FILE *fp,
				int num_frames, _jc_saved_frame *frames);
extern void		_jc_stack_clip(_jc_env *env);
extern void		_jc_stack_unclip(_jc_env *env);

/* string.c */
extern _jc_object	*_jc_new_string(_jc_env *env,
				const void *utf, size_t len);
extern _jc_object	*_jc_new_intern_string(_jc_env *env,
				const void *utf, size_t len);
extern jint		_jc_decode_string_chars(_jc_env *env,
				_jc_object *string, jchar *chars);
extern size_t		_jc_decode_string_utf8(_jc_env *env,
				_jc_object *string, char *buf);

/* thread.c */
extern jint		_jc_thread_init(void);
extern _jc_env		*_jc_allocate_thread(_jc_env *env);
extern void		_jc_free_thread(_jc_env **envp, int cachable);
extern void		_jc_free_thread_stacks(_jc_jvm *vm);
extern void		_jc_destroy_thread(_jc_env **envp);
extern _jc_env		*_jc_attach_thread(_jc_jvm *vm, _jc_ex_info *ex,
				_jc_c_stack *cstack);
extern void		_jc_detach_thread(_jc_env **envp);
extern jint		_jc_thread_create_instance(_jc_env *env,
				_jc_object *group, const char *name,
				jint priority, jboolean daemon);
extern _jc_env		*_jc_get_current_env(void);
extern void		_jc_stop_the_world(_jc_env *env);
extern void		_jc_resume_the_world(_jc_env *env);
extern void		_jc_halt_if_requested(_jc_env *env);
extern void		_jc_stopping_java(_jc_env *env, _jc_c_stack *cstack,
			    const char *fmt, ...);
extern void		_jc_resuming_java(_jc_env *env, _jc_c_stack *cstack);
extern void		*_jc_thread_start(void *arg);
extern jobject		_jc_internal_thread(_jc_env *env, const char *class);
extern jint		_jc_thread_check(_jc_env *env);
extern void		_jc_thread_interrupt(_jc_jvm *vm, _jc_env *thread);
extern void		_jc_thread_interrupt_instance(_jc_jvm *vm,
				_jc_object *instance);
extern void		_jc_thread_shutdown(_jc_env **envp);

/* tables.c */
extern const char	_jc_prim_chars[_JC_TYPE_MAX];
extern const char	*const _jc_vmex_names[_JC_VMEXCEPTION_MAX];
extern const char	*const _jc_prim_names[_JC_TYPE_MAX];
extern const char	*const _jc_prim_wrapper_class[_JC_TYPE_MAX];
extern const void	*const _jc_interp_funcs[_JC_TYPE_MAX];
extern const void	*const _jc_interp_native_funcs[_JC_TYPE_MAX];
extern const u_char	_jc_sig_types[0x100];
extern const u_char	_jc_dword_type[_JC_TYPE_MAX];
extern const size_t	_jc_type_sizes[_JC_TYPE_MAX];
extern const size_t	_jc_type_align[_JC_TYPE_MAX];
extern const int	_jc_field_type_sort[_JC_TYPE_MAX];
extern const size_t	_jc_array_head_sizes[_JC_TYPE_MAX];
extern const jlong	_jc_type_max_array_length[_JC_TYPE_MAX];
extern const char	_jc_hex_chars[16];
extern const char	*const _jc_verbose_names[_JC_VERBOSE_MAX];
extern const char	*const _jc_bytecode_names[0x100];

/* utf.c */
extern jint		_jc_utf_decode(const u_char *utf,
				jint ulen, jchar *buf);
extern size_t		_jc_utf_encode(const jchar *chars,
				jint clen, u_char *buf);

/* verify.c */
extern jint		_jc_verify_type(_jc_env *env, _jc_type *type);

/* vm.c */
extern jint		_jc_create_vm(void *args, _jc_jvm **vmp,
				_jc_env **envp);
extern void		_jc_free_vm(_jc_jvm **vmp);

/* vm_alloc.c */
extern void		*_jc_vm_alloc(_jc_env *env, size_t size);
extern void		*_jc_vm_zalloc(_jc_env *env, size_t size);
extern void		*_jc_vm_realloc(_jc_env *env, void *mem, size_t size);
extern char		*_jc_vm_strdup(_jc_env *env, const char *s);
extern char		*_jc_vm_strndup(_jc_env *env, const char *s,
				size_t len);
extern void		_jc_vm_free(void *pointerp);

/* zip.c */
extern _jc_zip		*_jc_zip_open(_jc_env *env, const char *path);
extern void		_jc_zip_close(_jc_zip **zipp);
extern int		_jc_zip_search(_jc_zip *zip, const char *name);
extern jint		_jc_zip_read(_jc_env *env, _jc_zip *zip,
				int indx, u_char *data);

/* java_lang_VMClassLoader.c */
extern _jc_object	*_jc_internal_load_class(_jc_env *env,
				_jc_object *name_string, _jc_object *loader_obj,
				jboolean resolve);

#include "inline.h"

#endif	/* _LIBJC_H_ */

