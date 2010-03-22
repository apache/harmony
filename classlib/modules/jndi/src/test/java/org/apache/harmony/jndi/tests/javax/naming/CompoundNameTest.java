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
package org.apache.harmony.jndi.tests.javax.naming;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.Properties;

import javax.naming.CompositeName;
import javax.naming.CompoundName;
import javax.naming.InvalidNameException;
import javax.naming.Name;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.apache.harmony.jndi.tests.javax.naming.util.Log;

public class CompoundNameTest extends TestCase {

	private Properties props2;

	static Log log = new Log(CompoundNameTest.class);

	private final Properties props = new Properties();

	@Override
    protected void setUp() throws Exception {
		super.setUp();
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.ignorecase", "false");
		props.put("jndi.syntax.trimblanks", "false");
		props.put("jndi.syntax.separator.ava", ",");
		props.put("jndi.syntax.separator.typeval", "=");
		props2 = (Properties) props.clone();
	}

	public void testConstructor_Simple() throws InvalidNameException {
		log.setMethod("testConstructor_Simple()");
		CompoundName name;

		name = new CompoundName("", props);
		assertNameEmpty(name);
		name = new CompoundName("/", props);
		assertNameEquals(name, "");
		name = new CompoundName("//", props);
		assertNameEquals(name, "", "");
		name = new CompoundName("///", props);
		assertNameEquals(name, "", "", "");
		name = new CompoundName("a", props);
		assertNameEquals(name, "a");
		name = new CompoundName("a/b", props);

		assertNameEquals(name, "a", "b");
		name = new CompoundName("a/b", props);
		assertNameEquals(name, "a", "b");
	}

	public void testConstructor_Null_String() throws InvalidNameException {
		log.setMethod("testConstructor_Null_String()");

		try {
			new CompoundName((String) null, props);
			fail("should be null pointer exception");
		} catch (NullPointerException e) {
		}
	}

	public void testConstructor_Null_Props() throws InvalidNameException {
		log.setMethod("testConstructor_Null_Props()");

		try {
			new CompoundName("abc", null);
			fail("should be null pointer exception");
		} catch (NullPointerException e) {
		}
	}

	public void testConstructor_WithProps_nothing() throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_nothing()");
		CompoundName name;
		props.clear();

		name = new CompoundName("a/b/c", props);
		assertNameEquals(name, "a/b/c");
	}

	public void testConstructor_WithProps_D_NotFlat()
			throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_D_NotFlat()");
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");

		try {
			new CompoundName("a/b/c", props);
			fail("has direction but no separator, should throw IllegalArgumentException");
		} catch (IllegalArgumentException e) {
		}
	}

	public void testConstructor_WithProps_D_Flat() throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_D_Flat()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "flat");

		name = new CompoundName("a/b/c", props);
		assertNameEquals(name, "a/b/c");
	}

	public void testConstructor_WithProps_DS() throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DS()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");

		name = new CompoundName("a\\/b/c", props);
		assertNameEquals(name, "a\\", "b", "c");

		name = new CompoundName("'a/a'/b/c", props);
		assertNameEquals(name, "'a", "a'", "b", "c");

		name = new CompoundName("\"a/a\"/b/c", props);
		assertNameEquals(name, "\"a", "a\"", "b", "c");

		name = new CompoundName("<a/a>/b/c", props);
		assertNameEquals(name, "<a", "a>", "b", "c");

		name = new CompoundName("a/b/c", props);
		assertFalse(name.equals(new CompoundName("A/B/C", props)));
		assertFalse(name.equals(new CompoundName(" a / b / c ", props)));
	}

	public void testConstructor_WithProps_DS2() throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DS2()");
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator2", "/");
		try {
			new CompoundName("a/b/c", props);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
		}
	}

	public void testConstructor_WithProps_DSS2() throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSS2()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.separator2", ":");
		name = new CompoundName("a:b/c:d", props);
		assertNameEquals(name, "a", "b", "c", "d");
		assertEquals("a/b/c/d", name.toString());
	}

	public void testConstructor_WithProps_DSS2E() throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSS2E()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.separator2", ":");
		props.put("jndi.syntax.escape", "\\");

		name = new CompoundName("a\\:b\\/c:d", props);
		assertNameEquals(name, "a:b/c", "d");
		assertEquals("a\\:b\\/c/d", name.toString());
	}

	public void testConstructor_WithProps_DSE() throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSE()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");

		name = new CompoundName("a\\/a/b/c", props);
		assertNameEquals(name, "a/a", "b", "c");

		name = new CompoundName("a\\/a/\\b/c", props);
		assertNameEquals(name, "a/a", "\\b", "c");

		name = new CompoundName("\\'a/b/c", props);
		assertNameEquals(name, "\\'a", "b", "c");
	}

	public void testConstructor_WithProps_DSEQ_Bq() throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSEQ_Bq()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "'");

		name = new CompoundName("'a/a'/b/c", props);
		assertNameEquals(name, "a/a", "b", "c");

		name = new CompoundName("a'a/a'a/b/c", props);
		assertNameEquals(name, "a'a", "a'a", "b", "c");

		name = new CompoundName("\\'a/b/c", props);
		assertNameEquals(name, "'a", "b", "c");

		assertInvalidName("'a/a'a/b/c", props);
	}

	public void testConstructor_WithProps_DSEQ_BqEq()
			throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSEQ_BqEq()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");

		name = new CompoundName("<a/a>/b/c", props);
		assertNameEquals(name, "a/a", "b", "c");

		name = new CompoundName("a<a/a>a/b/c", props);
		assertNameEquals(name, "a<a", "a>a", "b", "c");

		name = new CompoundName("\\<a/a>/b/c", props);
		assertNameEquals(name, "<a", "a>", "b", "c");

		name = new CompoundName("<a/a\\>>/b/c", props);
		assertNameEquals(name, "a/a>", "b", "c");

		assertInvalidName("<a/a>a/b/c", props);

		name = new CompoundName("<a\\>a>", props);
		assertNameEquals(name, "a>a");
		assertEquals("a>a", name.toString());

		name = new CompoundName("<a\\<a>", props);
		assertNameEquals(name, "a\\<a");
		assertEquals(new CompoundName(name.toString(), props), name);
		assertEquals("a\\\\<a", name.toString());

		name = new CompoundName("<a\\/a>", props);
		assertNameEquals(name, "a\\/a");
		assertEquals("<a\\/a>", name.toString());
	}

	public void testConstructor_WithProps_DSEQ_Eq() throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSEQ_Eq()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.endquote", ">");

		name = new CompoundName("<a/a>/b/c", props);
		assertNameEquals(name, "<a", "a>", "b", "c");

		name = new CompoundName(">a/a>/b/c", props);
		assertNameEquals(name, "a/a", "b", "c");
		assertEquals(">a/a>/b/c", name.toString());
	}

	public void testConstructor_WithProps_DSEQ_Bq2()
			throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSEQ_Bq2()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote2", "'");

		name = new CompoundName("'a/a'/b/c", props);
		assertNameEquals(name, "a/a", "b", "c");

		name = new CompoundName("a'a/a'a/b/c", props);
		assertNameEquals(name, "a'a", "a'a", "b", "c");

		name = new CompoundName("\\'a/b/c", props);
		assertNameEquals(name, "'a", "b", "c");

		assertInvalidName("'a/a'a/b/c", props);
	}

	public void testConstructor_WithProps_DSEQ_Bq2Eq2()
			throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSEQ_Bq2Eq2()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote2", "<");
		props.put("jndi.syntax.endquote2", ">");

		name = new CompoundName("<a/a>/b/c", props);
		assertNameEquals(name, "a/a", "b", "c");

		name = new CompoundName("a<a/a>a/b/c", props);
		assertNameEquals(name, "a<a", "a>a", "b", "c");

		name = new CompoundName("\\<a/a>/b/c", props);
		assertNameEquals(name, "<a", "a>", "b", "c");

		name = new CompoundName("<a/a\\>>/b/c", props);
		assertNameEquals(name, "a/a>", "b", "c");

		assertInvalidName("<a/a>a/b/c", props);

		name = new CompoundName("<a\\>a>", props);
		assertNameEquals(name, "a>a");
		assertEquals("a>a", name.toString());

		name = new CompoundName("<a\\<a>", props);
		assertNameEquals(name, "a\\<a");
		assertEquals(new CompoundName(name.toString(), props), name);
		assertEquals("a\\\\<a", name.toString());

		name = new CompoundName("<a\\/a>", props);
		assertNameEquals(name, "a\\/a");
		assertEquals(new CompoundName(name.toString(), props), name);
		assertEquals("<a\\/a>", name.toString());
	}

	public void testConstructor_WithProps_DSEQ_Eq2()
			throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSEQ_Eq2()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.endquote2", ">");

		name = new CompoundName("<a/a>/b/c", props);
		assertNameEquals(name, "<a", "a>", "b", "c");

		name = new CompoundName(">a/a>/b/c", props);
		assertNameEquals(name, "a/a", "b", "c");
		assertEquals(new CompoundName(name.toString(), props), name);
	}

	public void testConstructor_WithProps_DSEQ_DefaultIgnoreCase()
			throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSEQ_DefaultIgnoreCase()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");

		name = new CompoundName("a/b/c", props);
		assertFalse(name.equals(new CompoundName("A/B/C/", props)));
	}

	public void testConstructor_WithProps_DSEQ_DefaultTrimBlanks()
			throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSEQ_DefaultTrimBlanks()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");

		name = new CompoundName("a/b/c", props);
		assertFalse(name.equals(new CompoundName(" a / b / c ", props)));
	}

	public void testConstructor_WithProps_DSEQAv() throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSEQAv()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.separator.ava", ",");

		assertInvalidName("'a','b'/'c'", props);
		assertInvalidName("'a', 'b'/'c'", props);
		assertInvalidName("'a', 'b' /'c'", props);
		assertInvalidName("'a', 'b' / 'c'", props);
		assertInvalidName("'a'\\,'b'/'c'", props);
		name = new CompoundName(",/a", props);
		assertNameEquals(name, ",", "a");
		name = new CompoundName("a/,", props);
		assertNameEquals(name, "a", ",");
	}

	public void testConstructor_WithProps_DSEQTv() throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSEQTv()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.separator.typeval", "=");

		assertInvalidName("'a'=b", props);
		name = new CompoundName("a='b/b'/c", props);
		assertNameEquals(name, "a='b/b'", "c");
		name = new CompoundName("a=\\'b/b'/c", props);
		assertNameEquals(name, "a='b", "b'", "c");
		name = new CompoundName("a\\='b/b'/c", props);
		assertNameEquals(name, "a\\='b/b'", "c");
		assertInvalidName("'a/a'=b/c", props);
		name = new CompoundName("\\'a/a'=b/c", props);
		assertNameEquals(name, "'a", "a'=b", "c");
		assertInvalidName("'a/a\\'=b/c", props);
		name = new CompoundName("a=b,c=d/e", props);
		assertNameEquals(name, "a=b,c=d", "e");
	}

	public void testConstructor_WithProps_DSEQAvTv()
			throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSEQAvTv()");
		CompoundName name, name2;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.separator.ava", ",");
		props.put("jndi.syntax.separator.typeval", "=");

		name = new CompoundName("a='b'/c", props);
		assertNameEquals(name, "a='b'", "c");
		assertInvalidName("a='b',/c", props);
		assertInvalidName("a='b',c/d", props);
		assertInvalidName("a='b',c=d/e", props);
		name = new CompoundName("a=<b/b>/abc", props);
		assertNameEquals(name, "a=<b/b>", "abc");
		name = new CompoundName("a=b='c'/d", props);
		assertNameEquals(name, "a=b='c'", "d");
		assertInvalidName("CN=<Ima Random>, O=HMY, C=UK", props);
		assertInvalidName("CN=<Ima Random, O=HMY, C=UK", props);
		name = new CompoundName("CN=<Ima Random>/O=HMY/C=UK", props);
		assertNameEquals(name, "CN=<Ima Random>", "O=HMY", "C=UK");
		assertInvalidName("CN=<Ima Random/O=HMY/C=UK", props);
		name = new CompoundName("a=b'b',c='d'/abc'", props);
		assertNameEquals(name, "a=b'b',c='d'", "abc'");
		assertInvalidName("a='b\\',c='d'/abc", props);
		assertInvalidName("a='b',/abc", props);
		assertInvalidName("a='b',abc/abc", props);
		assertInvalidName("'<a>'=b,c=d/abc", props);
		assertInvalidName("'a=b,c=d/abc", props);
		name = new CompoundName("a=,c=d/abc", props);
		assertNameEquals(name, "a=,c=d", "abc");
		name = new CompoundName("=b,c=d/abc", props);
		assertNameEquals(name, "=b,c=d", "abc");
		assertInvalidName("''=b,c=d/abc", props);
		assertInvalidName("a='b\\,b',c=d/abc", props);
		assertInvalidName("a\\=a='b',c=d/abc", props);
		name = new CompoundName(",,/,,", props);
		assertNameEquals(name, ",,", ",,");
		name = new CompoundName("a=/bc", props);
		assertNameEquals(name, "a=", "bc");
		name = new CompoundName("=b/bc", props);
		assertNameEquals(name, "=b", "bc");
		name = new CompoundName("=/bc", props);
		assertNameEquals(name, "=", "bc");

		// Does escape work for typeval?
		name = new CompoundName("a\\='b/c'/d", props);
		assertEquals("<a\\='b/c'>/d", name.toString());
		assertNameEquals(name, "a\\='b/c'", "d");

		name = new CompoundName("a=b, c=d/a=b, c=d", props);
		name2 = new CompoundName("a=b,c=d/a=b,c=d", props);
		assertFalse(name.equals(name2));
		props.put("jndi.syntax.trimblanks", "true");
		name = new CompoundName("a=b, c=d/a=b, c=d", props);
		name2 = new CompoundName("a=b,c=d/a=b,c=d", props);
		assertFalse(name.equals(name2));

		name = new CompoundName("a=b,c=d/a=b,c=d", props);
		name2 = new CompoundName("A=B,C=D/A=B,C=D", props);
		assertFalse(name.equals(name2));
		props.put("jndi.syntax.ignorecase", "true");
		name = new CompoundName("a=b,c=d/a=b,c=d", props);
		name2 = new CompoundName("A=B,C=D/A=B,C=D", props);
		assertTrue(name.equals(name2));

	}

	public void testConstructor_WithProps_DSEQAvTv_sameSE()
			throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSEQAvTv_sameSE()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "/");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.separator.ava", ",");
		props.put("jndi.syntax.separator.typeval", "=");

		name = new CompoundName("/", props);
		assertNameEquals(name, "");
		name = new CompoundName("//", props);
		assertNameEquals(name, "", "");
		name = new CompoundName("a/b/c", props);
		assertNameEquals(name, "a", "b", "c");
		name = new CompoundName("a//b/c", props);
		assertNameEquals(name, "a", "", "b", "c");
	}

	public void testConstructor_WithProps_DSEQAvTv_sameSQ()
			throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSEQAvTv_sameSQ()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "/");
		props.put("jndi.syntax.endquote2", "/");
		props.put("jndi.syntax.separator.ava", ",");
		props.put("jndi.syntax.separator.typeval", "=");

		assertInvalidName("/", props);
		name = new CompoundName("//", props);
		assertNameEquals(name, "");
		name = new CompoundName("a/b/c", props);
		assertNameEquals(name, "a", "b", "c");
		name = new CompoundName("/a//b/c", props);
		assertNameEquals(name, "a", "b", "c");
	}

	public void testConstructor_WithProps_DSEQAvTv_sameEQ()
			throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSEQAvTv_sameEQ()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "\\");
		props.put("jndi.syntax.endquote2", "\\");
		props.put("jndi.syntax.separator.ava", ",");
		props.put("jndi.syntax.separator.typeval", "=");

		assertInvalidName("\\", props);
		name = new CompoundName("\\\\", props);
		assertNameEquals(name, "");
		name = new CompoundName("\\asdf\\", props);
		assertNameEquals(name, "asdf");
		name = new CompoundName("\\asdf\\/asdf", props);
		assertNameEquals(name, "asdf", "asdf");
	}

	public void testConstructor_WithProps_DSEQAvTv_sameSAv()
			throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSEQAvTv_sameSAv()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.separator.ava", "/");
		props.put("jndi.syntax.separator.typeval", "=");

		name = new CompoundName("//", props);
		assertNameEquals(name, "", "");
		name = new CompoundName("a/b/c", props);
		assertNameEquals(name, "a", "b", "c");
		name = new CompoundName("a=b/c=d", props);
		assertNameEquals(name, "a=b", "c=d");
	}

	public void testConstructor_WithProps_DSEQAvTv_sameSTv()
			throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSEQAvTv_sameSTv()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.separator.ava", ",");
		props.put("jndi.syntax.separator.typeval", "/");

		name = new CompoundName("//", props);
		assertNameEquals(name, "", "");
		name = new CompoundName("a/b/c", props);
		assertNameEquals(name, "a", "b", "c");
		name = new CompoundName("a/b,c/d", props);
		assertNameEquals(name, "a", "b,c", "d");
	}

	public void testConstructor_WithProps_DSQ() throws InvalidNameException {
		log.setMethod("testConstructor_WithProps_DSQ()");
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.ignorecase", "false");
		props.put("jndi.syntax.trimblanks", "false");
		props.put("jndi.syntax.separator.ava", ",");
		props.put("jndi.syntax.separator.typeval", "=");

		testBehavior("'a<b>c'", props);
	}

	public void testConstructor_WithIrregularProps() throws InvalidNameException {
		log.setMethod("testConstructor_WithIrregularProps()");
		try{
		    new CompoundName("name", new UncloneableProperties());
		}catch(UnsupportedOperationException e){
		    fail("unexpected UnsupportedOperationException");
		}
	}

	public void testConstructor_Advanced() throws InvalidNameException {
		log.setMethod("testConstructor_Advanced()");

		assertNameEmpty(new CompoundName("", props));
		assertNameEquals(new CompoundName("/", props), "");
		assertNameEquals(new CompoundName("//", props), "", "");
		assertNameEquals(new CompoundName("/a/", props), "", "a", "");
		assertNameEquals(new CompoundName("//a/", props), "", "", "a", "");
		assertNameEquals(new CompoundName("a/b/c/", props), "a", "b", "c", "");
		assertNameEquals(new CompoundName("/a/b/c", props), "", "a", "b", "c");

		assertNameEquals(new CompoundName("a/\\/b/c", props), "a", "/b", "c");
		assertNameEquals(new CompoundName("a/\\b/c", props), "a", "\\b", "c");
		assertInvalidName("a/b\\", props);

		assertNameEquals(new CompoundName("a/<b/>/c", props), "a", "b/", "c");
		assertNameEquals(new CompoundName("a/b<b/>b/c", props), "a", "b<b",
				">b", "c");
		assertNameEquals(new CompoundName("a/<b</>/c", props), "a", "b</", "c");
		assertNameEquals(new CompoundName("a/\\/b>>>/c", props), "a", "/b>>>",
				"c");
		assertNameEquals(new CompoundName("a/</b\\a\\>b>/c", props), "a",
				"/b\\a>b", "c");
		assertInvalidName("a/<b/>b/c", props);
		assertInvalidName("a/</b>>/c", props);

		assertNameEquals(new CompoundName("a/'b/'/c", props), "a", "b/", "c");
		assertNameEquals(new CompoundName("a/'/b\\a\\'b'/c", props), "a",
				"/b\\a'b", "c");
		assertInvalidName("a/b'b/'b/c", props);
		assertInvalidName("a/'b/'b/c", props);
	}

	public void testConstructor_MultiChar_Separator()
			throws InvalidNameException {
		log.setMethod("testConstructor_MultiChar_Separator()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "//");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.separator.ava", ",");
		props.put("jndi.syntax.separator.typeval", "=");

		name = new CompoundName("/", props);
		assertNameEquals(name, "/");
		name = new CompoundName("//", props);
		assertNameEquals(name, "");
		name = new CompoundName("///", props);
		assertNameEquals(name, "", "/");
		name = new CompoundName("////", props);
		assertNameEquals(name, "", "");
	}

	public void testConstructor_MultiChar_Escape() throws InvalidNameException {
		log.setMethod("testConstructor_MultiChar_Escape()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "@@");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.separator.ava", ",");
		props.put("jndi.syntax.separator.typeval", "=");

		name = new CompoundName("a@/b", props);
		assertNameEquals(name, "a@", "b");
		name = new CompoundName("a@@/b", props);
		assertNameEquals(name, "a/b");
		name = new CompoundName("a@@@/b", props);
		assertNameEquals(name, "a@/b");
		name = new CompoundName("a@@@@/b", props);
		assertNameEquals(name, "a@@", "b");
		/*
		 * IGNORE ODD CASE name = new CompoundName("a@@@@@/b", props);
		 * assertNameEquals(name, "a@/b");
		 */
	}

	public void testConstructor_MultiChar_Escape_Advanced()
			throws InvalidNameException {
		log.setMethod("testConstructor_MultiChar_Escape_Advanced()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "//");
		props.put("jndi.syntax.escape", "abc");
		props.put("jndi.syntax.beginquote", "<<");
		props.put("jndi.syntax.endquote", ">>");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.separator.ava", ",");
		props.put("jndi.syntax.separator.typeval", "=");

		name = new CompoundName("abca", props);
		assertNameEquals(name, "abca");
		name = new CompoundName("abcb", props);
		assertNameEquals(name, "abcb");
		name = new CompoundName("abcab", props);
		assertNameEquals(name, "abcab");
		name = new CompoundName("abcabc", props);
		assertNameEquals(name, "abc");
		assertInvalidName("xyzabc", props);
		assertInvalidName("<<abc>>", props);
		name = new CompoundName("abc//xyz", props);
		assertNameEquals(name, "//xyz");
		/*
		 * IGNORE ODD CASE name = new CompoundName("abc///xyz", props);
		 * assertNameEquals(name, "/", "xyz");
		 */
		/*
		 * IGNORE ODD CASE name = new CompoundName("<<abc>>>", props);
		 * assertNameEquals(name, ">");
		 */
		/*
		 * IGNORE ODD CASE assertInvalidName("<<abc>>>>", props);
		 */
	}

	public void testConstructor_MultiChar_Quote1() throws InvalidNameException {
		log.setMethod("testConstructor_MultiChar_Quote1()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<<");
		props.put("jndi.syntax.endquote", ">>");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.separator.ava", ",");
		props.put("jndi.syntax.separator.typeval", "=");

		name = new CompoundName("<a/b>", props);
		assertNameEquals(name, "<a", "b>");
		assertInvalidName("<<a/b>", props);
		name = new CompoundName("<<a/b>>", props);
		assertNameEquals(name, "a/b");
		name = new CompoundName("<<<a/b>>", props);
		assertNameEquals(name, "<a/b");
		assertInvalidName("<<a/b>>>", props);
	}

	public void testConstructor_MultiChar_Quote2() throws InvalidNameException {
		log.setMethod("testConstructor_MultiChar_Quote2()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "''");
		props.put("jndi.syntax.separator.ava", ",");
		props.put("jndi.syntax.separator.typeval", "=");

		name = new CompoundName("'a/b'", props);
		assertNameEquals(name, "'a", "b'");
		assertInvalidName("''a/b'", props);
		name = new CompoundName("''a/b''", props);
		assertNameEquals(name, "a/b");
		name = new CompoundName("'''a/b''", props);
		assertNameEquals(name, "'a/b");
		assertInvalidName("''a/b'''", props);
	}

	public void testConstructor_MultiChar_Typeval() throws InvalidNameException {
		log.setMethod("testConstructor_MultiChar_Typeval()");
		CompoundName name;
		props.clear();
		props.put("jndi.syntax.direction", "left_to_right");
		props.put("jndi.syntax.separator", "/");
		props.put("jndi.syntax.escape", "\\");
		props.put("jndi.syntax.beginquote", "<");
		props.put("jndi.syntax.endquote", ">");
		props.put("jndi.syntax.beginquote2", "'");
		props.put("jndi.syntax.endquote2", "'");
		props.put("jndi.syntax.separator.ava", ",");
		props.put("jndi.syntax.separator.typeval", "==");

		name = new CompoundName("a='b/c'/d", props);
		assertNameEquals(name, "a='b", "c'", "d");
		name = new CompoundName("a=='b/c'/d", props);
		assertNameEquals(name, "a=='b/c'", "d");
		/*
		 * IGNORE ODD CASE name = new CompoundName("a==='b/c'/d", props);
		 * assertNameEquals(name, "a==='b/c'", "d");
		 */
	}

	public void testGetAll() throws InvalidNameException {
		log.setMethod("testGetAll()");
		CompoundName name;
		Enumeration<?> enumeration;

		// has more than one elements
		name = new CompoundName("a/b/c", props);
		enumeration = name.getAll();
		assertTrue(enumeration.hasMoreElements());
		assertEquals("a", enumeration.nextElement());
		assertTrue(enumeration.hasMoreElements());
		assertEquals("b", enumeration.nextElement());
		assertTrue(enumeration.hasMoreElements());
		assertEquals("c", enumeration.nextElement());
		assertFalse(enumeration.hasMoreElements());

		// has no elements
		name = new CompoundName("", props);
		enumeration = name.getAll();
		assertFalse(enumeration.hasMoreElements());
	}

	public void testGet() throws InvalidNameException {
		log.setMethod("testGet()");
		CompoundName name;

		// has more than one elements
		name = new CompoundName("a/b/c", props);
		assertEquals("a", name.get(0));
		assertEquals("b", name.get(1));
		assertEquals("c", name.get(2));
		try {
			name.get(-1);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			name.get(3);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}

		// has no elements
		name = new CompoundName("", props);
		try {
			name.get(0);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}

	public void testGetPrefix() throws InvalidNameException {
		log.setMethod("testGetPrefix()");
		CompoundName name;

		// has more than one elements
		name = new CompoundName("a/b/c", props);
		assertNameEmpty(name.getPrefix(0));
		assertNameEquals(name.getPrefix(1), "a");
		assertNameEquals(name.getPrefix(2), "a", "b");
		assertNameEquals(name.getPrefix(3), "a", "b", "c");
		try {
			name.getPrefix(-1);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			name.getPrefix(4);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}

		// has no elements
		name = new CompoundName("", props);
		assertNameEmpty(name.getPrefix(0));
		try {
			name.getPrefix(-1);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			name.getPrefix(1);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}

	public void testGetSuffix() throws InvalidNameException {
		log.setMethod("testGetSuffix()");
		CompoundName name;

		// has more than one elements
		name = new CompoundName("a/b/c", props);
		assertNameEquals(name.getSuffix(0), "a", "b", "c");
		assertNameEquals(name.getSuffix(1), "b", "c");
		assertNameEquals(name.getSuffix(2), "c");
        // Follow spec: If posn is equal to size(), an empty compound name is returned.
        assertNameEmpty(name.getSuffix(3));
		try {
			name.getPrefix(-1);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			name.getPrefix(4);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}

		// has no elements
		name = new CompoundName("", props);
		assertNameEmpty(name.getPrefix(0));
		try {
			name.getPrefix(-1);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			name.getPrefix(1);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}

	public void testAddAll() throws InvalidNameException {
		log.setMethod("testAddAll()");
		CompoundName name;

		name = new CompoundName("", props);
		assertNameEmpty(name);
		name.addAll(new CompoundName("a", props));
		assertNameEquals(name, "a");
		name.addAll(new CompoundName("b/c", props));
		assertNameEquals(name, "a", "b", "c");
	}

	public void testAddAll_NotCompoundName() throws InvalidNameException {
		log.setMethod("testAddAll_NotCompoundName()");
		CompoundName name;

		name = new CompoundName("", props);
		try {
			name.addAll(new CompositeName("a/b/c"));
			fail("should throw InvalidNameException");
		} catch (InvalidNameException e) {
		}
	}

	public void testAddAll_Null() throws InvalidNameException {
		log.setMethod("testAddAll_Null()");
		CompoundName name;

		name = new CompoundName("", props);
		try {
			name.addAll(null);
			fail("should throw NullPointerException");
		} catch (NullPointerException e) {
		}
	}

	public void testAddAll_Flat() throws InvalidNameException {
		log.setMethod("testAddAll_Flat()");
		CompoundName name;

		name = new CompoundName("aaa", new Properties());
		try {
			name.addAll(new CompoundName("bbb", new Properties()));
			fail("should throw InvalidNameException");
		} catch (InvalidNameException e) {
			// Expected
		}
	}

	public void testAddAll_Indexed() throws InvalidNameException {
		log.setMethod("testAddAll_Indexed()");
		CompoundName name;

		name = new CompoundName("", props);
		assertNameEmpty(name);
		name.addAll(0, new CompoundName("a", props));
		assertNameEquals(name, "a");
		name.addAll(0, new CompoundName("b", props));
		assertNameEquals(name, "b", "a");
		name.addAll(1, new CompoundName("", props));
		assertNameEquals(name, "b", "a");
		name.addAll(2, new CompoundName("c", props));
		assertNameEquals(name, "b", "a", "c");

		try {
			name.addAll(-1, new CompoundName("d", props));
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			name.addAll(4, new CompoundName("d", props));
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}

	public void testAddAll_Indexed_NotCompoundName()
			throws InvalidNameException {
		log.setMethod("testAddAll_Indexed_NotCompoundName()");
		CompoundName name;

		name = new CompoundName("", props);
		try {
			name.addAll(0, new CompositeName("a/b/c"));
			fail("should throw InvalidNameException");
		} catch (InvalidNameException e) {
		}
	}

	public void testAddAll_Indexed_Null() throws InvalidNameException {
		log.setMethod("testAddAll_Indexed_Null()");
		CompoundName name;

		name = new CompoundName("", props);
		try {
			name.addAll(0, null);
			fail("should throw NullPointerException");
		} catch (NullPointerException e) {
		}
	}

	public void testAdd() throws InvalidNameException {
		log.setMethod("testAdd()");
		CompoundName name;

		name = new CompoundName("", props);
		assertNameEmpty(name);
		name.add("a");
		assertNameEquals(name, "a");
		name.add("'<b>/'");
		assertNameEquals(name, "a", "'<b>/'");
		name.add("'c");
		assertNameEquals(name, "a", "'<b>/'", "'c");

        // regression for HARMONY-2525
        name = new CompoundName("a", new Properties());
		try {
			name.add("b");
			fail("InvalidNameException expected");
		} catch (InvalidNameException e) {
			//expected
		}
	}

    // regression for HARMONY-2525
	public void testAdd_Null() throws InvalidNameException {
		log.setMethod("testAdd_Null()");
		CompoundName name;

        name = new CompoundName("", props);
		try {
			name.add(null);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			//expected
		}

		name = new CompoundName("", new Properties());
		try {
			name.add(null);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			//expected
		}
	}

	public void testAdd_Indexed() throws InvalidNameException {
		log.setMethod("testAdd_Indexed()");
		CompoundName name;

		name = new CompoundName("", props);
		assertNameEmpty(name);
		name.add(0, "a");
		assertNameEquals(name, "a");
		name.add(0, "b");
		assertNameEquals(name, "b", "a");
		name.add(1, "");
		assertNameEquals(name, "b", "", "a");
		try {
			name.addAll(-1, new CompoundName("d", props));
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
			//expected
		}
		try {
			name.addAll(5, new CompoundName("d", props));
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
			//expected
		}

        // regression for HARMONY-2525
		name = new CompoundName("a", new Properties());
		try {
			name.add(0, "b");
			fail("InvalidNameException expected");
		} catch (InvalidNameException e) {
			//expected
		}
		try {
			name.add(1, "b");
			fail("InvalidNameException expected");
		} catch (InvalidNameException e) {
			//expected
		}
		try {
			name.add(-1, "b");
			fail("InvalidNameException expected");
		} catch (InvalidNameException e) {
			//expected
		}
	}

    // regression for HARMONY-2525
    public void testAdd_Indexed_Null() throws InvalidNameException {
		log.setMethod("testAdd_Indexed_Null()");
		CompoundName name;

		name = new CompoundName("", props);
		try {
			name.add(0, null);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			//expected
		}

		// regression test for HARMONY-1021
		name = new CompoundName("", props);
		try {
			name.add(11, null);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			//expected
		}

		name = new CompoundName("", new Properties());
		try {
			name.add(0, null);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			//expected
		}
	}

	public void testRemove() throws InvalidNameException {
		log.setMethod("testRemove()");
		CompoundName name;

		name = new CompoundName("a/b/c/d", props);
		assertEquals("a", name.remove(0));
		assertNameEquals(name, "b", "c", "d");
		assertEquals("c", name.remove(1));
		assertNameEquals(name, "b", "d");
		assertEquals("d", name.remove(1));
		assertNameEquals(name, "b");
		assertEquals("b", name.remove(0));
		assertNameEmpty(name);

		try {
			name.remove(0);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		name = new CompoundName("a/b/c/d", props);
		try {
			name.remove(-1);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		try {
			name.remove(4);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}

	public void testClone() throws InvalidNameException {
		log.setMethod("testClone()");
		CompoundName name, name2;

		name = new CompoundName("a/b", props);
		name2 = (CompoundName) name.clone();
		assertNameEquals(name2, "a", "b");

		name.add("c");
		assertNameEquals(name, "a", "b", "c");
		assertNameEquals(name2, "a", "b");

		name2.remove(0);
		assertNameEquals(name, "a", "b", "c");
		assertNameEquals(name2, "b");
	}

	public void testClone_1() throws InvalidNameException {
		log.setMethod("testClone_1()");
		CompoundName name = null, name2 = null;

		try{
		    name = new CompoundName("name", new UncloneableProperties());
		}catch(UnsupportedOperationException e){
		    fail("unexpected UnsupportedOperationException");
		}

		try{
		    name2 = (CompoundName)name.clone();
		}catch(UnsupportedOperationException e){
		    fail("unexpected UnsupportedOperationException");
		}
		assertEquals(name, name2);
	}

	public void testCompareTo() throws InvalidNameException {
		log.setMethod("testCompareTo()");

		assertEquals(-1, new CompoundName("a/b/c/d", props)
				.compareTo(new CompoundName("a/c/c/d", props)));
		assertEquals(-1, new CompoundName("a/b/c/d", props)
				.compareTo(new CompoundName("ab/c/d", props)));
		assertEquals(-1, new CompoundName("a/b/c/d", props)
				.compareTo(new CompoundName("a/b/c/d/e", props)));
	}

	public void testCompareTo_Null() throws InvalidNameException {
		log.setMethod("testCompareTo_Null()");
		CompoundName name;

		name = new CompoundName("a/b/c/d", props);
		try {
			name.compareTo(null);
			fail("compareTo(null) should throw ClassCastException");
		} catch (ClassCastException e) {
		}
	}

	public void testCompareTo_NotCompoundName() throws InvalidNameException {
		log.setMethod("testCompareTo_NotCompoundName()");
		CompoundName name;

		name = new CompoundName("a/b/c/d", props);
		try {
			name.compareTo(new CompositeName("a/b/c"));
			fail("compareTo(CompositeName) should throw ClassCastException");
		} catch (ClassCastException e) {
		}
	}

	public void testCompareTo_IgnoreCaseAndTrimBlanks()
			throws InvalidNameException {
		log.setMethod("testCompareTo_IgnoreCaseAndTrimBlanks()");

		props.put("jndi.syntax.ignorecase", "false");
		props.put("jndi.syntax.trimblanks", "false");
		CompoundName abcd = new CompoundName("a/b/c/d", props);

		props2.put("jndi.syntax.ignorecase", "true");
		props2.put("jndi.syntax.trimblanks", "false");
		CompoundName abcd_ic = new CompoundName("a/b/c/d", props2);
		CompoundName ABCD_ic = new CompoundName("A/B/C/D", props2);
		assertEquals(0, abcd_ic.compareTo(ABCD_ic));
		assertEquals(0, ABCD_ic.compareTo(abcd_ic));
		assertEquals(0, abcd_ic.compareTo(abcd));
		assertEquals(0, abcd.compareTo(abcd_ic));
		assertEquals(0, ABCD_ic.compareTo(abcd));
		assertEquals(32, abcd.compareTo(ABCD_ic));

		props2.put("jndi.syntax.ignorecase", "false");
		props2.put("jndi.syntax.trimblanks", "true");
		CompoundName abcd_tb = new CompoundName("a/b/c/d", props2);
		CompoundName _a_b_c_d_tb = new CompoundName(" a / b / c / d ", props2);
		assertEquals(0, abcd_tb.compareTo(_a_b_c_d_tb));
		assertEquals(0, _a_b_c_d_tb.compareTo(abcd_tb));
		assertEquals(0, abcd_tb.compareTo(abcd));
		assertEquals(0, abcd.compareTo(abcd_tb));
		assertEquals(65, abcd.compareTo(_a_b_c_d_tb));
		assertEquals(0, _a_b_c_d_tb.compareTo(abcd));
	}

	public void testSize() throws InvalidNameException {
		log.setMethod("testSize()");
		CompoundName name;

		name = new CompoundName("a/b/c/d", props);
		assertEquals(4, name.size());
		name.remove(0);
		assertEquals(3, name.size());
		name.remove(0);
		assertEquals(2, name.size());
		name.remove(0);
		assertEquals(1, name.size());
		name.remove(0);
		assertEquals(0, name.size());
	}

	public void testIsEmpty() throws InvalidNameException {
		log.setMethod("testIsEmpty()");
		CompoundName name;

		name = new CompoundName("a/b/c/d", props);
		assertFalse(name.isEmpty());
		name.remove(0);
		assertFalse(name.isEmpty());
		name.remove(0);
		assertFalse(name.isEmpty());
		name.remove(0);
		assertFalse(name.isEmpty());
		name.remove(0);
		assertTrue(name.isEmpty());
	}

	public void testStartsWith() throws InvalidNameException {
		log.setMethod("testStartsWith()");
		CompoundName name;

		name = new CompoundName("a/b/c/d", props);
		assertTrue(name.startsWith(new CompoundName("", props)));
		assertTrue(name.startsWith(new CompoundName("a/b", props)));
		assertTrue(name.startsWith(new CompoundName("a/b/c/d", props)));

		assertFalse(name.startsWith(new CompoundName("b", props)));
		assertFalse(name.startsWith(new CompoundName("a/b/c/d/e", props)));
	}

	public void testStartsWith_NotCoumpoundName() throws InvalidNameException {
		log.setMethod("testStartsWith_NotCoumpoundName()");
		CompoundName name;

		name = new CompoundName("a/b/c/d", props);
		assertFalse(name.startsWith(new CompositeName("a/b")));
	}

	public void testStartsWith_Null() throws InvalidNameException {
		log.setMethod("testStartsWith_Null()");
		CompoundName name;

		name = new CompoundName("a/b/c/d", props);
		assertFalse(name.startsWith(null));
	}

	public void testEndsWith() throws InvalidNameException {
		log.setMethod("testEndsWith()");
		CompoundName name;

		name = new CompoundName("a/b/c/d", props);
		assertTrue(name.endsWith(new CompoundName("", props)));
		assertTrue(name.endsWith(new CompoundName("c/d", props)));
		assertTrue(name.endsWith(new CompoundName("a/b/c/d", props)));

		assertFalse(name.endsWith(new CompoundName("b", props)));
		assertFalse(name.endsWith(new CompoundName("a/b/c/d/e", props)));
	}

	public void testEndsWith_NotCoumpoundName() throws InvalidNameException {
		log.setMethod("testEndsWith_NotCoumpoundName()");
		CompoundName name;

		name = new CompoundName("a/b/c/d", props);
		assertFalse(name.endsWith(new CompositeName("a/b")));
	}

	public void testEndsWith_Null() throws InvalidNameException {
		log.setMethod("testEndsWith_Null()");
		CompoundName name;

		name = new CompoundName("a/b/c/d", props);
		assertFalse(name.endsWith(null));
	}

	public void testProperties_Separator() throws InvalidNameException {
		log.setMethod("testProperties_Separator()");
		CompoundName name;
		props.put("jndi.syntax.separator", ":");

		// a different separator
		name = new CompoundName(":a:b", props);
		assertNameEquals(name, "", "a", "b");
	}

	public void testProperties_Direction() throws InvalidNameException {
		log.setMethod("testProperties_Direction()");
		CompoundName name;
		props.put("jndi.syntax.direction", "right_to_left");

		// right to left
		name = new CompoundName("c/b/a", props);
		assertNameEquals(name, "a", "b", "c");

		// flat
		props.put("jndi.syntax.direction", "flat");
		name = new CompoundName("c/b/a", props);
		assertNameEquals(name, "c/b/a");

		// flat with no separator
		props.remove("jndi.syntax.separator");
		name = new CompoundName("c/b/a", props);
		assertNameEquals(name, "c/b/a");

		// flat other cases
		try {
			name = new CompoundName("<\"'''213^!$!@#$a/b//c///", props);
			fail();
		} catch (InvalidNameException e) {
		}
		name = new CompoundName("\"'''213^!$!@#$a/b//c///", props);
		assertNameEquals(name, "\"'''213^!$!@#$a/b//c///");
	}

	public void testProperties_EscapeAndQuote() throws InvalidNameException {
		log.setMethod("testProperties_EscapeAndQuote()");
		CompoundName name;

		name = new CompoundName("ab<abc>/de", props);
		assertNameEquals(name, "ab<abc>", "de");

		name = new CompoundName("<a>/<b//>/'<>sadf/'", props);
		assertNameEquals(name, "a", "b//", "<>sadf/");

		name = new CompoundName("<a\\>>/\\//'<>sadf/\\''", props);
		assertNameEquals(name, "a>", "/", "<>sadf/'");

		try {
			name = new CompoundName("<aba<b>c>/de", props);
			fail();
		} catch (InvalidNameException e) {
		}
	}

	public void testProperties_IgnoreCaseAndTrimBlank()
			throws InvalidNameException {
		log.setMethod("testProperties_IgnoreCaseAndTrimBlank()");

		props.put("jndi.syntax.ignorecase", "false");
		props.put("jndi.syntax.trimblanks", "false");
		CompoundName abcd = new CompoundName("a/b/c/d", props);

		props2.put("jndi.syntax.ignorecase", "true");
		props2.put("jndi.syntax.trimblanks", "false");
		CompoundName abcd_ic = new CompoundName("a/b/c/d", props2);
		CompoundName ABCD_ic = new CompoundName("A/B/C/D", props2);
		assertTrue(abcd_ic.equals(ABCD_ic));
		assertTrue(ABCD_ic.equals(abcd_ic));
		assertTrue(abcd_ic.equals(abcd));
		assertTrue(abcd.equals(abcd_ic));
		assertTrue(ABCD_ic.equals(abcd));
		assertFalse(abcd.equals(ABCD_ic));

		props2.put("jndi.syntax.ignorecase", "false");
		props2.put("jndi.syntax.trimblanks", "true");
		CompoundName abcd_tb = new CompoundName("a/b/c/d", props2);
		CompoundName _a_b_c_d_tb = new CompoundName(" a / b / c / d ", props2);
		assertTrue(abcd_tb.equals(_a_b_c_d_tb));
		assertTrue(_a_b_c_d_tb.equals(abcd_tb));
		assertTrue(abcd_tb.equals(abcd));
		assertTrue(abcd.equals(abcd_tb));
		assertFalse(abcd.equals(_a_b_c_d_tb));
		assertTrue(_a_b_c_d_tb.equals(abcd));
	}

	public void testProperties_IgnoreCaseAndTrimBlank_DefaultValue()
			throws InvalidNameException {
		log.setMethod("testProperties_IgnoreCaseAndTrimBlank_DefaultValue()");

		props.remove("jndi.syntax.ignorecase");
		props.remove("jndi.syntax.trimblanks");
		CompoundName abcd = new CompoundName("a/b/c/d", props);
		CompoundName ABCD = new CompoundName("A/B/C/D", props);
		CompoundName _a_b_c_d_ = new CompoundName(" a / b / c / d ", props);
		assertFalse(abcd.equals(ABCD));
		assertFalse(abcd.equals(_a_b_c_d_));

		props.remove("jndi.syntax.beginquote");
		props.remove("jndi.syntax.endquote");
		props.remove("jndi.syntax.beginquote2");
		props.remove("jndi.syntax.endquote2");
		CompoundName quote = new CompoundName("a/\\/b/c", props);
		assertEquals("a/\\/b/c", quote.toString());
	}

	public void testEquals() throws InvalidNameException {
		log.setMethod("testEquals()");
		CompoundName name;

		name = new CompoundName("a/b/c", props);
		assertTrue(name.equals(new CompoundName("a/b/c", props)));
		assertFalse(name.equals(new CompositeName("a/b/c")));

		CompoundName empty1 = new CompoundName("", props);
		CompoundName empty2 = new CompoundName("", new Properties());
		assertTrue(empty1.equals(empty2));
		assertTrue(empty2.equals(empty1));
	}

	public void testHashCode() throws InvalidNameException {
		log.setMethod("testHashCode()");
		CompoundName name;

		name = new CompoundName("a/b/c", props);
		assertTrue(name.hashCode() == new CompoundName("a/b/c", props)
				.hashCode());
		assertEquals(294, name.hashCode());

		props.put("jndi.syntax.ignorecase", "true");
		props.put("jndi.syntax.trimblanks", "false");
		name = new CompoundName(" A / B / c ", props);
		assertEquals(101466, name.hashCode());

		props.put("jndi.syntax.ignorecase", "false");
		props.put("jndi.syntax.trimblanks", "true");
		name = new CompoundName(" A / B / c ", props);
		assertEquals(230, name.hashCode());

		props.put("jndi.syntax.ignorecase", "true");
		props.put("jndi.syntax.trimblanks", "true");
		name = new CompoundName(" A / B / c ", props);
		assertEquals(294, name.hashCode());
	}
    
	public void testToString() throws InvalidNameException {
		log.setMethod("testToString()");

		testToString("", "");
		testToString("/", "/");
		testToString("//", "//");
		testToString("/a/", "/a/");
		testToString("//a/", "//a/");
		testToString("a/b/c/", "a/b/c/");
		testToString("/a/b/c", "/a/b/c");

		testToString("a/\\/b/c", "a/</b>/c");
		testToString("a/b\\", "fail");

		testToString("a/<b/>/c", "a/<b/>/c");
		testToString("a/b<b/>b/c", "a/b<b/>b/c");
		testToString("a/<b/>b/c", "fail");
		testToString("a/<b</>/c", "a/<b</>/c");
		testToString("a/\\/b>>>/c", "a/</b\\>\\>\\>>/c");
		testToString("a/</b>>/c", "fail");
		testToString("a/</b>>/c", "fail");
		testToString("a/</b\\a\\>b>/c", "a/</b\\a\\>b>/c");

		testToString("a/'b/'/c", "a/<b/>/c");
		testToString("a/b'b/'b/c", "fail");
		testToString("a/'b/'b/c", "fail");
		testToString("a/'/b\\a\\'b'/c", "a/</b\\a'b>/c");

	}
    
    public void testToStringRightToLeft() throws Exception{
        
        CompoundName name = new CompoundName("a/b/c", props);
        assertEquals("a/b/c", name.toString());
    }

	private void testBehavior(String str, Properties p) {
		try {
			CompoundName name = new CompoundName(str, p);
			log.log(str + "\t" + name.toString() + "\t" + toString(name));
		} catch (Throwable e) {
			log.log(str + " with props " + p + " causes error", e);
		}
	}

	private void testToString(String str, String expected)
			throws InvalidNameException {
		CompoundName name = null;
		try {
            props.put("jndi.syntax.direction", "left_to_right");
			name = new CompoundName(str, props);
			if ("fail".equals(expected)) {
				fail("fail.equals()" + expected);
			}
            assertEquals(expected, name.toString());
            props.put("jndi.syntax.direction", "right_to_left");
            name = new CompoundName(str, props);
            if ("fail".equals(expected)) {
                fail("fail.equals()" + expected);
            }
            assertEquals(expected, name.toString());
		} catch (Exception e) {
			if (!"fail".equals(expected)) {
				fail(str + "," + expected + "," + e.getMessage());
			}
		}
	}

	private String toString(Name n) {
		StringBuffer buf = new StringBuffer();
		buf.append("[" + n.size() + "]{");
		for (int i = 0; i < n.size(); i++) {
			if (i > 0) {
				buf.append("|");
			}
			buf.append(n.get(i));
		}
		buf.append("}");
		return buf.toString();
	}

	private void assertNameEmpty(Name n) {
		assertNameEquals(n, new String[0]);
	}

	private void assertNameEquals(Name n, String elem1) {
		assertNameEquals(n, new String[] { elem1 });
	}

	private void assertNameEquals(Name n, String elem1, String elem2) {
		assertNameEquals(n, new String[] { elem1, elem2 });
	}

	private void assertNameEquals(Name n, String elem1, String elem2,
			String elem3) {
		assertNameEquals(n, new String[] { elem1, elem2, elem3 });
	}

	private void assertNameEquals(Name n, String elem1, String elem2,
			String elem3, String elem4) {
		assertNameEquals(n, new String[] { elem1, elem2, elem3, elem4 });
	}

	private void assertNameEquals(Name n, String[] elems) {

		try {
			// compare
			assertEquals(elems.length, n.size());

			for (int i = 0; i < n.size(); i++) {
				assertEquals(elems[i], n.get(i));
			}

			int i = 0;
			Enumeration<?> enumeration = n.getAll();
			while (enumeration.hasMoreElements()) {
				assertEquals(elems[i++], enumeration.nextElement());
			}

			assertNotNull(n.toString());
		} catch (AssertionFailedError e) {
			// log
			StringBuffer buf = new StringBuffer();
			buf.append("Assert name ");
			buf.append(toString(n));
			buf.append(" has elements [" + elems.length + "]{");
			for (int i = 0; i < elems.length; i++) {
				if (i > 0) {
					buf.append("|");
				}
				buf.append(elems[i]);
			}
			buf.append("}");
			log.log(buf.toString());

			throw e;
		}
	}

	private void assertInvalidName(String str, Properties p) {
		try {
			new CompoundName(str, p);
			fail(str + " with props " + p + " should be an invalid name.");
		} catch (InvalidNameException e) {
		}
	}

	public void testWriteReadObject() throws Exception {
		log.setMethod("testWriteReadObject()");
		CompoundName name = new CompoundName("a/b/c/d", props);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ObjectOutputStream stream = new ObjectOutputStream(bout);
		stream.writeObject(name);
		stream.close();
		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(
				bout.toByteArray()));
		CompoundName name2 = (CompoundName) in.readObject();
		assertTrue(name.equals(name2));
		in.close();
	}

	public void testSerializationCompatibility() throws Exception {
		log.setMethod("testSerializationCompatibility()");

		try {
			ObjectInputStream in = new ObjectInputStream(getClass()
                    .getResourceAsStream(
                            "/serialization/javax/naming/CompoundName.ser"));
			CompoundName name = (CompoundName) in.readObject();
			assertEquals(new CompoundName("a/b/c/d", props), name);
			in.close();
		} catch (Exception e) {
			log.log(e);
			throw e;
		}
	}

	public void testSerializationCompatibility_bad() throws Exception {
		log.setMethod("testSerializationCompatibility_bad()");

		try {
			ObjectInputStream in = new ObjectInputStream(getClass()
                    .getResourceAsStream(
                            "/serialization/javax/naming/CompoundName_bad.ser"));
			CompoundName name = (CompoundName) in.readObject();
			assertEquals(new CompoundName("a/b/c/d", props), name);
			in.close();
			fail("Should throw IllegalArgumentException");
		} catch (IllegalArgumentException e) {
		}
	}

	public void testStringIndexOf() {
		assertEquals(3, "abc".indexOf("", 3));
	}

	public void testToStringHang() throws InvalidNameException {
		Properties synt = new Properties();
		synt.put("jndi.syntax.separator", "|");
		synt.put("jndi.syntax.ignorecase", "true");
		synt.put("jndi.syntax.direction", "left_to_right");
		synt.put("jndi.syntax.escape", "$");
		synt.put("jndi.syntax.beginquote", "`");
		synt.put("jndi.syntax.beginquote2", "/");

		String input = "'liberty|valley|army";
		CompoundName name = new CompoundName(input, synt);
		String output = name.toString();
		assertEquals(input, output);

		synt.put("jndi.syntax.separator", new String("||"));
		name = new CompoundName("||", synt);
		assertEquals("||", name.toString());
	}
    
    private static class UncloneableProperties extends Properties {
        private static final long serialVersionUID = 1L;

        @Override
        public Object clone() {
            throw new UnsupportedOperationException();
        }
    }
}
