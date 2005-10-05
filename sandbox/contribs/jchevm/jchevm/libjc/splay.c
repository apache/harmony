
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
 * $Id: splay.c,v 1.4 2004/07/26 01:41:37 archiecobbs Exp $
 */

#include "libjc.h"

/* Internal functions */
static _jc_splay_node	*_jc_splay(_jc_splay_tree *tree, _jc_splay_node *node,
				void *key, int *match);

/*
 * Initialize a new splay tree.
 */
void
_jc_splay_init(_jc_splay_tree *tree, _jc_splay_cmp_t *compare, size_t offset)
{
	memset(tree, 0, sizeof(*tree));
	tree->offset = offset;
	tree->compare = compare;
}

/*
 * Splay the node that matches 'key' to the top of the sub-tree rooted
 * at 'node' if found, or is the previous node we'd find if not found.
 */
static _jc_splay_node *
_jc_splay(_jc_splay_tree *tree, _jc_splay_node *node, void *key, int *match)
{
	_jc_splay_node *temp;
	_jc_splay_node *left;
	_jc_splay_node *right;
	_jc_splay_node fixed;
	int diff;

	/* Initialize */
	memset(&fixed, 0, sizeof(fixed));
	left = right = &fixed;
	*match = 0;

	/* Handle empty tree */
	if (node == NULL)
		return NULL;

	/* Search & splay */
	while (node != NULL) {
		_JC_ASSERT(node->inserted);
		diff = (*tree->compare)(key, _JC_NODE2ITEM(tree, node));
		if (diff < 0) {
			if (node->left == NULL)
				break;
			diff = (*tree->compare)(key,
			    _JC_NODE2ITEM(tree, node->left));
			if (diff < 0) {
				temp = node->left;	/* rotate right */
				node->left = temp->right;
				temp->right = node;
				node = temp;
				if (node->left == NULL)
					break;
			}
			right->left = node;		/* link right */
			right = node;
			node = node->left;
			continue;
		}
		if (diff > 0) {
			if (node->right == NULL)
				break;
			diff = (*tree->compare)(key,
			    _JC_NODE2ITEM(tree, node->right));
			if (diff > 0) {
				temp = node->right;	/* rotate left */
				node->right = temp->left;
				temp->left = node;
				node = temp;
				if (node->right == NULL)
					break;
			}
			left->right = node;		/* link left */
			left = node;
			node = node->right;
			continue;
		}
		*match = 1;
		break;
	}

	/* Reassemble */
	left->right = node->left;
	right->left = node->right;
	node->left = fixed.right;
	node->right = fixed.left;

	/* Return the new root */
	return node;
}

/* 
 * Find an item in the tree.
 */
void *
_jc_splay_find(_jc_splay_tree *tree, void *item)
{
	int match;

	/* Sanity check */
	_JC_ASSERT(item != NULL);

	/* Search for item */
	tree->root = _jc_splay(tree, tree->root, item, &match);
	return match ? _JC_NODE2ITEM(tree, tree->root) : NULL;
}

/*
 * Insert an item into the tree. It must not already be there.
 */
void
_jc_splay_insert(_jc_splay_tree *tree, void *item)
{
	_jc_splay_node *const new_node = _JC_ITEM2NODE(tree, item);
	_jc_splay_node *root = tree->root;
	int match;
	int diff;

	/* Sanity check */
	_JC_ASSERT(new_node->left == NULL);
	_JC_ASSERT(new_node->right == NULL);
	_JC_ASSERT(!new_node->inserted);

	/* Handle empty tree */
	if (root == NULL) {
		new_node->left = NULL;
		new_node->right = NULL;
		goto done;
	}

	/* Splay insertion point up to the root */
	root = tree->root = _jc_splay(tree, root, item, &match);
	_JC_ASSERT(!match);

	/* See where to add new node */
	diff = (*tree->compare)(item, _JC_NODE2ITEM(tree, tree->root));
	_JC_ASSERT(diff != 0);
	if (diff < 0) {
		new_node->left = root->left;
		new_node->right = root;
		root->left = NULL;
	} else {
		new_node->right = root->right;
		new_node->left = root;
		root->right = NULL;
	}

done:
	/* Done */
#ifndef NDEBUG
	new_node->inserted = 1;
#endif
	tree->root = new_node;
	tree->size++;
}

/*
 * Remove an item from the tree. It must be in the tree.
 */
void
_jc_splay_remove(_jc_splay_tree *tree, void *item)
{
#ifndef NDEBUG
	_jc_splay_node *const node = _JC_ITEM2NODE(tree, item);
#endif
	_jc_splay_node *root = tree->root;
	_jc_splay_node *new_root;
	int found;

	/* Sanity check */
	_JC_ASSERT(node->inserted);
	_JC_ASSERT(root != NULL);
	_JC_ASSERT(tree->size > 0);

	/* Splay node to the root */
	root = _jc_splay(tree, root, item, &found);
	_JC_ASSERT(found);

	/* Remove node */
	if (root->left == NULL)
		new_root = root->right;
	else {
		new_root = _jc_splay(tree, root->left, item, &found);
		new_root->right = root->right;
	}

#ifndef NDEBUG
	/* Nuke removed node */
	memset(node, 0, sizeof(*node));
#endif

	/* Done */
	tree->root = new_root;
	tree->size--;
}

/*
 * Generate a sorted list of all items in the tree.
 *
 * This assumes "items" points to an array of length at least tree->size.
 *
 * This algorithm uses the list memory as a stack, which grows upward from
 * the start of the list, while simultaneously building the list itself,
 * which grows downward from the end of the list. Because each node can
 * appear only once in either the stack or the list, the two never meet.
 *
 * The point of doing this is to avoid the stack usage that would result
 * from the typical recursive function approach.
 */
void
_jc_splay_list(_jc_splay_tree *tree, void **items)
{
	_jc_splay_node **const stack = (_jc_splay_node **)items;
	_jc_splay_node *node;
	int depth;		/* next slot for stack (grows upward) */
	int next;		/* next slot for list (grows downward) */
	int i;

	/* Initialize search stack */
	depth = 0;
	next = tree->size;
	node = tree->root;

	/* Recursively expand stack */
	while (depth > 0 || node != NULL) {

		/* Sanity check */
		_JC_ASSERT(depth <= next);

		/* Travelling down the tree */
		if (node != NULL) {
			stack[depth++] = node;
			node = node->right;
			continue;
		}

		/* Travelling back up the tree */
		node = stack[--depth];
		stack[--next] = node;
		node = node->left;
	}

	/* Convert nodes to items */
	for (i = 0; i < tree->size; i++)
		items[i] = _JC_NODE2ITEM(tree, stack[i]);
}

