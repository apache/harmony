
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
 * $Id: dep.c,v 1.3 2005/03/18 22:24:33 archiecobbs Exp $
 */

#include "cfdump.h"

/* A node in a tree of class references */
struct _jc_dep {
	_jc_class_ref	ref;
	_jc_splay_node	node;
};
typedef struct _jc_dep _jc_dep;

static void	add_deps(_jc_env *env, _jc_splay_tree *tree,
			_jc_classfile *cf, int flags);
static _jc_dep	*add_dep(_jc_splay_tree *tree, const char *name,
			size_t name_len);
static void	dump_deps(_jc_splay_tree *tree,
			_jc_splay_node *node, int flags);
static int	_jc_dep_cmp(const void *v1, const void *v2);
static const	char *encode_name(const char *name);

void
do_deps(_jc_env *env, _jc_classfile *cf, int flags)
{
	_jc_splay_tree tree;

	/* Build tree */
	_jc_splay_init(&tree, _jc_dep_cmp, _JC_OFFSETOF(_jc_dep, node));
	add_deps(env, &tree, cf, flags);

	/* Dump tree */
	dump_deps(&tree, tree.root, flags);

	/* Free tree */
	while (tree.root != NULL) {
		_jc_dep *const dep = _JC_NODE2ITEM(&tree, tree.root);

		_jc_splay_remove(&tree, dep);
		free(dep);
	}
}

static void
add_deps(_jc_env *env, _jc_splay_tree *tree, _jc_classfile *cf, int flags)
{
	_jc_classbytes *cb = NULL;
	_jc_classfile *scf;
	_jc_class_ref *deps;
	_jc_class_ref ref;
	int num_deps;
	int i;

	/* Already doing this class? */
	ref.name = cf->name;
	ref.len = strlen(cf->name);
	if (_jc_splay_find(tree, &ref) != NULL)
		return;

	/* Add this class */
	add_dep(tree, cf->name, strlen(cf->name));

	/* Dump superclasses' dependencies */
	if (cf->superclass == NULL)
		goto no_superclass;

	if ((cb = _jc_bootcl_find_classbytes(env, cf->superclass, NULL)) == NULL
	    || (scf = _jc_parse_classfile(env, cb, 2)) == NULL) {
		errx(1, "failed to load classfile: %s: %s",
		    _jc_vmex_names[env->ex.num], env->ex.msg);
	}
	_jc_free_classbytes(&cb);
	add_deps(env, tree, scf, flags);
	_jc_destroy_classfile(&scf);

no_superclass:
	/* Dump superinterfaces' dependencies */
	for (i = 0; i < cf->num_interfaces; i++) {
		if ((cb = _jc_bootcl_find_classbytes(env,
		      cf->interfaces[i], NULL)) == NULL
		    || (scf = _jc_parse_classfile(env, cb, 2)) == NULL) {
			errx(1, "failed to load classfile: %s: %s",
			    _jc_vmex_names[env->ex.num], env->ex.msg);
		}
		_jc_free_classbytes(&cb);
		add_deps(env, tree, scf, flags);
		_jc_destroy_classfile(&scf);
	}

	/* If 'supers_only', we're done */
	if ((flags & DUMP_SUPERS_ONLY) != 0)
		return;

	/* Update flags */
	if ((flags & DUMP_TRANS_CLOSURE) == 0)
		flags |= DUMP_SUPERS_ONLY;

	/* Dump this class's direct dependencies and their supers */
	if ((num_deps = _jc_gen_deplist(env, cf, &deps)) == -1)
		errx(1, "failed to generate dependency list: %s: %s",
		    _jc_vmex_names[env->ex.num], env->ex.msg);
	for (i = 0; i < num_deps; i++) {
		_jc_class_ref *const dep = &deps[i];
		char *name;

		if ((name = malloc(dep->len + 1)) == NULL)
			err(1, "malloc");
		memcpy(name, dep->name, dep->len);
		name[dep->len] = '\0';
		if ((cb = _jc_bootcl_find_classbytes(env, name, NULL)) == NULL
		    || (scf = _jc_parse_classfile(env, cb, 2)) == NULL) {
			errx(1, "failed to load classfile: %s: %s",
			    _jc_vmex_names[env->ex.num], env->ex.msg);
		}
		_jc_free_classbytes(&cb);
		add_deps(env, tree, scf, flags);
		_jc_destroy_classfile(&scf);
		free(name);
	}
	_jc_vm_free(&deps);
}

static _jc_dep *
add_dep(_jc_splay_tree *tree, const char *name, size_t name_len)
{
	_jc_class_ref ref;
	_jc_dep *dep;

	/* See if name is already in the tree */
	ref.name = name;
	ref.len = name_len;
	if ((dep = _jc_splay_find(tree, &ref)) != NULL)
		return dep;

	/* Add name to the tree */
	if ((dep = malloc(sizeof(*dep) + name_len + 1)) == NULL)
		err(1, "malloc");
	memset(dep, 0, sizeof(*dep));
	dep->ref.name = (char *)(dep + 1);
	dep->ref.len = name_len;
	memcpy(dep + 1, name, name_len);
	((char *)(dep + 1))[name_len] = '\0';
	_jc_splay_insert(tree, dep);
	return dep;
}

static void
dump_deps(_jc_splay_tree *tree, _jc_splay_node *node, int flags)
{
	_jc_dep *const dep = _JC_NODE2ITEM(tree, node);

	/* Sanity check */
	if (node == NULL)
		return;

	/* Do left subtree */
	dump_deps(tree, node->left, flags);

	/* Print this class */
	printf("%s\n", (flags & DUMP_ENCODE_NAMES) != 0 ?
	    encode_name(dep->ref.name) : dep->ref.name);

	/* Do right subtree */
	dump_deps(tree, node->right, flags);
}

static const char *
encode_name(const char *name)
{
	static const char hexdig[] = "0123456789abcdef";
	static char *buf;
	const char *s;
	char *t;
	int len;

	for (len = 0, s = name; *s != '\0'; s++) {
		if (isalnum(*s) || *s == '/')
			len++;
		len += 4;
	}
	free(buf);
	if ((buf = malloc(len + 1)) == NULL)
		err(1, "malloc");
	for (s = name, t = buf; *s != '\0'; s++) {
		if (isalnum(*s) || *s == '/') {
			*t++ = *s;
			continue;
		}
		assert(*s != '.');
		*t++ = '_';
		*t++ = '_';
		*t++ = hexdig[(*s >> 4) & 0x0f];
		*t++ = hexdig[*s & 0x0f];
	}
	*t = '\0';
	return buf;
}

static int
_jc_dep_cmp(const void *v1, const void *v2)
{
	const _jc_dep *const dep1 = v1;
	const _jc_dep *const dep2 = v2;

	return _jc_class_ref_compare(&dep1->ref, &dep2->ref);
}

