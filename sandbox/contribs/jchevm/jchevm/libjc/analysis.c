
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

#include "libjc.h"

/* Internal functions */
static jint	_jc_compute_stack_depth2(_jc_env *env,
			const _jc_depth_state *state0, int ip);

/*
 * Compute Java operand stack depth at the start of each instruction.
 *
 * Stores an exeption on error.
 */
jint
_jc_compute_stack_depth(_jc_env *env, _jc_method_code *code, int *depth)
{
	_jc_depth_state state;
	int i;

	/* Initialize state */
	memset(&state, 0, sizeof(state));
	state.code = code;
	state.depth = depth;
	if ((state.retsp = _JC_STACK_ALLOC(env,
	    code->num_insns * sizeof(*state.retsp))) == NULL)
		return JNI_ERR;
	memset(state.depth, 0xff, code->num_insns * sizeof(*state.depth));
	memset(state.retsp, 0xff, code->num_insns * sizeof(*state.retsp));

	/* Flow from entry point */
	if (_jc_compute_stack_depth2(env, &state, 0) != JNI_OK)
		return JNI_ERR;

	/* Flow from each trap target */
	state.sp = 1;
	for (i = 0; i < code->num_traps; i++) {
		_jc_interp_trap *const trap = &code->traps[i];

		if (_jc_compute_stack_depth2(env,
		    &state, trap->target - code->insns) != JNI_OK)
			return JNI_ERR;
	}

	/* Done */
	return JNI_OK;
}

static jint
_jc_compute_stack_depth2(_jc_env *env, const _jc_depth_state *state0, int ip)
{
	_jc_method_code *const code = state0->code;
	_jc_depth_state state;

	/* Initialize my own state based on given state */
	memset(&state, 0, sizeof(state));
	state.sp = state0->sp;
	state.ip = ip;
	state.code = state0->code;
	state.depth = state0->depth;
	state.retsp = state0->retsp;
	if ((state.locals = _JC_STACK_ALLOC(env, (code->max_locals
	    + code->max_stack) * sizeof(*state.locals))) == NULL)
		return JNI_ERR;
	state.stack = state.locals + code->max_locals;
	if (state0->locals == NULL) {
		memset(state.locals, 0xff, (code->max_locals
		    + code->max_stack) * sizeof(*state.locals));
	} else {
		memcpy(state.locals, state0->locals, (code->max_locals
		    + code->max_stack) * sizeof(*state.locals));
	}

	/* Flow forward */
	while (JNI_TRUE) {
		_jc_insn *const insn = &code->insns[state.ip];
		_jc_insn_info *const info = &insn->info;
		int delta;

		/* Sanity check */
		_JC_ASSERT(state.ip >= 0 && state.ip < code->num_insns);
		_JC_ASSERT(state.sp >= 0 && state.sp <= code->max_stack);

		/* Have we already done this chunk? */
		if (state.depth[state.ip] != -1) {
			_JC_ASSERT(state.depth[state.ip] == state.sp);
			break;
		}
		state.depth[state.ip] = state.sp;

		/*
		 * Update stack and locals state. We only care about
		 * operations that can affect "returnAddress" values,
		 * which we must track in order to handle "ret".
		 */
		switch (insn->action) {
		case _JC_astore:
			_JC_ASSERT(state.sp >= 1);
			state.locals[info->local] = state.stack[state.sp - 1];
			break;
		case _JC_dup:
			_JC_ASSERT(state.sp >= 1);
			state.stack[0] = state.stack[-1];
			break;
		case _JC_dup_x1:
			_JC_ASSERT(state.sp >= 2);
			state.stack[0] = state.stack[-1];
			state.stack[-1] = state.stack[-2];
			state.stack[-2] = state.stack[0];
			break;
		case _JC_dup_x2:
			_JC_ASSERT(state.sp >= 3);
			state.stack[0] = state.stack[-1];
			state.stack[-1] = state.stack[-2];
			state.stack[-2] = state.stack[-3];
			state.stack[-3] = state.stack[0];
			break;
		case _JC_dup2:
			_JC_ASSERT(state.sp >= 2);
			state.stack[1] = state.stack[-1];
			state.stack[0] = state.stack[-2];
			break;
		case _JC_dup2_x1:
			_JC_ASSERT(state.sp >= 3);
			state.stack[1] = state.stack[-1];
			state.stack[0] = state.stack[-2];
			state.stack[-1] = state.stack[-3];
			state.stack[-2] = state.stack[1];
			state.stack[-3] = state.stack[0];
			break;
		case _JC_dup2_x2:
			_JC_ASSERT(state.sp >= 4);
			state.stack[1] = state.stack[-1];
			state.stack[0] = state.stack[-2];
			state.stack[-1] = state.stack[-3];
			state.stack[-2] = state.stack[-4];
			state.stack[-3] = state.stack[1];
			state.stack[-4] = state.stack[0];
			break;
		case _JC_jsr:
			state.stack[0] = state.ip + 1;
			break;
		case _JC_swap:
		    {
		    	int temp;

			_JC_ASSERT(state.sp >= 2);
			temp = state.stack[-2];
			state.stack[-2] = state.stack[-1];
			state.stack[-1] = temp;
			break;
		    }
		}

		/* Update stack pointer */
		switch (insn->action) {
		case _JC_getstatic:
			delta = _jc_dword_type[_jc_sig_types[
			    (u_char)*info->field.field->signature]] ? 2 : 1;
			break;
		case _JC_invokeinterface:
		case _JC_invokespecial:
		case _JC_invokestatic:
		case _JC_invokevirtual:
		    {
			_jc_method *const method = info->invoke.method;
		    	u_char ptype;

			delta = -info->invoke.pop;
			ptype = method->param_ptypes[method->num_parameters];
			if (ptype != _JC_TYPE_VOID)
				delta += _jc_dword_type[ptype] ? 2 : 1;
			break;
		    }
		case _JC_multianewarray:
			delta = -info->multianewarray.dims + 1;
			break;
		case _JC_putstatic:
			delta = _jc_dword_type[_jc_sig_types[
			    (u_char)*info->field.field->signature]] ? -2 : -1;
			break;
		default:
			delta = (signed char)(_jc_bytecode_stackadj[
			    insn->action] ^ _JC_STACKADJ_INVALID);
			_JC_ASSERT(delta >= -4 && delta <= 2);
			break;
		}
		state.sp += delta;

		/* Flow to next instruction(s) */
		switch (insn->action) {
		case _JC_goto:
			state.ip = info->target - code->insns;
			break;
		case _JC_areturn:
		case _JC_dreturn:
		case _JC_freturn:
		case _JC_ireturn:
		case _JC_lreturn:
		case _JC_return:
		case _JC_athrow:
			return JNI_OK;
		case _JC_if_acmpeq:
		case _JC_if_acmpne:
		case _JC_if_icmpeq:
		case _JC_if_icmpne:
		case _JC_if_icmplt:
		case _JC_if_icmpge:
		case _JC_if_icmpgt:
		case _JC_if_icmple:
		case _JC_ifeq:
		case _JC_ifne:
		case _JC_iflt:
		case _JC_ifge:
		case _JC_ifgt:
		case _JC_ifle:
		case _JC_ifnonnull:
		case _JC_ifnull:
			if (_jc_compute_stack_depth2(env,
			    &state, info->target - code->insns) != JNI_OK)
				return JNI_ERR;
			state.ip++;
			break;
		case _JC_jsr:

			/* Flow into subroutine */
			if (_jc_compute_stack_depth2(env,
			    &state, info->target - code->insns) != JNI_OK)
				return JNI_ERR;

			/* Retrieve our new stack depth */
			_JC_ASSERT(state.retsp[info->target - code->insns] != -1);
			state.sp = state.retsp[info->target - code->insns];

			/* Continue with next instruction */
			state.ip++;
			break;
		case _JC_lookupswitch:
		    {
		    	_jc_lookupswitch *const lsw = info->lookupswitch;
			int i;

			for (i = 0; i < lsw->num_pairs; i++) {
				_jc_lookup *const entry = &lsw->pairs[i];

				if (_jc_compute_stack_depth2(env, &state,
				    entry->target - code->insns) != JNI_OK)
					return JNI_ERR;
			}
			state.ip = lsw->default_target - code->insns;
			break;
		    }
		case _JC_ret:
		    {
		    	int jsr_ip;
		    	int sub_ip;

			/* Let JSR instruction know what SP it gets back */
			jsr_ip = state.locals[info->local] - 1;
			_JC_ASSERT(jsr_ip >= 0 && jsr_ip <= code->num_insns);
			_JC_ASSERT(code->insns[jsr_ip].action == _JC_jsr);
			sub_ip = code->insns[jsr_ip].info.target - code->insns;
			_JC_ASSERT(sub_ip >= 0 && sub_ip <= code->num_insns);
			state.retsp[sub_ip] = state.sp;
			return JNI_OK;
		    }
		case _JC_tableswitch:
		    {
		    	_jc_tableswitch *const tsw = info->tableswitch;
			int i;

			for (i = 0; i < tsw->high - tsw->low + 1; i++) {
				if (_jc_compute_stack_depth2(env, &state,
				    tsw->targets[i] - code->insns) != JNI_OK)
					return JNI_ERR;
			}
			state.ip = tsw->default_target - code->insns;
			break;
		    }
		default:
			state.ip++;
			break;
		}
	}

	/* Done */
	return JNI_OK;
}


