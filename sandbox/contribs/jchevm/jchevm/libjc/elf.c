
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
 * $Id: elf.c,v 1.5 2005/02/23 22:48:16 archiecobbs Exp $
 */

#include "libjc.h"

/* Internal functions */
static jint		_jc_elf_load_link(_jc_env *env, _jc_elf *elf,
				char **vaddrp, Elf_Off *vsizep);
static void		_jc_elf_destroy_link_info(_jc_elf_info **infop);
static jint		_jc_elf_resolve(_jc_env *env, _jc_elf *elf,
				_jc_elf_resolver *resolver, void *arg);
static jint		_jc_elf_resolve_sym(_jc_env *env, _jc_elf *elf,
				_jc_elf_loadable *loadable,
				const Elf_Rela *rela,
				_jc_elf_resolver *resolver, void *arg);
static jint		_jc_elf_process(_jc_env *env, _jc_elf *elf);

/* Internal variables */
static const u_char	elf_magic[4] = { ELFMAG0, ELFMAG1, ELFMAG2, ELFMAG3 };

/*
 * Load an ELF object file, resolve all symbols that can be resolved
 * locally within the file, and do some initial processing. The ELF file
 * is returned with one reference.
 *
 * If unsuccessful an exception is stored.
 */
_jc_elf *
_jc_elf_load(_jc_env *env, _jc_class_loader *loader, const char *path)
{
	_jc_jvm *const vm = env->vm;
	const size_t pathlen = strlen(path);
	_jc_elf *elf;

	/* Lock loader */
	_JC_MUTEX_LOCK(env, loader->mutex);

	/* Initialize new 'elf' structure */
	if ((elf = _jc_cl_alloc(env, loader,
	    sizeof(*elf) + pathlen + 1)) == NULL) {
		_JC_MUTEX_UNLOCK(env, loader->mutex);
		return NULL;
	}

	/* Unlock loader */
	_JC_MUTEX_UNLOCK(env, loader->mutex);

	/* Initialize ELF structure */
	memset(elf, 0, sizeof(*elf));
	elf->refs = 1;
	elf->loader = loader;
	memcpy(elf->pathname, path, pathlen + 1);
	_jc_splay_init(&elf->types, _jc_type_cmp, _JC_OFFSETOF(_jc_type, node));

	/* Verbosity */
	if (loader == vm->boot.loader)
		VERBOSE(OBJ, vm, "trying `%s' (via bootstrap loader)", path);
	else {
		VERBOSE(OBJ, vm, "trying `%s' (via %s@%p)",
		    path, loader->instance->type->name, loader->instance);
	}

	/* Load object file */
	if (_jc_elf_load_link(env, elf,
	    &elf->vaddr, &elf->vsize) == JNI_ERR)
		goto fail;

	/* Verbosity */
	if (loader == vm->boot.loader) {
		VERBOSE(OBJ, vm, "processing `%s' (via bootstrap loader)",
		    path);
	} else {
		VERBOSE(OBJ, vm, "processing `%s' (via %s@%p)",
		    path, loader->instance->type->name, loader->instance);
	}

	/* Do initial linking */
	if (_jc_elf_resolve(env, elf, NULL, NULL) != JNI_OK)
		goto fail;

	/* Do initial processing */
	if (_jc_elf_process(env, elf) != JNI_OK)
		goto fail;

	/* Done */
	return elf;

fail:
	VERBOSE(OBJ, vm, "failed: %s: %s",
	    _jc_vmex_names[env->ex.num], env->ex.msg);
	_jc_elf_unref(&elf);
	return NULL;
}

/*
 * Increment reference count on an ELF object.
 */
void
_jc_elf_addref(_jc_elf *elf)
{
	_jc_word old_refs;

	/* Increment ref count */
	_JC_ASSERT(elf->refs > 0);
	do
		old_refs = elf->refs;
	while (!_jc_compare_and_swap(&elf->refs, old_refs, old_refs + 1));
}

/*
 * Unreference an ELF object.
 */
void
_jc_elf_unref(_jc_elf **elfp)
{
	_jc_class_loader *loader;
	_jc_elf *elf = *elfp;
	_jc_word old_refs;

	/* Sanity check */
	if (elf == NULL)
		return;
	*elfp = NULL;

	/* Decrement ref count */
	_JC_ASSERT(elf->refs > 0);
	do
		old_refs = elf->refs;
	while (!_jc_compare_and_swap(&elf->refs, old_refs, old_refs - 1));
	if (elf->refs > 0)
		return;

	/* Destroy ELF link info */
	_jc_elf_destroy_link_info(&elf->info);

	/* Lock loader */
	loader = elf->loader;
	_JC_MUTEX_LOCK(_jc_get_current_env(), loader->mutex);

	/* Try to give back class loader memory */
	_jc_cl_unalloc(loader, &elf->vaddr, elf->vsize);
	_jc_cl_unalloc(loader, &elf, sizeof(*elf) + strlen(elf->pathname) + 1);

	/* Unlock loader */
	_JC_MUTEX_UNLOCK(_jc_get_current_env(), loader->mutex);
}

/*
 * Link a loaded ELF file and flush the instruction cache.
 *
 * It is assumed that internally resolvable symbols have already
 * been resolved.
 */
jint
_jc_elf_link(_jc_env *env, _jc_elf *elf, _jc_elf_resolver *resolver, void *arg)
{
	_jc_elf_info *const info = elf->info;
	int i;

	/* Sanity check */
	_JC_ASSERT(resolver != NULL);
	_JC_ASSERT(elf->info != NULL);

	/* Resolve external references */
	if (_jc_elf_resolve(env, elf, resolver, arg) != JNI_OK)
		return JNI_ERR;

	/* Flush the i-cache for executable sections */
	for (i = 0; i < info->num_loadables; i++) {
		_jc_elf_loadable *const loadable = &info->loadables[i];
		const Elf_Shdr *const shdr = loadable->shdr;

		if ((shdr->sh_flags & SHF_EXECINSTR) != 0)
			_jc_iflush(loadable->vaddr, shdr->sh_size);
	}

	/* Done */
	return JNI_OK;
}

/*
 * Free up memory used by the linking and symbol resolution process.
 */
void
_jc_elf_link_cleanup(_jc_elf *elf)
{
	_jc_elf_destroy_link_info(&elf->info);
}

/*
 * Load and ELF file but don't do anything much with it.
 *
 * The object is loaded using class loader memory, in a region
 * described upon return by *vaddrp and *vsizep.
 *
 * If unsuccessful an exception is stored.
 */
static jint
_jc_elf_load_link(_jc_env *env, _jc_elf *elf, char **vaddrp, Elf_Off *vsizep)
{
	_jc_jvm *const vm = env->vm;
	_jc_class_loader *const loader = elf->loader;
	const Elf_Ehdr *ehdr;
	const Elf_Shdr *symtab = NULL;
	const Elf_Shdr *strtab = NULL;
	const Elf_Shdr *stabstr = NULL;
	const char *shdr_strings;
	char *aligned_vaddr;
	_jc_elf_info *info;
	Elf_Off max_align;
	char *vaddr = NULL;
	Elf_Off vsize = 0;
	Elf_Off offset;
	struct stat sb;
	int esave;
	int fd;
	int i;

	/* Sanity check */
	_JC_ASSERT(elf->info == NULL);

	/* Initialize new ELF linkage structure */
	if ((info = _jc_vm_zalloc(env, sizeof(*info))) == NULL)
		return JNI_ERR;

	/* Open the ELF file and mmap() it in read-only */
	if ((fd = open(elf->pathname, O_RDONLY)) == -1 || fstat(fd, &sb) == -1)
		goto fail_errno;
	(void)fcntl(fd, F_SETFD, 1);
	info->map_size = sb.st_size;
	info->map_base = mmap(NULL, info->map_size,
	    PROT_READ|PROT_WRITE, MAP_PRIVATE, fd, 0);
	esave = errno;
	close(fd);
	if (info->map_base == MAP_FAILED) {
		errno = esave;
		goto fail_errno;
	}
	ehdr = info->ehdr = (const Elf_Ehdr *)info->map_base;

	/* Check that we're looking at the right kind of file */
	if (memcmp(ehdr, elf_magic, sizeof(elf_magic)) != 0) {
		snprintf(env->ex.msg, sizeof(env->ex.msg),
		    "`%s' is not an ELF object file", elf->pathname);
		goto fail_errmsg;
	}
	if (ehdr->e_ident[EI_CLASS] != _JC_ELF_CLASS
	    || ehdr->e_ident[EI_DATA] != _JC_ELF_DATA
	    || ehdr->e_machine != _JC_ELF_MACHINE
	    || ehdr->e_shentsize != sizeof(Elf_Shdr)) {
		snprintf(env->ex.msg, sizeof(env->ex.msg),
		    "`%s' has an unsupported ELF format", elf->pathname);
		goto fail_errmsg;
	}
	if (ehdr->e_ident[EI_VERSION] != EV_CURRENT
	    || ehdr->e_version != EV_CURRENT) {
		snprintf(env->ex.msg, sizeof(env->ex.msg),
		    "`%s' has an unsupported ELF version", elf->pathname);
		goto fail_errmsg;
	}
	if (ehdr->e_type != ET_REL) {
		snprintf(env->ex.msg, sizeof(env->ex.msg),
		    "`%s' is not a relocatable object", elf->pathname);
		goto fail_errmsg;
	}
	if (ehdr->e_shoff == 0 || ehdr->e_shstrndx == SHN_UNDEF) {
		snprintf(env->ex.msg, sizeof(env->ex.msg),
		    "`%s' is missing required section header info",
		    elf->pathname);
		goto fail_errmsg;
	}

	/* Initialize pointers to section header table and its strings */
	info->shdrs = (const Elf_Shdr *)(info->map_base + ehdr->e_shoff);
	shdr_strings = info->map_base + info->shdrs[ehdr->e_shstrndx].sh_offset;

	/* Create array of loadable sections */
	if ((info->loadables = _jc_vm_zalloc(env,
	    ehdr->e_shnum * sizeof(*info->loadables))) == NULL)
		goto fail;

	/* Create array that maps section index back to loadable section */
	if ((info->shdr2section = _jc_vm_alloc(env,
	    ehdr->e_shnum * sizeof(*info->shdr2section))) == NULL)
		goto fail;

	/* Scan the section header entries for loadable sections */
	offset = 0;
	max_align = _JC_FULL_ALIGNMENT;
	for (info->num_loadables = i = 0; i < ehdr->e_shnum; i++) {
		const Elf_Shdr *const shdr = &info->shdrs[i];
		const char *const sname = shdr_strings + shdr->sh_name;
		_jc_elf_loadable *const loadable
		    = &info->loadables[info->num_loadables];

		/* Ignore non-loadable sections from this point on */
		if ((shdr->sh_flags & SHF_ALLOC) == 0)
			continue;

		/* Sanity check */
		if (shdr->sh_addr != 0) {
			snprintf(env->ex.msg, sizeof(env->ex.msg),
			    "section `%s' in `%s' has non-zero section"
			    " load address", sname, elf->pathname);
			goto fail_errmsg;
		}

		/* Initialize new loadable section */
		memset(loadable, 0, sizeof(*loadable));
		loadable->name = sname;
		loadable->shdr = shdr;
		loadable->bytes = info->map_base + shdr->sh_offset;
		info->shdr2section[i] = loadable;

		/* Keep track of maximum alignment for all loadable sections */
		if (shdr->sh_addralign > max_align)
			max_align = shdr->sh_addralign;

		/* Align the load offset for this section */
		if (shdr->sh_addralign > _JC_FULL_ALIGNMENT)
			offset = _JC_ROUNDUP2(offset, shdr->sh_addralign);
		loadable->offset = offset;

		/*
		 * Update offset to account for this section's length.
		 * Keep all sections aligned at least to _JC_FULL_ALIGNMENT.
		 */
		offset = _JC_ROUNDUP2(offset + shdr->sh_size,
		    _JC_FULL_ALIGNMENT);

		/* Continue */
		info->num_loadables++;
	}

	/* Scan section header entries for debug, symbol and string sections */
	for (i = 0; i < ehdr->e_shnum; i++) {
		const Elf_Shdr *const shdr = &info->shdrs[i];
		const char *const sname = shdr_strings + shdr->sh_name;
		_jc_elf_debug_lines *const dlines = &info->debug_lines;

		/* Remember symbol and string sections */
		if (strcmp(sname, ".symtab") == 0) {
			symtab = shdr;
			continue;
		}
		if (strcmp(sname, ".strtab") == 0) {
			strtab = shdr;
			continue;
		}
		if (strcmp(sname, ".stabstr") == 0) {
			stabstr = shdr;			/* stabs strings */
			continue;
		}

		/* Remember debug sections containing line number info */
		if (!vm->line_numbers)
			continue;
		if (strcmp(sname, ".debug_line") == 0)
			dlines->type = _JC_LINE_DEBUG_DWARF2;
		else if (strcmp(sname, ".stab") == 0)
			dlines->type = _JC_LINE_DEBUG_STABS;
		else
			continue;
		dlines->loadable.name = sname;
		dlines->loadable.shdr = shdr;
		dlines->loadable.bytes = info->map_base + shdr->sh_offset;
		dlines->loadable.vaddr = info->map_base + shdr->sh_offset;
		info->shdr2section[i] = &dlines->loadable;
	}

	/* Get pointers to symbols and strings */
	if (symtab == NULL || strtab == NULL) {
		snprintf(env->ex.msg, sizeof(env->ex.msg),
		    "`%s' is missing a symbol or string table",
		    elf->pathname);
		goto fail_errmsg;
	}
	info->symbols = (const Elf_Sym *)(info->map_base + symtab->sh_offset);
	info->num_symbols = symtab->sh_size / sizeof(*info->symbols);
	info->strings = info->map_base + strtab->sh_offset;

	/* If debug section was found get other associated info */
	switch (info->debug_lines.type) {
	case _JC_LINE_DEBUG_DWARF2:
	    {
		_jc_elf_debug_lines *const dlines = &info->debug_lines;

		for (i = 0; i < ehdr->e_shnum; i++) {
			const Elf_Shdr *const shdr = &info->shdrs[i];
			const char *const sname = shdr_strings + shdr->sh_name;

			if (strcmp(sname, ".rel.debug_line") == 0)
				dlines->loadable.rel = shdr;
			else if (strcmp(sname, ".rela.debug_line") == 0)
				dlines->loadable.rela = shdr;
		}
		break;
	    }
	case _JC_LINE_DEBUG_STABS:
	    {
		_jc_elf_debug_lines *const dlines = &info->debug_lines;

		if (stabstr == NULL) {
			snprintf(env->ex.msg, sizeof(env->ex.msg),
			    "`%s' has no .stabstr section", elf->pathname);
			goto fail_errmsg;
		}
		dlines->strings = info->map_base + stabstr->sh_offset;
		break;
	    }
	case _JC_LINE_DEBUG_NONE:
		break;
	default:
		_JC_ASSERT(JNI_FALSE);
	}

	/* Find relocation sections for all loadable sections */
	for (i = 0; i < info->num_loadables; i++) {
		_jc_elf_loadable *const loadable = &info->loadables[i];
		char relaname[64];
		char relname[64];
		int j;

		snprintf(relname, sizeof(relname), ".rel%s", loadable->name);
		snprintf(relaname, sizeof(relaname), ".rela%s", loadable->name);
		for (j = 0; j < ehdr->e_shnum; j++) {
			const Elf_Shdr *const shdr = &info->shdrs[j];
			const char *sname = shdr_strings + shdr->sh_name;

			if (strcmp(sname, relname) == 0)
				loadable->rel = shdr;
			else if (strcmp(sname, relaname) == 0)
				loadable->rela = shdr;
		}
	}

	/*
	 * Allocate memory for the loadable sections. Ensure that we can
	 * align ourselves to 'max_align' by allocating a little extra.
	 */
	offset += max_align - _JC_FULL_ALIGNMENT;
	vsize = offset;
	_JC_MUTEX_LOCK(env, loader->mutex);
	vaddr = _jc_cl_alloc(env, loader, vsize);
	_JC_MUTEX_UNLOCK(env, loader->mutex);
	if (vaddr == NULL)
		goto fail;
	aligned_vaddr = (void *)_JC_ROUNDUP2((Elf_Off)vaddr, max_align);

	/* Load the loadable sections */
	for (i = 0; i < info->num_loadables; i++) {
		_jc_elf_loadable *const loadable = &info->loadables[i];
		const Elf_Shdr *const shdr = loadable->shdr;

		loadable->vaddr = aligned_vaddr + loadable->offset;
		if (shdr->sh_type != SHT_NOBITS)
			memcpy(loadable->vaddr, loadable->bytes, shdr->sh_size);
		else
			memset(loadable->vaddr, 0, shdr->sh_size);
	}

	/* Done */
	*vaddrp = vaddr;
	*vsizep = vsize;
	elf->info = info;
	return JNI_OK;

fail_errno:
	snprintf(env->ex.msg, sizeof(env->ex.msg),
	    "%s: %s", elf->pathname, strerror(errno));

fail_errmsg:
	env->ex.num = _JC_LinkageError;

fail:
	/* Clean up after failure */
	_JC_MUTEX_LOCK(env, loader->mutex);
	_jc_cl_unalloc(loader, &vaddr, vsize);
	_jc_elf_destroy_link_info(&info);
	_JC_MUTEX_UNLOCK(env, loader->mutex);
	return JNI_ERR;
}

/*
 * Destroy an ELF linking structure.
 *
 * NOTE: This assume the associated class loader mutex is locked.
 */
static void
_jc_elf_destroy_link_info(_jc_elf_info **infop)
{
	_jc_elf_info *info = *infop;

	/* Sanity check */
	if (info == NULL)
		return;
	*infop = NULL;

	/* Destroy structure */
	_JC_ASSERT(info->resolver == NULL);
	munmap((void *)info->map_base, info->map_size);
	_jc_vm_free(&info->shdr2section);
	_jc_vm_free(&info->loadables);
	_jc_vm_free(&info);
}

/*
 * Resolve a symbol in an ELF object. There are two cases here.
 *
 * If 'resolver' is NULL, only symbols resolvable internally are
 * resolved, and exceptions are stored, not posted. Otherwise,
 * only symbols resolvable externally are resolved, 'resolver' is
 * used to resolve them, and exceptions are posted.
 */
static inline jint
_jc_elf_resolve_sym(_jc_env *env, _jc_elf *elf, _jc_elf_loadable *loadable,
	const Elf_Rela *rela, _jc_elf_resolver *resolver, void *arg)
{
	const Elf_Word type = ELF_R_TYPE(rela->r_info);
	_jc_elf_info *const info = elf->info;
	const Elf_Sym *const sym = &info->symbols[ELF_R_SYM(rela->r_info)];
	const char *name = info->strings + sym->st_name;

	switch (sym->st_shndx) {
	case SHN_ABS:
		if (resolver != NULL)
			break;
		if (_jc_elf_arch_reloc(env, elf->pathname,
		    loadable->vaddr, rela->r_offset, type,
		    sym->st_value, rela->r_addend) != JNI_OK)
			return JNI_ERR;
		break;
	case SHN_UNDEF:
	    {
		Elf_Addr value;

		if (resolver == NULL)
			break;
		if ((*resolver)(env, arg,
		    info->strings + sym->st_name, &value) != JNI_OK)
			return JNI_ERR;
		if (_jc_elf_arch_reloc(env, elf->pathname, loadable->vaddr,
		    rela->r_offset, type, value, rela->r_addend) != JNI_OK) {
			_jc_post_exception_info(env);
			return JNI_ERR;
		}
		break;
	    }
	case SHN_COMMON:
		_JC_ASSERT(resolver == NULL);
		_JC_EX_STORE(env, LinkageError,
		    "%s: ELF symbol `%s' is common (not supported)",
		    elf->pathname, name);
		return JNI_ERR;
	default:
	    {
		const _jc_elf_loadable *sym_section;

		/* Skip if only resolving externals */
		if (resolver != NULL)
			break;

		/* Sanity check the symbol's section */
		if (sym->st_shndx >= info->ehdr->e_shnum
		    || (sym_section
		      = info->shdr2section[sym->st_shndx]) == NULL) {
			_JC_EX_STORE(env, LinkageError,
			    "%s: invalid section index %d for symbol `%s'",
			    elf->pathname, sym->st_shndx, name);
			return JNI_ERR;
		}

		/* Apply relocation */
		if (_jc_elf_arch_reloc(env, elf->pathname,
		    loadable->vaddr, rela->r_offset, type,
		    (Elf_Addr)(sym_section->vaddr + sym->st_value),
		    rela->r_addend) != JNI_OK)
			return JNI_ERR;
		break;
	    }
	}

	/* Done */
	return JNI_OK;
}

/*
 * Resolve symbols in an ELF file.
 *
 * If 'resolver' is NULL, only symbols resolvable internally are
 * resolved. Otherwise, only symbols resolvable externally are
 * resolved, and 'resolver' is used to resolve them.
 *
 * Exceptions are posted if resolver != NULL, otherwise stored.
 */
static jint
_jc_elf_resolve(_jc_env *env, _jc_elf *elf,
	_jc_elf_resolver *resolver, void *arg)
{
	_jc_elf_info *const info = elf->info;
	_jc_elf_debug_lines *const dlines = &info->debug_lines;
	int i;

	/* Resolve normal relocations for loadable sections */
	for (i = 0; i < info->num_loadables; i++) {
		_jc_elf_loadable *const section = &info->loadables[i];
		const Elf_Shdr *const rsect = section->rel;
		const Elf_Rel *rels;
		int num_rels;
		int j;

		/* If no relocations, skip this section */
		if (rsect == NULL)
			continue;

		/* Get pointer to relocations and count */
		rels = (const Elf_Rel *)(info->map_base + rsect->sh_offset);
		num_rels = rsect->sh_size / sizeof(*rels);

		/* Process normal relocations */
		for (j = 0; j < num_rels; j++) {
			const Elf_Rel *const rel = &rels[j];
			Elf_Rela rela;

			rela.r_offset = rel->r_offset;
			rela.r_info = rel->r_info;
			rela.r_addend = 0;
			if (_jc_elf_resolve_sym(env, elf,
			    section, &rela, resolver, arg) != JNI_OK)
				return JNI_ERR;
		}
	}

	/* Resolve addend relocations for loadable sections */
	for (i = 0; i < info->num_loadables; i++) {
		_jc_elf_loadable *const section = &info->loadables[i];
		const Elf_Shdr *const rsect = section->rela;
		const Elf_Rela *relas;
		int num_relas;
		int j;

		/* If no relocations, skip this section */
		if (rsect == NULL)
			continue;

		/* Get pointer to relocations and count */
		relas = (const Elf_Rela *)(info->map_base + rsect->sh_offset);
		num_relas = rsect->sh_size / sizeof(*relas);

		/* Process addend relocations */
		for (j = 0; j < num_relas; j++) {
			if (_jc_elf_resolve_sym(env, elf,
			    section, &relas[j], resolver, arg) != JNI_OK)
				return JNI_ERR;
		}
	}

	/* Resolve normal relocations for debug line section, if any */
	if (resolver == NULL && dlines->loadable.rel != NULL) {
		_jc_elf_loadable *const section = &dlines->loadable;
		const Elf_Shdr *const rsect = section->rel;
		const Elf_Rel *rels;
		int num_rels;
		int j;

		/* Get pointer to relocations and count */
		rels = (const Elf_Rel *)(info->map_base + rsect->sh_offset);
		num_rels = rsect->sh_size / sizeof(*rels);

		/* Process normal relocations */
		for (j = 0; j < num_rels; j++) {
			const Elf_Rel *const rel = &rels[j];
			Elf_Rela rela;

			rela.r_offset = rel->r_offset;
			rela.r_info = rel->r_info;
			rela.r_addend = 0;
			if (_jc_elf_resolve_sym(env, elf,
			    section, &rela, NULL, NULL) != JNI_OK)
				return JNI_ERR;
		}
	}

	/* Resolve addend relocations for debug line section, if any */
	if (resolver == NULL && dlines->loadable.rela != NULL) {
		_jc_elf_loadable *const section = &dlines->loadable;
		const Elf_Shdr *const rsect = section->rela;
		const Elf_Rela *relas;
		int num_relas;
		int j;

		/* Get pointer to relocations and count */
		relas = (const Elf_Rela *)(info->map_base + rsect->sh_offset);
		num_relas = rsect->sh_size / sizeof(*relas);

		/* Process addend relocations */
		for (j = 0; j < num_relas; j++) {
			if (_jc_elf_resolve_sym(env, elf,
			    section, &relas[j], NULL, NULL) != JNI_OK)
				return JNI_ERR;
		}
	}

	/* Done */
	return JNI_OK;
}

/*
 * Scan a newly loaded ELF file and do the following:
 *
 * - Find all _jc_type definitions therein by scanning the symbol table
 *   and put them in the ELF file's unloaded types tree.
 *
 * - Compute the ending address of all method functions, which is
 *   computed using the 'size' attribute of the corresponding ELF symbol
 *
 * - Create the PC -> Java line number table index mapping, which is
 *   computed using line number information in the ELF debug section
 *
 * This assumes that the initial intra-file ELF linking has been done.
 *
 * If unsuccessful, an exception is stored.
 */
static jint
_jc_elf_process(_jc_env *env, _jc_elf *elf)
{
	_jc_elf_info *const info = elf->info;
	_jc_method_node *node;
	_jc_method_node key;
	_jc_splay_tree methods;
	_jc_method *method;
	_jc_uni_mem uni;
	int i;

	/* Initialize temporary method info tree */
	_jc_splay_init(&methods, _jc_method_node_compare,
	    _JC_OFFSETOF(_jc_method_node, node));

	/* Create temporary uni-allocator to hold _jc_method_node's */
	_jc_uni_alloc_init(&uni, 0, NULL);

	/* Scan symbol table and gather information about types & methods */
	for (i = 0; i < info->num_symbols; i++) {
		const Elf_Sym *const sym = &info->symbols[i];
		const char *sym_name = info->strings + sym->st_name;
		const _jc_elf_loadable *sym_section = NULL;
		const char *s;
		void *addr;

		/* Check symbol type */
		switch (sym->st_shndx) {
		case SHN_ABS:
			break;
		case SHN_UNDEF:
		case SHN_COMMON:
			continue;
		default:
			/* Sanity check the symbol's section */
			if (sym->st_shndx >= info->ehdr->e_shnum
			    || (sym_section
			      = info->shdr2section[sym->st_shndx]) == NULL)
				continue;
			break;
		}

		/* Check if symbol is a JC symbol */
		if (strncmp(sym_name, "_jc_", 4) != 0
		    || (s = strchr(sym_name + 4, '$')) == NULL)
			continue;

		/* Get symbol's value */
		switch (sym->st_shndx) {
		case SHN_ABS:				/* not likely! */
			addr = (void *)sym->st_value;
			break;
		default:
			addr = sym_section->vaddr + sym->st_value;
			break;
		}

		/* Check if symbol is for a _jc_type */
		if (strcmp(s + 1, "type") == 0) {
			_jc_type *const type = addr;

			_jc_splay_insert(&elf->types, type);
			type->loader = elf->loader;
			type->u.nonarray.u.elf = elf;
			continue;
		}

		/* Check if symbol is for a _jc_method or method function */
		if (strncmp(s + 1, "method", 6) != 0)
			continue;
		key.cname = sym_name + 4;
		key.clen = s - key.cname;
		s += 7;					/* skip "$method" */
		if (*s == '$') {			/* function symbol */
			key.mname = s + 1;
			method = NULL;
		} else if (strncmp(s, "_info$", 6) == 0) {
			key.mname = s + 6;		/* _jc_method symbol */
			method = (_jc_method *)addr;
		} else
			continue;			/* shouldn't happen */
		key.mlen = strlen(key.mname);

		/* Find/create method info node in tree */
		if ((node = _jc_splay_find(&methods, &key)) == NULL) {

			/* Don't create nodes for non-concrete methods */
			if (method != NULL && method->function == NULL)
				continue;

			/* Create new node */
			if ((node = _jc_uni_zalloc(env,
			    &uni, sizeof(*node))) == NULL)
				goto fail;
			node->cname = key.cname;
			node->clen = key.clen;
			node->mname = key.mname;
			node->mlen = key.mlen;
			_jc_splay_insert(&methods, node);
		}

		/* Update node with method function size or method pointer */
		if (method == NULL) {
			_JC_ASSERT(node->size == 0);
			_JC_ASSERT(sym->st_size > 0);
			node->size = sym->st_size;
		} else {
			_JC_ASSERT(node->method == NULL);
			node->method = method;
		}

		/* Compute function ending address if we have all the info */
		if (node->method != NULL && node->size > 0) {
			_JC_ASSERT(node->method->function != NULL);
			_JC_ASSERT(!_JC_ACC_TEST(node->method, INTERP));
			node->method->u.exec.function_end
			    = (const char *)node->method->function + node->size;
		}
	}

	/* Process any debug section containing line number info */
	switch (info->debug_lines.type) {
	case _JC_LINE_DEBUG_DWARF2:
		if (_jc_debug_line_dwarf2(env, elf, &methods) != JNI_OK)
			goto fail;
		break;
	case _JC_LINE_DEBUG_STABS:
		if (_jc_debug_line_stabs(env, elf, &methods) != JNI_OK)
			goto fail;
		break;
	default:
		break;
	}

	/* Done */
	_jc_uni_alloc_free(&uni);
	return JNI_OK;

fail:
	/* Clean up after failure */
	_jc_uni_alloc_free(&uni);
	return JNI_ERR;
}

