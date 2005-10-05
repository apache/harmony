
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
 * $Id: debug_line.c,v 1.7 2005/03/13 02:59:43 archiecobbs Exp $
 */

#include "libjc.h"

/* Internal functions */
static jint		_jc_debug_line_add(_jc_env *env, _jc_map_state *state,
				const void *pc, uint32_t cline);
static jint		_jc_debug_line_finish(_jc_env *env, _jc_method *method,
				_jc_class_loader *loader, _jc_map_state *state);
static Elf_Word		_jc_read_leb128(const u_char **datap, int is_signed);
static int		_jc_method_addr_cmp(const void *v1, const void *v2);

/*
 * Process stabs line number information.
 *
 * If unsuccessful an exception is stored.
 */
jint
_jc_debug_line_stabs(_jc_env *env, _jc_elf *elf, _jc_splay_tree *tree)
{
	_jc_elf_info *const info = elf->info;
	const Elf_Shdr *const shdr = info->debug_lines.loadable.shdr;
	_jc_stab *const stabs = (_jc_stab *)(info->map_base + shdr->sh_offset);
	const int num_stabs = shdr->sh_size / sizeof(*stabs);
	_jc_map_state state;
	_jc_method *method;
	int i;

	/* Sanity check */
	_JC_ASSERT(info->debug_lines.type == _JC_LINE_DEBUG_STABS);

	/* Initialize state */
	memset(&state, 0, sizeof(state));
	method = NULL;

	/* Run through stabs section */
	for (i = 0; i < num_stabs; i++) {
		_jc_stab *const stab = &stabs[i];

		/* Check type */
		switch (stab->type) {
		case STAB_FUN:
		    {
			_jc_method_node *node;
			_jc_method_node key;
			const char *fun;
			const char *s;

			/* Check for end of method; reset state if so */
			if (stab->sindex == 0) {

				/* Were we skipping this method? */
				if (method == NULL)
					continue;

				/* Finalize map */
				if (_jc_debug_line_finish(env,
				    method, elf->loader, &state) != JNI_OK)
					goto fail;
				method = NULL;
				break;
			}

			/* Already doing this function? */
			if (method != NULL)
				continue;

			/* Get FUN string containing function name */
			fun = (const char *)info->debug_lines.strings
			    + stab->sindex;

			/* Sanity check not already within a method */
			if (method != NULL) {
				_JC_EX_STORE(env, LinkageError,
				    "double opening stabs FUN entry `%s'", fun);
				goto fail;
			}

			/* Try to parse out class & method name from symbol */
			if (strncmp(fun, "_jc_", 4) != 0
			    || (s = strchr(fun + 4, '$')) == NULL
			    || strncmp(s + 1, "method$", 7) != 0)
				continue;
			key.cname = fun + 4;
			key.clen = s - key.cname;
			key.mname = s + 8;
			if ((s = strchr(key.mname, ':')) == NULL)
				s += strlen(s);		/* can this happen? */
			key.mlen = s - key.mname;

			/* Find corresponding method node, if any */
			if ((node = _jc_splay_find(tree, &key)) == NULL)
				continue;
			method = node->method;
			_JC_ASSERT(method != NULL);
			_JC_ASSERT(method->function != NULL);
			_JC_ASSERT(!_JC_ACC_TEST(method, INTERP));
			_JC_ASSERT(method->u.exec.function_end != NULL);

			/* If method has no line number table, nothing to do */
			if (method->u.exec.u.linenum.len == 0) {
				memset(&method->u.exec.u,
				    0, sizeof(method->u.exec.u));
				method = NULL;
				continue;
			}

			/* Initialize state for this method */
			_JC_ASSERT(state.pc_map.len == 0);
			_JC_ASSERT(state.last_linenum == 0);
			_JC_ASSERT(state.last_map == 0);
			state.linenum = method->u.exec.u.linenum;
			memset(&method->u.exec.u, 0, sizeof(method->u.exec.u));
			break;
		    }
		case STAB_SLINE:

			/* If skipping this method, skip lines */
			if (method == NULL)
				continue;

			/* Add entry */
			if (_jc_debug_line_add(env, &state,
			    (const char *)method->function + stab->value,
			    stab->desc) != JNI_OK)
				goto fail;
			break;
		default:
			break;
		}
	}

	/* Clean up and exit */
	_jc_vm_free(&state.pc_map.map);
	return JNI_OK;

fail:
	/* Clean up after failure */
	_jc_vm_free(&state.pc_map.map);
	return JNI_ERR;
}

/*
 * Read a DWARF leb128 value.
 */
static inline Elf_Word
_jc_read_leb128(const u_char **datap, int is_signed)
{
	Elf_Word value; 
	int bitpos;

	for (bitpos = value = 0; ; bitpos += 7) {
		const u_char byte = *(*datap)++;

		value |= (byte & 0x7f) << bitpos;
		if ((byte & 0x80) == 0)
			break;
	}
	if (is_signed && (value & (1 << (bitpos + 6))) != 0)
		value |= (Elf_Word)~0L << (bitpos + 7);
	return value;
}

/*
 * Process DWARF2 line number information.
 *
 * If unsuccessful an exception is stored.
 */
jint
_jc_debug_line_dwarf2(_jc_env *env, _jc_elf *elf, _jc_splay_tree *tree)
{
	_jc_elf_info *const info = elf->info;
	const Elf_Shdr *const shdr = info->debug_lines.loadable.shdr;
	const u_char *ptr = (const u_char *)info->map_base + shdr->sh_offset;
	const u_char *const section_end = ptr + shdr->sh_size;
	jboolean using64bit = JNI_FALSE;
	_jc_dwarf2_line_hdr *hdr;
	_jc_method_node **nodes;
	_jc_map_state state;
	_jc_method *method;
	unsigned long totlen;
	union _jc_value jvalue;
	int node_index;
	const u_char *ptr_end;
	const u_char *pc;
	int num_nodes;
	int cline;
	int i;

	/* Put nodes in a list and elide methods with no line number table */
	if ((nodes = _JC_STACK_ALLOC(env,
	    tree->size * sizeof(*nodes))) == NULL)
		goto fail;
	_jc_splay_list(tree, (void **)nodes);
	for (i = num_nodes = 0; i < tree->size; i++) {
		_jc_method_node *const node = nodes[i];
		_jc_method *const method = node->method;

		_JC_ASSERT(method != NULL);
		_JC_ASSERT(method->function != NULL);
		_JC_ASSERT(!_JC_ACC_TEST(method, INTERP));
		_JC_ASSERT(method->u.exec.function_end != NULL);
		if (method->u.exec.u.linenum.len > 0)
			nodes[num_nodes++] = nodes[i];
	}

	/* Anything to do? */
	if (num_nodes == 0)
		return JNI_OK;

	/* Sort methods by starting address */
	qsort(nodes, num_nodes, sizeof(*nodes), _jc_method_addr_cmp);

	/* Initialize map state */
	memset(&state, 0, sizeof(state));
	node_index = 0;
	method = NULL;

again:
	/* Read prologue header */
	memcpy(&jvalue, ptr, sizeof(jint));
	ptr += sizeof(jint);
	totlen = jvalue.i;
	if (totlen == 0xffffffff) {
		memcpy(&jvalue, ptr, sizeof(jlong));
		ptr += sizeof(jlong);
		totlen = jvalue.j;
		using64bit = JNI_TRUE;
	} else if (totlen == 0) {
		memcpy(&jvalue, ptr, sizeof(jint));
		ptr += sizeof(jint);
		totlen = jvalue.i;
		using64bit = JNI_TRUE;
	}
	ptr_end = ptr + totlen;
	ptr += 2;					/* skip version */
	ptr += 4;					/* skip header len */
	if (using64bit)
		ptr += 4;
	hdr = (_jc_dwarf2_line_hdr *)ptr;
	ptr += sizeof(*hdr) + hdr->opcode_base - 1;

	/* Skip over directory table */
	while (*ptr++ != '\0')
		ptr += strlen((const char *)ptr) + 1;

	/* Skip over file name table */
	while (*ptr++ != '\0') {
		ptr += strlen((const char *)ptr) + 1;
		(void)_jc_read_leb128(&ptr, JNI_FALSE);
		(void)_jc_read_leb128(&ptr, JNI_FALSE);
		(void)_jc_read_leb128(&ptr, JNI_FALSE);
	}

	/* Initialize statement program state */
	pc = NULL;
	cline = 1;

	/* Process statement program */
	while (ptr < ptr_end) {
		jboolean writeout = JNI_FALSE;
		jboolean reset = JNI_FALSE;
		u_char opcode;

		if ((opcode = *ptr++) >= hdr->opcode_base) {	/* special */
			opcode -= hdr->opcode_base;
			pc += (opcode / hdr->line_range)
			    * hdr->minimum_instruction_length;
			cline += hdr->line_base + opcode % hdr->line_range;
			writeout = JNI_TRUE;
		} else if (opcode == 0) {			/* extended */
			unsigned int oplen;
			u_char exop;

			oplen = _jc_read_leb128(&ptr, JNI_FALSE);
			exop = *ptr++;
			switch (exop) {
			case DW_LNE_end_sequence:
				reset = JNI_TRUE;
				writeout = JNI_TRUE;
				break;
			case DW_LNE_set_address:
			    {
				const u_char *new_pc;

				/* Get new PC */
				memcpy(&new_pc, ptr, sizeof(new_pc));

				/* We don't support out-of-spec reversals */
				if (new_pc < pc) {
					_JC_EX_STORE(env, LinkageError,
					    "address reversals in .debug_line"
					    " section are not supported");
					goto fail;
				}

				/* OK */
				pc = new_pc;
				break;
			    }
			default:
				break;
			}
			ptr += oplen - 1;
		} else {					/* standard */
			switch (opcode) {
			case DW_LNS_copy:
				writeout = JNI_TRUE;
				break;
			case DW_LNS_advance_pc:
				pc += _jc_read_leb128(&ptr, JNI_FALSE)
				    * hdr->minimum_instruction_length;
				break;
			case DW_LNS_advance_line:
				cline += _jc_read_leb128(&ptr, JNI_TRUE);
				break;
			case DW_LNS_const_add_pc:
				pc += ((255 - hdr->opcode_base)
				      / hdr->line_range)
				    * hdr->minimum_instruction_length;
				break;
			case DW_LNS_fixed_advance_pc:
			    {
				uint16_t advance;

				memcpy(&advance, ptr, 2);
				pc += advance;
				ptr += 2;
				break;
			    }
			default:
				for (i = 0; i < hdr->standard_opcode_lengths[
				    opcode - 1]; i++)
					_jc_read_leb128(&ptr, JNI_FALSE);
				break;
			}
		}

		/* Have we reached the next method? */
		if (method == NULL
		    && (const void *)pc
		      >= nodes[node_index]->method->function) {

			/* Initialize state for this method */
			_JC_ASSERT(state.pc_map.len == 0);
			_JC_ASSERT(state.last_linenum == 0);
			_JC_ASSERT(state.last_map == 0);
			method = nodes[node_index]->method;
			_JC_ASSERT(!_JC_ACC_TEST(method, INTERP));
			state.linenum = method->u.exec.u.linenum;
			memset(&method->u.exec.u, 0, sizeof(method->u.exec.u));
		}

		/* Finished with the current method? */
		if (method != NULL
		    && (const void *)pc >= method->u.exec.function_end) {

			/* Finalize map for current method */
			if (_jc_debug_line_finish(env,
			    method, elf->loader, &state) != JNI_OK)
				goto fail;
			method = NULL;

			/* Look for next method */
			if (++node_index == num_nodes)
				goto done;
		}

		/* Write matrix row */
		if (writeout && method != NULL) {
			if (_jc_debug_line_add(env,
			    &state, pc, cline) != JNI_OK)
				goto fail;
		}

		/* Reset after DW_LNE_end_sequence */
		if (reset) {
			pc = NULL;
			cline = 1;
		}
	}
	if (ptr < section_end)
		goto again;

done:
	/* Sanity check */
	_JC_ASSERT(method == NULL);
#ifndef NDEBUG
	for (i = 0; i < num_nodes; i++) {
		_jc_method_node *const node = nodes[i];
		_jc_method *const method = node->method;

		_JC_ASSERT(!_JC_ACC_TEST(method, INTERP));
		_JC_ASSERT(method->u.exec.u.pc_map.map != NULL);
	}
#endif

	/* Done */
	return JNI_OK;

fail:
	/* Failed */
	return JNI_ERR;
}

/*
 * Map a PC return address within a function to a Java line number.
 * This assumes the PC is within the function. Returns 0 if unknown.
 *
 * The PC return address should point to the instruction just after the
 * instruction that makes the function call.
 */
int
_jc_exec_pc_to_jline(_jc_method *method, const void *pc)
{
	_jc_pc_map_info *const pcmap = &method->u.exec.u.pc_map;
	_jc_pc_map *base;
	int span;

	/* Sanity check */
	_JC_ASSERT(!_JC_ACC_TEST(method, INTERP));

	/* Sanity check that the PC lies within the method */
	_JC_ASSERT(pc > method->function && pc <= method->u.exec.function_end);

	/* If PC map does not exist, bail out */
	if (!_JC_ACC_TEST(method, PCMAP))
		return 0;

	/* Binary search PC map */
	for (base = pcmap->map, span = pcmap->len; span != 0; span >>= 1) {
		_jc_pc_map *const sample = &base[span >> 1];

		if (pc <= sample->pc)
			continue;
		if (pc > (sample + 1)->pc) {
			base = sample + 1;
			span--;
			continue;
		}
		return sample->jline;
	}

	/* Not found */
	return 0;
}

/*
 * Add a new [addr => java line number] pair to the PC map, given
 * an [addr => cline] mapping. We search the line number table to derive
 * the intermediate [cline => line number table entry] mapping. If the
 * address is before the first line number table entry, do nothing.
 *
 * We assume that the entries in the map are delivered roughly in order.
 * Otherwise, this algorithm could be O(n^2) slow.
 *
 * If unsuccessful an exception is stored.
 */
static jint
_jc_debug_line_add(_jc_env *env, _jc_map_state *state,
	const void *pc, uint32_t cline)
{
	int linenum_index;
	int map_index;

	/* Sanity check */
	_JC_ASSERT(state->linenum.len > 0);

	/*
	 * Map C line number into line number table index. We expect
	 * this line number table index to be close to the last one.
	 * If it's not in the table, no mapping is needed.
	 */
	linenum_index = state->last_linenum;
	if (cline > state->linenum.table[linenum_index].cline) {
		while (linenum_index < state->linenum.len - 1
		    && cline >= state->linenum.table[linenum_index + 1].cline)
			linenum_index++;
	} else {
		while (linenum_index > 0
		   && cline < state->linenum.table[linenum_index].cline)
			linenum_index--;
		if (cline < state->linenum.table[0].cline)
			return JNI_OK;
	}

	/*
	 * Find insertion point for the new entry. We expect it
	 * to be near the previous insertion point, if any.
	 */
	map_index = state->last_map;
	if (state->pc_map.len == 0) {
		_JC_ASSERT(map_index == 0);
		goto insert;
	}
	if (pc > state->pc_map.map[map_index].pc) {
		while (map_index < state->pc_map.len
		    && pc >= state->pc_map.map[++map_index].pc)
			;
	} else {
		while (map_index > 0 && pc < state->pc_map.map[map_index].pc)
			map_index--;
	}

insert:
	/* Extend PC map to make room for the new entry */
	if (state->pc_map.len == state->map_alloc) {
		void *mem;

		state->map_alloc = state->map_alloc * 2 + 32;
		if ((mem = _jc_vm_realloc(env, state->pc_map.map,
		    state->map_alloc * sizeof(*state->pc_map.map))) == NULL)
			return JNI_ERR;
		state->pc_map.map = mem;
	}

	/* Shift higher entries up by one to make room */
	if (map_index < state->pc_map.len) {
		memmove(state->pc_map.map + map_index + 1,
		    state->pc_map.map + map_index,
		    (state->pc_map.len - map_index)
		      * sizeof(*state->pc_map.map));
	}

	/* Fill in new entry */
	state->pc_map.map[map_index].pc = pc;
	state->pc_map.map[map_index].jline
	    = state->linenum.table[linenum_index].jline;

	/* Update state */
	state->last_linenum = linenum_index;
	state->last_map = map_index;
	state->pc_map.len++;

	/* Done */
	return JNI_OK;
}

/*
 * Finalize a PC map and store it with its method.
 *
 * If unsuccessful an exception is stored.
 */
static jint
_jc_debug_line_finish(_jc_env *env, _jc_method *method,
	_jc_class_loader *loader, _jc_map_state *state)
{
	_jc_pc_map_info *const pcmap = &method->u.exec.u.pc_map;
	int num_dups;
	int i;
	int j;

	/* Sanity check */
	_JC_ASSERT(!_JC_ACC_TEST(method, INTERP));

	/* TODO XXX understand why this happens */
	if (_JC_ACC_TEST(method, PCMAP))
		goto done;

	/* Count number of redundant entries */
	for (num_dups = 0, i = 0; i < state->pc_map.len - 1; i++) {
		_jc_pc_map *const this = &state->pc_map.map[i];
		_jc_pc_map *const next = &state->pc_map.map[i + 1];

		_JC_ASSERT(this->pc <= next->pc);
		if (this->jline == next->jline)
			num_dups++;
	}

	/* Allocate memory for compressed PC map */
	_JC_ASSERT(pcmap->map == NULL);
	pcmap->len = state->pc_map.len - num_dups;
	_JC_MUTEX_LOCK(env, loader->mutex);
	pcmap->map = _jc_cl_alloc(env, loader,
	    (pcmap->len + 1) * sizeof(*state->pc_map.map));
	_JC_MUTEX_UNLOCK(env, loader->mutex);
	if (pcmap->map == NULL)
		return JNI_ERR;

	/* Copy non-redundant entries */
	for (i = j = 0; i < state->pc_map.len; j++) {
		memcpy(&pcmap->map[j],
		    &state->pc_map.map[i], sizeof(*state->pc_map.map));
		while (++i < state->pc_map.len
		    && state->pc_map.map[i].jline
		      == state->pc_map.map[i - 1].jline);
	}
	_JC_ASSERT(j == pcmap->len);

	/* Add terminating entry */
	pcmap->map[j].pc = method->u.exec.function_end;
	pcmap->map[j].jline = 0;

#if 0
    {
	printf("MAP FOR `%s.%s%s':\n",
	    method->class->name, method->name, method->signature);
	for (i = 0; i < pcmap->len; i++) {
		_jc_pc_map *const map = &pcmap->map[i];

		printf("    [%d] %p -> %d\n", i, map->pc, map->jline);
	}
    }
#endif

done:
	/* Reset map state */
	state->pc_map.len = 0;
	state->last_linenum = 0;
	state->last_map = 0;
	state->linenum.len = 0;
	state->linenum.table = NULL;

	/* Done */
	method->access_flags |= _JC_ACC_PCMAP;
	return JNI_OK;
}

static int
_jc_method_addr_cmp(const void *v1, const void *v2)
{
	_jc_method_node *const node1 = *((_jc_method_node **)v1);
	_jc_method_node *const node2 = *((_jc_method_node **)v2);

	return (node1->method->function > node2->method->function)
	    - (node1->method->function < node2->method->function);
}


