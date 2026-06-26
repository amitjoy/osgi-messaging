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
package in.bytehue.messaging.mqtt5.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

public class ConverterAdapterTest {

	private ConverterAdapter adapter;

	@Before
	public void setup() {
		adapter = new ConverterAdapter();
	}

	@Test
	public void testConvertStringToInteger() {
		final Integer result = adapter.convert("42").to(Integer.class);
		assertEquals(Integer.valueOf(42), result);
	}

	@Test
	public void testConvertIntegerToString() {
		final String result = adapter.convert(42).to(String.class);
		assertEquals("42", result);
	}

	@Test
	public void testConvertNullReturnsNull() {
		final String result = adapter.convert(null).to(String.class);
		assertNull("Converting null should return null", result);
	}

	@Test
	public void testFunctionReturnsNonNull() {
		assertNotNull("function() should return a non-null Functioning", adapter.function());
	}

	@Test
	public void testNewConverterBuilderReturnsNonNull() {
		assertNotNull("newConverterBuilder() should return a non-null ConverterBuilder", adapter.newConverterBuilder());
	}

}
