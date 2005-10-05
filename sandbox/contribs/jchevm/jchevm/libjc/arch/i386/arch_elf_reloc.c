
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
 * $Id: arch_elf_reloc.c,v 1.2 2004/07/05 21:03:28 archiecobbs Exp $
 */

#include "libjc.h"

static const char *_jc_elf_reloc_names[11] = {
	"R_386_NONE",
	"R_386_32",
	"R_386_PC32",
	"R_386_GOT32",
	"R_386_PLT32",
	"R_386_COPY",
	"R_386_GLOB_DAT",
	"R_386_JMP_SLOT",
	"R_386_RELATIVE",
	"R_386_GOTOFF",
	"R_386_GOTPC",
};
#define NUM_RELOC_NAMES							\
	(sizeof(_jc_elf_reloc_names) / sizeof(*_jc_elf_reloc_names))

/*
 * Apply an i386 ELF relocation.
 *
 * None of the i386 ELF relocations use the addend field, so we ignore it.
 *
 * Any exceptions are stored, not posted.
 */
jint
_jc_elf_arch_reloc(_jc_env *env, const char *path, char *target_base,
	Elf_Addr target_offset, Elf_Word type, Elf_Addr value, Elf_Off addend)
{
	Elf_Addr *const target_addr = (Elf_Addr *)(target_base + target_offset);

	switch (type) {
	case R_386_NONE:
		return JNI_OK;
	case R_386_32:
		*target_addr += value;
		return JNI_OK;
	case R_386_PC32:
		*target_addr += value - (Elf_Addr)target_addr;
		return JNI_OK;
	case R_386_GLOB_DAT:
		*target_addr = value;
		return JNI_OK;
	case R_386_RELATIVE:
		*target_addr += (Elf_Addr)target_base;
		return JNI_OK;
	default:
		_JC_EX_STORE(env, LinkageError, "unsupported ELF relocation"
		    " type `%s' in `%s'", (type < NUM_RELOC_NAMES) ?
		      _jc_elf_reloc_names[type] : "?", path);
		return JNI_ERR;
	}
}

