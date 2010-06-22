#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

#
# Makefile for module 'vmcore'
#

HY_BIN=$(HY_TARGET)/build/drlvm/vm/native/vmcore/
include $(HY_TARGET)/hdk/build/make/defines.mk
VM_HOME=../../../../../../../vm/

DEFINES += \
  -DREFS_USE_UNCOMPRESSED -D_IA32_ -DPLATFORM_POSIX \
  -D__SMP__ -DLINUX_TLS_OPT -D_LARGEFILE64_SOURCE \
  -DAPR_DECLARE_EXPORT -DBUILDING_VM

OPT += -fexceptions
CXXFLAGS += -Wno-deprecated
WARNFLAGS := # TOFIX

HDKINCLUDES := \
  $(HY_HDK)/include/platform_lowlevel.h \
  $(HY_HDK)/include/m2n_internal.h

INCLUDES := \
  -Iutil/include \
  -I$(SHAREDSUB)$(HY_ARCH)/lil/include \
  -I$(SHAREDSUB)$(HY_ARCH)/lil \
  -I$(SHAREDSUB)$(HY_ARCH)/util/natives \
  -I$(SHAREDSUB)kernel_classes \
  -I$(VM_HOME)include -I$(VM_HOME)port/include \
  -I$(VM_HOME)vmcore/include $(INCLUDES)

BUILDFILES := \
  class_support/Assertion_Registry.o class_support/C_Interface.o \
  class_support/Class_File_Loader.o class_support/class_impl.o \
  class_support/classloader.o class_support/Class.o \
  class_support/Environment.o class_support/Initialize.o \
  class_support/inline_info.o class_support/java_type.o \
  class_support/manifest.o class_support/method.o \
  class_support/Prepare.o class_support/Resolve.o \
  class_support/String_Pool.o class_support/type_access.o \
  class_support/type.o class_support/Verifier_stub.o \
  compmgr/component_manager_impl.o \
  exception/exceptions_impl.o exception/exceptions_int.o \
  exception/exceptions_jit.o exception/exceptions.o \
  gc/dll_gc.o gc/root_set_enum_common.o gc/stop_the_world_root_set_enum.o \
  init/finalize.o init/finalizer_thread.o init/parse_arguments.o \
  init/properties.o init/ref_enqueue_thread.o init/vm_init.o init/vm.o \
  init/vm_properties.o init/vm_shutdown.o \
  interpreter/interp_exports.o interpreter/interp_imports.o \
  jit/bit_vector.o jit/compile.o jit/dll_jit.o jit/dump.o jit/ini.o \
  jit/jit_runtime_support.o jit/native_overrides.o jit/primitives_support.o \
  jit/rt_helper_info.o \
  jni/jni_array.o jni/jni_field.o jni/jni_method.o jni/jni.o jni/jni_utils.o \
  jvmti/jvmti_break_intf.o jvmti/jvmti_break.o jvmti/jvmti_capability.o \
  jvmti/jvmti_class.o jvmti/jvmti_dasm.o jvmti/jvmti_enumerate.o \
  jvmti/jvmti_event.o jvmti/jvmti_extension.o jvmti/jvmti_field.o \
  jvmti/jvmti_general.o jvmti/jvmti_heap.o jvmti/jvmti_internal.o \
  jvmti/jvmti_jni.o jvmti/jvmti_locals.o jvmti/jvmti_mem.o \
  jvmti/jvmti_method.o jvmti/jvmti.o jvmti/jvmti_object.o \
  jvmti/jvmti_pop_frame.o jvmti/jvmti_property.o jvmti/jvmti_rawmon.o \
  jvmti/jvmti_roots.o jvmti/jvmti_stack.o jvmti/jvmti_step.o \
  jvmti/jvmti_tags.o jvmti/jvmti_thread_group.o jvmti/jvmti_thread.o \
  jvmti/jvmti_timer.o jvmti/jvmti_trace.o jvmti/jvmti_watch.o \
  kernel_classes/java_lang_EMThreadSupport.o \
  kernel_classes/java_lang_FinalizerThread.o \
  kernel_classes/java_lang_reflect_VMField.o \
  kernel_classes/java_lang_reflect_VMReflection.o \
  kernel_classes/java_lang_System.o \
  kernel_classes/java_lang_VMClassRegistry.o \
  kernel_classes/java_lang_VMExecutionEngine.o \
  kernel_classes/java_lang_VMMemoryManager.o \
  kernel_classes/java_lang_VMThreadManager.o \
  kernel_classes/java_util_concurrent_locks_LockSupport.o \
  kernel_classes/org_apache_harmony_drlvm_VMHelperFastPath.o \
  kernel_classes/org_apache_harmony_drlvm_VMHelper.o \
  kernel_classes/org_apache_harmony_kernel_vm_VM.o \
  kernel_classes/org_apache_harmony_lang_management_ClassLoadingMXBeanImpl.o \
  kernel_classes/org_apache_harmony_lang_management_CompilationMXBeanImpl.o \
  kernel_classes/org_apache_harmony_lang_management_GarbageCollectorMXBeanImpl.o \
  kernel_classes/org_apache_harmony_lang_management_MemoryManagerMXBeanImpl.o \
  kernel_classes/org_apache_harmony_lang_management_MemoryMXBeanImpl.o \
  kernel_classes/org_apache_harmony_lang_management_MemoryNotificationThread.o \
  kernel_classes/org_apache_harmony_lang_management_MemoryNotificationThreadShutdown.o \
  kernel_classes/org_apache_harmony_lang_management_MemoryPoolMXBeanImpl.o \
  kernel_classes/org_apache_harmony_lang_management_RuntimeMXBeanImpl.o \
  kernel_classes/org_apache_harmony_lang_management_ThreadMXBeanImpl.o \
  kernel_classes/org_apache_harmony_util_concurrent_Atomics.o \
  kernel_classes/org_apache_harmony_vm_VMDebug.o \
  kernel_classes/org_apache_harmony_vm_VMGenericsAndAnnotations.o \
  kernel_classes/org_apache_harmony_vm_VMStack.o \
  kernel_classes/Runtime.o \
  lil/lil_code_generator.o lil/lil_code_generator_arch.o \
  lil/lil_code_generator_utils.o lil/lil.o \
  lil/m2n.o lil/stack_iterator/m2n.o lil/stack_iterator.o \
  lil/stack_iterator/stack_iterator.o ncai/ncai_break.o ncai/ncai_event.o \
  ncai/ncai_memory.o ncai/ncai_methods.o ncai/ncai_modules.o \
  ncai/ncai_modules_os.o ncai/ncai.o ncai/ncai_registers.o \
  ncai/ncai_signals.o ncai/ncai_stack.o ncai/ncai_step_arch.o \
  ncai/ncai_step.o ncai/ncai_thread.o ncai/ncai_utils.o \
  ncai/ncai_utils_os.o \
  object/object_handles.o object/vm_arrays.o \
  reflection/annotations.o reflection/reflection.o \
  stack/native_stack.o stack/stack_trace.o \
  thread/atomics_common.o thread/lock_manager.o thread/mon_enter_exit.o \
  thread/object_generic.o thread/thread_dump.o thread/thread_generic.o \
  thread/thread_helpers.o thread/thread_java_attrs.o \
  thread/thread_java_basic.o thread/thread_java_interrupt.o \
  thread/thread_java_iterator.o thread/thread_java_monitors.o \
  thread/thread_java_park.o thread/thread_java_suspend.o \
  thread/thread_manager.o thread/thread_ti_instr.o \
  thread/thread_ti_monitors.o thread/thread_ti_others.o \
  thread/thread_ti_timing.o thread/verify_stack_enumeration.o \
  util/bytereader.o util/compile.o util/crash_dump.o \
  util/crash_dump_os.o util/ini.o util/jarfile_support.o \
  util/jit_lock_rt_support.o util/jit_runtime_support.o \
  util/mem_alloc.o util/natives/java_lang_thread.o util/natives_support.o \
  util/os_wrapper.o util/signals.o util/vm_stats.o \
  util/vm_strings.o

ifeq ($(HY_ARCH),x86)
BUILDFILES += util/optimize.o
endif

MDLLIBFILES += $(LIBPATH)libport.a $(LIBPATH)libencoder.a \
  $(LIBPATH)libapr-1.a $(LIBPATH)libverifier.a \
  $(DLLPATH)default/libch$(HY_LINKLIB_SUFFIX) \
  $(DLLPATH)default/libicuuc$(HY_LINKLIB_SUFFIX)

OSLIBS += $(STDCLIBS)

ifeq ($(HY_LOCAL_ZLIB),true)
OSLIBS += -lz
else
MDLLIBFILES +=   $(DLLPATH)libhyzlib$(HY_LINKLIB_SUFFIX)
endif

DLLNAME = $(DLLPATH)default/libharmonyvm$(HY_SHLIB_SUFFIX)
EXPNAME = VMCORE_0.1

AS := $(CXX)
ASFLAGS := -Wa,--fatal-warnings -c -x assembler-with-cpp -g

include $(HY_HDK)/build/make/rules.mk

$(HY_HDK)/include/%.h: util/include/%.h
	mkdir -p $(HY_HDK)/include
	cp -p $< $@

$(HY_HDK)/include/%.h: $(SHAREDSUB)$(HY_ARCH)/lil/%.h
	mkdir -p $(HY_HDK)/include
	cp -p $< $@
