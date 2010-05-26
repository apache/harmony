/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.text.tests.java.text;

import java.text.CollationElementIterator;
import java.text.Collator;
import java.text.RuleBasedCollator;
import java.text.StringCharacterIterator;
import java.util.Locale;

import junit.framework.TestCase;
import tests.support.Support_Excludes;

/**
 * Test CollationElementIterator
 * 
 * Only test normal condition.
 * 
 */
public class CollationElementIteratorTest extends TestCase {

	private RuleBasedCollator coll;

	protected void setUp() {
		coll = (RuleBasedCollator) Collator.getInstance(Locale.US);
	}

	public void testGetOffset() {
	    if (Support_Excludes.isExcluded()) {
	        return;
	    }

		String text = "abc";
		CollationElementIterator iterator = coll
				.getCollationElementIterator(text);
		int[] offsets = { 0, 1, 2, 3 };
		int offset = iterator.getOffset();
		int i = 0;
		assertEquals(offsets[i++], offset);
		while (offset != text.length()) {
			iterator.next();
			offset = iterator.getOffset();
			assertEquals(offsets[i++], offset);
		}
	}

	public void testNext() {
	    if (Support_Excludes.isExcluded()) {
	        return;
	    }

		String text = "abc";
		CollationElementIterator iterator = coll
				.getCollationElementIterator(text);
		int[] orders = new int[text.length()];
		int order = iterator.next();
		int i = 0;
		while (order != CollationElementIterator.NULLORDER) {
			orders[i++] = order;
			order = iterator.next();
		}

		int offset = iterator.getOffset();
		assertEquals(text.length(), offset);
		order = iterator.previous();

		while (order != CollationElementIterator.NULLORDER) {
			assertEquals(orders[--i], order);
			order = iterator.previous();
		}

		assertEquals(0, iterator.getOffset());
	}

	public void testReset() {
	    if (Support_Excludes.isExcluded()) {
	        return;
	    }

		String text = "abc";
		CollationElementIterator iterator = coll
				.getCollationElementIterator(text);
		int[] orders = new int[text.length()];
		int order = iterator.next();
		int i = 0;
		while (order != CollationElementIterator.NULLORDER) {
			orders[i++] = order;
			order = iterator.next();
		}

		int offset = iterator.getOffset();
		assertEquals(text.length(), offset);

		iterator.reset();
		assertEquals(0, iterator.getOffset());
	}

	public void testGetMaxExpansion() {
	    if (Support_Excludes.isExcluded()) {
	        return;
	    }

		String text = "cha";
		RuleBasedCollator rbColl = (RuleBasedCollator) Collator
				.getInstance(new Locale("es", "", "TRADITIONAL"));
		CollationElementIterator iterator = rbColl
				.getCollationElementIterator(text);
		int order = iterator.next();
		while (order != CollationElementIterator.NULLORDER) {
			assertEquals(1, iterator.getMaxExpansion(order));
			order = iterator.next();
		}

	}

	public void testPrimaryOrder() {
	    if (Support_Excludes.isExcluded()) {
	        return;
	    }

		RuleBasedCollator rbColl = (RuleBasedCollator) Collator
				.getInstance(new Locale("de", "DE"));
		String text = "\u00e6";
		CollationElementIterator iterator = rbColl
				.getCollationElementIterator(text);
		int order = iterator.next();
		int pOrder = CollationElementIterator.primaryOrder(order);
		CollationElementIterator iterator2 = rbColl
				.getCollationElementIterator("ae");
		int order2 = iterator2.next();
		int pOrder2 = CollationElementIterator.primaryOrder(order2);
		assertEquals(pOrder, pOrder2);
	}

	public void testSecondaryOrder() {
	    if (Support_Excludes.isExcluded()) {
	        return;
	    }

		RuleBasedCollator rbColl = (RuleBasedCollator) Collator
				.getInstance(new Locale("fr", "FR"));
		String text = "a\u00e0";
		CollationElementIterator iterator = rbColl
				.getCollationElementIterator(text);
		int order = iterator.next();
		int sOrder1 = CollationElementIterator.secondaryOrder(order);

		order = iterator.next();
		int sOrder2 = CollationElementIterator.secondaryOrder(order);

		assertEquals(sOrder1, sOrder2);
	}

	public void testTertiaryOrder() {
	    if (Support_Excludes.isExcluded()) {
	        return;
	    }

		RuleBasedCollator rbColl = (RuleBasedCollator) Collator
				.getInstance(new Locale("fr", "FR"));
		String text = "abAB";
		CollationElementIterator iterator = rbColl
				.getCollationElementIterator(text);
		int order = iterator.next();
		int tOrder1 = CollationElementIterator.tertiaryOrder(order);
		order = iterator.next();
		int tOrder2 = CollationElementIterator.tertiaryOrder(order);
		assertEquals(tOrder1, tOrder2);

		order = iterator.next();
		tOrder1 = CollationElementIterator.tertiaryOrder(order);
		order = iterator.next();
		tOrder2 = CollationElementIterator.tertiaryOrder(order);
		assertEquals(tOrder1, tOrder2);
	}

	public void testSetOffset() {
	    if (Support_Excludes.isExcluded()) {
	        return;
	    }

		RuleBasedCollator rbColl = (RuleBasedCollator) Collator
				.getInstance(new Locale("es", "", "TRADITIONAL"));
		String text = "cha";
		CollationElementIterator iterator = rbColl
				.getCollationElementIterator(text);
		iterator.setOffset(1);
		assertEquals(1, iterator.getOffset());
	}

	/*
	 * Class under test for void setText(java.lang.String)
	 */
	public void testSetTextString() {
	    if (Support_Excludes.isExcluded()) {
	        return;
	    }

		RuleBasedCollator rbColl = (RuleBasedCollator) Collator
				.getInstance(new Locale("es", "", "TRADITIONAL"));
		String text = "caa";
		CollationElementIterator iterator = rbColl
				.getCollationElementIterator(text);
		iterator.setOffset(1);
		assertEquals(1, iterator.getOffset());
		iterator.setText("cha");
		iterator.setOffset(1);
		assertEquals(1, iterator.getOffset());
	}

	/*
	 * Class under test for void setText(java.text.CharacterIterator)
	 */
	public void testSetTextCharacterIterator() {
	    if (Support_Excludes.isExcluded()) {
	        return;
	    }

		RuleBasedCollator rbColl = (RuleBasedCollator) Collator
				.getInstance(new Locale("es", "", "TRADITIONAL"));
		String text = "caa";
		CollationElementIterator iterator = rbColl
				.getCollationElementIterator(text);
		iterator.setOffset(1);
		assertEquals(1, iterator.getOffset());
		iterator.setText(new StringCharacterIterator("cha"));
		iterator.setOffset(1);
		assertEquals(1, iterator.getOffset());
	}
}
