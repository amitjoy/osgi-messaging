/*******************************************************************************
 * Copyright 2020-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package in.bytehue.messaging.mqtt5.provider.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import in.bytehue.messaging.mqtt5.provider.helper.FilterParser.Expression;

public class FilterParserTest {

	private FilterParser parser;

	@Before
	public void setup() {
		parser = new FilterParser();
	}

	@Test
	public void testSimpleEqualFilter() {
		final Expression expr = parser.parse("(key=value)");

		assertNotNull("Expression should not be null", expr);

		final Map<String, Object> matching = new HashMap<>();
		matching.put("key", "value");
		assertTrue("Should match when key equals value", expr.eval(matching));

		final Map<String, Object> nonMatching = new HashMap<>();
		nonMatching.put("key", "other");
		assertFalse("Should not match when key has different value", expr.eval(nonMatching));
	}

	@Test
	public void testAndFilter() {
		final Expression expr = parser.parse("(&(a=1)(b=2))");

		assertNotNull("Expression should not be null", expr);

		final Map<String, Object> matching = new HashMap<>();
		matching.put("a", "1");
		matching.put("b", "2");
		assertTrue("Should match when both conditions are met", expr.eval(matching));

		final Map<String, Object> partialMatch = new HashMap<>();
		partialMatch.put("a", "1");
		partialMatch.put("b", "3");
		assertFalse("Should not match when only one condition is met", expr.eval(partialMatch));
	}

	@Test
	public void testOrFilter() {
		final Expression expr = parser.parse("(|(a=1)(b=2))");

		assertNotNull("Expression should not be null", expr);

		final Map<String, Object> matchFirst = new HashMap<>();
		matchFirst.put("a", "1");
		assertTrue("Should match when first condition is met", expr.eval(matchFirst));

		final Map<String, Object> matchSecond = new HashMap<>();
		matchSecond.put("b", "2");
		assertTrue("Should match when second condition is met", expr.eval(matchSecond));

		final Map<String, Object> noMatch = new HashMap<>();
		noMatch.put("a", "x");
		noMatch.put("b", "y");
		assertFalse("Should not match when no condition is met", expr.eval(noMatch));
	}

	@Test
	public void testNotFilter() {
		final Expression expr = parser.parse("(!(a=1))");

		assertNotNull("Expression should not be null", expr);

		final Map<String, Object> matching = new HashMap<>();
		matching.put("a", "1");
		assertFalse("Should not match when inner expression matches", expr.eval(matching));

		final Map<String, Object> nonMatching = new HashMap<>();
		nonMatching.put("a", "2");
		assertTrue("Should match when inner expression does not match", expr.eval(nonMatching));
	}

	@Test
	public void testGreaterOrEqualFilter() {
		final Expression expr = parser.parse("(key>=5)");

		assertNotNull("Expression should not be null", expr);

		final Map<String, Object> matching = new HashMap<>();
		matching.put("key", 5);
		assertTrue("Should match when value equals", expr.eval(matching));

		final Map<String, Object> greater = new HashMap<>();
		greater.put("key", 10);
		assertTrue("Should match when value is greater", expr.eval(greater));

		final Map<String, Object> lesser = new HashMap<>();
		lesser.put("key", 3);
		assertFalse("Should not match when value is less", expr.eval(lesser));
	}

	@Test
	public void testLessOrEqualFilter() {
		final Expression expr = parser.parse("(key<=5)");

		assertNotNull("Expression should not be null", expr);

		final Map<String, Object> matching = new HashMap<>();
		matching.put("key", 5);
		assertTrue("Should match when value equals", expr.eval(matching));

		final Map<String, Object> lesser = new HashMap<>();
		lesser.put("key", 3);
		assertTrue("Should match when value is less", expr.eval(lesser));

		final Map<String, Object> greater = new HashMap<>();
		greater.put("key", 10);
		assertFalse("Should not match when value is greater", expr.eval(greater));
	}

	@Test
	public void testWildcardFilter() {
		final Expression expr = parser.parse("(key=foo*)");

		assertNotNull("Expression should not be null", expr);

		final Map<String, Object> matching = new HashMap<>();
		matching.put("key", "foobar");
		assertTrue("Should match wildcard prefix", expr.eval(matching));

		final Map<String, Object> nonMatching = new HashMap<>();
		nonMatching.put("key", "barfoo");
		assertFalse("Should not match when prefix differs", expr.eval(nonMatching));
	}

	@Test
	public void testApproximateFilter() {
		final Expression expr = parser.parse("(key~=value)");

		assertNotNull("Expression should not be null", expr);

		final Map<String, Object> matching = new HashMap<>();
		matching.put("key", "  VALUE  ");
		assertTrue("Should match case-insensitively with trimming", expr.eval(matching));
	}

	@Test
	public void testCacheHit() {
		final String filter = "(key=value)";
		final Expression first = parser.parse(filter);
		final Expression second = parser.parse(filter);

		assertSame("Second parse should return cached expression", first, second);
		assertEquals(1, parser.cache.size());
	}

	@Test(expected = RuntimeException.class)
	public void testInvalidFilterThrows() {
		parser.parse("invalid");
	}

}
